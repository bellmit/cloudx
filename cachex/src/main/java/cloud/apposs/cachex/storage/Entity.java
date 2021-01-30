package cloud.apposs.cachex.storage;

import cloud.apposs.util.Param;

/**
 * 数据实体类，代表数据库中的一行数据，
 * 以Key->Value形式保存数据
 */
public class Entity extends Param {
	/** 数据主键 */
	private Object identity;

	/** 数据主键字段名称 */
	private String primary;

	public Entity() {
		this(null, null, new Param(true));
	}

	public Entity(String primary) {
		this(primary, null, new Param(true));
	}

	public Entity(Param value) {
		this(null, null, value);
	}

	public Entity(String primary, Object identity, Param value) {
		super(value);
		this.primary = primary;
		this.identity = identity;
	}

	public Object getIdentity() {
		return identity;
	}

	public void setIdentity(Object identity) {
		this.identity = identity;
	}

	public String getPrimary() {
		return primary;
	}

	public void setPrimary(String primary) {
		this.primary = primary;
	}

	public Param getDatas() {
		return this;
	}
}
