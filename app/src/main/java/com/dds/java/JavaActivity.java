package com.dds.java;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.dds.java.socket.IUserState;
import com.dds.java.socket.SocketManager;
import com.dds.java.voip.CallSingleActivity;
import com.dds.java.voip.VoipEvent;
import com.dds.skywebrtc.SkyEngineKit;
import com.dds.skywebrtc.permission.Permissions;
import com.dds.webrtc.R;

/**
 * 拨打电话界面
 */
public class JavaActivity extends AppCompatActivity implements IUserState {

    //    private EditText wss;
    private EditText et_name;
    Spinner spinner;
    private TextView user_state;
    private TextView tv_filepath;
    public static int FILE_REQUSET_CODE = 156;
    public String[] FILE_TYPES = {"txt", "apk", "jpg", "gif", "png", "bmp", "webp", "mp4", "zip", "rar", "gz", "bz2", "xls", "xlsx", "pdf", "doc", "docx", "amr", "jpeg", "mp3", "rtf", "mov", "pptx", "ppt", "numbers", "key", "pages"};


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
//        wss = findViewById(R.id.et_wss);
        spinner = findViewById(R.id.spinner);

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
//        wss.setText("wss://webrtcnodeali.aoidc.net:1443/ws?ct=skyrtc");
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
                spinner.getSelectedItem().toString().trim(),
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
                Uri uri = data.getData();
                if (uri != null) {
                    String path = FileUtils.getPath(this, uri);
                    tv_filepath.setText(path);
                    showDialog(path);
                }

            }
        }
    }

    private void showDialog(String filePath) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("确定发送该文件？")
                .setPositiveButton("确定", (dialog, which) -> {
                    String phone = ((TextView) findViewById(R.id.et_phone)).getText().toString().trim();
                    SocketManager.getInstance().onSendFile(phone, filePath);
                })
                .setNegativeButton("取消", (dialog, which) -> {

                })
                .create()
                .show();
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
//                FilePicker.from(this).chooseForBrowser().isSingle()
//                        .requestCode(FILE_REQUSET_CODE).setFileTypes(FILE_TYPES)
//                        .start();
                // 调用系统文件管理器
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(Intent.createChooser(intent, "Choose File"), FILE_REQUSET_CODE);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "亲，木有文件管理器啊-_-!!", Toast.LENGTH_SHORT).show();
                }
            } else {
                // 权限拒绝
                Toast.makeText(this, "权限已拒绝", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
