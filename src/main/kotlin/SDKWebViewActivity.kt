package com.zipup.openapi.webview.sdk

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.zipup.openapi.webview.sdk.R
import android.graphics.Bitmap

class SDKWebViewActivity : AppCompatActivity() {

    private var webView: WebView? = null
    private var loadingOverlay: View? = null
    private var userKey: String = ""
    private var userPhone: String = ""
    private var proxyUrl: String = ""
    private var webViewManager: MyWebViewManager? = null
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val callback = fileChooserCallback
        fileChooserCallback = null
        if (uri != null) callback?.onReceiveValue(arrayOf(uri))
        else callback?.onReceiveValue(null)
    }

    companion object {
        @Volatile
        private var currentInstance: SDKWebViewActivity? = null
        
        fun getCurrentInstance(): SDKWebViewActivity? = currentInstance
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        currentInstance = this
        
        userKey = intent.getStringExtra("USER_KEY") ?: ""
        userPhone = intent.getStringExtra("USER_PHONE") ?: ""
        proxyUrl = intent.getStringExtra("PROXY_URL") ?: ""
        
        webViewManager = MyWebViewManager.getCurrentInstance()
        
        setContentView(R.layout.sdk_webview)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        webViewManager?.setOnReadyCallback { hideLoading() }

        webView = findViewById<WebView>(R.id.webView)?.apply {
            SDKConfig.configureWebView(this)
            
            webViewManager?.let { manager ->
                WebViewBridge.setManager(manager)
                addJavascriptInterface(WebViewBridge(), "Zipup")
            }
            
            webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    view?.let { executeInitClient(it) }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: android.webkit.WebChromeClient.FileChooserParams?
                ): Boolean {
                    fileChooserCallback?.onReceiveValue(null)
                    fileChooserCallback = filePathCallback
                    val mime = fileChooserParams?.acceptTypes?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "*/*"
                    fileChooserLauncher.launch(mime)
                    return true
                }
            }
        }

        val urlToLoad = webViewManager?.storedTestUrl?.takeIf { it.isNotBlank() } ?: SDKConfig.target
        webView?.loadUrl(urlToLoad)
    }
    
    private fun executeInitClient(webView: WebView) {
        val rootView = webView.rootView ?: window.decorView
        val insets = ViewCompat.getRootWindowInsets(rootView)?.getInsets(WindowInsetsCompat.Type.systemBars())
        val safeTop = insets?.top ?: 0
        val safeBottom = insets?.bottom ?: 0
        val safeLeft = insets?.left ?: 0
        val safeRight = insets?.right ?: 0

        webViewManager?.updateSafeArea(safeTop, safeBottom, safeLeft, safeRight)

        val deviceWidth = windowManager.defaultDisplay.width
        val deviceHeight = windowManager.defaultDisplay.height

        val density = resources.displayMetrics.density
        val webViewWidth = (deviceWidth / density).toInt()
        val webViewHeight = (deviceHeight / density).toInt()

        val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)

        val appVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }

        // Window._webViewConfig에 들어갈 JavaScript 객체 리터럴 문자열
        val webViewConfigJs = """
            {
                user: {
                    userKey: ${escapeJsString(userKey)},
                    userPhone: ${escapeJsString(userPhone)},
                },
                proxyUrl: ${escapeJsString(proxyUrl)},
                safeArea: { top: $safeTop, bottom: $safeBottom, left: $safeLeft, right: $safeRight },
                screen: {
                    device: { width: ${deviceWidth}, height: ${deviceHeight} },
                    webView: { width: ${webViewWidth}, height: ${webViewHeight} }
                },
                platform: "android",
                deviceVersion: ${escapeJsString(Build.VERSION.RELEASE ?: "")},
                sdkVersion: ${Build.VERSION.SDK_INT},
                appVersion: ${escapeJsString(appVersion)},
                deviceId: ${escapeJsString(deviceId)}
            }
        """.trimIndent()

        val jsCode = """
            (function() {
                try {
                    localStorage.setItem('userKey', ${escapeJsString(userKey)});
                    localStorage.setItem('userPhone', ${escapeJsString(userPhone)});
                    localStorage.setItem('proxyUrl', ${escapeJsString(proxyUrl)});
                    window._webViewConfig = $webViewConfigJs;
                    console.log('SDK init data saved to localStorage');
                } catch (e) {
                    console.error('Error saving to localStorage:', e);
                }
            })();
        """.trimIndent()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(jsCode, null)
        } else {
            @Suppress("DEPRECATION")
            webView.loadUrl("javascript:$jsCode")
        }
    }
    
    private fun hideLoading() {
        loadingOverlay?.visibility = View.GONE
    }

    private fun escapeJsString(value: String): String {
        return "\"" + value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") + "\""
    }

    override fun onBackPressed() {
        webView?.let {
            if (it.canGoBack()) {
                it.goBack()
            } else {
                super.onBackPressed()
            }
        } ?: super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        currentInstance = null
        webViewManager?.setOnReadyCallback(null)
        webView?.removeJavascriptInterface("Zipup")
        webView?.destroy()
        webView = null
        loadingOverlay = null
        webViewManager = null
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }
}

