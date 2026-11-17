package dev.lotd.offline;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final String TAG = "LOTD-Offline";
    private static final String ASSET_URL = "https://appassets.androidplatform.net/assets/www/index.html";
    private static final Set<String> ALLOWED_FILES = new HashSet<>(Arrays.asList(
            "index.html", "index.js", "index.wasm", "game.js", "game.data", "vnet.js"
    ));

    private FrameLayout root;
    private WebView webView;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        enterImmersive();

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);    // Required for profiles/saves (IDBFS + localStorage).
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);

        // All resources are loaded under a local HTTPS-style origin. Never use file://.
        WebViewAssetLoader assets = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", this::localResponse)
                .build();

        webView.setWebViewClient(new WebViewClientCompat() {
            @Override
            public WebResourceResponse shouldInterceptRequest(@NonNull WebView view,
                                                              @NonNull WebResourceRequest request) {
                android.net.Uri url = request.getUrl();
                if (!"https".equals(url.getScheme()) ||
                    !"appassets.androidplatform.net".equals(url.getHost())) {
                    // Block any accidental external URL, including multiplayer servers.
                    return notFound();
                }
                WebResourceResponse response = assets.shouldInterceptRequest(url);
                return response == null ? notFound() : response;
            }

            @Override
            public boolean shouldOverrideUrlLoading(@NonNull WebView view,
                                                     @NonNull WebResourceRequest request) {
                android.net.Uri url = request.getUrl();
                return !("https".equals(url.getScheme()) &&
                         "appassets.androidplatform.net".equals(url.getHost()) &&
                         url.getPath() != null && url.getPath().startsWith("/assets/www/"));
            }

            @Override
            public void onPageFinished(@NonNull WebView view, @NonNull String url) {
                Log.d(TAG, "Page loaded: " + url);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage m) {
                Log.d("LOTD-Web", m.message() + " [" + m.sourceId() + ":" + m.lineNumber() + "]");
                return true;
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (fullscreenView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                fullscreenView = view;
                fullscreenCallback = callback;
                webView.setVisibility(View.GONE);
                root.addView(view, new FrameLayout.LayoutParams(-1, -1));
                enterImmersive();
            }

            @Override
            public void onHideCustomView() {
                exitCustomFullscreen();
            }
        });

        // Detect missing game files with a visible error instead of a blank screen.
        try (InputStream ignored = getAssets().open("www/game.data")) {
            webView.loadUrl(ASSET_URL);
        } catch (IOException e) {
            Toast.makeText(this, "game.data missing: run copy_web_build.bat", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Missing game data", e);
        }
    }

    private WebResourceResponse localResponse(String path) {
        if (path == null || !path.startsWith("www/")) return notFound();
        String filename = path.substring(4);
        if (!ALLOWED_FILES.contains(filename)) return notFound();
        try {
            InputStream stream = getAssets().open(path, AssetManager.ACCESS_STREAMING);
            String mime;
            if (filename.endsWith(".html")) mime = "text/html";
            else if (filename.endsWith(".js")) mime = "application/javascript";
            else if (filename.endsWith(".wasm")) mime = "application/wasm";
            else mime = "application/octet-stream";
            WebResourceResponse response = new WebResourceResponse(
                    mime, (filename.endsWith(".js") || filename.endsWith(".html")) ? "UTF-8" : null,
                    200, "OK", Collections.singletonMap("Cache-Control", "no-store"), stream
            );
            return response;
        } catch (IOException ex) {
            Log.e(TAG, "Missing packaged file: " + path, ex);
            return notFound();
        }
    }

    private static WebResourceResponse notFound() {
        return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found",
                new HashMap<>(), new ByteArrayInputStream(new byte[0]));
    }

    private void enterImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
    }

    private void exitCustomFullscreen() {
        if (fullscreenView == null) return;
        root.removeView(fullscreenView);
        fullscreenView = null;
        webView.setVisibility(View.VISIBLE);
        if (fullscreenCallback != null) fullscreenCallback.onCustomViewHidden();
        fullscreenCallback = null;
        enterImmersive();
    }

    @Override
    public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) enterImmersive();
    }

    @Override
    public void onBackPressed() {
        if (fullscreenView != null) {
            exitCustomFullscreen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (fullscreenView != null) exitCustomFullscreen();
        if (webView != null) {
            root.removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
