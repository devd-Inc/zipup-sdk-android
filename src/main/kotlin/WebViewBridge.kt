package com.zipup.openapi.webview.sdk

import android.util.Log
import android.webkit.JavascriptInterface

class WebViewBridge {
    
    companion object {
        private const val TAG = "WebViewBridge"
        private var managerInstance: MyWebViewManager? = null
        
   
        fun setManager(manager: MyWebViewManager) {
            managerInstance = manager
        }

        fun getManager(): MyWebViewManager? = managerInstance
    }

    @JavascriptInterface
    fun postMessage(event: String, data: String?) {
        try {
            Log.d(TAG, "Received message - event: $event, data: $data")
            
            managerInstance?.sendEvent(event, data ?: "null") ?: run {
                Log.w(TAG, "MyWebViewManager instance is not set. Event will be ignored.")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing postMessage", e)
            managerInstance?.sendEvent("onError", "{\"error\":\"${e.message ?: "Unknown error"}\",\"originalEvent\":\"$event\"}") ?: run {
                Log.w(TAG, "MyWebViewManager instance is not set. Error event will be ignored.")
            }
        }
    }
    
    @JavascriptInterface
    fun ping(): String {
        Log.d(TAG, "Ping received from JavaScript")
        return "pong";
    }
}