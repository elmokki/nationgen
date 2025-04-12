package nationGen.items;

import com.elmokki.Dom3DB;
import com.elmokki.Generic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import nationGen.chances.ChanceDistribution;
import nationGen.chances.EntityChances;
import nationGen.entities.MagicItem;
import nationGen.misc.Arg;
import nationGen.misc.Args;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class CustomItemGen {

  Nation n;
  Random random;

  public CustomItemGen(Nation n) {
    this.n = n;
    random = new Random(n.random.nextInt());
  }

  public Optional<CustomItem> generateMagicItem(
    Unit unit,
    Item olditem,
    int maxpower,
    double powerUpChance,
    List<MagicItem> magicItems
  ) {
    Optional<CustomItem> maybeCustomizableItem = this.copyPropertiesFromItem(olditem);

    maybeCustomizableItem.ifPresent(customizableItem -> {
      customizeItem(unit, olditem, customizableItem, maxpower, powerUpChance, magicItems);
    });

    return maybeCustomizableItem;
  }

  private void customizeItem(
    Unit unit,
    Item originalItem,
    CustomItem customItem,
    int maxpower,
    double powerUpChance,
    List<MagicItem> magicItems
  ) {
    // Roll chance to make the item magic
    boolean isMagic = rollMagicItemChance(originalItem, customItem);

    // Roll number of power ups item will get
    int powerUpBudget = rollNumberOfPowerUps(powerUpChance);

    // If rolled magic, set the property
    if (isMagic == true) {
      customItem.setValue("#magic");
    }

    // Spend the power up budget into item upgrades
    this.spendPowerUpBudget(powerUpBudget, originalItem, customItem);

    // Add cost to unit based on item
    this.addToUnitCost(unit, powerUpBudget, isMagic);

    // Roll chance to make item even more epic
    if (random.nextDouble() > powerUpChance) {
      this.makeEpic(unit, originalItem, customItem, maxpower, magicItems);
    }
    
    // Name the custom item
    this.nameCustomItem(originalItem, customItem, isMagic);

    // Add magic item tags
    if (customItem.magicItem != null) {
      customItem.tags.addAll(customItem.magicItem.tags);
    }

    // Add to custom item lists
    n.customitems.add(customItem);
    n.nationGen.GetCustomItemsHandler().AddCustomItem(customItem);
    n.nationGen.weapondb.addToMap(customItem.id, customItem.getHashMap());
  }

  private Boolean rollMagicItemChance(
    Item originalItem,
    CustomItem customItem
  ) {
    return !originalItem.isRangedWeapon() &&
      customItem.magicItem != null &&
      random.nextDouble() > 0.75;
  }

  private int rollNumberOfPowerUps(double powerUpChance) {
    int powerUps = 1 + random.nextInt(1);
    if (random.nextDouble() > powerUpChance) powerUps++;
    if (random.nextDouble() > powerUpChance / 2) powerUps++;
    if (random.nextDouble() > powerUpChance / 4) powerUps++;
    return powerUps;
  }

  private void spendPowerUpBudget(int powerUpBudget, Item originalItem, CustomItem customItem) {
    Boolean isRangedWeapon = originalItem.isRangedWeapon();
    Boolean isLowAmmoWeapon = originalItem.isLowAmmoWeapon();
    Boolean hasDamageBitmask = customItem.hasDamageBitmask();
    ChanceDistribution<ItemPropertyPowerUp> powerUps = getPowerUpChances(isRangedWeapon, hasDamageBitmask);

    while (powerUpBudget > 0) {
      ItemPropertyPowerUp chosenPowerUp = powerUps.getRandom(random);
      powerUpBudget--;

      if (chosenPowerUp.isBoolean() == true) {
        customItem.setValue(chosenPowerUp.getProperty().getModCommand());
        powerUps.eliminate(chosenPowerUp);
      }

      else {
        ItemProperty property = chosenPowerUp.getProperty();
        String modCommand = property.getModCommand();
        int powerUpIncrease = chosenPowerUp.getIncrease();
        int originalValue = customItem.getIntValue(modCommand).orElseThrow();
        customItem.setValue(modCommand, originalValue + powerUpIncrease);
        powerUps.modifyChance(chosenPowerUp, new Arg("*0.33"));
      }
    }
  }

  private ChanceDistribution<ItemPropertyPowerUp> getPowerUpChances(Boolean isRangedWeapon, Boolean hasDamageBitmask) {
    ChanceDistribution<ItemPropertyPowerUp> powerUpChances = new ChanceDistribution<>();

    ItemPropertyPowerUp attackPowerUp = new ItemPropertyPowerUp(ItemProperty.ATTACK)
      .setCost(1)
      .setIncrease(1)
      .setChance(0.25);
      
    ItemPropertyPowerUp magicPowerUp = new ItemPropertyPowerUp(ItemProperty.IS_MAGIC, true)
      .setCost(1)
      .setChance(0.25);

    powerUpChances.setChance(attackPowerUp, attackPowerUp.getChance());
    powerUpChances.setChance(magicPowerUp, magicPowerUp.getChance());

    // Ranged weapons shouldn't get any defense power ups
    if (isRangedWeapon == false) {
      ItemPropertyPowerUp defencePowerUp = new ItemPropertyPowerUp(ItemProperty.DEFENCE)
        .setCost(1)
        .setIncrease(1)
        .setChance(0.25);

      powerUpChances.setChance(defencePowerUp, defencePowerUp.getChance());
    }

    // Weapons with a damage bitmask (such as those with dt_aff) should not get damage power ups
    if (hasDamageBitmask == false) {
      ItemPropertyPowerUp damagePowerUp = new ItemPropertyPowerUp(ItemProperty.DAMAGE)
        .setCost(1)
        .setIncrease(1)
        .setChance(0.25);

      powerUpChances.setChance(damagePowerUp, damagePowerUp.getChance());
    }

    return powerUpChances;
  }

  private void addToUnitCost(Unit unit, int powerUps, boolean isMagic) {
    // If item is magic, add more cost
    int itemGoldCost = Math.max((int) Math.round(1.5 * (double) powerUps), 3);
    int itemResCost = Math.max((int) Math.round(1.5 * (double) powerUps), 3);

    // Use item cost to calculate extra cost to the unit (either item cost or 10% of unit cost)
    int extraUnitGoldCost = (int)Math.max(itemGoldCost, unit.getGoldCost() * 0.1);
    int extraUnitResCost = (int)Math.max(itemResCost, unit.getResCost(true) * 0.1);

    // Add the costs to the unit
    unit.commands.add(Command.args("#gcost", "+" + extraUnitGoldCost));
    unit.commands.add(Command.args("#rcost", "+" + extraUnitResCost));
  }

  private void makeEpic(
    Unit unit,
    Item originalItem,
    CustomItem customItem,
    int maxpower,
    List<MagicItem> magicItems
  ) {
    Boolean isRangedWeapon = originalItem.isRangedWeapon();
    Boolean isLowAmmoWeapon = originalItem.isLowAmmoWeapon();
    String type = isRangedWeapon ? isLowAmmoWeapon ? "lowshots" : "ranged" : "melee";
    List<MagicItem> possibles = new ArrayList<>();

    for (MagicItem m : magicItems) {
      // Checks whether each of the items in the magicItems list is
      // compatible with this custom item (lowshots, ranged or melee)
      boolean typeIsNotRestricted = m.tags
        .getAllStrings("no")
        .stream()
        .noneMatch(type::equals);

      if (typeIsNotRestricted && m.power <= maxpower) {
        possibles.add(m);
      }
    }

    // No possible epic items, return early
    if (possibles.size() == 0) {
      return;
    }

    ChanceIncHandler chandler = new ChanceIncHandler(n, "customitemgenerator");
    MagicItem magicItem = chandler.handleChanceIncs(unit, possibles).getRandom(random);

    // Special looks
    List<Item> pos = new ArrayList<>();
    for (Args args : magicItem.tags.getAllArgs("weapon")) {
      if (args.size() > 1 && args.get(0).get().equals(originalItem.id)) {
        Item temp = unit.pose
          .getItems(originalItem.slot)
          .getItemWithName(args.get(1).get(), originalItem.slot);
        if (temp != null) pos.add(temp);
      }
    }

    if (!pos.isEmpty()) {
      Optional<CustomItem> customMagicItem =
        this.copyPropertiesFromItem(EntityChances.baseChances(pos).getRandom(random));

      if (customMagicItem.isPresent()) {
        customItem = customMagicItem.get();
      }
    }

    for (Command c : magicItem.getCommands()) {
      String key = c.command;

      if (c.args.size() > 0) {
        Arg value = c.args.get(0);

        Optional<Integer> oldvalue = customItem.getIntValue(key);

        int temp = (int) value.applyModTo(oldvalue.orElseThrow());
        customItem.setValue(key, temp);
      } else {
        customItem.setValue(c.command);
      }
    }

    if (!magicItem.effect.equals("-1")) {
      customItem.setValue("#secondaryeffect", magicItem.effect);
    }

    String name = n.nationGen.weapondb.GetValue(originalItem.id, "weapon_name");

    if (magicItem.nameSuffixes.size() > 0 || magicItem.namePrefixes.size() > 0) {
      String part;
      int rand =
        random.nextInt(
          magicItem.nameSuffixes.size() + magicItem.namePrefixes.size()
        ) +
        1;
      if (
        rand > magicItem.nameSuffixes.size() && magicItem.nameSuffixes.size() > 0
      ) {
        part = magicItem.nameSuffixes.get(
          random.nextInt(magicItem.nameSuffixes.size())
        );
        name = Generic.capitalize(name + " " + part);
      } else {
        part = magicItem.namePrefixes.get(
          random.nextInt(magicItem.namePrefixes.size())
        );
        name = Generic.capitalize(part + " " + name);
      }

      customItem.setValue("#name", name);
    }

    customItem.magicItem = magicItem;

    // Increase gcost
    for (Args args : magicItem.tags.getAllArgs("gcost")) {
      if (args.get(0).get().equals(type)) customItem.commands.add(
        new Command("#gcost", args.get(1))
      );
    }
    for (Args args : magicItem.tags.getAllArgs("rcost")) {
      if (args.get(0).get().equals(type)) customItem.commands.add(
        new Command("#rcost", args.get(1))
      );
    }
    customItem.tags.addAll(magicItem.tags);
  }

  private void nameCustomItem(Item originalItem, CustomItem customItem, Boolean isMagic) {
    String name = n.nationGen.weapondb.GetValue(originalItem.id, "weapon_name");

    if (!isMagic && (customItem.magicItem == null || !customItem.isNamed())) {
      customItem.setValue("#name", "Exceptional " + name);
    }

    else if (isMagic && (customItem.magicItem == null || !customItem.isNamed())) {
      customItem.setValue("#name", "Enchanted " + name);
    }

    String dname = "nation_" + n.nationid + "_customitem_" + (n.customitems.size() + 1);

    customItem.id = dname;
    customItem.name = dname;
  }

  public Optional<CustomItem> copyPropertiesFromItem(Item item) {
    if (item == null) {
      return Optional.empty();
    }

    if (!Generic.isNumeric(item.id)) {
      return Optional.empty();
    }

    // Custom armor not done
    if (item.armor == true) {
      return copyPropertiesFromArmor(item);
    }

    else {
      return copyPropertiesFromWeapon(item);
    }
  }

  // TODO: custom armors!
  public Optional<CustomItem> copyPropertiesFromArmor(Item item) {
    CustomItem customItem = CustomItem.fromItem(item, n.nationGen);
    return Optional.of(customItem);
  }

  public Optional<CustomItem> copyPropertiesFromWeapon(Item item) {
    CustomItem customItem = CustomItem.fromItem(item, n.nationGen);
    Dom3DB weaponDb = n.nationGen.weapondb;
    String weaponName = weaponDb.GetValue(item.id, "weapon_name");
    
    if (weaponName.isBlank()) {
      return Optional.empty();
    }

    else {
      customItem.setValue("#name", weaponName);
    }

    for (ItemProperty property : ItemProperty.values()) {
      String dbColumn = property.getDBColumn();
      String modCommand = property.getModCommand();
      String originalValue = weaponDb.GetValue(item.id, dbColumn, "");
      Boolean isBooleanProperty = property.isBoolean();
      
      if (originalValue.isBlank()) {
        continue;
      }

      // If this property is an on or off, like "MR Negates"...
      else if (isBooleanProperty == true) {
        if (originalValue == "0") {
          continue;
        }

        // Just add the mod command without a value if the DB has it as 1
        else if (originalValue == "1") {
          customItem.setValue(modCommand);
        }
      }

      // Special mod properties that need more than one value are in the below else ifs
      else if (property == ItemProperty.FLYSPRITE) {
        String speed = weaponDb.GetValue(item.id, ItemProperty.ANIM_LENGTH.getDBColumn(), "1");
        customItem.setValue(modCommand, originalValue, speed);
      }

      // Else just add the value of the item property from the db directly
      else if (modCommand.isBlank() == false) {
        customItem.setValue(modCommand, originalValue);
      }
    }

    return Optional.of(customItem);
  }
}
