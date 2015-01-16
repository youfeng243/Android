package com.example.server;

import android.app.Activity;

public class Common extends Activity
{
	public static final String TAG = "ServerDebug";
	public static final String HIDSERVICE = "com.example.server.HidService";
	
	//У׼����
	public static final String ACTION_CALIBRATION = "com.example.server.CALIBRATE";
	
	//У׼������Ϣ
	public static final String ACTION_CALIBDATA = "com.example.server.RECEIVERDATA";
	
	//�����ڽ��շ������Ϣ
	public static final String ACTION_RECIVEHIDSERVER = "com.example.server.RECIVEHIDSERVER";
	
	//�豸��Ϣ
	public static final int MT_PID = 0xFF01;
	public static final int MT_VID = 0x04D8;
	
	/*
	public static class CalibrationInfo
	{
		double[]   pScreenXBuffer = null;        //
        double[]   pScreenYBuffer = null;        //
        double[]   pUncalXBuffer  = null;         //
        double[]   pUncalYBuffer  = null;        //
        CalibrationInfo()
        {
        	pScreenXBuffer = new double[4];
        	pScreenYBuffer = new double[4];
        	pUncalXBuffer  = new double[4];
        	pUncalYBuffer  = new double[4];
        }
	}
	*/
	
	
	
	
	//�����Ի��� 
	/*
	static public void MessageBox( CharSequence message )
	{
		Builder builder = new Builder(getApplicationContext());
				builder.setTitle("ServerDebug");
				builder.setMessage(message);
				builder.setNegativeButton("OK", null);
				Dialog dialog=builder.create();
				dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
				dialog.show();
	}
	*/
	
	 /** 
     * byte������ȡint��ֵ��������������(��λ��ǰ����λ�ں�)��˳�򣬺ͺ�intToBytes��������ʹ��
     *  
     * @param src 
     *            byte���� 
     * @param offset 
     *            ������ĵ�offsetλ��ʼ 
     * @return int��ֵ 
     */  
	public static int bytesToInt(byte[] src) 
	{
		int value;	
		value = (int) ((src[0] & 0xFF) 
				| ((src[1] & 0xFF)<<8) 
				| ((src[2] & 0xFF)<<16) 
				| ((src[3] & 0xFF)<<24));
		return value;
	}
	
	 /** 
     * byte������ȡint��ֵ��������������(��λ�ں󣬸�λ��ǰ)��˳�򡣺�intToBytes2��������ʹ��
     */
	public static int bytesToInt2(byte[] src) {
		int value;	
		value = (int) ( ((src[0] & 0xFF)<<24)
				|((src[1] & 0xFF)<<16)
				|((src[2] & 0xFF)<<8)
				|(src[3] & 0xFF));
		return value;
	}
	
}
