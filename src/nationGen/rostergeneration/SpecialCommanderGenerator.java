package nationGen.rostergeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.entities.Filter;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.misc.Args;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.naming.NamePart;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class SpecialCommanderGenerator {

  Nation n;
  NationGen ng;
  ChanceIncHandler chandler;
  Random r;
  private NationGenAssets assets;

  public SpecialCommanderGenerator(
    Nation n,
    NationGen ng,
    NationGenAssets assets
  ) {
    this.n = n;
    this.ng = ng;
    this.assets = assets;
    this.chandler = new ChanceIncHandler(n);
    this.r = new Random(n.random.nextInt());
  }

  public void generate() {
    Race race = n.races.get(0);

    // Chance to use secondary race is third of the percentage of secondary race units in nation so far
    if (n.percentageOfRace(n.races.get(1)) / 3 > n.random.nextDouble()) {
      race = n.races.get(1);
    }

    Unit u = null;

    List<Filter> possibles = ChanceIncHandler.retrieveFilters(
      "specialcommanderfilters",
      "default_specialcommanderfilters",
      assets.filters,
      null,
      race
    );
    while (u == null) {
      if (chandler.handleChanceIncs(possibles).countPossible() == 0) return;

      Filter f = chandler.getRandom(possibles);
      possibles.remove(f);

      List<Pose> possiblePoses = getPossiblePoses(f, race);
      if (chandler.handleChanceIncs(race, possiblePoses).countPossible() > 0) {
        // get pose
        Pose p = chandler.getRandom(possiblePoses, race);

        // generate unit
        UnitGen ug = new UnitGen(ng, n, assets);
        u = ug.generateUnit(race, p);
        u.appliedFilters.add(f);

        boolean mage = p.roles.contains("mage") || p.roles.contains("priest");
        Command targettag = f.tags
          .getCommand("equipmenttargettag")
          .orElse(null);

        ug.armorUnit(u, null, null, targettag, mage);
        ug.armUnit(u, null, null, targettag, mage);

        // change color
        double d = r.nextDouble();
        if (d < 0.3) u.color = n.colors[2];
        else if (d < 0.4) u.color = n.colors[3];

        // get name
        List<String> names = f.tags.getAllStrings("unitname");
        String name = names.get(r.nextInt(names.size()));
        u.name.type = NamePart.newNamePart(name, ng);

        // leadership
        boolean hasleader = false;
        for (Command c : u.getCommands()) if (
          c.command.equals("#noleader") ||
          c.command.equals("#poorleader") ||
          c.command.equals("#okleader") ||
          c.command.equals("#goodleader") ||
          c.command.equals("#expertleader") ||
          c.command.equals("#superiorleader")
        ) hasleader = true;

        if (!hasleader) {
          if (r.nextBoolean()) u.commands.add(new Command("#noleader"));
          else u.commands.add(new Command("#poorleader"));
        }

        // cap only
        double chance = f.tags.getDouble("caponlychance").orElse(1D);

        if (r.nextDouble() < chance) u.caponly = true;

        // put to lists
        if (f.tags.containsName("troop")) {
          n.unitlists.computeIfAbsent("special", k -> new ArrayList<>()).add(u);
        } else {
          u.commands.add(Command.args("#gcost", "+30"));
          n.comlists
            .computeIfAbsent("specialcoms", k -> new ArrayList<>())
            .add(u);
        }
      }
    }
    chandler = null;
  }

  private boolean fillsRequirements(Pose p, Filter f) {
    Optional<Command> reqTag = f.tags.getCommand("requiredposetag");
    Optional<String> reqTheme = f.tags.getString("requiredposetheme");

    return (
      (reqTag.isEmpty() || p.tags.containsTag(reqTag.get())) &&
      (reqTheme.isEmpty() || p.themes.contains(reqTheme.get()))
    );
  }

  private List<Pose> getPossiblePoses(Filter f, Race race) {
    List<Pose> poses = new ArrayList<>();

    for (Args args : f.tags.getAllArgs("pose")) {
      String goal = args.get(1).get();
      if (args.get(0).get().equals("role")) {
        for (Pose p : race.poses) {
          if (
            p.roles.contains(goal) &&
            !poses.contains(p) &&
            fillsRequirements(p, f)
          ) poses.add(p);
        }
      } else if (args.get(0).get().equals("name")) {
        for (Pose p : race.poses) {
          if (
            p.name.equals(goal) && !poses.contains(p) && fillsRequirements(p, f)
          ) poses.add(p);
        }
      }
    }

    return poses;
  }
}
