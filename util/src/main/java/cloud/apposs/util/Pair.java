package cloud.apposs.util;

/**
 * 双值对，主要用于处理结果不上一个值的情况
 *
 * @param <K> 第一个值
 * @param <V> 第二个值
 */
public class Pair<K, V> {
    private final K key;
    private final V value;

    public static <K, V> Pair<K, V> build(K key, V value) {
        return new Pair<K, V>(key, value);
    }

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K key() {
        return key;
    }

    public V value() {
        return value;
    }

    @Override
    public int hashCode() {
        return key.hashCode() * 13 + (value == null ? 0 : value.hashCode());
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Pair) {
            Pair<K, V> pair = (Pair<K, V>) obj;
            if (!key.equals(pair.key)) {
                return false;
            }
            return value.equals(pair.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return key + "," + value;
    }
}
