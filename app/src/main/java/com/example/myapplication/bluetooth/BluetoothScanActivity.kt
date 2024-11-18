package com.example.myapplication.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class BluetoothScanActivity : ComponentActivity() {

    val set: HashSet<String> = HashSet()
    val arrayList: ArrayList<BluetoothDevice> = ArrayList()
    val rv: RecyclerView by lazy {
        findViewById(R.id.bluetooth_scan_recycler_view)
    }



    // Create a BroadcastReceiver for ACTION_FOUND.
    private val receiver = object : BroadcastReceiver() {

        @RequiresApi(Build.VERSION_CODES.R)
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action ?: ""
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.

                    intent.getParcelableExtra<BluetoothDevice?>(BluetoothDevice.EXTRA_DEVICE)?.run {
                        val device = this
                        Log.d(
                            "BluetoothScanActivity",
                            "DeviceFound: ${name} ${address} ${alias}"
                        )
                        if (!TextUtils.isEmpty(device.name) && !set.contains(device.address)) {
                            set.add(device.address)
                            arrayList.add(this)
                            rv.adapter?.notifyItemInserted(arrayList.size - 1)

                        }

                    }

                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bluetooth_scan_activity)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = object : RecyclerView.Adapter<TextViewHolder>(), OnClickListener {
            override fun onCreateViewHolder(
                parent: android.view.ViewGroup,
                viewType: Int
            ): TextViewHolder {
                return TextViewHolder(parent.context, this)
            }

            override fun onBindViewHolder(holder: TextViewHolder, position: Int) {
                holder.setData(arrayList[position])
            }

            override fun getItemCount(): Int {
                return arrayList.size
            }

            /**
             * Called when a view has been clicked.
             *
             * @param v The view that was clicked.
             */
            @SuppressLint("MissingPermission")
            override fun onClick(v: View) {
                val childLayoutPosition = rv.getChildLayoutPosition(v)
                val data = arrayList.get(childLayoutPosition)

                setResult(9, Intent().apply {
                    putExtra("dataDevice", data)
                })
                finish()
            }
        }
        scan()
        pair()
    }

    class TextViewHolder(context: Context, onClickListener: OnClickListener) :
        RecyclerView.ViewHolder(Button(context).apply {
            layoutParams = MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(10, 10, 10, 10)
            }
            setBackgroundColor(R.color.purple_200)
            setPadding(16, 16, 16, 16)
            setTextColor(Color.parseColor("#ffffff"))
            isAllCaps = false
//        setSupportAllCaps(false)
        }) {
        private val textView = itemView as TextView

        init {
            textView.setOnClickListener(onClickListener)
        }

        @SuppressLint("MissingPermission", "SetTextI18n")
        fun setData(device: BluetoothDevice) {
            textView.text =
                "name: ${device.name} address: ${device.address}"
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    @SuppressLint("MissingPermission")
    private fun scan() {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)


        val bluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        bluetoothAdapter?.bluetoothLeScanner?.startScan(
            null,
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build(),
            object :
                android.bluetooth.le.ScanCallback() {
                override fun onScanResult(
                    callbackType: Int,
                    result: android.bluetooth.le.ScanResult?
                ) {
                    super.onScanResult(callbackType, result)
                    if (result?.device?.name == null) {
                        return
                    }
                    Log.d(
                        "BluetoothScanActivity",
                        "onScanResult: ${result?.device?.name} ${result?.device?.address} ${result?.rssi}"
                    )
                }
            })

        val startDiscovery = bluetoothAdapter?.startDiscovery()
        Log.e("BluetoothScanActivity", "Scanning for devices $startDiscovery")

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            val deviceUUID = device.uuids
            Log.e("BluetoothScanActivity", "pairedDevices deviceName: $deviceName")
            for (uuid in deviceUUID) {
                Log.e("BluetoothScanActivity", "pairedDevices UUID: $uuid")
            }

            Log.e(
                "BluetoothScanActivity",
                "Scanning for connected devices $deviceName $deviceHardwareAddress ${deviceUUID.size}"
            )
        }
    }


    private fun pair() {

    }
}