#include <jni.h>
#include <string>
#include <dlfcn.h>
#include <android/log.h>
#include <GLES2/gl2.h>
#include <stdlib.h>
#include <stdint.h>
#include <cstring>

#define LOG_TAG "Ps1Bridge"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define RETRO_DEVICE_JOYPAD 1
#define RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY 9
#define RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY 31

// --- STRUCTS LIBRETRO ---
struct retro_game_info {
    const char *path;
    const void *data;
    size_t size;
    const char *meta;
};

// --- GLOBAIS ---
void* coreHandle = nullptr;
uint16_t current_buttons = 0;
std::string g_saveDirectory = ""; // Armazena o caminho do Memory Card

// OpenGL
GLuint g_textureId = 0;
GLuint g_program = 0;
GLint g_posHandle = 0;
GLint g_texHandle = 0;
uint16_t* g_pixelBuffer = nullptr;
int g_coreWidth = 0;
int g_coreHeight = 0;

// JNI Audio
JavaVM* g_jvm = nullptr;
jobject g_bridgeObject = nullptr;
jmethodID g_audioCallback = nullptr;

// Ponteiros de Função do Core
void (*retro_init)(void);
void (*retro_deinit)(void);
bool (*retro_load_game)(struct retro_game_info *info);
void (*retro_run)(void);
void (*retro_set_environment)(bool (*cb)(unsigned, void *));
void (*retro_set_video_refresh)(void (*cb)(const void *, unsigned, unsigned, size_t));
void (*retro_set_audio_sample)(void (*cb)(int16_t, int16_t));
void (*retro_set_audio_sample_batch)(size_t (*cb)(const int16_t *, size_t));
void (*retro_set_input_poll)(void (*cb)(void));
void (*retro_set_input_state)(int16_t (*cb)(unsigned, unsigned, unsigned, unsigned));

// --- SHADERS ---
const char* vertexShaderCode =
        "attribute vec4 a_Position;\n"
        "attribute vec2 a_TexCoordinate;\n"
        "varying vec2 v_TexCoordinate;\n"
        "void main() {\n"
        "  v_TexCoordinate = a_TexCoordinate;\n"
        "  gl_Position = a_Position;\n"
        "}";

const char* fragmentShaderCode =
        "precision mediump float;\n"
        "uniform sampler2D u_Texture;\n"
        "varying vec2 v_TexCoordinate;\n"
        "void main() {\n"
        "  gl_FragColor = texture2D(u_Texture, v_TexCoordinate);\n"
        "}";

GLuint loadShader(GLenum type, const char* code) {
    GLuint shader = glCreateShader(type);
    glShaderSource(shader, 1, &code, nullptr);
    glCompileShader(shader);
    return shader;
}

// --- CALLBACKS ---
bool environment_cb(unsigned cmd, void *data) {
    switch (cmd) {
        case 10: // SET_PIXEL_FORMAT
        {
            enum retro_pixel_format { RGB565 = 2 };
            const enum retro_pixel_format *fmt = (enum retro_pixel_format *)data;
            if (*fmt == RGB565) return true;
            return false;
        }
            // O Core pergunta onde salvar o Memory Card (.mcr)
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
        {
            const char** dir = (const char**)data;
            *dir = g_saveDirectory.c_str();
            return true;
        }
            // O Core pergunta onde salvar configs de sistema (usamos a mesma pasta)
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
        {
            const char** dir = (const char**)data;
            *dir = g_saveDirectory.c_str();
            return true;
        }
        default:
            return false;
    }
}

void video_refresh_cb(const void *data, unsigned width, unsigned height, size_t pitch) {
    if (!data || !g_pixelBuffer) return;
    g_coreWidth = width;
    g_coreHeight = height;
    const uint8_t* src = (const uint8_t*)data;
    uint8_t* dst = (uint8_t*)g_pixelBuffer;

    // Copia linha por linha respeitando o pitch
    for(unsigned y = 0; y < height; y++) {
        memcpy(dst, src, width * 2);
        src += pitch;
        dst += width * 2;
    }
}

size_t audio_sample_batch_cb(const int16_t *data, size_t frames) {
    if (g_jvm && g_bridgeObject && g_audioCallback) {
        JNIEnv* env;
        int getEnvStat = g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
        bool attached = false;
        if (getEnvStat == JNI_EDETACHED) {
            if (g_jvm->AttachCurrentThread(&env, nullptr) != 0) return 0;
            attached = true;
        }
        jshortArray audioData = env->NewShortArray(frames * 2);
        env->SetShortArrayRegion(audioData, 0, frames * 2, data);
        env->CallVoidMethod(g_bridgeObject, g_audioCallback, audioData);
        env->DeleteLocalRef(audioData);
        if (attached) g_jvm->DetachCurrentThread();
    }
    return frames;
}

void audio_sample_cb(int16_t left, int16_t right) {
    int16_t frame[2] = {left, right};
    audio_sample_batch_cb(frame, 1);
}

void input_poll_cb() { }

int16_t input_state_cb(unsigned port, unsigned device, unsigned index, unsigned id) {
    if (port == 0 && device == RETRO_DEVICE_JOYPAD) {
        return (current_buttons & (1 << id)) ? 1 : 0;
    }
    return 0;
}

// --- JNI EXPORTS ---

// NOVA FUNÇÃO: Define o diretório de saves
extern "C" JNIEXPORT void JNICALL
Java_com_example_gamelabs_data_Ps1Bridge_setDirectories(JNIEnv* env, jobject, jstring savePath) {
    const char *path = env->GetStringUTFChars(savePath, 0);
    g_saveDirectory = std::string(path);
    env->ReleaseStringUTFChars(savePath, path);

    // Garante barra no final
    if (!g_saveDirectory.empty() && g_saveDirectory.back() != '/') {
        g_saveDirectory += "/";
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_gamelabs_data_Ps1Bridge_loadCore(JNIEnv* env, jobject thiz, jstring corePath) {
    env->GetJavaVM(&g_jvm);
    if (g_bridgeObject) env->DeleteGlobalRef(g_bridgeObject);
    g_bridgeObject = env->NewGlobalRef(thiz);

    jclass cls = env->GetObjectClass(thiz);
    g_audioCallback = env->GetMethodID(cls, "onAudioBatch", "([S)V");

    if (!g_pixelBuffer) g_pixelBuffer = (uint16_t*)malloc(1024 * 1024 * 2);

    const char *path = env->GetStringUTFChars(corePath, 0);
    coreHandle = dlopen(path, RTLD_LAZY);
    if (!coreHandle) {
        LOGE("Falha dlopen: %s", dlerror());
        return false;
    }

    retro_init = (void (*)(void))dlsym(coreHandle, "retro_init");
    retro_deinit = (void (*)(void))dlsym(coreHandle, "retro_deinit");
    retro_load_game = (bool (*)(struct retro_game_info*))dlsym(coreHandle, "retro_load_game");
    retro_run = (void (*)(void))dlsym(coreHandle, "retro_run");
    retro_set_environment = (void (*)(bool (*)(unsigned, void*)))dlsym(coreHandle, "retro_set_environment");
    retro_set_video_refresh = (void (*)(void (*)(const void*, unsigned, unsigned, size_t)))dlsym(coreHandle, "retro_set_video_refresh");
    retro_set_audio_sample = (void (*)(void (*)(int16_t, int16_t)))dlsym(coreHandle, "retro_set_audio_sample");
    retro_set_audio_sample_batch = (void (*)(size_t (*)(const int16_t*, size_t)))dlsym(coreHandle, "retro_set_audio_sample_batch");
    retro_set_input_poll = (void (*)(void (*)(void)))dlsym(coreHandle, "retro_set_input_poll");
    retro_set_input_state = (void (*)(int16_t (*)(unsigned, unsigned, unsigned, unsigned)))dlsym(coreHandle, "retro_set_input_state");

    if (!retro_init) return false;

    retro_set_environment(environment_cb);
    retro_set_video_refresh(video_refresh_cb);
    retro_set_audio_sample(audio_sample_cb);
    retro_set_audio_sample_batch(audio_sample_batch_cb);
    retro_set_input_poll(input_poll_cb);
    retro_set_input_state(input_state_cb);

    retro_init();
    env->ReleaseStringUTFChars(corePath, path);
    return true;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_gamelabs_data_Ps1Bridge_loadGame(JNIEnv* env, jobject, jstring romPath) {
    const char *path = env->GetStringUTFChars(romPath, 0);
    struct retro_game_info game = { path, nullptr, 0, nullptr };
    bool res = false;
    if (retro_load_game) res = retro_load_game(&game);
    env->ReleaseStringUTFChars(romPath, path);
    return res;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_gamelabs_data_Ps1Bridge_runFrame(JNIEnv*, jobject) {
    if (retro_run) retro_run();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_gamelabs_data_Ps1Bridge_initGL(JNIEnv*, jobject) {
    GLuint vertexShader = loadShader(GL_VERTEX_SHADER, vertexShaderCode);
    GLuint fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentShaderCode);
    g_program = glCreateProgram();
    glAttachShader(g_program, vertexShader);
    glAttachShader(g_program, fragmentShader);
    glLinkProgram(g_program);
    g_posHandle = glGetAttribLocation(g_program, "a_Position");
    g_texHandle = glGetAttribLocation(g_program, "a_TexCoordinate");
    glGenTextures(1, &g_textureId);
    glBindTexture(GL_TEXTURE_2D, g_textureId);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_gamelabs_data_Ps1Bridge_resizeGL(JNIEnv*, jobject, jint width, jint height) {
    glViewport(0, 0, width, height);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_gamelabs_data_Ps1Bridge_drawFrameGL(JNIEnv*, jobject) {
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);
    if (g_coreWidth == 0 || g_coreHeight == 0) return;
    glUseProgram(g_program);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, g_textureId);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, g_coreWidth, g_coreHeight, 0, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, g_pixelBuffer);
    static const GLfloat squareCoords[] = { -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f };
    static const GLfloat textureCoords[] = { 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f };
    glVertexAttribPointer(g_posHandle, 2, GL_FLOAT, GL_FALSE, 0, squareCoords);
    glEnableVertexAttribArray(g_posHandle);
    glVertexAttribPointer(g_texHandle, 2, GL_FLOAT, GL_FALSE, 0, textureCoords);
    glEnableVertexAttribArray(g_texHandle);
    glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_gamelabs_data_Ps1Bridge_sendInput(JNIEnv*, jobject, jint, jint buttons) {
    current_buttons = (uint16_t)buttons;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_gamelabs_data_Ps1Bridge_closeCore(JNIEnv* env, jobject) {
    if (retro_deinit) retro_deinit();
    if (coreHandle) dlclose(coreHandle);
    if (g_pixelBuffer) { free(g_pixelBuffer); g_pixelBuffer = nullptr; }
    if (g_bridgeObject) { env->DeleteGlobalRef(g_bridgeObject); g_bridgeObject = nullptr; }
    coreHandle = nullptr;
    g_saveDirectory = "";
}