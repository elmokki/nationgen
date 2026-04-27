package nationGen.ids;

import com.elmokki.NationGenDB;
import com.elmokki.Generic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import nationGen.NationGen;
import nationGen.items.CustomItem;
import nationGen.misc.Arg;
import nationGen.misc.Command;
import nationGen.misc.ResourceStorage;

/**
 *
 * This class exists to clean up the custom items from NationGen.java.
 * Long term, the NationGenDB stuff should probably get culled. Perhaps with its own separate class. We'll see.
 * However, with them as they are now, these changes let this class be not dependent on Nationgen.
 * @author FlashFire
 *
 */
public class CustomItemsHandler {

  private ResourceStorage<CustomItem> customItemStorage = new ResourceStorage<>(CustomItem.class);
  private List<CustomItem> customItems; // Superset of chosenCustomItems. Has all generated custom items + some which are cached.
  private List<CustomItem> chosenCustomItems; // List of items used by natgen in current runthrough.
  private IdHandler idHandler; // Local ref of id handler to generate new IDs when fresh custom item needs a new ID.
  private NationGenDB weapondb; // Local ref of weapondb to remove nationgen dependency.
  private NationGenDB armordb; // Local ref of armordb to remove nationgen dependency.

  public CustomItemsHandler(
    NationGen nationGen,
    NationGenDB weapondb,
    NationGenDB armordb
  ) {
    chosenCustomItems = new ArrayList<>();
    customItemStorage.load(nationGen, "./data/items/custom/index.txt");
    customItems = customItemStorage.getAllValues();
    idHandler = null;
    this.weapondb = weapondb;
    this.armordb = armordb;

    PopulateDB();
  }

  /**
   * This populates the DB with predefined custom items.
   */
  private void PopulateDB() {
    customItems.forEach((CustomItem ci) -> {
      if (ci.isArmor()) {
        armordb.addToMap(ci.name, ci.getHashMap());
      } else {
        weapondb.addToMap(ci.name, ci.getHashMap());
      }
    });
  }

  /**
   * Returns given custom item.  Can't be contained in chosenCustomItems.
   * @param name: Name of the custom item
   * @return the custom item
   */
  public Optional<CustomItem> getCustomItem(String name) {
    return customItems
      .stream()
      .filter(ci -> ci.name.equals(name))
      .filter(ci -> !chosenCustomItems.contains(ci))
      .findFirst();
  }

  /**
   *  The idea is that we first see if the name is already chosen, and return its chosen ID if so.
   *  Else, if the item exists in the superset [customItems], we generate its ID & add it to chosen items.
   */
  public Integer getCustomItemId(String name) {
    for (CustomItem ci : chosenCustomItems) {
      if (ci.name.equals(name)) {
        return ci.dominionsId.getIngameId();
      }
    }

    CustomItem customItem = null;

    for (CustomItem ci : customItems) {
      if (ci.name.equals(name) && !chosenCustomItems.contains(ci)) {
        customItem = new CustomItem(ci);
        break;
      }
    }

    if (customItem == null) {
      throw new IllegalArgumentException(
        "CustomItemsHandler error: No custom item named " + name + " was found!"
      );
    }

    if (idHandler != null) {
      Integer nextId;

      if (customItem.isArmor()) {
        nextId = idHandler.nextArmorId();
        customItem.dominionsId.setIngameId(nextId);
      }
      
      else {
        nextId = idHandler.nextWeaponId();
        customItem.dominionsId.setIngameId(nextId);
      }
    }
    
    else {
      throw new IllegalArgumentException("CustomItemsHandler error: idHandler was not initialized!");
    }

    customItem.getCustomCommand("#secondaryeffect")
      .ifPresent(c -> resolveCustomEffectId(c));

    customItem.getCustomCommand("#secondaryeffectalways")
      .ifPresent(c -> resolveCustomEffectId(c));

    chosenCustomItems.add(customItem);

    if (!customItem.isArmor()) {
      armordb.addToMap(customItem.dominionsId.getNationGenId(), customItem.getHashMap());
    } else {
      weapondb.addToMap(customItem.dominionsId.getNationGenId(), customItem.getHashMap());
    }

    return customItem.dominionsId.getIngameId();
  }

  private void resolveCustomEffectId(Command effect) {
    Arg customEffectId = effect.args.get(0);

    if (!customEffectId.isNumeric()) {
      Integer id = getCustomItemId(customEffectId.get());
      effect.args.set(0, new Arg(id));
    }
  }

  /**
   * Add a new custom item.
   */
  public void AddCustomItem(CustomItem item) {
    customItems.add(item);
  }

  /**
   * Updates ID handler to match current handler. This *must* be called before other functions are used.
   * Ideally, this would just be passed in constructor and never changed per instance. C'est la vie.
   */
  public void UpdateIDHandler(IdHandler handler) {
    idHandler = handler;
  }

  /**
   * Generates the mod lines for the currently chosen custom items.
   */
  public List<String> writeCustomItemsLines() {
    List<String> lines = new ArrayList<>();
    if (!chosenCustomItems.isEmpty()) {
      lines.add("--- Generic custom items:");
      for (CustomItem ci : chosenCustomItems) {
        lines.addAll(ci.writeLines());
      }
    }
    return lines;
  }

  static public Boolean isIdResolved(String itemId) {
    return Generic.isNumeric(itemId);
  }
}
