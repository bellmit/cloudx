package cloud.apposs.logger.appender;

import cloud.apposs.logger.Appender;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 文件输出
 */
public class FileAppender extends Appender {
	private String filename;
	
	private String datepattern;
	
	private String filesurfix;
	
	private String lastFile;
	
	private OutputStreamWriter fw;
	
	public FileAppender(String file) {
		lastFile = parseFile(file);
		try {
			closeFw();
			FileOutputStream out = new FileOutputStream(lastFile, true);
			fw = new OutputStreamWriter(out, "UTF-8");
		} catch(FileNotFoundException e) {
			// 有可能是目录不存在，尝试创建目录
			String parentName = new File(lastFile).getParent();
			if (parentName != null) {
				File parentDir = new File(parentName);
				if (!parentDir.exists() && parentDir.mkdirs()) {
					try {
						FileOutputStream out = new FileOutputStream(lastFile, true);
						fw = new OutputStreamWriter(out, "UTF-8");
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			} else {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void append(List<String> msgList) {
		try {
			String file = getFile();
			if (!file.equals(lastFile)) {
				closeFw();
				lastFile = file;
				FileOutputStream out = new FileOutputStream(lastFile, true);
				fw = new OutputStreamWriter(out, "UTF-8");
			}
			for (String msg : msgList) {
				fw.write(msg);
			}
			fw.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String parseFile(String file) {
		if (file == null) {
			throw new IllegalArgumentException("file");
		}
		
		int i = 0;
		i = file.lastIndexOf(".");
		if (i != -1) {
			filename = file.substring(0, i);
			filesurfix = file.substring(i + 1, file.length());
		} else {
			filename = file;
		}
		
		i = filename.indexOf("{");
		if(i != -1) {
			int end = filename.indexOf('}', i);
			if (end > i) {
				filename = filename.substring(0, i);
				datepattern = file.substring(i + 1, end);
			}
		}
		
		return getFile();
	}

	public String getFile() {
		String file = filename;
		if (datepattern != null) {
			SimpleDateFormat df = new SimpleDateFormat(datepattern);
			String date = df.format(new Date());
			file += date;
		}
		if (filesurfix != null) {
			file += "." + filesurfix;
		}
		return file;
	}
	
	@Override
	public void close() {
		closeFw();
	}
	
	private void closeFw() {
		try {
			if (fw != null) {
				fw.close();
				fw = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
