package com.example.june.core.domain.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toFullDateWithDay(): String {
    val sdf = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toFullDate(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toFullDateTime(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    return sdf.format(Date(this))
}