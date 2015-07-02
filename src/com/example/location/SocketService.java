package com.example.location;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.location.NanoHttpD.HTTPSession;
import com.example.location.NanoHttpD.TempFileManager;
import com.example.location.NanoHttpD.Response.Status;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class SocketService extends Service {

	
	
	public static int[] portAry = new int[]{23456,23021,48990};
	
	/*默认端口*/
	public static int defaultPort = 23456;
	
	public static String BROADCAST_FROM_MAIN = "com.example.location.BROADCAST_FROM_MAIN";
	
	public static String REQUEST_CLIENT_RSSI = "com.example.main.rssi";
	
	public static String REQUEST_CLIENT_WIFI = "com.example.main.wifi";
	
	public static String REQUEST_CLIENT_MPU = "com.example.main.mpu";
	
	public static String REQUEST_CLIENT_MAG = "com.example.main.mag";
	
	private static String wifi;
	
	private static String rssi;
	
	private static String mpu;
	
	private static String mag;

	private static Map<String,Float> mapRssi;//ble
	
	private  static Map<String,Float> mapMpu;//mpu
	
	private static Map<String,Float> mapWifi;//wifi
	
	private static Map<String,Float> mapMag;//mag
	
	private static OnRequestListener onRequestListener;//监听器
	
	private static MyNanoHttpD nanoHttpD;
	
	public void setOnRequestListener(OnRequestListener orl)
	{
		this.onRequestListener = orl;
	}
		//网络开关状态改变的监听
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)){
				Log.e("service","网络状态切换...");
			}
		}
	};
	
	public void setResponseData(String param,Map<String,Float> str)
	{
		if(param.equals("WIFI"))
		{
			mapWifi = str;
			Log.i("WIFI ", mapWifi.toString());
		}
		else if(param.equals("RSSI"))
		{
			mapRssi = str;
			Log.i("RSSI ", mapRssi.toString());
			
		}
		else if(param.equals("MPU"))
		{
			mapMpu = str;
			Log.i("MPU", mapMpu.toString());
		}
		else if(param.equals("MAG"))
		{
			mapMag = str;
			Log.i("MGA", mapMag.toString());
		}
	}
	
	private IBinder mBinder = new LocalBinder();
	
	@Override
	public IBinder onBind(Intent intent) {
        return mBinder;
    }
	
    public class LocalBinder extends Binder {
        SocketService getService() {
            return SocketService.this;
        }
    }
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
    	Log.i("Service ", "service Unbind");
    	mBinder = null;
        return super.onUnbind(intent);
    }
	
    public void onDestroy()
    {
    	super.onDestroy();
    	Log.i("Service ", "Servie Destroy");
    	unregisterReceiver(receiver);
    	nanoHttpD.stop();
    }
	public void start() 
	{
		System.out.println(Thread.currentThread());
		//注册广播
		IntentFilter filter = new IntentFilter();
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(receiver, filter);
		//开启监听端口
		Log.i("Service ","监听开启...");
		for(int i=0;i<portAry.length;i++){
			if(checkPort(portAry[i])){
				defaultPort = portAry[i];
				break;
			}
		}
			// TODO Auto-generated method stub
			nanoHttpD = new MyNanoHttpD(defaultPort);  
        try{  
            nanoHttpD.start();  
            System.out.println("DEMO " + nanoHttpD.isAlive()+"");
        }catch(Exception e){  
            e.printStackTrace();  
        }
	}
	public boolean checkPort(int port){
		try{
			InetAddress theAddress=InetAddress.getByName("127.0.0.1");
			try {
				Socket socket = new Socket(theAddress,port);
				socket.close();
				socket = null;
				theAddress = null;
				return false;
			}catch (IOException e) {
				Log.e("异常:",port + " "+e.getMessage()+"检查端口号是否被占用");
			}catch(Exception e){
				Log.e("异常:",port + " "+e.getMessage()+"检查端口号是否被占用");
			}
		}catch(UnknownHostException e) {
			Log.e("异常:",port + " "+e.getMessage()+"检查端口号是否被占用");
		}
		return true;
    }
	class MyNanoHttpD extends NanoHttpD{

		/**
		 * @param args
		 */
	    public MyNanoHttpD(int port) {  
		        super(port);  
		    }  
		      
		    public MyNanoHttpD(String hostName,int port){  
		        super(hostName,port);  
		    }  		     		    
		     @SuppressLint("DefaultLocale")
			public Response serve(IHTTPSession session) {   
		         Method method = session.getMethod();  
		         System.out.println("DEMO" +" Method:"+method.toString());  
		         		                     	 
		         if(NanoHttpD.Method.GET.equals(method)){  
		             //get方式  
		             String queryParams = session.getQueryParameterString();  
		             System.out.println("DEMO"+ " params:"+queryParams);  

		             String param = queryParams.substring(queryParams.indexOf("=")+1);
		             System.out.println(param);
		             
		             
		             if("WIFI".equals(param.toUpperCase()))
		             {
		            	 onRequestListener.onRequest("WIFI");
		            	 
		            	 Intent intent = new Intent(SocketService.REQUEST_CLIENT_WIFI);		    
		            	 sendBroadcast(intent);
		            	 
		            	JSONObject json = new JSONObject(mapWifi);
		            	Response s = new Response(Status.OK,"text/html",json.toString());
			             return s;
		             }		           
		             else if("RSSI".equals(param.toUpperCase()))
		             { 		 
		            	 onRequestListener.onRequest("RSSI");
		            	 
			            Intent intent = new Intent(REQUEST_CLIENT_RSSI);
			            sendBroadcast(intent);
		            	 JSONObject json = new JSONObject(mapRssi);
		            	 Response s = new Response(Status.OK,"text/html",json.toString());
					     return s;
		             }
		             else if("MPU".equals(param.toUpperCase()))
		             {		  
		            	 onRequestListener.onRequest("MPU");
		            	 
			             Intent intent = new Intent(REQUEST_CLIENT_MPU);
			             sendBroadcast(intent);
		            	 JSONObject json = new JSONObject(mapMpu);
	            	 	 Response s = new Response(Status.OK,"text/html",json.toString());
				         return s;
	            	 		
	            	 }
		             else if("MAG".equals(param.toUpperCase()))
		             {
		            	 onRequestListener.onRequest("MAG");
		            	 
			             Intent intent = new Intent(REQUEST_CLIENT_MAG);
			             sendBroadcast(intent);
			             JSONObject json = new JSONObject(mapMag);
	            	 	 Response s = new Response(Status.OK,"text/html",json.toString());
				         return s;
		             }

		         }
		         else if(NanoHttpD.Method.POST.equals(method)){  
		             //post方式  
		         }  
		         return super.serve(session);  
		     }
		 }
	
	}