package com.dds.nodejs;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.dds.webrtc.R;


/**
 * Created by dds on 2018/11/7.
 * android_shuai@163.com
 */
public class NodejsActivity extends AppCompatActivity {
//    private EditText et_signal;
    private EditText et_room;
    Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nodejs);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initView();
        initVar();

    }

    private void initView() {
         spinner = findViewById(R.id.spinner);
        et_room = findViewById(R.id.et_room);
    }

    private void initVar() {
//        et_signal.setText("wss://webrtcnodeali.aoidc.net:1443");
//        et_signal.setText("wss://testwss.langrensha.game?ct=skyrtc");
        et_room.setText("232343");
    }

    /*-------------------------- nodejs版本服务器测试--------------------------------------------*/
    public void JoinRoomSingleVideo(View view) {
        WebrtcUtil.callSingle(this,
                spinner.getSelectedItem().toString(),
                et_room.getText().toString().trim(),
                true);
    }

    public void JoinRoom(View view) {
        WebrtcUtil.call(this, spinner.getSelectedItem().toString(), et_room.getText().toString().trim());

    }


}
