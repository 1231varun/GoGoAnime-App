 package com.survivalstudiosentertainment.gogoanime;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.webkit.WebBackForwardList;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

 public class MainActivity extends Activity {
    private WebView webView;
    private FrameLayout customViewContainer;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private View mCustomView;
    private myWebChromeClient mWebChromeClient;
    private myWebViewClient mWebViewClient;
    private SwipeRefreshLayout swipeLayout;
    private SharedPreferences prefs;
    private String defaultUrl = "https://gogoanime.io";
    private String splashUrl = "https://loadlasturl.gogoanime.io";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("lastUrl",0);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);
        customViewContainer = (FrameLayout) findViewById(R.id.customViewContainer);
        webView = (WebView) findViewById(R.id.webView);

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        mWebViewClient = new myWebViewClient();
        webView.setWebViewClient(mWebViewClient);

        mWebChromeClient = new myWebChromeClient();
        webView.setWebChromeClient(mWebChromeClient);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setSaveFormData(true);
        webView.setHapticFeedbackEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        String prefsLastUrl = prefs.getString("lastUrl",webView.getUrl());
        if (savedInstanceState != null)
            webView.restoreState(savedInstanceState);
        else if(prefsLastUrl != null && checkUrlBeforLoad(prefsLastUrl))
                webView.loadUrl(prefsLastUrl);
        else
        webView.loadUrl("file:///android_asset/splash.html");

        if(swipeLayout.isRefreshing()) {
            swipeLayout.setRefreshing(false);
        }

        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //your method to refresh content
                webView.loadUrl(webView.getUrl());
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
    }

    public boolean inCustomView() {
        return (mCustomView != null);
    }

    public void hideCustomView() {
        mWebChromeClient.onHideCustomView();
    }

    @Override
    protected void onPause() {
        super.onPause();    //To change body of overridden methods use File | Settings | File Templates.
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();    //To change body of overridden methods use File | Settings | File Templates.
        webView.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();    //To change body of overridden methods use File | Settings | File Templates.
        if (inCustomView()) {
            hideCustomView();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        String webUrl = webView.getUrl();
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (webView.canGoBack()) {
                if((webUrl.equals("https://gogoanime.io/"))||(webUrl.equals("file:///android_asset/error_page.html")||(webUrl.equals("file:///android_asset/splash.html")))){
                    new AlertDialog.Builder(this).setTitle("GoGoAnime")
                            .setIcon(R.mipmap.ic_launcher)
                            .setMessage("Are you sure you want to exit the app?")
                            .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    Intent intent = new Intent(Intent.ACTION_MAIN);
                                    intent.addCategory(Intent.CATEGORY_HOME);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                }
                            }).setNegativeButton("no", null).show();
                }
                else
                if (inCustomView()) {
                    hideCustomView();
                    return true;
                }
                else
                if ((mCustomView == null) && webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
                else
                    webView.goBack();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    class myWebChromeClient extends WebChromeClient {
        private Bitmap mDefaultVideoPoster;
        private View mVideoProgressView;

        @Override
        public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
            onShowCustomView(view, callback);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public void onShowCustomView(View view,CustomViewCallback callback) {

            // if a view already exists then immediately terminate the new one
            if (mCustomView != null) {
                callback.onCustomViewHidden();
                return;
            }
            mCustomView = view;
            webView.setVisibility(View.GONE);
            swipeLayout.setVisibility(View.GONE);

            customViewContainer.setVisibility(View.VISIBLE);
            customViewContainer.addView(view);
            customViewCallback = callback;
        }

        @Override
        public View getVideoLoadingProgressView() {

            if (mVideoProgressView == null) {
                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                mVideoProgressView = inflater.inflate(R.layout.video_progress, null);
            }
            return mVideoProgressView;
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();    //To change body of overridden methods use File | Settings | File Templates.
            if (mCustomView == null)
                return;

            webView.setVisibility(View.VISIBLE);
            swipeLayout.setVisibility(View.VISIBLE);
            customViewContainer.setVisibility(View.GONE);

            // Hide the custom view.
            mCustomView.setVisibility(View.GONE);

            // Remove the custom view from its container.
            customViewContainer.removeView(mCustomView);
            customViewCallback.onCustomViewHidden();

            mCustomView = null;
        }
    }
    private WebResourceResponse getTextWebResource(InputStream data) {
        return new WebResourceResponse("text/plain", "UTF-8", data);
    }

    private boolean checkUrlBeforLoad(String url) {
        try {
            URL myURL = new URL(url);
            String urlHostName= myURL.getHost();
            if (urlHostName.contains("gogoanime")) {
                Log.d("hidden", "URL loaded="+url);
                return true;
            }
            return false;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
    }
    class myWebViewClient extends WebViewClient {
        public boolean loadUrl(WebView view, String url){
            if (checkUrlBeforLoad(url)) {
                view.loadUrl(url);
            }
            return true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            view.loadUrl("file:///android_asset/error_page.html");
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if(checkUrlBeforLoad(url)){
                view.loadUrl(url);
            }
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon){

            String currentUrl = webView.getUrl();
            if(!currentUrl.equals(splashUrl) && currentUrl.contains("episode-")){
                Toast.makeText(getApplicationContext(),currentUrl+"saving",Toast.LENGTH_LONG).show();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("lastUrl",currentUrl);
                editor.commit();
            }
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms
                    webView.loadUrl("javascript:(function() { " +
                            "document.getElementsByClassName('mgbox')[0].style.display='none'; })()");
                    webView.loadUrl("javascript:(function() { " +
                            "document.getElementsByClassName('mgbox')[1].style.display='none'; })()");
                    webView.loadUrl("javascript:(function() { " +
                            "document.getElementsByClassName('mgbox')[2].style.display='none'; })()");
                    Log.d("hidden", "This is my message");
                }
            }, 15000);

        }

        @Override
        public void onPageFinished(WebView view, String url) {
            // hide element by class name
            webView.loadUrl("javascript:(function() { " +
                    "var listContainer = document.getElementsByClassName('menu_top')[0].getElementsByTagName('ul')[0].firstElementChild;"+
                    "listContainer.outerHtml.append = '<li class='user'><a href='"+prefs.getString("lastUrl",webView.getUrl())+"'' class='account'>Last Visted URL</a></li>'");
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms
                    webView.loadUrl("javascript:(function() { " +
                            "document.getElementsByClassName('mgbox')[0].style.display='none'; })()");
                    webView.loadUrl("javascript:(function() { " +
                            "document.getElementsByClassName('mgbox')[1].style.display='none'; })()");
                    webView.loadUrl("javascript:(function() { " +
                            "document.getElementsByClassName('mgbox')[2].style.display='none'; })()");
                    Log.d("hidden", "This is my message");
                }
            }, 15000);

            if (swipeLayout.isRefreshing()) {
                swipeLayout.setRefreshing(false);
            }
        }
    }

}

