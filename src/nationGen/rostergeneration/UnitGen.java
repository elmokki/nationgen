package nationGen.rostergeneration;

import com.elmokki.Generic;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.chances.EntityChances;
import nationGen.chances.ThemeInc;
import nationGen.entities.Filter;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.misc.Args;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.misc.ItemSet;
import nationGen.misc.Tags;
import nationGen.nation.Nation;
import nationGen.rostergeneration.montagtemplates.MontagTemplate;
import nationGen.units.Unit;

public class UnitGen {

  private NationGen nationGen;
  private Random random;
  private Nation nation;
  private NationGenAssets assets;
  public ChanceIncHandler chandler;

  public UnitGen(NationGen gen, Nation n, NationGenAssets assets) {
    this.nationGen = gen;
    this.nation = n;
    this.assets = assets;
    this.random = new Random(n.random.nextInt());
    this.chandler = new ChanceIncHandler(n);
  }

  /**
   * Generates a naked unit (hands, shadow, basesprite, legs)
   * @param race Race of the unit
   * @param pose Pose of the unit
   * @return
   */
  public Unit generateUnit(Race race, Pose pose) {
    Unit u = new Unit(nationGen, race, pose, nation);

    String[] slots = { "hands", "shadow", "basesprite", "legs" };
    for (String slot : slots) {
      if (u.pose.getListOfSlots().contains(slot)) {
        // Handle #lockslot
        Item item = race.themefilters
          .stream()
          .flatMap(t -> t.tags.getAllArgs("lockslot").stream())
          .filter(args -> args.get(0).get().equals(slot))
          .map(args ->
            u.pose.getItems(slot).getItemWithName(args.get(1).get(), slot)
          )
          .findFirst()
          // If no lockslot or no suitable item for lockslot
          .orElseGet(() ->
            chandler
              .handleChanceIncs(u, u.pose.getItems(slot))
              .getRandom(random)
          );

        u.setSlot(slot, item);
      }
    }

    // Handles generationchances and stuff
    this.handleExtraGeneration(u);

    return u;
  }

  /**
   * A simpler version of the troopgenerator version
   * @param u
   * @param query
   * @param defaultv
   */
  public void addTemplateFilter(Unit u, String query, String defaultv) {
    List<Filter> tFilters = ChanceIncHandler.retrieveFilters(
      query,
      defaultv,
      assets.templates,
      u.pose,
      u.race
    );
    tFilters.removeAll(u.appliedFilters);
    tFilters = ChanceIncHandler.getValidUnitFilters(tFilters, u);

    if (!tFilters.isEmpty()) {
      Filter t = chandler.getRandom(tFilters, u);

      u.appliedFilters.add(t);
    }
  }

  /**
   * Adds a free template filter if #freetemplatefilter was specified somewhere
   * @param u
   */
  public void addFreeTemplateFilters(Unit u) {
    for (Args args : Generic.getAllUnitTags(u).getAllArgs(
      "freetemplatefilter"
    )) {
      String type = null;
      String set = null;
      if (args.size() > 0) set = args.get(0).get();
      if (args.size() > 1) type = args.get(1).get();

      List<Filter> tFilters;
      if (set == null) tFilters = ChanceIncHandler.retrieveFilters(
        "freetemplatefilters",
        "default_freetemplatefilters",
        assets.templates,
        u.pose,
        u.race
      );
      else {
        if (
          assets.templates.get(set) == null
        ) throw new IllegalArgumentException(
          "No template filters were found for set " + set + "!"
        );
        else tFilters = new ArrayList<>(assets.templates.get(set));
      }

      List<Filter> possibles = new ArrayList<>(tFilters);

      if (type != null) possibles = ChanceIncHandler.getFiltersWithType(
        type,
        tFilters
      );

      if (possibles.size() == 0) {
        if (type != null) System.out.println(
          "No template filters of type " +
          type +
          " were found for #freetemplatefilter " +
          set +
          " " +
          type
        );
        else System.out.println(
          "No template filters were found for #freetemplatefilter " + set
        );
      } else {
        Filter t = chandler.getRandom(possibles, u);
        if (t != null) {
          u.appliedFilters.add(t);
          tFilters.remove(t);
        }
      }
    }
  }

  public void armorUnit(
    Unit u,
    ItemSet included,
    ItemSet excluded,
    Command targettag,
    boolean mage
  ) {
    armorUnit(u, included, excluded, null, targettag, mage);
  }

  /**
   * Armors unit up
   * @param u
   */
  public void armorUnit(
    Unit u,
    ItemSet included,
    ItemSet excluded,
    ItemSet poseexclusions,
    Command targettag,
    boolean ignoreChestArmor
  ) {
    // Handles possiblecommands
    this.handlePossibleCommands(u);

    // Ensure ItemSet parameters are defined and not null
    if (included == null) included = new ItemSet();
    if (excluded == null) excluded = new ItemSet();
    if (poseexclusions == null) poseexclusions = new ItemSet();

    // Equip armor if armor slot is empty and pose has armors
    if (u.isSlotEmpty("armor") && u.pose.getItems("armor") != null) {
      this.selectArmor(u, included, excluded, poseexclusions, targettag);
    }

    // Equip mount if mount slot is empty and pose has mounts
    if (
      u.isSlotEmpty("mount") &&
      u.pose.getItems("mount") != null &&
      u.pose.getItems("mount").size() > 0
    ) {
      this.selectMount(u, included, excluded, poseexclusions, targettag);
    }

    // Equip a helmet if offhand slot is empty and pose has helmets
    if (u.isSlotEmpty("helmet") && u.pose.getItems("helmet") != null) {
      this.selectHelmet(u, included, excluded, poseexclusions, targettag, ignoreChestArmor);
    }

    // Equip an offhand if offhand slot is empty and pose has offhands
    if (u.isSlotEmpty("offhand") && u.pose.getItems("offhand") != null) {
      // If chest armor is to be ignored (mostly for mages), select any offhand
      // This can be a shield or other offhand items like sutras, orbs, etc.
      if (ignoreChestArmor) {
        u.setSlot(
          "offhand",
          getSuitableItem("offhand", u, excluded, included, targettag)
        );
      // Otherwise select a shield that will take into account the armor's protection
      } else {
        this.selectShield(u, included, excluded, poseexclusions, targettag);
      }
    }
  }

  public void selectArmor(
    Unit u,
    ItemSet included,
    ItemSet excluded,
    ItemSet poseexclusions,
    Command targettag
  ) {
    Item selectedArmor;
    // Grab all possible pose armor but remove specific pose exclusions
    ItemSet finalArmorPool = u.pose.getItems("armor");
    finalArmorPool.removeAll(poseexclusions);

    // Pick one taking into account possible chanceincs
    selectedArmor = getSuitableItem(
      "armor",
      u,
      excluded,
      included,
      finalArmorPool,
      targettag
    );

    if (selectedArmor == null) {
      throw new IllegalArgumentException(
        "Found no possible armor for Unit (race: " +
        u.race.toString() +
        ", pose: " +
        u.pose.toString() +
        ") after handling chanceincs"
      );
    }

    u.setSlot("armor", selectedArmor);
  }

  public void selectHelmet(
    Unit u,
    ItemSet included,
    ItemSet excluded,
    ItemSet poseexclusions,
    Command targettag,
    boolean ignoreChestArmor
  ) {
    ItemSet finalHelmetPool = new ItemSet();
    Item selectedHelmet;

    // If we don't care about the chest armor prot (mages, mostly)
    if (ignoreChestArmor == true) {
      selectedHelmet = getSuitableItem("helmet", u, excluded, included, targettag);
    }
    
    else {
      int armorProt = u.getArmorProt();

      // Generate chance weights based on how far each helmet's prot is from the chest armor's prot
      List<ThemeInc> targetProtChanceIncs = generateTargetProtChanceIncs(armorProt, 8);

      // Filter the included helmets down to those that match our pose
      finalHelmetPool = included.filterSlot("helmet").filterForPose(u.pose);

      // Filter included helmet pool by protection close to the armor prot
      ItemSet helmetsFilteredByProt = finalHelmetPool
        .filterProt(nationGen.armordb, armorProt - 3, armorProt + 3);
      
      // If after narrowing down the included helmets and checking their
      // chanceincs we're down to 0, then just use the full list of pose helmets instead
      if (chandler.countPossibleFilters(helmetsFilteredByProt, u) == 0) {
        ItemSet poseHelmets = u.pose.getItems("helmet");
        finalHelmetPool.clear();
        finalHelmetPool.addAll(poseHelmets);
      }

      // Select a random helmet from our final pool after handling their chanceincs
      selectedHelmet = chandler
        .handleChanceIncs(u, finalHelmetPool, targetProtChanceIncs)
        .getRandom(random);
    }

    if (selectedHelmet == null) {
      throw new IllegalArgumentException(
        "Found no possible helmet for Unit (race: " +
        u.race.toString() +
        ", pose: '" +
        u.pose.toString() +
        "') with ignoreChestArmor = " + ignoreChestArmor +
        " after handling chanceincs"
      );
    }
    
    u.setSlot("helmet", selectedHelmet);
  }

  public void selectShield(
    Unit u,
    ItemSet included,
    ItemSet excluded,
    ItemSet poseexclusions,
    Command targettag
  ) {
    int armorProt = u.getArmorProt();
    ItemSet poseShieldPool = u.pose.getItems("offhand").filterArmor(true);
    ItemSet finalShieldPool = new ItemSet();
    boolean ignoreIncludedShields = random.nextDouble() < 0.1;
    Item selectedShield;

    // Small chance to ignore the Set from the included parameter
    if (ignoreIncludedShields == true) {
      included.clear();
    }

    else {
      // Filter the included set in the parameters by this pose
      included = included.filterForPose(u.pose);
    }

    // Start with the pose shield pool as our final pool
    finalShieldPool.addAll(poseShieldPool);

    // Remove exclusions from pool
    finalShieldPool.removeAll(excluded.filterForPose(u.pose));

    // No shields available for this pose; return early
    if (finalShieldPool.size() == 0) {
      return;
    }

    double roll = random.nextDouble();

    // Chance to remove tower and kite shields from final shield pool
    // (since armor protection is low, presumably unit is supposed to be light)
    if (armorProt < 10 && roll > 0.5) {
      finalShieldPool = finalShieldPool.filterProt(
        nationGen.armordb,
        0,
        19,
        true
      );
    }

    // Chances to remove bucklers from final shield pool
    // The higher the armor protection, the higher the chance
    else if (
      (armorProt > 18 && roll > 0.2) ||
      (armorProt > 15 && roll > 0.5) ||
      (armorProt > 12 && roll > 0.8)
    ) {
      finalShieldPool = finalShieldPool.filterProt(nationGen.armordb, 15, 100);
    }

    // If we removed all possible shields after checking chanceincs, use the full pose set
    if (chandler.countPossibleFilters(finalShieldPool, u) == 0) {
      finalShieldPool.clear();
      finalShieldPool.addAll(poseShieldPool);
    }

    selectedShield = this.getSuitableItem(
      "offhand",
      u,
      excluded,
      included,
      finalShieldPool,
      targettag
    );

    u.setSlot("offhand", selectedShield);
  }

  public void selectMount(
    Unit u,
    ItemSet included,
    ItemSet excluded,
    ItemSet poseexclusions,
    Command targettag
  ) {
    // Find chest armor protection to apply mounts' minprot and maxprot filters
    int prot = nationGen.armordb.GetInteger(u.getSlot("armor").id, "prot");
    ItemSet finalMountPool = new ItemSet();
    Command racialMountPreference = null;
    Item selectedMount = null;

    // Filter mounts passed directly into the function for pose
    ItemSet mountsFilteredByPose = included
      .filterSlot("mount")
      .filterForPose(u.pose);

    // Add all pose mounts too
    mountsFilteredByPose.addAll(u.pose.getItems("mount"));

    if (chandler.countPossibleFilters(mountsFilteredByPose, u) == 0) {
      throw new IllegalArgumentException(
        "Found no possible mounts for Unit (race: " +
        u.race.toString() +
        ", pose: '" +
        u.pose.toString() +
        "') after handling chanceincs"
      );
    }

    finalMountPool.addAll(mountsFilteredByPose);

    // Filter by... same sprite to use them as a possible way to determine existing
    // mount types so that huge varieties of mounts are a bit rarer.
    ItemSet mountsFilteredBySprite = mountsFilteredByPose
      .filterKeepSameSprite(
        included.filterSlot("mount").filterMinMaxProt(prot)
      );

    // Only use sprite filtering if it doesn't rule out all options
    if (chandler.countPossibleFilters(mountsFilteredBySprite, u) != 0) {
      finalMountPool.clear();
      finalMountPool.addAll(mountsFilteredBySprite);
    }

    ItemSet mountsFilteredByProt = finalMountPool.filterMinMaxProt(prot);

    // Only use prot filtering if it doesn't rule out all options
    if (chandler.countPossibleFilters(mountsFilteredByProt, u) != 0) {
      // Roll a small chance to ignore min/maxprot filters in the mount items
      double roll = random.nextDouble();
      double chanceToIgnoreProtFilters = 0.2;

      if (roll > chanceToIgnoreProtFilters) {
        // Roll failed; only include prot filtered mounts in final pool
        finalMountPool.clear();
        finalMountPool.addAll(mountsFilteredByProt);
      }
    }

    else {
      // Helpful console print to inform that the pose should probably have more suitable mount items
      System.out.println(
        "Pose '" + u.pose.toString() +
        "' (race: " +
        u.race.toString() +
        ") was left without possible mounts after filtering for min/maxprot and chanceincs; " + 
        "consider tweaking the mount list numbers or adding more options"
      );
    }

    // If the race has a preferred mount and a small chance succeeds, we'll use that preference
    if (u.race.tags.containsName("preferredmount") && random.nextDouble() > 0.70) {
      String preferredmount = u.race.tags.getValue("preferredmount").orElseThrow().get();
      racialMountPreference = Command.args("animal", preferredmount);
    }

    // Get the suitable mount from the final mount pool that was compiled
    selectedMount = getSuitableItem(
      "mount",
      u,
      excluded,
      null,
      finalMountPool,
      racialMountPreference
    );

    if (selectedMount == null) {
      throw new IllegalArgumentException(
        "Found no suitable Mount item for Unit (race: " +
        u.race.toString() +
        ", pose: " +
        u.pose.toString() +
        ") even after all safeties"
      );
    }

    // Set the mount slot with the selected mount, and exclude the mount from further picks
    u.setSlot("mount", selectedMount);
    excluded.add(u.getSlot("mount"));
  }

  public List<ThemeInc> generateTargetProtChanceIncs(int prot, int range) {
    List<ThemeInc> list = new ArrayList<>();

    for (int i = 0; i < range; i++) {
      // Below not applied instantly
      if (i >= 2) {
        int bottom = prot - i; // "below"
        list.add(ThemeInc.parse("thisarmorprot below " + bottom + " *0.8"));
      }

      int top = prot + i + 1; // above or at
      list.add(ThemeInc.parse("thisarmorprot " + top + " *0.5"));
    }

    return list;
  }

  public Item getBestMatchForSlot(Unit from, Pose to, String slot) {
    Item ui = from.getSlot(slot);
    return getBestMatchForSlot(ui, to, slot);
  }

  public Item getBestMatchForSlot(Item from, Pose to, String slot) {
    Item ui = from;
    if (to.getItems(slot) == null || ui == null) return null;

    // Same name and slot
    for (Item i : to.getItems(slot)) {
      if (i != null) if (i.name.equals(ui.name) && i.id.equals(ui.id)) return i;
    }

    // Same image and id
    for (Item i : to.getItems(slot)) {
      if (i != null) if (
        i.sprite.equals(ui.sprite) && i.id.equals(ui.id)
      ) return i;
    }

    // Same id and armor type
    for (Item i : to.getItems(slot)) {
      if (i != null) if (
        i.id.equals(ui.id) &&
        i.armor == ui.armor &&
        ((i.tags.containsName("elite") == ui.tags.containsName("elite")) ||
          i.tags.containsName("sacred") == ui.tags.containsName("sacred"))
      ) return i;
    }

    // Same id and armor type
    for (Item i : to.getItems(slot)) {
      if (i != null) if (i.id.equals(ui.id) && i.armor == ui.armor) return i;
    }

    return null;
  }

  public void armUnit(
    Unit u,
    ItemSet included,
    ItemSet excluded,
    Command targettag,
    boolean mage
  ) {
    if (u.getSlot("mount") != null && !mage) armCavalry(
      u,
      included,
      excluded,
      targettag,
      mage
    );
    else armInfantry(u, included, excluded, targettag, mage);
  }

  public void armInfantry(
    Unit u,
    ItemSet included,
    ItemSet excluded,
    Command targettag,
    boolean mage
  ) {
    boolean ignoreArmor = mage;

    if (excluded == null) excluded = new ItemSet();
    if (included == null) included = new ItemSet();

    if (u.isSlotEmpty("weapon") && u.pose.getItems("weapon") != null) {
      if (ignoreArmor) u.setSlot(
        "weapon",
        getSuitableItem("weapon", u, excluded, included, targettag)
      );
      else {
        ItemSet all = included.filterSlot("weapon");
        all.removeAll(excluded);

        if (all.possibleItems() == 0) all = u.pose.getItems("weapon");
        if (
          random.nextDouble() > 0.5 ||
          (included.filterSlot("weapon").size() < 2 &&
            random.nextDouble() > 0.15)
        ) {
          ItemSet test = u.pose.getItems("weapon");
          test.removeAll(included);
          if (test.possibleItems() > 0) {
            all = test;
          }
        }

        all.removeAll(excluded);

        // 50% chance to not give a twohander if possible and not mage
        if (
          included
              .filterDom3DB("2h", "Yes", false, nationGen.weapondb)
              .possibleItems() ==
            0 &&
          included
            .filterDom3DB("2h", "Yes", true, nationGen.weapondb)
            .possibleItems() !=
          0 &&
          random.nextDouble() > 0.5 &&
          !mage
        ) {
          ItemSet test = all.filterDom3DB(
            "2h",
            "Yes",
            false,
            nationGen.weapondb
          );
          if (test.possibleItems() > 0) all = test;
        }

        Item armor = u.getSlot("armor");
        int prot = armor == null
          ? 0
          : nationGen.armordb.GetInteger(armor.id, "prot");

        if (prot < 10 && random.nextDouble() > 0.5 && !mage) {
          ItemSet test = all.filterDom3DBInteger(
            "res",
            3,
            true,
            nationGen.weapondb
          );
          test = test.filterDom3DBInteger("dmg", 6, true, nationGen.weapondb);

          if (test.possibleItems() == 0) test = u.pose
            .getItems("weapon")
            .filterDom3DBInteger("res", 3, true, nationGen.weapondb)
            .filterDom3DBInteger("dmg", 6, true, nationGen.weapondb);

          if (test.possibleItems() > 0) all = test;
        } else if (prot > 12 && random.nextDouble() > 0.5 && !mage) {
          ItemSet test = all.filterDom3DBInteger(
            "res",
            1,
            false,
            nationGen.weapondb
          );
          test = test.filterDom3DBInteger("dmg", 4, false, nationGen.weapondb);

          if (test.possibleItems() == 0) test = u.pose
            .getItems("weapon")
            .filterDom3DBInteger("res", 1, false, nationGen.weapondb)
            .filterDom3DBInteger("dmg", 4, false, nationGen.weapondb);

          if (test.possibleItems() > 0) all = test;
        }

        if (all.possibleItems() > 0) u.setSlot(
          "weapon",
          getSuitableItem("weapon", u, excluded, all, targettag)
        );
      }
    }
  }

  public void armCavalry(
    Unit u,
    ItemSet included,
    ItemSet excluded,
    Command targettag,
    boolean ignoreArmor
  ) {
    if (excluded == null) excluded = new ItemSet();
    if (included == null) included = new ItemSet();

    included = included.filterForPose(u.pose);

    if (u.isSlotEmpty("weapon")) {
      if (ignoreArmor) u.setSlot(
        "weapon",
        getSuitableItem("weapon", u, excluded, included, targettag)
      );
      else {
        boolean canGetLance = false;
        if (u.pose.getItems("lanceslot") != null) {
          int ap = 10;
          for (Command c : u.getCommands()) if (c.command.equals("#ap")) ap =
            c.args.get(0).getInt();

          boolean availableLance = false;
          for (Item i : u.pose.getItems("lanceslot")) if (
            i.id.equals("4") || i.tags.containsName("lance")
          ) availableLance = true;

          if (10 + random.nextInt(20) > ap && availableLance) canGetLance =
            true;
        }

        Item weapon = null;
        boolean done = false;
        while (!done) {
          int choice = random.nextInt(4); // 0-3
          if (choice <= 1) {
            ItemSet lances = new ItemSet();
            if (
              u.pose.getItems("lanceslot") != null
            ) for (Item i : u.pose.getItems("lanceslot")) if (
              i.id.equals("4") || i.tags.containsName("lance")
            ) lances.add(i);

            ItemSet onehand = included
              .filterSlot("weapon")
              .filterDom3DB("2h", "Yes", false, nationGen.weapondb);

            if (chandler.handleChanceIncs(u, onehand).isEmpty()) {
              onehand = u.pose
                .getItems("weapon")
                .filterDom3DB("2h", "Yes", false, nationGen.weapondb);
            }

            ItemSet llances = new ItemSet();
            for (Item i : u.pose.getItems("weapon")) if (
              i.id.equals("357") || i.tags.containsName("lightlance")
            ) llances.add(i);

            onehand.removeAll(llances);

            if (onehand.possibleItems() > 0) {
              if (canGetLance) u.setSlot(
                "lanceslot",
                chandler.getRandom(lances, u)
              );
              weapon = chandler.getRandom(onehand, u);

              done = true;
            }
          } else if (choice == 2) {
            ItemSet lances = new ItemSet();
            for (Item i : u.pose.getItems("weapon")) if (
              i.id.equals("357") || i.tags.containsName("lightlance")
            ) lances.add(i);

            if (lances.possibleItems() > 0) {
              weapon = chandler.getRandom(lances, u);
              done = true;
            }
          } else if (choice == 3) {
            ItemSet onehand = included
              .filterSlot("weapon")
              .filterDom3DB("2h", "Yes", true, nationGen.weapondb);
            if (chandler.handleChanceIncs(u, onehand).isEmpty()) onehand =
              u.pose
                .getItems("weapon")
                .filterDom3DB("2h", "Yes", true, nationGen.weapondb);

            if (onehand.possibleItems() > 0) {
              weapon = chandler.getRandom(onehand, u);

              done = true;
            }
          }
        }

        if (weapon == null) System.out.println(
          "NULL WEAPON FOR " + u.race.name + " " + u.pose.name
        );
        u.setSlot("weapon", weapon);
      }
    }
  }

  public Item getSuitableItem(
    String slot,
    Unit u,
    ItemSet excluded,
    ItemSet included,
    Command targettag
  ) {
    return getSuitableItem(slot, u, excluded, included, null, targettag);
  }

  public Item getSuitableItem(
    String slot,
    Unit u,
    ItemSet excluded,
    ItemSet included,
    ItemSet all,
    Command targettag
  ) {
    if (all == null || all.size() == 0) all = u.pose.getItems(slot);
    if (all == null) return null;

    if (excluded == null) excluded = new ItemSet();
    if (included == null) included = new ItemSet();

    excluded = excluded.filterSlot(slot).filterForPose(u.pose);
    included = included.filterSlot(slot).filterForPose(u.pose);

    included.removeAll(excluded);
    all.removeAll(excluded);

    ItemSet remain = new ItemSet();
    remain.addAll(included);
    remain.retainAll(all);

    ItemSet chosen = null;

    if (chandler.countPossibleFilters(remain, u) > 0) chosen = remain;

    // Check whether we should ditch using old
    EntityChances<Item> chances = chandler.handleChanceIncs(
      u,
      u.pose.getItems(slot)
    );

    double allsum = 0;
    double oldsum = 0;
    for (Item i : chances.getPossible()) {
      if (remain.contains(i)) oldsum += chances.getChance(i);
      if (u.pose.getItems(slot).contains(i)) allsum += chances.getChance(i);
    }

    if (
      chosen == null ||
      chandler.countPossibleFilters(chosen, u) == 0 ||
      random.nextDouble() < 0.5 ||
      (oldsum / allsum) < 0.05
    ) {
      chosen = all;
    }

    if (chandler.countPossibleFilters(chosen, u) == 0) chosen = u.pose.getItems(
      slot
    );

    if (targettag != null) {
      ItemSet filtered = chosen.filterTag(targettag);
      if (filtered.possibleItems() > 0) chosen = filtered;
    }

    Item newitem = chandler.getRandom(chosen, u);

    // Pile of failsafes.
    if (newitem == null) newitem = chandler.getRandom(included, u);
    if (newitem == null) newitem = chandler.getRandom(all, u);
    if (newitem == null) newitem = chandler.getRandom(u.pose.getItems(slot), u);

    return newitem;
  }

  public void handleExtraGeneration(Unit u) {
    Tags tags = new Tags();
    tags.addAll(u.pose.tags);
    tags.addAll(u.race.tags);
    u.slotmap.items().forEach(i -> tags.addAll(i.tags));
    for (Filter f : u.appliedFilters) tags.addAll(f.tags);

    for (Args args : tags.getAllArgs("generateitem")) {
      String slot = args.get(1).get();
      double chance = args.get(0).getDouble();
      if (random.nextDouble() < chance) {
        Item item = this.getSuitableItem(slot, u, null, null, null);
        u.setSlot(slot, item);
      }
    }
  }

  /**
   * Checks whether unit has #montagpose
   * @param u
   */
  public boolean hasMontagPose(Unit u) {
    return u.pose.tags.containsName("montagpose");
  }

  /**
   * Adds #firstshape -<montag> to the unit and adds units to nation
   * @param u Unit
   * @return
   */
  public void handleMontagUnits(
    Unit u,
    MontagTemplate template,
    String listname
  ) {
    ArrayList<Unit> list = new ArrayList<>();
    LinkedHashMap<Pose, Double> montagposeChances = new LinkedHashMap<>();

    List<Pose> poses = new ArrayList<>(u.race.poses);

    for (Args args : u.pose.tags.getAllArgs("montagpose")) {
      String name = args.get(0).get();
      Double weight = args.size() > 1 ? args.get(1).getDouble() : null;

      for (Pose p : poses) if (p.name != null && p.name.equals(name)) {
        if (weight != null) montagposeChances.put(p, weight);
        else montagposeChances.put(p, p.basechance);
      }
    }
    if (montagposeChances.size() == 0) return;

    EntityChances<Pose> montagposes = chandler.handleChanceIncs(
      u,
      new EntityChances<>(montagposeChances)
    );

    // Determine unit amount
    int min = u.pose.tags.getInt("montagpose_min").orElse(10);
    int max = u.pose.tags.getInt("montagpose_max").orElse(10);

    int count = min + random.nextInt(max - min + 1);

    // Generate units
    int tries = 0;

    while (list.size() < count && tries < 100) {
      tries++;

      Pose p = montagposes.getRandom(random);
      Unit newunit = template.generateUnit(u, p);

      if (newunit != null) {
        list.add(newunit);
      }

      if (p.tags.containsName("maxunits")) {
        int maxunits = p.tags.getInt("maxunits").orElseThrow();
        int unitcount = (int) list.stream().filter(nu -> nu.pose == p).count();

        if (unitcount >= maxunits) montagposes.eliminate(p);
      }
    }

    if (list.size() > 0) {
      String montag = "montag" + nation.nationid + "_" + nation.mockid--;
      for (Unit nu : list) {
        nu.tags.addName("hasmontag");
        nu.commands.add(Command.args("#montag", montag));
      }
      u.commands.add(Command.args("#firstshape", montag));
      u.tags.addName("montagunit");
    }

    if (nation.unitlists.get(listname) == null) nation.unitlists.put(
      listname,
      list
    );
    else nation.unitlists.get(listname).addAll(list);

    template = null;
  }

  private void handlePossibleCommands(Unit u) {
    Tags tags = new Tags();
    tags.addAll(u.pose.tags);
    tags.addAll(u.race.tags);
    u.slotmap.items().forEach(i -> tags.addAll(i.tags));
    for (Filter f : u.appliedFilters) tags.addAll(f.tags);

    for (Args args : tags.getAllArgs("possiblecommandset")) {
      String name = args.get(0).get();
      double chance = args.get(1).getDouble();
      if (random.nextDouble() < chance) {
        for (Args args2 : tags.getAllArgs("possiblecommand")) {
          if (args2.get(0).get().equals(name)) {
            u.commands.add(args2.get(1).getCommand());
          }
        }
      }
    }
  }

  public List<Unit> getMontagUnits(Unit u) {
    List<Unit> units = new ArrayList<>();
    if (u.pose.tags.containsName("montagpose")) {
      String firstshape = u.getStringCommandValue("#firstshape", "");
      boolean numeric = Generic.isNumeric(firstshape);

      for (List<Unit> lu : nation.unitlists.values()) {
        for (Unit nu : lu) {
          if (
            !numeric &&
            nu.getStringCommandValue("#montag", "").equals(firstshape) &&
            u != nu
          ) units.add(nu);
          else if (
            numeric &&
            ("-" + nu.getStringCommandValue("#montag", "")).equals(firstshape)
          ) units.add(nu);
        }
      }
    }

    return units;
  }
}
