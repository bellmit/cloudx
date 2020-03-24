package cloud.apposs.util;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 数据加密工具类
 */
public class Encryptor {
    private static MessageDigest md5;

    /** 算法引擎 */
    static {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
        }
    }

    /**
     * MD5加密
     *
     * @param  str 要加密的字符串
     * @return 加密后的字符串
     */
    public static String md5(String str) {
        char[] charArray = str.toCharArray();
        byte[] byteArray = new byte[charArray.length];
        for (int i = 0; i < charArray.length; i++) {
            byteArray[i] = (byte) charArray[i];
        }

        byte[] md5Bytes = md5.digest(byteArray);
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int value = ((int) md5Bytes[i]) & 0xff;
            if (value < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(value));
        }

        return hexValue.toString();
    }

    public static byte[] encryptStringDES(String src, String key) {
        return encryptBytesDES(src.getBytes(), key);
    }

    public static byte[] encryptBytesDES(byte[] src, String key) {
        return encryptBytesDES(src, src.length, key);
    }

    public static byte[] encryptBytesDES(ByteBuffer buffer, String key) {
        int len = buffer.limit();
        byte[] array = buffer.array();
        return encryptBytesDES(array, len, key);
    }

    public static byte[] encryptBytesDES(byte[] src, int len, String key) {
        try {
            DESKeySpec dks = new DESKeySpec(key.getBytes());
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey securekey = keyFactory.generateSecret(dks);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, securekey);
            return cipher.doFinal(src, 0, len);
        } catch (Exception e) {
            return null;
        }
    }

    public static String decryptStringDES(byte[] srcBytes, String key) {
        return new String(decryptBytesDES(srcBytes, key));
    }

    public static byte[] decryptBytesDES(byte[] srcBytes, String key) {
        try {
            DESKeySpec dks = new DESKeySpec(key.getBytes());
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey securekey = keyFactory.generateSecret(dks);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, securekey);
            return cipher.doFinal(srcBytes);
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] sha1byte(byte[] src, int pos, int length) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            sha1.update(src, pos, length);
            return sha1.digest();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getFileMD5(File file) {
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(16);
    }

    public static String salt(String salt, String target) {
        return md5(salt + target);
    }

    /**
     * 随机生成密钥
     */
    public static String getAesKey() {
        String base = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 16; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return md5(sb.toString() + "_faisco");
    }

    /**
     * AES加密
     *
     * @param  content    待加密的内容
     * @param  encryptKey 加密密钥
     * @return 加密后的byte[]再经过base64 encode
     */
    public static String aesEncryptToBase64String(String content, String encryptKey) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed(encryptKey.getBytes());
        kgen.init(128, random);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(kgen.generateKey().getEncoded(), "AES"));
        return Base64.encodeBytes(cipher.doFinal(content.getBytes("utf-8")));
    }

    /**
     * AES解密
     *
     * @param  encryptString 待解密的String
     * @param  decryptKey 解密密钥
     * @return 解密后的String
     */
    public static String aesDecryptByBase64String(String encryptString, String decryptKey) throws Exception {
        byte[] encryptBytes = Base64.decode(encryptString);
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed(decryptKey.getBytes());
        kgen.init(128, random);

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(kgen.generateKey().getEncoded(), "AES"));
        byte[] decryptBytes = cipher.doFinal(encryptBytes);

        return new String(decryptBytes);
    }
}