package cloud.apposs.util;

import org.junit.Assert;
import org.junit.Test;

public class TestDecimalUtil {
    @Test
    public void testFloatError() {
        float a = 1.0f - 0.9f;
        float b = 0.9f - 0.8f;
        // 浮点数采用“尾数+阶码”的编码方式，类似于科学计数法的“有效数字+指数”的表示方式
        // 二进制无法精确表示大部分的十进制小数
        Assert.assertFalse(a == b);
        Assert.assertFalse((a + b) == 0.2);
    }

    @Test
    public void testDecimal() {
        double a = 1.0;
        double b = 0.9;
        // 输出0.09999999999999998，精度制缺失
        Assert.assertFalse((a - b) == 0.1);
        // 注意数字类型要是double或者整点数
        Assert.assertTrue(DecimalUtil.reduce(a, b) == 0.1);

        a = 0.05;
        b = 0.01;
        // 输出0.060000000000000005
        Assert.assertFalse((a + b) == 0.06);
        Assert.assertTrue(DecimalUtil.sum(a, b) == 0.06);

        a = 4.015;
        b = 100;
        // 输出401.49999999999994
        Assert.assertFalse((a * b) == 401.5);
        Assert.assertTrue(DecimalUtil.multiply(a, b) == 401.5);

        a = 123.3;
        b = 100;
        // 输出1.2329999999999999
        Assert.assertFalse((a / b) == 1.233);
        Assert.assertTrue(DecimalUtil.divide(a, b) == 1.233);
    }
}
