package nationGen.naming;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.chances.EntityChances;
import nationGen.magic.MageGenerator;
import nationGen.magic.MagicPath;
import nationGen.misc.ChanceIncHandler;
import nationGen.nation.Nation;
import nationGen.rostergeneration.UnitGen;
import nationGen.units.Unit;

public class MageNamer extends Namer {

  protected Random random;
  private NationGen ng;
  protected NationGenAssets assets;

  public MageNamer(NationGen ng, NationGenAssets assets) {
    this.ng = ng;
    this.assets = assets;
  }

  public List<NamePart> names = new ArrayList<>();
  public List<NamePart> adjectives = new ArrayList<>();
  public List<NamePart> nouns = new ArrayList<>();
  public List<NamePart> tieredpriest = new ArrayList<>();
  public List<NamePart> tieredmage = new ArrayList<>();
  public List<NamePart> rankedprefix = new ArrayList<>();
  public List<NamePart> extraparts_n = new ArrayList<>();
  public List<NamePart> extraparts_a = new ArrayList<>();

  protected void loadNameData() {
    n.races
      .get(0)
      .tags.getAllStrings("magenames")
      .forEach(set -> names.addAll(assets.magenames.get(set)));
    n.races
      .get(0)
      .tags.getAllStrings("mageadjectives")
      .forEach(set -> adjectives.addAll(assets.magenames.get(set)));
    n.races
      .get(0)
      .tags.getAllStrings("magenouns")
      .forEach(set -> nouns.addAll(assets.magenames.get(set)));
    n.races
      .get(0)
      .tags.getAllStrings("tieredpriestnames")
      .forEach(set -> tieredpriest.addAll(assets.magenames.get(set)));
    n.races
      .get(0)
      .tags.getAllStrings("tieredmagenames")
      .forEach(set -> tieredmage.addAll(assets.magenames.get(set)));
    n.races
      .get(0)
      .tags.getAllStrings("magerankedprefixes")
      .forEach(set -> rankedprefix.addAll(assets.magenames.get(set)));
    n.races
      .get(0)
      .tags.getAllStrings("extranamenouns")
      .forEach(set -> extraparts_n.addAll(assets.magenames.get(set)));
    n.races
      .get(0)
      .tags.getAllStrings("extranameadjectives")
      .forEach(set -> extraparts_a.addAll(assets.magenames.get(set)));

    if (names.size() == 0) names = assets.magenames.get("defaultnames");
    if (adjectives.size() == 0) adjectives = assets.magenames.get(
      "defaultadjectives"
    );
    if (nouns.size() == 0) nouns = assets.magenames.get("defaultnouns");
    if (tieredpriest.size() == 0) tieredpriest = assets.magenames.get(
      "defaulttieredpriestnames"
    );
    if (tieredmage.size() == 0) tieredmage = assets.magenames.get(
      "defaulttieredmagenames"
    );
    if (rankedprefix.size() == 0) rankedprefix = assets.magenames.get(
      "defaultprefixranks"
    );
    if (extraparts_n.size() == 0) extraparts_n = assets.magenames.get(
      "defaultextranamenouns"
    );
    if (extraparts_a.size() == 0) extraparts_a = assets.magenames.get(
      "defaultextranameadjectives"
    );
  }

  public void execute(Nation n) {
    this.n = n;
    random = new Random(n.random.nextInt());
    loadNameData();

    List<List<Unit>> all = n.getMagesInSeparateLists();
    execute(all, 3);

    UnitGen ug = new UnitGen(ng, n, assets);
    for (List<Unit> l : all) for (Unit u : l) {
      for (Unit mu : ug.getMontagUnits(u)) {
        mu.name = u.name.getCopy();
      }
    }
  }

  protected void execute(List<List<Unit>> all, int startTier) {
    List<Unit> primaries = all.get(0);
    List<Unit> secondaries = all.get(1);
    List<Unit> tertiaries = all.get(2);

    List<Unit> extras = null;
    if (all.size() > 3) extras = all.get(3);

    // prefixrank = <rank> <fixed name>
    // other = <rank based name>
    boolean prefixrank = random.nextBoolean();

    // whether extra special fun stuff should be suffix or prefix
    // ie apprentice storm druid or apprentice druid of astral graves
    // ie golden storm master or master of golden storm
    boolean suffix = random.nextBoolean();

    generateNewNames(primaries, startTier, prefixrank, suffix);

    if (secondaries.size() > 0) deriveNames(
      primaries,
      secondaries,
      startTier - 1,
      prefixrank,
      suffix
    );

    if (
      secondaries.size() > 0 &&
      secondaries.size() <= primaries.size() &&
      tertiaries.size() > 0
    ) deriveNames(secondaries, tertiaries, startTier - 2, prefixrank, suffix);
    else if (tertiaries.size() > 0) deriveNames(
      primaries,
      tertiaries,
      startTier - 2,
      prefixrank,
      suffix
    );

    boolean didstuff = false;
    if (secondaries.size() > 0) {
      if (
        secondaries.get(0).name.suffix == null &&
        secondaries.get(0).name.suffixprefix == null &&
        secondaries.get(0).name.prefix == null &&
        secondaries.get(0).name.prefixprefix == null
      ) for (Unit u : secondaries) u.name.setPrefix(
        n.name + n.nationalitysuffix
      );

      didstuff = true;
    }
    if (tertiaries.size() > 0) {
      if (
        (tertiaries.get(0).name.rankprefix == null || didstuff) &&
        tertiaries.get(0).name.suffix == null &&
        tertiaries.get(0).name.suffixprefix == null &&
        tertiaries.get(0).name.prefix == null &&
        tertiaries.get(0).name.prefixprefix == null
      ) for (Unit u : tertiaries) u.name.setPrefix(
        n.name + n.nationalitysuffix
      );
    }

    if (extras != null && extras.size() > 0) generateNewNames(
      extras,
      2,
      false,
      random.nextBoolean()
    );
  }

  public void deriveNames(
    List<Unit> from,
    List<Unit> to,
    int rank,
    boolean prefixrank,
    boolean suffix
  ) {
    List<NamePart> source = tieredmage;
    if (prefixrank) source = rankedprefix;

    ChanceIncHandler cha = new ChanceIncHandler(n);
    NamePart ff;
    boolean noname = false;

    do {
      EntityChances<NamePart> filters = cha.handleChanceIncs(
        to.get(0),
        this.filterByRank(source, rank)
      );
      if (filters.isEmpty()) {
        filters = cha.handleChanceIncs(to.get(0), source);
      }

      ff = filters.getRandom(random);
    } while (
      prefixrank
        ? (from.get(0).name.rankprefix != null &&
          ff.name.equals(from.get(0).name.rankprefix.name))
        : (from.get(0).name.type != null &&
          ff.name.equals(from.get(0).name.type.name))
    );

    boolean swapnoun = random.nextBoolean();
    double modify = random.nextDouble();

    List<String> used = new ArrayList<>();
    for (Unit u2 : from) used.add(u2.name.type.toString());
    for (int i = to.size() - 1; i >= 0; i--) {
      Unit u = to.get(i);

      if (to.size() == from.size()) {
        Unit u2 = from.get(i);

        u.name = u2.name.getCopy();
      } else if (to.size() < from.size()) {
        Name same = getSameParts(from);
        if (same.suffix == null && same.suffixprefix != null) {
          same.prefix = same.suffixprefix;
          same.suffixprefix = null;
        } else if (
          same.suffix != null &&
          same.suffixprefix == null &&
          from.get(0).name.suffixprefix != null
        ) {
          same.pluralsuffix = true;
        }

        u.name = same;
      } else { // to.size() > from.size()
        source = new ArrayList<>();
        //source.addAll(filterByRank(tieredmage, rank));
        source.addAll(names);

        Name same = getSameParts(from);
        if (same.suffix == null && same.suffixprefix != null) {
          same.prefix = same.suffixprefix;
          same.suffixprefix = null;
        } else if (
          modify < 0.5
        ) { // extra part, experimental
          if (same.suffixprefix == null && same.suffix != null) {
            same.prefix = same.suffix;
            same.suffix = null;
          } else if (same.suffixprefix != null && same.suffix != null) {
            if (!swapnoun) same.prefix = same.suffixprefix;
            else same.prefix = same.suffix;

            same.suffixprefix = null;
            same.suffix = null;
          }
        }

        List<NamePart> available = new ArrayList<>(source);

        NamePart ff2;
        do {
          EntityChances<NamePart> filters = cha.handleChanceIncs(
            u,
            this.filterByRank(available, rank)
          );
          if (filters.isEmpty()) {
            filters = cha.handleChanceIncs(u, available);
          }
          ff2 = filters.getRandom(random);
        } while (used.contains(ff2.toString()));

        used.add(ff2.toString());

        u.name = same;
        u.name.rankprefix = null;
        u.name.type = ff2.getCopy();
      }

      if (u.name.type == null) {
        List<NamePart> available = new ArrayList<>();

        if (!prefixrank) available.addAll(tieredmage);
        else available.addAll(names);

        NamePart ff2;
        do {
          EntityChances<NamePart> filters = cha.handleChanceIncs(
            u,
            this.filterByRank(available, rank)
          );
          if (filters.isEmpty()) {
            filters = cha.handleChanceIncs(u, available);
          }

          ff2 = filters.getRandom(random);
        } while (used.contains(ff2.toString()));
        used.add(ff2.toString());
        u.name.type = ff2.getCopy();
        noname = true;
      }

      if (to.size() <= from.size()) {
        if (prefixrank && !noname) u.name.rankprefix = ff.getCopy();
        else if (!prefixrank) u.name.type = ff.getCopy();
      }
    }
  }

  private Name getSameParts(List<Unit> units) {
    Name name = units.get(0).name.getCopy();
    for (Unit u : units) {
      if (
        u.name.rankprefix != null && !u.name.rankprefix.equals(name.rankprefix)
      ) name.rankprefix = null;
      if (
        u.name.prefix != null && !u.name.prefix.equals(name.prefix)
      ) name.prefix = null;
      if (
        u.name.prefixprefix != null &&
        !u.name.prefixprefix.equals(name.prefixprefix)
      ) name.prefixprefix = null;
      if (
        u.name.suffixprefix != null &&
        !u.name.suffixprefix.equals(name.suffixprefix)
      ) name.suffixprefix = null;
      if (
        u.name.suffix != null && !u.name.suffix.equals(name.suffix)
      ) name.suffix = null;
      if (u.name.suffix != null && !u.name.type.equals(name.type)) name.type =
        null;
    }

    return name;
  }

  public void generateNewNames(
    List<Unit> primaries,
    int rank,
    boolean prefixrank,
    boolean suffix
  ) {
    ChanceIncHandler cha = new ChanceIncHandler(n);

    //boolean twopartsuffixdefinite = false;

    // Prefix or suffix crap
    boolean noun = random.nextBoolean();

    // Should it be a suffix if it's a noun?
    boolean shouldbesuffix = random.nextBoolean();

    // Get common paths
    List<MagicPath> commons = new ArrayList<>();

    for (int j = 5; j > 0; j--) {
      for (MagicPath path : MagicPath.values()) {
        boolean has = true;
        for (Unit u : primaries) {
          if (u.getMagicPicks().get(path) < j || commons.contains(path)) has =
            false;
        }
        if (has) commons.add(path);
      }

      if (commons.size() >= 2) break;
    }

    // Vary name instead of some other parT?
    boolean varyname = false;
    if (prefixrank && primaries.size() > 1 && commons.size() > 0) varyname =
      random.nextDouble() > 0.85;

    // Base name "mage" "necromancer" etc
    List<NamePart> source;
    if (prefixrank && varyname) {
      if (noun) source = nouns;
      else source = adjectives;
    } else if (prefixrank) source = names;
    else source = filterByRank(tieredmage, rank);

    boolean strict = random.nextBoolean();

    EntityChances<NamePart> filters = cha.handleChanceIncs(
      primaries.get(0),
      source
    );

    if (filters.isEmpty()) {
      filters = cha.handleChanceIncs(primaries.get(0), source);
    }
    NamePart ff = filters.getRandom(random);

    for (Unit u : primaries) {
      if (!varyname) u.name.type = ff.getCopy();
      else {
        if (ff.tags.containsName("nosuffix")) shouldbesuffix = false;
        else if (ff.tags.containsName("noprefix")) shouldbesuffix = true;

        if (
          !noun || !shouldbesuffix
        ) { // Is noun or should be prefix -> Nouns for prefix and all kinds of adjectives
          u.name.prefixprefix = ff.getCopy();
        } else {
          u.name.suffix = ff.getCopy();
        }
      }
    }

    // If rank is a prefix, set it
    if (prefixrank) {
      filters = cha.handleChanceIncs(
        primaries.get(0),
        filterByRank(rankedprefix, rank)
      );
      if (filters.isEmpty()) filters = cha.handleChanceIncs(
        primaries.get(0),
        rankedprefix
      );

      NamePart prefixf = filters.getRandom(random);
      for (Unit u : primaries) u.name.rankprefix = prefixf.getCopy();
    }

    // Break now for single path mages (= priests usually) if the basic name contains information already
    if (
      primaries.size() == 1 &&
      commons.size() == 1 &&
      commons.contains(MagicPath.HOLY)
    ) {
      return;
    }

    // Get an extra name part. Not for prefixrank mages to avoid super long names.
    NamePart extra = null;
    if ((commons.size() > 1) && !prefixrank) {
      List<NamePart> source2 = new ArrayList<>();
      if (noun) {
        source2.addAll(adjectives);
        source2.addAll(extraparts_a);
      } else {
        source2.addAll(nouns);
        source2.addAll(extraparts_n);
      }

      filters = cha.handleChanceIncs(primaries.get(0), source2);
      if (filters.isEmpty()) filters = cha.handleChanceIncs(
        primaries.get(0),
        source2
      );
      extra = filters.getRandom(random);
    }

    // Set rest of the name
    for (Unit u : primaries) {
      List<MagicPath> d = MageGenerator.findDistinguishingPaths(u, primaries);

      if (d.size() == 0) d = commons;

      if (varyname) {
        filters = cha.handleChanceIncs(u, names);

        if (filters.isEmpty()) filters = cha.handleChanceIncs(
          primaries.get(0),
          names
        );
      } else if (noun) {
        filters = cha.handleChanceIncs(u, nouns);
        if (filters.isEmpty()) filters = cha.handleChanceIncs(
          primaries.get(0),
          nouns
        );
      } else {
        filters = cha.handleChanceIncs(u, adjectives);
        if (filters.isEmpty()) filters = cha.handleChanceIncs(
          primaries.get(0),
          adjectives
        );
      }

      NamePart suffixpart = filters.getRandom(random);

      if (suffixpart.tags.containsName("nosuffix")) shouldbesuffix = false;
      else if (suffixpart.tags.containsName("noprefix")) shouldbesuffix = true;

      if (varyname) {
        u.name.type = suffixpart.getCopy();
      } else if (
        !noun || !shouldbesuffix
      ) { // Is noun or should be prefix -> Nouns for prefix and all kinds of adjectives
        u.name.prefixprefix = suffixpart.getCopy();
      } else {
        u.name.suffix = suffixpart.getCopy();
      }

      if (!prefixrank && extra != null && shouldbesuffix) {
        if (noun) {
          u.name.suffixprefix = extra.getCopy();
        } else {
          suffixpart = extra;
          u.name.suffix = extra.getCopy();
          u.name.suffixprefix = u.name.prefixprefix;
          u.name.prefixprefix = null;
        }
      }

      if (
        u.name.suffix != null &&
        suffixpart != null &&
        suffixpart.name.equals(u.name.suffix.name)
      ) {
        boolean def = suffixpart.tags.containsName("definitesuffix");
        boolean plur = suffixpart.tags.containsName("pluralsuffix");

        if (def) u.name.definitesuffix = true;
        if (plur) u.name.pluralsuffix = true;
      }
      if (
        u.name.suffix != null &&
        extra != null &&
        extra.name.equals(u.name.suffix.name)
      ) {
        boolean def = extra.tags.containsName("definitesuffix");
        boolean plur = extra.tags.containsName("pluralsuffix");

        if (def) u.name.definitesuffix = true;
        if (plur) u.name.pluralsuffix = true;
      }
      if (
        u.name.suffix != null &&
        ff != null &&
        ff.name.equals(u.name.suffix.name)
      ) {
        boolean def = ff.tags.containsName("definitesuffix");
        boolean plur = ff.tags.containsName("pluralsuffix");

        if (def) u.name.definitesuffix = true;
        if (plur) u.name.pluralsuffix = true;
      }
    }
  }

  protected List<NamePart> filterByRank(List<NamePart> orig, int rank) {
    List<NamePart> list = new ArrayList<NamePart>();
    for (NamePart f : orig) {
      if (f.rank == rank) list.add(f);
    }
    return list;
  }
  // This method is commented because it's no longer possible to inspect a chanceinc like it did previously, but it's
  // possible that it wasn't really working properly previously anyways due to increased complexity of chanceincs.
  // If a name should only be available for a certain path, the chanceincs themselves should be handling it with *0
  // modifications and such, so there should be no need to do additional filtering.
  //	protected List<NamePart> filterByPaths(List<NamePart> orig, List<MagicPath> paths, boolean strict)
  //	{
  //		List<NamePart> list = new ArrayList<>();
  //		for(NamePart f : orig)
  //		{
  //			boolean ok = strict;
  //			boolean hadChanceInc = false;
  //			for(String c : f.chanceincs)
  //			{
  //
  //				List<String> args = Generic.parseArgs(c);
  //				if(args.get(0).equals("personalmagic"))
  //				{
  //					hadChanceInc = true;
  //					for(MagicPath path : MagicPath.values())
  //					{
  //						if(args.contains(path.name))
  //						{
  //							if(!paths.contains(path) && strict)
  //							{
  //								ok = false;
  //							}
  //							else if(paths.contains(path) && !strict)
  //							{
  //								ok = true;
  //							}
  //						}
  //					}
  //				}
  //
  //			}
  //			if(!hadChanceInc && (paths.size() == 0 || !strict))
  //				ok = true;
  //			else if(f.tags.containsName("alwaysok"))
  //				ok = true;
  //			else if(strict && !hadChanceInc)
  //				ok = false;
  //
  //			if(ok)
  //			{
  //				list.add(f);
  //
  //			}
  //
  //		}
  //
  //		return list;
  //	}

}
