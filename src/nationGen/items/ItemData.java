package nationGen.items;

import nationGen.NationGen;

public class ItemData {
  private String id = "-1";
  private String name = "";
  private NationGen nationGen;

  public ItemData(String id, String name, NationGen nationGen) {
    this.id = id;
    this.name = name;
    this.nationGen = nationGen;
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

  // TODO: work out a way to calculate a customitem's range. Need to fetch its #range command
  // defined originally, through nationGen.customItemsHandler().customItems. In cases of negative
  // #range values that scale up with strength, we can possibly return them directly, since the same
  // strength scaling on the same unit will result in the same range. But if it's for sorting by range,
  // have to make sure of whether lower negatives are lower or higher strength scaling
  public int getWeaponRange() {
    
    return nationGen.weapondb.GetInteger(
      id,
      "rng",
      0
    );
  }
}
