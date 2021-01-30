package cloud.apposs.util;

import org.junit.Test;

public class TestIdWorker {
    @Test
    public void testIdWorker() {
        IdWorker idWorker = IdWorker.builder(1, 31);
        for (int i = 0; i < 10; i++) {
            long id = idWorker.nextId();
            System.out.println(Long.toBinaryString(id) + " " + id);
        }
    }
}
