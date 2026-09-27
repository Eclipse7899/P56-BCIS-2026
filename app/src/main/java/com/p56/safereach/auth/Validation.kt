package com.p56.safereach.auth

import android.util.Patterns

fun isValidEmail(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS
        .matcher(email.trim())
        .matches()
}

fun isValidPassword(password: String): Boolean {
    return password.length >= 8
}