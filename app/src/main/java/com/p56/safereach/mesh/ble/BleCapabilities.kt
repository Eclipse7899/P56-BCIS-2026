package com.p56.safereach.mesh.ble

import android.bluetooth.BluetoothAdapter

object BleCapabilities {
    fun missingFeatures(adapter: BluetoothAdapter?): List<BleFeature> {
        if (adapter == null) return BleFeature.entries.toList()
        return missingFeatures(
            leCodedPhySupported = adapter.isLeCodedPhySupported,
            leExtendedAdvertisingSupported = adapter.isLeExtendedAdvertisingSupported,
        )
    }

    fun missingFeatures(
        leCodedPhySupported: Boolean,
        leExtendedAdvertisingSupported: Boolean,
    ): List<BleFeature> = buildList {
        if (!leCodedPhySupported) add(BleFeature.LeCodedPhy)
        if (!leExtendedAdvertisingSupported) add(BleFeature.LeExtendedAdvertising)
    }
}
