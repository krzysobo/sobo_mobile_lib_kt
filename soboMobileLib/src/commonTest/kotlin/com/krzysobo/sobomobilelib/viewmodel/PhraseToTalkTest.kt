package com.krzysobo.sobomobilelib.viewmodel

import com.krzysobo.soboapptpl.service.LocaleManager
import com.krzysobo.soboapptpl.service.LocaleWorker
import com.krzysobo.soboapptpl.service.LocaleWorkerAppSettingsOnly
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PhraseToTalkTest {
    private lateinit var savedWorker: LocaleWorker

    private val phrase = PhraseToTalk(
        id = "water",
        lang_versions = hashMapOf("pl" to "Poproszę wodę", "en" to "Water, please"),
        category = "comfort",
    )
    private val categoryNames = hashMapOf("pl" to "Komfort", "en" to "Comfort")

    @BeforeTest
    fun setUp() {
        savedWorker = LocaleManager.localeWorker
        LocaleManager.localeWorker = LocaleWorkerAppSettingsOnly()
    }

    @AfterTest
    fun tearDown() {
        LocaleManager.localeWorker = savedWorker
    }

    @Test
    fun phraseInLanguage_defaultsToPolish_andIsEmptyForMissingLanguage() {
        assertEquals("Poproszę wodę", phrase.phraseInLanguage())
        assertEquals("Water, please", phrase.phraseInLanguage("en"))
        assertEquals("", phrase.phraseInLanguage("tr"))
    }

    @Test
    fun phraseInCurrentLanguage_followsLocaleManager() {
        LocaleManager.setAppLocaleByLang("en")
        assertEquals("Water, please", phrase.phraseInCurrentLanguage())

        LocaleManager.setAppLocaleByLang("pl")
        assertEquals("Poproszę wodę", phrase.phraseInCurrentLanguage())

        LocaleManager.setAppLocaleByLang("de")
        assertEquals("", phrase.phraseInCurrentLanguage())
    }

    @Test
    fun categoryName_inCurrentLanguage() {
        LocaleManager.setAppLocaleByLang("en")
        assertEquals("Comfort", categoryNameInCurrentLanguage(categoryNames))
        assertEquals("Comfort", phrase.categoryNameInCurrentLanguage(categoryNames))

        LocaleManager.setAppLocaleByLang("fr")
        assertEquals("", categoryNameInCurrentLanguage(categoryNames))
    }
}
