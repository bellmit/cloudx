package cloud.apposs.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件类型操作
 */
public enum FileType {
    TYPE_UNKNOW(0, "unknow"),
    TYPE_JPG(1, "jpg"),
    TYPE_JPEG(2, "jpeg"),
    TYPE_PNG(3, "png"),
    TYPE_GIF(4, "gif"),
    TYPE_TXT(5, "txt"),
    TYPE_AVI(6, "avi"),
    TYPE_EXE(7, "exe"),
    ;
    /**
     * 文件类型
     */
    private final int type;

    /**
     * 文件后缀
     */
    private final String name;

    private static final Map<Integer, FileType> types = new HashMap<Integer, FileType>();
    static {
        types.put(TYPE_JPG.getType(), TYPE_JPG);
        types.put(TYPE_JPEG.getType(), TYPE_JPEG);
        types.put(TYPE_TXT.getType(), TYPE_TXT);
        types.put(TYPE_AVI.getType(), TYPE_AVI);
        types.put(TYPE_EXE.getType(), TYPE_EXE);
    }

    private FileType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据文件类型获取文件类型对象
     */
    public static FileType getFileType(int fileType) {
        return types.get(fileType);
    }

    /**
     * 根据文件类型获取文件类型对象
     */
    public static FileType getFileType(String fileName) {
        String fileSuffix = getFileSuffix(fileName);
        for (FileType fileType : types.values()) {
            if (fileSuffix.equalsIgnoreCase(fileType.getName())) {
                return fileType;
            }
        }
        return TYPE_UNKNOW;
    }

    /**
     * 判断文件类型是否为图片类型
     */
    public static boolean isImgType(int fileType) {
        if (fileType == TYPE_JPG.type || fileType == TYPE_JPEG.type
                || fileType == TYPE_PNG.type || fileType == TYPE_GIF.type) {
            return true;
        }
        return false;
    }

    /**
     * 获取文件后缀
     */
    public static String getFileSuffix(File file) {
        if (file == null) {
            throw new IllegalArgumentException("file");
        }
        String name = file.getName();
        int pos = name.lastIndexOf('.');
        if ((pos < 0) || (pos >= name.length())) {
            return "";
        }
        return name.substring(pos + 1);
    }

    /**
     * 获取文件的后缀
     */
    public static String getFileSuffix(String filePath) {
        if (StrUtil.isEmpty(filePath)) {
            throw new IllegalArgumentException("file");
        }
        String name = getFileName(filePath);
        int pos = name.lastIndexOf('.');
        if ((pos < 0) || (pos >= name.length())) {
            return "";
        }
        return name.substring(pos + 1);
    }

    /**
     * 获取文件名
     */
    public static String getFileName(String filePath) {
        if (StrUtil.isEmpty(filePath)) {
            throw new IllegalArgumentException("filePath");
        }
        String name = filePath.replace('\\', '/');
        int pos = name.lastIndexOf('/');
        if (pos < 0) {
            return name;
        }
        return name.substring(pos + 1);
    }
}
