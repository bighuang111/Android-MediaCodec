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

// Wrapper for native library

public class GLES3JNILib {

     static {
          System.loadLibrary("gles3jni");
     }

     public static native void init();
     public static native void resize(int width, int height);
     public static native void step();

     public static native void yuvToRGB(byte[] yuv, byte[] rgb, int width, int height);
     public static native void display(byte[] rgb, int width, int height);
     public static native void initDisplay(int w, int h, int disp_width, int disp_height);


}
