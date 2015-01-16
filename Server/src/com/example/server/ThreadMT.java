package com.example.server;

import java.nio.ByteBuffer;

import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;
 
public class ThreadMT extends Thread {
	
	//命令
	private static final byte PACKET_IDEN_READ_CONFIG = 0x30;
	private static final byte PACKET_IDEN_SET_TOUCHWIN_MODE = 0x35;
	private static final byte PACKET_IDEN_SEND_POS = 0x52;   //切换到苹果模式
	
	//工作模式
	private static final byte TOUCHWIN_MODE_HARDWARE = 0x02;   //无驱 工作模式
	
	private ServerThread m_Server = null;
	
	//中断传输接口
	private UsbEndpoint m_epIntOut = null;
	private UsbEndpoint m_epIntIn  = null;
	private UsbDeviceConnection m_connect = null;
	
	//是否停止线程
	private boolean m_Stop = false;
	
	//确保线程已经开始运行
	private boolean m_running = false;
	
	//设备信息
	private int m_LedWidth = 0;
	private int m_LedHeight = 0;
	
	//获得屏幕信息
	private int m_ScreenWidth = 0;
	private int m_ScreenHeight = 0;
	
	//多点队列
	private MutilQueue m_MutilQueue = null;
	
	//校准类
	private Calibrate m_calibrate = null;
	
	//发送数据的意图
	private Intent m_sendIntent = null; 
	
	//数据信息
	static class TouchData
	{
	    public int TouchNum;    //总共按下的点数

	    public int TouchType[] = null;   //当前触摸模式
	    public int TouchId[] = null;     //当前触摸点的ID

	    public float TouchX[] = null;
	    public float TouchY[] = null;

	    public float LedX[] = null;        //触摸框的坐标
	    public float LedY[] = null;
	    
	    TouchData()
	    {
	    	TouchNum = 0;    //总共按下的点数

	 	    TouchType = new int[2];   //当前触摸模式
	 	    TouchId   = new int[2];     //当前触摸点的ID

	 	    TouchX = new float[2];
	 	    TouchY = new float[2];

	 	    LedX = new float[2];        //触摸框的坐标
	 	    LedY = new float[2];
	    }
	}
	
	//输入到鼠标中的点
	
	static class MouseInput
	{
	    public int TouchType;    // 当前触摸模式
	    public double TouchX;
	    public double TouchY;
	    
	    public MouseInput()
	    {
	    	TouchType = 0;
	    	TouchX = 0;
	    	TouchY = 0;
	    }
	}
	
	//屏幕信息
	class ScreenInfo
	{
		public int ScreenWidth = 0;
		public int ScreenHeight = 0;
	}
	
	//校准点信息
	class CalibraInfo
	{
		double   xUnCalibrate;     //原始的x坐标
		double   yUnCalibrate;     //原始的y坐标
		double   pCalX;     //转换后的x坐标
		double   pCalY;      //转换后的y坐标
	}
	
	//保存前一点的信息
    public TouchData m_preData = null;
    public MouseInput m_preInput = null;
    public CalibraInfo m_CalibraInfo = null;
    
	public ThreadMT( ServerThread Server )
	{
		m_Server = Server;
		m_Stop = false;
		
		//获得屏幕信息
		m_ScreenWidth = Server.m_ScreenWidth;
		m_ScreenHeight = Server.m_ScreenHeight;
		
		//初始化校准类
		m_calibrate = new Calibrate(Server.m_Context);
		
		//初始化多点队列
		m_MutilQueue = new MutilQueue();
		
		Log.i(Common.TAG, "m_ScreenWidth = " + m_ScreenWidth);
		Log.i(Common.TAG, "m_ScreenHeight = " + m_ScreenHeight);
	}
	
	//MT线程初始化
	public void Init()
	{
		//设备信息
		m_connect = m_Server.m_connect;
		m_epIntOut = m_Server.m_epIntOut;
		m_epIntIn  = m_Server.m_epIntIn;
	}
	
	//开始线程 需要进行一些初始化
	public void MTStart()
	{
		m_Stop = false;
		m_running = false;
		m_LedWidth = 0;
		m_LedHeight = 0;
		
		//保存前一点的信息
	    m_preData = new TouchData();
	    m_preInput = new MouseInput();
	    m_CalibraInfo = new CalibraInfo();
		
		//校准初始化
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
		
		//初始化鼠标
		Mouse.InitMouse(m_ScreenWidth, m_ScreenHeight);
		
		//开始线程
		start();
		
		//确保线程已经开始运行
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
		
		Log.i(Common.TAG, "MT线程等待启动结束,成功！");
	}
	
	//校准
	public void MTCalibrate(
			double[]   pScreenXBuffer,        //
	        double[]   pScreenYBuffer,        //
	        double[]   pUncalXBuffer,         //
	        double[]   pUncalYBuffer          //
			)
	{
		m_calibrate.SetCalibration(pScreenXBuffer, pScreenYBuffer, pUncalXBuffer, pUncalYBuffer);
	}
	
	//停止线程
	public void MTStop()
	{
		m_Stop = true;
		
		//初始化释放
		Mouse.ReleaseMouse();
		
		try 
		{
			join(); // wait for secondary to finish
		} 
		catch (InterruptedException e) 
		{
			throw new RuntimeException(e);
		}
		
		Log.i(Common.TAG, "MT线程Stop成功");
	}
	
	//初始化宽高
	private boolean MTInitWidthAndHeight()
	{
		boolean iRet = false;
		ScreenInfo screen = new ScreenInfo();
		m_LedWidth = 0;
		m_LedHeight = 0;
		
		//读取配置信息
		do
		{
			if( MTGetConfig(screen) == false )
			{
				Log.e(Common.TAG, "获得配置信息失败");
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
	
	//运行读数据线程
	@Override
	public void run() 
	{
		int iRet = 0;
		TouchData m_TouchData = new TouchData();
		byte[] Data = new byte[64];
		ByteBuffer bBuffer = ByteBuffer.allocate(64);
		
		//设置线程已经开始运行
		synchronized (this) {
			m_running = true;
        }
		
		Log.i(Common.TAG, "开启运行MT读数据线程！！");
		
		//获得设备的宽高
		if( MTInitWidthAndHeight() == false )
		{
			m_Stop = true;
			
			Log.e(Common.TAG, "获得宽高失败！！");
			return;
		}
		
		//切换到工作模式
		if( MTSetWorkmode(TOUCHWIN_MODE_HARDWARE) == false )
		{
			Log.e(Common.TAG, "切换工作模式失败");
			return;
		}
		
		//切换到苹果模式
		if( MTSetApplemode() == false )
		{
			Log.e(Common.TAG, "切换到苹果模式失败");
			return;
		}
		
		//多点模式下使用
		m_preFirstPointId = -1;
		m_preSecondPointId = -1;
		
		//初始化鼠标
		//Mouse.InitMouse();
		
		//正式开始进程
		while( true )
		{
			synchronized (this) 
			{
				if( m_Stop == true )
				{
					break;
				}
        	}
		
			//这里开始读数据
			if( MTGetIOSData(Data, bBuffer) == false )
			{
				Log.e(Common.TAG, "读取触摸数据失败了!!!");
				break;
			}
			
			//这里开始写解析数据
			MTParatouchdata(Data, m_TouchData);
			
			//解析多点数据
			MTParaMultiInput( m_TouchData );
			
			//Log.i(Common.TAG, "读取触摸数据成功了  iRet = " + iRet);
			
			try 
			{
				sleep(10);
			} 
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}

		}
		
		//初始化释放
		//Mouse.ReleaseMouse();
	}
	
	//记录多点模式下 之前的点
	private int m_preFirstPointId = -1;
	private int m_preSecondPointId = -1;
	private TouchData m_preTouchData = new TouchData();
	
	//解析多点数据
	private void MTParaMultiInput( TouchData pTouchData )
	{
		//当前还没有记录之前的点
		if( m_preFirstPointId == -1 && m_preSecondPointId == -1 )
		{
			//只有一个点
			if( pTouchData.TouchNum == 1 )
			{
				//只有当第一个点是按下才生效
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
				//这种情况下不进行推点
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
		
		//先判断是单点 还是两点
		if( pTouchData.TouchNum == 1 )
		{
			//判断当前点是UP 还是down
			if( pTouchData.TouchType[0] == Mouse.TOUCH_DOWN )
			{
				//先判断是否
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
					
					//然后再发送点击的点
					m_preFirstPointId = pTouchData.TouchId[0];
					DisposeMultiinput(pTouchData);
					return;
				}
				
				//如果与第一个点相等
				if( pTouchData.TouchId[0] == m_preFirstPointId )
				{
					pTouchData.TouchType[0] = Mouse.TOUCH_MOVE;
					DisposeMultiinput(pTouchData);
					return;
				}
				
				//如果与第二个点相等
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
				//先判断是否
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
				
				//如果与第一个点相等
				if( pTouchData.TouchId[0] == m_preFirstPointId )
				{
					//pTouchData.TouchType[0] = Mouse.TOUCH_MOVE;
					m_preFirstPointId = -1;
					DisposeMultiinput(pTouchData);
					return;
				}
				
				//如果与第二个点相等
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
		
		//这里处理两点的情况
		//先判断是否两个点都不是之前记录的点
		if( pTouchData.TouchId[0] != m_preFirstPointId &&
			pTouchData.TouchId[0] != m_preSecondPointId &&
			pTouchData.TouchId[1] != m_preFirstPointId &&
			pTouchData.TouchId[1] != m_preSecondPointId )
		{
			//先给之前的点发送结束标志UP
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
			
			//如果两个点都是UP 则不需要处理
			if( pTouchData.TouchType[0] == Mouse.TOUCH_UP && 
				pTouchData.TouchType[1] == Mouse.TOUCH_UP	)
			{
				return;
			}
			
			//如果两个点都是Down
			if( pTouchData.TouchType[0] == Mouse.TOUCH_DOWN && 
				pTouchData.TouchType[1] == Mouse.TOUCH_DOWN )
			{
				m_preFirstPointId = pTouchData.TouchId[0];
				m_preSecondPointId = pTouchData.TouchId[1];
				DisposeMultiinput(pTouchData);
				return;
			}
			
			//如果第一个点是down
			if( pTouchData.TouchType[0] == Mouse.TOUCH_DOWN )
			{
				pTouchData.TouchNum = 1;
				m_preFirstPointId = pTouchData.TouchId[0];
				DisposeMultiinput(pTouchData);
				return;
			}
			
			//如果第二个点是down
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
		
		//如果两个点都是之前记录的点
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
		
		//如果其中只有一个点是之前记录的点
		if( pTouchData.TouchId[0] == m_preFirstPointId )
		{
			//把前一个点先发送结束标志
			if( m_preSecondPointId != -1 )
			{
				m_preTouchData.TouchNum = 1;
				m_preTouchData.TouchType[0] = Mouse.TOUCH_UP;
				m_preSecondPointId = -1;
				DisposeMultiinput(m_preTouchData);
			}
			//重新记录新点
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
			//把前一个点先发送结束标志
			if( m_preFirstPointId != -1 )
			{
				m_preTouchData.TouchNum = 1;
				m_preTouchData.TouchType[0] = Mouse.TOUCH_UP;
				m_preFirstPointId = -1;
				DisposeMultiinput(m_preTouchData);
			}
			//重新记录新点
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
			//把前一个点先发送结束标志
			if( m_preSecondPointId != -1 )
			{
				m_preTouchData.TouchNum = 1;
				m_preTouchData.TouchType[0] = Mouse.TOUCH_UP;
				m_preSecondPointId = -1;
				DisposeMultiinput(m_preTouchData);
			}
			//重新记录新点
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
			//把前一个点先发送结束标志
			if( m_preFirstPointId != -1 )
			{
				m_preTouchData.TouchNum = 1;
				m_preTouchData.TouchType[0] = Mouse.TOUCH_UP;
				m_preFirstPointId = -1;
				DisposeMultiinput(m_preTouchData);
			}
			//重新记录新点
			m_preFirstPointId = pTouchData.TouchId[0];
			
			if( pTouchData.TouchType[1] == Mouse.TOUCH_DOWN )
			{
				pTouchData.TouchType[1] = Mouse.TOUCH_MOVE;
			}
			DisposeMultiinput(pTouchData);
			return;
		}
		
	}
	
	//临时变量
	private int inputnum = 0;
	private int touchid = 0;          //记录是哪一笔的ID
	private boolean inputflag = false;
	private boolean rightclick = false;
	private MouseInput downinput = new MouseInput();
	private MouseInput tempInput = new MouseInput();
	
    //推送多点消息
    private void DisposeMultiinput( TouchData pTouchData )
    {
    	int TouchX = 0;
	    int TouchY = 0;
	    
    	double tempx = 0.0f;
	    double tempy = 0.0f;
    	
    	//判断第一个点是否为UP
    	if( pTouchData.TouchType[0] == Mouse.TOUCH_UP )
    	{
    		//发送校准数据
    		//坐标转换
	        tempx = pTouchData.TouchX[0] / 32767.0f * m_LedWidth;
	        tempy = pTouchData.TouchY[0] / 32767.0f * m_LedHeight;
	        
        	//初始化发送数据意图
    		m_sendIntent = new Intent(Common.ACTION_CALIBDATA);
    		m_sendIntent.putExtra("UncalX", tempx);
    		m_sendIntent.putExtra("UncalY", tempy);
    		m_Server.m_Context.sendBroadcast(m_sendIntent);
    		Log.i(Common.TAG, "发送了一个校准数据点");
    	}
    	
    	/**
    	 * 打印输出
    	 * */
    	/*
    	Log.d("TouchDebug", "2当前点个数: " + pTouchData.TouchNum);
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
    	
    	//如果按下的是两个点，则直接不响应右键
    	if( pTouchData.TouchNum >= 2 )
    	{
    		inputflag = false;
    		touchid = -1;
    	}
    	else if( pTouchData.TouchType[0] == Mouse.TOUCH_DOWN )
    	{
    		//开启消息拦截
    		inputflag = true;
    		rightclick = false;
    		inputnum = 0;
    		touchid = -1;
    	}
    	
    	//这里专门负责发送触摸消息
    	for( int i = 0; i < pTouchData.TouchNum; i++ )
    	{
    		tempx = pTouchData.TouchX[i] / 32767.0f * m_LedWidth;
	        tempy = pTouchData.TouchY[i] / 32767.0f * m_LedHeight;
    		
	        //Log.i(Common.TAG, "触摸框坐标:TouchX = " + tempx + " " + "TouchY = " + tempy);
	        
	        //Log.i("TouchDebug", "2第 " + (i + 1) + "个点:" + "TouchX = " + tempx + " " + "TouchY = " + tempy);
	        
    		switch( pTouchData.TouchType[i] )
    		{
    		case Mouse.TOUCH_DOWN:
    		{
    			//初始化队列
    			m_MutilQueue.InitQueue(pTouchData.TouchId[i]);
    			//添加第一个点
    			m_MutilQueue.AddPoint(pTouchData.TouchX[i], 
    					pTouchData.TouchY[i], pTouchData.TouchId[i], tempInput);
    			
    			m_CalibraInfo.xUnCalibrate = tempx;
	            m_CalibraInfo.yUnCalibrate = tempy;
	            
	            //校准坐标
	            m_calibrate.CalibrateAPoint(m_CalibraInfo);
	            
	            tempx = m_CalibraInfo.pCalX;
	            tempy = m_CalibraInfo.pCalY;
    			
	            downinput.TouchX = tempx;
	            downinput.TouchY = tempy;
	            
	            /**
	             * 这里是没有校准的测试
	             * */
		        //tempx = pTouchData.TouchX[i] / 32767.0f * m_ScreenWidth;
		        //tempy = pTouchData.TouchY[i] / 32767.0f * m_ScreenHeight;
	            
	            TouchX = (int)tempx;
	            TouchY = (int)tempy;
	            
	            Mouse.mouse_multi_down(TouchX, TouchY, pTouchData.TouchId[i]);
	            Log.i("TouchDebug", "Down: 4第 " + (i + 1) + "个点:" + "TouchX = " + TouchX + " " + "TouchY = " + TouchY);
    		}
    		break;
    		  
    		case Mouse.TOUCH_MOVE:
    		{
    			if( m_MutilQueue.AddPoint(pTouchData.TouchX[i], 
    									  pTouchData.TouchY[i], 
    									  pTouchData.TouchId[i], 
    									  tempInput) == true && rightclick == false )
    			{
    				//坐标转换
	                tempx = tempInput.TouchX / 32767.0f * m_LedWidth;
	                tempy = tempInput.TouchY / 32767.0f * m_LedHeight;
	                
	                //Log.i("TouchDebug", "3第 " + (i + 1) + "个点:" + "TouchX = " + tempx + " " + "TouchY = " + tempy);
	                
	                m_CalibraInfo.xUnCalibrate = tempx;
		            m_CalibraInfo.yUnCalibrate = tempy;
		            
		            //校准坐标
		            m_calibrate.CalibrateAPoint(m_CalibraInfo);
		            
		            tempx = m_CalibraInfo.pCalX;
		            tempy = m_CalibraInfo.pCalY;
 		             
		            /**
		             * 这里是没有校准的测试
		             * */
			        //tempx = pTouchData.TouchX[i] / 32767.0f * m_ScreenWidth;
			        //tempy = pTouchData.TouchY[i] / 32767.0f * m_ScreenHeight;
		            
		            TouchX = (int)tempx;
		            TouchY = (int)tempy;
		            
		            //判断是否开启了拦截消息
		            if( inputflag == true )  //如果发送右键后 就不再响应move
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
	                     
	                    //符合右键条件
	                    if( inputnum >= 140 )
	                    {
	                    	inputflag = false;
	                        inputnum = 0;
	                        rightclick = true;
	                        touchid = pTouchData.TouchId[i];
	                        
	                        //左键先弹起
	                        Mouse.mouse_multi_up(pTouchData.TouchId[i]);
	                        //m_MutilQueue.DestroyQueue(pTouchData.TouchId[i]);
	                        
	                        //先睡200毫秒
	                        try 
	            			{
	            				sleep(200);
	            			} 
	            			catch (InterruptedException e)
	            			{
	            				e.printStackTrace();
	            			}
	                        
	                        //发送右键
	                        Mouse.mouse_right_key();

	                        Log.i(Common.TAG, "数据层已发送右键");
	                        return;
	                    }
		            }
		            
		            Mouse.mouse_multi_move(TouchX, TouchY, pTouchData.TouchId[i]);
		            Log.i("TouchDebug", "Move: 4第 " + (i + 1) + "个点:" + "TouchX = " + TouchX + " " + "TouchY = " + TouchY);
    			}
    		}
    		break;
    		
    		case Mouse.TOUCH_UP:
    		{
    			//先销毁队列
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
	
	//获取数据
	private void MTParatouchdata( byte[] pData, TouchData pTouch )
	{
		if( pData == null || pTouch == null )
	    {
			Log.e(Common.TAG, "传入参数错误");
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
	         * 测试打印
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
	        
	        Log.i("TouchDebug", "1第 " + (i + 1) + "个点:" + "TouchX = " + Touchx + " " + "TouchY = " + Touchy);
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
    	 * 打印输出
    	 * */
		/*
    	Log.d("TouchDebug", "1当前点个数: " + pTouch.TouchNum);
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
	
	//获得苹果数据
	private boolean MTGetIOSData( byte[] Data, ByteBuffer bBuffer )
	{
		boolean iRet = false;
		int cnt = 0;
		
		do
		{
			if( m_connect == null )
			{
				Log.e(Common.TAG, "m_connect参数错误###!MTSetWordmode");
				break;
			}
			if( m_epIntIn == null || m_epIntOut == null )
			{
				//m_Server.MessageBox("输入端口没有找到###!");
				Log.e(Common.TAG, "输入端口没有找到###!MTSetWordmode");
				break;
			}
			if( bBuffer == null )
			{
				Log.e(Common.TAG, "bBuffer error###!MTSetWordmode");
				break;
			}
			
			iRet = false;
			
			//读一百次
	        cnt = 0;
	        while( cnt < 100 )
	        {
	        	Data[0] = 0;
	        	Data[1] = 0;
	        	iRet = ServerThread.WaitDeviceData(m_connect, m_epIntIn, Data);
	        	if( iRet == false )
				{
	        		Log.e(Common.TAG, "读取设备失败");
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
	            	//Log.i(Common.TAG, "读取通信数据成功!");
	                iRet = true;
	                break;
	            }
	            cnt++;
	        }
			
		}while( false );
		
		
		return iRet;
	}
	
	//切换到苹果系统模式
	private boolean MTSetApplemode()
	{
		int cnt = 0;
		boolean iRet = false;
		byte[] SendBuff = new byte[64];
		
		do
		{
			if( m_connect == null )
			{
				Log.e(Common.TAG, "m_connect参数错误###!MTSetWordmode");
				break;
			}
			if( m_epIntIn == null || m_epIntOut == null )
			{
				//m_Server.MessageBox("输入端口没有找到###!");
				Log.e(Common.TAG, "输入端口没有找到###!MTSetWordmode");
				break;
			}
			
			SendBuff[0] = (byte)0xFF;
		    SendBuff[1] = PACKET_IDEN_SEND_POS;
			
		    //写入设备
			if( ServerThread.WriteDeviceData(m_connect, m_epIntOut, SendBuff) == false )
			{
				//m_Server.MessageBox("写入设备失败了###!");
				Log.e(Common.TAG, "写入设备失败了###!MTSetWordmode");
				break;
			}
			
			//读一百次
	        cnt = 0;
	        while( cnt < 100 )
	        {
	        	if( ServerThread.ReadDeviceData(m_connect, m_epIntIn, SendBuff) == false )
				{
	        		Log.e(Common.TAG, "读取设备失败");
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
			Log.i(Common.TAG, "切换到苹果模式成功");
		}
		
		return iRet;
	}
	
	//切换模式
	private boolean MTSetWorkmode( byte Workmode )
	{
		boolean iRet = false;
		byte[] SendBuff = new byte[64];
		
		do
		{
			if( m_connect == null )
			{
				Log.e(Common.TAG, "m_connect参数错误###!MTSetWordmode");
				break;
			}
			if( m_epIntIn == null || m_epIntOut == null )
			{
				//m_Server.MessageBox("输入端口没有找到###!");
				Log.e(Common.TAG, "输入端口没有找到###!MTSetWordmode");
				break;
			}
			
			SendBuff[0] = (byte) 0xFF & 0;
			SendBuff[1] = (byte) 0xFF & PACKET_IDEN_SET_TOUCHWIN_MODE;
			SendBuff[5] = Workmode;
			
			//写入设备
			if( ServerThread.WriteDeviceData(m_connect, m_epIntOut, SendBuff) == false )
			{
				//m_Server.MessageBox("写入设备失败了###!");
				Log.e(Common.TAG, "写入设备失败了###!MTSetWordmode");
				break;
			}
			
			iRet = true;
			Log.i(Common.TAG,"设置工作模式成功");
		}while( false );
		
		
		return iRet;
	}
	
	//读取配置信息
	private boolean MTGetConfig( ScreenInfo screen )
	{
		boolean iRet = false;
		byte[] buff = new byte[64];
		
		do
		{
			if( m_connect == null )
			{
				Log.e(Common.TAG, "m_connect参数错误###!");
				break;
			}
			if( m_epIntIn == null || m_epIntOut == null )
			{
				//m_Server.MessageBox("输入端口没有找到###!");
				Log.e(Common.TAG, "输入端口没有找到###!");
				break;
			}
			
			buff[0] = (byte) 0xFF & 0;
			buff[1] = (byte) 0xFF & PACKET_IDEN_READ_CONFIG;
			
			//写入设备
			if( ServerThread.WriteDeviceData(m_connect, m_epIntOut, buff) == false )
			{
				//m_Server.MessageBox("写入设备失败了###!");
				Log.e(Common.TAG, "写入设备失败了###!");
				break;
			}
			
			//读取命令
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
					
					Log.i(Common.TAG, "获取配置命令成功!!!");
					break;
				}
			}
			
		}while( false );
		
		//这里是转换
		if( iRet == true )
		{
			/**
			 * 测试
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
			
			//m_Server.MessageBox("成功获得数据!");
			Log.i(Common.TAG, "成功宽高获得数据!");
		}
		
		return iRet;
	}
	
}
