#include "com_example_server_Mouse.h"

#define MAX_POINT_NUM  100

static int gHandle = -1;
static int gCurPointNum = 0;
static int gTrackId = 0;
static int gused[MAX_POINT_NUM];
static int gMaxNum = 0;         //统计当前最大的点数目


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

	gTrackId = 0;
	gCurPointNum = 0;
	memset(gused, 0, sizeof(gused));
	for( int i = 0; i < MAX_POINT_NUM; i++ )
	{
		gused[i] = -1;
	}

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
	if (ioctl(gHandle,UI_SET_ABSBIT,ABS_MT_TOUCH_MAJOR) < 0)
	{
		close(gHandle);
		gHandle = -1;
		LOGE("set input info error!!!!");
		return;
	}
	if (ioctl(gHandle,UI_SET_ABSBIT,ABS_MT_WIDTH_MAJOR) < 0)
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
	if (ioctl(gHandle, UI_SET_ABSBIT, ABS_MT_SLOT) < 0)
	{
		close(gHandle);
		gHandle = -1;
		LOGE("set input info error!!!!");
		return;
	}
	/*
	if ( ioctl(gHandle, UI_SET_KEYBIT, BTN_TOUCH) < 0 )
	{
		close(gHandle);
		gHandle = -1;
		LOGE("set input info error!!!!");
		return;
	}
	*/
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
	mouse.absmax[ABS_MT_SLOT] = 9;

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
 * Method:    mouse_right_key
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_example_server_Mouse_mouse_1right_1key
  (JNIEnv *Env, jclass)
{
	mouse_report_key(EV_KEY, BTN_RIGHT, 1);
	mouse_report_key(EV_SYN, SYN_REPORT, 0);

	mouse_report_key(EV_KEY, BTN_RIGHT, 0);
	mouse_report_key(EV_SYN, SYN_REPORT, 0);

	LOGI("RightUpDown");
}

/*
 * Class:     com_example_server_Mouse
 * Method:    mouse_left_key
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_example_server_Mouse_mouse_1left_1key
  (JNIEnv *Env, jclass, jint value)
{
	//mouse_report_key(EV_KEY, BTN_TOUCH, value);
	//mouse_report_key(EV_SYN, SYN_REPORT, 0);

	mouse_report_key(EV_KEY, BTN_LEFT, value);
	mouse_report_key(EV_SYN, SYN_REPORT, 0);
	//LOGI("LeftUpDown");
}

/*
 * Class:     com_example_server_Mouse
 * Method:    mouse_move
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_com_example_server_Mouse_mouse_1move
  (JNIEnv *Env, jclass, jint x, jint y)
{
	struct input_event ev;

	if( gHandle < 0 )
	{
		LOGE("device gHandle not open");
		return;
	}

	memset(&ev, 0, sizeof(struct input_event));
	gettimeofday(&ev.time, NULL);
	ev.type = EV_ABS;
	ev.code = ABS_X;
	ev.value = x;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("move error\n");
	}

	//memset(&ev, 0, sizeof(struct input_event));
	ev.type = EV_ABS;
	ev.code = ABS_Y;
	ev.value = y;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("move error\n");
	}


	//memset(&ev, 0, sizeof(struct input_event));
	ev.type = EV_SYN;
	ev.code = SYN_REPORT;
	ev.value = 0;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("move error\n");
	}

	//LOGI("Move");
}

/*
 * Class:     com_example_multitouch_Mouse
 * Method:    mouse_multi_down
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_com_example_server_Mouse_mouse_1multi_1down
  (JNIEnv *, jclass, jint x, jint y, jint touchId)
{
	struct input_event ev;

	gCurPointNum++;

	if( gCurPointNum > gMaxNum )
	{
		gMaxNum = gCurPointNum;
	}

	//查找ID
	/*
	for( int i = 0; i < gMaxNum; i++ )
	{
		if( gused[i] == -1 )
		{
			gused[i] = touchId;
			touchId = i;
			break;
		}
	}
	*/


	gTrackId++;
	if( gTrackId >= 65535 )
	{
		gTrackId = 0;
	}

	//发送笔迹ID
	memset(&ev, 0, sizeof(struct input_event));
	gettimeofday(&ev.time, NULL);
	//发送按下消息
	ev.type = EV_ABS;
	ev.code = ABS_MT_SLOT;
	ev.value = touchId;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("down error\n");
	}

	ev.type = EV_ABS;
	ev.code = ABS_MT_TRACKING_ID;
	ev.value = gTrackId;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("down error\n");
	}

	//mouse_report_key(EV_KEY, BTN_LEFT, MOUSE_DOWN);

	//mouse_report_key(EV_KEY, BTN_TOUCH, MOUSE_DOWN);
	//mouse_report_key(EV_KEY, BTN_LEFT, MOUSE_DOWN);

	//发送X坐标
	ev.type = EV_ABS;
	ev.code = ABS_MT_POSITION_X;
	ev.value = x;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("down error\n");
	}

	//发送Y坐标
	ev.type = EV_ABS;
	ev.code = ABS_MT_POSITION_Y;
	ev.value = y;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("down error\n");
	}

	//同步
	ev.type = EV_SYN;
	ev.code = SYN_REPORT;
	ev.value = 0;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("move error\n");
	}

	//LOGI( "x = %d y = %d\n", x, y );
}

/*
 * Class:     com_example_multitouch_Mouse
 * Method:    mouse_multi_move
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_com_example_server_Mouse_mouse_1multi_1move
  (JNIEnv *, jclass, jint x, jint y, jint touchId)
{
	struct input_event ev;

	//查找ID
	/*
	for( int i = 0; i < gMaxNum; i++ )
	{
		if( gused[i] == touchId )
		{
			touchId = i;
			break;
		}
	}
	*/

	//发送按下消息
	ev.type = EV_ABS;
	ev.code = ABS_MT_SLOT;
	ev.value = touchId;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("move error\n");
	}


	//发送X坐标
	ev.type = EV_ABS;
	ev.code = ABS_MT_POSITION_X;
	ev.value = x;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("move error\n");
	}

	//发送Y坐标
	ev.type = EV_ABS;
	ev.code = ABS_MT_POSITION_Y;
	ev.value = y;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("move error\n");
	}

	//同步
	ev.type = EV_SYN;
	ev.code = SYN_REPORT;
	ev.value = 0;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("move error\n");
	}

	//LOGI( "x = %d y = %d\n", x, y );
}

/*
 * Class:     com_example_multitouch_Mouse
 * Method:    mouse_multi_up
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_com_example_server_Mouse_mouse_1multi_1up
  (JNIEnv *, jclass, jint touchId)
{
	struct input_event ev;

	//查找ID
	/*
	for( int i = 0; i < gMaxNum; i++ )
	{
		if( gused[i] == touchId )
		{
			touchId = i;
			gused[i] = -1;
			break;
		}
	}
	*/

	memset(&ev, 0, sizeof(struct input_event));
	gettimeofday(&ev.time, NULL);
	ev.type = EV_ABS;
	ev.code = ABS_MT_SLOT;
	ev.value = touchId;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("up error\n");
	}

	ev.type = EV_ABS;
	ev.code = ABS_MT_TRACKING_ID;
	ev.value = -1;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("up error\n");
	}

	//mouse_report_key(EV_KEY, BTN_LEFT, MOUSE_UP);
	//mouse_report_key(EV_KEY, BTN_TOUCH, MOUSE_UP);
	//mouse_report_key(EV_KEY, BTN_LEFT, MOUSE_UP);

	//同步
	ev.type = EV_SYN;
	ev.code = SYN_REPORT;
	ev.value = 0;
	if (write(gHandle, &ev, sizeof(struct input_event)) < 0)
	{
		LOGE("up error\n");
	}

	gCurPointNum--;
}
