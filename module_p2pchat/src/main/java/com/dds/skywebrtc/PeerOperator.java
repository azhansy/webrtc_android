package com.dds.skywebrtc;

public interface PeerOperator {
    void createSdpSuccess();
    void sendOffer();
    void sendAnswer();
    void sendCandidate();
    void setSdpSuccess();
    void iceRestart();
    void connectedComplete();
    void connectedFailed();
    void receiveOffer();
    void receiveAnswer();

    void socketOpen(int offerSize, int iceSize);
    void loginSuccess(String userId);

    void socketState(String state);
}
