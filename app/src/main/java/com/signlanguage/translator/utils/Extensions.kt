package com.signlanguage.translator.utils

import java.util.Locale

fun Float.asPercentText(): String = String.format(Locale("tr", "TR"), "%%%d", (this * 100).toInt())

fun Long.asMillisText(): String = String.format(Locale("tr", "TR"), "%d ms", this)
