package nationGen.magic;


import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Essentially a {@code Map<MagicPath, Integer>}, but has convenience methods for handling values associated with each
 * magic path. Used for things like the magic pick distribution of a mage.
 */
public class MagicPathInts {
	
	private static class Int {
		int amount;
		
		@Override
		public String toString() {
			return Integer.toString(amount);
		}
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

	public void maxWith(MagicPathInts other) {
		Arrays.stream(MagicPath.values()).forEach(p -> set(p, Math.max(get(p), other.get(p))));
	}
	
	public void addAll(MagicPathInts map) {
		for (MagicPath path : MagicPath.values()) {
			add(path, map.get(path));
		}
	}
	
	public int getHighestAmount() {
		return this.amounts.values().stream()
				.map(e -> e.amount)
				.max(Comparator.naturalOrder())
				.orElseThrow();
	}

	public int getSecondHighestAmount() {
		return this.amounts.values().stream()
				.map(e -> e.amount)
				.sorted(Comparator.reverseOrder())
				.distinct()
				.skip(1)
				.findFirst()
				.orElseThrow();
	}
	
	public List<MagicPath> getAllHighestPaths() {
		int highest = getHighestAmount();
		return this.amounts.entrySet().stream()
				.filter(e -> e.getValue().amount == highest)
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
	}
	
	public MagicPath getHighestPath() {
		return this.amounts.entrySet().stream()
				.max(Comparator.comparingInt(e -> e.getValue().amount))
				.orElseThrow().getKey();
	}

	public NavigableMap<Integer, Set<MagicPath>> byAmount() {
		NavigableMap<Integer, Set<MagicPath>> map = new TreeMap<>();

		for (Map.Entry<MagicPath, Int> entry : this.amounts.entrySet()) {
			map.computeIfAbsent(entry.getValue().amount, i -> EnumSet.noneOf(MagicPath.class)).add(entry.getKey());
		}
		return map;
	}
	
	public Stream<MagicPathLevel> stream() {
		return this.amounts.entrySet().stream()
			.map(e -> new MagicPathLevel(e.getKey(), e.getValue().amount));
	}
}
