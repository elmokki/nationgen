package nationGen.items;

import com.elmokki.Dom3DB;
import com.elmokki.Generic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import nationGen.chances.EntityChances;
import nationGen.entities.MagicItem;
import nationGen.misc.Arg;
import nationGen.misc.Args;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class CustomItemGen {

  Nation n;
  Random random;

  public CustomItemGen(Nation n) {
    this.n = n;
    random = new Random(n.random.nextInt());
  }

  public CustomItem getMagicItem(
    Unit u,
    Item olditem,
    int maxpower,
    double epicness,
    List<MagicItem> magicItems
  ) {
    boolean named = false;
    CustomItem i = this.getFromItem(olditem);
    if (i == null) return null;

    boolean ranged = n.nationGen.weapondb.GetInteger(olditem.id, "rng") != 0;
    boolean lowshots =
      n.nationGen.weapondb.GetInteger(olditem.id, "shots", 100) < 4;

    String type = ranged ? lowshots ? "lowshots" : "ranged" : "melee";

    if (random.nextDouble() > epicness) {
      List<MagicItem> possibles = new ArrayList<>();
      for (MagicItem m : magicItems) {
        boolean good = m.tags
          .getAllStrings("no")
          .stream()
          .noneMatch(type::equals);

        if (good && m.power <= maxpower) possibles.add(m);
      }

      if (possibles.size() > 0) {
        ChanceIncHandler chandler = new ChanceIncHandler(
          n,
          "customitemgenerator"
        );
        MagicItem mitem = chandler
          .handleChanceIncs(u, possibles)
          .getRandom(random);

        // Special looks
        List<Item> pos = new ArrayList<>();
        for (Args args : mitem.tags.getAllArgs("weapon")) {
          if (args.size() > 1 && args.get(0).get().equals(olditem.id)) {
            Item temp = u.pose
              .getItems(olditem.slot)
              .getItemWithName(args.get(1).get(), olditem.slot);
            if (temp != null) pos.add(temp);
          }
        }

        if (!pos.isEmpty()) {
          CustomItem miten =
            this.getFromItem(EntityChances.baseChances(pos).getRandom(random));
          if (miten != null) i = miten;
        }

        for (Command c : mitem.getCommands()) {
          String key = c.command;

          if (c.args.size() > 0) {
            Arg value = c.args.get(0);

            Optional<Integer> oldvalue = i.getIntValue(key);

            int temp = (int) value.applyModTo(oldvalue.orElseThrow());
            i.setValue(key, temp);
          } else {
            i.setValue(c.command);
          }
        }

        if (!mitem.effect.equals("-1")) i.setValue(
          "#secondaryeffect",
          mitem.effect
        );

        String name = n.nationGen.weapondb.GetValue(olditem.id, "weapon_name");

        if (mitem.nameSuffixes.size() > 0 || mitem.namePrefixes.size() > 0) {
          String part;
          int rand =
            random.nextInt(
              mitem.nameSuffixes.size() + mitem.namePrefixes.size()
            ) +
            1;
          if (
            rand > mitem.nameSuffixes.size() && mitem.nameSuffixes.size() > 0
          ) {
            part = mitem.nameSuffixes.get(
              random.nextInt(mitem.nameSuffixes.size())
            );
            name = Generic.capitalize(name + " " + part);
          } else {
            part = mitem.namePrefixes.get(
              random.nextInt(mitem.namePrefixes.size())
            );
            name = Generic.capitalize(part + " " + name);
          }

          named = true;
          i.setValue("#name", name);
        }

        i.magicItem = mitem;

        // Increase gcost
        for (Args args : mitem.tags.getAllArgs("gcost")) {
          if (args.get(0).get().equals(type)) i.commands.add(
            new Command("#gcost", args.get(1))
          );
        }
        for (Args args : mitem.tags.getAllArgs("rcost")) {
          if (args.get(0).get().equals(type)) i.commands.add(
            new Command("#rcost", args.get(1))
          );
        }
        i.tags.addAll(mitem.tags);
      }
    }

    int runs = 1 + random.nextInt(1); // 1-2
    if (random.nextDouble() > epicness) runs++;
    if (random.nextDouble() > epicness / 2) runs++;
    if (random.nextDouble() > epicness / 4) runs++;

    boolean magic = false;
    if (!ranged && i.magicItem != null && random.nextDouble() > 0.75) {
      i.setValue("#magic");
      magic = true;
    }

    // Add gold cost
    int potentialgcost = runs;
    if (magic) potentialgcost = Math.max(
      (int) Math.round(1.5 * (double) (potentialgcost)),
      3
    );

    int gcost = u.getGoldCost();
    if (gcost * 0.1 < potentialgcost) u.commands.add(
      Command.args("#gcost", "+" + potentialgcost)
    );
    else u.commands.add(Command.args("#gcost", "*1.1"));

    // Add res cost
    int potentialrcost = runs;
    if (magic) potentialrcost = Math.max(
      (int) Math.round(1.5 * (double) (potentialrcost)),
      3
    );

    int rcost = u.getResCost(true);
    if (rcost * 0.1 < potentialrcost) u.commands.add(
      Command.args("#rcost", "+" + potentialrcost)
    );
    else u.commands.add(Command.args("#rcost", "*1.1"));

    double[] chances = { 1, 1, 1, 1 };
    while (runs > 0) {
      int sum = 0;
      for (int j = 0; j < chances.length; j++) sum += chances[j];

      double rand = random.nextDouble() * sum;

      if (ranged) chances[3] = 0;
      if (ranged || lowshots) chances[1] = 0;

      if ((runs <= 2 && ranged)) chances[0] = 0;

      if ((rand <= chances[0] && !ranged) || (runs > 2 && ranged)) {
        chances[0] *= 0.33;
        int att = i.getIntValue("#att").orElseThrow();
        att++;
        i.setValue("#att", att);

        if (ranged && !lowshots) runs -= 2;

        runs--;
      } else if (rand <= chances[0] + chances[1] && !ranged) {
        chances[1] *= 0.33;
        int def = i.getIntValue("#def").orElseThrow();
        def++;
        i.setValue("#def", def);
        runs--;
      } else if (rand <= chances[0] + chances[1] + chances[2]) {
        chances[2] *= 0.33;
        int dmg = i.getIntValue("#dmg").orElseThrow();
        if (
          dmg < 63
        ) dmg++; // Weapons with dmg 64+ are (with one exception, the Deadliest Poison at 75) not actually damage, but instead special effects encoded as damage, so we don't want to screw them up
        i.setValue("#dmg", dmg);
        runs--;
      } else if (
        rand <= chances[0] + chances[1] + chances[2] + chances[3] &&
        runs > 1 &&
        !ranged &&
        !magic
      ) {
        chances[3] = 0;
        i.setValue("#magic");
        magic = true;
        runs--;
      }
    }
    String name = n.nationGen.weapondb.GetValue(olditem.id, "weapon_name");

    if (!magic && (i.magicItem == null || !named)) i.setValue(
      "#name",
      "Exceptional " + name
    );
    else if (magic && (i.magicItem == null || !named)) i.setValue(
      "#name",
      "Enchanted " + name
    );

    String dname =
      "nation_" + n.nationid + "_customitem_" + (n.customitems.size() + 1);
    i.id = dname;
    i.name = dname;

    if (i.magicItem != null) i.tags.addAll(i.magicItem.tags);

    n.customitems.add(i);
    n.nationGen.GetCustomItemsHandler().AddCustomItem(i);
    n.nationGen.weapondb.addToMap(i.id, i.getHashMap());

    return i;
  }

  public CustomItem getFromItem(Item item) {
    if (item == null) {
      return null;
    }

    if (!Generic.isNumeric(item.id)) return null;

    CustomItem newitem = new CustomItem(n.nationGen);
    newitem.sprite = item.sprite;
    newitem.mask = item.mask;
    newitem.commands.addAll(item.commands);
    newitem.tags.addAll(item.tags);
    newitem.dependencies.addAll(item.dependencies);
    newitem.setOffsetX(item.getOffsetX());
    newitem.setOffsetY(item.getOffsetY());
    newitem.slot = item.slot;
    newitem.basechance = item.basechance;
    newitem.renderslot = item.renderslot;
    newitem.renderprio = item.renderprio;
    newitem.armor = item.armor;
    newitem.olditem = item;

    Dom3DB db = null;
    if (item.armor) db = n.nationGen.armordb;
    else db = n.nationGen.weapondb;

    if (!item.armor) {
      List<String> boolargs = db.getBooleanArgs();

      if (db.GetValue(item.id, "weapon_name").equals("")) {
        return null;
      }

      newitem.setValue("#att", 0);
      newitem.setValue("#len", 0);
      newitem.setValue("#dmg", 0);

      for (String def : db.getDefinition()) {
        if (def.equals("id")) {
          // do nothing
        } else if (def.equals("weapon_name")) {
          newitem.setValue("#name", db.GetValue(item.id, "weapon_name"));
        } else if (def.equals("res")) {
          newitem.setValue("#rcost", db.GetValue(item.id, "res"));
        } else if (def.equals("dt_blunt")) {
          if (db.GetValue(item.id, def).equals("1")) newitem.setValue("#blunt");
        } else if (def.equals("dt_slash")) {
          if (db.GetValue(item.id, def).equals("1")) newitem.setValue("#slash");
        } else if (def.equals("dt_pierce")) {
          if (db.GetValue(item.id, def).equals("1")) newitem.setValue(
            "#pierce"
          );
        } else if (def.equals("lgt")) {
          if (db.GetInteger(item.id, "lgt") > 0) newitem.setValue(
            "#len",
            db.GetValue(item.id, "lgt")
          );
        } else if (def.equals("rng")) {
          if (db.GetInteger(item.id, "rng") != 0) {
            newitem.setValue("#range", db.GetValue(item.id, "rng"));
          }
        } else if (def.equals("#att")) {
          if (db.GetInteger(item.id, "#att") != 1) newitem.setValue(
            "#nratt",
            db.GetValue(item.id, "#att")
          );
        } else if (def.equals("shots")) {
          if (db.GetInteger(item.id, "shots") > 0) newitem.setValue(
            "#ammo",
            db.GetValue(item.id, "shots")
          );
        } else if (def.equals("2h")) {
          if (db.GetValue(item.id, "2h").equals("1")) newitem.setValue(
            "#twohanded"
          );
        } else if (def.equals("flyspr")) {
          if (!db.GetValue(item.id, "flyspr", "derp").equals("derp")) {
            String flyspr = db.GetValue(item.id, "flyspr");
            String speed = db.GetValue(item.id, "animlength", "1");

            newitem.setValue("#flyspr", flyspr, speed);
          }
        }
        // Obsolete?
        else if (def.equals("onestrike")) {
          if (db.GetValue(item.id, "onestrike").equals("1")) newitem.setValue(
            "#ammo",
            "1"
          );
        } else if (def.equals("ap")) {
          if (db.GetValue(item.id, "ap").equals("1")) newitem.setValue(
            "#armorpiercing"
          );
        } else if (def.equals("an")) {
          if (db.GetValue(item.id, "an").equals("1")) newitem.setValue(
            "#armornegating"
          );
        }
        // Skippable stuff
        else if (def.equals("effect_record_id"));
        else if (
          def.equals("secondaryeffect") && db.GetValue(item.id, def).equals("0")
        );
        else if (
          def.equals("secondaryeffectalways") &&
          db.GetValue(item.id, def).equals("0")
        );
        else if (def.equals("animlength"));
        else if (def.equals("dt_norm"));
        else if (def.equals("aoe") && db.GetValue(item.id, def).equals("0"));
        // Generic handle for boolean args
        else if (boolargs.contains(def)) {
          if (db.GetValue(item.id, def).equals("1")) {
            newitem.setValue("#" + def);
          }
        }
        // Handle non-boolean args
        else if (!db.GetValue(item.id, def).equals("")) {
          newitem.setValue("#" + def, db.GetValue(item.id, def));
        }
      }

      // No magic item from spc damage items
      if (
        newitem.getStringValue("#dmg").orElseThrow().equals("spc") ||
        item.id.equals("-1")
      ) return null;
    } else {
      // ARMOR, not done
    }

    return newitem;
  }
}
