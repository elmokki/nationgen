package nationGen.rostergeneration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.Entity;
import nationGen.entities.Filter;
import nationGen.entities.MagicItem;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.CustomItem;
import nationGen.items.CustomItemGen;
import nationGen.items.Item;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.misc.ItemSet;
import nationGen.nation.Nation;
import nationGen.rostergeneration.montagtemplates.SacredMontagTemplate;
import nationGen.units.Unit;

public class SacredGenerator extends TroopGenerator {

	ItemSet usedItems = new ItemSet();
	public SacredGenerator(NationGen g, Nation n) {
		super(g, n, "sacgen");
		
		String[] slots_array = {"armor", "weapon", "offhand", "bonusweapon", "helmet", "cloakb", "hair"};
		for(Unit u : this.nation.generateTroopList())
		{
			for(String slot : slots_array)
			{
				Item i = u.getSlot(slot);
				if(i == null)
					continue;
				
				if(!usedItems.contains(i))
					usedItems.add(i);
			}
		}
		
		
	}
	
	private void addEpicness(Unit u, boolean sacred, int power)
	{

		int origpower = power;
		
		// Determine stat boost amount
		double extra = 0;
		

		int powerups = 1;
		
		if(random.nextDouble() < 0.25)
		{
			powerups++;
		}
		if(random.nextDouble() < 0.25)
		{
			powerups++;
		}
		
		if(random.nextDouble() < 0.07)
		{
			powerups -= 2;
			power++;
			origpower++;
		}
		

		

		// Determine magic resistance
		int mradd = power * 1/2;
		if(mradd > 4)
			mradd = 4;

		
		int origmr = u.getCommandValue("#mr", 10);
		if(origmr - 10 > 0)
		{
			mradd = mradd - ((origmr - 10) / 2);
		}
		
		u.commands.add(new Command("#mr", "+" + mradd));
		
		
		// Determine morale
		int origmor = u.getCommandValue("#mor", 10);
		
		int morbonus = power + random.nextInt(2); 
		if(random.nextDouble() > 0.25 *  Math.max(1, 1 + (origmor - 10) / 4))
			morbonus++;
		if(random.nextDouble() > 0.25 *  Math.max(1, 1 + (origmor - 10) / 4))
			morbonus++;
		
		if(origmor + morbonus > 16)
			morbonus -= (origmor + morbonus - 16) / 2;
			
		u.commands.add(new Command("#mor", "+" + morbonus ));


		// Filters and weapons
		List<Filter> filters = new ArrayList<Filter>();
		if(sacred)
		{
			String[] defaults = {"default_sacredfilters", "default_sacredfilters_shapeshift"};
			filters = ChanceIncHandler.retrieveFilters("sacredfilters", defaults, nationGen.filters, u.pose, u.race);
		
		}
		else
		{
			String[] defaults = {"default_elitefilters", "default_elitefilters_shapeshift"};
			filters = ChanceIncHandler.retrieveFilters("elitefilters", defaults, nationGen.filters, u.pose, u.race);
		}
		
		filters = ChanceIncHandler.getValidUnitFilters(filters, u);
		LinkedHashMap<Filter, Double> derp = chandler.handleChanceIncs(u, filters);
		filters.retainAll(derp.keySet());
		
		int filterCount = 0;
		boolean weapon = false;
		

		
		int maxfilters = Math.max(Math.min(power / 2, 5), 3);
		

		int cycles = 0;
		while(power > 0 && filterCount <= maxfilters)
		{

			cycles++;
			if(cycles > 50)
			{
				break;
			}
			

			double rand = random.nextDouble();
			
			boolean guaranteeds = false;
			String[] tslots = {"weapon", "bonusweapon", "offhand"};
			for(String s : tslots)
			{
				if(u.getSlot(s) != null && u.getSlot(s).tags.contains("guaranteedmagic") && !u.getSlot(s).armor)
					guaranteeds = true;
			}
			
			
			
			// Magic weapon is handled later since no weapons exist when this is run.
			if((guaranteeds || (sacred && rand < 0.07 + power*0.03) || (chandler.countPossibleFilters(filters, u) == 0)) && !weapon)
			{

				weapon = true;
				int cost = 1 + random.nextInt(Math.min(6, power));
				u.tags.add("NEEDSMAGICWEAPON " + cost);
				power -= cost;

			}
			// Add more stat boosts
			else if(random.nextDouble() < 1.1 - powerups / (double)origpower)
			{

				power--;
				powerups++;
			}
			// Add a filter
			else
			{
		
				
				List<Filter> choices = ChanceIncHandler.getFiltersWithPower(power, power, filters);
				
				if(choices.size() == 0 || random.nextDouble() > 0.8)
					choices = ChanceIncHandler.getFiltersWithPower(power - 1, power, filters);
				
				if(choices.size() == 0)
				{
					int range = 1;
					while(range < 20 && chandler.countPossibleFilters(choices, u) == 0)
					{
						choices = ChanceIncHandler.getFiltersWithPower(power - range, power, filters);
						range++;
					}
				}
				if(choices.size() == 0 || power <= 2 || (power <= 3 && random.nextDouble() < 0.35))
					choices = ChanceIncHandler.getFiltersWithPower(-100, power, filters);
				

				if(random.nextDouble() < 0.05)
				{
					List<Filter> maybe = ChanceIncHandler.getFiltersWithPower(-100, -1, filters);
					if(choices.size() > 0)
						choices = maybe;
					

				}
				
				choices = ChanceIncHandler.getValidUnitFilters(choices, u);

				if(choices.size() == 0)	
				{
					break;
				}
				Filter f = Filter.getRandom(random, chandler.handleChanceIncs(u, choices));
				
				if(f != null && ChanceIncHandler.canAdd(u, f))
				{
					chandler.removeRelated(f, filters);
					u.appliedFilters.add(f);
					u.tags.addAll(f.tags);
					power -= f.power;
					filterCount++;
					
				}
				

				
			}
		}


		
		// Extra stats
		powerups += power;
		powerups *= 3;
		powerups = (int) Math.round(0.5*(double)powerups + 0.75*random.nextDouble()*(double)powerups);
		
		int precadded = 0;
		int encadded = 0;
		String[] posup = {"#att", "#def", "#prec", "#enc", "#mr", "#hp"};
		int[] defvals = {10, 10, 10, 3, 10, u.getCommandValue("#hp", 10)};
		if(u.getCommandValue("#hp", 10) < 10)
			defvals[5] = u.getCommandValue("#hp", 10) - Math.min(3, 10 - u.getCommandValue("#hp", 10));
		
		
		
		while(powerups > 0)
		{
			// Generate probabilitites for powerups
			List<Filter> ups = new ArrayList<Filter>();
			for(int j = 0; j < posup.length; j++)
			{
				String s = posup[j];

				double dif = u.getCommandValue(s, defvals[j]) - defvals[j];

				// Lower enc is better
				if(s.equals("#enc"))
					dif = -dif;
				
				double price = 1;
				double divisor = 2;
		
		
				

				if(dif > 0)
					price *= (1 + Math.pow(dif/divisor, 2));
				else if(dif < 0)
					price *= 0.8 + 0.2/(1+Math.abs(dif));
				


		
				if(s.equals("#enc"))
					price *= 1.5;
				
				if(u.getCommandValue("#enc", 3) < 2 || encadded > 1 && s.equals("#enc"))
					price = 100;
				if(precadded > 0 && s.equals("#prec"))
					price = 100;
				

				
				if(price > 0 && price <= powerups)
					
				{
					Filter f = new Filter(nationGen);
					f.name = s;
				
					f.basechance = 1/(Math.pow(price, 2));
					f.power = price;
					
					if(s.equals("#enc"))
						f.basechance *= 0.5;
					if(!u.isRanged() && s.equals("#prec"))
						f.basechance *= 0.05;
					
					ups.add(f);
				}

			}
			
			if(ups.size() == 0)
				break;
			
			Filter f = Entity.getRandom(random, ups);
			
			if(f.name.equals("#enc"))
			{
				encadded++;
				u.commands.add(new Command(f.name, "-1"));
			}
			else if(f.name.equals("#hp"))
			{
				double amount = Math.max(defvals[5] * 0.05, 1);
				u.commands.add(new Command(f.name, "+" + (int)Math.round(amount)));
			}
			else
			{
				u.commands.add(new Command(f.name, "+1"));

			}
			if(f.name.equals("#prec"))
				precadded++;
			
			if(f.power == 0)
				System.out.println("ZERO POWER");
			
			powerups -= f.power;
			extra += 0.33;
			
			
		}


		u.commands.add(new Command("#gcost", "+" + (int)Math.round(extra)));
		
		
	}
	
	
	private void giveMagicWeapons(Unit u, int power)
	{
		

		
		CustomItemGen ciGen = new CustomItemGen(nation);
		List<MagicItem> magicItems = new ArrayList<MagicItem>();

		magicItems = ChanceIncHandler.retrieveFilters("magicitems", "defaultprimary", nationGen.magicitems, u.pose, u.race);

		CustomItem item = null;
		
		if(!u.getSlot("weapon").tags.contains("notepic"))
			item = ciGen.getMagicItem(u, u.getSlot("weapon"), power, random.nextDouble(), magicItems);
		
		if(item != null)
		{
			u.setSlot("weapon", item);

			double loss = 1;
			if(item.magicItem != null)
				loss = item.magicItem.power;
			
			power -= loss;
		
		}
		
		if(u.getSlot("bonusweapon") != null && !u.getSlot("bonusweapon").tags.contains("notepic") && (random.nextDouble() > 0.75 || u.getSlot("bonusweapon").tags.contains("guaranteedmagic")  || power >= 2))
		{
			CustomItem item2 = ciGen.getMagicItem(u, u.getSlot("bonusweapon"), 1, random.nextDouble(), magicItems);
			if(item2 != null)
				u.setSlot("bonusweapon", item2);
		}
		
		if(u.getSlot("offhand") != null && !u.getSlot("offhand").tags.contains("notepic") && !u.getSlot("offhand").armor && (random.nextDouble() > 0.75 || u.getSlot("offhand").tags.contains("guaranteedmagic") || power >= 3))
		{
			CustomItem item2 = ciGen.getMagicItem(u, u.getSlot("offhand"), power, random.nextDouble(), magicItems);
			if(item2 != null)
				u.setSlot("offhand", item2);
		}
		
	}
	
	private void addInitialFilters(Unit u)
	{
		
		unitGen.addFreeTemplateFilters(u);
		addTemplateFilter(u, "sacredtemplates", "default_sacredtemplates");
		if(random.nextDouble() < 0.1)
		{
			addTemplateFilter(u, "sacredtemplates", "default_sacredtemplates");
		}
	
	}
	
	private boolean canBeSacred(boolean sacred, Race race)
	{
		
		boolean canBeSacred = false;
		String[] roles = {"infantry", "mounted", "chariot", "ranged"};
		for(String r : roles)
		{
			if(race.hasRole(r) || (race.hasRole("sacred " + r) && sacred))
				canBeSacred = true;
		}
		
		return canBeSacred;
	}

	public Unit generateUnit(boolean sacred, int power)
	{
		return generateUnit(sacred, power, null);
	}
	
	
	public Unit generateUnit(boolean sacred, int power, Race race)
	{
		
		if(race == null)
			race = getRace(sacred);
		
		
		Pose p = getPose(sacred, power, race);
		
		

		
		double epicchance = random.nextDouble() * 0.5 + power * 0.25 + 0.25;

		Unit u = this.unitGen.generateUnit(race, p);

		if(Generic.containsTag(Generic.getAllUnitTags(u), "innate_sacred_power"))
		{
			power -= Integer.parseInt(Generic.getTagValue(Generic.getAllUnitTags(u), "innate_sacred_power"));
			
			if(sacred)
				power = Math.max(1, power);
			else
				power = Math.max(0, power);
			
		}
		
		u = getSacredUnit(u, power, sacred, epicchance);
		
		if(unitGen.hasMontagPose(u))
		{
			SacredMontagTemplate template = new SacredMontagTemplate(nation, nationGen);
			template.power = power;
			template.sacred = sacred;
			unitGen.handleMontagUnits(u, template, "montagsacreds");
			u.caponly = true;
		}
		else
		{
			calculatePower(u, power);
		}
		return u;
		
	}
	
	private Race getRace(boolean sacred)
	{

		
		Race race = null;
		race = nation.races.get(0);
		boolean foreigners = false;
		for(Unit u : nation.comlists.get("mages"))
		{
			if(u.race != race)
				foreigners = true;
		}
		for(Unit u : nation.generateTroopList())
		{
			if(u.race != race)
				foreigners = true;
		}

		double bonussecchance = 1;
		if(Generic.containsTag(race.tags, "secondaryracesacredmod"))
			bonussecchance += Double.parseDouble(Generic.getTagValue(race.tags, "secondaryracesacredmod"));
		if(Generic.containsTag(nation.races.get(1).tags, "primaryracesacredmod"))
			bonussecchance -= Double.parseDouble(Generic.getTagValue(nation.races.get(1).tags, "primaryracesacredmod"));
		
		
		if(foreigners || random.nextDouble() < 0.05 * bonussecchance)
		{
	
			if(random.nextDouble() < 0.2 * bonussecchance && nation.races.get(1) != null && canBeSacred(sacred, nation.races.get(1)))
				race = nation.races.get(1);
			
		}
		else if(!canBeSacred(sacred, nation.races.get(0)))
		{
			race = nation.races.get(1);
		}
		
		return race;
		
	}
	
	public Pose getPose(boolean sacred, int power, Race race)
	{
		// Handle sacred power settings
		double extrapower = this.nationGen.settings.get("sacredpower") - 1;
	
		
		power = (int) (power + power * extrapower * (1 + random.nextDouble() * 0.5) + extrapower);
		
	

		List<Pose> possibleposes = new ArrayList<Pose>();
		
		LinkedHashMap<String, Double> roles = new LinkedHashMap<String, Double>();
		roles.put("infantry", 1.0);
		roles.put("mounted", 0.25);
		roles.put("ranged", 0.125);
		roles.put("chariot", 0.05);
		
		String toGet = "sacred";
		if(Generic.containsTag(race.tags, toGet + "infantrychance"))
			roles.put("infantry", Double.parseDouble(Generic.getTagValue(race.tags, toGet + "infantrychance")));
		if(Generic.containsTag(race.tags, toGet + "mountedchance"))
			roles.put("mounted", Double.parseDouble(Generic.getTagValue(race.tags, toGet + "mountedchance")));
		if(Generic.containsTag(race.tags, toGet + "chariotchance"))
			roles.put("chariot", Double.parseDouble(Generic.getTagValue(race.tags, toGet + "chariotchance")));
		if(Generic.containsTag(race.tags, toGet + "rangedchance"))
			roles.put("ranged", Double.parseDouble(Generic.getTagValue(race.tags, toGet + "rangedchance")));

		
		if(!race.hasSpecialRole("infantry", sacred))
			roles.remove("infantry");
		if(!race.hasSpecialRole("ranged", sacred))
			roles.remove("ranged");
		if(!race.hasSpecialRole("chariot", sacred))
			roles.remove("chariot");
		if(!race.hasSpecialRole("mounted", sacred))
			roles.remove("mounted");
		

		String role = "";
		while(roles.size() > 0)
		{

			List<Filter> pf = new ArrayList<Filter>();
			for(String r : roles.keySet())
			{
				Filter f = new Filter(nationGen);
				f.name = r;
				f.basechance = roles.get(r);
				pf.add(f);
			}
			
			Filter f = chandler.getRandom(pf);
			if(f == null)
				break;
			
			roles.remove(f.name);
			role = f.name;
			

			for(Pose p : race.poses)
			{

				boolean isSacred = false;
				for(String trole : p.roles)
					if(trole.contains("sacred") && sacred || p.tags.contains("sacred") && sacred || trole.contains("elite") && !sacred || p.tags.contains("elite") && !sacred)
						isSacred = true;
				
				if(isSacred && ((sacred && p.roles.contains("sacred " + role)) || (!sacred && p.roles.contains("elite " + role)) || p.roles.contains(role)))
				{
					possibleposes.add(p);
				}
				else if(((race.tags.contains("all_troops_sacred") && sacred) || ((race.tags.contains("all_troops_elite") && !sacred))) && p.roles.contains(role))
				{
					possibleposes.add(p);
				}
			}
			
			//if(possibleposes.size() > 0)
				//break;
		}

		

		if(possibleposes.size() == 0)
		{
			System.out.println("WARNING: No sacred (" + sacred + " - actually elite if false) poses were found for " + race.name + ". Consider adding #all_troops_sacred or #all_troops_elite to race file to use normal poses.");
			return null;
		}
		
		Pose p = Pose.getRandom(random, chandler.handleChanceIncs(race, role, possibleposes));
		

		return p;
	}
	
	/**
	 * Adds some cost and caponlyness if unit is badass enough
	 * @param u
	 * @param role
	 * @param power
	 */
	public void calculatePower(Unit u, int power)
	{

		// Calculate some loose power rating
		double rating = 0;
		for(Filter f : u.appliedFilters)
			rating += Math.pow(2, f.power) / 2;
		
		// Rating should theoretically range from 0 to 4ish at this point
		
		
		if(u.pose.roles.contains("ranged") || u.pose.roles.contains("elite ranged") || u.pose.roles.contains("sacred ranged"))
		{
			if(u.getTotalProt() < 5)
				rating *= 2;
			else if(u.getTotalProt() < 8)
				rating *= 1.75;
			else if(u.getTotalProt() < 12)
				rating *= 1.5;
		}	
		else
		{
			if(u.getTotalProt() < 5)
				rating *= 0.25;
			else if(u.getTotalProt() < 8)
				rating *= 0.5;
			else if(u.getTotalProt() < 12)
				rating *= 0.8;
		}
		
		List<String> values = Generic.getTagValues(Generic.getAllUnitTags(u), "sacredratingmulti");
		double total = 1;
		for(String str : values)
		{
			total *= Double.parseDouble(str);
		}
		rating *= total;
		
		
		values = Generic.getTagValues(Generic.getAllUnitTags(u), "sacredcostmulti");
		total = 1;
		for(String str : values)
		{
			total *= Double.parseDouble(str);
		}
		u.commands.add(new Command("#gcost", "*" + total));

		
		// The highest caponlychance for the unit will apply if one is defined, unless the default formula is higher 
		values = Generic.getTagValues(Generic.getAllUnitTags(u), "caponlychance");
		double highestcaponlychance = 0;
		for(String str : values)
		{
			if(highestcaponlychance < Double.parseDouble(str))
				highestcaponlychance = Double.parseDouble(str);
		}
		
		if(highestcaponlychance < ((power + rating) / 10 + 0.3))
			highestcaponlychance = (power + rating) / 10 + 0.3;
		
		if(random.nextDouble() < highestcaponlychance)
			u.caponly = true;
		else
		{
			if(u.getGoldCost() <= 50)
				u.commands.add(new Command("#gcost", "+10"));
			else
				u.commands.add(new Command("#gcost", "*1.2"));

		}
	}
	

	
	public Unit getSacredUnit(Unit u, int power, boolean sacred, double epicchance)
	{

		Race race = u.race;
		
		//Unit u = this.unitGen.generateUnit(race, p);
		String role = "";
		
		// Filters
		this.addInitialFilters(u);
		



		// Add epic stuff
		if(!unitGen.hasMontagPose(u))
			this.addEpicness(u, sacred, power);
		
		// Equip
		for(String r : u.pose.roles)
			if(r.contains("infantry") || r.contains("sacred infantry"))
				role = "infantry";
			else if(r.contains("mounted") || r.contains("sacred mounted"))
				role = "mounted";
			else if(r.contains("chariot") || r.contains("sacred chariot"))
				role = "chariot";
			else if(r.contains("ranged") || r.contains("sacred ranged"))
				role = "ranged";
		
		
		if(role.equals(""))
		{
			System.out.println("WARNING: No role in some pose of " + race.name + ", possible pose name: " + u.pose.name + ", possible other roles: " + u.pose.roles);
			return null;
		}
		
		
		// Add a sacred generic filter
		Filter tf = new Filter(nationGen);
		
		if(sacred)
			tf.name = Generic.capitalize(role) + " sacred";
		else
			tf.name = Generic.capitalize(role) + " elite";
		
		tf.tags.add("not_montag_inheritable");

		if(sacred)
		{
			tf.themeincs.add("thisitemtheme elite *"  + epicchance * 20);
			tf.themeincs.add("thisitemtheme sacred *"   + epicchance * 50);
			tf.themeincs.add("thisitemtheme not sacred *"   + 1/epicchance * 0.50);
			tf.themeincs.add("thisitemtheme not elite *"   + 1/epicchance * 0.50);

		}
		else 
		{
			tf.themeincs.add("thisitemtheme elite *"  + epicchance * 50);
			tf.themeincs.add("thisitemtheme not elite *"   + 1/epicchance * 0.50);
			tf.themeincs.add("thisitemtheme sacred *0.05");
		}
		
		
		if(!sacred)
			u.tags.add("elite");
		if(sacred)
		{
			u.tags.add("sacred");
			tf.commands.add(new Command("#holy"));
		}
		u.appliedFilters.add(tf);
		
			
		// Equip
		unitGen.armorUnit(u, null, null, null, false);
		unitGen.armUnit(u, null, null, null, false);
		
		if(u.pose.getItems("offhand") != null && u.pose.getItems("offhand").possibleItems() > 0 && isDualWieldEligible(u) && (u.getSlot("offhand") == null || u.getSlot("offhand").armor))
		{
			ItemSet items = fetchItems(u, "offhand", sacred, epicchance);
			ItemSet weaps = items.filterArmor(false);
			
			double dwchance = this.getDualWieldChance(u, 0.15);
			Item item = null;
			if(weaps.size() > 0 && random.nextDouble() < dwchance)
			{
				item = chandler.getRandom(weaps, u);
			}

			if(item != null)
				u.setSlot("offhand", item);
		}

		if(u.pose.getItems("bonusweapon") != null && u.pose.getItems("bonusweapon").possibleItems() > 0)
		{
			int prot = nationGen.armordb.GetInteger(u.getSlot("armor").id, "prot");
			double local_bwchance = 0.4 + this.getBonusWeaponChance(u);
			if(random.nextDouble() < local_bwchance - (double)prot * 0.02)
			{
				Item weapon = chandler.getRandom(fetchItems(u, "bonusweapon", sacred, epicchance), u);
				u.setSlot("bonusweapon", weapon);
			}
		}

		if(u.pose.getItems("cloakb") != null && u.pose.getItems("cloakb").size() > 0 && random.nextDouble() > epicchance / 8)
		{
			
			Item weapon = chandler.getRandom(fetchItems(u, "cloakb", sacred, epicchance), u);
			u.setSlot("cloakb", weapon);
		}
		
		if(u.pose.roles.contains("ranged") || u.pose.roles.contains("sacred ranged") && u.pose.getItems("bonusweapon") != null && u.pose.getItems("bonusweapon").size() > 0)
		{
			Item weapon = chandler.getRandom(fetchItems(u, "bonusweapon", sacred, epicchance), u);
			u.setSlot("bonusweapon", weapon);
		}

		// Give magic weapons if they were promised:
		if(Generic.containsTag(u.tags, "NEEDSMAGICWEAPON") || (chandler.identifier.equals("herogen") && random.nextDouble() > 0.15))
		{
			
			int cost = 5;
			
			if(Generic.containsTag(u.tags, "NEEDSMAGICWEAPON"))
			{
				Integer.parseInt(Generic.getTagValue(u.tags, "NEEDSMAGICWEAPON"));
				u.tags.remove("NEEDSMAGICWEAPON " + cost);
			}
			giveMagicWeapons(u, cost);
		}

		// Clean up
		cleanUnit(u);

		// Adjust gcost
		adjustGoldCost(u, sacred);

		return u;
	}
	
	
	private void adjustGoldCost(Unit u, boolean sacred)
	{
		if(u.pose.types.contains("ranged") || u.pose.types.contains("elite ranged") || u.pose.types.contains("sacred ranged"))
			if(u.getGoldCost() < 15)
				u.commands.add(new Command("#gcost", "+10"));
			
		int cgcost = u.getGoldCost();
		
		cgcost -= 75;
		
		if(cgcost <= 0)
		{
			return;
		}
	
		
		int newprice = (int) Math.round(Math.pow(cgcost, 0.965));
		int discount = cgcost - newprice;
		
		if(sacred)
			discount = (int)Math.round(discount * (1/1.3));
		
		if(u.isRanged() && u.getGoldCost() - discount > 60)
		{
			discount += Math.sqrt(u.getGoldCost() - discount) * 2;
		}
	
		
		u.commands.add(new Command("#gcost", "-" + discount));
		

		

	}
	

	
	private ItemSet fetchItems(Unit u, String slot, boolean sacred, double epicchance)
	{
		ItemSet possibles = new ItemSet();
		
		if(u.pose.getItems(slot) == null)
			return possibles;
			

		if(possibles.size() == 0 && random.nextDouble() < 0.5)
			possibles = usedItems.filterForPose(u.pose).filterSlot(slot);
		if(possibles.size() == 0 || random.nextDouble() > epicchance * 1.5)
			possibles = u.pose.getItems(slot);
		
		if(possibles.size() == 0)
			System.out.println("No item in slot " + slot + " for a pose with roles " + u.pose.roles + " and race " + u.race.name); 
		
		
		return possibles;
	}

}
