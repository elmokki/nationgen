package nationGen.chances;


import java.util.Arrays;
import java.util.Optional;


/**
 * A type of comparison that can be made between two comparable values.
 */
public enum Comparison {
	EQUAL,
	BELOW,
	ABOVE;
	
	public static Optional<Comparison> find(String name) {
		return Arrays.stream(values()).filter(c -> c.name().equalsIgnoreCase(name)).findFirst();
	}
}
