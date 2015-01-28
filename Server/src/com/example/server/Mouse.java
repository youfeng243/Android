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
	
	//发送坐标
	public static native void SendInput(int x, int y);
	
	//发送ID
	public static native void SendTrackId( int TouchId );
	
	//发送当前点同步信息
	public static native void SendMtSync();
	
	//发送总同步信息
	public static native void SendSync();
	
	//发送右键
	public static native void mouse_right_key();
}
