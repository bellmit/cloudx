package cloud.apposs.util;

public final class Ref<T> {
	private T value = null;

	public Ref() {
	}
	
	public Ref(T value) {
		this.value = value;
	}
	
	public final T value() {
		return value;
	}
	
	public final void value(T value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return value == null ? null : value.toString();
	}
}
