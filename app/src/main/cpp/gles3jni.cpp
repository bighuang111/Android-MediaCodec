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

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include "gles3jni.h"

const Vertex QUAD[4] = {
    // Square with diagonal < 2 so that it fits in a [-1 .. 1]^2 square
    // regardless of rotation.
    {{-0.7f, -0.7f}, {0x00, 0xFF, 0x00}},
    {{ 0.7f, -0.7f}, {0x00, 0x00, 0xFF}},
    {{-0.7f,  0.7f}, {0xFF, 0x00, 0x00}},
    {{ 0.7f,  0.7f}, {0xFF, 0xFF, 0xFF}},
};

bool checkGlError(const char* funcName) {
    GLint err = glGetError();
    if (err != GL_NO_ERROR) {
        ALOGE("GL error after %s(): 0x%08x\n", funcName, err);
        return true;
    }
    return false;
}

GLuint createShader(GLenum shaderType, const char* src) {
    GLuint shader = glCreateShader(shaderType);
    if (!shader) {
        checkGlError("glCreateShader");
        return 0;
    }
    glShaderSource(shader, 1, &src, NULL);

    GLint compiled = GL_FALSE;
    glCompileShader(shader);
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
    if (!compiled) {
        GLint infoLogLen = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLogLen);
        if (infoLogLen > 0) {
            GLchar* infoLog = (GLchar*)malloc(infoLogLen);
            if (infoLog) {
                glGetShaderInfoLog(shader, infoLogLen, NULL, infoLog);
                ALOGE("Could not compile %s shader:\n%s\n",
                        shaderType == GL_VERTEX_SHADER ? "vertex" : "fragment",
                        infoLog);
                free(infoLog);
            }
        }
        glDeleteShader(shader);
        return 0;
    }

    return shader;
}

GLuint createProgram(const char* vtxSrc, const char* fragSrc) {
    GLuint vtxShader = 0;
    GLuint fragShader = 0;
    GLuint program = 0;
    GLint linked = GL_FALSE;

    vtxShader = createShader(GL_VERTEX_SHADER, vtxSrc);
    if (!vtxShader)
        goto exit;

    fragShader = createShader(GL_FRAGMENT_SHADER, fragSrc);
    if (!fragShader)
        goto exit;

    program = glCreateProgram();
    if (!program) {
        checkGlError("glCreateProgram");
        goto exit;
    }
    glAttachShader(program, vtxShader);
    glAttachShader(program, fragShader);

    glLinkProgram(program);
    glGetProgramiv(program, GL_LINK_STATUS, &linked);
    if (!linked) {
        ALOGE("Could not link program");
        GLint infoLogLen = 0;
        glGetProgramiv(program, GL_INFO_LOG_LENGTH, &infoLogLen);
        if (infoLogLen) {
            GLchar* infoLog = (GLchar*)malloc(infoLogLen);
            if (infoLog) {
                glGetProgramInfoLog(program, infoLogLen, NULL, infoLog);
                ALOGE("Could not link program:\n%s\n", infoLog);
                free(infoLog);
            }
        }
        glDeleteProgram(program);
        program = 0;
    }

exit:
    glDeleteShader(vtxShader);
    glDeleteShader(fragShader);
    return program;
}

static void printGlString(const char* name, GLenum s) {
    const char* v = (const char*)glGetString(s);
    ALOGV("GL %s: %s\n", name, v);
}

// ----------------------------------------------------------------------------

Renderer::Renderer()
:   mNumInstances(0),
    mLastFrameNs(0)
{
    memset(mScale, 0, sizeof(mScale));
    memset(mAngularVelocity, 0, sizeof(mAngularVelocity));
    memset(mAngles, 0, sizeof(mAngles));
}

Renderer::~Renderer() {
}

void Renderer::resize(int w, int h) {
    auto offsets = mapOffsetBuf();
    calcSceneParams(w, h, offsets);
    unmapOffsetBuf();

    // Auto gives a signed int :-(
    for (auto i = (unsigned)0; i < mNumInstances; i++) {
        mAngles[i] = drand48() * TWO_PI;
        mAngularVelocity[i] = MAX_ROT_SPEED * (2.0*drand48() - 1.0);
    }

    mLastFrameNs = 0;

    glViewport(0, 0, w, h);
}

void Renderer::calcSceneParams(unsigned int w, unsigned int h,
        float* offsets) {
    // number of cells along the larger screen dimension
    const float NCELLS_MAJOR = MAX_INSTANCES_PER_SIDE;
    // cell size in scene space
    const float CELL_SIZE = 2.0f / NCELLS_MAJOR;

    // Calculations are done in "landscape", i.e. assuming dim[0] >= dim[1].
    // Only at the end are values put in the opposite order if h > w.
    const float dim[2] = {fmaxf(w,h), fminf(w,h)};
    const float aspect[2] = {dim[0] / dim[1], dim[1] / dim[0]};
    const float scene2clip[2] = {1.0f, aspect[0]};
    const int ncells[2] = {
            static_cast<int>(NCELLS_MAJOR),
            (int)floorf(NCELLS_MAJOR * aspect[1])
    };

    float centers[2][MAX_INSTANCES_PER_SIDE];
    for (int d = 0; d < 2; d++) {
        auto offset = -ncells[d] / NCELLS_MAJOR; // -1.0 for d=0
        for (auto i = 0; i < ncells[d]; i++) {
            centers[d][i] = scene2clip[d] * (CELL_SIZE*(i + 0.5f) + offset);
        }
    }

    int major = w >= h ? 0 : 1;
    int minor = w >= h ? 1 : 0;
    // outer product of centers[0] and centers[1]
    for (int i = 0; i < ncells[0]; i++) {
        for (int j = 0; j < ncells[1]; j++) {
            int idx = i*ncells[1] + j;
            offsets[2*idx + major] = centers[0][i];
            offsets[2*idx + minor] = centers[1][j];
        }
    }

    mNumInstances = ncells[0] * ncells[1];
    mScale[major] = 0.5f * CELL_SIZE * scene2clip[0];
    mScale[minor] = 0.5f * CELL_SIZE * scene2clip[1];
}

void Renderer::step() {
    timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    auto nowNs = now.tv_sec*1000000000ull + now.tv_nsec;

    if (mLastFrameNs > 0) {
        float dt = float(nowNs - mLastFrameNs) * 0.000000001f;

        for (unsigned int i = 0; i < mNumInstances; i++) {
            mAngles[i] += mAngularVelocity[i] * dt;
            if (mAngles[i] >= TWO_PI) {
                mAngles[i] -= TWO_PI;
            } else if (mAngles[i] <= -TWO_PI) {
                mAngles[i] += TWO_PI;
            }
        }

        float* transforms = mapTransformBuf();
        for (unsigned int i = 0; i < mNumInstances; i++) {
            float s = sinf(mAngles[i]);
            float c = cosf(mAngles[i]);
            transforms[4*i + 0] =  c * mScale[0];
            transforms[4*i + 1] =  s * mScale[1];
            transforms[4*i + 2] = -s * mScale[0];
            transforms[4*i + 3] =  c * mScale[1];
        }
        unmapTransformBuf();
    }

    mLastFrameNs = nowNs;
}

void Renderer::displayRGBFrame(unsigned char *frameRGB, int m_displayWidth, int m_displayHeight) {
// set pixel store parameters

    glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
    glPixelStorei(GL_UNPACK_ROW_LENGTH, m_width);

// copy frame to GL textureut
    glBindTexture(GL_TEXTURE_2D, m_textureId);
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, m_width, m_height, GL_RGB, GL_UNSIGNED_BYTE, frameRGB);
    glBindTexture(GL_TEXTURE_2D, 0);

// blit to screen framebuffer
    glBindFramebuffer(GL_READ_FRAMEBUFFER, m_fbId);
    glBindFramebuffer(GL_DRAW_FRAMEBUFFER, m_displayFbId);
    glBlitFramebuffer(0, 0, m_width, m_height, 0, 0, m_displayWidth, m_displayHeight,
                      GL_COLOR_BUFFER_BIT, GL_NEAREST);
    glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
    glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);

    glFinish();

}

void Renderer::convertYUVToRGB(unsigned char *frame, unsigned char *frameRGB, int w, int h) {
    int y = 0;
    int u = 0;
    int v = 0;
    int gg = 0;
    unsigned char r = 0;
    unsigned char g = 0;
    unsigned char b = 0;
    for(int row = 0; row < h; row++) {
        for(int col = 0; col < w; col++) {
            y = frame[(row * w + col)];
            u = frame[(h * w) + ((row/2) * (w/2) + col/2)];
            v = frame[(h * w) + (h * w / 4) +((row/2) * (w/2) + col/2)];
            r  = y +                      + (v - 128) *  1.40200;
            gg = y + (u - 128) * -0.34414 + (v - 128) * -0.71414;
            g  = gg < 0 ? 0 : (gg > 255 ? 255 : gg);
            b  = y + (u - 128) *  1.77200;
            frameRGB[((h-1-row) * w + col) * 3 + 0] = r;
            frameRGB[((h-1-row) * w + col) * 3 + 1] = g;
            frameRGB[((h-1-row) * w + col) * 3 + 2] = b;
        }
    }
}



void Renderer::render() {
    step();

    glClearColor(0.2f, 0.2f, 0.3f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    draw(mNumInstances);
    checkGlError("Renderer::render");
}

// ----------------------------------------------------------------------------

static Renderer* g_renderer = NULL;

extern "C" {
    JNIEXPORT void JNICALL Java_com_intel_ngs_vpcc_GLES3JNILib_init(JNIEnv* env, jobject obj);
    JNIEXPORT void JNICALL Java_com_intel_ngs_vpcc_GLES3JNILib_resize(JNIEnv* env, jobject obj, jint width, jint height);
    JNIEXPORT void JNICALL Java_com_intel_ngs_vpcc_GLES3JNILib_step(JNIEnv* env, jobject obj);
    JNIEXPORT void JNICALL Java_com_intel_ngs_vpcc_GLES3JNILib_yuvToRGB(JNIEnv *env, jclass type, jbyteArray yuv_, jbyteArray rgb_, jint width, jint height);
    JNIEXPORT void JNICALL Java_com_intel_ngs_vpcc_GLES3JNILib_display(JNIEnv *env, jclass type, jbyteArray rgb_, jint width, jint height);
    JNIEXPORT void JNICALL Java_com_intel_ngs_vpcc_GLES3JNILib_initDisplay(JNIEnv *env, jclass type, jint w, jint h, jint disp_width, jint disp_height);
};

#if !defined(DYNAMIC_ES3)
static GLboolean gl3stubInit() {
    return GL_TRUE;
}
#endif

JNIEXPORT void JNICALL
Java_com_intel_ngs_vpcc_GLES3JNILib_init(JNIEnv* env, jobject obj) {
    if (g_renderer) {
        delete g_renderer;
        g_renderer = NULL;
    }

    printGlString("Version", GL_VERSION);
    printGlString("Vendor", GL_VENDOR);
    printGlString("Renderer", GL_RENDERER);
    printGlString("Extensions", GL_EXTENSIONS);

    const char* versionStr = (const char*)glGetString(GL_VERSION);
    if (strstr(versionStr, "OpenGL ES 3.") && gl3stubInit()) {
        g_renderer = createES3Renderer();
    } else if (strstr(versionStr, "OpenGL ES 2.")) {
        g_renderer = createES2Renderer();
    } else {
        ALOGE("Unsupported OpenGL ES version");
    }


}

JNIEXPORT void JNICALL
Java_com_intel_ngs_vpcc_GLES3JNILib_resize(JNIEnv* env, jobject obj, jint width, jint height) {
    if (g_renderer) {
        g_renderer->resize(width, height);
    }
}

JNIEXPORT void JNICALL
Java_com_intel_ngs_vpcc_GLES3JNILib_step(JNIEnv* env, jobject obj) {
    if (g_renderer) {
        g_renderer->render();

    }
}

//extern "C"
JNIEXPORT void JNICALL
Java_com_intel_ngs_vpcc_GLES3JNILib_yuvToRGB(JNIEnv *env, jclass type, jbyteArray yuv_,
                                               jbyteArray rgb_, jint width, jint height) {
    jbyte *yuv = env->GetByteArrayElements(yuv_, NULL);
    jbyte *rgb = env->GetByteArrayElements(rgb_, NULL);

    // TODO

    if (g_renderer) {
        g_renderer->convertYUVToRGB((unsigned char*)yuv,(unsigned char*)rgb,width,height);
        g_renderer->displayRGBFrame((unsigned char*)rgb, g_renderer->m_displayWidth, g_renderer->m_displayHeight);
    }

    env->ReleaseByteArrayElements(yuv_, yuv, 0);
    env->ReleaseByteArrayElements(rgb_, rgb, 0);
}

//extern "C"
JNIEXPORT void JNICALL
Java_com_intel_ngs_vpcc_GLES3JNILib_display(JNIEnv *env, jclass type, jbyteArray rgb_, jint width,
                                              jint height) {
    jbyte *rgb = env->GetByteArrayElements(rgb_, NULL);

    // TODO

    if (g_renderer) {
        g_renderer->displayRGBFrame((unsigned char*)rgb,width,height);
    }

    env->ReleaseByteArrayElements(rgb_, rgb, 0);
}

//extern "C"
JNIEXPORT void JNICALL
Java_com_intel_ngs_vpcc_GLES3JNILib_initDisplay(JNIEnv *env, jclass type, jint w, jint h, jint disp_width,
                                                  jint disp_height) {

    // TODO
    // generate offscreen framebuffer and texture
    glGenFramebuffers(1, (GLuint *)&g_renderer->m_fbId);
    glBindFramebuffer(GL_FRAMEBUFFER, g_renderer->m_fbId);
    glGenTextures(1, (GLuint *)&g_renderer->m_textureId);
    glBindTexture(GL_TEXTURE_2D, g_renderer->m_textureId);

    // set texture parameters and allocate video memory
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, w, h, 0, GL_RGB, GL_UNSIGNED_BYTE,
                 nullptr);

    // attach
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, g_renderer->m_textureId, 0);
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    glBindTexture(GL_TEXTURE_2D, 0);



    if (g_renderer) {
        g_renderer->m_width = w;
        g_renderer->m_height = h;
        g_renderer->m_displayWidth = disp_width;
        g_renderer->m_displayHeight = disp_height;
    }


}