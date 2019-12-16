package com.intel.ngs.vpcc;

import android.content.Context;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class VideoPlayView extends SurfaceView implements SurfaceHolder.Callback {
    private final static String TAG = "VideoPlayView";
    private static final String strVideo = Environment.getExternalStorageDirectory().getPath() + "/BasketballDrive.bin";

    private LongVideoDecoder thread;
    //private SoundDecodeThread soundDecodeThread;
    //public static boolean isCreate = false;
    public VideoPlayView(Context context) {
        super(context);
        getHolder().addCallback(this);
        Log.d(TAG, "VideoPath 0:"+strVideo);
    }

    public VideoPlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        Log.d(TAG, "VideoPath 1:"+strVideo);
    }

    public VideoPlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getHolder().addCallback(this);
        Log.d(TAG, "VideoPath 2:"+strVideo);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        //isCreate = true;
        //synchronized(thread.signalSurfaceCreate) {thread.signalSurfaceCreate.notify();}
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged "+ "w:"+Integer.toString(width)+" h:"+Integer.toString(height));
        thread.reconfigSurface(holder.getSurface());
        //soundDecodeThread.reconfig();
        thread.setUnpaused();
        //soundDecodeThread.setUnpaused();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        //isCreate = false;
    }

    public void start(){
        Log.d(TAG, "start");
        thread = new LongVideoDecoder(getHolder().getSurface(), strVideo);
        //soundDecodeThread = new SoundDecodeThread(strVideo);
        //soundDecodeThread.start();
        thread.start();
    }

    public void stop(){
        Log.d(TAG, "stop");
        thread.interrupt();
        //soundDecodeThread.interrupt();
    }

    public void pause(){
        Log.d(TAG, "pause");
        thread.setPaused();
        //soundDecodeThread.setPaused();
    }
}
