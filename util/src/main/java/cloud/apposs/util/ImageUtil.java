package cloud.apposs.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;

public class ImageUtil {
    public static final String DEFAULT_IMAGE_FORMAT = "png";

    /**
     * BASE64字符串转图片
     *
     * @param base64String 图片BASE64编码串
     */
    public static BufferedImage getBase64StrToImage(String base64String) throws IOException {
        java.util.Base64.Decoder decoder = Base64.getDecoder();
        byte[] bytes = decoder.decode(base64String);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        return ImageIO.read(inputStream);
    }

    public static String getImageToBase64Str(BufferedImage bufferedImage) throws IOException {
        return getImageToBase64Str(bufferedImage, DEFAULT_IMAGE_FORMAT);
    }

    /**
     * 图像对象转换为BASE64字符串
     *
     * @param bufferedImage 图像对象
     * @param format 格式化的图片，png/jpg等
     */
    public static String getImageToBase64Str(BufferedImage bufferedImage, String format) throws IOException {
        byte[] data = getImageToBytes(bufferedImage, format);
        java.util.Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(data);
    }

    public static byte[] getImageToBytes(BufferedImage bufferedImage) throws IOException {
        return getImageToBytes(bufferedImage, DEFAULT_IMAGE_FORMAT);
    }

    /**
     * 图像对象转换为字节码
     *
     * @param bufferedImage 图像对象
     * @param format 格式化的图片，png/jpg等
     */
    public static byte[] getImageToBytes(BufferedImage bufferedImage, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, format, out);
        return out.toByteArray();
    }

    public static WaterMarkImage waterMark(Image image, Image mark) throws IOException {
        return waterMark(image, mark, DEFAULT_IMAGE_FORMAT);
    }

    /**
     * 给图片添加图片水印，并返回初始化信息
     *
     * @param image 源图片，要添加水印的图片
     * @param mark 水印图片
     */
    public static WaterMarkImage waterMark(Image image, Image mark, String format) throws IOException {
        int imageWidth = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        int markWidth = mark.getWidth(null);
        int markHeight = mark.getHeight(null);
        int x = getRandomMarkX(imageWidth, markWidth);
        int y = getRandomMarkY(imageHeight, markHeight);
        BufferedImage bufferedImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        waterMark(bufferedImage, image, mark, markWidth, markHeight, x, y);
        String imgBase64Str = getImageToBase64Str(bufferedImage, format);

        WaterMarkImage markImageInfo = new WaterMarkImage();
        markImageInfo.setMarkPosX(x);
        markImageInfo.setMarkPosY(y);
        markImageInfo.setImageWidth(imageWidth);
        markImageInfo.setImageHeight(imageHeight);
        markImageInfo.setMarkWidth(markWidth);
        markImageInfo.setImageHeight(markHeight);
        markImageInfo.setImageBase64Str(imgBase64Str);
        return markImageInfo;
    }

    /**
     * 加图片水印
     */
    public static void waterMark(BufferedImage bufferedImage, Image img, Image markImg, int width, int height, int x, int y) {
        Graphics2D g = bufferedImage.createGraphics();
        g.drawImage(img, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), null);
        g.drawImage(markImg, x, y, width, height, null);
        g.dispose();
    }

    /**
     * 随机返回水印X坐标, 取值范围：水印宽+水印宽/2 <= X <= 底图宽-水印宽-水印宽/2
     * 水印距离底图左边最短距离（以底图左顶点为原点，水印图片左顶点为绘图原点）
     *
     * @param imgWidth 底图宽
     * @param markWidth 水印宽
     */
    public static int getRandomMarkX(int imgWidth, int markWidth) {
        Random random = new Random();
        int minX = markWidth + markWidth / 2;
        int maxX = imgWidth - markWidth - markWidth / 2;
        int X = random.nextInt(maxX - minX + 1) + minX;
        return X;
    }

    /**
     * 随机返回水印X坐标, 取值范围：水印高/4 <= Y <= 底图高-水印高-水印高/4
     * 水印距离底图左边最短距离（以底图左顶点为原点，水印图片左顶点为绘图原点）
     *
     * @param imgHeight 底图高
     * @param markHeight 水印高
     */
    public static int getRandomMarkY(int imgHeight, int markHeight) {
        Random random = new Random();
        int minY = markHeight / 4;
        int maxY = imgHeight - markHeight - markHeight / 4;
        int Y = random.nextInt(maxY - minY + 1) + minY;
        return Y;
    }

    public static class WaterMarkImage {
        private int markPosX;

        private int markPosY;

        private int imageWidth;

        private int imageHeight;

        private int markWidth;

        private int markHeight;

        private String imageBase64Str;

        public int getMarkPosX() {
            return markPosX;
        }

        public void setMarkPosX(int markPosX) {
            this.markPosX = markPosX;
        }

        public int getMarkPosY() {
            return markPosY;
        }

        public void setMarkPosY(int markPosY) {
            this.markPosY = markPosY;
        }

        public int getImageWidth() {
            return imageWidth;
        }

        public void setImageWidth(int imageWidth) {
            this.imageWidth = imageWidth;
        }

        public int getImageHeight() {
            return imageHeight;
        }

        public void setImageHeight(int imageHeight) {
            this.imageHeight = imageHeight;
        }

        public int getMarkWidth() {
            return markWidth;
        }

        public void setMarkWidth(int markWidth) {
            this.markWidth = markWidth;
        }

        public int getMarkHeight() {
            return markHeight;
        }

        public void setMarkHeight(int markHeight) {
            this.markHeight = markHeight;
        }

        public String getImageBase64Str() {
            return imageBase64Str;
        }

        public void setImageBase64Str(String imageBase64Str) {
            this.imageBase64Str = imageBase64Str;
        }
    }
}
