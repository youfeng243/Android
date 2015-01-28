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
	
	//��������
	public static native void SendInput(int x, int y);
	
	//����ID
	public static native void SendTrackId( int TouchId );
	
	//���͵�ǰ��ͬ����Ϣ
	public static native void SendMtSync();
	
	//������ͬ����Ϣ
	public static native void SendSync();
	
	//�����Ҽ�
	public static native void mouse_right_key();
}
