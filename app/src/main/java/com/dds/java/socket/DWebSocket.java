package com.dds.java.socket;

import android.annotation.SuppressLint;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.dds.skywebrtc.FileUtils;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
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
        Log.e("dds_error", "onClose:" + reason + "remote:" + remote + ",connectFlag:" + connectFlag);
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
        Log.e("dds_info", "onOpen,connectFlag=" + connectFlag);
        this.iEvent.onOpen();
        if (connectFlag) {
            //重连进入房间
//            this.iEvent.onReJoinRoom();
        }
        connectFlag = true;
    }

    @Override
    public void onMessage(String message) {
        Log.d(TAG, message);
        handleMessage(message);
    }


    @Override
    public void onMessage(ByteBuffer bytes) {
        Log.d(TAG, "ByteBuffer:" + bytes);

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
            return;

        }
        // 离开房间
//        if (eventName.equals("_leave_room")) {
//            handleLeave(map);
//        }
        if (eventName.equals("_remove_peer")) {
            handleLeave(map);
            return;

        }
        // 切换到语音
        if (eventName.equals("_trans_audio")) {
            handleTransAudio(map);
            return;

        }
        // 意外断开
//        if (eventName.equals("_disconnect")) {
//            handleDisConnect(map);
//            return;
//        }

//        if (eventName.equals("_send_file")) {
//            handleFile(map);
//            return;
//        }


    }

//    private void handleFile(Map map) {
//        Map data = (Map) map.get("data");
//        if (data != null) {
//            String byteString = (String) data.get("file_byte");
//            String fileName = (String) map.get("file_name");
//            String filePrefix = (String) map.get("file_prefix");
            //保存的文件名称，为传过来 加个copy名称做区分，后续可去掉
//            FileUtils.read2Io(byteString, "/storage/emulated/0/" + fileName + "_copy" + "." + filePrefix);
//        }
//    }

    private void handleDisConnect(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String user_id = (String) data.get("user_id");
            this.iEvent.onDisConnect(user_id);
        }
    }

    private void handleTransAudio(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String user_id = (String) data.get("user_id");
            this.iEvent.onTransAudio(user_id);
        }
    }

    private void handleLogin(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String user_id = (String) data.get("user_id");
            String avatar = (String) data.get("avatar");
            this.iEvent.loginSuccess(user_id, avatar);
        }


    }

    private void handleIceCandidate(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String user_id = (String) data.get("user_id");
            String id = (String) data.get("id");
            int label = (int) data.get("label");
            String candidate = (String) data.get("candidate");
            this.iEvent.onIceCandidate(user_id, id, label, candidate);
        }
    }

    private void handleAnswer(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String sdp = (String) data.get("sdp");
            String user_id = (String) data.get("user_id");
            this.iEvent.onAnswer(user_id, sdp);
        }
    }

    private void handleOffer(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String sdp = (String) data.get("sdp");
            String user_id = (String) data.get("user_id");
            this.iEvent.onOffer(user_id, sdp);
        }
    }

    private void handleReject(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String user_id = (String) data.get("user_id");
            int rejectType = (int) data.get("refuse_type");
            this.iEvent.onReject(user_id, rejectType);
        }
    }

    private void handlePeers(Map map) {
        Map data = (Map) map.get("data");
        JSONArray arr;

        if (data != null) {
            String user_id = (String) data.get("you");
            arr = (JSONArray) data.get("connections");
            String js = JSONObject.toJSONString(arr, SerializerFeature.WriteClassName);
            ArrayList<String> connections = (ArrayList<String>) JSONObject.parseArray(js, String.class);

            this.iEvent.onPeers(user_id, connections);
        }
    }

    private void handleNewPeer(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String user_id = (String) data.get("user_id");
            this.iEvent.onNewPeer(user_id);
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
            String user_id = (String) data.get("user_id");
            String to_user = (String) data.get("to_user");
            this.iEvent.onCancel(user_id);
        }
    }

    private void handleInvite(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String room = (String) data.get("room_id");
            int audioOnly = (int) data.get("audio_only");
            String user_id = (String) data.get("user_id");
            String to_user = (String) data.get("to_user");
            this.iEvent.onInvite(room, audioOnly == 1, user_id, to_user);
        }
    }

    private void handleLeave(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String user_id = (String) data.get("user_id");
            this.iEvent.onLeave(user_id);
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
        map.put("room_id", room);
//        map.put("roomSize", roomSize);
//        map.put("user_id", myId);

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
        map.put("room_id", room);
        map.put("audio_only", audioOnly ? 1 : 0);
//        map.put("user_id", myId);
        map.put("to_user", users);

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
//        map.put("user_id", useId);
        map.put("to_user", userList);


//        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // 发送响铃通知
    public void sendRing(String user_id, String to_user) {
        Map<String, Object> map = new HashMap<>();
//        map.put("eventName", "__ring");
        map.put("ct", "skyrtc");
        map.put("ac", "ring");
//        Map<String, Object> childMap = new HashMap<>();
//        map.put("user_id", user_id);
        map.put("to_user", to_user);


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
        map.put("ac", "join_room");
//        Map<String, String> childMap = new HashMap<>();
        map.put("room_id", room);
//        map.put("user_id", myId);
//        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // 拒接接听
    public void sendRefuse(String inviteID, String myId, int refuse_type) {
        Map<String, Object> map = new HashMap<>();
//        map.put("eventName", "__reject");
        map.put("ct", "skyrtc");
        map.put("ac", "reject");
//        Map<String, Object> childMap = new HashMap<>();
        map.put("to_user", inviteID);
//        map.put("user_id", myId);
        map.put("refuse_type", String.valueOf(refuse_type));

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
        map.put("ac", "leave_room");
//        Map<String, Object> childMap = new HashMap<>();
//        map.put("room_id", room);
//        map.put("user_id", myId);
//        map.put("to_user", userId);

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
        map.put("to_user", userId);
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
//        map.put("user_id", myId);
        map.put("to_user", userId);
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
        map.put("to_user", userId);
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
//        map.put("user_id", myId);
        map.put("to_user", userId);
//        map.put("data", childMap);
//        map.put("eventName", "__audio");

        map.put("ct", "skyrtc");
        map.put("ac", "trans_audio");
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // 断开重连
    public void sendDisconnect(String myId, String userId) {
        Map<String, Object> map = new HashMap<>();
//        Map<String, Object> childMap = new HashMap<>();
//        map.put("user_id", myId);
        map.put("to_user", userId);
//        map.put("data", childMap);
        map.put("ct", "skyrtc");
        map.put("ac", "disconnect");
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "isOpen()=" + isOpen() + ",send-->" + jsonString);
        if (isOpen()) {
            send(jsonString);
        }
    }

    public void sendFile(String userId, String path) {
//        Map<String, Object> map = new HashMap<>();
//        String byteString = FileUtils.read2String(path);
//        map.put("to_user", userId);
//        map.put("ct", "skyrtc");
//        map.put("ac", "send_file");
//        map.put("file_name", "test1");
//        map.put("file_prefix", "txt");
//        map.put("file_byte", byteString);
//        JSONObject object = new JSONObject(map);
//        final String jsonString = object.toString();
//        Log.d(TAG, "send--> " + jsonString);
//        send(jsonString);
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
