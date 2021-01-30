package cloud.apposs.webx.upload;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;

/**
 * HTTP表单文件上传字段和文件数据封装
 */
public final class MultiFormRequest {
    /**
     * 表单参数列表
     */
    private final Map<String, String> parameters = new HashMap<String, String>();

    /**
     * 表单上传的文件列表
     */
    private final Map<String, FileItem> files = new HashMap<String, FileItem>();

    /**
     * 添加表单字段
     * @param key 表单KEY
     * @param value 表单值
     */
    public void addParameter(String key, String value) {
        parameters.put(key, value);
    }

    /**
     * 获取表单参数
     */
    public String getParameter(String key) {
        return parameters.get(key);
    }

    /**
     * 获取所有表单参数
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * 添加上传的文件数据
     * @param key 文件名称
     * @param file 文件数据
     */
    public void addFile(String key, FileItem file) {
        files.put(key, file);
    }

    /**
     * 获取所有表单文件
     */
    public Map<String, FileItem> getFiles() {
        return files;
    }

    /**
     * 获取表单文件
     */
    public FileItem getFile(String key) {
        return files.get(key);
    }
}
