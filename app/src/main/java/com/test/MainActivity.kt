package com.test

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.printer.SearchBtActivity
import com.printer.utils.BluetoothUtil
import com.printer.utils.PrintUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_print.setOnClickListener({
            val intent = Intent()
            intent.setClass(this, SearchBtActivity::class.java)
            startActivityForResult(intent, SearchBtActivity.REQUEST_CODE_PRINT)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == SearchBtActivity.REQUEST_CODE_PRINT) {
            val devices = data?.extras!!.getParcelableArrayList<BluetoothDevice>("BluetoothDevice")
            print(devices)
        }
    }

    private fun print(printDevices: List<BluetoothDevice>) {
        printDevices.forEach { printer(it) }
    }

    // 连接并打印
    private fun printer(device: BluetoothDevice) {
        var disposable: Disposable? = null
        disposable = Observable.just(device).map { BluetoothUtil.connectDevice(it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it?.isConnected == false) {
                        Toast.makeText(this, "打印机连接失败", Toast.LENGTH_SHORT).show()
                        return@subscribe
                    }
                    printInfo(it)
                    it?.close()
                    disposable?.dispose()
                }, {
                    if (it.cause.toString() == "null") {
                        Toast.makeText(this, "打印机连接失败", Toast.LENGTH_SHORT).show()
                        return@subscribe
                    }
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                    disposable?.dispose()
                })
    }

    private fun printInfo(bluetoothSocket: BluetoothSocket?) {
        bluetoothSocket?.let {
                try {
                    val print = PrintUtil(bluetoothSocket.outputStream, "GBK")
                    // 标题 居中 放大
                    print.printAlignment(1)
                    print.printLargeText("测试")
                    print.printLine()
                    print.printAlignment(0)
                    print.printLine()

                    print.printText("客户：张三")
                    print.printLine()

                    print.printText("地址：北京")
                    print.printLine(2)

                    print.printTwoColumn("费用项", "金额")
                    print.printLine()

                    print.printDashLine()
                    print.printTwoColumn("费用项1", 1.0.toString())
                    print.printLine(2)
                    print.printTwoColumn("费用项2", 2.0.toString())
                    print.printLine(2)
                    print.printTwoColumn("费用项3", 3.0.toString())
                    print.printLine(2)
                    print.printTwoColumn("费用项4", 4.0.toString())
                    print.printLine(2)
                    print.printTwoColumn("费用项5", 5.0.toString())
                    print.printLine(2)
                    print.printTwoColumn("费用项6", 6.0.toString())
                    print.printDashLine()

                    print.printLine(2)

                    print.printText("支付时间：${Date()}")
                    print.printLine(2)

                    print.printText("签字：")

                    print.printLine(5)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    }

}
