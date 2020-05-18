package org.webrtc.awesome;

public class Offer {
    public String sdp;
    public String userId;

    public Offer(String userId, String sdp) {
        this.sdp = sdp;
        this.userId = userId;
    }
}
