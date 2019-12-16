package com.intel.ngs.vpcc;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.OrientationEventListener;

class ScreenOrientationListener extends OrientationEventListener {
    private Context context;
    private Activity activity;
    private int myOrientation;

    public ScreenOrientationListener(Context context) {
        super(context);
        this.context = context;
        activity = (Activity)context;
    }

    public ScreenOrientationListener(Context context, int rate) {
        super(context, rate);
    }

    public int getOrientation(){
        return myOrientation;
    }

    @Override
    public void onOrientationChanged(int orientation) {
        int screenOrientation = context.getResources().getConfiguration().orientation;

        if (((orientation >= 0) && (orientation < 45)) || (orientation > 315)) {    //set to SCREEN_ORIENTATION_PORTRAIT
            if (screenOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && orientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {

                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                myOrientation = 0;
                //Log.d("orientation:","SCREEN_ORIENTATION_PORTRAIT");
            }
        } else if (orientation > 225 && orientation < 315) { //set to SCREEN_ORIENTATION_LANDSCAPE
            if (screenOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                myOrientation = 1;
                Log.d("orientation:","SCREEN_ORIENTATION_LANDSCAPE");
            }
        } else if (orientation > 45 && orientation < 135) {//set to SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            if (screenOrientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                myOrientation = 1;
                Log.d("orientation:","SCREEN_ORIENTATION_REVERSE_LANDSCAPE");
            }
        } else if (orientation > 135 && orientation < 225) { //set to SCREEN_ORIENTATION_REVERSE_PORTRAIT
            if (screenOrientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                myOrientation = 0;
                //Log.d("orientation:","SCREEN_ORIENTATION_REVERSE_PORTRAIT");
            }
        }
    }
}