package com.dds.webrtclib.ws;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.webrtc.IceCandidate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by dds on 2019/1/3.
 * android_shuai@163.com
 */
public class JavaWebSocket implements IWebSocket {

    private final static String TAG = "dds_JavaWebSocket";

    private WebSocketClient mWebSocketClient;

    private ISignalingEvents events;

    private boolean isOpen; //是否连接成功过
    private boolean isFirstOpen = true; //是否已经连过一次

    private long lastHealth = -1;               // 最后一次心跳时间
    private long heartbeatIdleTime = 30 * 1000;     // 30秒空闲时间
    private long heartbeatInterval = 30 * 1000;     // 心跳间隔，10秒一次心跳
    private String heartbeatCMessage = "~H#C~";   // 客户端心跳发送内容
    private String heartbeatSMessage = "~H#S~";   // 服务器心跳发送内容
    private Handler mHandler = new Handler();// 心跳定时器

    private long reconnectTime = 2000;          // 重连时间，2秒
    //    this.reconnectObj = null;           // 重连定时器
    private boolean isDestroy = false;             // 是否销毁

    public JavaWebSocket(ISignalingEvents events) {
        this.events = events;
    }

    private String wss;

    @Override
    public void connect(String wss) {
        this.wss = wss;
        initSocketClient();
        if (wss.startsWith("wss")) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                if (sslContext != null) {
                    sslContext.init(null, new TrustManager[]{new TrustManagerTest()}, new SecureRandom());
                }

                SSLSocketFactory factory = null;
                if (sslContext != null) {
                    factory = sslContext.getSocketFactory();
                }

                if (factory != null) {
                    mWebSocketClient.setSocket(factory.createSocket());
                }
            } catch (NoSuchAlgorithmException | KeyManagementException | IOException e) {
                e.printStackTrace();
            }
        }

        mWebSocketClient.connect();
        mHandler.postDelayed(heartBeatRunnable, heartbeatInterval);//开启心跳检测
    }


    private void initSocketClient() {
        if (wss == null) {
            return;
        }
        if (mWebSocketClient != null) {
            return;
        }
        URI uri = null;
        try {
            uri = new URI(wss);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        if (uri == null) {
            return;
        }
        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                lastHealth = System.currentTimeMillis();
                isOpen = true;
                Log.e(TAG, "isFirstOpen: " + isFirstOpen);

                if (isFirstOpen) {
                    isFirstOpen = false;
                    events.onWebSocketOpen();
                } else {
                    //重连的;
                    events.onReconnect();
                }
                Log.e(TAG, "onOpen: " + handshake.getHttpStatus());

            }

            @Override
            public void onMessage(String message) {
                lastHealth = System.currentTimeMillis();
                isOpen = true;
                Log.d(TAG, message);
                if (!message.equals(heartbeatSMessage)) {
                    handleMessage(message);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.e(TAG, "onClose:" + reason);
                if (events != null && isFirstOpen) {
                    events.onWebSocketOpenFailed(reason);
                }
            }

            @Override
            public void onError(Exception ex) {
                Log.e(TAG, "onError:" + ex.toString());
                if (events != null && isFirstOpen) {
                    events.onWebSocketOpenFailed(ex.toString());
                }
            }
        };
    }

    //心跳检测
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (lastHealth != -1 && ((System.currentTimeMillis() - lastHealth) > heartbeatIdleTime)) {

                Log.e(TAG, "服务器没有响应，进入重连");
                reconnectWs();

                //此时应该触发关闭，然后进入重连
//                if (mWebSocketClient != null) {
//                    mWebSocketClient.close();
////                //如果client已为空，重新初始化连接
//                    mWebSocketClient = null;
//                }


            } else {
                if (mWebSocketClient != null) {
                    if (mWebSocketClient.isClosed()) {
                        Log.e(TAG, "连接在正常下，已断开， 可能断网了，可能是通话结束! ");
//                        mHandler.removeMessages(0);
//                        mWebSocketClient = null;
                        reconnectWs();

                    } else {
                        // 判断发送是否结束
                        Log.d(TAG, "send--> " + heartbeatCMessage);
                        mWebSocketClient.send(heartbeatCMessage);
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
        if (mWebSocketClient != null) {
            mWebSocketClient.close();
//                //如果client已为空，重新初始化连接
            mWebSocketClient = null;
        }

        mHandler.removeCallbacks(heartBeatRunnable);
        mHandler.postDelayed(() -> {
            Log.e(TAG, "开启重连=======" + wss);
            connect(wss);
        }, reconnectTime);
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    public void close() {
        if (mWebSocketClient != null) {
            mWebSocketClient.close();
        }

    }


    //============================需要发送的=====================================
    @Override
    public void joinRoom(String room) {
        // {"ct":"skyrtc","ac":"join_room","room_id":"232343"}
        Map<String, Object> map = new HashMap<>();
        map.put("ct", "skyrtc");
        map.put("ac", "join_room");
        map.put("room_id", room);
//        Map<String, String> childMap = new HashMap<>();
//        childMap.put("room", room);
//        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        mWebSocketClient.send(jsonString);
    }

    public void sendAnswer(String socketId, String sdp, String room_id) {
        Map<String, Object> childMap1 = new HashMap();
        childMap1.put("type", "answer");
        childMap1.put("sdp", sdp);
//        HashMap<String, Object> childMap2 = new HashMap();
        childMap1.put("socketId", socketId);
//        childMap1.put("sdp", childMap1);
//        HashMap<String, Object> map = new HashMap();
//        map.put("eventName", "answer");
        childMap1.put("ct", "skyrtc");
        childMap1.put("ac", "answer");
        childMap1.put("room_id", room_id);

//        map.put("data", childMap2);
        JSONObject object = new JSONObject(childMap1);
        String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        mWebSocketClient.send(jsonString);
    }


    public void sendOffer(String socketId, String sdp, String room_id) {
        HashMap<String, Object> childMap1 = new HashMap();
        childMap1.put("type", "offer");
        childMap1.put("sdp", sdp);

//        HashMap<String, Object> childMap2 = new HashMap();
        childMap1.put("socketId", socketId);
        childMap1.put("room_id", room_id);
//        childMap1.put("sdp", childMap1);

//        HashMap<String, Object> map = new HashMap();
//        map.put("eventName", "offer");
        childMap1.put("ct", "skyrtc");
        childMap1.put("ac", "offer");
//        map.put("data", childMap2);

        JSONObject object = new JSONObject(childMap1);
        String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        mWebSocketClient.send(jsonString);

    }

    public void sendIceCandidate(String socketId, IceCandidate iceCandidate, String room_id) {
        HashMap<String, Object> childMap = new HashMap();
        childMap.put("id", iceCandidate.sdpMid);
        childMap.put("label", iceCandidate.sdpMLineIndex);
        childMap.put("candidate", iceCandidate.sdp);
        childMap.put("socketId", socketId);
        childMap.put("room_id", room_id);

//        HashMap<String, Object> map = new HashMap();
//        map.put("eventName", "ice_candidate");
        childMap.put("ct", "skyrtc");
        childMap.put("ac", "ice_candidate");
//        childMap.put("data", childMap);
        JSONObject object = new JSONObject(childMap);
        String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        mWebSocketClient.send(jsonString);
    }
    //============================需要发送的=====================================


    //============================需要接收的=====================================
    @Override
    public void handleMessage(String message) {
        Map map = JSON.parseObject(message, Map.class);
        String eventName = (String) map.get("eventName");
        if (eventName == null) return;
        if (eventName.equals("_peers")) {
            handleJoinToRoom(map);
        }
        if (eventName.equals("_new_peer")) {
            handleRemoteInRoom(map);
        }
        if (eventName.equals("_ice_candidate")) {
            handleRemoteCandidate(map);
        }
        if (eventName.equals("_remove_peer")) {
            handleRemoteOutRoom(map);
        }
        if (eventName.equals("_offer")) {
            handleOffer(map);
        }
        if (eventName.equals("_answer")) {
            handleAnswer(map);
        }
    }

    // 自己进入房间
    private void handleJoinToRoom(Map map) {
        Map data = (Map) map.get("data");
        JSONArray arr;
        if (data != null) {
            arr = (JSONArray) data.get("connections");
            String js = JSONObject.toJSONString(arr, SerializerFeature.WriteClassName);
            ArrayList<String> connections = (ArrayList<String>) JSONObject.parseArray(js, String.class);
            String myId = (String) data.get("youSocketId");
            events.onJoinToRoom(connections, myId);
        }

    }

    // 自己已经在房间，有人进来
    private void handleRemoteInRoom(Map map) {
        Map data = (Map) map.get("data");
        String socketId;
        if (data != null) {
            socketId = (String) data.get("socketId");
            events.onRemoteJoinToRoom(socketId);
        }

    }

    // 处理交换信息
    private void handleRemoteCandidate(Map map) {
        Map data = (Map) map.get("data");
        String socketId;
        if (data != null) {
            socketId = (String) data.get("socketId");
            String sdpMid = (String) data.get("id");
            sdpMid = (null == sdpMid) ? "video" : sdpMid;
            int label = (int) Double.parseDouble(String.valueOf(data.get("label")));
            String candidate = (String) data.get("candidate");
            IceCandidate iceCandidate = new IceCandidate(sdpMid, label, candidate);
            events.onRemoteIceCandidate(socketId, iceCandidate);
        }


    }

    // 有人离开了房间
    private void handleRemoteOutRoom(Map map) {
        Map data = (Map) map.get("data");
        String socketId;
        if (data != null) {
            socketId = (String) data.get("socketId");
            events.onRemoteOutRoom(socketId);
        }

    }

    // 处理Offer
    private void handleOffer(Map map) {
        Map data = (Map) map.get("data");
//        Map sdpDic;
        if (data != null) {
//            sdpDic = (Map) data.get("sdp");
            String socketId = (String) data.get("socketId");
            String sdp = (String) data.get("sdp");
            events.onReceiveOffer(socketId, sdp);
        }

    }

    // 处理Answer
    private void handleAnswer(Map map) {
        Map data = (Map) map.get("data");
//        Map sdpDic;
        if (data != null) {
//            sdpDic = (Map) data.get("sdp");
            String socketId = (String) data.get("socketId");
            String sdp = (String) data.get("sdp");
            events.onReceiverAnswer(socketId, sdp);
        }

    }
    //============================需要接收的=====================================


    // 忽略证书
    public static class TrustManagerTest implements X509TrustManager {

        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }


}
