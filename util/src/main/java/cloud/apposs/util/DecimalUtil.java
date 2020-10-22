package cloud.apposs.util;

import java.math.BigDecimal;

/**
 * 数据计算工具，用于订单支付、金额转换等数字计算敏感操作避免浮点精度缺失
 */
public final class DecimalUtil {
    /**
     * 浮点数字相加
     *
     * @param one	运算数1，被加数或被乘数或被除数或被减数
     * @param two	运算数2，加，乘除或减数
     */
    public static double sum(double one, double two) {
        return sum(one, two, 2, false, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 浮点数字相加
     *
     * @param one	运算数1，被加数或被乘数或被除数或被减数
     * @param two	运算数2，加，乘除或减数
     * @param scale	表示表示除法需要精确到小数点以后几位
     */
    public static double sum(double one, double two, int scale) {
        return sum(one, two, scale, false, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 浮点数字相加
     *
     * @param one	运算数1，被加数或被乘数或被除数或被减数
     * @param two	运算数2，加，乘除或减数
     * @param scale	表示表示除法需要精确到小数点以后几位
     * @param forceScale 所有操作是否强制精确小数点
     * @param roundingMode 默认为ROUND_HALF_UP
     */
    public static double sum(double one, double two, int scale, boolean forceScale, int roundingMode) {
        BigDecimal valueOne = BigDecimal.valueOf(one);
        BigDecimal valueTwo = BigDecimal.valueOf(two);
        if (scale < 0) {
            scale = 0;
        }
        if (forceScale) {
            return valueOne.add(valueTwo).setScale(scale, roundingMode).doubleValue();
        }
        return valueOne.add(valueTwo).doubleValue();
    }

    /**
     * 浮点数字相减
     *
     * @param one	运算数1，被加数或被乘数或被除数或被减数
     * @param two	运算数2，加，乘除或减数
     */
    public static double reduce(double one, double two) {
        return reduce(one, two, 2, false, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 浮点数字相减
     *
     * @param one	运算数1，被加数或被乘数或被除数或被减数
     * @param two	运算数2，加，乘除或减数
     * @param scale	表示表示除法需要精确到小数点以后几位
     */
    public static double reduce(double one, double two, int scale) {
        return reduce(one, two, scale, false, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 浮点数字相减
     *
     * @param one	运算数1，被加数或被乘数或被除数或被减数
     * @param two	运算数2，加，乘除或减数
     * @param scale	表示表示除法需要精确到小数点以后几位
     * @param forceScale 所有操作是否强制精确小数点
     * @param roundingMode 默认为ROUND_HALF_UP
     */
    public static double reduce(double one, double two, int scale, boolean forceScale, int roundingMode) {
        BigDecimal valueOne = BigDecimal.valueOf(one);
        BigDecimal valueTwo = BigDecimal.valueOf(two);
        if (scale < 0) {
            scale = 0;
        }
        if (forceScale) {
            return valueOne.subtract(valueTwo).setScale(scale, roundingMode).doubleValue();
        }
        return valueOne.subtract(valueTwo).doubleValue();
    }

    /**
     * 浮点数字相乘
     *
     * @param one 运算数1，被加数或被乘数或被除数或被减数
     * @param two 运算数2，加，乘除或减数
     */
    public static double multiply(double one, double two) {
        return multiply(one, two, 2, false, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 浮点数字相乘
     *
     * @param one	运算数1，被加数或被乘数或被除数或被减数
     * @param two	运算数2，加，乘除或减数
     * @param scale	表示表示除法需要精确到小数点以后几位
     */
    public static double multiply(double one, double two, int scale) {
        return multiply(one, two, scale, false, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 浮点数字相乘
     *
     * @param one	运算数1，被加数或被乘数或被除数或被减数
     * @param two	运算数2，加，乘除或减数
     * @param scale	表示表示除法需要精确到小数点以后几位
     * @param forceScale 所有操作是否强制精确小数点
     * @param roundingMode 默认为ROUND_HALF_UP
     */
    public static double multiply(double one, double two, int scale, boolean forceScale, int roundingMode) {
        BigDecimal valueOne = BigDecimal.valueOf(one);
        BigDecimal valueTwo = BigDecimal.valueOf(two);
        if (scale < 0) {
            scale = 0;
        }
        if (forceScale) {
            return valueOne.multiply(valueTwo).setScale(scale, roundingMode).doubleValue();
        }
        return valueOne.multiply(valueTwo).doubleValue();
    }

    /**
     * 浮点数字相除
     *
     * @param one 运算数1，被加数或被乘数或被除数或被减数
     * @param two 运算数2，加，乘除或减数
     */
    public static double divide(double one, double two) {
        return divide(one, two, 2, false, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 浮点数字相除
     *
     * @param one	运算数1，被加数或被乘数或被除数或被减数
     * @param two	运算数2，加，乘除或减数
     * @param scale	表示表示除法需要精确到小数点以后几位
     */
    public static double divide(double one, double two, int scale) {
        return divide(one, two, scale, false, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 浮点数字相除
     *
     * @param one	运算数1，被加数或被乘数或被除数或被减数
     * @param two	运算数2，加，乘除或减数
     * @param scale	表示表示除法需要精确到小数点以后几位
     * @param forceScale 所有操作是否强制精确小数点
     * @param roundingMode 默认为ROUND_HALF_UP
     */
    public static double divide(double one, double two, int scale, boolean forceScale, int roundingMode) {
        BigDecimal valueOne = BigDecimal.valueOf(one);
        BigDecimal valueTwo = BigDecimal.valueOf(two);
        if (scale < 0) {
            scale = 0;
        }
        if (forceScale) {
            return valueOne.divide(valueTwo).setScale(scale, roundingMode).doubleValue();
        }
        return valueOne.divide(valueTwo).doubleValue();
    }

    /**
     * 提供精确的小数位四舍五入处理
     * 
     * @param  value 需要四舍五入的数字
     * @param  scale 小数点后保留几位
     * @return 四舍五入后的结果
     */
    public static double round(double value, int scale) {
        if (scale < 0) {
            scale = 0;
        }

        BigDecimal b = new BigDecimal(Double.toString(value));
        BigDecimal one = new BigDecimal("1");
        return b.divide(one, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 提供向下取整的处理
     * @param  value 需要向下取整
     * @param  scale 小数点后保留几位
     * @return 取整后的结果
     */
    public static double ceil(double value, int scale) {
        if (scale < 0) {
            scale = 0;
        }

        BigDecimal b = new BigDecimal(Double.toString(value));
        BigDecimal one = new BigDecimal("1");
        return b.divide(one, scale, BigDecimal.ROUND_DOWN).doubleValue();
    }
}
