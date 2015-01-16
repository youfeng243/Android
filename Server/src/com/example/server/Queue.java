package com.example.server;

import com.example.server.ThreadMT.MouseInput;

import android.util.Log;

public class Queue 
{
	private static final int QUEUE_MAX = 4;
	
	private int totalnum = 0;
	
	private MouseInput[] m_Queue = null;
	
	public Queue()
	{
		totalnum = 0;
		m_Queue = new MouseInput[QUEUE_MAX + 1];
		for( int i = 0; i < QUEUE_MAX + 1; i++ )
		{
			m_Queue[i] = new MouseInput();
		}
	}
	
	public boolean IsEmpty()
	{
		if( totalnum <= 0 )
	    {
	        return true;
	    }

	    return false;
	}
	
	//清空队列
	public void Clear( )
	{
	    totalnum = 0;
	}
	
	public boolean IsFull( )
	{
	    if( totalnum >= QUEUE_MAX )
	    {
	        return true;
	    }

	    return false;
	}
	
	//出队列
	public void Pop( )
	{
	    if( IsEmpty() == true )
	    {
	        Log.d(Common.TAG, "队列已空 不出队列");
	        return;
	    }

	    totalnum--;
	}
	
	//压入队列
	public void Push( MouseInput addpoint )
	{	 
	    if( IsFull() == true )
	    {
	        Pop();
	    }
	    
	    //Log.d(Common.TAG, "totalnum = " + totalnum);
	    
	    m_Queue[totalnum].TouchType = addpoint.TouchType;
	    m_Queue[totalnum].TouchX    = addpoint.TouchX;
	    m_Queue[totalnum].TouchY    = addpoint.TouchY;

	    //front = (front + 1) % QUEUE_MAX;
	    totalnum++;

	}
	
	//获得队列总数
	public int GetTotalNum( )
	{
	    return totalnum;
	}
	
	//均值计算
	public boolean AddPoint( MouseInput addpoint, MouseInput RetPoint )
	{
	    int i = 0;
	  
	    RetPoint.TouchType = Mouse.TOUCH_MOVE;
	    RetPoint.TouchX = addpoint.TouchX;
	    RetPoint.TouchY = addpoint.TouchY;

	    int count = GetTotalNum();
	    if( count == 0 )
	    {
	        Push(addpoint);
	        return true;
	    }
	    if( count < QUEUE_MAX )
	    {
	        RetPoint.TouchX = m_Queue[count - 1].TouchX + ( addpoint.TouchX - m_Queue[count - 1].TouchX ) / 2;
	        RetPoint.TouchY = m_Queue[count - 1].TouchY + ( addpoint.TouchY - m_Queue[count - 1].TouchY ) / 2;
	        Push(RetPoint);
	        return true;
	    }

	    for( i = 0; i < QUEUE_MAX - 1; i++ )
	    {
	        m_Queue[i].TouchX = m_Queue[i + 1].TouchX;
	        m_Queue[i].TouchY = m_Queue[i + 1].TouchY;
	    }
	    m_Queue[QUEUE_MAX - 1].TouchX = addpoint.TouchX;
	    m_Queue[QUEUE_MAX - 1].TouchY = addpoint.TouchY;
	    m_Queue[QUEUE_MAX - 1].TouchType = Mouse.TOUCH_MOVE;

	    RetPoint.TouchX = m_Queue[0].TouchX * 0.125f + m_Queue[1].TouchX * 0.375f + m_Queue[2].TouchX * 0.375f  + m_Queue[3].TouchX * 0.125f;
	    RetPoint.TouchY = m_Queue[0].TouchY * 0.125f + m_Queue[1].TouchY * 0.375f + m_Queue[2].TouchY * 0.375f  + m_Queue[3].TouchY * 0.125f;

	    return true;
	}
	
}
