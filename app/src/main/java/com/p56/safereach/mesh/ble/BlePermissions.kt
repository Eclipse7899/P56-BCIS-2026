package com.p56.safereach.mesh.ble

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object BlePermissions {
    fun requiredBlePermissions(): Set<String> {
        val permissions = mutableSetOf<String>()
        permissions.addAll(requiredScanPermissions())
        permissions.addAll(requiredAdvertisingPermissions())
        return permissions
    }

    fun requiredRuntimePermissions(): Collection<String> {
        val permissions = mutableSetOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }

        permissions.addAll(requiredBlePermissions())

        return permissions
    }

    fun requiredScanPermissions(): Set<String> {
        val permissions = mutableSetOf<String>()
        permissions += Manifest.permission.ACCESS_FINE_LOCATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_SCAN
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            permissions += Manifest.permission.BLUETOOTH
            permissions += Manifest.permission.BLUETOOTH_ADMIN
            permissions += Manifest.permission.ACCESS_COARSE_LOCATION
        }
        return permissions
    }

    fun requiredAdvertisingPermissions(): Set<String> {
        val permissions = mutableSetOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_ADVERTISE
        } else {
            permissions += Manifest.permission.BLUETOOTH
            permissions += Manifest.permission.BLUETOOTH_ADMIN
        }
        return permissions
    }

    fun missingBlePermissions(context: Context): List<String> {
        return requiredBlePermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) !=
                    PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasBlePermissions(context: Context): Boolean {
        return missingBlePermissions(context).isEmpty()
    }
}

