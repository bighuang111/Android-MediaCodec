package com.intel.ngs.vpcc;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class SurfaceViewActivity extends Activity {
    private  final static String TAG = "com.intel.ngs.vpcc";

    private GLES3JNIView mView;
    private int screenWidth;
    private int screenHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        screenWidth = getResources().getDisplayMetrics().widthPixels;
        screenHeight = getResources().getDisplayMetrics().heightPixels;
        Log.d(TAG, "screenWidth "+String.valueOf(screenWidth));

        mView = new GLES3JNIView(getApplication());
        setContentView(mView);
        //setContentView(R.layout.activity_surface_view);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        mView.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }
}
