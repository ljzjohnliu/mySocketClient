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
import java.io.InputStream;
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
    private EditText ipEdt;
    private Button mSendBtn;
    private Button mStartConnectBtn;
    private String ipStr;
    private TextView mReceveMesFromServicerText;

    private String mIpStr;

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
        ipEdt = findViewById(R.id.ip_server);
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
                if (!TextUtils.isEmpty(ipEdt.getText())) {
                    connectService(ipEdt.getText().toString());
                } else {
                    Toast.makeText(MyClientActivity.this, "请输入服务端的ip地址再连接！", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void connectService(String ipStr) {
        try {
//            ipStr = "192.168.43.153";
            mIpStr = ipStr;
            socket = new Socket(mIpStr, 9999);
//            socket.setSoTimeout(50000);
            Log.d(TAG, "与服务器建立连接：" + socket);
            String str = "与服务器建立连接：" + socket;
//            Toast.makeText(MyClientActivity.this, str, Toast.LENGTH_LONG).show();
            Log.d(TAG, "connectService, socket.isConnected ： " + socket.isConnected());
            if (socket.isConnected()) {
//                keepHeartBeat();
//                recvProtobufMsg();
//            recvStringMsg();
            }
//            initView(socket);
//            initViewTest(socket);
            //模拟粘包情况下 分包
            initViewTest2(socket);
        } catch (UnknownHostException e) {
            Log.e(TAG, "connectService: UnknownHostException e = " + e);
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "connectService: IOException e = " + e);
            e.printStackTrace();
        }
    }

    /**
     * 发送消息给服务端
     */
    public void sendToServiceMessage() {
        try {
            Log.d(TAG, "sendToServiceMessage: socket = " + socket + ", socket.isConnected = " + socket.isConnected());
//            socket.setKeepAlive(true);
            DataOutputStream writer = new DataOutputStream(socket.getOutputStream());
            Log.d(TAG, "sendToServiceMessage: writer = " + writer);
            String sendStr = mSendMessageEdit.getText().toString();
            Log.d(TAG, "sendToServiceMessage: sendStr = " + sendStr);
            if (TextUtils.isEmpty(sendStr)) {
                // 写一个UTF-8的信息
                writer.writeUTF("嘿嘿，你好啊，服务器..");
            } else {
                // 写一个UTF-8的信息
                writer.writeUTF(sendStr);
            }
            Toast.makeText(MyClientActivity.this, "发送消息", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG, "sendToServiceMessage: e = " + e);
            e.printStackTrace();
        }
    }

    private void initView(final Socket socket) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DataInputStream reader;
                try {
                    while(true) {
                        // 获取读取流
                        reader = new DataInputStream(socket.getInputStream());
                        Log.d(TAG, "initView, *等待服务端输入*");
                        // 读取数据
                        String msg = reader.readUTF();
                        Log.d(TAG, "initView, 获取到服务端的信息：" + msg);
                        Message mMessage = new Message();
                        mMessage.what = 1;
                        mMessage.obj = msg;
                        handler.sendMessage(mMessage);
                    }

                } catch (IOException e) {
                    Log.d(TAG, "initView, run: e = " + e);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //用该方法接受数据可以复现粘包情况
    private void initViewTest(final Socket socket) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                while (true) {
                    try {
                        byte[] byteBuffer = new byte[50];
                        StringBuffer receivBuffer = new StringBuffer();
                        InputStream reader = socket.getInputStream();
                        count = reader.read(byteBuffer);
                        if (count > 0) {
                            receivBuffer.append(new String(byteBuffer, 0, count));
                            Log.d(TAG, "initViewTest, receive data from client:" + receivBuffer.toString());
                        }
                        count = 0;
                    } catch (IOException e) {
                        Log.e(TAG, "initViewTest, run: e = " + e);
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    //用该方法接受数据 实现分包
    private void initViewTest2(final Socket socket) {
        Log.d(TAG, "initViewTest2: socket = " + socket);
        new Thread(new Runnable() {
            public static final int PACKET_HEAD_LENGTH=2;//包头长度
            private volatile byte[] bytes = new byte[0];

            @Override
            public void run() {
                int count =0;
                while (true) {
                    Log.d(TAG, "run: --------------------1111111111----------------");
                    try {
                        InputStream reader = socket.getInputStream();
                        Log.d(TAG, "run: -----------------222222222-------------------");
                        if (bytes.length < PACKET_HEAD_LENGTH) {
                            Log.d(TAG, "run: -----------------22221111-------------------");
                            byte[] head = new byte[PACKET_HEAD_LENGTH - bytes.length];
                            Log.d(TAG, "run: -----------------222221122-------------------");
                            int couter = reader.read(head);
                            Log.d(TAG, "run: -----------------333333333-------------------couter = " + couter);
                            if (couter < 0) {
                                continue;
                            }
                            bytes = mergebyte(bytes, head, 0, couter);
                            Log.d(TAG, "run: -----------------4444444444-------------------couter = " + couter);
                            if (couter < PACKET_HEAD_LENGTH) {
                                continue;
                            }
                        }
                        // 下面这个值请注意，一定要取2长度的字节子数组作为报文长度，你懂得
                        byte[] temp = new byte[0];
                        temp = mergebyte(temp, bytes, 0, PACKET_HEAD_LENGTH);
                        String templength = new String(temp);
                        int bodylength = Integer.parseInt(templength);//包体长度
                        Log.d(TAG, "initViewTest2, receive 第" + count + "条消息：bodylength = " + bodylength);
                        Log.d(TAG, "initViewTest2, receive bytes.length = " + bytes.length + ", PACKET_HEAD_LENGTH = " + PACKET_HEAD_LENGTH);
                        if (bytes.length - PACKET_HEAD_LENGTH < bodylength) {//不够一个包
                            Log.d(TAG, "不够一个包  ");
                            byte[] body = new byte[bodylength + PACKET_HEAD_LENGTH - bytes.length];//剩下应该读的字节(凑一个包)
                            Log.d(TAG, "剩下应该读的字节(凑一个包) ");
                            int couter = reader.read(body);
                            Log.d(TAG, "doInBackground:  剩下应该读的字节(凑一个包) couter 长度" + couter);
                            if (couter < 0) {
                                Log.d(TAG, "doInBackground:  couter 小于零");
                                continue;
                            }
                            bytes = mergebyte(bytes, body, 0, couter);
                            Log.d(TAG, "doInBackground:  数据放入 bytes里, bytes.length = " + bytes.length);
                            Log.d(TAG, "doInBackground:  数据放入 bytes里, couter = " + couter + ", body.length = " + body.length);
                            if (couter < body.length) {
                                Log.d(TAG, "doInBackground:  couter 小于零");
                                continue;
                            }
                        }
                        byte[] body = new byte[0];
                        body = mergebyte(body, bytes, PACKET_HEAD_LENGTH, bytes.length);
                        Log.d(TAG, "initViewTest2, receive 第" + count + "条消息：body = " + new String(body));
                        count++;
                        Message mMessage = new Message();
                        mMessage.what = 1;
                        mMessage.obj = new String(body);
                        handler.sendMessage(mMessage);
                        bytes = new byte[0];
                    } catch (Exception e) {
                        Log.e(TAG, "initViewTest2, run: e = " + e);
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public byte[] mergebyte(byte[] a, byte[] b, int begin, int end) {
        byte[] add = new byte[a.length + end - begin];
        int i = 0;
        for (i = 0; i < a.length; i++) {
            add[i] = a[i];
        }
        for (int k = begin; k < end; k++, i++) {
            add[i] = b[k];
        }
        return add;
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

    private final int HEARTBEART_PERIOD = 30 * 1000;
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
                    connectService(mIpStr);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            Log.e(TAG, "HeartBeatTask, 发送心跳，Socket.isClosed() = " + socket.isClosed() + "; connect = " + socket.isConnected());
        }
    }
}
