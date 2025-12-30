package com.example.gamelabs.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

object Ps1Bridge {
    init {
        // Carrega a biblioteca nativa que criamos no CMake (ps1-bridge)
        System.loadLibrary("ps1-bridge")
    }

    private var audioTrack: AudioTrack? = null

    // Funções Nativas (Vinculadas ao C++ ps1-bridge.cpp)
    external fun loadCore(corePath: String): Boolean
    external fun loadGame(romPath: String): Boolean
    external fun runFrame()
    external fun sendInput(playerId: Int, buttons: Int)
    external fun closeCore()
    external fun initGL()
    external fun resizeGL(width: Int, height: Int)
    external fun drawFrameGL()

    // Setup de Áudio específico para PS1 (44100Hz)
    fun setupAudio() {
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build())
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
    }

    // Callback chamado pelo C++
    fun onAudioBatch(data: ShortArray) {
        audioTrack?.write(data, 0, data.size)
    }

    fun stopAudio() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    fun getCorePath(context: Context): String {
        // Retorna sempre o core do PS1
        val libDir = context.applicationInfo.nativeLibraryDir
        return "$libDir/libpcsx_rearmed_libretro_android.so"
    }
}