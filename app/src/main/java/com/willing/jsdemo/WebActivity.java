package com.willing.jsdemo;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Set;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class WebActivity extends AppCompatActivity {

    private static final String TAG = WebActivity.class.getSimpleName();
    private Context context;
    private static String TITLE = "title", URL = "url";
    private String title, url;
    private Toolbar toolbar;
    private WebView webView;
    private View errorImage;
    private TextView errorText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        context = this;
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            title = bundle.getString(TITLE);
            url = bundle.getString(URL);
        }

        setDebugable();
        initToolbar();
        initView();
    }

    // 添加 chrome:inspect debug 功能
    @TargetApi(19)
    private void setDebugable() {
        if (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }

    @TargetApi(21)
    private void initToolbar() {
        toolbar = findViewById(R.id.web_toolbar);
        toolbar.setTitle(title);
    }

    private void initView() {
        webView = (WebView) findViewById(R.id.web_page);
        errorImage = (ImageView) findViewById(R.id.web_image);
        errorText = (TextView) findViewById(R.id.web_message);

        webView.setVisibility(View.VISIBLE);
        errorImage.setVisibility(View.GONE);
        errorText.setVisibility(View.GONE);
        initData();
    }

    private void initData() {
        initWebSetting();

        /**
         * 处理各种通知, 请求事件
         */
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "onPageFinished url = " + url);
                /**
                 * Native 调用 JS 的两种方法
                 * （由于 JS 代码的调用必须在网页加载完成后进行，因此直接在 onPageFinished 回调中演示）
                 * 1、直接通过 loadUrl 函数调用，参数为 <script_name>:<function_name>
                 *    调用方法简单，可以携带参数，但是无法收到 JS 的返回值，直接返回在网页中
                 * 2、通过 evaluateJavascript 函数调用，参数同上，在第二个参数的 onReceiveValue 回调中返回 JS 的返回值
                 *    相比直接调用就是多了一个回调，此方法在 Android 4.4 之后引入，无需刷新页面效率更高
                 */
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
//                    webView.loadUrl("javascript:callJS()");
                    webView.loadUrl("javascript:callJSwithPara('hello from native')");
                } else {
                    Log.d(TAG, "Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT");
//                    webView.evaluateJavascript("javascript:callJS()", null);
                    webView.evaluateJavascript("javascript:callJSwithPara('hello from native')", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            Log.d(TAG, "evaluateJavascript value = " + value);
                        }
                    });
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "[21-]shouldOverrideUrlLoading url = " + url);
                /**
                 * JS 调用 Native 的方法二：
                 * 在 JS 中访问某个 url，然后在 Native 的 shouldOverrideUrlLoading 回调拦截这个 url
                 * 注意，双方最好约定协议的 scheme 等字段，便于过滤
                 */
                Uri uri = Uri.parse(url);
                if (uri.getScheme().equals("jsbridge") && uri.getAuthority().equals("webview")) {
                    Set<String> keys = uri.getQueryParameterNames();
                    for (String key : keys) {
                        String content = uri.getQueryParameter(key);
                        AlertUtils.show(context, "Native Alert", content, null);
                    }
                    return true;
                }
                return super.shouldOverrideUrlLoading(webView, url);
            }

            @TargetApi(21)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Log.d(TAG, "[21+]shouldOverrideUrlLoading url = " + request.getUrl().toString());
                if (request.getUrl().getScheme().equals("jsbridge") && request.getUrl().getAuthority().equals("webview")) {
                    Set<String> keys = request.getUrl().getQueryParameterNames();
                    for (String key : keys) {
                        String content = request.getUrl().getQueryParameter(key);
                        /**
                         * 这里在 JS 中访问 url 并在 Native 中拦截，解析参数后在弹窗的点击事件中又调了下 JS 里面的方法
                         */
                        AlertUtils.show(context, "Native Alert", content, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                webView.loadUrl("javascript:returnRes('Hello, JS')");
                            }
                        });
                    }
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e(TAG,"onReceivedError errorCode = " + errorCode + ", description = " + description + ", failingUrl = " + failingUrl);
                super.onReceivedError(view, errorCode, description, failingUrl);
                webView.setVisibility(View.GONE);
                errorImage.setVisibility(View.VISIBLE);
                errorText.setVisibility(View.VISIBLE);
                errorText.setText("errorCode : " + errorCode + "\n" + description);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });

        /**
         * 处理JS的对话框、网址图标、网址标题和加载进度等
         */
        webView.setWebChromeClient(new WebChromeClient() {

            /**
             * JS 调用 Native 的方法三：
             * 直接在 JS 中调用 alert/confirm/prompt 方法，然后在 WebChromeClient 对象的 onJsAlert/onJsConfirm/onJsPrompt 回调中拦截
             * 其中 alert 是没有返回的弹窗，confirm 根据确认/取消返回布尔类型，prompt
             *
             */
            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                Log.d(TAG, "onJsAlert url = " + url + ", message = " + message);
                AlertUtils.show(context, "JS Alert", message, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.confirm();
                    }
                });
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                Log.d(TAG, "onJsConfirm url = " + url + ", message = " + message);
                Uri uri = Uri.parse(message);
                if (uri.getScheme().equals("jsbridge") && uri.getAuthority().equals("jsconfirm")) {
                    Set<String> keys = uri.getQueryParameterNames();
                    for (String key : keys) {
                        String content = uri.getQueryParameter(key);
                        AlertUtils.show(context, "Native Alert", content, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        });
                    }
                    return true;
                }
                return super.onJsConfirm(view, url, message, result);
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, final JsPromptResult result) {
                Log.d(TAG, "onJsPrompt url = " + url + ", message = " + message + ", defaultValue = " + defaultValue);
                Uri uri = Uri.parse(message);
                if (uri.getScheme().equals("jsbridge") && uri.getAuthority().equals("jsprompt")) {
                    Set<String> keys = uri.getQueryParameterNames();
                    for (String key : keys) {
                        String content = uri.getQueryParameter(key);
                        AlertUtils.show(context, "Native Alert", content, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm("Got it!");
                            }
                        });
                    }
                    return true;
                }
                return super.onJsPrompt(view, url, message, defaultValue, result);
            }
        });

        /**
         * JS 调用 Native 的方法一：
         * 通过 addJavascriptInterface 方法将本地类对象映射成 JS 中的对象
         * 注意，本地对象中被调用的方法需要添加 @JavascriptInterface 注解
         * 此方法调用简单，只要约定好对象映射关系就可以直接进行调用，但是可能造成严重的漏洞
         */
        webView.addJavascriptInterface(new NativeToJS(context), "NativeToJS");

        /**
         * WebView 加载用法示例：
         * 1、使用 loadUrl 接口，可以加载网页、应用内资源文件、本地文件；
         */
//        webView.loadUrl("https://www.baidu.com");
//        webView.loadUrl("file:///android_asset/javascript.html");
//        webView.loadUrl("content://com.android.htmlfileprovider/sdcard/javascript.html");
        /**
         * 2、使用 loadUrl(String url, Map<String, String> additionalHttpHeaders）接口，用法同1，可以
         * 附加 headers信息；
         *
         * 3、通过 loadData(String data, String mimeType, String encoding) 接口加载一段代码，通常为网页
         * 的部分内容，参数分别为内容、类型和编码方式
         */
//        webView.loadData("<html>\n" +
//                "<head>\n" +
//                "    <title>网页demo</title>\n" +
//                "</head>\n" +
//                "<body>\n" +
//                "<h2>\n" +
//                "    使用WebView加载网页代码\n" +
//                "</h2>\n" +
//                "</body>\n" +
//                "</html>", "text/html", "utf-8");
        /**
         * 4、通过 loadDataWithBaseURL(String baseUrl, String data, String mimeType, String
         * encoding, String historyUrl) 加载基于网页的内容，与上一个接口相似，兼容性更好，适用场景更多
         */
//        String img = "展示CSDN的图示，通过相对地址访问<img src='/cdn/content-toolbar/csdn-logo.png?v=20200416.1' />";
//        webView.loadDataWithBaseURL("https://csdnimg.cn", img, "text/html", "utf-8",null);

        webView.loadUrl(url);
    }

    @TargetApi(16)
    private void initWebSetting() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setSavePassword(true);
        // 允许网页与JS交互
        webSettings.setJavaScriptEnabled(true);
        // 允许通过JS打开新窗口
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        /**
         * File 域访问开关：
         * 1、setAllowFileAccess 允许开启 File 域访问，即可以通过 file:/// 协议访问本地文件，默认true
         * 2、setAllowFileAccessFromFileURLs 允许通过 File 域加载的文件内部访问其他文件，如通过加载的
         * js文件再去加载本地其他文件，Android 4.1 以前默认true
         * 3、setAllowUniversalAccessFromFileURLs 允许通过 File 域加载的文件内部访问其他源，包括
         * http/https，Android 4.1 以前默认true
         * 3 的条件中包含 2，因此开启 3 会覆盖 2 中的设置
         */
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        // 调节屏幕自适应
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        // 调节允许缩放，但不显示按钮
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        // 缓存设置
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadDataWithBaseURL(null, "", "", "", null);
            webView.clearCache(true);
            webView.clearHistory();
            ((ViewGroup)webView.getParent()).removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public static void startWebActivity(Context context, String title, String url) {
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(url)) {
            return;
        }
        Intent intent = new Intent(context, WebActivity.class);
        intent.putExtra(TITLE, title);
        intent.putExtra(URL, url);
        context.startActivity(intent);
    }

    public static void startWebPage(Context context, String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(intent);
    }
}