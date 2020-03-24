package cloud.apposs.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

public final class IoUtil {
    public static void close(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * 实现数据流的每行数据读取
     */
    public static final List<String> readLines(InputStream buffer) {
        return readLines(buffer, CharsetUtil.UTF_8);
    }

    /**
     * 实现数据流的每行数据读取
     */
    public static final List<String> readLines(InputStream buffer, Charset charset) {
        return readLines(buffer, charset, new LineProcessor<List<String>>() {
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

    public static final <T> T readLines(InputStream buffer, Charset charset, LineProcessor<T> processor) {
        SysUtil.checkNotNull(buffer);
        SysUtil.checkNotNull(charset);
        SysUtil.checkNotNull(processor);

        InputStreamReader ireader = null;
        BufferedReader breader = null;
        try {
            ireader = new InputStreamReader(buffer);
            breader = new BufferedReader(ireader);
            String line;
            while ((line = breader.readLine()) != null) {
                processor.processLine(line);
            }
        } catch (IOException e) {
            processor.onError(e);
        } finally {
            IoUtil.close(buffer);
            IoUtil.close(ireader);
            IoUtil.close(breader);
        }
        return processor.getResult();
    }

    public interface LineProcessor<T> {
        /**
         * 处理行数据
         */
        boolean processLine(String line) throws IOException;

        /**
         * 解析异常时的回调
         */
        void onError(IOException error);

        /**
         * 自定义返回的结果
         */
        T getResult();
    }
}
