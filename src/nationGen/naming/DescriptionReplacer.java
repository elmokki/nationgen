package nationGen.naming;

import com.elmokki.Generic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import nationGen.entities.Filter;
import nationGen.items.Item;
import nationGen.magic.MagicPath;
import nationGen.magic.MagicPathInts;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class DescriptionReplacer {

  public HashMap<String, String> descs = new HashMap<String, String>();
  private Nation n;

  public DescriptionReplacer(Nation n) {
    this.n = n;
    descs.put("%nation%", n.name);
    descs.put("%primaryrace%", n.races.get(0).toString());
    descs.put(
      "%primaryrace_plural%",
      Generic.capitalizeFirst(n.races.get(0).toString())
    );
    descs.put("%mages%", MageDescriber.getCommonName(n).toString());
    descs.put(
      "%mages_plural%",
      Generic.plural(MageDescriber.getCommonName(n).toString())
    );
    descs.put("%magenoun%", MageDescriber.getCommonNoun(n).toString());
    descs.put(
      "%sacredname%",
      n.unitlists.get("sacreds").get(0).name.toString()
    );
    descs.put(
      "%sacredname_plural%",
      Generic.capitalizeFirst(n.unitlists.get("sacreds").get(0).name.toString())
    );
  }

  public String replace(String line) {
    for (String key : descs.keySet()) line = line.replaceAll(
      key,
      descs.get(key)
    );

    return line;
  }

  public void calibrate(Unit u) {
    descs.put("%unitname%", u.name.toString());
    descs.put("%unitname_plural%", Generic.plural(u.name.toString()));
    descs.put("%race%", u.race.toString().toLowerCase());
    descs.put("%race_plural%", Generic.plural(u.race.toString().toLowerCase()));

    if (u.getSlot("mount") != null) {
      Optional<String> animal = u.getSlot("mount").tags.getString("animal");
      if (animal.isEmpty() || animal.get().isEmpty()) {
        throw new IllegalStateException(
          "Unit " + u + " doesn't have an animal tag specified for its mount!"
        );
      }
      descs.put("%mount%", animal.get());
      descs.put("%mount_plural%", Generic.plural(animal.get()));
    } else {
      descs.put("%mount%", "unspecified mount");
      descs.put("%mount_plural%", "unspecified mounts");
    }

    if (u.hasLeaderLevel("")) {
      descs.put("%role%", "commanders");

      if (u.hasCommand("#holy")) {
        descs.put("%role%", "priests");
      }
    }

    for (Filter f : u.getMagicFilters()) {
      if (f.name.equals("MAGICPICKS")) {
        descs.put("%role%", "mages");
      }
    }

    if (u.hasCommand("#fixedname")) {
      for (Command c : u.getCommands()) {
        if (c.command.equals("#fixedname")) descs.put(
          "%fixedname%",
          c.args.get(0).get()
        );
      }

      descs.put("%tobe%", "is");
      descs.put("%tohave%", "has");

      if (u.hasCommand("#female")) {
        descs.put("%Pronoun%", "She");
        descs.put("%pronoun%", "she");
        descs.put("%pronoun_obj%", "her");
        descs.put("%pronoun_pos%", "her");
      } else {
        descs.put("%Pronoun%", "He");
        descs.put("%pronoun%", "he");
        descs.put("%pronoun_obj%", "him");
        descs.put("%pronoun_pos%", "his");
      }
    } else {
      descs.put("%Pronoun%", "They");
      descs.put("%pronoun%", "they");
      descs.put("%pronoun_obj%", "them");
      descs.put("%pronoun_pos%", "their");
      descs.put("%tobe%", "are");
      descs.put("%tohave%", "have");
      descs.put("%fixedname%", u.name.toString());
    }
  }

  public void calibrate(List<Unit> units) {
    List<String> weapons = new ArrayList<String>();
    List<String> weapons_plural = new ArrayList<String>();
    String[] slots = { "weapon", "offhand" };

    for (Unit u : units) {
      for (String str : slots) {
        Item i = u.getSlot(str);
        if (i != null && !i.armor && !i.id.equals("-1")) {
          if (
            !n.nationGen.weapondb.GetValue(i.id, "weapon_name").equals("") &&
            !weapons.contains(
              n.nationGen.weapondb.GetValue(i.id, "weapon_name")
            )
          ) {
            weapons.add(
              NameGenerator.addPreposition(
                n.nationGen.weapondb.GetValue(i.id, "weapon_name").toLowerCase()
              )
            );
            weapons_plural.add(
              NameGenerator.plural(
                n.nationGen.weapondb.GetValue(i.id, "weapon_name").toLowerCase()
              )
            );
          } else if (
            !weapons.contains(
              n.nationGen.weapondb.GetValue(i.id, "weapon_name")
            )
          ) {
            weapons.add("NOT IN WEAPONDB: " + i.name + " in " + i.slot);
            weapons_plural.add("NOT IN WEAPONDB: " + i.name + " in " + i.slot);
          }
        }
      }
    }
    if (weapons.size() > 3) {
      weapons.clear();
      weapons_plural.clear();

      for (Unit u : units) {
        for (String slot : slots) {
          Item i = u.getSlot(slot);
          if (i != null && !i.armor && !i.id.equals("-1")) {
            if (
              !n.nationGen.weapondb.GetValue(i.id, "weapon_name").equals("") &&
              !weapons.contains(
                n.nationGen.weapondb.GetValue(i.id, "weapon_name")
              )
            ) {
              List<String> tmp = new ArrayList<String>();

              if (
                n.nationGen.weapondb.GetValue(i.id, "dt_blunt").equals("1")
              ) tmp.add("blunt");
              if (
                n.nationGen.weapondb.GetValue(i.id, "dt_slash").equals("1")
              ) tmp.add("slashing");
              if (
                n.nationGen.weapondb.GetValue(i.id, "dt_pierce").equals("1")
              ) tmp.add("piercing");

              for (String str : tmp) if (
                !str.equals("") && !weapons.contains(str)
              ) weapons.add(str);
            }
          }
        }
      }

      if (weapons.size() < 3) {
        descs.put(
          "%weapons%",
          NameGenerator.writeAsList(weapons, false) + " weapons"
        );
        descs.put(
          "%weapons_plural%",
          NameGenerator.writeAsList(weapons, false) + " weapons"
        );
      } else {
        descs.put("%weapons%", "various weapons");
        descs.put("%weapons_plural%", "various weapons");
      }
    } else {
      descs.put("%weapons%", NameGenerator.writeAsList(weapons, false, "or"));
      descs.put(
        "%weapons_plural%",
        NameGenerator.writeAsList(weapons_plural, false)
      );
    }

    weapons.clear();
    int minprot = 100;
    int maxprot = 0;

    for (Unit u : units) {
      Item i = u.getSlot("armor");

      int prot = 0;

      if (i != null && i.armor && !i.id.equals("-1")) {
        prot = n.nationGen.armordb.GetInteger(i.id, "prot", 0);

        if (
          !n.nationGen.armordb.GetValue(i.id, "armorname").equals("") &&
          !weapons.contains(n.nationGen.armordb.GetValue(i.id, "armorname"))
        ) {
          weapons.add(n.nationGen.armordb.GetValue(i.id, "armorname"));
        } else if (
          !weapons.contains(n.nationGen.armordb.GetValue(i.id, "armorname"))
        ) {
          weapons.add("NOT IN ARMORDB: " + i.name + " in " + i.slot);
        }
      }

      if (prot < minprot) minprot = prot;
      if (prot > maxprot) maxprot = prot;
    }

    descs.put("%armors%", NameGenerator.writeAsList(weapons, false));

    weapons.clear();

    int eraModifier = 1;

    //late era
    if ((n.nationGen.settings.getSettingInteger() & 2) == 2) eraModifier += 1;

    //early era
    if ((n.nationGen.settings.getSettingInteger() & 1) == 1) eraModifier -= 1;

    if (minprot <= 11 + eraModifier) weapons.add("light");

    if (minprot <= 16 + eraModifier && maxprot > 11 + eraModifier) weapons.add(
      "medium"
    );

    if (maxprot > 16 + eraModifier) weapons.add("heavy");

    if (weapons.size() >= 3 || weapons.size() == 0) {
      weapons.clear();
      weapons.add("all kinds of");
    }

    descs.put("%armortypes%", NameGenerator.writeAsList(weapons, false));
  }
}
