package nationGen.naming;

import com.elmokki.Generic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import nationGen.Settings.SettingsType;
import nationGen.entities.Filter;
import nationGen.items.Item;
import nationGen.items.ItemProperty;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.MountUnit;
import nationGen.units.Unit;

public class DescriptionReplacer {

  public HashMap<String, String> descs = new HashMap<String, String>();
  private Nation n;

  private Integer MAX_ITEMS_NAMED = 3;

  public DescriptionReplacer(Nation n) {
    this.n = n;
    descs.put("%nation%", n.name);
    descs.put("%primaryrace%", n.races.get(0).toString());
    descs.put(
      "%primaryrace_plural%",
      Generic.capitalizeFirst(n.races.get(0).toString())
    );
    descs.put("%mages%", MageDescriber.getCommonName(n).toString());
    descs.put(
      "%mages_plural%",
      Generic.plural(MageDescriber.getCommonName(n).toString())
    );
    descs.put("%magenoun%", MageDescriber.getCommonNoun(n).toString());
    descs.put(
      "%sacredname%",
      n.unitlists.get("sacreds").get(0).name.toString()
    );
    descs.put(
      "%sacredname_plural%",
      Generic.capitalizeFirst(n.unitlists.get("sacreds").get(0).name.toString())
    );
  }

  public String replace(String line) {
    for (String key : descs.keySet()) line = line.replaceAll(
      key,
      descs.get(key)
    );

    return line;
  }

  public void calibrate(Unit unit) {
    this.calibrateMounts(unit);
    descs.put("%unitname%", unit.name.toString());
    descs.put("%unitname_plural%", Generic.plural(unit.name.toString()));
    descs.put("%race%", unit.race.toString().toLowerCase());
    descs.put("%race_plural%", Generic.plural(unit.race.toString().toLowerCase()));

    if (unit.hasAnyLeadership()) {
      descs.put("%role%", "commanders");

      if (unit.hasCommand("#holy")) {
        descs.put("%role%", "priests");
      }
    }

    for (Filter f : unit.getMagicFilters()) {
      if (f.name.equals("MAGICPICKS")) {
        descs.put("%role%", "mages");
      }
    }

    if (unit.hasCommand("#fixedname")) {
      for (Command c : unit.gatherCommands()) {
        if (c.command.equals("#fixedname")) descs.put(
          "%fixedname%",
          c.args.get(0).get()
        );
      }

      descs.put("%tobe%", "is");
      descs.put("%tohave%", "has");

      if (unit.hasCommand("#female")) {
        descs.put("%Pronoun%", "She");
        descs.put("%pronoun%", "she");
        descs.put("%pronoun_obj%", "her");
        descs.put("%pronoun_pos%", "her");
      } else {
        descs.put("%Pronoun%", "He");
        descs.put("%pronoun%", "he");
        descs.put("%pronoun_obj%", "him");
        descs.put("%pronoun_pos%", "his");
      }
    } else {
      descs.put("%Pronoun%", "They");
      descs.put("%pronoun%", "they");
      descs.put("%pronoun_obj%", "them");
      descs.put("%pronoun_pos%", "their");
      descs.put("%tobe%", "are");
      descs.put("%tohave%", "have");
      descs.put("%fixedname%", unit.name.toString());
    }
  }

  public void calibrate(List<Unit> units) {
    this.calibrateWeapons(units);
    this.calibrateArmors(units);
  }

  public void calibrateWeapons(List<Unit> units) {
    List<String> slots = List.of("weapon", "offhand");
    List<Item> weapons = this.getEquipments(units, slots).stream().filter(Item::isWeapon).toList();
    List<Phrase> weaponDescriptions = this.describeEquipmentList(weapons, "name");
    List<String> weaponSingulars = weaponDescriptions.stream().map(p -> p.singular).toList();
    List<String> weaponPlurals = weaponDescriptions.stream().map(p -> p.plural).toList();

    if (weaponDescriptions.size() == 0) {
      descs.put("%weapons%", "unknown weapons");
      descs.put("%weapons_plural%", "unknown weapons");
    }

    else if (weaponDescriptions.size() <= MAX_ITEMS_NAMED) {
      String singularDescription = NameGenerator.writeAsList(weaponSingulars, false, "or");
      String pluralDescription = NameGenerator.writeAsList(weaponPlurals, false);

      descs.put("%weapons%", singularDescription);
      descs.put("%weapons_plural%", pluralDescription);
    }

    else {
      List<String> damageTypes = this.getWeaponDamageTypes(weapons);
      String damageTypesDescription = NameGenerator.writeAsList(damageTypes, false) + " weapons";

      if (damageTypes.size() > MAX_ITEMS_NAMED) {
        descs.put("%weapons%", "various weapons");
        descs.put("%weapons_plural%", "various weapons");
      }

      else {
        descs.put("%weapons%", damageTypesDescription);
        descs.put("%weapons_plural%", damageTypesDescription);
      }
    }
  }

  public void calibrateArmors(List<Unit> units) {
    List<String> armorSlot = List.of("armor");
    List<String> armorNames = new ArrayList<>();
    List<Item> armors = this.getEquipments(units, armorSlot).stream().filter(Item::isArmor).toList();
    
    int minProt = 100;
    int maxProt = 0;

    for (Item armor : armors) {
      int prot = 0;
      String armorName = armor.getValueFromDb(ItemProperty.NAME.toDBColumn(), "NOT IN ARMORDB: " + armor.name + " in armor slot");
      prot = armor.getIntegerFromDb(ItemProperty.PROTECTION.toDBColumn(), 0);

      if (armorNames.contains(armorName) == false) {
        armorNames.add(armorName);
        minProt = Math.min(minProt, prot);
        maxProt = Math.max(maxProt, prot);
      }
    }

    descs.put("%armors%", NameGenerator.writeAsList(armorNames, false));
    this.calibrateArmorTypes(minProt, maxProt);
  }

  public void calibrateArmorTypes(Integer minProt, Integer maxProt) {
    List<String> typeNames = new ArrayList<>();
    Integer eraModifier = n.nationGen.settings.get(SettingsType.era).intValue();

    if (minProt <= 11 + eraModifier) {
      typeNames.add("light");
    }

    if (minProt <= 16 + eraModifier && maxProt > 11 + eraModifier) {
      typeNames.add("medium");
    }

    if (maxProt > 16 + eraModifier) {
      typeNames.add("heavy");
    }

    if (typeNames.size() >= 3 || typeNames.size() == 0) {
      typeNames.clear();
      typeNames.add("all kinds of");
    }

    descs.put("%armortypes%", NameGenerator.writeAsList(typeNames, false));
  }

  public void calibrateMounts(Unit unit) {
    // If unit is a montag template, there may be multiple mounts
    List<MountUnit> mounts = this.getMounts(List.of(unit));
    List<Phrase> mountDescriptions = this.describeMountList(mounts);
    List<String> mountSingulars = mountDescriptions.stream().map(p -> p.singular).toList();
    List<String> mountPlurals = mountDescriptions.stream().map(p -> p.plural).toList();

    if (mountDescriptions.size() == 0) {
      descs.put("%mount%", "unknown beasts");
      descs.put("%mount_plural%", "unknown beasts");
    }

    else if (mountDescriptions.size() <= MAX_ITEMS_NAMED) {
      String singularDescription = NameGenerator.writeAsList(mountSingulars, false, "or");
      String pluralDescription = NameGenerator.writeAsList(mountPlurals, false);

      descs.put("%mount%", singularDescription);
      descs.put("%mount_plural%", pluralDescription);
    }

    else {
      descs.put("%weapons%", "various mounts");
      descs.put("%weapons_plural%", "various mounts");
    }
  }

  public List<Item> getEquipments(List<Unit> units, List<String> slots) {
    List<Item> equipments = new ArrayList<>();

    for (Unit unit : units) {
      for (String slotName : slots) {
        if (unit.isMontagRecruitableTemplate()) {
          List<Unit> montagUnits = unit.getMontagShapes();
          List<Item> montagItems = this.getEquipments(montagUnits, slots);
          equipments.addAll(montagItems);
        }

        else {
          Item item = unit.getSlot(slotName);
          equipments.add(item);
        }
      }
    }

    // Filter out non-equipment items
    equipments = equipments.stream()
      .filter(i -> i != null)
      // Mount items all have a gameid = -1, but they are still equipped with #mountmnr
      .filter(i -> i.isDominionsEquipment() || i.isMountItem())
      .collect(Collectors.toList());

    return equipments;
  }

  public List<MountUnit> getMounts(List<Unit> units) {
    List<MountUnit> mounts = new ArrayList<>();

    for (Unit unit : units) {
      if (unit.isMontagRecruitableTemplate()) {
        List<Unit> montagUnits = unit.getMontagShapes();
        List<MountUnit> montagMounts = this.getMounts(montagUnits);
        mounts.addAll(montagMounts);
      }

      else {
        MountUnit mount = unit.getMountUnit();
        mounts.add(mount);
      }
    }

    return mounts.stream().filter(mu -> mu != null).toList();
  }

  public List<Phrase> describeEquipmentList(List<Item> equipments, String itemNameDbColumn) {
    List<Phrase> descriptions = new ArrayList<>();
    List<String> addedNames = new ArrayList<>();

    for (Item item : equipments) {
      String itemName = item.getValueFromDb(itemNameDbColumn);
      Boolean nameExistsInDb = itemName.isBlank() == false;

      if (nameExistsInDb == false) {
        String notInDb = "NOT IN DB: " + item.name + " in " + item.slot + " slot";
        descriptions.add(new Phrase(notInDb, notInDb));
      }

      else if (!addedNames.contains(itemName)) {
        String lowerCasedName = itemName.toLowerCase();
        String nameWithPreposition = NameGenerator.addPreposition(lowerCasedName);
        String plural = NameGenerator.plural(lowerCasedName);

        descriptions.add(new Phrase(nameWithPreposition, plural));
        addedNames.add(itemName);
      }
    }

    return descriptions;
  }

  public List<Phrase> describeMountList(List<MountUnit> mountUnitList) {
    List<Phrase> descriptions = new ArrayList<>();
    List<String> addedNames = new ArrayList<>();
    
    for (MountUnit mountUnit : mountUnitList) {
      String mountName = mountUnit.mount.getName().toLowerCase();
      String nameWithPreposition = NameGenerator.addPreposition(mountName);
      String plural = NameGenerator.plural(mountName);

      if (!addedNames.contains(mountName)) {
        descriptions.add(new Phrase(nameWithPreposition, plural));
        addedNames.add(mountName);
      }
    }

    return descriptions;
  }

  public List<String> getWeaponDamageTypes(List<Item> weapons) {
    List<String> types = new ArrayList<>();
    List<String> addedNames = new ArrayList<>();

    for (Item weapon : weapons) {
      List<String> weaponDamageTypes = new ArrayList<String>();
      Boolean isBlunt = weapon.getValueFromDb("dt_blunt").equals("1");
      Boolean isSlash = weapon.getValueFromDb("dt_slash").equals("1");
      Boolean isPierce = weapon.getValueFromDb("dt_pierce").equals("1");
      String untypedDamage = "crushing";

      if (isBlunt) {
        weaponDamageTypes.add("blunt");
      }

      if (isSlash) {
        weaponDamageTypes.add("slashing");
      }

      if (isPierce) {
        weaponDamageTypes.add("piercing");
      }

      if (weaponDamageTypes.size() == 0) {
        weaponDamageTypes.add(untypedDamage);
      }

      for (String damageType : weaponDamageTypes) {
        if (!damageType.isBlank() && !addedNames.contains(damageType)) {
          types.add(damageType);
        }
      }
    }

    return types;
  }
}
