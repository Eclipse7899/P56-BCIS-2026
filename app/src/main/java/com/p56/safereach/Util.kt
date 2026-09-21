package com.p56.safereach

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

const val MS_IN_SECOND = 1000L

fun secondsToMillis(seconds: Long): Long = seconds * MS_IN_SECOND

fun hasPermissions(permissions: Iterable<String>, context: Context): Boolean {
    return permissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

sealed interface Either<out L, out R> {
    data class Left<out L>(val value: L) : Either<L, Nothing>
    data class Right<out R>(val value: R) : Either<Nothing, R>
}
