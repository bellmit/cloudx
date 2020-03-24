package cloud.apposs.util;

public class Pair<F, S> {
	private final F first;
	private final S second;
	
	public Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}

	public F getFirst() {
		return first;
	}

	public S getSecond() {
		return second;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Pair) {
			return false;
		}
		
		Pair<F, S> pair = (Pair<F, S>) obj;
        if (pair == null) {
        	return false;
        }
        if (!first.equals(pair.first)) {
        	return false;
        }
        return second.equals(pair.second);
	}

	@Override
	public String toString() {
		return first + "," + second;
	}
}
