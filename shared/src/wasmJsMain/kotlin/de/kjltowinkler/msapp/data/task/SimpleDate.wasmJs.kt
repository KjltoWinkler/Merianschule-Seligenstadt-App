package de.kjltowinkler.msapp.data.task

import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsGetFullYear(): Int = js("new Date().getFullYear()")

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsGetMonth(): Int = js("new Date().getMonth() + 1")

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsGetDate(): Int = js("new Date().getDate()")

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsGetHours(): Int = js("new Date().getHours()")

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsGetMinutes(): Int = js("new Date().getMinutes()")

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsDateNow(): Double = js("Date.now()")

actual fun getTodayDate(): SimpleDate = SimpleDate(
    year = jsGetFullYear(),
    monthNumber = jsGetMonth(),
    dayOfMonth = jsGetDate()
)

actual fun getCurrentTime(): SimpleTime = SimpleTime(
    hour = jsGetHours(),
    minute = jsGetMinutes()
)

actual fun getCurrentEpochMillis(): Long = jsDateNow().toLong()
