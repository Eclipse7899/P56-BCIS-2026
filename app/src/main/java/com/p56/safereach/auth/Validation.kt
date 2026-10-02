package com.p56.safereach.auth

import android.util.Patterns

fun isValidEmail(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS
        .matcher(email.trim())
        .matches()
}

fun isValidPassword(password: String): Boolean {
    val hasMinLength = password.length >= 8
    val hasNumber = password.any { it.isDigit() }
    val hasCapitalLetter = password.any { it.isUpperCase() }

    return hasMinLength &&
            hasNumber &&
            hasCapitalLetter
}