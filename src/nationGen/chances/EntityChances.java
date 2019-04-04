package nationGen.chances;


import nationGen.entities.Entity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * A {@link ChanceDistribution} which handles chances of entities.  This has the benefit of automatically using the
 * base chance of an entity.
 * @param <E> The type of {@link Entity}
 */
public class EntityChances<E extends Entity> extends ChanceDistribution<E> {
	
	public EntityChances() {
		super();
	}
	
	public EntityChances(Map<E, Double> chances) {
		super(chances);
	}
	
	public static <E extends Entity> EntityChances<E> baseChances(List<E> entities) {
		if (entities == null) {
			return new EntityChances<>();
		}
		Map<E, Double> chances = new LinkedHashMap<>();
		for (E entity : entities) {
			if (entity != null) {
				chances.put(entity, entity.basechance);
			}
		}
		return new EntityChances<>(chances);
	}
	
	@Override
	protected double baseChance(E entity) {
		return entity.basechance;
	}
}
