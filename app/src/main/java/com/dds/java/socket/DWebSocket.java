package com.dds.java.socket;

import android.annotation.SuppressLint;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.ess.filepicker.util.FileUtils;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.X509TrustManager;

/**
 * Created by dds on 2019/7/26.
 * android_shuai@163.com
 */
public class DWebSocket extends WebSocketClient {
    private final static String TAG = "dds_WebSocket";
    private IEvent iEvent;
    private boolean connectFlag = false;


    public DWebSocket(URI serverUri, IEvent event) {
        super(serverUri);
        this.iEvent = event;
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.e("dds_error", "onClose:" + reason + "remote:" + remote);
        if (connectFlag) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.iEvent.reConnect();
        } else {
            this.iEvent.logout("onClose");
        }

    }

    @Override
    public void onError(Exception ex) {
        Log.e("dds_error", "onError:" + ex.toString());
        this.iEvent.logout("onError");
        connectFlag = false;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.e("dds_info", "onOpen");
        this.iEvent.onOpen();
        connectFlag = true;
    }

    @Override
    public void onMessage(String message) {
        Log.d(TAG, message);
        handleMessage(message);
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        Log.d(TAG, "接收到buffer的文件: " + bytes.toString());
        try {
            FileChannel fc = new FileOutputStream("data.txt").getChannel();
            fc.write(bytes);
            fc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setConnectFlag(boolean flag) {
        connectFlag = flag;
    }

    /**
     * ---------------------------------------处理接收消息-------------------------------------
     */
    private void handleMessage(String message) {
        Map map = JSON.parseObject(message, Map.class);
        String eventName = (String) map.get("eventName");
        if (eventName == null) return;
        // 登录成功
        if (eventName.equals("_login_success")) {
            handleLogin(map);
            return;
        }
        // 被邀请
        if (eventName.equals("_invite")) {
            handleInvite(map);
            return;
        }
        // 取消拨出
        if (eventName.equals("_cancel")) {
            handleCancel(map);
            return;
        }
        // 响铃
        if (eventName.equals("_ring")) {
            handleRing(map);
            return;
        }
        // 进入房间
        if (eventName.equals("_peers")) {
            handlePeers(map);
            return;
        }
        // 新人入房间
        if (eventName.equals("_new_peer")) {
            handleNewPeer(map);
            return;
        }
        // 拒绝接听
        if (eventName.equals("_reject")) {
            handleReject(map);
            return;
        }
        // offer
        if (eventName.equals("_offer")) {
            handleOffer(map);
            return;
        }
        // answer
        if (eventName.equals("_answer")) {
            handleAnswer(map);
            return;
        }
        // ice-candidate
        if (eventName.equals("_ice_candidate")) {
            handleIceCandidate(map);
        }
        // 离开房间
        if (eventName.equals("_leave")) {
            handleLeave(map);
        }
        // 切换到语音
        if (eventName.equals("_audio")) {
            handleTransAudio(map);
        }
        // 意外断开
        if (eventName.equals("_disconnect")) {
            handleDisConnect(map);
        }


    }

    private void handleDisConnect(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String fromId = (String) data.get("fromID");
            this.iEvent.onDisConnect(fromId);
        }
    }

    private void handleTransAudio(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String fromId = (String) data.get("fromID");
            this.iEvent.onTransAudio(fromId);
        }
    }

    private void handleLogin(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String userID = (String) data.get("userID");
            String avatar = (String) data.get("avatar");
            this.iEvent.loginSuccess(userID, avatar);
        }


    }

    private void handleIceCandidate(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String userID = (String) data.get("userID");
            String id = (String) data.get("id");
            int label = (int) data.get("label");
            String candidate = (String) data.get("candidate");
            this.iEvent.onIceCandidate(userID, id, label, candidate);
        }
    }

    private void handleAnswer(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String sdp = (String) data.get("sdp");
            String userID = (String) data.get("fromID");
            this.iEvent.onAnswer(userID, sdp);
        }
    }

    private void handleOffer(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String sdp = (String) data.get("sdp");
            String userID = (String) data.get("fromID");
            this.iEvent.onOffer(userID, sdp);
        }
    }

    private void handleReject(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String socket_id = (String) data.get("socket_id");
            int rejectType = Integer.parseInt(String.valueOf(data.get("refuseType")));
            this.iEvent.onReject(socket_id, rejectType);
        }
    }

    private void handlePeers(Map map) {
        Map data = (Map) map.get("data");
        JSONArray arr;

        if (data != null) {
            String userID = (String) data.get("you");
            arr = (JSONArray) data.get("connections");
            String js = JSONObject.toJSONString(arr, SerializerFeature.WriteClassName);
            ArrayList<String> connections = (ArrayList<String>) JSONObject.parseArray(js, String.class);

            this.iEvent.onPeers(userID, connections);
        }
    }

    private void handleNewPeer(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String userID = (String) data.get("userID");
            this.iEvent.onNewPeer(userID);
        }
    }

    private void handleRing(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String fromId = (String) data.get("from_id");
            this.iEvent.onRing(fromId);
        }
    }

    private void handleCancel(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String socket_id = (String) data.get("socket_id");
//            String userList = (String) data.get("userList");
            this.iEvent.onCancel(socket_id);
        }
    }

    private void handleInvite(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String room = (String) data.get("room");
            int audioOnly = (int) data.get("audio_only");
            String inviteID = (String) data.get("inviteID");
            String userList = (String) data.get("userList");
            this.iEvent.onInvite(room, audioOnly == 1, inviteID, userList);
        }
    }

    private void handleLeave(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String fromID = (String) data.get("fromID");
            this.iEvent.onLeave(fromID);
        }
    }

    /**
     * ------------------------------发送消息----------------------------------------
     */
    public void createRoom(String room, int roomSize, String myId) {
        Map<String, Object> map = new HashMap<>();
//        map.put("eventName", "__create");
        map.put("ct", "skyrtc");
        map.put("ac", "join_room");
//        Map<String, Object> childMap = new HashMap<>();
        map.put("room", room);
//        map.put("roomSize", roomSize);
//        map.put("userID", myId);

//        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // 发送邀请
    public void sendInvite(String room, String myId, String users, boolean audioOnly) {
        Map<String, Object> map = new HashMap<>();
//        map.put("eventName", "__invite");
        map.put("ct", "skyrtc");
        map.put("ac", "invite");
//        Map<String, Object> childMap = new HashMap<>();
        map.put("room", room);
        map.put("audio_only", audioOnly ? 1 : 0);
//        map.put("inviteID", myId);
//        map.put("userList", users);

//        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    public void sendMeetingInvite(String room, String myId, String userId) {

    }

    // 取消邀请
    public void sendCancel(String useId, String userList) {
        Map<String, Object> map = new HashMap<>();
//        map.put("eventName", "__cancel");
        map.put("ct", "skyrtc");
        map.put("ac", "cancel");
//        Map<String, Object> childMap = new HashMap<>();
//        map.put("inviteID", useId);
//        map.put("userList", userList);


//        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // 发送响铃通知
    public void sendRing(String myId, String toId) {
        Map<String, Object> map = new HashMap<>();
//        map.put("eventName", "__ring");
        map.put("ct", "skyrtc");
        map.put("ac", "ring");
//        Map<String, Object> childMap = new HashMap<>();
        map.put("fromID", myId);
        map.put("toID", toId);


//        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    //加入房间
    public void sendJoin(String room, String myId) {
        Map<String, Object> map = new HashMap<>();
//        map.put("eventName", "__join");
        map.put("ct", "skyrtc");
        map.put("ac", "join");
//        Map<String, String> childMap = new HashMap<>();
        map.put("room", room);
        map.put("userID", myId);
//        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // 拒接接听
    public void sendRefuse(String inviteID, String myId, int refuseType) {
        Map<String, Object> map = new HashMap<>();
//        map.put("eventName", "__reject");
        map.put("ct", "skyrtc");
        map.put("ac", "reject");
//        Map<String, Object> childMap = new HashMap<>();
        map.put("toID", inviteID);
        map.put("fromID", myId);
        map.put("refuseType", String.valueOf(refuseType));

//        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // 离开房间
    public void sendLeave(String myId, String room, String userId) {
        Map<String, Object> map = new HashMap<>();
//        map.put("eventName", "__leave");
        map.put("ct", "skyrtc");
        map.put("ac", "leave");
//        Map<String, Object> childMap = new HashMap<>();
        map.put("room", room);
        map.put("fromID", myId);
        map.put("userID", userId);

//        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        if (isOpen()) {
            send(jsonString);
        }
    }

    // send offer
    public void sendOffer(String myId, String userId, String sdp) {
        Map<String, Object> map = new HashMap<>();
//        Map<String, Object> childMap = new HashMap<>();
        map.put("sdp", sdp);
        map.put("userID", userId);
        map.put("fromID", myId);
//        map.put("data", childMap);
//        map.put("eventName", "__offer");
        map.put("ct", "skyrtc");
        map.put("ac", "offer");
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // send answer
    public void sendAnswer(String myId, String userId, String sdp) {
        Map<String, Object> map = new HashMap<>();
//        Map<String, Object> childMap = new HashMap<>();
        map.put("sdp", sdp);
        map.put("fromID", myId);
        map.put("userID", userId);
//        map.put("data", childMap);
//        map.put("eventName", "__answer");
        map.put("ct", "skyrtc");
        map.put("ac", "answer");
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // send ice-candidate
    public void sendIceCandidate(String userId, String id, int label, String candidate) {
        Map<String, Object> map = new HashMap<>();
//        map.put("eventName", "__ice_candidate");
        map.put("ct", "skyrtc");
        map.put("ac", "ice_candidate");
//        Map<String, Object> childMap = new HashMap<>();
        map.put("userID", userId);
        map.put("id", id);
        map.put("label", label);
        map.put("candidate", candidate);

//        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        if (isOpen()) {
            send(jsonString);
        }
    }

    // 切换到语音
    public void sendTransAudio(String myId, String userId) {
        Map<String, Object> map = new HashMap<>();
//        Map<String, Object> childMap = new HashMap<>();
        map.put("fromID", myId);
        map.put("userID", userId);
//        map.put("data", childMap);
//        map.put("eventName", "__audio");

        map.put("ct", "skyrtc");
        map.put("ac", "audio");
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // 断开重连
    public void sendDisconnect(String myId, String userId) {
        Map<String, Object> map = new HashMap<>();
//        Map<String, Object> childMap = new HashMap<>();
        map.put("fromID", myId);
        map.put("userID", userId);
//        map.put("data", childMap);
        map.put("ct", "skyrtc");
        map.put("ac", "audio");
//        map.put("eventName", "__disconnect");
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    public void sendFile(String path) {
        byte[] bytes = FileUtils.readBytes(path);
        Log.d(TAG, "send--> " + Arrays.toString(bytes));
        send(bytes);
    }

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
