package cloud.apposs.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import cloud.apposs.util.NetUtil.NetInterface;

public class TestNetUtil {
    @Test
    public void getLocalAddressList() {
        List<NetInterface> addressList = NetUtil.getLocalAddressList();
        Assert.assertTrue(!addressList.isEmpty());
    }

    @Test
    public void getLocalAddressInfo() {
        Map<String, NetInterface> addressInfo = NetUtil.getLocalAddressInfo();
        Assert.assertTrue(!addressInfo.isEmpty());
    }
}
