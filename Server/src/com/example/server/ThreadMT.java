package com.example.server;

import java.nio.ByteBuffer;

import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;
 
public class ThreadMT extends Thread {
	
	//����
	private static final byte PACKET_IDEN_READ_CONFIG = 0x30;
	private static final byte PACKET_IDEN_SET_TOUCHWIN_MODE = 0x35;
	private static final byte PACKET_IDEN_SEND_POS = 0x52;   //�л���ƻ��ģʽ
	
	//����ģʽ
	private static final byte TOUCHWIN_MODE_HARDWARE = 0x02;   //���� ����ģʽ
	
	private ServerThread m_Server = null;
	
	//�жϴ���ӿ�
	private UsbEndpoint m_epIntOut = null;
	private UsbEndpoint m_epIntIn  = null;
	private UsbDeviceConnection m_connect = null;
	
	//�Ƿ�ֹͣ�߳�
	private boolean m_Stop = false;
	
	//ȷ���߳��Ѿ���ʼ����
	private boolean m_running = false;
	
	//�豸��Ϣ
	private int m_LedWidth = 0;
	private int m_LedHeight = 0;
	
	//�����Ļ��Ϣ
	private int m_ScreenWidth = 0;
	private int m_ScreenHeight = 0;
	
	//������
	private MutilQueue m_MutilQueue = null;
	
	//У׼��
	private Calibrate m_calibrate = null;
	
	//�������ݵ���ͼ
	private Intent m_sendIntent = null; 
	
	//������Ϣ
	static class TouchData
	{
	    public int TouchNum;    //�ܹ����µĵ���

	    public int TouchType[] = null;   //��ǰ����ģʽ
	    public int TouchId[] = null;     //��ǰ�������ID

	    public float TouchX[] = null;
	    public float TouchY[] = null;

	    public float LedX[] = null;        //�����������
	    public float LedY[] = null;
	    
	    TouchData()
	    {
	    	TouchNum = 0;    //�ܹ����µĵ���

	 	    TouchType = new int[2];   //��ǰ����ģʽ
	 	    TouchId   = new int[2];     //��ǰ�������ID

	 	    TouchX = new float[2];
	 	    TouchY = new float[2];

	 	    LedX = new float[2];        //�����������
	 	    LedY = new float[2];
	    }
	}
	
	//���뵽����еĵ�
	
	static class MouseInput
	{
	    public int TouchType;    // ��ǰ����ģʽ
	    public double TouchX;
	    public double TouchY;
	    
	    public MouseInput()
	    {
	    	TouchType = 0;
	    	TouchX = 0;
	    	TouchY = 0;
	    }
	}
	
	//��Ļ��Ϣ
	class ScreenInfo
	{
		public int ScreenWidth = 0;
		public int ScreenHeight = 0;
	}
	
	//У׼����Ϣ
	class CalibraInfo
	{
		double   xUnCalibrate;     //ԭʼ��x����
		double   yUnCalibrate;     //ԭʼ��y����
		double   pCalX;     //ת�����x����
		double   pCalY;      //ת�����y����
	}
	
	//����ǰһ�����Ϣ
    public TouchData m_preData = null;
    public MouseInput m_preInput = null;
    public CalibraInfo m_CalibraInfo = null;
    
	public ThreadMT( ServerThread Server )
	{
		m_Server = Server;
		m_Stop = false;
		
		//�����Ļ��Ϣ
		m_ScreenWidth = Server.m_ScreenWidth;
		m_ScreenHeight = Server.m_ScreenHeight;
		
		//��ʼ��У׼��
		m_calibrate = new Calibrate(Server.m_Context);
		
		//��ʼ��������
		m_MutilQueue = new MutilQueue();
		
		Log.i(Common.TAG, "m_ScreenWidth = " + m_ScreenWidth);
		Log.i(Common.TAG, "m_ScreenHeight = " + m_ScreenHeight);
	}
	
	//MT�̳߳�ʼ��
	public void Init()
	{
		//�豸��Ϣ
		m_connect = m_Server.m_connect;
		m_epIntOut = m_Server.m_epIntOut;
		m_epIntIn  = m_Server.m_epIntIn;
	}
	
	//��ʼ�߳� ��Ҫ����һЩ��ʼ��
	public void MTStart()
	{
		m_Stop = false;
		m_running = false;
		m_LedWidth = 0;
		m_LedHeight = 0;
		
		//����ǰһ�����Ϣ
	    m_preData = new TouchData();
	    m_preInput = new MouseInput();
	    m_CalibraInfo = new CalibraInfo();
		
		//У׼��ʼ��
		double screenX[] = new double[4];
		double screenY[] = new double[4];
		screenX[0] = 100.0;
	    screenX[1] = m_ScreenWidth - 100.0;
	    screenX[2] = m_ScreenWidth - 100.0;
	    screenX[3] = 100.0;

	    screenY[0] = 100.0;
	    screenY[1] = 100.0;
	    screenY[2] = m_ScreenHeight - 100.0;
	    screenY[3] = m_ScreenHeight - 100.0;
		m_calibrate.Init(screenX, screenY);
		
		//��ʼ�����
		Mouse.InitMouse(m_ScreenWidth, m_ScreenHeight);
		
		//��ʼ�߳�
		start();
		
		//ȷ���߳��Ѿ���ʼ����
		while( m_running == false )
		{
			try 
			{
				sleep(100);
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
		}
		
		Log.i(Common.TAG, "MT�̵߳ȴ���������,�ɹ���");
	}
	
	//У׼
	public void MTCalibrate(
			double[]   pScreenXBuffer,        //
	        double[]   pScreenYBuffer,        //
	        double[]   pUncalXBuffer,         //
	        double[]   pUncalYBuffer          //
			)
	{
		m_calibrate.SetCalibration(pScreenXBuffer, pScreenYBuffer, pUncalXBuffer, pUncalYBuffer);
	}
	
	//ֹͣ�߳�
	public void MTStop()
	{
		m_Stop = true;
		
		//��ʼ���ͷ�
		Mouse.ReleaseMouse();
		
		try 
		{
			join(); // wait for secondary to finish
		} 
		catch (InterruptedException e) 
		{
			throw new RuntimeException(e);
		}
		
		Log.i(Common.TAG, "MT�߳�Stop�ɹ�");
	}
	
	//��ʼ�����
	private boolean MTInitWidthAndHeight()
	{
		boolean iRet = false;
		ScreenInfo screen = new ScreenInfo();
		m_LedWidth = 0;
		m_LedHeight = 0;
		
		//��ȡ������Ϣ
		do
		{
			if( MTGetConfig(screen) == false )
			{
				Log.e(Common.TAG, "���������Ϣʧ��");
				break;
			}
			
			if( screen.ScreenWidth <= 0 || screen.ScreenHeight <= 0 )
			{
				Log.e(Common.TAG, "ScreenWidth = 0 and ScreenHeight = 0");
				break;
			}
			
			m_LedWidth = screen.ScreenWidth;
			m_LedHeight = screen.ScreenHeight;
			
			iRet = true;
		}while( false );
		
		Log.d(Common.TAG, "m_LedWidth = " + m_LedWidth);
		Log.d(Common.TAG, "m_LedHeight = " + m_LedHeight);
		return iRet;
	}
	
	//���ж������߳�
	@Override
	public void run() 
	{
		int iRet = 0;
		TouchData m_TouchData = new TouchData();
		byte[] Data = new byte[64];
		ByteBuffer bBuffer = ByteBuffer.allocate(64);
		
		//�����߳��Ѿ���ʼ����
		synchronized (this) {
			m_running = true;
        }
		
		Log.i(Common.TAG, "��������MT�������̣߳���");
		
		//����豸�Ŀ��
		if( MTInitWidthAndHeight() == false )
		{
			m_Stop = true;
			
			Log.e(Common.TAG, "��ÿ��ʧ�ܣ���");
			return;
		}
		
		//�л�������ģʽ
		if( MTSetWorkmode(TOUCHWIN_MODE_HARDWARE) == false )
		{
			Log.e(Common.TAG, "�л�����ģʽʧ��");
			return;
		}
		
		//�л���ƻ��ģʽ
		if( MTSetApplemode() == false )
		{
			Log.e(Common.TAG, "�л���ƻ��ģʽʧ��");
			return;
		}
		
		//���ģʽ��ʹ��
		m_preFirstPointId = -1;
		m_preSecondPointId = -1;
		
		//��ʼ�����
		//Mouse.InitMouse();
		
		//��ʽ��ʼ����
		while( true )
		{
			synchronized (this) 
			{
				if( m_Stop == true )
				{
					break;
				}
        	}
		
			//���￪ʼ������
			if( MTGetIOSData(Data, bBuffer) == false )
			{
				Log.e(Common.TAG, "��ȡ��������ʧ����!!!");
				break;
			}
			
			//���￪ʼд��������
			MTParatouchdata(Data, m_TouchData);
			
			//�����������
			MTParaMultiInput( m_TouchData );
			
			//Log.i(Common.TAG, "��ȡ�������ݳɹ���  iRet = " + iRet);
			
			try 
			{
				sleep(10);
			} 
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}

		}
		
		//��ʼ���ͷ�
		//Mouse.ReleaseMouse();
	}
	
	//��¼���ģʽ�� ֮ǰ�ĵ�
	private int m_preFirstPointId = -1;
	private int m_preSecondPointId = -1;
	private TouchData m_preTouchData = new TouchData();
	
	//�����������
	private void MTParaMultiInput( TouchData pTouchData )
	{
		//��ǰ��û�м�¼֮ǰ�ĵ�
		if( m_preFirstPointId == -1 && m_preSecondPointId == -1 )
		{
			//ֻ��һ����
			if( pTouchData.TouchNum == 1 )
			{
				//ֻ�е���һ�����ǰ��²���Ч
				if( pTouchData.TouchType[0] == Mouse.TOUCH_DOWN )
				{
					m_preFirstPointId = pTouchData.TouchId[0];
					DisposeMultiinput(pTouchData);
					return;
				}
				return;
			}
			
			if( pTouchData.TouchNum == 2 )
			{
				//��������²������Ƶ�
				if( pTouchData.TouchType[0] == Mouse.TOUCH_UP &&
					pTouchData.TouchType[1] == Mouse.TOUCH_UP )
				{
					return;
				}
				
				if( pTouchData.TouchType[1] == Mouse.TOUCH_UP )
				{
					pTouchData.TouchNum = 1;
					m_preFirstPointId = pTouchData.TouchId[0];
					DisposeMultiinput(pTouchData);
					return;
				}
				
				if( pTouchData.TouchType[0] == Mouse.TOUCH_UP )
				{
					pTouchData.LedX[0] = pTouchData.LedX[1];
					pTouchData.LedY[0] = pTouchData.LedY[1];
					pTouchData.TouchX[0] = pTouchData.TouchX[1];
					pTouchData.TouchY[0] = pTouchData.TouchY[1];
					pTouchData.TouchType[0] = pTouchData.TouchType[1];
					pTouchData.TouchId[0] = pTouchData.TouchId[1];
					
					pTouchData.TouchNum = 1;
					m_preFirstPointId = pTouchData.TouchId[0];
					DisposeMultiinput(pTouchData);
					return;
				}
				m_preFirstPointId = pTouchData.TouchId[0];
				m_preSecondPointId = pTouchData.TouchId[1];
				DisposeMultiinput(pTouchData);
				return;
			}
			return;
		}
		
		//���ж��ǵ��� ��������
		if( pTouchData.TouchNum == 1 )
		{
			//�жϵ�ǰ����UP ����down
			if( pTouchData.TouchType[0] == Mouse.TOUCH_DOWN )
			{
				//���ж��Ƿ�
				if( pTouchData.TouchId[0] != m_preFirstPointId && 
					pTouchData.TouchId[0] != m_preSecondPointId	)
				{
					if( m_preFirstPointId != -1 )
					{
						m_preTouchData.TouchNum = 1;
						m_preTouchData.TouchType[0] = Mouse.TOUCH_UP;
						m_preFirstPointId = -1;
						DisposeMultiinput(m_preTouchData);
					}
					
					if( m_preSecondPointId != -1 )
					{
						m_preTouchData.TouchNum = 1;
						m_preTouchData.TouchType[0] = Mouse.TOUCH_UP;
						m_preSecondPointId = -1;
						DisposeMultiinput(m_preTouchData);
					}
					
					//Ȼ���ٷ��͵���ĵ�
					m_preFirstPointId = pTouchData.TouchId[0];
					DisposeMultiinput(pTouchData);
					return;
				}
				
				//������һ�������
				if( pTouchData.TouchId[0] == m_preFirstPointId )
				{
					pTouchData.TouchType[0] = Mouse.TOUCH_MOVE;
					DisposeMultiinput(pTouchData);
					return;
				}
				
				//�����ڶ��������
				if( pTouchData.TouchId[0] == m_preSecondPointId )
				{
					pTouchData.TouchType[0] = Mouse.TOUCH_MOVE;
					DisposeMultiinput(pTouchData);
					return;
				}
				
				return;
			}
			
			if( pTouchData.TouchType[0] == Mouse.TOUCH_UP )
			{
				//���ж��Ƿ�
				if( pTouchData.TouchId[0] != m_preFirstPointId && 
					pTouchData.TouchId[0] != m_preSecondPointId	)
				{
					if( m_preFirstPointId != -1 )
					{
						m_preTouchData.TouchNum = 1;
						m_preTouchData.TouchType[0] = Mouse.TOUCH_UP;
						m_preFirstPointId = -1;
						DisposeMultiinput(m_preTouchData);
					}
					
					if( m_preSecondPointId != -1 )
					{
						m_preTouchData.TouchNum = 1;
						m_preTouchData.TouchType[0] = Mouse.TOUCH_UP;
						m_preSecondPointId = -1;
						DisposeMultiinput(m_preTouchData);
					}
					return;
				}
				
				//������һ�������
				if( pTouchData.TouchId[0] == m_preFirstPointId )
				{
					//pTouchData.TouchType[0] = Mouse.TOUCH_MOVE;
					m_preFirstPointId = -1;
					DisposeMultiinput(pTouchData);
					return;
				}
				
				//�����ڶ��������
				if( pTouchData.TouchId[0] == m_preSecondPointId )
				{
					//pTouchData.TouchType[0] = Mouse.TOUCH_MOVE;
					m_preSecondPointId = -1;
					DisposeMultiinput(pTouchData);
					return;
				}
				
				return;
			}
			
			return;
		}
		
		//���ﴦ����������
		//���ж��Ƿ������㶼����֮ǰ��¼�ĵ�
		if( pTouchData.TouchId[0] != m_preFirstPointId &&
			pTouchData.TouchId[0] != m_preSecondPointId &&
			pTouchData.TouchId[1] != m_preFirstPointId &&
			pTouchData.TouchId[1] != m_preSecondPointId )
		{
			//�ȸ�֮ǰ�ĵ㷢�ͽ�����־UP
			if( m_preFirstPointId != -1 )
			{
				m_preTouchData.TouchNum = 1;
				m_preTouchData.TouchType[0] = Mouse.TOUCH_UP;
				m_preFirstPointId = -1;
				DisposeMultiinput(m_preTouchData);
			}
			
			if( m_preSecondPointId != -1 )
			{
				m_preTouchData.TouchNum = 1;
				m_preTouchData.TouchType[0] = Mouse.TOUCH_UP;
				m_preSecondPointId = -1;
				DisposeMultiinput(m_preTouchData);
			}
			
			//��������㶼��UP ����Ҫ����
			if( pTouchData.TouchType[0] == Mouse.TOUCH_UP && 
				pTouchData.TouchType[1] == Mouse.TOUCH_UP	)
			{
				return;
			}
			
			//��������㶼��Down
			if( pTouchData.TouchType[0] == Mouse.TOUCH_DOWN && 
				pTouchData.TouchType[1] == Mouse.TOUCH_DOWN )
			{
				m_preFirstPointId = pTouchData.TouchId[0];
				m_preSecondPointId = pTouchData.TouchId[1];
				DisposeMultiinput(pTouchData);
				return;
			}
			
			//�����һ������down
			if( pTouchData.TouchType[0] == Mouse.TOUCH_DOWN )
			{
				pTouchData.TouchNum = 1;
				m_preFirstPointId = pTouchData.TouchId[0];
				DisposeMultiinput(pTouchData);
				return;
			}
			
			//����ڶ�������down
			if( pTouchData.TouchType[1] == Mouse.TOUCH_DOWN )
			{
				pTouchData.TouchNum = 1;
				pTouchData.LedX[0] = pTouchData.LedX[1];
				pTouchData.LedY[0] = pTouchData.LedY[1];
				pTouchData.TouchX[0] = pTouchData.TouchX[1];
				pTouchData.TouchY[0] = pTouchData.TouchY[1];
				pTouchData.TouchType[0] = pTouchData.TouchType[1];
				pTouchData.TouchId[0] = pTouchData.TouchId[1];
				m_preFirstPointId = pTouchData.TouchId[0];
				DisposeMultiinput(pTouchData);
				return;
			}
			return;
		}
		
		//��������㶼��֮ǰ��¼�ĵ�
		if( pTouchData.TouchId[0] == m_preFirstPointId &&
			pTouchData.TouchId[1] == m_preSecondPointId )
		{
			if( pTouchData.TouchType[0] == Mouse.TOUCH_DOWN )
			{
				pTouchData.TouchType[0] = Mouse.TOUCH_MOVE;
			}
			else if( pTouchData.TouchType[0] == Mouse.TOUCH_UP )
			{
				m_preFirstPointId = -1;
			}
			
			if( pTouchData.TouchType[1] == Mouse.TOUCH_DOWN )
			{
				pTouchData.TouchType[1] = Mouse.TOUCH_MOVE;
			}
			else if( pTouchData.TouchType[1] == Mouse.TOUCH_UP )
			{
				m_preSecondPointId = -1;
			}
			DisposeMultiinput(pTouchData);
			return;
		}
		if( pTouchData.TouchId[0] == m_preSecondPointId &&
			pTouchData.TouchId[1] == m_preFirstPointId )
		{
			if( pTouchData.TouchType[0] == Mouse.TOUCH_DOWN )
			{
				pTouchData.TouchType[0] = Mouse.TOUCH_MOVE;
			}
			else if( pTouchData.TouchType[0] == Mouse.TOUCH_UP )
			{
				m_preSecondPointId = -1;
			}
			
			if( pTouchData.TouchType[1] == Mouse.TOUCH_DOWN )
			{
				pTouchData.TouchType[1] = Mouse.TOUCH_MOVE;
			}
			else if( pTouchData.TouchType[1] == Mouse.TOUCH_UP )
			{
				m_preFirstPointId = -1;
			}
			DisposeMultiinput(pTouchData);
			return;
		}
		
		//�������ֻ��һ������֮ǰ��¼�ĵ�
		if( pTouchData.TouchId[0] == m_preFirstPointId )
		{
			//��ǰһ�����ȷ��ͽ�����־
			if( m_preSecondPointId != -1 )
			{
				m_preTouchData.TouchNum = 1;
				m_preTouchData.TouchType[0] = Mouse.TOUCH_UP;
				m_preSecondPointId = -1;
				DisposeMultiinput(m_preTouchData);
			}
			//���¼�¼�µ�
			m_preSecondPointId = pTouchData.TouchId[1];
			
			if( pTouchData.TouchType[0] == Mouse.TOUCH_DOWN )
			{
				pTouchData.TouchType[0] = Mouse.TOUCH_MOVE;
			}
			DisposeMultiinput(pTouchData);
			return;
		}
		if( pTouchData.TouchId[0] == m_preSecondPointId )
		{
			//��ǰһ�����ȷ��ͽ�����־
			if( m_preFirstPointId != -1 )
			{
				m_preTouchData.TouchNum = 1;
				m_preTouchData.TouchType[0] = Mouse.TOUCH_UP;
				m_preFirstPointId = -1;
				DisposeMultiinput(m_preTouchData);
			}
			//���¼�¼�µ�
			m_preFirstPointId = pTouchData.TouchId[1];
			
			if( pTouchData.TouchType[0] == Mouse.TOUCH_DOWN )
			{
				pTouchData.TouchType[0] = Mouse.TOUCH_MOVE;
			}
			DisposeMultiinput(pTouchData);
			return;
		}
		
		if( pTouchData.TouchId[1] == m_preFirstPointId )
		{
			//��ǰһ�����ȷ��ͽ�����־
			if( m_preSecondPointId != -1 )
			{
				m_preTouchData.TouchNum = 1;
				m_preTouchData.TouchType[0] = Mouse.TOUCH_UP;
				m_preSecondPointId = -1;
				DisposeMultiinput(m_preTouchData);
			}
			//���¼�¼�µ�
			m_preSecondPointId = pTouchData.TouchId[0];
			
			if( pTouchData.TouchType[1] == Mouse.TOUCH_DOWN )
			{
				pTouchData.TouchType[1] = Mouse.TOUCH_MOVE;
			}
			DisposeMultiinput(pTouchData);
			return;
		}
		if( pTouchData.TouchId[1] == m_preSecondPointId )
		{
			//��ǰһ�����ȷ��ͽ�����־
			if( m_preFirstPointId != -1 )
			{
				m_preTouchData.TouchNum = 1;
				m_preTouchData.TouchType[0] = Mouse.TOUCH_UP;
				m_preFirstPointId = -1;
				DisposeMultiinput(m_preTouchData);
			}
			//���¼�¼�µ�
			m_preFirstPointId = pTouchData.TouchId[0];
			
			if( pTouchData.TouchType[1] == Mouse.TOUCH_DOWN )
			{
				pTouchData.TouchType[1] = Mouse.TOUCH_MOVE;
			}
			DisposeMultiinput(pTouchData);
			return;
		}
		
	}
	
	//��ʱ����
	private int inputnum = 0;
	private int touchid = 0;          //��¼����һ�ʵ�ID
	private boolean inputflag = false;
	private boolean rightclick = false;
	private MouseInput downinput = new MouseInput();
	private MouseInput tempInput = new MouseInput();
	
    //���Ͷ����Ϣ
    private void DisposeMultiinput( TouchData pTouchData )
    {
    	int TouchX = 0;
	    int TouchY = 0;
	    
    	double tempx = 0.0f;
	    double tempy = 0.0f;
    	
    	//�жϵ�һ�����Ƿ�ΪUP
    	if( pTouchData.TouchType[0] == Mouse.TOUCH_UP )
    	{
    		//����У׼����
    		//����ת��
	        tempx = pTouchData.TouchX[0] / 32767.0f * m_LedWidth;
	        tempy = pTouchData.TouchY[0] / 32767.0f * m_LedHeight;
	        
        	//��ʼ������������ͼ
    		m_sendIntent = new Intent(Common.ACTION_CALIBDATA);
    		m_sendIntent.putExtra("UncalX", tempx);
    		m_sendIntent.putExtra("UncalY", tempy);
    		m_Server.m_Context.sendBroadcast(m_sendIntent);
    		Log.i(Common.TAG, "������һ��У׼���ݵ�");
    	}
    	
    	/**
    	 * ��ӡ���
    	 * */
    	/*
    	Log.d("TouchDebug", "2��ǰ�����: " + pTouchData.TouchNum);
    	for( int i = 0; i < pTouchData.TouchNum; i++ )
    	{
    		switch( pTouchData.TouchType[i] )
    		{
    		case Mouse.TOUCH_DOWN:
    		{
    			Log.d("TouchDebug", "2TOUCH_DOWN");
    		}
    		break;
    		 
    		case Mouse.TOUCH_MOVE:
    		{
    			Log.d("TouchDebug", "2TOUCH_MOVE");
    		}
    		break;
    		
    		case Mouse.TOUCH_UP:
    		{
    			Log.d("TouchDebug", "2TOUCH_UP");
    		}
    		break;
    		
    		default:
    			break;
    		}
    		Log.d("TouchDebug", "2TouchType[" + i + "] = " + pTouchData.TouchType[i]);
    		Log.d("TouchDebug", "2TouchId[" + i + "] = " + pTouchData.TouchId[i]);
    		
    		Log.d("TouchDebug", "2TouchX[" + i + "] = " + pTouchData.TouchX[i]);
    		Log.d("TouchDebug", "2TouchY[" + i + "] = " + pTouchData.TouchY[i]);
    	}
    	Log.d("TouchDebug", " ");
    	*/
    	
    	//������µ��������㣬��ֱ�Ӳ���Ӧ�Ҽ�
    	if( pTouchData.TouchNum >= 2 )
    	{
    		inputflag = false;
    		touchid = -1;
    	}
    	else if( pTouchData.TouchType[0] == Mouse.TOUCH_DOWN )
    	{
    		//������Ϣ����
    		inputflag = true;
    		rightclick = false;
    		inputnum = 0;
    		touchid = -1;
    	}
    	
    	//����ר�Ÿ����ʹ�����Ϣ
    	for( int i = 0; i < pTouchData.TouchNum; i++ )
    	{
    		tempx = pTouchData.TouchX[i] / 32767.0f * m_LedWidth;
	        tempy = pTouchData.TouchY[i] / 32767.0f * m_LedHeight;
    		
	        //Log.i(Common.TAG, "����������:TouchX = " + tempx + " " + "TouchY = " + tempy);
	        
	        //Log.i("TouchDebug", "2�� " + (i + 1) + "����:" + "TouchX = " + tempx + " " + "TouchY = " + tempy);
	        
    		switch( pTouchData.TouchType[i] )
    		{
    		case Mouse.TOUCH_DOWN:
    		{
    			//��ʼ������
    			m_MutilQueue.InitQueue(pTouchData.TouchId[i]);
    			//��ӵ�һ����
    			m_MutilQueue.AddPoint(pTouchData.TouchX[i], 
    					pTouchData.TouchY[i], pTouchData.TouchId[i], tempInput);
    			
    			m_CalibraInfo.xUnCalibrate = tempx;
	            m_CalibraInfo.yUnCalibrate = tempy;
	            
	            //У׼����
	            m_calibrate.CalibrateAPoint(m_CalibraInfo);
	            
	            tempx = m_CalibraInfo.pCalX;
	            tempy = m_CalibraInfo.pCalY;
    			
	            downinput.TouchX = tempx;
	            downinput.TouchY = tempy;
	            
	            /**
	             * ������û��У׼�Ĳ���
	             * */
		        //tempx = pTouchData.TouchX[i] / 32767.0f * m_ScreenWidth;
		        //tempy = pTouchData.TouchY[i] / 32767.0f * m_ScreenHeight;
	            
	            TouchX = (int)tempx;
	            TouchY = (int)tempy;
	            
	            Mouse.mouse_multi_down(TouchX, TouchY, pTouchData.TouchId[i]);
	            Log.i("TouchDebug", "Down: 4�� " + (i + 1) + "����:" + "TouchX = " + TouchX + " " + "TouchY = " + TouchY);
    		}
    		break;
    		  
    		case Mouse.TOUCH_MOVE:
    		{
    			if( m_MutilQueue.AddPoint(pTouchData.TouchX[i], 
    									  pTouchData.TouchY[i], 
    									  pTouchData.TouchId[i], 
    									  tempInput) == true && rightclick == false )
    			{
    				//����ת��
	                tempx = tempInput.TouchX / 32767.0f * m_LedWidth;
	                tempy = tempInput.TouchY / 32767.0f * m_LedHeight;
	                
	                //Log.i("TouchDebug", "3�� " + (i + 1) + "����:" + "TouchX = " + tempx + " " + "TouchY = " + tempy);
	                
	                m_CalibraInfo.xUnCalibrate = tempx;
		            m_CalibraInfo.yUnCalibrate = tempy;
		            
		            //У׼����
		            m_calibrate.CalibrateAPoint(m_CalibraInfo);
		            
		            tempx = m_CalibraInfo.pCalX;
		            tempy = m_CalibraInfo.pCalY;
 		             
		            /**
		             * ������û��У׼�Ĳ���
		             * */
			        //tempx = pTouchData.TouchX[i] / 32767.0f * m_ScreenWidth;
			        //tempy = pTouchData.TouchY[i] / 32767.0f * m_ScreenHeight;
		            
		            TouchX = (int)tempx;
		            TouchY = (int)tempy;
		            
		            //�ж��Ƿ�����������Ϣ
		            if( inputflag == true )  //��������Ҽ��� �Ͳ�����Ӧmove
		            {
		            	int tempabsx = (int)downinput.TouchX;
	                    int tempabsy = (int)downinput.TouchY;

	                    if( Math.abs(tempabsx - TouchX) <= 15 &&
	                    	Math.abs(tempabsy - TouchY) <= 15 )
	                    {
	                        inputnum++;
	                    }
	                    else
	                    {
	                        inputnum = 0;
	                        inputflag = false;
	                    }
	                     
	                    //�����Ҽ�����
	                    if( inputnum >= 140 )
	                    {
	                    	inputflag = false;
	                        inputnum = 0;
	                        rightclick = true;
	                        touchid = pTouchData.TouchId[i];
	                        
	                        //����ȵ���
	                        Mouse.mouse_multi_up(pTouchData.TouchId[i]);
	                        //m_MutilQueue.DestroyQueue(pTouchData.TouchId[i]);
	                        
	                        //��˯200����
	                        try 
	            			{
	            				sleep(200);
	            			} 
	            			catch (InterruptedException e)
	            			{
	            				e.printStackTrace();
	            			}
	                        
	                        //�����Ҽ�
	                        Mouse.mouse_right_key();

	                        Log.i(Common.TAG, "���ݲ��ѷ����Ҽ�");
	                        return;
	                    }
		            }
		            
		            Mouse.mouse_multi_move(TouchX, TouchY, pTouchData.TouchId[i]);
		            Log.i("TouchDebug", "Move: 4�� " + (i + 1) + "����:" + "TouchX = " + TouchX + " " + "TouchY = " + TouchY);
    			}
    		}
    		break;
    		
    		case Mouse.TOUCH_UP:
    		{
    			//�����ٶ���
    			m_MutilQueue.DestroyQueue(pTouchData.TouchId[i]);
    			
    			inputnum = 0;
	            inputflag = false;
    			if( touchid == pTouchData.TouchId[i] )
    			{
    				if( rightclick == true )
    	            {
    	                rightclick = false;
    	                touchid = -1;
    	                return;
    	            }
    			}
    			
    			Mouse.mouse_multi_up(pTouchData.TouchId[i]);
    		}
    		break;
    		
    		default:
    			break;
    		
    		}
    	}
    	
    }
	
	//��ȡ����
	private void MTParatouchdata( byte[] pData, TouchData pTouch )
	{
		if( pData == null || pTouch == null )
	    {
			Log.e(Common.TAG, "�����������");
	        return;
	    }
		
		pTouch.TouchNum = pData[63] > 2 ? 2 : pData[63];
		
		for( int i = 0; i < pTouch.TouchNum; i++ )
	    {
	        pTouch.TouchType[i] = (int) (0xFF & pData[3 + 6 * i]);
	        pTouch.TouchId[i] = (int) (0xFF & pData[4 + 6 * i]);

	        pTouch.TouchX[i] = (float)(((0xFF & pData[6 + 6 * i]) << 8) | ( 0xFF & pData[5 + 6 * i] ));
	        pTouch.TouchY[i] = (float)(((0xFF & pData[8 + 6 * i]) << 8) | ( 0xFF & pData[7 + 6 * i] ));
	        
	        //Log.i(Common.TAG, "TouchX[" + i + "]" + "=" + pTouch.TouchX[i]);
            //Log.i(Common.TAG, "TouchY[" + i + "]" + "=" + pTouch.TouchY[i]);
	         
	        if( m_LedHeight > 0 && m_LedWidth > 0 )
	        {
	            pTouch.LedX[i] = (pTouch.TouchX[i] * m_LedWidth / 32767.0f);
	            pTouch.LedY[i] = (pTouch.TouchY[i] * m_LedHeight / 32767.0f);
	            //Log.i(Common.TAG, "LedX[" + i + "]" + "=" + pTouch.LedX[i]);
	            //Log.i(Common.TAG, "LedY[" + i + "]" + "=" + pTouch.LedY[i]);
	        }
	        
	        /**
	         * ���Դ�ӡ
	         * */
	        /*
	        double tempx = 0;
	        double tempy = 0;
	        int Touchx = 0;
	        int Touchy = 0;
	        tempx = pTouch.TouchX[i] / 32767.0f * m_LedWidth;
	        tempy = pTouch.TouchY[i] / 32767.0f * m_LedHeight;
	        Touchx = (int)tempx;
	        Touchy = (int)tempy;
	        
	        Log.i("TouchDebug", "1�� " + (i + 1) + "����:" + "TouchX = " + Touchx + " " + "TouchY = " + Touchy);
	        */
	    }
		
		/*
		for( int i = 0; i < 64; i++ )
		{
			int temp = (int)( 0xFF & pData[i] );
			Log.d("TouchDebug", "pData[" + i + "] = " + temp );
		}
		*/
		
		
		/**
    	 * ��ӡ���
    	 * */
		/*
    	Log.d("TouchDebug", "1��ǰ�����: " + pTouch.TouchNum);
    	for( int i = 0; i < pTouch.TouchNum; i++ )
    	{
    		switch( pTouch.TouchType[i] )
    		{
    		case Mouse.TOUCH_DOWN:
    		{
    			Log.d("TouchDebug", "1TOUCH_DOWN");
    		}
    		break;
    		
    		case Mouse.TOUCH_MOVE:
    		{
    			Log.d("TouchDebug", "1TOUCH_MOVE");
    		}
    		break;
    		
    		case Mouse.TOUCH_UP:
    		{
    			Log.d("TouchDebug", "1TOUCH_UP");
    		}
    		break;
    		
    		default:
    			break;
    		}
    		Log.d("TouchDebug", "1TouchType[" + i + "] = " + pTouch.TouchType[i]);
    		Log.d("TouchDebug", "1TouchId[" + i + "] = " + pTouch.TouchId[i]);
    		
    		Log.d("TouchDebug", "1TouchX[" + i + "] = " + pTouch.TouchX[i]);
    		Log.d("TouchDebug", "1TouchY[" + i + "] = " + pTouch.TouchY[i]);
    	}
    	Log.d("TouchDebug", " ");
    	*/
		
	}
	
	//private static byte[] m_preUpData = new byte[64];
	
	//���ƻ������
	private boolean MTGetIOSData( byte[] Data, ByteBuffer bBuffer )
	{
		boolean iRet = false;
		int cnt = 0;
		
		do
		{
			if( m_connect == null )
			{
				Log.e(Common.TAG, "m_connect��������###!MTSetWordmode");
				break;
			}
			if( m_epIntIn == null || m_epIntOut == null )
			{
				//m_Server.MessageBox("����˿�û���ҵ�###!");
				Log.e(Common.TAG, "����˿�û���ҵ�###!MTSetWordmode");
				break;
			}
			if( bBuffer == null )
			{
				Log.e(Common.TAG, "bBuffer error###!MTSetWordmode");
				break;
			}
			
			iRet = false;
			
			//��һ�ٴ�
	        cnt = 0;
	        while( cnt < 100 )
	        {
	        	Data[0] = 0;
	        	Data[1] = 0;
	        	iRet = ServerThread.WaitDeviceData(m_connect, m_epIntIn, Data);
	        	if( iRet == false )
				{
	        		Log.e(Common.TAG, "��ȡ�豸ʧ��");
					break;
				}
	        	
	        	
	        	
	            if( Data[0] == (byte)0xFF &&
	            	Data[1] == (byte)0x52 )
	            {
	            	/*
	            	for( int i = 0; i < Data.length; i++ )
	            	{
	            		m_preUpData[i] = Data[i];
	            	}
	            	m_preUpData[3 + 7 * 0] = Mouse.TOUCH_UP;
	            	m_preUpData[3 + 7 * 1] = Mouse.TOUCH_UP;
	            	*/
	            	//Log.i(Common.TAG, "��ȡͨ�����ݳɹ�!");
	                iRet = true;
	                break;
	            }
	            cnt++;
	        }
			
		}while( false );
		
		
		return iRet;
	}
	
	//�л���ƻ��ϵͳģʽ
	private boolean MTSetApplemode()
	{
		int cnt = 0;
		boolean iRet = false;
		byte[] SendBuff = new byte[64];
		
		do
		{
			if( m_connect == null )
			{
				Log.e(Common.TAG, "m_connect��������###!MTSetWordmode");
				break;
			}
			if( m_epIntIn == null || m_epIntOut == null )
			{
				//m_Server.MessageBox("����˿�û���ҵ�###!");
				Log.e(Common.TAG, "����˿�û���ҵ�###!MTSetWordmode");
				break;
			}
			
			SendBuff[0] = (byte)0xFF;
		    SendBuff[1] = PACKET_IDEN_SEND_POS;
			
		    //д���豸
			if( ServerThread.WriteDeviceData(m_connect, m_epIntOut, SendBuff) == false )
			{
				//m_Server.MessageBox("д���豸ʧ����###!");
				Log.e(Common.TAG, "д���豸ʧ����###!MTSetWordmode");
				break;
			}
			
			//��һ�ٴ�
	        cnt = 0;
	        while( cnt < 100 )
	        {
	        	if( ServerThread.ReadDeviceData(m_connect, m_epIntIn, SendBuff) == false )
				{
	        		Log.e(Common.TAG, "��ȡ�豸ʧ��");
					break;
				}
	        
	            if( SendBuff[0] == (byte)0xFF &&
	                SendBuff[1] == (byte)0x52 &&
	                SendBuff[2] == (byte)0x01 &&
	                SendBuff[3] == (byte)0x11 &&
	                SendBuff[5] == (byte)0x01 )
	            {
	                iRet = true;
	                break;
	            }
	            cnt++;
	        }
			
		}while( false );
		
		if( iRet == true )
		{
			Log.i(Common.TAG, "�л���ƻ��ģʽ�ɹ�");
		}
		
		return iRet;
	}
	
	//�л�ģʽ
	private boolean MTSetWorkmode( byte Workmode )
	{
		boolean iRet = false;
		byte[] SendBuff = new byte[64];
		
		do
		{
			if( m_connect == null )
			{
				Log.e(Common.TAG, "m_connect��������###!MTSetWordmode");
				break;
			}
			if( m_epIntIn == null || m_epIntOut == null )
			{
				//m_Server.MessageBox("����˿�û���ҵ�###!");
				Log.e(Common.TAG, "����˿�û���ҵ�###!MTSetWordmode");
				break;
			}
			
			SendBuff[0] = (byte) 0xFF & 0;
			SendBuff[1] = (byte) 0xFF & PACKET_IDEN_SET_TOUCHWIN_MODE;
			SendBuff[5] = Workmode;
			
			//д���豸
			if( ServerThread.WriteDeviceData(m_connect, m_epIntOut, SendBuff) == false )
			{
				//m_Server.MessageBox("д���豸ʧ����###!");
				Log.e(Common.TAG, "д���豸ʧ����###!MTSetWordmode");
				break;
			}
			
			iRet = true;
			Log.i(Common.TAG,"���ù���ģʽ�ɹ�");
		}while( false );
		
		
		return iRet;
	}
	
	//��ȡ������Ϣ
	private boolean MTGetConfig( ScreenInfo screen )
	{
		boolean iRet = false;
		byte[] buff = new byte[64];
		
		do
		{
			if( m_connect == null )
			{
				Log.e(Common.TAG, "m_connect��������###!");
				break;
			}
			if( m_epIntIn == null || m_epIntOut == null )
			{
				//m_Server.MessageBox("����˿�û���ҵ�###!");
				Log.e(Common.TAG, "����˿�û���ҵ�###!");
				break;
			}
			
			buff[0] = (byte) 0xFF & 0;
			buff[1] = (byte) 0xFF & PACKET_IDEN_READ_CONFIG;
			
			//д���豸
			if( ServerThread.WriteDeviceData(m_connect, m_epIntOut, buff) == false )
			{
				//m_Server.MessageBox("д���豸ʧ����###!");
				Log.e(Common.TAG, "д���豸ʧ����###!");
				break;
			}
			
			//��ȡ����
			int cnt = 0;
			while( cnt < 100 )
			{
				if( ServerThread.ReadDeviceData(m_connect, m_epIntIn, buff) == false )
				{
					break;
				}
				
				if( buff[1] == 0x30 &&
					buff[2] == 0x01 &&
					buff[3] == 0x11 )
				{
					iRet = true;
					
					Log.i(Common.TAG, "��ȡ��������ɹ�!!!");
					break;
				}
			}
			
		}while( false );
		
		//������ת��
		if( iRet == true )
		{
			/**
			 * ����
			 **/
			/*
			for( int i = 0; i < 64; i++ )
			{
				Log.d(Common.TAG, "buff[" + i + "]" + " = " + buff[i]);
			}
			*/
			
			
			byte[] width = new byte[4];
			byte[] height = new byte[4];
			
			width[0] = buff[11 + 5];
			width[1] = buff[12 + 5];
			width[2] = buff[13 + 5];
			width[3] = buff[14 + 5];
			
			height[0] = buff[15 + 5];
			height[1] = buff[16 + 5];
			height[2] = buff[17 + 5];
			height[3] = buff[18 + 5];
			
			/*
			for( int i = 0; i < 4; i++ )
			{
				Log.d(Common.TAG, "width[" + i + "]" + " = " + width[i]);
			}
		
			for( int i = 0; i < 4; i++ )
			{
				Log.d(Common.TAG, "height[" + i + "]" + " = " + height[i]);
			}
			*/
			
			//Log.d(Common.TAG, "-101" + " = " + ((-101) & 0xFF));
			
	
			screen.ScreenWidth = Common.bytesToInt(width);
			screen.ScreenHeight = Common.bytesToInt(height);
			
			//m_Server.MessageBox("�ɹ��������!");
			Log.i(Common.TAG, "�ɹ���߻������!");
		}
		
		return iRet;
	}
	
}
