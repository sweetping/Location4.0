package com.example.location;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.conn.util.InetAddressUtils;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener,OnClickListener{

	public SensorManager mSensorManager;

	private BluetoothManager mBluetoothManager;
	
	private BluetoothAdapter mBluetoothAdapter;
	
	private WifiManager mWifiManager;
	
	private ConnectivityManager mConnManager;
	
	public List<ScanResult> listWifi;//wifi
	
	private boolean mStart = false;
	
	private static boolean mpuFlag = true;
	
	private static boolean magFlag = true;
	
	private TextView mLog;
	
	private Button clear;
	
	public  StringBuilder log;
	
	private TextView ipAddr;
	
	private SocketService mSocketService = null;
	
	private Map<String,Float> mapRssi;//ble
	
	private  Map<String,Float> mapMpu;//mpu
	
	private Map<String,Float> mapMag;//mag
	
	private Map<String,Float> mapWifi;//wifi

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("Activity","This is create");
		setContentView(R.layout.activity_main);
		getActionBar().setTitle("welcome");
		//getActionBar().setDisplayHomeAsUpEnabled(true);
		getOverflowMenu();
		mapMpu = new HashMap<String,Float>();
		mapMag = new HashMap<String,Float>();
		mapRssi = new HashMap<String,Float>();
		mapWifi = new HashMap<String,Float>();
		mLog = (TextView)findViewById(R.id.log);
		ipAddr = (TextView)findViewById(R.id.ipAddr);
		clear = (Button)findViewById(R.id.clear);
		clear.setOnClickListener(this);
		log = new StringBuilder();
		init();
	}
	
	private final OnRequestListener myOnRequestListener = new OnRequestListener()
	{
		@Override
		public void onRequest(String param) {
			// TODO Auto-generated method stub
			if(param.equals("WIFI"))
			{	
				getWifiList();
				mSocketService.setResponseData(param,mapWifi);
			}
			else if(param.equals("RSSI"))
			{
				scanLeDevice(true);
				mSocketService.setResponseData(param, mapRssi);
			}
			else if(param.equals("MPU"))
			{
				
				mSocketService.setResponseData(param, mapMpu);mpuFlag = false;
			}
			else if(param.equals("MAG"))
			{
				
				mSocketService.setResponseData(param, mapMag);magFlag = false;
			}
		}
	};
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mSocketService = ((SocketService.LocalBinder) service).getService();
            mSocketService.setOnRequestListener(myOnRequestListener);
            mSocketService.start();
            ipAddr.setText(getIpAddr() + ":" + mSocketService.defaultPort);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            
        	mSocketService = null;
            
        }
    };
    private final BroadcastReceiver mServiceReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			final String action = intent.getAction();
			if(action.equals(SocketService.REQUEST_CLIENT_WIFI))
			{
				log.append("Accept request from matlab: WIFI");
				show();
				log.append("The WIFI data is:");
				show();
				log.append(mapWifi.toString());
				show();		
			}
			else if(action.equals(SocketService.REQUEST_CLIENT_RSSI))
			{
				log.append("Accept request from matlab: RSSI");
				show();
				log.append("The RSSI data is:");
				show();
				log.append(mapRssi.toString());
				show();
			}
			else if(action.equals(SocketService.REQUEST_CLIENT_MPU))
			{
				log.append("Accept request from matlab: MPU");
				show();
				log.append("The MPU data is:");
				show();
				log.append(mapMpu.toString());
				show();
				mpuFlag = true;
			}
			else if(action.equals(SocketService.REQUEST_CLIENT_MAG))
			{
				log.append("Accept request from matlab: MAG");
				show();
				log.append("The MAG data is:");
				show();
				log.append(mapMag.toString());
				show();
				magFlag = true;
			}
		}
    	
    };
    private static IntentFilter mServiceIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SocketService.REQUEST_CLIENT_RSSI);
        intentFilter.addAction(SocketService.REQUEST_CLIENT_WIFI);
        intentFilter.addAction(SocketService.REQUEST_CLIENT_MPU);
        intentFilter.addAction(SocketService.REQUEST_CLIENT_MAG);
        return intentFilter;
    }

	private void init()
	{
		
		
		mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		
		mSensorManager.registerListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),SensorManager.SENSOR_DELAY_GAME);
		
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
		
		mConnManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		
		mWifiManager=(WifiManager)getSystemService(Context.WIFI_SERVICE);
		
		isWifiOpen();
		
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "This devices doesn`t support BLE", Toast.LENGTH_SHORT).show();
            finish();
        }
		// 初始化 Bluetooth adapter, 通过蓝牙管理器得到一个参考蓝牙适配器(API必须在以上android4.3或以上和版本)
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
		log.append("Initialize done!!!");
		show();
	}
	private void show()
	{
		mLog.setText(log.append("\n\r"));
	}
	private void getOverflowMenu() {
	      try {
	         ViewConfiguration config = ViewConfiguration.get(this);
	         Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
	         if(menuKeyField != null) {
	             menuKeyField.setAccessible(true);
	             menuKeyField.setBoolean(config, false);
	         }
	     } catch (Exception e) {
	         e.printStackTrace();
	     }
	 }
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		if(!mStart) {
            menu.findItem(R.id.stop).setVisible(false);
            menu.findItem(R.id.start).setVisible(true);
            menu.findItem(R.id.title).setVisible(false);
            menu.findItem(R.id.title).setActionView(null);
            
        } else {
            menu.findItem(R.id.stop).setVisible(true);
            menu.findItem(R.id.start).setVisible(false);
            menu.findItem(R.id.title).setActionView(R.layout.action_progress_bar);
                   
        }
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
        case R.id.title:
        	Toast.makeText(getApplicationContext(), "title", Toast.LENGTH_LONG).show();
            break;
        case R.id.stop:
        	mStart = false;
            finish();
            break;
        case R.id.start:
        	mStart = true;
        	log.append("******open service for HttpServer******");
        	show();
        	Intent gattServiceIntent = new Intent(this, SocketService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
            scanLeDevice(true);
            log.append("Ready to accept HttpRequest form Client.....");
            show();
        	this.invalidateOptionsMenu();
        	break;
        case android.R.id.home:
        	this.onBackPressed();
        	break;
		}
		return super.onOptionsItemSelected(item);
		
	}
	
	private Handler mHandler = new Handler();
	private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, 100);
            mStart = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mStart = true;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

	public void isWifiOpen() {  
        if (!mWifiManager.isWifiEnabled()) { 
         Toast.makeText(this, "请先连接上wifi。。。", Toast.LENGTH_LONG).show();
         finish();
         //mWifiManager.setWifiEnabled(true);  
        }  
    }
	
	protected void onPause()
	{
		super.onPause();
		Log.i("Activity","This is pause");
		
	}
	protected void onStop()
	{
		super.onStop();
		Log.i("Activity","This is stop");
		
	}
	protected void onResume()
	{
		super.onResume();
		Log.i("Activity", "this is resume");
		System.out.println(mStart);
		registerReceiver(mServiceReceiver,mServiceIntentFilter());
		if(mSocketService != null)
		{
		ipAddr.setText(getLocalHostIp() + ":" + mSocketService.defaultPort);
		}
		this.invalidateOptionsMenu();

	     // 检查设备上是否支持蓝牙
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        
        //为了确保设备上蓝牙能使用, 如果当前蓝牙设备没启用,弹出对话框向用户要求授予权限来启用
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
        
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		float values[] = event.values;
		int sensorType = event.sensor.getType();
		if(mpuFlag)
		{
			switch(sensorType)
			{
				case Sensor.TYPE_ORIENTATION:
					mapMpu.put("ORZ",values[0]);
					mapMpu.put("ORX",values[1]);
					mapMpu.put("ORY",values[2]);
					break;
				case Sensor.TYPE_ACCELEROMETER:
					mapMpu.put("ACX",values[0]);
					mapMpu.put("ACY",values[1]);
					mapMpu.put("ACZ",values[2]);
					break;
				case Sensor.TYPE_GYROSCOPE:
					mapMpu.put("GRX",values[0]);
					mapMpu.put("GRY",values[1]);
					mapMpu.put("GRZ",values[2]);
					break;
				default:
					break;
			}
		}
		if(magFlag)
		{
			switch(sensorType)
			{
			case Sensor.TYPE_MAGNETIC_FIELD:
				mapMag.put("MAX",values[0]);
				mapMag.put("MAY",values[1]);
				mapMag.put("MAZ",values[2]);
				break;
			default:
				break;
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	protected void onDestroy()
	{
		Log.i("Activity", "This is Destroyed");
		mSensorManager.unregisterListener(this);
		if(mSocketService != null)
		{
		unbindService(mServiceConnection);
		}
		unregisterReceiver(mServiceReceiver);
		super.onDestroy();
	}
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	mapRssi.put(device.getName(), (float)rssi);
					Log.i("Rssi","device name: " + device.getName() + "Rssi is "+ rssi);
                }
            });
        }
    };
    public void getWifiList()
    {
 	   mWifiManager.startScan();  
        //得到扫描结果  
        listWifi = mWifiManager.getScanResults(); 
        
        if(listWifi!=null){
   	 		for(int i=0;i<listWifi.size();i++){
  			//得到扫描结果
   	 			ScanResult mScanResult=listWifi.get(i);
   	 			mapWifi.put(mScanResult.SSID,(float)mScanResult.level);
  			}
   	 	}
      else
      {
     	 System.out.println("no data...................");
      }
    }
    
    public void closeWifi(){  
        if(!mWifiManager.isWifiEnabled()){  
            mWifiManager.setWifiEnabled(false);  
        }  
    }
    //使用wifi联网时获取ip
    public String getIpAddr()
    {
    	if(mWifiManager != null)
    	{
    		WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
    		return (mWifiInfo == null) ? null : intToIp(mWifiInfo.getIpAddress());
    	}
		return null;
    }
    private String intToIp(int i)  
    {
    	return (i & 0xFF)+ "." + ((i >> 8 ) & 0xFF) + "." + ((i >> 16 ) & 0xFF) +"."+((i >> 24 ) & 0xFF);
    }
    //使用移动网络联网时获取本机ip
    public String getLocalHostIp(){
        String ipaddress = "";
        try{
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            // 遍历所用的网络接口
            while (en.hasMoreElements()){
                NetworkInterface nif = en.nextElement();// 得到每一个网络接口绑定的所有ip
                Enumeration<InetAddress> inet = nif.getInetAddresses();
                // 遍历每一个接口绑定的所有ip
                while (inet.hasMoreElements()){
                    InetAddress ip = inet.nextElement();
                    if (!ip.isLoopbackAddress()&& InetAddressUtils.isIPv4Address(ip.getHostAddress())){
                        return ip.getHostAddress();
                    }
                }
            }
        }catch (SocketException e){
            Log.e("Activity", "获取本地ip地址失败");
        }
        return ipaddress;
    }

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		log.delete(0, log.length());
		show();
	}

}
