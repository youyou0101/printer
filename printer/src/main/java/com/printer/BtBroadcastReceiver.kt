package com.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import com.printer.bean.BtCallBack

/**
 *
 * @author wsl
 * @date 2018/8/9 0009
 */
class BtBroadcastReceiver(private val btCallBack: BtCallBack):BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            val action = intent.action
            if (TextUtils.isEmpty(action)) {
                return
            }

            when(action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> btCallBack.discoveryStarted(it)
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> btCallBack.discoveryFinished(it)
                BluetoothAdapter.ACTION_STATE_CHANGED -> btCallBack.stateChanged(it)
                BluetoothDevice.ACTION_FOUND -> btCallBack.found(it)
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> btCallBack.boundStateChanged(it)
                BluetoothDevice.ACTION_PAIRING_REQUEST -> btCallBack.pairingRequest(it)
            }
        }
    }
}