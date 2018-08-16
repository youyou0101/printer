package com.printer

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.Toast
import com.base.ui.activity.BaseActivity
import com.base.utils.StatusBarUtil
import com.chad.library.adapter.base.BaseQuickAdapter
import com.freeler.titlebar.TitleBar
import com.printer.adapter.BtAdapter
import com.printer.adapter.Decoration
import com.printer.bean.BluetoothBean
import com.printer.bean.BtCallBack
import com.printer.utils.BluetoothUtil
import kotlinx.android.synthetic.main.activity_printer.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions

/**
 * yuxiaor
 *
 * @author wsl
 * @date 2018/8/9 0009
 */
@RuntimePermissions
class SearchBtOnReceiverActivity : BaseActivity(), BtCallBack, BaseQuickAdapter.OnItemClickListener {

    //  private var progressDialog: MaterialDialog? = null

    private var btBroadcastReceiver: BtBroadcastReceiver? = null
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var btAdapter: BtAdapter
    private var devices: MutableList<BluetoothBean> = mutableListOf()

    companion object {
        const val REQUEST_CODE_PRINT = 12
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        isImmersive = StatusBarUtil.StatusBarLightMode(this) != 0
        super.onCreate(savedInstanceState)
    }

    override fun buildView(): Int {
        return R.layout.activity_printer
    }

    override fun viewDidCreated() {
        initTitleBar()
        btBroadcastReceiver = BtBroadcastReceiver(this)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        initRecyclerView()
        //initProgressDialog()
        searchDeviceOrOpenBluetooth()
    }

    fun initTitleBar() {
        titleBar.hideStateBar(!isImmersive)

        val refreshAction = object : TitleBar.ImageAction(R.drawable.icon_refresh) {
            override fun performAction(view: View) {
                BluetoothUtil.cancelDiscovery(bluetoothAdapter)
                searchDeviceOrOpenBluetooth()
            }
        }

        titleBar.addAction(refreshAction)

        titleBar.defaultWhiteStyle(this, "取消", "选择打印机", "打印", {
            finish()
        }, {
            val selectedBeans = devices.filter { it.isSelected }
            if (selectedBeans.isEmpty()) {
                Toast.makeText(this, "请选择打印设备", Toast.LENGTH_SHORT).show()
            } else {
                val devices: ArrayList<BluetoothDevice> = selectedBeans.map { it.bluetoothDevice } as ArrayList<BluetoothDevice>
                val bundle = Bundle()
                bundle.putParcelableArrayList("BluetoothDevice", devices)
                setResult(Activity.RESULT_OK, Intent().putExtras(bundle))
                finish()
            }
        })

    }

    private fun initRecyclerView() {
        recyclerView.addItemDecoration(Decoration())
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = RecyclerView.VERTICAL
        recyclerView.layoutManager = linearLayoutManager
        btAdapter = BtAdapter(this, R.layout.item_bluetooth, devices)
        btAdapter.onItemClickListener = this
        recyclerView.adapter = btAdapter
    }

    private fun searchDeviceOrOpenBluetooth() {
        openBluetoothWithPermissionCheck()
    }

    @NeedsPermission(Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION)
    fun openBluetooth() {
        // 先清空已有数据
        devices.clear()
        btAdapter.notifyDataSetChanged()
        // 1、判断蓝牙是否打开
        if (BluetoothUtil.isOpen(bluetoothAdapter)) {
            BluetoothUtil.searchDevices(bluetoothAdapter)
        }
    }

//    private fun initProgressDialog() {
//            progressDialog = MaterialDialog.Builder(context)
//                    .content("请稍等...")
//                    .canceledOnTouchOutside(false)
//                    .progress(true, 0)
//                    .build()
//    }

    override fun onStart() {
        super.onStart()
        registerBtBroadcastReceiver()
    }

    override fun onStop() {
        super.onStop()
        unRegisterBtReceiver()
        BluetoothUtil.cancelDiscovery(bluetoothAdapter)
    }

    private fun registerBtBroadcastReceiver() {
        if (btBroadcastReceiver != null) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            intentFilter.addAction(BluetoothDevice.ACTION_FOUND)
            intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            intentFilter.addAction("android.bluetooth.device.action.PAIRING_REQUEST")
            registerReceiver(btBroadcastReceiver, intentFilter)
        }
    }

    private fun unRegisterBtReceiver() {
        btBroadcastReceiver?.let { unregisterReceiver(it) }
    }

    override fun discoveryStarted(intent: Intent) {
        // progressDialog?.show()
    }

    override fun discoveryFinished(intent: Intent) {
        // progressDialog?.dismiss()
    }

    override fun stateChanged(intent: Intent) {
        when(bluetoothAdapter.state) {
            BluetoothAdapter.STATE_OFF -> bluetoothAdapter.enable()//蓝牙被关闭时强制打开
            BluetoothAdapter.STATE_ON ->  searchDeviceOrOpenBluetooth()//蓝牙打开时搜索蓝牙
        }
    }

    override fun found(intent: Intent) {
        // 判断是否是蓝牙热敏打印机
        //E7810A71-73AE-499D-8C15-FAA9AEF0C3F2
        //e7810a71-73ae-499d-8c15-faa9aef0c3f2
        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        device?.let {d ->
            val hasDevice: BluetoothBean? = devices.filter { it.bluetoothDevice.address == d.address }.elementAtOrNull(0)
            if (hasDevice == null) {
                devices.add(BluetoothBean(d, false))
            }
            this.notifyDataSetChanged()
        }
    }

    override fun boundStateChanged(intent: Intent) {
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        when (device.bondState) {
            BluetoothDevice.BOND_BONDING//正在配对
            -> Log.e("wsl", "正在配对......")
            BluetoothDevice.BOND_BONDED//配对结束
            -> {
                Log.e("wsl", "完成配对")
                notifyDataSetChanged()
            }
            BluetoothDevice.BOND_NONE//取消配对/未配对
            -> Log.e("wsl", "取消配对")
            else -> {
            }
        }
    }

    override fun pairingRequest(intent: Intent) {

    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>, view: View, position: Int) {
        btAdapter.getItem(position) ?: return

        val bean = btAdapter.getItem(position)

        bean?.bluetoothDevice?.let {
            if (it.bondState == BluetoothDevice.BOND_BONDED) {
                bean.isSelected = !bean.isSelected
                notifyDataSetChanged()
            } else {
                val name = it.name ?: it.address
                AlertDialog.Builder(this)
                        .setTitle("绑定$name?")
                        .setMessage("点击确认绑定蓝牙设备")
                        .setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }
                        .setPositiveButton("确认") { _, _ ->
                            try {
                                BluetoothUtil.cancelDiscovery(bluetoothAdapter)
                                val createBondMethod = BluetoothDevice::class.java.getMethod("createBond")
                                createBondMethod.invoke(it)
                                btAdapter.notifyDataSetChanged()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(this, "蓝牙绑定失败,请重试", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .create()
                        .show()
            }
        }
    }

    fun notifyDataSetChanged() {
        this.devices = sortByBond(this.devices)
        btAdapter.notifyDataSetChanged()
    }

    private fun sortByBond(mDevices: MutableList<BluetoothBean>): MutableList<BluetoothBean> {
        if (mDevices.size < 2) {
            return mDevices
        }
        mDevices.sortBy { it.bluetoothDevice.bondState != BluetoothDevice.BOND_BONDED }
        return mDevices
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

}