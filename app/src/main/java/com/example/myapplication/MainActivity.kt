package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.STATE_CONNECTED
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val  TAG = "BluetoothDevice"
    val executor = Executors.newSingleThreadExecutor()
    lateinit var launcher: ActivityResultLauncher<Intent>
    var currentSelectDevice: BluetoothDevice? = null
    val deviceTv: TextView by lazy {
        findViewById(R.id.device_tv)
    }
    val bondTv: TextView by lazy {
        findViewById(R.id.bond_tv)
    }

    val connectTv: TextView by lazy {
        findViewById(R.id.connected_tv)
    }
    val bluetoothAdapter by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    val handlerThread = HandlerThread("connect").apply {
        start()
    }
    val handler = object : Handler(handlerThread.looper) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "BYD Bluetooth connect"

        launcher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
                ActivityResultCallback {
                    Log.e(
                        "MainActivity",
                        "onActivityResult: ${it.resultCode} ${
                            it.data?.extras?.get("dataDevice").toString()
                        }"
                    )
                    val result: BluetoothDevice? =
                        it.data?.extras?.get("dataDevice") as? BluetoothDevice
                    onDeviceSelected(result)

                })
    }

    private fun loop() {
        Log.e(TAG,"loop start")
        currentSelectDevice?.run {
            ConnectThread(
                baseContext,
                bluetoothAdapter!!,
                address
            ) {
                handler.postDelayed({
                    loop()
                }, 2000)
            }.run()
        }


    }

    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun onDeviceSelected(result: BluetoothDevice?) {
        currentSelectDevice = result
        result?.run {
            deviceTv.setText("${name} ${address}")
            val bondState = bluetoothAdapter.getRemoteDevice(address).bondState
            bondTv.text = "Bond status:" + when (bondState) {
                BluetoothDevice.BOND_NONE -> {
                    "Bond None"
                }

                BluetoothDevice.BOND_BONDING -> {
                    "Bonding"
                }

                BluetoothDevice.BOND_BONDED -> {
                    " Bonded"
                }

                else -> {
                    "Unknown"
                }
            }
        } ?: kotlin.run {
            deviceTv.text = "No device selected"
        }
    }

    fun scanBluetoothDevices(view: View) {
        val permissions: ArrayList<String> = arrayListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (!checkAllPermissionGranted(permissions)) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                1
            )
            Toast.makeText(
                this,
                "Please grant permission to scan bluetooth devices",
                Toast.LENGTH_SHORT
            ).show()

        } else {
            launcher.launch(
                Intent(
                    this,
                    com.example.myapplication.bluetooth.BluetoothScanActivity::class.java
                )
            )
        }
    }

    private fun checkAllPermissionGranted(permission: List<String>): Boolean {
        permission.forEach {
            if (ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    fun connectDevice(view: View) {
        if (currentSelectDevice == null) {
            Toast.makeText(this, "Please select a device to connect", Toast.LENGTH_SHORT).show()
            return
        }

        ConnectThread(view.context, bluetoothAdapter!!, currentSelectDevice!!.address) {
            handler.postDelayed({
                loop()
            }, 2000)
        }.run()
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(
        val context: Context,
        val bluetoothAdapter: BluetoothAdapter,
        val address: String,
        val endCallback: () -> Unit
    ) {
        private val device: BluetoothDevice by lazy {
            bluetoothAdapter.getRemoteDevice(address)
        }
        private val socket: BluetoothGatt? by lazy(LazyThreadSafetyMode.NONE) {
            device.connectGatt(context, false, object : BluetoothGattCallback() {
                //连接状态变更
                override fun onConnectionStateChange(
                    gatt: BluetoothGatt?,
                    status: Int,
                    newState: Int
                ) {
                    Log.e("Connect", "onConnectionStateChange: $status $newState")
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        runOnUiThread {
                            bondTv.text = "Connected status: Connected"
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        runOnUiThread {
                            bondTv.text = "Connected status: DisConnected"
                        }
                    }
                }

                //发现服务回调
                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    // 调用 mBleGatt?.discoverServices() 时触发该回调
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        //失败
                        return
                    }
                    //获取指定GATT服务，UUID 由远程设备提供
                    val bleGattService = socket?.getService(UUID.fromString("8888888"))
                    //获取指定GATT特征，UUID 由远程设备提供
                    val bleGattCharacteristic =
                        bleGattService?.getCharacteristic(UUID.fromString("777777"))
                    //启用特征通知，如果远程设备修改了特征，则会触发 onCharacteristicChange() 回调
                    socket?.setCharacteristicNotification(bleGattCharacteristic, true)
                    //启用客户端特征配置【固定写法】
                    val bleGattDescriptor =
                        bleGattCharacteristic?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    bleGattDescriptor?.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    socket?.writeDescriptor(bleGattDescriptor)
                }

                //启用客户端特征配置结果回调
                override fun onDescriptorWrite(
                    gatt: BluetoothGatt?,
                    descriptor: BluetoothGattDescriptor?,
                    status: Int
                ) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        //此时蓝牙设备连接才算真正连接成功，即具备读写数据的能力
                    }
                }

                //App修改特征回调，即 App 给设备发送数据结果回调
                override fun onCharacteristicWrite(
                    gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int
                ) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        //数据写入完成
                        // 调用 characteristic?.value 得到的 ByteArray 与 发送数据一样
                    }
                }

                //远程设备修改特征描述回调，即设备给 App 发送数据
                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?
                ) {
                    //调用 characteristic?.value 获取远程设备发送过来的数据
                }
            })
        }

        fun run() {
            val hasConnected = isDeviceConnected();
            Log.e(TAG, "HasConnected : $hasConnected")

            if (hasConnected) {
                endCallback.invoke()
                return
            }

            socket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                while (true) {
                    try {
                        socket.connect()
                        Log.e(TAG, "connect")
                        break
                    } catch (e: Exception) {
                        Log.e(TAG, Log.getStackTraceString(e))
                        Thread.sleep(1000)
                    }
                }
                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
            }
            endCallback.invoke()
        }

        fun isDeviceConnected(): Boolean {

            var isConnected = false
            val countDownLatch = CountDownLatch(1)
            executor.run {
                bluetoothAdapter.getProfileProxy(
                    context,
                    object : BluetoothProfile.ServiceListener {
                        /**
                         * Called to notify the client when the proxy object has been
                         * connected to the service.
                         *
                         * @param profile - One of [.HEADSET] or [.A2DP]
                         * @param proxy - One of [BluetoothHeadset] or [BluetoothA2dp]
                         */
                        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                            proxy?.getDevicesMatchingConnectionStates(intArrayOf(STATE_CONNECTED))
                                ?.forEach { blueDevice ->
                                    Log.e(
                                        TAG,
                                        "onServiceConnected "+blueDevice.name + "," + blueDevice.address
                                    )
                                    if (blueDevice.address.equals(device.address)) {
                                        isConnected = true
                                    }
                                }
                        }

                        /**
                         * Called to notify the client that this proxy object has been
                         * disconnected from the service.
                         *
                         * @param profile - One of [.HEADSET] or [.A2DP]
                         */
                        override fun onServiceDisconnected(profile: Int) {
                            Log.e(
                                TAG,
                                "onServiceDisconnected "
                            )
                        }

                    },
                    BluetoothProfile.GATT
                )
            }
            val await = countDownLatch.await(5L, TimeUnit.SECONDS)
            Log.e(TAG,"timeOut : $await" )
            return isConnected
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                socket?.close()
            } catch (e: IOException) {
                Log.e("Connect", "Could not close the client socket", e)
            }
        }
    }
}

