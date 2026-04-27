package nationGen.misc;

import com.elmokki.NationGenDB;
import java.util.*;
import java.util.stream.Collectors;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.items.ItemProperty;
import nationGen.units.Unit;

public class ItemSet extends ArrayList<Item> {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  /**
   * Adds items of set to the new set
   * @param set
   */
  public ItemSet(ItemSet set) {
    addAll(set);
  }

  public ItemSet() {}

  public boolean addAll(ItemSet set) {
    return super.addAll(set);
  }

  @Override
  public boolean addAll(Collection<? extends Item> c) {
    return super.addAll(
      c.stream().filter(Objects::nonNull).collect(Collectors.toList())
    );
  }

  // Overwriting add to avoid nulls
  public boolean add(Item value) {
    if (value != null) return super.add(value);

    //System.out.println("Tried to add null to an itemset.");
    return false;
  }

  public ItemSet filterProt(NationGenDB armordb, int min, int max) {
    return filterProt(armordb, min, max, false);
  }

  public ItemSet filterKeepSameSprite(ItemSet old) {
    ItemSet newlist = new ItemSet();

    for (Item i : this) for (Item i2 : old) if (
      i.sprite.equals(i2.sprite)
    ) newlist.add(i);

    return newlist;
  }

  public ItemSet filterRemoveSameSprite(ItemSet old) {
    ItemSet newlist = new ItemSet();
    newlist.addAll(this);

    for (Item i : this) for (Item i2 : old) if (
      i.sprite.equals(i2.sprite)
    ) newlist.remove(i);

    return newlist;
  }

  public ItemSet filterProt(NationGenDB armordb, int min, int max, boolean includeDef) {
    ItemSet newlist = new ItemSet();
  
    for (Item i : this) {
      int prot = i.getIntegerFromDb(ItemProperty.PROTECTION.toDBColumn(), 0);

      if (includeDef ?
          (prot + i.getIntegerFromDb(ItemProperty.DEFENCE.toDBColumn(), 0) <= max && prot >= min) :
          (prot <= max && prot >= min)
      ) {
        newlist.add(i);
      }
    }

    return newlist;
  }

  public ItemSet filterDef(NationGenDB armordb, int min, int max) {
    ItemSet newlist = new ItemSet();

    for (Item i : this) {
      Integer def = i.getIntegerFromDb(ItemProperty.DEFENCE.toDBColumn(), 0);

      if (def <= max && def >= min) {
        newlist.add(i);
      }
    }

    return newlist;
  }

  public ItemSet filterMinMaxProt(int itemprot) {
    ItemSet newlist = new ItemSet();
    for (Item i : this) {
      int minprot = i.tags.getInt("minprot").orElse(0);
      int maxprot = i.tags.getInt("maxprot").orElse(100);

      if (itemprot >= minprot && itemprot <= maxprot) newlist.add(i);
    }

    return newlist;
  }

  public Item getItemWithID(String id, String slot) {
    ItemSet possibles = this;
    //this.filterTheme("elite", false).filterTheme("sacred", false);

    for (Item item : possibles) {
      Boolean idMatches = item.hasSameDominionsId(id) || item.hasSameCustomItemName(id);

      if (idMatches && item.slot.equals(slot)) {
        return item;
      }
    }

    return null;
  }

public ItemSet getItemsWithID(String id, String slot) {
    ItemSet matchingItems = new ItemSet();
    ItemSet possibles = this;
    //this.filterTheme("elite", false).filterTheme("sacred", false);

    for (Item item : possibles) {
      Item matchingItem = this.getItemWithID(id, slot);

      if (matchingItem != null) {
        matchingItems.add(item);
      }
    }

    return matchingItems;
  }

  public Item getItemWithName(String name, String slot) {
    for (Item item : this) {
      if (item.name.equals(name) && item.slot.equals(slot)) return item;
    }
    return null;
  }

  public boolean alreadyHas(Item item) {
    if (item == null) {
      return true;
    }

    for (Item i : this) {
      // Dominions equipments must have same CustomItem id and type of equipment
      if (i.isDominionsEquipment() && i.hasSameCustomItemName(item) && i.isArmor() == item.isArmor()) {
        return true;
      }

      // Non Dominions equipments just match their Entity.name
      else if (i.hasSameCustomItemName(item) && i.name.equals(item.name)) {
        return true;
      }
    }

    return false;
  }

  public ItemSet filterNationGenDB(
    String filterProperty,
    String filterValue,
    boolean exclude,
    NationGenDB db
  ) {
    ItemSet newlist = new ItemSet();

    for (Item i : this) {
      String itemValue = i.getValueFromDb(filterProperty, "0");
      Boolean isEqualValue = itemValue.equals(filterValue);
      Boolean shouldKeepItem = isEqualValue == exclude;

      if (shouldKeepItem) {
        newlist.add(i);
      }
    }

    return newlist;
  }

  public ItemSet filterNationGenDBInteger(
    String filterProperty,
    int filterValue,
    boolean hasToBeLower,
    NationGenDB db
  ) {
    ItemSet newlist = new ItemSet();

    for (Item i : this) {
      Integer itemValue = i.getIntegerFromDb(filterProperty, 0);

      if (hasToBeLower && itemValue < filterValue) {
        newlist.add(i);
      }
        
      else if (!hasToBeLower && itemValue > filterValue) {
        newlist.add(i);
      }
    }

    return newlist;
  }

  public ItemSet filterForRole(String role, Race race) {
    ItemSet filtered = new ItemSet();

    for (Item item : this) {
      for (Pose pose : race.poses) {
        if (!pose.roles.contains(role)) {
          continue;
        }

        if (pose.getItems(item.slot) == null) {
          continue;
        }

        for (Item poseItem : pose.getItems(item.slot)) {
          if (!poseItem.isDominionsEquipment() && !item.isDominionsEquipment()) {
            filtered.add(poseItem);
          }

          else if (poseItem.hasSameCustomItemName(item) && poseItem.name.equals(item.name)) {
            filtered.add(poseItem);
          }

          else if (poseItem.hasSameCustomItemName(item) && poseItem.sprite.equals(item.sprite)) {
            filtered.add(poseItem);
          }
        }
      }
    }

    return filtered;
  }

  public ItemSet filterForPosesWith(String role, Race race, Item olditem) {
    ItemSet newlist = new ItemSet();
    for (Item i : this) {
      for (Pose p : race.poses) {
        if (
          !p.roles.contains(role) ||
          p.getItems(olditem.slot) == null ||
          !p.getItems(olditem.slot).contains(olditem)
        ) continue;

        if (
          p.getItems(i.slot) != null &&
          p.getItems(i.slot).contains(i) &&
          !newlist.contains(i)
        ) newlist.add(i);
      }
    }

    return newlist;
  }

  public Item getRandom(ChanceIncHandler ch, Unit u, Random random) {
    return ch.handleChanceIncs(u, this).getRandom(random);
  }

  public ItemSet filterForPose(Pose pose) {
    ItemSet filtered = new ItemSet();

    // First pass - stricter filter
    for (Item item : this) {
      if (pose.getItems(item.slot) == null) {
        continue;
      }
        
      for (Item poseItem : pose.getItems(item.slot)) {
        if (item.hasSameCustomItemName(poseItem) && item.name.equals(poseItem.name)) {
          filtered.add(poseItem);
        }
      }
    }

    // If no results, make a less strict 2nd pass
    if (filtered.possibleItems() == 0) {
      for (Item item : this) {
        if (pose.getItems(item.slot) == null) {
          continue; 
        }

        for (Item poseItem : pose.getItems(item.slot)) {
          // Only check if both items share a Dominions equipment, even if they are not the same Item
          if (item.isDominionsEquipment() && item.hasSameCustomItemName(poseItem)) {
            filtered.add(poseItem);
          }
          
          // For others with the same CustomItem name (even if it's "no custom item"),
          // add them if they share a name or a sprite
          else if (item.hasSameCustomItemName(poseItem)) {
            if (item.name.equals(poseItem.name) || item.sprite.equals(poseItem.sprite)) {
              filtered.add(poseItem);
            }
          }
        }
      }
    }

    return filtered;
  }

  public ItemSet filterForOneHandedWeapons() {
    ItemSet oneHanded = new ItemSet();

    this.forEach(weapon -> {
        boolean isOneHanded = !weapon.getBooleanFromDb(ItemProperty.IS_2H.toDBColumn());

        if (isOneHanded) {
          oneHanded.add(weapon);
        }
      });

    return oneHanded;
  }

  public ItemSet filterForTwoHandedWeapons() {
    ItemSet twoHanded = new ItemSet();

    this.forEach(weapon -> {
        boolean isTwoHanded = weapon.getBooleanFromDb(ItemProperty.IS_2H.toDBColumn());

        if (isTwoHanded) {
          twoHanded.add(weapon);
        }
      });

    return twoHanded;
  }

  public ItemSet filterForLances() {
    ItemSet lances = new ItemSet();
    int vanillaLanceId = 4;

    this.forEach(l -> {
        boolean isVanillaLance = l.hasSameDominionsId(vanillaLanceId);
        boolean hasLanceTag = l.tags.containsName("lance");

        if (isVanillaLance || hasLanceTag) {
          lances.add(l);
        }
      });

    return lances;
  }

  public ItemSet filterForLightLances() {
    ItemSet lightLances = new ItemSet();
    int vanillaLightLanceId = 357;

    this.forEach(l -> {
        boolean isVanillaLance = l.hasSameDominionsId(vanillaLightLanceId);
        boolean hasLightLanceTag = l.tags.containsName("lightlance");

        if (isVanillaLance || hasLightLanceTag) {
          lightLances.add(l);
        }
      });

    return lightLances;
  }

  public ItemSet getCopy() {
    return new ItemSet(this);
  }

  public ItemSet filterSlot(String slot) {
    ItemSet newlist = new ItemSet();
    for (Item i : this) if (i.slot.equals(slot)) newlist.add(i);

    return newlist;
  }

  public ItemSet filterTag(Command tagLine) {
    ItemSet newlist = new ItemSet();
    for (Item i : this) if (i.tags.containsTag(tagLine)) newlist.add(i);

    return newlist;
  }

  public ItemSet filterOutTag(Command tagLine) {
    ItemSet newlist = new ItemSet();
    for (Item i : this) if (!i.tags.containsTag(tagLine)) newlist.add(i);

    return newlist;
  }

  public ItemSet filterTheme(String tag, boolean keepTag) {
    ItemSet newlist = new ItemSet();
    for (Item i : this) if (i.themes.contains(tag) == keepTag) newlist.add(i);

    return newlist;
  }

  public int possibleItems() {
    int items = 0;
    for (Item item : this) if (item.basechance > 0) items++;

    return items;
  }

  public ItemSet filterArmor(boolean keepArmor) {
    ItemSet newlist = new ItemSet();
    for (Item i : this) if (i.isArmor() == keepArmor) newlist.add(i);

    return newlist;
  }

  public ItemSet filterAbstracts() {
    ItemSet newlist = new ItemSet();
    for (Item i : this) {
      if (i.isDominionsEquipment()) {
        newlist.add(i);
      }
    }

    return newlist;
  }

  public ItemSet filterImpossibleAdditions(ItemSet old) {
    if (old == null) return this;

    List<Item> list = new ArrayList<>(old);
    return filterImpossibleAdditions(list);
  }

  public ItemSet filterImpossibleAdditions(List<Item> list) {
    ItemSet filtered = new ItemSet();
    filtered.addAll(this);

    for (Item item : list) {
      filtered.remove(item);

      // Remove stuff with the same ids or names or customid tag
      List<Item> sameItems = new ArrayList<>();

      for (Item otherItem : filtered) {
        if (
          otherItem.isArmor() == item.isArmor() &&
          otherItem.isDominionsEquipment() &&
          otherItem.hasSameCustomItemName(item) &&
          otherItem.slot.equals(item.slot)
        ) {
          sameItems.add(otherItem);
        }
      }

      filtered.removeAll(sameItems);
    }

    return filtered;
  }
}
