package nationGen.items;

import nationGen.misc.Command;

public enum WeaponNumericalDamageType {
  DT_ACID(ItemProperty.DT_ACID),
  DT_BLUNT(ItemProperty.DT_BLUNT),
  DT_COLD(ItemProperty.DT_COLD),
  DT_DRAIN(ItemProperty.DT_DRAIN),
  DT_FIRE(ItemProperty.DT_FIRE),
  DT_HOLY(ItemProperty.DT_HOLY),
  DT_NORM(ItemProperty.DT_NORM),
  DT_PIERCE(ItemProperty.DT_PIERCE),
  DT_POISON(ItemProperty.DT_POISON),
  DT_SHOCK(ItemProperty.DT_SHOCK),
  DT_SLASH(ItemProperty.DT_SLASH),
  DT_STUN(ItemProperty.DT_STUN),
  DT_WEAPON_DRAIN(ItemProperty.DT_WEAPON_DRAIN),
  DT_WEAKNESS(ItemProperty.DT_WEAKNESS);

  private ItemProperty property;

  WeaponNumericalDamageType(ItemProperty property) {
    this.property = property;
  }

  public ItemProperty getProperty() {
    return this.property;
  }

  public static Boolean isModCommandANumericalDamageType(Command command) {
    return isModCommandANumericalDamageType(command.command);
  }

  public static Boolean isModCommandANumericalDamageType(String command) {
    for (WeaponNumericalDamageType type : WeaponNumericalDamageType.values()) {
      if (type.getProperty().getModCommand().equals(command)) {
        return true;
      }
    }

    return false;
  }
}
