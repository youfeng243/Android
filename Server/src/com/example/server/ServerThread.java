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

//�����ר�����������豸
public class ServerThread 
{	
	//��ǰ�豸��pid vid
	public int m_pid = 0;
	public int m_vid = 0;
	
	//�豸�����Ϣ
	public int m_ScreenWidth = 0;
	public int m_ScreenHeight = 0;
	
	//USB�豸����
	private UsbManager m_usbmanager  = null;
	private UsbDevice  m_usbDevice   = null;
	private UsbInterface m_Interface = null;
	//�жϴ���ӿ�
	public UsbEndpoint m_epIntOut = null;
	public UsbEndpoint m_epIntIn  = null;
	public UsbDeviceConnection m_connect = null;
	
	//�߳�����
	private ThreadEngine m_ThreadEngine = null;
	
	//������
	Context m_Context;
	
	//������Ϣ
	HidService m_hidServer;
	
	//���캯��
	public ServerThread(int pid, int vid, UsbManager usbmanager, Context AppContext, HidService hidServer)  //���캯���в���̫�ദ��
	{
		m_pid = pid;
		m_vid = vid;
		m_usbmanager = usbmanager;
		m_Context = AppContext;
		
		//������Ϣ
		m_hidServer = hidServer;
		
		//��Ļ��Ϣ
		DisplayMetrics dm2 = hidServer.getResources().getDisplayMetrics();
		m_ScreenWidth = dm2.widthPixels;
		m_ScreenHeight = dm2.heightPixels;
		
		//Ҫ�ȴ��豸���� ��ʼ�� �߳�����
		m_ThreadEngine = new ThreadEngine(this);
    }
	
	//�����Ի���  ���߳�ͬ��
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

	//�����߳�
	public void Start()
	{		
		//���豸
		if( ServerOpen() == false )
		{
			Log.e(Common.TAG, "�豸��ʧ��");
			//MessageBox("�豸��ʧ��");
			return;
		}
		
		//��ʼ���߳�����
		m_ThreadEngine.Init();
		
		//ֱ�ӿ���ͨ���߳�
		if( m_ThreadEngine.BeginServerThread() == false )
		{
			Log.e(Common.TAG, "�̴߳�ʧ��");
			//MessageBox("�̴߳�ʧ��");
			return;
		}
		
		//MessageBox("�߳̿����ɹ�####");
		Log.d(Common.TAG, "�߳̿����ɹ�####");
	}
	
	//�ж��߳��Ƿ���������
	public boolean isAlive()
	{
		return m_ThreadEngine.isAlive();
	}
	
	//У׼�豸
	public void Calibrate(
			double[]   pScreenXBuffer,        //
	        double[]   pScreenYBuffer,        //
	        double[]   pUncalXBuffer,         //
	        double[]   pUncalYBuffer          // 
			)
	{
		m_ThreadEngine.Calibrate(pScreenXBuffer, pScreenYBuffer, pUncalXBuffer, pUncalYBuffer);
	}
	
	//�ر��߳�
	public void Stop()
	{
		if( m_ThreadEngine == null )
		{
			//�ر��豸
			ServerClose();
			Log.e(Common.TAG, "m_ThreadEngine ָ��ΪNULL");
			return;
		}
		
		//�رն������߳�
		m_ThreadEngine.EndServerThread();
		
		//�ر��豸
		ServerClose();
	}
	
	
	//���豸
	private boolean ServerOpen()
	{
		boolean iRet = false;
		
		do
		{
			//�ȹر��豸
			ServerClose();
			
			//�����豸
			m_usbDevice = FindDevice(m_pid, m_vid, m_usbmanager);
			if( m_usbDevice == null )
			{
				//MessageBox("can not find device");
				Log.e(Common.TAG, "can not find device");
				break;
			}
			
			//�����豸�ӿ�
			if( FindDeviceInterface( m_usbDevice ) == false )
			{
				//MessageBox("�����豸�ӿ�ʧ��");
				Log.e(Common.TAG, "�����豸�ӿ�ʧ��");
				break;
			}
			
			//��ö˵�
			if( GetEndPoints( m_Interface ) == false )
			{
				//MessageBox("��ö˵�ʧ�ܣ�");
				Log.e(Common.TAG, "��ö˵�ʧ��!");
				break;
			}
			
			//�ж��Ƿ��в����豸Ȩ��
			if( m_usbmanager.hasPermission(m_usbDevice) == false )
			{
				//MessageBox("û�в����豸Ȩ��");
				Log.e(Common.TAG, "û�в����豸Ȩ��");
				break;
			}
			
			//���豸
			UsbDeviceConnection conn = null; 
			conn = m_usbmanager.openDevice(m_usbDevice);
			if( conn == null )
			{
				//MessageBox("���豸ʧ��");
				Log.e(Common.TAG, "���豸ʧ��");
				break;
			}
			
			//�󶨽ӿ�
			if( conn.claimInterface(m_Interface, true) == false )
			{
				//MessageBox("�󶨽ӿ�ʧ��");
				Log.e(Common.TAG, "�󶨽ӿ�ʧ��");
				conn.close();
				break;
			}
			m_connect = conn;
			iRet = true;
			
			//MessageBox("�ɹ������豸");
			Log.d(Common.TAG, "�ɹ������豸");
		}while( false );
	
		return iRet;
	}
	
	//�ر�Server
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
			
			//MessageBox("�ɹ��ر��豸");
			Log.d(Common.TAG, "�ɹ��ر��豸");
		}
		m_usbDevice = null;
		m_epIntOut = null;
		m_epIntIn = null;
	}
	
	//��ö˵�
	private boolean GetEndPoints( UsbInterface Interface )
	{
		boolean iRet = false;
		
		do
		{
			if( Interface == null )
			{
				//MessageBox("Interface��������");
				Log.e(Common.TAG, "Interface��������");
				break;
			}
			
			for (int i = 0; i < Interface.getEndpointCount(); i++) 
			{  
	            UsbEndpoint ep = Interface.getEndpoint(i);  
	            
	            //ֱ���жϴ���
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
				//MessageBox("û���ҵ�����˿�");
				Log.e(Common.TAG, "û���ҵ�����˿�");
			}
			
		}while( false );
		
		return iRet;
	}
	
	//�����豸�ӿ�
	private boolean FindDeviceInterface( UsbDevice  usbDevice )
	{
		boolean iRet = false;
		
		do
		{
			if( usbDevice == null )
			{
				//MessageBox("��������");
				Log.e(Common.TAG, "FindDeviceInterface��������");
				break;
			}
		
			if( usbDevice.getInterfaceCount() <= 0 )
			{
				//MessageBox("û�нӿ�");
				Log.e(Common.TAG, "FindDeviceInterfaceû�нӿ�");
				break;
			}
			m_Interface = usbDevice.getInterface(0);  
            
            iRet = true;
            
		}while( false );
		
		return iRet;
	}
	
	//�����豸
	public UsbDevice FindDevice( int pid, int vid, UsbManager usbmanager )
	{
		UsbDevice iRet = null;
		
		do
		{
			if( usbmanager == null )
			{
				Log.e(Common.TAG, "FindDevice��������");
				//MessageBox("usbmanager��������");
				break;
			}
			
			HashMap<String, UsbDevice> deviceList = usbmanager.getDeviceList();  
            if ( deviceList.isEmpty() == true )
            {
            	Log.e(Common.TAG, "FindDeviceû���豸");
            	//MessageBox("deviceListû���豸");
            	break;
            }
            
            Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();  
            while (deviceIterator.hasNext()) 
            {  
                UsbDevice device = deviceIterator.next();  
               
                // ����ƥ�䵽���豸  
                if (device.getProductId() == pid && device.getVendorId() == vid ) 
                {  
                	iRet = device; // ��ȡUSBDevice
                	break;
                }  
            }			
		}while( false );
		
		return iRet;
	}
	
	//д�豸
	static public boolean WriteDeviceData(UsbDeviceConnection connect, UsbEndpoint Endout, byte[] buff )
	{
		boolean iRet = false;
		
		do
		{
			if( connect == null || Endout == null || buff == null )
			{
				//Log.d(Common.TAG, "д���豸��������");
				//MessageBox("�鴫���������");
				Log.e(Common.TAG, "WriteBlukDeviceд���豸��������");
				break;
			}
			
			if( connect.bulkTransfer(Endout, buff, buff.length, 300) < 0 )
			{
				//MessageBox("д�����ݴ���");
				Log.e(Common.TAG, "WriteBlukDeviceд�����ݴ���");
				break;
			}
			iRet = true;
		}while( false );
		
		return iRet;
	}
	
	//���豸
	static public boolean ReadDeviceData( UsbDeviceConnection connect, UsbEndpoint EndIn, byte[] buff )
	{
		boolean iRet = false;
		int len = 0;
		
		do
		{
			if( connect == null || EndIn == null || buff == null )
			{
				//Log.d(Common.TAG, "д���豸��������");
				//MessageBox("�鴫���������");
				Log.e(Common.TAG, "ReadBlukDevice��������");
				break;
			}
			
			len = connect.bulkTransfer(EndIn, buff, buff.length, 300);
			if( len < 0 )
			{
				//MessageBox("д�����ݴ���");
				//MessageBox("len = " + len);
				Log.e(Common.TAG, "ReadBlukDevice��ȡ���ݴ���");
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
	
	//�ȴ��豸����
	static public boolean WaitDeviceData( UsbDeviceConnection connect, UsbEndpoint EndIn, byte[] buff )
	{
		boolean iRet = false;
		int len = 0;
		
		do
		{
			if( connect == null || EndIn == null || buff == null )
			{
				//Log.d(Common.TAG, "д���豸��������");
				//MessageBox("�鴫���������");
				Log.e(Common.TAG, "ReadBlukDevice��������");
				break;
			}
			
			//Log.i(Common.TAG, "���е��������ˣ�");
			len = connect.bulkTransfer(EndIn, buff, buff.length, 0);
			if( len < 0 )
			{
				//MessageBox("д�����ݴ���");
				//MessageBox("len = " + len);
				Log.e(Common.TAG, "ReadBlukDevice��ȡ���ݴ���");
				Log.e(Common.TAG, "len = " + len);
				break;
			}
			//Log.i(Common.TAG, "���й��˶������ˣ�");
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
				Log.e(Common.TAG, "WaitDeviceData��������");
				break;
			}
			
			//bBuffer = ByteBuffer.allocate(buff.length);
			
			 UsbRequest request = new UsbRequest();
             // ��ʼ������endpointΪIN�ж϶˵�
             request.initialize(connect, EndIn);
             request.queue(bBuffer, buff.length);
             if (connect.requestWait() == request) 
             {
            	 len = connect.bulkTransfer(EndIn, buff, buff.length, 300); 
            	 if( len < 0 )
            	 {
            		 Log.e(Common.TAG, "�ȴ����ݶ�ȡ���� ���ǲ��˳� len = " + len);
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
