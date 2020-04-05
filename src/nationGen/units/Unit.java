package nationGen.units;


import com.elmokki.Dom3DB;
import com.elmokki.Generic;
import nationGen.NationGen;
import nationGen.entities.Filter;
import nationGen.entities.MagicFilter;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.items.ItemDependency;
import nationGen.magic.MageGenerator;
import nationGen.magic.MagicPath;
import nationGen.magic.MagicPathInts;
import nationGen.misc.Arg;
import nationGen.misc.Args;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.misc.FileUtil;
import nationGen.misc.Operator;
import nationGen.misc.Tags;
import nationGen.naming.Name;
import nationGen.nation.Nation;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Unit {

	public Name name = new Name();
	public Race race;
	public Pose pose;
	public final SlotMap slotmap = new SlotMap();
	public static class SlotMap {
		private LinkedHashMap<String, Deque<Item>> slotmemory = new LinkedHashMap<>();
		
		public Set<String> getSlots() {
			return slotmemory.keySet();
		}
		
		public Stream<String> slots() {
			return slotmemory.keySet().stream();
		}
		
		public Stream<Item> items() {
			return slotmemory.values().stream().map(Deque::peek).filter(Objects::nonNull);
		}
		
		Item get(String slot) {
			Deque<Item> stack = slotmemory.get(slot);
			return stack == null ? null : stack.peek();
		}
		void push(String slot, Item item) {
			getSlotStack(slot).push(item);
		}
		Item pop(String slot) {
			return getSlotStack(slot).pollFirst();
		}
		void addAllFrom(SlotMap source) {
			source.slotmemory.forEach((slot, stack) -> getSlotStack(slot).addAll(stack));
		}
		
		private Deque<Item> getSlotStack(String slot) {
			return slotmemory.computeIfAbsent(slot, k -> new LinkedList<>());
		}
	}

	public Color color = Color.white;
	public int id = -1;
	public List<Command> commands = new ArrayList<>();
	public NationGen nationGen;
	public boolean caponly = false;
	public Tags tags = new Tags();
	public List<Filter> appliedFilters = new ArrayList<>();
	public boolean polished = false;
	public boolean invariantMonster = false;  // Is the unit a monster that must be used as-is instead of copying? (E.g., hydras)
	private Nation nation = null;
	private List<Command> percentCostCommands = new ArrayList<>();

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
	
	public boolean isSlotEmpty(String slot) {
		return slotmap.get(slot) == null;
	}
	
	public Unit getCopy()
	{
		Unit unit = new Unit(nationGen, race, pose);
		
		
		// Copy unit
		unit.slotmap.addAllFrom(this.slotmap);
		
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
	public MagicPathInts getMagicPicks()
	{
		return getMagicPicks(false);
	}
	
	/**
	 * Gets unit's magic picks as int[8]
	 * @param randoms if true, randoms at 25% chance are included
	 * @return
	 */
	public MagicPathInts getMagicPicks(boolean randoms)
	{

		MagicPathInts picks = new MagicPathInts();
		
		double prob = 1;
		if(randoms)
			prob = 0.25;
		
		for(Filter f : appliedFilters)
		{
			if(f.name.equals("MAGICPICKS") || f.name.equals("PRIESTPICKS"))
			{
				MagicFilter m = (MagicFilter)f;
				MagicPathInts withRandoms = m.pattern.getPathsWithRandoms(m.prio, prob);
				for(MagicPath path : MagicPath.values())
				{
					picks.add(path, withRandoms.get(path));
				}
			}
		}
		

		
		return picks;
	}
	
	public int getHolyPicks()
	{
		return getMagicPicks().get(MagicPath.HOLY);
	}
	
	
	public double getMagicAmount(double prob)
	{
		double n = 0;	
		for(Filter f : appliedFilters)
			if(f.name.equals("MAGICPICKS") && !f.tags.containsName("holy"))
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
			if(f.name.equals("MAGICPICKS") && !f.tags.containsName("holy"))
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
			if(f.name.equals("MAGICPICKS") && !f.tags.containsName("holy"))
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
			if(f.name.equals("MAGICPICKS") && !f.tags.containsName("holy"))
			{
				MagicFilter m = (MagicFilter)f;
				n += m.pattern.getPathsAtleastAt(1, prob);
			}
		
		return n;
	}
	
	
	private int handleModifier(Arg mod, int value)
	{
		Optional<Operator> operator = mod.getOperator();
		if (operator.isPresent() && (operator.get() == Operator.ADD || operator.get() == Operator.SUBTRACT))
		{
			value += mod.getInt();
		}
		else
			value = mod.getInt();
		
		return value;
	}
	
	
	protected Optional<String> writeBodytypeLine()
	{
		String[] coms = {"#lizard", "#quadruped", "#bird", "#snake", "#djinn", "#miscshape", "#humanoid", "#mountedhumanoid", "#troglodyte", "#naga", "#copystats"};
		for(String str : coms)
			if(this.hasCommand(str))
			{
				return Optional.empty();
			}

		int slots = this.getItemSlots();
		
		// has feet and an arm
		if(	Generic.containsBitmask(slots, 2048) && Generic.containsBitmask(slots, 2))
		{
			// has head
			if(Generic.containsBitmask(slots, 128))
				return Optional.of("#humanoid");
			else
				return Optional.of("#troglodyte");

		}
		// no feet, but arm
		else if (Generic.containsBitmask(slots, 2))
		{
			boolean mounted = this.getSlot("mount") != null;
			if(mounted)
				return Optional.of("#mountedhumanoid");
			else
				return Optional.of("#naga");

		}
		// feet, no arm
		else if(Generic.containsBitmask(slots, 2048))
		{
			return Optional.of("#quadruped");

		}
		// no feet nor arm
		else
		{
			return Optional.of("#miscshape");
		}
		
	}
	
	public int getItemSlots()
	{
		
		int slots = -1;
		for(Command c : this.getCommands())
			if(c.command.equals("#itemslots"))
				slots = c.args.get(0).getInt();
		
		if(slots == -1)
		{
			slots = 0;

			Tags unitTags = Generic.getAllUnitTags(this);
			
			Tags itemTags = new Tags();
			Item basesprite = this.slotmap.get("basesprite");
			if(basesprite != null)
				itemTags.addAll(basesprite.tags);

			this.slotmap.items()
				.filter(i -> i != basesprite)
				.forEach(i -> itemTags.addAll(i.tags));
			
			// itemTags.addAll(this.tags);
			
			
			int head = 1;
			int body = 1;
			int feet = 1;
			int hand = 2;
			int misc = 2;
			
			for(Args args : unitTags.getAllArgs("baseitemslot"))
			{
				String slot = args.get(0).get();
				Arg modifier = args.get(1);
				switch (slot) {
					case "head":
						head = handleModifier(modifier, head);
						break;
					case "misc":
						misc = handleModifier(modifier, misc);
						break;
					case "body":
						body = handleModifier(modifier, body);
						break;
					case "hand":
						hand = handleModifier(modifier, hand);
						break;
					case "feet":
						feet = handleModifier(modifier, feet);
						break;
				}
			}
			
			for(Args args : tags.getAllArgs("itemslot"))
			{
				String slot = args.get(0).get();
				Arg modifier = args.get(1);
				switch (slot) {
					case "head":
						head += handleModifier(modifier, head);
						break;
					case "misc":
						misc += handleModifier(modifier, misc);
						break;
					case "body":
						body += handleModifier(modifier, body);
						break;
					case "hand":
						hand += handleModifier(modifier, hand);
						break;
					case "feet":
						feet += handleModifier(modifier, feet);
						break;
				}
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
		
		return nationGen.weapondb.GetValue(getSlot("weapon").id, "rng", "0").equals("0");
	}
	
	
	private void handleRemoveDependency(Item i)
	{
	
		if(i == null)
			return;
		
		for(ItemDependency d : i.dependencies)
		{
			if(d.lagged)
				continue;
			
			String slot = d.slot;
			
			Item old = slotmap.pop(slot);

			handleSlotChange(slot, old, slotmap.get(slot));
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
		for(ItemDependency d : getSlot(slotname).dependencies)
		{
			if(d.lagged != lagged)
				continue;
			
			String target = d.target;
			String slot = d.slot;
			

			if(!d.type) // Handle needs and setslot
			{
				String command = lagged ? "#forceslot" : "#needs";
				
				if(pose.getItems(slot) == null)
				{
					throw new IllegalStateException(command + " for " + slotname + ", item " + target + " and item " + getSlot(slotname).name + " on slot " + slot + " failed. Roles " + this.pose.roles + ", race " + race.name );
				}
				Item item = pose.getItems(slot).getItemWithName(target, slot);
				
				
				if(item == null)
				{
					throw new IllegalStateException(getSlot(slotname).name + " on slot " + slotname + " tried to link to " + target + " on list " + slot + ", but such item was not found. Check your definitions! Pose " + this.pose.roles + ", race " + this.race.name);
				}
				setSlot(slot, item);
			}
			else if(chandler != null) // Handle needstype and setslottype
			{
				String command = lagged ? "#forceslottype" : "#needstype";
			
		
				if(pose.getItems(slot) == null)
				{
					System.out.println(command + " for " + slotname + ", type " + target + " and item " + getSlot(slotname).name + " on slot " + slot + " failed due to the slot having no items: " + pose + ", "+ this.pose.roles + ", race " + race.name );
					break;
				}
				
				
				List<Item> possibles = ChanceIncHandler.getFiltersWithType(target, pose.getItems(slot));
				Item item = chandler.handleChanceIncs(this, possibles).getRandom(r);
				
				if(item != null)
				{
					setSlot(slot, item);
				}
			}
		}
		

	}


	private void handleRemoveThemeinc(Item item)
	{
		if(item == null)
			return;
		
		if(item.filter != null)
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
		
		slotmap.push(slotname, newitem);
		
		handleSlotChange(slotname, olditem, newitem);
	}
	
	private void handleSlotChange(String slotName, Item oldItem, Item newItem) {
		if (oldItem != null) {
			handleRemoveThemeinc(oldItem);
			handleRemoveDependency(oldItem);
		}
		
		if (newItem != null) {
			handleDependency(slotName, false);
			handleAddThemeinc(newItem);
		}
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
				cost += c.args.get(0).getInt();
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
			return this.nationGen.units.GetValue("" + stats, "unitname");
		}
		
		return this.name.toString(this);
		
	}
	

	public String getLeaderLevel()
	{
		return getLeaderLevel("");
	}
	
	public String getLeaderLevel(String prefix)
	{
		String level = "ok";
		for(Command c : this.getCommands())
		{
			if(c.command.endsWith("leader")) {
				String lead = c.command.substring(1, c.command.indexOf(prefix + "leader"));
				
				if (Generic.LEADERSHIP_LEVELS.contains(lead))
					level = lead;
			}
		}
		
		return level;
	}
	
	public boolean hasCommand(String cmd)
	{		
		for(Command c : this.getCommands())
		{
			if(c.command.equals(cmd))
				return true;

		}
		
		return false;		
	}
	
	
	
	
	public boolean hasLeaderLevel(String prefix)
	{
		String level = null;
		for(Command c : this.getCommands())
		{
			if(c.command.endsWith(prefix + "leader")) {
				String lead = c.command.substring(1, c.command.indexOf(prefix + "leader"));
				
				if (Generic.LEADERSHIP_LEVELS.contains(lead))
					level = lead;
			}
		}
		
		return level != null;
		
	}
	
	public int getResCost(boolean useSize)
	{
		return getResCost(useSize, this.getCommands());
	}

	private int getResCost(boolean useSize, List<Command> commands)
	{
		int size = 2;
		int ressize = -1;
		int extrares = 0;
		
		Dom3DB weapondb = nationGen.weapondb;
		Dom3DB armordb = nationGen.armordb;
		

		for(Command c : commands)
			if(c.command.equals("#rcost"))
				extrares += c.args.get(0).getInt();
			else if(c.command.equals("#size"))
				size = c.args.get(0).getInt();
			else if(c.command.equals("#ressize"))
				ressize = c.args.get(0).getInt();
		
		
		int res = this.slotmap.items()
			.filter(i -> !i.id.equals("-1"))
			.mapToInt(i -> (i.armor ? armordb : weapondb).GetInteger(i.id, "res"))
			.sum();


		
		if(useSize) {
			if (ressize > 0)
				res = ressize * res / 2;
			else
				res = size * res / 2;
		}
		
		// Dom3 minimum res amount is 1.
		return Math.max(res + extrares, 1);
	}
	
	public List<Command> getCommands()
	{

		List<Command> allCommands = new ArrayList<>();
		
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
			slotmap.items().forEach(item -> allCommands.addAll(item.commands));


			// Filters
			for(Filter f : this.appliedFilters)
			{
				
				for(Command c : f.getCommands())
				{
			
					
					Command tc = c;
					final int tier = tags.getInt("schoolmage").orElse(2);
					
					if(c.args.size() > 0 && c.args.get(0).get().contains("%value%"))
					{
						int multi = f.tags.getInt("valuemulti").orElse(10);
						int base = f.tags.getInt("basevalue").orElse(-5);
						
						int result = base + multi * tier;
						
						String resstring = "" + result;
						if(result > 0)
						{
							resstring = "+" + resstring;
						}
						
						if(result != 0)
							tc = Command.args(c.command, resstring);
						
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
			List<Command> adjustmentcommands = new ArrayList<>();
			for(Arg str : Generic.getAllUnitTags(this).getAllValues("adjustmentcommand"))
			{
				adjustmentcommands.add(str.getCommand());
			}
			
			
			// Special case: #fixedrescost
			Generic.getAllUnitTags(this).getValue("fixedrescost").ifPresent(arg -> {
			
				// If we have many, we use the first one. The order is Race, pose, filter, theme.
				// Assumedly these exist mostly in one of these anyway
				int cost = arg.getInt();
				int currentcost = getResCost(true, allCommands);
				
				cost -= currentcost;
				
				if(cost > 0)
					adjustmentcommands.add(Command.args("#rcost", "+" + cost));
				else if(cost < 0)
					adjustmentcommands.add(Command.args("#rcost", "" + cost));
			});
			
			
			
			// Add adjustments
			allCommands.addAll(adjustmentcommands);
			

		}
		




		// Now handle them!
		List<Command> multiCommands = new ArrayList<>();
		List<Command> tempCommands = new ArrayList<>();
		
		for(Command c : allCommands)
			if(c.args.size() > 0 && c.args.get(0).get().startsWith("*") && c.args.get(0).isNumeric())
				multiCommands.add(c);
			else
				handleCommand(tempCommands, c);

	
	

		//Percentual cost increases
		for(Command c : multiCommands)
		{
			handleCommand(tempCommands, c);
		}


		
		return tempCommands;
	}

	
	private boolean handleLowEncCommandPolish(Tags tags)
	{

		if(!tags.containsName("lowenctreshold"))
			return false;
		
		int treshold = tags.getValue("lowenctreshold").orElseThrow().getInt();

		int enc = 0;
		if(getSlot("armor") != null)
			enc += nationGen.armordb.GetInteger(getSlot("armor").id, "enc");
		if(getSlot("offhand") != null && getSlot("offhand").armor)
			enc += nationGen.armordb.GetInteger(getSlot("offhand").id, "enc");
		if(getSlot("helmet") != null)
			enc += nationGen.armordb.GetInteger(getSlot("helmet").id, "enc");
		
		if(enc <= treshold && tags.containsName("lowenccommand"))
		{
			Command fullCommand = tags.getCommand("lowenccommand").orElseThrow();
			String command = fullCommand.command;
			for(Command c : this.getCommands())
			{
				if(command.equals(c.command))
				{
					return false;
		
				}
			}
			this.commands.add(fullCommand);
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
				stats = c.args.get(0).getInt();
		}
		
	
		return stats;
	}
	
	
	public String guessRole()
	{
		String r = "infantry";
		for(String role : pose.roles)
			if (role.contains("ranged")) {
				r = "ranged";
				break;
			}
		for(String role : pose.roles)
			if (role.contains("mounted")) {
				r = "mounted";
				break;
			}
		return r;
	}
	
	public void polish()
	{
		if(this.polished)
			return;
		
		final Unit u = this;
	
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
		this.slotmap.slots()
			.filter(slot -> getSlot(slot) != null)
			.forEach(slot -> handleDependency(slot, true));
		
		
		// Slot removal
		this.slotmap.items()
			.flatMap(item -> item.tags.getAllStrings("noslot").stream())
			.collect(Collectors.toSet())
			.forEach(slot -> setSlot(slot, null));
		
		
		// +2hp to mounted
		if(this.getSlot("mount") != null)
		{
			this.commands.add(Command.args("#hp", "+2"));
			this.tags.addArgs("itemslot", "feet", -1);
		}
		
		// Handle custom equipment
		for(String str : this.slotmap.slots().collect(Collectors.toList())) // copy to avoid concurrent modification (sigh)
		{
			
			Item i = this.slotmap.get(str);
			if(i == null)
				continue;
			
			if(!Generic.isNumeric(i.id))
			{
				Item ti = i.getCopy();
				ti.tags.add("OLDID", i.id);
				ti.id = nationGen.GetCustomItemsHandler().getCustomItemId(i.id);
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
			
			this.commands.add(Command.args("#ambidextrous", "+" + len));

		}
		
		
		
		

		
		// Fist for things without proper weapons
		if(!pose.tags.containsName("no_free_fist") && !copystats && getClass() != ShapeChangeUnit.class)
			if(this.getSlot("weapon") == null
					|| this.getSlot("weapon").id.equals("-1")
					|| nationGen.weapondb.GetInteger(getSlot("weapon").id, "rng") != 0)
			{
				if(this.getSlot("bonusweapon") == null
						|| this.getSlot("bonusweapon").id.equals("-1")
						|| nationGen.weapondb.GetInteger(getSlot("bonusweapon").id, "rng") != 0)
				{

					this.commands.add(new Command("#weapon", Args.of(new Arg(92)), "Fist given to units that could otherwise only kick."));
		
				}
			}
		

		// Autocalc enabler
		//u.commands.add(new Command("#gcost", "+10000"));


		// #price_per_command
		for(Args args : Generic.getAllUnitTags(this).getAllArgs("price_per_command"))
		{
			int value = u.getCommandValue(args.get(0).get(), 0);
			double cost = args.get(1).getDouble();
			int threshold = 0;
			if(args.size() > 3)
				threshold = args.get(2).getInt();
			
			
			if(value > threshold)
			{
				value -= threshold;
				
				int total = (int)Math.round((double)value * cost);
				
				if(total > 0)
					commands.add(Command.args("#gcost", "+" + total));
				else
					commands.add(Command.args("#gcost", "" + total));
			}
		}
		
		// #price_if_command
		for (Args args : Generic.getAllUnitTags(this).getAllArgs("price_if_command"))
		{
			
			int value = u.getCommandValue(args.get(args.size() - 3).get(), 0);
			int target = args.get(args.size() - 2).getInt();
			String cost = args.get(args.size() - 1).get();
			
			boolean at = args.contains(new Arg("at"));
			boolean below = args.contains(new Arg("below"));
			boolean above = args.contains(new Arg("above"));
			
			if((value > target && above) || (value == target && at)  || (value < target) && below)
			{
				commands.add(Command.args("#gcost", cost));
			}
		}
		

		
		// Clean up commands
		List<Command> commands = u.getCommands();
		

		for(Command c : commands)
		{
			
			// Goldcost for holy units
			if(c.command.equals("#holy"))
			{
				this.handleCommand(commands, Command.args("#gcost", "*1.3"));
			}
			
			// Discount for slowrec units
			if(c.command.equals("#slowrec"))
			{
				this.handleCommand(commands, Command.args("#gcost", "*0.9"));
			}
			
		
			if(c.args.size() > 0 && c.args.get(0) != null && !c.args.get(0).get().equals(""))
			{
				
				
				// Mapmove is at least 1
				if(c.command.equals("#mapmove") && c.args.get(0).getInt() < 1 && !pose.tags.containsName("immobile"))
					c.args.set(0, new Arg(1));
				

		
				// Weapons
				if(c.command.equals("#weapon"))
				{	
				 
					Arg realarg = c.args.get(0);
					
					if (!realarg.isNumeric()) {
						c.args.set(0, new Arg(nationGen.GetCustomItemsHandler().getCustomItemId(realarg.get()) + ""));
					}
				}
				
			}
		}
		u.commands = commands;



		

		// Montag mean costs
		if(this.pose.tags.containsName("montagpose") && !this.pose.tags.containsName("no_montag_mean_costs"))
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
				this.handleCommand(commands, new Command("#gcost", new Arg(gold)));
				this.handleCommand(commands, new Command("#rcost", new Arg(res)));
			}
			
				
		}
		
		// %cost stuff
			
		for(Command c : commands)
			if(c.args.size() > 0 && c.args.get(0).get().startsWith("%cost") && Generic.isNumeric(c.args.get(0).get().substring(5)))
				percentCostCommands.add(c);

		int gcost = percentCostCommands.size() > 0 ? this.getGoldCost() : 0;
		
		for(Command c : percentCostCommands)
		{
			
			if(gcost == 0)
				continue;
			
			double multi = Double.parseDouble(c.args.get(0).get().substring(5)) / 100;
			
			int price = (int)Math.round((double)gcost * multi);
			Command d = Command.args(c.command, price + "");
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
				int cost = c.args.get(0).getInt();
				
				if(cost > 30)
				{
					cost = (int)Math.round((double)cost / 5) * 5;
					c.args.set(0, new Arg(cost));
				}
			}
			
			// morale 50 if over 50
			if(c.command.equals("#mor"))
			{
				int mor = c.args.get(0).getInt();
				if(mor > 50)
				{
					c.args.set(0, new Arg(50));
				}
				else if(mor <= 0)
				{
					c.args.set(0, new Arg(1));
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
		List<String> uniques = List.of("#weapon", "#custommagic", "#magicskill", "#magicboost");
		
		int copystats = -1;

		Command old = null;
		for(Command cmd : commands)
		{
			if(cmd.command.equals(c.command))
				old = cmd;
			
			if(cmd.command.equals("#copystats"))
				copystats = cmd.args.get(0).getInt();
		}
		

		// If the unit has #copystats it doesn't have defined stats. Thus we need to fetch value from database
		if(c.args.size() > 0 && (c.args.get(0).get().startsWith("+")) && copystats != -1 && old == null)
		{
			String value = this.nationGen.units.GetValue(copystats + "", c.command.substring(1));
			if(value.equals(""))
				value = "0";
			
			old = Command.args(c.command, value);
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
				Arg arg = c.args.get(i);
				Arg oldarg = old.args.get(i);
				if(arg.getOperator().isPresent())
				{
					Operator operator = arg.getOperator().get();
					if (operator == Operator.ADD || operator == Operator.SUBTRACT) {
						try {
							int value = oldarg.get().startsWith("%") ? arg.getInt() : (oldarg.getInt() + arg.getInt());
							
							old.args.set(i, new Arg(value));
						} catch (NumberFormatException e) {
							throw new IllegalArgumentException("FATAL ERROR 1: Argument parsing " + oldarg + " + " + arg + " on " + c + " caused crash.", e);
						}
					} else if (operator == Operator.MULTIPLY) {
						try {
							int value = oldarg.get().startsWith("%") ? 0 : ((int)(oldarg.getInt() * arg.getDouble()));
							
							old.args.set(i, new Arg(value));
						} catch(Exception e) {
							throw new IllegalArgumentException("FATAL ERROR 2: Argument parsing " + oldarg + " * " + arg + " on " + c.command + " caused crash.", e);
						}
					}
				}
				else
				{
					if(!uniques.contains(c.command))
					{
						old.args.set(i, arg);
					}
					else
					{
						commands.add(c.copy());
					}
				}
			}
		}
		else 
		{
			Args args = new Args();
			for(Arg arg : c.args)
			{
				args.add(arg.applyModToNothing());
			}

			commands.add(new Command(c.command, args, c.comment));
		}
	
		

	}
	

		
	/**
	 * Returns unit hp, only basesprite and race count.
	 * @return
	 */
	public int getHP()
	{
		Unit u = this;
		int hp = 0;
		for(Command c : u.race.unitcommands)
			if(c.command.equals("#hp"))
				hp += c.args.get(0).getInt();
		
		for(Command c : u.getSlot("basesprite").commands)
			if(c.command.equals("#hp"))
				hp += c.args.get(0).getInt();
		
		if(hp > 0)
			return hp;
		else
			return 10;
	}
	
	public String writeSlotLine(Item i)
	{
		Dom3DB armordb = nationGen.armordb;
		Dom3DB weapondb = nationGen.weapondb;
		
		if(i.armor)
			return "#armor " + i.id + " --- " + armordb.GetValue(i.id, "armorname") + " / " + i.name;
		else
			return "#weapon " + i.id + " --- " + weapondb.GetValue(i.id, "wpname") + " / " + i.name;
		
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
				natural += c.args.get(0).getInt();
			}
		}
	
		Dom3DB armordb = nationGen.armordb;
		
		for(String slot : slotmap.getSlots())
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
				natural += c.args.get(0).getInt();
			}
		}
	
		Dom3DB armordb = nationGen.armordb;
		
		
		for(String slot : slotmap.getSlots())
		{
			if(getSlot(slot) != null && getSlot(slot).armor && !getSlot(slot).id.equals("-1"))
			{
				eqenc += armordb.GetInteger(getSlot(slot).id, "enc", 0);
			}
		}
		



		double prot = eqenc + natural;
		
		return (int)prot;
	}
	
	
	public List<String> writeLines(String spritedir)
	{ 
		List<String> lines = new ArrayList<>();

		lines.add("--- " + getName() + " (" + race.name + "), Gold: " + getGoldCost() + ", Resources: " + getResCost(true) + ", Roles: " + pose.roles + " (" + pose.name + ")");
		lines.add("--- OFFSET DEBUG: ");
		if(this.getSlot("weapon") != null)
		{
			lines.add("-- Weapon: " + this.getSlot("weapon").getOffsetX() + ", " + this.getSlot("weapon").getOffsetY());
		}
		if(this.getSlot("armor") != null)
		{
			lines.add("-- Armor: " + this.getSlot("armor").getOffsetX() + ", " + this.getSlot("armor").getOffsetY());
		}
		if(this.getSlot("offhand") != null)
		{
			lines.add("-- Offhand: " + this.getSlot("offhand").getOffsetX() + ", " + this.getSlot("offhand").getOffsetY());
		}
		lines.add("--- Generation tags: " + this.tags);
		
		lines.add("--- Applied filters: " + this.appliedFilters.stream()
				.map(f -> f.name + (f.name.equals("MAGICPICKS") ? " (" + ((MagicFilter)f).pattern.getPrice() + ")" : ""))
				.collect(Collectors.joining(", ")));
		
		lines.add("#newmonster " + id);
		
		if(!this.name.toString(this).equals("UNNAMED"))
		{
			lines.add("#name \"" + name.toString(this) +  "\"");
		}
		
		if(this.getSlot("basesprite") != null)
		{
			lines.add("#spr1 \"" + (spritedir + "/unit_" + this.id + "_a.tga\""));
			lines.add("#spr2 \"" + (spritedir + "/unit_" + this.id + "_b.tga\""));
		}
		//lines.add("#descr \"" + desc + "\"");
		
		
		// Write all instead of just some stuff (14.3.2014)
		slotmap.items()
			.filter(i -> Integer.parseInt(i.id) > 0)
			.forEach(i -> lines.add(writeSlotLine(i)));
		
		lines.addAll(writeCommandLines());
		
		lines.add("#end");
		lines.add("");
		
		return lines;
	}
	
	public int getCommandValue(String command, int defaultv)
	{
		int value = defaultv;
		for(Command c : this.getCommands())
		{
			if(c.command.equals(command) && c.args.size() > 0)
				value = c.args.get(0).getInt();
		}
		return value;
	}
	
	public String getStringCommandValue(String command, String defaultv)
	{
		String value = defaultv;
		for(Command c : this.getCommands())
		{
			if(c.command.equals(command) && c.args.size() > 0)
				value = c.args.get(0).get();
		}
		return value;
	}
	
	
	// 20150522EA : my OOP prof back in undergrad would probably shoot me for this method...
	public void setCommandValue(String command, String newValue)
	{
		for(Command cmd : this.commands)
		{
			if(cmd.command.equals(command))
			{
				cmd.args.set(0, new Arg(newValue));
			}
		}
	}
	
	/**
	 * Calculates recruitment point cost as gcost from race+pose+basesprite
	 */
	protected Optional<String> writeRecpointsLine()
	{
		if(this.hasCommand("#rpcost") || this.hasCommand("#copystats"))
			return Optional.empty();
		
		
		return Optional.of("#rpcost " + (calculateRp() * 1000));
	}
	
	private int calculateRp() {
		int baserp = 0;
		
		List<Command> clist = new ArrayList<>();
		clist.addAll(this.race.unitcommands);
		clist.addAll(this.pose.getCommands());
		
		if(this.getSlot("basesprite") != null)
			clist.addAll(this.getSlot("basesprite").commands);
		
		for(Command c : clist)
		{
			if(c.command.equals("#gcost"))
			{
				baserp = handleModifier(c.args.get(0), baserp);
			}
		}
		
		return baserp;
	}
	
	private List<String> writeCommandLines()
	{
		
		List<String> lines = new ArrayList<>();
		
		writeBodytypeLine().ifPresent(lines::add);
		
		writeRecpointsLine().ifPresent(lines::add);

		for(Command c : this.commands)
		{
			if(c.args.size() > 0)
			{
				if(!c.command.equals("#itemslots"))
					lines.add(c.toModLine());
			}
			else
				lines.add(c.command);
		}
		
		lines.add("#itemslots " + this.getItemSlots());

		return lines;
	}
	
	public void writeSprites(String spritedir)
	{
		if(getSlot("basesprite") == null) {
			throw new IllegalStateException("Unit " + this.name + " has no basesprite and can't be rendered!");
		}
		
		FileUtil.writeTGA(this.render(0), "/mods/" + spritedir + "/unit_" + this.id + "_a.tga");

		// The super awesome attack sprite generation:
		FileUtil.writeTGA(this.render(-5), "/mods/" + spritedir + "/unit_" + this.id + "_b.tga");
	}
	
	
	public BufferedImage render()
	{
		return render(0);
	}
	
	public String getMountOffsetSlot()
	{
		return Generic.getAllUnitTags(this).getString("mount_offset_slot").orElse("mount");
	}
	
	public BufferedImage render(int offsetX)
	{
		if(this.getClass() == ShapeChangeUnit.class)
			return null;
		
		
		// Get width and height;
		Dimension d = getSpriteDimensions();
		
		BufferedImage combined = new BufferedImage(d.width, d.height, BufferedImage.TYPE_3BYTE_BGR);
		// paint both images, preserving the alpha channels
		Graphics g = combined.getGraphics();
		g.setColor(Color.black);
		g.fillRect(0, 0, d.width, d.height);
		g.translate(offsetX, 0);
		
		paint(g);
			
		// Save as new image
		return combined; 
	}
	
	private void paint(Graphics g) {
		
		String mountslot = getMountOffsetSlot();
		
		for(String s : pose.renderorder.split(" "))
		{
			
			if(s.equals(mountslot)
				|| (!pose.tags.containsName("non_mount_overlay")
				&& s.equals("overlay") && getSlot(s) != null
				&& getSlot("overlay").getOffsetX() == 0
				&& getSlot("overlay").getOffsetY() == 0))
			{
				renderSlot(g, this, s, false);
			}
			else if(s.equals("basesprite") && slotmap.get(mountslot) == null)
				renderSlot(g, this, s, false);
			else if(s.equals("offhandw") && (getSlot("offhand") != null && !getSlot("offhand").armor))
				renderSlot(g, this, "offhand", true);
			else if(s.equals("offhanda") && (getSlot("offhand") != null && getSlot("offhand").armor))
				renderSlot(g, this, "offhand", true);
			else
				renderSlot(g, this, s, true);
			
		}
		
	}
	
	private void renderSlot(Graphics g, Unit u, String slot, boolean useoffset)
	{
		
		List<Item> possibleitems = slotmap.items()
			.filter(i -> slot.equals(i.renderslot))
			.collect(Collectors.toList());
		


		
		for(int i = 10; i >= 1; i--)
		{
			for(Item item : possibleitems)
			{
				if(item.renderprio == i)
				{
					renderItem(g, item, useoffset);
					return;
				}
			}
		}
		
	}
	
	
	private void renderItem(Graphics g, Item i, boolean useoffset)
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
			
			i.render(g, true, offsetx, offsety, this.color);

		}
		else
			i.render(g, false, 0, 0, this.color);
		
	}
	
	public Dimension getSpriteDimensions()
	{
		int w = 64;
		int h = 64;
		
		String mountslot = getMountOffsetSlot();
		if(this.slotmap.get(mountslot) != null)
		{
			BufferedImage base = FileUtil.readImage(this.slotmap.get(mountslot).sprite);
			w = Math.max(w, base.getWidth());
			h = Math.max(h, base.getHeight());
		}
		else if (this.slotmap.get("basesprite") != null)
		{
			BufferedImage base = FileUtil.readImage(this.slotmap.get("basesprite").sprite);
			w = Math.max(w, base.getWidth());
			h = Math.max(h, base.getHeight());
		}
		else {
			System.out.println("No base sprite for " + pose.roles + " unit of " + race.name + ", id " + id + ".");
		}
		
		return new Dimension(w, h);
	}
}
