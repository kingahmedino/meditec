package com.app.meditec.utils

object StringFormatter {
    private val regex = Regex("\\<.*?\\>")

    fun removeAngleBrackets(text: String): String {
        return text.replace(regex, "")
    }
}