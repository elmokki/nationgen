package nationGen.rostergeneration;

import java.util.*;
import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.chances.ChanceDistribution;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.misc.*;
import nationGen.nation.Nation;
import nationGen.rostergeneration.montagtemplates.TroopMontagTemplate;
import nationGen.units.Unit;

public class RosterGenerator {

  NationGen nationGen;
  private Nation nation;
  private Random r;

  private List<Unit> infantry = new ArrayList<>();
  private List<Unit> ranged = new ArrayList<>();
  private List<Unit> cavalry = new ArrayList<>();
  private List<Unit> chariot = new ArrayList<>();
  private TroopGenerator tgen;
  private ChanceIncHandler chandler;
  private final double skipchance;
  private List<TroopTemplate> templates = new ArrayList<>();

  public RosterGenerator(NationGen g, Nation n, NationGenAssets assets) {
    nationGen = g;
    nation = n;
    this.r = new Random(n.random.nextInt());
    tgen = new TroopGenerator(nationGen, nation, assets);
    tgen.setTemplates(this.templates);
    this.chandler = new ChanceIncHandler(n, "rostergen");
    skipchance = r.nextDouble() * 0.1 + 0.05;
  }

  private boolean canRollNewUnit(Race race) {
    return getChances(race).hasPossible();
  }

  public boolean hasPosesWithoutMaxUnits(Race race, String role) {
    return getPosesWithoutMaxUnits(race.getPoses(role), race).size() > 0;
  }

  public void execute() {
    Race primary = nation.races.get(0);
    Race secondary = nation.races.get(1);

    int maxtroops = primary.tags.getInt("maxtroops").orElse(10);
    int mintroops = primary.tags.getInt("mintroops").orElse(6);

    int max = mintroops + r.nextInt(maxtroops - mintroops + 1); // 6-10 by default
    int units = 0;
    int secs = 0;
    int prims = 0;

    double bonussecchance = 0.5;
    bonussecchance +=
    primary.tags.getDouble("secondaryracetroopmod").orElse(0D);
    bonussecchance -=
    secondary.tags.getDouble("primaryracetroopmod").orElse(0D);

    double minsecaffinity = primary.tags.getDouble("minsecaffinity").orElse(0D);
    double maxsecaffinity = primary.tags
      .getDouble("maxsecaffinity")
      .orElse(1000D);

    // Affinity
    double secaffinity = r.nextDouble() * bonussecchance;

    double nosecaffinitychance = primary.tags
      .getDouble("nosecaffinitychance")
      .orElse(0.35);

    if (
      primary.tags
        .getInt("minsecondaryracetroops")
        .filter(i -> i > 0)
        .isEmpty() &&
      primary.tags
        .getDouble("minsecondaryracetroopshare")
        .filter(d -> d > 0)
        .isEmpty() &&
      r.nextDouble() < nosecaffinitychance
    ) {
      secaffinity = 0;
    }

    secaffinity = Math.max(minsecaffinity, secaffinity);
    secaffinity = Math.min(maxsecaffinity, secaffinity);

    // Amount

    int maxprimaries = 100;

    // Primary amounts
    if (primary.tags.containsName("maxprimaryracetroopshare")) maxprimaries =
      (int) Math.round(
        max *
        primary.tags
          .getValue("maxprimaryracetroopshare")
          .orElseThrow()
          .getDouble()
      );
    else if (primary.tags.containsName("maxprimaryracetroops")) maxprimaries =
      primary.tags.getValue("maxprimaryracetroops").orElseThrow().getInt();
    else if (primary.tags.containsName("minsecondaryracetroops")) maxprimaries =
      max -
      primary.tags.getValue("minsecondaryracetroops").orElseThrow().getInt();
    else if (
      primary.tags.containsName("minsecondaryracetroopshare")
    ) maxprimaries = max -
    (int) Math.round(
      max *
      primary.tags
        .getValue("minsecondaryracetroopshare")
        .orElseThrow()
        .getDouble()
    );

    // Secondary amount
    double secamount = max * 0.3;
    if (r.nextDouble() < bonussecchance) {
      secamount += max * 0.2;
      if (r.nextDouble() < bonussecchance / 2) secamount += max * 0.1;
    }

    // Amount adjust
    if (max - maxprimaries > 0) secamount = Math.max(
      max - maxprimaries,
      Math.round(secamount)
    );

    if (secondary.tags.containsName("maxthisracetroops_as_secondary")) {
      secamount = Math.min(
        secamount,
        secondary.tags
          .getValue("maxthisracetroops_as_secondary")
          .orElseThrow()
          .getInt()
      );
    }

    if (secaffinity == 0) secamount = 0;

    // Max

    if (secaffinity > 0.5) {
      max = Math.min(10, max + 2);
    } else if (secaffinity > 0.3) max = Math.min(10, max + 1);

    if (secamount == 0) max = Math.min(max, maxprimaries);
    else max = (int) Math.min(max, maxprimaries + secamount);

    Map<String, Integer> maxamounts = new HashMap<>();
    // 1-2 ranged maximum
    maxamounts.put("ranged", 1 + r.nextInt(2)); // 1-2
    maxamounts.put("infantry", 8);
    maxamounts.put("mounted", 4);
    // Random chariot maximum
    maxamounts.put("chariot", r.nextInt(3));

    int cycles = 0;
    int incs = 1;

    while (units < max) {
      Race race = null;
      if (prims < maxprimaries && r.nextDouble() > secaffinity) {
        race = primary;
      } else if (secs < secamount) {
        race = secondary;
      } else if (prims < maxprimaries) {
        race = primary;
      }

      if (!canRollNewUnit(race)) {
        if (race == primary) {
          if (canRollNewUnit(secondary)) race = secondary;
          else break;
        } else if (race == secondary) {
          if (canRollNewUnit(primary)) race = primary;
          else break;
        }
      }

      cycles++;
      if (cycles > 100 * incs) {
        incs++;
        for (String role : maxamounts.keySet()) {
          maxamounts.put(role, maxamounts.get(role) + 1);
        }

        if (secaffinity > 0) secamount++;
      }

      Map<String, Integer> amounts = new HashMap<>();
      amounts.put("ranged", ranged.size());
      amounts.put("infantry", infantry.size());
      amounts.put("mounted", cavalry.size());
      amounts.put("chariot", chariot.size());

      String roll = null;
      int rolls = 0;
      while (roll == null) {
        rolls++;

        roll = rollRole(getChances(race), maxamounts, amounts, race);
        if (!canGetMoreUnits(race, roll)) {
          roll = null;
        }

        if (rolls > 100) break;
      }
      if (rolls > 100) break;

      List<Unit> target = null;
      if (
        roll.equals("ranged") &&
        maxamounts.get("ranged") > ranged.size() &&
        hasPosesWithoutMaxUnits(race, "ranged")
      ) target = ranged;
      else if (
        roll.equals("infantry") &&
        maxamounts.get("infantry") > infantry.size() &&
        hasPosesWithoutMaxUnits(race, "infantry")
      ) target = infantry;
      else if (
        roll.equals("mounted") &&
        maxamounts.get("mounted") > cavalry.size() &&
        hasPosesWithoutMaxUnits(race, "mounted")
      ) target = cavalry;
      else if (
        roll.equals("chariot") &&
        maxamounts.get("chariot") > chariot.size() &&
        hasPosesWithoutMaxUnits(race, "chariot")
      ) target = chariot;

      if (race != null && target != null && race.hasRole(roll)) {
        TroopTemplate t = this.chooseTemplate(race, roll);
        Unit u = tgen.generateUnit(t);

        if (u != null) {
          target.add(u);

          units++;

          if (race == primary) prims++;
          else if (race == secondary) secs++;
        }
      }
    }

    //System.out.println("Start " + ZonedDateTime.now().toLocalTime().truncatedTo(ChronoUnit.SECONDS));

    for (Unit unit : cavalry) if (
      tgen.unitGen.hasMontagPose(unit)
    ) tgen.unitGen.handleMontagUnits(
      unit,
      new TroopMontagTemplate(nationGen, nation, tgen),
      "montagtroops"
    );

    for (Unit unit : chariot) if (
      tgen.unitGen.hasMontagPose(unit)
    ) tgen.unitGen.handleMontagUnits(
      unit,
      new TroopMontagTemplate(nationGen, nation, tgen),
      "montagtroops"
    );

    for (Unit unit : ranged) if (
      tgen.unitGen.hasMontagPose(unit)
    ) tgen.unitGen.handleMontagUnits(
      unit,
      new TroopMontagTemplate(nationGen, nation, tgen),
      "montagtroops"
    );

    for (Unit unit : infantry) if (
      tgen.unitGen.hasMontagPose(unit)
    ) tgen.unitGen.handleMontagUnits(
      unit,
      new TroopMontagTemplate(nationGen, nation, tgen),
      "montagtroops"
    );

    //System.out.println("End " + ZonedDateTime.now().toLocalTime().truncatedTo(ChronoUnit.SECONDS));

    putToNation("ranged", sortToLists(ranged));
    putToNation("infantry", sortToLists(infantry));
    putToNation("mounted", sortToLists(cavalry));
    putToNation("chariot", sortToLists(chariot));

    tgen = null;
  }

  /**
   * Chooses whether to generate an unit from existing template or generates a new template
   * @param race
   * @param role
   * @return
   */
  private TroopTemplate chooseTemplate(Race race, String role) {
    boolean isPrimaryRace = (race == nation.races.get(0));

    // Choose templates of suitable race and role
    List<TroopTemplate> temptemplates = new ArrayList<>();
    for (TroopTemplate t : templates) if (
      !shouldSkipTemplate(t, isPrimaryRace, t.maxvar, race, role)
    ) temptemplates.add(t);

    // If we don't have templates or skip chance triggers, let's get a new template

    if (
      temptemplates.size() == 0 ||
      r.nextDouble() < 1 - Math.pow(1 - skipchance, temptemplates.size())
    ) {
      TroopTemplate t = getNewTemplate(race, role);
      templates.add(t);
      return t;
    }

    // ...and otherwise let's randomize from the existing ones
    // This time with pose chances taken into account!

    List<Pose> allPoses = new ArrayList<>();
    for (TroopTemplate t : temptemplates) allPoses.add(t.pose);

    Pose winner = chandler.getRandom(allPoses, race);
    List<TroopTemplate> newtemps = new ArrayList<>();
    for (TroopTemplate t : temptemplates) if (t.pose == winner) newtemps.add(t);

    return newtemps.get(r.nextInt(newtemps.size()));
  }

  /**
   * Ugly method for checking whether to skip, but this makes the main code clearer.
   * @param t
   * @param isPrimaryRace
   * @param maxvarieties
   * @param race
   * @param role
   * @return
   */
  private boolean shouldSkipTemplate(
    TroopTemplate t,
    boolean isPrimaryRace,
    int maxvarieties,
    Race race,
    String role
  ) {
    int occurances = 0;
    for (TroopTemplate t2 : templates) {
      if (
        t.armor.id.equals(t2.armor.id) &&
        Math.abs(t.template.getHP() - t2.template.getHP()) < 4 &&
        t.role.equals(t2.role) &&
        t.template.getSlot("mount") == t2.template.getSlot("mount")
      ) {
        occurances += t.units.size();
      }
    }

    /*	
		double rand = r.nextDouble();
		if(rand <= skipchance)
		{
			return true;
		}
		else
		*/

    if (t.race != race) {
      return true;
    } else if (!t.pose.roles.contains(role)) {
      return true;
    } else if (t.weapons.size() >= t.maxvar) {
      return true;
    } else if (occurances >= 4) {
      return true;
    } else if (!isPrimaryRace && !t.canBeSecondary) {
      return true;
    }

    return false;
  }

  private TroopTemplate getNewTemplate(Race race, String role) {
    Unit dummy = new Unit(nationGen, race, new Pose(nationGen));
    tgen.removeEliteSacred(dummy, role);

    Unit u = null;
    while (u == null) {
      ItemSet armors = new ItemSet();
      for (TroopTemplate t : templates) armors.add(t.armor);

      List<Pose> poses = new ArrayList<>();
      for (Pose p : getPosesWithoutMaxUnits(race.getPoses(role), race)) {
        List<Item> poseArmors = new ArrayList<>(p.getItems("armor"));
        poseArmors.removeAll(armors);
        if (poseArmors.size() > 0) poses.add(p);
      }

      if (chandler.handleChanceIncs(race, poses).isEmpty()) poses.addAll(
        getPosesWithoutMaxUnits(race.getPoses(role), race)
      );

      if (chandler.handleChanceIncs(race, poses).isEmpty()) {
        return null;
      }
      Pose p = chandler.getRandom(poses, race);
      if (p == null) return null;

      // Remove armors that are on poses of the same type
      ItemSet pointless = new ItemSet();
      for (TroopTemplate t : templates) {
        if (t.template.pose.roles.contains(role)) {
          pointless.add(t.armor);
          for (Item i : t.template.pose.getItems("armor")) if (
            i.id.equals(t.armor.id) && i.isCustomIdResolved()
          ) pointless.add(i);
        }
      }
      armors.removeAll(pointless);
      tgen.used.removeAll(pointless);

      // Generate unit!

      u = tgen.unitGen.generateUnit(race, p);
      tgen.addInitialFilters(u, role);

      pointless.addAll(tgen.exclusions);

      if (r.nextBoolean() && armors.possibleItems() > 0) tgen.unitGen.armorUnit(
        u,
        tgen.used,
        pointless,
        armors,
        null,
        false
      );
      else tgen.unitGen.armorUnit(
        u,
        null,
        pointless,
        u.pose.getItems("armor"),
        null,
        false
      );
      //unitGen.handleExtraGeneration(u);

    }

    //if(u == null || armor == null) // EA20150529: The only case where this should occur is when #maxunits has been exceeded
    //	return null;

    TroopTemplate t = TroopTemplate.getNew(
      u.getSlot("armor"),
      race,
      u,
      role,
      u.pose,
      tgen
    );

    //templates.add(t);

    // Exclude similar shield/armor
    for (Pose p2 : this.getPossiblePoses(role, race)) {
      if (
        p2.getItems("offhand") != null &&
        u.getSlot("offhand") != null &&
        u.getSlot("offhand").armor
      ) for (Item i : p2.getItems("offhand").filterArmor(true)) if (
        i.id.equals(u.getSlot("offhand").id) &&
        (!i.sprite.equals(u.getSlot("offhand").sprite) &&
          !i.name.equals(u.getSlot("offhand").name))
      ) {
        if (!tgen.exclusions.contains(i)) {
          this.tgen.exclusions.add(i);
        }
      }
    }

    return t;
  }

  /**
   * Tells whether given race-role combo can get more units
   * @param race
   * @param role
   * @return
   */
  public boolean canGetMoreUnits(Race race, String role) {
    int pos = 0;
    for (Pose p : chandler
      .handleChanceIncs(race, race.getPoses(role))
      .getPossible()) {
      if (poseHasMaxUnits(p)) continue;

      pos++;
    }

    return (pos > 0);
  }

  /**
   * Tells whether given race can get more troops
   * @param race
   * @return
   */
  public boolean canGetMoreUnits(Race race) {
    List<Pose> poses = new ArrayList<>();

    String[] roles = { "infantry", "mounted", "ranged", "chariot" };
    for (String role : roles) poses.addAll(
      chandler.handleChanceIncs(race, race.getPoses(role)).getPossible()
    );

    int pos = 0;
    for (Pose p : poses) {
      if (poseHasMaxUnits(p)) continue;

      pos++;
    }

    return (pos > 0);
  }

  /**
   * Finds possible poses for certain role/race with certain prot range
   * @param role
   * @param race
   * @return
   */
  private List<Pose> getPossiblePoses(String role, Race race) {
    // Search for poses with suitable armor
    List<Pose> possiblePoses = new ArrayList<Pose>();
    for (Pose p : race.poses) {
      if (!p.roles.contains(role)) continue;

      possiblePoses.add(p);
    }

    return possiblePoses;
  }

  private int getMaxUnits(Pose p) {
    return p.tags.getInt("maxunits").orElse(100);
  }

  public boolean poseHasMaxUnits(Pose p) {
    // Make sure #maxunits is taken into account
    int maxunits = getMaxUnits(p);

    int count = 0;
    for (TroopTemplate t : this.templates) if (t.pose.equals(p)) count++;

    return count >= maxunits;
  }

  private List<Pose> getPosesWithoutMaxUnits(List<Pose> orig, Race race) {
    List<Pose> poses = new ArrayList<>();
    List<Pose> possible = chandler.handleChanceIncs(race, orig).getPossible();

    for (Pose p : orig) {
      if (!poseHasMaxUnits(p) && possible.contains(p)) {
        poses.add(p);
      }
    }
    return poses;
  }

  private ChanceDistribution<String> getChances(Race race) {
    ChanceDistribution<String> chances = new ChanceDistribution<>();
    chances.setChance("ranged", 0.66);
    chances.setChance("infantry", 1);
    chances.setChance("mounted", 0.3);
    chances.setChance("chariot", 0.125);

    for (Args args : race.tags.getAllArgs("generationchance")) {
      String role = args.getString(0);

      if (chances.getChance(role) == null) {
        throw new IllegalArgumentException(
          String.format(
            "Unrecognized generationchance role name %s in %s tags",
            role,
            race.name
          )
        );
      }

      chances.setChance(role, args.getDouble(1));
    }

    for (String role : chances.getEntities()) {
      if (!canGetMoreUnits(race, role)) {
        chances.setChance(role, 0);
      }
    }

    for (String role : chances.getEntities()) {
      if (!race.hasRole(role)) {
        chances.setChance(role, 0);
      }
    }

    return chances;
  }

  private String rollRole(
    ChanceDistribution<String> chances,
    Map<String, Integer> maxamounts,
    Map<String, Integer> amounts,
    Race race
  ) {
    for (String role : chances.getEntities()) {
      if (amounts.get(role) >= maxamounts.get(role)) {
        chances.setChance(role, 0);
      }
    }

    // Tweak shares if primary race does not have something and secondary does
    Race primary = nation.races.get(0);
    Race secondary = nation.races.get(1);

    if (
      !primary.hasRole("ranged") &&
      secondary.hasRole("ranged") &&
      race == secondary
    ) chances.modifyChance("ranged", new Arg("*2"));
    if (
      !primary.hasRole("mounted") &&
      secondary.hasRole("mounted") &&
      race == secondary
    ) chances.modifyChance("mounted", new Arg("*2"));

    return chances.getRandom(r);
  }

  private void putToNation(String role, List<List<Unit>> lists) {
    int i = 0;
    for (List<Unit> list : lists) {
      i++;
      if (nation.unitlists.get(role + "-" + i) == null) nation.unitlists.put(
        role + "-" + i,
        new ArrayList<Unit>()
      );

      nation.unitlists.get(role + "-" + i).addAll(list);
    }
  }

  private List<List<Unit>> sortToLists(List<Unit> templates) {
    List<List<Unit>> lists = this.sortToListsByBasesprite(templates);

    List<List<Unit>> newlist = new ArrayList<List<Unit>>();
    for (List<Unit> mlist : lists) {
      newlist.addAll(this.sortByArmor(mlist));
    }

    lists = null;
    return newlist;
  }

  private List<Unit> sortByGcost(List<Unit> templates) {
    List<Unit> newlist = new ArrayList<Unit>();

    newlist.add(templates.get(0));
    templates.remove(0);

    while (templates.size() > 0) {
      int gcost = templates.get(0).getGoldCost();
      for (int i = newlist.size() - 1; i >= 0; i--) {
        if (gcost > newlist.get(i).getGoldCost()) {
          newlist.add(templates.get(0));
          templates.remove(0);
          break;
        } else if (i == 0) {
          newlist.add(0, templates.get(0));
          templates.remove(0);
        }
      }
    }

    return newlist;
  }

  private List<List<Unit>> sortByArmor(List<Unit> templates) {
    List<List<Unit>> finallist = new ArrayList<List<Unit>>();

    List<Item> allArmor = new ArrayList<Item>();
    for (Unit u : templates) if (
      !allArmor.contains(u.getSlot("armor"))
    ) allArmor.add(u.getSlot("armor"));

    if (
      allArmor.size() == 0 || (allArmor.size() == 1 && allArmor.get(0) == null)
    ) {
      finallist.add(templates);
      return finallist;
    }

    while (templates.size() > 0) {
      String lowestID = allArmor.get(0).id;
      int lowestProt = nationGen.armordb.GetInteger("" + lowestID, "body");
      for (Item armor : allArmor) if (
        nationGen.armordb.GetInteger(armor.id, "body") < lowestProt
      ) {
        lowestID = armor.id;
        lowestProt = nationGen.armordb.GetInteger("" + lowestID, "body");
      }

      List<Item> foobarArmor = new ArrayList<Item>();
      for (Item armor : allArmor) if (
        armor.id.equals("" + lowestID)
      ) foobarArmor.add(armor);
      allArmor.removeAll(foobarArmor);

      List<Unit> newlist = new ArrayList<Unit>();
      for (Unit u : templates) if (
        u.getSlot("armor").id.equals(lowestID)
      ) newlist.add(u);

      templates.removeAll(newlist);
      finallist.add(sortByGcost(newlist));
      //System.out.println("Removing all units with " + nationGen.armordb.GetValue(lowestID, "armorname") + ", #" + newlist.size() + ". " + templates.size() + " remain.");
    }

    return finallist;
  }

  private List<List<Unit>> sortToListsByBasesprite(List<Unit> templates) {
    List<List<Unit>> troops = new ArrayList<List<Unit>>();

    // Sort to lists!
    List<Item> allArmor = new ArrayList<Item>();
    for (Unit u : templates) if (
      !allArmor.contains(u.getSlot("basesprite"))
    ) allArmor.add(u.getSlot("basesprite"));

    while (templates.size() > 0) {
      int lowestHP = this.getHP(templates.get(0));
      int prio = 5;

      for (Unit u : templates) if (this.getHP(u) < lowestHP) {
        lowestHP = this.getHP(u);
      } else if (this.getHP(u) == lowestHP) {
        if (getPrio(u) > prio) {
          int newprio = getPrio(u);
          prio = newprio;
          lowestHP = this.getHP(u);
        }
      }

      List<Unit> newlist = new ArrayList<Unit>();
      for (Unit u : templates) if (
        getHP(u) <= lowestHP + 2 && getPrio(u) == prio
      ) newlist.add(u);

      templates.removeAll(newlist);
      troops.add(newlist);
      //System.out.println("Removing all units with " + nationGen.armordb.GetValue(lowestID, "armorname") + ", #" + newlist.size() + ". " + templates.size() + " remain.");
    }

    return troops;
  }

  /**
   * Returns unit hp, only basesprite and race count.
   * @param u
   * @return
   */
  private int getHP(Unit u) {
    int hp = 0;
    for (Command c : u.race.unitcommands) if (c.command.equals("#hp")) hp +=
    c.args.get(0).getInt();

    for (Command c : u.getSlot("basesprite").commands) if (
      c.command.equals("#hp")
    ) {
      hp += c.args.get(0).getInt();
    }

    if (hp > 0) return hp;
    else return 10;
  }

  private int getPrio(Unit u) {
    return u.getSlot("basesprite").tags.getInt("basespritepriority").orElse(5);
  }
}
