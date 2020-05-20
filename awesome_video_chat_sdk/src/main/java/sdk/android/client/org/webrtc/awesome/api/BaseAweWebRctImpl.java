package org.webrtc.awesome.api;

import android.content.Context;

import androidx.annotation.NonNull;

import org.webrtc.awesome.SimpleActivityLifecycleCallbacks;
import org.webrtc.awesome.SkyEngineKit;
import org.webrtc.awesome.socket.IUserState;
import org.webrtc.awesome.socket.SocketManager;
import org.webrtc.awesome.voip.CallSingleActivity;
import org.webrtc.awesome.voip.SkyWebrtcUtils;
import org.webrtc.awesome.voip.VoipEvent;

/**
 * @Author: hsh
 * @Description: 视频聊天基础实现类
 * @CreateDate: 2020-05-17
 */

class BaseAweWebRctImpl implements AweWebRtc {

    BaseAweWebRctImpl(@NonNull Context context) {
        SkyWebrtcUtils.init(context.getApplicationContext());
    }

    @Override
    public void login(@NonNull String signalServer,@NonNull  String userId) {
        SocketManager.getInstance().connect(signalServer, userId);
    }

    @Override
    public void logout() {
        SocketManager.getInstance().unConnect();
    }

    @Override
    public void startVideoChat(@NonNull Context context, @NonNull String dstUserId) {
        SkyEngineKit.init(new VoipEvent());
        CallSingleActivity.openActivity(context, dstUserId, true, false);
    }

    @Override
    public void startAudioChat(@NonNull Context context, @NonNull String dstUserId) {
        SkyEngineKit.init(new VoipEvent());
        CallSingleActivity.openActivity(context, dstUserId, true, true);
    }

    @Override
    public void setIUserStateCallback(IUserState userState) {
        SocketManager.getInstance().addUserStateCallback(userState);
    }

    @Override
    public int getUserLoginState() {
        return SocketManager.getInstance().getUserState();
    }
}
