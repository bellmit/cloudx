package cloud.apposs.ioc.sample.bean;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.ioc.annotation.Prototype;

@Component
@Prototype
public class UserBean {
	private int id;
	
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
