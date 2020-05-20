package org.webrtc.awesome;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.webrtc.awesome.api.AweChatFactory;
import org.webrtc.awesome.api.AweWebRtc;
import org.webrtc.awesome.socket.IUserState;

/**
 * @Author: hsh
 * @Description: 视频语音入口类
 * @CreateDate: 2020-05-17
 */
public enum AweChat implements AweWebRtc {
    INSTANCE;

    private AweWebRtc aweWebRtc;

    public static void init(Application context) {
        INSTANCE.create(context);
        context.registerActivityLifecycleCallbacks(SimpleActivityLifecycleCallbacks.instance);
    }

    private void create(Context context) {
        aweWebRtc = AweChatFactory.createAweChat(context);
    }

    @Override
    public void login(@NonNull String signalServer, @NonNull String userId) {
        checkAwe();

        if (TextUtils.isEmpty(signalServer)) {
            throw new IllegalArgumentException("signalServer can not be empty!");
        }

        if (TextUtils.isEmpty(userId)) {
            throw new IllegalArgumentException("userId can not be empty!");
        }

        aweWebRtc.login(signalServer, userId);
    }

    @Override
    public void logout() {
        checkAwe();
        aweWebRtc.logout();
    }

    @Override
    public void startVideoChat(@NonNull Context context, @NonNull String dstUserId) {
        checkAwe();

        if (TextUtils.isEmpty(dstUserId)) {
            throw new IllegalArgumentException("dstUserId can not be empty!");
        }
        aweWebRtc.startVideoChat(context, dstUserId);
    }

    @Override
    public void startAudioChat(@NonNull Context context, @NonNull String dstUserId) {
        checkAwe();

        if (TextUtils.isEmpty(dstUserId)) {
            throw new IllegalArgumentException("dstUserId can not be empty!");
        }
        aweWebRtc.startAudioChat(context, dstUserId);
    }

    @Override
    public void setIUserStateCallback(IUserState userState) {
        checkAwe();

        aweWebRtc.setIUserStateCallback(userState);
    }

    @Override
    public int getUserLoginState() {
        checkAwe();
        return aweWebRtc.getUserLoginState();
    }

    private void checkAwe() {
        if (aweWebRtc == null) {
            throw new RuntimeException("AweWebRtc should be init first!");
        }
    }
}
