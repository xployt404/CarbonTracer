package com.example.carbontracerrevised

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AudioRecorder {
    private lateinit var mediaRecorder: MediaRecorder
    var isRecording = false

    companion object {
        private const val TAG = "Audio Recorder"
    }


    fun startRecording(context: Context, lifecycleScope: CoroutineScope) {
        isRecording = true
        Log.d(TAG, "Starting to record...")

        // Check for microphone permission
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Allow Microphone Permissions", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // Create the output file on the IO dispatcher
            val outputFile = withContext(Dispatchers.IO) {
                File(context.filesDir, "recording.ogg").apply {
                    // Ensure the file is created
                    if (!exists()) {
                        createNewFile()
                    }
                }
            }

            // Initialize the MediaRecorder on the IO dispatcher
            withContext(Dispatchers.IO) {
                mediaRecorder = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.OGG)
                    setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                    setOutputFile(outputFile.absolutePath)
                    prepare() // This should now be on the IO thread
                    start()    // This should also be on the IO thread
                }
            }

            Log.d(TAG, "Recording started: ${outputFile.absolutePath}")
        }
    }


    fun stopRecording() {
        try {
            if (isRecording) {
                isRecording = false
                // To stop recording
                mediaRecorder.stop()
                mediaRecorder.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "stopRecording: ${e.message}")
        }
    }
}