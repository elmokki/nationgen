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
    boolean mage
  ) {
    // Handles possiblecommands
    this.handlePossibleCommands(u);

    // Failsafe
    if (included == null) included = new ItemSet();
    if (excluded == null) excluded = new ItemSet();
    if (poseexclusions == null) poseexclusions = new ItemSet();

    boolean ignoreArmor = mage;

    // Armor
    if (u.isSlotEmpty("armor") && u.pose.getItems("armor") != null) {
      ItemSet all = u.pose.getItems("armor");
      all.removeAll(poseexclusions);

      if (all.size() == 0) all = u.pose.getItems("armor");

      Item armor = getSuitableItem(
        "armor",
        u,
        excluded,
        included,
        all,
        targettag
      );

      u.setSlot("armor", armor);
    }

    // Mount
    if (
      u.isSlotEmpty("mount") &&
      u.pose.getItems("mount") != null &&
      u.pose.getItems("mount").size() > 0
    ) {
      int prot = nationGen.armordb.GetInteger(u.getSlot("armor").id, "prot");

      ItemSet possibleMounts = included
        .filterSlot("mount")
        .filterForPose(u.pose)
        .filterMinMaxProt(prot);
      possibleMounts.addAll(
        u.pose
          .getItems("mount")
          .filterMinMaxProt(prot)
          .filterKeepSameSprite(
            included.filterSlot("mount").filterMinMaxProt(prot)
          )
      );

      if (
        chandler.countPossibleFilters(possibleMounts, u) == 0 ||
        random.nextDouble() > 0.8
      ) {
        possibleMounts = u.pose.getItems("mount").filterMinMaxProt(prot);
        if (possibleMounts.possibleItems() == 0) possibleMounts =
          u.pose.getItems("mount");
      }

      // Make sure there is a mount to add in some weirder cases of a very limited mount being already taken
      ItemSet test = new ItemSet();
      test.addAll(possibleMounts.filterForPose(u.pose));
      test.removeAll(excluded.filterForPose(u.pose));
      if (chandler.countPossibleFilters(test, u) == 0) {
        excluded.removeAll(possibleMounts.filterForPose(u.pose));
      }

      ItemSet possibleMounts2 = possibleMounts.filterMinMaxProt(prot);
      if (
        chandler.countPossibleFilters(possibleMounts2, u) > 0
      ) possibleMounts = possibleMounts2;

      if (chandler.countPossibleFilters(possibleMounts, u) > 0) {
        String pref = null;
        if (
          u.race.tags.containsName("preferredmount") &&
          random.nextDouble() > 0.70
        ) {
          pref = u.race.tags.getValue("preferredmount").orElseThrow().get();
        }

        Item mount = null;
        if (pref != null) mount = getSuitableItem(
          "mount",
          u,
          excluded,
          null,
          possibleMounts,
          Command.args("animal", pref)
        );
        if (mount == null) mount = getSuitableItem(
          "mount",
          u,
          excluded,
          null,
          possibleMounts,
          null
        );

        u.setSlot("mount", mount);
        excluded.add(u.getSlot("mount"));

        if (u.getSlot("mount") == null) {
          System.out.println(
            "No mount found for " +
            u.race.name +
            " with armor prot " +
            prot +
            " / " +
            u.pose.roles +
            " -- ERROR VERSION 1"
          );
          return;
        }
      } else {
        System.out.println(
          "No mount found for " +
          u.race.name +
          " with armor prot " +
          prot +
          " / " +
          u.pose.roles +
          " -- ERROR VERSION 2"
        );
        return;
      }
    }

    if (u.isSlotEmpty("helmet") && u.pose.getItems("helmet") != null) {
      if (!ignoreArmor) {
        int prot;
        if (u.getSlot("armor") == null) prot = 0;
        else prot = nationGen.armordb.GetInteger(u.getSlot("armor").id, "prot");

        ItemSet inchere = included.filterSlot("helmet").filterForPose(u.pose);
        if (inchere.possibleItems() == 0) inchere = u.pose.getItems("helmet");

        if (u.getSlot("helmet") == null) {
          // Fancy chanceinc implementation
          if (
            chandler
              .handleChanceIncs(
                u,
                inchere.filterProt(nationGen.armordb, prot - 3, prot + 3)
              )
              .isEmpty()
          ) {
            inchere = u.pose.getItems("helmet");
          }
          Item helmet = chandler
            .handleChanceIncs(u, inchere, generateTargetProtChanceIncs(prot, 8))
            .getRandom(random);

          if (helmet != null) u.setSlot("helmet", helmet);

          // Traditional implementation
          /*
					int range = 1;
					int amount1 = Math.min(inchere.possibleItems(), 2);
					int amount2 = Math.min(u.pose.getItems("helmet").possibleItems(), 4);
		
					while(chandler.countPossibleFilters(inchere.filterProt(nationGen.armordb,  prot - range, prot + range), u) < amount1 
							&& u.pose.getItems("helmet").filterProt(nationGen.armordb,  prot - range, prot + range).possibleItems() < amount2)
					{
						range = range + 1;
					}
					
					if(inchere.filterProt(nationGen.armordb,  prot - range, prot + range).possibleItems() > 0)
						u.setSlot("helmet", getSuitableItem("helmet", u, excluded, inchere.filterProt(nationGen.armordb,  prot - range, prot + range), targettag));
					else if(u.pose.getItems("helmet").filterProt(nationGen.armordb,  prot - range, prot + range).possibleItems() > 0)
						u.setSlot("helmet", getSuitableItem("helmet", u, excluded, u.pose.getItems("helmet").filterProt(nationGen.armordb,  prot - range, prot + range), targettag));
					*/

          // Failsafe
          if (u.getSlot("helmet") == null) {
            u.setSlot(
              "helmet",
              getSuitableItem(
                "helmet",
                u,
                excluded,
                u.pose.getItems("helmet"),
                targettag
              )
            );
          }
        }
      } else {
        u.setSlot(
          "helmet",
          getSuitableItem("helmet", u, excluded, included, targettag)
        );
      }
    }

    if (u.isSlotEmpty("offhand") && u.pose.getItems("offhand") != null) {
      if (ignoreArmor) {
        u.setSlot(
          "offhand",
          getSuitableItem("offhand", u, excluded, included, targettag)
        );
      } else {
        ItemSet all = u.pose.getItems("offhand").filterArmor(true);

        if (all.size() > 0) {
          all.removeAll(excluded.filterForPose(u.pose));
          ItemSet newincluded = new ItemSet();
          newincluded.addAll(included.filterForPose(u.pose));

          double deviance = random.nextDouble();
          int prot = Math.round(
            nationGen.armordb.GetInteger(u.getSlot("armor").id, "prot")
          );

          int operation = 0;
          // Chance to remove bucklers
          if (deviance > 0.8 && prot > 12) operation = -1;
          if (deviance > 0.5 && prot > 15) operation = -1;
          if (deviance > 0.2 && prot >= 18) operation = -1;

          // chance to remove tower shield and kite shield
          if (deviance > 0.5 && prot < 10) operation = 1;

          if (operation == -1) {
            newincluded = newincluded.filterProt(nationGen.armordb, 15, 100);
          } else if (operation == 1) {
            newincluded = newincluded.filterProt(
              nationGen.armordb,
              0,
              19,
              true
            );
          }

          if (newincluded.possibleItems() == 0) newincluded = included;

          if (random.nextDouble() < 0.1) newincluded = null;

          Item shield =
            this.getSuitableItem(
                "offhand",
                u,
                excluded,
                newincluded,
                u.pose.getItems("offhand").filterArmor(true),
                targettag
              );
          u.setSlot("offhand", shield);
        }
      }
    }
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
