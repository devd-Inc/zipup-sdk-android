package com.zipup.openapi.webview.sdk

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import java.nio.charset.StandardCharsets

data class SDKInitData(
    val userKey: String,
    val userPhone: String,
    val proxyUrl: String,
)

/** 웹뷰 세이프티 에어리어 (px). 내부 저장 및 getConfig()에서 반환. */
data class SafeArea(
    val top: Int,
    val bottom: Int,
    val left: Int,
    val right: Int,
)

/** getConfig()로 반환하는 설정 객체. 플러그인 사용 앱에서 사용. */
data class SDKClientConfig(
    val userKey: String,
    val userPhone: String,
    val proxyUrl: String,
    val safeArea: SafeArea?,
)

interface SDKEventListener {
    fun onEvent(event: String, data: String)
}

object SDKConfig {
    private var encoded: String = "aHR0cHM6Ly9zZGsu" + "ZGV2ZC5jby5rcg=="

    var target: String
        get() = String(Base64.decode(encoded, Base64.NO_WRAP), StandardCharsets.UTF_8)
        set(value) {
            encoded = Base64.encodeToString(value.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        }

    fun configureWebView(webView: WebView) {
        webView.settings.javaScriptEnabled = true // 자바스크립트 허용
        webView.settings.domStorageEnabled = true // DOM Storage 허용
        webView.settings.setSupportZoom(true) // 줌 지원
        webView.settings.builtInZoomControls = true // 줌 컨트롤 표시
        webView.settings.displayZoomControls = false // 줌 컨트롤 UI 숨김
    }
}

/**
 * SDK 웹뷰 매니저
 * 액티비티 방식으로 SDK 내부 웹뷰를 실행합니다.
 */
class MyWebViewManager(private val context: Context) {
    
    private var webView: WebView? = null
    private var isInitialized = false
    private var userKey: String = ""
    private var userPhone: String = ""
    private var proxyUrl: String = ""
    private var safeArea: SafeArea? = null

    private var eventListener: SDKEventListener? = null
    private var onReadyCallback: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        @Volatile
        private var currentInstance: MyWebViewManager? = null
        
        fun getCurrentInstance(): MyWebViewManager? = currentInstance
    }
    
    init {
        currentInstance = this
    }

    fun setWebView(webView: WebView) {
        this.webView = webView
    }

    /**
     * SDK 내부 액티비티를 실행하여 웹뷰를 표시합니다.
     * 사용자는 이 메서드를 호출하기만 하면 SDK 자체에서 웹뷰 액티비티가 실행됩니다.
     */
    fun launchWebViewActivity() {
        if (!isInitialized) {
            throw IllegalStateException("SDK is not initialized. Call init() first.")
        }
        
        val intent = android.content.Intent(context, SDKWebViewActivity::class.java).apply {
            putExtra("USER_KEY", userKey)
            putExtra("USER_PHONE", userPhone)
            putExtra("PROXY_URL", proxyUrl)
        }
        context.startActivity(intent)
    }

    fun init(data: SDKInitData): SDKInitData {
        this.userKey = data.userKey
        this.userPhone = data.userPhone
        this.proxyUrl = data.proxyUrl
        this.isInitialized = true

        return data
    }

    /** 세이프티 에어리어 갱신 (웹뷰 로드 시 액티비티에서 호출). */
    fun updateSafeArea(top: Int, bottom: Int, left: Int, right: Int) {
        safeArea = SafeArea(top = top, bottom = bottom, left = left, right = right)
    }

    /**
     * 현재 SDK 설정을 반환합니다. 플러그인을 사용하는 앱에서 호출합니다.
     * safeArea는 웹뷰 액티비티가 한 번이라도 로드된 이후에만 값이 있습니다.
     */
    fun getConfig(): SDKClientConfig {
        return SDKClientConfig(
            userKey = userKey,
            userPhone = userPhone,
            proxyUrl = proxyUrl,
            safeArea = safeArea,
        )
    }

    fun setEventListener(eventListener: SDKEventListener) {
        this.eventListener = eventListener
    }

    /** ready 이벤트 수신 시 호출될 콜백 (로딩 UI 숨김 등). 메인 스레드에서 호출됩니다. */
    fun setOnReadyCallback(callback: (() -> Unit)?) {
        this.onReadyCallback = callback
    }

    fun sendEvent(event: String, data: String) {
        if (event == "ready") {
            mainHandler.post { onReadyCallback?.invoke() }
        }
        eventListener?.onEvent(event, data)
    }

    fun close() {
        SDKWebViewActivity.getCurrentInstance()?.finish()
    }
  
}
