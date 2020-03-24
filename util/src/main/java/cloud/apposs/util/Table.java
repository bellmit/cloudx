package cloud.apposs.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Json数组结构容器，
 * 注意是默认非线程安全，如果需要线程安全需要传递SYNC参数，
 * 因为内部是采用链表遍历时建议采用Iterator迭代
 */
public class Table<T> implements List<T> {
	private final List<T> data;
	
	public Table() {
		this(false);
	}
	
	public Table(boolean sync) {
		if (sync) {
			this.data = new LinkedList<T>();
		} else {
			this.data = new CopyOnWriteArrayList<T>();
		}
	}
	
	public Param getParam(int index) {
		return (Param) data.get(index);
	}
	
	@SuppressWarnings("unchecked")
	public Table<T> setParam(Param param) {
		data.add((T) param);
		return this;
	}
	
	public Object getObject(int index) {
		return data.get(index);
	}
	
	public Table<T> setObject(T value) {
		data.add(value);
		return this;
	}
	
	@Override
	public T get(int index) {
		return data.get(index);
	}

	@Override
	public boolean add(T e) {
		return data.add(e);
	}

	@Override
	public void add(int index, T element) {
		data.add(index, element);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		return data.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		return data.addAll(index, c);
	}

	@Override
	public boolean contains(Object o) {
		return data.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return data.containsAll(c);
	}

	@Override
	public int indexOf(Object o) {
		return data.indexOf(o);
	}
	
	@Override
	public int lastIndexOf(Object o) {
		return data.lastIndexOf(o);
	}
	
	@Override
	public int size() {
		return data.size();
	}

	@Override
	public boolean isEmpty() {
		return data.isEmpty();
	}

	@Override
	public Iterator<T> iterator() {
		return data.iterator();
	}

	@Override
	public ListIterator<T> listIterator() {
		return data.listIterator();
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		return data.listIterator(index);
	}

	@Override
	public boolean remove(Object o) {
		return data.remove(o);
	}

	@Override
	public T remove(int index) {
		return data.remove(index);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return data.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return false;
	}

	@Override
	public T set(int index, T element) {
		return data.set(index, element);
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		return data.subList(fromIndex, toIndex);
	}

	@Override
	public Object[] toArray() {
		return data.toArray();
	}

	@SuppressWarnings("hiding")
	@Override
	public <T> T[] toArray(T[] a) {
		return data.toArray(a);
	}
	
	@Override
	public String toString() {
		return toJson(false, 0, null);
	}
	
	public String toJson(boolean format) {
		return toJson(format, 0, null);
	}
	
	public String toJson(boolean format, String line) {
		return toJson(format, 0, line);
	}
	
	/**
	 * 将Param输出成Json格式
	 * 
	 * @param format 是否格式化输出
	 * @param tab 制表符缩进
	 * @param line 换行，Linux是\n，Windows是\r\n
	 */
	public String toJson(boolean format, int tab, String line) {
		StringBuilder info = new StringBuilder(512);
		if (!format) {
			info.append("[");
			int total = data.size();
			for (int i = 0; i < total; i++) {
				T value = data.get(i);
				info.append(JsonUtil.toJson(value, format, tab, line));
				if (i < total - 1) {
					info.append(",");
				}
			}
			info.append("]");
		} else {
			if (StrUtil.isEmpty(line)) {
				line = "\n";
			}
			String tab1 = "";
			for (int t = 0; t < tab; t++) {
				tab1 += "  ";
			}
			
			info.append("[").append(line).append(tab1);
			int total = data.size();
			for (int i = 0; i < total; i++) {
				T value = data.get(i);
				info.append(JsonUtil.toJson(value, format, tab + 1, line));
				if (i < total - 1) {
					if (value instanceof Param) {
						info.append(",").append(line);
					} else {
						info.append(",").append(" ");
					}
				}
			}
			info.append(line).append(tab1).append("]");
		}
		return info.toString();
	}

	@Override
	public void clear() {
		data.clear();
	}
}
