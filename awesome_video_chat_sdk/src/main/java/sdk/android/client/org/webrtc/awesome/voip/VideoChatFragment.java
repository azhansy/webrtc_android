package org.webrtc.awesome.voip;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;

import org.webrtc.R;
import org.webrtc.awesome.CallSession;
import org.webrtc.awesome.EnumType;
import org.webrtc.awesome.PeerStateListener;
import org.webrtc.awesome.SkyEngineKit;

/**
 * @Author: hsh
 * @Description: 视频聊天界面
 * @CreateDate: 2020-05-22
 */
public class VideoChatFragment extends Fragment  implements CallSession.CallSessionCallback, View.OnClickListener, PeerStateListener {

    private SkyEngineKit gEngineKit;
    private CallSingleActivity activity;
    private boolean isOutgoing;
    private boolean isFromFloatingView;
    private TextView tvCallingState;
    private TextView tvCallingName;
    private Chronometer tvCallingTime;
    private ImageView ivCallingAudio;
    private ImageView ivCallingCancel;
    private Group groupCalling;
    private TextView tvCalledName;
    private TextView tvCalledState;
    private ImageView ivCalledCancel;
    private ImageView ivCalledAudio;
    private ImageView ivCalledAnswer;
    private Group groupCalled;
    private TextView tvChatName;
    private TextView tvChatState;
    private Chronometer tvChatTime;
    private ImageView ivRevered;
    private ImageView ivSmallWindow;
    private FrameLayout pipVideoView;
    private ImageView ivChatAudio;
    private ImageView ivChatCancel;
    private ImageView ivChatMute;
    private Group groupChat;
    private FrameLayout fullVideoView;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (CallSingleActivity) getActivity();
        if (activity != null) {
            isOutgoing = activity.isOutgoing();
            isFromFloatingView = activity.isFromFloatingView();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_chat, container, false);
        initView(view);
        init();
        return view;
    }

    private void init() {
        gEngineKit = activity.getEngineKit();
        CallSession session = gEngineKit.getCurrentSession();
        if (session == null || EnumType.CallState.Idle == session.getState()) {
            activity.finish();
            return;
        } else if (EnumType.CallState.Connected == session.getState()) {
            groupCalling.setVisibility(View.GONE);
            groupCalled.setVisibility(View.GONE);
            groupChat.setVisibility(View.VISIBLE);
        } else {
            if (isOutgoing) {
                groupCalling.setVisibility(View.VISIBLE);
                groupCalled.setVisibility(View.GONE);
                groupChat.setVisibility(View.GONE);
                tvCallingState.setText(R.string.av_waiting);
            } else {
                groupCalling.setVisibility(View.GONE);
                groupCalled.setVisibility(View.VISIBLE);
                groupChat.setVisibility(View.GONE);
                tvCalledState.setText(R.string.av_video_invite);
            }
        }

        if (isFromFloatingView) {
            didCreateLocalVideoTrack();
            didReceiveRemoteVideoTrack();
        }

        session.setPeerStateListener(this);
    }

    private void startRefreshTime() {
        CallSession session = SkyEngineKit.Instance().getCurrentSession();
        if (session == null) {
            return;
        }
        if (tvCallingTime != null) {
            tvCallingTime.setVisibility(View.VISIBLE);
            tvCallingTime.setBase(SystemClock.elapsedRealtime() - (System.currentTimeMillis() - session.getStartTime()));
            tvCallingTime.start();
        }
    }

    private void initView(View view) {
        tvCallingState = view.findViewById(R.id.tv_state);
        tvCallingName = view.findViewById(R.id.tv_name);
        tvCallingTime = view.findViewById(R.id.tv_time);
        ivCallingAudio = view.findViewById(R.id.iv_audio);
        ivCallingCancel = view.findViewById(R.id.iv_cancel);
        groupCalling = view.findViewById(R.id.gp_calling);
        fullVideoView = view.findViewById(R.id.fullscreen_video_view);

        tvCalledName = view.findViewById(R.id.tv_called_name);
        tvCalledState = view.findViewById(R.id.tv_called_state);
        ivCalledCancel = view.findViewById(R.id.iv_called_cancel);
        ivCalledAudio = view.findViewById(R.id.iv_called_audio);
        ivCalledAnswer = view.findViewById(R.id.iv_called_answer);
        groupCalled = view.findViewById(R.id.gp_called);

        tvChatName = view.findViewById(R.id.tv_chat_name);
        tvChatState = view.findViewById(R.id.tv_chat_state);
        tvChatTime = view.findViewById(R.id.tv_chat_time);
        ivRevered = view.findViewById(R.id.iv_revered);
        ivSmallWindow = view.findViewById(R.id.iv_small_window);
        pipVideoView = view.findViewById(R.id.pip_video_view);
        ivChatAudio = view.findViewById(R.id.iv_chat_audio);
        ivChatCancel = view.findViewById(R.id.iv_chat_cancel);
        ivChatMute = view.findViewById(R.id.iv_chat_mute);
        groupChat = view.findViewById(R.id.gp_chat);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void didCallEndWithReason(EnumType.CallEndReason var1) {

    }

    @Override
    public void didChangeState(EnumType.CallState var1) {

    }

    @Override
    public void didChangeMode(boolean isAudioOnly) {

    }

    @Override
    public void didCreateLocalVideoTrack() {

    }

    @Override
    public void didReceiveRemoteVideoTrack() {

    }

    @Override
    public void didError(String error) {

    }

    @Override
    public void onDisconnect() {

    }

    @Override
    public void onReconnecting() {

    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onConnectedFail() {

    }

    @Override
    public void onConnecting() {

    }
}
