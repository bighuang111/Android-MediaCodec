/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.ngs.vpcc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

class GLES3JNIView extends GLSurfaceView {
    private static final String TAG = "GLES3JNI";
    private static final boolean DEBUG = true;

    private static int m_width = 2048;
    private static int m_height = 2048;

    private static int frameBytes;
    private static byte[] yuv;
    private static byte[] rgb;

    //private static  int frameBytes = m_width*m_height*3/2;
    //private static byte[] yuv = new byte[frameBytes];
    //private  static  byte[] rgb = new byte[frameBytes*2];

    private static String IP = MainActivity.serverIP;
    private static int port = MainActivity.serverPort;

    //private static int FPS = 12;

    //private static byte[][] fpsYUV = new byte[FPS][frameBytes];

    private int screenWidth ;
    private int screenHeight ;

    private SurfaceHolder mHolder = null;

    private static boolean poseChanged = false;
    private static float[] newPose = new float[6];

    private Handler mhandler = null;
    private static final int  SEND_TOUCH_POS_DATA = 102;
    private SendPosThread sendTread;

    public GLES3JNIView(Context context) {
        super(context);

        screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        screenHeight = context.getResources().getDisplayMetrics().heightPixels;

        m_width = screenWidth;
        m_height = screenHeight;

        //m_width = 2048;
        //m_height = 2048;


        frameBytes = m_width*m_height*3/2;
        yuv = new byte[frameBytes];
        rgb = new byte[frameBytes*2];

        // Pick an EGLConfig with RGB8 color, 16-bit depth, no stencil,
        // supporting OpenGL ES 2.0 or later backwards-compatible versions.
        setEGLConfigChooser(8, 8, 8, 0, 16, 0);
        setEGLContextClientVersion(3);

        mHolder = this.getHolder();

        this.setFocusable(true);
        this.setClickable(true);


        setRenderer(new Renderer(mHolder,screenWidth,screenHeight));



        this.setOnTouchListener(new TouchScreenListener() {
            @Override
            public void onTouchAction(float[] pos, String posString) {
                refreshTouchPosData(pos);
                sendTouchPosData(posString);
            }
        });


        sendTread = new SendPosThread();
        sendTread.start();
    }

    private void sendTouchPosData(String str)
    {
        if(mhandler == null)
            return;


        Message msg = mhandler.obtainMessage();
        msg.what = SEND_TOUCH_POS_DATA;
        msg.obj = str;
        mhandler.sendMessage(msg);
    }

    private void refreshTouchPosData(float[] position)
    {

        newPose[0] = position[3];
        newPose[1] = position[4];
        newPose[2] = position[5];
        newPose[3] = position[0];
        newPose[4] = position[1];
        newPose[5] = position[2];
        poseChanged = true;
    }

    public class SendPosThread extends Thread {
        private String mAddress;
        private DatagramSocket mSocket;
        private int mPort;
        private InetAddress mServerAddress;

        private static final int SEND_POS_DATA_TO_SERVER = 101;

        public SendPosThread() {
            this.mAddress = IP;
            this.mPort = 27000;
            startSocket(mAddress,mPort);
        }

        public void startSocket(String address, int port) {
            try {
                mServerAddress = InetAddress.getByName(address);
                mSocket = new DatagramSocket(port);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        private void stopSocket(){
            if(mSocket != null ){
                mSocket.close();
                mSocket = null;
            }
        }

        @Override
        public void run() {
            //super.run();
            Looper.prepare();
            mhandler = new Handler() {

                @Override
                public void handleMessage(Message msg) {
                    if(sendTread.isInterrupted()){
                        stopSocket();
                        return;
                    }
                    if(msg == null || mSocket == null)
                    {
                        return;
                    }

                    switch(msg.what){
                        case SEND_TOUCH_POS_DATA :
                        {
                            try {
                                String posString = msg.obj.toString();
                                byte[] data = posString.getBytes();
                                Log.d("UdpSendPosThread", "HandleMessage obj = " + posString);
                                DatagramPacket packet = new DatagramPacket(data, data.length, mServerAddress, mPort);
                                mSocket.send(packet);
                                //Thread.sleep(1000);
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                        default:
                            break;
                    }

                }
            };
            Looper.loop();
        }
    }

    private static class Renderer implements GLSurfaceView.Renderer {
        private int screenWidth;
        private int screenHight;
        private SurfaceHolder m_holder;
        private SocketThread mSocketThread = null;

        private boolean oneFrameReceived = false;

        private boolean isRunning = false;


        public Renderer(SurfaceHolder holder, int w, int h) {
            screenWidth = w;
            screenHight = h;
            m_holder = holder;

        }

        public void onDrawFrame(GL10 gl) {

            //GLES3JNILib.step();

            if(oneFrameReceived)
                GLES3JNILib.yuvToRGB(yuv, rgb,  m_width,m_height);


          }

        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES3JNILib.resize(width, height);
        }

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {

            GLES3JNILib.init();

            GLES3JNILib.initDisplay(m_width, m_height, screenWidth, screenHight);

            if (null == mSocketThread) {
                mSocketThread = new SocketThread();
                mSocketThread.start();

            }

        }


        private class SocketThread extends Thread {

            Socket socket = null;

            String ServerIP = IP;
            int ServerPort = port;

            //String ServerIP ="10.238.225.69";
            //String ServerIP ="172.16.113.191";
            //String ServerIP = "192.168.1.2";
            //int ServerPort = 9099;

            InputStream in = null;
            OutputStream out = null;


            public SocketThread() {

                socket = new Socket();
                connectSocket();

                try {
                    in = socket.getInputStream();
                    out = socket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                isRunning = true;
            }

            public void connectSocket(){

                try {
                    socket.connect(new InetSocketAddress(ServerIP, ServerPort));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            public void sendControlMsg(byte[] msg){
                try {
                    out.write(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            public byte[] constructMessageBody(int[] resolution, float[] focal, float[] center, float[] position, float[] rotation){
                byte[] messageBody;

                messageBody = addBytes(IntToByte(resolution[0]),IntToByte(resolution[1]));

                byte[] tempFocal = addBytes(float2Byte(focal[0]),float2Byte(focal[1]));
                messageBody = addBytes(messageBody,tempFocal);

                byte[] tempCenter = addBytes(float2Byte(center[0]),float2Byte(center[1]));
                messageBody = addBytes(messageBody,tempCenter);


                byte[] tempPosition = addBytes(float2Byte(position[0]),float2Byte(position[1]));
                tempPosition = addBytes(tempPosition,float2Byte(position[2]));
                messageBody = addBytes(messageBody,tempPosition);

                byte[] tempRotation = addBytes(float2Byte(rotation[0]),float2Byte(rotation[1]));
                tempRotation = addBytes(tempRotation,float2Byte(rotation[2]));
                messageBody = addBytes(messageBody,tempRotation);

                return messageBody;
            }

            public byte[] constructPoseMessageBody(float[] pose){

                byte[] tempPosition = addBytes(float2Byte(pose[0]),float2Byte(pose[1]));
                tempPosition = addBytes(tempPosition,float2Byte(pose[2]));
                tempPosition = addBytes(tempPosition,float2Byte(pose[3]));
                tempPosition = addBytes(tempPosition,float2Byte(pose[4]));
                tempPosition = addBytes(tempPosition,float2Byte(pose[5]));

                return tempPosition;
            }


            public void stopSocket() {

                isRunning = false;
            }

            public void run() {

                int receivedBytes = 0;

                byte [] msgHeader = addBytes(IntToByte(0),IntToByte(48));
                sendControlMsg(msgHeader);


                int[] resolution = {screenWidth,screenHight};
                float[] focal = {0.0f, 0.0f};
                float[] center = {0.0f, 0.0f};
                float[] position = {0.0f, 0.0f, 0.0f};
                float[] rotation  = {0.0f, 0.0f, 0.0f};
                byte [] msgBody = constructMessageBody(resolution,focal,center,position,rotation);

                sendControlMsg(msgBody);

                /*try {
                    out.write(msgHeader);
                    out.write(msgBody);
                } catch (IOException e) {
                    e.printStackTrace();
                }*/


                while (isRunning) {

                    byte[] frameMsg = new byte[8];
                    try {
                        int len = in.read(frameMsg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    receivedBytes = 0;
                    while (receivedBytes < frameBytes) {
                        try {
                            receivedBytes += in.read(yuv, receivedBytes, frameBytes - receivedBytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    oneFrameReceived = true;

                    if(poseChanged)
                    {
                        byte [] poseMsgHeader = addBytes(IntToByte(1),IntToByte(24));
                        sendControlMsg(poseMsgHeader);



                        byte [] poseMsgBody = constructPoseMessageBody(newPose);

                        sendControlMsg(poseMsgBody);

                        poseChanged = false;
                    }


                }
            }

            private byte[]IntToByte(int num){
                byte[]bytes=new byte[4];
                bytes[0]=(byte) ((num>>24)&0xff);
                bytes[1]=(byte) ((num>>16)&0xff);
                bytes[2]=(byte) ((num>>8)&0xff);
                bytes[3]=(byte) (num&0xff);
                return bytes;
            }


            private byte[] float2Byte (float value)

            {

                return dataValueRollback(ByteBuffer.allocate(4).putFloat(value).array());

            }

            private byte[] dataValueRollback(byte[] data) {
                ArrayList<Byte> al = new ArrayList<Byte>();
                for (int i = data.length - 1; i >= 0; i--) {
                    al.add(data[i]);
                }

                byte[] buffer = new byte[al.size()];
                for (int i = 0; i <= buffer.length - 1; i++) {
                    buffer[i] = al.get(i);
                }

                return buffer;
            }

            private byte[] addBytes(byte[] data1, byte[] data2) {
                byte[] data3 = new byte[data1.length + data2.length];
                System.arraycopy(data1, 0, data3, 0, data1.length);
                System.arraycopy(data2, 0, data3, data1.length, data2.length);
                return data3;

            }




        }


    }



}
