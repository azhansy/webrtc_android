package org.webrtc.awesome.api;

import android.content.Context;

/**
 * @Author: hsh
 * @Description: 视频聊天工厂
 * @CreateDate: 2020-05-17
 */
public final class AweChatFactory {

    private static final String TAG = "AweChatFactory";

    private AweChatFactory() {
        throw new RuntimeException(this.getClass().getSimpleName() + " should not be instantiated");
    }

    public static AweWebRtc createAweChat(Context context) {
        return new BaseAweWebRctImpl(context);
    }

}
