package nationGen.rostergeneration;

import com.elmokki.Generic;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.chances.EntityChances;
import nationGen.entities.Filter;
import nationGen.items.Item;
import nationGen.magic.MagicPath;
import nationGen.misc.Arg;
import nationGen.misc.Command;
import nationGen.misc.ItemSet;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class CommanderGenerator extends TroopGenerator {

  private Random r;

  public CommanderGenerator(NationGen g, Nation n, NationGenAssets assets) {
    super(g, n, assets, "commandergen");
    r = new Random(n.random.nextInt());
  }

  public int strength = 2;

  private List<Unit> getUnitsWithTag(List<Unit> list, String tag) {
    return list
      .stream()
      .filter(u -> Generic.getAllUnitTags(u).containsName(tag))
      .collect(Collectors.toList());
  }

  private void removeMontagUnits(List<Unit> possibleComs) {
    List<Unit> tempComs = possibleComs
      .stream()
      .filter(u -> u.tags.containsName("montagunit"))
      .collect(Collectors.toList());

    possibleComs.removeAll(tempComs);
    tempComs.clear();
  }

  private List<Unit> getListOfSuitableUnits(String role) {
    List<Unit> possibleComs = new ArrayList<>();

    nation.selectTroops(role).forEach(possibleComs::add);
    nation
      .selectTroops("montagtroops")
      .filter(u -> u.guessRole().equals(role))
      .forEach(possibleComs::add);

    nation.selectTroops("montagsacreds").forEach(possibleComs::add);

    return possibleComs;
  }

  public void generateComs() {
    List<Unit> tempComs = new ArrayList<>();
    List<Unit> possibleComs = new ArrayList<>();
    nation.selectTroops("infantry").forEach(possibleComs::add);
    nation.selectTroops("mounted").forEach(possibleComs::add);
    nation.selectTroops("chariot").forEach(possibleComs::add);
    nation.selectTroops("sacred").forEach(possibleComs::add);

    nation
      .selectTroops("montagtroops")
      .filter(
        u ->
          u.guessRole().equals("infantry") ||
          u.guessRole().equals("chariot") ||
          u.guessRole().equals("mounted")
      )
      .forEach(possibleComs::add);

    nation.selectTroops("montagsacreds").forEach(possibleComs::add);

    List<String> features = new ArrayList<>();
    List<String> allFeatures = new ArrayList<>();
    allFeatures.add("#flying");
    allFeatures.add("#stealthy");
    allFeatures.add("#amphibian");
    allFeatures.add("#pooramphibian");
    allFeatures.add("#mounted");
    allFeatures.add("#mountainsurvival");
    allFeatures.add("#wastesurvival");
    allFeatures.add("#forestsurvival");
    allFeatures.add("#swampsurvival");
    allFeatures.add("#demon");
    allFeatures.add("#undead");
    allFeatures.add("#sailing");
    allFeatures.add("#magicbeing");

    int orig = 1;
    boolean hasSlaves = false;
    boolean hasAnimals = false;

    if (r.nextBoolean()) {
      orig++;
      allFeatures.add("#holy");
    }

    // Check to see if #secondaryracecommand is going to add #magicbeing, etc. to secondary race troops when they're finalized

    //List<Command> all = new ArrayList<Command>();
    boolean secondaryMagicBeings = false;
    boolean secondaryDemons = false;
    boolean secondaryUndead = false;
    boolean secondarySlaves = false;
    boolean secondaryAnimals = false;

    List<Command> commands = nation.races
      .get(0)
      .tags.getAllCommands("secondaryracecommand");
    commands.addAll(
      nation.races
        .get(0)
        .tags.getAllCommands("secondaryracecommand_conditional")
    );

    for (Command c : commands) {
      secondaryMagicBeings = (c.command.equals("#magicbeing") ||
        secondaryMagicBeings);
      secondaryDemons = (c.command.equals("#demon") || secondaryDemons);
      secondaryUndead = (c.command.equals("#undead") ||
        (c.command.equals("#almostundead") || secondaryUndead));
      secondarySlaves = (c.command.equals("#slave") || secondarySlaves);
      secondaryAnimals = (c.command.equals("#animal") || secondaryAnimals);
    }

    // Get all commands that at least one commander has to fulfill.

    int magicbeings = 0;
    int demons = 0;
    int undeadbeings = 0;
    for (Unit u : possibleComs) {
      if (u.race.equals(nation.races.get(1)) && secondaryMagicBeings) {
        magicbeings++;
        if (!features.contains("#magicbeing")) {
          features.add("#magicbeing");
        }
      }
      if (u.race.equals(nation.races.get(1)) && secondaryDemons) {
        demons++;
        if (!features.contains("#demon")) {
          features.add("#demon");
        }
      }
      if ((u.race.equals(nation.races.get(1)) && secondaryUndead)) {
        undeadbeings++;
        if (!features.contains("#undead")) {
          features.add("#undead");
        }
      }

      if (u.race.equals(nation.races.get(1)) && secondarySlaves) {
        hasSlaves = true;
      }
      if (u.race.equals(nation.races.get(1)) && secondaryAnimals) {
        hasAnimals = true;
      }

      for (Command c : u.getCommands()) {
        if (
          c.command.equals("#magicbeing") &&
          !(u.race.equals(nation.races.get(1)) || secondaryMagicBeings)
        ) magicbeings++; // Make sure we don't double-count
        if (
          c.command.equals("#demon") &&
          !(u.race.equals(nation.races.get(1)) || secondaryDemons)
        ) demons++; // Make sure we don't double-count
        if (
          (c.command.equals("#undead") || c.command.equals("#almostundead")) &&
          !(u.race.equals(nation.races.get(1)) || secondaryUndead)
        ) undeadbeings++; // Make sure we don't double-count

        if (
          c.command.equals("#slave") ||
          (u.race.equals(nation.races.get(1)) && secondarySlaves)
        ) hasSlaves = true;
        if (
          c.command.equals("#animal") ||
          (u.race.equals(nation.races.get(1)) && secondaryAnimals)
        ) hasAnimals = true;

        if (allFeatures.contains(c.command) && !features.contains(c.command)) {
          features.add(c.command);
        }
      }
    }

    double magicshare = (double) magicbeings / (double) possibleComs.size();
    double demonshare = (double) (demons) / (double) possibleComs.size();
    double undeadshare = (double) (undeadbeings) / (double) possibleComs.size();
    double demonuneadshare =
      (double) (demons + undeadbeings) / (double) possibleComs.size();

    // Clear the list and add only infantry for first pass!
    possibleComs.clear();
    possibleComs.addAll(getListOfSuitableUnits("infantry"));
    removeMontagUnits(possibleComs);

    List<Unit> cantbes =
      this.getUnitsWithTag(possibleComs, "cannot_be_commander");
    if (cantbes.size() < possibleComs.size()) possibleComs.removeAll(cantbes);

    List<Unit> shouldbes =
      this.getUnitsWithTag(possibleComs, "should_be_commander");
    if (shouldbes.size() > 0) possibleComs = shouldbes;

    // Add one random infantry com
    if (
      getListOfSuitableUnits("infantry").size() > 0 && possibleComs.size() > 0
    ) {
      Unit com = possibleComs.get(r.nextInt(possibleComs.size()));
      tempComs.add(com);
      possibleComs.remove(com);

      if (com != null) {
        for (Command c : com.getCommands()) {
          if (allFeatures.contains(c.command)) {
            features.remove(c.command);
          }
        }
      }
    } else orig++;

    possibleComs.addAll(getListOfSuitableUnits("chariot"));
    possibleComs.addAll(getListOfSuitableUnits("mounted"));
    possibleComs.addAll(getListOfSuitableUnits("sacred"));
    removeMontagUnits(possibleComs);

    int minimumcoms = orig;

    possibleComs.removeAll(tempComs);
    minimumcoms = Math.min(minimumcoms, possibleComs.size());

    cantbes = this.getUnitsWithTag(possibleComs, "cannot_be_commander");
    if (
      possibleComs.size() - cantbes.size() >= minimumcoms
    ) possibleComs.removeAll(cantbes);

    int tries = 0;
    while ((minimumcoms > 0 || features.size() > 0) && tries < 200) {
      tries++;

      List<Unit> temps = new ArrayList<>();
      shouldbes = this.getUnitsWithTag(possibleComs, "should_be_commander");
      if (shouldbes.size() >= 1) temps.addAll(shouldbes);
      else temps.addAll(possibleComs);

      temps.removeAll(tempComs);

      Unit u = null;

      if (features.size() > 0) {
        do {
          u = getUnitWith(features, allFeatures, temps);

          if (u == null) {
            u = getUnitWith(features, allFeatures, possibleComs);

            if (u == null) {
              features.clear();
              break;
            }
          }

          for (Command c : u.getCommands()) {
            if (allFeatures.contains(c.command)) {
              features.remove(c.command);
            }
          }
        } while (tempComs.contains(u));
      } else {
        possibleComs.removeAll(getListOfSuitableUnits("sacred"));
        if (possibleComs.size() > 0) do {
          u = possibleComs.get(r.nextInt(possibleComs.size()));
        } while (
          u == null ||
          tempComs.contains(u) ||
          (shouldReRoll(u, tempComs) && r.nextBoolean())
        );
      }

      if (u != null) {
        minimumcoms--;
        tempComs.add(u);
      }
    }

    List<Unit> coms = new ArrayList<>();
    for (Unit u : tempComs) {
      //NamePart part = new NamePart();
      //part.text = "Commander";

      Unit unit = u.getCopy();

      // Remove montags
      List<Command> toremove = new ArrayList<>();
      for (Command c : unit.commands) {
        if (c.command.equals("#montag")) toremove.add(c);
      }
      unit.commands.removeAll(toremove);

      // Elitefy
      process(unit);

      //unit.name.type = part;
      //unit.commands.add(new Command("#gcost", "+20"));
      coms.add(unit);

      // Add stuff to sacreds sometimes
      boolean priest = false;
      boolean stealthy = false;
      for (Command c : unit.getCommands()) {
        if (c.command.equals("#holy")) priest = true;
        if (c.command.equals("#stealthy")) stealthy = true;
      }

      if (r.nextBoolean() && priest) unit.commands.add(
        Command.parse("#magicskill 9 1")
      );

      if (priest && stealthy) {
        double rand = r.nextDouble();
        if (rand < 0.03) {
          unit.commands.add(new Command("#assassin"));
        }
      }
    }

    tempComs.clear();
    for (int i = coms.size() - 1; i >= 0; i--) tempComs.add(coms.get(i));

    tempComs = orderComs(tempComs);

    // Leadership!

    for (int i = 0; i < tempComs.size(); i++) {
      Unit com = tempComs.get(i);

      boolean magicbeing = false;
      boolean demon = false;
      boolean undead = false;
      boolean slave = false;
      boolean animal = false;
      boolean undisciplined = false;
      boolean mindless = false;
      boolean good_leader = com.tags.containsName("#good_leader");
      boolean superior_leader = com.tags.containsName("#superior_leader");

      for (Command c : com.getCommands()) {
        if (c.command.equals("#magicbeing")) magicbeing = true;
        else if (c.command.equals("#demon")) demon = true;
        else if (
          c.command.equals("#undead") || c.command.equals("#almostundead")
        ) undead = true;
        else if (c.command.equals("#slave")) slave = true;
        else if (c.command.equals("#animal")) animal = true;
        else if (c.command.equals("#undisciplined")) undisciplined = true;
        else if (
          c.command.equals("#mor") && c.args.get(0).getInt() == 50
        ) mindless = true;
      }

      // Mindless magical beings can be inspiring, but others cannot
      if (magicbeing && mindless) {
        mindless = false;
      }

      int power;
      // Determine eventual base leadership level
      double random = r.nextDouble();
      if (
        (random > 1 - 0.125 * i) || (superior_leader && r.nextDouble() < 0.5)
      ) power = 3;
      else if (
        (random > 0.25 - 0.05 * i) ||
        (superior_leader && r.nextDouble() < 0.75) ||
        (good_leader && r.nextDouble() < 0.5)
      ) power = 2;
      else power = 1;

      // Undisciplined troops are more likely to make for less talented commanders
      if (power > 1 && undisciplined && r.nextDouble() < 0.5) power -= 1;

      // Unless we see a reason not to, rec points will be 1
      com.commands.add(Command.parse("#rpcost 1"));

      // Are we or most of our troops "special" beings, and if so will we get better special leadership and/or worse normal?
      int magicpower = power - (r.nextInt(1));
      int undeadpower = power - (r.nextInt(1));
      int demonpower = undeadpower;
      int powerpenalty = 0;
      if (magicshare > 0.66 || (undead && (undeadshare > 0.33))) {
        if (r.nextDouble() > 0.5) {
          undeadpower = power++;
          if (undead) powerpenalty = r.nextInt(3);
          else powerpenalty = Math.min(0, (r.nextInt(3) - 1));
        }
      }
      if (magicshare > 0.8 || (magicbeing && (magicshare > 0.4))) {
        if (r.nextDouble() > 0.5) {
          magicpower = power++;
          if (powerpenalty == 0 && magicbeing) powerpenalty = Math.min(
            0,
            r.nextInt(4) - 1
          );
          else if (powerpenalty == 0) powerpenalty = Math.min(
            0,
            (r.nextInt(3) - 1)
          );
        }
      }
      if (demonshare > 0.8 || (demon && (demonshare > 0.4))) {
        if (r.nextDouble() > 0.5) {
          demonpower = power++;
          if (powerpenalty == 0 && demon) powerpenalty = Math.min(
            0,
            r.nextInt(5) - 3
          );
          else if (powerpenalty == 0) powerpenalty = Math.min(
            0,
            (r.nextInt(3) - 1)
          );
        }
      }

      if ((i == 0 || r.nextDouble() < 0.2) && hasSlaves) {
        com.commands.add(Command.args("#taskmaster", "+1"));
        com.commands.add(Command.args("#gcost", "+5"));
      }

      if (com.caponly && r.nextDouble() > 0.5) com.caponly = false;

      com.commands.add(Command.args("#gcost", "+30"));

      // Expertleader
      if (power == 3) {
        com.commands.add(Command.args("#gcost", "+50"));
        com.commands.add(Command.args("#expertleader"));

        if (powerpenalty == 2) {
          com.commands.add(Command.args("#command", "-80"));
          com.commands.add(Command.args("#gcost", "-40"));
        } else if (powerpenalty == 1) {
          com.commands.add(Command.args("#command", "-40"));
          com.commands.add(Command.args("#gcost", "-20"));
        } else com.commands.add(Command.args("#rpcost", "2"));

        if (r.nextDouble() > 0.5 && !mindless) {
          com.commands.add(Command.args("#inspirational", "+1"));
          com.commands.add(Command.args("#gcost", "+10"));
        }

        if (r.nextDouble() > 0.75 && !mindless) {
          com.commands.add(Command.args("#inspirational", "+1"));
          com.commands.add(Command.args("#gcost", "+10"));
        }

        if ((r.nextDouble() < 0.25 && hasSlaves) || (slave && !mindless)) {
          if (slave && !mindless) {
            int amount = Math.min(0, (r.nextInt(4) - 1));
            if (amount > 0) {
              com.commands.add(Command.args("#taskmaster", "+" + amount));
              com.commands.add(Command.args("#gcost", "+" + (amount * 5)));

              amount = r.nextInt(amount + 1);
              if (amount > 0) {
                com.commands.add(Command.args("#inspirational", "-" + amount));
                com.commands.add(Command.args("#gcost", "-" + (amount * 10)));
              }
            }
          } else {
            int amount = r.nextInt(2);
            com.commands.add(Command.args("#taskmaster", "+" + (amount + 1)));
            com.commands.add(Command.args("#gcost", "+" + (amount * 5)));
          }
        }

        if ((r.nextDouble() < 0.25 && hasAnimals) || (animal && !mindless)) {
          if (animal && !mindless) {
            int amount = Math.min(0, (r.nextInt(6) - 2));
            if (amount > 0) {
              com.commands.add(Command.args("#beastmaster", "+" + amount));
              com.commands.add(Command.args("#gcost", "+" + (amount * 5)));

              amount = r.nextInt(amount + 1);

              if (amount > 0) {
                com.commands.add(Command.args("#inspirational", "-" + amount));
                com.commands.add(Command.args("#gcost", "-" + (amount * 10)));
              }
            }
          } else {
            int amount = r.nextInt(2) + 1;
            com.commands.add(Command.args("#beastmaster", "+" + amount));
            com.commands.add(Command.args("#gcost", "+" + (amount * 5)));
          }
        }
      }
      // Goodleader
      else if (power == 2) {
        if (powerpenalty == 2) {
          com.commands.add(Command.args("#goodleader"));
          com.commands.add(Command.args("#command", "-70"));
        } else if (powerpenalty == 1) {
          com.commands.add(Command.args("#goodleader"));
          com.commands.add(Command.args("#command", "-40"));
          com.commands.add(Command.args("#gcost", "+10"));
        } else if ((animal || mindless || slave) && r.nextDouble() > 0.25) {
          com.commands.add(Command.args("#okleader"));
          com.commands.add(Command.args("#command", "+40"));
          com.commands.add(Command.args("#gcost", "+10"));
        } else {
          com.commands.add(Command.args("#goodleader"));
          com.commands.add(Command.args("#gcost", "+20"));
        }

        if (r.nextDouble() > 0.65 && !mindless) {
          com.commands.add(Command.args("#inspirational", "+1"));
          com.commands.add(Command.args("#gcost", "+10"));
        }
        if (r.nextDouble() > 0.85) {
          com.commands.add(Command.args("#inspirational", "+1"));
          com.commands.add(Command.args("#gcost", "+10"));
        }

        if (r.nextDouble() < 0.5 && hasSlaves) {
          int amount = r.nextInt(2) + 1;
          com.commands.add(Command.args("#taskmaster", "+" + amount));
          com.commands.add(Command.args("#gcost", "+" + (amount * 5)));

          amount = r.nextInt(amount + 1);
          if (amount > 0) {
            com.commands.add(Command.args("#inspirational", "-" + amount));
            com.commands.add(Command.args("#gcost", "-" + (amount * 10)));
          }
        }

        if ((r.nextDouble() < 0.125 && hasAnimals) || (animal && !mindless)) {
          if (animal && !mindless) {
            int amount = Math.min(0, (r.nextInt(4) - 1));
            if (amount > 0) {
              com.commands.add(Command.args("#beastmaster", "+" + amount));
              com.commands.add(Command.args("#gcost", "+" + (amount * 5)));

              amount = r.nextInt(amount + 1);

              if (amount > 0) {
                com.commands.add(Command.args("#inspirational", "-" + amount));
                com.commands.add(Command.args("#gcost", "-" + (amount * 10)));
              }
            }
          } else {
            int amount = r.nextInt(1) + 1;
            com.commands.add(Command.args("#beastmaster", "+" + amount));
            com.commands.add(Command.args("#gcost", "+" + (amount * 5)));
          }
        }
      }
      // Okleader
      else { // if(power == 1)
        com.commands.add(new Command("#okleader"));

        if (powerpenalty > 0) {
          com.commands.add(Command.args("#command", "-30"));
          com.commands.add(Command.args("#gcost", "-10"));
        }

        if (r.nextDouble() > 0.75 && !mindless) {
          com.commands.add(Command.args("#inspirational", "+1"));
          com.commands.add(Command.args("#gcost", "+10"));
        }
        if (r.nextDouble() > 0.5) {
          com.commands.add(Command.args("#command", "+20"));
          if (r.nextDouble() > 0.875) {
            com.commands.add(Command.args("#command", "+20"));
            com.commands.add(Command.args("#gcost", "+10"));
          }
        }

        if (r.nextDouble() < 0.25 && hasSlaves) {
          int amount = r.nextInt(2) + 1;
          com.commands.add(Command.args("#taskmaster", "+" + amount));
          com.commands.add(Command.args("#gcost", "+" + (amount * 5)));

          amount = r.nextInt(amount + 1);
          if (amount > 0) {
            com.commands.add(Command.args("#inspirational", "-" + amount));
            com.commands.add(Command.args("#gcost", "-" + (amount * 10)));
          }
        }

        if ((r.nextDouble() < 0.125 && (hasAnimals || (animal && !mindless)))) {
          int amount = Math.min(0, (r.nextInt(5) - 2));
          if (amount > 0) {
            com.commands.add(Command.args("#beastmaster", "+" + amount));
            com.commands.add(Command.args("#gcost", "+" + (amount * 5)));

            amount = r.nextInt(amount + 1);
            if (amount > 0) {
              com.commands.add(Command.args("#inspirational", "-" + amount));
              com.commands.add(Command.args("#gcost", "-" + (amount * 10)));
            }
          }
        }
      }

      // Magic leadership
      assignMagicUDLeadership(com, magicpower, magicbeing, magicshare, "magic");
      assignMagicUDLeadership(
        com,
        Math.max(demonpower, undeadpower),
        (demon || undead),
        demonuneadshare,
        "undead"
      );

      generateDescription(com, magicbeing, (demon || undead), hasSlaves); // Kludge to provide basic madlib commander descriptions
    }

    nation.comlists
      .computeIfAbsent("commanders", k -> new ArrayList<>())
      .addAll(tempComs);

    chandler = null;
    unitGen = null;
  }

  private void assignMagicUDLeadership(
    Unit com,
    int power,
    boolean being,
    double magicshare,
    String str
  ) {
    int origpower = power;

    if (magicshare <= 0.65) power -= 1;
    else if (magicshare <= 0.35) {
      if (power > 1) power = 1;
      else power = 0;
    } else if (magicshare <= 0.25) power = 0;

    if (power < 2 && being) power = Math.min(Math.max(2, power + 1), 4);
    if (power < origpower && being && magicshare > 0.4) power = origpower;

    if (power >= 4) com.commands.add(new Command("#expert" + str + "leader"));
    if (power >= 3) com.commands.add(new Command("#good" + str + "leader"));
    else if (power >= 2) com.commands.add(new Command("#ok" + str + "leader"));
    else if (power >= 1) com.commands.add(
      new Command("#poor" + str + "leader")
    );
  }

  private List<Unit> orderComs(List<Unit> coms) {
    List<Unit> templist = new ArrayList<>();

    for (Unit u : coms) {
      boolean holy = false;
      boolean mounted = false;
      for (Command c : u.getCommands()) {
        if (c.command.equals("#holy")) holy = true;
        if (c.command.equals("#mounted")) mounted = true;
      }
      if (!holy && !mounted) templist.add(u);
    }
    List<Unit> newlist = new ArrayList<>(orderOneListByProt(templist));

    templist.clear();
    for (Unit u : coms) {
      boolean holy = false;
      boolean mounted = false;
      for (Command c : u.getCommands()) {
        if (c.command.equals("#holy")) holy = true;
        if (c.command.equals("#mounted")) mounted = true;
      }
      if (!holy && mounted) templist.add(u);
    }
    newlist.addAll(orderOneListByProt(templist));

    templist.clear();
    for (Unit u : coms) {
      boolean holy = u
        .getCommands()
        .stream()
        .anyMatch(c -> c.command.equals("#holy"));
      if (holy) templist.add(u);
    }

    newlist.addAll(orderOneListByProt(templist));

    return newlist;
  }

  private List<Unit> orderOneListByProt(List<Unit> units) {
    List<Unit> newList = new ArrayList<>();

    while (units.size() > 0) {
      int lowestProt = 100;
      Unit lowest = null;
      for (Unit u : units) {
        int prot = u.getTotalProt();
        if (prot < lowestProt) {
          lowest = u;
          lowestProt = prot;
        }
      }

      newList.add(lowest);
      units.remove(lowest);
    }

    return newList;
  }

  private boolean shouldReRoll(Unit u, List<Unit> list) {
    boolean mounted = u
      .getCommands()
      .stream()
      .anyMatch(c -> c.command.equals("#mounted"));

    Item armor = u.getSlot("armor");

    for (Unit unit : list) {
      Item unitArmor = unit.getSlot("armor");
      boolean unitMounted = unit
        .getCommands()
        .stream()
        .anyMatch(c -> c.command.equals("#mounted"));

      if (
        unitArmor == armor &&
        (unit.getSlot("mount") == u.getSlot("mount") &&
          (unitMounted && mounted))
      ) return true;
    }

    return false;
  }

  private Unit getUnitWith(
    List<String> features,
    List<String> allFeatures,
    List<Unit> possibleComs
  ) {
    Unit unit = null;
    int highest = 0;

    for (Unit u : possibleComs) {
      int count = 0;
      for (Command c : u.getCommands()) {
        for (String feature : features) {
          if (feature.equals(c.command)) count++;
        }
      }

      if (count > 0 && count > highest) {
        unit = u;
        highest = count;
      }
    }

    return unit;
  }

  private void process(Unit u) {
    u.commands.add(Command.args("#att", "+" + strength));
    u.commands.add(Command.args("#def", "+" + strength));
    u.commands.add(Command.args("#mor", "+" + strength));
    u.commands.add(Command.args("#hp", "+" + strength));
    u.commands.add(Command.args("#prec", "+" + strength));

    int racialMapMove = 2;
    for (Command c : u.race.unitcommands) {
      if (c.command.equals("#mapmove")) racialMapMove = c.args.get(0).getInt();
    }

    for (Command c : u.getCommands()) {
      if (c.command.equals("#mapmove")) {
        int mapmove = c.args.get(0).getInt();
        if (mapmove < racialMapMove) u.commands.add(
          Command.args("#mapmove", "+" + (racialMapMove - mapmove))
        );
      }
    }

    int stuffmask = r.nextInt(3) + 1;

    if (strength == 1) stuffmask = 1;
    else if (strength == 2) stuffmask = 3;

    if (stuffmask == 1 || stuffmask == 3) {
      //System.out.println(items.filterForPose(u.pose).filterSlot("cloakb").getRandom(new ArrayList<Tag>(), false).xoffset + ", " + items.filterForPose(u.pose).filterSlot("cloakb").getRandom(new ArrayList<Tag>(), false).yoffset);
      u.setSlot(
        "cloakb",
        EntityChances.baseChances(u.pose.getItems("cloakb")).getRandom(r)
      );
      //System.out.println(u.getSlot("cloakb").xoffset + ", " + u.getSlot("cloakb").yoffset);

    }

    if (stuffmask == 2 || stuffmask == 3) {
      equipEliteItem(u, "helmet");
      equipEliteItem(u, "weapon");
      equipEliteItem(u, "offhand");
    }

    // Remove troop only filters
    List<Filter> removef = new ArrayList<>();
    for (Filter f : u.appliedFilters) if (
      f.themes.contains("trooponly")
    ) removef.add(f);
    u.appliedFilters.removeAll(removef);
  }

  private ItemSet getSuitableCommanderItems(Unit u, String slot) {
    ItemSet all = new ItemSet();

    Item i = u.getSlot(slot);
    boolean sacred =
      u.tags.containsName("sacred") ||
      (i != null && i.tags.containsName("sacred"));

    all.addAll(u.pose.getItems(slot).filterTheme("elite", true));
    if (sacred) all.addAll(u.pose.getItems(slot).filterTheme("sacred", true));

    if (i != null && i.name != null) all.removeAll(
      u.pose
        .getItems(slot)
        .filterTag(new Command("eliteversion", new Arg(i.name)))
    );

    return all;
  }

  private void equipEliteItem(Unit u, String slot) {
    if (u.getSlot(slot) == null) return;

    // Try to get elite version
    Item item = u.getSlot(slot);

    ItemSet eliteHelmets = item.tags
      .getAllValues("eliteversion")
      .stream()
      .map(Arg::get)
      .map(name -> u.pose.getItems(slot).getItemWithName(name, slot))
      .collect(Collectors.toCollection(ItemSet::new));

    Item helmet = null;
    if (!eliteHelmets.isEmpty()) {
      helmet = chandler.getRandom(eliteHelmets, u);
    }

    // No elite version and already elite or sacred? Yeah. Let's not bother.
    if (
      helmet == null &&
      (u.getSlot(slot).themes.contains("elite") ||
        u.getSlot(slot).themes.contains("sacred"))
    ) return;

    // Try to get an elite item with same id in same slot
    if (helmet == null && u.pose.getItems(slot) != null) {
      try {
        ItemSet helmets = new ItemSet();

        for (Item it : getSuitableCommanderItems(u, slot).filterSlot(slot)) if (
          it.id.equals(item.id)
        ) helmets.add(it);

        helmet = chandler.getRandom(helmets, u);
      } catch (Exception e) {
        throw new IllegalStateException("Is this fixable?", e);
      }
    }

    // Try to get an elite armor of some suitable sort
    if (u.getSlot(slot).armor && helmet == null && !slot.equals("offhand")) {
      int helmetprot = item.armor
        ? nationGen.armordb.GetInteger(item.id, "prot")
        : 0;

      helmet = chandler.getRandom(
        getSuitableCommanderItems(u, slot).filterProt(
          nationGen.armordb,
          helmetprot,
          helmetprot
        ),
        u
      );

      if (helmet == null) helmet = chandler.getRandom(
        getSuitableCommanderItems(u, slot).filterProt(
          nationGen.armordb,
          helmetprot - 2,
          helmetprot + 8
        ),
        u
      );
    }

    if (helmet != null) u.setSlot(slot, helmet);
  }

  public void generateDescription(
    Unit com,
    boolean magicbeing,
    boolean demonud,
    boolean hasSlaves
  ) {
    List<String> normalLd = new ArrayList<>();
    int leader = 1;
    int magicLd = 0;
    int demonUDLd = 0;
    int holyRank = 0;

    boolean awe = false;
    boolean fear = false;
    boolean inspiring = false;
    boolean taskmaster = false;
    boolean beastmaster = false;
    /*
		
		normalLd.add(pickRandomSubstring("unexceptional adequate undistinguished unremarkable"));
		normalLd.add(pickRandomSubstring("willingly obediently dutifully freely heedfully carefully"));
		
		for(Command c : com.getCommands())
		{
			switch (c.command) {
				case "#expertleader":
					normalLd.set(0, pickRandomSubstring("calculating masterful adroit deft wiley cunning expert consummate veteran"));
					normalLd.set(1, pickRandomSubstring("unquestioningly fearlessly unhesitatingly zealously fanatically dauntlessly"));
					leader = 3;
					break;
				case "#goodleader":
					normalLd.set(0, pickRandomSubstring("competent decisive skilled able experienced clever adept skillful practiced capable effective"));
					normalLd.set(1, pickRandomSubstring("confidently boldly loyally cheerfully courageously stalwartly swiftly"));
					leader = 2;
					break;
				case "#poorleader":
					normalLd.set(0, pickRandomSubstring("incompetent rash short-sighted inexperienced indecisive craven foolish"));
					normalLd.set(1, pickRandomSubstring("grudgingly skittishly anxiously apprehensively spiritlessly confusedly recklessly wildly"));
					leader = 0;
					break;
				case "#noleader":
					leader = -1;
					break;
				case "#goodmagicleader":
					magicLd = 3;
					break;
				case "#okmagicleader":
					magicLd = 2;
					break;
				case "#poormagicleader":
					magicLd = 1;
					break;
				case "#goodundeadleader":
					demonUDLd = 3;
					break;
				case "#okundeadleader":
					demonUDLd = 2;
					break;
				case "#poorundeadleader":
					demonUDLd = 1;
					break;
				case "#awe":
					awe = true;
					break;
				case "#fear":
					fear = true;
					break;
				case "#inspirational":
					inspiring = true;
					break;
				case "#taskmaster":
					taskmaster = true;
					break;
				case "#beastmaster":
					beastmaster = true;
					break;
				case "#magicskill":
					if (c.args.get(0).getMagicPath() == MagicPath.HOLY)            // We only care about pure priests here
						holyRank = c.args.get(1).getInt();
					break;
				case "#holy":
					if (holyRank < 1)
						holyRank = 1;
					break;
			}
		}
		
		String desc = "The %unitname_plural% of %nation% are ";
		
		if(awe && fear)
		{
			desc += pickRandomSubstring("cruel brutal terrifying savage vicious ruthless bloodthirsty") + " yet " + pickRandomSubstring("beloved adored revered venerated exalted") 
					+ " figures whose " + normalLd.get(0);
		}
		else if(awe)
		{
			desc += pickRandomSubstring("daunting aloof unapproachable glorious venerated formidible lofty")
					+ " figures whose " + normalLd.get(0);
		}
		else if(fear)
		{
			desc += pickRandomSubstring("cruel brutal terrifying savage vicious ruthless dreaded")
					+ " leaders whose " + normalLd.get(0);
			if(inspiring && leader <= 1)
				desc += " yet";
			else if(taskmaster && leader > 1)
				desc += " but";
			else if(inspiring || taskmaster)
				desc += " and";
		}
		else if(leader == -1)
		{
			desc += "not " 
					+ pickRandomSubstring("entrusted charged burdened encumbered bothered troubled concerned occupied")
					+ " with the "
					+ pickRandomSubstring("responsibilities duties tasks details problems honor chore privilege")
					+ " of commanding the armies of %nation%.";
		}
		else 
			desc += normalLd.get(0) + " leaders whose";
		
		if(leader == -1){}
		else if(inspiring && taskmaster)
			desc += " " + pickRandomSubstring("compelling forceful ardent vehement fierce gruff arresting");
		else if(inspiring)
			desc += " " + pickRandomSubstring("inspiring rousing stirring earnest fiery impassioned");
		else if(taskmaster)
			desc += " " + pickRandomSubstring("harsh demanding exacting stern dour stringent");
		
		desc += " " + pickRandomSubstring("orders commands decrees mandates instructions plans") 
				+ " are " + normalLd.get(1) + " " + pickRandomSubstring("carried out,executed,obeyed,enacted", ",") 
				+ " by ";
		
		if(leader == -1){}
		else if(beastmaster)
			desc += "man and beast alike.";
		else if(hasSlaves && taskmaster)
			desc += "their subordinates and slaves.";
		else if(holyRank > 0)
			desc += "their " + pickRandomSubstring("followers flock juniors disciples minions guardians subordinates attendants companions escorts lackeys assistants") + ".";
		else
			desc += "their " + pickRandomSubstring("troops soldiers warriors minions followers forces") + ".";
		
		if(leader == -1){}
		else if(magicLd > 0 || demonUDLd > 0)
		{
			desc += " The %unitname_plural% are also charged with " + pickRandomSubstring("commanding leading overseeing deploying overseeing");  
			
			if(magicLd == 0)
				desc += " the unholy servants of %nation%, but its magical forces are beyond their " 
						+ pickRandomSubstring("power ability ken understanding expertise") + ".";
			else if(demonUDLd == 0)
				desc += " the magical servants of %nation%, but its darker forces are beyond their " 
						+ pickRandomSubstring("power ability ken understanding expertise") + ".";
			else
			{
				if(magicbeing || demonud)
					desc += " the enchanted and unholy warriors of %nation%";
				else
					desc += " any supernatural entity that the %mages_plural% of %nation% might " + pickRandomSubstring("conjur,summon,raise,bind into service", ",");
				
				if(magicLd < 2 && demonUDLd < 2)
					desc += ", although they can only control a handful of these beings.";
				else
					desc += ".";
			}
		}
		else if(magicbeing || demonud)
		{
			if(magicbeing && demonud)
				desc += " However, leading the magical and unholy servants ";
			else if(magicbeing)
				desc += " However, leading the enchanted servitors ";
			else if(demonud)
				desc += " However, leading the darkest forces ";
			
			desc += "of %nation% is beyond their " + pickRandomSubstring("power ability ken understanding expertise") + ".";
		}
		
		if(holyRank > 0)
		{
			if(holyRank == 1)
				desc += " The %unitname_plural% hold a very minor place in %nation%'s heirarchy, with " 
						+ pickRandomSubstring("little,hardly,almost no,barely", ",") + " more " 
						+ pickRandomSubstring("power insight numina understanding standing status prestige respect dignity training authority") + " than a %sacredname%.";
			else if(holyRank == 2)
				desc += " The %unitname_plural% make up the " 
						+ pickRandomSubstring("core,backbone,heart,rank and file,foundation,body,majority", ",") + " of %nation%'s "
						+ pickRandomSubstring("clergy,priests,faith,religion,temple,cult,priesthood", ",") +", playing a crucial role in all almost matters of faith.";
			else
				desc += " At the  " + pickRandomSubstring("pinnacle top head peak summit") + " of %nation%'s religious heirarchy, the %unitname_plural% are the "
					+ pickRandomSubstring("foremost,most exalted,greatest,most powerful,most prestigious,wisest", ",") +" of their faith.";
		}
		
		com.commands.add(Command.args("#descr", desc));
		*/
  }

  private String pickRandomSubstring(String string) {
    String[] words = null;

    words = string.split(" ");

    return words[r.nextInt(words.length)];
  }

  private String pickRandomSubstring(String string, String token) {
    String[] words = null;

    words = string.split(token);

    return words[r.nextInt(words.length)];
  }
}
