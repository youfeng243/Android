package com.example.server;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.DisplayMetrics;
import android.util.Log;

//这个类专门用来管理设备
public class ServerThread 
{	
	//当前设备的pid vid
	public int m_pid = 0;
	public int m_vid = 0;
	
	//设备宽高信息
	public int m_ScreenWidth = 0;
	public int m_ScreenHeight = 0;
	
	//USB设备管理
	private UsbManager m_usbmanager  = null;
	private UsbDevice  m_usbDevice   = null;
	private UsbInterface m_Interface = null;
	//中断传输接口
	public UsbEndpoint m_epIntOut = null;
	public UsbEndpoint m_epIntIn  = null;
	public UsbDeviceConnection m_connect = null;
	
	//线程引擎
	private ThreadEngine m_ThreadEngine = null;
	
	//上下文
	Context m_Context;
	
	//服务信息
	HidService m_hidServer;
	
	//构造函数
	public ServerThread(int pid, int vid, UsbManager usbmanager, Context AppContext, HidService hidServer)  //构造函数中不做太多处理
	{
		m_pid = pid;
		m_vid = vid;
		m_usbmanager = usbmanager;
		m_Context = AppContext;
		
		//服务信息
		m_hidServer = hidServer;
		
		//屏幕信息
		DisplayMetrics dm2 = hidServer.getResources().getDisplayMetrics();
		m_ScreenWidth = dm2.widthPixels;
		m_ScreenHeight = dm2.heightPixels;
		
		//要先打开设备才能 初始化 线程引擎
		m_ThreadEngine = new ThreadEngine(this);
    }
	
	//弹出对话框  多线程同步
	/*
	public synchronized void MessageBox( CharSequence message )
	{
		Builder builder = new Builder( m_Context );
				builder.setTitle("ServerThread");
				builder.setMessage(message);
				builder.setNegativeButton("OK", null);
				Dialog dialog=builder.create();
				dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
				dialog.show();
	}
	*/

	//开启线程
	public void Start()
	{		
		//打开设备
		if( ServerOpen() == false )
		{
			Log.e(Common.TAG, "设备打开失败");
			//MessageBox("设备打开失败");
			return;
		}
		
		//初始化线程引擎
		m_ThreadEngine.Init();
		
		//直接开启通信线程
		if( m_ThreadEngine.BeginServerThread() == false )
		{
			Log.e(Common.TAG, "线程打开失败");
			//MessageBox("线程打开失败");
			return;
		}
		
		//MessageBox("线程开启成功####");
		Log.d(Common.TAG, "线程开启成功####");
	}
	
	//判断线程是否正在运行
	public boolean isAlive()
	{
		return m_ThreadEngine.isAlive();
	}
	
	//校准设备
	public void Calibrate(
			double[]   pScreenXBuffer,        //
	        double[]   pScreenYBuffer,        //
	        double[]   pUncalXBuffer,         //
	        double[]   pUncalYBuffer          // 
			)
	{
		m_ThreadEngine.Calibrate(pScreenXBuffer, pScreenYBuffer, pUncalXBuffer, pUncalYBuffer);
	}
	
	//关闭线程
	public void Stop()
	{
		if( m_ThreadEngine == null )
		{
			//关闭设备
			ServerClose();
			Log.e(Common.TAG, "m_ThreadEngine 指针为NULL");
			return;
		}
		
		//关闭读数据线程
		m_ThreadEngine.EndServerThread();
		
		//关闭设备
		ServerClose();
	}
	
	
	//打开设备
	private boolean ServerOpen()
	{
		boolean iRet = false;
		
		do
		{
			//先关闭设备
			ServerClose();
			
			//查找设备
			m_usbDevice = FindDevice(m_pid, m_vid, m_usbmanager);
			if( m_usbDevice == null )
			{
				//MessageBox("can not find device");
				Log.e(Common.TAG, "can not find device");
				break;
			}
			
			//查找设备接口
			if( FindDeviceInterface( m_usbDevice ) == false )
			{
				//MessageBox("查找设备接口失败");
				Log.e(Common.TAG, "查找设备接口失败");
				break;
			}
			
			//获得端点
			if( GetEndPoints( m_Interface ) == false )
			{
				//MessageBox("获得端点失败！");
				Log.e(Common.TAG, "获得端点失败!");
				break;
			}
			
			//判断是否有操作设备权限
			if( m_usbmanager.hasPermission(m_usbDevice) == false )
			{
				//MessageBox("没有操作设备权限");
				Log.e(Common.TAG, "没有操作设备权限");
				break;
			}
			
			//打开设备
			UsbDeviceConnection conn = null; 
			conn = m_usbmanager.openDevice(m_usbDevice);
			if( conn == null )
			{
				//MessageBox("打开设备失败");
				Log.e(Common.TAG, "打开设备失败");
				break;
			}
			
			//绑定接口
			if( conn.claimInterface(m_Interface, true) == false )
			{
				//MessageBox("绑定接口失败");
				Log.e(Common.TAG, "绑定接口失败");
				conn.close();
				break;
			}
			m_connect = conn;
			iRet = true;
			
			//MessageBox("成功连接设备");
			Log.d(Common.TAG, "成功连接设备");
		}while( false );
	
		return iRet;
	}
	
	//关闭Server
	private void ServerClose()
	{	
		if( m_connect != null )
		{
			if( m_Interface != null )
			{
				m_connect.releaseInterface(m_Interface);
				m_Interface = null;
			}
			m_connect.close();
			m_connect = null;
			
			//MessageBox("成功关闭设备");
			Log.d(Common.TAG, "成功关闭设备");
		}
		m_usbDevice = null;
		m_epIntOut = null;
		m_epIntIn = null;
	}
	
	//获得端点
	private boolean GetEndPoints( UsbInterface Interface )
	{
		boolean iRet = false;
		
		do
		{
			if( Interface == null )
			{
				//MessageBox("Interface参数错误");
				Log.e(Common.TAG, "Interface参数错误");
				break;
			}
			
			for (int i = 0; i < Interface.getEndpointCount(); i++) 
			{  
	            UsbEndpoint ep = Interface.getEndpoint(i);  
	            
	            //直接中断传输
	            if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) 
	            {  
	                if (ep.getDirection() == UsbConstants.USB_DIR_OUT) 
	                {  
	                	m_epIntOut = ep;
	                }  
	                if (ep.getDirection() == UsbConstants.USB_DIR_IN) 
	                {  
	                	m_epIntIn = ep;  
	                }  
	            }     
	        }  
	        
			if( m_epIntOut != null && m_epIntIn != null )
			{
				iRet = true;
			}
			
			if( iRet == false )
			{
				//MessageBox("没有找到传输端口");
				Log.e(Common.TAG, "没有找到传输端口");
			}
			
		}while( false );
		
		return iRet;
	}
	
	//查找设备接口
	private boolean FindDeviceInterface( UsbDevice  usbDevice )
	{
		boolean iRet = false;
		
		do
		{
			if( usbDevice == null )
			{
				//MessageBox("参数错误");
				Log.e(Common.TAG, "FindDeviceInterface参数错误");
				break;
			}
		
			if( usbDevice.getInterfaceCount() <= 0 )
			{
				//MessageBox("没有接口");
				Log.e(Common.TAG, "FindDeviceInterface没有接口");
				break;
			}
			m_Interface = usbDevice.getInterface(0);  
            
            iRet = true;
            
		}while( false );
		
		return iRet;
	}
	
	//查找设备
	public UsbDevice FindDevice( int pid, int vid, UsbManager usbmanager )
	{
		UsbDevice iRet = null;
		
		do
		{
			if( usbmanager == null )
			{
				Log.e(Common.TAG, "FindDevice参数错误");
				//MessageBox("usbmanager参数错误");
				break;
			}
			
			HashMap<String, UsbDevice> deviceList = usbmanager.getDeviceList();  
            if ( deviceList.isEmpty() == true )
            {
            	Log.e(Common.TAG, "FindDevice没有设备");
            	//MessageBox("deviceList没有设备");
            	break;
            }
            
            Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();  
            while (deviceIterator.hasNext()) 
            {  
                UsbDevice device = deviceIterator.next();  
               
                // 保存匹配到的设备  
                if (device.getProductId() == pid && device.getVendorId() == vid ) 
                {  
                	iRet = device; // 获取USBDevice
                	break;
                }  
            }			
		}while( false );
		
		return iRet;
	}
	
	//写设备
	static public boolean WriteDeviceData(UsbDeviceConnection connect, UsbEndpoint Endout, byte[] buff )
	{
		boolean iRet = false;
		
		do
		{
			if( connect == null || Endout == null || buff == null )
			{
				//Log.d(Common.TAG, "写入设备参数错误");
				//MessageBox("块传输参数错误");
				Log.e(Common.TAG, "WriteBlukDevice写入设备参数错误");
				break;
			}
			
			if( connect.bulkTransfer(Endout, buff, buff.length, 300) < 0 )
			{
				//MessageBox("写入数据错误");
				Log.e(Common.TAG, "WriteBlukDevice写入数据错误");
				break;
			}
			iRet = true;
		}while( false );
		
		return iRet;
	}
	
	//读设备
	static public boolean ReadDeviceData( UsbDeviceConnection connect, UsbEndpoint EndIn, byte[] buff )
	{
		boolean iRet = false;
		int len = 0;
		
		do
		{
			if( connect == null || EndIn == null || buff == null )
			{
				//Log.d(Common.TAG, "写入设备参数错误");
				//MessageBox("块传输参数错误");
				Log.e(Common.TAG, "ReadBlukDevice参数错误");
				break;
			}
			
			len = connect.bulkTransfer(EndIn, buff, buff.length, 300);
			if( len < 0 )
			{
				//MessageBox("写入数据错误");
				//MessageBox("len = " + len);
				Log.e(Common.TAG, "ReadBlukDevice读取数据错误");
				Log.e(Common.TAG, "len = " + len);
				break;
			}
			
			if( len == 0 )
			{
				Log.d(Common.TAG, "ReadDeviceData len = 0" + len);
			}
			
			iRet = true;
		}while( false );
		
		return iRet;
	}
	
	//等待设备数据
	static public boolean WaitDeviceData( UsbDeviceConnection connect, UsbEndpoint EndIn, byte[] buff )
	{
		boolean iRet = false;
		int len = 0;
		
		do
		{
			if( connect == null || EndIn == null || buff == null )
			{
				//Log.d(Common.TAG, "写入设备参数错误");
				//MessageBox("块传输参数错误");
				Log.e(Common.TAG, "ReadBlukDevice参数错误");
				break;
			}
			
			//Log.i(Common.TAG, "运行到读数据了！");
			len = connect.bulkTransfer(EndIn, buff, buff.length, 0);
			if( len < 0 )
			{
				//MessageBox("写入数据错误");
				//MessageBox("len = " + len);
				Log.e(Common.TAG, "ReadBlukDevice读取数据错误");
				Log.e(Common.TAG, "len = " + len);
				break;
			}
			//Log.i(Common.TAG, "运行过了读数据了！");
			if( len == 0 )
			{
				Log.d(Common.TAG, "ReadDeviceData len = 0" + len);
			}
			
			iRet = true;
		}while( false );
		
		return iRet;
	}
	
	/*
	static public int WaitDeviceData( UsbDeviceConnection connect, UsbEndpoint EndIn, byte[] buff, ByteBuffer bBuffer )
	{
		int len = -1;
		//ByteBuffer bBuffer = null;
		
		do
		{
			if( connect == null || EndIn == null || buff == null || bBuffer == null )
			{
				Log.e(Common.TAG, "WaitDeviceData参数错误");
				break;
			}
			
			//bBuffer = ByteBuffer.allocate(buff.length);
			
			 UsbRequest request = new UsbRequest();
             // 初始化请求，endpoint为IN中断端点
             request.initialize(connect, EndIn);
             request.queue(bBuffer, buff.length);
             if (connect.requestWait() == request) 
             {
            	 len = connect.bulkTransfer(EndIn, buff, buff.length, 300); 
            	 if( len < 0 )
            	 {
            		 Log.e(Common.TAG, "等待数据读取错误 但是不退出 len = " + len);
            		 //break;
            		 len = 0;
            	 }
            	 else
            	 {
            		 len = 1;
            	 }
             }
             
		}while( false );
		
		return len;
	}
	*/
	
}
