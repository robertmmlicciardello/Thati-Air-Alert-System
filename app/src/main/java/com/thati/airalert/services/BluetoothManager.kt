package com.thati.airalert.services

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.*
import java.util.*

/**
 * Bluetooth Low Energy Manager
 * BLE ကို အသုံးပြုပြီး alert messages များ ပို့ဆောင်ရန်
 */
class BluetoothManager(
    private val context: Context,
    private val onMessageReceived: (String) -> Unit
) {
    
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var gattServer: BluetoothGattServer? = null
    private var isAdvertising = false
    private var isScanning = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "BluetoothManager"
        
        // Custom service UUID for Thati Alert
        private val SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc")
        private val CHARACTERISTIC_UUID = UUID.fromString("87654321-4321-4321-4321-cba987654321")
        
        // Advertisement data
        private const val DEVICE_NAME = "ThatiAlert"
    }
    
    /**
     * Bluetooth ကို initialize လုပ်ခြင်း
     */
    fun initialize(): Boolean {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter
            
            if (bluetoothAdapter == null) {
                Log.e(TAG, "Bluetooth not supported")
                return false
            }
            
            if (!bluetoothAdapter!!.isEnabled) {
                Log.w(TAG, "Bluetooth is not enabled")
                return false
            }
            
            bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
            bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
            
            if (bluetoothLeAdvertiser == null || bluetoothLeScanner == null) {
                Log.e(TAG, "BLE advertising/scanning not supported")
                return false
            }
            
            Log.d(TAG, "Bluetooth initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Bluetooth", e)
            false
        }
    }
    
    /**
     * BLE advertising ကို စတင်ခြင်း (Admin mode အတွက်)
     */
    fun startAdvertising() {
        if (isAdvertising) return
        
        try {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build()
            
            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(ParcelUuid(SERVICE_UUID))
                .build()
            
            bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
            
            // GATT Server ကို စတင်ပါ
            startGattServer()
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for advertising", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting advertising", e)
        }
    }
    
    /**
     * BLE advertising ကို ရပ်ခြင်း
     */
    fun stopAdvertising() {
        if (!isAdvertising) return
        
        try {
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            gattServer?.close()
            gattServer = null
            isAdvertising = false
            Log.d(TAG, "Advertising stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for stopping advertising", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping advertising", e)
        }
    }
    
    /**
     * BLE scanning ကို စတင်ခြင်း (User mode အတွက်)
     */
    fun startScanning() {
        if (isScanning) return
        
        try {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            
            val filters = listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(SERVICE_UUID))
                    .build()
            )
            
            bluetoothLeScanner?.startScan(filters, settings, scanCallback)
            isScanning = true
            Log.d(TAG, "Scanning started")
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for scanning", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting scan", e)
        }
    }
    
    /**
     * BLE scanning ကို ရပ်ခြင်း
     */
    fun stopScanning() {
        if (!isScanning) return
        
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
            Log.d(TAG, "Scanning stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for stopping scan", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan", e)
        }
    }
    
    /**
     * GATT Server ကို စတင်ခြင်း
     */
    private fun startGattServer() {
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
            
            gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
            
            // Service နှင့် Characteristic ကို ဖန်တီးပါ
            val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            
            val characteristic = BluetoothGattCharacteristic(
                CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            
            service.addCharacteristic(characteristic)
            gattServer?.addService(service)
            
            Log.d(TAG, "GATT Server started")
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for GATT server", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting GATT server", e)
        }
    }
    
    /**
     * Message ပို့ခြင်း (BLE မှတစ်ဆင့်)
     */
    fun sendMessage(message: String, deviceAddress: String) {
        scope.launch {
            try {
                val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
                device?.let { 
                    connectAndSendMessage(it, message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
            }
        }
    }
    
    /**
     * Device နှင့် connect လုပ်ပြီး message ပို့ခြင်း
     */
    private fun connectAndSendMessage(device: BluetoothDevice, message: String) {
        try {
            val gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(TAG, "Connected to ${device.address}")
                        gatt?.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, "Disconnected from ${device.address}")
                        gatt?.close()
                    }
                }
                
                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        val service = gatt?.getService(SERVICE_UUID)
                        val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
                        
                        characteristic?.let { char ->
                            char.value = message.toByteArray()
                            gatt.writeCharacteristic(char)
                        }
                    }
                }
                
                override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d(TAG, "Message sent successfully")
                    } else {
                        Log.e(TAG, "Failed to send message: $status")
                    }
                    gatt?.disconnect()
                }
            })
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for connecting", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device", e)
        }
    }
    
    /**
     * Advertise callback
     */
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
            Log.d(TAG, "Advertising started successfully")
        }
        
        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "Advertising failed: $errorCode")
        }
    }
    
    /**
     * Scan callback
     */
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { scanResult ->
                Log.d(TAG, "Found device: ${scanResult.device.address}")
                // Device တွေ့ရင် connect လုပ်ကြည့်ပါ
                connectToDevice(scanResult.device)
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed: $errorCode")
        }
    }
    
    /**
     * GATT Server callback
     */
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Device connected: ${device?.address}")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Device disconnected: ${device?.address}")
            }
        }
        
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            value?.let { data ->
                val message = String(data)
                Log.d(TAG, "Received message: $message")
                onMessageReceived(message)
            }
            
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }
        }
    }
    
    /**
     * Device နှင့် connect လုပ်ခြင်း
     */
    private fun connectToDevice(device: BluetoothDevice) {
        // Implementation for connecting to discovered device
        Log.d(TAG, "Attempting to connect to ${device.address}")
    }
    
    /**
     * Resources များကို သန့်ရှင်းခြင်း
     */
    fun cleanup() {
        scope.cancel()
        stopAdvertising()
        stopScanning()
    }
}