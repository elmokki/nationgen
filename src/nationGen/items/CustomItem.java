package nationGen.items;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import nationGen.NationGen;
import nationGen.entities.MagicItem;
import nationGen.misc.Arg;
import nationGen.misc.Args;
import nationGen.misc.Command;

public class CustomItem extends Item {

  private List<Command> customItemCommands = new ArrayList<>();
  public Item olditem = null;
  public MagicItem magicItem = null;

  public CustomItem(NationGen nationGen) {
    super(nationGen);
    this.customItemCommands.add(new Command(
      ItemProperty.RESOURCE_COST.toModCommand(),
      new Arg(0)
    ));

    this.customItemCommands.add(new Command(
      ItemProperty.DEFENCE.toModCommand(),
      new Arg(0)
    ));
  }

  public CustomItem(NationGen nationGen, LinkedHashMap<String, String> dbMap) {
      super(nationGen);
      Iterator<Map.Entry<String, String>> it = dbMap.entrySet().iterator();

      while(it.hasNext()) {
          Map.Entry<String, String> entry = it.next();
          ItemProperty property = ItemProperty.fromDbColumn(entry.getKey());
          Command command = Command.args(property.toModCommand(), entry.getValue());

          // The #flyspr mod command expects a second argument; the animation length
          if (property == ItemProperty.FLYSPRITE) {
              String animLength = dbMap.get(ItemProperty.ANIM_LENGTH.toDBColumn());

              if (animLength == null) {
                  throw new IllegalArgumentException("Expected DominionsItem hash map to contain an animation length for the flysprite!");
              }

              command.args.add(1, new Arg(animLength));
          }

          this.customItemCommands.add(command);
      }
  }

  public CustomItem(CustomItem customItem) {
    super(customItem);
    this.customItemCommands = new ArrayList<>(customItem.customItemCommands)
      .stream()
      .map(c -> new Command(c))
      .collect(Collectors.toList());

    this.olditem = (customItem.olditem != null) ? new Item(customItem.olditem) : null;
    this.magicItem = (customItem.magicItem != null) ? new MagicItem(customItem.magicItem) : null;
  }

  public Optional<Command> getCustomCommand(String commandName) {
    return this.customItemCommands.stream().filter(c -> c.command.equals(commandName)).findFirst();
  }

  public Boolean hasCustomCommand(String commandName) {
    return this.customItemCommands
      .stream()
      .filter(c -> c.command.equals(commandName))
      .findFirst()
      .isPresent();
  }

  public Optional<Args> getCustomValue(String commandName) {
    return this.getCustomCommand(commandName)
      .map(c -> c.args);
  }

  public Optional<String> getCustomStringValue(String commandName) {
    return getCustomValue(commandName).map(l -> l.get(0).get());
  }

  public Optional<Integer> getCustomIntValue(String commandName) {
    return getCustomValue(commandName).map(l -> l.get(0).getInt());
  }

  public void setCustomCommand(String commandName) {
    if (getCustomValue(commandName).isEmpty()) {
      this.customItemCommands.add(new Command(commandName));
    }
  }

  public void setCustomCommand(String commandName, String value) {
    Optional<Args> args = getCustomValue(commandName);
    if (args.isPresent()) {
      args.get().set(0, new Arg(value));
    } else {
      this.customItemCommands.add(Command.args(commandName, value));
    }
  }

  public void setCustomCommand(String commandName, int value) {
    Optional<Args> args = getCustomValue(commandName);
    if (args.isPresent()) {
      args.get().set(0, new Arg(value));
    } else {
      this.customItemCommands.add(new Command(commandName, new Arg(value)));
    }
  }

  public void setCustomCommand(String commandName, String value1, String value2) {
    Optional<Args> args = getCustomValue(commandName);
    if (args.isPresent()) {
      args.get().set(0, new Arg(value1));
      args.get().set(1, new Arg(value2));
    } else {
      this.customItemCommands.add(Command.args(commandName, value1, value2));
    }
  }

  public void applyEnchantment(MagicItem enchantment) {
    // Whether enchantment has an on-damage effect
    Boolean hasSecondaryEffect = !enchantment.effect.equals("-1");
    List<Command> enchantmentCommands = enchantment.getCommands();

    if (hasSecondaryEffect == true) {
      this.setCustomCommand(ItemProperty.SEC_EFF.toModCommand(), enchantment.effect);
    }

    // Copy commands from the enchantment over to the item
    for (Command c : enchantmentCommands) {
      String key = c.command;
      Boolean isCommandWithArguments = c.args.size() > 0;

      // This is a command with arguments, like #atk +1
      if (isCommandWithArguments == true) {
        // Likely a -X or +X argument
        Arg value = c.args.get(0);

        // Grab item's original command value
        Optional<Integer> oldvalue = this.getCustomIntValue(key);

        // Resolve the modifier from the magic effect
        int modifiedValue = (int) value.applyModTo(oldvalue.orElseThrow());

        // Apply new value
        this.setCustomCommand(key, modifiedValue);
      }
      
      // If it's a command without a value, just add it
      else {
        this.setCustomCommand(c.command);
      }
    }

    this.magicItem = enchantment;
  }

  public Boolean isMagic() {
    return this.hasCustomCommand(ItemProperty.IS_MAGIC_WEAPON.toModCommand());
  }

  public Boolean hasCustomName() {
    return this.getCustomValue(ItemProperty.NAME.toModCommand()).isPresent();
  }

  public Boolean hasDamageBitmask() {
    return this.customItemCommands
      .stream()
      .filter(c -> {
        return WeaponNumericalDamageType.isModCommandANumericalDamageType(c);
      })
      .count() > 0;
  }

  @Override
  public void handleOwnCommand(Command str) {
    try {
      if (str.command.equals("#command")) {
        if (str.args.size() != 1) {
          throw new IllegalArgumentException(
            "#command must have a single arg. Surround the command with quotes if needed."
          );
        }
        this.customItemCommands.add(str.args.get(0).getCommand());
      } else {
        super.handleOwnCommand(str);
      }
    } catch (IndexOutOfBoundsException e) {
      throw new IllegalArgumentException(
        "Wrong number of arguments for line: " + str,
        e
      );
    }
  }

  @Override
  protected void finish() {
    this.dominionsId.setNationGenId(this.name);

    if (this.isArmor()) {
      return;
    }

    // If not an explicit armor type, this must be a weapon
    this.addType(ItemType.WEAPON);

    // Check CustomItem mod command definition for range command
    if (!this.hasType(ItemType.RANGED) && this.hasCustomCommand(ItemProperty.RANGE.toModCommand())) {
      this.addType(ItemType.RANGED);
    }

    else if (!this.hasType(ItemType.MELEE)) {
      this.addType(ItemType.MELEE);
    }
  }

  public LinkedHashMap<String, String> getHashMap() {
    LinkedHashMap<String, String> map = new LinkedHashMap<>();

    map.put(ItemProperty.ATTACK.toDBColumn(), "0");
    map.put(ItemProperty.AMMO.toDBColumn(), "0");
    map.put(ItemProperty.RANGE.toDBColumn(), "0");
    map.put(ItemProperty.DEFENCE.toDBColumn(), "0");
    map.put(ItemProperty.LENGTH.toDBColumn(), "0");
    map.put(ItemProperty.DAMAGE.toDBColumn(), "0");
    map.put(ItemProperty.IS_2H.toDBColumn(), "0");
    map.put(ItemProperty.RESOURCE_COST.toDBColumn(), "0");

    for (Command command : this.customItemCommands) {
      ItemProperty property = ItemProperty.fromCommand(command);

      if (property == null) {
          continue;
      }
      
      if (property.isBoolean()) {
          map.put(property.toDBColumn(), "1");
      }

      else {
        String firstValue = command.args.get(0).get();

        if (property == ItemProperty.FLYSPRITE) {
            String secondValue = command.args.get(1).get();
            map.put(property.toDBColumn(), firstValue);
            map.put(ItemProperty.ANIM_LENGTH.toDBColumn(), secondValue);
        }

        else {
            map.put(property.toDBColumn(), firstValue);
        }
      }
    }
    
    return map;
  }

  public List<String> writeLines() {
    List<String> lines = new ArrayList<>();

    if (this.isArmor()) {
      lines.add("#newarmor " + this.dominionsId.getIngameId());
    }

    else {
      lines.add("#newweapon " + this.dominionsId.getIngameId());
    }

    for (Command command : this.customItemCommands) {
      lines.add(command.toModLine());
    }

    lines.add("#end");
    lines.add("");

    return lines;
  }

  public static CustomItem fromItem(Item item, NationGen nationGen) {
    CustomItem customItem = new CustomItem(nationGen);

    // Minimal valid weapon customItemCommands. Might get overwritten below if already exist
    if (item.isArmor() == false) {
      customItem.setCustomCommand(ItemProperty.ATTACK.toModCommand(), 0);
      customItem.setCustomCommand(ItemProperty.LENGTH.toModCommand(), 0);
      customItem.setCustomCommand(ItemProperty.DAMAGE.toModCommand(), 0);
    }

    customItem.sprite = item.sprite;
    customItem.mask = item.mask;
    customItem.addCommands(item.getCommands());
    customItem.tags.addAll(item.tags);
    customItem.dependencies.addAll(item.dependencies);
    customItem.setOffsetX(item.getOffsetX());
    customItem.setOffsetY(item.getOffsetY());
    customItem.slot = item.slot;
    customItem.basechance = item.basechance;
    customItem.renderslot = item.renderslot;
    customItem.renderprio = item.renderprio;
    customItem.olditem = item;
    customItem.addType(item.getItemTypes());
    return customItem;
  }
}
