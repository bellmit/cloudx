package cloud.apposs.util;

import org.junit.Assert;
import org.junit.Test;

public class TestParam {
    @Test
    public void testParseJsonParam() {
        Param param = JsonUtil.parseJsonParam("{'name':'qun', 'id':1}");
        Assert.assertTrue(param.getInt("id") == 1);
    }
}
