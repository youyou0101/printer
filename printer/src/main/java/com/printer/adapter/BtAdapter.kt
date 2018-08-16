package com.printer.adapter

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.support.v4.content.ContextCompat
import android.widget.ImageView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.printer.bean.BluetoothBean
import com.printer.R

/**
 *
 * @author wsl
 * @date 2018/8/9 0009
 */
class BtAdapter(val context: Context, layoutResId: Int, data: List<BluetoothBean>): BaseQuickAdapter<BluetoothBean, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, item: BluetoothBean) {
        val name = item.bluetoothDevice.name?: item.bluetoothDevice.address
        val state = if (item.bluetoothDevice.bondState == BluetoothDevice.BOND_BONDED) {
            "已配对"
        } else {
            "未配对"
        }

        helper.setText(R.id.name, "$name($state)")
        val imgSelect = helper.getView<ImageView>(R.id.img_select)
        imgSelect.setImageDrawable(ContextCompat.getDrawable(context, if (item.isSelected) R.drawable.icon_choice else R.drawable.icon_not_select))
    }
}
