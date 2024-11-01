package nationGen.chances;

import com.elmokki.Generic;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import nationGen.entities.Race;
import nationGen.items.CustomItem;
import nationGen.items.Item;
import nationGen.magic.MagicPath;
import nationGen.magic.MagicPathInts;
import nationGen.misc.*;
import nationGen.units.Unit;

/**
 * An enumeration of types of conditions that are available to use in a chanceinc command.  Each type has the ability to
 * parse given arguments into a {@link Condition} which takes a {@link ChanceIncData} and returns a boolean.
 */
public enum ChanceIncConditionType {
  // ******************* General ***************

  TRUE("true") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      return d -> true;
    }
  },
  FALSE("false") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      return d -> false;
    }
  },
  RANDOM("random") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      double chance = args.nextDecimal();
      return d -> d.n.random.nextDouble() < chance;
    }
  },

  // ******************** NationInc *********************

  MAGIC("magic") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      List<Predicate<Set<MagicPath>>> checks = new ArrayList<>();
      while (!args.isEmpty()) {
        boolean not = args.nextOptionalFlag("not");
        MagicPath path = args.nextMagicPath();
        checks.add(highest -> highest.contains(path) != not);
      }

      return d -> checks.stream().allMatch(p -> p.test(d.atHighest));
    }
  },

  MAGE_WITH_PATHS("magewithpaths") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      List<Predicate<MagicPathInts>> checks = new ArrayList<>();
      while (!args.isEmpty()) {
        boolean not = args.nextOptionalFlag("not");
        MagicPath path = args.nextMagicPath();
        checks.add(paths -> paths.get(path) > 0 != not);
      }

      return d ->
        d.n
          .selectCommanders("mage")
          .map(Unit::getMagicPicks)
          .anyMatch(picks -> checks.stream().allMatch(p -> p.test(picks)));
    }
  },

  ANY_MAGIC("anymagic") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      MagicPath path = args.nextMagicPath();

      return d -> d.nonrandom_paths.get(path) > 0 != not;
    }
  },

  SHAPE("shape") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String shape = args.nextString();

      return d ->
        d.n.secondshapes
          .stream()
          .anyMatch(u -> u.thisForm.name.equals(shape)) !=
        not;
    }
  },

  MAGIC_DIVERSITY("magicdiversity") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      int div = args.nextInt();

      return d -> (d.diversity >= div) != not;
    }
  },

  PICKS_AT_LEVEL("picksatlevel") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      int level = args.nextInt();
      int amount = args.nextInt();

      return d -> {
        int picks = 0;
        for (int z = level; z < 10; z++) picks += d.at[z];

        return (picks >= amount) != not;
      };
    }
  },
  SPELLS("spells") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String name = args.nextString();

      return d ->
        d.n.spells.stream().anyMatch(s -> s.name.equalsIgnoreCase(name)) != not;
    }
  },

  MODULE_ID("moduleid") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String id = args.nextString();

      return d -> id.equals(d.moduleId) != not;
    }
  },

  HAS_THEME("hastheme") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String theme = args.nextString();

      return d ->
        d.n.races.size() > 0 &&
        d.n.races
          .get(0)
          .themefilters.stream()
          .anyMatch(t -> t.name.equals(theme)) !=
        not;
    }
  },

  ANY_THEME("anytheme") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String theme = args.nextString();

      return d ->
        d.n.races.size() > 0 &&
        (d.n.races
            .get(0)
            .themefilters.stream()
            .anyMatch(t -> t.themes.contains(theme)) ||
          d.n.races.get(0).themes.contains(theme)) !=
        not;
    }
  },

  HAS_THEME_THEME("hasthemetheme") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String theme = args.nextString();

      return d ->
        d.n.races.size() > 0 &&
        d.n.races
          .get(0)
          .themefilters.stream()
          .anyMatch(t -> t.themes.contains(theme)) !=
        not;
    }
  },

  SECONDARY_THEME_THEME("secondarythemetheme") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String theme = args.nextString();

      return d ->
        d.n.races.size() > 1 &&
        d.n.races
          .get(1)
          .themefilters.stream()
          .anyMatch(t -> t.themes.contains(theme)) !=
        not;
    }
  },

  SECONDARY_RACE_THEME("secondaryracetheme") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String theme = args.nextString();

      return d ->
        d.n.races.size() > 1 &&
        d.n.races
          .get(1)
          .themefilters.stream()
          .anyMatch(t -> t.name.equals(theme)) !=
        not;
    }
  },

  ERA("era") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      int era = args.nextInt();

      return d -> (era == d.n.era) != not;
    }
  },

  HAS_PRIMARY_RACE("hasprimaryrace") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");

      return d -> (d.n.races.size() > 0) != not;
    }
  },

  NATION_COMMAND("nationcommand") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      return generateCommandChanceInc(args, d -> {
        List<Command> coms = d.n.getCommands();
        coms.addAll(d.n.races.get(0).nationcommands);
        return coms.stream();
      });
    }
  },

  MAGIC_PRIORITY("magicpriority") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      MagicPath path = args.nextMagicPath();
      boolean not =
        args.nextOptionalFlag("not") ^ args.nextOptionalFlag("below");
      double target = args.nextDecimal();

      return d -> {
        double chance = 1;
        for (Args priorityArgs : Generic.getAllNationTags(d.n).getAllArgs(
          "magicpriority"
        )) {
          if (path == priorityArgs.get(0).getMagicPath()) {
            chance *= priorityArgs.get(priorityArgs.size() - 1).getDouble();
          }
        }
        return chance >= target != not;
      };
    }
  },

  PRIMARY_RACE_COMMAND("primaryracecommand") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      return generateCommandChanceInc(args, d ->
        d.n.races.get(0).getCommands().stream()
      );
    }
  },

  COMMAND("command") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      return generateCommandChanceInc(args, d ->
        Stream.of(
          d.n.selectTroops(),
          d.n.selectCommanders("mage"),
          d.n.selectCommanders("priest")
        )
          .flatMap(s -> s)
          .flatMap(u -> u.getCommands().stream())
      );
    }
  },

  PERCENTAGE_OF_COMMAND("percentageofcommand") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      String command = args.nextString();
      boolean not =
        args.nextOptionalFlag("not") ^ args.nextOptionalFlag("below");
      double percent = args.nextDecimal();

      return d -> {
        List<Unit> units = d.n.listTroops();
        d.n.selectCommanders("mage").forEach(units::add);
        d.n.selectCommanders("priest").forEach(units::add);

        int all = units.size();
        int found = 0;

        for (Unit u : units) {
          for (Command c : u.getCommands()) {
            if (c.command.equals(command) || c.command.equals("#" + command)) {
              found++;
            }
          }
        }

        double share = (double) found / all;

        return (share >= percent) != not;
      };
    }
  },

  PERCENTAGE_OF_RACE("percentageofrace") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      String race = args.nextString();
      boolean not =
        args.nextOptionalFlag("not") ^ args.nextOptionalFlag("below");
      double percent = args.nextDecimal();

      return d -> {
        Optional<Race> r = d.n.races
          .stream()
          .filter(r2 -> race.equalsIgnoreCase(r2.name))
          .findFirst();

        return (
          (r.isPresent() && d.n.percentageOfRace(r.get()) >= percent) != not
        );
      };
    }
  },

  PRIMARY_RACE("primaryrace") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String race = args.nextString();

      return d ->
        (d.n.races.size() > 0 &&
          d.n.races.get(0).name.equalsIgnoreCase(race)) !=
        not;
    }
  },

  SECONDARY_RACE("secondaryrace") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String race = args.nextString();

      return d ->
        (d.n.races.size() > 1 &&
          d.n.races.get(1).name.equalsIgnoreCase(race)) !=
        not;
    }
  },

  ANY_UNIT_FILTER("anyunitfilter") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String filter = args.nextString();

      return d ->
        not ^
        (d.n
            .selectTroops()
            .flatMap(u -> u.appliedFilters.stream())
            .anyMatch(f -> f.name.equalsIgnoreCase(filter)) ||
          d.n
            .selectCommanders()
            .flatMap(u -> u.appliedFilters.stream())
            .anyMatch(f -> f.name.equalsIgnoreCase(filter)));
    }
  },

  NATION_TAG("nationtag") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      Command tag = args.nextCommand();

      return d ->
        (d.n.nationthemes.stream().anyMatch(f -> f.tags.containsTag(tag)) ||
          (d.n.races.size() > 0 && d.n.races.get(0).tags.containsTag(tag))) !=
        not;
    }
  },

  AVERAGE_GOLD("avggold") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      double gold = args.nextDecimal();

      return d -> d.avggold >= gold != not;
    }
  },

  AVERAGE_RESOURCES("avgres") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      double res = args.nextDecimal();

      return d -> d.avgres >= res != not;
    }
  },

  UNITS_WITH_RES_ABOVE("unitswithresabove") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not =
        args.nextOptionalFlag("not") ^ args.nextOptionalFlag("below");
      double res = args.nextDecimal();
      int count = args.nextInt();

      return d ->
        d.n.selectTroops().filter(u -> u.getResCost(true) > res).count() >=
          count !=
        not;
    }
  },

  CAP_ONLY_UNITS_WITH_RES_ABOVE("caponlyunitswithresabove") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not =
        args.nextOptionalFlag("not") ^ args.nextOptionalFlag("below");
      double res = args.nextDecimal();
      int count = args.nextInt();

      return d ->
        d.n
            .selectTroops()
            .filter(u -> u.caponly)
            .filter(u -> u.getResCost(true) > res)
            .count() >=
          count !=
        not;
    }
  },

  UNITS_WITH_SIZE("unitswithsize") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not =
        args.nextOptionalFlag("not") ^ args.nextOptionalFlag("below");
      int size = args.nextInt();
      int count = args.nextInt();

      return d ->
        d.n
            .selectTroops()
            .filter(u -> u.getCommandValue("#size", 2) >= size)
            .count() >=
          count !=
        not;
    }
  },

  // ************* Checks both Nation and Unit ***************

  RACE_TAG("racetag") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      Command tag = args.nextCommand();
      return d ->
        (d.n.races.size() > 0 &&
          d.n.races.get(0).tags.containsTag(tag) != not) ||
        (d.u != null && d.u.race.tags.containsTag(tag) != not);
    }
  },

  // ********************** UnitInc **************************

  POSE("pose") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String pose = args.nextString();

      return d ->
        (d.u.pose.roles.contains(pose) ||
          d.u.pose.roles.contains("elite " + pose) ||
          d.u.pose.roles.contains("sacred " + pose)) !=
        not;
    }
  },

  PERSONAL_COMMAND("personalcommand") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      return generateCommandChanceInc(args, d -> d.u.getCommands().stream()); // there used to be a null check on d.u returning false, not sure if necessary
    }
  },

  PERSONAL_SHAPE("personalshape") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String shape = args.nextString();

      return d ->
        d.u != null &&
        d.n.secondshapes
          .stream()
          .anyMatch(
            u -> u.thisForm.name.equals(shape) && u.otherForm.equals(d.u)
          ) !=
        not;
    }
  },

  FILTER("filter") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String filter = args.nextString();

      return d ->
        d.u.appliedFilters.stream().anyMatch(f -> f.name.equals(filter)) != not;
    }
  },

  TAG("tag", "unittag") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      Command tag = args.nextCommand();

      return d -> Generic.getAllUnitTags(d.u).containsTag(tag) != not;
    }
  },

  UNIT_THEME("unittheme") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String theme = args.nextString();

      return d ->
        (d.u.race.themes.contains(theme) ||
          d.u.pose.themes.contains(theme) ||
          d.u.race.themefilters
            .stream()
            .anyMatch(f -> f.themes.contains(theme)) ||
          d.u.appliedFilters
            .stream()
            .anyMatch(f -> f.themes.contains(theme))) !=
        not;
    }
  },

  RACE_THEME("racetheme") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String theme = args.nextString();

      return d ->
        (d.race.themes.contains(theme) ||
          d.race.themefilters
            .stream()
            .anyMatch(f -> f.name.equals(theme) || f.themes.contains(theme))) !=
        not;
    }
  },

  POSE_TAG("posetag") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      Command tag = args.nextCommand();

      return d -> d.u.pose.tags.containsTag(tag) != not;
    }
  },

  POSE_THEME("posetheme") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String theme = args.nextString();

      return d -> d.u.pose.themes.contains(theme) != not;
    }
  },

  SLOT("slot") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      String slot = args.nextString();
      boolean not = args.nextOptionalFlag("not");
      boolean armor = args.nextOptionalFlag("armor");
      boolean weapon = args.nextOptionalFlag("weapon");
      Optional<String> id = args.nextIf(a -> true).map(Arg::get);

      return d -> {
        if (d.u == null) {
          return false;
        }
        Item i = d.u.getSlot(slot);
        if (i != null) {
          boolean contains = false;
          if ((!armor || i.armor) && (!weapon || !i.armor)) {
            if (id.isEmpty() || i.id.equals(id.get())) {
              contains = true;
            } else if (i instanceof CustomItem) {
              CustomItem ci = (CustomItem) i;
              contains = (ci.olditem != null &&
                ci.olditem.id != null &&
                ci.olditem.id.equals(id.get())) ||
              (Integer.parseInt(i.id) >= (i.armor ? 250 : 800) &&
                i.tags.getString("OLDID").stream().anyMatch(id.get()::equals));
            }
          }
          return contains != not;
        }
        return false;
      };
    }
  },

  SLOT_NAME("slotname") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      String slot = args.nextString();
      boolean not = args.nextOptionalFlag("not");
      boolean armor = args.nextOptionalFlag("armor");
      boolean weapon = args.nextOptionalFlag("weapon");
      Optional<String> name = args.nextIf(a -> true).map(Arg::get);

      return d -> {
        if (d.u == null) {
          return false;
        }
        Item i = d.u.getSlot(slot);
        if (i != null) {
          return (
            ((!armor || i.armor) &&
              (!weapon || !i.armor) &&
              (name.isEmpty() || i.name.equals(name.get()))) !=
            not
          );
        }
        return false;
      };
    }
  },

  SLOT_TAG("slottag") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      // TODO: make this only accept 'not' in certain locations or make them do different things in each location
      boolean notSlot = args.nextOptionalFlag("not");
      String slot = args.nextString();
      boolean not = notSlot || args.nextOptionalFlag("not");
      String tagName = args.nextString();

      return d -> {
        if (d.u == null) {
          return false;
        }
        Item i = d.u.getSlot(slot);
        return i != null && i.tags.containsName(tagName) != not;
      };
    }
  },

  SLOT_TAG_VALUE("slottagvalue") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      // TODO: make this only accept 'not' in certain locations or make them do different things in each location
      boolean notSlot = args.nextOptionalFlag("not");
      String slot = args.nextString();
      boolean notName = notSlot || args.nextOptionalFlag("not");
      String tagName = args.nextString();
      boolean not = notName || args.nextOptionalFlag("not");
      Arg tagValue = args.next("tag value");

      return d -> {
        if (d.u == null) {
          return false;
        }
        Item i = d.u.getSlot(slot);
        return (
          i != null &&
          i.tags.getValue(tagName).stream().anyMatch(tagValue::equals) != not
        );
      };
    }
  },

  ITEM_TAG("itemtag") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      Command tag = args.nextCommand();

      return d ->
        d.u.slotmap.items().anyMatch(i -> i.tags.containsTag(tag)) != not;
    }
  },

  ITEM_THEME("itemtheme") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String theme = args.nextString();

      return d ->
        d.u.slotmap.items().anyMatch(i -> i.themes.contains(theme)) != not;
    }
  },

  POSE_ITEM_THEME("poseitemtheme") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String slot = args.nextString();
      String theme = args.nextString();

      return d ->
        Optional.ofNullable(d.u.pose.getItems(slot))
          .map(set ->
            set
              .stream()
              .filter(Objects::nonNull)
              .anyMatch(i -> i.themes.contains(theme))
          )
          .orElse(false) !=
        not;
    }
  },

  TAGGED_MAGIC("taggedmagic") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      List<MagicPath> paths = new ArrayList<>();
      paths.add(args.nextMagicPath());
      while (!args.isEmpty()) {
        paths.add(args.nextMagicPath());
      }

      return d -> {
        Tags tags = new Tags();

        tags.addAll(d.u.tags);
        d.u.appliedFilters.forEach(fu -> tags.addAll(fu.tags));
        d.u.slotmap.items().forEach(i -> tags.addAll(i.tags));

        return paths
          .stream()
          .allMatch(path -> tags.contains("path", new Arg(path)) != not);
      };
    }
  },

  COMPARE_MAGIC("comparemagic") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      MagicPath path1 = args.nextMagicPath();
      Comparison comparison = Comparison.find(args.nextString()).orElseThrow();
      MagicPath path2 = args.nextMagicPath();

      return d -> {
        MagicPathInts picks = d.u.getMagicPicks();

        switch (comparison) {
          case BELOW:
            return picks.get(path1) < picks.get(path2) != not;
          case ABOVE:
            return picks.get(path1) > picks.get(path2) != not;
          case EQUAL:
            return (picks.get(path1) == picks.get(path2)) != not;
          default:
            throw new IllegalArgumentException(
              "comparemagic doesn't know how to compare with " + comparison
            );
        }
      };
    }
  },

  PERSONAL_MAGIC("personalmagic") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      List<Predicate<MagicPathInts>> checks = new ArrayList<>();
      while (!args.isEmpty()) {
        MagicPath path = args.nextMagicPath();
        boolean below = args.nextOptionalFlag("below");
        int level = args.nextOptionalInt().orElse(1);
        checks.add(picks -> picks.get(path) >= level != below);
      }

      return d -> {
        MagicPathInts picks = d.u.getMagicPicks();
        return checks.stream().allMatch(check -> check.test(picks));
      };
    }
  },

  ORIG_NAME("origname") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String name = args.nextString();

      return d -> name.equalsIgnoreCase(d.u.name.type.name) != not;
    }
  },

  RACE("race") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String race = args.nextString();

      return d -> d.race != null && race.equalsIgnoreCase(d.race.name) != not;
    }
  },

  PROT("prot") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      boolean below = args.nextOptionalFlag("below");
      int target = args.nextInt();

      return d -> d.u.getTotalProt(true) >= target != (below ^ not);
    }
  },

  ENC("enc") {
    @Override
    Condition<ChanceIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      boolean below = args.nextOptionalFlag("below");
      int target = args.nextInt();

      return d -> d.u.getTotalEnc() >= target != (below ^ not);
    }
  };

  private static final Map<String, ChanceIncConditionType> LOOKUP;

  static {
    Map<String, ChanceIncConditionType> temp = new HashMap<>();
    for (ChanceIncConditionType type : values()) {
      if (temp.put(type.name, type) != null) {
        throw new IllegalArgumentException(
          String.format(
            "ConditionType %s has duplicate name '%s'",
            type,
            type.name
          )
        );
      }
      for (String name : type.alternateNames) {
        if (temp.put(name, type) != null) {
          throw new IllegalArgumentException(
            String.format(
              "ConditionType %s has duplicate name '%s'",
              type,
              name
            )
          );
        }
      }
    }
    LOOKUP = Collections.unmodifiableMap(temp);
  }

  final String name;
  final String[] alternateNames;

  ChanceIncConditionType(String name, String... alternateNames) {
    this.name = name;
    this.alternateNames = alternateNames;
  }

  abstract Condition<ChanceIncData> parseConditionArguments(ArgParser args);

  static Condition<ChanceIncData> create(Args args) {
    if (args.isEmpty()) {
      throw new IllegalArgumentException(
        "Condition<ChanceIncData> has no arguments."
      );
    }
    ArgParser parser = args.parse();
    final String chanceIncName = parser.nextString();
    return from(chanceIncName)
      .orElseThrow(() ->
        new IllegalArgumentException(
          String.format(
            "No #chanceinc condition type found with initial argument [%s]. Args: [%s].",
            chanceIncName,
            args
          )
        )
      )
      .parseConditionArguments(parser);
  }

  public static Optional<ChanceIncConditionType> from(String name) {
    return Optional.ofNullable(LOOKUP.get(name));
  }

  private static Condition<ChanceIncData> generateCommandChanceInc(
    ArgParser args,
    Function<ChanceIncData, Stream<Command>> commandsToTest
  ) {
    boolean firstNot = args.nextOptionalFlag("not");
    String command = args.nextString();
    boolean not = firstNot ^ args.nextOptionalFlag("not");
    Comparison comp = args
      .nextIfConvertable(a -> Comparison.find(a.get().toUpperCase()))
      .orElse(Comparison.EQUAL);
    Args remaining = args.remaining();

    return d ->
      not ^
      commandsToTest
        .apply(d)
        .anyMatch(c -> commandMatches(c, command, comp, remaining));
  }

  private static boolean commandMatches(
    Command c,
    String command,
    Comparison comp,
    Args remaining
  ) {
    if (c.command.equals(command) || c.command.equals("#" + command)) {
      if (c.args.isEmpty() || remaining.isEmpty()) {
        return true;
      }

      Arg arg = c.args.get(0);
      switch (comp) {
        case EQUAL:
          return arg.equals(remaining.get(0));
        case BELOW:
          return arg.getInt() < remaining.get(0).getInt();
        case ABOVE:
          return arg.getInt() > remaining.get(0).getInt();
      }
    }
    return false;
  }
}
