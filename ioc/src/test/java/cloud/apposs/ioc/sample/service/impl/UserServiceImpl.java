package cloud.apposs.ioc.sample.service.impl;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.ioc.sample.service.inf.IUserService;

@Component
public class UserServiceImpl implements IUserService {
	@Override
	public void addUser() {
		System.out.println("adding user...");
	}
}
