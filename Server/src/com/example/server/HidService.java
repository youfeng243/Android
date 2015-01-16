package com.example.server;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;

//这个类主要用于后台读取数据
public class HidService extends Service 
{
	//判断是否重启
	private boolean m_restart = false;  
	
	//USB操作权限
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	
	//USB 权限控制
	private PendingIntent m_pendingIntent;
	
	//USB 申请权限标志
	private boolean m_reqPermission = false;
	
	//时钟定义
	private static final int TIMER_MONITOR = 1;
	private static final int TIMER_STARTTHREAD = 2;
	
	//检测时钟
	private Timer m_monitorTimer = null;
	private TimerTask m_monitorTask = null;
	
	//开启线程时钟
	private Timer m_startThreadTimer = null;
	private TimerTask m_startThreadTask = null;
	
	//USB管理
	private UsbManager m_usbManager = null;
	
	//Server核心
	private ServerThread m_ServerThread = null;
	
	//弹出对话框 
	public synchronized void MessageBox( CharSequence message )
	{
		Builder builder = new Builder(getApplicationContext());
				builder.setTitle("HidService");
				builder.setMessage(message);
				builder.setNegativeButton("OK", null);
				Dialog dialog=builder.create();
				dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
				dialog.show();
	}
	
	
	//任务
	@SuppressLint("HandlerLeak")
	final Handler handler = new Handler( ) 
	{
		public void handleMessage(Message msg) 
		{
			switch (msg.what) 
			{
			case TIMER_MONITOR:
				{	
					//判断读数据线程是否正在正常运行
					MonitorThread();
					
				}
				break;
				
			case TIMER_STARTTHREAD:
				{
					//开启线程
					StartThread();
				}
				break;
			
			default:
				break;
			}
		}
	};
	
	//开启线程
	public void StartThread()
	{
		if( m_reqPermission == true )
		{
			//当前正在申请权限，不再往下执行
			//MessageBox("正在申请权限！！！");
			return;
		}
		
		//如果线程已经开启了 也不用往下执行了
		if( m_ServerThread.isAlive() == true )
		{
			//MessageBox("线程已经开启了!!!!");
			return;
		}
		
		//获得当前设备
		UsbDevice Device = m_ServerThread.FindDevice(Common.MT_PID, Common.MT_VID, m_usbManager);
		if( Device == null )
		{
			//MessageBox("获得设备为NULL！！！");
			return;
		}
		
		//判断权限
		if( m_usbManager.hasPermission(Device) )
		{
			//MessageBox("有权限,直接开启线程!");
			
			//有权限，直接开启线程
			m_ServerThread.Start();
				
		}
		else
		{	
			//设置标志位 当前正在申请权限
			m_reqPermission = true;
			
			//申请权限
			m_usbManager.requestPermission(Device, m_pendingIntent);
		}
	}
	
	//开启线程时钟
	private void StartThreadTimer()
	{
		//先关闭时钟
		StopThreadTimer();
		
		m_startThreadTask = new TimerTask( ) 
    	{
    		public void run ( ) 
    		{
    			Message message = new Message( );
    			message.what = TIMER_STARTTHREAD;
    			handler.sendMessage(message);
    		}
    	};
    	m_startThreadTimer = new Timer();
    	m_startThreadTimer.schedule(m_startThreadTask, 200, 400);
    	
    	//MessageBox("启动线程时钟开启成功");
	}
	
	//关闭线程时钟
	private void StopThreadTimer()
	{
		if( m_startThreadTimer != null )
		{
			m_startThreadTimer.cancel();
			m_startThreadTimer = null;
			//MessageBox("startThreadTimer被关闭");
		}
		
		if( m_startThreadTask != null )
		{
			m_startThreadTask.cancel();
			m_startThreadTask = null;
			//MessageBox("startThreadTask被关闭");
		}
	}
	
	//检测线程
	private void MonitorThread()
	{
		//先判断线程是否已经运行
		if( m_ServerThread.isAlive() == true )
		{
			//正在运行 不用进行任何处理
			StopThreadTimer();
			
			//MessageBox("MonitorThread 关闭了StartTimer时钟");
			return;
		}
		
		//判断设备是否已经连接
		if( m_ServerThread.FindDevice(Common.MT_PID, Common.MT_VID, m_usbManager) == null )
		{
			//设备未连接 不用处理
			StopThreadTimer();
			//MessageBox("MonitorThread 设备未连接关闭了StartTimer时钟");
			return;
		}
		
		//判断是否已经开启时钟跟踪线程
		if( m_startThreadTimer == null )
		{
			//开启时钟
			StartThreadTimer();
			
			//MessageBox("MonitorThread 开启了时钟");
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	//获得root权限
	public static boolean RootCommand(String command)
    {
        Process process = null;
        DataOutputStream os = null;
        try
        {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();

            int exitValue = process.waitFor();  
            if (exitValue != 0)
            {  
                return false;  
            }  
        } catch (Exception e)
        {
            Log.e(Common.TAG, "ROOT ERROR" + e.getMessage());
            return false;
        } 
        finally
        {
            try
            {
                if (os != null)
                {
                    os.close();
                }
                process.destroy();
            }
            catch (Exception e)
            {
            	e.printStackTrace();  
            }
        }
       
       	Log.i(Common.TAG, "Root SUCCESS ");
        return true;
    }

	//开启服务 创建设备相关 这个是创建时调用
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		
		//是否在申请权限
		m_reqPermission = false;
		
		//获得文件拷贝路径
		String fileCmd = " \n";
		try {
			fileCmd = GetfileCmd();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//获得root权限
		String apkRoot = "chmod 777  " + " /dev/uinput \n";
		if( RootCommand(fileCmd + apkRoot) == false )
		{
			MessageBox("没有root授权，无法开启触摸屏功能，请重新启动程序授权！");
			
    		//发送意图 退出程序
    		sendBroadcast(new Intent(Common.ACTION_RECIVEHIDSERVER));
			
			//退出服务
			stopSelf();
			 
			Log.e(Common.TAG, "没有获得root权限");
			return;
		}
		
		//获得USB管理类
        m_usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        if( m_usbManager == null )
        {
        	//MessageBox("获取USB管理类失败 退出重启");
        	Log.e(Common.TAG, "获取USB管理类失败 退出重启");
        	stopSelf();
        	return;
        }
		
        m_ServerThread = new ServerThread(Common.MT_PID, Common.MT_VID, m_usbManager, getApplicationContext(), this);
        if( m_ServerThread == null )
        {
        	//MessageBox("申请内存失败 退出重启");
        	Log.e(Common.TAG, "申请内存失败 退出重启");
			stopSelf();
        	return;
        }
        
        //线程自动重启
        m_restart = true;
        
		//检测设备插拔
        m_pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(Common.ACTION_CALIBRATION);
        registerReceiver(mUsbReceiver, filter);
        
        //创建一个时钟时刻去检测设备是否保持连接
        m_monitorTask = new TimerTask( ) 
    	{
    		public void run ( ) 
    		{
    			Message message = new Message( );
    			message.what = TIMER_MONITOR;
    			handler.sendMessage(message);
    		}
    	};
        m_monitorTimer = new Timer();
        m_monitorTimer.schedule(m_monitorTask, 200, 1000);
        
        //MessageBox("Hid Service created success");
        Log.d(Common.TAG, "Hid Service created success");
	}
	
	//拷贝文件到应用程序文件夹
	private String CopyFile( String Filename ) throws IOException
	{
		// 获取应用包名  
        String sPackage = this.getPackageName(); 
        String tagetfile = "/data/data/" + sPackage + "/" + Filename;
        File mSaveFile = new File(tagetfile);
        if( mSaveFile.exists() == true )
        {
        	Log.i(Common.TAG, "配置文件已经存在!");
        	return tagetfile;
        }
        
        InputStream myInput;  
        OutputStream myOutput = new FileOutputStream(tagetfile);  
        myInput = this.getAssets().open(Filename); 
        
        byte[] buffer = new byte[1024];  
        int length = myInput.read(buffer);
        while( length > 0 )
        {
            myOutput.write(buffer, 0, length); 
            length = myInput.read(buffer);
        }
        
        myOutput.flush();  
        myInput.close();  
        myOutput.close(); 
        
		return tagetfile;
	}
	
	//获得文件拷贝命令行
	private String GetfileCmd() throws IOException
	{
		String cmd = " \n";
		String targetpath = "/system/usr/idc/";
		String targetfile = targetpath + Mouse.DEVICE_NAME + ".idc";
		do
		{
			File file = new File(targetfile);	
			if( file.exists() == true )
			{
				Log.i(Common.TAG, "配置文件已经存在");
				break;
			}
			
			//拷贝文件到应用程序文件夹
			String targetFile = CopyFile(Mouse.DEVICE_NAME + ".idc");
			Log.i(Common.TAG, "文件的路径: " + targetFile);
			
			cmd = "mount - o remount rw /system \n" + "cp " + targetFile + " " + targetpath + " \n";
			
			Log.i(Common.TAG, "cmd:" + cmd);
		}while(false);
		
		
		return cmd;
	}
	
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() 
    {
        public void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();
            
            if( Common.ACTION_CALIBRATION.equals(action) )
            {
            	double[]   pScreenXBuffer = new double[4];        //
                double[]   pScreenYBuffer = new double[4];        //
                double[]   pUncalXBuffer  = new double[4];         //
                double[]   pUncalYBuffer  = new double[4];        //
            	
                Log.i(Common.TAG, "接收到校准数据,准备校准!!!");
                
                //校准
            	Bundle mBundle = intent.getExtras();
            	if( mBundle != null )
            	{
            		pScreenXBuffer[0] = mBundle.getDouble("ScreenX0");
            		pScreenXBuffer[1] = mBundle.getDouble("ScreenX1");
            		pScreenXBuffer[2] = mBundle.getDouble("ScreenX2");
            		pScreenXBuffer[3] = mBundle.getDouble("ScreenX3");
            		
            		pScreenYBuffer[0] = mBundle.getDouble("ScreenY0");
            		pScreenYBuffer[1] = mBundle.getDouble("ScreenY1");
            		pScreenYBuffer[2] = mBundle.getDouble("ScreenY2");
            		pScreenYBuffer[3] = mBundle.getDouble("ScreenY3");
            		
            		pUncalXBuffer[0] = mBundle.getDouble("UncalX0");
            		pUncalXBuffer[1] = mBundle.getDouble("UncalX1");
            		pUncalXBuffer[2] = mBundle.getDouble("UncalX2");
            		pUncalXBuffer[3] = mBundle.getDouble("UncalX3");
            		
            		pUncalYBuffer[0] = mBundle.getDouble("UncalY0");
            		pUncalYBuffer[1] = mBundle.getDouble("UncalY1");
            		pUncalYBuffer[2] = mBundle.getDouble("UncalY2");
            		pUncalYBuffer[3] = mBundle.getDouble("UncalY3");
            		
            		//校准
            		m_ServerThread.Calibrate(pScreenXBuffer, pScreenYBuffer, pUncalXBuffer, pUncalYBuffer);
            	}
            	
            }
            else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) 
            {   
            	UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            	if( device.getProductId() == Common.MT_PID && device.getVendorId() == Common.MT_VID )
            	{
            		//MessageBox("监视的设备插入了!");
            		
            		Log.d(Common.TAG, "监视的设备插入了!");
            		
            		//开启时钟延迟启动线程
            		StartThreadTimer();
            	}
            } 
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) 
            {
            	UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            	if( device.getProductId() == Common.MT_PID && device.getVendorId() == Common.MT_VID )
            	{
            		//MessageBox("监视的设备拔出了!");
            		
            		Log.d(Common.TAG, "监视的设备拔出了!");
            		
            		//停止数据线程
            		if( m_ServerThread != null )
            		{
            			m_ServerThread.Stop();
            			//MessageBox("停止数据线程!!!");
            			Log.d(Common.TAG, "停止数据线程!!!!");
            		}
            		
            		//MessageBox("监视的设备拔出了!");
            	}
            }
            else if( ACTION_USB_PERMISSION.equals(action) )  //权限申请
            {
            	synchronized (this) 
	            {
            		boolean isSuccess = false;
            		do
            		{
            			UsbDevice usbDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            			if( usbDevice == null )
            			{
            				break;
            			}
            			
            			if( usbDevice.getProductId() != Common.MT_PID || usbDevice.getVendorId() != Common.MT_VID )
            			{
            				break;
            			}
            			
            			//没有授权
            			if( intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false ) == false )
            			{
            				//不授权就直接退出，让用户自己再次手动启动
            				
            				MessageBox("没有USB访问授权，无法开启触摸屏功能，请重新启动程序授权！");
            				
            				//Log.d(Common.TAG, "停止数据线程!!!!");
            				
            				//不再重启
            				m_restart = false;
            				
    	            		//发送意图 退出程序
    	            		sendBroadcast(new Intent(Common.ACTION_RECIVEHIDSERVER));
            				
            				//退出服务
            				stopSelf();
            				return;
            			}
            			
            			isSuccess = true;
            		}while( false );
            		
            		if( isSuccess == true )
            		{
            			//权限申请结束
                		m_reqPermission = false; 
            			m_ServerThread.Start();
            			StopThreadTimer();
            		}
	            }
            }
        }
    };
	
	//开启服务时调用
	@Override
	
	public void onStart(Intent intent, int startId) 
	{
		// TODO Auto-generated method stub
		
		
		//MessageBox("Hid Service Started success");
	}
	
	//关闭服务 销毁设备相关
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		try {
			//这里销毁相关信息
			unregisterReceiver(mUsbReceiver);
		} catch (Exception e) {
			// TODO: handle exception
			Log.e(Common.TAG, "还没有注册!");
		}
		
		
		//关闭检测时钟
		if ( m_monitorTimer != null ) 
		{
			m_monitorTimer.cancel();
			m_monitorTimer = null;
		}
		if( m_monitorTask != null )
		{
			m_monitorTask.cancel();
			m_monitorTask = null;
		}
		
		//关闭开启线程时钟
		StopThreadTimer();
		
		//终止数据线程
		if( m_ServerThread != null )
		{
			m_ServerThread.Stop();
			m_ServerThread = null;
		}
		
		//重新启动服务，服务不能被异常终止
		if( m_restart )
		{
			Intent sevice = new Intent(this, HidService.class);  
		    this.startService(sevice);
		    //MessageBox("Hid Service Restart success!");
		    Log.d(Common.TAG, "Hid Service Restart success!");
		    //Log.d();
		}
	}
}
