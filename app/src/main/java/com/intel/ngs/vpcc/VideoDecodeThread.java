package com.intel.ngs.vpcc;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.lang.Object;

public class VideoDecodeThread extends Thread {

	private final static String TAG = "VideoPlayView";

	private MediaCodec mediaCodec;
	private MediaExtractor mediaExtractor;
	private MediaFormat mediaFormat;
	private Surface surface;
	private String path;
	private volatile boolean paused;
	private final Object signal = new Object();

	public VideoDecodeThread(Surface surface, String path) {
		this.surface = surface;
		this.path = path;
		this.paused = false;
	}

	private boolean initMediaCodec()
	{
		Log.d(TAG, "initMediaCodec");
		mediaExtractor = new MediaExtractor();
		try {
			mediaExtractor.setDataSource(path);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		String mimeType = null;
		for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
			mediaFormat = mediaExtractor.getTrackFormat(i);
			Log.d(TAG, "mediaFormat"+String.valueOf(mediaFormat));
			Log.d(TAG, "mediaFormat"+String.valueOf(mediaFormat));
			mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
			if (mimeType.startsWith("video/")) {
				Log.d(TAG, mimeType);
				mediaExtractor.selectTrack(i);
				try {
					mediaCodec = MediaCodec.createDecoderByType(mimeType);
				} catch (IOException e) {
					e.printStackTrace();
				}
				mediaCodec.configure(mediaFormat, surface, null, 0);
				break;
			}
		}
		if (mediaCodec == null) {
			Log.e(TAG, "Can't find video info!");
			mediaExtractor.release();
			return false;
		}
		return true;
	}

	@Override
	public void run() {
		Log.d(TAG, "run");
		if(!initMediaCodec())
			return;

		mediaCodec.start();
		ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
		ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
		MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
		boolean bIsEos = false;
		boolean bInterupted = false;
		long startMs = System.currentTimeMillis();

		while (!Thread.interrupted()) {
			bInterupted = false;

			if (!bIsEos) {
				int inIndex = mediaCodec.dequeueInputBuffer(0);
				if (inIndex >= 0) {
					ByteBuffer buffer = inputBuffers[inIndex];
					int nSampleSize = mediaExtractor.readSampleData(buffer, 0);
					Log.d(TAG, "length:" + String.valueOf(nSampleSize));
					if (nSampleSize < 0) {
						Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
						mediaCodec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
						bIsEos = true;
					} else {
						mediaCodec.queueInputBuffer(inIndex, 0, nSampleSize, mediaExtractor.getSampleTime(), 0);
						mediaExtractor.advance();
					}
				}
			}

			int outIndex = mediaCodec.dequeueOutputBuffer(info, 0);
			Log.d(TAG,"outIndex:"+outIndex);
			switch (outIndex) {
				case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
					Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
					outputBuffers = mediaCodec.getOutputBuffers();
					break;
				case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
					Log.d(TAG, "New format " + mediaCodec.getOutputFormat());
					break;
				case MediaCodec.INFO_TRY_AGAIN_LATER:
					Log.d(TAG, "dequeueOutputBuffer timed out!");
					break;
				default:
					ByteBuffer buffer = outputBuffers[outIndex];
					Log.d(TAG, "We can't use this buffer but render it due to the API limit, " + buffer);

					while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
						try {
							sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
							bInterupted = true;
							Log.d(TAG, "Interupt during sleep");
							break;
						}
					}
					if(!bInterupted)
						mediaCodec.releaseOutputBuffer(outIndex, true);
					break;
			}

			while(paused) {
				Log.d(TAG, "paused");
				mediaCodec.stop();
				synchronized(signal) {
					try {
						signal.wait();
						Log.d(TAG, "unpaused");
						mediaCodec.start();
						inputBuffers = mediaCodec.getInputBuffers();
						outputBuffers = mediaCodec.getOutputBuffers();
						info = new MediaCodec.BufferInfo();
					} catch (InterruptedException e){
						e.printStackTrace();
						bInterupted = true;
						Log.d(TAG, "Interupt during wait");
						break;
					}
				}
			 }

			// All decoded frames have been rendered, we can stop playing
			// now
			if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0||
				Thread.interrupted()||bInterupted) {
				break;
			}
		}
		stopReleaseMediaCodec();
	}

	public void stopReleaseMediaCodec()
	{
		Log.d(TAG, "stopReleaseMediaCodec");
		mediaCodec.stop();
		mediaCodec.release();
		mediaExtractor.release();
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
		synchronized(signal) {signal.notify();}
	}

	public void reconfigSurface(Surface surface){
		Log.d(TAG, "reconfigSurface "+Boolean.toString(paused));
		if(paused) {
			this.surface = surface;
			mediaCodec.configure(mediaFormat, surface, null, 0);
		}
	}
}
