package cloud.apposs.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public final class CachedFileStream {
	public static final int DEFAULT_THRESHOLD = Integer.MAX_VALUE;
	public static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
	public static final String DEFAULT_TMP_FILE_PREFIX = "tmp_";
	public static final String DEFAULT_TMP_FILE_SUFFIX = "";
	
	/** 写入文件的最大字节阀值 */
	private final int threshold;
	
	/** 是否已经超过最大字节数 */
	private boolean exceeded = false;
	
	/** 字节流 */
    private OutputStream stream;
    
    /** 存储文件 */
    private File file;
    /** 临时文件存储相关配置 */
    private final String prefix;
    private final String suffix;
    private final File directory;
    
    /** 写入的字节数 */
	private long written;
	
	public CachedFileStream() {
		this(DEFAULT_THRESHOLD, null, null, null);
	}
	
	public CachedFileStream(int threshold, File directory) {
		this(threshold, DEFAULT_TMP_FILE_PREFIX, DEFAULT_TMP_FILE_SUFFIX, directory);
	}
	
	public CachedFileStream(int threshold, String prefix, File directory) {
		this(threshold, prefix, DEFAULT_TMP_FILE_SUFFIX, directory);
	}
	
	public CachedFileStream(int threshold, String prefix, String suffix, File directory) {
		this.threshold = threshold;
		this.stream = new ByteArrayOutputStream();
		this.prefix = prefix;
		this.suffix = suffix;
		this.directory = directory;
	}

	public File getFile() {
        return file;
    }
	
	public boolean isThresholdExceeded() {
        return written > threshold;
    }
	
	public boolean isInMemory() {
        return !isThresholdExceeded();
    }
	
	public byte[] getRawData() {
		if (isInMemory()) {
			ByteArrayOutputStream baos = (ByteArrayOutputStream) stream;
			return baos.toByteArray();
		}
		return null;
	}
	
	public long write(int b) throws IOException {
		checkThreshold(1);
		stream.write(b);
		written++;
		return 1;
	}
	
	public long write(String str) throws IOException {
		SysUtil.checkNotNull(str, "str");
		return write(str.getBytes());
	}
	
	public long write(String str, String charset) throws IOException {
		SysUtil.checkNotNull(str, "str");
		return write(str.getBytes(charset));
	}
	
    public long write(byte b[]) throws IOException {
        checkThreshold(b.length);
        stream.write(b);
        written += b.length;
        return b.length;
    }
	
    public long write(byte b[], int off, int len) throws IOException {
        checkThreshold(len);
        stream.write(b, off, len);
        written += len;
        return len;
    }
    
    public long write(ByteBuffer src) throws IOException {
    	return write(src, src.position(), src.limit());
    }
    
    public long write(ByteBuffer src, int off, int len) throws IOException {
        checkThreshold(len);
        for (int i = off; i < len; i++) {
    	    stream.write(src.get(i));
        }
        written += len;
        return len;
    }
    
    public long write(File src) throws IOException {
    	long len = src.length();
    	checkThreshold(len);
    	FileInputStream fis = null;
    	try {
    		int n;
    		long count = 0;
    		fis = new FileInputStream(src);
    		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    		while ((n = fis.read(buffer)) != -1) {
            	stream.write(buffer, 0, n);
            	count += n;
            }
    		written += count;
        	return count;
    	} finally {
    		if (fis != null) {
        		try {
            		fis.close();
            	} catch(IOException ignore) {
            	}
        	}
    	}
    }
	
	public long transfer(OutputStream out) throws IOException {
        if(isInMemory()) {
        	ByteArrayOutputStream baos = (ByteArrayOutputStream) stream;
        	int count = baos.size();
        	baos.writeTo(out);
        	return count;
        } else {
            FileInputStream fis = null;
            try {
            	int n;
            	long count = 0;
            	fis = new FileInputStream(file);
                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                while ((n = fis.read(buffer)) != -1) {
                	out.write(buffer, 0, n);
                	count += n;
                }
                return count;
            } finally {
            	if (fis != null) {
            		try {
                		fis.close();
                	} catch(IOException ignore) {
                	}
            	}
            }
        }
    }
	
	public long transfer(File dst) throws IOException {
        if(isInMemory()) {
        	FileOutputStream fos = new FileOutputStream(dst);
        	try {
        		ByteArrayOutputStream baos = (ByteArrayOutputStream) stream;
        		int count = baos.size();
        		baos.writeTo(fos);
        		return count;
        	} finally {
        		try {
        			fos.close();
            	} catch(IOException ignore) {
            	}
        	}
        } else {
            FileInputStream fis = null;
            FileOutputStream fos = null;
            FileChannel fisChannel = null;
            FileChannel fosChannel = null;
            try {
            	fis = new FileInputStream(file);
            	fos = new FileOutputStream(dst);
            	fisChannel = fis.getChannel();
                fosChannel = fos.getChannel();
                
                long trans = 0;
                long offset = 0;
                long total = fisChannel.size();
                long max = DEFAULT_BUFFER_SIZE * 100;
                while (trans < total) {
        			// 文件之间的传输也要配置一次传输缓冲，避免一开始传输量过大也会导致OOM
                	long remain = total - trans;
        			long needTrans = max > remain ? remain : max;
        			long length = fisChannel.transferTo(offset, needTrans, fosChannel);
        			trans += length;
        			offset += length;
        		}
                return trans;
            } finally {
            	if (fisChannel != null) {
                    try {
                        fisChannel.close();
                    } catch (IOException ignore) {
                    }
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ignore) {
                    }
                }
                if (fosChannel != null) {
                    try {
                        fosChannel.close();
                    } catch (IOException ignore) {
                    }
                }
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }
    }
	
	public long rename(File dst) throws IOException {
		if(isInMemory()) {
        	FileOutputStream fos = new FileOutputStream(dst);
        	try {
        		ByteArrayOutputStream baos = (ByteArrayOutputStream) stream;
        		int count = baos.size();
        		baos.writeTo(fos);
        		return count;
        	} finally {
        		try {
        			fos.close();
            	} catch(IOException ignore) {
            	}
        	}
        } else {
        	if (file.renameTo(dst)) {
        		return file.length();
        	}
        	return -1;
        }
	}
	
	public void flush() throws IOException {
		stream.flush();
	}

	public void close() {
		try {
			flush();
		} catch (final IOException ignored) {
        }
		try {
			stream.close();
		} catch (final IOException ignored) {
        }
		if (file != null && file.exists()) {
			file.delete();
		}
	}
	
	private void checkThreshold(long count) throws IOException {
        if (!exceeded && written + count > threshold) {
            exceeded = true;
            if (prefix != null) {
                file = File.createTempFile(prefix, suffix, directory);
            }
            final FileOutputStream fos = new FileOutputStream(file);
            try {
                ((ByteArrayOutputStream) stream).writeTo(fos);
            } catch (IOException e){
                fos.close();
                throw e;
            }
            stream = fos;
        }
    }
}
