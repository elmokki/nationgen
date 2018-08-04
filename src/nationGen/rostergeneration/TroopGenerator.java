package nationGen.rostergeneration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;














import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.entities.Entity;
import nationGen.entities.Filter;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.misc.ItemSet;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class TroopGenerator {
	public NationGen nationGen;
	public Nation nation;
	public UnitGen unitGen;
	protected NationGenAssets assets;
	public ItemSet used = new ItemSet();
	public ItemSet exclusions = new ItemSet();
	private double bonusrangedness = 0;
	
	private List<Filter> unitTemplates = new ArrayList<Filter>();
	private int maxtemplates = 0;
	private int maxdifferenttemplates = 0;
	private int appliedtemplates = 0;
	
	private List<TroopTemplate> templates = new ArrayList<TroopTemplate>();
	protected ChanceIncHandler chandler;
	protected Random random;
	

	public void setTemplates(List<TroopTemplate> templates)
	{
		this.templates = templates;
	}
	
	
	public int getMaxVarieties(Unit u)
	{
		int maxvar = 3;
		
		if(Generic.getTagValue(u.pose.tags, "maxvarieties") != null)
			maxvar = Integer.parseInt(Generic.getTagValue(u.pose.tags, "maxvarieties"));
		else
		{
			maxvar = 3;
			if(u.pose.roles.contains("mounted"))
				maxvar = 2;
			if(u.pose.roles.contains("chariot"))
				maxvar = 1;
		}

		if(u.getSlot("armor") != null && Generic.getTagValue(u.getSlot("armor").tags, "maxvarieties") != null)
			maxvar = Math.min(maxvar, Integer.parseInt(Generic.getTagValue(u.getSlot("armor").tags, "maxvarieties")));
		
		if(u.getSlot("mount") != null && Generic.getTagValue(u.getSlot("mount").tags, "maxvarieties") != null)
			maxvar = Math.min(maxvar, Integer.parseInt(Generic.getTagValue(u.getSlot("mount").tags, "maxvarieties")));
		

		maxvar = Math.min(chandler.handleChanceIncs(u, u.pose.getItems("weapon")).size(), maxvar);

		for(Filter f : u.appliedFilters)
			if(Generic.getTagValue(f.tags, "maxvarieties") != null)
			{
				maxvar = Math.min(maxvar, Integer.parseInt(Generic.getTagValue(f.tags, "maxvarieties")));
			}


		if(u.pose.roles.contains("mounted"))
			maxvar = Math.min(getCavVarieties(u.pose), maxvar);
		

		if(Generic.getTagValue(u.pose.tags, "maxvarieties_override") != null)
		{
			maxvar = Integer.parseInt(Generic.getTagValue(u.pose.tags, "maxvarieties_override"));
		}
		
		return maxvar;

	}
	
	public TroopGenerator(NationGen g, Nation n, NationGenAssets assets, String id)
	{
		nationGen = g;
		nation = n;
		this.assets = assets;
		unitGen = new UnitGen(g, n, assets);
		random = new Random(n.random.nextInt());

		bonusrangedness = random.nextDouble() * 0.3;
		chandler = new ChanceIncHandler(nation, id);
		unitGen.chandler.identifier = id;
		
		// Maxtemplates distribution
		//
		// 1 template:  12.5%
		// 2 templates: 37.5%
		// 3 templates: 25.0%
		// 4 templates: 25.0%
		maxtemplates = 1 + random.nextInt(4); // 1-4
		if(maxtemplates == 1 && random.nextBoolean())
			maxtemplates++;
		
		// Max different templates distribution
		//
		// 1 template:  16.7%
		// 2 templates: 50.0%
		// 3 templates: 33.3%
		maxdifferenttemplates = 1 + random.nextInt(3); // 1-3
		if(maxdifferenttemplates == 1 && random.nextBoolean())
			maxdifferenttemplates++;
		
	}
	
	public TroopGenerator(NationGen g, Nation n, NationGenAssets assets)
	{
		this(g, n, assets, "troopgen");
	}
	
	
	public void setIdentifier(String id)
	{
		this.chandler.identifier = id;
		this.unitGen.chandler.identifier = id;
	}
	
	protected boolean isDualWieldEligible(Unit u)
	{
	
		

		Item it = u.getSlot("weapon");
		if(it == null)
				return false;
			
		List<String> tags = Generic.getAllUnitTags(u);
		for(Item i : u.slotmap.values())
			if(i != null)
			{
				tags.addAll(i.tags);
			}
				
		
		int dw_maxlength = 2;
		if(Generic.containsTag(tags, "dw_maxlength"))
			dw_maxlength = Integer.parseInt(Generic.getTagValue(tags, "dw_maxlength"));
		
		boolean regularok = true;
		if((nationGen.weapondb.GetInteger(it.id, "lgt") > dw_maxlength) || nationGen.weapondb.GetInteger(it.id, "2h") == 1)
		{
			regularok = false;
		}
		
		if(Generic.containsTag(tags, "ignore_dw_restrictions"))
			regularok = true;
		
		// System.out.println(regularok + " - " + i.tags);
		return regularok;
		
	}
	
	protected double getDualWieldChance(Unit u, double basechance)
	{
		
		double local_dwchance = basechance;

		List<String> tags = Generic.getAllUnitTags(u); 
				
		
		List<String> values = Generic.getTagValues(tags, "dwchance");
		if(values.size() > 0)
		{
			List<Double> intvalues = new ArrayList<Double>();
			for(String s : values)
				intvalues.add(Double.parseDouble(s));
			
			
			double largest = intvalues.get(0);
			for(double d : intvalues)
				if(d > largest)
					largest = d;
			
			local_dwchance = largest;
		}
		
		values = Generic.getTagValues(tags, "dwchancebonus");
		if(values.size() > 0)
		{
			for(String s : values)
				local_dwchance +=  Double.parseDouble(s);	
		}
		
		return local_dwchance;
	
	}
	
	private int getCavVarieties(Pose p)
	{
		int stuff = 0;
		
		ItemSet tempweps = new ItemSet();
		ItemSet removethese = new ItemSet();
		tempweps.addAll(p.getItems("weapon"));
		for(Item i : tempweps)
		{
			if(i.id.equals("357") || i.tags.contains("lightlance"))
			{
				removethese.add(i);
				stuff = 1;
			}
		}
		tempweps.removeAll(removethese);
		
		boolean has1h = has1H(tempweps);
		if(has1h)
			stuff++;
		boolean has2h = (has2H(tempweps));
		if(has2h)
			stuff++;
	
		
		return stuff;
	}
	
	
	/** 
	 * For montag units
	 * @param u
	 * @return
	 */
	public Unit equipUnit(Unit u)
	{

		Pose pose = u.pose;
		Race race = u.race;

		unitGen.addFreeTemplateFilters(u);
		if(random.nextDouble() < 0.1 && canGetMoreFilters(u))
			addTemplateFilter(u, "trooptemplates", "default_templates");

		this.removeEliteSacred(u, u.guessRole());
		

		
		unitGen.armorUnit(u, u.pose.getItems("armor"), null, null, false);

	
		
		String role = "infantry";
		if(pose.roles.contains("mounted") || pose.roles.contains("sacred mounted") || pose.roles.contains("elite mounted"))
			role = "mounted";
		if(pose.roles.contains("ranged") || !pose.roles.contains("sacred ranged") || !pose.roles.contains("elite ranged"))
			role = "ranged";
		
		TroopTemplate t = TroopTemplate.getNew(u.getSlot("armor"), race, u, role, u.pose, this);
	
		boolean success = false;
		if(!role.equals("mounted"))
		{
			success = armInfantry(u, t);
			
		}
		else 
			success = armCavalry(u, t);
	
	
		if(!success)
		{
			u = null;
		}

		// Bonusweapon
			
		if(u != null)
			this.equipBonusWeapon(u, role, race, t);
		
		//unitGen.handleExtraGeneration(u);
		this.armSpecialSlot(u, null, used);

		cleanUnit(u);
		

		
		return u;
	}
	
	/**
	 * Generates an unit
	 * @param role
	 * @param race
	 * @param amount
	 * @param minprot
	 * @param maxprot
	 * @param isPrimaryRace
	 * @return
	 */
	public Unit generateUnit(TroopTemplate t)
	{
		Unit unit = null;
		

		int cycles = 0;
		do
		{

			cycles++;
			if(cycles > 50)
			{
				break;
			}
			
	
	

			// Copy unit
			unit = t.template.getCopy();

	
			boolean success = false;
			if(!t.role.equals("mounted"))
			{
				success = armInfantry(unit, t);

			}
			else
			{
				success = armCavalry(unit, t);
			}
			
			
			if(!success)
			{
				unit = null;
				continue;
			}
			else if(unit != null)
			{
				t.units.add(unit);
			}
			
			// Bonusweapon
				
			if(unit != null)
				this.equipBonusWeapon(unit, t.role, t.race, t);
			
			this.armSpecialSlot(unit, t, used);

	
		} while(unit == null);

		
		if(unit != null)
		{

			
			// Add everything to used;
			addToUsed(unit);
			
			// Clean equipment
			this.cleanUnit(unit);


	
			
			//System.out.println("CREATED A " + role + ": " + unit.getSlot("armor").name + " and " + unit.getSlot("weapon").name + " / " + unit.pose.roles + " | " + unit.pose.name);
			//if(unit.getSlot("hands") != null)
			//	System.out.println("DPEDPER: " + unit.getSlot("hands").name + " " + unit.getSlot("basesprite").name);
		}

		
		return unit;
	}
	
	protected boolean armInfantry(Unit unit, TroopTemplate t)
	{


		
		ItemSet possibleWeapons = t.template.pose.getItems("weapon");
		if(possibleWeapons.possibleItems() - t.weapons.size() <= 0)
		{
			unit = null;
			return false;
		}
		else
		{
			ItemSet tempweps = new ItemSet();
			for(TroopTemplate t2 : templates)
				if(t.armor.id.equals(t2.armor.id) && Math.abs(t.template.getHP() - t2.template.getHP()) < 3  && t.role.equals(t2.role))
				{
					tempweps.addAll(t.weapons);
				}
			

			
			Item weapon = this.getNewItem("weapon", t.role, unit, null, tempweps);
			unit.setSlot("weapon", weapon);

			t.weapons.add(weapon);
			

			if(t.role.equals("infantry") && unit.pose.getItems("offhand") != null && isDualWieldEligible(unit) && (unit.getSlot("offhand") == null || unit.getSlot("offhand").armor))
			{
				double local_dwchance = this.getDualWieldChance(unit, 0.05);
				
				if(random.nextDouble() < local_dwchance)
					
				{
					this.equipOffhand(unit, t.role, unit.race, t);
					
				
				}
			}
		
		}
		return true;
	}
	
	
	

	
	public boolean armSpecialSlot(Unit u, TroopTemplate t, ItemSet used)
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
			if(args.get(0).equals("additionalweaponslot"))
			{
				String slot = args.get(args.size() - 1);
				

				double chance = 1;
				if(args.size() > 2)
					chance = Double.parseDouble(args.get(1));
			
				if(random.nextDouble() < chance)
				{
										
					ItemSet newused = used.filterSlot(slot).filterForPose(u.pose);
					List<Item> oldItems = new ArrayList<Item>();
					
					if(t != null)
					{
						for(Unit unit : t.units)
						{
							if(unit.getSlot(slot) != null)
								oldItems.add(unit.getSlot(slot));
						}
					}
					
					newused.removeAll(oldItems);
					
					ItemSet select;
					if(newused.size() > 0)
						select = newused;
					else
					{
						select = new ItemSet();
						select.addAll(u.pose.getItems(slot));
						select.removeAll(oldItems);
					}
					
					if(select.size() > 0)
					{
						Item newitem = chandler.getRandom(select, u);
						u.setSlot(slot, newitem);
					}
				}
			}
		}
		
		return true;
	}
	

	

	
	protected boolean armCavalry(Unit unit, TroopTemplate t)
	{
		
			
			boolean hasllance = false;
			
			ItemSet tempweps = new ItemSet();
			ItemSet oldweps = new ItemSet();
			
			oldweps.addAll(t.weapons);
			ItemSet lances = new ItemSet();
			for(Item i : oldweps)
			{
				if(i.id.equals("357") || i.tags.contains("lightlance"))
				{
					lances.add(i);
					hasllance = true;
				}
			}
			oldweps.removeAll(lances);
			
			boolean has1h = has1H(oldweps);
			boolean has2h = (!has1H(oldweps) && oldweps.size() > 0);
			
			boolean done = false;
			int r = -1;
			
			tempweps.clear();

			while(!done)
			{
		
				r = random.nextInt(6);
				if((r == 0 || r == 1 || r == 2) && !has1h) // 1h
				{

					tempweps.addAll(t.pose.getItems("weapon").filterDom3DB("2h", "0", true, nationGen.weapondb));
					tempweps.removeAll(oldweps);
					for(Item i : t.pose.getItems("weapon"))
					{
						if(i.id.equals("357") || i.tags.contains("lightlance"))
							tempweps.remove(i);
					}
					done = true;
				}
				else if(r == 3 && !has2h) // 2h
				{

					tempweps.addAll(t.pose.getItems("weapon").filterDom3DB("2h", "1", true, nationGen.weapondb));
					tempweps.removeAll(oldweps);
					for(Item i : t.pose.getItems("weapon"))
					{
						if(i.id.equals("357") || i.tags.contains("lightlance"))
							tempweps.remove(i);
					}
					done = true;
				}
				else if((r == 4 || r == 5) && !hasllance) // lightlance
				{
					for(Item i : t.pose.getItems("weapon"))
					{
						if(i.id.equals("357") || i.tags.contains("lightlance"))
						{
							tempweps.add(i);
						}
					}
					if(tempweps.size() > 0)
						done = true;
				}
			}
				
			Item weapon = this.getNewItem("weapon", "mounted", unit, tempweps, null);
			

			
			unit.setSlot("weapon", weapon);

			t.weapons.add(weapon);
			
			
			// Lance
			if(r < 3 && t.pose.getItems("lanceslot") != null)
			{
				
				int ap = 0;
				for(Command c : t.template.getSlot("mount").commands)
				{
					if(c.command.equals("#ap"))
						ap = Integer.parseInt(c.args.get(0));
				}
				
				int lancelimit = 5 + random.nextInt(25);
				if(random.nextDouble() > 0.75)
					lancelimit = lancelimit * 2;

				boolean getsLance = (ap >= lancelimit);
				getsLance = true;
				if(getsLance && t.pose.getItems("lanceslot").size() > 0)
				{
					tempweps.clear();
					for(Item i : t.pose.getItems("lanceslot"))
					{
						if(!i.id.equals("4") && !i.tags.contains("lance"))
							tempweps.add(i);
					}
					
		
					Item lance = this.getNewItem("lanceslot", "mounted", unit, tempweps, null);
					unit.setSlot("lanceslot", lance);

				}
				
				
			}

			
			return true;
			
	}


	
	
	

	
	


	
	

	public void addTemplateFilter(Unit u, String query, String defaultv)
	{
		
		// If we have #onlyfilter, skip!
		if(!canGetMoreFilters(u))
			return;
			
		List<Filter> possibleFilters = new ArrayList<Filter>();
		possibleFilters.addAll(this.unitTemplates);
		possibleFilters.removeAll(u.appliedFilters);
		
		// Add unit template filter to available template filters
		if(unitTemplates.size() < maxdifferenttemplates || possibleFilters.size() == 0)
		{
			List<Filter> tFilters = ChanceIncHandler.retrieveFilters(query, defaultv, assets.templates, u.pose, u.race);
			
			possibleFilters.retainAll(tFilters);
			
			// Remove #onlyfilter
			List<Filter> removef = new ArrayList<Filter>();
			List<Filter> match = new ArrayList<Filter>();
			match.addAll(u.appliedFilters);
			match.retainAll(tFilters);
			
			for(Filter f : tFilters)
				if(f.tags.contains("onlyfilter") && match.size() > 0)
					removef.add(f);
			

			tFilters.removeAll(removef);
			tFilters.removeAll(unitTemplates);		
			tFilters.removeAll(u.appliedFilters);	
						
			tFilters = ChanceIncHandler.getValidUnitFilters(tFilters, u);
			

			
			

			
			tFilters.removeAll(removef);
			
			

			Filter t = chandler.getRandom(tFilters, u);
			if(t != null)
			{

				unitTemplates.add(t);
				possibleFilters.add(t);
			}
		
		}
		
		List<Filter> match = new ArrayList<Filter>();
		for(Filter f : possibleFilters)
			if(Generic.containsTag(f.tags, "maxunits"))
			{
				int max = Integer.parseInt(Generic.getTagValue(f.tags, "maxunits"));
				int count = 1;
				for(TroopTemplate t : this.templates)
					if(t.template.appliedFilters.contains(f))
						count++;
				
				
				if(count >= max)
				{
					match.add(f);
				}
			}
		possibleFilters.removeAll(match);

		Filter f = chandler.getRandom(possibleFilters, u);
		if(f != null)
		{
			appliedtemplates++;
			u.appliedFilters.add(f);
		}
	}
	
	
	private boolean canGetMoreFilters(Unit u)
	{
		boolean ok = true;
		for(Filter f : u.appliedFilters)
			if(f.tags.contains("onlyfilter"))
				ok = false;
		
		return ok;
	}
	public void addInitialFilters(Unit u, String role)
	{

		unitGen.addFreeTemplateFilters(u);

		
		double templatechance = 0.25;
		if(Generic.getTagValue(Generic.getAllUnitTags(u), "trooptemplatechance") != null)
		{
			templatechance = Double.parseDouble(Generic.getTagValue(Generic.getAllUnitTags(u), "trooptemplatechance"));
		}
		
		if((random.nextDouble() < templatechance || appliedtemplates < maxtemplates) && canGetMoreFilters(u))
		{

			addTemplateFilter(u, "trooptemplates", "default_templates");
			if(random.nextDouble() < 0.125  && canGetMoreFilters(u))
			{
				addTemplateFilter(u, "trooptemplates", "default_templates");
			}
		}	

		removeEliteSacred(u, role);

	}
	
	protected void removeEliteSacred(Unit u, String role)
	{
		// Remove elite and sacred items
		Filter tf = new Filter(nationGen);
		tf.name = Generic.capitalize(role) + " unit";
		
		tf.tags.add("not_montag_inheritable");
		
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

		u.appliedFilters.add(tf);
	}

	public void addToUsed(Unit unit)
	{
		
		for(Item i : unit.slotmap.values())
			if(!used.contains(i))
				used.add(i);
		
	}

	private void equipOffhand(Unit u, String role, Race race, TroopTemplate t)
	{
		ItemSet stuff = new ItemSet();
		
		if(u.pose.getItems("offhand") == null)
			return;
		
		int rolls = 0;
		while(stuff.possibleItems() == 0 && rolls < 20)
		{

			rolls++;
			double roll = random.nextDouble();
			if(roll < 0.66)
			{
				stuff = u.pose.getItems("offhand").filterArmor(false);
			}
			else
			{
				Item offhand = chandler.getRandom(u.pose.getItems("offhand").filterArmor(false).getItemsWithID(u.getSlot("weapon").id, "offhand"), u);
				if(offhand != null)
					stuff.add(offhand);
			}

		}
		
		if(stuff.possibleItems() > 0)
		{
			Item wep = Entity.getRandom(random, chandler.handleChanceIncs(u, stuff));
			u.setSlot("offhand", wep);
		}
		
	}
	
	
	protected double getBonusWeaponChance(Unit u)
	{
		double local_bwchance = 0;
		List<String> tags = Generic.getAllUnitTags(u);
		List<String> values = Generic.getTagValues(tags, "bonusweaponchance");
		if(values.size() > 0)
		{
			List<Double> intvalues = new ArrayList<Double>();
			for(String s : values)
				intvalues.add(Double.parseDouble(s));
			
			
			double largest = intvalues.get(0);
			for(double d : intvalues)
				if(d > largest)
					largest = d;
			
			local_bwchance = largest;
		}
		
		values = Generic.getTagValues(tags, "bonusweaponchancebonus");
		if(values.size() > 0)
		{
			for(String s : values)
				local_bwchance +=  Double.parseDouble(s);	
		}
		
		return local_bwchance;
	
		
	}
	
	private void equipBonusWeapon(Unit u, String role, Race race, TroopTemplate t)
	{
		if(role.equals("ranged"))
			return;
		
		
		ItemSet bonuses = used.filterSlot("bonusweapon").filterForPose(t.pose);
		if(bonuses.possibleItems() < 1 || random.nextDouble() < 0.5)
		{
			bonuses.add(this.getNewItem("bonusweapon", role, u, bonuses));
		}
		
		bonuses = bonuses.filterForPose(t.pose);
		
		Item bonusweapon = Entity.getRandom(random, chandler.handleChanceIncs(u, bonuses));
		if(bonusweapon == null)
			return;
		
		// Tieruniqueness.
		if(bonusweapon.tags.contains("tierunique"))
		{
			for(TroopTemplate t2 : templates)
			{
				if(t2.bonusweapons.getItemWithID(bonusweapon.id, "bonusweapon") != null && t2.armor.id.equals(u.getSlot("armor").id))				
				{
					return;
				}
			}
		}
		
		// Prot level
		int maxprot = 100;
		int minprot = 0;
		int totalprot = u.getTotalProt(false);
		

		if(Generic.containsTag(u.race.tags, "zeroarmor"))
			totalprot -= Integer.parseInt(Generic.getTagValue(u.race.tags, "zeroarmor"));
		if(Generic.containsTag(bonusweapon.tags, "maxprot"))
			maxprot = Integer.parseInt(Generic.getTagValue(bonusweapon.tags, "maxprot"));
		if(Generic.containsTag(bonusweapon.tags, "minprot"))
			minprot = Integer.parseInt(Generic.getTagValue(bonusweapon.tags, "minprot"));
		totalprot = Math.max(0, totalprot);
		
		if(totalprot < minprot || totalprot > maxprot)
			return;
		
		


		double local_bwchance = 0.05 + this.getBonusWeaponChance(u);
	
	
		
		double chance = bonusrangedness + local_bwchance;


		
		double rescost = u.getResCost(false);
		
		rescost += 4 * nationGen.weapondb.GetInteger(u.getSlot("weapon").id, "res");
		if(nationGen.weapondb.GetValue(u.getSlot("weapon").id, "2h").equals("1"))
			rescost += 2 * nationGen.weapondb.GetInteger(u.getSlot("weapon").id, "res");


		
		if(u.getSlot("offhand") != null && u.getSlot("offhand").armor)
			rescost += 4 * nationGen.armordb.GetInteger(u.getSlot("offhand").id, "res");
		else if(u.getSlot("offhand") != null)
			rescost += 4 * nationGen.weapondb.GetInteger(u.getSlot("offhand").id, "res");
		
		if(nationGen.weapondb.GetInteger(u.getSlot("weapon").id, "dmg") <= 4)
			rescost *= 0.75;
		
		if(role.equals("mounted")) // +15% res cost for calculations for cavalry
			rescost = rescost * 1.15;
		if(u.getSlot("lanceslot") != null) // +15% if there already is a lance
			rescost = rescost * 1.15;

		
		if((rescost - 6) / 32 < chance)
		{
			u.setSlot("bonusweapon", bonusweapon);
			used.add(bonusweapon);
			t.bonusweapons.add(bonusweapon);
		}
		
	}

	protected void cleanUnit(Unit u)
	{
		// TODO: Handle more than one hand \:D/
		Item weapon = u.getSlot("weapon");
		
		
	
		boolean twohand = nationGen.weapondb.GetValue(weapon.id, "2h").equals("1");
		if(twohand && !this.isDualWieldEligible(u))
			u.setSlot("offhand", null);
	
	}
	

	

	
	



	
	public Item getNewItem(String slot, String role, Race race, Pose p, ItemSet used)
	{
		Unit u = new Unit(nationGen, race, p);
		return this.getNewItem(slot, role, u,  p.getItems(slot), used);
	}
	
	public Item getNewItem(String slot, String role, Unit u, ItemSet used)
	{
		return getNewItem(slot, role, u, u.pose.getItems(slot), used);
	}

	public Item getNewItem(String slot, String role, Unit u, ItemSet all, ItemSet used)
	{

		Pose p = null;
		Race race = null;
		
		if(u != null)
		{
			p = u.pose;
			race = u.race;
		}
		

		
		if(p != null && p.getItems(slot) == null)
			return null;
		
		ItemSet old;
		if(p == null)
			old = this.used.filterSlot(slot).filterForRole(role, race);
		else
			old = this.used.filterSlot(slot).filterForPose(p);
		
		old.retainAll(race.getItems(slot, role));
		
	
		if(all == null && p != null)
			all = p.getItems(slot);

		
		if(p != null && u != null && chandler.countPossibleFilters(all, u) == 0)
			all = p.getItems(slot);


		
		old = old.filterImpossibleAdditions(used);
		all = all.filterImpossibleAdditions(used);
		
		if(p != null)
		{
			old = old.filterForPose(p);
			all = all.filterForPose(p);
		}
		
		// Infantry has a high chance of onehanders if available
		if(slot.equals("weapon") && role.equals("infantry"))
		{
			if(!this.has1H(used) && random.nextDouble() < 0.5)
			{
				if(has1H(all))
					all = all.filterDom3DB("2h", "0", true, nationGen.weapondb);
				if(has1H(old))
					old = old.filterDom3DB("2h", "0", true, nationGen.weapondb);
			}
		}
		

		// Old is the ideal set
		ItemSet select = old;
		
		// Check whether we should ditch using old
		LinkedHashMap<Item, Double> map = null;
		if(u != null)
			map = chandler.handleChanceIncs(u, p.getItems(slot));
		else
			map = chandler.handleChanceIncs(new Unit(nationGen, race, p, nation), race.getItems(slot, role));
	
		double allsum = 0;
		double oldsum = 0;
		for(Item i : map.keySet())
		{
			if(old.contains(i))
				oldsum += map.get(i);
			if(p.getItems(slot).contains(i))
				allsum += map.get(i);
		}
		

		
		// If nothing is available or under 5% of probability stuff is used, use all
		if(chandler.countPossibleFilters(select, u) == 0 || (oldsum/allsum) < 0.05)
		{
			select = all;
		}
		


		
		// If nothing was possible, get a general solution
		if(chandler.countPossibleFilters(select, u) == 0)
		{
			if(p == null)
				select = race.getItems(slot, role);
			else
				select = p.getItems(slot);
		}
		
		// Chance to just select whatever
		if(random.nextDouble() < 0.5 && chandler.countPossibleFilters(all, u) > 0)
			select = all;
		
		
		if(u != null)
			return Entity.getRandom(random, chandler.handleChanceIncs(u, select));
		else
			return Entity.getRandom(random, chandler.handleChanceIncs(new Unit(nationGen, race, p, nation), select));

	}
	
	public boolean foundInSet(Item item, ItemSet set)
	{
		for(Item i : set)
			if(i.id.equals(item.id))
				return true;
		
		return false;
	}
	
	public boolean has1H(ItemSet used)
	{
		used = used.filterSlot("weapon").filterDom3DB("rng", "0", true, nationGen.weapondb).filterDom3DB("2h", "0", true, nationGen.weapondb);
		return(used.size() > 0);
	}
	
	public boolean has2H(ItemSet used)
	{
		used = used.filterSlot("weapon").filterDom3DB("rng", "0", true, nationGen.weapondb).filterDom3DB("2h", "1", true, nationGen.weapondb);
		return(used.size() > 0);
	}
	
	public List<Pose> getPosesWith(Race race, String role, Item item)
	{
		List<Pose> poses = new ArrayList<Pose>();
		for(Pose pose : race.poses)
			if(pose.getItems(item.slot) != null && pose.roles.contains(role))
				for(Item item2 : pose.getItems(item.slot))
				{
					boolean ok = false;
					if(item2 == item)
						ok = true;
					
					/*
					if(item.id.equals("-1") && item2.id.equals("-1") && item.sprite.equals(item2.sprite))
						ok = true;
					else if(item.id.equals(item2.id) && item.name.equals(item2.name))
						ok = true;
					*/
					
					if(ok && !poses.contains(pose))
						poses.add(pose);
						
				}
			
		return poses;
	}
	

	public Item getMatchingItem(Pose p, String role, Item item)
	{
	

		List<Item> items = new ArrayList<Item>();
		
		for(Item item2 : p.getItems(item.slot))
		{
			boolean ok = false;
			if(item.id.equals("-1") && item2.id.equals("-1") && item.sprite.equals(item2.sprite))
				ok = true;
			else if(item.id.equals(item2.id) && item.name.equals(item2.name))
				ok = true;
			
			if(ok && !items.contains(item2))
				items.add(item2);
				
		}
		
		if(items.size() == 0)
			for(Item item2 : p.getItems(item.slot))
			{
				boolean ok = false;
				if(item.id.equals("-1") && item2.id.equals("-1") && item.sprite.equals(item2.sprite))
					ok = true;
				else if(item.id.equals(item2.id))
					ok = true;
				
				if(ok && !items.contains(item2))
					items.add(item2);
					
			}
			
		return Entity.getRandom(random, chandler.handleChanceIncs(items));
	}
	
	
	

	
	



	
}
