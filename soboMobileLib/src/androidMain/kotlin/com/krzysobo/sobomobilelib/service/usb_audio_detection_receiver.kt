package com.krzysobo.sobomobilelib.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.media.*
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.lang.reflect.Method


/**
 * // Inside your Activity or Fragment
 * val usbReceiver = object : BroadcastReceiver() {
 *     override fun onReceive(context: Context, intent: Intent) {
 *         when (intent.action) {
 *             UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
 *                 val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
 *                 if (device != null) {
 *                     // Handle the device attachment
 *                     Log.d(TAG, "USB Device Attached: ${device.deviceName}")
 *                 }
 *             }
 *         }
 *     }
 * }
 *
 * // In your onCreate() or onResume()
 * val filter = IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
 * registerReceiver(usbReceiver, filter)
 */
class UsbAudioDetectionReceiver() : BroadcastReceiver() {  // Extend in your MainService
    private lateinit var audioManager: AudioManager

    //    private lateinit var context: Context
    private var usbDevice: AudioDeviceInfo? = null
    private var originalRoutingEnabled = true  // Track state

    val AUDIO_SYSTEM_DEVICE_OUT_USB_DEVICE: Int = 0x4000
    val AUDIO_SYSTEM_DEVICE_OUT_USB_ACCESSORY: Int = 0x2000
    val AUDIO_SYSTEM_DEVICE_OUT_USB_HEADSET: Int = 0x4000000


    @RequiresApi(Build.VERSION_CODES.S)
    override fun onReceive(context: Context, intent: Intent) {
//        this.context = context
        Log.d("USB_DEVICE", "onReceive - 0000 ")
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                findUsbDevice(context)
                if (usbDevice != null) {
                    Log.d("USB_DEVICE", "onReceive::  USB DEVICE $usbDevice")
                    // Briefly disable/enable routing to make visible (if needed)
                    toggleUsbRoutingInternally()
                } else {
                    Log.d("USB_DEVICE", "onReceive::  USB DEVICE IS NULL!!!")
                }
            }

            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                clearUsbPreference()
                usbDevice = null
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun findUsbDevice(context: Context): AudioDeviceInfo? {
        Log.d("USB_DEVICE", "findUsbDevice - 0000")
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//        val devices = audioManager.getAvailableCommunicationDevices()  // Or getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val devices =
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)  // Or getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        Log.d("USB_DEVICE", "findUsbDevice - 11110000 - DEVICES: $devices")
        for (device in devices) {
            Log.d(
                "USB_DEVICE",
                "findUsbDevice - 11111111 -- device: ${device.id} NAME: ${device.productName} TYPE: ${device.type}"
            )
        }
        usbDevice =
            devices.find { it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET }

        Log.d("USB_DEVICE", "findUsbDevice - 22220000 - USB DEVICE: $usbDevice")
        return usbDevice
    }

    // Reflection to toggle internal USB routing (mimics dev option; safe on Android 11+)
    private fun toggleUsbRoutingInternally() {
        Log.d("USB_DEVICE", "toggleUsbRoutingInternally - 0000")
        try {
            val setWiredDeviceMethod: Method = AudioManager::class.java.getDeclaredMethod(
                "setWiredDeviceConnectionState",
                Int::class.javaPrimitiveType,  // device type (e.g., AudioSystem.DEVICE_OUT_USB_HEADSET)
                Int::class.javaPrimitiveType,  // state (1=connected, 0=disconnected)
                String::class.java  // address, e.g., ""
            )
            Log.d("USB_DEVICE", "toggleUsbRoutingInternally - 1111")
            setWiredDeviceMethod.isAccessible = true

            Log.d("USB_DEVICE", "toggleUsbRoutingInternally - 2222")
            // Briefly "disconnect" then "reconnect" USB to force visibility
            // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/media/java/android/media/AudioSystem.java

//            val usbType = AudioSystem.DEVICE_OUT_USB_HEADSET  // Adjust if your DAC reports differently
            val usbType =
                AUDIO_SYSTEM_DEVICE_OUT_USB_HEADSET  // Adjust if your DAC reports differently
            Log.d("USB_DEVICE", "toggleUsbRoutingInternally - 3333")
            /*
    public static final int DEVICE_OUT_USB_DEVICE = 0x4000;
    public static final int DEVICE_OUT_USB_ACCESSORY = 0x2000;
        public static final int DEVICE_OUT_USB_HEADSET = 0x4000000;
             */
            setWiredDeviceMethod.invoke(audioManager, usbType, 0, "")
            Thread.sleep(100)  // Short delay
            setWiredDeviceMethod.invoke(audioManager, usbType, 1, "")
            originalRoutingEnabled = false  // Assume disabled now for app control
        } catch (e: Exception) {
            // Fallback: Prompt user to toggle dev option, or use getDevices(GET_DEVICES_OUTPUTS)
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun clearUsbPreference() {
        // Reset to default (HDMI)
        audioManager.clearCommunicationDevice()
        // Restore original via reflection if needed
    }

//    init {
//        // Register receiver
//        val filter = IntentFilter().apply {
//            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
//            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
//        }
//        context.registerReceiver(this, filter)
//    }


}