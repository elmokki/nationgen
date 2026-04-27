package nationGen.items;

public enum ItemType {
  ARMOR("armor", ItemType::isArmor),
  BARDING("barding", ItemType::isBarding),
  BODY_ARMOR("bodyarmor", ItemType::isBodyArmor),
  HELMET("helmet", ItemType::isHelmet),
  LOW_SHOTS("lowshots", ItemType::isLowAmmo),
  MELEE("melee", ItemType::isMelee),
  MOUNT("mount", ItemType::isMount),
  RANGED("ranged", ItemType::isRanged),
  SHIELD("shield", ItemType::isShield),
  WEAPON("weapon", ItemType::isWeapon);

  private String id;
  private ItemTypeCheck checkInterface;

  ItemType(String typeId, ItemTypeCheck itemTypeCheck) {
    this.id = typeId;
    this.checkInterface = itemTypeCheck;
  }

  public String getId() {
    return this.id;
  }

  public Boolean check(Item item) {
    return this.checkInterface.check(item);
  }

  // Armor for now has to be determined at file-parsing time.
  // Mainly because armor and weapon DBs share ids, so any given
  // item id will be present on both. The only way to diferentiate
  // them is declaring #armor as a tag on the item data template.
  private static Boolean isArmor(Item item) {
    return item.isArmor();
  }

  private static Boolean isBarding(Item item) {
    Boolean hasBardingType = item.getIntegerFromDb(ItemProperty.TYPE.toDBColumn(), 0) == 9;
    return hasBardingType;
  }

  private static Boolean isBodyArmor(Item item) {
    Boolean hasBodyArmorType = item.getIntegerFromDb(ItemProperty.TYPE.toDBColumn(), 0) == 5;
    return hasBodyArmorType;
  }

  private static Boolean isHelmet(Item item) {
    Boolean hasHelmetType = item.getIntegerFromDb(ItemProperty.TYPE.toDBColumn(), 0) == 6;
    return hasHelmetType;
  }

  private static Boolean isLowAmmo(Item item) {
    // If its ammo is less than 4, it's a lowshots weapon
    Boolean hasLowAmmo = item.getIntegerFromDb(ItemProperty.TYPE.toDBColumn(), 100) < 4;
    return hasLowAmmo;
  }

  private static Boolean isMelee(Item item) {
    // If its range is 0, it's melee
    Boolean hasZeroRange = item.getIntegerFromDb("rng", 0) == 0;
    return item.isWeapon() && hasZeroRange;
  }

  private static Boolean isMount(Item item) {
    Boolean hasMountReference = item.getCommands().stream()
      .anyMatch(c -> c.command.equals("#mountmnr"));
  
    return hasMountReference;
  }

  private static Boolean isRanged(Item item) {
    // If it's got a range property, it should be ranged
    Boolean hasRange = item.getIntegerFromDb("rng", 0) != 0;
    return item.isWeapon() && hasRange;
  }

  private static Boolean isShield(Item item) {
    Boolean hasShieldType = item.getIntegerFromDb(ItemProperty.TYPE.toDBColumn(), 0) == 4;
    return hasShieldType;
  }

  // Weapon for now has to be determined at file-parsing time.
  // Mainly because armor and weapon DBs share ids, so any given
  // item id will be present on both. The only way to diferentiate
  // them is declaring #armor as a tag on the item data template.
  private static Boolean isWeapon(Item item) {
    return item.isWeapon();
  }
}
