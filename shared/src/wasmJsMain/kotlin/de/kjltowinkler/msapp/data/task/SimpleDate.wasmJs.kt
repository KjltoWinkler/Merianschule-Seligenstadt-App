package de.kjltowinkler.msapp.data.task

import kotlin.js.JsFun

@JsFun("() => new Date().getFullYear()")
private external fun jsGetFullYear(): Int

@JsFun("() => new Date().getMonth() + 1")
private external fun jsGetMonth(): Int

@JsFun("() => new Date().getDate()")
private external fun jsGetDate(): Int

@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

actual fun getTodayDate(): SimpleDate = SimpleDate(
    year = jsGetFullYear(),
    monthNumber = jsGetMonth(),
    dayOfMonth = jsGetDate()
)

actual fun getCurrentEpochMillis(): Long = jsDateNow().toLong()
