package org.webrtc.awesome;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dds on 2020/3/11.
 * android_shuai@163.com
 */
public class Peer implements SdpObserver, PeerConnection.Observer, DataChannel.Observer {
    private final static String TAG = "dds_Peer";
    PeerConnection pc;
    private DataChannel mDataChannel;
    private String userId;
    private List<IceCandidate> queuedRemoteCandidates;
    private SessionDescription localSdp;
    private CallSession mSession;


    private boolean isOffer;
    private Context mContext;

    private boolean isIceRestart;

    public Peer(Context context, CallSession session, String userId) {
        this.mSession = session;
        this.pc = createPeerConnection();
        this.mContext = context;
        DataChannel.Init dcInit = new DataChannel.Init();
        dcInit.id = 1;
        mDataChannel = pc.createDataChannel("P2P MSG DC", dcInit);
        this.userId = userId;
        queuedRemoteCandidates = new ArrayList<>();


    }

    public PeerConnection createPeerConnection() {
        // 管道连接抽象类实现方法
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(mSession.avEngineKit.getIceServers());
        rtcConfig.iceConnectionReceivingTimeout = 60*1000;
        rtcConfig.iceUnwritableTimeMs = 15*1000;
        rtcConfig.iceUnwritableMinChecks = 60;
        return mSession._factory.createPeerConnection(rtcConfig, this);
    }

    public void setOffer(boolean isOffer) {
        this.isOffer = isOffer;
    }


    // 创建offer
    public void createOffer() {
        if (pc == null) return;
        pc.createOffer(this, offerOrAnswerConstraint());
    }

    // 创建answer
    public void createAnswer() {
        if (pc == null) return;
        pc.createAnswer(this, offerOrAnswerConstraint());

    }

    public void iceRestart() {
        if (pc == null) return;
        this.isOffer = true;
        this.isIceRestart = true;
        pc.createOffer(this, offerOrAnswerRestartConstraint());
    }

    public void setRemoteDescription(SessionDescription sdp) {
        if (pc == null) return;
        pc.setRemoteDescription(this, sdp);
    }

    public void addLocalStream(MediaStream stream) {
        if (pc == null) return;
        pc.addStream(stream);
    }


    public void addRemoteIceCandidate(final IceCandidate candidate) {
        if (pc != null) {
            if (queuedRemoteCandidates != null) {
                queuedRemoteCandidates.add(candidate);
            } else {
                pc.addIceCandidate(candidate);
            }
        }
    }

    public void removeRemoteIceCandidates(final IceCandidate[] candidates) {
        if (pc == null) {
            return;
        }
        drainCandidates();
        pc.removeIceCandidates(candidates);
    }


    public void close() {
        if (pc != null) {
            pc.close();
        }
    }

    //------------------------------Observer-------------------------------------
    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.i(TAG, "onSignalingChange: " + signalingState);
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {
        Log.i(TAG, "onIceConnectionChange: " + newState.toString());

//        if (newState == PeerConnection.IceConnectionState.CONNECTED) {
//            mSession.peerOperator.connectedComplete();
//        } else if (newState == PeerConnection.IceConnectionState.FAILED) {
//            mSession.peerOperator.connectedFailed();
//        }

        if (mSession._callState != EnumType.CallState.Connected) return;
        if (newState == PeerConnection.IceConnectionState.DISCONNECTED || newState == PeerConnection.IceConnectionState.FAILED) {
            if (mSession.lastDisconnectedTime > 0 && (System.currentTimeMillis() - mSession.lastDisconnectedTime) / 1000 < 10) {
                iceRestart();
//                mSession.peerOperator.iceRestart();
                mSession.lastDisconnectedTime = 0;
            }
        }

    }

    @Override
    public void onIceConnectionReceivingChange(boolean receiving) {
        Log.i(TAG, "onIceConnectionReceivingChange:" + receiving);
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {
        Log.i(TAG, "onIceGatheringChange:" + newState.toString());
    }


    @Override
    public void onIceCandidate(IceCandidate candidate) {
        Log.i(TAG, "onIceCandidate:");
        // 发送IceCandidate
        mSession.executor.execute(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            mSession.peerOperator.sendCandidate();
            mSession.avEngineKit.mEvent.sendIceCandidate(userId, candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp);
        });


    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {
        Log.i(TAG, "onIceCandidatesRemoved:");
    }

    @Override
    public void onAddStream(MediaStream stream) {
        mSession.setRemoteStream(stream);
        Log.i(TAG, "onAddStream:");
        if (stream.audioTracks.size() > 0) {
            stream.audioTracks.get(0).setEnabled(true);
        }
        if (mSession.sessionCallback.get() != null) {
            mSession.sessionCallback.get().didReceiveRemoteVideoTrack();
        }


    }

    @Override
    public void onRemoveStream(MediaStream stream) {
        Log.i(TAG, "onRemoveStream:");
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Log.i(TAG, "onDataChannel:");
        dataChannel.registerObserver(this);
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.i(TAG, "onRenegotiationNeeded:");
    }

    @Override
    public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {
        Log.i(TAG, "onAddTrack:");
    }


    //-------------SdpObserver--------------------
    @Override
    public void onCreateSuccess(SessionDescription origSdp) {
        Log.d(TAG, "sdp创建成功       " + origSdp.type);
        String sdpString = origSdp.description;
        final SessionDescription sdp = new SessionDescription(origSdp.type, sdpString);
        localSdp = sdp;
//        mSession.peerOperator.createSdpSuccess();

        mSession.executor.execute(() -> pc.setLocalDescription(this, sdp));
    }

    @Override
    public void onSetSuccess() {
        mSession.executor.execute(() -> {
//            mSession.peerOperator.setSdpSuccess();

            Log.d(TAG, "sdp连接成功   " + pc.signalingState().toString());
            if (pc == null) return;
            // 发送者
            if (isOffer) {
                if (pc.getRemoteDescription() == null || isIceRestart) {
                    Log.d(TAG, "Local SDP set succesfully");
                    if (!isOffer) {
                        //接收者，发送Answer
//                        mSession.peerOperator.sendAnswer();
                        mSession.avEngineKit.mEvent.sendAnswer(userId, localSdp.description);
                    } else {
                        //发送者,发送自己的offer
//                        mSession.peerOperator.sendOffer();
                        mSession.avEngineKit.mEvent.sendOffer(userId, localSdp.description);
                    }
                    isIceRestart = false;
                } else {
                    Log.d(TAG, "Remote SDP set succesfully");

                    drainCandidates();
                }

            } else {
                if (pc.getLocalDescription() != null) {
                    Log.d(TAG, "Local SDP set succesfully");
                    if (!isOffer) {
//                        mSession.peerOperator.sendAnswer();

                        //接收者，发送Answer
                        mSession.avEngineKit.mEvent.sendAnswer(userId, localSdp.description);
                    } else {
//                        mSession.peerOperator.sendOffer();
                        //发送者,发送自己的offer
                        mSession.avEngineKit.mEvent.sendOffer(userId, localSdp.description);
                    }

                    drainCandidates();
                } else {
                    Log.d(TAG, "Remote SDP set succesfully");
                }
            }
        });


    }

    @Override
    public void onCreateFailure(String error) {
        Log.i(TAG, " SdpObserver onCreateFailure:" + error);
    }

    @Override
    public void onSetFailure(String error) {
        Log.i(TAG, "SdpObserver onSetFailure:" + error);
    }


    private void drainCandidates() {
        if (queuedRemoteCandidates != null) {
            Log.d(TAG, "Add " + queuedRemoteCandidates.size() + " remote candidates");
            for (IceCandidate candidate : queuedRemoteCandidates) {
                pc.addIceCandidate(candidate);
            }
            queuedRemoteCandidates = null;
        }
    }

    private MediaConstraints offerOrAnswerConstraint() {
        MediaConstraints mediaConstraints = new MediaConstraints();
        ArrayList<MediaConstraints.KeyValuePair> keyValuePairs = new ArrayList<>();
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        mediaConstraints.mandatory.addAll(keyValuePairs);
        return mediaConstraints;
    }

    private MediaConstraints offerOrAnswerRestartConstraint() {
        MediaConstraints mediaConstraints = new MediaConstraints();
        ArrayList<MediaConstraints.KeyValuePair> keyValuePairs = new ArrayList<>();
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        keyValuePairs.add(new MediaConstraints.KeyValuePair("IceRestart", "true"));
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        mediaConstraints.mandatory.addAll(keyValuePairs);
        return mediaConstraints;
    }

    /////////////////////////////DataChannel=============

    @Override
    public void onBufferedAmountChange(long previousAmount) {
        Log.i(TAG, "DataChannel onBufferedAmountChange:" + previousAmount);

    }

    @Override
    public void onStateChange() {
        Log.i(TAG, "DataChannel onStateChange:");

    }

    @Override
    public void onMessage(DataChannel.Buffer buffer) {
        //保存的文件名称，为传过来 加个copy名称做区分，后续可去掉
//        FileUtils.read2Io(byteString, "/storage/emulated/0/" + fileName + "_copy" + "." + filePrefix);
        // onMessage 回调中处理消息
        ByteBuffer data = buffer.data;
        final byte[] bytes = new byte[data.capacity()];
        data.get(bytes);
        String msg = new String(bytes);
        Log.d(TAG, "onMessage " + msg);

        JSONObject jsonObject = JSONObject.parseObject(msg);
//        Map map = JSON.parseObject(msg, Map.class);
        String type = (String) jsonObject.get("type");
        if (type == null) return;
////        // 传输文件
        if (type.equals("file")) {
            handleFile(jsonObject);
            return;
        }

    }

    private void handleFile(JSONObject map) {
        JSONObject data = map.getJSONObject("data");
        if (data != null) {
            Log.d(TAG, "onMessage handleFile,data=" + data);
            int chunks = (int) data.get("chunks");
            int chunk = (int) data.get("chunk");
            String file_name = (String) data.get("file_name");
            String send_id = (String) data.get("send_id");
            int block_size = (int) data.get("block_size");
            String fileStr = (String) data.get("file_byte");
            File file = FileUtils.getAppDir(mContext);
            String filePath = file.getAbsolutePath() + "/" + file_name;

            Log.d(TAG, "onMessage handleFile,filePath=" + filePath);

            FileUtils.appendFileWithInstream(filePath,
                    new ByteArrayInputStream(fileStr.getBytes()),
                    block_size * chunk);
        }
    }


    void sendDataChannelMessage(String filePath, String sendId) {
        long blockSize = FileUtils.SIZE_1M; //每块大小
        long chunks = 1L; //默认1块
        File file = new File(filePath);
        long fileLength = file.length();
        if (fileLength < blockSize) {  //如果文件大小 小于1MB，chunks默认为1，并且blockSize为file.length
            blockSize = fileLength;
        } else {
            if (fileLength % blockSize == 0L) {  //块数
                chunks = (fileLength / blockSize);
            } else {
                chunks = ((fileLength / blockSize)) + 1;
            }
        }

        for (long i = 0; i < chunks; i++) {
            long offset = i * blockSize;

            if (chunks > 1 && i == chunks - 1) {
                //如果块数大于1个,并且是最后一块数据，需要计算最后一块的blockSize
                blockSize = (fileLength - (blockSize * (chunks - 1)));
            }

            byte[] mBlock = FileUtils.getBlock(offset, file, (int) blockSize);

//            Map<String, Object> map = new HashMap<>();
//            Map<String, Object> childMap = new HashMap<>();

            JSONObject jsonObject = new JSONObject();
            JSONObject childObject = new JSONObject();
            childObject.put("chunks", chunks);
            childObject.put("chunk", i);
            childObject.put("file_name", getSuffixName(filePath));
            childObject.put("block_size", blockSize);
            childObject.put("send_id", sendId);
            childObject.put("file_byte", new String(mBlock));
            jsonObject.put("type", "file");
            jsonObject.put("data", childObject);
            DataChannel.Buffer buffer = new DataChannel.Buffer(ByteBuffer.wrap(jsonObject.toString().getBytes()), false);
            mDataChannel.send(buffer);
            Log.i(TAG, "DataChannel 开始传输文件分片，i=" + i + ",map=" + jsonObject.toString());

        }

//        byte[] msg = "azhansy".getBytes();
//        DataChannel.Buffer buffer = new DataChannel.Buffer(ByteBuffer.wrap(msg), false);
//        mDataChannel.send(buffer);
    }

    String getSuffixName(String filePath) {
        return filePath.substring(filePath.lastIndexOf("/") + 1);
    }

//

}
