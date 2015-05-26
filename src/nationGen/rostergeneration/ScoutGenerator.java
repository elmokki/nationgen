package nationGen.rostergeneration;


import java.awt.Color;
import java.util.Random;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.Entity;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.misc.Command;
import nationGen.misc.ItemSet;
import nationGen.nation.Nation;
import nationGen.units.Unit;


public class ScoutGenerator extends TroopGenerator {
	



	public ScoutGenerator(NationGen g, Nation n) {
		super(g, n);
	}

	public Unit generateScout(Race race)
	{
		String posename = "infantry";
		if(race.hasRole("scout"))
			posename = "scout";
		


		Random r = nation.random;
		int tier = 1 + r.nextInt(3);
		
		Pose p = null;
		Item weapon = null;
		
		int tries = 0;
		while(p == null || weapon == null)
		{
			tries++;
			if(tier != 3)
				weapon = race.getItems("weapon", posename).filterDom3DB("2h", "0", true, nationGen.weapondb).getRandom(nation.random);
			else
				weapon = race.getItems("weapon", posename).filterDom3DB("2h", "0", true, nationGen.weapondb).filterDom3DB("lgt", "3", false, nationGen.weapondb).filterDom3DB("lgt", "4", false, nationGen.weapondb).getRandom(nation.random);
			
			if(weapon == null)
			{
				weapon = race.getItems("weapon", posename).getRandom(nation.random);
			}
	
			
			for(Pose p2 : race.poses)
			{
				if(p2.roles.contains(posename) && p2.getItems("weapon").contains(weapon))
					p = p2;
			}
			
			//System.out.println(p + " / " + weapon);
			
			Item armor = null;
			if(posename.equals("scout"))
			{
				armor = nation.usedItems.filterSlot("armor").filterForPose(p).getRandom(nation.random);
				if(armor == null)
					armor = p.getItems("armor").getRandom(nation.random);
			}
			else
			{
				armor = nation.usedItems.filterSlot("armor").filterForPose(p).filterProt(nationGen.armordb, 0, 10).getRandom(nation.random);
				if(armor == null)
					armor = p.getItems("armor").filterProt(nationGen.armordb, 0, 10).getRandom(nation.random);
			}
			
			if(tries > 20 && armor == null)
			{
				System.out.println(race.name + " seems to lack a scout pose and an infantry pose with an armor below 11 prot. Now you have a fairly heavy scout. If this is intentional, whine and this message may be removed.");
				break;
			}
			else if(armor == null)
			{
				p = null;
			}
		}

		
		
		Unit template = unitGen.generateUnit(race, p);
		
		
		template.setSlot("weapon", weapon);
		//template.setSlot("shirt", template.pose.getItems("shirt").getRandom(nation.random));
		
		
		Item armor = null;
		if(posename.equals("scout"))
		{
			armor = nation.usedItems.filterSlot("armor").filterForPose(p).getRandom(nation.random);
			if(armor == null)
				armor = p.getItems("armor").getRandom(nation.random);
		}
		else
		{
			armor = nation.usedItems.filterSlot("armor").filterForPose(p).filterProt(nationGen.armordb, 0, 10).getRandom(nation.random);
			if(armor == null)
				armor = p.getItems("armor").filterProt(nationGen.armordb, 0, 10).getRandom(nation.random);
		}
		
		
		if(armor == null)
		{
			return null;
		}
		
		template.setSlot("armor", armor);
		
		Item helmet = null;
		int range = 0;
		int prot = nationGen.armordb.GetInteger(template.getSlot("armor").id, "prot");
		
		if(template.pose.getItems("helmet") != null)
		{
			while(helmet == null && range < 16)
			{
				range++;
				ItemSet helms = template.pose.getItems("helmet").filterProt(nationGen.armordb, prot - range, prot + range);
				
				if((range < 6 && helms.possibleItems() < 3) || range >= 6)
					helmet = helms.getRandom(nation.random);
			}
			template.setSlot("helmet", helmet);
		}
	
		if(r.nextDouble() < 0.15 && template.pose.getItems("bonusweapon") != null)
		{
			template.setSlot("bonusweapon", nation.usedItems.filterSlot("bonusweapon").filterTag("noelite", false).filterThoseNotInArmorRange(template.getTotalProt(false)).getRandom(nation.random));
			if(template.getSlot("bonusweapon") == null)
				template.setSlot("bonusweapon", template.pose.getItems("bonusweapon").filterTag("noelite", false).filterThoseNotInArmorRange(template.getTotalProt(false)).getRandom(nation.random));

		}
		
		
		double dwchance = 0.1;


		
		if(tier == 3)
			dwchance = 1;
			
		if(p.getItems("offhand") != null && r.nextDouble() < (dwchance * 3) && p.getItems("offhand").filterArmor(false).size() > 0 && nationGen.weapondb.GetInteger(template.getSlot("weapon").id, "lgt") < 3)
		{	
		
			if(r.nextDouble() > 0.25 && p.getItems("offhand").filterArmor(false).getItemWithID(weapon.id, "offhand") != null)
			{
				template.setSlot("offhand", p.getItems("offhand").filterArmor(false).getItemWithID(weapon.id, "offhand"));
			}
			else
			{
				template.setSlot("offhand", p.getItems("offhand").filterArmor(false).getRandom(nation.random));
			}
			
		}

		if(p.getItems("cloakb") != null)
			template.setSlot("cloakb", p.getItems("cloakb").filterForPose(template.pose).getRandom(nation.random));
		
		
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
			String pref = null;
			if(Generic.getTagValue(template.race.tags, "preferredmount") != null && nation.random.nextDouble() > 0.80)
			{
				pref = Generic.getTagValue(template.race.tags, "preferredmount");
			}
	
			template.setSlot("mount", unitGen.getSuitableItem("mount", template, p.getItems("mount").filterTag("heavy", false), null, p.getItems("mount"), "animal " + pref, false, false));
							
			if(template.getSlot("mount") == null)
			{
				template.setSlot("mount", p.getItems("mount").getRandom(nation.random));
				// System.out.println("..\nCouldn't find old mount; using new\n..");
			}
		
		}
	
		
		this.addStats(template, tier);
		template.color = new Color(60, 60, 60);
		
		this.handleExtraGeneration(template, posename);
		
		return template;
	}
	
	private void addStats(Unit u, int tier)
	{
		//int gcost = 10 + tier * 10;
		//u.commands.add(new Command("#gcost", "" + gcost));
		
		if(tier > 1)
				u.commands.add(new Command("#stealthy", "+25"));
		else
				u.commands.add(new Command("#stealthy", "+10"));
		
		if(tier == 3)
		{
			u.commands.add(new Command("#gcost", "+40"));
			u.commands.add(new Command("#assassin"));
			u.commands.add(new Command("#att", "+3"));
			u.commands.add(new Command("#def", "+3"));
			u.commands.add(new Command("#prec", "+3"));
			u.commands.add(new Command("#mor", "+3"));
			u.commands.add(new Command("#mr", "+1"));
			u.commands.add(new Command("#str", "+1"));
			//NamePart part = new NamePart();
			//part.text = "Assassin";
			//u.name.type = part;
		}
		else if(tier == 2)
		{
			u.commands.add(new Command("#gcost", "+40"));
			u.commands.add(new Command("#spy"));
			//NamePart part = new NamePart();
			//part.text = "Spy";
			//u.name.type = part;
		}
		else
		{
			u.commands.add(new Command("#gcost", "+20"));

			//NamePart part = new NamePart();
			//part.text = "Scout";
			//u.name.type = part;
		}
		
		u.commands.add(new Command("#noleader"));
	}
}
