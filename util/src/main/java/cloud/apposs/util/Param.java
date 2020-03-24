package cloud.apposs.util;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Json Key-Value结构容器，
 * 注意是默认非线程安全，如果需要线程安全需要传递SYNC参数
 */
public class Param implements Map<String, Object> {
	public static final String DATE_PATTERN_DEFAULT = "yyyy-MM-dd";
	
	private final Map<String, Object> datas;

	public Param() {
		this(false, null);
	}

	public Param(boolean sync) {
		this(sync, null);
	}

	public Param(Param datas) {
		this(false, datas);
	}

	public Param(boolean sync, Param datas) {
		if (sync) {
			this.datas = new ConcurrentHashMap<String, Object>();
		} else {
			this.datas = new HashMap<String, Object>();
		}
		if (datas != null) {
			this.datas.putAll(datas);
		}
	}
	
	@Override
	public Object get(Object key) {
		return datas.get(key);
	}
	
	public Boolean getBoolean(String key) {
		return getBoolean(key, null);
	}
	
	public Boolean getBoolean(String key, Boolean defaultValue) {
		Object value = get(key);
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof String) {
			return Parser.parseBoolean((String) value);
		}
		return (Boolean)value;
	}
	
	public Param setBoolean(String key, Boolean value) {
		put(key, value);
		return this;
	}
	
	public Integer getInt(String key) {
		return getInt(key, null);
	}
	
	public Integer getInt(String key, Integer defaultValue) {
		Object value = get(key);
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof String) {
			return Parser.parseInt((String) value);
		}
		return (Integer)value;
	}
	
	public Param setInt(String key, Integer value) {
		put(key, value);
		return this;
	}
	
	public String getString(String key) {
		return getString(key, null);
	}
	
	public String getString(String key, String defaultValue) {
		Object value = get(key);
		if (!(value instanceof String)) {
			return null;
		}
		if (StrUtil.isEmpty((String) value)) {
			return defaultValue;
		}
		return (String)value;
	}
	
	public Param setString(String key, String value) {
		put(key, value);
		return this;
	}
	
	public Long getLong(String key) {
		return getLong(key, null);
	}
	
	public Long getLong(String key, Long defaultValue) {
		Object value = get(key);
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof String) {
			return Parser.parseLong((String) value);
		}
		return (Long)value;
	}
	
	public Param setLong(String key, Long value) {
		put(key, value);
		return this;
	}
	
	public Double getDouble(String key) {
		return getDouble(key, null);
	}
	
	public Double getDouble(String key, Double defaultValue) {
		Object value = get(key);
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof String) {
			return Parser.parseDouble((String) value);
		}
		return (Double)value;
	}
	
	public Param setDouble(String key, Double value) {
		put(key, value);
		return this;
	}
	
	public Float getFloat(String key) {
		return getFloat(key, null);
	}
	
	public Float getFloat(String key, Float defaultValue) {
		Object value = get(key);
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof String) {
			return Parser.parseFloat((String) value);
		}
		return (Float)value;
	}
	
	public Param setFloat(String key, Float value) {
		put(key, value);
		return this;
	}
	
	public Short getShort(String key) {
		return getShort(key, null);
	}
	
	public Short getShort(String key, Short defaultValue) {
		Object value = get(key);
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof String) {
			return Parser.parseShort((String) value);
		}
		return (Short)value;
	}
	
	public Param setShort(String key, Short value) {
		put(key, value);
		return this;
	}
	
	public Map<String, Object> getMap(String key) {
		return getMap(key, null);
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> getMap(String key, Param defaultValue) {
		Object value = get(key);
		if (value == null) {
			return defaultValue;
		}
		return (Map<String, Object>)value;
	}
	
	public Map<String, Object> getMapWithoutNull(String key) {
		return getMapWithoutNull(key, false);
	}
	
	public Map<String, Object> getMapWithoutNull(String key, boolean sync) {
		Param value = getParam(key);
		if (value == null) {
			return new Param(sync);
		}
		return value;
	}
	
	public Param setMap(String key, Map<String, Object> value) {
		put(key, value);
		return this;
	}
	
	public Param getParam(String key) {
		return getParam(key, null);
	}
	
	public Param getParam(String key, Param defaultValue) {
		Object value = get(key);
		if (value == null) {
			return defaultValue;
		}
		return (Param)value;
	}
	
	public Param getParamWithoutNull(String key) {
		return getParamWithoutNull(key, false);
	}
	
	public Param getParamWithoutNull(String key, boolean sync) {
		Param value = getParam(key);
		if (value == null) {
			return new Param(sync);
		}
		return value;
	}
	
	public Param setParam(String key, Param value) {
		put(key, value);
		return this;
	}
	
	public <T> Table<T> getTable(String key) {
		return getTable(key, null);
	}
	
	public <T> Table<T> getTableWithoutNull(String key) {
		return getTableWithoutNull(key, false);
	}
	
	public <T> Table<T> getTableWithoutNull(String key, boolean sync) {
		Table<T> value = getTable(key);
		if (value == null) {
			return new Table<T>(sync);
		}
		return value;
	}
	
	@SuppressWarnings("unchecked")
	public <T> Table<T> getTable(String key, Table<T> defaultValue) {
		Object value = get(key);
		if (value == null) {
			return defaultValue;
		}
		return (Table<T>)value;
	}
	
	public <T> List<T> getList(String key) {
		return getList(key, null);
	}
	
	public <T> List<T> getListWithoutNull(String key) {
		List<T> value = getList(key);
		if (value == null) {
			return new ArrayList<T>();
		}
		return value;
	}
	
	@SuppressWarnings("unchecked")
	public <T> List<T> getList(String key, List<T> defaultValue) {
		Object value = get(key);
		if (value == null) {
			return defaultValue;
		}
		return (List<T>)value;
	}
	
	public <T> Param setList(String key, List<T> value) {
		put(key, value);
		return this;
	}
	
	public Long getBuffer(String key) {
		return getLong(key, null);
	}
	
	public ByteBuffer getBuffer(String key, ByteBuffer defaultValue) {
		Object value = get(key);
		if (value == null) {
			return defaultValue;
		}
		return (ByteBuffer)value;
	}
	
	public Param setBuffer(String key, ByteBuffer value) {
		put(key, value);
		return this;
	}
	
	public Calendar getCalendar(String key) {
		return getCalendar(key, null);
	}
	
	public Calendar getCalendar(String key, Calendar defaultValue) {
		return getCalendar(key, DATE_PATTERN_DEFAULT, defaultValue);
	}
	
	public Calendar getCalendar(String key, String format, Calendar defaultValue) {
		Object value = get(key);
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof Long) {
			Calendar newValue = Calendar.getInstance();
			newValue.setTimeInMillis(getLong(key));
			return newValue;
		}
		if (value instanceof String) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(format);
			try {
				Date date = dateFormat.parse((String) value);
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				return calendar;
			} catch (ParseException e) {
				return defaultValue;
			}
		}
		return (Calendar)value;
	}
	
	public Param setCalendar(String key, Calendar value) {
		put(key, value);
		return this;
	}
	
	public Object getObject(String key) {
		return getObject(key, null);
	}
	
	public Object getObject(String key, Object defaultValue) {
		Object value = get(key);
		if (value == null) {
			return defaultValue;
		}
		return value;
	}
	
	public Param setObject(String key, Object value) {
		put(key, value);
		return this;
	}
	
	@Override
	public Object put(String key, Object value) {
		if (value == null) {
			return null;
		}
		return datas.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		datas.putAll(m);
	}
	
	@Override
	public Collection<Object> values() {
		return datas.values();
	}
	
	@Override
	public Set<Entry<String, Object>> entrySet() {
		return datas.entrySet();
	}

	@Override
	public boolean isEmpty() {
		return datas.isEmpty();
	}
	
	@Override
	public int size() {
		return datas.size();
	}

	@Override
	public boolean containsKey(Object key) {
		return datas.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return datas.containsValue(value);
	}
	
	@Override
	public Set<String> keySet() {
		return datas.keySet();
	}

	@Override
	public Object remove(Object key) {
		return datas.remove(key);
	}
	
	@Override
	public String toString() {
		return toJson(false, 0, null);
	}
	
	public String toJson() {
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
			info.append("{");
			int count = 0, total = datas.size();
			for (Entry<String, Object> entry : datas.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				info.append("\"").append(key).append("\":");
				info.append(JsonUtil.toJson(value, format, tab, line));
				if (++count < total) {
					info.append(",");
				}
			}
			info.append("}");
		} else {
			if (StrUtil.isEmpty(line)) {
				line = "\n";
			}
			String tab1 = "";
			for (int t = 0; t < tab; t++) {
				tab1 += "  ";
			}
			String tab2 = tab1 + "  ";
			
			info.append(tab1).append("{").append(line);
			int count = 0, total = datas.size();
			for (Entry<String, Object> entry : datas.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				info.append(tab2).append("\"").append(key).append("\": ");
				info.append(JsonUtil.toJson(value, format, tab + 1, line));
				if (++count < total) {
					info.append(",").append(line);
				} else {
					info.append(line);
				}
			}
			info.append(tab1).append("}");
		}
		return info.toString();
	}

	@Override
	public void clear() {
		datas.clear();
	}
}
