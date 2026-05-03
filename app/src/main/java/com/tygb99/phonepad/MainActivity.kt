package com.tygb99.phonepad

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale
import java.util.concurrent.Executor
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var statusText: TextView
    private lateinit var deviceText: TextView
    private lateinit var dragButton: Button
    private lateinit var registerButton: Button
    private lateinit var unregisterButton: Button
    private lateinit var trackpadSurface: FrameLayout

    private var hidDevice: BluetoothHidDevice? = null
    private var activeHost: BluetoothDevice? = null
    private var appRegistered = false
    private var pendingRegister = false
    private var dragMode = false
    private var lastX = 0f
    private var lastY = 0f
    private var gestureDistance = 0f
    private var usingScrollGesture = false

    private val mainExecutor = Executor { command -> runOnUiThread(command) }

    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager.adapter

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDevice = proxy as BluetoothHidDevice
            setStatus("HID 프로필을 열었습니다. 이제 HID 등록을 시도할 수 있습니다.")
            if (pendingRegister) {
                pendingRegister = false
                registerHidApp()
            }
            refreshControls()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            releaseAllMouseButtons("hid_profile_disconnected")
            hidDevice = null
            activeHost = null
            appRegistered = false
            dragMode = false
            setStatus("HID 프로필 연결이 끊어졌습니다.")
            refreshControls()
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            appRegistered = registered
            if (registered) {
                activeHost = pluggedDevice ?: activeHost ?: firstConnectedHost()
                setStatus("HID 앱이 등록됐습니다. PC/macOS Bluetooth 설정에서 PhonePad를 페어링하세요.")
            } else {
                activeHost = null
                dragMode = false
                setStatus("HID 앱 등록이 해제됐습니다.")
            }
            refreshControls()
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    activeHost = device
                    setStatus("호스트와 연결됐습니다: ${device.safeLabel()}")
                }
                BluetoothProfile.STATE_CONNECTING -> setStatus("호스트 연결 중: ${device.safeLabel()}")
                BluetoothProfile.STATE_DISCONNECTING -> {
                    releaseAllMouseButtons("host_disconnecting")
                    setStatus("호스트 연결 해제 중: ${device.safeLabel()}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    releaseAllMouseButtons("host_disconnected")
                    if (activeHost?.address == device?.address) activeHost = null
                    dragMode = false
                    setStatus("호스트 연결이 끊어졌습니다.")
                }
            }
            refreshControls()
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice?) {
            releaseAllMouseButtons("virtual_cable_unplug")
            if (activeHost?.address == device?.address) activeHost = null
            dragMode = false
            setStatus("가상 케이블 연결이 해제됐습니다.")
            refreshControls()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        setContentView(buildContentView())
        updateDeviceText()
        refreshCompatibility()
        refreshControls()
    }

    override fun onStop() {
        releaseAllMouseButtons("activity_stopped")
        dragMode = false
        refreshControls()
        super.onStop()
    }

    override fun onDestroy() {
        releaseAllMouseButtons("activity_destroyed")
        unregisterHidApp()
        hidDevice?.let { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it) }
        hidDevice = null
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != NEARBY_DEVICES_REQUEST) return
        val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (granted) {
            setStatus("Nearby devices 권한이 허용됐습니다.")
            if (pendingRegister) openHidProfile()
        } else {
            pendingRegister = false
            setStatus("Nearby devices 권한이 필요합니다. 권한 없이는 HID 등록을 진행할 수 없습니다.")
        }
        refreshControls()
    }

    private fun buildContentView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_BACKGROUND)
        }
        root.setOnApplyWindowInsetsListener { view, insets ->
            @Suppress("DEPRECATION")
            view.setPadding(
                dp(18),
                insets.systemWindowInsetTop + dp(12),
                dp(18),
                insets.systemWindowInsetBottom + dp(12),
            )
            insets
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }

        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(title, matchWrap())

        deviceText = TextView(this).apply {
            textSize = 14f
            setTextColor(COLOR_MUTED)
            setPadding(0, dp(4), 0, dp(10))
        }
        root.addView(deviceText, matchWrap())

        statusText = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.WHITE)
            setLineSpacing(2f, 1.05f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(COLOR_PANEL, dp(8), COLOR_STROKE)
        }
        root.addView(statusText, matchWrap(bottom = 14))

        val setupRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        setupRow.addView(actionButton("권한", ::requestNearbyDevicePermissions), rowButtonParams())
        setupRow.addView(actionButton("Bluetooth 설정", ::openBluetoothSettings), rowButtonParams())
        root.addView(setupRow, matchWrap(bottom = 8))

        val hidRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        registerButton = actionButton("HID 등록", ::prepareHidRegistration)
        unregisterButton = actionButton("해제", ::unregisterHidApp)
        hidRow.addView(registerButton, rowButtonParams())
        hidRow.addView(unregisterButton, rowButtonParams())
        root.addView(hidRow, matchWrap(bottom = 14))

        trackpadSurface = FrameLayout(this).apply {
            background = rounded(COLOR_TRACKPAD, dp(8), COLOR_TRACKPAD_STROKE)
            setOnTouchListener(::handleTrackpadTouch)
            addView(
                TextView(context).apply {
                    text = "터치패드"
                    textSize = 18f
                    setTextColor(COLOR_TRACKPAD_LABEL)
                    gravity = Gravity.CENTER
                },
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
        }
        root.addView(trackpadSurface, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f,
        ).apply { bottomMargin = dp(14) })

        val clickRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        clickRow.addView(actionButton("왼쪽 클릭") { clickMouse(LEFT_BUTTON) }, rowButtonParams())
        clickRow.addView(actionButton("오른쪽 클릭") { clickMouse(RIGHT_BUTTON) }, rowButtonParams())
        root.addView(clickRow, matchWrap(bottom = 8))

        val motionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        motionRow.addView(actionButton("스크롤 ↑") { sendMouseReport(0, 0, currentButtons(), 4, 0) }, rowButtonParams())
        motionRow.addView(actionButton("스크롤 ↓") { sendMouseReport(0, 0, currentButtons(), -4, 0) }, rowButtonParams())
        motionRow.addView(actionButton("테스트 이동") { sendMouseReport(30, 0, currentButtons(), 0, 0) }, rowButtonParams())
        root.addView(motionRow, matchWrap(bottom = 8))

        dragButton = actionButton("Drag", ::toggleDragMode).apply {
            minHeight = dp(52)
        }
        root.addView(dragButton, matchWrap())

        return scroll
    }

    private fun updateDeviceText() {
        deviceText.text = String.format(
            Locale.US,
            "%s %s · Android %s(API %d) · targetSdk 36",
            Build.MANUFACTURER.replaceFirstChar { it.titlecase(Locale.US) },
            Build.MODEL,
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT,
        )
    }

    private fun refreshCompatibility() {
        val adapter = bluetoothAdapter
        when {
            adapter == null -> setStatus("Bluetooth 어댑터를 찾을 수 없습니다.")
            !adapter.isEnabled -> setStatus("Bluetooth가 꺼져 있습니다. Bluetooth를 켠 뒤 HID 등록을 진행하세요.")
            hasNearbyDevicePermissions() -> setStatus("준비됐습니다. HID 등록을 누른 뒤 PC/macOS에서 Bluetooth 기기 추가를 진행하세요.")
            else -> setStatus("Nearby devices 권한을 허용하면 HID 등록을 진행할 수 있습니다.")
        }
    }

    private fun refreshControls() {
        runOnUiThread {
            val canUseHid = hasNearbyDevicePermissions() && bluetoothAdapter?.isEnabled == true
            registerButton.isEnabled = canUseHid && !appRegistered
            unregisterButton.isEnabled = hidDevice != null || appRegistered
            trackpadSurface.isEnabled = appRegistered
            dragButton.text = if (dragMode) "Dragging" else "Drag"
            dragButton.background = rounded(
                if (dragMode) COLOR_DRAG_ACTIVE else COLOR_BUTTON,
                dp(8),
                if (dragMode) COLOR_DRAG_STROKE else COLOR_BUTTON_STROKE,
            )
        }
    }

    private fun prepareHidRegistration() {
        pendingRegister = true
        if (!hasNearbyDevicePermissions()) {
            requestNearbyDevicePermissions()
            return
        }
        if (bluetoothAdapter?.isEnabled != true) {
            pendingRegister = false
            setStatus("Bluetooth가 꺼져 있습니다. Bluetooth 설정에서 켜주세요.")
            openBluetoothSettings()
            return
        }
        if (hidDevice == null) {
            openHidProfile()
            return
        }
        pendingRegister = false
        registerHidApp()
    }

    @SuppressLint("MissingPermission")
    private fun openHidProfile() {
        if (!hasNearbyDevicePermissions()) return
        val opened = bluetoothAdapter?.getProfileProxy(this, serviceListener, BluetoothProfile.HID_DEVICE) == true
        setStatus(
            if (opened) "Android HID 프로필을 여는 중입니다."
            else "이 기기에서는 Android HID Device를 사용할 수 없습니다.",
        )
        refreshControls()
    }

    @SuppressLint("MissingPermission")
    private fun registerHidApp() {
        val device = hidDevice
        if (device == null) {
            pendingRegister = true
            openHidProfile()
            return
        }
        if (!hasNearbyDevicePermissions()) {
            pendingRegister = true
            requestNearbyDevicePermissions()
            return
        }
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "PhonePad",
            "Bluetooth HID mouse for PhonePad",
            "PhonePad",
            BluetoothHidDevice.SUBCLASS1_MOUSE,
            MOUSE_DESCRIPTOR,
        )
        val accepted = device.registerApp(sdp, null, null, mainExecutor, hidCallback)
        setStatus(
            if (accepted) "HID 등록 요청을 보냈습니다. 결과를 기다리는 중입니다."
            else "HID 등록 요청이 거절됐습니다. 기기 또는 제조사 정책상 HID Device가 막혀 있을 수 있습니다.",
        )
        refreshControls()
    }

    @SuppressLint("MissingPermission")
    private fun unregisterHidApp() {
        releaseAllMouseButtons("unregister")
        dragMode = false
        hidDevice?.unregisterApp()
        appRegistered = false
        activeHost = null
        setStatus("HID 앱을 해제했습니다.")
        refreshControls()
    }

    private fun requestNearbyDevicePermissions() {
        if (hasNearbyDevicePermissions()) {
            setStatus("필요한 Bluetooth 권한이 이미 허용돼 있습니다.")
            refreshControls()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                ),
                NEARBY_DEVICES_REQUEST,
            )
        }
    }

    private fun hasNearbyDevicePermissions(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
    }

    private fun openBluetoothSettings() {
        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    private fun handleTrackpadTouch(view: View, event: MotionEvent): Boolean {
        if (!appRegistered) {
            setStatus("먼저 HID 등록과 호스트 페어링을 완료해주세요.")
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                gestureDistance = 0f
                usingScrollGesture = false
                view.parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                usingScrollGesture = event.pointerCount >= 2
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                lastX = event.x
                lastY = event.y
                gestureDistance += abs(dx) + abs(dy)
                if (event.pointerCount >= 2 || usingScrollGesture) {
                    val wheel = (-dy / 12f).roundToInt().coerceIn(-12, 12)
                    if (wheel != 0) sendMouseReport(0, 0, currentButtons(), wheel, 0)
                } else {
                    val moveX = (dx * POINTER_SCALE).roundToInt().coerceIn(-127, 127)
                    val moveY = (dy * POINTER_SCALE).roundToInt().coerceIn(-127, 127)
                    if (moveX != 0 || moveY != 0) {
                        sendMouseReport(moveX, moveY, currentButtons(), 0, 0)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                view.parent?.requestDisallowInterceptTouchEvent(false)
                if (!dragMode && !usingScrollGesture && gestureDistance < dp(8)) {
                    clickMouse(LEFT_BUTTON)
                } else if (dragMode) {
                    sendMouseReport(0, 0, LEFT_BUTTON, 0, 0)
                }
                usingScrollGesture = false
            }
            MotionEvent.ACTION_CANCEL -> {
                view.parent?.requestDisallowInterceptTouchEvent(false)
                if (!dragMode) releaseAllMouseButtons("touch_cancel")
            }
        }
        return true
    }

    private fun toggleDragMode() {
        if (!appRegistered) {
            setStatus("Drag Mode 전에 HID 등록과 호스트 페어링이 필요합니다.")
            return
        }
        if (dragMode) {
            val ok = releaseAllMouseButtons("drag_off")
            dragMode = false
            setStatus(if (ok) "Drag Mode를 껐습니다." else "버튼 해제 보고서를 보낼 호스트가 없습니다.")
        } else {
            val ok = sendMouseReport(0, 0, LEFT_BUTTON, 0, 0)
            dragMode = ok
            setStatus(if (ok) "Drag Mode를 켰습니다." else "Drag Mode 시작에 실패했습니다. 호스트 연결을 확인하세요.")
        }
        refreshControls()
    }

    private fun clickMouse(button: Int) {
        if (dragMode) {
            setStatus("Drag Mode 중에는 클릭 대신 왼쪽 버튼을 유지합니다.")
            return
        }
        val down = sendMouseReport(0, 0, button, 0, 0)
        val up = releaseAllMouseButtons("click")
        setStatus(
            if (down && up) "클릭 보고서를 보냈습니다."
            else "클릭 보고서 전송에 실패했습니다. 호스트 연결을 확인하세요.",
        )
    }

    private fun currentButtons(): Int = if (dragMode) LEFT_BUTTON else 0

    @SuppressLint("MissingPermission")
    private fun sendMouseReport(dx: Int, dy: Int, buttons: Int, wheel: Int, horizontalWheel: Int): Boolean {
        if (!hasNearbyDevicePermissions()) return false
        val device = activeHost ?: firstConnectedHost()
        val hid = hidDevice
        if (hid == null || device == null || !appRegistered) return false
        val payload = byteArrayOf(
            buttons.coerceIn(0, 7).toByte(),
            dx.coerceIn(-127, 127).toByte(),
            dy.coerceIn(-127, 127).toByte(),
            wheel.coerceIn(-127, 127).toByte(),
            horizontalWheel.coerceIn(-127, 127).toByte(),
        )
        return hid.sendReport(device, MOUSE_REPORT_ID, payload)
    }

    @SuppressLint("MissingPermission")
    private fun releaseAllMouseButtons(reason: String): Boolean {
        val device = activeHost ?: firstConnectedHost()
        val hid = hidDevice ?: return false
        if (!hasNearbyDevicePermissions() || device == null || !appRegistered) return false
        val ok = hid.sendReport(device, MOUSE_REPORT_ID, byteArrayOf(0, 0, 0, 0, 0))
        if (!ok && reason != "activity_destroyed") {
            setStatus("버튼 해제 보고서 전송에 실패했습니다: $reason")
        }
        return ok
    }

    @SuppressLint("MissingPermission")
    private fun firstConnectedHost(): BluetoothDevice? {
        if (!hasNearbyDevicePermissions()) return null
        return hidDevice?.connectedDevices?.firstOrNull()?.also { activeHost = it }
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice?.safeLabel(): String {
        if (this == null) return "알 수 없음"
        return if (hasNearbyDevicePermissions()) {
            name?.takeIf { it.isNotBlank() } ?: address
        } else {
            "권한 필요"
        }
    }

    private fun setStatus(message: String) {
        runOnUiThread {
            statusText.text = message
        }
    }

    private fun actionButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 14f
            isAllCaps = false
            minHeight = dp(46)
            setTextColor(Color.WHITE)
            background = rounded(COLOR_BUTTON, dp(8), COLOR_BUTTON_STROKE)
            setOnClickListener { onClick() }
        }
    }

    private fun rounded(color: Int, radius: Int, strokeColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = radius.toFloat()
            setStroke(dp(1), strokeColor)
        }
    }

    private fun matchWrap(bottom: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(bottom) }
    }

    private fun rowButtonParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            leftMargin = dp(4)
            rightMargin = dp(4)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val NEARBY_DEVICES_REQUEST = 4028
        private const val MOUSE_REPORT_ID = 1
        private const val LEFT_BUTTON = 1
        private const val RIGHT_BUTTON = 2
        private const val POINTER_SCALE = 1.3f

        private val COLOR_BACKGROUND = Color.rgb(12, 14, 18)
        private val COLOR_PANEL = Color.rgb(30, 34, 42)
        private val COLOR_STROKE = Color.rgb(63, 71, 86)
        private val COLOR_TRACKPAD = Color.rgb(20, 24, 31)
        private val COLOR_TRACKPAD_STROKE = Color.rgb(82, 93, 112)
        private val COLOR_TRACKPAD_LABEL = Color.rgb(132, 143, 160)
        private val COLOR_MUTED = Color.rgb(164, 174, 190)
        private val COLOR_BUTTON = Color.rgb(43, 50, 63)
        private val COLOR_BUTTON_STROKE = Color.rgb(93, 105, 126)
        private val COLOR_DRAG_ACTIVE = Color.rgb(164, 53, 65)
        private val COLOR_DRAG_STROKE = Color.rgb(236, 118, 130)

        private val MOUSE_DESCRIPTOR = intArrayOf(
            0x05, 0x01,
            0x09, 0x02,
            0xA1, 0x01,
            0x85, MOUSE_REPORT_ID,
            0x09, 0x01,
            0xA1, 0x00,
            0x05, 0x09,
            0x19, 0x01,
            0x29, 0x03,
            0x15, 0x00,
            0x25, 0x01,
            0x95, 0x03,
            0x75, 0x01,
            0x81, 0x02,
            0x95, 0x01,
            0x75, 0x05,
            0x81, 0x03,
            0x05, 0x01,
            0x09, 0x30,
            0x09, 0x31,
            0x09, 0x38,
            0x15, 0x81,
            0x25, 0x7F,
            0x75, 0x08,
            0x95, 0x03,
            0x81, 0x06,
            0x05, 0x0C,
            0x0A, 0x38, 0x02,
            0x15, 0x81,
            0x25, 0x7F,
            0x75, 0x08,
            0x95, 0x01,
            0x81, 0x06,
            0xC0,
            0xC0,
        ).map { it.toByte() }.toByteArray()
    }
}
