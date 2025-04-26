package nationGen.items;

public enum ItemType {
  MELEE("melee"),
  RANGED("ranged"),
  LOW_SHOTS("lowshots");

  private String id;

  ItemType(String magicWeaponType) {
    this.id = magicWeaponType;
  }

  public String getId() {
    return this.id;
  }
}
