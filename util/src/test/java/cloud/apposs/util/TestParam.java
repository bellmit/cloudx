package cloud.apposs.util;

import org.junit.Assert;
import org.junit.Test;

public class TestParam {
    @Test
    public void testParseJsonParam() {
        Param param = JsonUtil.parseJsonParam("{'name':'qun', 'id':1}");
        Assert.assertTrue(param.getInt("id") == 1);
    }

    @Test
    public void testToHtmlJson() {
        String str = "{\"id\":1,\"value\":\"<script>alert(123)</script>\"}";
        Param param = JsonUtil.parseJsonParam(str);
        System.out.println(param.toHtmlJson());
    }
}
