package com.intel.ngs.vpcc;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import android.content.res.Configuration;


public class MainActivity extends Activity {
    private final static String TAG = "com.intel.ngs.vpcc";

    private Button btn1;
    private EditText ed1;
    private EditText ed2;
    private EditText edGood;
    private EditText edBad;

    private Button playLocalVideo;
    private VideoPlayView localVideoPlayView;


    public static String  serverIP;
    public static  int serverPort;

    private boolean flag = true;

    private VideoDecodeThread thread;
    private SoundDecodeThread soundDecodeThread;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edGood = findViewById(R.id.edt_good);
        edBad = findViewById(R.id.edt_bad);

        //myView = this.getWindow().getDecorView();

        //获取所支持的编码信息的方法
        HashMap<String, MediaCodecInfo.CodecCapabilities> mEncoderInfos = new HashMap<>();
        for(int i = MediaCodecList.getCodecCount() - 1; i >= 0; i--){
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if(codecInfo.isEncoder()){
                for(String t : codecInfo.getSupportedTypes()){
                    try{
                        mEncoderInfos.put(t, codecInfo.getCapabilitiesForType(t));
                    } catch(IllegalArgumentException e){
                        Toast toast=Toast.makeText(MainActivity.this,e.getLocalizedMessage(),Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        e.printStackTrace();
                    }
                }
            }
        }

        btn1 = (Button)findViewById(R.id.service_btn);

        btn1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                ed1 = (EditText) findViewById(R.id.IP_edit);
                serverIP = ed1.getText().toString();
                if (!isIp(serverIP)) {
                    Toast.makeText(MainActivity.this, R.string.set_ip_message, Toast.LENGTH_SHORT).show();
                    return;
                }

                ed2 = (EditText) findViewById(R.id.port_edit);
                String port = ed2.getText().toString();
                if (port == null || port == "") {
                    Toast.makeText(MainActivity.this, R.string.set_port_message, Toast.LENGTH_SHORT).show();
                    return;
                }
                serverPort = Integer.parseInt(port);

                Intent intent = new Intent(MainActivity.this, SurfaceViewActivity.class);

                startActivity(intent);
            }
        });

        playLocalVideo = findViewById(R.id.btn_localView);
        localVideoPlayView = findViewById(R.id.localPlayer);
        localVideoPlayView.setVisibility(View.GONE);
        playLocalVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(edGood.getText().toString().isEmpty() || edBad.getText().toString().isEmpty()){
                    //Toast.makeText(MainActivity.this, R.string.set_matrix_message, Toast.LENGTH_SHORT).show();
                }else {
                    Values.GOOD = Double.valueOf(edGood.getText().toString());
                    Values.BAD = Double.valueOf(edBad.getText().toString());
                    Log.d("huang", String.valueOf(Values.GOOD));
                    Log.d("huang", String.valueOf(Values.BAD));
                }

                if(flag == true){
                    localVideoPlayView.setVisibility(View.VISIBLE);
                    playLocalVideo.setText("Stop Local Video");
                    flag = false;
                    localVideoPlayView.start();
                }else {
                    playLocalVideo.setText("Play Local Video");
                    flag = true;
                    localVideoPlayView.stop();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            localVideoPlayView.setVisibility(View.GONE);
                        }
                    },100);
                }
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(TAG, "onConfigurationChanged "+(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE?"LANDSCAPE":"PORTRAIT"));
		super.onConfigurationChanged(newConfig);
        //localVideoPlayView.pause();

	}

    private static boolean isIp(String ip) {
        if (ip == null || "".equals(ip))
            return false;
        String regex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
        return ip.matches(regex);
    }

}
