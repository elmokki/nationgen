package nationGen.magic;

import java.util.List;
import java.util.Random;
import nationGen.NationGenAssets;
import nationGen.chances.EntityChances;
import nationGen.entities.Filter;
import nationGen.misc.ChanceIncHandler;
import nationGen.nation.Nation;

public class SpellGen {

  private Nation nation;

  public SpellGen(Nation nation) {
    this.nation = nation;
  }

  public void execute(NationGenAssets assets) {
    ChanceIncHandler chandler = new ChanceIncHandler(nation);
    List<Filter> guaranteedSpellList = ChanceIncHandler.retrieveFilters(
        "guaranteedspells",
        "guaranteedspells_default",
        assets.spells,
        null,
        this.nation.races.get(0)
      );

    SpellGen.selectGuaranteedSpells(this.nation, guaranteedSpellList, chandler);

    // Count spell count weighted by square roots
    int spellcount = 0;

    for (Filter spellsFilter : this.nation.spells) {
      int spellsContained = spellsFilter.tags.getAllValues("spell").size();
      spellcount += Math.sqrt(spellsContained);
    }

    int pathDiversityScore = SpellGen.calculatePathDiversityScore(this.nation);

    // Random spell count
    int randompicks = SpellGen.calculateNumberOfRandomSpellPicks(6, pathDiversityScore, spellcount, this.nation.random);

    // Get all random spells
    List<Filter> randomSpellList = ChanceIncHandler.retrieveFilters(
      "randomspells",
      "randomspells_default",
      assets.spells,
      null,
      nation.races.get(0)
    );

    // Keep selecting random spells until there are no picks left
    SpellGen.selectRandomSpells(randompicks, this.nation, randomSpellList, chandler);
  }

  /**
   * Given a nation and a list of spells, the function will recursively call itself until all possible spell filters
   * have been selected. It will re-run the chanceincs at every iteration, too, to account for conditions which depend
   * on other spells that the nation may have obtained (e.g. to not give multiple types of spellsinger communions).
   * @param nation - The nation that will get the spells assigned
   * @param spellList - The list of spell filters to pick from
   * @param chandler - The ChanceIncHandler to run all conditions
   */
  private static void selectGuaranteedSpells(Nation nation, List<Filter> spellList, ChanceIncHandler chandler) {
    // Run all conditions to get the probability weights
    EntityChances<Filter> possibleSpells = chandler.handleChanceIncs(spellList);

    // Select a spell filter based on the weights
    Filter selected = possibleSpells.getRandom(nation.random);

    // If one was found, add to the nation, remove from the list
    if (selected != null) {
      nation.spells.add(selected);
      spellList.remove(selected);
      possibleSpells.eliminate(selected);
    }

    // If there are still spells in the pool with more than 0 weights, re-run
    if (possibleSpells.hasPossible()) {
      SpellGen.selectGuaranteedSpells(nation, spellList, chandler);
    }
  }

  /**
   * Given a maximum number of picks, a nation and a list of spells, the function will recursively call itself until all
   * possible spell filters have been selected. It will re-run the chanceincs at every iteration, too, to account for
   * conditions which depend on other spells that the nation may have obtained.
   * @param numberOfPicks - Non-inclusive maximum boundary (up to, not including)
   * @param nation - Nation that will get the spells assigned
   * @param spellList - The list of spell filters to pick from
   * @param chandler - The ChanceIncHandler to run all conditions
   */
  private static void selectRandomSpells(int numberOfPicks, Nation nation, List<Filter> spellList, ChanceIncHandler chandler) {
    // Run all conditions to get the probability weights
    EntityChances<Filter> possibleSpells = chandler.handleChanceIncs(spellList);

    // Select a spell filter based on the weights
    Filter selectedSpell = possibleSpells.getRandom(nation.random);

    // Reduce number of picks regardless of whether one was found (we want to make sure recursion ends)
    numberOfPicks--;

    // If a spell filter that contains spells was found, remove it from the list and add it to the nation
    if (selectedSpell != null && selectedSpell.tags.getAllValues("spell").size() > 0) {
      nation.spells.add(selectedSpell);
      spellList.remove(selectedSpell);
    }

    // If there are still picks possible, re-run
    if (numberOfPicks > 0) {
      SpellGen.selectRandomSpells(numberOfPicks, nation, spellList, chandler);
    }
  }

  /**
   * Calculate a nation's path diversity score. This will check the maximum level
   * in each path that a nation's mage roster can achieve, and score it.
   * @param nation - The Nation to score
   * @return An integer score where the higher value means the more path diversity
   */
  private static int calculatePathDiversityScore(Nation nation) {
    int diversity = 0;
    MagicPathInts maxPaths = new MagicPathInts();

    // Get the max value of the nation in each path
    nation.selectCommanders("mage")
      .filter(u -> u.tags.containsName("schoolmage"))
    .forEach(u -> maxPaths.maxWith(u.getMagicPicks(true)));

    for (MagicPath path : MagicPath.values()) {
      // For each path above 1, increase diversity
      if (maxPaths.get(path) > 1) {
        diversity++;
      }

      // If max astral is only 1, increase diversity anyway
      else if (path == MagicPath.ASTRAL && maxPaths.get(path) == 1) {
        diversity++;
      }
      
      // If max blood is only 1, increase diversity anyway
      else if (path == MagicPath.BLOOD && maxPaths.get(path) == 1) {
        diversity++;
      }
    }

    return diversity;
  }

  /**
   * Determine how many random spell picks a Nation should get based on an arbitrary upper limit,
   * its path diversity score, and a weighted count of already selected spells.
   * @param upperLimit - The upper limit (not included) of spells that can be rolled
   * @param pathDiversityScore - An integer representing how diverse a Nation's path roster is
   * @param weighterGuaranteedSpellCount - A weighted count of the spells that the nation already selected
   * @param random - A Random object to keep probabilities seeded
   * @return The maximum number of random spell picks that this Nation is allowed
   */
  private static int calculateNumberOfRandomSpellPicks(int upperLimit, int pathDiversityScore, int weighterGuaranteedSpellCount, Random random) {
    int numberOfPicks = random.nextInt(upperLimit);

    // 50% chance to halve number
    if (random.nextBoolean() && pathDiversityScore > 2) {
      numberOfPicks *= 0.5;
    }

    else if (pathDiversityScore < 3) {
      numberOfPicks = Math.max(5 - pathDiversityScore, numberOfPicks);
    }

    if (weighterGuaranteedSpellCount >= 8) numberOfPicks--;
    if (weighterGuaranteedSpellCount >= 12) numberOfPicks--;
    if (weighterGuaranteedSpellCount >= 14) numberOfPicks--;
    if (weighterGuaranteedSpellCount >= 16) numberOfPicks--;

    return numberOfPicks;
  }
}
