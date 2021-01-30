package cloud.apposs.webx;

import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;

public class DemoApplication {
    public static final String BASE_PATH = System.getProperty("user.dir") + "/webx";

    public static void main(String[] args) throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8090);
        System.setProperty("catalina.base", BASE_PATH);
        StandardContext ctx = (StandardContext) tomcat.addWebapp("/",
                new File(BASE_PATH + "/src/test/webroot").getAbsolutePath());
        ctx.setReloadable(false);
        File additionWebInfClasses = new File("target/classes");
        WebResourceRoot resources = new StandardRoot(ctx);
        resources.addPreResources(new DirResourceSet(resources,
                "/WEB-INF/classes", additionWebInfClasses.getAbsolutePath(), "/"));
        tomcat.start();
        tomcat.getServer().await();
    }
}
