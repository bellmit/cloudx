package cloud.apposs.webx.schedule;

import java.lang.reflect.Method;

/**
 * 要执行的定时任务
 */
public class CronTask {
	/** 任务名称 */
	private final String name;
	
	private final Object target;
	
	private final Method method;
	
	private final String expression;

	public CronTask(String name, Object target, Method method, String expression) {
		this.name = name;
		this.target = target;
		this.method = method;
		this.expression = expression;
	}

	public String getName() {
		return name;
	}

	public Object getTarget() {
		return target;
	}

	public Method getMethod() {
		return method;
	}

	public String getExpression() {
		return expression;
	}
}
