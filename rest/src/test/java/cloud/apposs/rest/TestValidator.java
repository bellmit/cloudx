package cloud.apposs.rest;

import cloud.apposs.rest.validator.Validator;
import cloud.apposs.rest.validator.checker.Digits;
import cloud.apposs.rest.validator.checker.Email;
import cloud.apposs.rest.validator.checker.Length;
import cloud.apposs.rest.validator.checker.Mobile;
import cloud.apposs.rest.validator.checker.NotEmpty;
import cloud.apposs.rest.validator.checker.NotNull;
import cloud.apposs.rest.validator.checker.Number;
import cloud.apposs.rest.validator.checker.Pattern;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import org.junit.Test;

import java.util.List;

public class TestValidator {
    @Test
    public void testObjectDeserialize() throws Exception {
        String jsonValue = "{" +
                "\"field1\":\"value1\"," +
                "\"field2\":\"value2\"," +
                "\"field3\":10," +
                "\"field4\":\"hello\"," +
                "\"field5\":\"wayken@qq.com\"," +
                "\"field6\":\"12222222212\"," +
                "\"field7\":\"word\"," +
                "\"field8\":{\"sub1\":\"MyTitle1\",\"sub2\":\"MyName1\",\"sub3\":{\"id\":2,\"name\":\"MySubName2\"}}," +
                "\"field9\":[{\"id\":1,\"name\":\"MySubName1\"},{\"id\":2,\"name\":\"MySubName2\"}]" +
                "}";
        Param value = JsonUtil.parseJsonParam(jsonValue);
        System.out.println(value);
        MyObject instance = Validator.deserialize(MyObject.class, value);
        System.out.println(instance);
    }

    public static class MyObject {
        @NotNull
        private String field1;

        @NotEmpty
        private String field2;

        @Number(require = true, min = 0, max = 12)
        private int field3;

        @Length(require = true, min = 0, max = 5)
        private String field4;

        @Email(require = true)
        private String field5;

        @Mobile(require = false)
        private String field6;

        @Pattern(regex = "[a-zA-Z][a-zA-Z0-9_]*")
        private String field7;

        @NotNull
        private Param field8;

        @NotNull
        private List<MyOption> field9;

        public String getField1() {
            return field1;
        }

        public void setField1(String field1) {
            this.field1 = field1;
        }

        public String getField2() {
            return field2;
        }

        public void setField2(String field2) {
            this.field2 = field2;
        }

        public int getField3() {
            return field3;
        }

        public void setField3(int field3) {
            this.field3 = field3;
        }

        public String getField4() {
            return field4;
        }

        public void setField4(String field4) {
            this.field4 = field4;
        }

        public String getField5() {
            return field5;
        }

        public void setField5(String field5) {
            this.field5 = field5;
        }

        public String getField6() {
            return field6;
        }

        public void setField6(String field6) {
            this.field6 = field6;
        }

        public String getField7() {
            return field7;
        }

        public void setField7(String field7) {
            this.field7 = field7;
        }

        public Param getField8() {
            return field8;
        }

        public void setField8(Param field8) {
            this.field8 = field8;
        }

        public List<MyOption> getField9() {
            return field9;
        }

        public void setField9(List<MyOption> field9) {
            this.field9 = field9;
        }
    }

    public static class MyOption {
        @Digits
        private int id;

        @NotEmpty
        private String name;

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
    }
}
