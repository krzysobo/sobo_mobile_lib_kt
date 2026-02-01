package com.krzysobo.sobomobilelib.service

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class BluetoothManager(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var bluetoothA2dp: BluetoothProfile? = null
    private var isScoConnected = false
    private var tts: TextToSpeech? = null

    private val requiredPermissions = listOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // BroadcastReceiver for SCO state
    private val scoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val state = intent?.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1) ?: -1
            isScoConnected = state == AudioManager.SCO_AUDIO_STATE_CONNECTED
            Log.d(TAG, "SCO state: $state, Connected: $isScoConnected")
        }
    }

    init {
        val filter = IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        context.registerReceiver(scoReceiver, filter)
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
    }

    fun hasRequiredPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return if (hasRequiredPermissions()) {
            bluetoothAdapter?.isEnabled == true
        } else {
            Log.e(TAG, "Bluetooth permissions not granted")
            false
        }
    }

    fun enableBluetooth(onResult: (Boolean) -> Unit) {
        if (!hasRequiredPermissions()) {
            Log.e(TAG, "Cannot enable Bluetooth: permissions missing")
            onResult(false)
            return
        }
        if (!bluetoothAdapter?.isEnabled!!) {
            onResult(false)
        } else {
            onResult(true)
        }
    }

    fun listBluetoothDevices(
        onDevicesUpdated: (List<BluetoothDevice>) -> Unit,
        onDiscoveryFinished: () -> Unit,
    ) {
        if (!hasRequiredPermissions()) {
            Log.e(TAG, "Cannot list devices: permissions missing (BLUETOOTH_SCAN required)")
            onDevicesUpdated(emptyList())
            onDiscoveryFinished()
            return
        }
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth not enabled")
            onDevicesUpdated(emptyList())
            onDiscoveryFinished()
            return
        }

        // Initialize with paired devices
        val devices = mutableListOf<BluetoothDevice>()
        bluetoothAdapter?.bondedDevices?.let {
            devices.addAll(it)
            Log.d(TAG, "Paired devices: ${it.joinToString { d -> d.name ?: "Unknown" }}")
            onDevicesUpdated(devices.toList())
        }

        // Start discovery for new devices
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter.cancelDiscovery()
            Log.d(TAG, "Cancelled ongoing discovery")
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                        Log.d(TAG, "Discovery started")
                    }

                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            if (!devices.contains(it)) {
                                devices.add(it)
                                Log.d(TAG, "Found device: ${it.name ?: "Unknown"} (${it.address})")
                                onDevicesUpdated(devices.toList())
                            }
                        }
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        Log.d(TAG, "Discovery finished")
                        context?.unregisterReceiver(this)
                        onDiscoveryFinished()
                    }
                }
            }
        }
        context.registerReceiver(receiver, filter)
        try {
            bluetoothAdapter?.startDiscovery()
            Log.d(TAG, "Started Bluetooth discovery")
        } catch (e: SecurityException) {
            Log.e(TAG, "Discovery failed: $e")
            onDevicesUpdated(devices.toList())
            onDiscoveryFinished()
        }
        CoroutineScope(Dispatchers.IO).launch {
            delay(12000)
            if (bluetoothAdapter?.isDiscovering == true) {
                try {
                    bluetoothAdapter.cancelDiscovery()
                    Log.d(TAG, "Discovery timed out")
                } catch (e: SecurityException) {
                    Log.e(TAG, "Cancel discovery failed: $e")
                }
                context.unregisterReceiver(receiver)
                onDiscoveryFinished()
            }
        }
    }

    fun discoverAndPair(device: BluetoothDevice, onPaired: (Boolean) -> Unit) {
        if (!hasRequiredPermissions()) {
            Log.e(TAG, "Cannot pair: permissions missing")
            onPaired(false)
            return
        }
        if (device.bondState == BluetoothDevice.BOND_BONDED) {
            onPaired(true)
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = device.createBond()
                Log.d(TAG, "Pairing ${device.name ?: "Unknown"}: $success")
                onPaired(success)
            } catch (e: SecurityException) {
                Log.e(TAG, "Pairing failed: $e")
                onPaired(false)
            }
        }
    }


    fun connectToSpeaker(device: BluetoothDevice, onConnected: (Boolean) -> Unit) {
        if (!hasRequiredPermissions()) {
            Log.e(TAG, "Cannot connect to speaker: permissions missing")
            onConnected(false)
            return
        }
        bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (profile == BluetoothProfile.A2DP && proxy != null) {
                    try {
                        val connectMethod = proxy.javaClass.getDeclaredMethod(
                            "connect",
                            BluetoothDevice::class.java
                        )
                        connectMethod.isAccessible = true
                        val success = connectMethod.invoke(proxy, device) as Boolean
                        Log.d(TAG, "Connected to ${device.name ?: "Unknown"}: $success")
                        onConnected(success)
                    } catch (e: Exception) {
                        Log.e(TAG, "Connection failed", e)
                        onConnected(false)
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                bluetoothA2dp = null
            }
        }, BluetoothProfile.A2DP)
    }

    private fun disconnectFromSpeaker(device: BluetoothDevice, onConnected: (Boolean) -> Unit = {}) {
        println("VOICE_VOICE BLUETOOTH DISCONNECT -- disconnectFromSpeaker - 0000 DEVICE: ${device.address}")
        if (!hasRequiredPermissions()) {
            Log.e(TAG, "BLUETOOTH -- Cannot disconnect from speaker: permissions missing")
            onConnected(false)
            return
        }
        println("VOICE_VOICE BLUETOOTH DISCONNECT -- disconnectFromSpeaker - 0000-1 ")
        bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                println("VOICE_VOICE BLUETOOTH DISCONNECT -- disconnectFromSpeaker - 1111-0 ")
                if (profile == BluetoothProfile.A2DP && proxy != null) {
                    try {
                        println("VOICE_VOICE BLUETOOTH DISCONNECT -- disconnectFromSpeaker - 1111-1 - trying to disconnect... ")
                        val disconnectMethod = proxy.javaClass.getDeclaredMethod(
                            "disconnect",
                            BluetoothDevice::class.java
                        )
                        disconnectMethod.isAccessible = true
                        val success = disconnectMethod.invoke(proxy, device) as Boolean
                        println("VOICE_VOICE BLUETOOTH DISCONNECT -- disconnectFromSpeaker - 1111-2 - trying to disconnect... ")
                        println("VOICE_VOICE BLUETOOTH DISCONNECT - SUCCESS?? $success")
//                        Log.d(TAG, "Connected to ${device.name ?: "Unknown"}: $success")
                        onConnected(success)
                    } catch (e: Exception) {
                        Log.e(TAG, "VOICE_VOICE BLUETOOTH DISCONNECT -- Disconnection failed", e)
                        onConnected(false)
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                bluetoothA2dp = null
            }
        }, BluetoothProfile.A2DP)
    }

    fun enableScoForTts(onReady: (Boolean) -> Unit) {
        if (!hasRequiredPermissions()) {
            Log.e(TAG, "Cannot enable SCO: permissions missing")
            onReady(false)
            return
        }
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isBluetoothScoOn = true
        audioManager.startBluetoothSco()
        CoroutineScope(Dispatchers.Main).launch {
            delay(1500)
            onReady(isScoConnected)
        }
    }

    fun connectToSpeakerByMac(speakerMac: String, onConnected: (Boolean) -> Unit = {}) {
        println("BLUETOOTH - connectToCurrentSpeaker - 0000-0 - SPEAKER MAC: $speakerMac")
        if (speakerMac != "") {
            val device = bluetoothAdapter?.getRemoteDevice(speakerMac)
            println("BLUETOOTH - connectToCurrentSpeaker - 0000-1 - DEVICE: $device")
            if (device != null) {
                println("BLUETOOTH - connectToCurrentSpeaker - 0000-2 - FOUND THE DEVICE: $device, TRYING TO CONNECT...")
                connectToSpeaker(
                    device, onConnected = onConnected
//                    {
//                    onConnectedIn()
//                    println("BLUETOOTH - connectToCurrentSpeaker - 1111 - THE DEVICE: $device IS CONNECTED OK NOW!!!")
//                }

                )
            }
        }
    }

    // Disconnect A2DP
    fun disconnectSpeakerByMac(speakerMac: String, onConnected: (Boolean) -> Unit = {}) {
        println("VOICE_VOICE BLUETOOTH - disconnectSpeaker - 0000 - BLUETOOTHA2DP EXISTS?? ${bluetoothA2dp != null}")
        println("VOICE_VOICE BLUETOOTH - disconnectSpeaker - 0000-0 - SPEAKER MAC: $speakerMac")
        if (speakerMac != "") {
            val device = bluetoothAdapter?.getRemoteDevice(speakerMac)
            println("VOICE_VOICE BLUETOOTH - disconnectSpeaker - 0000-1 - DEVICE: $device")
            if (device != null) {
                println("BLUETOOTH - disconnectSpeaker - 0000-2 - TRYING TO DISCONNECT THE CONNECTED DEVICE: $device")
                disconnectFromSpeaker(device, onConnected = onConnected)
            }
        } else {
            onConnected(false)
        }
    }


    fun disableSco() {
        audioManager.stopBluetoothSco()  // deprecated, use audioManager.clearCommunicationDevice() instead
//        audioManager.clearCommunicationDevice()
        audioManager.isBluetoothScoOn = false
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    private fun loadPairedSpeaker(speakerMac: String, onDevice: (BluetoothDevice?) -> Unit) {
        if (!hasRequiredPermissions()) {
            Log.e(TAG, "Cannot load paired speaker: permissions missing")
            onDevice(null)
            return
        }
//        val prefs = context.getSharedPreferences("app_config", Context.MODE_PRIVATE)
//        val macAddress = prefs.getString("paired_speaker", null)

        speakerMac.let {
            val device = bluetoothAdapter?.getRemoteDevice(it)
            onDevice(device)
        } ?: onDevice(null)
    }

    fun speakTtsOnBt(text: String, speakerMac: String, lang: String, onError: () -> Unit = {}) {
        if (!hasRequiredPermissions()) {
            Log.e(TAG, "VOICE_VOICE SCO:: Cannot speak TTS: permissions missing")
            onError()
            return
        }
        loadPairedSpeaker(speakerMac) { device ->
            println("VOICE_VOICE SCO - 00001111 MAC: $speakerMac DEVICE: $device")
            device?.let {
                enableScoForTts { ready ->
                    if (ready) {
                        val params = Bundle().apply {
                            putInt(
                                TextToSpeech.Engine.KEY_PARAM_STREAM,
                                AudioManager.STREAM_VOICE_CALL
                            )
                        }
                        println("VOICE_VOICE SCO - 00002222")
                        tts?.setLanguage(Locale.forLanguageTag(lang))
                        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "tts_id")
                        println("VOICE_VOICE SCO - 00003333")
                        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onDone(utteranceId: String?) {
                                disableSco()
                            }

                            override fun onStart(utteranceId: String?) {}
                            override fun onError(utteranceId: String?) {}
                        })
                        println("VOICE_VOICE SCO - 00004444")
                    } else {
                        Log.e(TAG, "VOICE_VOICE SCO -- SCO not ready, falling back to default speaker")
                        tts?.setLanguage(Locale.forLanguageTag(lang))
                        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_id")
                        onError()
                    }
                }
            } ?: run {
                Log.e(TAG, "VOICE_VOICE SCO -- No paired speaker found")
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_id")
                onError()
            }
        }
    }

    fun cleanup() {
        context.unregisterReceiver(scoReceiver)
        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.A2DP, bluetoothA2dp)
        tts?.shutdown()
    }

    companion object {
        private const val TAG = "BluetoothManager"
    }
}