package cloud.apposs.rest.sample;

import cloud.apposs.rest.annotation.Action;
import cloud.apposs.rest.annotation.Request;

@Action
public class UserAction {
    @Request("/")
    public String root() {
        return "Hello Index Html";
    }
}
