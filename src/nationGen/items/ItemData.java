package nationGen.items;

import java.util.Optional;

import nationGen.CustomItemsHandler;
import nationGen.NationGen;

public class ItemData {
  private String id = "-1";
  private String oldId = "";
  private String name = "";
  private NationGen nationGen;

  public ItemData(String id, String name, NationGen nationGen) {
    this.id = id;
    this.name = name;
    this.nationGen = nationGen;
  }

  public ItemData(Item item) {
    if (item instanceof CustomItem) {
      this.oldId = item.tags.getString("OLDID").get();
    }

    this.id = item.id;
    this.name = item.name;
    this.nationGen = item.nationGen;
  }

  public String getId() {
    return this.id;
  }

  public Boolean hasName() {
    return !this.name.isBlank();
  }

  public String getName() {
    return this.name;
  }

  public String getDisplayName(String dbColumn) {
    String dominionsName = nationGen.weapondb.GetValue(
      id,
      dbColumn,
      ""
    );

    if (dominionsName.isBlank()) {
      return this.name;
    }

    return dominionsName;
  }

  public int getWeaponRange(int unitStrength) {
    CustomItemsHandler customItemsHandler = nationGen.GetCustomItemsHandler();
    Optional<CustomItem> customItem = customItemsHandler.getCustomItem(this.oldId);
    Optional<Integer> possibleRange;
    int range;
    int strengthScaling;

    // If not a custom item, just use the Dominions database to determine range
    if (customItem.isPresent() == false) {
      range = nationGen.weapondb.GetInteger(id, "rng", 0);
      return range;
    }
    
    possibleRange = customItem.get().getIntValue("#range");

    if (possibleRange.isPresent() == false) {
      return 0;
    }

    range = possibleRange.get();

    if (range >= 0) {
      return range;
    }

    // Range values might be negative (-1, -2, -3) for those ranges that scale with strength
    // -1 range is full strength, -2 is half strength (/2), -3 is / 3
    strengthScaling = Math.abs(range);
    return unitStrength / strengthScaling;
  }
}
