package nationGen.rostergeneration;

import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.chances.ThemeInc;
import nationGen.entities.Filter;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.misc.Command;
import nationGen.misc.ItemSet;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class ScoutGenerator extends TroopGenerator {

  public ScoutGenerator(NationGen g, Nation n, NationGenAssets assets) {
    super(g, n, assets, "scoutgen");
  }

  public Unit generateScout(Race race) {
    String posename = "infantry";
    if (race.hasRole("scout")) posename = "scout";

    Random r = new Random(nation.random.nextInt());

    // Select whether it is a spy, scout or assassin
    double scoutchance = race.tags.getDouble("scoutchance").orElse(0.5);
    double spychance = race.tags.getDouble("spychance").orElse(0.15);
    double assassinchance = race.tags.getDouble("assassinchance").orElse(0.1);

    double all = scoutchance + spychance + assassinchance;
    double roll = r.nextDouble() * all;

    int tier;
    if (roll < assassinchance) tier = 3;
    else if (roll < spychance + assassinchance) tier = 2;
    else tier = 1;

    // Get possible poses
    List<Pose> possiblePoses = race
      .getPoses(posename)
      .stream()
      .filter(p -> tier != 1 || !p.tags.containsName("cannot_be_scout"))
      .filter(p -> tier != 2 || !p.tags.containsName("cannot_be_spy"))
      .filter(p -> tier != 3 || !p.tags.containsName("cannot_be_assassin"))
      .collect(Collectors.toList());

    // Select pose
    Pose p = chandler.getRandom(possiblePoses, race);
    Unit template = unitGen.generateUnit(race, p);

    chandler.getRandom(
      p.getItems("weapon").filterDom3DB("2h", "0", true, nationGen.weapondb),
      template
    );
    // Select a mainhand weapon
    Item weapon;
    if (
      tier != 3
    ) weapon = chandler.getRandom( //  Scout/spy gets whatever non-2h
      p.getItems("weapon").filterDom3DB("2h", "0", true, nationGen.weapondb),
      template
    );
    else { // Assassin gets max length 3 weapons
      weapon = chandler.getRandom(
        p
          .getItems("weapon")
          .filterDom3DBInteger("lgt", 3, true, nationGen.weapondb),
        template
      );
    }
    // Failsafe
    if (weapon == null) {
      weapon = chandler.getRandom(p.getItems("weapon"), template);
    }

    // Chance to give a whatever weapon rarely
    if (r.nextDouble() > 0.9 + (tier - 1) * 0.25) {
      Item tweapon = chandler.getRandom(p.getItems("weapon"), template);
      if (tweapon != null) weapon = tweapon;
    }

    this.addStats(template, tier);
    unitGen.addFreeTemplateFilters(template);

    template.setSlot("weapon", weapon);
    //template.setSlot("shirt", template.pose.getItems("shirt").getRandom(nation.random));

    Item armor;
    if (posename.equals("scout") && p.roles.size() == 1) {
      armor = nation.usedItems
        .filterSlot("armor")
        .filterForPose(p)
        .getRandom(chandler, template, nation.random);
      if (armor == null) armor = chandler.getRandom(
        p.getItems("armor"),
        template
      );
    } else {
      armor = nation.usedItems
        .filterSlot("armor")
        .filterForPose(p)
        .filterProt(nationGen.armordb, 0, 10)
        .getRandom(chandler, template, nation.random);
      if (armor == null) armor = chandler.getRandom(
        p.getItems("armor").filterProt(nationGen.armordb, 0, 10),
        template
      );
      if (armor == null) armor = chandler.getRandom(
        p.getItems("armor").filterProt(nationGen.armordb, 0, 12),
        template
      );
      if (armor == null) armor = chandler.getRandom(
        p.getItems("armor"),
        template
      );
    }

    if (armor == null) {
      System.out.println(
        "Error giving scout an armor. Pose " +
        p.name +
        " / " +
        p.roles +
        " of race " +
        race.name
      );
      return null;
    }

    template.setSlot("armor", armor);

    int prot = nationGen.armordb.GetInteger(
      template.getSlot("armor").id,
      "prot"
    );

    if (template.pose.getItems("helmet") != null) {
      ItemSet helms = template.pose.getItems("helmet");
      Item helmet = chandler
        .handleChanceIncs(
          template,
          helms,
          unitGen.generateTargetProtChanceIncs(prot, 4)
        )
        .getRandom(nation.random);

      template.setSlot("helmet", helmet);
    }

    if (
      r.nextDouble() < 0.15 && template.pose.getItems("bonusweapon") != null
    ) {
      Item bweapon = chandler.getRandom(
        nation.usedItems
          .filterSlot("bonusweapon")
          .filterOutTag(new Command("noelite"))
          .filterMinMaxProt(template.getTotalProt(false)),
        template
      );
      template.setSlot("bonusweapon", bweapon);

      if (template.getSlot("bonusweapon") == null) {
        bweapon = chandler.getRandom(
          template.pose
            .getItems("bonusweapon")
            .filterOutTag(new Command("noelite"))
            .filterMinMaxProt(template.getTotalProt(false)),
          template
        );
        template.setSlot("bonusweapon", bweapon);
      }
    }

    double dwchance = 0.3;

    if (tier == 3) dwchance = 1;

    if (
      p.getItems("offhand") != null &&
      r.nextDouble() < dwchance &&
      p.getItems("offhand").filterArmor(false).size() > 0 &&
      nationGen.weapondb.GetInteger(template.getSlot("weapon").id, "lgt") < 3 &&
      nationGen.weapondb.GetInteger(template.getSlot("weapon").id, "2h") == 0
    ) {
      if (
        r.nextDouble() > 0.25 &&
        p
          .getItems("offhand")
          .filterArmor(false)
          .getItemWithID(weapon.id, "offhand") !=
        null
      ) {
        template.setSlot(
          "offhand",
          chandler.getRandom(
            p
              .getItems("offhand")
              .filterArmor(false)
              .getItemsWithID(weapon.id, "offhand"),
            template
          )
        );
      } else {
        template.setSlot(
          "offhand",
          chandler.getRandom(p.getItems("offhand").filterArmor(false), template)
        );
      }
    }

    if (p.getItems("cloakb") != null) template.setSlot(
      "cloakb",
      chandler.getRandom(
        p.getItems("cloakb").filterForPose(template.pose),
        template
      )
    );

    // 2015.03.26 Sloppy code derived from unitGen to add mounts
    if (p.getItems("mount") != null) {
      //Item mount = nation.usedItems.filterSlot("mount").filterForPose(p).getRandom(nation.random);

      /*if(mount == null)
			{
				mount = p.getItems("mount").getRandom(nation.random);
				System.out.println("..\nCouldn't find old mount; using new\n..");
			}*/

      //template.setSlot("mount", mount);

      // Scouts usually ride the typical racial mount
      // 2015.07.08: Made the 20% chance to 80% chance so it's actually usually
      Command animal = null;
      if (
        template.race.tags.containsName("preferredmount") &&
        nation.random.nextDouble() < 0.80
      ) {
        animal = Command.args(
          "animal",
          template.race.tags.getString("preferredmount").orElseThrow()
        );
      }

      template.setSlot(
        "mount",
        unitGen.getSuitableItem(
          "mount",
          template,
          p.getItems("mount").filterOutTag(new Command("heavy")),
          null,
          animal
        )
      );

      if (template.getSlot("mount") == null) {
        template.setSlot(
          "mount",
          chandler.getRandom(p.getItems("mount"), template)
        );
        // System.out.println("..\nCouldn't find old mount; using new\n..");
      }
    }

    // Crude block to survival skills
    boolean mounted = false;
    boolean flying = false;
    boolean amphibian = false;
    boolean aquatic = false;
    boolean mountain = false;
    boolean forest = false;
    boolean waste = false;
    boolean swamp = false;
    int survivals = 0;
    for (Command c : template.getCommands()) {
      if (c.command.equals("#mounted")) mounted = true;
      if (c.command.equals("#flying")) flying = true;
      if (c.command.equals("#amphibian")) amphibian = true;
      if (c.command.equals("#aquatic")) aquatic = true;
      if (c.command.equals("#mountainsurvival") && !mountain) {
        mountain = true;
        survivals++;
      }
      if (c.command.equals("#forestsurvival") && !forest) {
        forest = true;
        survivals++;
      }
      if (c.command.equals("#wastesurvival") && !waste) {
        waste = true;
        survivals++;
      }
      if (c.command.equals("#swampsurvival") && !swamp) {
        swamp = true;
        survivals++;
      }
    }

    // If we are a scout with no special movement type and no more than 2 survivals, ensure they end up with 2-3 survivals
    if (
      !flying &&
      !mounted &&
      !aquatic &&
      !amphibian &&
      tier == 1 &&
      survivals <= 2
    ) {
      if (survivals == 0) {
        template.commands.add(new Command("#mountainsurvival"));
        mountain = true;
        template.commands.add(new Command("#forestsurvival"));
        forest = true;
      }

      if (survivals == 1) {
        if (mountain || swamp) {
          template.commands.add(new Command("#forestsurvival"));
          forest = true;
        } else if (forest || waste) {
          template.commands.add(new Command("#mountainsurvival"));
          mountain = true;
        }
      }

      double bonuschance = r.nextDouble();
      if (mountain && forest) {
        if (bonuschance < 0.2) template.commands.add(
          new Command("#wastesurvival")
        );
        else if (bonuschance < 0.25) template.commands.add(
          new Command("#swampsurvival")
        );
      } else if (!mountain && bonuschance < 0.25) {
        template.commands.add(new Command("#mountainsurvival"));
      } else if (!forest && bonuschance < 0.25) {
        template.commands.add(new Command("#forestsurvival"));
      }
    }

    template.color = new Color(
      (150 - tier * 30),
      (150 - tier * 30),
      (150 - tier * 30)
    );

    unitGen.handleExtraGeneration(template);

    chandler = null;
    unitGen = null;
    return template;
  }

  private void addStats(Unit u, int tier) {
    Filter tf = new Filter(nationGen);
    if (tier == 3) {
      tf.name = "Assassin";
      tf.tags.add("filterdesc", "assassin desc");
    } else if (tier == 2) {
      tf.name = "Spy";
      tf.tags.add("filterdesc", "spy desc");
    } else {
      tf.name = "Scout";
      tf.tags.add("filterdesc", "scout desc");
    }

    boolean elite = false;
    boolean sacred = false;
    for (Filter f : u.appliedFilters) {
      if (f.tags.containsName("alloweliteitems")) elite = true;
      if (f.tags.containsName("allowsacreditems")) sacred = true;
    }

    if (!elite || !sacred) {
      if (!elite) {
        tf.themeincs.add(ThemeInc.parse("thisitemtag elite *0"));
        tf.themeincs.add(ThemeInc.parse("thisitemtheme elite *0"));
      }
      if (!sacred) {
        tf.themeincs.add(ThemeInc.parse("thisitemtag sacred *0"));
        tf.themeincs.add(ThemeInc.parse("thisitemtheme sacred *0"));
      }
    }

    tf.themeincs.add(ThemeInc.parse("thisarmorprot below 11 *4"));
    tf.themeincs.add(ThemeInc.parse("thisarmorprot below 9 *4"));

    tf.themeincs.add(ThemeInc.parse("thisitemtheme scout *20"));
    tf.themeincs.add(ThemeInc.parse("thisitemtheme stealthy *20"));
    if (tier == 3) tf.themeincs.add(
      ThemeInc.parse("thisitemtheme assassin *20")
    );
    else if (tier == 2) tf.themeincs.add(
      ThemeInc.parse("thisitemtheme spy *20")
    );

    if (tier > 1) tf.commands.add(Command.args("#stealthy", "+25"));
    else tf.commands.add(Command.args("#stealthy", "+10"));

    if (tier == 3) {
      tf.commands.add(Command.args("#gcost", "+40"));
      tf.commands.add(Command.args("#assassin"));
      tf.commands.add(Command.args("#att", "+3"));
      tf.commands.add(Command.args("#def", "+3"));
      tf.commands.add(Command.args("#prec", "+3"));
      tf.commands.add(Command.args("#mor", "+3"));
      tf.commands.add(Command.args("#mr", "+1"));
      tf.commands.add(Command.args("#str", "+1"));
      //NamePart part = new NamePart();
      //part.text = "Assassin";
      //u.name.type = part;
    } else if (tier == 2) {
      tf.commands.add(Command.args("#gcost", "+40"));
      tf.commands.add(Command.args("#spy"));
      tf.commands.add(Command.args("#rpcost", "2"));
      //NamePart part = new NamePart();
      //part.text = "Spy";
      //u.name.type = part;
    } else {
      tf.commands.add(Command.args("#gcost", "+20"));
      //NamePart part = new NamePart();
      //part.text = "Scout";
      //u.name.type = part;
    }

    tf.commands.add(Command.args("#noleader"));
    u.appliedFilters.add(tf);

    assets.initializeFilters(List.of(tf), "ScoutGenerator");
  }
}
