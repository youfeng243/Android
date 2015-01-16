package com.example.server;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener 
{
	//画板跟校准按钮
	private Button m_CalibrateBtn = null;
	private Button m_DrawBtn = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		// 无title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
		setContentView(R.layout.activity_main);
		
		 
        //判断服务是否存在 不存在则开启服务
        StartHidServer();
        
        //接收服务消息
        IntentFilter filter = new IntentFilter();
        filter.addAction(Common.ACTION_RECIVEHIDSERVER);
        registerReceiver(mServerReceiver, filter);
        
        
        //这里是UI处理
        m_CalibrateBtn = (Button)findViewById(R.id.Calibrate);
        m_DrawBtn = (Button)findViewById(R.id.Draw);
        
        //设置接口
        m_CalibrateBtn.setOnClickListener(this);
        m_DrawBtn.setOnClickListener(this);
	}
	
	//启动服务
	public void StartHidServer()
	{
		//先判断服务是否已经运行
		if( isServiceRunning( Common.HIDSERVICE ) == false )
		{
			Log.d(Common.TAG, "服务还没有运行,启动服务!");
			
			//MessageBox("服务还未运行，启动开启服务");
			//启动服务
			startService(new Intent(this, HidService.class));
		}
		else
		{
			//MessageBox("服务已经开启!");
			Log.d(Common.TAG, "服务已经开启!"); 
		}
	}
	
	//判断服务是否已经运行
	public boolean isServiceRunning( String className ) 
	{
		ActivityManager manager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) 
	    {
	    	Log.d(Common.TAG, service.service.getClassName());  
	        if (className.equals(service.service.getClassName())) 
	        {
	        	Log.d(Common.TAG, "Find Service:" + service.service.getClassName()); 
	            return true;
	        }
	    }
	    return false;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) 
		{   
		
		case R.id.Calibrate:
		{
			Log.d(Common.TAG, "点击校准了");
			//这里启动画板
			Intent intent = new Intent(MainActivity.this, CalibrateActivity.class);
			startActivity(intent);
		}
		break;
		
		case R.id.Draw:
		{
			Log.d(Common.TAG, "点击画板了");
			
			//这里启动画板
			Intent intent = new Intent(MainActivity.this, DrawActivity.class);
			startActivity(intent);
		}
		break;
		
        default:
            break;
		}  
	}
	
	protected void onDestroy() {
		Log.d(Common.TAG, "主窗口注销了");
		super.onDestroy();
		try {
			//取消注册检测设备
			unregisterReceiver(mServerReceiver);
		} catch (Exception e) {
			// TODO: handle exception
			Log.e(Common.TAG, "还没有进行注册");
		}
		
	};
	
	//用于接收底层服务的消息
	BroadcastReceiver mServerReceiver = new BroadcastReceiver() 
    {
        public void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();
            if( action.equals(Common.ACTION_RECIVEHIDSERVER) )
            {
            	Log.i(Common.TAG, "退出主窗口");
            	finish();
            }
        }
    };
	
}
