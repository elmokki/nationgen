package nationGen.magic;


import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;


/**
 * Essentially a {@code Map<MagicPath, Double>}, but has convenience methods for the purposes of tracking values
 * associated with each magic path.
 */
public class MagicPathDoubles {
	private class Value {
		double amount;
	}
	private final Map<MagicPath, Value> amounts = new EnumMap<>(MagicPath.class);
	
	public MagicPathDoubles() {
		for (MagicPath path : MagicPath.values()) {
			amounts.put(path, new Value());
		}
	}
	
	public double get(MagicPath path) {
		return this.amounts.get(path).amount;
	}
	
	public void set(MagicPath path, double amount) {
		this.amounts.get(path).amount = amount;
	}
	
	public double add(MagicPath path, double addend) {
		return this.amounts.get(path).amount += addend;
	}
	
	public double scale(MagicPath path, double factor) {
		return this.amounts.get(path).amount *= factor;
	}
	
	public void addAll(MagicPathInts map) {
		for (MagicPath path : MagicPath.values()) {
			add(path, map.get(path));
		}
	}
	
	public void addAll(MagicPathDoubles map) {
		for (MagicPath path : MagicPath.values()) {
			add(path, map.get(path));
		}
	}
	
	public double getHighestAmount() {
		return this.amounts.entrySet().stream()
				.max(Comparator.comparingDouble(e -> e.getValue().amount))
				.orElseThrow().getValue().amount;
	}
	
	public MagicPath getHighestPath() {
		return this.amounts.entrySet().stream()
				.max(Comparator.comparingDouble(e -> e.getValue().amount))
				.orElseThrow().getKey();
	}
}
