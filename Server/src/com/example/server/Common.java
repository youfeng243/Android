package com.example.server;

import android.app.Activity;

public class Common extends Activity
{
	public static final String TAG = "ServerDebug";
	public static final String HIDSERVICE = "com.example.server.HidService";
	
	//校准操作
	public static final String ACTION_CALIBRATION = "com.example.server.CALIBRATE";
	
	//校准数据信息
	public static final String ACTION_CALIBDATA = "com.example.server.RECEIVERDATA";
	
	//主窗口接收服务的消息
	public static final String ACTION_RECIVEHIDSERVER = "com.example.server.RECIVEHIDSERVER";
	
	//设备信息
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
	
	
	
	
	//弹出对话框 
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
     * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和和intToBytes（）配套使用
     *  
     * @param src 
     *            byte数组 
     * @param offset 
     *            从数组的第offset位开始 
     * @return int数值 
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
     * byte数组中取int数值，本方法适用于(低位在后，高位在前)的顺序。和intToBytes2（）配套使用
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
