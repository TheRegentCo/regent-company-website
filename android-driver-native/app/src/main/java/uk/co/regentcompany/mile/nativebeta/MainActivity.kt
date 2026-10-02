package uk.co.regentcompany.mile.nativebeta

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader
import com.stripe.android.identity.IdentityVerificationSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), LocationListener {
    companion object {
        private const val EDGE = "https://anmrzqcvzxhjvmitkczc.supabase.co/functions/v1/regent-comply1"
        private const val COMPLIANCE_EDGE = "https://anmrzqcvzxhjvmitkczc.supabase.co/functions/v1/regent-driver-compliance"
        private const val AUTH = "https://anmrzqcvzxhjvmitkczc.supabase.co/auth/v1"
        private const val PUBLISHABLE_KEY = "sb_publishable_Vx49qBswhnTo58pRILfz1A_hnTWNZPh"
    }

    private lateinit var webView: WebView
    private lateinit var identitySheet: IdentityVerificationSheet
    private val prefs by lazy { getSharedPreferences("regent_mile", MODE_PRIVATE) }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(40, TimeUnit.SECONDS)
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val locationManager by lazy { getSystemService(LOCATION_SERVICE) as LocationManager }
    private var isOnline = false
    private var lastLocation: Location? = null
    private var heartbeat: Job? = null
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var pendingOnlineAfterPermission = false

    private val filePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uris = if (result.resultCode == RESULT_OK) {
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        } else null
        fileCallback?.onReceiveValue(uris)
        fileCallback = null
    }

    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            if (pendingOnlineAfterPermission) startOnline()
        } else {
            isOnline = false
            sendNativeState("Location permission is required")
        }
        pendingOnlineAfterPermission = false
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        sendNativeState(if (it) "Notifications enabled" else "Notifications are off")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val logoUri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(packageName)
            .appendPath("drawable")
            .appendPath("regent_logo")
            .build()
        identitySheet = IdentityVerificationSheet.create(
            this,
            IdentityVerificationSheet.Configuration(brandLogo = logoUri, brandColor = 0xFFBFA66A.toInt())
        ) { result ->
            when (result) {
                is IdentityVerificationSheet.VerificationFlowResult.Completed -> {
                    callbackIdentity("completed", "Verification submitted to Stripe")
                    scope.launch { pollIdentityStatus() }
                }
                is IdentityVerificationSheet.VerificationFlowResult.Canceled -> callbackIdentity("canceled", "Verification canceled")
                is IdentityVerificationSheet.VerificationFlowResult.Failed -> callbackIdentity("failed", result.throwable.localizedMessage ?: "Stripe verification failed")
            }
        }

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
        webView = WebView(this).apply {
            id = R.id.webview
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowContentAccess = true
            settings.allowFileAccess = false
            settings.mediaPlaybackRequiresUserGesture = true
            addJavascriptInterface(NativeBridge(), "RegentNative")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?) = request?.url?.let(assetLoader::shouldInterceptRequest)
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val u = request?.url ?: return false
                    if (u.host == "appassets.androidplatform.net") return false
                    openExternal(u.toString())
                    return true
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    fileCallback?.onReceiveValue(null)
                    fileCallback = filePathCallback
                    val intent = (fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_OPEN_DOCUMENT)).apply {
                        action = Intent.ACTION_OPEN_DOCUMENT
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "application/pdf"))
                    }
                    return try { filePicker.launch(intent); true } catch (_: Throwable) { fileCallback = null; false }
                }
            }
            loadUrl("https://appassets.androidplatform.net/assets/index.html")
        }
        setContentView(webView)

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript("Boolean(window.handleAndroidBack && window.handleAndroidBack())") { value ->
                    if (value != "true") finish()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            webView.evaluateJavascript("window.onPaymentResume && window.onPaymentResume()", null)
            sendNativeState()
        }
    }

    override fun onDestroy() {
        stopLocationUpdates(false)
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    private fun token(): String = prefs.getString("device_token", "") ?: ""
    private fun setToken(value: String) = prefs.edit().putString("device_token", value).apply()

    private inner class NativeBridge {
        @JavascriptInterface fun loadState(): String = prefs.getString("web_state", "{}") ?: "{}"
        @JavascriptInterface fun saveState(value: String) { prefs.edit().putString("web_state", value).apply() }
        @JavascriptInterface fun connected(): Boolean = token().length >= 32

        @JavascriptInterface fun call(id: String, action: String, json: String) {
            scope.launch {
                val result = try {
                    when (action) {
                        "auth_signin" -> signIn(JSONObject(json))
                        "auth_signup" -> signUp(JSONObject(json))
                        else -> edgeCall(action, JSONObject(json), token().ifBlank { null })
                    }
                } catch (t: Throwable) {
                    JSONObject().put("error", t.message ?: "Request failed")
                }
                val newToken = result.optString("token")
                if (newToken.length >= 32) setToken(newToken)
                js("window.onControlResult(${JSONObject.quote(id)},${JSONObject.quote(result.toString())})")
            }
        }

        @JavascriptInterface fun online() = runOnUiThread { requestOnline() }
        @JavascriptInterface fun offline() = runOnUiThread { stopLocationUpdates(true) }
        @JavascriptInterface fun clearOffer() { }
        @JavascriptInterface fun refresh() = runOnUiThread { sendNativeState(); webView.evaluateJavascript("window.onPaymentResume && window.onPaymentResume()", null) }
        @JavascriptInterface fun disconnect() = runOnUiThread { setToken(""); stopLocationUpdates(false); sendNativeState("Signed out") }
        @JavascriptInterface fun reset() = runOnUiThread { prefs.edit().clear().apply(); stopLocationUpdates(false); webView.reload() }

        @JavascriptInterface fun notifications() = runOnUiThread {
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                runCatching { startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)) }
            }
        }

        @JavascriptInterface fun navigate(app: String, url: String) = runOnUiThread { openExternal(url) }
        @JavascriptInterface fun openStripe(url: String) = runOnUiThread { openExternal(url) }
        @JavascriptInterface fun openSupport(url: String) = runOnUiThread { openExternal(url) }

        @JavascriptInterface fun contactCustomer(jobId: String, mode: String) {
            scope.launch {
                try {
                    val r = edgeCall("customer_contact", JSONObject().put("id", jobId).put("mode", mode), token())
                    val phone = r.getString("phone")
                    val intent = if (mode == "text") Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phone)}")) else Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))
                    startActivity(intent)
                } catch (_: Throwable) { }
            }
        }

        @JavascriptInterface fun printInvoice() = runOnUiThread {
            val manager = getSystemService(PRINT_SERVICE) as android.print.PrintManager
            manager.print("Regent Mile invoice", webView.createPrintDocumentAdapter("Regent Mile invoice"), null)
        }

        @JavascriptInterface fun startIdentity() = runOnUiThread { startStripeIdentity() }
    }

    private suspend fun signUp(data: JSONObject): JSONObject {
        val body = JSONObject()
            .put("email", data.optString("email"))
            .put("password", data.optString("password"))
            .put("data", JSONObject().put("driver_name", data.optString("name")).put("vehicle", data.optString("vehicle", "car")))
        val r = requestJson("$AUTH/signup", body, null, mapOf("apikey" to PUBLISHABLE_KEY))
        if (r.has("error") || r.has("error_description") || r.has("msg")) throw IllegalStateException(errorMessage(r))
        return if (r.optJSONObject("session") == null && r.optString("access_token").isBlank()) JSONObject().put("confirmation_required", true) else signIn(data)
    }

    private suspend fun signIn(data: JSONObject): JSONObject {
        val auth = requestJson("$AUTH/token?grant_type=password", JSONObject().put("email", data.optString("email")).put("password", data.optString("password")), null, mapOf("apikey" to PUBLISHABLE_KEY))
        val access = auth.optString("access_token")
        if (access.isBlank()) throw IllegalStateException(errorMessage(auth))
        val sessionData = JSONObject()
            .put("previous_token", token())
            .put("name", data.optString("name"))
            .put("vehicle", data.optString("vehicle", "car"))
            .put("device_label", "Regent Mile Android")
        val session = edgeCall("auth_session", sessionData, access)
        val device = session.optString("token")
        if (device.length < 32) throw IllegalStateException("Driver session was not created")
        setToken(device)
        return session
    }

    private suspend fun edgeCall(action: String, data: JSONObject, bearer: String?): JSONObject =
        requestJson(EDGE, JSONObject().put("action", action).put("data", data), bearer)

    private suspend fun complianceCall(action: String, data: JSONObject = JSONObject()): JSONObject =
        requestJson(COMPLIANCE_EDGE, JSONObject().put("action", action).put("data", data), token())

    private suspend fun requestJson(url: String, json: JSONObject, bearer: String?, extra: Map<String,String> = emptyMap()): JSONObject = withContext(Dispatchers.IO) {
        val b = Request.Builder().url(url).post(json.toString().toRequestBody("application/json".toMediaType())).header("Content-Type", "application/json")
        if (!bearer.isNullOrBlank()) b.header("Authorization", "Bearer $bearer")
        extra.forEach { (k,v) -> b.header(k,v) }
        client.newCall(b.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val obj = runCatching { JSONObject(text) }.getOrElse { JSONObject().put("error", if (response.isSuccessful) "Invalid server response" else "Request failed (${response.code})") }
            if (!response.isSuccessful || obj.has("error")) throw IllegalStateException(errorMessage(obj))
            obj
        }
    }

    private fun errorMessage(obj: JSONObject): String = listOf("error_description","msg","message","error").firstNotNullOfOrNull { k -> obj.optString(k).takeIf { it.isNotBlank() } } ?: "Request could not be completed"

    private fun requestOnline() {
        if (token().length < 32) { sendNativeState("Sign in first"); return }
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            pendingOnlineAfterPermission = true
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }
        startOnline()
    }

    private fun startOnline() {
        isOnline = true
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000L, 3f, this)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 6000L, 5f, this)
        } catch (_: Throwable) { }
        heartbeat?.cancel()
        heartbeat = scope.launch {
            while (isOnline) {
                sendPresence(lastLocation)
                delay(18000)
            }
        }
        sendNativeState("Finding GPS")
    }

    private fun stopLocationUpdates(sendServer: Boolean) {
        isOnline = false
        heartbeat?.cancel(); heartbeat = null
        runCatching { locationManager.removeUpdates(this) }
        if (sendServer && token().length >= 32) scope.launch { runCatching { edgeCall("presence", JSONObject().put("online", false), token()) } }
        lastLocation = null
        if (::webView.isInitialized) sendNativeState("Location is off")
    }

    override fun onLocationChanged(location: Location) {
        lastLocation = location
        if (isOnline) {
            sendNativeState("GPS active")
            scope.launch { sendPresence(location) }
        }
    }

    private suspend fun sendPresence(location: Location?) {
        if (!isOnline || token().length < 32) return
        val data = JSONObject().put("online", true).put("consent", true).put("supports_stops", true)
        if (location != null) data.put("lat", location.latitude).put("lon", location.longitude).put("accuracy", location.accuracy.toDouble()).put("captured_at", Instant.ofEpochMilli(location.time).toString())
        runCatching { edgeCall("presence", data, token()) }
    }

    private fun sendNativeState(status: String? = null) {
        val l = lastLocation
        val notifications = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val o = JSONObject().put("online", isOnline).put("notifications", notifications).put("status", status ?: if (isOnline) if (l == null) "Finding GPS" else "GPS active" else "Location is off")
        if (l != null) o.put("lat", l.latitude).put("lon", l.longitude).put("accuracy", l.accuracy).put("time", l.time)
        js("window.onNativeState && window.onNativeState(${o})")
    }

    private fun startStripeIdentity() {
        if (token().length < 32) { callbackIdentity("failed", "Sign in first"); return }
        scope.launch {
            try {
                val r = complianceCall("identity_start")
                identitySheet.present(r.getString("id"), r.getString("ephemeral_key_secret"))
            } catch (t: Throwable) {
                callbackIdentity("failed", t.message ?: "Stripe identity verification could not start")
            }
        }
    }

    private suspend fun pollIdentityStatus() {
        repeat(3) { attempt ->
            delay(if (attempt == 0) 900 else 2200)
            try {
                val r = complianceCall("identity_status")
                val v = r.optJSONObject("verification")
                val status = v?.optString("status") ?: "processing"
                if (status == "verified") { callbackIdentity("verified", "Identity verified"); return }
                if (status == "requires_input" || status == "failed") { callbackIdentity("failed", v?.optString("last_error_reason") ?: "Stripe needs another verification attempt"); return }
            } catch (_: Throwable) { }
        }
        callbackIdentity("completed", "Stripe is still processing the verification")
    }

    private fun callbackIdentity(status: String, message: String) = js("window.onIdentityResult && window.onIdentityResult(${JSONObject.quote(status)},${JSONObject.quote(message)})")

    private fun openExternal(url: String) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        if (uri.scheme !in listOf("https", "http", "geo", "google.navigation", "waze", "mailto", "tel", "smsto")) return
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }

    private fun js(code: String) {
        if (!::webView.isInitialized) return
        Handler(Looper.getMainLooper()).post { webView.evaluateJavascript(code, null) }
    }
}
