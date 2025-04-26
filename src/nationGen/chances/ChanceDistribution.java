package nationGen.chances;

import java.util.*;
import java.util.stream.Collectors;
import nationGen.misc.Arg;

/**
 * Holds a distribution of chances to randomly select certain elements.
 * @param <E> The type of element being randomly selected.
 */
public class ChanceDistribution<E> {

  private final Map<E, Double> chances = new LinkedHashMap<>();

  public ChanceDistribution() {}

  public ChanceDistribution(Map<E, Double> chances) {
    this.chances.putAll(chances);
  }

  public Set<E> getEntities() {
    return this.chances.keySet();
  }

  public void setChance(E entity, double chance) {
    this.chances.put(entity, chance);
  }

  public Double getChance(E entity) {
    return this.chances.get(entity);
  }

  public void modifyChance(E f, Arg mod) {
    this.chances.compute(f, (entity, chance) ->
        mod.applyModTo(chance == null ? baseChance(f) : chance)
      );
  }

  protected double baseChance(E element) {
    return 0;
  }

  public void eliminate(E f) {
    this.chances.remove(f);
  }

  private Map<E, Double> positiveChances() {
    Map<E, Double> positives = new LinkedHashMap<>();
    this.chances.entrySet()
      .stream()
      .filter(e -> e.getValue() > 0)
      .forEach(e -> positives.put(e.getKey(), e.getValue()));
    return positives;
  }

  public boolean isEmpty() {
    return this.chances.values().stream().noneMatch(chance -> chance > 0);
  }

  public boolean hasPossible() {
    return !isEmpty();
  }

  public int countPossible() {
    return (int) this.chances.values()
      .stream()
      .filter(chance -> chance > 0)
      .count();
  }

  public List<E> getPossible() {
    return this.chances.entrySet()
      .stream()
      .filter(e -> e.getValue() > 0)
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());
  }

  public double getTotalChances() {
    Map<E, Double> positiveChances = positiveChances();
    double total = 0;

    for (Double chance : positiveChances.values()) {
      total += chance;
    }

    return total;
  }

  // TODO: Since all the chances in this distribution could be <= 0, this should really return Optional<E>
  // and maybe have a separate method that enforces the thing existing
  public E getRandom(Random r) {
    if (isEmpty()) {
      return null;
    }

    Map<E, Double> positiveChances = positiveChances();

    double max = this.getTotalChances();
    double target = r.nextDouble() * max;
    double current = 0;

    E item = null;

    for (Map.Entry<E, Double> entry : positiveChances.entrySet()) {
      current += entry.getValue();
      if (current >= target) {
        item = entry.getKey();
        break;
      }
    }

    if (item == null) throw new IllegalStateException(
      "ITEM PICKING FAILURE. Strange, eh?"
    );

    return item;
  }
}
