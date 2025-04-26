package nationGen.nation;

import com.elmokki.Generic;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import nationGen.NationGen;
import nationGen.Settings.SettingsType;
import nationGen.misc.Command;
import nationGen.misc.Tags;
import nationGen.units.Unit;

public class PDSelector {

  private NationGen ng;
  private Nation n;
  private Random r;

  // The amount of "points" we get for every 1 militia/starting unit.
  // This covers both gold and resource cost. If the budget is 20, and
  // the selected unit costs 10 gold and 10 resources, then we get 1 unit.
  private static final int BUDGET_PER_MILITIA_UNIT = 25;

  // The multiplier for the above budget. If the budget above is enough for
  // 1 unit, and this number is 40, then we'll get 40 * 1 = 40 units.
  // If the militia was more expensive and yielded a ratio of 0.5,
  // then we'd get 40 * 0.5 = 20 units.
  private static final int NUMBER_OF_RELATIVE_UNITS = 40;

  // Exponentially increases the budget cost of a unit's resources. More resource
  // cost means more protection which means much lower attrition when expanding,
  // so higher resource costs should be priced higher than low resource costs.
  private static final float RESOURCE_COST_INFLATION = 1.12f;

  public PDSelector(Nation n, NationGen ng) {
    this.ng = ng;
    this.n = n;
    r = new Random(n.random.nextInt());
  }

  public Unit getStartArmyCommander() {
    return getCommander(1, true);
  }

  public Unit getPDCommander(int rank) {
    return getCommander(rank, false);
  }

  public int getIDforPD(Unit u) {
    if (u.pose.tags.containsName("montagpose")) {
      int id = -1;
      for (Command c : u.getCommands()) if (
        c.command.equals("#firstshape")
      ) id = c.args.get(0).getInt();

      if (id != -1) return id;
      else return u.id;
    } else {
      return u.id;
    }
  }

  private Unit getCommander(int rank, boolean startarmy) {
    if (rank < 1) rank = 1;
    if (rank > 2) rank = 2;

    List<Unit> commanders = n.listCommanders("commander");

    // Chance to get a priest as potential commander for PD; ~75% of Dom6 nations have a caster (usually priest or mage-priest as PD rank 2)
    // For now, there's a small chance to a T2 - and a very good chance to get a T1 - for rank 2, and much smaller chances a T1 for rank 1
    if (!startarmy) {
      List<Unit> priests = n.listCommanders("priest");
      double rand = r.nextDouble();
      if (rank == 1 && rand < 0.05) commanders.add(0, priests.get(0));
      else if (rank == 2 && rand < 0.15 && priests.size() > 1) commanders.add(
        1,
        priests.get(1)
      );
      else if (rank == 2 && rand < 0.75) commanders.add(1, priests.get(0));
    }

    List<Unit> others = new ArrayList<>();
    if (startarmy) {
      others.add(getMilitia(1, 1, !startarmy));
      others.add(getMilitia(1, 2, !startarmy));
    } else if (rank == 1) {
      others.add(getMilitia(1, 1, !startarmy));
      others.add(getMilitia(2, 1, !startarmy));
    } else if (rank == 2) {
      others.add(getMilitia(1, 1, !startarmy));
      others.add(getMilitia(2, 1, !startarmy));
    }

    boolean ud_demon = false;
    boolean magicbeing = false;

    for (Unit u : others) for (Command c : u.commands) {
      if (
        c.command.equals("#undead") ||
        c.command.equals("#demon") ||
        c.command.equals("#almostundead")
      ) ud_demon = true;
      else if (c.command.equals("#magicbeing")) magicbeing = true;
    }

    List<String> badprefixes = new ArrayList<>();
    badprefixes.add("no");
    if (startarmy) badprefixes.add("poor");

    // First pass at trying to find a suitable commander
    List<Unit> validComs = new ArrayList<>();
    for (Unit u : commanders) {
      boolean udvalid = false;
      boolean magicvalid = false;
      for (Command c : u.commands) {
        if (ud_demon && c.command.contains("undeadleader")) {
          String leader = c.command.substring(
            1,
            c.command.indexOf("undeadleader")
          );
          if (!badprefixes.contains(leader)) {
            udvalid = true;
          }
        }
        if (magicbeing && c.command.contains("magicleader")) {
          String leader = c.command.substring(
            1,
            c.command.indexOf("magicleader")
          );
          if (!badprefixes.contains(leader)) {
            magicvalid = true;
          }
        }
      }

      if (!ud_demon || udvalid) if (!magicbeing || magicvalid) validComs.add(u);
    }

    boolean failsafe = false;
    if (validComs.size() == 0 || (validComs.size() == 1 && rank == 2)) {
      failsafe = true;
      validComs = commanders;
      if (commanders.size() == 1 && rank == 2) rank = 1;
    }

    Unit com = validComs.get(rank - 1);
    if (failsafe) {
      if (magicbeing) com.commands.add(Command.args("#magiccommand", "40"));
      if (ud_demon) com.commands.add(Command.args("#undcommand", "40"));
    }

    return com;
  }

  public Unit getWallUnit(boolean montag_chassis_allowed) {
    // Ranged units and infantry with decent ranged weapons
    List<Unit> units = n.combineTroopsToList("ranged");
    List<Unit> tempunits = n.combineTroopsToList("infantry");
    for (Unit u : tempunits) {
      if (u.getSlot("bonusweapon") != null) {
        if (
          n.nationGen.weapondb.GetInteger(
            u.getSlot("bonusweapon").id,
            "rng",
            0
          ) >
          15
        ) {
          units.add(u);
        }
      }
    }
    if (!montag_chassis_allowed) {
      tempunits = n.combineTroopsToList("montagtroops");
      for (Unit u : tempunits) {
        if (
          u.getSlot("bonusweapon") != null &&
          n.nationGen.weapondb.GetInteger(
            u.getSlot("bonusweapon").id,
            "rng",
            0
          ) >
          15
        ) {
          units.add(u);
        }
        if (
          u.getSlot("weapon") != null &&
          n.nationGen.weapondb.GetInteger(u.getSlot("weapon").id, "rng", 0) > 15
        ) {
          units.add(u);
        }
      }
    }

    removeUnsuitable(montag_chassis_allowed, units);
    List<Unit> unsuitable;

    // No options: Any infantry with any ranged
    if (units.size() == 0) {
      tempunits = n.combineTroopsToList("infantry");
      for (Unit u : tempunits) {
        if (u.getSlot("bonusweapon") != null) {
          if (
            n.nationGen.weapondb.GetInteger(
              u.getSlot("bonusweapon").id,
              "rng",
              0
            ) !=
            0
          ) {
            units.add(u);
          }
        }
      }

      unsuitable = new ArrayList<>();
      for (Unit u : units) if (
        Generic.getAllUnitTags(u).containsName("cannot_be_pd") ||
        (u.pose.tags.containsName("montagpose") && !montag_chassis_allowed)
      ) unsuitable.add(u);

      if (units.size() > unsuitable.size()) units.removeAll(unsuitable);
    }

    // No options: Any infantry
    if (units.size() == 0) {
      units = n.combineTroopsToList("infantry");
      unsuitable = new ArrayList<>();
      for (Unit u : units) if (
        Generic.getAllUnitTags(u).containsName("cannot_be_pd") ||
        (u.pose.tags.containsName("montagpose") && !montag_chassis_allowed)
      ) unsuitable.add(u);

      if (units.size() > unsuitable.size()) units.removeAll(unsuitable);
    }
    // Try to get unit
    Unit unit = getWallUnit(units);

    // Failsafe: Just get something
    if (unit == null) {
      units = n.combineTroopsToList("infantry");
      units.addAll(n.combineTroopsToList("mounted"));
      units.addAll(n.combineTroopsToList("ranged"));
      if (!montag_chassis_allowed) units.addAll(
        n.combineTroopsToList("montagtroops")
      );

      unit = getWallUnit(units);
    }

    return unit;
  }

  public Unit getWallUnit(List<Unit> units) {
    double totalr = 0;
    double totalg = 0;
    for (Unit u : units) {
      totalr += u.getResCost(true);
      totalg += u.getGoldCost();
    }
    double targetgcost = (totalg / units.size());
    double targetrcost = (totalr / units.size());

    Unit best = units.get(0);
    double bestscore = scoreForMilitia(best, targetrcost, targetgcost);
    for (Unit u : units) {
      double score =
        scoreForMilitia(u, targetrcost, targetgcost) *
        ((u.getCommandValue("#castledef", 0) + 1));
      if (bestscore >= score) {
        bestscore = score;
        best = u;
      }
    }

    return best;
  }

  public Unit getGateUnit(boolean montag_chassis_allowed) {
    List<Unit> units = n.combineTroopsToList("infantry");
    List<Unit> unsuitable = new ArrayList<>();

    for (Unit u : units) {
      if (
        Generic.getAllUnitTags(u).containsName("cannot_be_pd") ||
        (u.pose.tags.containsName("montagpose") && !montag_chassis_allowed)
      ) {
        unsuitable.add(u);
      }
    }

    if (units.size() > unsuitable.size()) {
      units.removeAll(unsuitable);
    }

    // Try to get unit
    Unit unit = getGateUnit(units);

    // Failsafe: Just get something
    if (unit == null) {
      units = n.combineTroopsToList("infantry");
      units.addAll(n.combineTroopsToList("mounted"));
      units.addAll(n.combineTroopsToList("ranged"));
      if (!montag_chassis_allowed) units.addAll(
        n.combineTroopsToList("montagtroops")
      );

      unit = getGateUnit(units);
    }

    return unit;
  }

  public Unit getGateUnit(List<Unit> units) {
    double totalr = 0;
    double totalg = 0;

    for (Unit u : units) {
      totalr += u.getResCost(true);
      totalg += u.getGoldCost();
    }

    double targetgcost = (totalg / units.size());
    double targetrcost = (totalr / units.size());

    Unit best = units.get(0);
    double bestscore = scoreForMilitia(best, targetrcost, targetgcost);

    for (Unit u : units) {
      double score =
        scoreForMilitia(u, targetrcost, targetgcost) *
        ((u.getCommandValue("#castledef", 0) + 1));

      if (bestscore >= score) {
        bestscore = score;
        best = u;
      }
    }

    return best;
  }

  public Unit getMilitia(int rank, int tier) {
    return getMilitia(rank, tier, true);
  }

  /**
   * Gets the rank:th best militia unit of tier:th tier
   * @param rank rankth best (1-2)
   * @param tier tier increases possible resource cost.
   * @return the matching unit
   */
  public Unit getMilitia(int rank, int tier, boolean montag_chassis_allowed) {
    if (rank < 1) rank = 1;
    if (rank > 4) rank = 4;

    List<List<Unit>> militia = new ArrayList<>();

    List<Unit> units = n.combineTroopsToList("infantry");
    units.addAll(n.combineTroopsToList("mounted"));
    units.addAll(n.combineTroopsToList("ranged"));
    if (!montag_chassis_allowed) units.addAll(
      n.combineTroopsToList("montagtroops")
    );

    removeUnsuitable(montag_chassis_allowed, units);

    // Target resource/gcost is instead means
    double totalr = 0;
    double totalg = 0;
    for (Unit u : units) {
      totalr += u.getResCost(true);
      totalg += u.getGoldCost();
    }

    // handle commands for increasing/decreasing the target
    Tags tags = Generic.getAllNationTags(n);

    double targetgcost = tags
      .getDouble("pd_targetgcost")
      .orElse(totalg / units.size());
    double targetrcost = tags
      .getDouble("pd_targetrcost")
      .orElse(totalr / units.size());

    for (Double multi : tags.getAllDoubles(
      "pd_targetrcostmulti"
    )) targetrcost *= multi;
    for (Double multi : tags.getAllDoubles(
      "pd_targetgcostmulti"
    )) targetgcost *= multi;

    // Do the magic

    for (int i = 0; i < tier; i++) {
      if (units.size() == 0) {
        //System.out.println("NOT ENOUGH UNITS FOR MILITIA?! WHAT IS THIS MADNESS? (Nation " + this.nationid + ")");

        units = n.combineTroopsToList("infantry");
        units.addAll(n.combineTroopsToList("mounted"));
        units.addAll(n.combineTroopsToList("ranged"));
        //break;
      }

      List<Unit> rankunits = new ArrayList<>();

      targetrcost = targetrcost * 1.2;
      while (rankunits.size() < 2 && units.size() > 0) {
        boolean canBeRanged = true;
        if (i > 0 && militia.get(i - 1).get(0).isRanged()) canBeRanged = false;
        if (rankunits.size() > 0 && rankunits.get(0).isRanged()) canBeRanged =
          false;

        Unit best = units.get(0);
        double bestscore = scoreForMilitia(best, targetrcost, targetgcost);
        for (Unit u : units) {
          if (!u.isRanged() || canBeRanged) {
            double score = scoreForMilitia(u, targetrcost, targetgcost);
            if (bestscore >= score) {
              bestscore = score;
              best = u;
            }
          }
        }
        units.remove(best);
        rankunits.add(best);
      }

      while (rankunits.size() < 4) {
        //System.out.println("MILITIA PROBLEM - FAILSAFE INITIATED FOR RACE " + this.races.get(0).name + " NATION " + this.nationid);
        rankunits.add(rankunits.get(0));
      }

      militia.add(i, rankunits);
    }

    return militia.get(tier - 1).get(rank - 1);
  }

  private void removeUnsuitable(
    boolean montag_chassis_allowed,
    List<Unit> units
  ) {
    List<Unit> unsuitable = new ArrayList<>();
    for (Unit u : units) if (
      Generic.getAllUnitTags(u).containsName("cannot_be_pd") ||
      (u.pose.tags.containsName("montagpose") && !montag_chassis_allowed)
    ) unsuitable.add(u);

    if (units.size() > unsuitable.size()) units.removeAll(unsuitable);
  }

  private double scoreForMilitia(Unit u, double targetres, double targetgold) {
    double goldscore = Math.abs(u.getGoldCost() - targetgold);
    if (u.getGoldCost() < targetgold * 0.7) goldscore = goldscore * 2;
    if (u.getGoldCost() > targetgold * 2) goldscore = goldscore * 2;

    double resscore = Math.abs(u.getResCost(false) - targetres);
    if (u.getResCost(false) < targetres * 0.7) resscore = resscore * 2;
    if (u.getResCost(false) > targetres * 2.25) resscore = resscore * 2;

    if (u.isRanged() && u.getResCost(false) < 20) resscore = resscore * 0.25;

    //System.out.println("TARGET: " + targetres + " " + targetgold);
    //System.out.println("UNIT  : " + u.getResCost() + " " + u.getGoldCost() + " - " + u.id + " / " + u.name);
    //System.out.println("--> " + (0.75*goldscore + resscore));
    return 0.75 * goldscore + resscore;
  }

  public int getStartArmyAmount(Unit u) {
    // Handle filter etc stuff
    double filtermulti = 1;
    Tags tags = Generic.getAllNationTags(n);
    for (Double multi : tags.getAllDoubles(
      "startarmy_amountmulti"
    )) filtermulti *= multi;

    double amount = militiaAmount(u) * filtermulti;

    return (int) Math.round(amount);
  }

  public int getMilitiaAmount(Unit u) {
    // Handle filter etc stuff
    double filtermulti = 1;
    Tags tags = Generic.getAllNationTags(n);

    for (Double multi : tags.getAllDoubles("pd_amountmulti")) {
      filtermulti *= multi;
    }

    double amount = militiaAmount(u) * filtermulti;
    return (int) Math.round(amount);
  }

  private double militiaAmount(Unit u) {
    int res = u.getResCost(true);
    int gold = u.getGoldCost();

    if (
      res > ng.settings.get(SettingsType.resUpperTreshold) &&
      ng.settings.get(SettingsType.resUpperTresholdChange) < 0
    ) res = Math.max(
      (int) (res + ng.settings.get(SettingsType.resUpperTresholdChange)),
      (int) (0 + ng.settings.get(SettingsType.resUpperTreshold))
    );
    else if (
      res > ng.settings.get(SettingsType.resUpperTreshold) &&
      ng.settings.get(SettingsType.resUpperTresholdChange) > 0
    ) res += ng.settings.get(SettingsType.resUpperTresholdChange);
    if (
      res < ng.settings.get(SettingsType.resLowerTreshold) &&
      ng.settings.get(SettingsType.resLowerTresholdChange) > 0
    ) res = Math.min(
      (int) (res + ng.settings.get(SettingsType.resLowerTresholdChange)),
      (int) (0 + ng.settings.get(SettingsType.resLowerTreshold))
    );
    else if (
      res < ng.settings.get(SettingsType.resLowerTreshold) &&
      ng.settings.get(SettingsType.resLowerTresholdChange) < 0
    ) res += ng.settings.get(SettingsType.resLowerTresholdChange);
    if (
      gold > ng.settings.get(SettingsType.goldUpperTreshold) &&
      ng.settings.get(SettingsType.goldUpperTresholdChange) < 0
    ) gold = Math.max(
      (int) (gold + ng.settings.get(SettingsType.goldUpperTresholdChange)),
      (int) (0 + ng.settings.get(SettingsType.goldUpperTreshold))
    );
    else if (
      gold > ng.settings.get(SettingsType.goldUpperTreshold) &&
      ng.settings.get(SettingsType.goldUpperTresholdChange) > 0
    ) gold += ng.settings.get(SettingsType.goldUpperTresholdChange);
    if (
      gold < ng.settings.get(SettingsType.goldLowerTreshold) &&
      ng.settings.get(SettingsType.goldLowerTresholdChange) > 0
    ) gold = Math.min(
      (int) (gold + ng.settings.get(SettingsType.goldLowerTresholdChange)),
      (int) (0 + ng.settings.get(SettingsType.goldLowerTreshold))
    );
    else if (
      gold < ng.settings.get(SettingsType.goldLowerTreshold) &&
      ng.settings.get(SettingsType.goldLowerTresholdChange) < 0
    ) gold += ng.settings.get(SettingsType.goldLowerTresholdChange);

    if (res > ng.settings.get(SettingsType.resMultiTreshold)) res =
      (int) (ng.settings.get(SettingsType.resMultiTreshold) +
        (res - ng.settings.get(SettingsType.resMultiTreshold)) *
        ng.settings.get(SettingsType.resMulti));

    // The higher the score, the smaller your starting army will be:
    // 20 / (rescost + goldcost) * 10 * militiaMultiplier = amount of specified unit in militia
    // multiplier is 1 by default in settings, but may be increased by commands in generation

    double score = gold + Math.pow(res, RESOURCE_COST_INFLATION);

    double multi = BUDGET_PER_MILITIA_UNIT / score;

    double result = multi * NUMBER_OF_RELATIVE_UNITS;

    result = result * ng.settings.get(SettingsType.militiaMultiplier);

    return result;
  }

  // Get the mult amount for the #wallmult and #guardmult mod commands
  public int getCastleDefenderMult(Unit u) {
    // Handle filter etc stuff
    double filtermulti = 1;
    Tags tags = Generic.getAllNationTags(n);

    for (Double multi : tags.getAllDoubles("pd_amountmulti")) {
      filtermulti *= multi;
    }

    double amount = castleDefenderMult(u) * filtermulti;
    return (int) Math.round(amount);
  }

  private double castleDefenderMult(Unit u) {
    double amount = militiaAmount(u);
    return Math.floor(amount * 0.5);
  }
}
