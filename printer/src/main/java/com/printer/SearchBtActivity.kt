package com.printer

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.os.ParcelUuid
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.base.ui.activity.BaseActivity
import com.base.utils.StatusBarUtil
import com.chad.library.adapter.base.BaseQuickAdapter
import com.freeler.titlebar.TitleBar
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.internal.RxBleLog
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import com.printer.adapter.BtAdapter
import com.printer.adapter.Decoration
import com.printer.bean.BluetoothBean
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_printer.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.util.*
import kotlin.collections.ArrayList

/**
 *
 * @author wsl
 * @date 2018/8/9 0009
 */
@RuntimePermissions
class SearchBtActivity : BaseActivity(), BaseQuickAdapter.OnItemClickListener {

    private lateinit var btAdapter: BtAdapter
    private var devices: MutableList<BluetoothBean> = mutableListOf()

    private lateinit var rxBleClient: RxBleClient
    private var scanDisposable: Disposable? = null

    private var progressDialog: MaterialDialog? = null

    companion object {
        const val REQUEST_CODE_PRINT = 12
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        isImmersive = StatusBarUtil.StatusBarLightMode(this) != 0
        super.onCreate(savedInstanceState)
    }

    private fun findBle() {
        val parcelUuid = ParcelUuid(UUID.fromString("e7810a71-73ae-499d-8c15-faa9aef0c3f2"))
        scanDisposable = rxBleClient.scanBleDevices(
                ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .build(),
                ScanFilter.Builder()
                        .setServiceUuid(parcelUuid)
                        .build()
        )
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally {
                    scanDisposable = null
                    btAdapter.data.clear()
                    btAdapter.notifyDataSetChanged()
                }
                .subscribe({ onFound(it) }, { this.onScanFailure(it) })
    }

    override fun buildView(): Int {
        return R.layout.activity_printer
    }

    override fun viewDidCreated() {
        initTitleBar()
        initRecyclerView()
        initProgressDialog()

        ///////////////////////////////////
        rxBleClient = RxBleClient.create(this)
        RxBleClient.setLogLevel(RxBleLog.VERBOSE)
    }

    private fun initTitleBar() {
        titleBar.hideStateBar(!isImmersive)

        val refreshAction = object : TitleBar.ImageAction(R.drawable.icon_refresh) {
            override fun performAction(view: View) {
                searchDeviceOrOpenBluetooth()
            }
        }

        titleBar.addAction(refreshAction)

        titleBar.defaultWhiteStyle(this, "取消", "选择打印机", "打印", {
            finish()
        }, {
            val selectedBeans = devices.filter { it.isSelected }
            if (selectedBeans.isEmpty()) {
               // toast("请选择打印设备")
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

        private fun initProgressDialog() {
            progressDialog = MaterialDialog.Builder(this)
                    .content("请稍等...")
                    .canceledOnTouchOutside(false)
                    .progress(true, 0)
                    .build()
    }

    private fun searchDeviceOrOpenBluetooth() {
        openBluetoothWithPermissionCheck()
    }

    @NeedsPermission(Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION)
    fun openBluetooth() {
        // 先清空已有数据
        scanDisposable?.dispose()
        scanDisposable = null
        devices.clear()
        btAdapter.notifyDataSetChanged()
        progressDialog?.show()
        findBle()
    }

    override fun onStart() {
        super.onStart()
        searchDeviceOrOpenBluetooth()
    }

    override fun onStop() {
        super.onStop()
       dispose()
    }

    private fun dispose() {
        scanDisposable?.dispose()
        scanDisposable = null
        progressDialog?.dismiss()
    }

    private fun onFound(scanResult: ScanResult) {
        val blueDevice = scanResult.bleDevice.bluetoothDevice
        if (devices.filter { it.bluetoothDevice.address == blueDevice.address }.elementAtOrNull(0) == null) {
            devices.add(BluetoothBean(blueDevice, false))
            devices[0].isSelected = true
            btAdapter.notifyDataSetChanged()
        }

        if (devices.isNotEmpty()) {
            progressDialog?.dismiss()
        }
    }

    private fun handleBleScanException(bleScanException: BleScanException) {
        val text: String = when (bleScanException.reason) {
            BleScanException.BLUETOOTH_NOT_AVAILABLE -> "蓝牙不可用"
            BleScanException.BLUETOOTH_DISABLED -> "请打开蓝牙后重试"
            BleScanException.LOCATION_PERMISSION_MISSING -> "请打开定位权限"
            BleScanException.LOCATION_SERVICES_DISABLED -> "Location services needs to be enabled on Android 6.0"
            BleScanException.SCAN_FAILED_ALREADY_STARTED -> "Scan with the same filters is already started"
            BleScanException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Failed to register application for bluetooth scan"
            BleScanException.SCAN_FAILED_FEATURE_UNSUPPORTED -> "Scan with specified parameters is not supported"
            BleScanException.SCAN_FAILED_INTERNAL_ERROR -> "Scan failed due to internal error"
            BleScanException.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "搜索失败"
            BleScanException.UNDOCUMENTED_SCAN_THROTTLE -> ""
            BleScanException.UNKNOWN_ERROR_CODE, BleScanException.BLUETOOTH_CANNOT_START -> ""
            else -> "搜索失败"
        }

        Log.e("wsl", text, bleScanException)
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun onScanFailure(throwable: Throwable) {
        dispose()
        if (throwable is BleScanException) {
            handleBleScanException(throwable)
        }
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>, view: View, position: Int) {
        btAdapter.getItem(position) ?: return

        val bean = btAdapter.getItem(position)

        bean?.bluetoothDevice?.let {
            if (it.bondState == BluetoothDevice.BOND_BONDED) {
                bean.isSelected = !bean.isSelected
                //notifyDataSetChanged()
                btAdapter.notifyDataSetChanged()
            } else {
                val name = it.name ?: it.address
                AlertDialog.Builder(this)
                        .setTitle("绑定$name?")
                        .setMessage("点击确认绑定蓝牙设备")
                        .setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }
                        .setPositiveButton("确认") { _, _ ->
                            try {
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        dispose()
        progressDialog = null
    }

}