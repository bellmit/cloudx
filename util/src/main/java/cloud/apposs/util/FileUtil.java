package cloud.apposs.util;

import cloud.apposs.util.IoUtil.LineProcessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

/**
 * 文件操作工具类
 */
public final class FileUtil {
    private static final int TEMP_DIR_ATTEMPTS = 3;

    public static String readString(File file) {
        return readString(file, CharsetUtil.UTF_8);
    }

    /**
     * 读取文件文本内容
     */
    public static final String readString(File file, Charset charset) {
        SysUtil.checkNotNull(file);
        SysUtil.checkNotNull(charset);

        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            return null;
        }
        byte[] content = new byte[(int) length];
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            in.read(content);
            return new String(content, charset);
        } catch (IOException e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * 读取文件字节内容
     */
    public static final byte[] readByte(File file) {
        SysUtil.checkNotNull(file);

        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            return null;
        }
        byte[] content = new byte[(int) length];
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            in.read(content);
            return content;
        } catch (IOException e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * 添加文件内容
     *
     * @param  content 文件内容
     * @param  file 文件句柄
     * @return 成功添加返回true
     */
    public static final boolean write(String content, File file) {
        return write(content, file, false);
    }

    /**
     * 添加文件内容
     *
     * @param  content 文件内容
     * @param  file 文件句柄
     * @param  append 是否在文件末尾添加内容
     * @return 成功添加返回true
     */
    public static final boolean write(String content, File file, boolean append) {
        SysUtil.checkNotNull(content);
        SysUtil.checkNotNull(file);

        FileWriter writer = null;
        try {
            writer = new FileWriter(file, append);
            writer.write(content);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return 存在为true, 不存在为false
     */
    public static final boolean exists(String filePath) {
        File file = new File(filePath);
        if (file.isFile()) {
            return file.exists();
        }
        return false;
    }

    /**
     * 检查是否存在目录
     *
     * @param folderPath 目录路径
     * @return 是目录返回true
     */
    public static final boolean isDirectory(String folderPath) {
        File f = new File(folderPath);
        if (f.isFile()) {
            return false;
        } else {
            return f.exists();
        }
    }

    /**
     * 检查是否存在文件
     *
     * @param filePath 文件路径
     * @return 是文件返回true
     */
    public static final boolean isFile(String filePath) {
        File f = new File(filePath);
        return f.exists() && f.isFile();
    }

    /**
     * 删除文件
     *
     * @param file 文件对象
     * @return 成功删除返回true
     */
    public static final boolean delete(File file) {
        if (file != null && file.exists() && file.isFile()) {
            return file.delete();
        }
        return false;
    }

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return 成功删除返回true
     */
    public static final boolean delete(String filePath) {
        return delete(new File(filePath));
    }

    /**
     * 创建文件夹(如果文件夹不存在)
     *
     * @param folderPath 目录路径
     * @return 创建成功返回true
     */
    public static final boolean mkdirs(String folderPath) {
        if (folderPath == null || "".equals(folderPath)) {
            return false;
        }
        File file = new File(folderPath);
        if (file.exists()) {
            return false;
        }
        file.mkdirs();
        return true;
    }

    /**
     * 创建一个空文件(如果不存在该文件夹则顺便创建该文件夹)
     *
     * @param filePath 文件路径
     * @return 创建成功返回true
     */
    public static final boolean create(String filePath) {
        if (filePath == null || "".equals(filePath)) {
            return false;
        }
        File file = new File(filePath);
        return FileUtil.create(file);
    }

    /**
     * 创建一个空文件(如果不存在该文件夹则顺便创建该文件夹)
     *
     * @param file 文件对象
     * @return 创建成功返回true
     */
    public static final boolean create(File file) {
        if (file == null || file.exists()) {
            return false;
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * 创建一个带内容的文件
     *
     * @param filePath 文件路径
     * @param content  内容
     * @return 创建成功返回true
     */
    public static final boolean create(String filePath, String content) {
        return FileUtil.create(filePath, content, CharsetUtil.UTF_8);
    }

    /**
     * 创建一个带内容的文件
     *
     * @param filePath 文件路径
     * @param content  内容
     * @param charset 编码
     * @return 创建成功返回true
     */
    public static final boolean create(String filePath, String content, Charset charset) {
        PrintWriter pw = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                FileUtil.create(file);
            }
            pw = new PrintWriter(filePath, charset.name());
            pw.print(content);
        } catch (Exception e) {
            return false;
        } finally {
            if (pw != null) {
                pw.close();
                pw = null;
            }
        }
        return true;
    }

    /**
     * 创建临时文件
     *
     * @return 成功创建的临时文件
     */
    public static File createTempDir() {
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        String baseName = System.currentTimeMillis() + "-";

        for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
            File tempDir = new File(baseDir, baseName + counter);
            if (tempDir.mkdir()) {
                return tempDir;
            }
        }
        throw new IllegalStateException("Failed to create tmp directory " + baseDir);
    }

    /**
     * 更新文件为最新修改时间
     */
    public static final void touch(File file) {
        SysUtil.checkNotNull(file);
        file.setLastModified(System.currentTimeMillis());
    }

    /**
     * 构造一个表示指定路径名的 file:URI
     *
     * @param filePath 文件路径
     * @return 文件URI
     */
    public static final String toURI(String filePath) {
        return toURI(new File(filePath));
    }

    /**
     * 构造一个表示指定路径名的 file:URI
     *
     * @param  file 文件
     * @return 文件URI
     */
    public static final String toURI(File file) {
        String uri = "";
        try {
            uri = file.toURI().toURL().toString();
        } catch (MalformedURLException e) {
            return null;
        }
        return uri;
    }

    /**
     * 实现文件的每行数据读取
     */
    public static final List<String> readLines(File file) {
        return readLines(file, CharsetUtil.UTF_8);
    }

    /**
     * 实现文件的每行数据读取
     */
    public static final List<String> readLines(File file, Charset charset) {
        return readLines(file, charset, new LineProcessor<List<String>>() {
            final List<String> result = new LinkedList<String>();
            @Override
            public boolean processLine(String line) throws IOException {
                result.add(line);
                return true;
            }
            @Override
            public void onError(IOException error) {
            }
            @Override
            public List<String> getResult() {
                return result;
            }
        });
    }

    /**
     * 实现文件的每行数据读取
     */
    public static final <T> T readLines(File file, LineProcessor<T> processor) {
        return readLines(file, CharsetUtil.UTF_8, processor);
    }

    public static final <T> T readLines(File file, Charset charset, LineProcessor<T> processor) {
        SysUtil.checkNotNull(file);

        try {
            return IoUtil.readLines(new FileInputStream(file), charset, processor);
        } catch (IOException e) {
            processor.onError(e);
        }
        return processor.getResult();
    }
}
