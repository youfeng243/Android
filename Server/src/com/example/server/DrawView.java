package com.example.server;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

public class DrawView extends View {

	public Canvas canvas;
	public Paint p;
	private Bitmap bitmap;
	float x,y;
	int bgColor;
	
	public DrawView(Context context) {
		super(context);
		
		//��Ļ��Ϣ
		DisplayMetrics dm2 = context.getResources().getDisplayMetrics();
		//m_ScreenWidth = dm2.widthPixels;
		//m_ScreenHeight = dm2.heightPixels;
		//int[]  colors={Color.WHITE,Color.BLACK,Color.BLUE};
		bitmap = Bitmap.createBitmap(dm2.widthPixels, dm2.heightPixels, Bitmap.Config.ARGB_8888);    //����λͼ���߾ͻ���λͼ���棬��һ����������λͼ��͸�
		//bitmap.
		canvas=new Canvas();         
		canvas.setBitmap(bitmap);       
		p = new Paint(Paint.DITHER_FLAG);
		p.setAntiAlias(true);                //���ÿ���ݣ�һ����Ϊtrue
		p.setColor(Color.RED);              //�����ߵ���ɫ
		p.setStrokeCap(Paint.Cap.ROUND);     //�����ߵ�����
		p.setStrokeWidth(8);                //�����ߵĿ��
		
		//canvas.drawLine(0, 100, 100, 100, p);
		//canvas.drawLine(100, 0, 100, 100, p);
	}
	
	//�����¼�
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		if (event.getAction() == MotionEvent.ACTION_MOVE) {    //�϶���Ļ
			canvas.drawLine(x, y, event.getX(), event.getY(), p);   //���ߣ�x��y���ϴε����꣬event.getX(), event.getY()�ǵ�ǰ����
			invalidate();
		}

		if (event.getAction() == MotionEvent.ACTION_DOWN) {    //������Ļ
			x = event.getX();				
			y = event.getY();
			canvas.drawPoint(x, y, p);                //����
			invalidate();
		}
		if (event.getAction() == MotionEvent.ACTION_UP) {    //�ɿ���Ļ
		
		}
		x = event.getX();   //��¼����
		y = event.getY();
		return true;
	}
	
	@Override
	public void onDraw(Canvas c) {			    		
		c.drawBitmap(bitmap, 0, 0, null);	      
	}

}
