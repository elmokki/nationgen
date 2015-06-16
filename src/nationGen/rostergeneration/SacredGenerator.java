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
import nationGen.units.Unit;

public class SacredGenerator extends TroopGenerator {

	ItemSet usedItems = new ItemSet();
	public SacredGenerator(NationGen g, Nation n) {
		super(g, n);
		
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

		double extra = 0;
		
		
		int powerups = Math.max(1, power - 2);
		
		if(random.nextDouble() < 0.5)
		{
			powerups++;
			if(random.nextDouble() < 0.25)
			{
				powerups++;
				if(random.nextDouble() < 0.125)
					powerups++;
			}
		}
		
		if(power > 2 && random.nextBoolean())
		{
			powerups++;
			power--;
			if(power > 2 && random.nextBoolean())
			{
				powerups++;
				power--;
			}

		}
		


		
		
		int precadded = 0;
		int encadded = 0;
		int hpadded = 0;
		String[] posup = {"#att", "#def", "#prec", "#enc", "#mr", "#hp"};
		int[] defvals = {10, 10, 10, 3, 10, u.getCommandValue("#hp", 10)};
		
		for(int i = 0; i < powerups; i++)
		{
			List<Filter> ups = new ArrayList<Filter>();
			for(int j = 0; j < posup.length; j++)
			{
				String s = posup[j];

				int dif = u.getCommandValue(s, 10) - defvals[j];
		
				double chance = 1;
				double scaler = 2;
				
				if(s.equals("#enc"))
				{
					dif = -dif;
					scaler = 4;
				}
				
				if(dif < 0)
					scaler = scaler * 0.5;
				
				chance = (double)defvals[j] / ((double)defvals[j] + Math.pow((double)dif, scaler));
				
			
				if(s.equals("#enc"))
				{
					chance = chance / 4;
				}
				
				
				
				
				if(u.getCommandValue("#enc", 3) < 2 || encadded > 1 && s.equals("#enc"))
					chance = 0;
				if(precadded > 0 && s.equals("#prec"))
					chance = 0;
				
				
				if(chance > 0)
				{
					Filter f = new Filter(nationGen);
					f.name = s;
					f.basechance = chance;
					ups.add(f);
				}

			}
			
			for(int j = 0; j < Math.min(3, ups.size()); j++)
			{
				
				Filter f = Entity.getRandom(random, ups);
				ups.remove(f);
				

				if(f.name.equals("#enc"))
				{
					encadded++;
					u.commands.add(new Command(f.name, "-1"));
				}
				else if(f.name.equals("#hp"))
				{
					double amount = Math.max(defvals[5] * 0.05, 1);
					u.commands.add(new Command(f.name, "+" + (int)Math.round(amount)));
					hpadded++;
				}
				else
				{
					u.commands.add(new Command(f.name, "+1"));

				}
				if(f.name.equals("#prec"))
					precadded++;
				
				extra += 0.33;
			}
			
		}
			

		u.commands.add(new Command("#gcost", "+" + (int)Math.round(extra)));
		
		

		
		int mradd = power;
		if(power > 5)
			mradd = 5;

		
		int origmr = u.getCommandValue("#mr", 10);
		if(origmr - 10 > 0)
		{
			mradd = mradd - ((origmr - 10) / 2);
		}
		
		u.commands.add(new Command("#mr", "+" + mradd));
		
		int morbonus = power + random.nextInt(2); 
		if(random.nextDouble() > 0.25)
			morbonus++;
		if(random.nextDouble() > 0.25)
			morbonus++;
		
	
		
		u.commands.add(new Command("#mor", "+" + morbonus ));

		
		
		List<Filter> filters = new ArrayList<Filter>();
		if(sacred)
			filters = ChanceIncHandler.retrieveFilters("sacredfilters", "default_sacredfilters", nationGen.filters, u.pose, u.race);
		else
			filters = ChanceIncHandler.retrieveFilters("elitefilters", "default_elitefilters", nationGen.filters, u.pose, u.race);

		filters = ChanceIncHandler.getValidUnitFilters(filters, u);

		
		int filterCount = 0;
		boolean weapon = false;
		
		ChanceIncHandler chandler = new ChanceIncHandler(nation);
		
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

				power -= cost;

			}
			else // Filters
			{
				

				List<Filter> choices = ChanceIncHandler.getFiltersWithPower(power, power, filters);
				
				
				if(chandler.countPossibleFilters(choices, u) == 0 || random.nextDouble() > 0.8)
					choices = ChanceIncHandler.getFiltersWithPower(power - 1, power, filters);
				
				
				
				if(chandler.countPossibleFilters(choices, u) == 0)
				{
					int range = 1;
					while(range < 20 && chandler.countPossibleFilters(choices, u) == 0)
					{
						choices = ChanceIncHandler.getFiltersWithPower(power - range, power, filters);
						range++;
					}
				}
				if(chandler.countPossibleFilters(choices, u) == 0 || power <= 2 || (power <= 3 && random.nextDouble() < 0.35))
					choices = ChanceIncHandler.getFiltersWithPower(-100, power, filters);
				

				if(random.nextDouble() < 0.05)
				{
					List<Filter> maybe = ChanceIncHandler.getFiltersWithPower(-100, -1, filters);
					if(chandler.countPossibleFilters(maybe, u) > 0)
						choices = maybe;
					

				}
				
				if(chandler.countPossibleFilters(choices, u) == 0)	
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
	}
	
	
	private void giveMagicWeapons(Unit u, int power)
	{
		

		
		CustomItemGen ciGen = new CustomItemGen(nation);
		List<MagicItem> magicItems = new ArrayList<MagicItem>();

		magicItems = ChanceIncHandler.retrieveFilters("magicitems", "defaultprimary", nationGen.magicitems, u.pose, u.race);

		CustomItem item = ciGen.getMagicItem(u, u.getSlot("weapon"), power, random.nextDouble(), magicItems);
		
		if(item != null)
		{
			u.setSlot("weapon", item);

			int loss = 1;
			if(item.magicItem != null)
				loss = item.magicItem.power;
			
			power -= loss;
		
		}
		
		if(u.getSlot("bonusweapon") != null && (random.nextDouble() > 0.75 || u.getSlot("bonusweapon").tags.contains("guaranteedmagic")  || power >= 2))
		{
			CustomItem item2 = ciGen.getMagicItem(u, u.getSlot("bonusweapon"), 1, random.nextDouble(), magicItems);
			if(item2 != null)
				u.setSlot("bonusweapon", item2);
		}
		
		if(u.getSlot("offhand") != null && !u.getSlot("offhand").armor && (random.nextDouble() > 0.75 || u.getSlot("offhand").tags.contains("guaranteedmagic") || power >= 3))
		{
			CustomItem item2 = ciGen.getMagicItem(u, u.getSlot("offhand"), power, random.nextDouble(), magicItems);
			if(item2 != null)
				u.setSlot("offhand", item2);
		}
		
	}
	
	private void addInitialFilters(Unit u)
	{

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
		// Handle sacred power settings
		double extrapower = this.nationGen.settings.get("sacredpower") - 1;
	
		power = (int) (power + power * extrapower * (1 + random.nextDouble() * 0.5) + extrapower);
		
		// Continue normally
		double epicchance = random.nextDouble() * 0.5 + power * 0.25 + 0.25;

	

		if(race == null)
		{		

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
		}
		List<Pose> possibleposes = new ArrayList<Pose>();
		
		LinkedHashMap<String, Double> roles = new LinkedHashMap<String, Double>();
		roles.put("infantry", 1.0);
		roles.put("mounted", 0.25);
		roles.put("ranged", 0.125);
		roles.put("chariot", 0.05);
		
		String toGet = "sacred";
		if(Generic.containsTag(race.tags, toGet + "infantrychance"))
			roles.put("infantry", Double.parseDouble(Generic.getTagValue(race.tags, toGet + "infantrychance")));
		if(Generic.containsTag(race.tags, toGet + "cavalrychance"))
			roles.put("mounted", Double.parseDouble(Generic.getTagValue(race.tags, toGet + "cavalrychance")));
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
		Unit u = this.getSacredUnit(race, p, power, sacred, epicchance);
		
		// Calculate some loose power rating
		double rating = 0;
		for(Filter f : u.appliedFilters)
			rating += Math.pow(2, f.power) / 2;
		
		// Rating should theoretically range from 0 to 4ish at this point
		
		
		if(!role.equals("ranged"))
		{
			if(u.getTotalProt() < 5)
				rating *= 0.3;
			else if(u.getTotalProt() < 8)
				rating *= 0.6;
			else if(u.getTotalProt() < 12)
				rating *= 0.8;
		}	
		else
		{
			if(u.getTotalProt() < 5)
				rating *= 1.8;
			else if(u.getTotalProt() < 8)
				rating *= 1.5;
			else if(u.getTotalProt() < 12)
				rating *= 1.2;
		}
		
		List<String> values = Generic.getTagValues(Generic.getAllUnitTags(u, nation), "sacredratingmulti");
		double total = 1;
		for(String str : values)
		{
			total *= Double.parseDouble(str.substring(1));
		}
		rating *= total;
		
		
		values = Generic.getTagValues(Generic.getAllUnitTags(u, nation), "sacredcostmulti");
		total = 1;
		for(String str : values)
		{
			total *= Double.parseDouble(str.substring(1));
		}
		u.commands.add(new Command("#gcost", "*" + total));

		
		if(random.nextDouble() < (power + rating) / 12 + 0.3)
			u.caponly = true;
		
		return u;
	}
	
	public Unit getSacredUnit(Race race, Pose p, int power, boolean sacred, double epicchance)
	{

		
		Unit u = this.unitGen.generateUnit(race, p);
		String role = "";
		
		// Filters
		this.addInitialFilters(u);
		
		// Add a sacred generic filter
		
		Filter tf = new Filter(nationGen);
		tf.name = Generic.capitalize(role) + " sacred";
		
		

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

		
		// Add epic stuff
		this.addEpicness(u, sacred, power);
		this.handleExtraGeneration(u, role);
		
		
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
		

		
		unitGen.armorUnit(u, null, null, null, false);
		unitGen.armUnit(u, null, null, null, false);
		
		if(u.pose.getItems("offhand") != null && u.pose.getItems("offhand").possibleItems() > 0 && nationGen.weapondb.GetValue(u.getSlot("weapon").id, "2h").equals("0"))
		{
			ItemSet items = fetchItems(u, "offhand", sacred, epicchance);
			ItemSet weaps = items.filterArmor(false);
			
			Item item = null;
			if((weaps.size() > 0 && random.nextDouble() < 0.15 && nationGen.weapondb.GetInteger(u.getSlot("weapon").id, "lgt") < 4) || u.pose.tags.contains("always_dw"))
			{
				item = chandler.getRandom(weaps, u);
			}

			if(item != null)
				u.setSlot("offhand", item);
		}
		
		if(u.pose.getItems("bonusweapon") != null && u.pose.getItems("bonusweapon").possibleItems() > 0)
		{
			int prot = nationGen.armordb.GetInteger(u.getSlot("armor").id, "prot");
			if(random.nextDouble() > 0.4 + (double)prot * 0.02)
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
		if(Generic.containsTag(u.tags, "NEEDSMAGICWEAPON"))
		{
			int cost = Integer.parseInt(Generic.getTagValue(u.tags, "NEEDSMAGICWEAPON"));
			u.tags.remove("NEEDSMAGICWEAPON " + cost);
			giveMagicWeapons(u, cost);
		}
		
		// Clean up
		this.cleanUnit(u);
		
		return u;
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
