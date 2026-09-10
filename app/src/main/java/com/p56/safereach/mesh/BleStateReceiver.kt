package com.p56.safereach.mesh

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class BleStateReceiver(
    private val onBluetoothEnabledChanged: (Boolean) -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            when (state) {
                BluetoothAdapter.STATE_ON -> onBluetoothEnabledChanged(true)
                BluetoothAdapter.STATE_TURNING_OFF,
                BluetoothAdapter.STATE_OFF,
                BluetoothAdapter.STATE_TURNING_ON -> onBluetoothEnabledChanged(false)
            }
        }
    }

    companion object {
        const val ACTION = BluetoothAdapter.ACTION_STATE_CHANGED
        fun filter(): IntentFilter = IntentFilter(ACTION)
    }
}