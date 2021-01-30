package cloud.apposs.webx.sample.listener;

import cloud.apposs.rest.Handler;
import cloud.apposs.rest.listener.statistics.HandlerStatListener;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LogStatListener extends HandlerStatListener<HttpServletRequest, HttpServletResponse> {
    @Override
    public void setStartTime(HttpServletRequest request, HttpServletResponse response, Handler handler) {
        request.setAttribute("AttrStartTime", System.currentTimeMillis());
    }

    @Override
    public long getStartTime(HttpServletRequest request, HttpServletResponse response, Handler handler) {
        Object startTime = request.getAttribute("AttrStartTime");
        if (startTime == null) {
            return -1;
        }
        return (long) startTime;
    }
}
