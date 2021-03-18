package com.willing.jsdemo;

import android.content.Context;
import android.webkit.JavascriptInterface;

public class NativeToJS extends Object {

    private Context context;
    public NativeToJS(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void callNative() {
        AlertUtils.show(context, "Native Alert", "JS调用Native的callNative()方法", null);
    }

    @JavascriptInterface
    public void callNative(String message) {
        AlertUtils.show(context, "Native Alert", "JS调用Native的callNative(" + message + ")方法", null);
    }
}