package nationGen.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.chances.ThemeInc;
import nationGen.entities.Drawable;
import nationGen.entities.Filter;
import nationGen.misc.Command;

public class Item extends Drawable {

  public String id = "-1";
  public boolean armor = false;
  public Filter filter = null;
  private ItemType itemType;

  public ArrayList<ItemDependency> dependencies = new ArrayList<>();
  //public LinkedHashMap<String, String> dependencies = new LinkedHashMap<>();
  //public LinkedHashMap<String, String> typedependencies = new LinkedHashMap<>();

  public List<Command> commands = new ArrayList<>();
  public String slot = "";
  public String set = "";

  public Item(NationGen nationGen) {
    super(nationGen);
  }

  public boolean containsMount() {
    Optional<Command> possibleMountmnr = this.commands
      .stream()
      .filter(c -> c.command.equals("#mountmnr"))
      .findAny();

    if (possibleMountmnr.isPresent()) {
      return true;
    }

    else {
      return false;
    }
  }

  public CustomItem getCustomItemCopy() {
    CustomItem item = new CustomItem(nationGen);
    item.sprite = sprite;
    item.mask = mask;
    item.id = id;
    item.armor = armor;
    item.offsetx = offsetx;
    item.offsety = offsety;
    item.dependencies.addAll(dependencies);
    item.commands.addAll(commands);
    item.slot = slot;
    item.set = set;
    item.renderslot = renderslot;
    item.renderprio = renderprio;
    item.name = this.name;
    item.filter = this.filter;
    item.basechance = this.basechance;
    item.tags.addAll(tags);
    return item;
  }

  public Item getCopy() {
    return this.getCustomItemCopy();
  }

  public Boolean isArmor() {
    return this.armor == true;
  }

  public Boolean isWeapon() {
    return this.armor == false;
  }

  public Boolean isRangedWeapon() {
    this.attemptToDetermineItemType();
    return this.itemType == ItemType.RANGED;
  }

  public Boolean isLowAmmoWeapon() {
    this.attemptToDetermineItemType();
    return this.itemType == ItemType.LOW_SHOTS;
  }

  public Boolean isMeleeWeapon() {
    this.attemptToDetermineItemType();
    return this.itemType == ItemType.MELEE;
  }

  public ItemType getItemType() {
    this.attemptToDetermineItemType();
    return this.itemType;
  }

  private void attemptToDetermineItemType() {
    if (this.itemType != null) {
      return;
    }

    // If it's got a range property, it might be ranged
    if (nationGen.weapondb.GetInteger(this.id, "rng") != 0) {
      // If its ammo is less than 4, it's a lowshots weapon
      if (nationGen.weapondb.GetInteger(this.id, "shots", 100) < 4) {
        this.itemType = ItemType.LOW_SHOTS;
      }
      
      else {
        this.itemType = ItemType.RANGED;
      }
    }

    else {
      this.itemType = ItemType.MELEE;
    } 
  }

  public boolean isCustomIdResolved() {
    return Generic.isNumeric(this.id);
  }

  /**
   * Returns a copy of this item with its custom id resolved. For example, we have the custom item
   * with id atl_conchshield, once it's resolved to a Dominions parsable id it'll be a number, such
   * as 5008. We assign that number here and return the copy. Note that we need to return a copy of
   * the item with the original customId because we don't want to modify the original, as that is
   * (probably) an asset instance that gets equipped with the same reference every time.
   * 
   * @param item - an item with a custom id, such as "atl_conchshield"
   * @return a copy of the item with a resolved id, or the same item of the id was already resolved
   */
  static public Item resolveId(Item item) {
    if (item.isCustomIdResolved() == false) {
      Item copy = item.getCopy();
      copy.tags.add("OLDID", item.id);
      copy.id = item.nationGen.GetCustomItemsHandler().getCustomItemId(item.id);
      return copy;
    }

    // If the id is already numeric, like 5008, it should have
    // already been resolved, so we just a copy of the item
    else {
      return item;
    }
  }

  @Override
  public void handleOwnCommand(Command command) {
    try {
      switch (command.command) {
        case "#gameid":
          this.id = command.args.get(0).get();
          break;
        case "#armor":
          this.armor = true;
          break;
        case "#addthemeinc":
          if (this.filter == null) {
            this.filter = new Filter(nationGen);
            filter.tags.addName("do_not_show_in_descriptions");
            if (this.name != null) filter.name = "Item " +
            this.name +
            " generation effects";
          }
          this.filter.themeincs.add(ThemeInc.from(command.args));
          break;
        case "#name":
          if (filter != null) filter.name = "Item " +
          command.args.get(0).get() +
          " generation effects";
          super.handleOwnCommand(command);
          break;
        case "#needs":
          this.dependencies.add(
              new ItemDependency(
                command.args.get(0).get(),
                command.args.get(1).get(),
                false,
                false
              )
            );
          break;
        case "#needstype":
          this.dependencies.add(
              new ItemDependency(
                command.args.get(0).get(),
                command.args.get(1).get(),
                true,
                false
              )
            );
          break;
        case "#forceslot":
          this.dependencies.add(
              new ItemDependency(
                command.args.get(0).get(),
                command.args.get(1).get(),
                false,
                true
              )
            );
          break;
        case "#forceslottype":
          this.dependencies.add(
              new ItemDependency(
                command.args.get(0).get(),
                command.args.get(1).get(),
                true,
                true
              )
            );
          break;
        case "#command":
        case "#define":
          if (command.args.size() != 1) {
            throw new IllegalArgumentException(
              "#command or #define must have a single arg. Surround the command with quotes if needed."
            );
          }
          this.commands.add(command.args.get(0).getCommand());
          break;
        default:
          super.handleOwnCommand(command);
          break;
      }
    } catch (IndexOutOfBoundsException e) {
      throw new IllegalArgumentException(
        "Command [" +
        command +
        "] has insufficient arguments (" +
        this.name +
        ")",
        e
      );
    }
  }
}
