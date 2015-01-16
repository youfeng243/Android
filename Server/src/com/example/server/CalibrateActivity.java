package com.example.server;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsoluteLayout;
import android.widget.Button;

@SuppressWarnings("deprecation")
public class CalibrateActivity extends Activity implements OnClickListener
{
	private int m_clicktimes = 0;
	private CalibrateView m_CaView = null;
	private int m_screenwidth = 0;
	private int m_screenheight = 0;
	
	//发送校准信息
	private Intent m_calibraIntent = null;  
	
	//接收信息
	private CalibReceiver m_msgReceiver = null;
	
	//校准数据
	private double[]   pScreenXBuffer = null;        //
	private double[]   pScreenYBuffer = null;        //
	private double[]   pUncalXBuffer  = null;         //
	private double[]   pUncalYBuffer  = null;        //
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		// 无title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
      //屏幕信息
		DisplayMetrics dm2 = getResources().getDisplayMetrics();
		m_screenwidth = dm2.widthPixels;
		m_screenheight = dm2.heightPixels;
        
        //使用绝对布局
        AbsoluteLayout layout = new AbsoluteLayout(this);
        
        //点击次数
        m_clicktimes = 0;
        m_CaView = new CalibrateView(this);
        
        //设置按钮
        Button back = new Button(this);
        back.setText("退出校准");
        back.setId(1000);
        back.setWidth(500);
        back.setHeight(100);
        back.setX((m_screenwidth - 500) / 2);
        back.setY(m_screenheight / 3 * 2);
        back.setOnClickListener(this);
        
        layout.addView(m_CaView);
        layout.addView(back);
        
        //设置布局
        setContentView(layout);
        
        //数据信息
        pScreenXBuffer = new double[4];
    	pScreenYBuffer = new double[4];
    	pUncalXBuffer  = new double[4];
    	pUncalYBuffer  = new double[4];
        
    	pScreenXBuffer[0] = 100.0f;
    	pScreenXBuffer[1] = m_screenwidth - 100.0f;
    	pScreenXBuffer[2] = m_screenwidth - 100.0f;
    	pScreenXBuffer[3] = 100.0f;

    	pScreenYBuffer[0] = 100.0f;
    	pScreenYBuffer[1] = 100.0f;
    	pScreenYBuffer[2] = m_screenheight - 100.0f;
    	pScreenYBuffer[3] = m_screenheight - 100.0f;
    	
       //动态注册广播接收器  
        m_msgReceiver = new CalibReceiver();  
        IntentFilter intentFilter = new IntentFilter();  
        intentFilter.addAction(Common.ACTION_CALIBDATA);  
        registerReceiver(m_msgReceiver, intentFilter);
        
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		try {
			//注销广播  
	        unregisterReceiver(m_msgReceiver);  
		} catch (Exception e) {
			// TODO: handle exception
			Log.e(Common.TAG, "还没有注册!");
		}
		
		
		Log.i(Common.TAG, "退出校准界面了!!!");
	}
	
	 /** 
     * 广播接收器 
     * @author len 
     * 
     */  
    public class CalibReceiver extends BroadcastReceiver{  
  
        @Override  
        public void onReceive(Context context, Intent intent) {  
            
        	String action = intent.getAction();
        	
        	if( Common.ACTION_CALIBDATA.equals(action) )
        	{
        		if( m_clicktimes >= 0 && m_clicktimes <= 3 )
        		{
        			//这里获得数据
        			pUncalXBuffer[m_clicktimes] = intent.getDoubleExtra("UncalX", 0);
        			pUncalYBuffer[m_clicktimes] = intent.getDoubleExtra("UncalY", 0);
        			
        			Log.i(Common.TAG, "收到校准数据!!!");
        			
	        		m_clicktimes++;
	            	if( m_clicktimes > 3 ) 
	            	{
	            		//这里发送校准数据
	            		Bundle mBundle = new Bundle();
	            		
	            		mBundle.putDouble("ScreenX0", pScreenXBuffer[0]);
	            		mBundle.putDouble("ScreenX1", pScreenXBuffer[1]);
	            		mBundle.putDouble("ScreenX2", pScreenXBuffer[2]);
	            		mBundle.putDouble("ScreenX3", pScreenXBuffer[3]);
	            		
	            		mBundle.putDouble("ScreenY0", pScreenYBuffer[0]);
	            		mBundle.putDouble("ScreenY1", pScreenYBuffer[1]);
	            		mBundle.putDouble("ScreenY2", pScreenYBuffer[2]);
	            		mBundle.putDouble("ScreenY3", pScreenYBuffer[3]);
	            		
	            		mBundle.putDouble("UncalX0", pUncalXBuffer[0]);
	            		mBundle.putDouble("UncalX1", pUncalXBuffer[1]);
	            		mBundle.putDouble("UncalX2", pUncalXBuffer[2]);
	            		mBundle.putDouble("UncalX3", pUncalXBuffer[3]);
	            		
	            		mBundle.putDouble("UncalY0", pUncalYBuffer[0]);
	            		mBundle.putDouble("UncalY1", pUncalYBuffer[1]);
	            		mBundle.putDouble("UncalY2", pUncalYBuffer[2]);
	            		mBundle.putDouble("UncalY3", pUncalYBuffer[3]);
	            		
	            		m_calibraIntent = new Intent(Common.ACTION_CALIBRATION); 
	            		
	            		//发送数据
	            		m_calibraIntent.putExtras(mBundle);
	            		//发送意图
	            		sendBroadcast(m_calibraIntent);
	            		
	            		Log.i(Common.TAG, "发送了全部校准数据!!!!!!");
	            		Log.i(Common.TAG, "准备退出view");
	            		finish();
	            		return;
	            	}
	            	//刷图
	            	m_CaView.invalidate();
        		}
        	}
        	
        }  
          
    }  
	
	public class CalibrateView extends View 
	{
		public Canvas m_canvas = null;
		public Paint m_paint = null;
		private Bitmap m_bitmap = null;
		
		public CalibrateView(Context context) {
			super(context);
			
			
			m_bitmap = Bitmap.createBitmap(m_screenwidth, m_screenheight, Bitmap.Config.ARGB_8888);    //设置位图，线就画在位图上面，第一二个参数是位图宽和高
			
			m_canvas = new Canvas();         
			m_canvas.setBitmap(m_bitmap);   
			m_paint = new Paint(Paint.DITHER_FLAG);
			m_paint.setAntiAlias(true);                //设置抗锯齿，一般设为true
			m_paint.setColor(Color.BLUE);              //设置线的颜色
			m_paint.setStrokeCap(Paint.Cap.ROUND);     //设置线的类型
			m_paint.setStrokeWidth(2);                //设置线的宽度
			
			//绘图
			m_canvas.drawLine(80, 80, m_screenwidth - 80, 80, m_paint);
			m_canvas.drawLine(80, 80, 80, m_screenheight - 80, m_paint);
			m_canvas.drawLine(80, m_screenheight - 80, m_screenwidth - 80, m_screenheight - 80, m_paint);
			m_canvas.drawLine(m_screenwidth - 80, 80, m_screenwidth - 80, m_screenheight - 80, m_paint);
			
			//左上角
			m_canvas.drawLine(120, 0, 120, 120, m_paint);
			m_canvas.drawLine(0, 120, 120, 120, m_paint);
			
			//右上角
			m_canvas.drawLine(m_screenwidth - 120, 0, m_screenwidth - 120, 120, m_paint);
			m_canvas.drawLine(m_screenwidth - 0, 120, m_screenwidth - 120, 120, m_paint);
			
			//右下角
			m_canvas.drawLine(m_screenwidth - 120, m_screenheight - 0, m_screenwidth - 120, m_screenheight - 120, m_paint);
			m_canvas.drawLine(m_screenwidth - 0, m_screenheight - 120, m_screenwidth - 120, m_screenheight - 120, m_paint);

		    //左下角
			m_canvas.drawLine(120, m_screenheight - 0, 120, m_screenheight - 120, m_paint);
			m_canvas.drawLine(0, m_screenheight - 120, 120, m_screenheight - 120, m_paint);
		}
		
		//画矩形
		private void drawRect( float startX, float startY, float stopX, float stopY, Canvas c)
		{
			c.drawLine(startX, startY, stopX, startY, m_paint);
			c.drawLine(startX, startY, startX, stopY, m_paint);
			
			c.drawLine(startX, stopY, stopX, stopY, m_paint);
			c.drawLine(stopX, startY, stopX, stopY, m_paint);
		}
		
		@SuppressLint("DrawAllocation")
		@Override
		public void onDraw(Canvas c) 
		{			    		
			String str = null;
			c.drawBitmap(m_bitmap, 0, 0, null);					
			m_paint.setColor(Color.RED);              //设置线的颜色
			switch( m_clicktimes )
			{
			case 0:
			{
				c.drawLine(100, 80, 100, 120, m_paint);
				c.drawLine(80,  100, 120, 100, m_paint);
				
				drawRect(90, 90, 110, 110, c);
				
				str = "请点击左上角!(执行次数 1)";
			}
			break;
			
			case 1:
			{
				c.drawLine(m_screenwidth - 100, 80,
                        m_screenwidth - 100, 120, m_paint);
		        c.drawLine(m_screenwidth - 120, 100,
		                m_screenwidth - 80, 100, m_paint);
		        
		        drawRect(m_screenwidth - 110, 90, m_screenwidth - 90, 110, c);
		        
		        str = "请点击右上角!(执行次数 2)";
			}
			break;
			
			case 2:
			{
				c.drawLine(m_screenwidth - 100, m_screenheight - 120,
                        m_screenwidth - 100, m_screenheight - 80, m_paint);
				c.drawLine(m_screenwidth - 120, m_screenheight - 100,
                        m_screenwidth - 80,  m_screenheight - 100, m_paint);
				
				drawRect(m_screenwidth - 110, m_screenheight - 110, m_screenwidth - 90, m_screenheight - 90, c);
				
				str = "请点击右下角!(执行次数 3)";
			}
			break;
			
			case 3:
			{
				c.drawLine(100, m_screenheight - 120,
                        100, m_screenheight - 80, m_paint);
				c.drawLine(80,  m_screenheight - 100,
                        120, m_screenheight - 100, m_paint);
				
				drawRect(90, m_screenheight - 110, 110, m_screenheight - 90, c);
				
				str = "请点击左下角!(执行次数 4)";
			}
			break;
			
			default:
				break;
			}
			
			Rect targetRect = new Rect(0, 0, m_screenwidth, m_screenheight);
			m_paint.setTextSize(80);
			FontMetricsInt fontMetrics = m_paint.getFontMetricsInt();
			int baseline = targetRect.top + (targetRect.bottom - targetRect.top - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top;
			m_paint.setTextAlign(Paint.Align.CENTER);
			c.drawText(str, targetRect.centerX(), baseline, m_paint);
			
			//提示用户点击退出按钮退出
			m_paint.setTextSize(50);
			baseline += fontMetrics.bottom - fontMetrics.top + 10;
			c.drawText("Tip:点击按钮退出校准!", targetRect.centerX(), baseline, m_paint);
		}

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch( v.getId() )
		{
		case 1000:
		{
			Log.i(Common.TAG, "点击退出按钮了");
			finish();
		}
		break;
		
		default:
			break;
		}
	}
}
