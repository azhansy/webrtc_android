package org.webrtc.awesome.voip;

import android.content.Context;

import org.webrtc.awesome.socket.SocketManager;


/**
 * Created by dds on 2019/8/5.
 * android_shuai@163.com
 */
public class SkyWebrtcUtils {

    public static Context appContext;
    public static String ACTION_VOIP_RECEIVER;

    public static void init(Context appContext) {
        SkyWebrtcUtils.appContext = appContext;
        ACTION_VOIP_RECEIVER = appContext.getPackageName() + ".voip.Receiver";
    }
}
