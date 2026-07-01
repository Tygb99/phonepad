package com.tygb99.phonepad

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
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
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
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
    private lateinit var connectionStatusChip: TextView
    private lateinit var dragButton: Button
    private lateinit var connectionDrawer: FrameLayout
    private lateinit var connectionDrawerButton: Button
    private lateinit var languageToggleButton: Button
    private lateinit var compactHostOsButton: Button
    private lateinit var hostOsButton: Button
    private lateinit var discoverableButton: Button
    private lateinit var previousHostButton: Button
    private lateinit var nextHostButton: Button
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var removeBondButton: Button
    private lateinit var guideToggleButton: Button
    private lateinit var guideBox: TextView
    private lateinit var debugToggleButton: Button
    private lateinit var advancedToggleButton: Button
    private lateinit var advancedControls: LinearLayout
    private lateinit var trackpadSurface: FrameLayout
    private lateinit var doubleTapDragButton: Button
    private lateinit var scrollSpeedButton: Button

    private var hidDevice: BluetoothHidDevice? = null
    private var activeHost: BluetoothDevice? = null
    private var appRegistered = false
    private var pendingRegister = false
    private var dragMode = false
    private var lastX = 0f
    private var lastY = 0f
    private var gestureDistance = 0f
    private var usingScrollGesture = false
    private var doubleTapDragEnabled = false
    private var doubleTapDragActive = false
    private var lastTapUpTime = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f
    private var scrollSpeedPreset = SCROLL_SPEED_DEFAULT
    private var bondedHosts: List<BluetoothDevice> = emptyList()
    private var selectedHostIndex = 0
    private var connectionState = BluetoothProfile.STATE_DISCONNECTED
    private var sentReportCount = 0
    private var failedReportCount = 0
    private var lastReportSummary = "아직 전송 없음"
    private var previousBluetoothName: String? = null
    private var activeScrollButton: Button? = null
    private var activeScrollWheel = 0
    private var guideExpanded = false
    private var debugExpanded = false
    private var advancedExpanded = false
    private var drawerBackCallbackRegistered = false
    private var drawerBackCallback: Any? = null
    private var autoSessionActive = false
    private var externalBluetoothFlowActive = false
    private var autoReconnectAttempted = false
    private var skipAutoReconnectOnce = false
    private var pendingAutoReconnectAddress: String? = null
    private var pendingDiscoverableRequest = false
    private var pendingConnectionAddress: String? = null
    private var pendingConnectionReason: String? = null
    private var pendingHostSwitchAddress: String? = null
    private var pendingHostSwitchReason: String? = null
    private var timedOutConnectionAddress: String? = null
    private var preDiscoverableBondedAddresses: Set<String> = emptySet()
    private var hidForegroundServiceRunning = false
    private var compactHostOsDownTimeMs = 0L
    private var compactHostOsPressCycled = false
    private var compactHostOsPressCanceled = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val mainExecutor = Executor { command -> runOnUiThread(command) }
    private val compactHostOsLongPressRunnable = Runnable {
        if (HostInputPolicy.shouldCycleCompactHostOsPress(
                HostInputPolicy.COMPACT_HOST_OS_LONG_PRESS_MS,
                compactHostOsPressCycled,
            )
        ) {
            compactHostOsPressCycled = true
            compactHostOsButton.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            cycleHostOsPreset()
        }
    }
    private val scrollRepeatRunnable = object : Runnable {
        override fun run() {
            val button = activeScrollButton ?: return
            val wheel = activeScrollWheel
            if (wheel == 0) return
            if (readyHostForInput(showStatus = false) == null) {
                stopContinuousScroll()
                return
            }
            if (!sendMouseReport(0, 0, currentButtons(), wheel, 0)) {
                stopContinuousScroll()
                refreshControls()
                return
            }
            button.postDelayed(this, scrollRepeatIntervalMs())
        }
    }

    private val sessionCleanupRunnable = Runnable {
        autoSessionActive = false
        autoReconnectAttempted = false
        skipAutoReconnectOnce = false
        pendingAutoReconnectAddress = null
        pendingDiscoverableRequest = false
        pauseInputSession("activity_stopped")
    }

    private val autoReconnectTimeoutRunnable = Runnable {
        val address = pendingAutoReconnectAddress ?: return@Runnable
        if (activeHost?.address == address && connectionState == BluetoothProfile.STATE_CONNECTING) {
            clearPendingConnection(address)
            activeHost = null
            connectionState = BluetoothProfile.STATE_DISCONNECTED
            pendingAutoReconnectAddress = null
            setStatus("최근 PC 자동 재연결이 응답하지 않습니다. PC 후보를 선택해 다시 연결하거나 새 PC 연결을 눌러주세요.")
            reconcileHidForegroundService("auto_reconnect_timeout")
            refreshControls()
        }
        pendingAutoReconnectAddress = null
    }

    @SuppressLint("MissingPermission")
    private val connectionTimeoutRunnable = Runnable {
        val address = pendingConnectionAddress ?: return@Runnable
        val reason = pendingConnectionReason ?: "unknown"
        val host = activeHost?.takeIf { it.address == address }
            ?: selectedHost()?.takeIf { it.address == address }
            ?: bluetoothAdapter?.bondedDevices?.firstOrNull { it.address == address }

        val hostState = host?.let { hidDevice?.getConnectionState(it) }
        clearPendingConnection(address)
        if (host != null && hostState != BluetoothProfile.STATE_CONNECTED) {
            timedOutConnectionAddress = address
            logHostDiagnostic("connect_timeout_$reason", host)
            hidDevice?.disconnect(host)
            if (activeHost?.address == address) activeHost = null
            connectionState = BluetoothProfile.STATE_DISCONNECTED
            setStatus(connectionTimeoutMessage(reason, host))
            reconcileHidForegroundService("connect_timeout_$reason")
            refreshControls()
        }
    }

    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager.adapter

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDevice = proxy as BluetoothHidDevice
            refreshBondedHosts(showStatus = false)
            setStatus("HID 프로필을 열었습니다. 자동 세션을 준비하는 중입니다.")
            if (pendingRegister) {
                pendingRegister = false
                registerHidApp()
            } else {
                startInputSession()
            }
            refreshControls()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            stopContinuousScroll()
            releaseAllMouseButtons("hid_profile_disconnected")
            releaseAllKeyboardKeys("hid_profile_disconnected")
            pendingAutoReconnectAddress = null
            pendingDiscoverableRequest = false
            clearPendingConnection()
            clearPendingHostSwitch()
            timedOutConnectionAddress = null
            mainHandler.removeCallbacks(autoReconnectTimeoutRunnable)
            hidDevice = null
            activeHost = null
            appRegistered = false
            dragMode = false
            connectionState = BluetoothProfile.STATE_DISCONNECTED
            stopHidForegroundService("hid_profile_disconnected")
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
                setStatus("HID 세션이 준비됐습니다. 최근 PC 자동 재연결을 확인하는 중입니다.")
                if (pendingDiscoverableRequest) {
                    requestDiscoverable()
                } else if (skipAutoReconnectOnce) {
                    skipAutoReconnectOnce = false
                    setStatus("새 PC 연결 흐름에서 돌아왔습니다. PC 후보를 확인하는 중입니다.")
                } else {
                    attemptAutoReconnect()
                }
                reconcileHidForegroundService("hid_app_status_changed")
            } else {
                clearPendingConnection()
                clearPendingHostSwitch()
                pendingDiscoverableRequest = false
                activeHost = null
                dragMode = false
                connectionState = BluetoothProfile.STATE_DISCONNECTED
                stopHidForegroundService("hid_app_unregistered")
                setStatus("HID 앱 등록이 해제됐습니다.")
            }
            refreshControls()
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            connectionState = state
            logHostDiagnostic("callback_state_${state.toConnectionLabel()}", device)
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    activeHost = device
                    pendingAutoReconnectAddress = null
                    clearPendingConnection()
                    clearPendingHostSwitch()
                    if (timedOutConnectionAddress == device?.address) timedOutConnectionAddress = null
                    mainHandler.removeCallbacks(autoReconnectTimeoutRunnable)
                    device?.rememberSuccessfulHost()
                    refreshBondedHosts(showStatus = false)
                    startHidForegroundService("host_connected")
                    setStatus("호스트와 연결됐습니다: ${device.safeLabel()}")
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    startHidForegroundService("host_connecting")
                    setStatus("호스트 연결 중: ${device.safeLabel()}")
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    stopContinuousScroll()
                    releaseAllMouseButtons("host_disconnecting")
                    releaseAllKeyboardKeys("host_disconnecting")
                    if (
                        timedOutConnectionAddress != device?.address &&
                        pendingConnectionAddress == null &&
                        pendingHostSwitchAddress == null
                    ) {
                        setStatus("호스트 연결 해제 중: ${device.safeLabel()}")
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    stopContinuousScroll()
                    releaseAllMouseButtons("host_disconnected")
                    releaseAllKeyboardKeys("host_disconnected")
                    if (doubleTapDragActive) doubleTapDragActive = false
                    val wasAutoReconnect = pendingAutoReconnectAddress == device?.address
                    val wasPendingConnection = pendingConnectionAddress == device?.address
                    val wasTimedOutConnection = timedOutConnectionAddress == device?.address
                    val hasOtherPendingConnection = pendingConnectionAddress != null && !wasPendingConnection
                    val hasPendingHostSwitch = pendingHostSwitchAddress != null
                    if (wasAutoReconnect) {
                        pendingAutoReconnectAddress = null
                        mainHandler.removeCallbacks(autoReconnectTimeoutRunnable)
                        setStatus("최근 PC 자동 재연결에 실패했습니다. PC 후보를 선택해 다시 연결하거나 새 PC 연결을 눌러주세요.")
                    }
                    if (wasPendingConnection) {
                        clearPendingConnection(device?.address)
                        setStatus("호스트 연결에 실패했습니다: ${device.safeLabel()}. Windows Bluetooth에서 장치를 삭제한 뒤 새 PC 연결로 다시 페어링해 보세요.")
                    }
                    if (activeHost?.address == device?.address) activeHost = null
                    dragMode = false
                    if (hasPendingHostSwitch) {
                        setStatus("이전 호스트 연결이 끊어졌습니다. 다음 호스트 연결을 준비합니다.")
                        attemptPendingHostSwitch()
                    } else if (!wasAutoReconnect && !wasPendingConnection && !wasTimedOutConnection && !hasOtherPendingConnection) {
                        setStatus("호스트 연결이 끊어졌습니다.")
                    }
                    if (!hasPendingHostSwitch) {
                        reconcileHidForegroundService("host_disconnected")
                    }
                }
            }
            refreshControls()
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice?) {
            stopContinuousScroll()
            releaseAllMouseButtons("virtual_cable_unplug")
            releaseAllKeyboardKeys("virtual_cable_unplug")
            if (pendingAutoReconnectAddress == device?.address) {
                pendingAutoReconnectAddress = null
                mainHandler.removeCallbacks(autoReconnectTimeoutRunnable)
            }
            clearPendingConnection(device?.address)
            clearPendingHostSwitch(device?.address)
            if (timedOutConnectionAddress == device?.address) timedOutConnectionAddress = null
            if (activeHost?.address == device?.address) activeHost = null
            dragMode = false
            connectionState = BluetoothProfile.STATE_DISCONNECTED
            stopHidForegroundService("virtual_cable_unplug")
            setStatus("가상 케이블 연결이 해제됐습니다.")
            refreshControls()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        loadInputSettings()
        setContentView(buildContentView())
        updateDeviceText()
        refreshBondedHosts(showStatus = false)
        refreshCompatibility()
        refreshControls()
    }

    override fun onStart() {
        super.onStart()
        mainHandler.removeCallbacks(sessionCleanupRunnable)
        if (!autoSessionActive) {
            autoSessionActive = true
            autoReconnectAttempted = false
            skipAutoReconnectOnce = false
            pendingAutoReconnectAddress = null
            pendingDiscoverableRequest = false
        }
        val returnedFromBluetoothFlow = externalBluetoothFlowActive
        if (externalBluetoothFlowActive) externalBluetoothFlowActive = false
        if (returnedFromBluetoothFlow) skipAutoReconnectOnce = true
        startInputSession()
        if (returnedFromBluetoothFlow) {
            mainHandler.postDelayed({ handleReturnFromNewPcFlow() }, NEW_PAIRING_SCAN_DELAY_MS)
        }
    }

    override fun onResume() {
        super.onResume()
        if (externalBluetoothFlowActive) externalBluetoothFlowActive = false
    }

    override fun onStop() {
        stopContinuousScroll()
        if (externalBluetoothFlowActive) {
            mainHandler.removeCallbacks(sessionCleanupRunnable)
            mainHandler.postDelayed(sessionCleanupRunnable, EXTERNAL_FLOW_STOP_GRACE_MS)
            super.onStop()
            return
        }
        mainHandler.removeCallbacks(sessionCleanupRunnable)
        mainHandler.postDelayed(sessionCleanupRunnable, SESSION_STOP_GRACE_MS)
        super.onStop()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        mainHandler.removeCallbacks(sessionCleanupRunnable)
        unregisterDrawerBackCallback()
        stopInputSession("activity_destroyed", closeProfile = true)
        super.onDestroy()
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && isConnectionPanelVisible()) {
            hideConnectionPanel()
            return true
        }
        return super.onKeyUp(keyCode, event)
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
            startInputSession()
        } else {
            pendingRegister = false
            setStatus("Nearby devices 권한이 필요합니다. 권한 없이는 HID 세션과 호스트 연결을 진행할 수 없습니다.")
        }
        refreshControls()
    }

    private fun buildContentView(): View {
        val root = FrameLayout(this).apply {
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

        val touchpadPanel = buildTouchpadPanel()
        root.addView(
            touchpadPanel,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        connectionDrawer = FrameLayout(this).apply {
            visibility = View.GONE
            isClickable = true
            setBackgroundColor(COLOR_DRAWER_SCRIM)
            setOnClickListener { hideConnectionPanel() }
        }
        val connectionPanel = buildConnectionPanel().apply {
            setOnClickListener { }
        }
        connectionDrawer.addView(
            connectionPanel,
            FrameLayout.LayoutParams(dp(430), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START),
        )
        root.addView(
            connectionDrawer,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        return root
    }

    private fun buildConnectionPanel(): View {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleRow.addView(
            TextView(this).apply {
                text = getString(R.string.app_name)
                textSize = 24f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        titleRow.addView(actionButton("닫기") { hideConnectionPanel() }, fixedButtonParams(92))
        column.addView(titleRow, matchWrap())

        deviceText = TextView(this).apply {
            textSize = 13f
            setTextColor(COLOR_MUTED)
            setPadding(0, dp(2), 0, dp(12))
        }
        column.addView(deviceText, matchWrap())

        column.addView(sectionLabel("연결 영역"), matchWrap(bottom = 8))

        statusText = infoBox()
        column.addView(statusText, matchWrap(bottom = 10))

        discoverableButton = actionButton("새 PC 연결") { requestDiscoverable() }
        column.addView(discoverableButton, matchWrap(bottom = 10))

        hostText = infoBox()
        column.addView(hostText, matchWrap(bottom = 8))

        hostOsButton = actionButton("자동 추정", ::cycleHostOsPreset)
        column.addView(hostOsButton, matchWrap(bottom = 8))

        previousHostButton = actionButton("이전", ::selectPreviousHost)
        nextHostButton = actionButton("다음", ::selectNextHost)
        column.addView(buttonRow(previousHostButton, nextHostButton), matchWrap(bottom = 8))

        connectButton = actionButton("호스트 연결/전환", ::connectSelectedHost)
        disconnectButton = actionButton("연결 해제", ::disconnectActiveHost)
        column.addView(buttonRow(connectButton, disconnectButton), matchWrap(bottom = 8))

        guideToggleButton = actionButton("순서 가이드 보기") {
            guideExpanded = !guideExpanded
            refreshControls()
        }
        column.addView(guideToggleButton, matchWrap(bottom = 8))
        guideBox = infoBox().apply { text = setupGuideText() }
        column.addView(guideBox, matchWrap(bottom = 8))

        debugToggleButton = actionButton("전송 카운터 보기") {
            debugExpanded = !debugExpanded
            refreshControls()
        }
        column.addView(debugToggleButton, matchWrap(bottom = 8))
        reportText = infoBox()
        column.addView(reportText, matchWrap(bottom = 8))

        advancedToggleButton = actionButton("고급 연결 도구 보기") {
            advancedExpanded = !advancedExpanded
            refreshControls()
        }
        column.addView(advancedToggleButton, matchWrap(bottom = 8))
        advancedControls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                buttonRow(
                    actionButton("권한", ::requestNearbyDevicePermissions),
                    actionButton("Bluetooth 설정", ::openBluetoothSettings),
                ),
                matchWrap(bottom = 8),
            )
            addView(
                buttonRow(
                    actionButton("목록 새로고침") { refreshBondedHosts(showStatus = true) },
                    actionButton("페어링 설정", ::openBluetoothSettings),
                ),
                matchWrap(bottom = 8),
            )
            removeBondButton = actionButton("Android 페어링 삭제", ::removeSelectedHostBond)
            addView(removeBondButton, matchWrap())
        }
        column.addView(advancedControls, matchWrap())

        return ScrollView(this).apply {
            background = rounded(COLOR_PANEL, dp(8), COLOR_STROKE)
            isFillViewport = true
            addView(column)
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun buildTouchpadPanel(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(COLOR_PANEL_ALT, dp(8), COLOR_STROKE)
        }

        val touchpadColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        touchpadColumn.addView(
            TextView(this).apply {
                text = "터치패드 모드"
                textSize = 22f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
            },
            matchWrap(bottom = 2),
        )
        touchpadColumn.addView(
            TextView(this).apply {
                text = "한 손가락 이동 · 두 손가락 스크롤 · 탭 클릭"
                textSize = 13f
                setTextColor(COLOR_MUTED)
            },
            matchWrap(bottom = 10),
        )

        trackpadSurface = FrameLayout(this).apply {
            isClickable = true
            isFocusable = true
            background = rounded(COLOR_TRACKPAD, dp(8), COLOR_TRACKPAD_STROKE)
            setOnTouchListener(::handleTrackpadTouch)
        }
        trackpadHintText = TextView(this).apply {
            text = "자동 HID 세션을 준비하는 중입니다"
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
        touchpadColumn.addView(
            trackpadSurface,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
        )
        panel.addView(
            touchpadColumn,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                rightMargin = dp(14)
            },
        )

        val controlsColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        fun controlButton(text: String, onClick: () -> Unit): Button {
            return actionButton(text, onClick).apply {
                minHeight = dp(40)
                minimumHeight = dp(40)
                setPadding(dp(4), 0, dp(4), 0)
            }
        }
        fun controlScrollButton(text: String, direction: Int): Button {
            return scrollButton(text, direction).apply {
                minHeight = dp(40)
                minimumHeight = dp(40)
                setPadding(dp(4), 0, dp(4), 0)
            }
        }

        connectionStatusChip = statusChip()
        controlsColumn.addView(connectionStatusChip, matchWrap(bottom = 6))

        compactHostOsButton = controlButton("자동") {
            setStatus("호스트 OS 변경 잠금: 1초 이상 길게 눌러 변경하세요.")
        }
        compactHostOsButton.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.isPressed = true
                    compactHostOsDownTimeMs = event.eventTime
                    compactHostOsPressCycled = false
                    compactHostOsPressCanceled = false
                    mainHandler.postDelayed(
                        compactHostOsLongPressRunnable,
                        HostInputPolicy.COMPACT_HOST_OS_LONG_PRESS_MS,
                    )
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!compactHostOsPressCanceled && event.isOutside(view)) {
                        compactHostOsPressCanceled = true
                        mainHandler.removeCallbacks(compactHostOsLongPressRunnable)
                        view.isPressed = false
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    mainHandler.removeCallbacks(compactHostOsLongPressRunnable)
                    val pressDurationMs = event.eventTime - compactHostOsDownTimeMs
                    if (!compactHostOsPressCanceled &&
                        HostInputPolicy.shouldCycleCompactHostOsPress(pressDurationMs, compactHostOsPressCycled)
                    ) {
                        compactHostOsPressCycled = true
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        cycleHostOsPreset()
                    } else if (!compactHostOsPressCanceled && !compactHostOsPressCycled) {
                        view.performClick()
                    }
                    view.isPressed = false
                    compactHostOsDownTimeMs = 0L
                    compactHostOsPressCycled = false
                    compactHostOsPressCanceled = false
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    mainHandler.removeCallbacks(compactHostOsLongPressRunnable)
                    view.isPressed = false
                    compactHostOsDownTimeMs = 0L
                    compactHostOsPressCycled = false
                    compactHostOsPressCanceled = false
                    true
                }
                else -> true
            }
        }
        controlsColumn.addView(compactHostOsButton, matchWrap(bottom = 6))

        controlsColumn.addView(
            buttonRow(
                controlButton("한영", ::sendLanguageToggle).also { languageToggleButton = it },
                controlButton("연결", ::showConnectionPanel).also { connectionDrawerButton = it },
            ),
            matchWrap(bottom = 6),
        )

        controlsColumn.addView(
            buttonRow(
                controlButton("왼쪽 클릭") { clickMouse(LEFT_BUTTON) },
                controlButton("오른쪽 클릭") { clickMouse(RIGHT_BUTTON) },
            ),
            matchWrap(bottom = 6),
        )

        controlsColumn.addView(
            buttonRow(
                controlScrollButton("스크롤 ↑", SCROLL_UP),
                controlScrollButton("스크롤 ↓", SCROLL_DOWN),
            ),
            matchWrap(bottom = 6),
        )

        dragButton = controlButton("Drag", ::toggleDragMode)
        controlsColumn.addView(
            buttonRow(
                controlButton("테스트 이동") { sendMouseReport(35, 0, currentButtons(), 0, 0) },
                dragButton,
            ),
            matchWrap(bottom = 6),
        )

        doubleTapDragButton = controlButton("", ::toggleDoubleTapDrag)
        scrollSpeedButton = controlButton("", ::cycleScrollSpeed)
        controlsColumn.addView(buttonRow(doubleTapDragButton, scrollSpeedButton), matchWrap())

        panel.addView(
            controlsColumn,
            LinearLayout.LayoutParams(dp(300), ViewGroup.LayoutParams.MATCH_PARENT),
        )

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
            !adapter.isEnabled -> setStatus("Bluetooth가 꺼져 있습니다. Bluetooth를 켜면 HID 세션이 자동으로 준비됩니다.")
            hasNearbyDevicePermissions() -> setStatus("준비됐습니다. 앱이 HID 세션을 자동으로 준비합니다.")
            else -> setStatus("Nearby devices 권한을 허용하면 HID 세션이 자동으로 준비됩니다.")
        }
    }

    private fun refreshControls() {
        runOnUiThread {
            val hasPermissions = hasNearbyDevicePermissions()
            val adapterEnabled = bluetoothAdapter?.isEnabled == true
            val host = connectedHost()
            val canUseBluetooth = hasPermissions && adapterEnabled
            val settingsHost = hostForInputSettings()
            val resolvedHostOs = resolvedHostOsPreset(settingsHost)
            discoverableButton.isEnabled = canUseBluetooth
            previousHostButton.isEnabled = bondedHosts.size > 1
            nextHostButton.isEnabled = bondedHosts.size > 1
            connectButton.isEnabled = canUseBluetooth && appRegistered && hidDevice != null && selectedHost() != null
            disconnectButton.isEnabled = canUseBluetooth && hidDevice != null && host != null
            removeBondButton.isEnabled = canUseBluetooth && selectedHost() != null
            guideBox.visibility = if (guideExpanded) View.VISIBLE else View.GONE
            reportText.visibility = if (debugExpanded) View.VISIBLE else View.GONE
            advancedControls.visibility = if (advancedExpanded) View.VISIBLE else View.GONE
            guideToggleButton.text = if (guideExpanded) "순서 가이드 숨기기" else "순서 가이드 보기"
            debugToggleButton.text = if (debugExpanded) "전송 카운터 숨기기" else "전송 카운터 보기"
            advancedToggleButton.text = if (advancedExpanded) "고급 연결 도구 숨기기" else "고급 연결 도구 보기"
            trackpadSurface.isEnabled = true
            dragButton.isEnabled = appRegistered
            dragButton.text = if (dragMode) "Dragging" else "Drag"
            dragButton.background = rounded(
                if (dragMode) COLOR_DRAG_ACTIVE else COLOR_BUTTON,
                dp(8),
                if (dragMode) COLOR_DRAG_STROKE else COLOR_BUTTON_STROKE,
            )
            doubleTapDragButton.text = if (doubleTapDragEnabled) "더블탭 Drag ON" else "더블탭 Drag OFF"
            doubleTapDragButton.background = rounded(
                if (doubleTapDragEnabled) COLOR_INPUT_ACTIVE else COLOR_BUTTON,
                dp(8),
                if (doubleTapDragEnabled) COLOR_INPUT_STROKE else COLOR_BUTTON_STROKE,
            )
            scrollSpeedButton.text = String.format(Locale.US, "스크롤 %s", scrollSpeedLabel())
            hostOsButton.isEnabled = settingsHost != null
            hostOsButton.text = hostOsControlText(settingsHost)
            compactHostOsButton.isEnabled = settingsHost != null
            compactHostOsButton.text = resolvedHostOs.shortLabel
            languageToggleButton.text = if (resolvedHostOs == HostOsPreset.AUTO) "한영 설정" else "한영"
            languageToggleButton.isEnabled = canUseBluetooth && appRegistered && host != null
            connectionDrawerButton.text = if (!hasPermissions || !adapterEnabled || host == null) "연결!" else "연결"
            connectionDrawerButton.background = rounded(
                if (!hasPermissions || !adapterEnabled || host == null) COLOR_INPUT_ACTIVE else COLOR_BUTTON,
                dp(8),
                if (!hasPermissions || !adapterEnabled || host == null) COLOR_INPUT_STROKE else COLOR_BUTTON_STROKE,
            )
            connectionStatusChip.text = connectionChipText(host, appRegistered, hasPermissions, adapterEnabled)
            connectionStatusChip.background = rounded(
                if (host != null) COLOR_INPUT_ACTIVE else COLOR_INFO,
                dp(8),
                if (host != null) COLOR_INPUT_STROKE else COLOR_STROKE,
            )
            hostText.text = buildHostText(host)
            reportText.text = buildReportText()
            trackpadHintText.text = when {
                !appRegistered -> "HID 세션 자동 준비 중"
                host == null -> "호스트 연결 대기 중"
                dragMode -> "Dragging · 손가락을 밀면 왼쪽 버튼을 누른 채 이동"
                else -> "터치패드 활성 · 한 손가락 이동 / 두 손가락 스크롤 / 탭 클릭"
            }
            trackpadHintText.setTextColor(if (host == null) COLOR_TRACKPAD_LABEL else COLOR_TRACKPAD_READY)
        }
    }

    private fun startInputSession() {
        pendingRegister = true
        if (!hasNearbyDevicePermissions()) {
            requestNearbyDevicePermissions()
            return
        }
        if (bluetoothAdapter?.isEnabled != true) {
            pendingRegister = false
            setStatus("Bluetooth가 꺼져 있습니다. Bluetooth를 켜면 HID 세션이 자동으로 준비됩니다.")
            return
        }
        if (hidDevice == null) {
            openHidProfile()
            return
        }
        if (!appRegistered) {
            pendingRegister = false
            registerHidApp()
            return
        }
        pendingRegister = false
        if (pendingDiscoverableRequest) {
            requestDiscoverable()
        } else if (skipAutoReconnectOnce) {
            skipAutoReconnectOnce = false
            setStatus("새 PC 연결 흐름에서 돌아왔습니다. PC 후보를 확인하는 중입니다.")
        } else {
            attemptAutoReconnect()
        }
        refreshControls()
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
            advertisedDeviceName(),
            "Bluetooth HID mouse and keyboard for PhonePad",
            "PhonePad",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            MOUSE_KEYBOARD_DESCRIPTOR,
        )
        val accepted = device.registerApp(sdp, null, null, mainExecutor, hidCallback)
        setStatus(
            if (accepted) "HID 세션 등록 요청을 보냈습니다. 결과를 기다리는 중입니다."
            else "HID 세션 등록 요청이 거절됐습니다. 기기 또는 제조사 정책상 HID Device가 막혀 있을 수 있습니다.",
        )
        refreshControls()
    }

    @SuppressLint("MissingPermission")
    private fun stopInputSession(reason: String, closeProfile: Boolean) {
        stopContinuousScroll()
        releaseAllMouseButtons(reason)
        releaseAllKeyboardKeys(reason)
        pendingAutoReconnectAddress = null
        skipAutoReconnectOnce = false
        pendingDiscoverableRequest = false
        clearPendingConnection()
        clearPendingHostSwitch()
        mainHandler.removeCallbacks(autoReconnectTimeoutRunnable)
        dragMode = false
        doubleTapDragActive = false
        if (hasNearbyDevicePermissions()) hidDevice?.unregisterApp()
        appRegistered = false
        activeHost = null
        connectionState = BluetoothProfile.STATE_DISCONNECTED
        if (closeProfile) {
            hidDevice?.let { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it) }
            hidDevice = null
            stopHidForegroundService("input_session_closed")
        }
        if (!closeProfile && ::statusText.isInitialized) {
            setStatus("앱이 백그라운드로 이동해 HID 세션을 정리했습니다.")
            refreshControls()
        }
    }

    private fun startHidForegroundService(reason: String) {
        if (hidForegroundServiceRunning) return
        val intent = Intent(this, HidSessionService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            hidForegroundServiceRunning = true
            Log.d(LOG_TAG, "hid_foreground_service=start reason=$reason")
        } catch (error: RuntimeException) {
            Log.w(LOG_TAG, "Unable to start HID foreground service", error)
        }
    }

    private fun stopHidForegroundService(reason: String) {
        stopService(Intent(this, HidSessionService::class.java))
        if (hidForegroundServiceRunning) {
            Log.d(LOG_TAG, "hid_foreground_service=stop reason=$reason")
        }
        hidForegroundServiceRunning = false
    }

    @SuppressLint("MissingPermission")
    private fun reconcileHidForegroundService(reason: String) {
        val shouldRun = HidForegroundServicePolicy.shouldRun(
            appRegistered = appRegistered,
            connected = hasConnectedHostForForegroundService(),
            connectionPending = connectionState == BluetoothProfile.STATE_CONNECTING ||
                pendingConnectionAddress != null ||
                pendingAutoReconnectAddress != null ||
                pendingHostSwitchAddress != null,
            discoverablePending = pendingDiscoverableRequest || externalBluetoothFlowActive,
        )
        if (shouldRun) {
            startHidForegroundService(reason)
        } else {
            stopHidForegroundService(reason)
        }
    }

    @SuppressLint("MissingPermission")
    private fun hasConnectedHostForForegroundService(): Boolean {
        val active = activeHost
        val hid = hidDevice
        if (!hasNearbyDevicePermissions() || hid == null) {
            return active != null && connectionState == BluetoothProfile.STATE_CONNECTED
        }
        return active?.let { hid.getConnectionState(it) == BluetoothProfile.STATE_CONNECTED } == true ||
            hid.connectedDevices.isNotEmpty()
    }

    private fun pauseInputSession(reason: String) {
        stopContinuousScroll()
        if (dragMode || doubleTapDragActive) {
            releaseAllMouseButtons(reason)
        }
        pendingAutoReconnectAddress = null
        skipAutoReconnectOnce = false
        pendingDiscoverableRequest = false
        clearPendingConnection()
        clearPendingHostSwitch()
        mainHandler.removeCallbacks(autoReconnectTimeoutRunnable)
        dragMode = false
        doubleTapDragActive = false
        if (::statusText.isInitialized) {
            setStatus("앱이 백그라운드로 이동했습니다. HID 연결은 유지합니다.")
            refreshControls()
        }
    }

    private fun requestNearbyDevicePermissions() {
        if (hasNearbyDevicePermissions()) {
            setStatus("필요한 Bluetooth 권한이 이미 허용돼 있습니다.")
            refreshBondedHosts(showStatus = false)
            startInputSession()
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
        externalBluetoothFlowActive = true
        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    @SuppressLint("MissingPermission")
    private fun requestDiscoverable() {
        pendingDiscoverableRequest = true
        if (!hasNearbyDevicePermissions()) {
            requestNearbyDevicePermissions()
            return
        }
        if (bluetoothAdapter?.isEnabled != true) {
            openBluetoothSettings()
            return
        }
        if (hidDevice == null || !appRegistered) {
            pendingAutoReconnectAddress = null
            mainHandler.removeCallbacks(autoReconnectTimeoutRunnable)
            setStatus("새 PC 연결을 위해 HID 세션을 먼저 준비합니다.")
            startHidForegroundService("discoverable_prepare")
            startInputSession()
            refreshControls()
            return
        }
        startHidForegroundService("discoverable_request")
        val adapter = bluetoothAdapter
        val visibleName = setAdvertisedBluetoothName()
        if (visibleName == null) {
            setStatus("Bluetooth 이름을 PhonePad 형식으로 바꾸지 못했습니다. PC에는 '${adapter?.name ?: "Android"}' 이름으로 보일 수 있습니다.")
        }
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_SECONDS)
        }
        preDiscoverableBondedAddresses = allBondedAddresses()
        externalBluetoothFlowActive = true
        pendingDiscoverableRequest = false
        startActivity(intent)
        setStatus("PC의 Bluetooth 기기 추가 화면에서 ${visibleName ?: adapter?.name ?: "PhonePad"}를 검색하세요.")
    }

    @SuppressLint("MissingPermission")
    private fun handleReturnFromNewPcFlow() {
        if (!hasNearbyDevicePermissions()) return
        val newHost = bluetoothAdapter?.bondedDevices
            ?.filter { it.address !in preDiscoverableBondedAddresses }
            ?.sortedWith(compareByDescending<BluetoothDevice> { it.isLikelyComputerHost() }.thenBy { it.safeSortLabel() })
            ?.firstOrNull()

        if (newHost == null) {
            refreshBondedHosts(showStatus = false)
            reconcileHidForegroundService("new_pc_flow_no_candidate")
            return
        }

        rememberCandidateHost(newHost)
        refreshBondedHosts(showStatus = false)
        val index = bondedHosts.indexOfFirst { it.address == newHost.address }
        if (index >= 0) selectedHostIndex = index
        autoReconnectAttempted = true
        pendingAutoReconnectAddress = null
        mainHandler.removeCallbacks(autoReconnectTimeoutRunnable)
        setStatus("새 PC 후보를 선택했습니다: ${newHost.safeLabel()}. 0.1.5 방식처럼 호스트 연결/전환을 눌러 연결하세요.")

        if (!appRegistered || hidDevice == null) startInputSession()
        reconcileHidForegroundService("new_pc_flow_candidate_selected")
        refreshControls()
    }

    @SuppressLint("MissingPermission")
    private fun attemptAutoReconnect() {
        if (autoReconnectAttempted || !hasNearbyDevicePermissions()) return
        val hid = hidDevice ?: return
        if (!appRegistered) return
        autoReconnectAttempted = true

        if (connectedHost() != null) {
            startHidForegroundService("auto_reconnect_already_connected")
            setStatus("최근 PC와 이미 연결돼 있습니다.")
            refreshControls()
            return
        }

        refreshBondedHosts(showStatus = false)
        val candidate = autoReconnectCandidate()
        if (candidate == null) {
            setStatus("HID 세션이 준비됐습니다. 기존 PC가 없으면 새 PC 연결을 눌러 페어링하세요.")
            reconcileHidForegroundService("auto_reconnect_no_candidate")
            refreshControls()
            return
        }

        val index = bondedHosts.indexOfFirst { it.address == candidate.address }
        if (index >= 0) selectedHostIndex = index

        connectHost(candidate, reason = "auto_reconnect")
        refreshControls()
    }

    @SuppressLint("MissingPermission")
    private fun setAdvertisedBluetoothName(): String? {
        if (!hasNearbyDevicePermissions()) return null
        val adapter = bluetoothAdapter ?: return null
        val targetName = advertisedDeviceName()
        if (adapter.name == targetName) return targetName
        previousBluetoothName = adapter.name
        return if (adapter.setName(targetName)) targetName else null
    }

    @SuppressLint("MissingPermission")
    private fun advertisedDeviceName(): String {
        val currentName = if (hasNearbyDevicePermissions()) bluetoothAdapter?.name?.trim().orEmpty() else ""
        val alias = currentName
            .takeIf { it.isNotBlank() }
            ?.takeUnless { it == ADVERTISED_DEVICE_PREFIX || it.startsWith("$ADVERTISED_DEVICE_PREFIX -") }
            ?: deviceModelAlias()
        return "$ADVERTISED_DEVICE_PREFIX - ${alias.take(MAX_DEVICE_ALIAS_LENGTH)}"
    }

    private fun deviceModelAlias(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.titlecase(Locale.US) }
        return "$manufacturer ${Build.MODEL}".trim().ifBlank { "Android" }
    }

    @SuppressLint("MissingPermission")
    private fun refreshBondedHosts(showStatus: Boolean) {
        val knownHostAddresses = knownHostAddresses()
        val candidateHostAddresses = candidateHostAddresses()
        val selectedAddress = selectedHost()?.address
        val activeAddress = activeHost?.address
        val lastAddress = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_LAST_HOST_ADDRESS, null)
        val rememberedHostAddresses = knownHostAddresses +
            candidateHostAddresses +
            listOfNotNull(selectedAddress, activeAddress, lastAddress)
        bondedHosts = if (hasNearbyDevicePermissions()) {
            bluetoothAdapter?.bondedDevices
                ?.filter { device ->
                    device.address in rememberedHostAddresses ||
                        device.isLikelyComputerHost() ||
                        device.isLikelyNamedHost()
                }
                ?.sortedBy { it.safeSortLabel() }
                ?: emptyList()
        } else {
            emptyList()
        }
        val preferredAddress = listOfNotNull(selectedAddress, activeAddress, lastAddress)
            .firstOrNull { address -> bondedHosts.any { it.address == address } }
        selectedHostIndex = if (preferredAddress != null) {
            bondedHosts.indexOfFirst { it.address == preferredAddress }
        } else if (selectedHostIndex in bondedHosts.indices) {
            selectedHostIndex
        } else {
            0
        }
        if (showStatus) setStatus("페어링된 PC 후보 ${bondedHosts.size}개를 불러왔습니다.")
        refreshControls()
    }

    private fun selectPreviousHost() {
        if (bondedHosts.isEmpty()) return
        selectedHostIndex = (selectedHostIndex - 1 + bondedHosts.size) % bondedHosts.size
        setStatus("전환 대상: ${selectedHost().safeLabel()}")
        refreshControls()
    }

    private fun selectNextHost() {
        if (bondedHosts.isEmpty()) return
        selectedHostIndex = (selectedHostIndex + 1) % bondedHosts.size
        setStatus("전환 대상: ${selectedHost().safeLabel()}")
        refreshControls()
    }

    @SuppressLint("MissingPermission")
    private fun connectSelectedHost() {
        if (!hasNearbyDevicePermissions()) {
            requestNearbyDevicePermissions()
            return
        }
        if (!appRegistered || hidDevice == null) {
            setStatus("HID 세션을 자동 준비 중입니다. 잠시 후 다시 시도하세요.")
            return
        }
        val host = selectedHost()
        if (host == null) {
            refreshBondedHosts(showStatus = false)
            setStatus("페어링된 PC 후보가 없습니다. PC Bluetooth 설정에서 ${advertisedDeviceName()}를 먼저 페어링하세요.")
            return
        }
        connectHost(host, reason = "manual_switch")
        refreshControls()
    }

    private fun clearPendingConnection(address: String? = null) {
        if (address == null || pendingConnectionAddress == address) {
            pendingConnectionAddress = null
            pendingConnectionReason = null
            mainHandler.removeCallbacks(connectionTimeoutRunnable)
        }
    }

    private fun clearPendingHostSwitch(address: String? = null) {
        if (address == null || pendingHostSwitchAddress == address) {
            pendingHostSwitchAddress = null
            pendingHostSwitchReason = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectionTimeoutMessage(reason: String, host: BluetoothDevice): String {
        val actionLabel = when (reason) {
            "new_pairing" -> "새 PC 연결"
            else -> "호스트 연결/전환"
        }
        return "$actionLabel 응답이 없습니다: ${host.safeLabel()}. Windows에서만 삭제했다면 Android 페어링 삭제로 휴대폰 쪽 bond도 지운 뒤 새 PC 연결을 다시 시도하세요."
    }

    @SuppressLint("MissingPermission")
    private fun attemptPendingHostSwitch() {
        val address = pendingHostSwitchAddress ?: return
        val reason = pendingHostSwitchReason ?: "manual_switch"
        if (!hasNearbyDevicePermissions() || !appRegistered || hidDevice == null) {
            clearPendingHostSwitch(address)
            reconcileHidForegroundService("host_switch_not_ready")
            return
        }
        startHidForegroundService("host_switch_pending")
        refreshBondedHosts(showStatus = false)
        val host = bondedHosts.firstOrNull { it.address == address }
            ?: bluetoothAdapter?.bondedDevices?.firstOrNull { it.address == address }
            ?: run {
                clearPendingHostSwitch(address)
                reconcileHidForegroundService("host_switch_missing_target")
                setStatus("전환 대상 PC를 찾지 못했습니다. 목록 새로고침 또는 새 PC 연결을 다시 시도하세요.")
                refreshControls()
                return
            }
        mainHandler.postDelayed({
            clearPendingHostSwitch(address)
            logHostDiagnostic("connect_after_switch_$reason", host)
            connectHost(host, reason)
        }, HOST_SWITCH_CONNECT_DELAY_MS)
    }

    @SuppressLint("MissingPermission")
    private fun connectHost(host: BluetoothDevice, reason: String): Boolean {
        val hid = hidDevice
        if (!hasNearbyDevicePermissions() || hid == null || !appRegistered) return false
        val current = connectedHost()
        if (current?.address == host.address) {
            activeHost = current
            connectionState = BluetoothProfile.STATE_CONNECTED
            clearPendingConnection(host.address)
            startHidForegroundService("connect_already_connected")
            setStatus("이미 연결된 호스트입니다: ${host.safeLabel()}")
            return true
        }
        if (current != null && current.address != host.address) {
            startHidForegroundService("host_switch_request")
            pendingHostSwitchAddress = host.address
            pendingHostSwitchReason = reason
            pendingAutoReconnectAddress = null
            clearPendingConnection()
            timedOutConnectionAddress = null
            mainHandler.removeCallbacks(autoReconnectTimeoutRunnable)
            releaseAllMouseButtons("host_switch")
            releaseAllKeyboardKeys("host_switch")
            logHostDiagnostic("defer_switch_$reason", host)
            val accepted = hid.disconnect(current)
            connectionState = BluetoothProfile.STATE_DISCONNECTING
            if (accepted) {
                setStatus("현재 호스트 연결 해제 후 전환합니다: ${host.safeLabel()}")
            } else {
                clearPendingHostSwitch(host.address)
                connectionState = BluetoothProfile.STATE_CONNECTED
                setStatus("현재 호스트 연결 해제 요청이 거절됐습니다. 다시 시도하세요.")
            }
            refreshControls()
            return accepted
        }
        pendingAutoReconnectAddress = if (reason == "auto_reconnect") host.address else null
        clearPendingConnection()
        clearPendingHostSwitch(host.address)
        timedOutConnectionAddress = null
        mainHandler.removeCallbacks(autoReconnectTimeoutRunnable)
        logHostDiagnostic("connect_request_$reason", host)
        startHidForegroundService("connect_request_$reason")
        val accepted = hid.connect(host)
        if (accepted) {
            activeHost = host
            connectionState = BluetoothProfile.STATE_CONNECTING
            if (reason == "auto_reconnect") {
                mainHandler.postDelayed(autoReconnectTimeoutRunnable, AUTO_RECONNECT_TIMEOUT_MS)
            } else {
                pendingConnectionAddress = host.address
                pendingConnectionReason = reason
                mainHandler.postDelayed(connectionTimeoutRunnable, CONNECTION_TIMEOUT_MS)
            }
            val actionLabel = when (reason) {
                "auto_reconnect" -> "최근 PC 자동 재연결 시도 중"
                "new_pairing" -> "새 PC 연결 시도 중"
                else -> "호스트 연결/전환 요청을 보냈습니다"
            }
            setStatus("$actionLabel: ${host.safeLabel()}")
        } else {
            pendingAutoReconnectAddress = null
            clearPendingConnection()
            connectionState = BluetoothProfile.STATE_DISCONNECTED
            val detail = hostDiagnosticSummary(host)
            Log.w(LOG_TAG, "connect request rejected reason=$reason $detail")
            reconcileHidForegroundService("connect_rejected_$reason")
            setStatus("호스트 연결 요청이 거절됐습니다. 로그에서 Windows/HID 상태를 확인하세요.")
        }
        refreshControls()
        return accepted
    }

    @SuppressLint("MissingPermission")
    private fun disconnectActiveHost() {
        if (!hasNearbyDevicePermissions()) return
        val host = connectedHost() ?: activeHost ?: selectedHost()
        if (host == null) {
            setStatus("연결 해제할 호스트가 없습니다.")
            return
        }
        stopContinuousScroll()
        releaseAllMouseButtons("manual_disconnect")
        releaseAllKeyboardKeys("manual_disconnect")
        clearPendingConnection(host.address)
        clearPendingHostSwitch(host.address)
        if (timedOutConnectionAddress == host.address) timedOutConnectionAddress = null
        val accepted = hidDevice?.disconnect(host) == true
        activeHost = null
        dragMode = false
        connectionState = BluetoothProfile.STATE_DISCONNECTED
        stopHidForegroundService("manual_disconnect")
        setStatus(if (accepted) "호스트 연결 해제를 요청했습니다." else "호스트 연결 해제 요청이 거절됐습니다.")
        refreshControls()
    }

    @SuppressLint("MissingPermission")
    private fun removeSelectedHostBond() {
        if (!hasNearbyDevicePermissions()) {
            requestNearbyDevicePermissions()
            return
        }
        val host = selectedHost()
        if (host == null) {
            setStatus("삭제할 PC 후보가 없습니다.")
            return
        }
        stopContinuousScroll()
        if (connectedHost()?.address == host.address) {
            releaseAllMouseButtons("remove_bond")
            releaseAllKeyboardKeys("remove_bond")
            hidDevice?.disconnect(host)
            stopHidForegroundService("remove_bond")
        }
        pendingAutoReconnectAddress = null
        clearPendingConnection(host.address)
        clearPendingHostSwitch(host.address)
        if (timedOutConnectionAddress == host.address) timedOutConnectionAddress = null
        forgetHostRecord(host.address)
        logHostDiagnostic("remove_bond_request", host)
        val accepted = host.removeBondCompat()
        if (accepted) {
            if (activeHost?.address == host.address) activeHost = null
            connectionState = BluetoothProfile.STATE_DISCONNECTED
            setStatus("Android 페어링 삭제를 요청했습니다: ${host.safeLabel()}. Windows에서도 PhonePad를 삭제한 뒤 새 PC 연결로 다시 페어링하세요.")
            mainHandler.postDelayed({
                refreshBondedHosts(showStatus = false)
                refreshControls()
            }, BOND_REFRESH_DELAY_MS)
        } else {
            setStatus("Android 페어링 삭제 요청이 거절됐습니다. Android Bluetooth 설정에서 ${host.safeLabel()}를 삭제한 뒤 새 PC 연결을 다시 시도하세요.")
        }
        refreshBondedHosts(showStatus = false)
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
                doubleTapDragActive = shouldStartDoubleTapDrag(event)
                if (doubleTapDragActive) {
                    lastTapUpTime = 0L
                    sendMouseReport(0, 0, LEFT_BUTTON, 0, 0)
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    setStatus("더블 탭 Drag 중입니다. 손을 떼면 해제됩니다.")
                }
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
                if (doubleTapDragActive) {
                    doubleTapDragActive = false
                    releaseAllMouseButtons("double_tap_drag_up")
                } else if (!dragMode && !usingScrollGesture && gestureDistance < dp(8)) {
                    clickMouse(LEFT_BUTTON)
                    lastTapUpTime = event.eventTime
                    lastTapX = event.x
                    lastTapY = event.y
                } else if (dragMode) {
                    sendMouseReport(0, 0, LEFT_BUTTON, 0, 0)
                }
                usingScrollGesture = false
            }
            MotionEvent.ACTION_CANCEL -> {
                view.parent?.requestDisallowInterceptTouchEvent(false)
                if (doubleTapDragActive) {
                    doubleTapDragActive = false
                    releaseAllMouseButtons("double_tap_drag_cancel")
                } else if (!dragMode) {
                    releaseAllMouseButtons("touch_cancel")
                }
            }
        }
        return true
    }

    private fun readyHostForInput(showStatus: Boolean): BluetoothDevice? {
        val host = connectedHost()
        val message = when {
            !hasNearbyDevicePermissions() -> "Bluetooth 권한이 필요합니다."
            !appRegistered -> "HID 세션을 자동 준비 중입니다."
            host == null -> "호스트 연결이 없습니다. 연결 패널에서 페어링된 PC를 선택해 연결하세요."
            else -> null
        }
        if (message != null && showStatus) {
            setStatus(message)
            refreshControls()
        }
        return host
    }

    private fun showConnectionPanel() {
        connectionDrawer.visibility = View.VISIBLE
        registerDrawerBackCallback()
    }

    private fun hideConnectionPanel() {
        connectionDrawer.visibility = View.GONE
        unregisterDrawerBackCallback()
    }

    private fun isConnectionPanelVisible(): Boolean {
        return ::connectionDrawer.isInitialized && connectionDrawer.visibility == View.VISIBLE
    }

    private fun registerDrawerBackCallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || drawerBackCallbackRegistered) return
        val callback = OnBackInvokedCallback { hideConnectionPanel() }
        drawerBackCallback = callback
        onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            callback,
        )
        drawerBackCallbackRegistered = true
    }

    private fun unregisterDrawerBackCallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !drawerBackCallbackRegistered) return
        (drawerBackCallback as? OnBackInvokedCallback)?.let {
            onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it)
        }
        drawerBackCallback = null
        drawerBackCallbackRegistered = false
    }

    private fun connectionChipText(
        host: BluetoothDevice?,
        registered: Boolean,
        hasPermissions: Boolean,
        adapterEnabled: Boolean,
    ): String {
        return when {
            !hasPermissions -> "권한 필요"
            !adapterEnabled -> "Bluetooth 꺼짐"
            host != null -> "연결됨: ${host.safeLabel()}"
            registered -> "연결 대기"
            else -> "HID 준비 중"
        }
    }

    private fun hostForInputSettings(): BluetoothDevice? {
        return connectedHost() ?: selectedHost() ?: activeHost
    }

    private fun savedHostOsPreset(host: BluetoothDevice?): HostOsPreset {
        val address = host?.address ?: return HostOsPreset.AUTO
        return HostOsPreset.fromStorageValue(
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(hostOsPresetKey(address), null),
        )
    }

    private fun resolvedHostOsPreset(host: BluetoothDevice?): HostOsPreset {
        return HostInputPolicy.resolvePreset(savedHostOsPreset(host), host.safeLabel())
    }

    private fun hostOsControlText(host: BluetoothDevice?): String {
        val saved = savedHostOsPreset(host)
        val resolved = resolvedHostOsPreset(host)
        return if (saved == HostOsPreset.AUTO && resolved != HostOsPreset.AUTO) {
            "호스트 OS: ${resolved.shortLabel} 자동"
        } else {
            "호스트 OS: ${saved.label}"
        }
    }

    private fun cycleHostOsPreset() {
        val host = hostForInputSettings()
        if (host == null) {
            setStatus("호스트 OS를 저장할 PC 후보가 없습니다. 먼저 호스트를 연결하거나 선택하세요.")
            showConnectionPanel()
            return
        }
        val next = savedHostOsPreset(host).nextManualPreset()
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(hostOsPresetKey(host.address), next.storageValue)
            .apply()
        setStatus("${host.safeLabel()} 한영 전환 설정: ${next.label}")
        refreshControls()
    }

    private fun sendLanguageToggle() {
        val host = readyHostForInput(showStatus = true) ?: return
        val preset = resolvedHostOsPreset(host)
        val stroke = HostInputPolicy.languageToggleStroke(preset)
        if (stroke == null) {
            setStatus("호스트 OS를 먼저 선택하세요. Mac 또는 Windows 한영 전환 방식을 고를 수 있습니다.")
            showConnectionPanel()
            return
        }
        val ok = sendKeyboardStroke(stroke, "language_toggle preset=${preset.storageValue}")
        if (ok) {
            trackpadSurface.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            setStatus(languageToggleSentStatus(preset))
        } else {
            setStatus("한영 전환 키 전송에 실패했습니다. 호스트 연결을 확인하세요.")
        }
        refreshControls()
    }

    private fun languageToggleSentStatus(preset: HostOsPreset): String {
        return if (preset == HostOsPreset.MAC) {
            "Mac 한영 전환 키를 보냈습니다. 반응이 없으면 Mac Bluetooth에서 PhonePad를 삭제한 뒤 새 PC 연결로 다시 페어링하세요."
        } else {
            "${preset.shortLabel} 한영 전환 키를 보냈습니다."
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendKeyboardStroke(stroke: KeyboardStroke, summary: String): Boolean {
        if (!hasNearbyDevicePermissions()) return recordReport(false, "권한 없음")
        val device = connectedHost()
        val hid = hidDevice
        if (hid == null || device == null || !appRegistered) return recordReport(false, "호스트 없음")
        val needsModifierLead = stroke.modifier != 0 && stroke.keyUsage != 0
        val ok = sendKeyboardReport(
            device,
            hid,
            stroke.modifier,
            if (needsModifierLead) 0 else stroke.keyUsage,
            if (needsModifierLead) "$summary modifier_down" else summary,
        )
        if (ok) {
            if (needsModifierLead) {
                mainHandler.postDelayed({
                    val chordOk = sendKeyboardReport(device, hid, stroke.modifier, stroke.keyUsage, "$summary key_down")
                    if (chordOk) {
                        mainHandler.postDelayed({ releaseAllKeyboardKeys("keyboard_key_up") }, KEY_RELEASE_DELAY_MS)
                    } else {
                        releaseAllKeyboardKeys("keyboard_key_down_failed")
                    }
                }, KEY_CHORD_STAGE_DELAY_MS)
            } else {
                mainHandler.postDelayed({ releaseAllKeyboardKeys("keyboard_key_up") }, KEY_RELEASE_DELAY_MS)
            }
        }
        return ok
    }

    @SuppressLint("MissingPermission")
    private fun sendKeyboardReport(
        device: BluetoothDevice,
        hid: BluetoothHidDevice,
        modifier: Int,
        keyUsage: Int,
        summary: String,
    ): Boolean {
        if (!hasNearbyDevicePermissions() || !appRegistered || connectedHost()?.address != device.address) {
            return recordReport(false, "호스트 없음")
        }
        val payload = byteArrayOf(
            modifier.coerceIn(0, 255).toByte(),
            0,
            keyUsage.coerceIn(0, 255).toByte(),
            0,
            0,
            0,
            0,
            0,
        )
        val ok = hid.sendReport(device, KEYBOARD_REPORT_ID, payload)
        val reportSummary = "$summary modifier=$modifier key=$keyUsage"
        if (!ok) Log.w(LOG_TAG, "send keyboard report failed host=${device.address} $reportSummary")
        return recordReport(ok, reportSummary)
    }

    private fun toggleDragMode() {
        if (readyHostForInput(showStatus = true) == null) return
        if (dragMode) {
            val ok = releaseAllMouseButtons("drag_off")
            dragMode = false
            setStatus(if (ok) "Drag Mode를 껐습니다." else "버튼 해제 보고서를 보낼 호스트가 없습니다.")
        } else {
            doubleTapDragActive = false
            val ok = sendMouseReport(0, 0, LEFT_BUTTON, 0, 0)
            dragMode = ok
            if (ok) trackpadSurface.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            setStatus(if (ok) "Drag Mode를 켰습니다." else "Drag Mode 시작에 실패했습니다. 호스트 연결을 확인하세요.")
        }
        refreshControls()
    }

    private fun toggleDoubleTapDrag() {
        doubleTapDragEnabled = !doubleTapDragEnabled
        if (!doubleTapDragEnabled && doubleTapDragActive) {
            doubleTapDragActive = false
            releaseAllMouseButtons("double_tap_drag_disabled")
        }
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DOUBLE_TAP_DRAG_ENABLED, doubleTapDragEnabled)
            .apply()
        setStatus(if (doubleTapDragEnabled) "더블 탭 Drag를 켰습니다." else "더블 탭 Drag를 껐습니다.")
        refreshControls()
    }

    private fun shouldStartDoubleTapDrag(event: MotionEvent): Boolean {
        if (!doubleTapDragEnabled || dragMode || usingScrollGesture) return false
        if (lastTapUpTime == 0L) return false
        val withinTime = event.eventTime - lastTapUpTime <= DOUBLE_TAP_DRAG_TIMEOUT_MS
        val withinDistance = abs(event.x - lastTapX) + abs(event.y - lastTapY) <= dp(DOUBLE_TAP_DRAG_SLOP_DP)
        return withinTime && withinDistance
    }

    private fun cycleScrollSpeed() {
        scrollSpeedPreset = when (scrollSpeedPreset) {
            SCROLL_SPEED_SLOW -> SCROLL_SPEED_DEFAULT
            SCROLL_SPEED_DEFAULT -> SCROLL_SPEED_FAST
            else -> SCROLL_SPEED_SLOW
        }
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putInt(KEY_SCROLL_SPEED_PRESET, scrollSpeedPreset)
            .apply()
        setStatus("스크롤 버튼 속도: ${scrollSpeedLabel()}")
        refreshControls()
    }

    private fun sendSingleScroll(wheel: Int) {
        if (readyHostForInput(showStatus = true) == null) return
        val ok = sendMouseReport(0, 0, currentButtons(), wheel, 0)
        if (ok) {
            trackpadSurface.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } else {
            setStatus("스크롤 보고서 전송에 실패했습니다. 호스트 연결을 확인하세요.")
            refreshControls()
        }
    }

    private fun startContinuousScroll(button: Button, initialWheel: Int, repeatWheel: Int): Boolean {
        if (readyHostForInput(showStatus = true) == null) return false
        stopContinuousScroll()
        activeScrollButton = button
        activeScrollWheel = repeatWheel
        val ok = sendMouseReport(0, 0, currentButtons(), initialWheel, 0)
        if (!ok) {
            stopContinuousScroll()
            setStatus("스크롤 보고서 전송에 실패했습니다. 호스트 연결을 확인하세요.")
            refreshControls()
            return false
        }
        button.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        button.postDelayed(scrollRepeatRunnable, scrollInitialDelayMs())
        return true
    }

    private fun stopContinuousScroll() {
        activeScrollButton?.removeCallbacks(scrollRepeatRunnable)
        activeScrollButton = null
        activeScrollWheel = 0
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

    private fun currentButtons(): Int = if (dragMode || doubleTapDragActive) LEFT_BUTTON else 0

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
        if (!ok) Log.w(LOG_TAG, "send mouse report failed host=${device.address} $summary")
        return recordReport(ok, summary)
    }

    @SuppressLint("MissingPermission")
    private fun releaseAllMouseButtons(reason: String): Boolean {
        val device = connectedHost()
        val hid = hidDevice ?: return false
        if (!hasNearbyDevicePermissions() || device == null || !appRegistered) return false
        val ok = hid.sendReport(device, MOUSE_REPORT_ID, byteArrayOf(0, 0, 0, 0, 0))
        if (!ok) Log.w(LOG_TAG, "release mouse buttons failed host=${device.address} reason=$reason")
        recordReport(ok, "release_all reason=$reason")
        if (!ok && reason != "activity_destroyed") {
            setStatus("버튼 해제 보고서 전송에 실패했습니다: $reason")
        }
        return ok
    }

    @SuppressLint("MissingPermission")
    private fun releaseAllKeyboardKeys(reason: String): Boolean {
        val device = connectedHost()
        val hid = hidDevice ?: return false
        if (!hasNearbyDevicePermissions() || device == null || !appRegistered) return false
        val ok = hid.sendReport(device, KEYBOARD_REPORT_ID, byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0))
        if (!ok) Log.w(LOG_TAG, "release keyboard keys failed host=${device.address} reason=$reason")
        recordReport(ok, "release_keyboard reason=$reason")
        if (!ok && reason != "activity_destroyed") {
            setStatus("키보드 해제 보고서 전송에 실패했습니다: $reason")
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
            current.rememberSuccessfulHost()
            return current
        }
        val selectedAddress = selectedHost()?.address
        val lastAddress = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_LAST_HOST_ADDRESS, null)
        val connectedDevices = hid.connectedDevices
        val connected = connectedDevices.firstOrNull { it.address == selectedAddress }
            ?: connectedDevices.firstOrNull { it.address == lastAddress }
            ?: connectedDevices.firstOrNull()
        if (connected != null) {
            activeHost = connected
            connectionState = BluetoothProfile.STATE_CONNECTED
            connected.rememberSuccessfulHost()
        }
        return connected
    }

    private fun selectedHost(): BluetoothDevice? = bondedHosts.getOrNull(selectedHostIndex)

    private fun buildHostText(host: BluetoothDevice?): String {
        val selected = selectedHost()
        return buildString {
            appendLine("활성 호스트: ${host.safeLabel()}")
            appendLine("연결 상태: ${connectionState.toConnectionLabel()}")
            appendLine("한영 전환: ${resolvedHostOsPreset(selected ?: host).label}")
            appendLine("최근 PC: ${lastSuccessfulHostName() ?: "없음"}")
            appendLine("전환 대상: ${selected.safeLabel()}")
            append("PC 후보 목록: ${bondedHosts.size}개")
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
            appendLine("2. 기존 PC는 자동 재연결 대기")
            appendLine("3. 실패하면 전환 대상 선택 후 연결")
            appendLine("4. 새 PC는 새 PC 연결 누르기")
            appendLine("5. Windows만 삭제했다면 Android 페어링 삭제")
            appendLine("6. PC Bluetooth에서 ${advertisedDeviceName()} 검색")
            appendLine("7. 페어링 후 호스트 연결/전환 누르기")
            appendLine("8. 한영이 반응 없으면 PC Bluetooth에서 PhonePad 삭제 후 다시 페어링")
            append("9. 오른쪽 터치패드 사용 · 스크롤 버튼 길게 누르기")
        }
    }

    private fun loadInputSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        doubleTapDragEnabled = prefs.getBoolean(KEY_DOUBLE_TAP_DRAG_ENABLED, false)
        scrollSpeedPreset = prefs.getInt(KEY_SCROLL_SPEED_PRESET, SCROLL_SPEED_DEFAULT)
            .coerceIn(SCROLL_SPEED_SLOW, SCROLL_SPEED_FAST)
    }

    private fun scrollSpeedLabel(): String {
        return when (scrollSpeedPreset) {
            SCROLL_SPEED_SLOW -> "느림"
            SCROLL_SPEED_FAST -> "빠름"
            else -> "기본"
        }
    }

    private fun scrollTapStep(): Int {
        return when (scrollSpeedPreset) {
            SCROLL_SPEED_FAST -> 3
            else -> 2
        }
    }

    private fun scrollRepeatStep(): Int {
        return when (scrollSpeedPreset) {
            SCROLL_SPEED_FAST -> 2
            else -> 1
        }
    }

    private fun scrollInitialDelayMs(): Long {
        return when (scrollSpeedPreset) {
            SCROLL_SPEED_SLOW -> 300L
            SCROLL_SPEED_FAST -> 210L
            else -> 260L
        }
    }

    private fun scrollRepeatIntervalMs(): Long {
        return when (scrollSpeedPreset) {
            SCROLL_SPEED_SLOW -> 175L
            SCROLL_SPEED_FAST -> 90L
            else -> 135L
        }
    }

    private fun knownHostAddresses(): Set<String> {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getStringSet(KEY_KNOWN_HOSTS, emptySet())
            ?.toSet()
            ?: emptySet()
    }

    private fun candidateHostAddresses(): Set<String> {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getStringSet(KEY_CANDIDATE_HOSTS, emptySet())
            ?.toSet()
            ?: emptySet()
    }

    private fun BluetoothDevice.rememberSuccessfulHost() {
        val knownHosts = knownHostAddresses()
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_KNOWN_HOSTS, knownHosts + address)
            .putString(KEY_LAST_HOST_ADDRESS, address)
            .putString(KEY_LAST_HOST_NAME, safeLabel())
            .putLong(KEY_LAST_HOST_CONNECTED_AT, System.currentTimeMillis())
            .apply()
    }

    private fun rememberCandidateHost(device: BluetoothDevice) {
        val candidates = candidateHostAddresses()
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_CANDIDATE_HOSTS, candidates + device.address)
            .apply()
    }

    private fun forgetHostRecord(address: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()
            .putStringSet(KEY_KNOWN_HOSTS, knownHostAddresses() - address)
            .putStringSet(KEY_CANDIDATE_HOSTS, candidateHostAddresses() - address)
            .remove(hostOsPresetKey(address))
        if (prefs.getString(KEY_LAST_HOST_ADDRESS, null) == address) {
            editor
                .remove(KEY_LAST_HOST_ADDRESS)
                .remove(KEY_LAST_HOST_NAME)
                .remove(KEY_LAST_HOST_CONNECTED_AT)
        }
        editor.apply()
    }

    private fun hostOsPresetKey(address: String): String = "$KEY_HOST_OS_PRESET_PREFIX$address"

    @SuppressLint("MissingPermission")
    private fun allBondedAddresses(): Set<String> {
        if (!hasNearbyDevicePermissions()) return emptySet()
        return bluetoothAdapter?.bondedDevices?.mapTo(mutableSetOf()) { it.address } ?: emptySet()
    }

    @SuppressLint("MissingPermission")
    private fun autoReconnectCandidate(): BluetoothDevice? {
        if (!hasNearbyDevicePermissions()) return null
        val bonded = bluetoothAdapter?.bondedDevices.orEmpty()
        val lastAddress = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_LAST_HOST_ADDRESS, null)
        val lastHost = bonded.firstOrNull { it.address == lastAddress }
        if (lastHost != null) return lastHost
        return bondedHosts.singleOrNull()
    }

    private fun lastSuccessfulHostName(): String? {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getString(KEY_LAST_HOST_NAME, null)
            ?.takeIf { it.isNotBlank() }
    }

    @SuppressLint("MissingPermission")
    private fun logHostDiagnostic(event: String, device: BluetoothDevice?) {
        Log.d(LOG_TAG, "host_diag event=$event ${hostDiagnosticSummary(device)}")
    }

    @SuppressLint("MissingPermission")
    private fun hostDiagnosticSummary(device: BluetoothDevice?): String {
        if (device == null) return "host=null"
        val hidState = if (hasNearbyDevicePermissions()) {
            hidDevice?.getConnectionState(device)?.toConnectionLabel() ?: "profile_null"
        } else {
            "permission_missing"
        }
        return "name=${device.safeLabel()} address=${device.address} bond=${device.bondState} major=${device.bluetoothClass?.majorDeviceClass} hidState=$hidState selected=${selectedHost()?.address == device.address} known=${device.address in knownHostAddresses()} candidate=${device.address in candidateHostAddresses()} last=${getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_LAST_HOST_ADDRESS, null) == device.address}"
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.isLikelyComputerHost(): Boolean {
        if (!hasNearbyDevicePermissions()) return false
        return bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.COMPUTER
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.isLikelyNamedHost(): Boolean {
        if (!hasNearbyDevicePermissions()) return false
        val normalized = name.orEmpty().lowercase(Locale.US)
        return normalized.contains("mac") ||
            normalized.contains("windows") ||
            normalized.contains("win11") ||
            normalized.contains("win10") ||
            normalized.contains(" pc") ||
            normalized.endsWith("pc")
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

    private fun BluetoothDevice.removeBondCompat(): Boolean {
        return runCatching {
            javaClass.getMethod("removeBond").invoke(this) as? Boolean == true
        }.onFailure {
            Log.w(LOG_TAG, "remove bond failed address=$address", it)
        }.getOrDefault(false)
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

    private fun MotionEvent.isOutside(view: View): Boolean {
        return x < 0f || y < 0f || x > view.width || y > view.height
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

    private fun statusChip(): TextView {
        return TextView(this).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            maxLines = 2
            minHeight = dp(40)
            setPadding(dp(10), 0, dp(10), 0)
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

    @SuppressLint("ClickableViewAccessibility")
    private fun scrollButton(text: String, direction: Int): Button {
        var touchScrollConsumed = false
        val button = actionButton(text) {
            if (touchScrollConsumed) {
                touchScrollConsumed = false
            } else {
                sendSingleScroll(direction * scrollTapStep())
            }
        }
        button.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.isPressed = true
                    touchScrollConsumed = true
                    startContinuousScroll(
                        button,
                        direction * scrollTapStep(),
                        direction * scrollRepeatStep(),
                    )
                    true
                }
                MotionEvent.ACTION_UP -> {
                    stopContinuousScroll()
                    view.isPressed = false
                    view.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    stopContinuousScroll()
                    view.isPressed = false
                    touchScrollConsumed = false
                    true
                }
                else -> true
            }
        }
        return button
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

    private fun fixedButtonParams(widthDp: Int, left: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(dp(widthDp), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            if (left > 0) leftMargin = dp(left)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val NEARBY_DEVICES_REQUEST = 4028
        private const val MOUSE_REPORT_ID = 1
        private const val KEYBOARD_REPORT_ID = 2
        private const val LEFT_BUTTON = 1
        private const val RIGHT_BUTTON = 2
        private const val POINTER_SCALE = 1.55f
        private const val DISCOVERABLE_SECONDS = 300
        private const val CLICK_RELEASE_DELAY_MS = 35L
        private const val KEY_CHORD_STAGE_DELAY_MS = 30L
        private const val KEY_RELEASE_DELAY_MS = 45L
        private const val SCROLL_UP = 1
        private const val SCROLL_DOWN = -1
        private const val SCROLL_SPEED_SLOW = 0
        private const val SCROLL_SPEED_DEFAULT = 1
        private const val SCROLL_SPEED_FAST = 2
        private const val DOUBLE_TAP_DRAG_TIMEOUT_MS = 320L
        private const val DOUBLE_TAP_DRAG_SLOP_DP = 18
        private const val SESSION_STOP_GRACE_MS = 1200L
        private const val EXTERNAL_FLOW_STOP_GRACE_MS = 330000L
        private const val NEW_PAIRING_SCAN_DELAY_MS = 900L
        private const val HOST_SWITCH_CONNECT_DELAY_MS = 300L
        private const val BOND_REFRESH_DELAY_MS = 900L
        private const val AUTO_RECONNECT_TIMEOUT_MS = 4500L
        private const val CONNECTION_TIMEOUT_MS = 12000L
        private const val MAX_DEVICE_ALIAS_LENGTH = 32
        private const val PREFS_NAME = "phonepad"
        private const val KEY_KNOWN_HOSTS = "known_hosts"
        private const val KEY_CANDIDATE_HOSTS = "candidate_hosts"
        private const val KEY_LAST_HOST_ADDRESS = "last_host_address"
        private const val KEY_LAST_HOST_NAME = "last_host_name"
        private const val KEY_LAST_HOST_CONNECTED_AT = "last_host_connected_at"
        private const val KEY_DOUBLE_TAP_DRAG_ENABLED = "double_tap_drag_enabled"
        private const val KEY_SCROLL_SPEED_PRESET = "scroll_speed_preset"
        private const val KEY_HOST_OS_PRESET_PREFIX = "host_os_preset_"
        private const val LOG_TAG = "PhonePad"
        private const val ADVERTISED_DEVICE_PREFIX = "PhonePad"

        private val COLOR_BACKGROUND = Color.rgb(10, 12, 15)
        private val COLOR_DRAWER_SCRIM = Color.argb(184, 0, 0, 0)
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
        private val COLOR_INPUT_ACTIVE = Color.rgb(58, 106, 92)
        private val COLOR_INPUT_STROKE = Color.rgb(132, 207, 178)

        private val MOUSE_KEYBOARD_DESCRIPTOR = intArrayOf(
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
            0x05, 0x01,
            0x09, 0x06,
            0xA1, 0x01,
            0x85, KEYBOARD_REPORT_ID,
            0x05, 0x07,
            0x19, 0xE0,
            0x29, 0xE7,
            0x15, 0x00,
            0x25, 0x01,
            0x75, 0x01,
            0x95, 0x08,
            0x81, 0x02,
            0x95, 0x01,
            0x75, 0x08,
            0x81, 0x03,
            0x95, 0x06,
            0x75, 0x08,
            0x15, 0x00,
            0x26, 0xFF, 0x00,
            0x05, 0x07,
            0x19, 0x00,
            0x2A, 0xFF, 0x00,
            0x81, 0x00,
            0xC0,
        ).map { it.toByte() }.toByteArray()
    }
}
