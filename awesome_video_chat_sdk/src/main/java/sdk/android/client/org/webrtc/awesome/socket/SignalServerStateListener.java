package org.webrtc.awesome.socket;

/**
 * @Author: hsh
 * @Description: 监听socket状态接口
 * @CreateDate: 2020-05-21
 */
public interface SignalServerStateListener {
    int STATE_DISCONNECT = -1;
    int STATE_CONNECTING = 0;
    int STATE_CONNECTED = 1;

    void serverStateChange(int state);
}
