package com.example.server;

import android.util.Log;

import com.example.server.ThreadMT.MouseInput;

public class MutilQueue 
{
	private static final int QUEUE_MAX_POINT = 4;
	private static final int QUEUE_NUM = 20;
	
	private class Queue
	{
		private int m_totalnum = 0;          //队列个数
		private int m_id = 0;                //队列id
		private boolean m_dirty = false;     //判断是否正在被使用  false 没有 true 有          
		
		public class PointInfo
		{
		    public double TouchX;
		    public double TouchY;
		    
		    public PointInfo()
		    {
		    	TouchX = 0;
		    	TouchY = 0;
		    }
		}
		public PointInfo[] m_Queue = null;
		
		public Queue( int id )
		{
			m_id = id;
			m_totalnum = 0;
			m_Queue = new PointInfo[QUEUE_MAX_POINT + 1];
			for( int i = 0; i < QUEUE_MAX_POINT + 1; i++ )
			{
				m_Queue[i] = new PointInfo();
			}
			m_dirty = false;
		}
		
		//获得ID
		public int getid()
		{
			return m_id;
		}
		
		//初始化
		public void init( int id )
		{
			m_id = id;
			m_totalnum = 0;
			m_dirty = true;
		}
		
		//销毁队列
		public void destroy()
		{
			m_dirty = false;
			m_id = -1;
		}
		
		//判断当前队列是否正在被使用
		public boolean isused()
		{
			return m_dirty;
		}
		
		//压入
		public void push( double x, double y )
		{
			m_Queue[m_totalnum].TouchX = x;
			m_Queue[m_totalnum].TouchY = y;
			m_totalnum++;
		}
		
		//获得总的数目
		public int gettotalnum()
		{
			return m_totalnum;
		}
		
	}
	
	//创建四个队列
	private Queue[] m_MutilQueue = null;
	
	public MutilQueue()
	{	
		//初始化队列信息
		m_MutilQueue = new Queue[QUEUE_NUM];
		for( int i = 0; i < QUEUE_NUM; i++ )
		{
			m_MutilQueue[i] = new Queue(-1);
		}
		
	}
	
	//初始化一个队列
	public void InitQueue( int id )
	{
		for( int i = 0; i < QUEUE_NUM; i++ )
		{
			if( m_MutilQueue[i].isused() == false )
			{
				Log.e(Common.TAG, "创建队列! id = " + id);
				m_MutilQueue[i].init(id);
				break;
			}
		}
	}
	
	//销毁一个队列
	public void DestroyQueue( int id )
	{
		for( int i = 0; i < QUEUE_NUM; i++ )
		{
			if( m_MutilQueue[i].isused() == true )
			{
				if( m_MutilQueue[i].getid() == id )
				{
					m_MutilQueue[i].destroy();
					Log.e(Common.TAG, "销毁队列! id = " + id);
					break;
				}
			}
		}
	}
	
	//压入队列
	private void PushByIndex( double x, double y, int index )
	{
		if( index >= 0 && index < QUEUE_NUM )
		{
			m_MutilQueue[index].push(x, y);
		}
		else
		{
			Log.e(Common.TAG, "压入队列的索引错误");
		}
	}
	
	//获得总点数
	private int GetTotalNumByIndex( int index )
	{
		if( index >= 0 && index < QUEUE_NUM )
		{
			return m_MutilQueue[index].gettotalnum();
		}
		return 0;
	}
	
	//获得队列的索引
	private int GetIndex( int id )
	{
		for( int i = 0; i < QUEUE_NUM; i++ )
		{
			if( m_MutilQueue[i].isused() == true )
			{
				if( m_MutilQueue[i].getid() == id )
				{
					return i;
				}
			}
		}
		return -1;
	}
	
	
	//往队列中压入一个点
	public boolean AddPoint( double x, double y, int id, MouseInput RetPoint )
	{
		RetPoint.TouchX = x;
		RetPoint.TouchY = y;
		
		//获得索引
		int index = GetIndex(id);
		if( index == -1 )
		{
			Log.e(Common.TAG, "队列还未初始化");
			return false;
		}
		
		int count = GetTotalNumByIndex(index);
		if( count == 0 )
		{
			PushByIndex(x, y, index);
			return true;
		}
		
		if( count < QUEUE_MAX_POINT )
	    {
	        RetPoint.TouchX = m_MutilQueue[index].m_Queue[count - 1].TouchX + 
	        		( x - m_MutilQueue[index].m_Queue[count - 1].TouchX ) / 2;
	        RetPoint.TouchY = m_MutilQueue[index].m_Queue[count - 1].TouchY + 
	        		( y - m_MutilQueue[index].m_Queue[count - 1].TouchY ) / 2;
	        PushByIndex(RetPoint.TouchX, RetPoint.TouchY, index);
	        return true;
	    }
		
		for( int i = 0; i < QUEUE_MAX_POINT - 1; i++ )
		{
			m_MutilQueue[index].m_Queue[i].TouchX = m_MutilQueue[index].m_Queue[i + 1].TouchX;
			m_MutilQueue[index].m_Queue[i].TouchY = m_MutilQueue[index].m_Queue[i + 1].TouchY;
		}
		
		m_MutilQueue[index].m_Queue[QUEUE_MAX_POINT - 1].TouchX = x;
		m_MutilQueue[index].m_Queue[QUEUE_MAX_POINT - 1].TouchY = y;


	    RetPoint.TouchX = m_MutilQueue[index].m_Queue[0].TouchX * 0.125f + 
	    		m_MutilQueue[index].m_Queue[1].TouchX * 0.375f + 
	    		m_MutilQueue[index].m_Queue[2].TouchX * 0.375f  + 
	    		m_MutilQueue[index].m_Queue[3].TouchX * 0.125f;
	    RetPoint.TouchY = m_MutilQueue[index].m_Queue[0].TouchY * 0.125f + 
	    		m_MutilQueue[index].m_Queue[1].TouchY * 0.375f + 
	    		m_MutilQueue[index].m_Queue[2].TouchY * 0.375f + 
	    		m_MutilQueue[index].m_Queue[3].TouchY * 0.125f;

		
		return true;
	}
	
}
