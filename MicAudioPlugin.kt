package com.shottimer.app

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.view.WindowManager
import com.getcapacitor.JSObject
import com.getcapacitor.PermissionState
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import kotlin.concurrent.thread
import kotlin.math.sqrt

@CapacitorPlugin(
    name = "MicAudio",
    permissions = [
        Permission(strings = [Manifest.permission.RECORD_AUDIO], alias = "microphone")
    ]
)
class MicAudioPlugin : Plugin() {

    private val sampleRate = 44100
    private val samplesPerBuffer = 1024
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    @Volatile private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var activeSourceName: String = "UNKNOWN"

    @PluginMethod
    fun checkPermission(call: PluginCall) {
        val ret = JSObject()
        ret.put("granted", getPermissionState("microphone") == PermissionState.GRANTED)
        call.resolve(ret)
    }

    @PluginMethod
    fun requestPermission(call: PluginCall) {
        if (getPermissionState("microphone") == PermissionState.GRANTED) {
            val ret = JSObject()
            ret.put("granted", true)
            call.resolve(ret)
            return
        }
        requestPermissionForAlias("microphone", call, "permissionCallback")
    }

    @PermissionCallback
    private fun permissionCallback(call: PluginCall) {
        val ret = JSObject()
        ret.put("granted", getPermissionState("microphone") == PermissionState.GRANTED)
        call.resolve(ret)
    }

    @PluginMethod
    fun start(call: PluginCall) {
        if (isRecording) {
            call.reject("already_recording")
            return
        }
        if (getPermissionState("microphone") != PermissionState.GRANTED) {
            call.reject("permission_denied")
            return
        }

        try {
            val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (minBuffer <= 0) {
                call.reject("invalid_min_buffer")
                return
            }
            val bufferSizeBytes = maxOf(minBuffer, samplesPerBuffer * 2 * 4)

            val record = tryBuildRecorder(bufferSizeBytes)
            if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                record?.release()
                call.reject("audio_record_init_failed")
                return
            }

            audioRecord = record
            record.startRecording()
            isRecording = true

            keepScreenOn(true)

            recordingThread = thread(start = true, name = "MicAudioThread") {
                runRecordingLoop()
            }

            val ret = JSObject()
            ret.put("sampleRate", sampleRate)
            ret.put("samplesPerBuffer", samplesPerBuffer)
            ret.put("source", activeSourceName)
            call.resolve(ret)
        } catch (e: SecurityException) {
            call.reject("security_exception: ${e.message}")
        } catch (e: Exception) {
            call.reject("start_failed: ${e.message}")
        }
    }

    @PluginMethod
    fun stop(call: PluginCall) {
        stopInternal()
        call.resolve()
    }

    private fun tryBuildRecorder(bufferSizeBytes: Int): AudioRecord? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val rec = safeCreate(MediaRecorder.AudioSource.UNPROCESSED, bufferSizeBytes)
            if (rec != null && rec.state == AudioRecord.STATE_INITIALIZED) {
                activeSourceName = "UNPROCESSED"
                return rec
            }
            rec?.release()
        }
        val voiceRec = safeCreate(MediaRecorder.AudioSource.VOICE_RECOGNITION, bufferSizeBytes)
        if (voiceRec != null && voiceRec.state == AudioRecord.STATE_INITIALIZED) {
            activeSourceName = "VOICE_RECOGNITION"
            return voiceRec
        }
        voiceRec?.release()
        val micRec = safeCreate(MediaRecorder.AudioSource.MIC, bufferSizeBytes)
        if (micRec != null && micRec.state == AudioRecord.STATE_INITIALIZED) {
            activeSourceName = "MIC"
            return micRec
        }
        micRec?.release()
        return null
    }

    private fun safeCreate(source: Int, bufferSize: Int): AudioRecord? {
        return try {
            AudioRecord(source, sampleRate, channelConfig, audioFormat, bufferSize)
        } catch (e: Exception) {
            null
        }
    }

    private fun runRecordingLoop() {
        val buffer = ShortArray(samplesPerBuffer)
        val rec = audioRecord ?: return
        var prev = 0
        while (isRecording) {
            val read = try {
                rec.read(buffer, 0, buffer.size)
            } catch (e: Exception) {
                break
            }
            if (read <= 0) continue

            var sumSq = 0.0
            var diffSumSq = 0.0
            var peak = 0
            for (i in 0 until read) {
                val s = buffer[i].toInt()
                sumSq += (s.toDouble() * s.toDouble())
                val diff = s - prev
                diffSumSq += (diff.toDouble() * diff.toDouble())
                prev = s
                val abs = if (s < 0) -s else s
                if (abs > peak) peak = abs
            }
            val rms = sqrt(sumSq / read) / 32768.0
            val peakNorm = peak / 32768.0
            val hfRms = sqrt(diffSumSq / read) / 32768.0
            val hfRatio = if (rms > 1e-6) hfRms / rms else 0.0

            val event = JSObject()
            event.put("rms", rms)
            event.put("peak", peakNorm)
            event.put("hfRatio", hfRatio)
            event.put("t", System.nanoTime() / 1_000_000.0)
            notifyListeners("audioLevel", event)
        }
    }

    private fun stopInternal() {
        isRecording = false
        try {
            audioRecord?.stop()
        } catch (e: Exception) {}
        try {
            audioRecord?.release()
        } catch (e: Exception) {}
        audioRecord = null
        try {
            recordingThread?.join(500)
        } catch (e: Exception) {}
        recordingThread = null
        keepScreenOn(false)
    }

    private fun keepScreenOn(enabled: Boolean) {
        val act = activity ?: return
        act.runOnUiThread {
            if (enabled) {
                act.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                act.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    override fun handleOnDestroy() {
        stopInternal()
        super.handleOnDestroy()
    }
}
