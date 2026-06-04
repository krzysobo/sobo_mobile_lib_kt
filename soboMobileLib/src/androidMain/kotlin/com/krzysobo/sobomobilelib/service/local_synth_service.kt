package com.krzysobo.sobomobilelib.service

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.krzysobo.sobomobilelib.viewmodel.PhraseToTalk
import java.io.File
import java.util.Locale


class FreeTextUtteranceProgressListener(
    val onStartLmb: (utteranceId: String) -> Unit = {it ->},
    val onDoneLmb: (utteranceId: String) -> Unit = {it ->},
    val onStopLmb: (utteranceId: String, interrupted: Boolean) -> Unit = {it, it2 ->},
    val onErrorLmb: (utteranceId: String, errorCode: Int) -> Unit = {it, it2 -> },

) : UtteranceProgressListener() {
    override fun onStart(utteranceId: String) {
        Log.d("SYNTH_LISTENER","onStart - utteranceId: $utteranceId")
        onStartLmb(utteranceId)
    }

    override fun onDone(utteranceId: String) {
        Log.d("SYNTH_LISTENER","onDone - utteranceId: $utteranceId")
        onDoneLmb(utteranceId)
    }

    @Deprecated(message="this onError is deprecated, use the one " +
            "with utteranceId: String, errorCode: Int instead")
    override fun onError(utteranceId: String) {
        Log.d("SYNTH_LISTENER","onDone DEPRECATED - utteranceId: $utteranceId")
    }

    override fun onError(utteranceId: String, errorCode: Int) {
        Log.d("SYNTH_LISTENER","onDone GOOD - utteranceId: $utteranceId, errorCode: $errorCode")
        onErrorLmb(utteranceId, errorCode)
    }

    override fun onStop(utteranceId: String, interrupted: Boolean) {
        Log.d("SYNTH_LISTENER","onStop - utteranceId: $utteranceId, interrupted? $interrupted")
        onStopLmb(utteranceId, interrupted)
    }
}


class LocalSynthService {
    var defaultSpeechLang = "en"
    var speechLang = ""

    private lateinit var tts: TextToSpeech
    private var _isTtsInitialized: Boolean = false

    var isTtsInitialized
        get() = _isTtsInitialized
        set(value) {
            _isTtsInitialized = value
        }

    fun setLanguage(lang: String): Boolean {
        val resLangSetting = tts.setLanguage(Locale.forLanguageTag(lang))
        Log.d("SPEECH_VIEWX", "====> LocalSynthService.setLanguage: lang: $lang RESULT: $resLangSetting")
        if (resLangSetting in listOf(
                TextToSpeech.LANG_MISSING_DATA,
                TextToSpeech.LANG_NOT_SUPPORTED
            )
        ) {
            Log.e(
                "SPEECH_VIEWX",
                "====> LocalSynthService.setLanguage: lang: $lang RESULT: $resLangSetting - FAILED!!!"
            )
            return false
        } else {
            Log.d(
                "SPEECH_VIEWX",
                "====> LocalSynthService.setLanguage: lang: $lang RESULT: $resLangSetting - OK!!!"
            )
            return true
        }
    }

    fun initTextToSpeech(context: Context, lmbAfterInit: () -> Unit = {}): Boolean {
        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                return@TextToSpeech
            }

            isTtsInitialized = true
            Log.d(
                "SPEECH_VIEWX",
                "SPEECH_VIEWX - initTextToSpeech. is initialized: $isTtsInitialized"
            )
//            Log.d("SPEECH_VIEWX", "SPEECH_VIEWX -- voices when initialized: ${tts.voices} ")
            lmbAfterInit()
        }

        return isTtsInitialized
    }


    //    @Composable
    fun initTextToSpeechForLang(
        context: Context,
        lang: String = defaultSpeechLang,
        lmbAfterInit: () -> Unit = {},
    ): Boolean {
        speechLang = lang
        tts = TextToSpeech(context) { status ->
            println("SPEECH_VIEWX - initTextToSpeechForLang LANG: $lang STATUS: $status")
            if (status != TextToSpeech.SUCCESS) {
                return@TextToSpeech
            }

            val resLangSetting = tts.setLanguage(Locale.forLanguageTag(lang))
            println("====> initTextToSpeechForLang: lang: $lang RESULT: $resLangSetting")
            if (resLangSetting in listOf(
                    TextToSpeech.LANG_MISSING_DATA,
                    TextToSpeech.LANG_NOT_SUPPORTED
                )
            ) {
                return@TextToSpeech
            }

            isTtsInitialized = true
            Log.d(
                "SPEECH_VIEWX",
                "SPEECH_VIEWX - initTextToSpeechForLang LANG: $lang is initialized: $isTtsInitialized"
            )
            Log.d("SPEECH_VIEWX", "SPEECH_VIEWX -- voices when initialized: ${tts.voices} ")
            lmbAfterInit()
        }

        return isTtsInitialized
    }


    /*
pl-pl-x-bmg-network - PL-MALE    (index 1)
pl-PL-language      - PL-FEMALE  (index 2)

     */
    fun getVoiceForNameAndLang(name: String, langToSpeak: String = speechLang): Voice? {
        Log.d("SPEECH_VIEWXY", "getVoiceForNameAndLang - $name LANG: $langToSpeak")
        val voices = getVoicesForLang(langToSpeak)
        if (voices.isEmpty()) {
            Log.d(
                "SPEECH_VIEWXYZ",
                "getVoiceForNameAndLang - $name LANG: $langToSpeak EMPTY VOICES LIST"
            )
            return null
        }

//        Log.d("SPEECH_VIEWXYZ", "getVoiceForNameAndLang - $name LANG: $langToSpeak VOICES FOUND: $voices")

        for (voice in voices) {
//            Log.d("SPEECH_VIEWX", "getVoiceForNameAndLang - $name LANG: $langToSpeak VOICE: ${voice.name}")
            if (voice.name.contains(name)) {
                Log.d(
                    "SPEECH_VIEWXYZ",
                    "getVoiceForNameAndLang - $name LANG: $langToSpeak VOICE FOUND: ${voice.name}"
                )
                return voice
            }
        }

        return null
    }


    fun getVoicesForLang(langToSpeak: String = speechLang): List<Voice> {
        var voicesForLang: MutableList<Voice> = mutableListOf()
//        Log.d("SPEECH_VIEWX", "getVoicesForLang - tts voices: ${tts.voices} LANG TO SPEAK: $langToSpeak")

        // it MAY be null, though undocumented, and it brings the Java null pointer exception
        if (tts.voices == null) {
            return voicesForLang
        }
        for (voice in tts.voices) {
            if (voice.locale.language == langToSpeak) {
                voicesForLang.add(voice)
            }
        }

        return voicesForLang
    }

    fun talkFreeText(
        freeText: String,
        langToSpeak: String = speechLang,
        voiceToSpeak: Voice? = null,
    ) {
        if (speechLang == "") {
            speechLang = defaultSpeechLang
        }

        if (!isTtsInitialized) {
            println("====> Tts IS NOT initialized. lang to speak: $langToSpeak")
        }

        if (tts.isSpeaking) {
            tts.stop()
        }

        if (voiceToSpeak != null) {
            tts.setVoice(voiceToSpeak)
        }
        tts.speak(freeText, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    fun setVoice(voice: Voice) {
        tts.setVoice(voice)
    }

    fun synthFreeTextToFile(text: String, params: Bundle?, file: File, utteranceId: String = "", listener: UtteranceProgressListener? = null) {
        if (listener != null) {
            Log.d("SYNTH_LISTENER", "synthFreeTextToFile LISTENER IS NOT NULL -- TEXT: $text FILE: $file UTTERANCE ID: $utteranceId")
            tts.setOnUtteranceProgressListener(listener)
        } else {
            Log.d("SYNTH_LISTENER", "synthFreeTextToFile LISTENER IS NULL -- TEXT: $text FILE: $file UTTERANCE ID: $utteranceId")
        }
        Log.d("SYNTH_LISTENER", "synthFreeTextToFile TEXT: $text FILE: $file UTTERANCE ID: $utteranceId")
//        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "")
        tts.synthesizeToFile(text, params, file, utteranceId)
    }

    fun talkPhraseToFile(
        phrase: PhraseToTalk,
        file: File,
        langToSpeak: String = speechLang,
        voiceToSpeak: Voice? = null,
    ) {
        println("TALK TO DOCTOR - TTS VOICES: ${tts.voices.size}")

        if (speechLang == "") {
            speechLang = defaultSpeechLang
        }

        if (!isTtsInitialized) {
            println("====> Tts IS NOT initialized. lang to speak: $langToSpeak")
        }

        if (tts.isSpeaking) {
            tts.stop()
        }

        println("====> VOICE_VOICE -- Tts IS initialized. lang to speak: $langToSpeak")

        val text = phrase.phraseInLanguage(langToSpeak)
        if (text == "") {
            println("====> VOICE_VOICE -- PHRASE ID: ${phrase.id} - TEXT NOT FOUND FOR LANGUAGE $langToSpeak")
            return
        } else {
            println(
                "====> PHRASE ID ${phrase.id} - SAYING it in language $langToSpeak: \n\t" +
                        "${phrase.lang_versions[langToSpeak]}"
            )
        }

        if (voiceToSpeak != null) {
            tts.setVoice(voiceToSpeak)
        }

        tts.synthesizeToFile(text, null, file, "")
//        tts.speak("test", TextToSpeech.QUEUE_ADD, null, "")
//        tts.playSilentUtterance(3000, TextToSpeech.QUEUE_ADD, "")
//        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "")
    }


    fun talkPhrase(
        phrase: PhraseToTalk,
        langToSpeak: String = speechLang,
        voiceToSpeak: Voice? = null,
    ) {
        println("TALK TO DOCTOR - TTS VOICES: ${tts.voices.size}")

        if (speechLang == "") {
            speechLang = defaultSpeechLang
        }

        if (!isTtsInitialized) {
            println("====> Tts IS NOT initialized. lang to speak: $langToSpeak")
        }

        if (tts.isSpeaking) {
            tts.stop()
        }

        println("====> VOICE_VOICE -- Tts IS initialized. lang to speak: $langToSpeak")

        val text = phrase.phraseInLanguage(langToSpeak)
        if (text == "") {
            println("====> VOICE_VOICE -- PHRASE ID: ${phrase.id} - TEXT NOT FOUND FOR LANGUAGE $langToSpeak")
            return
        } else {
            println(
                "====> PHRASE ID ${phrase.id} - SAYING it in language $langToSpeak: \n\t" +
                        "${phrase.lang_versions[langToSpeak]}"
            )
        }

        if (voiceToSpeak != null) {
            tts.setVoice(voiceToSpeak)
        }

        tts.speak("test", TextToSpeech.QUEUE_ADD, null, "")
//        tts.playSilentUtterance(3000, TextToSpeech.QUEUE_ADD, "")
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "")
    }

}
