package cloud.apposs.util;

import org.junit.Assert;
import org.junit.Test;

public class TestConvertor {
    @Test
    public void testHex2bytes() {
        String value = "53d001bf713fa503435070bf89875b2c";
        byte[] bytes = Convertor.hex2bytes(value);
        Assert.assertTrue(value.equals(Convertor.bytes2hex(bytes)));
    }
}
