package com.printer.utils

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.util.Log
import java.io.IOException
import java.util.*

/**
 *
 * @author wsl
 * @date 2018/8/9 0009
 */

class BluetoothUtil {

    companion object {
        /**
         * 蓝牙是否打开
         */
        fun isBluetoothOn(): Boolean {
            val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (mBluetoothAdapter != null)
            // 蓝牙已打开
                if (mBluetoothAdapter.isEnabled)
                    return true
            return false
        }

        fun isOpen(adapter: BluetoothAdapter?): Boolean {
            return null != adapter && adapter.isEnabled
        }

        // 搜索蓝牙设备
        fun searchDevices(adapter: BluetoothAdapter?) {
            // 寻找蓝牙设备，android会将查找到的设备以广播形式发出去
            adapter?.startDiscovery()
        }

        // 取消搜索
        fun cancelDiscovery(adapter: BluetoothAdapter?) {
            adapter?.cancelDiscovery()
        }


        /**
         * 获取所有已配对的设备
         */
        fun getPairedDevices(): List<BluetoothDevice> {
            val deviceList = ArrayList<BluetoothDevice>()
            val pairedDevices = BluetoothAdapter.getDefaultAdapter().bondedDevices
            Log.e("wsl", "pairedDevices count ${pairedDevices.size}")
            if (!pairedDevices.isEmpty()) {
                deviceList.addAll(pairedDevices)
            }
            return deviceList
        }

        /**
         * 获取所有已配对的打印类设备
         */
        @Suppress("unused")
        fun getPairedPrinterDevices(): List<BluetoothDevice> {
            return getSpecificDevice(BluetoothClass.Device.Major.IMAGING)
        }

        /**
         * 从已配对设配中，筛选出某一特定类型的设备展示
         *
         * @param deviceClass
         * @return
         */
        private fun getSpecificDevice(deviceClass: Int): List<BluetoothDevice> {
            val devices = BluetoothUtil.getPairedDevices()
            val printerDevices = ArrayList<BluetoothDevice>()

            for (device in devices) {
                val klass = device.bluetoothClass
                // 关于蓝牙设备分类参考 http://stackoverflow.com/q/23273355/4242112
                if (klass.majorDeviceClass == deviceClass)
                    printerDevices.add(device)
            }

            return printerDevices
        }

        /**
         * 弹出系统对话框，请求打开蓝牙
         */
        fun openBluetooth(activity: Activity) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(enableBtIntent, 666)
        }

        fun connectDevice(device: BluetoothDevice): BluetoothSocket? {
            var socket: BluetoothSocket? = null
            try {
                socket = device.createRfcommSocketToServiceRecord(
                        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                socket?.connect()
            } catch (e: IOException) {
                try {
                    socket?.close()
                } catch (closeException: IOException) {
                    return null
                }

                return null
            }
            return socket
        }

    }

}