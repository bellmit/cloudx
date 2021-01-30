package cloud.apposs.bootor.sample.action;

import cloud.apposs.ioc.annotation.Prototype;
import cloud.apposs.rest.annotation.Action;
import cloud.apposs.rest.annotation.Request;
import cloud.apposs.rest.annotation.WriteCmd;

@Action
@Prototype
@WriteCmd
public class PayAction {
    @Request.Read({"/pay", "/give"})
    public String pay() {
        return "I am pay in " + this.toString();
    }

    @Request.Post("/pay2")
    public String pay2() {
        return "I am pay2 in " + this.toString();
    }
}
