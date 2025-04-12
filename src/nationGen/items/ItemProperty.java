package nationGen.items;

public enum ItemProperty {
  AN("an", "#armornegating", true),
  ANIM_LENGTH("animlength", ""),
  AOE("aoe", "#aoe"),
  AP("ap", "#armorpiercing", true),
  AMMO("shots", "#ammo"),
  ATTACK("att", "#att"),
  BOW_STR("bowstr", "#bowstr", true),
  CHARGE_BONUS("charge", "#charge", true),
  DEFENCE("def", "#def"),
  DEMON_ONLY("demononly", "#demononly", true),
  DAMAGE("dmg", "#dmg"),
  DT_ACID("acid", "#acid", true),
  DT_BLUNT("dt_blunt", "#blunt", true),
  DT_CAPPED("dt_cap", "#dt_cap", true),
  DT_COLD("cold", "#cold", true),
  DT_DEMON("dt_demon", "#dt_demon", true),
  DT_DRAIN("dt_drain", "#dt_drain", true),
  DT_FIRE("fire", "#fire", true),
  DT_HOLY("dt_holy", "#dt_holy", true),
  DT_LARGE("dt_large", "#dt_large", true),
  DT_MAGIC_BEING("dt_magic", "#dt_magic", true),
  DT_MIND("mind", "#mind", true),
  DT_NORM("dt_norm", "#dt_normal", true),
  DT_PARALYZE("dt_paralyze", "#dt_paralyze", true),
  DT_PIERCE("dt_pierce", "#pierce", true),
  DT_POISON("dt_poison", "#dt_poison", true),
  DT_RAISE("dt_raise", "#dt_raise", true),
  DT_SHOCK("shock", "#shock", true),
  DT_SLASH("dt_slash", "#slash", true),
  DT_SMALL("dt_small", "#dt_small", true),
  DT_STUN("dt_stun", "#dt_stun", true),
  DT_WEAPON_DRAIN("dt_weapondrain", "#dt_weapondrain", true),
  DT_WEAKNESS("dt_weakness", "#dt_weakness", true),
  FLYSPRITE("flyspr", "#flyspr"),
  FULL_STR("fullstr", "#fullstr", true),
  HALF_STR("halfstr", "#halfstr", true),
  INTRINSIC("bonus", "#bonus", true),
  IS_2H("2h", "#twohanded", true),
  IS_FLAIL("flail", "#flail", true),
  IS_IRON("ironweapon", "#ironweapon"),
  IS_MAGIC("magic", "#magic", true),
  IS_WOOD("woodenweapon", "#woodenweapon", true),
  LENGTH("lgt", "#len"),
  MM_PENALTY("mmpenalty", "#enc"),
  MR_NEGATES("mrnegates", "#mrnegates", true),
  NAME("weapon_name", "#name"),
  NO_REPEL("norepel", "#norepel", true),
  NO_STR("nostr", "#nostr", true),
  NUM_ATTACKS("#att", "#nratt"),
  PRECISION("att", "#att"),
  RANGE("rng", "#range"),
  RES_COST("res", "#rcost"),
  SACRED_ONLY("sacredonly", "#sacredonly", true),
  SEC_EFF("secondaryeffect", "#secondaryeffect"),
  SEC_EFF_ALWAYS("secondaryeffectalways", "#secondaryeffectalways"),
  SIZE_RESIST("sizeresist", "#sizeresist", true),
  THIRD_STR("thirdstr", "#thirdstr", true);

  private String dbColumn;
  private String modCommand;
  private Boolean isBooleanProperty = false;

  ItemProperty(String dbColumn, String modCommand) {
    this.dbColumn = dbColumn;
    this.modCommand = modCommand;
  }

  ItemProperty(String dbColumn, String modCommand, Boolean isBoolean) {
    this.dbColumn = dbColumn;
    this.modCommand = modCommand;
    this.isBooleanProperty = isBoolean;
  }

  public String getModCommand() {
    return this.modCommand;
  }

  public String getDBColumn() {
    return this.dbColumn;
  }

  public Boolean isBoolean() {
    return this.isBooleanProperty;
  }
}
