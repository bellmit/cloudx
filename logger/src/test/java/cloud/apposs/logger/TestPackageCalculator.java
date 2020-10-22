package cloud.apposs.logger;

import org.junit.Assert;
import org.junit.Test;

public class TestPackageCalculator {
    @Test
    public void testGetClassLocation() {
        PackageCalculator pc = new PackageCalculator();
        String classLocation = pc.getClassLocation(Log.class);
        Assert.assertNotNull(classLocation);
        String classVersion = pc.getImplementationVersion(Log.class);
        Assert.assertNotNull(classVersion);
    }

    @Test
    public void testGetStraceTraceFrames() {
        PackageCalculator pc = new PackageCalculator();
        String string = pc.printStraceTraceFrames(new Throwable().getStackTrace());
        Assert.assertNotNull(string);
        String[] frames = pc.getStraceTraceFrames(new Throwable().getStackTrace());
        Assert.assertTrue(frames.length > 0);
    }
}
