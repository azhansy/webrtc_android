package com.dds.skywebrtc.client.socket;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.dds.skywebrtc.CallSession;
import com.dds.skywebrtc.Offer;
import com.dds.skywebrtc.SkyEngineKit;
import com.dds.skywebrtc.UserIceCandidate;
import com.dds.skywebrtc.client.voip.SkyWebrtcUtils;
import com.dds.skywebrtc.client.voip.VoipReceiver;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * Created by dds on 2019/7/26.
 * android_shuai@163.com
 */
public class SocketManager implements IEvent {
    private final static String TAG = "dds_SocketManager";
    private DWebSocket webSocket;
    private int userState;
    private String myId;


    private Handler handler = new Handler(Looper.getMainLooper());
    private Offer offer;

    private List<UserIceCandidate> candidateList = new ArrayList<>();
//    public PeerOperator peerOperator;

    private long lastHealth = -1;               // 最后一次心跳时间
    private long heartbeatIdleTime = 30 * 1000;     // 30秒空闲时间
    private long heartbeatInterval = 30 * 1000;     // 心跳间隔，10秒一次心跳
    private String heartbeatCMessage = "~H#C~";   // 客户端心跳发送内容
    private Handler mHandler = new Handler();// 心跳定时器

    private long reconnectTime = 2000;          // 重连时间，2秒
    private String url;

    public void clear() {
        offer = null;
        candidateList.clear();
    }

    private SocketManager() {

    }


    public static class Holder {
        static SocketManager socketManager = new SocketManager();
    }

    public static SocketManager getInstance() {
        return Holder.socketManager;
    }


//    public void setPeerOperator(PeerOperator peerOperator) {
//        this.peerOperator = peerOperator;
//    }

    public void connect(String url, String userId) {
        this.url = url;
        if (webSocket == null || !webSocket.isOpen()) {
            myId = userId;
            URI uri;
            try {
                String urls = url + "&user_id=" + userId;
                Log.i(TAG, "urls=" + urls);

                uri = new URI(urls);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return;
            }
            webSocket = new DWebSocket(uri, this);
            // 设置wss
            if (url.startsWith("wss")) {
                try {
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    if (sslContext != null) {
                        sslContext.init(null, new TrustManager[]{new DWebSocket.TrustManagerTest()}, new SecureRandom());
                    }

                    SSLSocketFactory factory = null;
                    if (sslContext != null) {
                        factory = sslContext.getSocketFactory();
                    }

                    if (factory != null) {
                        webSocket.setSocket(factory.createSocket());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // 开始connect
            webSocket.connect();
            mHandler.postDelayed(heartBeatRunnable, heartbeatInterval);//开启心跳检测
        }


    }

    //心跳检测
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (lastHealth != -1 && ((System.currentTimeMillis() - lastHealth) > heartbeatIdleTime)) {

                Log.e(TAG, "服务器没有响应，进入重连");
                reconnectWs();

            } else {
                if (webSocket != null && webSocket.isOpen()) {
                    if (webSocket.isClosed()) {
                        Log.e(TAG, "连接在正常下，已断开， 可能断网了，可能是通话结束! ");

                        reconnectWs();

                    } else {
                        // 判断发送是否结束
                        Log.d(TAG, "send--> " + heartbeatCMessage);
                        webSocket.send(heartbeatCMessage);
                    }
                }

            }
            //每隔一定的时间，对长连接进行一次心跳检测
            mHandler.postDelayed(this, heartbeatInterval);
        }
    };

    /**
     * 开启重连
     */
    private void reconnectWs() {
        //此时应该触发关闭，然后进入重连
        if (webSocket != null) {
            webSocket.close();
            //如果client已为空，重新初始化连接
            webSocket = null;
        }

        mHandler.removeCallbacks(heartBeatRunnable);
        mHandler.postDelayed(() -> {
            connect(url, myId);
        }, reconnectTime);
    }

    public void unConnect() {
        if (webSocket != null) {
            webSocket.setConnectFlag(false);
            webSocket.close();
            webSocket = null;
        }

    }

    @Override
    public void onOpen() {
        Log.i(TAG, "socket is open!");
        //服务器在连接成功后，没有返回loginSuccess，我们自己调用一下
        loginSuccess(myId, "");

        if (webSocket != null && webSocket.connectFlag && SkyEngineKit.Instance().getCurrentSession().mRoom != null) {
            webSocket.reJoinRoom(SkyEngineKit.Instance().getCurrentSession().mRoom, 1);
        }

//        if (peerOperator != null) {
//            peerOperator.socketOpen(offer == null ? 0 : 1, candidateList.size());
//        }

        if (offer != null) {
            sendOffer(offer.userId, offer.sdp);
            offer = null;
        }

        if (!candidateList.isEmpty()) {
            for (UserIceCandidate candidate :
                    candidateList) {
                sendIceCandidate(candidate.userId, candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp);
            }
            candidateList.clear();
        }
    }

    @Override
    public void loginSuccess(String userId, String avatar) {
        Log.i(TAG, "loginSuccess:" + userId);
//        if (peerOperator != null) {
//            peerOperator.loginSuccess(userId);
//        }
        myId = userId;
        userState = 1;
        if (iUserState != null && iUserState.get() != null) {
            iUserState.get().userLogin();
        }
    }


    // ======================================================================================
    //我们的流程不用创建，这里是直接加入房间
    public void createRoom(String room, int roomSize) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.sendJoin(room, myId);
        } else {
            Log.e(TAG, "send--> socket close !");
            onSocketException();
        }

    }

    public void sendInvite(String room, String users, boolean audioOnly) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.sendInvite(room, myId, users, audioOnly);
        } else {
            Log.e(TAG, "send--> socket close !");
            onSocketException();
        }
    }

    public void sendLeave(String room, String userId) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.sendLeave(myId, room, userId);
        } else {
            Log.e(TAG, "send--> socket close !");
            onSocketException();
        }
    }

    public void sendRingBack(String targetId) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.sendRing(myId, targetId);
        } else {
            Log.e(TAG, "send--> socket close !");
            onSocketException();
        }
    }

    public void sendRefuse(String inviteId, int refuseType) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.sendRefuse(inviteId, myId, refuseType);
        } else {
            Log.e(TAG, "send--> socket close !");
            onSocketException();
        }
    }

    public void sendCancel(String userId) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.sendCancel(myId, userId);
        } else {
            Log.e(TAG, "send--> socket close !");
            onSocketException();
        }
    }

    public void sendJoin(String room) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.sendJoin(room, myId);
        } else {
            Log.e(TAG, "send--> socket close !");
            onSocketException();
        }
    }

    public void sendMeetingInvite(String userList) {

    }

    public void sendOffer(String userId, String sdp) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.sendOffer(myId, userId, sdp);
        } else {
            offer = new Offer(userId, sdp);
            Log.e(TAG, "send--> socket close !");
            onSocketException();
        }
    }

    public void sendAnswer(String userId, String sdp) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.sendAnswer(myId, userId, sdp);
        } else {
            Log.e(TAG, "send--> socket close !");
            onSocketException();
        }
    }

    public void sendIceCandidate(String userId, String id, int label, String candidate) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.sendIceCandidate(userId, id, label, candidate);
        } else {
            candidateList.add(new UserIceCandidate(userId, id, label, candidate));
            Log.e(TAG, "send--> socket close !");
            onSocketException();
        }
    }

    public void sendTransAudio(String userId) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.sendTransAudio(myId, userId);
        } else {
            Log.e(TAG, "send--> socket close !");
            onSocketException();
        }
    }

    public void sendDisconnect(String userId) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.sendDisconnect(myId, userId);
        } else {
            Log.e(TAG, "send--> socket close !");
            onSocketException();
        }
    }


    // ========================================================================================
    @Override
    public void onInvite(String room, boolean audioOnly, String inviteId, String userList) {
        Intent intent = new Intent();
        intent.putExtra("room", room);
        intent.putExtra("audio_only", audioOnly);
        intent.putExtra("from_uid", inviteId);
        intent.putExtra("to_uid", userList);
        intent.setAction(SkyWebrtcUtils.ACTION_VOIP_RECEIVER);
        intent.setComponent(new ComponentName(SkyWebrtcUtils.appContext.getPackageName(), VoipReceiver.class.getName()));
        // 发送广播
        SkyWebrtcUtils.appContext.sendBroadcast(intent);

    }

    @Override
    public void onCancel(String fromId) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onCancel(fromId);
            }
        });
    }

    @Override
    public void onRing(String fromId) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onRingBack(fromId);
            }
        });


    }

    @Override  // 加入房间
    public void onPeers(String myId, ArrayList<String> userId) {
        handler.post(() -> {
            //自己进入了房间，然后开始发送offer
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onJoinHome(myId, userId);
            }
        });

    }

    @Override
    public void onNewPeer(String userId) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.newPeer(userId);
            }
        });

    }

    @Override
    public void onReject(String userId, int type) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onRefuse(userId);
            }
        });

    }

    @Override
    public void onOffer(String userId, String sdp) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onReceiveOffer(userId, sdp);
            }
        });


    }

    @Override
    public void onAnswer(String userId, String sdp) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onReceiverAnswer(userId, sdp);
            }
        });

    }

    @Override
    public void onIceCandidate(String userId, String id, int label, String candidate) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onRemoteIceCandidate(userId, id, label, candidate);
            }
        });

    }

    @Override
    public void onLeave(String userId) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onLeave(userId);
            }
        });
    }

    @Override
    public void logout(String str) {
        Log.i(TAG, "logout:" + str);
        userState = 0;
        if (iUserState != null && iUserState.get() != null) {
            iUserState.get().userLogout();
        }
    }

    @Override
    public void onTransAudio(String userId) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onTransAudio(userId);
            }
        });
    }

    @Override
    public void onDisConnect(String userId) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onDisConnect(userId);
            }
        });
    }

    @Override
    public void reConnect() {
        handler.post(() -> {
            webSocket.reconnect();
        });
    }

    @Override
    public void onSendFile(String userId, String filePath) {
        handler.post(() -> {
            if (webSocket != null && webSocket.isOpen()) {
                webSocket.sendFile(userId, filePath);
            }
        });
    }


    private void onSocketException() {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onSocketException();
            }
        });
    }

//    @Override
//    public void onReJoinRoom() {
//        handler.post(() -> {
//            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
//            if (webSocket != null && currentSession != null) {
//                webSocket.sendJoin(currentSession.mRoom, currentSession.mMyId);
//            }
//        });
//    }

//    @Override
//    public PeerOperator getPeerOperate() {
//        return peerOperator;
//    }

    //===========================================================================================


    public int getUserState() {
        return userState;
    }

    private WeakReference<IUserState> iUserState;

    public void addUserStateCallback(IUserState userState) {
        iUserState = new WeakReference<>(userState);
    }

}
