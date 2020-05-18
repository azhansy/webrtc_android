package org.webrtc.awesome;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class NetConnectUtil {

    // ping指定的地址检查网络是否联通
    public static String ip = "www.baidu.com";

    public static boolean isNetworkOnline() {
        Runtime runtime = Runtime.getRuntime();
        Process ipProcess = null;
        try {
            // ping baidu， 查看网络是否联通
            ipProcess = runtime.exec("ping -c 1 -w 3 " + ip);
            InputStream input = ipProcess.getInputStream();

            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            StringBuilder stringBuffer = new StringBuilder();
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
}
