package nationGen.rostergeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;



import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.Entity;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.misc.Command;
import nationGen.misc.ItemSet;
import nationGen.units.Unit;

public class UnitGen {
	private NationGen nationGen;
	private Random random;
	
	public UnitGen(NationGen gen, Random random)
	{
		this.nationGen = gen;
		this.random = random; //random;
	}
	
	public Unit generateUnit(Race race, Pose pose)
	{
		Unit u = new Unit(nationGen, race, pose);


		if(u.pose.getListOfSlots().contains("hands"))
			u.setSlot("hands", Entity.getRandom(random, u.pose.getItems("hands")));
		u.setSlot("shadow", Entity.getRandom(random, u.pose.getItems("shadow")));
		u.setSlot("basesprite", Entity.getRandom(random, u.pose.getItems("basesprite")));
		u.setSlot("legs", Entity.getRandom(random, u.pose.getItems("legs")));
	
		return u;
	}
	

	public Item getBestMatchForSlot(Unit from, Pose to, String slot)
	{
		Item ui = from.getSlot(slot);
		return getBestMatchForSlot(ui, to, slot);
	}

	public Item getBestMatchForSlot(Item from, Pose to, String slot)
	{
		
		Item ui = from;
		if(to.getItems(slot) == null || ui == null)
			return null;
		
		// Same name and slot
		for(Item i : to.getItems(slot))
		{
			if(i != null)
				if(i.name.equals(ui.name) && i.id.equals(ui.id))
					return i;
		}
		
		// Same image and id
		for(Item i : to.getItems(slot))
		{
			if(i != null)
				if(i.sprite.equals(ui.sprite) && i.id.equals(ui.id))
					return i;
		}
		
		// Same id and armor type
		for(Item i : to.getItems(slot))
		{
			if(i != null)
				if(i.id.equals(ui.id) && i.armor == ui.armor && ((i.tags.contains("elite") == ui.tags.contains("elite")) || i.tags.contains("sacred") == ui.tags.contains("sacred")))
					return i;
		}
		
		// Same id and armor type
		for(Item i : to.getItems(slot))
		{
			if(i != null)
				if(i.id.equals(ui.id) && i.armor == ui.armor)
					return i;
		}
		
		return null;
	}
	
	public Unit transformToPose(Unit from, Pose to)
	{
		Unit u = generateUnit(from.race, to);
		u = this.equipUnit(u, null, null, null, true);
	
		for(String slot : from.slotmap.keySet())
		{
	
			Item i = getBestMatchForSlot(from, to, slot);
			if(i != null)
				u.setSlot(slot, i);
		}
		
		return u;
	}
	
	
	public Unit armUnit(Unit u, ItemSet included, ItemSet excluded, String targettag, boolean ignoreArmor)
	{
		if(excluded == null)
			excluded = new ItemSet();
		if(included == null)
			included = new ItemSet();
		
		if(!hasItem(u, "weapon"))
		{
			if(ignoreArmor)
				u.setSlot("weapon", getSuitableItem("weapon", u, excluded, included, targettag));
			else
			{
				int prot = nationGen.armordb.GetInteger(u.getSlot("armor").id, "prot");
				
				ItemSet all = included.filterSlot("weapon");
				all.removeAll(excluded);
				
				if(all.possibleItems() == 0)
					all = u.pose.getItems("weapon");
				if(random.nextDouble() > 0.5 || (included.filterSlot("weapon").size() < 2 && random.nextDouble() > 0.15))
				{
			
					ItemSet test = u.pose.getItems("weapon");
					test.removeAll(included);
					if(test.possibleItems() > 0)
					{
						all = test;
					}
				}
				
				all.removeAll(excluded);
				
				
				
				
				if(included.filterDom3DB("2h", "Yes", false, nationGen.weapondb).possibleItems() == 0 && random.nextDouble() > 0.5)
				{
					ItemSet test = all.filterDom3DB("2h", "Yes", false, nationGen.weapondb);
					if(test.possibleItems() > 0)
						all = test;
				}
				
				
				
				if(prot < 10 && random.nextDouble() > 0.25)
				{
					ItemSet test = all.filterDom3DBInteger("res", 3, true, nationGen.weapondb);
					test = test.filterDom3DBInteger("dmg", 6, true, nationGen.weapondb);
					
					if(test.possibleItems() == 0)
						test = u.pose.getItems("weapon").filterDom3DBInteger("res", 3, true, nationGen.weapondb)
								.filterDom3DBInteger("dmg", 6, true, nationGen.weapondb);
					
					if(test.possibleItems() > 0)
						all = test;
			
				}
				else if(prot > 12 && random.nextDouble() > 0.25)
				{
					ItemSet test = all.filterDom3DBInteger("res", 1, false, nationGen.weapondb);
					test = test.filterDom3DBInteger("dmg", 4, false, nationGen.weapondb);
					
					
					if(test.possibleItems() == 0)
						test = u.pose.getItems("weapon").filterDom3DBInteger("res", 1, false, nationGen.weapondb)
								.filterDom3DBInteger("dmg", 4, false, nationGen.weapondb);
					
					if(test.possibleItems() > 0)
						all = test;
					
				}

	
				if(all.possibleItems() > 0)
					u.setSlot("weapon", getSuitableItem("weapon", u, excluded, all, targettag));
			}
		}
		
		return u;
	}
	
	
	public Unit armCavalry(Unit u, ItemSet included, ItemSet excluded, String targettag, boolean ignoreArmor)
	{
		included = included.filterForPose(u.pose);
		if(!hasItem(u, "weapon"))
		{
			if(ignoreArmor)
				u.setSlot("weapon", getSuitableItem("weapon", u, excluded, included, targettag));
			else
			{
				boolean has2H = false;
				boolean hasLance = false;
				boolean has1H = false;
				boolean hasLLance = false;
				
				for(Item i : excluded)
				{
					if(i.slot.equals("weapon"))
					{
						if(nationGen.weapondb.GetValue(i.id, "2h").equals("Yes"))
							has2H = true;
						else if(!(i.id.equals("357") || i.tags.contains("lightlance")))
							has1H = true;
						
						if(i.id.equals("357") || i.tags.contains("lightlance"))
							hasLLance = true;
					}
					if(i.slot.equals("bonusweapon") && (i.id.equals("4") || i.tags.contains("lance")))
					{
						hasLance = true;
					}
				}
				
				boolean canGetLance = false;
				if(!has1H && !hasLance)
				{
					int ap = 10;
					for(Command c : u.getCommands())
						if(c.args.equals("#ap"))
							ap = Integer.parseInt(c.args.get(0));
					
					boolean availableLance = false;
					for(Item i : u.pose.getItems("bonusweapon"))
						if(i.id.equals("4") || i.tags.contains("lance"))
							availableLance = true;
					
					if(10 + random.nextInt(20) > ap && availableLance)
						canGetLance = true;
				}
				
				boolean done = false;
				while(!done)
				{
					int choice = random.nextInt(5);
					if(choice < 2 && !has1H)
					{
						ItemSet lances = new ItemSet();
						for(Item i : u.pose.getItems("bonusweapon"))
							if(i.id.equals("4") || i.tags.contains("lance"))
								lances.add(i);
						
				
						
						ItemSet onehand = included.filterSlot("weapon").filterDom3DB("2h", "Yes", false, nationGen.weapondb);
						if(onehand.possibleItems() == 0)
						{

		
							onehand = u.pose.getItems("weapons").filterDom3DB("2h", "Yes", false, nationGen.weapondb);
						}
						ItemSet llances = new ItemSet();
						for(Item i : u.pose.getItems("weapon"))
							if(i.id.equals("357") || i.tags.contains("lightlance"))
								llances.add(i);
						
						onehand.removeAll(llances);
						
						if(onehand.possibleItems() > 0)
						{
					
							if(canGetLance)
								u.setSlot("bonusweapon", Entity.getRandom(random, lances));
							u.setSlot("weapon", Entity.getRandom(random, onehand));
							
							done = true;
						}
						
						
					}
					else if(choice < 4 && !hasLLance)
					{
						ItemSet lances = new ItemSet();
						for(Item i : u.pose.getItems("weapon"))
							if(i.id.equals("357") || i.tags.contains("lightlance"))
								lances.add(i);
				
						if(lances.possibleItems() > 0)
						{
						
							u.setSlot("weapon", Entity.getRandom(random, lances));
							done = true;
						}
					}
					else if(choice > 3 && !has2H)
					{
						ItemSet onehand = included.filterSlot("weapon").filterDom3DB("2h", "Yes", true, nationGen.weapondb);
						if(onehand.possibleItems() == 0)
							onehand = u.pose.getItems("weapon").filterDom3DB("2h", "Yes", true, nationGen.weapondb);
			
						if(onehand.possibleItems() > 0)
						{
						
							Item weapon = Entity.getRandom(random, onehand);
							u.setSlot("weapon", weapon);
						
					
							
							done = true;
						}
					}
			
				}
				
				
				
			}
		}
		
		return u;
	}
	
	public Unit equipUnit(Unit u, ItemSet included, ItemSet excluded, String targettag, boolean ignoreArmor)
	{		
		if(included == null)
			included = new ItemSet();

		if(excluded == null)
			excluded = new ItemSet();
		

		
		if(!hasItem(u, "armor") && u.pose.getItems("armor") != null)
		{
			u.setSlot("armor", getSuitableItem("armor", u, excluded, included, targettag));
		}
		
		
		if(u.pose.getItems("mount") != null && u.pose.getItems("mount").size() > 0)
		{
			int prot = nationGen.armordb.GetInteger(u.getSlot("armor").id, "prot");
			
			ItemSet possibleMounts = included.filterSlot("mount").filterForPose(u.pose).filterMinMaxProt(prot);
			

			if(possibleMounts.possibleItems() == 0 || random.nextDouble() > 0.66)
				possibleMounts = u.pose.getItems("mount").filterMinMaxProt(prot);
			

			boolean elites = false;
			if(random.nextDouble() < 0.1 && possibleMounts.filterTag("elite", true).possibleItems() > 0)
			{
				possibleMounts = possibleMounts.filterTag("elite", true);
				elites = true;
			}
			else
				possibleMounts = possibleMounts.filterTag("elite", false);
			

			// Make sure there is a mount to add in some weirder cases of a very limited mount being already taken
			ItemSet test = new ItemSet();
			test.addAll(possibleMounts.filterForPose(u.pose));
			test.removeAll(excluded.filterForPose(u.pose));
			if(test.possibleItems() == 0)
			{
				excluded.removeAll(possibleMounts.filterForPose(u.pose));
			}

			possibleMounts = possibleMounts.filterMinMaxProt(prot);
			if(possibleMounts.possibleItems() > 0)
			{

				String pref = null;
				if(Generic.getTagValue(u.race.tags, "preferredmount") != null && random.nextDouble() > 0.80)
				{
					pref = Generic.getTagValue(u.race.tags, "preferredmount");
				}
	

				
				u.setSlot("mount", getSuitableItem("mount", u, excluded, null, possibleMounts, "animal " + pref, elites, elites));
				excluded.add(u.getSlot("mount"));
				
				
				if(u.getSlot("mount") == null)
				{
					//System.out.println("No mount found for " + u.race.name + " with armor prot " + prot + " / " + u.pose.roles + " -- ERROR VERSION 1");
					return null;
				}
	
			}
			else
			{

				//System.out.println("No mount found for " + u.race.name + " with armor prot " + prot + " / " + u.pose.roles + " -- ERROR VERSION 2");

				return null;
			}
	
		}
		


		if(!hasItem(u, "helmet") && u.pose.getItems("helmet") != null)
		{
			if(!ignoreArmor)
			{
		

				int prot = nationGen.armordb.GetInteger(u.getSlot("armor").id, "prot");
				
				ItemSet inchere = included.filterSlot("helmet");
				if(inchere.possibleItems() == 0)
					inchere = u.pose.getItems("helmet");
				
				
				if(inchere.filterProt(nationGen.armordb,  prot - 3, prot + 6).possibleItems() > 0)
				{
					u.setSlot("helmet", getSuitableItem("helmet", u, excluded, inchere.filterProt(nationGen.armordb,  prot - 3, prot + 6), targettag));
				}
				else
				{		
					if(inchere.filterProt(nationGen.armordb,  prot - 5, prot + 5).possibleItems() > 0)
					{
	
						u.setSlot("helmet", getSuitableItem("helmet", u, excluded, inchere.filterProt(nationGen.armordb,  prot - 5, prot + 5), targettag));
					}
					else if(u.pose.getItems("helmet").filterProt(nationGen.armordb,  prot - 5, prot + 5).possibleItems() > 0)
					{

						u.setSlot("helmet", getSuitableItem("helmet", u, excluded, u.pose.getItems("helmet").filterProt(nationGen.armordb,  prot - 5, prot + 5), targettag));
					}
					else
					{
				
						int range = 1;
						int amount1 = Math.min(inchere.possibleItems(), 2);
						int amount2 = Math.min(u.pose.getItems("helmet").possibleItems(), 2);

						while(inchere.filterProt(nationGen.armordb,  prot - range*2, prot + range).possibleItems() < amount1 && u.pose.getItems("helmet").filterProt(nationGen.armordb,  prot - range*2, prot + range).possibleItems() < amount2)
						{
							range = range + 1;
						}
						
						if(inchere.filterProt(nationGen.armordb,  prot - range*2, prot + range).possibleItems() > 0)
							u.setSlot("helmet", getSuitableItem("helmet", u, excluded, inchere.filterProt(nationGen.armordb,  prot - range*2, prot + range), targettag));
						else if(u.pose.getItems("helmet").filterProt(nationGen.armordb,  prot - range*2, prot + range).possibleItems() > 0)
							u.setSlot("helmet", getSuitableItem("helmet", u, excluded, u.pose.getItems("helmet").filterProt(nationGen.armordb,  prot - range*2, prot + range), targettag));
					}
					
				}
				
				//int prot2 = nationGen.armordb.GetInteger(u.getSlot("helmet").id, "prot");
		
			}
			else
				if(ignoreArmor)
				{
			
					u.setSlot("helmet", getSuitableItem("helmet", u, excluded, included, targettag));
				}
				
		}
		

		
		if(!hasItem(u, "offhand") && u.pose.getItems("offhand") != null)
		{
			if(ignoreArmor)
			{
				if(included.filterArmor(false).possibleItems() > 0)
					u.setSlot("offhand", getSuitableItem("offhand", u, excluded, included.filterArmor(true), u.pose.getItems("offhand").filterArmor(true), targettag));
				else
					u.setSlot("offhand", getSuitableItem("offhand", u, excluded, included.filterArmor(true), u.pose.getItems("offhand").filterArmor(true), targettag));
			}
			else
			{
				ItemSet all = u.pose.getItems("offhand").filterArmor(true);
				
		
				all.removeAll(excluded.filterForPose(u.pose));
				ItemSet newincluded = new ItemSet();
				newincluded.addAll(included.filterForPose(u.pose));
				
				double deviance = random.nextDouble();
				int prot = (int)Math.round(nationGen.armordb.GetInteger(u.getSlot("armor").id, "prot"));
				
				int operation = 0;
				// Chance to remove bucklers
				if(deviance > 0.8 && prot > 12)
					operation = -1;
				if(deviance > 0.5 && prot > 15)
					operation = -1;
				if(deviance > 0.2 && prot >= 18)
					operation = -1;
				
				// chance to remove tower shield and kite shield
				if(deviance > 0.5 && prot < 10)
					operation = 1;

				if(operation == -1)
				{
					all = all.filterProt(nationGen.armordb, 15, 100);
					newincluded = newincluded.filterProt(nationGen.armordb, 15, 100);
				}
				else if(operation == 1)
				{
					all = all.filterProt(nationGen.armordb, 0, 19, true);
					newincluded = newincluded.filterProt(nationGen.armordb, 0, 19, true);
				}
				
				if(newincluded.possibleItems() == 0)
					newincluded = included;
				
				if(all.possibleItems() == 0)
					all = null;
				
				if(random.nextDouble() < 0.1)
					newincluded = null;
				
				Item shield = this.getSuitableItem("offhand", u, excluded, newincluded, u.pose.getItems("offhand").filterArmor(true), targettag);
				u.setSlot("offhand", shield);
			}
		}
		
		return u;
	}
	
	
	private boolean hasItem(Unit u, String slot)
	{
		return (u.getSlot(slot) != null);
	}
	
	

	
	public Item getSuitableItem(String slot, Unit u, ItemSet excluded, ItemSet included, String targettag)
	{
		return this.getSuitableItem(slot, u, excluded, included, null, targettag);
	}
	
	
	public Item getSuitableItem(String slot, Unit u, ItemSet excluded, ItemSet included, ItemSet all, String targettag)
	{
		return this.getSuitableItem(slot, u, excluded, included, all, targettag, false, false);
	}
	
	public Item getSuitableItem(String slot, Unit u, ItemSet excluded, ItemSet included, ItemSet all, String targettag, boolean elite, boolean sacred)
	{
		
		if(all == null)
			all = u.pose.getItems(slot);
		if(all == null)
			return null;
		
		if(excluded == null)
			excluded = new ItemSet();
		if(included == null)
			included = new ItemSet();
				
		if(!sacred)
		{
			included = included.filterTag("sacred", false);
			all = all.filterTag("sacred", false);
		}
		if(!elite)
		{
			included = included.filterTag("elite", false);
			all = all.filterTag("elite", false);
		}
		
		excluded = excluded.filterSlot(slot).filterForPose(u.pose);
		included = included.filterSlot(slot).filterForPose(u.pose);
		

		
		
		included.removeAll(excluded);
		all.removeAll(excluded);
		


		
		ItemSet remain = new ItemSet();
		remain.addAll(included);
		remain.retainAll(all);
		
		
		ItemSet chosen = null;

		if(remain.possibleItems() > 0)
			chosen = remain;
		else
		{
			chosen = all;	
		}
	

		if(targettag != null && chosen.filterTag(targettag, true).possibleItems() > 0)
		{
			chosen = chosen.filterTag(targettag, true);
		}
		Item newitem = Entity.getRandom(random, chosen);


		
		return newitem;
		
		
		
	}
	

	

	

	
}
