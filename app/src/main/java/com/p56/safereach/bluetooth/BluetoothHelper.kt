package com.p56.safereach.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission

object BluetoothHelper {

    const val TAG = "BluetoothHelper"

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun requestEnableBluetooth(context: Context) {
        try {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            context.startActivity(intent)
            return
        } catch (e: Exception) {
            Log.w(TAG, "ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS failed, falling back", e)
        }
    }
}