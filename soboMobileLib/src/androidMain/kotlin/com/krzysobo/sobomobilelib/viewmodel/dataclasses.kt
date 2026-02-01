package com.krzysobo.sobomobilelib.viewmodel

import android.content.Intent
import androidx.compose.ui.focus.FocusRequester
import com.krzysobo.soboapptpl.service.AnyRes
import com.krzysobo.soboapptpl.viewmodel.AnyImage

data class AppItem(
    val packageName: String = "",
    val title: AnyRes? = null,
    val image: AnyImage? = null,
    val focusRequester: FocusRequester? = null,   // ADDED HERE
    val launchIntent: Intent? = null,
)
