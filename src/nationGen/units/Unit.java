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
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.naming.Name;
import nationGen.nation.Nation;



public class Unit {

	public Name name = new Name();
	public Race race;
	public Pose pose;
	public LinkedHashMap<String, Item> slotmap = new LinkedHashMap<String, Item>();
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
	
	public boolean isRanged()
	{
		if(getSlot("weapon") == null)
			return false;
		
		Dom3DB weapondb = nationGen.weapondb;
		if(weapondb.GetValue(getSlot("weapon").id, "rng", "0").equals("0"))
			return true;
		
		return false;
	}
	
	private void handleDependency(String slotname)
	{
		if(getSlot(slotname) == null)
			return;
		
		// This handles #needs
		if(getSlot(slotname).dependencies.size() > 0)
		{
			for(String itemname : getSlot(slotname).dependencies.keySet())
			{
				String slot = getSlot(slotname).dependencies.get(itemname);
				
	
				if(pose.getItems(slot) == null)
				{
			
					System.out.println("#needs for " + slotname + ", item " + itemname + " and item " + getSlot(slotname).name + " on slot " + slot + " failed. Roles " + this.pose.roles + ", race " + race.name );
					break;
				}
				Item item = pose.getItems(slot).getItemWithName(itemname, slot);
				
				
				if(item != null)
					setSlot(slot, item); 
				else
				{


					System.out.println("!!! " + getSlot(slotname).name + " on slot " + slotname + " tried to link to " + itemname + " on list " + slot + ", but such item was not found. Check your definitions! Pose " + this.pose.roles + ", race " + this.race.name);
				}
			}
		}
		
		// This handles #needstype
		if(getSlot(slotname).typedependencies.size() > 0 && this.nation != null)
		{
			ChanceIncHandler chandler = new ChanceIncHandler(nation);
			Random r = new Random(nation.random.nextInt());
			
			for(String type : getSlot(slotname).typedependencies.keySet())
			{
				String slot = getSlot(slotname).typedependencies.get(type);
				
	
				if(pose.getItems(slot) == null)
				{
					System.out.println("#needstype for " + slotname + ", type " + type + " and item " + getSlot(slotname).name + " on slot " + slot + " failed due to the slot having no items. Roles " + this.pose.roles + ", race " + race.name );
					break;
				}
				
				
				List<Item> possibles = ChanceIncHandler.getFiltersWithType(type, pose.getItems(slot));
		
				
				Item item = Entity.getRandom(r, chandler.handleChanceIncs(this, possibles));
				
				if(item != null)
					setSlot(slot, item); 
	
			}
		}
	}

	
	public void setSlot(String slotname, Item newitem)
	{
		
	
	
		slotmap.put(slotname, newitem);
		if(newitem == null)
			return;


		handleDependency(slotname);
		

		
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
	
	public int getResCost(boolean useSize)
	{
		int res = 0;
		int size = 2;
		int ressize = -1;
		int extrares = 0;
		
		Dom3DB weapondb = nationGen.weapondb;
		Dom3DB armordb = nationGen.armordb;
		
		List<Command> commands = this.getCommands();
		for(Command c : commands)
			if(c.command.equals("#rcost"))
				extrares = extrares + Integer.parseInt(c.args.get(0));
			else if(c.command.equals("#size"))
				size = Integer.parseInt(c.args.get(0));
			else if(c.command.equals("#ressize"))
				ressize = Integer.parseInt(c.args.get(0));
		
		if(getSlot("armor") != null)
			res = res + armordb.GetInteger(this.getSlot("armor").id, "res");
		if(getSlot("helmet") != null)
			res = res + armordb.GetInteger(this.getSlot("helmet").id, "res");
		if(getSlot("weapon") != null)
			res = res + weapondb.GetInteger(this.getSlot("weapon").id, "res");
		if(getSlot("bonusweapon") != null)
			res = res + weapondb.GetInteger(this.getSlot("bonusweapon").id, "res");
		if(getSlot("offhand") != null)
		{
			if(getSlot("offhand").armor)
				res = res + armordb.GetInteger(this.getSlot("offhand").id, "res");
			else
				res = res + weapondb.GetInteger(this.getSlot("offhand").id, "res");
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
		List<Command> percentCostCommands = new ArrayList<Command>();
		
		if(polished)
			return this.commands;
		else
		{
			allCommands.addAll(race.unitcommands);
			allCommands.addAll(pose.getCommands());
	
	

		
			// Item commands
			Iterator<Item> itr = slotmap.values().iterator();
			while(itr.hasNext())
			{
				Item item = itr.next();
				
				if(item != null)
					allCommands.addAll(item.commands);
			}
			

			
			// Filters
			for(Filter f : this.appliedFilters)
			{
				for(Command c : f.getCommands())
				{
					Command tc = c;
					int tier = 2;
					if(Generic.getTagValue(this.tags, "schoolmage") != null)
						tier = Integer.parseInt(Generic.getTagValue(this.tags, "schoolmage"));
					
					if(c.args.size() > 0 && c.args.get(0).contains("%value%"))
					{
						int multi = 10;
						int base = -5;
						try
						{
							multi = Integer.parseInt(Generic.getTagValue(f.tags, "valuemulti"));
							base = Integer.parseInt(Generic.getTagValue(f.tags, "basvalue"));
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
					allCommands.add(tc);
					
			
				}
			}
		}
		
		
		// Adjustment stuff
		allCommands.addAll(this.commands);


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
		// Stuff with %cost as argument
		if(percentCostCommands.size() > 0)
		{
			int gcost = 0;
			for(Command c : allCommands)
				if(c.command.equals("#gcost") && c.args.size() > 0)
				{
					
					if(!multiCommands.contains(c))
					{
						String arg = c.args.get(0);
						if(arg.startsWith("+"))
							arg = arg.substring(1);
						gcost += Integer.parseInt(arg);
					}
					else if(!percentCostCommands.contains(c))
						gcost *= Double.parseDouble(c.args.get(0).substring(1));
				}
			
			for(Command c : percentCostCommands)
			{
				double multi = 0;
				
				if(Generic.isNumeric(c.args.get(0).substring(5)))
						multi = Double.parseDouble(c.args.get(0).substring(5)) / 100;
				else if(c.args.size() > 1 && Generic.isNumeric(c.args.get(1)))
						multi = Double.parseDouble(c.args.get(1)) / 100;
				
				
				int price = (int)Math.round((double)gcost * multi);
				Command d = new Command(c.command, price + "");
				handleCommand(tempCommands, d);
			}
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
	
	
	public void polish()
	{
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
		
		// +2hp to mounted
		if(this.getSlot("mount") != null)
		{
			this.commands.add(new Command("#hp", "+2"));
		}
		

		// Slot removal
		for(String slot : this.pose.renderorder.split(" "))
		{
			if(getSlot(slot) != null)
			{
				for(String tag : getSlot(slot).tags)
					if(tag.startsWith("noslot "))
						this.setSlot(tag.split(" ")[1], null);
			}
		}
		
		// Handle custom equipment
		for(String str : this.slotmap.keySet())
		{
			Item i = this.slotmap.get(str);
			if(i == null)
				continue;
			
			if(!Generic.isNumeric(i.id))
			{

				Item ti = i.getCopy();
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
			if(getSlot("armor") != null)
				enc += nationGen.armordb.GetInteger(getSlot("armor").id, "enc");
			if(getSlot("offhand") != null && getSlot("offhand").armor)
				enc += nationGen.armordb.GetInteger(getSlot("offhand").id, "enc");
			if(getSlot("helmet") != null)
				enc += nationGen.armordb.GetInteger(getSlot("helmet").id, "enc");
			
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
				

				// Second shapes
				if(c.command.contains("shape") && !nationGen.hasShapeShift(c.args.get(0)))
				{
				
					nationGen.handleShapeshift(c, u);
				}
				
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

		// Separate loop to round gcost at the end
		
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
					cost = Math.round(cost / 5) * 5;
					c.args.set(0, "" + cost);
				}
			}
		}
		
		
		polished = true;
	}
	
	protected void handleCommand(List<Command> commands, Command c)
	{


		// List of commands that may appear more than once per unit
		List<String> uniques = new ArrayList<String>();
		uniques.add("#weapon");
		uniques.add("#custommagic");
		uniques.add("#magicskill");
		
		
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
		
		
		if(c.args.size() > 0 && (c.args.get(0).startsWith("+")) && copystats != -1 && old == null)
		{
			String value = this.nationGen.units.GetValue(copystats + "", c.command.substring(1));
			if(!value.equals(""))
			{
				old = new Command(c.command, value);
				commands.add(old);
			}
		}
		else if(old != null && !uniques.contains(c.command))
		{
			/*
			if(this.tags.contains("sacred") && c.command.equals("#gcost"))
				System.out.println(c.command + "  " + c.args);
			*/
			for(int i = 0; i < c.args.size(); i++)
			{
			
				String arg = c.args.get(i);
				String oldarg = old.args.get(i);
				if(arg.startsWith("+") || arg.startsWith("-"))
				{
					if(arg.startsWith("+"))
						arg = arg.substring(1);
					

					try
					{
						//if(this.tags.contains("schoolmage 3"))
							//System.out.println(arg + " + " + oldarg + " = " + (int)(Integer.parseInt(oldarg) + Double.parseDouble(arg)));
						
						
						oldarg = "" + (Integer.parseInt(oldarg) + Integer.parseInt(arg));
						old.args.set(i, oldarg);
					}
					catch(NumberFormatException e)
					{
						System.out.println("FATAL ERROR: Argument parsing " + oldarg + " + " + arg + " on " + c.command + " caused crash.");
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
						System.out.println("FATAL ERROR: Argument parsing " + oldarg + " * " + arg + " on " + c.command + " caused crash.");
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
		
		if(getSlot("armor") != null)
			armorprot = armordb.GetInteger(getSlot("armor").id, "prot", 0);
		
		if(getSlot("helmet") != null)
			helmetprot = armordb.GetInteger(getSlot("helmet").id, "prot", 0);

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
		int armorprot = 0;
		int helmetprot = 0;
		int offhandprot = 0;
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
		
		if(getSlot("armor") != null)
			armorprot = armordb.GetInteger(getSlot("armor").id, "enc", 0);
		
		if(getSlot("helmet") != null)
			helmetprot = armordb.GetInteger(getSlot("helmet").id, "enc", 0);

		
		if(getSlot("offhand") != null)
			offhandprot = armordb.GetInteger(getSlot("offhand").id, "enc", 0);



		double prot = offhandprot + armorprot + helmetprot + natural;
		
		return (int)prot;
	}
	
	
	public void write(PrintWriter tw, String spritedir) throws IOException
	{ 


		tw.println("--- " + getName() + " (" + race.name + "), Gold: " + getGoldCost() + ", Resources: " + getResCost(true) + ", Roles: " + pose.roles + " (" + pose.name + ")");
		tw.println("--- OFFSET DEBUG: ");
		if(this.getSlot("weapon") != null)
		{
			tw.println("- Weapon: " + this.getSlot("weapon").getOffsetX() + ", " + this.getSlot("weapon").getOffsetY());
		}
		if(this.getSlot("armor") != null)
		{
			tw.println("- Armor: " + this.getSlot("armor").getOffsetX() + ", " + this.getSlot("armor").getOffsetY());
		}
		if(this.getSlot("offhand") != null)
		{
			tw.println("- Offhand: " + this.getSlot("offhand").getOffsetX() + ", " + this.getSlot("offhand").getOffsetY());
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
		
		

		if(slotmap.get("mount") != null)
		{
			base = ImageIO.read(new File("./", u.slotmap.get("mount").sprite));
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

					
	
			if(s.equals("mount") || s.equals("overlay"))
			{
				renderSlot(g, this, s, false, offsetX);
			}
			else if(s.equals("basesprite") && u.slotmap.get("mount") == null)
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
		

		
		int offsetx = 0;
		int offsety = 0;
		if(this.getSlot("mount") != null)
		{
			offsetx += getSlot("mount").getOffsetX();
			offsety += getSlot("mount").getOffsetY();
		}

		if(!useoffset)
			i.render(g, false, 0, 0, this.color, extraX);
		else
			i.render(g, true, offsetx, offsety, this.color, extraX);
	}
}
