package nationGen.rostergeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.Entity;
import nationGen.entities.Filter;
import nationGen.entities.MagicFilter;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.magic.MagicPattern;
import nationGen.magic.RandomEntry;
import nationGen.misc.Command;
import nationGen.misc.ItemSet;
import nationGen.nation.Nation;
import nationGen.units.Unit;



public class CommanderGenerator extends TroopGenerator {

	private Random r;
	public CommanderGenerator(NationGen g, Nation n) {
		super(g, n);
		r = new Random(n.random.nextInt());

	}


	public int strength = 2;

	


	private List<Unit> getUnitsWithTag(List<Unit> list, String tag)
	{
		List<Unit> newlist = new ArrayList<Unit>();
		for(Unit u : list)
			if(Generic.getAllUnitTags(u).contains(tag))
				newlist.add(u);
		
		return newlist;
	}
	
	
	private void removeMontagUnits(List<Unit> possibleComs)
	{
		
		List<Unit> tempComs = new ArrayList<Unit>();
		for(Unit u : possibleComs)
			if(u.tags.contains("montagunit"))
				tempComs.add(u);

		possibleComs.removeAll(tempComs);
		tempComs.clear();
	}
	
	private List<Unit> getListOfSuitableUnits(String role)
	{
		List<Unit> possibleComs = new ArrayList<Unit>();
		possibleComs.addAll(nation.generateUnitList(role));
		for(Unit u : nation.generateUnitList("montagtroops"))
		{
			if(u.guessRole().equals(role))
				possibleComs.add(u);
		}
		possibleComs.addAll(nation.generateUnitList("montagsacreds"));
		return possibleComs;
	}
	
	public void generateComs()
	{

		
		
		List<Unit> tempComs = new ArrayList<Unit>();
		List<Unit> possibleComs = new ArrayList<Unit>();
		possibleComs.addAll(nation.generateUnitList("infantry"));
		possibleComs.addAll(nation.generateUnitList("mounted"));
		possibleComs.addAll(nation.generateUnitList("chariot"));
		possibleComs.addAll(nation.generateUnitList("sacred"));
		

		
		for(Unit u : nation.generateUnitList("montagtroops"))
		{
			if(u.guessRole().equals("infantry") || u.guessRole().equals("chariot") || u.guessRole().equals("mounted"))
				possibleComs.add(u);
		}
		possibleComs.addAll(nation.generateUnitList("montagsacreds"));
		
		
		List<String> features = new ArrayList<String>();
		List<String> allFeatures = new ArrayList<String>();
		allFeatures.add("#flying");
		allFeatures.add("#stealthy");
		allFeatures.add("#amphibian");
		allFeatures.add("#pooramphibian");
		allFeatures.add("#mounted");
		allFeatures.add("#mountainsurvival");
		allFeatures.add("#wastesurvival");
		allFeatures.add("#forestsurvival");
		allFeatures.add("#swampsurvival");
		allFeatures.add("#demon");
		allFeatures.add("#undead");
		allFeatures.add("#magicbeing");

		int orig = 1;
		boolean hasSlaves = false;
		
		if(r.nextBoolean())
		{
			orig++;
			allFeatures.add("#holy");
		}
		
		// Check to see if #secondaryracecommand is going to add #magicbeing, etc. to secondary race troops when they're finalized
		
		List<Command> all = new ArrayList<Command>();
		Boolean secondaryMagicBeings = false;
		Boolean secondaryDemons = false;
		Boolean secondaryUndead = false;
		Boolean secondarySlaves = false;
		
		for(String tag : nation.races.get(0).tags)
		{
			List<String> args = Generic.parseArgs(tag);
			if(args.get(0).equals("secondaryracecommand") || args.get(0).equals("secondaryracecommand_conditional"))
			{

				Command c = Command.parseCommand(args.get(1));
				secondaryMagicBeings = (c.command.equals("#magicbeing") || secondaryMagicBeings);
				secondaryDemons = (c.command.equals("#demon") || secondaryDemons);
				secondaryUndead = (c.command.equals("#undead") || secondaryUndead);
				secondarySlaves = (c.command.equals("#slave") || secondarySlaves);
			}
		}

		
		// Get all commands that at least one commander has to fulfill.
		
		int magicbeings = 0;
		int demons = 0;
		for(Unit u : possibleComs)
		{
			if(u.race.equals(nation.races.get(1)) && secondaryMagicBeings)
			{
				magicbeings++;
				if(!features.contains("#magicbeing"))
				{
					features.add("#magicbeing");
				}
			}
			if(u.race.equals(nation.races.get(1)) && secondaryDemons)
			{
				demons++;
				if(!features.contains("#demon"))
				{
					features.add("#demon");
				}
			}
			if((u.race.equals(nation.races.get(1)) && secondaryUndead))
			{
				if(!secondaryDemons)  // Make sure we don't double-count
					demons++;
				if(!features.contains("#undead"))
				{
					features.add("#undead");
				}
			}
			if(u.race.equals(nation.races.get(1)) && secondarySlaves)
			{
				hasSlaves = true;
			}
			for(Command c : u.getCommands())
			{
				if(c.command.equals("#magicbeing") && !(u.race.equals(nation.races.get(1)) || secondaryMagicBeings))  // Make sure we don't double-count
					magicbeings++;
				if(c.command.equals("#demon") && !(u.race.equals(nation.races.get(1)) || secondaryDemons))  // Make sure we don't double-count
					demons++;
				if(c.command.equals("#undead")&& !(u.race.equals(nation.races.get(1)) || secondaryUndead))  // Make sure we don't double-count
					demons++;
				
				if(c.command.equals("#slave") || (u.race.equals(nation.races.get(1)) && secondarySlaves))
					hasSlaves = true;
				
				if(allFeatures.contains(c.command) && !features.contains(c.command))
				{
					features.add(c.command);
				}
			}
		}


		double magicshare = (double)magicbeings/(double)possibleComs.size();
		double udshare = (double)demons/(double)possibleComs.size();

		// Clear the list and add only infantry for first pass!
		possibleComs.clear();
		possibleComs.addAll(getListOfSuitableUnits("infantry"));
		removeMontagUnits(possibleComs);
		
		
		List<Unit> cantbes = this.getUnitsWithTag(possibleComs, "cannot_be_commander");
		if(cantbes.size() < possibleComs.size())
			possibleComs.removeAll(cantbes);
		
		List<Unit> shouldbes = this.getUnitsWithTag(possibleComs, "should_be_commander");
		if(shouldbes.size() > 0)
			possibleComs = shouldbes;
		
		
		
		// Add one random infantry com
		if(getListOfSuitableUnits("infantry").size() > 0 && possibleComs.size() > 0)
		{
			Unit com = possibleComs.get(r.nextInt(possibleComs.size()));
			tempComs.add(com);
			possibleComs.remove(com);
		}
		else
			orig++;
		
		
		possibleComs.addAll(getListOfSuitableUnits("chariot"));
		possibleComs.addAll(getListOfSuitableUnits("mounted"));
		possibleComs.addAll(getListOfSuitableUnits("sacred"));
		removeMontagUnits(possibleComs);
		
		int minimumcoms = orig;
	
		
		possibleComs.removeAll(tempComs);
		minimumcoms = Math.min(minimumcoms, possibleComs.size());
		
		
		cantbes = this.getUnitsWithTag(possibleComs, "cannot_be_commander");
		if(possibleComs.size() - cantbes.size() >= minimumcoms)
			possibleComs.removeAll(cantbes);
		

		
		int tries = 0;
		while((minimumcoms > 0 || features.size() > 0) && tries < 200)
		{			
			tries++;
			
			List<Unit> temps = new ArrayList<Unit>();
			shouldbes = this.getUnitsWithTag(possibleComs, "should_be_commander");
			if(shouldbes.size() >= 1)
				temps.addAll(shouldbes);
			else
				temps.addAll(possibleComs);
			
			Unit u = null;
			if(features.size() > 0)
			{
				do
				{
					u = getUnitWith(features, allFeatures, temps);
					if(u == null)
					{
						u = getUnitWith(features, allFeatures, possibleComs);
						if(u == null)
						{
							features.clear();
							break;
						}
					}
				} 
				while(tempComs.contains(u));
			}
			else
			{

				do
				{

					u = possibleComs.get(r.nextInt(possibleComs.size()));
				} 
				while(u == null || tempComs.contains(u) || (shouldReRoll(u, tempComs) && r.nextBoolean()));
			}
			
			if(u != null)
			{
				minimumcoms--;
				tempComs.add(u);
			}
			
		}

		List<Unit> coms = new ArrayList<Unit>();
		for(Unit u : tempComs)
		{
			//NamePart part = new NamePart();
			//part.text = "Commander";
			
	
			Unit unit = this.unitGen.generateUnit(u.race, u.pose);
			for(String str : u.slotmap.keySet())
				unit.setSlot(str, u.getSlot(str));
			
			
			
			process(unit);
			unit.color = u.color;
			unit.commands.addAll(u.commands);
			unit.appliedFilters.addAll(u.appliedFilters);
			unit.caponly = u.caponly;
			unit.tags.addAll(u.tags);
			
			//unit.name.type = part;
			//unit.commands.add(new Command("#gcost", "+20"));
			coms.add(unit);
			
			
			
			// Add stuff to sacreds sometimes
			boolean priest = false;
			boolean stealthy = false;
			for(Command c : unit.getCommands())
			{
				if(c.command.equals("#holy"))
					priest = true;
				if(c.command.equals("#stealthy"))
					stealthy = true;
			}
			
		
			if(r.nextBoolean() && priest)
				unit.commands.add(new Command("#magicskill", "8 1"));
			
			if(priest && stealthy)
			{
				double rand = r.nextDouble();
				if(rand < 0.03)
				{
					unit.commands.add(new Command("#assassin"));
				}
			}
			

		}
		
		tempComs.clear();
		for(int i = coms.size() - 1; i >= 0; i--)
			tempComs.add(coms.get(i));
		
		
		tempComs = orderComs(tempComs);
		
		// Leadership!
		
		int power = 1;
		for(int i = 0; i < tempComs.size(); i++)
		{
			Unit com = tempComs.get(i);
			
			
			if((i == 0 || r.nextDouble() < 0.2) && hasSlaves)
			{
				com.commands.add(new Command("#taskmaster", "+1"));
			}
			
			if(com.caponly && r.nextDouble() > 0.5)
				com.caponly = false;
			
			com.commands.add(new Command("#gcost", "+30"));


			// Goodleader
			// Expertleader
			double random = r.nextDouble();
			if(random > 1 - 0.125 * i)
			{
				power = 3;
				com.commands.add(new Command("#gcost", "+50"));
				com.commands.add(new Command("#expertleader"));
				
				if(r.nextDouble() > 0.5)
				{
					com.commands.add(new Command("#inspirational", "+1"));
					com.commands.add(new Command("#gcost", "+10"));
				}
			
				if(r.nextDouble() > 0.75)
				{
					com.commands.add(new Command("#inspirational", "+1"));
					com.commands.add(new Command("#gcost", "+10"));
				}
				
				if(r.nextDouble() < 0.25 && hasSlaves)
				{
					int amount = r.nextInt(2) + 1;
					com.commands.add(new Command("#taskmaster", "+" + amount));
				}
				
		
				
			}
			else if(random > 0.25 - 0.05 * i)
			{
				power = 2;
				com.commands.add(new Command("#gcost", "+20"));
				com.commands.add(new Command("#goodleader"));
				
				if(r.nextDouble() > 0.65)
				{
					com.commands.add(new Command("#inspirational", "+1"));
					com.commands.add(new Command("#gcost", "+10"));
				}
				if(r.nextDouble() > 0.85)
				{
					com.commands.add(new Command("#inspirational", "+1"));
					com.commands.add(new Command("#gcost", "+10"));
				}
				
				if(r.nextDouble() < 0.5 && hasSlaves)
				{
					int amount = r.nextInt(2) + 1;
					com.commands.add(new Command("#taskmaster", "+" + amount));
				}
			}
			else
			{
				com.commands.add(new Command("#okleader"));

				if(r.nextDouble() > 0.75)
				{
					com.commands.add(new Command("#inspirational", "+1"));
					com.commands.add(new Command("#gcost", "+10"));
				}
				if(r.nextDouble() > 0.5)
				{
					com.commands.add(new Command("#command", "+20"));
					if(r.nextDouble() > 0.875)
					{
						com.commands.add(new Command("#command", "+20"));
						com.commands.add(new Command("#gcost", "+10"));
					}
				}
			}
			
			// Magic leadership
			boolean magicbeing = false;
			boolean demonud = false;
			
			for(Command c : com.getCommands())
			{
				if(c.command.equals("#magicbeing"))
					magicbeing = true;
				else if(c.command.equals("#demon"))
					demonud = true;
				else if(c.command.equals("#undead"))
					demonud = true;
			}
				
			assignMagicUDLeadership(com, power, magicbeing, magicshare, "magic");
			assignMagicUDLeadership(com, power, demonud, udshare, "undead");
			
			generateDescription(com, magicbeing, demonud, hasSlaves);  // Kludge to provide basic madlib commander descriptions

		}
		
		if(nation.comlists.get("commanders") == null)
			nation.comlists.put("commanders", new ArrayList<Unit>());
		
		nation.comlists.get("commanders").addAll(tempComs);
	}
	


	private void assignMagicUDLeadership(Unit com, int power, boolean being, double magicshare, String str)
	{
	
		int origpower = power;

		if(magicshare <= 0.65)
			power -= 1;
		else if(magicshare <= 0.35)
		{	
			if(power > 1)
				power = 1;
			else
				power = 0;
		}
		else if(magicshare <= 0.25)
			power = 0;
		

		
		if(power < 2 && being)
			power = Math.min(Math.max(2, power + 1), 3);		
		if(power < origpower && being && magicshare > 0.4)
			power = origpower;
			
		if(power >= 3)
			com.commands.add(new Command("#good" + str + "leader"));
		else if(power >= 2)
			com.commands.add(new Command("#ok" + str + "leader"));
		else if(power >= 1)
			com.commands.add(new Command("#poor" + str + "leader"));
		
	}

	
	private List<Unit> orderComs(List<Unit> coms)
	{
		List<Unit> newlist = new ArrayList<Unit>();
		List<Unit> templist = new ArrayList<Unit>();
		

		for(Unit u : coms)
		{
			boolean holy = false;
			boolean mounted = false;
			for(Command c : u.getCommands())
			{
				if(c.command.equals("#holy"))
					holy = true;
				if(c.command.equals("#mounted"))
					mounted = true;
			}
			if(!holy && !mounted)
				templist.add(u);
		}
		newlist.addAll(orderOneListByProt(templist));

		templist.clear();
		for(Unit u : coms)
		{
			boolean holy = false;
			boolean mounted = false;
			for(Command c : u.getCommands())
			{
				if(c.command.equals("#holy"))
					holy = true;
				if(c.command.equals("#mounted"))
					mounted = true;
			}
			if(!holy && mounted)
				templist.add(u);
		}
		newlist.addAll(orderOneListByProt(templist));
		
		templist.clear();
		for(Unit u : coms)
		{
			boolean holy = false;
			for(Command c : u.getCommands())
			{
				if(c.command.equals("#holy"))
					holy = true;
			}
			if(holy)
				templist.add(u);
		}
		
		newlist.addAll(orderOneListByProt(templist));

		return newlist;
	}
	
	private List<Unit> orderOneListByProt(List<Unit> units)
	{
		List<Unit> newList = new ArrayList<Unit>();
		
		while(units.size() > 0)
		{
			int lowestProt = 100;
			Unit lowest = null;
			for(Unit u : units)
			{
				int prot = u.getTotalProt();
				if(prot < lowestProt)
				{
					lowest = u;
					lowestProt = prot;
				}
			}
			
			newList.add(lowest);
			units.remove(lowest);
		}
		
		return newList;
		
	}
	
	private boolean shouldReRoll(Unit u, List<Unit> list)
	{
		boolean mounted = false;
		for(Command c : u.getCommands())
			if(c.command.equals("#mounted"))
				mounted = true;
		
		Item armor = u.getSlot("armor");
		
		for(Unit unit : list)
		{
			Item unitArmor = unit.getSlot("armor");
			boolean unitMounted = false;
			for(Command c : unit.getCommands())
				if(c.command.equals("#mounted"))
					unitMounted = true;
			
			if(unitArmor == armor && (unit.getSlot("mount") == u.getSlot("mount") && (unitMounted && mounted)))
				return true;
		}
		
		return false;
		
	}
	
	
	private Unit getUnitWith(List<String> features, List<String> allFeatures, List<Unit> possibleComs)
	{
		Unit unit = null;
		for(Unit u : possibleComs)
		{
			for(Command c : u.getCommands())
			{
					if(features.contains(c.command))
						unit = u; 
			}
		}
		
		if(unit == null)
		{
			return unit;
		}
		

		for(Command c : unit.getCommands())
		{
				if(allFeatures.contains(c.command))
				{
					features.remove(c.command);
				}
		}
		
		return unit;
	}
	

	

	private void process(Unit u) {
		u.commands.add(new Command("#att", "+" + strength));
		u.commands.add(new Command("#def", "+" + strength));
		u.commands.add(new Command("#mor", "+" + strength));
		u.commands.add(new Command("#hp", "+" + strength));
		u.commands.add(new Command("#prec", "+" + strength));

		int racialMapMove = 2;
		for(Command c : u.race.unitcommands)
		{
			if(c.command.equals("#mapmove"))
				racialMapMove = Integer.parseInt(c.args.get(0));
		}
		
		for(Command c : u.getCommands())
		{
			if(c.command.equals("#mapmove"))
			{
				int mapmove = Integer.parseInt(c.args.get(0));
				if(mapmove < racialMapMove)
					u.commands.add(new Command("#mapmove", "+" + (racialMapMove - mapmove)));
			}
		}
		
		Race race = u.race;
		
		Random r = nation.random;
				
		int stuffmask = r.nextInt(3) + 1;
		
		if(strength == 1)
			stuffmask = 1;
		else if(strength == 2)
			stuffmask = 3;
		
		if(stuffmask == 1 || stuffmask == 3)
		{
			

			//System.out.println(items.filterForPose(u.pose).filterSlot("cloakb").getRandom(new ArrayList<Tag>(), false).xoffset + ", " + items.filterForPose(u.pose).filterSlot("cloakb").getRandom(new ArrayList<Tag>(), false).yoffset);
			u.setSlot("cloakb", Entity.getRandom(r, u.pose.getItems("cloakb")));
			//System.out.println(u.getSlot("cloakb").xoffset + ", " + u.getSlot("cloakb").yoffset);
		
		}
		
		if(stuffmask == 2 || stuffmask == 3)
		{
			equipEliteItem(u, "helmet");
			equipEliteItem(u, "weapon");
			equipEliteItem(u, "offhand");
	
		}
		
		// Remove troop only filters
		List<Filter> removef = new ArrayList<Filter>();
		for(Filter f : u.appliedFilters)
			if(f.themes.contains("trooponly"))
				removef.add(f);
		u.appliedFilters.removeAll(removef);

	}

	
	private ItemSet getSuitableCommanderItems(Unit u, String slot)
	{
		ItemSet all = new ItemSet();
		
		Item i = u.getSlot(slot);
		boolean sacred = u.tags.contains("sacred") || (i != null && i.tags.contains("sacred"));
		
		all.addAll(u.pose.getItems(slot).filterTheme("elite", true));
		if(sacred)
			all.addAll(u.pose.getItems(slot).filterTheme("sacred", true));

		if(i != null && i.name != null)
			all.removeAll(u.pose.getItems(slot).filterTag("eliteversion " + i.name, true));

		
		return all;
		
	}

	private void equipEliteItem(Unit u, String slot)
	{

		
		
		int helmetprot = 0;
		
		Item helmet = null;
		if(u.getSlot(slot) == null)
			return;
		
		
		
		if(u.getSlot(slot).armor)
			helmetprot = nationGen.armordb.GetInteger(u.getSlot(slot).id, "prot");
			
		// Try to get elite version
		Item item = u.getSlot(slot);
		if(Generic.containsTag(item.tags, "eliteversion"))
		{
			ItemSet helmets = new ItemSet();
			for(String str : Generic.getTagValues(item.tags, "eliteversion"))
				helmets.add(u.pose.getItems(slot).getItemWithName(str, slot));
			
			helmet = chandler.getRandom(helmets, u);
		}
		
		
		// No elite version and already elite or sacred? Yeah. Let's not bother.
		if(helmet == null && (u.getSlot(slot).themes.contains("elite") || u.getSlot(slot).themes.contains("sacred")))
			return;
	

		// Try to get an elite item with same id in same slot
		if(helmet == null && item != null && u.pose.getItems(slot) != null)
		{
			try
			{
				ItemSet helmets = new ItemSet();
			
				for(Item it : getSuitableCommanderItems(u, slot).filterSlot(slot))
					if(it.id.equals(item.id))
						helmets.add(it);
				
				helmet = chandler.getRandom(helmets, u);
				
			}
			catch(Exception e)
			{
			
			}
		}
		
		// Try to get an elite armor of some suitable sort
		if(u.getSlot(slot).armor && helmet == null && !slot.equals("offhand"))
		{
				helmet = chandler.getRandom(getSuitableCommanderItems(u, slot).filterProt(nationGen.armordb, helmetprot, helmetprot));
			
				if(helmet == null)
					helmet = chandler.getRandom(getSuitableCommanderItems(u, slot).filterProt(nationGen.armordb, helmetprot - 2, helmetprot + 8));
		
		}


		
		if(helmet != null)
			u.setSlot(slot, helmet);
		
	}
	
	public void generateDescription(Unit com, boolean magicbeing,	boolean demonud, boolean hasSlaves)
	{
		String desc = new String("");
		List<String> normalLd = new ArrayList<String>();
		int leader = 1;
		int magicLd = 0;
		int demonUDLd = 0;
		int holyRank = 0;

		boolean awe = false;
		boolean fear = false;
		boolean inspiring = false;
		boolean taskmaster = false;
		boolean beastmaster = false;
		
		normalLd.add(pickRandomSubstring("unexceptional adequate undistinguished unremarkable"));
		normalLd.add(pickRandomSubstring("willingly obediently dutifully freely heedfully carefully"));
		
		for(Command c : com.getCommands())
		{
			if(c.command.equals("#expertleader"))
			{
				normalLd.set(0, pickRandomSubstring("calculating masterful adroit deft wiley cunning expert consummate veteran"));
				normalLd.set(1, pickRandomSubstring("unquestioningly fearlessly unhesitatingly zealously fanatically dauntlessly"));
				leader = 3;
			}
			else if(c.command.equals("#goodleader"))
			{
				normalLd.set(0, pickRandomSubstring("competent decisive skilled able experienced clever adept skillful practiced capable effective"));
				normalLd.set(1, pickRandomSubstring("confidently boldly loyally cheerfully courageously stalwartly swiftly"));
				leader = 2;
			}
			else if(c.command.equals("#poorleader"))
			{
				normalLd.set(0, pickRandomSubstring("incompetent rash short-sighted inexperienced indecisive craven foolish"));
				normalLd.set(1, pickRandomSubstring("grudgingly skittishly anxiously apprehensively spiritlessly confusedly recklessly wildly"));
				leader = 0;
			}
			else if(c.command.equals("#goodmagicleader"))
			{
				magicLd = 3;
			}
			else if(c.command.equals("#okmagicleader"))
			{
				magicLd = 2;
			}
			else if(c.command.equals("#poormagicleader"))
			{
				magicLd = 1;
			}
			else if(c.command.equals("#goodundeadleader"))
			{
				demonUDLd = 3;
			}
			else if(c.command.equals("#okundeadleader"))
			{
				demonUDLd = 2;
			}
			else if(c.command.equals("#poorundeadleader"))
			{
				demonUDLd = 1;
			}
			else if(c.command.equals("#awe"))
				awe = true;
			else if(c.command.equals("#fear"))
				fear = true;
			else if(c.command.equals("#inspirational"))
				inspiring = true;
			else if(c.command.equals("#taskmaster"))
				taskmaster = true;
			else if(c.command.equals("#magicskill"))
			{
				String tmp = null;
				
				if(c.args.get(0) != null)
					tmp = c.args.get(0);
		
				if(tmp != null)			// We only care about pure priests here
					if(tmp.startsWith("8") && tmp.length() > 2)
					{
						holyRank = Integer.parseInt(tmp.substring(2,3));
					}
				}
			else if(c.command.equals("#holy"))
			{
				if(holyRank < 1) 
					holyRank = 1;
			}
		}
		
		desc = "The %unitname_plural% of %nation% are ";
		
		if(awe && fear)
		{
			desc += pickRandomSubstring("cruel brutal terrifying savage vicious ruthless bloodthirsty") + " yet " + pickRandomSubstring("beloved adored revered venerated exalted") 
					+ " figures whose " + normalLd.get(0);
		}
		else if(awe)
		{
			desc += pickRandomSubstring("daunting aloof unapproachable glorious venerated formidible lofty")
					+ " figures whose " + normalLd.get(0);
		}
		else if(fear)
		{
			desc += pickRandomSubstring("cruel brutal terrifying savage vicious ruthless dreaded")
					+ " leaders whose " + normalLd.get(0);
			if(inspiring && leader <= 1)
				desc += " yet";
			else if(taskmaster && leader > 1)
				desc += " but";
			else if(inspiring || taskmaster)
				desc += " and";
		}
		else 
			desc += normalLd.get(0) + " leaders whose";
		
		if(inspiring && taskmaster)
			desc += " " + pickRandomSubstring("compelling forceful ardent vehement fierce gruff arresting");
		else if(inspiring)
			desc += " " + pickRandomSubstring("inspiring rousing stirring earnest fiery impassioned");
		else if(taskmaster)
			desc += " " + pickRandomSubstring("harsh demanding exacting stern dour stringent");
		
		desc += " " + pickRandomSubstring("orders commands decrees mandates instructions plans") 
				+ " are " + normalLd.get(1) + " " + pickRandomSubstring("carried out,executed,obeyed,enacted", ",") 
				+ " by ";
		
		if(beastmaster)
			desc += "man and beast alike.";
		else if(hasSlaves && taskmaster)
			desc += "their subordinates and slaves.";
		else if(holyRank > 0)
			desc += "their " + pickRandomSubstring("followers flock juniors disciples minions guardians subordinates attendants companions escorts lackeys assistants") + ".";
		else
			desc += "their " + pickRandomSubstring("troops soldiers warriors minions followers forces") + ".";
		
		if(magicLd > 0 || demonUDLd > 0)
		{
			desc += " The %unitname_plural% are also charged with " + pickRandomSubstring("commanding leading overseeing deploying overseeing");  
			
			if(magicLd == 0)
				desc += " the unholy servants of %nation%, but its magical forces are beyond their " 
						+ pickRandomSubstring("power ability ken understanding expertise") + ".";
			else if(demonUDLd == 0)
				desc += " the magical servants of %nation%, but its darker forces are beyond their " 
						+ pickRandomSubstring("power ability ken understanding expertise") + ".";
			else
			{
				if(magicbeing || demonud)
					desc += " the enchanted and unholy warriors of %nation%";
				else
					desc += " any supernatural entity that the %mages_plural% of %nation% might " + pickRandomSubstring("conjur,summon,raise,bind into service", ",");
				
				if(magicLd < 2 && demonUDLd < 2)
					desc += ", although they can only control a handful of these beings.";
				else
					desc += ".";
			}
		}
		else if(magicbeing || demonud)
		{
			if(magicbeing && demonud)
				desc += " However, leading the magical and unholy servants ";
			else if(magicbeing)
				desc += " However, leading the enchanted servitors ";
			else if(demonud)
				desc += " However, leading the darkest forces ";
			
			desc += "of %nation% is beyond their " + pickRandomSubstring("power ability ken understanding expertise") + ".";
		}
		
		if(holyRank > 0)
		{
			if(holyRank == 1)
				desc += " The %unitname_plural% hold a very minor place in %nation%'s heirarchy, with " 
						+ pickRandomSubstring("little,hardly,almost no,barely", ",") + " more " 
						+ pickRandomSubstring("power insight numina understanding standing status prestige respect dignity training authority") + " than a %sacredname%.";
			else if(holyRank == 2)
				desc += " The %unitname_plural% make up the " 
						+ pickRandomSubstring("core,backbone,heart,rank and file,foundation,body,majority", ",") + " of %nation%'s "
						+ pickRandomSubstring("clergy,priests,faith,religion,temple,cult,priesthood", ",") +", playing a crucial role in all almost matters of faith.";
			else
				desc += " At the  " + pickRandomSubstring("pinnacle top head peak summit") + " of %nation%'s religious heirarchy, the %unitname_plural% are the "
					+ pickRandomSubstring("foremost,most exalted,greatest,most powerful,most prestigious,wisest", ",") +" of their faith.";
		}
		
		List<String> body = new ArrayList<String>();
		body.add(desc);
		com.commands.add(new Command("#descr", body));
	}



	private String pickRandomSubstring(String string)
	{
		String[] words = null;
		
		words = string.split(" ");
		
		return words[r.nextInt(words.length)];

	}

	private String pickRandomSubstring(String string, String token) {

		String[] words = null;
		
		words = string.split(token);
		
		return words[r.nextInt(words.length)];
		
	}
	
}
