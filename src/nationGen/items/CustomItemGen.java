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

  public Optional<CustomItem> customizeItem(
    Unit unit,
    Item olditem,
    int maxPower,
    double powerUpChance,
    List<MagicItem> magicItems
  ) {
    Optional<CustomItem> maybeCustomizableItem = this.copyPropertiesFromItem(olditem);

    maybeCustomizableItem.ifPresent(customizableItem -> {
      customizeItem(unit, olditem, customizableItem, maxPower, powerUpChance, magicItems);
    });

    return maybeCustomizableItem;
  }

  private void customizeItem(
    Unit unit,
    Item originalItem,
    CustomItem customItem,
    int maxPower,
    double powerUpChance,
    List<MagicItem> magicItems
  ) {
    // Roll number of power ups item will get
    int powerUpBudget = rollPowerUpBudget(powerUpChance);

    // Spend the power up budget into item upgrades
    this.spendPowerUpBudget(powerUpBudget, originalItem, customItem);

    // Roll chance to grant the item an extra enchantment (decay, curse, poison, etc.)
    // These are typically defined in the default_magicweapons file
    if (random.nextDouble() <= powerUpChance) {
      this.grantEnchantment(unit, originalItem, customItem, maxPower, magicItems);

      // Roll chance to make the item magic
      if (shouldBeMagic(originalItem, customItem) == true) {
        customItem.setCustomCommand("#magic");
      }
    }

    // Add cost to unit based on item
    this.addToUnitCost(unit, powerUpBudget, customItem.isMagic());

    // Name the custom item
    this.nameCustomItem(originalItem, customItem, customItem.isMagic());

    // Add magic item tags
    if (customItem.magicItem != null) {
      customItem.tags.addAll(customItem.magicItem.tags);
    }

    // Add to custom item lists
    n.customitems.add(customItem);
    n.nationGen.GetCustomItemsHandler().AddCustomItem(customItem);
    n.nationGen.weapondb.addToMap(customItem.id, customItem.getHashMap());
  }

  private Boolean shouldBeMagic(
    Item originalItem,
    CustomItem customItem
  ) {
    return !originalItem.isRangedWeapon() &&
      random.nextDouble() > 0.75;
  }

  private int rollPowerUpBudget(double powerUpChance) {
    int powerUpBudget = 1 + random.nextInt(1);
    if (random.nextDouble() <= powerUpChance) powerUpBudget++;
    if (random.nextDouble() <= powerUpChance / 2) powerUpBudget++;
    if (random.nextDouble() <= powerUpChance / 4) powerUpBudget++;
    return powerUpBudget;
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
        customItem.setCustomCommand(chosenPowerUp.getProperty().getModCommand());
        powerUps.eliminate(chosenPowerUp);
      }

      else {
        ItemProperty property = chosenPowerUp.getProperty();
        String modCommand = property.getModCommand();
        int powerUpIncrease = chosenPowerUp.getIncrease();
        int originalValue = customItem.getCustomIntValue(modCommand).orElseThrow();
        customItem.setCustomCommand(modCommand, originalValue + powerUpIncrease);
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

  private void grantEnchantment(
    Unit unit,
    Item originalItem,
    CustomItem customItem,
    int maxPower,
    List<MagicItem> magicItems
  ) {
    ItemType type = originalItem.getItemType();
    Optional<MagicItem> possibleEffect = this.selectRandomEnchantment(unit, type, maxPower, magicItems);

    if (possibleEffect.isPresent() == false) {
      return;
    }

    // Roll an enchantment and apply it
    MagicItem enchantment = possibleEffect.get();
    customItem = this.assignSpecialLooks(unit, originalItem, customItem, possibleEffect.get());
    customItem.applyEnchantment(enchantment);

    // Use the enchantment to create an adjective for the item name
    String name = n.nationGen.weapondb.GetValue(originalItem.id, "weapon_name");
    name = this.addEnchantmentAdjectives(name, enchantment);
    customItem.setCustomCommand("#name", name);

    // Increase item gold cost if enchantment has extra cost for its type
    for (Args args : enchantment.tags.getAllArgs("gcost")) {
      String itemTypeWithExtraCost = args.get(0).get();
      Boolean hasExtraCostForItemType = itemTypeWithExtraCost.equals(type.getId());

      if (hasExtraCostForItemType == true) {
        Command enchantmentGoldCost = new Command("#gcost", args.get(1));
        customItem.commands.add(enchantmentGoldCost);
      }
    }

    // Increase item resource cost if enchantment has extra cost for its type
    for (Args args : enchantment.tags.getAllArgs("rcost")) {
      String itemTypeWithExtraCost = args.get(0).get();
      Boolean hasExtraCostForItemType = itemTypeWithExtraCost.equals(type.getId());

      if (hasExtraCostForItemType == true) {
        Command enchantmentResourceCost = new Command("#rcost", args.get(1));
        customItem.commands.add(enchantmentResourceCost);
      }
    }

    customItem.tags.addAll(enchantment.tags);
  }

  private String addEnchantmentAdjectives(String itemName, MagicItem enchantment) {
    int numberOfPrefixes = enchantment.namePrefixes.size();
    int numberOfSuffixes = enchantment.nameSuffixes.size();
    Boolean hasPrefixes = numberOfPrefixes > 0;
    Boolean hasSuffixes = numberOfSuffixes > 0;
    Boolean hasAdjectives = hasPrefixes || hasSuffixes;

    if (hasAdjectives == false) {
      return itemName;
    }

    int adjectiveRoll = random.nextInt(numberOfPrefixes +  numberOfSuffixes) + 1;

    if (hasSuffixes == true && adjectiveRoll > numberOfSuffixes) {
      int suffixRoll = random.nextInt(numberOfSuffixes);
      String suffix = enchantment.nameSuffixes.get(suffixRoll);
      itemName = Generic.capitalize(itemName + " " + suffix);
    }
    
    else {
      int prefixRoll = random.nextInt(numberOfPrefixes);
      String prefix = enchantment.namePrefixes.get(prefixRoll);
      itemName = Generic.capitalize(prefix + " " + itemName);
    }

    return itemName;
  }

  private Optional<MagicItem> selectRandomEnchantment(
    Unit unit, ItemType type, int maxPower, List<MagicItem> magicItems
  ) {
    List<MagicItem> possibleEffects = new ArrayList<>();

    for (MagicItem m : magicItems) {
      // Checks whether each of the items in the magicItems list is
      // compatible with this custom item (lowshots, ranged or melee)
      boolean isTypeRestricted = !m.tags
        .getAllStrings("no")
        .stream()
        .noneMatch(type.getId()::equals);

      if (isTypeRestricted == false && m.power <= maxPower) {
        possibleEffects.add(m);
      }
    }

    // No possible magic effects for this item; return empty
    if (possibleEffects.size() == 0) {
      return Optional.empty();
    }

    ChanceIncHandler chandler = new ChanceIncHandler(n, "customitemgenerator");
    MagicItem magicItem = chandler.handleChanceIncs(unit, possibleEffects).getRandom(random);
    return Optional.of(magicItem);
  }

  private CustomItem assignSpecialLooks(Unit unit, Item originalItem, CustomItem customItem, MagicItem effect) {
    // Check the selected effect for a list of the possible special looks
    List<Item> possibleLooks = this.getPossibleSpecialLooks(unit, originalItem, effect);
    Optional<CustomItem> possibleSpecialLook;

    // If none found, just return the custom item as it was
    if (possibleLooks.isEmpty()) {
      return customItem;
    }
    
    // If some, select a random one
    Item selectedMagicItem = EntityChances.baseChances(possibleLooks).getRandom(random);
    possibleSpecialLook = this.copyPropertiesFromItem(selectedMagicItem);

    if (possibleSpecialLook.isPresent()) {
      return possibleSpecialLook.get();
    }

    return customItem;
  }

  private List<Item> getPossibleSpecialLooks(Unit unit, Item originalItem, MagicItem effect) {
    List<Item> possibleSpecialLooks = new ArrayList<>();
    List<Args> possibleWeaponTags = effect.tags.getAllArgs("weapon");

    // Check all the <#tag weapon> of this magic weapon effect
    for (Args args : possibleWeaponTags) {
      // TODO: This should be parsed and validated when reading the magic weapons file...
      if (args.size() <= 1) {
        throw new IllegalArgumentException(
          "Magic Weapon Effect " +
          effect.name +
          "'s weapon tags are missing arguments. " +
          "They should have a Dominions weapon id and a NationGen custom id. " +
          "For example, #tag \"weapon 8 banebroadsword\"."
        );
      }

      // Get the Dominions weapon id of the #tag. I.e.
      String dominionsWeaponId = args.get(0).get();

      // Get the Natgen custom item id that defines the special looks
      String natgenCustomId = args.get(1).get();

      // If there is one and it's the same as the original item id (same type of weapon)
      if (args.size() > 1 && dominionsWeaponId.equals(originalItem.id)) {
        // Then try to find a special look within the pose item options
        Item specialLooksItem = unit.pose
          .getItems(originalItem.slot)
          .getItemWithName(natgenCustomId, originalItem.slot);

        // If one exists, add it as a possibility
        if (specialLooksItem != null) {
          possibleSpecialLooks.add(specialLooksItem);
        }
      }
    }

    return possibleSpecialLooks;
  }

  private void nameCustomItem(Item originalItem, CustomItem customItem, Boolean isMagic) {
    String name = n.nationGen.weapondb.GetValue(originalItem.id, "weapon_name");

    // If not a magic item and doesn't already have a custom display name, give it a generic one
    if (!isMagic && (customItem.magicItem == null || !customItem.hasCustomName())) {
      customItem.setCustomCommand("#name", "Exceptional " + name);
    }

    // If it is a magic item and doesn't already have a custom display name, give it a generic one
    else if (isMagic && (customItem.magicItem == null || !customItem.hasCustomName())) {
      customItem.setCustomCommand("#name", "Enchanted " + name);
    }

    // Construct an underlying name id for later use (not a display name)
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
      customItem.setCustomCommand("#name", weaponName);
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
          customItem.setCustomCommand(modCommand);
        }
      }

      // Special mod properties that need more than one value are in the below else ifs
      else if (property == ItemProperty.FLYSPRITE) {
        String speed = weaponDb.GetValue(item.id, ItemProperty.ANIM_LENGTH.getDBColumn(), "1");
        customItem.setCustomCommand(modCommand, originalValue, speed);
      }

      // Else just add the value of the item property from the db directly
      // Don't add zero values - the db uses these as placeholder for "empty"
      // For example all weapons without secondaryeffect have #secondaryeffect 0 in the db
      else if (modCommand.isBlank() == false && originalValue.equals("0") == false) {
        customItem.setCustomCommand(modCommand, originalValue);
      }
    }

    return Optional.of(customItem);
  }
}
