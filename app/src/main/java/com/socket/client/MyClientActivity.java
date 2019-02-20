package com.socket.client;

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

import com.socket.client.util.WifiInfoUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MyClientActivity extends AppCompatActivity {

    private static final String TAG = "MyClientActivity";
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
    public void onCreate(Bundle savedInstanceState) {
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
                isKeepHeartBeat = true;
                connectService();
            }
        });

    }

    private void connectService() {
        try {
            ipStr = "192.168.43.153";
            socket = new Socket(ipStr, 9999);
            Log.d(TAG, "与服务器建立连接：" + socket);
            String str = "与服务器建立连接：" + socket;
//            Toast.makeText(MyClientActivity.this, str, Toast.LENGTH_LONG).show();
            Log.d(TAG, "connectService, socket.isConnected ： " + socket.isConnected());
            if (socket.isConnected()) {
                keepHeartBeat();
//                recvProtobufMsg();
//            recvStringMsg();
            }
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
    public void sendToServiceMessage() {
        try {
            // socket.getInputStream()
            Log.d(TAG, "sendToServiceMessage: socket = " + socket);
            Log.d(TAG, "sendToServiceMessage: socket.isConnected = " + socket.isConnected());
            Log.d(TAG, "sendToServiceMessage: socket.isInputShutdown = " + socket.isInputShutdown());
            Log.d(TAG, "sendToServiceMessage: socket.isOutputShutdown = " + socket.isOutputShutdown());
            Log.d(TAG, "sendToServiceMessage: socket.isClosed = " + socket.isClosed());
//            socket.setKeepAlive(true);
            DataOutputStream writer = new DataOutputStream(socket.getOutputStream());
            Log.d(TAG, "sendToServiceMessage: writer = " + writer);
            String sendStr = mSendMessageEdit.getText().toString();
            Log.d(TAG, "sendToServiceMessage: sendStr = " + sendStr);
            if (TextUtils.isEmpty(sendStr)) {
                writer.writeUTF("嘿嘿，你好啊，服务器.."); // 写一个UTF-8的信息
            } else {
                writer.writeUTF(sendStr); // 写一个UTF-8的信息
            }
            Toast.makeText(MyClientActivity.this, "发送消息", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initView(final Socket socket) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DataInputStream reader;
                try {
                    // 获取读取流
                    reader = new DataInputStream(socket.getInputStream());
                    Log.d(TAG, "*等待服务端输入*");
                    // 读取数据
                    String msg = reader.readUTF();
                    Log.d(TAG, "获取到服务端的信息：" + msg);
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

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                String info = msg.obj.toString();
                mReceveMesFromServicerText.setText("这是来自服务器的数据:" + msg.obj.toString());
                Toast.makeText(MyClientActivity.this, "收到服务端消息  " + info, Toast.LENGTH_LONG).show();
            }
        }
    };

    //开启心跳检测
    boolean isKeepHeartBeat = false;

    private final int HEARTBEART_PERIOD = 6 * 1000;
    ScheduledExecutorService executor;//定位定时器
    HeartBeatTask mHeartBeatTask;
    /**
     * 心跳维护
     */
    private void keepHeartBeat() {
        //设置心跳频率，启动心跳
        Log.d(TAG, "keepHeartBeat: isKeepHeartBeat = " + isKeepHeartBeat);
        if (isKeepHeartBeat) {
            if (mHeartBeatTask == null) {
                mHeartBeatTask = new HeartBeatTask();
            }
            try {
                if (executor != null) {
                    executor.shutdownNow();
                    executor = null;
                }
                executor = Executors.newScheduledThreadPool(1);
                executor.scheduleAtFixedRate(
                        mHeartBeatTask,
                        1000,  //initDelay
                        HEARTBEART_PERIOD,  //period
                        TimeUnit.MILLISECONDS);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    class HeartBeatTask implements Runnable {
        @Override
        public void run() {
            //执行发送心跳
            try {
                Log.d(TAG, "HeartBeatTask, run: sendUrgentData ");
                socket.sendUrgentData(65);
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    Log.e(TAG, "socket心跳异常，尝试断开，重连");
                    socket.close();
                    socket = null;
                    //然后尝试重连
                    connectService();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            Log.e(TAG, "HeartBeatTask, 发送心跳，Socket.isClosed() = " + socket.isClosed() + "; connect = " + socket.isConnected());
        }
    }
}
