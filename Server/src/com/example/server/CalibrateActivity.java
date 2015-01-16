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
	
	//����У׼��Ϣ
	private Intent m_calibraIntent = null;  
	
	//������Ϣ
	private CalibReceiver m_msgReceiver = null;
	
	//У׼����
	private double[]   pScreenXBuffer = null;        //
	private double[]   pScreenYBuffer = null;        //
	private double[]   pUncalXBuffer  = null;         //
	private double[]   pUncalYBuffer  = null;        //
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		// ��title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // ȫ��
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
      //��Ļ��Ϣ
		DisplayMetrics dm2 = getResources().getDisplayMetrics();
		m_screenwidth = dm2.widthPixels;
		m_screenheight = dm2.heightPixels;
        
        //ʹ�þ��Բ���
        AbsoluteLayout layout = new AbsoluteLayout(this);
        
        //�������
        m_clicktimes = 0;
        m_CaView = new CalibrateView(this);
        
        //���ð�ť
        Button back = new Button(this);
        back.setText("�˳�У׼");
        back.setId(1000);
        back.setWidth(500);
        back.setHeight(100);
        back.setX((m_screenwidth - 500) / 2);
        back.setY(m_screenheight / 3 * 2);
        back.setOnClickListener(this);
        
        layout.addView(m_CaView);
        layout.addView(back);
        
        //���ò���
        setContentView(layout);
        
        //������Ϣ
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
    	
       //��̬ע��㲥������  
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
			//ע���㲥  
	        unregisterReceiver(m_msgReceiver);  
		} catch (Exception e) {
			// TODO: handle exception
			Log.e(Common.TAG, "��û��ע��!");
		}
		
		
		Log.i(Common.TAG, "�˳�У׼������!!!");
	}
	
	 /** 
     * �㲥������ 
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
        			//����������
        			pUncalXBuffer[m_clicktimes] = intent.getDoubleExtra("UncalX", 0);
        			pUncalYBuffer[m_clicktimes] = intent.getDoubleExtra("UncalY", 0);
        			
        			Log.i(Common.TAG, "�յ�У׼����!!!");
        			
	        		m_clicktimes++;
	            	if( m_clicktimes > 3 ) 
	            	{
	            		//���﷢��У׼����
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
	            		
	            		//��������
	            		m_calibraIntent.putExtras(mBundle);
	            		//������ͼ
	            		sendBroadcast(m_calibraIntent);
	            		
	            		Log.i(Common.TAG, "������ȫ��У׼����!!!!!!");
	            		Log.i(Common.TAG, "׼���˳�view");
	            		finish();
	            		return;
	            	}
	            	//ˢͼ
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
			
			
			m_bitmap = Bitmap.createBitmap(m_screenwidth, m_screenheight, Bitmap.Config.ARGB_8888);    //����λͼ���߾ͻ���λͼ���棬��һ����������λͼ��͸�
			
			m_canvas = new Canvas();         
			m_canvas.setBitmap(m_bitmap);   
			m_paint = new Paint(Paint.DITHER_FLAG);
			m_paint.setAntiAlias(true);                //���ÿ���ݣ�һ����Ϊtrue
			m_paint.setColor(Color.BLUE);              //�����ߵ���ɫ
			m_paint.setStrokeCap(Paint.Cap.ROUND);     //�����ߵ�����
			m_paint.setStrokeWidth(2);                //�����ߵĿ��
			
			//��ͼ
			m_canvas.drawLine(80, 80, m_screenwidth - 80, 80, m_paint);
			m_canvas.drawLine(80, 80, 80, m_screenheight - 80, m_paint);
			m_canvas.drawLine(80, m_screenheight - 80, m_screenwidth - 80, m_screenheight - 80, m_paint);
			m_canvas.drawLine(m_screenwidth - 80, 80, m_screenwidth - 80, m_screenheight - 80, m_paint);
			
			//���Ͻ�
			m_canvas.drawLine(120, 0, 120, 120, m_paint);
			m_canvas.drawLine(0, 120, 120, 120, m_paint);
			
			//���Ͻ�
			m_canvas.drawLine(m_screenwidth - 120, 0, m_screenwidth - 120, 120, m_paint);
			m_canvas.drawLine(m_screenwidth - 0, 120, m_screenwidth - 120, 120, m_paint);
			
			//���½�
			m_canvas.drawLine(m_screenwidth - 120, m_screenheight - 0, m_screenwidth - 120, m_screenheight - 120, m_paint);
			m_canvas.drawLine(m_screenwidth - 0, m_screenheight - 120, m_screenwidth - 120, m_screenheight - 120, m_paint);

		    //���½�
			m_canvas.drawLine(120, m_screenheight - 0, 120, m_screenheight - 120, m_paint);
			m_canvas.drawLine(0, m_screenheight - 120, 120, m_screenheight - 120, m_paint);
		}
		
		//������
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
			m_paint.setColor(Color.RED);              //�����ߵ���ɫ
			switch( m_clicktimes )
			{
			case 0:
			{
				c.drawLine(100, 80, 100, 120, m_paint);
				c.drawLine(80,  100, 120, 100, m_paint);
				
				drawRect(90, 90, 110, 110, c);
				
				str = "�������Ͻ�!(ִ�д��� 1)";
			}
			break;
			
			case 1:
			{
				c.drawLine(m_screenwidth - 100, 80,
                        m_screenwidth - 100, 120, m_paint);
		        c.drawLine(m_screenwidth - 120, 100,
		                m_screenwidth - 80, 100, m_paint);
		        
		        drawRect(m_screenwidth - 110, 90, m_screenwidth - 90, 110, c);
		        
		        str = "�������Ͻ�!(ִ�д��� 2)";
			}
			break;
			
			case 2:
			{
				c.drawLine(m_screenwidth - 100, m_screenheight - 120,
                        m_screenwidth - 100, m_screenheight - 80, m_paint);
				c.drawLine(m_screenwidth - 120, m_screenheight - 100,
                        m_screenwidth - 80,  m_screenheight - 100, m_paint);
				
				drawRect(m_screenwidth - 110, m_screenheight - 110, m_screenwidth - 90, m_screenheight - 90, c);
				
				str = "�������½�!(ִ�д��� 3)";
			}
			break;
			
			case 3:
			{
				c.drawLine(100, m_screenheight - 120,
                        100, m_screenheight - 80, m_paint);
				c.drawLine(80,  m_screenheight - 100,
                        120, m_screenheight - 100, m_paint);
				
				drawRect(90, m_screenheight - 110, 110, m_screenheight - 90, c);
				
				str = "�������½�!(ִ�д��� 4)";
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
			
			//��ʾ�û�����˳���ť�˳�
			m_paint.setTextSize(50);
			baseline += fontMetrics.bottom - fontMetrics.top + 10;
			c.drawText("Tip:�����ť�˳�У׼!", targetRect.centerX(), baseline, m_paint);
		}

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch( v.getId() )
		{
		case 1000:
		{
			Log.i(Common.TAG, "����˳���ť��");
			finish();
		}
		break;
		
		default:
			break;
		}
	}
}
