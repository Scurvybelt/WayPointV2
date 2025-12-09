package com.example.waypointv2.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {
    
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTime: Long = 0
    private var isRecordingState = false
    
    /**
     * Inicia la grabación de audio
     * @return File donde se está grabando o null si falla
     */
    fun startRecording(): File? {
        return try {
            // Crear archivo temporal para el audio
            val timestamp = System.currentTimeMillis()
            outputFile = File(context.cacheDir, "audio_${timestamp}.m4a")
            
            // Configurar MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile?.absolutePath)
                
                prepare()
                start()
            }
            
            startTime = System.currentTimeMillis()
            isRecordingState = true
            outputFile
        } catch (e: IOException) {
            e.printStackTrace()
            releaseRecorder()
            null
        }
    }
    
    /**
     * Detiene la grabación de audio
     * @return File con el audio grabado o null si falla
     */
    fun stopRecording(): File? {
        return try {
            if (isRecordingState) {
                mediaRecorder?.apply {
                    stop()
                    reset()
                }
                isRecordingState = false
                outputFile
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            releaseRecorder()
        }
    }
    
    /**
     * Obtiene la duración actual de la grabación en milisegundos
     */
    fun getRecordingDuration(): Long {
        return if (isRecordingState) {
            System.currentTimeMillis() - startTime
        } else {
            0L
        }
    }
    
    /**
     * Verifica si está grabando actualmente
     */
    fun isRecording(): Boolean = isRecordingState
    
    /**
     * Cancela la grabación y elimina el archivo
     */
    fun cancelRecording() {
        try {
            if (isRecordingState) {
                mediaRecorder?.apply {
                    stop()
                    reset()
                }
                isRecordingState = false
            }
            outputFile?.delete()
            outputFile = null
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            releaseRecorder()
        }
    }
    
    /**
     * Libera los recursos del MediaRecorder
     */
    private fun releaseRecorder() {
        mediaRecorder?.release()
        mediaRecorder = null
    }
    
    /**
     * Limpieza al destruir
     */
    fun cleanup() {
        cancelRecording()
    }
}
