package cloud.apposs.configure;

import java.util.List;
import java.util.Map;

@Optional("bean-config")
public class MyConfigBean {
    private String name;

    private int id;

    private List<String> courses;

    private Map<String, Integer> scores;

    private MyLogConfigBean log;

    private SvrOption svr;

    private List<GuardRule> ruleList;

    private Map<String, GuardRule> ruleInfo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<String> getCourses() {
        return courses;
    }

    public void setCourses(List<String> courses) {
        this.courses = courses;
    }

    public Map<String, Integer> getScores() {
        return scores;
    }

    public void setScores(Map<String, Integer> scores) {
        this.scores = scores;
    }

    public MyLogConfigBean getLog() {
        return log;
    }

    public void setLog(MyLogConfigBean log) {
        this.log = log;
    }

    public SvrOption getSvr() {
        return svr;
    }

    public void setSvr(SvrOption svr) {
        this.svr = svr;
    }

    public List<GuardRule> getRuleList() {
        return ruleList;
    }

    public void setRuleList(List<GuardRule> ruleList) {
        this.ruleList = ruleList;
    }

    public Map<String, GuardRule> getRuleInfo() {
        return ruleInfo;
    }

    public void setRuleInfo(Map<String, GuardRule> ruleInfo) {
        this.ruleInfo = ruleInfo;
    }

    @Optional("svr")
    public static class SvrOption {
    }

    public static class MyLogConfigBean {
        private String name;

        private String path;

        private List<Integer> levels;

        @Optional("limit-config")
        private MyLogLimitBean limit;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public List<Integer> getLevels() {
            return levels;
        }

        public void setLevels(List<Integer> levels) {
            this.levels = levels;
        }

        public MyLogLimitBean getLimit() {
            return limit;
        }

        public void setLimit(MyLogLimitBean limit) {
            this.limit = limit;
        }
    }

    public static class MyLogLimitBean {
        private String name;

        private int id;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }

    public static class GuardRule {
        private String type;

        private String resource;

        private int threshold;

        private List<String> courses;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public int getThreshold() {
            return threshold;
        }

        public void setThreshold(int threshold) {
            this.threshold = threshold;
        }

        public List<String> getCourses() {
            return courses;
        }

        public void setCourses(List<String> courses) {
            this.courses = courses;
        }
    }
}
