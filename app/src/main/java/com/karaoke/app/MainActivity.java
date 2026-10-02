package com.karaoke.app;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private WebView web;
    private LinearLayout form;
    private TextView msg;
    private EditText field;
    private PermissionRequest pending;
    private String host = "";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        int bg = Color.parseColor("#130e29");

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(bg);

        web = new WebView(this);
        web.setBackgroundColor(bg);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView v, SslErrorHandler h, SslError e) {
                String eh = Uri.parse(e.getUrl()).getHost();
                if (eh != null && eh.equals(host)) h.proceed(); else h.cancel();
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) {
                    showForm("Não consegui conectar. Veja se o PC está ligado, com o karaokê aberto, e confira o endereço.");
                }
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest r) {
                runOnUiThread(() -> {
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        r.grant(r.getResources());
                    } else {
                        pending = r;
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                    }
                });
            }
        });
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));

        form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setGravity(Gravity.CENTER);
        form.setPadding(48, 48, 48, 48);
        form.setBackgroundColor(bg);
        TextView title = new TextView(this);
        title.setText("Karaokê");
        title.setTextSize(32);
        title.setTextColor(Color.WHITE);
        msg = new TextView(this);
        msg.setTextColor(Color.parseColor("#ffc857"));
        msg.setPadding(0, 24, 0, 24);
        field = new EditText(this);
        field.setHint("Endereço do PC, ex.: 192.168.0.10:8443");
        field.setHintTextColor(Color.GRAY);
        field.setTextColor(Color.WHITE);
        field.setSingleLine(true);
        Button go = new Button(this);
        go.setText("Conectar");
        go.setOnClickListener(v -> connect(field.getText().toString()));
        form.addView(title);
        form.addView(msg);
        form.addView(field);
        form.addView(go);
        root.addView(form, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        SharedPreferences p = getSharedPreferences("k", MODE_PRIVATE);
        String saved = p.getString("url", "");
        if (saved.isEmpty()) {
            showForm("Digite o endereço que aparece na janela preta do PC (linha CELULAR).");
        } else {
            field.setText(saved);
            connect(saved);
        }
    }

    private void connect(String in) {
        String u = in.trim();
        if (u.isEmpty()) return;
        if (!u.startsWith("http")) u = "https://" + u;
        String h = Uri.parse(u).getHost();
        if (h == null) {
            showForm("Endereço inválido.");
            return;
        }
        host = h;
        getSharedPreferences("k", MODE_PRIVATE).edit().putString("url", in.trim()).apply();
        form.setVisibility(View.GONE);
        web.loadUrl(u);
    }

    private void showForm(String m) {
        msg.setText(m);
        form.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (form.getVisibility() == View.VISIBLE) {
            finish();
        } else {
            web.loadUrl("about:blank");
            showForm("Toque em Conectar para voltar ao karaokê, ou troque o endereço.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        if (pending != null) {
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) {
                pending.grant(pending.getResources());
            } else {
                pending.deny();
            }
            pending = null;
        }
    }
}
