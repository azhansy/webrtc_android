package com.dds.skywebrtc.client.voip;

import android.content.Context;

import com.dds.skywebrtc.client.socket.SocketManager;

/**
 * Created by dds on 2019/8/5.
 * android_shuai@163.com
 */
public class SkyWebrtcUtils {

    public static Context appContext;
    public static String ACTION_VOIP_RECEIVER;

    public static void init(Context appContext) {
        SkyWebrtcUtils.appContext = appContext.getApplicationContext();
        ACTION_VOIP_RECEIVER = appContext.getPackageName() + ".voip.Receiver";
    }

    public static void login(String server, String userId){
        if (appContext == null) {
            throw new RuntimeException("appContext not init!");
        }
        SocketManager.getInstance().connect(server, userId);
    }

    public static void logout() {
        SocketManager.getInstance().unConnect();
    }

}
