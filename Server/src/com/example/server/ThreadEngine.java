package com.example.server;

import android.util.Log;

public class ThreadEngine 
{
	ServerThread m_Server = null;
	
	//MT系列设备
	ThreadMT m_MTThread = null;
	
	//构造函数
	public ThreadEngine( ServerThread Server )
	{
		m_Server = Server;
		
		//MT系列设备
		m_MTThread = new ThreadMT(Server);
	}
	
	//初始化线程引擎
	public void Init()
	{
		//先判断是哪个设备
		if( m_Server.m_pid == Common.MT_PID && m_Server.m_vid == Common.MT_VID )
		{
			//m_Server.MessageBox("开启MT线程");
			Log.d(Common.TAG, "初始化MT线程");
			//这里处理MT系列的设备
	
			if( m_MTThread != null )
			{
				//线程初始化
				m_MTThread.Init();
			}
		}
		else 
		{
			 //这里添加其他设备线程
		}
	}
	
	//线程相关
	public boolean BeginServerThread()
	{
		boolean iRet = false;
		
		//先判断是哪个设备
		if( m_Server.m_pid == Common.MT_PID && m_Server.m_vid == Common.MT_VID )
		{
			//m_Server.MessageBox("开启MT线程");
			Log.d(Common.TAG, "开启MT线程");
			//这里处理MT系列的设备
			iRet = true;
			BeginThreadMT();
		}
		else 
		{
			 //这里添加其他设备线程
		}
		
		return iRet;
	}
	
	//校准设备
	public void Calibrate( 
			double[]   pScreenXBuffer,        //
	        double[]   pScreenYBuffer,        //
	        double[]   pUncalXBuffer,         //
	        double[]   pUncalYBuffer          // 
	        )
	{
		//先判断是哪个设备
		if( m_Server.m_pid == Common.MT_PID && m_Server.m_vid == Common.MT_VID )
		{
			//这里处理MT系列的设备
			//iRet = ;
			if( m_MTThread != null )
			{
				m_MTThread.MTCalibrate(pScreenXBuffer, pScreenYBuffer, pUncalXBuffer, pUncalYBuffer);
			}
		}
		else 
		{
			 //这里添加其他设备线程
		}
		
	}
	
	//判断线程是否仍然在运行
	public boolean isAlive()
	{
		boolean iRet = false;
		
		//先判断是哪个设备
		if( m_Server.m_pid == Common.MT_PID && m_Server.m_vid == Common.MT_VID )
		{
			//这里处理MT系列的设备
			//iRet = ;
			if( m_MTThread != null )
			{
				iRet = m_MTThread.isAlive();
			}
		}
		else 
		{
			 //这里添加其他设备线程
		}
		
		return iRet;
	}
	
	//关闭线程
	public void EndServerThread()
	{
		//先判断是哪个设备
		if( m_Server.m_pid == Common.MT_PID && m_Server.m_vid == Common.MT_VID )
		{
			//这里处理MT系列的设备
			EndThreadMT();
		}
		else 
		{
			 //这里添加其他设备线程
		}
	}
	
	//MT 系列设备
	public void BeginThreadMT()
	{
		//判断是否还在运行
		if( m_MTThread != null )
		{
			if( m_MTThread.isAlive() )
			{
				return;
			}
		}
		
		//重新申请线程内存 反正JAVA不需要释放内存
		m_MTThread = new ThreadMT(m_Server);
		m_MTThread.Init();
		
		//线程开始
		m_MTThread.MTStart();

	}
	public void EndThreadMT()
	{
		if( m_MTThread == null )
		{
			Log.d(Common.TAG, "MT线程指针为NULL 不往下执行");
			return;
		}
		
		//停止线程
		m_MTThread.MTStop();
		m_MTThread = null;
	}
		
}
