package cloud.apposs.okhttp;

import cloud.apposs.netkit.rxio.io.http.HttpForm;
import cloud.apposs.netkit.rxio.io.http.enctype.FormEnctypt;

import java.io.IOException;

/**
 * HTTP FORM表单数据请求
 */
public final class FormEntity {
    private final HttpForm form;

    public static FormEntity builder() {
        return new FormEntity(FormEnctypt.FORM_ENCTYPE_URLENCODE, "utf-8");
    }

    public static FormEntity builder(int formEnctype) {
        return new FormEntity(formEnctype, "utf-8");
    }

    public static FormEntity builder(int formEnctype, String charset) {
        return new FormEntity(formEnctype, charset);
    }

    private FormEntity(int formEnctype, String charset) {
        this.form = new HttpForm(formEnctype, charset);
    }

    public FormEntity add(String name, Object value) throws IOException {
        form.add(name, value);
        return this;
    }

    public HttpForm getForm() {
        return form;
    }
}
