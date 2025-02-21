package nationGen.chances;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.misc.ArgParser;
import nationGen.misc.Args;
import nationGen.misc.Command;

/**
 * The different types of conditions available to a {@link ThemeInc} command.  Each type has the ability to read some
 * arguments and return a {@link Condition} which consumes a {@link ThemeIncData} and returns a boolean.
 */
public enum ThemeIncConditionType {
  // ********************* THEME INCS *******************

  THEME("theme") {
    @Override
    Condition<ThemeIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String theme = args.nextString();

      return d -> d.f.themes.contains(theme) != not;
    }
  },

  OWN_TAG("owntag") {
    @Override
    Condition<ThemeIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      Command tag = args.nextCommand();

      return d -> d.f.tags.containsTag(tag) != not;
    }
  },

  RACE_NAME("racename") {
    @Override
    Condition<ThemeIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String raceName = args.nextString();

      return d ->
        d.f instanceof Race &&
        (d.f.name != null && d.f.name.equalsIgnoreCase(raceName)) != not;
    }
  },

  THIS_ITEM_TAG("thisitemtag") {
    @Override
    Condition<ThemeIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      Command tag = args.nextCommand();

      return d -> d.f instanceof Item && d.f.tags.containsTag(tag) != not;
    }
  },

  THIS_ITEM_SLOT_TAG("thisitemslottag") {
    @Override
    Condition<ThemeIncData> parseConditionArguments(ArgParser args) {
      boolean notSlot = args.nextOptionalFlag("not");
      String slot = args.nextString();
      boolean not = notSlot || args.nextOptionalFlag("not");
      Command tag = args.nextCommand();

      return d ->
        d.f instanceof Item &&
        ((Item) d.f).slot.equals(slot) &&
        (d.f.tags.containsTag(tag) != not);
    }
  },

  THIS_ITEM_THEME("thisitemtheme") {
    @Override
    Condition<ThemeIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      String theme = args.nextString();

      return d -> d.f instanceof Item && d.f.themes.contains(theme) != not;
    }
  },

  THIS_ITEM_SLOT_THEME("thisitemslottheme") {
    @Override
    Condition<ThemeIncData> parseConditionArguments(ArgParser args) {
      boolean notSlot = args.nextOptionalFlag("not");
      String slot = args.nextString();
      boolean not = notSlot || args.nextOptionalFlag("not");
      String theme = args.nextString();

      return d ->
        d.f instanceof Item &&
        ((Item) d.f).slot.equals(slot) &&
        d.f.themes.contains(theme) != not;
    }
  },

  IS_FERROUS_ITEM("isferrousitem") {
    @Override
    Condition<ThemeIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");

      return d -> {
        if (d.f instanceof Item) {
          Item i = (Item) d.f;
          boolean ferrous = i.armor
            ? (d.nationGen.armordb.GetInteger(i.id, "ferrous", 0) == 1)
            : (d.nationGen.weapondb.GetInteger(i.id, "ironweapon", 0) == 1);
          return ferrous != not;
        }
        return false;
      };
    }
  },

  WEAPON_UW_PENALTY("weaponuwpenalty") {
    @Override
    Condition<ThemeIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      boolean below = args.nextOptionalFlag("below");
      int target = args.nextInt();

      return d -> {
        if (d.f instanceof Item) {
          Item i = (Item) d.f;
          if (!i.armor) {
            boolean slash =
              d.nationGen.weapondb.GetInteger(i.id, "dt_slash", 0) == 1;
            boolean blunt =
              d.nationGen.weapondb.GetInteger(i.id, "dt_blunt", 0) == 1;
            boolean pierce =
              d.nationGen.weapondb.GetInteger(i.id, "dt_pierce", 0) == 1;
            int lgt = d.nationGen.weapondb.GetInteger(i.id, "lgt", 0);

            int penalty = 0;
            if (!pierce && (blunt || slash)) penalty = lgt;
            else if (pierce && (blunt || slash)) penalty = (int) Math.round(
              (double) lgt / 2
            );

            return (penalty >= target) != (not ^ below);
          }
        }
        return false;
      };
    }
  },

  THIS_ARMOR_PROT("thisarmorprot") {
    @Override
    Condition<ThemeIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      boolean below = args.nextOptionalFlag("below");
      int target = args.nextInt();

      return d -> {
        if (d.f instanceof Item) {
          Item i = (Item) d.f;

          if (i.armor && !i.slot.equals("offhand")) {
            int prot = d.nationGen.armordb.GetInteger(i.id, "prot", 0);
            return (prot >= target) != (not ^ below);
          }
        }
        return false;
      };
    }
  },

  THIS_ARMOR_ENC("thisarmorenc") {
    @Override
    Condition<ThemeIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      boolean below = args.nextOptionalFlag("below");
      int target = args.nextInt();

      return d -> {
        if (d.f instanceof Item) {
          Item i = (Item) d.f;

          if (i.armor && !i.slot.equals("offhand")) {
            int enc = d.nationGen.armordb.GetInteger(i.id, "enc", 0);
            return (enc >= target) != (not ^ below);
          }
        }
        return false;
      };
    }
  },

  THIS_ARMOR_DB("thisarmordb") {
    @Override
    Condition<ThemeIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      boolean below = args.nextOptionalFlag("below");
      String attribute = args.nextString();
      int target = args.nextInt();

      return d -> {
        if (d.f instanceof Item) {
          Item i = (Item) d.f;

          if (i.armor) {
            int value = d.nationGen.armordb.GetInteger(i.id, attribute);
            return (value >= target) != (not ^ below);
          }
        }
        return false;
      };
    }
  },

  THIS_WEAPON_DB("thisweapondb") {
    @Override
    Condition<ThemeIncData> parseConditionArguments(ArgParser args) {
      boolean not = args.nextOptionalFlag("not");
      boolean below = args.nextOptionalFlag("below");
      String attribute = args.nextString();
      int target = args.nextInt();

      return d -> {
        if (d.f instanceof Item) {
          Item i = (Item) d.f;

          if (!i.armor) {
            int value = d.nationGen.weapondb.GetInteger(i.id, attribute);
            return (value >= target) != (not ^ below);
          }
        }
        return false;
      };
    }
  };

  private static final Map<String, ThemeIncConditionType> LOOKUP;

  static {
    Map<String, ThemeIncConditionType> temp = new HashMap<>();
    for (ThemeIncConditionType type : values()) {
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

  ThemeIncConditionType(String name, String... alternateNames) {
    this.name = name;
    this.alternateNames = alternateNames;
  }

  abstract Condition<ThemeIncData> parseConditionArguments(ArgParser args);

  static Condition<ThemeIncData> create(Args args) {
    if (args.isEmpty()) {
      throw new IllegalArgumentException(
        "Condition<ThemeIncData> has no arguments."
      );
    }
    ArgParser parser = args.parse();
    String themeIncName = parser.nextString();
    return from(themeIncName)
      .orElseThrow(() ->
        new IllegalArgumentException(
          String.format(
            "No #themeinc condition type found with initial argument [%s]. Args: [%s].",
            themeIncName,
            args
          )
        )
      )
      .parseConditionArguments(parser);
  }

  public static Optional<ThemeIncConditionType> from(String name) {
    return Optional.ofNullable(LOOKUP.get(name));
  }
}
