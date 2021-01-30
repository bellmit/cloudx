package cloud.apposs.gateway;

import cloud.apposs.gateway.configure.Block;
import cloud.apposs.gateway.configure.BlockDirective;
import cloud.apposs.gateway.configure.ConfigParseException;
import cloud.apposs.gateway.configure.ConfigParser;
import cloud.apposs.util.FileUtil;
import org.junit.Test;

import java.io.File;
import java.util.List;

public class TestConfigParser {
    private static final String DIR = System.getProperty("user.dir") + "/target/classes/";

    @Test
    public void testConfigParseFile() throws ConfigParseException {
        String content = FileUtil.readString(new File(DIR + "gateway.conf"));
        List<Block> blockList = ConfigParser.parseConfig(content);
        GatewayConfig config = new GatewayConfig();
        BlockDirective.initialize(blockList, config);
        System.out.println(config);
    }
}
