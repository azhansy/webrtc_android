package com.dds.java;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.dds.java.socket.IUserState;
import com.dds.java.socket.SocketManager;
import com.dds.java.voip.CallSingleActivity;
import com.dds.java.voip.VoipEvent;
import com.dds.skywebrtc.SkyEngineKit;
import com.dds.skywebrtc.permission.Permissions;
import com.dds.webrtc.R;
import com.ess.filepicker.FilePicker;
import com.ess.filepicker.model.EssFile;
import com.ess.filepicker.util.Const;

import java.util.ArrayList;
import java.util.List;

/**
 * 拨打电话界面
 */
public class JavaActivity extends AppCompatActivity implements IUserState {

    private EditText wss;
    private EditText et_name;
    private TextView user_state;
    private TextView tv_filepath;
    public static int FILE_REQUSET_CODE = 156;
    public  String[] FILE_TYPES = {"txt", "apk", "jpg", "gif", "png", "bmp", "webp", "mp4", "zip", "rar", "gz", "bz2", "xls", "xlsx", "pdf", "doc", "docx", "amr", "jpeg", "mp3", "rtf", "mov", "pptx", "ppt", "numbers", "key", "pages"};


    // 0 phone 1 pc
    private int device;

    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_java);
        initView();
        initData();

    }


    private void initView() {
        wss = findViewById(R.id.et_wss);
        et_name = findViewById(R.id.et_name);
        user_state = findViewById(R.id.user_state);
        tv_filepath = findViewById(R.id.tv_filepath);
        RadioGroup deviceRadioGroup = findViewById(R.id.device);
        deviceRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.phone) {
                device = 0;
            } else if (checkedId == R.id.pc) {
                device = 1;
            }

        });
    }

    private void initData() {
        wss.setText("wss://webrtcnodeali.aoidc.net:1443/ws");
//        wss.setText("ws://192.168.1.138:5000/ws");
        SocketManager.getInstance().addUserStateCallback(this);
        int userState = SocketManager.getInstance().getUserState();
        if (userState == 1) {
            loginState();
        } else {
            logoutState();
        }


    }

    // 登录
    public void connect(View view) {
        SocketManager.getInstance().connect(
                wss.getText().toString().trim(),
                et_name.getText().toString().trim(),
                device);

    }

    // 退出
    public void unConnect(View view) {
        SocketManager.getInstance().unConnect();
    }

    // 拨打语音
    public void call(View view) {
        String phone = ((TextView) findViewById(R.id.et_phone)).getText().toString().trim();
        SkyEngineKit.init(new VoipEvent());
        CallSingleActivity.openActivity(this, phone, true, true);

    }

    // 拨打视频
    public void callVideo(View view) {
        String phone = ((TextView) findViewById(R.id.et_phone)).getText().toString().trim();
        SkyEngineKit.init(new VoipEvent());
        CallSingleActivity.openActivity(this, phone, true, false);
    }

    @Override
    public void userLogin() {
        handler.post(this::loginState);
    }

    @Override
    public void userLogout() {
        handler.post(this::logoutState);
    }

    //--------------------------------------------------------------------------------------

    public void loginState() {
        user_state.setText("用户登录状态：已登录");
        user_state.setTextColor(ContextCompat.getColor(JavaActivity.this, android.R.color.holo_red_light));
    }

    public void logoutState() {
        user_state.setText("用户登录状态：未登录");
        user_state.setTextColor(ContextCompat.getColor(JavaActivity.this, android.R.color.darker_gray));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == FILE_REQUSET_CODE && data != null) {
                List<EssFile> files = data.getParcelableArrayListExtra(Const.EXTRA_RESULT_SELECTION);
                if (!files.isEmpty()) {
                    String path = files.get(0).getAbsolutePath();
                    tv_filepath.setText(path);
                    SocketManager.getInstance().onSendFile(path);
                }

            }
        }
    }


    /**
     * 选择文件
     *
     * @param view
     */
    public void chooseFile(View view) {
        // 权限检测
        String[] per = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        Permissions.request(this, per, integer -> {
            if (integer == 0) {
                // 权限同意
                FilePicker.from(this).chooseForBrowser().isSingle()
                        .requestCode(FILE_REQUSET_CODE).setFileTypes(FILE_TYPES)
                        .start();
            } else {
                // 权限拒绝
                Toast.makeText(this, "权限已拒绝", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
