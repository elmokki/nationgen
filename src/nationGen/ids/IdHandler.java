package nationGen.ids;

import java.util.Properties;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.misc.FileUtil;

public class IdHandler {
  
  public final static int MIN_CUSTOM_ARMOR_ID = 300;
  public final static int MAX_CUSTOM_ARMOR_ID = 999;

  public final static int MIN_CUSTOM_MONSTER_ID = 5000;
  public final static int MAX_CUSTOM_MONSTER_ID = 8999;

  public final static int MIN_CUSTOM_MONTAG_ID = 1000;
  public final static int MAX_CUSTOM_MONTAG_ID = 100000;

  public final static int MIN_CUSTOM_NAMETYPE_ID = 170;
  public final static int MAX_CUSTOM_NAMETYPE_ID = 399;

  public final static int MIN_CUSTOM_NATION_ID = 150;
  public final static int MAX_CUSTOM_NATION_ID = 499;

  public final static int MIN_CUSTOM_SITE_ID = 1700;
  public final static int MAX_CUSTOM_SITE_ID = 3999;

  public final static int MIN_CUSTOM_SPELL_ID = 1300;
  public final static int MAX_CUSTOM_SPELL_ID = 3999;

  public final static int MIN_CUSTOM_WEAPON_ID = 1000;
  public final static int MAX_CUSTOM_WEAPON_ID = 3999;

  
  private int lastAssignedArmorId = MIN_CUSTOM_ARMOR_ID;
  private int lastAssignedMonsterId = MIN_CUSTOM_MONSTER_ID;
  private int lastAssignedMontagId = MIN_CUSTOM_MONTAG_ID;
  private int lastAssignedNametypeId = MIN_CUSTOM_NAMETYPE_ID;
  private int lastAssignedNationId = MIN_CUSTOM_NATION_ID;
  private int lastAssignedSiteId = MIN_CUSTOM_SITE_ID;
  private int lastAssignedSpellId = MIN_CUSTOM_SPELL_ID;
  private int lastAssignedWeaponId = MIN_CUSTOM_WEAPON_ID;

  private int maxAllowedArmorId = MAX_CUSTOM_ARMOR_ID;
  private int maxAllowedMonsterId = MAX_CUSTOM_MONSTER_ID;
  private int maxAllowedMontagId = MAX_CUSTOM_MONTAG_ID;
  private int maxAllowedNametypeId = MAX_CUSTOM_NAMETYPE_ID;
  private int maxAllowedNationId = MAX_CUSTOM_NATION_ID;
  private int maxAllowedSiteId = MAX_CUSTOM_SITE_ID;
  private int maxAllowedSpellId = MAX_CUSTOM_SPELL_ID;
  private int maxAllowedWeaponId = MAX_CUSTOM_WEAPON_ID;


  public IdHandler() {
    this.loadConfiguration();
  }

  public int nextArmorId() {
    int nextId = this.lastAssignedArmorId+1;

    if (nextId > this.maxAllowedArmorId) {
      throw new IllegalStateException(
        "Max armor id reached! Cannot assign id " +
        nextId +
        " as it is above the maximum of " +
        this.maxAllowedArmorId
      );
    }

    this.lastAssignedArmorId = nextId;
    return nextId;
  }

  public int nextMontagId() {
    int nextId = this.lastAssignedMontagId+1;

    if (nextId > this.maxAllowedMontagId) {
      throw new IllegalStateException(
        "Max montag id reached! Cannot assign id " +
        nextId +
        " as it is above the maximum of " +
        this.maxAllowedMontagId
      );
    }

    this.lastAssignedMontagId = nextId;
    return nextId;
  }

  public int nextNametypeId() {
    int nextId = this.lastAssignedNametypeId+1;

    if (nextId > this.maxAllowedNametypeId) {
      throw new IllegalStateException(
        "Max nametype id reached! Cannot assign id " +
        nextId +
        " as it is above the maximum of " +
        this.maxAllowedNametypeId
      );
    }

    this.lastAssignedNametypeId = nextId;
    return nextId;
  }

  public int nextNationId() {
    int nextId = this.lastAssignedNationId+1;

    if (nextId > this.maxAllowedNationId) {
      throw new IllegalStateException(
        "Max nation id reached! Cannot assign id " +
        nextId +
        " as it is above the maximum of " +
        this.maxAllowedNationId
      );
    }

    this.lastAssignedNationId = nextId;
    return nextId;
  }

  public int nextSiteId() {
    int nextId = this.lastAssignedSiteId+1;

    if (nextId > this.maxAllowedSiteId) {
      throw new IllegalStateException(
        "Max site id reached! Cannot assign id " +
        nextId +
        " as it is above the maximum of " +
        this.maxAllowedSiteId
      );
    }

    this.lastAssignedSiteId = nextId;
    return nextId;
  }

  public int nextSpellId() {
    int nextId = this.lastAssignedSpellId+1;

    if (nextId > this.maxAllowedSpellId) {
      throw new IllegalStateException(
        "Max spell id reached! Cannot assign id " +
        nextId +
        " as it is above the maximum of " +
        this.maxAllowedSpellId
      );
    }

    this.lastAssignedSpellId = nextId;
    return nextId;
  }

  public int nextUnitId() {
    int nextId = this.lastAssignedMonsterId+1;

    if (nextId > this.maxAllowedMonsterId) {
      throw new IllegalStateException(
        "Max unit id reached! Cannot assign id " +
        nextId +
        " as it is above the maximum of " +
        this.maxAllowedMonsterId
      );
    }

    this.lastAssignedMonsterId = nextId;
    return nextId;
  }

  public int nextWeaponId() {
    int nextId = this.lastAssignedWeaponId+1;

    if (nextId > this.maxAllowedWeaponId) {
      throw new IllegalStateException(
        "Max weapon id reached! Cannot assign id " +
        nextId +
        " as it is above the maximum of " +
        this.maxAllowedWeaponId
      );
    }

    this.lastAssignedWeaponId = nextId;
    return nextId;
  }

  static private boolean isCustomGameId(int id, int minInclusive, int maxInclusive) {
    return id >= minInclusive && id <= maxInclusive;
  }

  static public boolean isCustomArmorGameId(int id) {
    return isCustomGameId(id, IdHandler.MIN_CUSTOM_ARMOR_ID, IdHandler.MAX_CUSTOM_ARMOR_ID);
  }

  static public boolean isCustomWeaponGameId(int id) {
    return isCustomGameId(id, IdHandler.MIN_CUSTOM_WEAPON_ID, IdHandler.MAX_CUSTOM_WEAPON_ID);
  }

  private void loadConfiguration() {
    Properties allowedIdsConfiguration = FileUtil.readProperties(NationGen.appPropertiesPath);

    lastAssignedArmorId = loadIntProperty(allowedIdsConfiguration, "min.custom.armor.id");
    lastAssignedMonsterId = loadIntProperty(allowedIdsConfiguration, "min.custom.monster.id");
    lastAssignedMontagId = loadIntProperty(allowedIdsConfiguration, "min.custom.montag.id");
    lastAssignedNametypeId = loadIntProperty(allowedIdsConfiguration, "min.custom.nametype.id");
    lastAssignedNationId = loadIntProperty(allowedIdsConfiguration, "min.custom.nation.id");
    lastAssignedSiteId = loadIntProperty(allowedIdsConfiguration, "min.custom.site.id");
    lastAssignedSpellId = loadIntProperty(allowedIdsConfiguration, "min.custom.spell.id");
    lastAssignedWeaponId = loadIntProperty(allowedIdsConfiguration, "min.custom.weapon.id");

    maxAllowedArmorId = loadIntProperty(allowedIdsConfiguration, "max.custom.armor.id");
    maxAllowedMonsterId = loadIntProperty(allowedIdsConfiguration, "max.custom.monster.id");
    maxAllowedMontagId = loadIntProperty(allowedIdsConfiguration, "max.custom.montag.id");
    maxAllowedNametypeId = loadIntProperty(allowedIdsConfiguration, "max.custom.nametype.id");
    maxAllowedNationId = loadIntProperty(allowedIdsConfiguration, "max.custom.nation.id");
    maxAllowedSiteId = loadIntProperty(allowedIdsConfiguration, "max.custom.site.id");
    maxAllowedSpellId = loadIntProperty(allowedIdsConfiguration, "max.custom.spell.id");
    maxAllowedWeaponId = loadIntProperty(allowedIdsConfiguration, "max.custom.weapon.id");
  }

  private int loadIntProperty(Properties config, String propertyKey) {
    String value = config.getProperty(propertyKey);

    if (!Generic.isNumeric(value)) {
      throw new IllegalArgumentException("Expected value to be an integer; instead got <" + value + ">");
    }

    return Integer.parseInt(value);
  }
}
