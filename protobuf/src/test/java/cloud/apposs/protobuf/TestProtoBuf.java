package cloud.apposs.protobuf;

import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloud.apposs.util.Encoder;
import cloud.apposs.util.SysUtil;
import org.junit.Test;

import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

public class TestProtoBuf {
    @Test
    public void testPutInt() throws Exception {
        int value = Integer.MAX_VALUE;
        ProtoBuf buffer = ProtoBuf.allocate();
        System.out.println(buffer);
        buffer.putInt(value);
        System.out.println(buffer);
        assertTrue(value == buffer.getInt());
    }

    @Test
    public void testPutLong() throws Exception {
        long value = Long.MIN_VALUE;
        ProtoBuf buffer = ProtoBuf.allocate();
        buffer.putLong(value);
        assertTrue(value == buffer.getLong());
    }

    @Test
    public void testPutByte() throws Exception {
        byte value = -124;
        ProtoBuf buffer = ProtoBuf.allocate();
        buffer.putByte(value);
        assertTrue(value == buffer.getByte());
    }

    @Test
    public void testPutBoolean() throws Exception {
        boolean value = false;
        ProtoBuf buffer = ProtoBuf.allocate();
        buffer.putBoolean(value);
        assertTrue(value == buffer.getBoolean());
    }

    @Test
    public void testPutShort() throws Exception {
        short value = -12412;
        ProtoBuf buffer = ProtoBuf.allocate();
        buffer.putShort(value);
        assertTrue(value == buffer.getShort());
    }

    @Test
    public void testPutFloat() throws Exception {
        float value = -1008611.11F;
        ProtoBuf buffer = ProtoBuf.allocate();
        buffer.putFloat(value);
        assertTrue(value == buffer.getFloat());
    }

    @Test
    public void testPutDouble() throws Exception {
        double value = -10089909611.11789;
        ProtoBuf buffer = ProtoBuf.allocate();
        buffer.putDouble(value);
        assertTrue(value == buffer.getDouble());
    }

    @Test
    public void testPutString() throws Exception {
        String value = "Hello My Product中文";
        ProtoBuf buffer = ProtoBuf.allocate();
        buffer.putString(value);
        assertTrue(value.equals(buffer.getString()));
    }

    @Test
    public void testPutBuffer() throws Exception {
        ByteBuffer value = ByteBuffer.allocate(12);
        value.putInt(-889);
        value.flip();
        ProtoBuf buffer = ProtoBuf.allocate();
        buffer.putBuffer(value);
        assertTrue(value.getInt() == buffer.getBuffer().getInt());
    }

    @Test
    public void testPutCalendar() throws Exception {
        Calendar value = Calendar.getInstance();
        ProtoBuf buffer = ProtoBuf.allocate();
        buffer.putCalendar(value);
        assertTrue(value.getTimeInMillis() == buffer.getCalendar().getTimeInMillis());
    }

    @Test
    public void testPutObject() throws Exception {
        Product1 p1 = new Product1();
        p1.setId(-100876);
        ProtoBuf buffer = ProtoBuf.allocate();
        ProtoSchema schema = ProtoSchema.getSchema(Product1.class);
        buffer.putObject(p1, schema);
        Product1 p2 = buffer.getObject(Product1.class, schema);
        assertTrue(p1.toString().equals(p2.toString()));
    }

    @Test
    public void testPutObject2() throws Exception {
        Product2 p1 = new Product2();
        p1.setId(-100876);
        p1.setName("MyProdct2");
        Order order = new Order(99, "MyOrder");
        Rider rider = new Rider();
        rider.setName("nibi");
        rider.setPhone(138888888);
        order.setRider(rider);
        p1.setOrder(order);
        ProtoBuf buffer = ProtoBuf.allocate();
        ProtoSchema schema = ProtoSchema.getSchema(Product2.class);
        buffer.putObject(p1, schema);
        Product2 p2 = buffer.getObject(Product2.class, schema);
        assertTrue(p1.toString().equals(p2.toString()));
    }

    /**
     * 自定义对象Map Schema，以便于支持跨版本对象传输
     */
    @Test
    public void testPutObject3() throws Exception {
        Car c1 = new Car();
        c1.addThing("aa1", "thing1");
        c1.addThing("aa2", "thing2");

        ProtoSchema schema = ProtoSchema.getSchema(Car.class);
        ProtoSchema mapSchema = ProtoSchema.mapSchema();
        mapSchema.addKey("aa1", String.class);
        mapSchema.addKey("aa2", String.class);
        schema.addField("things", mapSchema);

        ProtoBuf buffer = ProtoBuf.allocate();
        buffer.putObject(c1, schema);
        Car c2 = buffer.getObject(Car.class, schema);
        System.out.println(c2);
    }

    /**
     * 编写自定义对象Schema以便于兼容跨版本对象传递
     */
    @Test
    public void testPutObject4() throws Exception {
        Product1 p1 = new Product1();
        p1.setId(-100876);
        p1.setName("HelloProduct");
        ProtoBuf buffer = ProtoBuf.allocate();
        ProtoSchema schema = ProtoSchema.objectSchema(Product1.class);
        schema.addField("id");
        schema.addField("name");
        buffer.putObject(p1, schema);
        Product1 p2 = buffer.getObject(Product1.class, schema);
        assertTrue(p1.toString().equals(p2.toString()));
    }

    @Test
    public void testPutObjectBatch() throws Exception {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            Product1 p1 = new Product1();
            p1.setId(-100876);
            p1.setName("MyPhone");
            ProtoBuf buffer = ProtoBuf.allocate();
            ProtoSchema schema = ProtoSchema.getSchema(Product1.class);
            buffer.putObject(p1, schema);
            buffer.getObject(Product1.class, schema);
        }
        System.out.println("batch execute:" + (System.currentTimeMillis() - start));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPutMap() throws Exception {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", -1009189811);
        data.put("name", "MyElementfaa-@#afdafe");
        Map<String, Object> data2 = new HashMap<String, Object>();
        data2.put("id2", -1229189811);
        data2.put("name2", "2MyElementfaa-@#afdafe");
        data.put("data2", data2);
        Product1 p1 = new Product1();
        p1.setId(-1008767);
        p1.setName("MyPhone2");
        data.put("product", p1);

        ProtoSchema schema = ProtoSchema.mapSchema();
        schema.addKey("id", Integer.class);
        schema.addKey("name", String.class);
        ProtoSchema schema2 = ProtoSchema.mapSchema();
        schema2.addKey("id2", Integer.class);
        schema2.addKey("name2", String.class);
        schema.addKey("data2", Map.class, schema2);
        schema.addKey("product", Product1.class);

        ProtoBuf buffer = ProtoBuf.allocate(512);
        buffer.putMap(data, schema);
        Map data3 = buffer.getMap(schema);
        assertTrue(data.get("name").equals(data3.get("name")));
    }

    @Test
    public void testPutMap2() throws Exception {
        Map<String, String> value = new HashMap<String, String>();
        value.put("aa1", "value1");
        value.put("aa2", "value2");
        ProtoBuf buffer = ProtoBuf.allocate();
        ProtoSchema schema = ProtoSchema.getSchema(HashMap.class);
        buffer.putObject(value, schema);
        System.out.println(buffer.getObject(HashMap.class, schema));
    }

    @Test
    public void testPutMapBatch() throws Exception {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("id", -1009189811);
            data.put("name", "MyElementfaa-@#afdafe");

            ProtoSchema schema = ProtoSchema.mapSchema();
            schema.addKey("id", Integer.class);
            schema.addKey("name", String.class);

            ProtoBuf buffer = ProtoBuf.allocate(512);
            buffer.putMap(data, schema);
            buffer.getMap(schema);
        }
        System.out.println("batch map execute:" + (System.currentTimeMillis() - start));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPutList() throws Exception {
        List<Integer> list = new ArrayList<Integer>();
        list.add(1);
        list.add(-1008912);

        ProtoSchema schema = ProtoSchema.getSchema(list, Integer.class);

        ProtoBuf buffer = ProtoBuf.allocate();
        buffer.putList(list, schema);
        List<Integer> list2 = (List<Integer>) buffer.getList(schema);
        assertTrue(list.get(0) == list2.get(0));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPutList2() throws Exception {
        List<String> list = new ArrayList<String>();
        list.add("Hello");
        list.add("Words");

        ProtoSchema schema = ProtoSchema.getSchema(list, String.class);

        ProtoBuf buffer = ProtoBuf.allocate();
        buffer.putList(list, schema);
        List list2 = (List) buffer.getList(schema);
        System.out.println(list2);
        assertTrue(list.get(0).equals(list2.get(0)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPutList3() throws Exception {
        List<Product1> list = new ArrayList<Product1>();
        Product1 p1 = new Product1(1, "MyProduct1");
        Product1 p2 = new Product1(2, "MyProduct2");
        list.add(p1);
        list.add(p2);

        ProtoSchema schema = ProtoSchema.getSchema(list, Product1.class);

        ProtoBuf buffer = ProtoBuf.allocate();
        buffer.putList(list, schema);
        List list2 = (List) buffer.getList(schema);
        System.out.println(list2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPutList4() throws Exception {
        List<Map> list = new ArrayList<Map>();
        Map data = new HashMap();
        data.put("aa1", "hello");
        data.put("aa2", "world");
        Map data2 = new HashMap();
        data2.put("aa1", "hello1");
        data2.put("aa2", "world1");

        ProtoSchema schema0 = ProtoSchema.mapSchema();
        schema0.addKey("aa1", String.class);
        schema0.addKey("aa2", String.class);

        list.add(data);
        list.add(data2);

        ProtoSchema schema = ProtoSchema.getSchema(list, Map.class, schema0);

        ProtoBuf buffer = ProtoBuf.allocate();
        buffer.putList(list, schema);
        List list2 = (List) buffer.getList(schema);
        System.out.println(list2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPutList5() throws Exception {
        List<Map> list = new ArrayList<Map>();
        Map data = new HashMap();
        Product1 procduct1 = new Product1(1, "Product1");
        Product1 procduct2 = new Product1(2, "Product2");
        Product1 procduct3 = new Product1(3, "Product3");
        Product1 procduct4 = new Product1(4, "Product4");
        data.put("aa1", procduct1);
        data.put("aa2", procduct2);
        Map data2 = new HashMap();
        data2.put("aa1", procduct3);
        data2.put("aa2", procduct4);

        ProtoSchema schema0 = ProtoSchema.mapSchema();
        schema0.addKey("aa1", Product1.class);
        schema0.addKey("aa2", Product1.class);

        list.add(data);
        list.add(data2);

        ProtoSchema schema = ProtoSchema.getSchema(list, Map.class, schema0);

        ProtoBuf buffer = ProtoBuf.allocate();
        buffer.putList(list, schema);
        List list2 = (List) buffer.getList(schema);
        System.out.println(list2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPutList6() throws Exception {
        List list = new ArrayList();

        Product2 product = new Product2(1, "MyPhone");
        Order order = new Order(99, "MyOrder");
        Rider rider = new Rider();
        rider.setName("nibi");
        rider.setPhone(138888888);
        order.setRider(rider);
        product.setOrder(order);

        Product2 product2 = new Product2(2, "MyPhone2");
        Order order2 = new Order(992, "MyOrder2");
        Rider rider2 = new Rider();
        rider2.setName("nibi2");
        rider2.setPhone(138888888);
        order2.setRider(rider2);
        product2.setOrder(order2);

        list.add(product);
        list.add(product2);

        ProtoSchema schema = ProtoSchema.getSchema(list, Product2.class);

        ProtoBuf buffer = ProtoBuf.allocate();
        buffer.putList(list, schema);
        List list2 = (List) buffer.getList(schema);
        System.out.println(list2);
    }

    @Test
    public void testPutList7() throws Exception {
        List<Integer> value = new ArrayList<Integer>();
        value.add(100);
        value.add(879);
        ProtoBuf buffer = ProtoBuf.allocate();
        ProtoSchema schema = ProtoSchema.getSchema(ArrayList.class);
        buffer.putObject(value, schema);
        System.out.println(buffer.getObject(ArrayList.class, schema));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPutParam() throws Exception {
        Param data = new Param();
        data.put("id", -1009189811);
        data.put("name", "MyElementfaa-@#afdafe");
        Param data2 = new Param();
        data2.put("id2", -1229189811);
        data2.put("name2", "2MyElementfaa-@#afdafe");
        data.put("data2", data2);
        Product1 p1 = new Product1();
        p1.setId(-1008767);
        p1.setName("MyPhone2");
        data.put("product", p1);

        ProtoSchema schema = ProtoSchema.paramSchema();
        schema.addKey("id", Integer.class);
        schema.addKey("name", String.class);
        ProtoSchema schema2 = ProtoSchema.paramSchema();
        schema2.addKey("id2", Integer.class);
        schema2.addKey("name2", String.class);
        schema.addKey("data2", Map.class, schema2);
        schema.addKey("product", Product1.class);

        ProtoBuf buffer = ProtoBuf.allocate(512);
        buffer.putMap(data, schema);
        Map data3 = buffer.getMap(schema);
        System.out.println(data3);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPutTable() throws Exception {
        Table<String> list = new Table<String>();
        list.add("Hello123");
        list.add("Wordss");

        ProtoSchema schema = ProtoSchema.getSchema(list, String.class);

        ProtoBuf buffer = ProtoBuf.allocate();
        buffer.putList(list, schema);
        Table list2 = buffer.getTable(schema);
        System.out.println(list2);
        assertTrue(list.get(0).equals(list2.get(0)));
    }

    @Test
    public void testGenRandom() {
        ProtoBuf buffer = ProtoBuf.allocate();
        buffer.putInt(0, SysUtil.random());
        buffer.buffer().rewind();
        StringBuilder valideSign = new StringBuilder(256);
        valideSign.append("VS");
        valideSign.append(1);
        valideSign.append(Encoder.encodeBase64(buffer.buffer()));
        System.out.println(valideSign.toString());
    }

    public static class Product1 {
        private int id;

        private String name;

        public Product1() {
        }

        public Product1(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "id:" + id + ";name:" + name;
        }
    }

    public static class Product2 {
        private int id;

        private String name;

        private Order order;

        public Product2() {
        }

        public Product2(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Order getOrder() {
            return order;
        }

        public void setOrder(Order order) {
            this.order = order;
        }

        @Override
        public String toString() {
            return "id:" + id + ";name:" + name + ";order:" + order;
        }
    }

    public static class Order {
        private int orderId;

        private String orderName;

        private Rider rider;

        public Order() {
        }

        public Order(int orderId, String orderName) {
            this.orderId = orderId;
            this.orderName = orderName;
        }

        public int getOrderId() {
            return orderId;
        }

        public String getOrderName() {
            return orderName;
        }

        public void setOrderName(String orderName) {
            this.orderName = orderName;
        }

        public void setOrderId(int orderId) {
            this.orderId = orderId;
        }

        public Rider getRider() {
            return rider;
        }

        public void setRider(Rider rider) {
            this.rider = rider;
        }

        @Override
        public String toString() {
            return "orderid:" + orderId + ";ordername:" + orderName + ";rider:" + rider;
        }
    }

    public static class Rider {
        private int phone;

        private String name;

        public int getPhone() {
            return phone;
        }

        public void setPhone(int phone) {
            this.phone = phone;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "phone:" + phone + ";name:" + name;
        }
    }

    public static class Car {
        private Map<String, String> things = new HashMap<String, String>();

        public void addThing(String key, String thing) {
            things.put(key, thing);
        }

        @Override
        public String toString() {
            return things.toString();
        }
    }
}
