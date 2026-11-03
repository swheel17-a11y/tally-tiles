package com.swheel.starquest;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import java.util.Locale;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

// Star Quest runs from files inside the app, so it works with no internet and has no browser bar.
public class MainActivity extends Activity {
  private WebView web;
  private TextToSpeech tts;
  private boolean ttsReady = false;

  // Lets the game talk out loud with the tablet's voice (the in-app browser has no speech of its own).
  public class Voice {
    @JavascriptInterface public void speak(String text) {
      if (tts != null && ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sq");
    }
    @JavascriptInterface public void stop() { if (tts != null) tts.stop(); }
  }

  @Override protected void onCreate(Bundle saved) {
    super.onCreate(saved);
    web = new WebView(this);
    web.setBackgroundColor(0xFFDFF1FF);
    setContentView(web);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);          // keeps stars, stickers and levels
    s.setDatabaseEnabled(true);
    s.setAllowFileAccess(true);
    s.setMediaPlaybackRequiresUserGesture(false);
    tts = new TextToSpeech(this, status -> {
      if (status == TextToSpeech.SUCCESS) { tts.setLanguage(Locale.US); tts.setSpeechRate(0.9f); tts.setPitch(1.15f); ttsReady = true; }
    });
    web.addJavascriptInterface(new Voice(), "SQAndroid");
    web.setWebChromeClient(new WebChromeClient());
    // Stay inside the game: never open other web pages.
    web.setWebViewClient(new WebViewClient() {
      @Override public boolean shouldOverrideUrlLoading(WebView v, String url) { return !url.startsWith("file:"); }
    });
    if (saved != null) web.restoreState(saved); else web.loadUrl("file:///android_asset/index.html");
    hideBars();
  }

  private void hideBars() {
    getWindow().getDecorView().setSystemUiVisibility(
      View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
      | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
  }

  @Override public void onWindowFocusChanged(boolean focus) { super.onWindowFocusChanged(focus); if (focus) hideBars(); }
  @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); web.saveState(out); }
  // Back goes back inside the game (game -> home); from the home screen it leaves the app.
  @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
  @Override protected void onPause() { super.onPause(); web.onPause(); if (tts != null) tts.stop(); }
  @Override protected void onDestroy() { if (tts != null) tts.shutdown(); super.onDestroy(); }
  @Override protected void onResume() { super.onResume(); web.onResume(); hideBars(); }
}
