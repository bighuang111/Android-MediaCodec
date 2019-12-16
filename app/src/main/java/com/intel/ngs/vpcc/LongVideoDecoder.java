package com.intel.ngs.vpcc;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class LongVideoDecoder extends Thread {
    private final static String TAG = "VideoPlayView";
    private MediaCodec mediaCodec;
    private MediaFormat mediaFormat;
    private Surface surface;
    private String path;
    private volatile boolean paused;
    private final Object signal = new Object();
    private int FRAME_RATE = 24;
    private int HEIGHT = 1504;
    private int WIDTH = 736;
    private List<byte[]> splitBytes = null;
    private byte[] bytes = null;
    private double[][] matrix = {{Values.GOOD,1-Values.GOOD},{1-Values.BAD,Values.BAD}};
    private int state = 0;


    public LongVideoDecoder(Surface surface, String path) {
        this.surface = surface;
        this.path = path;
        this.paused = false;
    }

    public int calProbability(int currentState){
        Log.d(TAG,"Value Good" + Values.GOOD);
        Log.d(TAG,"Valus Bad" + Values.BAD);
        double[] probability = matrix[currentState];
        Random random = new Random();
        double a = random.nextDouble();
        if(a > probability[0])
            return 1;
        else
            return 0;

    }

    @Override
    public void run() {

        Log.d(TAG, "run");
        String mimeType = "video/hevc";
        try {
            mediaCodec = MediaCodec.createDecoderByType(mimeType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        long startMs = System.currentTimeMillis();

        mediaFormat = MediaFormat.createVideoFormat(mimeType, WIDTH, HEIGHT);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        mediaCodec.configure(mediaFormat, surface, null, 0);
        mediaCodec.start();

        try {
            RandomAccessFile File = new RandomAccessFile(path, "r");
            FileChannel fileChannel = File.getChannel();
            //long fileSize = fileChannel.size();
            ByteBuffer buffer = ByteBuffer.allocate(6553600);
            byte[] pattern = {0,0,0,1};

            while(fileChannel.read(buffer)> 0){
                buffer.flip();
                bytes = buffer.array();
                splitBytes = split(bytes,pattern);
                int i = 0;
                boolean isFirst = true;
                while (i < splitBytes.size()) {
                    if(i < 4){
                        state = 0;
                    }else {
                        //state = calProbability(state);
                        Log.d(TAG,"currentState: "+ state);
                    }
                    byte nal = splitBytes.get(i)[4];
                    if((nal == 0x4e && isFirst == false) || state == 1 ){
                        i++;
                        continue;
                    }
                    int inIndex = mediaCodec.dequeueInputBuffer(-1);
                    //Log.d(TAG, "inIndex:" + inIndex);
                    mediaCodec.getInputBuffer(inIndex).put(splitBytes.get(i), 0, splitBytes.get(i).length);
                    if(nal == 0x40 || nal == 0x42 || nal == 0x44){
                        mediaCodec.queueInputBuffer(inIndex, 0, splitBytes.get(i).length, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                    }
                    else if(nal == 0x4e && isFirst){
                        mediaCodec.queueInputBuffer(inIndex, 0, splitBytes.get(i).length, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                        isFirst = false;
                    } else {
                        mediaCodec.queueInputBuffer(inIndex, 0, splitBytes.get(i).length, 0, 0);
                    }

                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    int outIndex = mediaCodec.dequeueOutputBuffer(info, 0);
                    //Log.d(TAG, "outIndex:" + outIndex);
                    switch (outIndex) {
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                            //mediaCodec.getOutputBuffer(outIndex);
                            break;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            Log.d(TAG, "New format " + mediaCodec.getOutputFormat());
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            Log.d(TAG, "dequeueOutputBuffer timed out!");
                            break;
                        default:
                            mediaCodec.getOutputBuffer(outIndex);
                            Log.d(TAG, "We can't use this buffer but render it due to the API limit, ");
                            while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                                try {
                                    sleep(10);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    break;
                                }
                            }
                            mediaCodec.releaseOutputBuffer(outIndex, true);
                            break;
                    }
                    i++;
                }
                buffer.clear();
            }
            stopReleaseMediaCodec();
            fileChannel.close();
            File.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    public void stopReleaseMediaCodec() {
        Log.d(TAG, "stopReleaseMediaCodec");
        mediaCodec.stop();
        mediaCodec.release();
        //mediaExtractor.release();
    }

    public void setPaused() {
        Log.d(TAG, "setPaused");
        paused = true;
        //mediaCodec.stop();
    }

    public void setUnpaused() {
        Log.d(TAG, "setUnpaused");
        paused = false;
        //mediaCodec.start();
        synchronized (signal) {
            signal.notify();
        }
    }

    public void reconfigSurface(Surface surface) {
        Log.d(TAG, "reconfigSurface " + Boolean.toString(paused));
        if (paused) {
            this.surface = surface;
            mediaCodec.configure(mediaFormat, surface, null, 0);
        }
    }

    private List<byte[]> split(byte[] array, byte[] delimiter)
    {
        List<byte[]> byteArrays = new LinkedList<byte[]>();
        if (delimiter.length == 0) {
            return byteArrays;
        }
        int begin = 0;
        outer:
        for (int i = 0; i < array.length - delimiter.length + 1; i++) {
            for (int j = 0; j < delimiter.length; j++) {
                if (array[i + j] != delimiter[j]) {
                    continue outer;
                }
            }
            // If delimiter is at the beginning then there will not be any data.
            if (begin != i){
                byte[] both = concat(delimiter, Arrays.copyOfRange(array, begin, i));
                byteArrays.add(both);

            }
            begin = i + delimiter.length;
        }
        // delimiter at the very end with no data following?
        if (begin != array.length){
            byte[] both = concat(delimiter,Arrays.copyOfRange(array, begin, array.length));
            byteArrays.add(both);
        }
        return byteArrays;
    }

    private byte[] concat(byte[] a, byte[] b) {

        byte[] c= new byte[a.length+b.length];

        System.arraycopy(a, 0, c, 0, a.length);

        System.arraycopy(b, 0, c, a.length, b.length);

        return c;

    }
}
