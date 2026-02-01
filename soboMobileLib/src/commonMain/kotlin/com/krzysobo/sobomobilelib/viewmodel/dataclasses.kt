package com.krzysobo.sobomobilelib.viewmodel

import com.krzysobo.soboapptpl.service.LocaleManager
import com.krzysobo.soboapptpl.viewmodel.AnyImage
import com.krzysobo.soboapptpl.widgets.LangOpt

fun categoryNameInCurrentLanguage(catData: HashMap<String, String>): String {
    val lang = LocaleManager.getCurrentLang()
    if (catData.containsKey(lang)) {
        return catData[lang]!!
    } else {
        return ""
    }
}

data class PhraseToTalk(
    val id: String,
    val lang_versions: HashMap<String, String> = hashMapOf(),
    val category: String = "",
) {
    fun phraseInLanguage(lang: String = "pl"): String {
        if (lang_versions.containsKey(lang)) {
            return lang_versions[lang]!!
        } else {
            return ""
        }
    }

    fun phraseInCurrentLanguage(): String {
        return phraseInLanguage(LocaleManager.getCurrentLang())
    }

    fun categoryNameInCurrentLanguage(catData: HashMap<String, String>): String {
        val lang = LocaleManager.getCurrentLang()
        if (catData.containsKey(lang)) {
            return catData[lang]!!
        } else {
            return ""
        }
    }
}

data class LangWithFlag(val langOpt: LangOpt, val image: AnyImage)
