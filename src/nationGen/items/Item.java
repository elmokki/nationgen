package nationGen.items;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.elmokki.NationGenDB;
import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.chances.ThemeInc;
import nationGen.entities.Drawable;
import nationGen.entities.Filter;
import nationGen.ids.DominionsId;
import nationGen.misc.Command;

public class Item extends Drawable {

  public String slot = "";
  public String set = "";
  public Filter filter = null;
  public DominionsId dominionsId = new DominionsId();
  public List<ItemDependency> dependencies = new ArrayList<>();

  private List<ItemType> itemTypes = new ArrayList<>();
  private String bardingId;

  public Item(NationGen nationGen) {
    super(nationGen);
  }

  public Item(Item item) {
    super(item);
    this.dominionsId = item.dominionsId;
    this.filter = (item.filter != null) ? new Filter(item.filter) : null;
    this.itemTypes = new ArrayList<>(item.itemTypes);
    this.dependencies = new ArrayList<>(item.dependencies)
      .stream()
      .map(d -> new ItemDependency(d))
      .collect(Collectors.toList());

    this.slot = item.slot;
    this.set = item.set;
  }

  public String getValueFromDb(String dbColumn) {
    NationGenDB db = this.isArmor() ? this.nationGen.armordb : this.nationGen.weapondb;
    String itemIdInDb = String.valueOf(this.dominionsId.getIngameId());
    String value = db.GetValue(itemIdInDb, dbColumn);

    if (value.isBlank()) {
      itemIdInDb = this.dominionsId.getNationGenId();
      value = db.GetValue(itemIdInDb, dbColumn);
    }

    return value;
  }

  public String getValueFromDb(String dbColumn, String defaultValue) {
    String value = this.getValueFromDb(dbColumn);

    if (value == null || value.isBlank()) {
      return defaultValue;
    }

    return value;
  }

  public Boolean getBooleanFromDb(String dbColumn) {
    String value = this.getValueFromDb(dbColumn);

    if (value.equals("1")) {
      return true;
    }

    return false;
  }

  public Integer getIntegerFromDb(String dbColumn, Integer defauInteger) {
    String value = this.getValueFromDb(dbColumn);

    if (!Generic.isNumeric(value)) {
      return 0;
    }

    return Integer.valueOf(value);
  }

  public String getBardingId() {
    return this.bardingId;
  }

  public int getBardingProtection() {
    int protection = this.nationGen.armordb.GetInteger(this.bardingId, ItemProperty.PROTECTION.toDBColumn(), 0);
    return protection;
  }

  public int getBardingResCost() {
    int rcost = this.nationGen.armordb.GetInteger(this.bardingId, ItemProperty.RESOURCE_COST.toDBColumn(), 0);
    return rcost;
  }

  public Boolean anyTypesMatchString(String typeStr) {
    return this.itemTypes.stream().anyMatch(t -> t.getId() == typeStr);
  }

  public void addType(ItemType type) {
    if (this.hasType(type) == true) {
      return;
    }

    this.itemTypes.add(type);
  }

  public void addType(List<ItemType> types) {
    types.forEach(t -> this.addType(t));
  }

  public Boolean hasType(ItemType type) {
    return this.itemTypes.contains(type);
  }

  public Boolean isOfType(ItemType type) {
    Boolean hasType = this.itemTypes.contains(type);

    if (hasType == false && type.check(this) == true) {
      hasType = true;
      this.itemTypes.add(type);
    }

    return hasType;
  }

  // Armor type for now has to be determined at file-parsing time
  // through the #armor tag. This is because armor and weapon DBs
  // share ids, so any given item id will be present on both.
  public Boolean isArmor() {
    return this.itemTypes.contains(ItemType.ARMOR);
  }
  
  public Boolean isBarding() {
    return this.isArmor() && this.isOfType(ItemType.BARDING);
  }
  
  public Boolean isBodyArmor() {
    return this.isArmor() && this.isOfType(ItemType.BODY_ARMOR);
  }
  
  public Boolean hasHelmet() {
    return this.isArmor() && this.isOfType(ItemType.HELMET);
  }
  
  public Boolean isShield() {
    return this.isArmor() && this.isOfType(ItemType.SHIELD);
  }

  // Weapon type for now has to be determined at file-parsing time
  // through the #armor tag. This is because armor and weapon DBs
  // share ids, so any given item id will be present on both.
  public Boolean isWeapon() {
    return this.isArmor() == false;
  }

  public Boolean isRangedWeapon() {
    return this.isOfType(ItemType.RANGED);
  }

  public Boolean isLowAmmoWeapon() {
    return this.isOfType(ItemType.LOW_SHOTS);
  }

  public Boolean isMeleeWeapon() {
    return this.isOfType(ItemType.MELEE);
  }

  public Boolean isMountItem() {
    return this.isOfType(ItemType.MOUNT);
  }

  public List<ItemType> getItemTypes() {
    return this.itemTypes;
  }

  public Boolean isDominionsEquipment() {
    return this.dominionsId != null && !this.dominionsId.isBlank();
  }

  /**
   * Gets the dominions equipment id that this item contains, if it does.
   * The returning id will be an actual Dominions id number, if it is
   * already resolved, or the CustomItem.name, if it is not yet resolved.
   * @return String Integer or CustomItem.name
   */
  public String getDominionsEquipmentId() {
    if (!this.isDominionsEquipment()) {
      return null;
    }

    if (this.dominionsId.isResolved()) {
      return String.valueOf(this.dominionsId.getIngameId());
    }

    else {
      return this.dominionsId.getNationGenId();
    }
  }

  public Boolean isSameDominionsEquipment(Integer dominionsId) {
    return this.isDominionsEquipment() && this.hasSameDominionsId(dominionsId);
  }

  public Boolean isSameDominionsEquipment(Item otherItem) {
    return this.isDominionsEquipment() &&
      (
        this.hasSameCustomItemName(otherItem) ||
        this.hasSameDominionsId(otherItem)
      );
  }

  public Boolean isDominionsIdAssigned() {
    return this.isDominionsEquipment() && this.dominionsId.isResolved();
  }

  public Boolean hasSameCustomItemName(Item other) {
    return this.hasSameCustomItemName(other.dominionsId.getNationGenId());
  }

  public Boolean hasSameCustomItemName(String otherName) {
    return this.dominionsId.getNationGenId().equals(otherName);
  }

  public Boolean hasSameDominionsId(Item other) {
    return this.hasSameDominionsId(other.dominionsId.getIngameId());
  }

  public Boolean hasSameDominionsId(String otherId) {
    if (!Generic.isNumeric(otherId)) {
      return false;
    }

    return this.hasSameDominionsId(Integer.valueOf(otherId));
  }

  public Boolean hasSameDominionsId(Integer otherId) {
    return this.dominionsId.getIngameId() == otherId;
  }

  /**
   * Returns a copy of this item with its Dominions id resolved, or the same item if already resolved.
   * For example, if this item was defined with #gameid atl_conchshield, that will be resolved to a
   * Dominions id that is useable in-game, such as 3008. Note that we need to return a copy of
   * the item with the original customId because we don't want to modify the original, as that is
   * (probably) an asset instance that gets equipped with the same reference every time.
   * 
   * @param item - an item which contains a Domiions equipment
   * @return a copy of the item with a numeric Dominions id, or the same item of the id was already resolved
   */
  static public Item resolveId(Item item) {
    if (!item.isDominionsEquipment()) {
      throw new IllegalArgumentException("Item does not contain a Dominions equipment with a Dominions id");
    }

    if (!item.isDominionsIdAssigned()) {
      Item copy = new Item(item);
      Integer resolvedDominionsId = item.nationGen
        .GetCustomItemsHandler()
        .getCustomItemId(item.dominionsId.getNationGenId());

      item.dominionsId.setIngameId(resolvedDominionsId);
      return copy;
    }

    // If the id is already numeric, like 5008, it should have
    // already been resolved, so we just return the same item
    else {
      return item;
    }
  }

  @Override
  public void handleOwnCommand(Command command) {
    try {
      switch (command.command) {
        case "#gameid":
          String gameId = command.args.get(0).get();

          if (Generic.isNumeric(gameId)) {
            this.dominionsId.setIngameId(Integer.valueOf(gameId));
          }

          else {
            this.dominionsId.setNationGenId(gameId);
          }
          break;
        case "#armor":
          this.itemTypes.add(ItemType.ARMOR);
          break;
        case "#barding":
          this.bardingId = command.args.getString(0);
          this.itemTypes.add(ItemType.BARDING);
          break;
        case "#mountmnr":
          this.itemTypes.add(ItemType.MOUNT);
          this.addCommands(command);
          break;
        case "#addthemeinc":
          if (this.filter == null) {
            this.filter = new Filter(nationGen);
            filter.tags.addName("do_not_show_in_descriptions");
            if (this.name != null) filter.name = "Item " +
            this.name +
            " generation effects";
          }
          this.filter.themeincs.add(ThemeInc.from(command.args));
          break;
        case "#name":
          if (filter != null) filter.name = "Item " +
          command.args.get(0).get() +
          " generation effects";
          super.handleOwnCommand(command);
          break;
        case "#needs":
          this.dependencies.add(
              new ItemDependency(
                command.args.get(0).get(),
                command.args.get(1).get(),
                false,
                false
              )
            );
          break;
        case "#needstype":
          this.dependencies.add(
              new ItemDependency(
                command.args.get(0).get(),
                command.args.get(1).get(),
                true,
                false
              )
            );
          break;
        case "#forceslot":
          this.dependencies.add(
              new ItemDependency(
                command.args.get(0).get(),
                command.args.get(1).get(),
                false,
                true
              )
            );
          break;
        case "#forceslottype":
          this.dependencies.add(
              new ItemDependency(
                command.args.get(0).get(),
                command.args.get(1).get(),
                true,
                true
              )
            );
          break;
        case "#command":
        case "#define":
          if (command.args.size() != 1) {
            throw new IllegalArgumentException(
              "#command or #define must have a single arg. Surround the command with quotes if needed."
            );
          }
          this.addCommands(command.args.get(0).getCommand());
          break;
        default:
          super.handleOwnCommand(command);
          break;
      }
    } catch (IndexOutOfBoundsException e) {
      throw new IllegalArgumentException(
        "Command [" +
        command +
        "] has insufficient arguments (" +
        this.name +
        ")",
        e
      );
    }
  }

  @Override
  protected void finish() {
    if (this.isDominionsEquipment() == false) {
      return;
    }

    if (this.isArmor()) {
      return;
    }

    // If not an explicit armor type, this must be a weapon
    this.addType(ItemType.WEAPON);

    // Check DB for vanilla item range to determine if it's ranged
    if (this.getIntegerFromDb(ItemProperty.RANGE.toDBColumn(), 0) > 0) {
      this.addType(ItemType.RANGED);
    }

    // If not ranged, this must be melee
    else {
      this.addType(ItemType.MELEE);
    }
  }
}
