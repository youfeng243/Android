#include "com_example_server_Mouse.h"

static int gHandle = -1;

/*
 * Class:     com_example_server_Mouse
 * Method:    InitMouse
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_example_server_Mouse_InitMouse
  (JNIEnv *Env, jclass, jint screen_width, jint screen_height)
{
	int ret = 0;
	struct uinput_user_dev mouse;

	gHandle = open("/dev/uinput", O_WRONLY | O_NONBLOCK);
	if( gHandle < 0 )
	{
		LOGE("open virtual mouse fail");
		return;
	}

	if (ioctl(gHandle, UI_SET_EVBIT, EV_ABS) < 0)
	{
		close(gHandle);
		gHandle = -1;
		LOGE("set input info error!!!!");
		return;
	}
	if (ioctl(gHandle, UI_SET_EVBIT, EV_SYN) < 0)
	{
		close(gHandle);
		gHandle = -1;
		LOGE("set input info error!!!!");
		return;
	}
	if( ioctl(gHandle, UI_SET_EVBIT, EV_KEY) < 0 )
	{
		close(gHandle);
		gHandle = -1;
		LOGE("set input info error!!!!");
		return;
	}
	if( ioctl(gHandle, UI_SET_KEYBIT, BTN_RIGHT) < 0 )
	{
		close(gHandle);
		gHandle = -1;
		LOGE("set input info error!!!!");
		return;
	}
	if( ioctl(gHandle, UI_SET_KEYBIT, BTN_LEFT) < 0 )
	{
		close(gHandle);
		gHandle = -1;
		LOGE("set input info error!!!!");
		return;
	}
	if (ioctl(gHandle,UI_SET_ABSBIT,ABS_MT_POSITION_X) < 0)
	{
		close(gHandle);
		gHandle = -1;
		LOGE("set input info error!!!!");
		return;
	}
	if (ioctl(gHandle,UI_SET_ABSBIT,ABS_MT_POSITION_Y) < 0)
	{
		close(gHandle);
		gHandle = -1;
		LOGE("set input info error!!!!");
		return;
	}
	if (ioctl(gHandle, UI_SET_ABSBIT, ABS_MT_TRACKING_ID) < 0)
	{
		close(gHandle);
		gHandle = -1;
		LOGE("set input info error!!!!");
		return;
	}
	if( ioctl(gHandle, UI_SET_PROPBIT, INPUT_PROP_DIRECT) < 0 )
	{
		close(gHandle);
		gHandle = -1;
		LOGE("set input info error!!!!");
		return;
	}

	memset(&mouse, 0, sizeof(struct uinput_user_dev));
	snprintf(mouse.name, UINPUT_MAX_NAME_SIZE, DEVICE_NAME);
	mouse.id.bustype = BUS_USB;
	mouse.id.vendor = 0x1234;
	mouse.id.product = 0xfedc;
	mouse.id.version = 1;

	mouse.absmax[ABS_MT_POSITION_X] = screen_width;
	mouse.absmax[ABS_MT_POSITION_Y] = screen_height;
	mouse.absmax[ABS_MT_TRACKING_ID] = 65535;

	ret = write(gHandle, &mouse, sizeof(struct uinput_user_dev));
	if( ret != sizeof(struct uinput_user_dev) )
	{
		close(gHandle);
		gHandle = -1;
		LOGE("write inputinfo error!!!!");
		return;
	}

	ret = ioctl(gHandle, UI_DEV_CREATE);
	if (ret < 0)
	{
		close(gHandle);
		gHandle = -1;
		LOGE("create virtual mouse fail");
		return;
	}

	LOGI("virtual mouse created success!");
}

/*
 * Class:     com_example_server_Mouse
 * Method:    ReleaseMouse
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_example_server_Mouse_ReleaseMouse
  (JNIEnv *Env, jclass)
{
	if( gHandle >= 0 )
	{
		ioctl(gHandle, UI_DEV_DESTROY);
		close(gHandle);
		gHandle = -1;
		LOGI("destroy virtual mouse");
	}
}

/*
 * Class:     com_example_server_Mouse
 * Method:    SendTrackId
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_example_server_Mouse_SendTrackId
  (JNIEnv *, jclass, jint touchId)
{
	struct input_event ev;

	memset(&ev, 0, sizeof(struct input_event));
	gettimeofday(&ev.time, NULL);
	ev.type = EV_ABS;
	ev.code = ABS_MT_TRACKING_ID;
	ev.value = touchId;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("down error\n");
	}
}

/*
 * Class:     com_example_server_Mouse
 * Method:    SendInput
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_com_example_server_Mouse_SendInput
  (JNIEnv *, jclass, jint x, jint y)
{
	struct input_event ev;



	//发送X坐标
	memset(&ev, 0, sizeof(struct input_event));
	gettimeofday(&ev.time, NULL);
	ev.type = EV_ABS;
	ev.code = ABS_MT_POSITION_X;
	ev.value = x;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("down error\n");
	}

	//发送Y坐标
	memset(&ev, 0, sizeof(struct input_event));
	gettimeofday(&ev.time, NULL);
	ev.type = EV_ABS;
	ev.code = ABS_MT_POSITION_Y;
	ev.value = y;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("down error\n");
	}
}

/*
 * Class:     com_example_server_Mouse
 * Method:    SendMtSync
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_example_server_Mouse_SendMtSync
  (JNIEnv *, jclass)
{

	struct input_event ev;

	memset(&ev, 0, sizeof(struct input_event));
	gettimeofday(&ev.time, NULL);
	ev.type = EV_SYN;
	ev.code = SYN_MT_REPORT;
	ev.value = 0;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("move error\n");
	}
}

/*
 * Class:     com_example_server_Mouse
 * Method:    SendSync
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_example_server_Mouse_SendSync
  (JNIEnv *, jclass)
{
	struct input_event ev;

	memset(&ev, 0, sizeof(struct input_event));
	gettimeofday(&ev.time, NULL);
	ev.type = EV_SYN;
	ev.code = SYN_REPORT;
	ev.value = 0;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("move error\n");
	}
}

//发送点击消息
static void mouse_report_key( uint16_t type, uint16_t keycode, int32_t value)
{
    struct input_event ev;

    if( gHandle < 0 )
    {
    	LOGE("gHandle not open");
        return;
    }

    memset(&ev, 0, sizeof(struct input_event));
    gettimeofday(&ev.time, NULL);
    ev.type = type;
    ev.code = keycode;
    ev.value = value;

    if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
    {
    	LOGE("key report error\n");
    }
}

//发送右键
JNIEXPORT void JNICALL Java_com_example_server_Mouse_mouse_1right_1key
  (JNIEnv *, jclass)
{
	mouse_report_key(EV_KEY, BTN_RIGHT, 1);
	mouse_report_key(EV_SYN, SYN_REPORT, 0);
	mouse_report_key(EV_KEY, BTN_RIGHT, 0);
	mouse_report_key(EV_SYN, SYN_REPORT, 0);
}

