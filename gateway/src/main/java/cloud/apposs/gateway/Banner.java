package cloud.apposs.gateway;

import java.io.PrintStream;

public class Banner {
	private static final String[] BANNER = {
		"       _                 _      __ __  ",
		"  ___ | | ___  _   _  __| |_  __\\ \\\\ \\ ",
		" / __ | |/ _ \\| | | |/ _` \\ \\/ / \\ \\\\ \\",
		"| (__ | | (_) | |_| | (_| |>  <  / // /",
		" \\___ |_|\\___/ \\__,_|\\__,_/_/\\_\\/_//_/"
		};
	private static final String CLOUDX_BOOT = " :: CloudX Gateway :: ";
	private static final int STRAP_LINE_SIZE = 38;
	
	public void printBanner(PrintStream printStream) {
		for (String line : BANNER) {
			printStream.println(line);
		}
		StringBuilder padding = new StringBuilder();
		while (padding.length() < STRAP_LINE_SIZE - (GatewayConstants.GATEWAY_VERSION.length() + CLOUDX_BOOT.length())) {
			padding.append(" ");
		}
		printStream.println(CLOUDX_BOOT + padding.toString() + GatewayConstants.GATEWAY_VERSION);
		printStream.println();
		printStream.flush();
	}
}
