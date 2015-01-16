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

//�������Ҫ���ں�̨��ȡ����
public class HidService extends Service 
{
	//�ж��Ƿ�����
	private boolean m_restart = false;  
	
	//USB����Ȩ��
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	
	//USB Ȩ�޿���
	private PendingIntent m_pendingIntent;
	
	//USB ����Ȩ�ޱ�־
	private boolean m_reqPermission = false;
	
	//ʱ�Ӷ���
	private static final int TIMER_MONITOR = 1;
	private static final int TIMER_STARTTHREAD = 2;
	
	//���ʱ��
	private Timer m_monitorTimer = null;
	private TimerTask m_monitorTask = null;
	
	//�����߳�ʱ��
	private Timer m_startThreadTimer = null;
	private TimerTask m_startThreadTask = null;
	
	//USB����
	private UsbManager m_usbManager = null;
	
	//Server����
	private ServerThread m_ServerThread = null;
	
	//�����Ի��� 
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
	
	
	//����
	@SuppressLint("HandlerLeak")
	final Handler handler = new Handler( ) 
	{
		public void handleMessage(Message msg) 
		{
			switch (msg.what) 
			{
			case TIMER_MONITOR:
				{	
					//�ж϶������߳��Ƿ�������������
					MonitorThread();
					
				}
				break;
				
			case TIMER_STARTTHREAD:
				{
					//�����߳�
					StartThread();
				}
				break;
			
			default:
				break;
			}
		}
	};
	
	//�����߳�
	public void StartThread()
	{
		if( m_reqPermission == true )
		{
			//��ǰ��������Ȩ�ޣ���������ִ��
			//MessageBox("��������Ȩ�ޣ�����");
			return;
		}
		
		//����߳��Ѿ������� Ҳ��������ִ����
		if( m_ServerThread.isAlive() == true )
		{
			//MessageBox("�߳��Ѿ�������!!!!");
			return;
		}
		
		//��õ�ǰ�豸
		UsbDevice Device = m_ServerThread.FindDevice(Common.MT_PID, Common.MT_VID, m_usbManager);
		if( Device == null )
		{
			//MessageBox("����豸ΪNULL������");
			return;
		}
		
		//�ж�Ȩ��
		if( m_usbManager.hasPermission(Device) )
		{
			//MessageBox("��Ȩ��,ֱ�ӿ����߳�!");
			
			//��Ȩ�ޣ�ֱ�ӿ����߳�
			m_ServerThread.Start();
				
		}
		else
		{	
			//���ñ�־λ ��ǰ��������Ȩ��
			m_reqPermission = true;
			
			//����Ȩ��
			m_usbManager.requestPermission(Device, m_pendingIntent);
		}
	}
	
	//�����߳�ʱ��
	private void StartThreadTimer()
	{
		//�ȹر�ʱ��
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
    	
    	//MessageBox("�����߳�ʱ�ӿ����ɹ�");
	}
	
	//�ر��߳�ʱ��
	private void StopThreadTimer()
	{
		if( m_startThreadTimer != null )
		{
			m_startThreadTimer.cancel();
			m_startThreadTimer = null;
			//MessageBox("startThreadTimer���ر�");
		}
		
		if( m_startThreadTask != null )
		{
			m_startThreadTask.cancel();
			m_startThreadTask = null;
			//MessageBox("startThreadTask���ر�");
		}
	}
	
	//����߳�
	private void MonitorThread()
	{
		//���ж��߳��Ƿ��Ѿ�����
		if( m_ServerThread.isAlive() == true )
		{
			//�������� ���ý����κδ���
			StopThreadTimer();
			
			//MessageBox("MonitorThread �ر���StartTimerʱ��");
			return;
		}
		
		//�ж��豸�Ƿ��Ѿ�����
		if( m_ServerThread.FindDevice(Common.MT_PID, Common.MT_VID, m_usbManager) == null )
		{
			//�豸δ���� ���ô���
			StopThreadTimer();
			//MessageBox("MonitorThread �豸δ���ӹر���StartTimerʱ��");
			return;
		}
		
		//�ж��Ƿ��Ѿ�����ʱ�Ӹ����߳�
		if( m_startThreadTimer == null )
		{
			//����ʱ��
			StartThreadTimer();
			
			//MessageBox("MonitorThread ������ʱ��");
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	//���rootȨ��
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

	//�������� �����豸��� ����Ǵ���ʱ����
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		
		//�Ƿ�������Ȩ��
		m_reqPermission = false;
		
		//����ļ�����·��
		String fileCmd = " \n";
		try {
			fileCmd = GetfileCmd();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//���rootȨ��
		String apkRoot = "chmod 777  " + " /dev/uinput \n";
		if( RootCommand(fileCmd + apkRoot) == false )
		{
			MessageBox("û��root��Ȩ���޷��������������ܣ�����������������Ȩ��");
			
    		//������ͼ �˳�����
    		sendBroadcast(new Intent(Common.ACTION_RECIVEHIDSERVER));
			
			//�˳�����
			stopSelf();
			 
			Log.e(Common.TAG, "û�л��rootȨ��");
			return;
		}
		
		//���USB������
        m_usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        if( m_usbManager == null )
        {
        	//MessageBox("��ȡUSB������ʧ�� �˳�����");
        	Log.e(Common.TAG, "��ȡUSB������ʧ�� �˳�����");
        	stopSelf();
        	return;
        }
		
        m_ServerThread = new ServerThread(Common.MT_PID, Common.MT_VID, m_usbManager, getApplicationContext(), this);
        if( m_ServerThread == null )
        {
        	//MessageBox("�����ڴ�ʧ�� �˳�����");
        	Log.e(Common.TAG, "�����ڴ�ʧ�� �˳�����");
			stopSelf();
        	return;
        }
        
        //�߳��Զ�����
        m_restart = true;
        
		//����豸���
        m_pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(Common.ACTION_CALIBRATION);
        registerReceiver(mUsbReceiver, filter);
        
        //����һ��ʱ��ʱ��ȥ����豸�Ƿ񱣳�����
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
	
	//�����ļ���Ӧ�ó����ļ���
	private String CopyFile( String Filename ) throws IOException
	{
		// ��ȡӦ�ð���  
        String sPackage = this.getPackageName(); 
        String tagetfile = "/data/data/" + sPackage + "/" + Filename;
        File mSaveFile = new File(tagetfile);
        if( mSaveFile.exists() == true )
        {
        	Log.i(Common.TAG, "�����ļ��Ѿ�����!");
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
	
	//����ļ�����������
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
				Log.i(Common.TAG, "�����ļ��Ѿ�����");
				break;
			}
			
			//�����ļ���Ӧ�ó����ļ���
			String targetFile = CopyFile(Mouse.DEVICE_NAME + ".idc");
			Log.i(Common.TAG, "�ļ���·��: " + targetFile);
			
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
            	
                Log.i(Common.TAG, "���յ�У׼����,׼��У׼!!!");
                
                //У׼
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
            		
            		//У׼
            		m_ServerThread.Calibrate(pScreenXBuffer, pScreenYBuffer, pUncalXBuffer, pUncalYBuffer);
            	}
            	
            }
            else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) 
            {   
            	UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            	if( device.getProductId() == Common.MT_PID && device.getVendorId() == Common.MT_VID )
            	{
            		//MessageBox("���ӵ��豸������!");
            		
            		Log.d(Common.TAG, "���ӵ��豸������!");
            		
            		//����ʱ���ӳ������߳�
            		StartThreadTimer();
            	}
            } 
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) 
            {
            	UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            	if( device.getProductId() == Common.MT_PID && device.getVendorId() == Common.MT_VID )
            	{
            		//MessageBox("���ӵ��豸�γ���!");
            		
            		Log.d(Common.TAG, "���ӵ��豸�γ���!");
            		
            		//ֹͣ�����߳�
            		if( m_ServerThread != null )
            		{
            			m_ServerThread.Stop();
            			//MessageBox("ֹͣ�����߳�!!!");
            			Log.d(Common.TAG, "ֹͣ�����߳�!!!!");
            		}
            		
            		//MessageBox("���ӵ��豸�γ���!");
            	}
            }
            else if( ACTION_USB_PERMISSION.equals(action) )  //Ȩ������
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
            			
            			//û����Ȩ
            			if( intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false ) == false )
            			{
            				//����Ȩ��ֱ���˳������û��Լ��ٴ��ֶ�����
            				
            				MessageBox("û��USB������Ȩ���޷��������������ܣ�����������������Ȩ��");
            				
            				//Log.d(Common.TAG, "ֹͣ�����߳�!!!!");
            				
            				//��������
            				m_restart = false;
            				
    	            		//������ͼ �˳�����
    	            		sendBroadcast(new Intent(Common.ACTION_RECIVEHIDSERVER));
            				
            				//�˳�����
            				stopSelf();
            				return;
            			}
            			
            			isSuccess = true;
            		}while( false );
            		
            		if( isSuccess == true )
            		{
            			//Ȩ���������
                		m_reqPermission = false; 
            			m_ServerThread.Start();
            			StopThreadTimer();
            		}
	            }
            }
        }
    };
	
	//��������ʱ����
	@Override
	
	public void onStart(Intent intent, int startId) 
	{
		// TODO Auto-generated method stub
		
		
		//MessageBox("Hid Service Started success");
	}
	
	//�رշ��� �����豸���
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		try {
			//�������������Ϣ
			unregisterReceiver(mUsbReceiver);
		} catch (Exception e) {
			// TODO: handle exception
			Log.e(Common.TAG, "��û��ע��!");
		}
		
		
		//�رռ��ʱ��
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
		
		//�رտ����߳�ʱ��
		StopThreadTimer();
		
		//��ֹ�����߳�
		if( m_ServerThread != null )
		{
			m_ServerThread.Stop();
			m_ServerThread = null;
		}
		
		//�����������񣬷����ܱ��쳣��ֹ
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
