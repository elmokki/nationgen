package nationGen.magic;


import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Essentially a {@code Map<MagicPath, Integer>}, but has convenience methods for handling values associated with each
 * magic path. Used for things like the magic pick distribution of a mage.
 */
public class MagicPathInts {
	private class Int {
		int amount;
	}
	private final Map<MagicPath, Int> amounts = new EnumMap<>(MagicPath.class);
	
	public MagicPathInts() {
		for (MagicPath path : MagicPath.values()) {
			amounts.put(path, new Int());
		}
	}
	
	public int get(MagicPath path) {
		return this.amounts.get(path).amount;
	}
	
	public void set(MagicPath path, int amount) {
		this.amounts.get(path).amount = amount;
	}
	
	public int add(MagicPath path, int addend) {
		return this.amounts.get(path).amount += addend;
	}
	
	public int scale(MagicPath path, int factor) {
		return this.amounts.get(path).amount *= factor;
	}
	
	public void addAll(MagicPathInts map) {
		for (MagicPath path : MagicPath.values()) {
			add(path, map.get(path));
		}
	}
	
	public int getHighestAmount() {
		return this.amounts.entrySet().stream()
				.max(Comparator.comparingInt(e -> e.getValue().amount))
				.orElseThrow().getValue().amount;
	}
	
	public List<MagicPath> getAllHighestPaths() {
		int highest = getHighestAmount();
		return this.amounts.entrySet().stream().filter(e -> e.getValue().amount == highest).map(Map.Entry::getKey)
				.collect(Collectors.toList());
	}
	
	public MagicPath getHighestPath() {
		return this.amounts.entrySet().stream()
				.max(Comparator.comparingInt(e -> e.getValue().amount))
				.orElseThrow().getKey();
	}
}
