package cloud.apposs.configure;

import java.util.List;
import java.util.Map;

import cloud.apposs.configure.MyConfigBean.MyLogConfigBean;
import cloud.apposs.configure.MyConfigBean.GuardRule;
import cloud.apposs.configure.MyConfigBean.MyLogLimitBean;
import cloud.apposs.configure.MyConfigBean.SvrOption;

public class TestConfiguration {
    public static final boolean USE_XML = true;
    public static final String BEAN_CFG_JSON = "bean-config.conf";
    public static final String BEAN_CFG_XML = "bean-config.xml";

    public static void main(String[] args) throws Exception {
        MyConfigBean bean = new MyConfigBean();
        bean.setSvr(new MySvrOption());
        if (!USE_XML) {
            ConfigurationParser cp = ConfigurationFactory.getConfigurationParser(ConfigurationFactory.JSON);
            cp.parse(bean, BEAN_CFG_JSON);
        } else {
            ConfigurationParser cp = ConfigurationFactory.getConfigurationParser(ConfigurationFactory.XML);
            cp.parse(bean, BEAN_CFG_XML);
        }
        System.out.println("Name:" + bean.getName());
        System.out.println("Id:" + bean.getId());
        List<String> courses = bean.getCourses();
        for (String course : courses) {
            System.out.println("List:" + course);
        }
        Map<String, Integer> scores = bean.getScores();
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            System.out.println("Map:" + entry.getKey() + ":" + entry.getValue());
        }
        MyLogConfigBean log = bean.getLog();
        System.out.println("Log:" + log.getName());
        System.out.println("Log:" + log.getPath());
        List<Integer> levels = log.getLevels();
        for (Integer level : levels) {
            System.out.println("Log Level:" + level);
        }
        MyLogLimitBean limit = log.getLimit();
        System.out.println("Limit Id:" + limit.getId());
        System.out.println("Limit Name:" + limit.getName());
        MySvrOption svrOpt = (MySvrOption) bean.getSvr();
        System.out.println("Svr Usr:" + svrOpt.getUser());
        System.out.println("Svr Pwd:" + svrOpt.getPwd());
		List<GuardRule> guardRules = bean.getRuleList();
        System.out.println(guardRules.get(0).getResource());
        Map<String, GuardRule> guardRuleInfo = bean.getRuleInfo();
        GuardRule rule = guardRuleInfo.get("fuse_avg_resptime");
        System.out.println(rule.getResource());
    }

    public static class MySvrOption extends SvrOption {
        private String user;

        private String pwd;

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPwd() {
            return pwd;
        }

        public void setPwd(String pwd) {
            this.pwd = pwd;
        }
    }
}
