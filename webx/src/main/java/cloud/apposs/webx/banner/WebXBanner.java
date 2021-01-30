package cloud.apposs.webx.banner;

import cloud.apposs.webx.WebXConstants;

import java.io.PrintStream;

public class WebXBanner implements Banner {
    private static final String[] BANNER = {
            "  _      _________  _  __",
            " | | /| / / __/ _ )| |/_/",
            " | |/ |/ / _// _  |>  <  ",
            " |__/|__/___/____/_/|_|  "
    };
    private static final String CLOUDX_WEB = " ::WebX:: ";
    private static final int STRAT_LINE_SIZE = 25;

    @Override
    public void printBanner(PrintStream printStream) {
        for (String line : BANNER) {
            printStream.println(line);
        }
        StringBuilder padding = new StringBuilder();
        while (padding.length() < STRAT_LINE_SIZE - (WebXConstants.WEBX_VERSION.length() + CLOUDX_WEB.length())) {
            padding.append(" ");
        }
        printStream.println(CLOUDX_WEB + padding.toString() + WebXConstants.WEBX_VERSION);
        printStream.println();
        printStream.flush();
    }
}
