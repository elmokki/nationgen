package nationGen.rostergeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.Entity;
import nationGen.entities.MagicFilter;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.magic.MagicPattern;
import nationGen.magic.RandomEntry;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;



public class CommanderGenerator extends TroopGenerator {

	public CommanderGenerator(NationGen g, Nation n) {
		super(g, n);

	}


	public int strength = 2;

	


	public void generateComs()
	{

		Random r = nation.random;
		
		List<Unit> tempComs = new ArrayList<Unit>();
		List<Unit> possibleComs = new ArrayList<Unit>();
		possibleComs.addAll(nation.generateUnitList("infantry"));
		possibleComs.addAll(nation.generateUnitList("mounted"));
		possibleComs.addAll(nation.generateUnitList("chariot"));
		possibleComs.addAll(nation.generateUnitList("sacred"));
		
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
		

		// Get all commands that at least one commander has to fulfill.
		
		int magicbeings = 0;
		int demons = 0;
		for(Unit u : possibleComs)
		{
			for(Command c : u.getCommands())
			{
				if(c.command.equals("#magicbeing"))
					magicbeings++;
				if(c.command.equals("#demon"))
					demons++;
				if(c.command.equals("#undead"))
					demons++;
				
				if(c.command.equals("#slave"))
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
		possibleComs.addAll(nation.generateUnitList("infantry"));
		
		// Add one random infantry com
		if(nation.generateUnitList("infantry").size() > 0)
		{
			Unit com = possibleComs.get(r.nextInt(possibleComs.size()));
			tempComs.add(com);
			possibleComs.remove(com);
		}
		else
			orig++;
		
		
		possibleComs.addAll(nation.generateUnitList("mounted"));
		possibleComs.addAll(nation.generateUnitList("chariot"));
		possibleComs.addAll(nation.generateUnitList("sacred"));
		
		int minimumcoms = orig;
	
		
		while(minimumcoms > 0 || features.size() > 0)
		{			
			
			Unit u = null;
			if(features.size() > 0)
			{
				do
				{
					u = getUnitWith(features, allFeatures, possibleComs);
					if(u == null)
					{
						features.clear();
						break;
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
			if(random > 1 - 0.075 * i)
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
			else if(random > 0.2 - 0.05 * i)
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
				if(r.nextDouble() > 0.75)
				{
					com.commands.add(new Command("#inspirational", "+1"));
					com.commands.add(new Command("#gcost", "+10"));
				}
			}
			
			// Magic leadership
			boolean magicbeing = false;
			boolean demonud = false;
			
			for(Command c : com.getCommands())
				if(c.command.equals("#magicbeing"))
					magicbeing = true;
				else if(c.command.equals("#demon"))
					demonud = true;
				else if(c.command.equals("#undead"))
					demonud = true;
				
			assignMagicUDLeadership(com, power, magicbeing, magicshare, "magic");
			assignMagicUDLeadership(com, power, demonud, udshare, "undead");

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
			com.commands.add(new Command("#expert" + str + "leader"));
		else if(power >= 2)
			com.commands.add(new Command("#good" + str + "leader"));
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
			u.setSlot("cloakb", Entity.getRandom(nation.random, u.pose.getItems("cloakb")));
			//System.out.println(u.getSlot("cloakb").xoffset + ", " + u.getSlot("cloakb").yoffset);
		
		}
		
		if(stuffmask == 2 || stuffmask == 3)
		{
			equipEliteItem(u, "helmet");
			equipEliteItem(u, "weapon");
			equipEliteItem(u, "offhand");
	
		}

	}


	private void equipEliteItem(Unit u, String slot)
	{


		int helmetprot = 0;
		
		Item helmet = null;
		if(u.getSlot(slot) == null)
			return;
		
		if(u.getSlot(slot).armor)
			helmetprot = nationGen.armordb.GetInteger(u.getSlot(slot).id, "prot");
			
		Item item = u.getSlot(slot);
		if(Generic.containsTag(item.tags, "eliteversion"))
		{
			helmet = u.pose.getItems(slot).filterTag("elite", true).getItemWithName(Generic.getTagValue(item.tags, "eliteversion"), slot);
		}
		
		// No elite version and already elite or sacred? Yeah. Let's not bother.
		if(u.getSlot(slot) != null)
		{
			if(u.getSlot(slot).tags.contains("elite") || u.getSlot(slot).tags.contains("sacred"))
				return;
		}

	
		
		if(u.getSlot(slot).armor && helmet == null && !slot.equals("offhand"))
		{
				helmet = Entity.getRandom(nation.random, u.pose.getItems(slot).filterTag("elite", true).filterProt(nationGen.armordb, helmetprot, helmetprot));
			
				if(helmet == null)
					helmet = Entity.getRandom(nation.random, u.pose.getItems(slot).filterTag("elite", true).filterProt(nationGen.armordb, helmetprot, helmetprot + 8));
		}

		if(helmet == null && item != null && u.pose.getItems(slot) != null)
		{
			try
			{
				helmet = u.pose.getItems(slot).getItemWithID(item.id, slot);
			}
			catch(Exception e)
			{
			
			}
		}
		
		if(helmet != null)
			u.setSlot(slot, helmet);
		
	}
	
}
