package com.dds.java.voip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.dds.App;
import com.dds.skywebrtc.SkyEngineKit;

/**
 * Created by dds on 2019/8/25.
 * android_shuai@163.com
 */
public class VoipReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Utils.ACTION_VOIP_RECEIVER.equals(action)) {
            String room = intent.getStringExtra("room");
            boolean audioOnly = intent.getBooleanExtra("audio_only", true);
            String inviteId = intent.getStringExtra("from_uid");
            //服务器不返回这个参数了
//            String userList = intent.getStringExtra("to_uid");
//            String[] list = userList.split(",");
            SkyEngineKit.init(new VoipEvent());
            boolean b = SkyEngineKit.Instance().startInCall(App.getInstance(), room, inviteId, audioOnly);
            if (b) {
//                if (list.length == 1) {
                    CallSingleActivity.openActivity(context, inviteId, false, audioOnly);
//                } else {
                    // 群聊
//                }

            }


        }

    }
}
