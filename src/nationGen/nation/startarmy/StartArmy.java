package nationGen.nation.startarmy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.elmokki.Generic;

import nationGen.Settings;
import nationGen.Settings.SettingsType;
import nationGen.misc.Command;
import nationGen.misc.Tags;
import nationGen.nation.Nation;
import nationGen.nation.pd.Militia;
import nationGen.units.LeadershipType;
import nationGen.units.Unit;

/**
 * Militia are all of the national units that will be used for starting armies,
 * PD, and castle defenders. They are directly related to the Dominions' modding
 * commands #defunit1 and #defunit2.
 * 
 * There are four #defunit1 commands: from #defunit1 to #defunit1d.
 * These are the national unit PD in an unforted province that has 20+ PD,
 * or in a forted province from 1 PD.
 *
 * There are two #defunit2 commands: from #defunit2 to #defunit2b.
 * These are the national unit PD in a forted province that has 20+ PD.
 * 
 * Thus, in Dominions 6, a forted province with 20+ PD can have a total of
 * 6 (heh) different (or duplicated) types of national units defending it.
 */
public class StartArmy {
  private Nation nation;
  private Militia militia;

  private List<Unit> armyUnits = new ArrayList<>();
  private Unit armyCommander;
  private Unit scout;

  private Double militiaMultiplierSetting;

  private Double resMultiplierSetting;
  private Double resUpperThreshold;
  private Double resLowerThreshold;
  private Double resMultiplierThresholdSetting;

  private Double resUpperThresholdChangeSetting;
  private Double resLowerThresholdChangeSetting;

  private Double goldUpperThresholdSetting;
  private Double goldLowerThresholdSetting;

  private Double goldUpperThresholdChangeSetting;
  private Double goldLowerThresholdChangeSetting;

  // The amount of "points" of starting army that will determine how many
  // of each unit the nation gets at the beginning.
  // This budget includes both gold and resource cost. The more expensive
  // the units are, the less we'll get.
  private final int ARMY_BUDGET = 400;

  // The #startunittypeX command to set a nation's starting army unit types
  // accepts 2 different unit types; #startunittype1 and #startunittype2
  private final int NUMBER_OF_START_ARMY_UNIT_TYPES = 2;

  public StartArmy(Nation nation, Militia militia, Settings settings) {
      this.nation = nation;
      this.militia = militia;

      this.militiaMultiplierSetting = settings.get(SettingsType.militiaMultiplier);

      this.resMultiplierSetting = settings.get(SettingsType.resMultiplier);
      this.resUpperThreshold = settings.get(SettingsType.resUpperThreshold);
      this.resLowerThreshold = settings.get(SettingsType.resLowerThreshold);
      this.resMultiplierThresholdSetting = settings.get(SettingsType.resMultiplierThreshold);

      this.resUpperThresholdChangeSetting = settings.get(SettingsType.resUpperThresholdChange);
      this.resLowerThresholdChangeSetting = settings.get(SettingsType.resLowerThresholdChange);

      this.goldUpperThresholdSetting = settings.get(SettingsType.goldUpperThreshold);
      this.goldLowerThresholdSetting = settings.get(SettingsType.goldLowerThreshold);

      this.goldUpperThresholdChangeSetting = settings.get(SettingsType.goldUpperThresholdChange);
      this.goldLowerThresholdChangeSetting = settings.get(SettingsType.goldLowerThresholdChange);

      this.armyUnits.addAll(this.selectSuitableStartArmyUnits(1, 1));
  }

  public Unit getArmyCommander() {
    if (this.armyCommander == null) {
      this.armyCommander = this.selectArmyCommander();
    }

    return this.armyCommander;
  }

  public Unit getScout() {
    if (this.scout == null) {
      this.scout = this.selectScout();
    }

    return this.scout;
  }

  /**
   * Gets the number for the #startunitnbrs modding command. This determines
   * how many of the given unit are given to the player as the starting army.
   * @param unit
   * @return
   */
  public int getAmountOfArmyUnit(Unit unit) {
    // Handle filter etc stuff
    double totalMultiplier = 1;
    Tags tags = Generic.getAllNationTags(nation);
    List<Double> pdMultipliers = tags.getAllDoubles("startarmy_amountmulti");

    for (Double multiplier : pdMultipliers) {
      totalMultiplier *= multiplier;
    }

    double amount = calculateArmyUnitAmount(unit) * totalMultiplier;
    return (int) Math.round(amount);
  }

  public List<String> writeModLines() {
    List<String> lines = new ArrayList<>();
    Unit startUnit1;
    Unit startUnit2;

    if (this.armyUnits.size() < NUMBER_OF_START_ARMY_UNIT_TYPES) {
      throw new IllegalStateException(
        "Expected " +
        NUMBER_OF_START_ARMY_UNIT_TYPES +
        " types of units for the nation's starting army; instead only got " +
        this.armyUnits.size()
      );
    }

    else {
      startUnit1 = this.armyUnits.get(0);
      startUnit2 = this.armyUnits.get(1);
    }

    lines.add("#startcom " + this.getArmyCommander().getRootId());
    lines.add("");

    lines.add("#startunittype1 " + startUnit1.getRootId());
    lines.add("#startunitnbrs1 " + this.getAmountOfArmyUnit(startUnit1));
    lines.add("");

    lines.add("#startunittype2 " + startUnit2.getRootId());
    lines.add("#startunitnbrs2 " + this.getAmountOfArmyUnit(startUnit2));
    lines.add("");

    lines.add("#startscout " + this.getScout().getRootId());
    lines.add("");

    return lines;
  }

  private List<Unit> selectSuitableStartArmyUnits(double goldMultiplier, double resMultiplier) {
    List<Unit> suitableUnits = new ArrayList<>();
    
    // Don't add more than one ranged unit type
    int maxRangedUnitsAllowed = 1;
    int rangedUnitsSelected = 0;

    // Create a list of all possible militia units based on the nation's poses
    List<Unit> possibleUnits = this.nation.combineTroopsToList("infantry");
    possibleUnits.addAll(nation.combineTroopsToList("mounted"));
    possibleUnits.addAll(nation.combineTroopsToList("ranged"));
    possibleUnits.addAll(nation.combineTroopsToList("montagtroops"));

    // Remove unit poses that may be unfit to be militia
    removeUnsuitable(possibleUnits);
    Collections.shuffle(possibleUnits, this.nation.random);

    // Target gold and res cost are the costs with the most chances to get picked.
    // If a unit has these costs, it will very likely be part of the start army.
    double targetGoldCost = this.getTargetGoldCost(possibleUnits) * goldMultiplier;
    double targetResCost = this.getTargetResCost(possibleUnits) * resMultiplier;

    for (int i = 0; i < possibleUnits.size(); i++) {
      Unit nextUnit = possibleUnits.get(i);

      // Max ranged units reached; remove the rest from the possible units' list
      if (nextUnit.isRanged() && rangedUnitsSelected >= maxRangedUnitsAllowed) {
        continue;
      }

      // Select unit that best fits the target gold/resources
      Unit best = selectBestStartArmyUnit(possibleUnits, targetGoldCost, targetResCost);

      // Remove from pool and add to our militia
      possibleUnits.remove(best);
      suitableUnits.add(best);

      if (best.isRanged()) {
        rangedUnitsSelected++;
      }

      if (suitableUnits.size() == this.NUMBER_OF_START_ARMY_UNIT_TYPES) {
        break;
      }
    }

    if (suitableUnits.isEmpty()) {
      throw new IllegalStateException(
        "No single suitable unit pose to start army was found! (race: " +
        this.nation.races.get(0).name +
        ", nation seed: " +
        this.nation.getSeed() +
        ". Are too many poses tagged with #cannot_be_start_army?"
      );
    }

    return suitableUnits;
  }

  private void removeUnsuitable(List<Unit> units) {
    List<Unit> unsuitable = new ArrayList<>();

    for (Unit u : units) {
      if (u.isMontagRecruitableTemplate() == true) {
        unsuitable.add(u);
      }
    }

    // Remove other unsuitable units assuming that there would still be some left to pick
    if (unsuitable.size() < units.size()) {
      units.removeAll(unsuitable);
    }

    // If ALL units were deemed unsuitable, we will make an exception and not remove them,
    // but this should get resolved by making sure each race has a good number of unit poses
    else {
      throw new IllegalStateException(
        "Nation " +
        this.nation.nationid +
        " (primary race: "+
        this.nation.races.get(0).name +
        ") does not have any suitable units for their start army."
      );
    }
  }

  private double getTargetGoldCost(List<Unit> possibleUnits) {
    double totalGold = 0;
    double targetGoldCost = 0;
    Tags tags = Generic.getAllNationTags(nation);
    List<Double> startArmyGoldCostMultipliers = tags.getAllDoubles("startarmy_targetgcostmulti");
    List<Double> pdGoldCostMultipliers = tags.getAllDoubles("pd_targetgcostmulti");
    List<Double> effectiveGoldCostMultipliers = new ArrayList<>(startArmyGoldCostMultipliers);

    if (startArmyGoldCostMultipliers.isEmpty()) {
      effectiveGoldCostMultipliers.addAll(pdGoldCostMultipliers);
    }

    for (Unit u : possibleUnits) {
      totalGold += u.getGoldCost(true);
    }

    // Set a fixed target if neither #startarmy_targetgcost nor #pd_targetgcost exist
    // on the nation, or just average all suitable unit gold costs.
    targetGoldCost = tags
      .getDouble("startarmy_targetgcost")
      .orElse(
        tags.getDouble("pd_targetgcost")
          .orElse(totalGold / possibleUnits.size())
      );

    // Factor in all cost multipliers
    for (Double multiplier : effectiveGoldCostMultipliers) {
      targetGoldCost *= multiplier;
    }

    return targetGoldCost;
  }

  private double getTargetResCost(List<Unit> possibleUnits) {
    double totalRes = 0;
    double targetResCost = 0;
    Tags tags = Generic.getAllNationTags(nation);
    List<Double> startArmyResCostMultipliers = tags.getAllDoubles("startarmy_targetrcostmulti");
    List<Double> pdResCostMultipliers = tags.getAllDoubles("pd_targetrcostmulti");
    List<Double> effectiveResCostMultipliers = new ArrayList<>(startArmyResCostMultipliers);

    if (startArmyResCostMultipliers.isEmpty()) {
      effectiveResCostMultipliers.addAll(pdResCostMultipliers);
    }

    for (Unit u : possibleUnits) {
      totalRes += u.getResCost(true, true);
    }

    // Set a fixed target if neither #startarmy_targetrcost nor #pd_targetrcost exist
    // on the nation, or just average all suitable unit resource costs.
    targetResCost = tags
      .getDouble("startarmy_targetrcost")
      .orElse(
        tags.getDouble("pd_targetrcost")
          .orElse(totalRes / possibleUnits.size())
      );

    // Factor in all cost multipliers
    for (Double multiplier : effectiveResCostMultipliers) {
      targetResCost *= multiplier;
    }

    return targetResCost;
  }
  
  private Unit selectBestStartArmyUnit(
    List<Unit> possibleUnits,
    double targetGoldCost,
    double targetResCost
  ) {
    Unit best = possibleUnits.get(0);
    double bestscore = calculateScoreForStartArmy(best, targetGoldCost, targetResCost);

    // Iterate through all units, updating the best one if a better one is found
    for (Unit unit : possibleUnits) {
      double score = calculateScoreForStartArmy(unit, targetGoldCost, targetResCost);

      if (score > bestscore) {
        bestscore = score;
        best = unit;
      }
    }

    return best;
  }

  private double calculateScoreForStartArmy(Unit unit, double targetGoldCost, double targetResCost) {
    double goldScore = getUnitGoldScoreForStartArmy(unit, targetGoldCost);
    double resScore = getUnitResScoreForStartArmy(unit, targetResCost);

    // Gold weighs less than resource cost for this purpose
    return 0.75 * goldScore + resScore;
  }

  private double getUnitGoldScoreForStartArmy(Unit unit, double targetGoldCost) {
    double unitGoldCost = unit.getGoldCost(true);

    // Lower score the further away from 0 (a perfect match when target gold and unit gold are the same)
    double score = -Math.pow(targetGoldCost - unitGoldCost, 2);
    return score;
  }

  private double getUnitResScoreForStartArmy(Unit unit, double targetResCost) {
    double unitResCost = unit.getResCost(true, true);

    // Lower score the further away from 0 (a perfect match when target resources and unit resources are the same)
    double score = -Math.pow(targetResCost - unitResCost, 2);
    return score;
  }

  private Unit selectArmyCommander() {
    List<Unit> commanders = this.nation.listCommanders("commander");

    List<Unit> pdUnits = militia.getUsedBasicPdTypes()
      .stream()
      .map(type -> militia.getMilitiaUnit(type))
      .collect(Collectors.toList());

    return this.selectArmyCommander(commanders, pdUnits);
  }

  private Unit selectScout() {
    List<Unit> scouts = this.nation.listCommanders("scout");

    if (scouts.size() == 0) {
      throw new IllegalStateException(
        "Expected nation to have at least 1 scout; instead found none"
      );
    }

    return scouts.get(0);
  }

  private Unit selectArmyCommander(List<Unit> commanderCandidates, List<Unit> pdUnits) {
    boolean needsUndeadLeadership = pdUnits.stream()
      .filter(u -> u.isUndead() || u.isAlmostUndead() || u.isDemon())
      .findAny()
      .isPresent();

    boolean needsMagicLeadership = pdUnits.stream()
      .filter(u -> u.isMagicBeing())
      .findAny()
      .isPresent();

    List<Unit> validCommanders = commanderCandidates.stream()
      .filter(c -> {
        return (needsUndeadLeadership == false || c.hasLeadership(LeadershipType.UNDEAD)) &&
            (needsMagicLeadership == false || c.hasLeadership(LeadershipType.MAGIC_BEING));
      })
      .collect(Collectors.toList());

    Unit selectedCommander;

    if (validCommanders.size() == 0) {
      selectedCommander = commanderCandidates.getFirst();

      if (needsUndeadLeadership) {
        selectedCommander.addCommands(Command.args("#undcommand", "40"));
      }

      if (needsMagicLeadership) {
        selectedCommander.addCommands(Command.args("#magiccommand", "40"));
      }
    }
    
    else {
      selectedCommander = validCommanders.getFirst();
    }

    return selectedCommander;
  }

  /**
   * Calculate the number of units of this type for the start army, as per the
   * #startunitnbrs modding command. This is affected by some NationGen settings,
   * which set thresholds and flexible margins for the effective gold and
   * resources cost of a unit as part of the starting army.
   * 
   * This adjusted cost is then used to divide the army budget, which
   * is simply an arbitrary constant. The higher the budget, the bigger the
   * resulting amounts will be. A final result of 20 here means "20 units of
   * this type in the starting army".
   * @param unit
   * @return
   */
  private double calculateArmyUnitAmount(Unit unit) {
    int res = getAdjustedUnitResCost(unit, true, true);
    int gold = getAdjustedUnitGoldCost(unit, true);
    double score = gold + res;
    double multiplier = this.ARMY_BUDGET / score;
    multiplier = multiplier * this.militiaMultiplierSetting;
    return multiplier;
  }

  /**
   * Adjust a unit's effective resource cost based on the current NationGen settings.
   * Note this is simply to determine how costly the unit is in terms of its presence
   * in the starting army. It does not perform any changes to the unit's actual recruitment
   * res cost.
   * @param unit
   * @param useSize
   * @return adjusted resource cost for start army amount calculations
   */
  private int getAdjustedUnitResCost(Unit unit, boolean useSize, boolean includeMount) {
    int res = unit.getResCost(useSize, includeMount);

    double adjustedUpperResCost = res + this.resUpperThresholdChangeSetting;
    double adjustedLowerResCost = res + this.resLowerThresholdChangeSetting;

    boolean isResCostAboveThreshold = res > this.resUpperThreshold;
    boolean isResCostBelowThreshold = res < this.resLowerThreshold;

    if (isResCostAboveThreshold && this.resUpperThresholdChangeSetting < 0) {
      res = (int) Math.max((adjustedUpperResCost), (0 + this.resUpperThreshold));
    }

    else if (isResCostAboveThreshold && this.resUpperThresholdChangeSetting > 0) {
      res = (int) adjustedUpperResCost;
    }

    if (isResCostBelowThreshold && this.resLowerThresholdChangeSetting > 0) {
      res = (int) Math.min(adjustedLowerResCost, (0 + this.resLowerThreshold));
    }

    else if (isResCostBelowThreshold && this.resLowerThresholdChangeSetting < 0) {
      res = (int) adjustedLowerResCost;
    }
    
    if (res > this.resMultiplierThresholdSetting) {
      res = (int) (
        this.resMultiplierThresholdSetting +
        (res - this.resMultiplierThresholdSetting) *
        this.resMultiplierSetting
      );
    }

    return res;
  }
  
  /**
   * Adjust a unit's effective gold cost based on the current NationGen settings.
   * Note this is simply to determine how costly the unit is in terms of its presence
   * in the starting army. It does not perform any changes to the unit's actual recruitment
   * gold cost.
   * @param unit
   * @param useSize
   * @return adjusted gold cost for start army amount calculations
   */
  private int getAdjustedUnitGoldCost(Unit unit, boolean includeMount) {
    int gold = unit.getGoldCost(includeMount);

    double adjustedUpperGoldCost = gold + this.goldUpperThresholdChangeSetting;
    double adjustedLowerGoldCost = gold + this.goldLowerThresholdChangeSetting;

    boolean isGoldCostAboveThreshold = gold > this.goldUpperThresholdSetting;
    boolean isGoldCostBelowThreshold = gold < this.goldLowerThresholdSetting;

    if (isGoldCostAboveThreshold && this.goldUpperThresholdChangeSetting < 0) {
      gold = (int) Math.max((adjustedUpperGoldCost), (0 + this.goldUpperThresholdChangeSetting));
    }

    else if (isGoldCostAboveThreshold && this.goldUpperThresholdChangeSetting > 0) {
      gold = (int) adjustedUpperGoldCost;
    }

    if (isGoldCostBelowThreshold && this.goldLowerThresholdChangeSetting > 0) {
      gold = (int) Math.min(adjustedLowerGoldCost, (0 + this.goldLowerThresholdChangeSetting));
    }

    else if (isGoldCostBelowThreshold && this.goldLowerThresholdChangeSetting < 0) {
      gold = (int) adjustedLowerGoldCost;
    }

    return gold;
  }
}
