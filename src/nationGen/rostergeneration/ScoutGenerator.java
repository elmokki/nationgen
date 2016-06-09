package nationGen.rostergeneration;


import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.Entity;
import nationGen.entities.Filter;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.misc.Command;
import nationGen.misc.ItemSet;
import nationGen.nation.Nation;
import nationGen.units.Unit;


public class ScoutGenerator extends TroopGenerator {
	



	public ScoutGenerator(NationGen g, Nation n) {
		super(g, n, "scoutgen");
	}

	public Unit generateScout(Race race)
	{
		String posename = "infantry";
		if(race.hasRole("scout"))
			posename = "scout";
		


		Random r = new Random(nation.random.nextInt());
	
		
		// Select whether it is a spy, scout or assassin
		double scoutchance = 0.5;
		double spychance = 0.25;
		double assassinchance = 0.25;
		
		if(Generic.containsTag(race.tags, "scoutchance"))
			scoutchance = Double.parseDouble(Generic.getTagValue(race.tags, "scoutchance"));
		if(Generic.containsTag(race.tags, "spychance"))
			scoutchance = Double.parseDouble(Generic.getTagValue(race.tags, "spychance"));
		if(Generic.containsTag(race.tags, "assassinchance"))
			scoutchance = Double.parseDouble(Generic.getTagValue(race.tags, "assassinchance"));
		
		
		double all = scoutchance + spychance + assassinchance;
		double roll = r.nextDouble() * all;
		
		int tier = 1;
		if(roll < assassinchance)
			tier = 3;
		else if(roll < spychance + assassinchance)
			tier = 2;

		
		// Get possible poses
		List<Pose> possiblePoses = new ArrayList<Pose>();
		for(Pose p : race.getPoses(posename))
		{
			if(p.tags.contains("cannot_be_scout") && tier == 1)
				continue;
			else if(p.tags.contains("cannot_be_spy") && tier == 2)
				continue;
			else if(p.tags.contains("cannot_be_assassin") && tier == 3)
				continue;	
			possiblePoses.add(p);
		}
		
		// Select pose
		Pose p = chandler.getRandom(possiblePoses, race, posename);
		Unit template = unitGen.generateUnit(race, p);

		chandler.getRandom(p.getItems("weapon").filterDom3DB("2h", "0", true, nationGen.weapondb), template);
		// Select a mainhand weapon
		Item weapon = null;
		if(tier != 3) //  Scout/spy gets whatever non-2h
			weapon = chandler.getRandom(p.getItems("weapon").filterDom3DB("2h", "0", true, nationGen.weapondb), template);
		else // Assassin gets max length 3 weapons
		{
			weapon = chandler.getRandom(p.getItems("weapon").filterDom3DBInteger("lgt", 3, true, nationGen.weapondb), template);
		}
		// Failsafe
		if(weapon == null)
		{
			weapon = chandler.getRandom(p.getItems("weapon"), template);

		}
		
		// Chance to give a whatever weapon rarely
		if(r.nextDouble() > 0.9 + (tier - 1) * 0.25)
		{
			Item tweapon = 	chandler.getRandom(p.getItems("weapon"), template);
			if(tweapon != null)
				weapon = tweapon;
		}

		
		this.addStats(template, tier);
		unitGen.addFreeTemplateFilters(template);
		
		
		
		template.setSlot("weapon", weapon);
		//template.setSlot("shirt", template.pose.getItems("shirt").getRandom(nation.random));
		
		
		Item armor = null;
		if(posename.equals("scout") && p.roles.size() == 1)
		{
			armor = nation.usedItems.filterSlot("armor").filterForPose(p).getRandom(chandler, nation.random);
			if(armor == null)
				armor = chandler.getRandom(p.getItems("armor"), template);

		}
		else
		{
			armor = nation.usedItems.filterSlot("armor").filterForPose(p).filterProt(nationGen.armordb, 0, 10).getRandom(chandler, nation.random);
			if(armor == null)
				armor = chandler.getRandom(p.getItems("armor").filterProt(nationGen.armordb, 0, 10), template);
			if(armor == null)
				armor = chandler.getRandom(p.getItems("armor").filterProt(nationGen.armordb, 0, 12), template);
			if(armor == null)
				armor = chandler.getRandom(p.getItems("armor"), template);
		}
		
		if(armor == null)
		{
			System.out.println("Error giving scout an armor. Pose " + p.name + " / " + p.roles + " of race " + race.name);
			return null;
		}
		
		template.setSlot("armor", armor);
		
		Item helmet = null;
		int prot = nationGen.armordb.GetInteger(template.getSlot("armor").id, "prot");
		
		if(template.pose.getItems("helmet") != null)
		{
			ItemSet helms = template.pose.getItems("helmet");
			helmet = Entity.getRandom(nation.random, chandler.handleChanceIncs(template, helms, unitGen.generateTargetProtChanceIncs(prot, 4)));
	
			
	
			template.setSlot("helmet", helmet);
	
		}
	
		if(r.nextDouble() < 0.15 && template.pose.getItems("bonusweapon") != null)
		{
			Item bweapon = chandler.getRandom(nation.usedItems.filterSlot("bonusweapon").filterTag("noelite", false).filterThoseNotInArmorRange(template.getTotalProt(false)), template);
			template.setSlot("bonusweapon", bweapon);
			
			if(template.getSlot("bonusweapon") == null)
			{			
				bweapon = chandler.getRandom(template.pose.getItems("bonusweapon").filterTag("noelite", false).filterThoseNotInArmorRange(template.getTotalProt(false)), template);
				template.setSlot("bonusweapon", bweapon);
			}
		}
		
		
		double dwchance = 0.3;


		
		if(tier == 3)
			dwchance = 1;
			
		if(p.getItems("offhand") != null && r.nextDouble() < dwchance && p.getItems("offhand").filterArmor(false).size() > 0 && nationGen.weapondb.GetInteger(template.getSlot("weapon").id, "lgt") < 3 && nationGen.weapondb.GetInteger(template.getSlot("weapon").id, "2h") == 0)
		{	
		
			if(r.nextDouble() > 0.25 && p.getItems("offhand").filterArmor(false).getItemWithID(weapon.id, "offhand") != null)
			{
				template.setSlot("offhand", chandler.getRandom(p.getItems("offhand").filterArmor(false).getItemsWithID(weapon.id, "offhand"), template));
			}
			else
			{
				template.setSlot("offhand", chandler.getRandom(p.getItems("offhand").filterArmor(false), template));
			}
			
		}

		if(p.getItems("cloakb") != null)
			template.setSlot("cloakb", chandler.getRandom(p.getItems("cloakb").filterForPose(template.pose), template));
		
		
		// 2015.03.26 Sloppy code derived from unitGen to add mounts
		if(p.getItems("mount") != null)
		{
			//Item mount = nation.usedItems.filterSlot("mount").filterForPose(p).getRandom(nation.random);

			/*if(mount == null)
			{
				mount = p.getItems("mount").getRandom(nation.random);
				System.out.println("..\nCouldn't find old mount; using new\n..");
			}*/
			
			//template.setSlot("mount", mount);
			
			// Scouts usually ride the typical racial mount 
			// 2015.07.08: Made the 20% chance to 80% chance so it's actually usually 
			String pref = null;
			if(Generic.getTagValue(template.race.tags, "preferredmount") != null && nation.random.nextDouble() < 0.80)
			{
				pref = Generic.getTagValue(template.race.tags, "preferredmount");
			}
	
			template.setSlot("mount", unitGen.getSuitableItem("mount", template, p.getItems("mount").filterTag("heavy", false), null, "animal " + pref));
							
			if(template.getSlot("mount") == null)
			{
				template.setSlot("mount", chandler.getRandom(p.getItems("mount"), template));
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
		for(Command c : template.getCommands())
		{
			if(c.command.equals("#mounted"))
				mounted = true;
			if(c.command.equals("#flying"))
				flying = true;
			if(c.command.equals("#amphibian"))
				amphibian = true;
			if(c.command.equals("#aquatic"))
				aquatic = true;
			if(c.command.equals("#mountainsurvival") && !mountain)
			{
				mountain = true;
				survivals++;
			}
			if(c.command.equals("#forestsurvival") && !forest)
			{
				forest = true;
				survivals++;
			}
			if(c.command.equals("#wastesurvival") && !waste)
			{
				waste = true;
				survivals++;
			}
			if(c.command.equals("#swampsurvival") && !swamp)
			{
				swamp = true;
				survivals++;
			}
		}
		
		// If we are a scout with no special movement type and no more than 2 survivals, ensure they end up with 2-3 survivals
		if(!flying &&
				!mounted &&
				!aquatic &&
				!amphibian &&
				tier == 1 &&
				survivals <= 2)
		{

			if(survivals == 0)
			{
				template.commands.add(new Command("#mountainsurvival"));
				mountain = true;
				template.commands.add(new Command("#forestsurvival"));
				forest = true;
			}
			
			if(survivals == 1)
			{
				if(mountain || swamp)
				{
					template.commands.add(new Command("#forestsurvival"));
					forest = true;
				}
				else if(forest || waste)
				{
					template.commands.add(new Command("#mountainsurvival"));
					mountain = true;
				}
			}
			
			double bonuschance = r.nextDouble();
			if(mountain && forest)
			{
				if(bonuschance < 0.2)
					template.commands.add(new Command("#wastesurvival"));
				else if(bonuschance < 0.25)
					template.commands.add(new Command("#swampsurvival"));					
			}
			else if(!mountain && bonuschance < 0.25)
			{
				template.commands.add(new Command("#mountainsurvival"));				
			}
			else if(!forest && bonuschance < 0.25)
			{
				template.commands.add(new Command("#forestsurvival"));				
			}

		}
		
		template.color = new Color(60, 60, 60);
		
		unitGen.handleExtraGeneration(template);
		
		return template;
	}
	
	private void addStats(Unit u, int tier)
	{
		Filter tf = new Filter(nationGen);
		tf.name = "Scout";
		
		boolean elite = false;
		boolean sacred = false;
		for(Filter f : u.appliedFilters)
		{
			if(f.tags.contains("alloweliteitems"))
				elite = true;
			if(f.tags.contains("allowsacreditems"))
				sacred = true;
		}
		
		if(!elite || !sacred)
		{

			if(!elite)
			{
				tf.themeincs.add("thisitemtag elite *0");
				tf.themeincs.add("thisitemtheme elite *0");
			}
			if(!sacred)
			{
				tf.themeincs.add("thisitemtag sacred *0");
				tf.themeincs.add("thisitemtheme sacred *0");

			}
			
		}	
		
		tf.themeincs.add("thisarmorprot below 11 *4");
		tf.themeincs.add("thisarmorprot below 9 *4");

		tf.themeincs.add("thisitemtheme scout *20");
		tf.themeincs.add("thisitemtheme stealthy *20");
		if(tier == 3)
			tf.themeincs.add("thisitemtheme assassin *20");
		else if(tier == 2)
			tf.themeincs.add("thisitemtheme spy *20");


		if(tier > 1)
				tf.commands.add(new Command("#stealthy", "+25"));
		else
				tf.commands.add(new Command("#stealthy", "+10"));
		
		if(tier == 3)
		{
			tf.commands.add(new Command("#gcost", "+40"));
			tf.commands.add(new Command("#assassin"));
			tf.commands.add(new Command("#att", "+3"));
			tf.commands.add(new Command("#def", "+3"));
			tf.commands.add(new Command("#prec", "+3"));
			tf.commands.add(new Command("#mor", "+3"));
			tf.commands.add(new Command("#mr", "+1"));
			tf.commands.add(new Command("#str", "+1"));
			//NamePart part = new NamePart();
			//part.text = "Assassin";
			//u.name.type = part;
		}
		else if(tier == 2)
		{
			tf.commands.add(new Command("#gcost", "+40"));
			tf.commands.add(new Command("#spy"));
			//NamePart part = new NamePart();
			//part.text = "Spy";
			//u.name.type = part;
		}
		else
		{
			tf.commands.add(new Command("#gcost", "+20"));

			//NamePart part = new NamePart();
			//part.text = "Scout";
			//u.name.type = part;
		}
		
		tf.commands.add(new Command("#noleader"));
		u.appliedFilters.add(tf);

	}
}
