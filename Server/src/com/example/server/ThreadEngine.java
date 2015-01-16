package com.example.server;

import android.util.Log;

public class ThreadEngine 
{
	ServerThread m_Server = null;
	
	//MTϵ���豸
	ThreadMT m_MTThread = null;
	
	//���캯��
	public ThreadEngine( ServerThread Server )
	{
		m_Server = Server;
		
		//MTϵ���豸
		m_MTThread = new ThreadMT(Server);
	}
	
	//��ʼ���߳�����
	public void Init()
	{
		//���ж����ĸ��豸
		if( m_Server.m_pid == Common.MT_PID && m_Server.m_vid == Common.MT_VID )
		{
			//m_Server.MessageBox("����MT�߳�");
			Log.d(Common.TAG, "��ʼ��MT�߳�");
			//���ﴦ��MTϵ�е��豸
	
			if( m_MTThread != null )
			{
				//�̳߳�ʼ��
				m_MTThread.Init();
			}
		}
		else 
		{
			 //������������豸�߳�
		}
	}
	
	//�߳����
	public boolean BeginServerThread()
	{
		boolean iRet = false;
		
		//���ж����ĸ��豸
		if( m_Server.m_pid == Common.MT_PID && m_Server.m_vid == Common.MT_VID )
		{
			//m_Server.MessageBox("����MT�߳�");
			Log.d(Common.TAG, "����MT�߳�");
			//���ﴦ��MTϵ�е��豸
			iRet = true;
			BeginThreadMT();
		}
		else 
		{
			 //������������豸�߳�
		}
		
		return iRet;
	}
	
	//У׼�豸
	public void Calibrate( 
			double[]   pScreenXBuffer,        //
	        double[]   pScreenYBuffer,        //
	        double[]   pUncalXBuffer,         //
	        double[]   pUncalYBuffer          // 
	        )
	{
		//���ж����ĸ��豸
		if( m_Server.m_pid == Common.MT_PID && m_Server.m_vid == Common.MT_VID )
		{
			//���ﴦ��MTϵ�е��豸
			//iRet = ;
			if( m_MTThread != null )
			{
				m_MTThread.MTCalibrate(pScreenXBuffer, pScreenYBuffer, pUncalXBuffer, pUncalYBuffer);
			}
		}
		else 
		{
			 //������������豸�߳�
		}
		
	}
	
	//�ж��߳��Ƿ���Ȼ������
	public boolean isAlive()
	{
		boolean iRet = false;
		
		//���ж����ĸ��豸
		if( m_Server.m_pid == Common.MT_PID && m_Server.m_vid == Common.MT_VID )
		{
			//���ﴦ��MTϵ�е��豸
			//iRet = ;
			if( m_MTThread != null )
			{
				iRet = m_MTThread.isAlive();
			}
		}
		else 
		{
			 //������������豸�߳�
		}
		
		return iRet;
	}
	
	//�ر��߳�
	public void EndServerThread()
	{
		//���ж����ĸ��豸
		if( m_Server.m_pid == Common.MT_PID && m_Server.m_vid == Common.MT_VID )
		{
			//���ﴦ��MTϵ�е��豸
			EndThreadMT();
		}
		else 
		{
			 //������������豸�߳�
		}
	}
	
	//MT ϵ���豸
	public void BeginThreadMT()
	{
		//�ж��Ƿ�������
		if( m_MTThread != null )
		{
			if( m_MTThread.isAlive() )
			{
				return;
			}
		}
		
		//���������߳��ڴ� ����JAVA����Ҫ�ͷ��ڴ�
		m_MTThread = new ThreadMT(m_Server);
		m_MTThread.Init();
		
		//�߳̿�ʼ
		m_MTThread.MTStart();

	}
	public void EndThreadMT()
	{
		if( m_MTThread == null )
		{
			Log.d(Common.TAG, "MT�߳�ָ��ΪNULL ������ִ��");
			return;
		}
		
		//ֹͣ�߳�
		m_MTThread.MTStop();
		m_MTThread = null;
	}
		
}
