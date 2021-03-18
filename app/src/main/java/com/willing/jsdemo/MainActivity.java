package com.willing.jsdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        url = ((EditText) findViewById(R.id.url)).getText().toString();

        /**
         * 暴露组件，通过 am 命令调起
         */
        Intent intent = getIntent();
        if (intent != null) {
            Uri uri = intent.getData();
            if (uri != null) {
                url = uri.toString();
            }
        }
    }

    public void openWebView(View v) {
        WebActivity.startWebActivity(this, "webview", url);
    }

    public void openWebPage(View v) {
        WebActivity.startWebPage(this, url);
    }
}