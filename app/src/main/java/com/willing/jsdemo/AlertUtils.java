package com.willing.jsdemo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class AlertUtils {

    private AlertUtils() {
        throw new UnsupportedOperationException("cannot be instantiated!");
    }

    public static void show(Context context, String title, String message, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", listener)
                .setCancelable(false)
                .create().show();
    }
}
