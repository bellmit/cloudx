package cloud.apposs.webx.sample.bean;

import cloud.apposs.rest.validator.checker.Digits;
import cloud.apposs.rest.validator.checker.NotEmpty;
import cloud.apposs.rest.validator.checker.NotNull;
import cloud.apposs.rest.validator.checker.Number;
import cloud.apposs.util.Param;
import cloud.apposs.webx.resolver.parameter.ModelParametric;

import java.util.List;

public class MyBean extends ModelParametric {
    @NotNull
    private String field1;

    @NotEmpty
    private String field2;

    @Number(require = true, min = 0, max = 12)
    private int field3;

    private Param field4;

    private List<MyOption> field5;

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

    public Param getField4() {
        return field4;
    }

    public void setField4(Param field4) {
        this.field4 = field4;
    }

    public List<MyOption> getField5() {
        return field5;
    }

    public void setField5(List<MyOption> field5) {
        this.field5 = field5;
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
