package com.printer.bean

import android.content.Intent

/**
 *
 * @author wsl
 * @date 2018/8/9 0009
 */

interface BtCallBack {

    // BluetoothAdapter.ACTION_DISCOVERY_STARTED
    fun discoveryStarted(intent: Intent)

    // BluetoothAdapter.ACTION_DISCOVERY_FINISHED
    fun discoveryFinished(intent: Intent)

    // BluetoothAdapter.ACTION_STATE_CHANGED
    fun stateChanged(intent: Intent)

    // BluetoothDevice.ACTION_FOUND
    fun found(intent: Intent)

    // BluetoothDevice.ACTION_BOND_STATE_CHANGED
    fun boundStateChanged(intent: Intent)

    // BluetoothDevice.ACTION_PAIRING_REQUEST
    fun pairingRequest(intent: Intent)

}