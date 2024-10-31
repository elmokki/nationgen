package nationGen.magic;

import java.util.Random;
import nationGen.NationGenAssets;
import nationGen.chances.EntityChances;
import nationGen.entities.Filter;
import nationGen.misc.ChanceIncHandler;
import nationGen.nation.Nation;

public class SpellGen {

  private Nation n;
  private Random r;

  public SpellGen(Nation n) {
    this.n = n;
    this.r = new Random(n.random.nextInt());
  }

  public void execute(NationGenAssets assets) {
    ChanceIncHandler chandler = new ChanceIncHandler(n);

    // Guaranteed spells
    EntityChances<Filter> map = chandler.handleChanceIncs(
      ChanceIncHandler.retrieveFilters(
        "guaranteedspells",
        "guaranteedspells_default",
        assets.spells,
        null,
        n.races.get(0)
      )
    );

    while (map.hasPossible()) {
      Filter f = map.getRandom(r);

      if (f != null) {
        n.spells.add(f);
      }

      map.eliminate(f);
    }

    // Count spell count weighted by square roots
    int spellcount = 0;
    for (Filter f : n.spells) spellcount +=
    Math.sqrt(f.tags.getAllValues("spell").size());

    // Diagnostics
    MagicPathInts paths = new MagicPathInts();
    n
      .selectCommanders("mage")
      .filter(u -> u.tags.containsName("schoolmage"))
      .forEach(u -> paths.maxWith(u.getMagicPicks(true)));

    int diversity = 0;
    for (MagicPath path : MagicPath.values()) {
      if (
        paths.get(path) > 1 ||
        (path == MagicPath.ASTRAL && paths.get(path) == 1) ||
        (path == MagicPath.BLOOD && paths.get(path) == 1)
      ) diversity++;
    }

    // Random spell count
    int randompicks = r.nextInt(6); // 0-5

    if (r.nextBoolean() && diversity > 2) randompicks *= 0.5;

    if (diversity < 3) randompicks = Math.max(5 - diversity, randompicks);

    if (spellcount >= 8) randompicks--;
    if (spellcount >= 12) randompicks--;
    if (spellcount >= 14) randompicks--;
    if (spellcount >= 16) randompicks--;

    EntityChances<Filter> possible = chandler.handleChanceIncs(
      ChanceIncHandler.retrieveFilters(
        "randomspells",
        "randomspells_default",
        assets.spells,
        null,
        n.races.get(0)
      )
    );

    for (int i = 0; i < randompicks; i++) {
      Filter f = possible.getRandom(n.random);

      if (f != null && f.tags.getAllValues("spell").size() > 0) {
        possible.eliminate(f);
        n.spells.add(f);
      }
    }
  }
}
