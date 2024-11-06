package com.example.myapplication.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class BluetoothScanActivity : ComponentActivity() {


    private val BLU
    // Create a BroadcastReceiver for ACTION_FOUND.
    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action ?: ""
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    Log.d(
                        "BluetoothScanActivity",
                        "DeviceFound: ${device?.name} ${device?.address} ${device?.alias}"
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bluetooth_scan_activity)
        val rv =findViewById<RecyclerView>(R.id.bluetooth_scan_recycler_view)
        rv.adapter = object : RecyclerView.Adapter<TextViewHolder>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): TextViewHolderr {
                return TextViewHolder(parent.context)
            }

            override fun onBindViewHolder(holder: TextViewHolder, position: Int) {
            }

            override fun getItemCount(): Int {
                return 0
            }
        }
        scan()
        pair()
    }

    class TextViewHolder(context: Context) : RecyclerView.ViewHolder(TextView(context)){
        private val textView = itemView as TextView

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