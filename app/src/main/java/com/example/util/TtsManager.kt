package com.example.util

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

enum class TtsPlaybackState {
    STOPPED,
    PLAYING,
    PAUSED
}

class TtsManager(context: Context) : TextToSpeech.OnInitListener {
    private val TAG = "TtsManager"
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _playbackState = MutableStateFlow(TtsPlaybackState.STOPPED)
    val playbackState = _playbackState.asStateFlow()

    private val _currentSentenceIndex = MutableStateFlow(0)
    val currentSentenceIndex = _currentSentenceIndex.asStateFlow()

    private val _currentSentenceText = MutableStateFlow("")
    val currentSentenceText = _currentSentenceText.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate = _speechRate.asStateFlow()

    private var sentences = listOf<String>()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val arResult = tts?.setLanguage(Locale("ar"))
            if (arResult == TextToSpeech.LANG_MISSING_DATA || arResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setSpeechRate(_speechRate.value)
            setupListener()
            isInitialized = true
        } else {
            Log.e(TAG, "TTS initialization failed")
        }
    }

    private fun setupListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _playbackState.value = TtsPlaybackState.PLAYING
            }

            override fun onDone(utteranceId: String?) {
                val nextIdx = _currentSentenceIndex.value + 1
                if (nextIdx < sentences.size) {
                    _currentSentenceIndex.value = nextIdx
                    _currentSentenceText.value = sentences[nextIdx]
                    speakCurrentSentence()
                } else {
                    _playbackState.value = TtsPlaybackState.STOPPED
                    _currentSentenceIndex.value = 0
                    _currentSentenceText.value = ""
                }
            }

            override fun onError(utteranceId: String?) {
                _playbackState.value = TtsPlaybackState.STOPPED
            }
        })
    }

    fun prepareText(fullText: String) {
        // Split text into meaningful spoken sentences / clauses
        sentences = fullText
            .split(Regex("(?<=[.!؟\n])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        _currentSentenceIndex.value = 0
        if (sentences.isNotEmpty()) {
            _currentSentenceText.value = sentences[0]
        }
    }

    fun play() {
        if (!isInitialized || sentences.isEmpty()) return
        _playbackState.value = TtsPlaybackState.PLAYING
        speakCurrentSentence()
    }

    fun pause() {
        tts?.stop()
        _playbackState.value = TtsPlaybackState.PAUSED
    }

    fun stop() {
        tts?.stop()
        _playbackState.value = TtsPlaybackState.STOPPED
        _currentSentenceIndex.value = 0
        _currentSentenceText.value = ""
    }

    fun seekForward() {
        val next = (_currentSentenceIndex.value + 1).coerceAtMost(sentences.lastIndex)
        _currentSentenceIndex.value = next
        if (next in sentences.indices) {
            _currentSentenceText.value = sentences[next]
            if (_playbackState.value == TtsPlaybackState.PLAYING) {
                speakCurrentSentence()
            }
        }
    }

    fun seekBackward() {
        val prev = (_currentSentenceIndex.value - 1).coerceAtLeast(0)
        _currentSentenceIndex.value = prev
        if (prev in sentences.indices) {
            _currentSentenceText.value = sentences[prev]
            if (_playbackState.value == TtsPlaybackState.PLAYING) {
                speakCurrentSentence()
            }
        }
    }

    fun setSpeed(rate: Float) {
        _speechRate.value = rate
        tts?.setSpeechRate(rate)
    }

    private fun speakCurrentSentence() {
        val idx = _currentSentenceIndex.value
        if (idx in sentences.indices) {
            val textToSpeak = sentences[idx]
            val params = Bundle()
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "utt_$idx")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
