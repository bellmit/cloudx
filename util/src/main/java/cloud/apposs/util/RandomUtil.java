package cloud.apposs.util;

import java.util.Random;

/**
 * 随机生成数据工具类
 */
public final class RandomUtil {
    /**
     * 随机范围内数字
     */
    public static Integer getRandomInt(int start, int end) {
        return new Random().nextInt(end - start) + start;
    }

    /**
     * 获取随机字符串
     */
    public static String getRandomString(int length){
        String fullStr = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer randomStr = new StringBuffer();
        for(int i = 0; i < length; i++){
            int number = random.nextInt(62);
            randomStr.append(fullStr.charAt(number));
        }
        return randomStr.toString();
    }
}
