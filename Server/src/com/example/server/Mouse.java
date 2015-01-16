package com.example.server;

public class Mouse {
	
	static 
	{
        System.loadLibrary("mouse");
    }

	public static final String DEVICE_NAME = "Virtualmouse";
	
	public static final int LEFTKEY_DOWN = 1;
	public static final int LEFTKEY_UP = 0;
	
	//����ģʽ
	public static final int TOUCH_DOWN = 0x03;
	public static final int TOUCH_MOVE = 0x05;
	public static final int TOUCH_UP   = 0x04;
	
	/*
	 * ������
	 */
	//��ʼ�����
	public static native void InitMouse(int screen_width, int screen_height);

	//�ͷ����
	public static native void ReleaseMouse();
	
	//�����Ҽ������Ϣ
	public static native void mouse_right_key();
	 
	//���������Ϣ
	public static native void mouse_left_key( int value );
	
	//�ƶ����
	public static native void mouse_move( int x, int y );
	
	//��㴥��
	//����
	public static native void mouse_multi_down( int x, int y, int touchId );
	
	//�ƶ�
	public static native void mouse_multi_move( int x, int y, int touchId );
	
	//����
	public static native void mouse_multi_up( int touchId );
}
