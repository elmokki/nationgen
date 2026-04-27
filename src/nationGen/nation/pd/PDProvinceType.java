package nationGen.nation.pd;

public enum PDProvinceType {
  FORTED_OR_WITH_20PD("#defunit1", "#defmult1", 4),
  FORTED_WITH_20PD("#defunit2", "#defmult2", 2);

  private String baseModCommand;
  private String multiplierModCommand;
  private int maxUnitTypes;

  PDProvinceType(String baseModCommand, String multiplierModCommand, int maxUnitTypes) {
    this.baseModCommand = baseModCommand;
    this.multiplierModCommand = multiplierModCommand;
    this.maxUnitTypes = maxUnitTypes;
  }

  public String getBaseModCommand() {
    return this.baseModCommand;
  }

  public String getMultiplierModCommand() {
    return this.multiplierModCommand;
  }

  public int getMaxUnitTypes() {
    return this.maxUnitTypes;
  }
}
