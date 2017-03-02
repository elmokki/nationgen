package nationGen.units;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;














import com.elmokki.Dom3DB;
import com.elmokki.Drawing;
import com.elmokki.Generic;
















import nationGen.NationGen;
import nationGen.entities.Entity;
import nationGen.entities.Filter;
import nationGen.entities.MagicFilter;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.items.ItemDependency;
import nationGen.magic.MageGenerator;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.naming.Name;
import nationGen.nation.Nation;



public class Unit {

	public Name name = new Name();
	public Race race;
	public Pose pose;
	public LinkedHashMap<String, Item> slotmap = new LinkedHashMap<String, Item>();
	public LinkedHashMap<String, Item> slotmemory = new LinkedHashMap<String, Item>();

	public Color color = Color.white;
	public int id = -1;
	public List<Command> commands = new ArrayList<Command>();
	public NationGen nationGen;
	public boolean caponly = false;
	public List<String> tags = new ArrayList<String>();
	public List<Filter> appliedFilters = new ArrayList<Filter>();
	public boolean polished = false;
	public boolean invariantMonster = false;  // Is the unit a monster that must be used as-is instead of copying? (E.g., hydras)
	private Nation nation = null;
	private List<Command> percentCostCommands = new ArrayList<Command>();

	public Unit(NationGen nationGen, Race race, Pose pose)
	{
		this.nationGen = nationGen;
		this.pose = pose;
		this.race = race;
	}
	
	public Unit(NationGen nationGen, Race race, Pose pose, Nation n)
	{
		this(nationGen, race, pose);
		this.nation = n;
	}

	
	public Item getSlot(String s)
	{
		return slotmap.get(s);

	}
	
	public Unit getCopy()
	{
		Unit unit = new Unit(nationGen, race, pose);
		
		
		// Copy unit
		for(String slot : this.slotmap.keySet())
		{
			unit.setSlot(slot, this.getSlot(slot));
		}
		for(String slot : this.slotmemory.keySet())
		{
			unit.slotmemory.put(slot, this.slotmemory.get(slot));
		}
		
		unit.color = color;
		unit.nation = nation;
		unit.polished = this.polished;
		unit.invariantMonster = this.invariantMonster;
		unit.caponly = caponly;
		unit.tags.addAll(tags);
		unit.commands.addAll(commands);
		unit.appliedFilters.addAll(appliedFilters);
		return unit;
	}
	
	/**
	 * Gets unit's magic picks as int[8] without randoms
	 * @return
	 */
	public int[] getMagicPicks()
	{
		return getMagicPicks(false);
	}
	
	/**
	 * Gets unit's magic picks as int[8]
	 * @param randoms if true, randoms at 25% chance are included
	 * @return
	 */
	public int[] getMagicPicks(boolean randoms)
	{

		int[] picks = {0, 0, 0, 0, 0, 0, 0, 0, 0};
		
		double prob = 1;
		if(randoms)
			prob = 0.25;
		
		for(Filter f : appliedFilters)
		{
			if(f.name.equals("MAGICPICKS") || f.name.equals("PRIESTPICKS"))
			{
				MagicFilter m = (MagicFilter)f;
				for(int i = 0; i < picks.length; i++)
				{
					picks[i] += m.pattern.getPathsWithRandoms(m.prio, prob)[i];
				}
			}
		}
		

		
		return picks;
	}
	
	public int getHolyPicks()
	{
		return getMagicPicks()[8];
	}
	
	
	public double getMagicAmount(double prob)
	{
		double n = 0;	
		for(Filter f : appliedFilters)
			if(f.name.equals("MAGICPICKS") && !f.tags.contains("holy"))
			{
				MagicFilter m = (MagicFilter)f;
				n += m.pattern.getPicks(prob);
			}
		
		return n;
	}
	
	public double getRandoms(double prob)
	{
		double n = 0;	
		for(Filter f : appliedFilters)
			if(f.name.equals("MAGICPICKS") && !f.tags.contains("holy"))
			{
				MagicFilter m = (MagicFilter)f;
				n += m.pattern.getRandoms(prob);
			}
		
		return n;
	}
	
	public List<MagicFilter> getMagicFilters()
	{
		List<MagicFilter> fs = new ArrayList<MagicFilter>();
		for(Filter f : appliedFilters)
			if(f.name.equals("MAGICPICKS"))
			{
				MagicFilter m = (MagicFilter)f;
				fs.add(m);
			}
		
		return fs;
	}
	
	
	public double getMagicAtHighest(double prob)
	{
		double n = 0;	
		for(Filter f : appliedFilters)
			if(f.name.equals("MAGICPICKS") && !f.tags.contains("holy"))
			{
				MagicFilter m = (MagicFilter)f;
				n += m.pattern.getHighestReachable(prob);
			}
		
		return n;
	}
	
	public double getMagicSpread(double prob)
	{
		double n = 0;	
		for(Filter f : appliedFilters)
			if(f.name.equals("MAGICPICKS") && !f.tags.contains("holy"))
			{
				MagicFilter m = (MagicFilter)f;
				n += m.pattern.getPathsAtleastAt(1, prob);
			}
		
		return n;
	}
	
	
	private int handleModifier(String mod, int value)
	{
		if(mod.startsWith("+") || mod.startsWith("-"))
		{
			if(mod.startsWith("+"))
				mod = mod.substring(1);
			
			value += Integer.parseInt(mod);
		}
		else
			value = Integer.parseInt(mod);
		
		return value;
	}
	
	public int getItemSlots()
	{
		
		int slots = -1;
		for(Command c : this.getCommands())
			if(c.command.equals("#itemslots"))
				slots = Integer.parseInt(c.args.get(0));
		
		if(slots == -1)
		{
			slots = 0;

			ArrayList<String> unitTags =  (ArrayList<String>) Generic.getAllUnitTags(this);
			
			ArrayList<String> itemTags =  new ArrayList<String>();
			if(this.slotmap.get("basesprite") != null)
				itemTags.addAll(this.slotmap.get("basesprite").tags);

			for(Item i : this.slotmap.values())
				if(i != null && i != this.slotmap.get("basesprite"))
					itemTags.addAll(i.tags);
			
			// itemTags.addAll(this.tags);
			
			
			int head = 1;
			int body = 1;
			int feet = 1;
			int hand = 2;
			int misc = 2;
			
			List<String> unitArgs = Generic.getTagValues(unitTags, "baseitemslot");
			for(String arg : unitArgs)
			{
				if(arg.split(" ")[0].equals("head"))
					head = handleModifier(arg.split(" ")[1], head);
				else if(arg.split(" ")[0].equals("misc"))
					misc = handleModifier(arg.split(" ")[1], misc);
				else if(arg.split(" ")[0].equals("body"))
					body = handleModifier(arg.split(" ")[1], body);
				else if(arg.split(" ")[0].equals("hand"))
					hand = handleModifier(arg.split(" ")[1], hand);
				else if(arg.split(" ")[0].equals("feet"))
					feet = handleModifier(arg.split(" ")[1], feet);			
			}
			
			List<String> args = Generic.getTagValues(tags, "itemslot");
			for(String arg : args)
			{
				if(arg.split(" ")[0].equals("head"))
					head += handleModifier(arg.split(" ")[1], head);
				else if(arg.split(" ")[0].equals("misc"))
					misc += handleModifier(arg.split(" ")[1], misc);
				else if(arg.split(" ")[0].equals("body"))
					body += handleModifier(arg.split(" ")[1], body);
				else if(arg.split(" ")[0].equals("hand"))
					hand += handleModifier(arg.split(" ")[1], hand);
				else if(arg.split(" ")[0].equals("feet"))
					feet += handleModifier(arg.split(" ")[1], feet);			
			}
			
			head = Math.min(head, 2);
			misc = Math.min(misc, 5);
			body = Math.min(body, 1);
			hand = Math.min(hand, 4);
			feet = Math.min(feet, 1);
			
			head = Math.max(head, 0);
			misc = Math.max(misc, 0);
			body = Math.max(body, 0);
			hand = Math.max(hand, 0);
			feet = Math.max(feet, 0);
			
			if(hand > 0)
				for(int i = 0; i < hand; i++)
					slots += Math.pow(2, (i+1));
			if(head > 0)
				for(int i = 0; i < head; i++)
					slots += Math.pow(2, (i+7));
			if(body > 0)
				slots += 1024;
			if(feet > 0)
				slots += 2048;
			if(misc > 0)
				for(int i = 0; i < misc; i++)
					slots += Math.pow(2, (i+12));
			
			if(slots == 0)
					slots = 1;
			
			return slots;
		}
		
		return slots;
	}
	
	public boolean isRanged()
	{
		if(getSlot("weapon") == null)
			return false;
		
		Dom3DB weapondb = nationGen.weapondb;
		if(weapondb.GetValue(getSlot("weapon").id, "rng", "0").equals("0"))
			return true;
		
		return false;
	}
	
	
	private void handleRemoveDependency(Item i)
	{
	
		if(i == null)
			return;
		
		if(i.dependencies.size() > 0)
		{
			String slot = null;
			for(ItemDependency d : i.dependencies)
			{
				if(d.lagged)
					continue;
				
				slot = d.slot;
				
				Item n = null;
				n = this.slotmemory.get(slot);

				setSlot(slot, n); 
				
			}
		}
		

	}
	
	private void handleDependency(String slotname, boolean lagged)
	{
		if(getSlot(slotname) == null)
			return;
		
		ChanceIncHandler chandler = null;
		Random r = null;
		if(nation != null)
		{
			chandler = new ChanceIncHandler(nation);
			r = new Random(nation.random.nextInt());
		}
		
		// This handles #needs
		if(getSlot(slotname).dependencies.size() > 0)
		{
			String target = null;
			String slot = null;
			String command = null;
			Item item = null;
			List<Item> possibles = null;
			
			
			for(ItemDependency d : getSlot(slotname).dependencies)
			{
				if(d.lagged != lagged)
					continue;
				
				target = d.target;
				slot = d.slot;
				
	
				if(!d.type) // Handle needs and setslot
				{
					command = "#needs";
					if(lagged)
						command = "#forceslot";
					
					if(pose.getItems(slot) == null)
					{
				
						System.out.println(command + " for " + slotname + ", item " + target + " and item " + getSlot(slotname).name + " on slot " + slot + " failed. Roles " + this.pose.roles + ", race " + race.name );
						break;
					}
					item = pose.getItems(slot).getItemWithName(target, slot);
					
					
					if(item != null)
					{
						setSlot(slot, item); 
					}
					else
					{
	
	
						System.out.println("!!! " + getSlot(slotname).name + " on slot " + slotname + " tried to link to " + target + " on list " + slot + ", but such item was not found. Check your definitions! Pose " + this.pose.roles + ", race " + this.race.name);
					}
				}
				else if(this.nation != null) // Handle needstype and setslottype
				{
					command = "#needstype";
					if(lagged)
						command = "#forceslottype";
					
			
				
			
					if(pose.getItems(slot) == null)
					{
						System.out.println(command + " for " + slotname + ", type " + target + " and item " + getSlot(slotname).name + " on slot " + slot + " failed due to the slot having no items: " + pose + ", "+ this.pose.roles + ", race " + race.name );
						break;
					}
					
					
					possibles = ChanceIncHandler.getFiltersWithType(target, pose.getItems(slot));
					item = Entity.getRandom(r, chandler.handleChanceIncs(this, possibles));
					
					if(item != null)
					{
						setSlot(slot, item); 
					}
				}
			}
		}
		

	}


	private void handleRemoveThemeinc(Item item)
	{
		if(item == null)
			return;
		
		if(item.filter != null && appliedFilters.contains(item.filter))	
		{
			appliedFilters.remove(item.filter);
		}
		
	}
	private void handleAddThemeinc(Item item)
	{
		if(item == null)
			return;
		
		if(item.filter != null && !appliedFilters.contains(item.filter))	
		{
			appliedFilters.add(item.filter);
		}
		
	}
	
	public void setSlot(String slotname, Item newitem)
	{
		
	
		Item olditem = getSlot(slotname);
		
		slotmap.put(slotname, newitem);
		
		if(olditem != null)
		{
			handleRemoveThemeinc(olditem);
			handleRemoveDependency(olditem);
			slotmemory.put(slotname, olditem);
		}
		
		
		if(newitem == null)
			return;
		
		


		handleDependency(slotname, false);
		handleAddThemeinc(newitem);

		
	}
	

	
	public int getGoldCost()
	{
		List<Command> commands = this.getCommands();
		double holy = 1;
		double slowrec = 1;
		
		int cost = 0;
		for(Command c : commands)
		{
			if(c.command.equals("#gcost"))
			{
				cost += Integer.parseInt(c.args.get(0));
			}
			if(c.command.equals("#holy"))
				holy = 1.3;
			if(c.command.equals("#slowrec"))
				slowrec = 0.9;

		}
			
		int stats = this.getCopyStats();
		if(stats > -1)
		{
			cost = this.nationGen.units.GetInteger("" + stats, "basecost");
			if(cost >= 10000)
				cost -= 10000;

			return cost;
		}
		
		if(!polished)
		{
			return (int)Math.round((double)cost * holy * slowrec);
		}		
		else
		{			
			return cost;
		}
	
	}
	
	

	
	public String getName()
	{
		
		
		int stats = this.getCopyStats();
		if(stats > -1)
		{
			String name = this.nationGen.units.GetValue("" + stats, "unitname");
			return name;
		}
		
		return this.name.toString(this);
		
	}
	

	public String getLeaderLevel()
	{
		return getLeaderLevel("");
	}
	
	public String getLeaderLevel(String prefix)
	{
		List<String> levels = Generic.getLeadershipLevels();
		
		String level = "ok";
		for(Command c : this.getCommands())
		{
			String lead = null;
			if(c.command.endsWith("leader"))
				lead = c.command.substring(1, c.command.indexOf(prefix + "leader"));
			
			if(levels.contains(lead))
				level = lead;
		}
		
		return level;
	}
	
	
	public boolean hasLeaderLevel(String prefix)
	{
		List<String> levels = Generic.getLeadershipLevels();
		
		String level = null;
		for(Command c : this.getCommands())
		{
			String lead = null;
			if(c.command.endsWith(prefix + "leader"))
				lead = c.command.substring(1, c.command.indexOf(prefix + "leader"));
			
			if(levels.contains(lead))
				level = lead;
		}
		
		return level != null;
		
	}
	
	public int getResCost(boolean useSize)
	{
		return getResCost(useSize, this.getCommands());
	}

	private int getResCost(boolean useSize, List<Command> commands)
	{
		int res = 0;
		int size = 2;
		int ressize = -1;
		int extrares = 0;
		
		Dom3DB weapondb = nationGen.weapondb;
		Dom3DB armordb = nationGen.armordb;
		

		for(Command c : commands)
			if(c.command.equals("#rcost"))
				extrares = extrares + Integer.parseInt(c.args.get(0));
			else if(c.command.equals("#size"))
				size = Integer.parseInt(c.args.get(0));
			else if(c.command.equals("#ressize"))
				ressize = Integer.parseInt(c.args.get(0));
		
		
		for(Item i : this.slotmap.values())
		{
			if(i == null || i.id.equals("-1"))
				continue;
			
			Dom3DB db = null;
			if(i.armor)
				db = armordb;
			else
				db = weapondb;
			
			res = res + db.GetInteger(i.id, "res");

		}



		
		if(!useSize){/*do nothing*/}
		else if(ressize > 0)
			res = ressize * res / 2;
		else
			res = size * res / 2;
		
		// Dom3 minimum res amount is 1.
		return Math.max(res + extrares, 1);
	}
	
	public List<Command> getCommands()
	{

		List<Command> tempCommands = new ArrayList<Command>();
		List<Command> allCommands = new ArrayList<Command>();
		List<Command> multiCommands = new ArrayList<Command>();
		
		if(polished)
			return this.commands;
		else
		{
			// Shapechangeunits aren't really of the race/pose their other form is
			if(this.getClass() != ShapeChangeUnit.class)
			{
				allCommands.addAll(race.unitcommands);
				allCommands.addAll(pose.getCommands());

			}
	

	

		
			// Item commands
			Iterator<Item> itr = slotmap.values().iterator();
			while(itr.hasNext())
			{
				Item item = itr.next();
				
				if(item != null)
				{
					allCommands.addAll(item.commands);
				}
			}
			


			// Filters
			for(Filter f : this.appliedFilters)
			{
				
				for(Command c : f.getCommands())
				{
			
					
					Command tc = c;
					int tier = 2;
					
					if(Generic.containsTag(tags, "schoolmage"))
						tier = Integer.parseInt(Generic.getTagValue(this.tags, "schoolmage"));
					
					if(c.args.size() > 0 && c.args.get(0).contains("%value%"))
					{
						int multi = 10;
						int base = -5;
						try
						{
							multi = Integer.parseInt(Generic.getTagValue(f.tags, "valuemulti"));
							base = Integer.parseInt(Generic.getTagValue(f.tags, "basevalue"));
						}
						catch(Exception e)
						{
							
						}
						
						int result = base + multi * tier;
						
						String resstring = "" + result;
						if(result > 0)
						{
							resstring = "+" + resstring;
						}
						
						if(result != 0)
							tc = new Command(c.command, resstring);
						
					}
					
					// Shape change units handle #spr1/#spr2 separately
					if(this.getClass() == ShapeChangeUnit.class)
					{
				
						if(!tc.command.equals("#spr1") && !tc.command.equals("#spr2"))
							allCommands.add(tc);
					}
					else
						allCommands.add(tc);
				
					
			
				}
			}
			
			
			// Adjustment stuff
			allCommands.addAll(this.commands);
			

			// Adjustment commands
			List<Command> adjustmentcommands = new ArrayList<Command>();
			for(String str : Generic.getTagValues(Generic.getAllUnitTags(this), "adjustmentcommand"))
			{
				adjustmentcommands.add(Command.parseCommand(str));
			}
			
			
			// Special case: #fixedrescost
			if(Generic.getTagValues(Generic.getAllUnitTags(this), "fixedrescost").size() > 0)
			{
				// If we have many, we use the first one. The order is Race, pose, filter, theme.
				// Assumedly these exist mostly in one of these anyway
				int cost = Integer.parseInt(Generic.getTagValues(Generic.getAllUnitTags(this), "fixedrescost").get(0));
				int currentcost = getResCost(true, allCommands);
				
				cost -= currentcost;
				
				if(cost > 0)
					adjustmentcommands.add(new Command("#rcost", "+" + cost));
				else if(cost < 0)
					adjustmentcommands.add(new Command("#rcost", "" + cost));

			}
			
			
			
			// Add adjustments
			allCommands.addAll(adjustmentcommands);
			

		}
		




		// Now handle them!
		
		for(Command c : allCommands)
			if(c.args.size() > 0 && c.args.get(0).startsWith("*") && Generic.isNumeric(c.args.get(0).substring(1)))
				multiCommands.add(c);
			else if(c.args.size() > 0 && c.args.get(0).startsWith("%cost") && (Generic.isNumeric(c.args.get(0).substring(5)) || (c.args.size() > 1 && Generic.isNumeric(c.args.get(1)))))
				percentCostCommands.add(c);
			else
				handleCommand(tempCommands, c);

	
	

		//Percentual cost increases
		for(Command c : multiCommands)
		{
			handleCommand(tempCommands, c);
		}


		
		return tempCommands;
	}

	
	private boolean handleLowEncCommandPolish(List<String> tags)
	{

		if(!Generic.containsTag(tags, "lowenctreshold"))
			return false;
		
		int treshold = Integer.parseInt(Generic.getTagValue(tags, "lowenctreshold"));

		int enc = 0;
		if(getSlot("armor") != null)
			enc += nationGen.armordb.GetInteger(getSlot("armor").id, "enc");
		if(getSlot("offhand") != null && getSlot("offhand").armor)
			enc += nationGen.armordb.GetInteger(getSlot("offhand").id, "enc");
		if(getSlot("helmet") != null)
			enc += nationGen.armordb.GetInteger(getSlot("helmet").id, "enc");
		
		if(enc <= treshold && Generic.containsTag(tags, "lowenccommand"))
		{
			String command = Command.parseCommand(Generic.getTagValue(tags, "lowenccommand")).command;
			for(Command c : this.getCommands())
			{
				if(command.equals(c.command))
				{
					return false;
		
				}
			}
			this.commands.add(Command.parseCommand(Generic.getTagValue(tags, "lowenccommand")));
			return true;
		}
		
		return false;
		
	}
	
	private boolean hasCopyStats()
	{
		
		boolean copystats = false;
		for(Command c : this.getCommands())
		{
	
			if(c.command.equals("#copystats"))
				copystats = true;
		}
		
		return copystats;
	}
	
	private int getCopyStats()
	{
		
		
		int stats = -1;
		for(Command c : this.getCommands())
		{
			
			if(c.command.equals("#copystats"))
				stats = Integer.parseInt(c.args.get(0));
		}
		
	
		return stats;
	}
	
	
	public String guessRole()
	{
		String r = "infantry";
		for(String role : pose.roles)
			if(role.contains("ranged"))
				r = "ranged";
		for(String role : pose.roles)
			if(role.contains("mounted"))
				r = "mounted";
		return r;
	}
	
	public void polish()
	{
		if(this.polished)
			return;
		
		Unit u = this;
	
		handleLowEncCommandPolish(u.pose.tags);
		handleLowEncCommandPolish(u.race.tags);
		for(Filter f : appliedFilters)
			handleLowEncCommandPolish(f.tags);
		
		boolean copystats = hasCopyStats();

		
		if(u.name.toString(this).equals("\"\""))
		{
			System.out.println("UNIT NAMING ERROR! PLEASE REPORT THE SEED!");
		}
		
		// Slot hard setting
		for(String slot : this.slotmap.keySet())
		{
			if(getSlot(slot) != null)
			{
				handleDependency(slot, true);
			}
		}
		
		
		// Slot removal
		List<String> removeslots = new ArrayList<String>();
		for(String slot : this.slotmap.keySet())
		{
			if(getSlot(slot) != null)
			{
				for(String tag : getSlot(slot).tags)
					if(tag.startsWith("noslot "))
					{
						removeslots.add(tag.split(" ")[1]);
					}
				}
		}
		for(String slot : removeslots)
			this.setSlot(slot, null);

		
		// +2hp to mounted
		if(this.getSlot("mount") != null)
		{
			this.commands.add(new Command("#hp", "+2"));
			this.tags.add("itemslot feet -1");
		}
		
		// Handle custom equipment
		List<String> slots = new ArrayList<String>();
		slots.addAll(this.slotmap.keySet());
		for(String str : slots)
		{
			
			Item i = this.slotmap.get(str);
			if(i == null)
				continue;
			
			if(!Generic.isNumeric(i.id))
			{
				Item ti = i.getCopy();
				ti.tags.add("OLDID " + i.id);
				ti.id = nationGen.getCustomItemId(i.id);
				this.setSlot(str, ti);
			}
		}
		
		
		// Ambidextrous. Should be after custom equipment handling and before command cleanup
		if(this.getSlot("offhand") != null && !this.getSlot("offhand").armor)
		{
			int len = 0;
			
			if(this.getSlot("weapon") != null)
				len = len + this.nationGen.weapondb.GetInteger(this.getSlot("weapon").id, "lgt");
			
			len = len + this.nationGen.weapondb.GetInteger(this.getSlot("offhand").id, "lgt");
			
			this.commands.add(new Command("#ambidextrous", "+" + len));

		}
		
		
		
		
		// Mapmove
		if(this.getSlot("mount") == null)
		{
			int MM = 2;
				
			for(Command c : race.unitcommands)
				if(c.command.equals("#mapmove"))
					MM = Integer.parseInt(c.args.get(0));
			
			for(Command c : pose.getCommands())
				if(c.command.equals("#mapmove"))
				{
					if(c.args.get(0).startsWith("-") || c.args.get(0).startsWith("+"))
					{
						String str = c.args.get(0);
						if(c.args.get(0).startsWith("+"))
							str = c.args.get(0).substring(1);
						
						MM += Integer.parseInt(str);
					}
					else
						MM = Integer.parseInt(c.args.get(0));
				}
			
			int origMM = MM;
			
			int enc = 0;
			for(String slot : slotmap.keySet())
			{
				if(getSlot(slot) != null && getSlot(slot).armor && !getSlot(slot).id.equals("-1"))
				{
					enc += nationGen.armordb.GetInteger(getSlot(slot).id, "enc", 0);
				}
			}
			
			
			int prot = this.getTotalProt(false);
			
			int enclimit = 4;
			if(Generic.containsTag(pose.tags, "mapmovepenaltyenc"))
				enclimit = Integer.parseInt(Generic.getTagValue(pose.tags, "mapmovepenaltyenc"));
			else if(Generic.containsTag(race.tags, "mapmovepenaltyenc"))
				enclimit = Integer.parseInt(Generic.getTagValue(race.tags, "mapmovepenaltyenc"));

			int protlimit = 14;
			if(Generic.containsTag(pose.tags, "mapmovepenaltyprot"))
				protlimit = Integer.parseInt(Generic.getTagValue(pose.tags, "mapmovepenaltyprot"));
			else if(Generic.containsTag(race.tags, "mapmovepenaltyprot"))
				protlimit = Integer.parseInt(Generic.getTagValue(race.tags, "mapmovepenaltyprot"));
			
			int penalty = 1;
			if(Generic.containsTag(pose.tags, "mapmovepenaltyamount"))
				penalty = Integer.parseInt(Generic.getTagValue(pose.tags, "mapmovepenaltyamount"));
			else if(Generic.containsTag(race.tags, "mapmovepenaltyamount"))
				penalty = Integer.parseInt(Generic.getTagValue(race.tags, "mapmovepenaltyamount"));
			
			if(enc >= enclimit || prot >= protlimit)
				MM = Math.max(1, MM - penalty);
			
			if(MM < origMM)
			{
				this.commands.add(new Command("#mapmove", "-" + (origMM - MM)));
			}
			
		}
		
		// Fist for things without proper weapons
		if(!Generic.containsTag(pose.tags, "no_free_fist") && !copystats && getClass() != ShapeChangeUnit.class)
			if(this.getSlot("weapon") == null || this.getSlot("weapon").id.equals("-1") || nationGen.weapondb.GetInteger(getSlot("weapon").id, "rng") != 0)
			{
				if(this.getSlot("bonusweapon") == null || this.getSlot("bonusweapon").id.equals("-1") || nationGen.weapondb.GetInteger(getSlot("bonusweapon").id, "rng") != 0)
				{

					this.commands.add(new Command("#weapon", "92 --- Fist given to units that could otherwise only kick."));
		
				}
			}
		

		// Autocalc enabler
		//u.commands.add(new Command("#gcost", "+10000"));


		// #price_per_command
		if(Generic.containsTag(Generic.getAllUnitTags(this), "price_per_command"))
		{
			List<String> pptags = Generic.getTags(Generic.getAllUnitTags(u), "price_per_command");

			for(String tag : pptags)
			{
				List<String> args = Generic.parseArgs(tag);
				int value = u.getCommandValue(args.get(1), 0);
				double cost = Double.parseDouble(args.get(2));
				int threshold = 0;
				if(args.size() > 3)
					threshold = Integer.parseInt(args.get(3));
				
			
				
				if(value > threshold)
				{
					value -= threshold;
					
					int total = (int)Math.round((double)value * cost);
					
					if(total > 0)
						commands.add(new Command("#gcost", "+" + total));
					else
						commands.add(new Command("#gcost", "" + total));
					

				}
				
			}
		}
		
		// #price_if_command
		if(Generic.containsTag(Generic.getAllUnitTags(this), "price_if_command"))
		{
			List<String> pptags = Generic.getTags(Generic.getAllUnitTags(u), "price_if_command");

			for(String tag : pptags)
			{
				List<String> args = Generic.parseArgs(tag);
				
				
				int value = u.getCommandValue(args.get(args.size() - 3), 0);
				int target = Integer.parseInt(args.get(args.size() - 2));
				String cost = args.get(args.size() - 1);
				
				
				boolean at = args.contains("at");
				boolean below = args.contains("below");
				boolean above = args.contains("above");
				
				if((value > target && above) || (value == target && at)  || (value < target) && below)
				{
					commands.add(new Command("#gcost", cost));
				}
				
			}
		}
		

		
		// Clean up commands
		List<Command> commands = u.getCommands();
		

		for(Command c : commands)
		{
			
			// Goldcost for holy units
			if(c.command.equals("#holy"))
			{
				this.handleCommand(commands, new Command("#gcost", "*1.3"));
			}
			
			// Discount for slowrec units
			if(c.command.equals("#slowrec"))
			{
				this.handleCommand(commands, new Command("#gcost", "*0.9"));
			}
			
		
			if(c.args.size() > 0 && c.args.get(0) != null && !c.args.get(0).equals(""))
			{
				
				
				// Mapmove is at least 1
				if(c.command.equals("#mapmove") && Integer.parseInt(c.args.get(0)) < 1 && !pose.tags.contains("immobile"))
					c.args.set(0, "1");
				

		
				// Weapons
				if(c.command.equals("#weapon"))
				{	
				 
					String realarg = c.args.get(0);
					if(realarg.contains(" "))
						realarg = c.args.get(0).split(" ")[0];
					
					try
					{
						Integer.parseInt(realarg);
					}
					catch(Exception e)
					{
						c.args.set(0, nationGen.getCustomItemId(c.args.get(0)) + "");
					}
		
				}
				
			}
		}
		u.commands = commands;



		

		// Montag mean costs
		if(Generic.containsTag(this.pose.tags, "montagpose") && !Generic.containsTag(this.pose.tags, "no_montag_mean_costs"))
		{
			int n = 0;
			int res = 0;
			int gold = 0;
			
			String firstshape = getStringCommandValue("#firstshape", "");
			for(List<Unit> lu : nation.unitlists.values())
			{
				for(Unit nu : lu)
					if(nu.getStringCommandValue("#montag", "").equals(firstshape) && u != nu)
					{
						nu.polish();
						res += nu.getResCost(true);
						gold += nu.getGoldCost();
						n++;
						
					}
			}
			
			if(n > 0)
			{
				res = (int)Math.round((double)res/(double)n) - getResCost(true);
				gold = (int)Math.round((double)gold/(double)n);
				this.handleCommand(commands, new Command("#gcost", "" + gold));
				this.handleCommand(commands, new Command("#rcost", "" + res));
			}
			
				
		}
		
		// %cost stuff
		int gcost = 0;
		
		if(percentCostCommands.size() > 0)
			gcost = this.getGoldCost();
		
		for(Command c : percentCostCommands)
		{
			if(gcost == 0)
				continue;
			
			double multi = 0;
			
			if(Generic.isNumeric(c.args.get(0).substring(5)))
					multi = Double.parseDouble(c.args.get(0).substring(5)) / 100;
			else if(c.args.size() > 1 && Generic.isNumeric(c.args.get(1)))
					multi = Double.parseDouble(c.args.get(1)) / 100;
			
			
			int price = (int)Math.round((double)gcost * multi);
			Command d = new Command(c.command, price + "");
			handleCommand(commands, d);
		}
		
		// Separate loop to round gcost at the end
		// Check for morale over 50
		for(Command c : commands)
		{

			
			if(c.args.size() == 0)
				continue;
			
			// goldcost rounding if gcost > 30
			if(c.command.equals("#gcost"))
			{
				int cost = Integer.parseInt(c.args.get(0));
				
				if(cost > 30)
				{
					cost = (int)Math.round((double)cost / 5) * 5;
					c.args.set(0, "" + cost);
				}
			}
			
			// morale 50 if over 50
			if(c.command.equals("#mor"))
			{
				int mor = Integer.parseInt(c.args.get(0));
				if(mor > 50)
				{
					c.args.set(0, "50");
				}
				else if(mor <= 0)
				{
					c.args.set(0, "1");
				}
			}
		}
		
		boolean isMage = false;
		
		for(Command c : commands)
		{
			if(c.command.equals("#magicskill") || c.command.equals("#custommagic"))
				isMage = true;
		}
		
		if(isMage) 
			MageGenerator.determineSpecialLeadership(u, false);
		
		polished = true;
	}
	
	protected void handleCommand(List<Command> commands, Command c)
	{


		// List of commands that may appear more than once per unit
		List<String> uniques = new ArrayList<String>();
		uniques.add("#weapon");
		uniques.add("#custommagic");
		uniques.add("#magicskill");
		uniques.add("#magicboost");
		
		int copystats = -1;

		c = new Command(c.command, c.args);
		Command old = null;
		for(Command cmd : commands)
		{
			if(cmd.command.equals(c.command))
				old = cmd;
			
			if(cmd.command.equals("#copystats"))
				copystats = Integer.parseInt(cmd.args.get(0));
		}
		

		// If the unit has #copystats it doesn't have defined stats. Thus we need to fetch value from database
		if(c.args.size() > 0 && (c.args.get(0).startsWith("+")) && copystats != -1 && old == null)
		{
			String value = this.nationGen.units.GetValue(copystats + "", c.command.substring(1));
			if(value.equals(""))
				value = "0";
			
			old = new Command(c.command, value);
			commands.add(old);	
			
		}
	
		
		if(old != null && !uniques.contains(c.command))
		{

			
			/*
			if(this.tags.contains("sacred") && c.command.equals("#gcost"))
				System.out.println(c.command + "  " + c.args);
			*/
			for(int i = 0; i < c.args.size(); i++)
			{
			
				String arg = c.args.get(i);
				String oldarg = old.args.get(i);
				if(arg.startsWith("+") || (arg.startsWith("-") && !arg.startsWith("--")))
				{
			
					
					if(arg.startsWith("+"))
						arg = arg.substring(1);
					

					try
					{
						oldarg = "" + (Integer.parseInt(oldarg) + Integer.parseInt(arg));
						old.args.set(i, oldarg);
					}
					catch(NumberFormatException e)
					{
						System.out.println("FATAL ERROR 1: Argument parsing " + oldarg + " + " + arg + " on " + c + " caused crash.");
					}
					continue;
			
				}
				else if(arg.startsWith("*"))
				{
				
					
					arg = arg.substring(1);
					try
					{
						oldarg = "" + (int)(Integer.parseInt(oldarg) * Double.parseDouble(arg));
						old.args.set(i, oldarg);
					}
					catch(Exception e)
					{
						System.out.println("FATAL ERROR 2: Argument parsing " + oldarg + " * " + arg + " on " + c.command + " caused crash.");
					}
					continue;
				}
				else
				{
			
					
					if(!uniques.contains(c.command))
					{
						oldarg = arg;
						old.args.set(i, oldarg);
						continue;
	
					}
					else
					{
						commands.add(c);
						continue;
					}
				}
			}
		}
		else 
		{
		
			
			for(int i = 0; i < c.args.size(); i++)
			{
				if(c.args.get(i).startsWith("+"))
					c.args.set(i, c.args.get(i).substring(1));
				else if(c.args.get(i).startsWith("*"))
					c.args.set(i, 0 + "");

			}

			commands.add(c);
		}
	
		

	}
	

		
	/**
	 * Returns unit hp, only basesprite and race count.
	 * @param u
	 * @return
	 */
	public int getHP()
	{
		Unit u = this;
		int hp = 0;
		for(Command c : u.race.unitcommands)
			if(c.command.equals("#hp"))
				hp += Integer.parseInt(c.args.get(0));
		
		for(Command c : u.getSlot("basesprite").commands)
			if(c.command.equals("#hp"))
			{
				String arg = c.args.get(0);
				if(c.args.get(0).startsWith("+"))
					arg = arg.substring(1);
				
				hp += Integer.parseInt(arg);
			}
		
		if(hp > 0)
			return hp;
		else
			return 10;
	}
	
	public void writeSlot(String slot, PrintWriter tw)
	{
		Item i = getSlot(slot);
		
		if(i == null || Integer.parseInt(i.id) < 1)
			return;
		
		Dom3DB armordb = nationGen.armordb;
		Dom3DB weapondb = nationGen.weapondb;
		
		if(i.armor)
			tw.println("#armor " + i.id + " --- " + armordb.GetValue(i.id, "armorname") + " / " + i.name);
		else
			tw.println("#weapon " + i.id + " --- " + weapondb.GetValue(i.id, "wpname") + " / " + i.name);
		
	}
	
	public boolean isLight()
	{
		if(this.getTotalProt(true) > 10)
			return false;
		
		return true;
	}
	
	public boolean isHeavy()
	{
		if(this.getTotalProt(true) <= 12)
			return false;

		return true;
	}

	
	public int getTotalProt()
	{
		return getTotalProt(true);
	}
	
	
	public int getTotalProt(boolean naturalprot)
	{
		int armorprot = 0;
		int helmetprot = 0;
		int natural = 0;
		
		for(Command c : this.getCommands())
		{
			if(c.command.equals("#prot"))
			{
				String arg = c.args.get(0).replace("+", "");
				natural = natural + Integer.parseInt(arg);
			}
		}
	
		Dom3DB armordb = nationGen.armordb;
		
		for(String slot : slotmap.keySet())
		{
			if(getSlot(slot) != null && getSlot(slot).armor)
			{
				if(armordb.GetInteger(getSlot(slot).id, "type") == 5)
				{
					armorprot += armordb.GetInteger(getSlot(slot).id, "prot", 0);
				}
				else if(armordb.GetInteger(getSlot(slot).id, "type") == 6)
				{
					helmetprot += armordb.GetInteger(getSlot(slot).id, "prot", 0);
				}

			}
		
		}
		
		// To not make special stuff that may not be handled considered light
		if(armorprot == 0 && getSlot("armor") != null && !getSlot("armor").id.equals("-1"))
			armorprot = 10;
		if(helmetprot == 0 && getSlot("helmet") != null && !getSlot("helmet").id.equals("-1"))
			helmetprot = 10;
		
		double prot = (0.2 * (double)helmetprot + 0.8 * (double)armorprot);
		if(!naturalprot)
			return (int)Math.round(prot);
		
		return (int)Math.round(natural + prot - (natural * prot / 40));
	}
	
	public int getTotalEnc()
	{
		int eqenc = 0;
		int natural = 0;
		
		for(Command c : this.getCommands())
		{
			if(c.command.equals("#enc"))
			{
				String arg = c.args.get(0).replace("+", "");
				natural = natural + Integer.parseInt(arg);
			}
		}
	
		Dom3DB armordb = nationGen.armordb;
		
		
		for(String slot : slotmap.keySet())
		{
			if(getSlot(slot) != null && getSlot(slot).armor && !getSlot(slot).id.equals("-1"))
			{
				eqenc += armordb.GetInteger(getSlot(slot).id, "enc", 0);
			}
		}
		



		double prot = eqenc + natural;
		
		return (int)prot;
	}
	
	
	public void write(PrintWriter tw, String spritedir) throws IOException
	{ 


		tw.println("--- " + getName() + " (" + race.name + "), Gold: " + getGoldCost() + ", Resources: " + getResCost(true) + ", Roles: " + pose.roles + " (" + pose.name + ")");
		tw.println("--- OFFSET DEBUG: ");
		if(this.getSlot("weapon") != null)
		{
			tw.println("-- Weapon: " + this.getSlot("weapon").getOffsetX() + ", " + this.getSlot("weapon").getOffsetY());
		}
		if(this.getSlot("armor") != null)
		{
			tw.println("-- Armor: " + this.getSlot("armor").getOffsetX() + ", " + this.getSlot("armor").getOffsetY());
		}
		if(this.getSlot("offhand") != null)
		{
			tw.println("-- Offhand: " + this.getSlot("offhand").getOffsetX() + ", " + this.getSlot("offhand").getOffsetY());
		}
		tw.print("--- Generation tags: ");
		for(String str : this.tags)
			tw.print(str + ", ");
		tw.println();
		tw.print("--- Applied filters: ");
		for(Filter f : this.appliedFilters)
		{
			if(f.name.equals("MAGICPICKS"))
				tw.print(f.name + " (" + ((MagicFilter)f).pattern.getPrice() + "), ");
			else
				tw.print(f.name + ", ");
		}
		tw.println();
		
		tw.println("#newmonster " + id);
		
		if(!this.name.toString(this).equals("UNNAMED"))
		{
			tw.println("#name \"" + name.toString(this) +  "\"");
		}
		
		if(this.getSlot("basesprite") != null)
		{
			tw.println("#spr1 \"" + (spritedir + "unit_" + this.id + "_a.tga\""));
			tw.println("#spr2 \"" + (spritedir + "unit_" + this.id + "_b.tga\""));
		}
		//tw.println("#descr \"" + desc + "\"");
		
		writeCommands(tw);
		
		// Write all instead of just some stuff (14.3.2014)
		for(String slot : slotmap.keySet())
		{
			if(slotmap.get(slot) != null)
			{
				writeSlot(slot, tw);
			}
		}
		
		tw.println("#end");
		tw.println("");
		
	}
	
	public int getCommandValue(String command, int defaultv)
	{
		int value = defaultv;
		for(Command c : this.getCommands())
		{
			if(c.command.equals(command) && c.args.size() > 0)
				value = Integer.parseInt(c.args.get(0));
		}
		return value;
	}
	
	public String getStringCommandValue(String command, String defaultv)
	{
		String value = defaultv;
		for(Command c : this.getCommands())
		{
			if(c.command.equals(command) && c.args.size() > 0)
				value = c.args.get(0);
		}
		return value;
	}
	
	
	// 20150522EA : my OOP prof back in undergrad would probably shoot me for this method...
	public void setCommandValue(String command, int argIndex, String newValue)
	{
		for(int x = 0; x < this.commands.size(); x++)
		{
			if(this.commands.get(x).toString().startsWith("#descr") && this.commands.get(x).args.size() > argIndex)
			{
				if(newValue.startsWith("\""))
					this.commands.get(x).args.set(argIndex, newValue);
				else
					this.commands.get(x).args.set(argIndex, "\"" + newValue + "\"");
			}
		}
	}
	
	private void writeCommands(PrintWriter tw)
	{
	

		List<Command> tempCommands = this.commands;

		
		tw.println("#itemslots " + this.getItemSlots());

		for(Command c : tempCommands)
		{
			if(c.args.size() > 0)
			{
				if(c.command.toString().startsWith("#descr"))
				{
					if(Generic.listToString(c.args).startsWith("\""))
						tw.print(c.command + " " + Generic.listToString(c.args));
					else
						tw.print(c.command + " \"" + Generic.listToString(c.args));
					if(Generic.listToString(c.args).endsWith("\""))
						tw.print("\n");
					else
						tw.println("\"");
				}
				else if(c.command.equals("#itemslots"))
				{
					// Skipped here
				}
				else
					tw.println(c.command + " " + Generic.listToString(c.args));
			}
			else
				tw.println(c.command);
		}
	
		
	}
	
	public void draw(String spritedir) throws IOException
	{
		if(getSlot("basesprite") == null)
			return;
		
		Drawing.writeTGA(this.render(0), "./mods/" + spritedir + "unit_" + this.id + "_a.tga");

		// The super awesome attack sprite generation:
		Drawing.writeTGA(this.render(-5), "./mods/" + spritedir + "unit_" + this.id + "_b.tga");
	}
	
	
	public BufferedImage render() throws IOException
	{
		return render(0);
	}
	
	public String getMountOffsetSlot()
	{
		
		String mountslot = "mount";
		if(Generic.containsTag(Generic.getAllUnitTags(this), "mount_offset_slot"))
		{
			mountslot = Generic.getTagValue(Generic.getAllUnitTags(this), "mount_offset_slot");
		}
		return mountslot;
	}
	
	public BufferedImage render(int offsetX) throws IOException
	{
		if(this.getClass() == ShapeChangeUnit.class)
			return null;
		
		
		Unit u = this;

		// Get width and height;
		
	
		BufferedImage base;
		try
		{ 
			base = ImageIO.read(new File("./", u.slotmap.get("basesprite").sprite));
		}
		catch(Exception e)
		{
			System.out.println("No base sprite for " + pose.roles + " unit of " + race.name + ", id " + id + ".");
			if(u.slotmap.get("basesprite") != null)
				System.out.println("Tried to open " + u.slotmap.get("basesprite").sprite);
			else
				System.out.println("Unit had no basesprite.");
			
			base = new BufferedImage(64, 64, BufferedImage.TYPE_3BYTE_BGR);
		}
	
		// create the new image, canvas size is the max of both image sizes
		int w = base.getWidth();
		int h = base.getHeight();
		
		
		
		String mountslot = getMountOffsetSlot();
		if(slotmap.get(mountslot) != null && !slotmap.get(mountslot).sprite.equals(""))
		{
			base = ImageIO.read(new File("./", u.slotmap.get(mountslot).sprite));
			w = base.getWidth();
			h = base.getHeight();
		}
	
		
		BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		// paint both images, preserving the alpha channels
		Graphics g = combined.getGraphics();
		g.setColor(Color.black);
		g.fillRect(0, 0, w, h);
		

		

		for(String s : pose.renderorder.split(" "))
		{

					
	
			if(s.equals(mountslot) || (!u.pose.tags.contains("non_mount_overlay") && s.equals("overlay") && u.getSlot(s) != null && u.getSlot("overlay").getOffsetX() == 0 && u.getSlot("overlay").getOffsetY() == 0))
			{
				renderSlot(g, this, s, false, offsetX);
			}
			else if(s.equals("basesprite") && u.slotmap.get(mountslot) == null)
				renderSlot(g, this, s, false, offsetX);
			else if(s.equals("offhandw") && (getSlot("offhand") != null && !getSlot("offhand").armor))
				renderSlot(g, this, "offhand", true, offsetX);
			else if(s.equals("offhanda") && (getSlot("offhand") != null && getSlot("offhand").armor))
				renderSlot(g, this, "offhand", true, offsetX);
			else
				renderSlot(g, this, s, true, offsetX);

		}
		
			
		// Save as new image
		return combined; 
	}
	
	private void renderSlot(Graphics g, Unit u, String slot, boolean useoffset, int extraX) throws IOException
	{
		
		List<Item> possibleitems = new ArrayList<Item>();
		
		Iterator<Item> itr = slotmap.values().iterator();
		while(itr.hasNext())
		{
			Item i = itr.next();
			if(i != null && slot.equals(i.renderslot))
				possibleitems.add(i);
		}
		


		
		for(int i = 10; i >= 1; i--)
		{
			for(Item item : possibleitems)
			{
				if(item.renderprio == i)
				{
					renderItem(g, item, useoffset, extraX);
					return;
				}
			}
		}
		
	}
	
	
	private void renderItem(Graphics g, Item i, boolean useoffset, int extraX) throws IOException
	{
		if(i == null)
			return;
		
		if(useoffset)
		{
			String mountslot = getMountOffsetSlot();
			
			int offsetx = 0;
			int offsety = 0;
			if(this.getSlot(mountslot) != null)
			{
				offsetx += getSlot(mountslot).getOffsetX();
				offsety += getSlot(mountslot).getOffsetY();
			}
			
			i.render(g, true, offsetx, offsety, this.color, extraX);

		}
		else
			i.render(g, false, 0, 0, this.color, extraX);
		
	}
}
