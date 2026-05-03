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
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
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
    private lateinit var hostText: TextView
    private lateinit var reportText: TextView
    private lateinit var trackpadHintText: TextView
    private lateinit var dragButton: Button
    private lateinit var registerButton: Button
    private lateinit var unregisterButton: Button
    private lateinit var discoverableButton: Button
    private lateinit var previousHostButton: Button
    private lateinit var nextHostButton: Button
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
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
    private var bondedHosts: List<BluetoothDevice> = emptyList()
    private var selectedHostIndex = 0
    private var connectionState = BluetoothProfile.STATE_DISCONNECTED
    private var sentReportCount = 0
    private var failedReportCount = 0
    private var lastReportSummary = "아직 전송 없음"
    private var previousBluetoothName: String? = null

    private val mainExecutor = Executor { command -> runOnUiThread(command) }

    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager.adapter

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDevice = proxy as BluetoothHidDevice
            refreshBondedHosts(showStatus = false)
            setStatus("HID 프로필을 열었습니다. HID 등록 또는 페어링된 호스트 연결을 진행하세요.")
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
            connectionState = BluetoothProfile.STATE_DISCONNECTED
            setStatus("HID 프로필 연결이 끊어졌습니다.")
            refreshControls()
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            appRegistered = registered
            refreshBondedHosts(showStatus = false)
            if (registered) {
                activeHost = pluggedDevice ?: connectedHost()
                setStatus("HID 앱이 등록됐습니다. 왼쪽 연결 영역에서 PC를 연결한 뒤 터치패드를 사용하세요.")
            } else {
                activeHost = null
                dragMode = false
                connectionState = BluetoothProfile.STATE_DISCONNECTED
                setStatus("HID 앱 등록이 해제됐습니다.")
            }
            refreshControls()
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            connectionState = state
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
            connectionState = BluetoothProfile.STATE_DISCONNECTED
            setStatus("가상 케이블 연결이 해제됐습니다.")
            refreshControls()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        setContentView(buildContentView())
        updateDeviceText()
        refreshBondedHosts(showStatus = false)
        refreshCompatibility()
        refreshControls()
    }

    override fun onStop() {
        if (dragMode) {
            releaseAllMouseButtons("activity_stopped")
            dragMode = false
            refreshControls()
        }
        super.onStop()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        releaseAllMouseButtons("activity_destroyed")
        if (hasNearbyDevicePermissions()) hidDevice?.unregisterApp()
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
            refreshBondedHosts(showStatus = false)
            if (pendingRegister) openHidProfile()
        } else {
            pendingRegister = false
            setStatus("Nearby devices 권한이 필요합니다. 권한 없이는 HID 등록과 호스트 연결을 진행할 수 없습니다.")
        }
        refreshControls()
    }

    private fun buildContentView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(COLOR_BACKGROUND)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        root.setOnApplyWindowInsetsListener { view, insets ->
            @Suppress("DEPRECATION")
            view.setPadding(
                insets.systemWindowInsetLeft + dp(16),
                insets.systemWindowInsetTop + dp(10),
                insets.systemWindowInsetRight + dp(16),
                insets.systemWindowInsetBottom + dp(10),
            )
            insets
        }

        val connectionPanel = buildConnectionPanel()
        root.addView(
            connectionPanel,
            LinearLayout.LayoutParams(dp(370), ViewGroup.LayoutParams.MATCH_PARENT).apply {
                rightMargin = dp(14)
            },
        )

        val touchpadPanel = buildTouchpadPanel()
        root.addView(
            touchpadPanel,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f),
        )

        return root
    }

    private fun buildConnectionPanel(): View {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 24f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        column.addView(title, matchWrap())

        deviceText = TextView(this).apply {
            textSize = 13f
            setTextColor(COLOR_MUTED)
            setPadding(0, dp(2), 0, dp(12))
        }
        column.addView(deviceText, matchWrap())

        column.addView(sectionLabel("연결 영역"), matchWrap(bottom = 8))

        statusText = infoBox()
        column.addView(statusText, matchWrap(bottom = 10))

        column.addView(sectionLabel("순서 가이드"), matchWrap(bottom = 6))
        column.addView(
            infoBox().apply { text = setupGuideText() },
            matchWrap(bottom = 10),
        )

        column.addView(
            buttonRow(
                actionButton("권한", ::requestNearbyDevicePermissions),
                actionButton("Bluetooth 설정", ::openBluetoothSettings),
            ),
            matchWrap(bottom = 8),
        )

        discoverableButton = actionButton("검색 허용") { requestDiscoverable() }
        column.addView(discoverableButton, matchWrap(bottom = 8))

        registerButton = actionButton("HID 등록", ::prepareHidRegistration)
        unregisterButton = actionButton("HID 해제", ::unregisterHidApp)
        column.addView(buttonRow(registerButton, unregisterButton), matchWrap(bottom = 12))

        hostText = infoBox()
        column.addView(hostText, matchWrap(bottom = 8))

        previousHostButton = actionButton("이전", ::selectPreviousHost)
        nextHostButton = actionButton("다음", ::selectNextHost)
        column.addView(buttonRow(previousHostButton, nextHostButton), matchWrap(bottom = 8))

        column.addView(
            buttonRow(
                actionButton("목록 새로고침") { refreshBondedHosts(showStatus = true) },
                actionButton("페어링 설정", ::openBluetoothSettings),
            ),
            matchWrap(bottom = 8),
        )

        connectButton = actionButton("선택 호스트 연결", ::connectSelectedHost)
        disconnectButton = actionButton("연결 해제", ::disconnectActiveHost)
        column.addView(buttonRow(connectButton, disconnectButton), matchWrap(bottom = 12))

        reportText = infoBox()
        column.addView(reportText, matchWrap())

        return ScrollView(this).apply {
            background = rounded(COLOR_PANEL, dp(8), COLOR_STROKE)
            isFillViewport = true
            addView(column)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun buildTouchpadPanel(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(COLOR_PANEL_ALT, dp(8), COLOR_STROKE)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(
            TextView(this).apply {
                text = "터치패드 모드"
                textSize = 22f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        header.addView(
            TextView(this).apply {
                text = "한 손가락 이동 · 두 손가락 스크롤 · 탭 클릭"
                textSize = 13f
                setTextColor(COLOR_MUTED)
                gravity = Gravity.END
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        panel.addView(header, matchWrap(bottom = 10))

        trackpadSurface = FrameLayout(this).apply {
            isClickable = true
            isFocusable = true
            background = rounded(COLOR_TRACKPAD, dp(8), COLOR_TRACKPAD_STROKE)
            setOnTouchListener(::handleTrackpadTouch)
        }
        trackpadHintText = TextView(this).apply {
            text = "왼쪽 연결 영역에서 HID 등록과 호스트 연결을 완료하세요"
            textSize = 17f
            setTextColor(COLOR_TRACKPAD_LABEL)
            gravity = Gravity.CENTER
            setPadding(dp(18), 0, dp(18), 0)
        }
        trackpadSurface.addView(
            trackpadHintText,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        panel.addView(
            trackpadSurface,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                bottomMargin = dp(12)
            },
        )

        panel.addView(
            buttonRow(
                actionButton("왼쪽 클릭") { clickMouse(LEFT_BUTTON) },
                actionButton("오른쪽 클릭") { clickMouse(RIGHT_BUTTON) },
                actionButton("스크롤 ↑") { sendMouseReport(0, 0, currentButtons(), 5, 0) },
                actionButton("스크롤 ↓") { sendMouseReport(0, 0, currentButtons(), -5, 0) },
                actionButton("테스트 이동") { sendMouseReport(35, 0, currentButtons(), 0, 0) },
            ),
            matchWrap(bottom = 10),
        )

        dragButton = actionButton("Drag", ::toggleDragMode).apply {
            minHeight = dp(54)
        }
        panel.addView(dragButton, matchWrap())

        return panel
    }

    private fun updateDeviceText() {
        deviceText.text = String.format(
            Locale.US,
            "%s %s · Android %s(API %d) · targetSdk 36 · landscape",
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
            hasNearbyDevicePermissions() -> setStatus("준비됐습니다. HID 등록 후 PC를 페어링하거나 페어링된 호스트를 연결하세요.")
            else -> setStatus("Nearby devices 권한을 허용하면 HID 등록을 진행할 수 있습니다.")
        }
    }

    private fun refreshControls() {
        runOnUiThread {
            val hasPermissions = hasNearbyDevicePermissions()
            val adapterEnabled = bluetoothAdapter?.isEnabled == true
            val host = connectedHost()
            val canUseBluetooth = hasPermissions && adapterEnabled
            registerButton.isEnabled = canUseBluetooth && !appRegistered
            unregisterButton.isEnabled = hidDevice != null || appRegistered
            discoverableButton.isEnabled = canUseBluetooth
            previousHostButton.isEnabled = bondedHosts.size > 1
            nextHostButton.isEnabled = bondedHosts.size > 1
            connectButton.isEnabled = canUseBluetooth && appRegistered && hidDevice != null && selectedHost() != null
            disconnectButton.isEnabled = canUseBluetooth && hidDevice != null && host != null
            trackpadSurface.isEnabled = true
            dragButton.isEnabled = appRegistered
            dragButton.text = if (dragMode) "Dragging" else "Drag"
            dragButton.background = rounded(
                if (dragMode) COLOR_DRAG_ACTIVE else COLOR_BUTTON,
                dp(8),
                if (dragMode) COLOR_DRAG_STROKE else COLOR_BUTTON_STROKE,
            )
            hostText.text = buildHostText(host)
            reportText.text = buildReportText()
            trackpadHintText.text = when {
                !appRegistered -> "HID 등록 전입니다"
                host == null -> "호스트 연결 대기 중"
                dragMode -> "Dragging · 손가락을 밀면 왼쪽 버튼을 누른 채 이동"
                else -> "터치패드 활성 · 한 손가락 이동 / 두 손가락 스크롤 / 탭 클릭"
            }
            trackpadHintText.setTextColor(if (host == null) COLOR_TRACKPAD_LABEL else COLOR_TRACKPAD_READY)
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
        connectionState = BluetoothProfile.STATE_DISCONNECTED
        setStatus("HID 앱을 해제했습니다.")
        refreshControls()
    }

    private fun requestNearbyDevicePermissions() {
        if (hasNearbyDevicePermissions()) {
            setStatus("필요한 Bluetooth 권한이 이미 허용돼 있습니다.")
            refreshBondedHosts(showStatus = false)
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

    @SuppressLint("MissingPermission")
    private fun requestDiscoverable() {
        if (!hasNearbyDevicePermissions()) {
            requestNearbyDevicePermissions()
            return
        }
        if (bluetoothAdapter?.isEnabled != true) {
            openBluetoothSettings()
            return
        }
        val adapter = bluetoothAdapter
        if (adapter != null && adapter.name != ADVERTISED_DEVICE_NAME) {
            previousBluetoothName = adapter.name
            val renamed = adapter.setName(ADVERTISED_DEVICE_NAME)
            if (!renamed) {
                setStatus("Bluetooth 이름을 PhonePad로 바꾸지 못했습니다. PC에는 '${adapter.name}' 이름으로 보일 수 있습니다.")
            }
        }
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_SECONDS)
        }
        startActivity(intent)
        setStatus("PC의 Bluetooth 기기 추가 화면에서 PhonePad를 검색하세요. 안 보이면 폰 이름 '${previousBluetoothName ?: adapter?.name ?: "Android"}'도 확인하세요.")
    }

    @SuppressLint("MissingPermission")
    private fun refreshBondedHosts(showStatus: Boolean) {
        bondedHosts = if (hasNearbyDevicePermissions()) {
            bluetoothAdapter?.bondedDevices
                ?.sortedBy { it.safeSortLabel() }
                ?: emptyList()
        } else {
            emptyList()
        }
        if (selectedHostIndex !in bondedHosts.indices) selectedHostIndex = 0
        if (showStatus) setStatus("페어링된 기기 ${bondedHosts.size}개를 불러왔습니다.")
        refreshControls()
    }

    private fun selectPreviousHost() {
        if (bondedHosts.isEmpty()) return
        selectedHostIndex = (selectedHostIndex - 1 + bondedHosts.size) % bondedHosts.size
        setStatus("선택 호스트: ${selectedHost().safeLabel()}")
        refreshControls()
    }

    private fun selectNextHost() {
        if (bondedHosts.isEmpty()) return
        selectedHostIndex = (selectedHostIndex + 1) % bondedHosts.size
        setStatus("선택 호스트: ${selectedHost().safeLabel()}")
        refreshControls()
    }

    @SuppressLint("MissingPermission")
    private fun connectSelectedHost() {
        if (!hasNearbyDevicePermissions()) {
            requestNearbyDevicePermissions()
            return
        }
        if (!appRegistered || hidDevice == null) {
            setStatus("먼저 HID 등록을 완료하세요.")
            return
        }
        val host = selectedHost()
        if (host == null) {
            refreshBondedHosts(showStatus = false)
            setStatus("페어링된 호스트가 없습니다. PC Bluetooth 설정에서 PhonePad를 먼저 페어링하세요.")
            return
        }
        val accepted = hidDevice?.connect(host) == true
        if (accepted) {
            activeHost = host
            connectionState = BluetoothProfile.STATE_CONNECTING
            setStatus("호스트 연결 요청을 보냈습니다: ${host.safeLabel()}")
        } else {
            setStatus("호스트 연결 요청이 거절됐습니다. PC Bluetooth 화면에서 PhonePad를 직접 연결해보세요.")
        }
        refreshControls()
    }

    @SuppressLint("MissingPermission")
    private fun disconnectActiveHost() {
        if (!hasNearbyDevicePermissions()) return
        val host = connectedHost() ?: activeHost ?: selectedHost()
        if (host == null) {
            setStatus("연결 해제할 호스트가 없습니다.")
            return
        }
        releaseAllMouseButtons("manual_disconnect")
        val accepted = hidDevice?.disconnect(host) == true
        activeHost = null
        dragMode = false
        connectionState = BluetoothProfile.STATE_DISCONNECTED
        setStatus(if (accepted) "호스트 연결 해제를 요청했습니다." else "호스트 연결 해제 요청이 거절됐습니다.")
        refreshControls()
    }

    private fun handleTrackpadTouch(view: View, event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN && readyHostForInput(showStatus = true) == null) {
            return true
        }
        if (readyHostForInput(showStatus = false) == null) return true

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
                    val wheel = (-dy / 11f).roundToInt().coerceIn(-12, 12)
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

    private fun readyHostForInput(showStatus: Boolean): BluetoothDevice? {
        val host = connectedHost()
        val message = when {
            !hasNearbyDevicePermissions() -> "Bluetooth 권한이 필요합니다."
            !appRegistered -> "HID 등록이 필요합니다."
            host == null -> "HID는 등록됐지만 호스트 연결이 없습니다. 왼쪽에서 페어링된 PC를 선택해 연결하세요."
            else -> null
        }
        if (message != null && showStatus) {
            setStatus(message)
            refreshControls()
        }
        return host
    }

    private fun toggleDragMode() {
        if (readyHostForInput(showStatus = true) == null) return
        if (dragMode) {
            val ok = releaseAllMouseButtons("drag_off")
            dragMode = false
            setStatus(if (ok) "Drag Mode를 껐습니다." else "버튼 해제 보고서를 보낼 호스트가 없습니다.")
        } else {
            val ok = sendMouseReport(0, 0, LEFT_BUTTON, 0, 0)
            dragMode = ok
            if (ok) trackpadSurface.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            setStatus(if (ok) "Drag Mode를 켰습니다." else "Drag Mode 시작에 실패했습니다. 호스트 연결을 확인하세요.")
        }
        refreshControls()
    }

    private fun clickMouse(button: Int) {
        if (dragMode) {
            setStatus("Drag Mode 중에는 클릭 대신 왼쪽 버튼을 유지합니다.")
            return
        }
        if (readyHostForInput(showStatus = true) == null) return
        val down = sendMouseReport(0, 0, button, 0, 0)
        trackpadSurface.postDelayed({
            val up = releaseAllMouseButtons("click")
            if (down && up) {
                trackpadSurface.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                setStatus("클릭 보고서를 보냈습니다.")
            } else {
                setStatus("클릭 보고서 전송에 실패했습니다. 호스트 연결을 확인하세요.")
            }
            refreshControls()
        }, CLICK_RELEASE_DELAY_MS)
    }

    private fun currentButtons(): Int = if (dragMode) LEFT_BUTTON else 0

    @SuppressLint("MissingPermission")
    private fun sendMouseReport(dx: Int, dy: Int, buttons: Int, wheel: Int, horizontalWheel: Int): Boolean {
        if (!hasNearbyDevicePermissions()) return recordReport(false, "권한 없음")
        val device = connectedHost()
        val hid = hidDevice
        if (hid == null || device == null || !appRegistered) return recordReport(false, "호스트 없음")
        val payload = byteArrayOf(
            buttons.coerceIn(0, 7).toByte(),
            dx.coerceIn(-127, 127).toByte(),
            dy.coerceIn(-127, 127).toByte(),
            wheel.coerceIn(-127, 127).toByte(),
            horizontalWheel.coerceIn(-127, 127).toByte(),
        )
        val ok = hid.sendReport(device, MOUSE_REPORT_ID, payload)
        val summary = "dx=$dx dy=$dy wheel=$wheel hWheel=$horizontalWheel buttons=$buttons"
        Log.d(LOG_TAG, "send mouse report ok=$ok host=${device.address} $summary")
        return recordReport(ok, summary)
    }

    @SuppressLint("MissingPermission")
    private fun releaseAllMouseButtons(reason: String): Boolean {
        val device = connectedHost()
        val hid = hidDevice ?: return false
        if (!hasNearbyDevicePermissions() || device == null || !appRegistered) return false
        val ok = hid.sendReport(device, MOUSE_REPORT_ID, byteArrayOf(0, 0, 0, 0, 0))
        Log.d(LOG_TAG, "release mouse buttons ok=$ok host=${device.address} reason=$reason")
        recordReport(ok, "release_all reason=$reason")
        if (!ok && reason != "activity_destroyed") {
            setStatus("버튼 해제 보고서 전송에 실패했습니다: $reason")
        }
        return ok
    }

    private fun recordReport(ok: Boolean, summary: String): Boolean {
        if (ok) sentReportCount++ else failedReportCount++
        lastReportSummary = summary
        if (!ok || sentReportCount % 8 == 0) refreshControls()
        return ok
    }

    @SuppressLint("MissingPermission")
    private fun connectedHost(): BluetoothDevice? {
        if (!hasNearbyDevicePermissions()) return null
        val hid = hidDevice ?: return null
        val current = activeHost
        if (current != null && hid.getConnectionState(current) == BluetoothProfile.STATE_CONNECTED) {
            connectionState = BluetoothProfile.STATE_CONNECTED
            return current
        }
        val connected = hid.connectedDevices.firstOrNull()
        if (connected != null) {
            activeHost = connected
            connectionState = BluetoothProfile.STATE_CONNECTED
        }
        return connected
    }

    private fun selectedHost(): BluetoothDevice? = bondedHosts.getOrNull(selectedHostIndex)

    private fun buildHostText(host: BluetoothDevice?): String {
        val selected = selectedHost()
        return buildString {
            appendLine("활성 호스트: ${host.safeLabel()}")
            appendLine("연결 상태: ${connectionState.toConnectionLabel()}")
            appendLine("선택 호스트: ${selected.safeLabel()}")
            append("페어링 목록: ${bondedHosts.size}개")
        }
    }

    private fun buildReportText(): String {
        return buildString {
            appendLine("전송 성공: $sentReportCount")
            appendLine("전송 실패: $failedReportCount")
            append("마지막: $lastReportSummary")
        }
    }

    private fun setupGuideText(): String {
        return buildString {
            appendLine("1. 권한 허용")
            appendLine("2. HID 등록")
            appendLine("3. 검색 허용")
            appendLine("4. PC Bluetooth에서 PhonePad 검색")
            appendLine("5. 목록 새로고침")
            appendLine("6. 선택 호스트 연결")
            append("7. 오른쪽 터치패드 사용")
        }
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice?.safeLabel(): String {
        if (this == null) return "없음"
        return if (hasNearbyDevicePermissions()) {
            name?.takeIf { it.isNotBlank() } ?: address
        } else {
            "권한 필요"
        }
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.safeSortLabel(): String {
        return if (hasNearbyDevicePermissions()) {
            "${name ?: ""} $address"
        } else {
            address
        }
    }

    private fun Int.toConnectionLabel(): String {
        return when (this) {
            BluetoothProfile.STATE_CONNECTED -> "연결됨"
            BluetoothProfile.STATE_CONNECTING -> "연결 중"
            BluetoothProfile.STATE_DISCONNECTING -> "연결 해제 중"
            else -> "연결 안 됨"
        }
    }

    private fun setStatus(message: String) {
        runOnUiThread {
            statusText.text = message
        }
    }

    private fun sectionLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(COLOR_ACCENT)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0f
        }
    }

    private fun infoBox(): TextView {
        return TextView(this).apply {
            textSize = 13f
            setTextColor(Color.WHITE)
            setLineSpacing(2f, 1.05f)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(COLOR_INFO, dp(8), COLOR_STROKE)
        }
    }

    private fun actionButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 13f
            isAllCaps = false
            minHeight = dp(44)
            setTextColor(Color.WHITE)
            background = rounded(COLOR_BUTTON, dp(8), COLOR_BUTTON_STROKE)
            setOnClickListener { onClick() }
        }
    }

    private fun buttonRow(vararg buttons: Button): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            buttons.forEachIndexed { index, button ->
                addView(
                    button,
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (index > 0) leftMargin = dp(6)
                    },
                )
            }
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val NEARBY_DEVICES_REQUEST = 4028
        private const val MOUSE_REPORT_ID = 1
        private const val LEFT_BUTTON = 1
        private const val RIGHT_BUTTON = 2
        private const val POINTER_SCALE = 1.55f
        private const val DISCOVERABLE_SECONDS = 300
        private const val CLICK_RELEASE_DELAY_MS = 35L
        private const val LOG_TAG = "PhonePad"
        private const val ADVERTISED_DEVICE_NAME = "PhonePad"

        private val COLOR_BACKGROUND = Color.rgb(10, 12, 15)
        private val COLOR_PANEL = Color.rgb(28, 32, 39)
        private val COLOR_PANEL_ALT = Color.rgb(23, 27, 34)
        private val COLOR_INFO = Color.rgb(35, 41, 50)
        private val COLOR_STROKE = Color.rgb(72, 83, 100)
        private val COLOR_TRACKPAD = Color.rgb(15, 19, 25)
        private val COLOR_TRACKPAD_STROKE = Color.rgb(94, 111, 134)
        private val COLOR_TRACKPAD_LABEL = Color.rgb(136, 149, 169)
        private val COLOR_TRACKPAD_READY = Color.rgb(223, 232, 244)
        private val COLOR_MUTED = Color.rgb(166, 177, 194)
        private val COLOR_ACCENT = Color.rgb(132, 189, 177)
        private val COLOR_BUTTON = Color.rgb(46, 55, 68)
        private val COLOR_BUTTON_STROKE = Color.rgb(96, 112, 134)
        private val COLOR_DRAG_ACTIVE = Color.rgb(169, 57, 68)
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
