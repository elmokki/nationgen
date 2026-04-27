package nationGen.unrelated;

import com.elmokki.NationGenDB;
import com.elmokki.Generic;
import java.util.ArrayList;
import java.util.List;

import nationGen.items.ItemProperty;
import nationGen.misc.FileUtil;

/**
 * Converts LarzM's mod inspector database files to weapons/armor files for nationgen
 * You'll need to convert the .csvs to semicolon-separated first.
 * @author Elmokki
 *
 */
public class DatabaseConverter {

  public static void main(String[] args) {
    // Weapons
    NationGenDB previousWeapons = new NationGenDB("/db/nationgen/weapons.csv");
    NationGenDB inspectorWeapons = new NationGenDB("/db/inspector/weapons.csv");

    addAttributes(inspectorWeapons, "/db/inspector/attributes_by_weapon.csv");
    addEffects(inspectorWeapons);
    inspectorWeapons.removeColumn("end");
    inspectorWeapons.removeColumn("weapon");
    inspectorWeapons.setColumn(
      ItemProperty.FLYSPRITE.toDBColumn(),
      previousWeapons.getColumnAsMap(ItemProperty.FLYSPRITE.toDBColumn())
    );
    inspectorWeapons.setColumn(
      ItemProperty.ANIM_LENGTH.toDBColumn(),
      previousWeapons.getColumnAsMap(ItemProperty.ANIM_LENGTH.toDBColumn())
    );
    inspectorWeapons.setDefinition(previousWeapons.getDefinition());

    inspectorWeapons.saveToFile("/db/nationgen/weapons.csv");

    // Armor
    NationGenDB previousArmor = new NationGenDB("/db/nationgen/armors.csv");
    NationGenDB armor = new NationGenDB("/db/inspector/armors.csv");

    addAttributes(armor, "/db/inspector/attributes_by_armor.csv");
    addArmorProt(armor);
    armor.removeColumn("end");
    armor.setDefinition(previousArmor.getDefinition());

    armor.saveToFile("/db/nationgen/armors.csv");
  }

  /**
   * Adds armor protection
   *
   * ...turns out cuirasses add limb protection etc and the formula is
   * (chest + mean_limb) / 2
   *
   * @param db
   */
  private static void addArmorProt(NationGenDB db) {
    List<String> fileLines = FileUtil.readLines(
      "/db/inspector/protections_by_armor.csv"
    );

    fileLines.remove(0);

    List<String> lines = new ArrayList<>();
    // Let's read the unit data then.

    for (String line : fileLines) {
      // Set units[id] to line of that unit.
      if (line.length() > 0 && line.split(";").length > 0) lines.add(line);
      else if (line.length() > 0 && line.split("\t").length > 0) lines.add(
        line
      );
    }

    for (String key : db.entryMap.keySet()) {
      int[] zone = new int[7];
      for (String str : lines) {
        String[] stuff = str.split(";");
        if (stuff.length < 2) stuff = str.split("\t");

        if (stuff[2].equals(key)) {
          int zonenbr = Integer.parseInt(stuff[0]);
          zone[zonenbr] = Integer.parseInt(stuff[1]);
        }
      }

      int prot = 0;

      // misc
      if (zone[6] != 0) prot = zone[6];

      // head
      if (zone[1] != 0) prot = zone[1];

      // shield
      if (zone[5] != 0) prot = zone[5];

      // others

      int tempprot = (zone[2] + (zone[3] + zone[4]) / 2) / 2;
      if (tempprot > 0) prot = tempprot;

      db.setValue(key, prot + "", ItemProperty.PROTECTION.toDBColumn());
    }
  }

  /**
   * Handles attributes_by_[thing].csv
   * @param db
   * @param fname
   */
  private static void addAttributes(NationGenDB db, String fname) {
    for (String key : db.entryMap.keySet()) {
      db.setValue(key, "0", ItemProperty.IS_IRON_ARMOR.toDBColumn());
      db.setValue(key, "0", ItemProperty.IS_WOOD_ARMOR.toDBColumn());
      db.setValue(key, "0", ItemProperty.MM_PENALTY.toDBColumn());
    }

    List<String> lines = FileUtil.readLines(fname);

    lines.remove(0);

    for (String ssgd : lines) {
      String[] line = ssgd.split("\t");

      if (!db.entryMap.keySet().contains(line[0])) continue;

      String key = line[0];
      String attr = line[1];

      if (!"".equals(attr)) {
        if (attr.equals("266") || attr.equals("267")) db.setValue(
          key,
          "1",
          ItemProperty.IS_IRON_ARMOR.toDBColumn()
        );

        if (attr.equals("268") || attr.equals("269")) db.setValue(
          key,
          "1",
          ItemProperty.IS_WOOD_ARMOR.toDBColumn()
        );

        if (attr.equals("582")) {
          db.setValue(key, line[2], ItemProperty.MM_PENALTY.toDBColumn());
        }
      }
    }
  }

  /**
   * Adds effects for weapons from effects_weapons.csv
   * @param db
   */
  private static void addEffects(NationGenDB db) {
    NationGenDB attr = new NationGenDB("/db/inspector/effects_weapons.csv");

    for (String key : db.entryMap.keySet()) {
      String attr_id = db.GetValue(key, "effect_record_id");

      // Damage
      db.setValue(
        key, attr.GetValue(attr_id, "raw_argument"),
        ItemProperty.DAMAGE.toDBColumn()
      );

      // Effect numbers
      int effnbr = Integer.parseInt(attr.GetValue(attr_id, "effect_number"));

      if (effnbr == 2) db.setValue(key, "1", ItemProperty.DT_NORM.toDBColumn());
      else db.setValue(key, "0", ItemProperty.DT_NORM.toDBColumn());

      if (effnbr == 3) db.setValue(key, "1", ItemProperty.DT_STUN.toDBColumn());
      else db.setValue(key, "0", ItemProperty.DT_STUN.toDBColumn());

      if (effnbr == 7) db.setValue(key, "1", ItemProperty.DT_POISON.toDBColumn());
      else db.setValue(key, "0", ItemProperty.DT_POISON.toDBColumn());

      if (effnbr == 24) db.setValue(key, "1", ItemProperty.DT_HOLY.toDBColumn());
      else db.setValue(key, "0", ItemProperty.DT_HOLY.toDBColumn());

      if (effnbr == 32) db.setValue(key, "1", ItemProperty.DT_LARGE.toDBColumn());
      else db.setValue(key, "0", ItemProperty.DT_LARGE.toDBColumn());

      if (effnbr == 33) db.setValue(key, "1", ItemProperty.DT_SMALL.toDBColumn());
      else db.setValue(key, "0", ItemProperty.DT_SMALL.toDBColumn());

      if (effnbr == 66) db.setValue(key, "1", ItemProperty.DT_PARALYZE.toDBColumn());
      else db.setValue(key, "0", ItemProperty.DT_PARALYZE.toDBColumn());

      if (effnbr == 67) db.setValue(key, "1", ItemProperty.DT_WEAKNESS.toDBColumn());
      else db.setValue(key, "0", ItemProperty.DT_WEAKNESS.toDBColumn());

      if (effnbr == 73) db.setValue(key, "1", ItemProperty.DT_MAGIC_BEING.toDBColumn());
      else db.setValue(key, "0", ItemProperty.DT_MAGIC_BEING.toDBColumn());

      if (effnbr == 74) db.setValue(key, "1", ItemProperty.DT_RAISE.toDBColumn());
      else db.setValue(key, "0", ItemProperty.DT_RAISE.toDBColumn());

      if (effnbr == 103) db.setValue(key, "1", ItemProperty.DT_DRAIN.toDBColumn());
      else db.setValue(key, "0", ItemProperty.DT_DRAIN.toDBColumn());

      if (effnbr == 104) db.setValue(key, "1", ItemProperty.DT_WEAPON_DRAIN.toDBColumn());
      else db.setValue(key, "0", ItemProperty.DT_WEAPON_DRAIN.toDBColumn());

      if (effnbr == 107) db.setValue(key, "1", ItemProperty.DT_DEMON.toDBColumn());
      else db.setValue(key, "0", ItemProperty.DT_DEMON.toDBColumn());

      if (effnbr == 109) db.setValue(key, "1", ItemProperty.DT_CAPPED.toDBColumn());
      else db.setValue(key, "0", ItemProperty.DT_CAPPED.toDBColumn());

      // Effect bitmasks

      long effbm = Long.parseLong(attr.GetValue(attr_id, "modifiers_mask"));

      if (Generic.containsLongBitmask(effbm, 1)) {
        db.setValue(key, "0", ItemProperty.NO_STR.toDBColumn());
        db.setValue(key, "0", ItemProperty.BOW_STR.toDBColumn());
      } else {
        if (
          !Generic.containsLongBitmask(effbm, 134217728) &&
          Integer.parseInt(attr.GetValue(attr_id, "range_base", "0")) > 0
        ) {
          db.setValue(key, "0", ItemProperty.NO_STR.toDBColumn());
          db.setValue(key, "1", ItemProperty.BOW_STR.toDBColumn());
        } else {
          db.setValue(key, "1", ItemProperty.NO_STR.toDBColumn());
          db.setValue(key, "0", ItemProperty.BOW_STR.toDBColumn());
        }
      }
      if (Generic.containsLongBitmask(effbm, 2)) db.setValue(key, "1", ItemProperty.IS_2H.toDBColumn());
      else db.setValue(key, "0", ItemProperty.IS_2H.toDBColumn());

      if (Generic.containsLongBitmask(effbm, 4)) db.setValue(key, "1", ItemProperty.IS_FLAIL.toDBColumn());
      else db.setValue(key, "0", ItemProperty.IS_FLAIL.toDBColumn());

      if (Generic.containsLongBitmask(effbm, 8)) db.setValue(
        key,
        "1",
        ItemProperty.DEMON_ONLY.toDBColumn()
      );
      else db.setValue(key, "0", ItemProperty.DEMON_ONLY.toDBColumn());

      if (Generic.containsLongBitmask(effbm, 32)) db.setValue(key, "1", ItemProperty.DT_FIRE.toDBColumn());
      else db.setValue(key, "0", ItemProperty.DT_FIRE.toDBColumn());

      if (Generic.containsLongBitmask(effbm, 64)) db.setValue(key, "1", ItemProperty.AP.toDBColumn());
      else db.setValue(key, "0", ItemProperty.AP.toDBColumn());

      if (Generic.containsLongBitmask(effbm, 128)) db.setValue(key, "1", ItemProperty.AN.toDBColumn());
      else db.setValue(key, "0", ItemProperty.AN.toDBColumn());

      if (Generic.containsLongBitmask(effbm, 512)) db.setValue(
        key,
        "1",
        ItemProperty.DT_COLD.toDBColumn()
      );
      else db.setValue(key, "0", ItemProperty.DT_COLD.toDBColumn());

      if (Generic.containsLongBitmask(effbm, 2048)) db.setValue(
        key,
        "1",
        ItemProperty.DT_SHOCK.toDBColumn()
      );
      else db.setValue(key, "0", ItemProperty.DT_SHOCK.toDBColumn());

      if (Generic.containsLongBitmask(effbm, 4096)) db.setValue(
        key,
        "1",
        ItemProperty.MR_NEGATES.toDBColumn()
      );
      else db.setValue(key, "0", ItemProperty.MR_NEGATES.toDBColumn());

      if (Generic.containsLongBitmask(effbm, 32768)) db.setValue(
        key,
        "1",
        ItemProperty.SACRED_ONLY.toDBColumn()
      );
      else db.setValue(key, "0", ItemProperty.SACRED_ONLY.toDBColumn());

      if (Generic.containsLongBitmask(effbm, 131072)) db.setValue(
        key,
        "1",
        ItemProperty.DT_MIND.toDBColumn()
      );
      else db.setValue(key, "0", ItemProperty.DT_MIND.toDBColumn());

      if (Generic.containsLongBitmask(effbm, 2097152)) db.setValue(
        key,
        "0",
        ItemProperty.IS_MAGIC_WEAPON.toDBColumn()
      );
      else db.setValue(key, "1", ItemProperty.IS_MAGIC_WEAPON.toDBColumn());

      if (Generic.containsLongBitmask(effbm, 134217728)) db.setValue(
        key,
        "1",
        ItemProperty.INTRINSIC.toDBColumn()
      );
      else db.setValue(key, "0", ItemProperty.INTRINSIC.toDBColumn());

      if (Generic.containsLongBitmask(effbm, 2147483648L)) db.setValue(
        key,
        "1",
        ItemProperty.CHARGE_BONUS.toDBColumn()
      );
      else db.setValue(key, "0", ItemProperty.CHARGE_BONUS.toDBColumn());

      if (Generic.containsLongBitmask(effbm, 137438953472L)) db.setValue(
        key,
        "1",
        ItemProperty.NO_REPEL.toDBColumn()
      );
      else db.setValue(key, "0", ItemProperty.NO_REPEL.toDBColumn());

      if (Generic.containsLongBitmask(effbm, 274877906944L)) db.setValue(
        key,
        "1",
        ItemProperty.DT_PIERCE.toDBColumn()
      );
      else db.setValue(key, "0", ItemProperty.DT_PIERCE.toDBColumn());

      if (Generic.containsLongBitmask(effbm, 549755813888L)) db.setValue(
        key,
        "1",
        ItemProperty.DT_BLUNT.toDBColumn()
      );
      else db.setValue(key, "0", ItemProperty.DT_BLUNT.toDBColumn());

      if (Generic.containsLongBitmask(effbm, 1099511627776L)) db.setValue(
        key,
        "1",
        ItemProperty.DT_SLASH.toDBColumn()
      );
      else db.setValue(key, "0", ItemProperty.DT_SLASH.toDBColumn());

      if (Generic.containsLongBitmask(effbm, 2199023255552L)) db.setValue(
        key,
        "1",
        ItemProperty.DT_ACID.toDBColumn()
      );
      else db.setValue(key, "0", ItemProperty.DT_ACID.toDBColumn());

      if (Generic.containsLongBitmask(effbm, 4398046511104L)) db.setValue(
        key,
        "1",
        ItemProperty.SIZE_RESIST.toDBColumn()
      );
      else db.setValue(key, "0", ItemProperty.SIZE_RESIST.toDBColumn());

      // Misc
      db.setValue(key, attr.GetValue(attr_id, "range_base"), ItemProperty.RANGE.toDBColumn());

      if (!attr.GetValue(attr_id, "range_strength_divisor").equals("")) {
        db.setValue(
          key,
          "-" + attr.GetValue(attr_id, "range_strength_divisor"),
          ItemProperty.RANGE.toDBColumn()
        );
      }

      db.setValue(key, attr.GetValue(attr_id, "area_base"), ItemProperty.AOE.toDBColumn());
      db.setValue(
        key,
        attr.GetValue(attr_id, "flight_sprite_number"),
        ItemProperty.FLYSPRITE.toDBColumn()
      );
      db.setValue(
        key,
        attr.GetValue(attr_id, "flight_sprite_length"),
        ItemProperty.ANIM_LENGTH.toDBColumn()
      );

      if (db.GetValue(key, "ammo").equals("1")) db.setValue(
        key,
        "1",
        "onestrike"
      );
      else db.setValue(key, "0", "onestrike");
    }
  }
}
