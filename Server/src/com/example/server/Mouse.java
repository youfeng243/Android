package com.example.server;

public class Mouse {
	
	static 
	{
        System.loadLibrary("mouse");
    }

	public static final String DEVICE_NAME = "Virtualmouse";
	
	public static final int LEFTKEY_DOWN = 1;
	public static final int LEFTKEY_UP = 0;
	
	//触摸模式
	public static final int TOUCH_DOWN = 0x03;
	public static final int TOUCH_MOVE = 0x05;
	public static final int TOUCH_UP   = 0x04;
	
	/*
	 * 鼠标操作
	 */
	//初始化鼠标
	public static native void InitMouse(int screen_width, int screen_height);

	//释放鼠标
	public static native void ReleaseMouse();
	
	//发送右键点击消息
	public static native void mouse_right_key();
	 
	//发送左键消息
	public static native void mouse_left_key( int value );
	
	//移动鼠标
	public static native void mouse_move( int x, int y );
	
	//多点触摸
	//按下
	public static native void mouse_multi_down( int x, int y, int touchId );
	
	//移动
	public static native void mouse_multi_move( int x, int y, int touchId );
	
	//弹起
	public static native void mouse_multi_up( int touchId );
}
