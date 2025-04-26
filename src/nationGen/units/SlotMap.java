package nationGen.units;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import nationGen.items.Item;

public class SlotMap {
  private LinkedHashMap<String, Deque<Item>> slotmemory =
    new LinkedHashMap<>();

  public Set<String> getSlots() {
    return slotmemory.keySet();
  }

  public Stream<String> slots() {
    return slotmemory.keySet().stream();
  }

  public Stream<Item> items() {
    return slotmemory
      .values()
      .stream()
      .map(Deque::peek)
      .filter(Objects::nonNull);
  }

  Item get(String slot) {
    Deque<Item> stack = slotmemory.get(slot);
    return stack == null ? null : stack.peek();
  }

  Stream<Item> getArmor() {
    return this.items()
      .filter(i -> i.armor == true);
  }

  Stream<Item> getWeapons() {
    return this.items()
      .filter(i -> i.armor == false);
  }

  Stream<Item> getResolvedWeapons() {
    return this.items()
      .filter(i -> i.armor == false && i.isCustomIdResolved());
  }

  void push(String slot, Item item) {
    getSlotStack(slot).push(item);
  }

  Item pop(String slot) {
    return getSlotStack(slot).pollFirst();
  }

  void addAllFrom(SlotMap source) {
    source.slotmemory.forEach((slot, stack) ->
      getSlotStack(slot).addAll(stack)
    );
  }

  private Deque<Item> getSlotStack(String slot) {
    return slotmemory.computeIfAbsent(slot, k -> new LinkedList<>());
  }
}
