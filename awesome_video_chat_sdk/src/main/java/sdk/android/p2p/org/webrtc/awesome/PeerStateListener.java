package org.webrtc.awesome;

/**
 * @Author: hsh
 * @Description: 监听Ice通道状态
 * @CreateDate: 2020-05-21
 */
public interface PeerStateListener {
    void onDisconnect();

    void onReconnecting();

    void onConnected();

    void onConnectedFail();

    void onConnecting();
}
