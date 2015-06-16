package nationGen.rostergeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;














import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.AbilityTemplate;
import nationGen.entities.Entity;
import nationGen.entities.Filter;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.entities.Theme;
import nationGen.items.Item;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.misc.ItemSet;
import nationGen.nation.Nation;
import nationGen.rostergeneration.TroopGenerator.Template;
import nationGen.units.Unit;

public class UnitGen {
	private NationGen nationGen;
	private Random random;
	private Nation nation;
	private ChanceIncHandler chandler;
	
	
	
	
	public UnitGen(NationGen gen, Nation n)
	{
		this.nationGen = gen;
		this.nation = n;
		this.random = new Random(n.random.nextInt());
		this.chandler = new ChanceIncHandler(n);
	}
	
	/**
	 * Generates a naked unit (hands, shadow, basesprite, legs)
	 * @param race Race of the unit
	 * @param pose Pose of the unit
	 * @return
	 */
	public Unit generateUnit(Race race, Pose pose)
	{
		Unit u = new Unit(nationGen, race, pose, nation);


		
		String[] slots = {"hands", "shadow", "basesprite", "legs"};
		for(int i = 0; i < slots.length; i++)
		{
			String slot = slots[i];
			
			
			
			if(u.pose.getListOfSlots().contains(slot))
			{
				Item item = null;

				// Handle #lockslot
				for(Theme t : race.themefilters)
					for(String tag : t.tags)
					{
						List<String> args = Generic.parseArgs(tag);
						if(args.get(0).equals("lockslot"))
						{
							if(args.get(1).equals(slot))
							{
								String name = args.get(2);
								item = u.pose.getItems(slot).getItemWithName(name, slot);
								
								if(item != null)
									break;

							}
						}
						
						
						if(item != null)
							break;
					}
				
				// If no lockslot or no suitable item for lockslot
				if(item == null)
					item = Entity.getRandom(random, chandler.handleChanceIncs(u, u.pose.getItems(slot)));
				
				u.setSlot(slot, item);
			}
		}
		

		
		
		
		return u;
	}
	
	
	
	
	
	/**
	 * Armors unit up
	 * @param u
	 */
	public void armorUnit(Unit u, ItemSet included, ItemSet excluded, String targettag, boolean mage)
	{
		// Handles generationchances and stuff
		this.handleExtraGeneration(u);
		// Failsafe
		if(included == null)
			included = new ItemSet();	
		if(excluded == null)
			excluded = new ItemSet();
		
		
		boolean ignoreArmor = mage;
		

		// Armor
		if(!hasItem(u, "armor") && u.pose.getItems("armor") != null)
		{
			Item armor = getSuitableItem("armor", u, excluded, included, targettag);
			u.setSlot("armor", armor);
		}
		
		// Mount
		if(u.pose.getItems("mount") != null && u.pose.getItems("mount").size() > 0)
		{
			int prot = nationGen.armordb.GetInteger(u.getSlot("armor").id, "prot");
			
			ItemSet possibleMounts = included.filterSlot("mount").filterForPose(u.pose).filterMinMaxProt(prot);
			

			if(chandler.countPossibleFilters(possibleMounts, u) == 0 || random.nextDouble() > 0.66)
			{
				possibleMounts = u.pose.getItems("mount").filterMinMaxProt(prot);
				if(possibleMounts.possibleItems() == 0)
					possibleMounts = u.pose.getItems("mount");
			}

		
			

			// Make sure there is a mount to add in some weirder cases of a very limited mount being already taken
			ItemSet test = new ItemSet();
			test.addAll(possibleMounts.filterForPose(u.pose));
			test.removeAll(excluded.filterForPose(u.pose));
			if(chandler.countPossibleFilters(test, u) == 0)
			{
				excluded.removeAll(possibleMounts.filterForPose(u.pose));
			}

			ItemSet possibleMounts2 = possibleMounts.filterMinMaxProt(prot);
			if(chandler.countPossibleFilters(possibleMounts2, u) > 0)
				possibleMounts = possibleMounts2;
		
		
			if(chandler.countPossibleFilters(possibleMounts, u) > 0)
			{

				String pref = null;
				if(Generic.getTagValue(u.race.tags, "preferredmount") != null && random.nextDouble() > 0.70)
				{
					pref = Generic.getTagValue(u.race.tags, "preferredmount");
				}
	

				Item mount = getSuitableItem("mount", u, excluded, null, possibleMounts, "animal " + pref);
				if(mount == null)
					mount = getSuitableItem("mount", u, excluded, null, possibleMounts, null);
				
				
				
				u.setSlot("mount", mount);
				excluded.add(u.getSlot("mount"));
				
				
				if(u.getSlot("mount") == null)
				{
					System.out.println("No mount found for " + u.race.name + " with armor prot " + prot + " / " + u.pose.roles + " -- ERROR VERSION 1");
					return;
				}
	
			}
			else
			{

				System.out.println("No mount found for " + u.race.name + " with armor prot " + prot + " / " + u.pose.roles + " -- ERROR VERSION 2");
				return;
			}
	
		}
		


		if(!hasItem(u, "helmet") && u.pose.getItems("helmet") != null)
		{
			if(!ignoreArmor)
			{
		

				int prot = nationGen.armordb.GetInteger(u.getSlot("armor").id, "prot");
				
				ItemSet inchere = included.filterSlot("helmet").filterForPose(u.pose);
				if(inchere.possibleItems() == 0)
					inchere = u.pose.getItems("helmet");
				
	
				if(u.getSlot("helmet") == null)
				{

					// Fancy chanceinc implementation
					if(inchere.filterProt(nationGen.armordb,  prot - 3, prot + 3).possibleItems() == 0)
					{
						inchere = u.pose.getItems("helmet");
					}
					Item helmet = Entity.getRandom(random, chandler.handleChanceIncs(u, inchere, this.generateTargetProtChanceIncs(prot, 8)));
					

					u.setSlot("helmet", helmet);
					

					// Traditional implementation
					/*
					int range = 1;
					int amount1 = Math.min(inchere.possibleItems(), 2);
					int amount2 = Math.min(u.pose.getItems("helmet").possibleItems(), 4);
		
					while(chandler.countPossibleFilters(inchere.filterProt(nationGen.armordb,  prot - range, prot + range), u) < amount1 
							&& u.pose.getItems("helmet").filterProt(nationGen.armordb,  prot - range, prot + range).possibleItems() < amount2)
					{
						range = range + 1;
					}
					
					if(inchere.filterProt(nationGen.armordb,  prot - range, prot + range).possibleItems() > 0)
						u.setSlot("helmet", getSuitableItem("helmet", u, excluded, inchere.filterProt(nationGen.armordb,  prot - range, prot + range), targettag));
					else if(u.pose.getItems("helmet").filterProt(nationGen.armordb,  prot - range, prot + range).possibleItems() > 0)
						u.setSlot("helmet", getSuitableItem("helmet", u, excluded, u.pose.getItems("helmet").filterProt(nationGen.armordb,  prot - range, prot + range), targettag));
					*/
					
					
					// Failsafe
					if(u.getSlot("helmet") == null)
					{
						u.setSlot("helmet", getSuitableItem("helmet", u, excluded, u.pose.getItems("helmet"), targettag));
					}
					
					

				}
				
				

		
			}
			else 
			{
				u.setSlot("helmet", getSuitableItem("helmet", u, excluded, included, targettag));
			}
				
		}
		

		
		if(!hasItem(u, "offhand") && u.pose.getItems("offhand") != null)
		{
			if(ignoreArmor)
			{
						u.setSlot("offhand", getSuitableItem("offhand", u, excluded, included, targettag));
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
		
	
	}
	
	
	private List<String> generateTargetProtChanceIncs(int prot, int range)
	{
		List<String> list = new ArrayList<String>();
		
		for(int i = 0; i < range; i++)
		{
		
			
			// Below not applied instantly
			if(i >= 2)
			{
				int bottom = prot - i; // "below"
				list.add("thisarmorprot below " + bottom + " *0.8");
			}
			
			int top = prot + i + 1; // above or at
			list.add("thisarmorprot " + top + " *0.8");
		}
		
		return list;
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
	

	public Unit armUnit(Unit u, ItemSet included, ItemSet excluded, String targettag, boolean mage)
	{		
		

		if(u.getSlot("mount") != null && !mage)
			return armCavalry(u, included, excluded, targettag, mage);
		else
			return armInfantry(u, included, excluded, targettag, mage);
	}
	
	
	public Unit armInfantry(Unit u, ItemSet included, ItemSet excluded, String targettag, boolean mage)
	{
		boolean ignoreArmor = mage;
		
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
				
				
				
				// 50% chance to not give a twohander if possible and not mage
				if(included.filterDom3DB("2h", "Yes", false, nationGen.weapondb).possibleItems() == 0 
				   && included.filterDom3DB("2h", "Yes", true, nationGen.weapondb).possibleItems() != 0 && random.nextDouble() > 0.5 && !mage)
				{
					ItemSet test = all.filterDom3DB("2h", "Yes", false, nationGen.weapondb);
					if(test.possibleItems() > 0)
						all = test;
				}
				
				
				if(prot < 10 && random.nextDouble() > 0.5 && !mage)
				{
					ItemSet test = all.filterDom3DBInteger("res", 3, true, nationGen.weapondb);
					test = test.filterDom3DBInteger("dmg", 6, true, nationGen.weapondb);
					
					if(test.possibleItems() == 0)
						test = u.pose.getItems("weapon").filterDom3DBInteger("res", 3, true, nationGen.weapondb)
								.filterDom3DBInteger("dmg", 6, true, nationGen.weapondb);
					
					if(test.possibleItems() > 0)
						all = test;
			
				}
				else if(prot > 12 && random.nextDouble() > 0.5 && !mage)
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
		if(excluded == null)
			excluded = new ItemSet();
		if(included == null)
			included = new ItemSet();
		
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
				

	

				boolean canGetLance = false;
				if(!has1H && !hasLance && u.pose.getItems("lanceslot") != null)
				{
					int ap = 10;
					for(Command c : u.getCommands())
						if(c.args.equals("#ap"))
							ap = Integer.parseInt(c.args.get(0));
					
					boolean availableLance = false;
					for(Item i : u.pose.getItems("lanceslot"))
						if(i.id.equals("4") || i.tags.contains("lance"))
							availableLance = true;
					
					if(10 + random.nextInt(20) > ap && availableLance)
						canGetLance = true;
				}
				

				Item weapon = null;
				boolean done = false;
				while(!done)
				{
					int choice = random.nextInt(4); // 0-3
					if(choice <= 1 && !has1H)
					{

						ItemSet lances = new ItemSet();
						if(u.pose.getItems("lanceslot") != null)
							for(Item i : u.pose.getItems("lanceslot"))
								if(i.id.equals("4") || i.tags.contains("lance"))
									lances.add(i);
						
				
						ItemSet onehand = included.filterSlot("weapon").filterDom3DB("2h", "Yes", false, nationGen.weapondb);
						

						if(chandler.handleChanceIncs(u,onehand).size() == 0)
						{
							onehand = u.pose.getItems("weapon").filterDom3DB("2h", "Yes", false, nationGen.weapondb);

						}
						
						
						ItemSet llances = new ItemSet();
						for(Item i : u.pose.getItems("weapon"))
							if(i.id.equals("357") || i.tags.contains("lightlance"))
								llances.add(i);
						
						onehand.removeAll(llances);
						
						if(onehand.possibleItems() > 0)
						{
					
							if(canGetLance)
							u.setSlot("lanceslot", chandler.getRandom(lances, u));
							weapon = chandler.getRandom(onehand, u);
							
							done = true;
						}
						
						
					}
					else if(choice == 2 && !hasLLance)
					{

						ItemSet lances = new ItemSet();
						for(Item i : u.pose.getItems("weapon"))
							if(i.id.equals("357") || i.tags.contains("lightlance"))
								lances.add(i);
				
						if(lances.possibleItems() > 0)
						{
						
							weapon = chandler.getRandom(lances, u);
							done = true;
						}
					}
					else if(choice == 3 && !has2H)
					{

						ItemSet onehand = included.filterSlot("weapon").filterDom3DB("2h", "Yes", true, nationGen.weapondb);
						if(chandler.handleChanceIncs(u,onehand).size() == 0)
							onehand = u.pose.getItems("weapon").filterDom3DB("2h", "Yes", true, nationGen.weapondb);
			
						if(onehand.possibleItems() > 0)
						{
						
							weapon = chandler.getRandom(onehand, u);
						
					
							
							done = true;
						}
					}
					
		
			
				}
				
				if(weapon == null)
					System.out.println("NULL WEAPON FOR " + u.race.name + " " + u.pose.name);
				u.setSlot("weapon", weapon);


				
				
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
		return getSuitableItem(slot, u, excluded, included, null, targettag);
	}
	
	public Item getSuitableItem(String slot, Unit u, ItemSet excluded, ItemSet included, ItemSet all, String targettag)
	{
		
		if(all == null || all.size() == 0)
			all = u.pose.getItems(slot);
		if(all == null)
			return null;
		
		if(excluded == null)
			excluded = new ItemSet();
		if(included == null)
			included = new ItemSet();
				

		excluded = excluded.filterSlot(slot).filterForPose(u.pose);
		included = included.filterSlot(slot).filterForPose(u.pose);
		



		
		included.removeAll(excluded);
		all.removeAll(excluded);
		
	


		
		
		ItemSet remain = new ItemSet();
		remain.addAll(included);
		remain.retainAll(all);
		
	
		
		ItemSet chosen = null;

		

		if(chandler.countPossibleFilters(remain, u) > 0 && random.nextDouble() > 0.75)
			chosen = remain;
		else
		{
			chosen = all;	
		}
	

		if(targettag != null && chosen.filterTag(targettag, true).possibleItems() > 0)
		{
			chosen = chosen.filterTag(targettag, true);
		}
		

		Item newitem = chandler.getRandom(chosen, u);


		
		return newitem;
		
		
		
	}
	

	private void handleExtraGeneration(Unit u)
	{
		List<String> tags = new ArrayList<String>();
		tags.addAll(u.pose.tags);
		tags.addAll(u.race.tags);
		for(Item i : u.slotmap.values())
			if(i != null)
				tags.addAll(i.tags);
		for(Filter f : u.appliedFilters)
			tags.addAll(f.tags);
		
		
		for(String tag : tags)
		{
			List<String> args = Generic.parseArgs(tag);
			if(args.get(0).equals("generateitem"))
			{
				String slot = args.get(2);
				double chance = Double.parseDouble(args.get(1));
				if(nation.random.nextDouble() < chance)
				{
					Item item = this.getSuitableItem(slot, u, null, null, null); 
					u.setSlot(slot, item);

				}
			}
		}
	}
	
	



	
}
