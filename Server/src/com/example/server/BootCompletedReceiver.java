package com.example.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

//开机启动读数据后台服务类
public class BootCompletedReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) 
	    { 
			 //Intent newIntent = new Intent(context, MainActivity.class); 
		     Intent newIntent = new Intent(context, HidService.class); 
		     newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  //注意，必须添加这个标记，否则启动会失败 
		     //context.startActivity(newIntent);    
		     context.startService(newIntent);
	    }    
	}

}
