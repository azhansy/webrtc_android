package org.webrtc.awesome.api;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import org.webrtc.awesome.SkyEngineKit;
import org.webrtc.awesome.socket.IUserState;
import org.webrtc.awesome.socket.SignalServerStateListener;
import org.webrtc.awesome.socket.SocketManager;
import org.webrtc.awesome.voip.CallSingleActivity;
import org.webrtc.awesome.voip.SkyWebrtcUtils;
import org.webrtc.awesome.voip.VoipEvent;

/**
 * @Author: hsh
 * @Description: 视频聊天基础实现类
 * @CreateDate: 2020-05-17
 */

class BaseAweWebRctImpl implements AweWebRtc {

    BaseAweWebRctImpl(@NonNull Context context) {
        SkyWebrtcUtils.init(context.getApplicationContext());
    }

    @Override
    public void login(@NonNull String signalServer, @NonNull String userId) {
        SocketManager.getInstance().connect(signalServer, userId);
    }

    @Override
    public void logout() {
        SocketManager.getInstance().unConnect();
    }

    @Override
    public void startVideoChat(@NonNull Context context, @NonNull String dstUserId) {
        if (checkPermission(context)) {
            return;
        }
        SkyEngineKit.init(new VoipEvent());
        CallSingleActivity.openActivity(context, dstUserId, true, false);
    }

    private boolean checkPermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (!notificationManager.isNotificationPolicyAccessGranted()) {
                new AlertDialog.Builder(context)
                        .setTitle("权限通知")
                        .setMessage("需要授予免打扰权限操作")
                        .setPositiveButton("去授予", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent mIntent = new Intent(
                                        android.provider.Settings
                                                .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                                mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(mIntent);
                            }
                        }).show();
                return true;
            }
        }
        return false;
    }

    @Override
    public void startAudioChat(@NonNull Context context, @NonNull String dstUserId) {
        if (checkPermission(context)) {
            return;
        }
        SkyEngineKit.init(new VoipEvent());
        CallSingleActivity.openActivity(context, dstUserId, true, true);
    }

    @Override
    public void setIUserStateCallback(IUserState userState) {
        SocketManager.getInstance().addUserStateCallback(userState);
    }

    @Override
    public int getUserLoginState() {
        return SocketManager.getInstance().getUserState();
    }

    @Override
    public void addOnSignalServerStateChangeListener(SignalServerStateListener mListener) {

    }
}
