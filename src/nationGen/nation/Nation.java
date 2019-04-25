package nationGen.nation;


import com.elmokki.Drawing;
import com.elmokki.Generic;
import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.Settings.SettingsType;
import nationGen.entities.Filter;
import nationGen.entities.Race;
import nationGen.entities.Theme;
import nationGen.items.CustomItem;
import nationGen.magic.MageGenerator;
import nationGen.magic.MagicPath;
import nationGen.magic.SpellGen;
import nationGen.misc.*;
import nationGen.naming.Summary;
import nationGen.restrictions.NationRestriction;
import nationGen.restrictions.NationRestriction.RestrictionType;
import nationGen.rostergeneration.*;
import nationGen.units.ShapeChangeUnit;
import nationGen.units.Unit;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;


public class Nation {
	
	public int mockid = -2;
	public Color[] colors = new Color[5];
	public int nationid;
	public int era;
	public String name = "UNNAMED";
	public String epithet = "NO EPITHET";
	
	public boolean passed = true;
	public String restrictionFailed = "";
	
	public NationGen nationGen;
	private NationGenAssets assets;
	
	private final long seed;
	public final Random random;
	
	public ItemSet usedItems = new ItemSet();

	public List<Unit> heroes = new ArrayList<>();

	public List<Command> commands = new ArrayList<>();
	public List<CustomItem> usedcustomitems = new ArrayList<>();
	public List<ShapeChangeUnit> secondshapes = new ArrayList<>();
	public List<CustomItem> customitems = new ArrayList<>();
	public List<Command> gods = new ArrayList<>();
	public List<Site> sites = new ArrayList<>();
	public List<Theme> nationthemes = new ArrayList<>();
	public List<Filter> spells = new ArrayList<>();
	
	public LinkedHashMap<String, List<Unit>> unitlists = new LinkedHashMap<>();
	public LinkedHashMap<String, List<Unit>> comlists = new LinkedHashMap<>();
	
	public List<Race> races = new ArrayList<>();
	public String nationalitysuffix;
	
	public List<Filter> castles = new ArrayList<>();
	
	public Summary summary = new Summary(this);
	
	public BufferedImage flag = null;
	
	public int PDRanks = 2;

	public Nation(NationGen ngen, long seed, int tempid, List<NationRestriction> restrictions, NationGenAssets assets)
	{
		this.nationid = tempid;
		this.nationGen = ngen;
		this.random = new Random(seed);
		this.seed = seed;
		this.assets = assets;
		this.era = (int)Math.round(nationGen.settings.get(SettingsType.era));
		
		comlists.put("scouts", new ArrayList<>());
		comlists.put("commanders", new ArrayList<>());
		comlists.put("priests", new ArrayList<>());
		comlists.put("mages", new ArrayList<>());
		
		generate(restrictions);
	}
	
	public long getSeed()
	{
		return seed;
	}
	
	private void getColors()
	{
		colors[0] = Drawing.getColor(random);

		int direction = -1;
		if(random.nextDouble() < 0.5)
			direction = 1;
		
		int amount = random.nextInt(40) + 30;
		
		colors[1] = new Color(Math.min(255, Math.max(0, colors[0].getRed() + direction*amount)), Math.min(255, Math.max(0, colors[0].getGreen() + direction*amount)), Math.min(255, Math.max(0, colors[0].getBlue() + direction*amount)));
		colors[2] = Drawing.getColor(random);
		colors[3] = Drawing.getColor(random);
		colors[4] = Drawing.getColor(random);
	}
	
	private void getRaces()
	{
		ChanceIncHandler chandler = new ChanceIncHandler(this);

		// choose primary race
		List<Race> allRaces = new ArrayList<>();

		for(Race r : assets.races)
		{
			if (!r.tags.containsName("secondary"))
			{
				allRaces.add(r);
			}
		}
		Race race = chandler.getRandom(allRaces);
				
		races.add(race.getCopy());
		

		// Add themes
		addRaceThemes(races.get(0));
	
		// Handle possible newly added racial commands
		for(Command c : races.get(0).nationcommands)
		{
			this.handleCommand(commands, c);
		}

		// Add nation themes
		addNationThemes();
		
		// Secondary race after themes since themes may affect it
		allRaces.clear();
		allRaces.addAll(assets.races);
		allRaces.remove(race);
		
		race = chandler.handleChanceIncs(allRaces).getRandom(random);
		races.add(race.getCopy());
		
		// Add themes for secondary race
		addRaceThemes(races.get(1));
		
		// Apply the nation and primary race theme secondary race effects
		for(Theme t : races.get(0).themefilters)
		{
			for(Command str : t.secondarynationeffects)
			{
				races.get(1).addOwnLine(str);
			}
			for(Command str2 : t.bothnationeffects)
			{
				races.get(1).addOwnLine(str2);
			}
		}
		for(Theme t : this.nationthemes)
		{
			for(Command str : t.secondarynationeffects)
			{
				races.get(1).addOwnLine(str);
			}
			for(Command str2 : t.bothnationeffects)
			{
				races.get(1).addOwnLine(str2);
			}
		}
		
		
		
		// Add secondaryracecommands to the secondary race
		for(Command command : this.races.get(0).tags.getAllCommands("secondaryracecommand"))
		{
			races.get(1).addCommand(command);
		}

	}
	
	
	private void generateMagesAndPriests()
	{
		// Mages and priests
		MageGenerator mageGen = new MageGenerator(nationGen, this, assets);
		comlists.get("mages").addAll(mageGen.generateMages());
		comlists.get("priests").addAll(mageGen.generatePriests());
	}
	
	private void generateTroops()
	{
		// Troops
		RosterGenerator g = new RosterGenerator(nationGen, this, assets);
		g.execute();
		g = null;
		System.gc();
		
	}
	
	private void generateSacreds()
	{
		//// Sacreds and elites
		SacredGenerator sacGen = new SacredGenerator(nationGen, this, assets);
		List<Unit> sacreds = new ArrayList<Unit>();
		

		// Elite
		int sacredcount = 1;
		if(random.nextDouble() > 0.8) 
		{
			int power = 0; 
			if(random.nextDouble() > 0.25)
				power++;
			if(random.nextDouble() > 0.25)
				power++;
			if(random.nextDouble() > 0.5)
				power++;
			if(random.nextDouble() > 0.75)
				power++;
			if(random.nextDouble() > 0.9)
				power++;
			
			Unit sacred = sacGen.generateUnit(false, power);
			List<Unit> elites = new ArrayList<Unit>();
			elites.add(sacred);
			
			String role = "";
			if(sacred.pose.roles.contains("infantry") || sacred.pose.roles.contains("elite infantry") || sacred.pose.roles.contains("sacred infantry"))
				role = "infantry";
			else if(sacred.pose.roles.contains("mounted")  || sacred.pose.roles.contains("elite mounted") || sacred.pose.roles.contains("sacred mounted"))
				role = "mounted";
			else if(sacred.pose.roles.contains("ranged")  || sacred.pose.roles.contains("elite ranged") || sacred.pose.roles.contains("sacred ranged"))
				role = "ranged";
			else if(sacred.pose.roles.contains("chariot")  || sacred.pose.roles.contains("elite chariot") || sacred.pose.roles.contains("sacred chariot"))
				role = "chariot";
			

			if(role.equals(""))
				System.out.println(sacred.race + " " + sacred.pose.roles + " AS ELITE UNIT HAS RECEIVED NO ROLES!");
			
			if(sacred != null)
			{
				if(unitlists.get(role) != null)
					System.out.print(role + " - " + unitlists.get(role).size() + " -> ");
				unitlists.put(role, elites);
			}
			
		}
		else if(random.nextDouble() > 0.9)
		{
			sacredcount++;
		}
		
		// Sacred
		for(int i = 0; i < sacredcount; i++)
		{
		
			int power = 1; 
			

			
			if(random.nextDouble() < 0.9)
			{			
				power++;
				if(random.nextDouble() < 0.8)
				{
					power++;
					if(random.nextDouble() < 0.7)
					{
						power++;
						if(random.nextDouble() < 0.5)
						{
							power++;
							if(random.nextDouble() < 0.4)
							{
								power++;
								if(random.nextDouble() < 0.3)
								{
									power++;
								}
								if(random.nextDouble() < 0.3)
								{
									power++;
								}
							}
						}
					}
				}
			}
	
	
			Unit sacred = sacGen.generateUnit(true, power);
			if(sacred != null)
			{
				sacreds.add(sacred);
			}
		}
		if(sacreds.size() > 0)
			unitlists.put("sacreds", sacreds);
		sacGen = null;
		System.gc();
		
	}
	
	private void generateScouts()
	{
		// Scouts
		
		ScoutGenerator scoutgen = new ScoutGenerator(nationGen, this, assets);
		
		if(races.get(0).hasRole("scout") && !races.get(0).tags.containsName("#no_scouts"))
			comlists.get("scouts").add(scoutgen.generateScout(races.get(0)));
		else if(races.get(1).hasRole("scout") && !races.get(1).tags.containsName("#no_scouts"))
			comlists.get("scouts").add(scoutgen.generateScout(races.get(1)));
		scoutgen = null;
		System.gc();
	}
	
	private void generateSpecialComs()
	{

		// Special commanders

		double specialcomchance = races.get(0).tags.getDouble("specialcommanderchance").orElse(0.05);
		
		if(random.nextDouble() < specialcomchance)
		{
			SpecialCommanderGenerator scg = new SpecialCommanderGenerator(this, nationGen, assets);
			scg.generate();
			scg = null;
			System.gc();
		}
	}
	
	private void generateGods()
	{
		// Gods
		GodGen gg = new GodGen(this, assets);
		this.gods.addAll(gg.giveGods());
		gg = null;
	}
	
	private void getForts()
	{
		ChanceIncHandler chandler = new ChanceIncHandler(this);

		// Forts
		List<Filter> possibleForts = ChanceIncHandler.retrieveFilters("forts", "default_forts", assets.miscdef, null, races.get(0));
		possibleForts = ChanceIncHandler.getFiltersWithType("era " + era, possibleForts);
		Filter forts = chandler.getRandom(possibleForts);
		this.commands.addAll(forts.commands);
	}
	
	private void generateHeroes()
	{
		// Heroes
		
		HeroGenerator hg = new HeroGenerator(nationGen, this, assets);
		int heroes = 1 + random.nextInt(3); // 1-3
		this.heroes.addAll(hg.generateHeroes(heroes));
		hg = null;
		System.gc();
	}
	
	
	private void generateComs()
	{
		// Commanders
		CommanderGenerator comgen = new CommanderGenerator(nationGen, this, assets);
		comgen.generateComs();
		comgen = null;
		System.gc();
	}

	private void generateMonsters()
	{
		
		// Monsters
		double monsterchance = races.get(0).tags.getDouble("monsterchance").orElse(0.05);
		
		if(random.nextDouble() < monsterchance)
		{
			MonsterGenerator mGen = new MonsterGenerator(this, this.nationGen, assets);
			Unit monster = mGen.generateMonster();
			if(monster != null)
			{
				this.unitlists.put("monsters", new ArrayList<>());
				this.unitlists.get("monsters").add(monster);
			}
		}
		System.gc();
	}
	
	private void generateSpells()
	{
		// Spells
		SpellGen spellgenerator = new SpellGen(this);
		spellgenerator.execute(assets);
	}
	
	private void generateFlag()
	{
		// Flag
		FlagGen fg = new FlagGen(this, assets);
		this.flag = fg.generateFlag(this);
		
	}
	
	private void getStartAffinity()
	{
		ChanceIncHandler chandler = new ChanceIncHandler(this);

		// Start affinity
		int cycles = 2;
		
		List<Filter> posaff = ChanceIncHandler.retrieveFilters("startaffinities", "startaffinities", assets.miscdef, null, races.get(0));

		
		Filter startaff = null;
		while(cycles > 0)
		{
			startaff = chandler.handleChanceIncs(posaff).getRandom(random);
			
			if(startaff.getCommands().size() > 0)
			{
				for(Command c : startaff.getCommands())
				{
					handleCommand(this.commands, c, nationGen);
				}
				posaff.remove(startaff);
			}
		
			cycles--;
		}
		posaff = null;
		System.gc();
	}
	
	public static void handleCommand(List<Command> commands, Command c, NationGen nationGen)
	{
		
		// List of commands that may appear more than once per unit
		List<String> uniques = new ArrayList<>();
		uniques.add("#weapon");
		uniques.add("#custommagic");
		uniques.add("#magicskill");
		
		
		int copystats = -1;
		
		Command old = null;
		for(Command cmd : commands)
		{
			if(cmd.command.equals(c.command))
				old = cmd;
			
			if(cmd.command.equals("#copystats"))
				copystats = cmd.args.get(0).getInt();
		}
		
		
		if(c.args.size() > 0
				&& c.args.get(0).getOperator().isPresent()
				&& copystats != -1
				&& old == null)
		{
			String value = nationGen.units.GetValue(copystats + "", c.command.substring(1));
			if(!value.equals(""))
			{
				old = new Command(c.command, new Arg(value));
				commands.add(old);
			}
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
				Optional<Operator> operator = arg.getOperator();
				
				if (operator.isPresent()) {
					try {
						old.args.set(i, new Arg(arg.applyModTo(oldarg.getInt())));
					} catch (NumberFormatException e) {
						throw new IllegalStateException("FATAL ERROR: Argument parsing " + oldarg + " " + arg + " on " + c.command + " caused crash.", e);
					}
				} else {
					old.args.set(i, arg);
				}
			}
		}
		else
		{
			Args newArgs = new Args();
			for(Arg arg : c.args)
			{
				newArgs.add(arg.applyModToNothing());
			}
			commands.add(new Command(c.command, newArgs, c.comment));
		}
	}
	
	
	public void generate(List<NationRestriction> restrictions)
	{
		getColors();
		getRaces();

		if (!checkRestrictions(restrictions, RestrictionType.NoPrimaryRace, RestrictionType.PrimaryRace,
				RestrictionType.NationTheme))
		{
			return;
		}

		generateMagesAndPriests();

		if (!checkRestrictions(restrictions, RestrictionType.MageWithAccess, RestrictionType.MagicAccess,
				RestrictionType.MagicDiversity, RestrictionType.PrimaryRace))
		{
			return;
		}

		generateTroops();
		generateSacreds();

		if (!checkRestrictions(restrictions, RestrictionType.SacredRace, RestrictionType.RecAnywhereSacreds))
		{
			return;
		}

		generateScouts();
		generateSpecialComs();
		generateGods();
		getForts();
		generateHeroes();
		applyNationWideFilter();
		generateComs();

		if (!checkRestrictions(restrictions, RestrictionType.UnitCommand, RestrictionType.UnitFilter,
				RestrictionType.UnitRace, RestrictionType.NoUnitOfRace))
		{
			return;
		}

		generateMonsters();
		SiteGenerator.generateSites(this, assets);
		generateSpells();
		generateFlag();
		getStartAffinity();

		double extraPDMulti = this.races.get(0).tags.getDouble("extrapdmulti").orElse(1D);
		
		if (random.nextDouble() < 0.1 * extraPDMulti)
		{
			if (random.nextDouble() < 0.02 * extraPDMulti)
				PDRanks = 4;
			else
				PDRanks = 3;
		}

		// finalizeUnits();
	}
	
	

	private void addNationThemes()
	{


		Race race = this.races.get(0);
		
		List<Theme> possibleThemes = ChanceIncHandler.retrieveFilters("nationthemes", "default_nationthemes", assets.themes, null, race);
		ChanceIncHandler chandler = new ChanceIncHandler(this);
		
		for(String str : race.tags.getAllStrings("freenationtheme"))
		{
			List<Theme> frees = ChanceIncHandler.getFiltersWithType(str, possibleThemes);
			
			if(frees.isEmpty()) {
				throw new IllegalStateException(race + " has #freenationtheme " + str + " but no filters of #type " + str);
			}
			
			Theme t = chandler.handleChanceIncs(frees).getRandom(random);
			
			possibleThemes.remove(t);
			race.handleTheme(t);
			for(Command c : t.commands)
				this.handleCommand(commands, c);
			this.nationthemes.add(t);
			
		}
		
		
		int guaranteedthemes = race.tags.getInt("guaranteednationthemes").orElse(1);
		
		if(random.nextDouble() > 0.7)
		{	
			guaranteedthemes++;
			if(random.nextDouble() > 0.9)
			{	
				guaranteedthemes++;
			}
		}
		
		// Guaranteed themes
		for(int i = 0; i < guaranteedthemes; i++)
		{
	
			possibleThemes = ChanceIncHandler.getValidFilters(possibleThemes, this.nationthemes);
			if(possibleThemes.size() > 0)
			{
				Theme t = chandler.handleChanceIncs(possibleThemes).getRandom(random);
				if(t != null)
				{
					race.handleTheme(t);
					possibleThemes.remove(t);
					for(Command c : t.commands)
					{
						this.handleCommand(commands, c);
					}
					this.nationthemes.add(t);
				}
			}
		}	
		
	}
	
	public List<String> getSpells()
	{
		return this.spells.stream()
				.flatMap(f -> f.tags.getAllStrings("spell").stream())
				.distinct()
				.collect(Collectors.toList());
	}
	
	private void addRaceThemes(Race race)
	{
		
		
		List<Theme> possibleThemes = ChanceIncHandler.retrieveFilters("racethemes", "default_racethemes", assets.themes, null, race);
		ChanceIncHandler chandler = new ChanceIncHandler(this);
		
		for(String str : race.tags.getAllStrings("freetheme"))
		{
			List<Theme> frees = ChanceIncHandler.getFiltersWithType(str, possibleThemes);
			
			if (frees.isEmpty()) {
				throw new IllegalStateException(race + " has #freetheme " + str + " but no filters of #type " + str);
			}
			
			Theme t = chandler.handleChanceIncs(frees).getRandom(random);
			
			possibleThemes.remove(t);
			race.themefilters.add(t);
			race.handleTheme(t);
		}
		
		
		
		int guaranteedthemes = race.tags.getInt("guaranteedthemes").orElse(1);
		
		// TODO: This is unused
		boolean getsNonFreeThemes = race == races.get(0) || race.tags.containsName("normal_themes_as_secondary");
		
		// Guaranteed themes
		for(int i = 0; i < guaranteedthemes; i++)
		{
	
			possibleThemes = ChanceIncHandler.getValidFilters(possibleThemes, race.themefilters);
			if(!possibleThemes.isEmpty())
			{
				Theme t = chandler.handleChanceIncs(possibleThemes).getRandom(random);
				race.themefilters.add(t);
				race.handleTheme(t);
				possibleThemes.remove(t);
			}
		}
		
		// 10% chance for second theme
		possibleThemes = ChanceIncHandler.getValidFilters(possibleThemes, race.themefilters);
		if(!possibleThemes.isEmpty() && random.nextDouble() < 0.1)
		{
			Theme t = chandler.handleChanceIncs(possibleThemes).getRandom(random);
			race.themefilters.add(t);
			race.handleTheme(t);
		}
		
		
	}
	
	public void finalizeUnits()
	{
		List<Command> conditional =
			this.races.get(0).tags.getAllValues("secondaryracecommand_conditional").stream()
				.map(Arg::getCommand)
				.collect(Collectors.toList());
		
		
		for(List<Unit> l : this.comlists.values())
			for(Unit u : l)
			{
				u.commands.addAll(u.race.specialcommands);
				giveSecondaryRaceSpecialCommands(u, conditional);
				
				int priest = u.getMagicPicks().get(MagicPath.HOLY);
				if(priest > 0)
				{
					Generic.getAllNationTags(this).getAllValues("priestextracost").stream()
						.map(Arg::getInt)
						.max(Integer::compareTo)
						.ifPresent(highest -> u.commands.add(new Command("#gcost", new Arg("+" + (highest * priest)))));
				}
				
				u.polish();
			}
		for(List<Unit> l : this.unitlists.values())
			for(Unit u : l)
			{
				// TODO: is this supposed to check pose roles instead of tags??
				if(u.tags.containsName("elite") || u.tags.containsName("sacred"))
					u.commands.addAll(u.race.specialcommands);
				giveSecondaryRaceSpecialCommands(u, conditional);
				u.polish();
			}
		
		for(Unit u : heroes)
		{
			u.commands.addAll(u.race.specialcommands);
			giveSecondaryRaceSpecialCommands(u, conditional);
			u.polish();
		}
		

	}

	
	private void applyNationWideFilter()
	{
		// Nation wide filters
		int count = 0;
		if(random.nextDouble() < 0.33)
		{
			count++;
			if(random.nextDouble() < 0.33)
				count++;
		}
		
		List<Filter> possibles = ChanceIncHandler.retrieveFilters("nationwidefilters", "default_nationwidefilters", assets.filters, null, races.get(0));

		ChanceIncHandler chandler = new ChanceIncHandler(this);
		boolean primary = true;
		boolean both = false;
		
		if(random.nextDouble() < this.percentageOfRace(races.get(1)))
		{
			possibles.retainAll(ChanceIncHandler.retrieveFilters("nationwidefilters", "default_nationwidefilters", assets.filters, null, races.get(1)));
			if(possibles.size() > 0 && random.nextDouble() < 0.1)
			{
				both = true;
			}
			else
			{
				primary = false;
				possibles = ChanceIncHandler.retrieveFilters("nationwidefilters", "default_nationwidefilters", assets.filters, null, races.get(1));
			}
		}
		

		int fails = 0;
		for(int i = 0; i < count; i++)
		{
			if(possibles.size() == 0)
				break;
			
			int added = 0;
			Filter f = chandler.handleChanceIncs(possibles).getRandom(random);
			
			
			for(List<Unit> l : this.comlists.values())
				for(Unit u : l)
				{
					if(((u.race == races.get(0)) == primary || both) && ChanceIncHandler.canAdd(u, f) && !f.tags.containsName("trooponly"))
					{
						added++;
						u.appliedFilters.add(f);
					}
						
				}
			for(List<Unit> l : this.unitlists.values())
				for(Unit u : l)
				{
					if(((u.race == races.get(0)) == primary || both) && ChanceIncHandler.canAdd(u, f) && !f.tags.containsName("commanderonly"))
					{
						added++;
						u.appliedFilters.add(f);
					}
				}
			

			for(Unit u : heroes)
			{
				if(((u.race == races.get(0)) == primary || both) && ChanceIncHandler.canAdd(u, f) && !f.tags.containsName("trooponly"))
				{
					added++;
					u.appliedFilters.add(f);
				}
			}


			
			if(added == 0 && fails < 5)
			{
				fails++;
				i--;
			}

			
			chandler.removeRelated(f, possibles);
			possibles.remove(f);
			
		
		}

	}
	
	private void giveSecondaryRaceSpecialCommands(Unit u, List<Command> conditional)
	{
		if(u.race == races.get(0))
			return;
		
		for(Command cc : conditional)
		{
			boolean ok = true;
			for(Command c : u.race.getCommands())
			{
				if(c.command.equals(cc.command))
					ok = false;
			}
			for(Command c : u.pose.getCommands())
			{
				if(c.command.equals(cc.command))
					ok = false;
			}
			if(ok)
			{
				u.commands.add(cc);
			}
		}

	}
	
	public List<List<Unit>> getMagesInSeparateLists()
	{
		List<Unit> all = this.generateComList("mages");
		List<List<Unit>> lists = new ArrayList<List<Unit>>();
		
		for(int i = 3; i >= 1; i--)
		{
			List<Unit> l = new ArrayList<Unit>();
			for(Unit u : all)
			{
			
				if(u.tags.contains("schoolmage", i))
					l.add(u);
			}
			lists.add(l);
		}
		
		List<Unit> l = new ArrayList<Unit>();
		for(Unit u : all)
			if(u.tags.containsName("extramage"))
				l.add(u);
		
		lists.add(l);
	
		return lists;
	}
	
	/**
	 * Writes the nation
	 */
	public List<String> writeLines(String spritedir)
	{

		Color c = colors[0];

		double r = (double)(Math.round((double)c.getRed()/255*10)) / 10;
		double b = (double)(Math.round((double)c.getBlue()/255*10)) / 10;
		double g = (double)(Math.round((double)c.getGreen()/255*10)) / 10;
	
		List<String> lines = new ArrayList<>();
		lines.add("");
		lines.add("-- Nation " + nationid + ": " + this.name + ", " + this.epithet);
		lines.add("---------------------------------------------------------------");
		lines.add("-- Generated with themes: " + this.nationthemes);
		lines.add("-- Generated with " + races.get(0) + " race themes: " + races.get(0).themefilters);
		lines.add("-- Generated with " + races.get(1) + " race themes: " + races.get(1).themefilters);
		lines.add("---------------------------------------------------------------");

		//writeNationInfo(tw);
		lines.add("#selectnation " + nationid);
		lines.add("#clear");
		lines.add("#era " + era);
		lines.add("#name \"" + name + "\"");
		lines.add("#epithet \"" + epithet + "\"");
	
	
		lines.add("#descr \"" + "A glorious NationGen nation! Generated from seed " + seed + " with settings integer " + 
				   nationGen.settings.getSettingInteger() + "\"");
	
		lines.add("#summary \"" + summary + "\"");
		lines.add("#brief \"No description\"");
		lines.add("#color " + (r) + " " + (b) + " " + (g)); 
		
		//Generic.generateFlag(this, spritedir + "/flag.tga");
		
		
		lines.add("#flag \"" + spritedir + "/flag.tga\"");
		lines.add("");
		// Sites
		lines.add("#clearsites");
 
	   
		for(Site site : this.sites)
		{
			lines.add("#startsite \"" + site.name +  "\"");
		}
		lines.add("");
	   
		
		// Add recruitables
		lines.add("#clearrec");
		
		lines.addAll(writeRecLines(false, unitlists));
		lines.addAll(writeRecLines(true, comlists));

		
		// PD
		PDSelector pds = new PDSelector(this, nationGen);
		
		
		lines.add("");
		lines.add("#defcom1 " + pds.getIDforPD(pds.getPDCommander(1)));
		lines.add("#defunit1 " + pds.getIDforPD(pds.getMilitia(1, 1)));
		lines.add("#defmult1 " + pds.getMilitiaAmount(pds.getMilitia(1, 1)));
		lines.add("#defunit1b " + pds.getIDforPD(pds.getMilitia(2, 1)));
		lines.add("#defmult1b " + pds.getMilitiaAmount(pds.getMilitia(2, 1)));
		if(PDRanks > 2)
		{
			lines.add("#defunit1c " + pds.getIDforPD(pds.getMilitia(3, 1)));
			lines.add("#defmult1c " + pds.getMilitiaAmount(pds.getMilitia(3, 1)));
			
			if(PDRanks > 3)
			{
				lines.add("#defunit1d " + pds.getIDforPD(pds.getMilitia(4, 1)));
				lines.add("#defmult1d " + pds.getMilitiaAmount(pds.getMilitia(4, 1)));
			}
		}
		lines.add("#defcom2 " + pds.getIDforPD(pds.getPDCommander(2)));
		lines.add("#defunit2 " + pds.getIDforPD(pds.getMilitia(1, 2)));
		lines.add("#defmult2 " + pds.getMilitiaAmount(pds.getMilitia(1, 2)));
		lines.add("#defunit2b " + pds.getIDforPD(pds.getMilitia(2, 2)));
		lines.add("#defmult2b " + pds.getMilitiaAmount(pds.getMilitia(2, 2)));
		lines.add("");
	
		// Wall units
		
		lines.add("#wallcom " + pds.getIDforPD(pds.getPDCommander(1)));
		lines.add("#wallunit " + pds.getIDforPD(pds.getWallUnit(true)));
		lines.add("#wallmult " + pds.getMilitiaAmount(pds.getWallUnit(true)));

		
		// Start army
		lines.add("#startcom " + pds.getStartArmyCommander().id);
		if(comlists.get("scouts").size() > 0)
			lines.add("#startscout " + comlists.get("scouts").get(0).id);
		else
			lines.add("#startscout " + pds.getPDCommander(2).id);
		
		
		lines.add("#startunittype1 " + pds.getMilitia(1, 1, false).id);
		lines.add("#startunittype2 " + pds.getMilitia(1, 2, false).id);
		int amount1 = pds.getStartArmyAmount(pds.getMilitia(1, 1, false));
		lines.add("#startunitnbrs1 " + amount1);
		int amount2 = pds.getStartArmyAmount(pds.getMilitia(1, 2, false));
		lines.add("#startunitnbrs2 " + amount2);
		lines.add("");
		
		// Heroes
		for(int i = 0; i < heroes.size(); i++)
		{
			lines.add("#hero" + (i+1) + " " + heroes.get(i).id);
		}
		
		
		lines.add("");

		// Custom coms
		
		for(Command cmd : this.getCommands())
		{
			lines.add(cmd.toModLine());
		}
		
		
		lines.add("");
		
		// Gods
		for(Command cmd : this.gods)
			lines.add(cmd.toModLine());
		
		
		
		lines.add("");
		
		
		lines.add("#templepic " + random.nextInt(19));
		lines.add("");
		
		
		lines.add("#end");
		lines.add("");
		
		lines.add("");
		

		return lines;
	}
	
	
	public List<Unit> combineTroopsToList(String role)
	{
		List<Unit> unitlist = new ArrayList<>();
		
		for(Entry<String, List<Unit>> entry  : unitlists.entrySet())
		{
			if(entry.getKey().startsWith(role))
			{
				unitlist.addAll(entry.getValue());
			}
		}
		return unitlist;
	}
	
	public List<ShapeChangeUnit> getShapeChangeUnits() {
		List<Unit> units = new ArrayList<>();
		for(List<Unit> list : this.comlists.values())
			units.addAll(list);
		for(List<Unit> list : this.unitlists.values())
			units.addAll(list);
		units.addAll(heroes);
		
		List<ShapeChangeUnit> sus = new ArrayList<>();
		for(ShapeChangeUnit su : nationGen.forms)
		{
			if(units.contains(su.otherForm))
				sus.add(su);
		}
		
		return sus;
	}
	
	public void writeSprites(String spritedir)
	{
		for(ShapeChangeUnit su : getShapeChangeUnits())
		{
			su.writeSprites(spritedir);
		}
		
		for(List<Unit> list : unitlists.values()) {
			for (Unit u : list) {
				if (u.color == Color.white) {
					u.color = colors[0];
				}
				
				if(!u.invariantMonster) {
					u.writeSprites(spritedir);
				}
			}
		}
		for(List<Unit> list : comlists.values()) {
			for (Unit u : list) {
				if (u.color == Color.white) {
					u.color = colors[0];
				}
				
				u.writeSprites(spritedir);
			}
		}
		for(Unit u : heroes)
		{
			u.writeSprites(spritedir);
		}
	}
	
	public List<String> writeUnitsLines(String spritedir)
	{
		List<String> lines = new ArrayList<>();
		lines.add("");
		lines.add("--- Unit definitions for " + this.name);
		lines.add("");
		
		for(ShapeChangeUnit su : getShapeChangeUnits())
		{
			lines.addAll(su.writeLines(spritedir));
		}
   
		for(List<Unit> list : unitlists.values())
		{
			for(Unit u : list)
			{
				if(!u.invariantMonster)
				{
					lines.addAll(u.writeLines(spritedir));
				}
				else  // The monster uses in-game graphics and stats; just print out name, ID, and costs
				{
					lines.add("--- " + u.name + " (Unit ID " + u.id + "), Gold: " + u.getGoldCost() + ", Resources: " + u.getResCost(true) + "\n");
				}
			}
		}
		
		for(List<Unit> list : comlists.values())
		{
			for(Unit u : list)
			{
				lines.addAll(u.writeLines(spritedir));
			}
		}
		
		for(Unit u : heroes)
		{
			lines.addAll(u.writeLines(spritedir));
		}
		
		return lines;
	}
	

	private List<String> writeRecLines(boolean coms, LinkedHashMap<String, List<Unit>> unitlists)
	{
		List<String> order = Generic.parseArgs("ranged infantry mounted chariot special sacred monsters");
		if(coms)
			order = Generic.parseArgs("scouts commanders specialcoms priests mages");
		

		
		String line = "#addreccom";
		if(!coms)
			line = "#addrecunit";
		
		List<String> foreigntags = new ArrayList<>();
		foreigntags.add("forestrec");
		foreigntags.add("mountainrec");
		foreigntags.add("swamprec");
		foreigntags.add("wasterec");
		foreigntags.add("caverec");
		
		List<String> lines = new ArrayList<>();

		List<String> listnames = new ArrayList<>(unitlists.keySet());
		for(String str : order)
		{
			for(int i = 1; i <= 10; i++)
			{
				if(listnames.contains(str + "-" + i))
				{
					for(Unit u : unitlists.get(str + "-" + i))
					{
						for(String tag : foreigntags)
							if(u.tags.containsName(tag))
							{
								if(coms)
								{
									lines.add("#" + tag.substring(0, tag.length() - 3) + "com " + u.id);
								}
								else
									lines.add("#" + tag + " " + u.id);
							}
						
						if(!u.caponly)
							lines.add(line + " " + u.id);
					}
					
					listnames.remove((str + "-" + i));
				}
			}
			
			for(String listname : listnames)
			{
				if(listname.startsWith(str))
					for(Unit u : unitlists.get(listname))
					{
						for(String tag : foreigntags)
							if(u.tags.containsName(tag))
							{
								if(coms)
									lines.add("#" + tag.substring(0, tag.length() - 3) + "com " + u.id);
								else
									lines.add("#" + tag+ " " + u.id);
							}
						
						if(!u.caponly)
							lines.add(line + " " + u.id);
					}
			}
			
		}
		return lines;
	}
	
	public List<Command> getCommands()
	{
		List<Command> coms  = new ArrayList<>();
		for(Command c : this.commands)
		{
			this.handleCommand(coms, c);
		}
		return coms;
	}
	
	private void handleCommand(List<Command> commands, Command c)
	{
		// List of commands that may appear more than once per nation
		List<String> uniques = new ArrayList<>();
		
		Command old = null;
		for(Command cmd : commands)
		{
			if(cmd.command.equals(c.command))
				old = cmd;
		}
		
		if(old != null)
		{
			
			for(int i = 0; i < c.args.size(); i++)
			{
				Arg arg = c.args.get(i);
				Arg oldarg = old.args.get(i);
				
				if(arg.getOperator().filter(o -> o == Operator.ADD).isPresent())
				{
					if(!uniques.contains(c.command))
					{
						int argint = oldarg.getInt() + arg.getInt();
						if(c.command.equals("#idealcold"))
						{
							argint = Math.min(3, argint);
							argint = Math.max(-3, argint);
						}
						
						old.args.set(i, new Arg(argint));
						return;
					}
					else
					{
						commands.add(c);
						return;
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
						return;
					}
				}
				
			}
		}	
		else // there is no existing copy
		{
			Command toAdd = c.copy();
			if(toAdd.args.size() > 0)
			{
				// If the command starts with +, remove +.
				toAdd.args.set(0, toAdd.args.get(0).applyModToNothing());
			}
			commands.add(toAdd);
		}

	}
	
	public List<Unit> generateTroopList()
	{
		List<Unit> units = new ArrayList<>();
		
		for (List<Unit> units1 : unitlists.values())
			units.addAll(units1);

		return units;
	}

	public List<Unit> generateUnitList()
	{
		List<Unit> units = new ArrayList<>();
		
		for(List<Unit> units1 : comlists.values())
			units.addAll(units1);
		
		for(List<Unit> units1 : unitlists.values())
			units.addAll(units1);

		return units;
	}
	
	public List<Unit> generateComList()
	{
		List<Unit> units = new ArrayList<>();
		
		for (List<Unit> units1 : comlists.values())
			units.addAll(units1);

		return units;
	}
	public List<Unit> generateUnitList(String type)
	{
		List<Unit> list = new ArrayList<>();
		for (Entry<String, List<Unit>> entry : unitlists.entrySet()) {
			if (entry.getKey().startsWith(type)) {
				list.addAll(entry.getValue());
			}
		}
		return list;
	}
	
	public List<Unit> generateComList(String type)
	{
		List<Unit> list = new ArrayList<>();
		for (Entry<String, List<Unit>> entry : comlists.entrySet()) {
			if (entry.getKey().startsWith(type)) {
				list.addAll(entry.getValue());
			}
		}
		return list;
	}
	
	
	public List<List<Unit>> getListsOfType(String type, boolean coms, boolean troops)
	{
		List<List<Unit>> lists = new ArrayList<>();
		
		if(troops)
		{
			for (Entry<String, List<Unit>> entry : unitlists.entrySet()) {
				if (entry.getKey().startsWith(type)) {
					lists.add(entry.getValue());
				}
			}
		}
		
		if(coms)
		{
			for (Entry<String, List<Unit>> entry : comlists.entrySet()) {
				if (entry.getKey().startsWith(type)) {
					lists.add(entry.getValue());
				}
			}
		}
		return lists;
	}

	public double percentageOfRace(Race race)
	{
		List<Unit> units = this.generateTroopList();
		for(List<Unit> ul : this.comlists.values())
				units.addAll(ul);
		
		int all = units.size();
		int other = 0;
		for(Unit u : units)
			if(u.race == race)
				other++;
		
		return (double)other / (double)all;
			
	}
	
	public List<String> writeSitesLines()
	{
		List<String> lines = new ArrayList<>();
		lines.add("--- Sites for nation " + nationid + ": " + name);
		for(Site site : sites)
			lines.addAll(site.writeLines());
		return lines;
	}
	
	public boolean checkRestrictions(List<NationRestriction> restrictions, RestrictionType... restrictionTypes)
	{
		for(var restriction: restrictions)
		{
			if (Set.of(restrictionTypes).contains(restriction.getType()) && !restriction.doesThisPass(this)) 
			{
				passed = false;
				restrictionFailed = restriction.toString().toUpperCase();
				return false;
			}
		}
		return true;
	}
}
