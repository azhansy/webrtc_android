package com.dds.skywebrtc;

public class UserIceCandidate {
    public String userId;
    public String sdpMid;
    public int sdpMLineIndex;
    public String sdp;

    public UserIceCandidate(String userId, String sdpMid, int sdpMLineIndex, String sdp) {
        this.userId = userId;
        this.sdpMid = sdpMid;
        this.sdpMLineIndex = sdpMLineIndex;
        this.sdp = sdp;
    }
}
