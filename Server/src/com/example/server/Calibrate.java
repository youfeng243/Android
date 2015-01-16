package com.example.server;

import com.example.server.ThreadMT.CalibraInfo;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class Calibrate {
	
	private static final String PREFS_NAME = "Calibrate";
	
	//校准信息
    private double m_led_x[] = new double[4];
    private double m_led_y[] = new double[4];

    //校准矩阵参数
    private double m_H[] = new double[8];

    //判断是否已经校准过
    private boolean m_CalibraFlag = false;
    
    //上下文
    private Context m_context = null;
    
 // 获取SharedPreferences对象  
    private static SharedPreferences m_sp;  
    private static Editor m_editor; 
    
    public Calibrate( Context context )
    {
    	m_CalibraFlag = false;
    	m_context = context;
    }
    
  //校准初始化
    public void Init( double[] screen_x, double[] screen_y )
    {
        if( screen_x == null || screen_y == null )
        {
            Log.e(Common.TAG, "传入参数错误");
            return;
        }

        //加载数据
        LoadFile();

        //这里进行校准
        SetCalibration( screen_x, screen_y, m_led_x, m_led_y );
    }

    //call by calibration ap  4点
    public void SetCalibration
    (
        double[]   pScreenXBuffer,        //
        double[]   pScreenYBuffer,        //
        double[]   pUncalXBuffer,         //
        double[]   pUncalYBuffer          //
        )
    {
        int i = 0;
        double x[][] = new double[8][8];
        double c[] = new double[8];

        m_led_x[0] = pUncalXBuffer[0];
        m_led_x[1] = pUncalXBuffer[1];
        m_led_x[2] = pUncalXBuffer[2];
        m_led_x[3] = pUncalXBuffer[3];

        m_led_y[0] = pUncalYBuffer[0];
        m_led_y[1] = pUncalYBuffer[1];
        m_led_y[2] = pUncalYBuffer[2];
        m_led_y[3] = pUncalYBuffer[3];

        m_CalibraFlag = false;

        for( i = 0; i < 4; i++)
        {
            x[i][0] = pUncalXBuffer[i];
            x[i][1] = pUncalYBuffer[i];
            x[i][2] = 1;
            x[i][3] = 0;
            x[i][4] = 0;
            x[i][5] = 0;
            x[i][6] = (-1) * pUncalXBuffer[i] * pScreenXBuffer[i];
            x[i][7] = (-1) * pUncalYBuffer[i] * pScreenXBuffer[i];
        }
        for( i = 4; i < 8; i++)
        {
            x[i][0] = 0;
            x[i][1] = 0;
            x[i][2] = 0;
            x[i][3] = pUncalXBuffer[i - 4];
            x[i][4] = pUncalYBuffer[i - 4];
            x[i][5] = 1;
            x[i][6] = (-1) * pUncalXBuffer[i - 4] * pScreenYBuffer[i - 4];
            x[i][7] = (-1) * pUncalYBuffer[i - 4] * pScreenYBuffer[i - 4];
        }
        
        //调试打印
        /*
        for( i = 0; i < 8; i++ )
        {
        	for(int j = 0; j < 8; j++ )
        	{
        		Log.d(Common.TAG, "x[" + i + "][" + j + "] = " + x[i][j]);
        	}
        }
        Log.d(Common.TAG, " ");
        */
        
        for( i = 0; i < 8; i++)
        { 
            if( i < 4 )
            {
                c[i] = pScreenXBuffer[i];
            }
            else
            {
                c[i] = pScreenYBuffer[ i - 4 ];
            }
            //Log.d(Common.TAG, "c[" + i + "] = " + c[i]);
        }
        
        
        CaculChange(x, c);

        m_CalibraFlag = true;


        //校准后保存文件
        SaveFile();

        Log.i(Common.TAG, "校准完成!");
    }
    
    public void  CaculChange(double[][] x, double[] c)
    {
        int i = 0;
        int j = 0;
        int k = 0;
        int M = 8;
        int N = 2 * 8;
        double temp = 0.0;
        double a[][] = new double[8][16];
        double r[][] = new double[8][8];

        for( i = 0; i < M; i++ )
        {
            for( j = 0; j < M; j++ )
            {
                a[i][j] = x[i][j];  //赋值
                //Log.d(Common.TAG, "x[" + i + "][" + j + "] = " + x[i][j]);
            }
        }
        for( i = 0; i < M; i++ )
        {
            for( j = M; j < N; j++ )  //扩展 E
            {
                if(i == ( j - M ))
                {
                    a[i][j] = 1;
                }
                else
                {
                    a[i][j] = 0;
                }
            }
        }
        
        //打印输出
        /*
        for( i = 0; i < M; i++ )
        {
        	for( j = 0; j < N; j++ )
        	{
        		Log.d(Common.TAG, "a[" + i + "][" + j + "] = " + a[i][j]);
        	}
        }
        */
        ///---------------------------下面进行求逆运算-----------------
        for( i = 0; i < M; i++ )
        {
            if( a[i][i] == 0 )
            {
                for( k = i; k < M; k++ )
                {
                    if(  a[k][i] != 0 )
                    {
                        for( j = 0; j < N; j++)
                        {
                            temp = a[i][j];
                            a[i][j] = a[k][j];
                            a[k][j] = temp;
                        }
                        break;
                    }
                }
            }
            for( j = N - 1; j >= i; j-- )
            {
            	//Log.d(Common.TAG, "before a[" + i + "][" + j + "] = " + a[i][j]);
            	//Log.d(Common.TAG, "before a[" + i + "][" + i + "] = " + a[i][i]);
            	if( Math.abs(a[i][i]) <= 0.001 )
            	{
            		Log.e(Common.TAG, "无穷大");
            	}
            	
                a[i][j] /= a[i][i];
                
                //Log.i(Common.TAG, "after a[" + i + "][" + j + "] = " + a[i][j]);
            }

            for( k = 0; k < M; k++)
            {
                if( k != i )
                {
                    temp = a[k][i];
                    for( j = 0; j < N; j++ )
                    {
                        a[k][j] -= temp * a[i][j];
                    }
                }
            }
        }

        ///-------------------------导出a逆结果----------------------
        for( i = 0; i < M; i++)
        {
            for( j = M; j < N; j++ )
            {
                r[i][j - M] = a[i][j];
            }
        }

        // a * h = c
        // h = a \ c
        // h = a逆(r) * c
        //计划透视矩阵参数H H = r * c
        for( i = 0; i < 8; i++)
        {
            m_H[i] = 0.0f;
            for( j = 0; j < 8; j++ )
            {
                m_H[i] += (r[i][j] * c[j]);
                
                //Log.d(Common.TAG, "r[" + i + "][" + j + "] = " + r[i][j]);
                //Log.d(Common.TAG, "c[" + j + "] = " + c[j]);
            }
            Log.i(Common.TAG, "m_H[" + i + "] = " + m_H[i]);
        }
    } 
 
    //坐标转换 这个函数需要改
    public void CalibrateAPoint( CalibraInfo m_calibraInfo )
    {
        do
        {
            if ( m_CalibraFlag == false )
            {
            	m_calibraInfo.pCalX = m_calibraInfo.xUnCalibrate;
            	m_calibraInfo.pCalY = m_calibraInfo.yUnCalibrate;
                break;
            }

            m_calibraInfo.pCalX = (m_H[0] * m_calibraInfo.xUnCalibrate + m_H[1] * m_calibraInfo.yUnCalibrate + m_H[2]) /
                    (m_H[6] * m_calibraInfo.xUnCalibrate + m_H[7] * m_calibraInfo.yUnCalibrate + 1);
            m_calibraInfo.pCalY = (m_H[3] * m_calibraInfo.xUnCalibrate + m_H[4] * m_calibraInfo.yUnCalibrate + m_H[5]) /
                    (m_H[6] * m_calibraInfo.xUnCalibrate + m_H[7] * m_calibraInfo.yUnCalibrate + 1);

        }while( false );


        //dbgprintf("原始坐标:");
        //dbgprintf("xUnCalibrate = %f yUnCalibrate = %f", xUnCalibrate, yUnCalibrate);
        //dbgprintf("转换坐标:");
        //dbgprintf("*pCalX = %f *pCalY = %f", *pCalX, *pCalY);

    }

    //保存校准信息
    public void SaveFile( )
    {
        //判断是否已经校准过
        if( m_CalibraFlag == false )
        {
            Log.d(Common.TAG, "还没有进行过校准");
            return;
        }

        try 
        {  
	        m_sp = m_context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	        m_editor = m_sp.edit();
	        
	        
	        m_editor.putFloat("id_x_0", (float)m_led_x[0]);
	        Log.i(Common.TAG, "m_led_x[0] = " + m_led_x[0]);
	        m_editor.putFloat("id_x_1", (float)m_led_x[1]);
	        Log.i(Common.TAG, "m_led_x[1] = " + m_led_x[1]);
	        m_editor.putFloat("id_x_2", (float)m_led_x[2]);
	        Log.i(Common.TAG, "m_led_x[2] = " + m_led_x[2]);
	        m_editor.putFloat("id_x_3", (float)m_led_x[3]);
	        Log.i(Common.TAG, "m_led_x[3] = " + m_led_x[3]);
	        
	        m_editor.putFloat("id_y_0", (float)m_led_y[0]);
	        Log.i(Common.TAG, "m_led_y[0] = " + m_led_y[0]);
	        m_editor.putFloat("id_y_1", (float)m_led_y[1]);
	        Log.i(Common.TAG, "m_led_y[1] = " + m_led_y[1]);
	        m_editor.putFloat("id_y_2", (float)m_led_y[2]);
	        Log.i(Common.TAG, "m_led_y[2] = " + m_led_y[2]);
	        m_editor.putFloat("id_y_3", (float)m_led_y[3]);
	        Log.i(Common.TAG, "m_led_y[3] = " + m_led_y[3]);
	        
	        //提交
	        m_editor.commit();
        } 
        catch (Exception e) 
        {  
            Log.e(Common.TAG, "-------------文件异常-------------");  
            e.printStackTrace();  
        }
    }
 

    //加载校准信息
    public void LoadFile( )
    {
    	try 
        { 
	    	m_sp = m_context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	    	
	    	m_led_x[0] = m_sp.getFloat("id_x_0", 0.0f);
	    	Log.i(Common.TAG, "m_led_x[0] = " + m_led_x[0]);
	    	m_led_x[1] = m_sp.getFloat("id_x_1", 155.0f);
	    	Log.i(Common.TAG, "m_led_x[1] = " + m_led_x[1]);
	    	m_led_x[2] = m_sp.getFloat("id_x_2", 155.0f);
	    	Log.i(Common.TAG, "m_led_x[2] = " + m_led_x[2]);
	    	m_led_x[3] = m_sp.getFloat("id_x_3", 0.0f);
	    	Log.i(Common.TAG, "m_led_x[3] = " + m_led_x[3]);
	    	
	    	m_led_y[0] = m_sp.getFloat("id_y_0", 0.0f);
	    	Log.i(Common.TAG, "m_led_y[0] = " + m_led_y[0]);
	    	m_led_y[1] = m_sp.getFloat("id_y_1", 0.0f);
	    	Log.i(Common.TAG, "m_led_y[1] = " + m_led_y[1]);
	    	m_led_y[2] = m_sp.getFloat("id_y_2", 87.0f);
	    	Log.i(Common.TAG, "m_led_y[2] = " + m_led_y[2]);
	    	m_led_y[3] = m_sp.getFloat("id_y_3", 87.0f);
	    	Log.i(Common.TAG, "m_led_y[3] = " + m_led_y[3]);
        } 
        catch (Exception e) 
        {  
            Log.e(Common.TAG, "-------------文件异常-------------");  
            e.printStackTrace();  
        }
    }
    
}
