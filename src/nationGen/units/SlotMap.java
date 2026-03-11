package nationGen.units;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import nationGen.items.Item;

public class SlotMap {
  private LinkedHashMap<String, Deque<Item>> slotmemory =
    new LinkedHashMap<>();

  public SlotMap() {};

  // Deep-copy constructor
  public SlotMap(SlotMap slotMap) {
    slotMap.slotmemory.forEach((slotName, itemDeque) -> {
      Deque<Item> copiedDeque = new LinkedList<>();

      itemDeque.forEach(item -> {
        Item itemCopy = (item != null) ? new Item(item) : null;
        copiedDeque.addLast(itemCopy);
      });

      this.slotmemory.put(slotName, copiedDeque);
    });
  }

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

  Stream<Item> getDominionsEquipment() {
    return this.items().filter(Item::isDominionsEquipment);
  }

  Stream<Item> getEquippedArmors() {
    return this.items()
      .filter(Item::isArmor)
      .filter(Item::isDominionsEquipment);
  }

  Stream<Item> getEquippedShields() {
    return this.items()
      .filter(Item::isShield)
      .filter(Item::isDominionsEquipment);
  }

  Stream<Item> getEquippedWeapons() {
    return this.items()
      .filter(Item::isWeapon)
      .filter(Item::isDominionsEquipment);
  }

  Stream<Item> getResolvedWeapons() {
    return this.items()
      .filter(Item::isWeapon)
      .filter(Item::isDominionsEquipment)
      .filter(i -> i.dominionsId.isResolved());
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

  void resolveItems() {
    this.items()
      .filter(Item::isDominionsEquipment)
      .filter(Predicate.not(Item::isDominionsIdAssigned))
      .forEach(i -> {
        i = Item.resolveId(i);
      });
  }

  private Deque<Item> getSlotStack(String slot) {
    return slotmemory.computeIfAbsent(slot, k -> new LinkedList<>());
  }

  public List<Item> getItemsInSlotStack(String slotName) {
    return this.getSlotStack(slotName).stream().filter(i -> i != null).toList();
  }
}
