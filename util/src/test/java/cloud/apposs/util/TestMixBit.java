package cloud.apposs.util;

import org.junit.Assert;
import org.junit.Test;

public class TestMixBit {
    @Test
    public void testMixBit() {
        MixBit bit = MixBit.build(0);
        bit.active(0x01);
        bit.active(0x02);
        bit.active(0x08);
        Assert.assertTrue(bit.matched(0x02));
        Assert.assertTrue(bit.matched(0x08));
    }

    @Test
    public void testMixBit2() {
        MixBit bit = MixBit.build(~0);
        bit.deactive(0x01);
        bit.deactive(0x02);
        bit.deactive(0x08);
        Assert.assertFalse(bit.matched(0x02));
        Assert.assertFalse(bit.matched(0x08));
    }
}
