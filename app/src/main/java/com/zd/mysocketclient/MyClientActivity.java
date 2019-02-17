package com.zd.mysocketclient;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.zd.mysocketclient.util.WifiInfoUtil;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

public class MyClientActivity extends AppCompatActivity {
    private Socket socket;
    private TextView mWifiName;
    private TextView mIpAddress;
    private EditText mSendMessageEdit;
    private boolean mIsWifiEnable;
    private Button mSendBtn;
    private Button mStartConnectBtn;
    private String ipStr;
    private TextView mReceveMesFromServicerText;
    @Override
    public void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.client_activity);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        mWifiName = findViewById(R.id.wifi_name);
        mIpAddress = findViewById(R.id.ip_address);
        mSendMessageEdit = findViewById(R.id.message_edit);
        mIsWifiEnable = WifiInfoUtil.isWifiEnabled(this);
        mSendBtn = findViewById(R.id.send_message_toservice_btn);

        mStartConnectBtn = findViewById(R.id.start_connect_btn);

        mReceveMesFromServicerText = findViewById(R.id.receve_message_from_service__text);

        if (mIsWifiEnable) {
            String ssidStr = WifiInfoUtil.getSSID(this);
            if (!TextUtils.isEmpty(ssidStr)) {
                mWifiName.setText(ssidStr);
            }

            int wifiAdress = WifiInfoUtil.getWifiIp(this);
            if (wifiAdress != -1) {
                ipStr = WifiInfoUtil.intToIp(wifiAdress);
                mIpAddress.setText(ipStr);
            }
        }

//        initView();

        mSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendToServiceMessage();
            }
        });

        mStartConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectService();
            }
        });

    }



    private void connectService(){
        try {
//            socket = new Socket(ipStr, 9999);
            ipStr = "192.168.1.104";
            socket = new Socket(ipStr, 9999);
            Log.i("zhengdan", "与服务器建立连接：" + socket);
            String str = "与服务器建立连接：" + socket;
            Toast.makeText(MyClientActivity.this, str, Toast.LENGTH_LONG).show();
            initView(socket);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 发送消息给服务端
     */
    public void sendToServiceMessage(){
        try {
            // socket.getInputStream()
            DataOutputStream writer = new DataOutputStream(socket.getOutputStream());
            String sendStr = mSendMessageEdit.getText().toString();
            if(TextUtils.isEmpty(sendStr)){
                writer.writeUTF("嘿嘿，你好啊，服务器.."); // 写一个UTF-8的信息
            }else{
                writer.writeUTF(sendStr); // 写一个UTF-8的信息
            }
            Toast.makeText(MyClientActivity.this,"发送消息",Toast.LENGTH_LONG).show();
            System.out.println("发送消息");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void initView(final Socket socket){
        new Thread(new Runnable() {
            @Override
            public void run() {
                    DataInputStream reader;
                    try {
                        // 获取读取流
                        reader = new DataInputStream(socket.getInputStream());
                        Log.e("zhengdan", "*等待服务端输入*");
                        // 读取数据
                        String msg = reader.readUTF();
                        Log.e("zhengdan", "获取到服务端的信息：" + msg);
                        Message mMessage = new Message();
                        mMessage.what = 1;
                        mMessage.obj = msg;
                        handler.sendMessage(mMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }).start();

    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1){
                String info = msg.obj.toString();
                mReceveMesFromServicerText.setText("这是来自服务器的数据:"+msg.obj.toString());
                Toast.makeText(MyClientActivity.this, "收到服务端消息  " + info, Toast.LENGTH_LONG).show();
            }
        }
    };
}
