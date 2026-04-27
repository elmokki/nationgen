package nationGen.items;

@FunctionalInterface
public interface ItemTypeCheck {
    Boolean check(Item item);
}
