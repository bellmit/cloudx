package cloud.apposs.util;

import org.junit.Test;

public class TestEncoder {
    @Test
    public void testEncodeHtml() {
        String html = "<div>Hello Html</div>";
        String encodeHtml = Encoder.encodeHtml(html);
        System.out.println(encodeHtml);
        System.out.println(Encoder.encodeHtml(encodeHtml));
    }

    @Test
    public void testEncodeJavaScript() {
        String js = "aaa\");alert(document.cookie);<script>alert('123')</script>";
//        String js2 = "<s>a&aa</s>'b\"c <textarea>bb</textarea>";
//        String js3 = "aaa\";alert(document.cookie);//";
//        String js4 = "lijbaqet';document[`write`](atob(`PHNjcmlwdCBzcmM9L`))";
        String encodeJs = Encoder.encodeJavaScript(js);
        System.out.println(encodeJs);
//        System.out.println(Encoder.encodeJavaScript(encodeJs));
//        System.out.println(Encoder.encodeJavaScript(js2));
//        System.out.println(Encoder.encodeJavaScript(js3));
//        System.out.println(Encoder.encodeJavaScript(js4));
    }

    @Test
    public void testEncodeUrl() {
        String str = "中国";
        String encodeStr = Encoder.encodeUrl(str);
        System.out.println(encodeStr);
        System.out.println(Encoder.decodeUrl(encodeStr));
    }
}
