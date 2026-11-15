package cc.framextv.tv;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Wrapper de Android TV con navegación estilo Netflix:
 *  - Flechas: saltan entre cards/links/botones (el seleccionado se agranda y resalta)
 *  - OK: abre el elemento seleccionado (en un campo de texto, abre el teclado)
 *  - Atrás: cierra el teclado / cierra carteles (modals) / sale del video fullscreen / vuelve atrás / sale
 *  - Las ventanas emergentes (publicidad) se bloquean
 *
 * La lógica de navegación está en assets/tvnav.js y se inyecta en cada página.
 */
public class MainActivity extends Activity {

    // >>> Cambiá esta URL si querés apuntar a otro sitio <<<
    private static final String HOME_URL = "https://framextv.cc";

    private FrameLayout root;
    private WebView web;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private WebChromeClient chromeClient;
    private String navScript;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        navScript = readAsset("tvnav.js");

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        // Bloquea pop-ups / pop-unders de publicidad (window.open, target=_blank)
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        // Saca la marca "; wv" para que el sitio no detecte que es un WebView
        s.setUserAgentString(s.getUserAgentString().replace("; wv", ""));
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.addJavascriptInterface(new TvBridge(), "TV");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false; // todo se abre dentro de la app
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                injectNav();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                injectNav(); // el script se protege solo contra doble carga
            }
        });

        chromeClient = new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                return false; // no se abre ninguna ventana nueva
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                root.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                view.requestFocus();
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                root.removeView(customView);
                customView = null;
                if (customViewCallback != null) customViewCallback.onCustomViewHidden();
                customViewCallback = null;
                web.requestFocus();
            }
        };
        web.setWebChromeClient(chromeClient);

        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        root.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        web.requestFocus();

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl(HOME_URL);
        }
    }

    private void injectNav() {
        if (navScript != null) web.evaluateJavascript(navScript, null);
    }

    private String readAsset(String name) {
        try (InputStream in = getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        boolean isUp = e.getAction() == KeyEvent.ACTION_UP;
        boolean isDown = e.getAction() == KeyEvent.ACTION_DOWN;

        // Video en pantalla completa: Atrás lo cierra, el resto va al reproductor
        if (customView != null) {
            if (code == KeyEvent.KEYCODE_BACK) {
                if (isUp) chromeClient.onHideCustomView();
                return true;
            }
            return super.dispatchKeyEvent(e);
        }

        switch (code) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                if (isDown && e.getRepeatCount() == 0) {
                    web.evaluateJavascript("window.__tvOk && window.__tvOk()", null);
                }
                return true;

            case KeyEvent.KEYCODE_BACK:
                if (isUp) {
                    web.evaluateJavascript("window.__tvBack ? window.__tvBack() : false", result -> {
                        if ("true".equals(result)) return; // cerró el teclado / salió del campo
                        if (web.canGoBack()) web.goBack();
                        else finish();
                    });
                }
                return true;

            default:
                // Las flechas llegan a la página y las maneja tvnav.js
                return super.dispatchKeyEvent(e);
        }
    }

    /** Puente mínimo JS → Android: solo sirve para mostrar el teclado en pantalla. */
    private class TvBridge {
        @JavascriptInterface
        public void showKeyboard() {
            web.post(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(web, InputMethodManager.SHOW_IMPLICIT);
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onDestroy() {
        web.destroy();
        super.onDestroy();
    }
}
