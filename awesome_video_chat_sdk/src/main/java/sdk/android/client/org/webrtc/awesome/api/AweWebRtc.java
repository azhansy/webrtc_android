package org.webrtc.awesome.api;

import android.content.Context;

import androidx.annotation.NonNull;

/**
 * @Author: hsh
 * @Description: SDK对外开发的api
 * @CreateDate: 2020-05-17
 */
public interface AweWebRtc {

    /**
     *
     * @param signalServer 信令服务器地址
     * @param userId 登录用户id
     */
    void login(@NonNull String signalServer, @NonNull String userId);

    void logout();

    /**
     *
     * @param context
     * @param dstUserId 目标用户Id
     */
    void startVideoChat(@NonNull Context context, @NonNull String dstUserId);

    /**
     *
     * @param context
     * @param dstUserId 目标用户Id
     */
    void startAudioChat(@NonNull Context context, @NonNull String dstUserId);

}
