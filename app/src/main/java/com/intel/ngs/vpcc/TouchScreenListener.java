package com.intel.ngs.vpcc;


import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

public  abstract class TouchScreenListener implements View.OnTouchListener{
    final static String TAG = "com.intel.ngs.vpcc";
    private int mode = 0;
    private SixDofKeyAction myKeyAction = new SixDofKeyAction();
    private int resetState = 0;

    private static final long DOUBLE_TIME = 1000;
    private static long lastClickTime = 0;


    //public abstract void onTouchAction(String s);
    public abstract void onTouchAction(float[] pos, String posString);

    private void setResetState()
    {
        float [] resetOrientation = new float[3];
        float [] resetTranslation = new float[3];
        int poseInfo = 0;

        String poseString = null;
        do{
            poseInfo = myKeyAction.getKeyActionByArcoreSensor(resetTranslation,resetOrientation,2);

            poseString = String.format(Locale.ENGLISH, "%d,", poseInfo);
            float [] temp = myKeyAction.getMyCurrentPose();
            poseString += String.format(Locale.ENGLISH, " %.3f, %.3f, %.3f, %.2f, %.2f, %.2f", temp[3],temp[4],temp[5], temp[0],temp[1],temp[2]);

            Log.d("reset:",poseString);

            //onTouchAction(poseString);
            if(poseInfo == 0)
            {
                temp = new float[6];
            }
            onTouchAction(temp, poseString);
        }while (poseInfo != 0);

        resetState = 0;

    }

    public boolean onTouch(View V, MotionEvent e) {
        int x;
        int y;
        int[] touchPose = new int[4];
        String poseString;
        switch(e.getAction()){
            case MotionEvent.ACTION_DOWN:
                x=(int)e.getX();
                y=(int)e.getY();
                mode = e.getPointerCount();
                Log.d(TAG,"ACTION_DOWN mode: " + mode);
                break;
            case MotionEvent.ACTION_MOVE:
                touchPose[0] = (int)e.getX(0);
                touchPose[1] = (int)e.getY(0);
                mode = e.getPointerCount();
                Log.d(TAG,"ACTION_MOVE mode: " + mode);
                Log.d(TAG,"ACTION_MOVE mode:  x1="+touchPose[0]+", y1="+touchPose[1]);

                if(3 == e.getPointerCount())
                {
                    long currentTimeMillis = System.currentTimeMillis();
                    if(resetState == 0)
                    {
                        lastClickTime = currentTimeMillis;
                        resetState = 1;
                    }
                    else if(resetState == 1 )
                    {
                        if (currentTimeMillis - lastClickTime > DOUBLE_TIME) {
                            Log.d(TAG,"ACTION_MOVE mode: reset");
                            resetState = 2;
                            setResetState();
                        }
                    }

                }
                if(2 == e.getPointerCount())
                {
                    touchPose[2] = (int)e.getX(1);
                    touchPose[3] = (int)e.getY(1);
                }
                else
                {
                    touchPose[2] = 0;
                    touchPose[3] = 0;
                }
                int poseChanged = myKeyAction.getKeyActionByTouch(touchPose);
                if(poseChanged != 0)
                {
                    poseString = String.format(Locale.ENGLISH, "%d,", poseChanged);
                    float [] temp = myKeyAction.getMyCurrentPose();
                    poseString += String.format(Locale.ENGLISH, " %.3f, %.3f, %.3f, %.2f, %.2f, %.2f", temp[3],temp[4],temp[5], temp[0],temp[1],temp[2]);

                    onTouchAction(temp, poseString);
                }
                break;
            case MotionEvent.ACTION_UP:
                if(resetState == 1) {
                    resetState = 0;
                }
                x=(int)e.getX();
                y=(int)e.getY();
                mode = e.getPointerCount();
                Log.d(TAG,"ACTION_UP mode: " + mode);
                myKeyAction.resetKeyActionByTouch();
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                mode = e.getPointerCount();
                Log.d(TAG,"ACTION_POINTER_DOWN mode: " + mode);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mode = e.getPointerCount();
                Log.d(TAG,"ACTION_POINTER_UP mode: " + mode);
                break;

        }

        // TODO
        return false;


    }


}