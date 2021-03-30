package com.app.meditec.utils

import org.junit.Test

import org.junit.Assert.*

class StringFormatterTest {

    @Test
    fun removeAngleBrackets() {
        val formattedString = StringFormatter.removeAngleBrackets("Good <b>guy</b>")
        assertEquals(formattedString, "Good  guy")
    }
}