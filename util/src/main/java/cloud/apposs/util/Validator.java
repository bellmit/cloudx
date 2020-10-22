package cloud.apposs.util;

import java.util.List;

public final class Validator {
    private Validator() {
    }

    public static void checkParamKeyValid(Param info, List<String> keys) {
        for (String key : keys) {
            if (!info.containsKey(key)) {
                throw new IllegalArgumentException("Not Contains Key [" + key + "]");
            }
        }
    }
}
