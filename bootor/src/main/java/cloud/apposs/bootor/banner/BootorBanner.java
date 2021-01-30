package cloud.apposs.bootor.banner;

import cloud.apposs.bootor.BootorConstants;

import java.io.PrintStream;

public class BootorBanner implements Banner {
    private static final String[] BANNER = {
            "       _                 _      __ __  ",
            "  ___ | | ___  _   _  __| |_  __\\ \\\\ \\ ",
            " / __ | |/ _ \\| | | |/ _` \\ \\/ / \\ \\\\ \\",
            "| (__ | | (_) | |_| | (_| |>  <  / // /",
            " \\___ |_|\\___/ \\__,_|\\__,_/_/\\_\\/_//_/"
    };
    private static final String CLOUDX_BOOT = " :: CloudX Boot :: ";
    private static final int STRAT_LINE_SIZE = 38;

    @Override
    public void printBanner(PrintStream printStream) {
        for (String line : BANNER) {
            printStream.println(line);
        }
        StringBuilder padding = new StringBuilder();
        while (padding.length() < STRAT_LINE_SIZE - (BootorConstants.CLOUDX_VERSION.length() + CLOUDX_BOOT.length())) {
            padding.append(" ");
        }
        printStream.println(CLOUDX_BOOT + padding.toString() + BootorConstants.CLOUDX_VERSION);
        printStream.println();
        printStream.flush();
    }
}
