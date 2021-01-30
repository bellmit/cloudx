package cloud.apposs.balance.ping;

import cloud.apposs.balance.IPing;
import cloud.apposs.balance.Peer;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 基于HTTP协议的请求检测
 */
public class HttpPing implements IPing {
    @Override
    public boolean isAlive(Peer peer) {
        HttpURLConnection uc = null;
        try {
            String url = peer.getUrl();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            uc = (HttpURLConnection) (new URL(url)).openConnection();
            return uc.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        } finally {
            if (uc != null) {
                uc.disconnect();
                uc = null;
            }
        }
    }
}
