package com.p56.safereach.android

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
            permissions.plusAssign(Manifest.permission.POST_NOTIFICATIONS)
        }

        permissions.addAll(requiredBlePermissions())

        return permissions
    }

    fun requiredConnectedDevicePermissions(): Set<String> {
        val permissions = mutableSetOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.plusAssign(Manifest.permission.BLUETOOTH_SCAN)
            permissions.plusAssign(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.plusAssign(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        return permissions
    }

    fun requiredLocationPermissions(): Set<String> {
        val permissions = mutableSetOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            permissions.plusAssign(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        return permissions
    }

    fun requiredScanPermissions(): Set<String> {
        val permissions = mutableSetOf<String>()
        permissions.plusAssign(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.plusAssign(Manifest.permission.BLUETOOTH_SCAN)
            permissions.plusAssign(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.plusAssign(Manifest.permission.BLUETOOTH)
            permissions.plusAssign(Manifest.permission.BLUETOOTH_ADMIN)
            permissions.plusAssign(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        return permissions
    }

    fun requiredAdvertisingPermissions(): Set<String> {
        val permissions = mutableSetOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.plusAssign(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            permissions.plusAssign(Manifest.permission.BLUETOOTH)
            permissions.plusAssign(Manifest.permission.BLUETOOTH_ADMIN)
        }
        return permissions
    }

    fun missingBlePermissions(context: Context): List<String> {
        return missingPermissions(context, requiredBlePermissions())
    }

    fun hasBlePermissions(context: Context): Boolean {
        return missingBlePermissions(context).isEmpty()
    }

    fun missingConnectedDevicePermissions(context: Context): List<String> {
        return missingPermissions(context, requiredConnectedDevicePermissions())
    }

    fun hasConnectedDevicePermissions(context: Context): Boolean {
        return missingConnectedDevicePermissions(context).isEmpty()
    }

    fun missingLocationPermissions(context: Context): List<String> {
        return missingPermissions(context, requiredLocationPermissions())
    }

    fun hasLocationPermissions(context: Context): Boolean {
        return missingLocationPermissions(context).isEmpty()
    }

    private fun missingPermissions(
        context: Context,
        permissions: Collection<String>,
    ): List<String> {
        return permissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) !=
                    PackageManager.PERMISSION_GRANTED
        }
    }
}