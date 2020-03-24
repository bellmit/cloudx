package cloud.apposs.util;

import java.util.HashMap;

public class GetOpt extends HashMap<String, String> {
	private static final long serialVersionUID = 4583267958480853918L;

	public GetOpt(String[] args) {
        super();
        if (args==null) {
            return;
        }
        String key = null;
        for (int i = 0; i < args.length; i++) {
            String parsed = args[i];
            if (parsed.startsWith("-")) {
                key = parsed.replaceAll("-","");
            } else {
                if (key != null) {
	                this.put(key, parsed);
	                key = null;
                }
            }
        }
    }
}
