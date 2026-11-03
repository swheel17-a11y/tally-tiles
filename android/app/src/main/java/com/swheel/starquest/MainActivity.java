package com.swheel.starquest;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import org.json.JSONArray;
import org.json.JSONObject;
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
  public class SpeechBridge {
    @JavascriptInterface public void speak(String text) {
      if (tts != null && ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sq");
    }
    @JavascriptInterface public void stop() { if (tts != null) tts.stop(); }
    // English voices on this tablet, so a grown-up can pick the one that sounds best
    @JavascriptInterface public String voices() {
      JSONArray a = new JSONArray();
      try {
        if (tts != null && ttsReady && tts.getVoices() != null) for (Voice v : tts.getVoices()) {
          if (!"en".equals(v.getLocale().getLanguage())) continue;
          JSONObject o = new JSONObject();
          o.put("name", v.getName()); o.put("locale", v.getLocale().toString());
          o.put("q", v.getQuality()); o.put("net", v.isNetworkConnectionRequired());
          o.put("on", tts.getVoice() != null && v.getName().equals(tts.getVoice().getName()));
          a.put(o);
        }
      } catch (Exception e) { }
      return a.toString();
    }
    @JavascriptInterface public void setVoice(String name) {
      try { for (Voice v : tts.getVoices()) if (v.getName().equals(name)) { tts.setVoice(v); prefs.edit().putString("voice", name).apply(); } } catch (Exception e) { }
    }
    @JavascriptInterface public void setPace(float rate, float pitch) {
      if (tts != null) { tts.setSpeechRate(rate); tts.setPitch(pitch); prefs.edit().putFloat("rate", rate).putFloat("pitch", pitch).apply(); }
    }
  }
  private SharedPreferences prefs;

  // Use the saved voice, otherwise the highest-quality US English voice that works without internet.
  private void pickVoice() {
    try {
      String saved = prefs.getString("voice", null);
      Voice best = null;
      for (Voice v : tts.getVoices()) {
        if (saved != null && v.getName().equals(saved)) { best = v; break; }
        if (!"en".equals(v.getLocale().getLanguage())) continue;
        int score = v.getQuality() * 10 + ("US".equals(v.getLocale().getCountry()) ? 5 : 0) + (v.isNetworkConnectionRequired() ? 0 : 3);
        int bestScore = best == null ? -1 : best.getQuality() * 10 + ("US".equals(best.getLocale().getCountry()) ? 5 : 0) + (best.isNetworkConnectionRequired() ? 0 : 3);
        if (score > bestScore) best = v;
      }
      if (best != null) tts.setVoice(best);
    } catch (Exception e) { }
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
    prefs = getSharedPreferences("starquest", MODE_PRIVATE);
    tts = new TextToSpeech(this, status -> {
      if (status == TextToSpeech.SUCCESS) {
        tts.setLanguage(Locale.US); pickVoice();
        tts.setSpeechRate(prefs.getFloat("rate", 0.95f)); tts.setPitch(prefs.getFloat("pitch", 1.0f));
        ttsReady = true;
      }
    });
    web.addJavascriptInterface(new SpeechBridge(), "SQAndroid");
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
