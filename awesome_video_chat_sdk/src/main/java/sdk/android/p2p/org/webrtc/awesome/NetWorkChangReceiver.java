package org.webrtc.awesome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class NetWorkChangReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            Toast.makeText(context, "network change!!", Toast.LENGTH_SHORT).show();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d("NetWorkChangReceiver", "isNetworkOnline = " + isNetworkOnline());
                }
            }).start();
        }
    }

    public boolean isNetworkOnline() {
        Runtime runtime = Runtime.getRuntime();
        Process ipProcess = null;
        try {
            // ping baidu， 查看网络是否联通
            ipProcess = runtime.exec("ping -c 1 -w 3 www.baidu.com");
            InputStream input = ipProcess.getInputStream();

            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            StringBuffer stringBuffer = new StringBuffer();
            String content = "";
            while ((content = in.readLine()) != null) {
                stringBuffer.append(content);
            }

            int exitValue = ipProcess.waitFor();
            if (exitValue == 0) {
                //WiFi连接，网络正常
                return true;
            } else {
                if (stringBuffer.length() == 0 || stringBuffer.indexOf("100% packet loss") != -1) {
                    //网络丢包严重，判断为网络未连接
                    return false;
                } else {
                    //网络未丢包，判断为网络连接
                    return true;
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (ipProcess != null) {
                ipProcess.destroy();
            }
            runtime.gc();
        }
        return false;
    }

    /**
     * 判断MOBILE网络是否可用
     *
     * @param context
     * @return
     * @throws Exception
     */
    public static boolean isMobileDataEnable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isMobileDataEnable = false;
        isMobileDataEnable = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected();
        return isMobileDataEnable;
    }

    /**
     * 判断wifi 是否可用
     *
     * @param context
     * @return
     * @throws Exception
     */
    public static boolean isWifiDataEnable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isWifiDataEnable = false;
        isWifiDataEnable = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
        return isWifiDataEnable;
    }
}
