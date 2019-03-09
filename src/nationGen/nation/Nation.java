package nationGen.nation;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import com.elmokki.Drawing;
import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.Settings.SettingsType;
import nationGen.entities.Entity;
import nationGen.entities.Filter;
import nationGen.entities.Race;
import nationGen.entities.Theme;
import nationGen.items.CustomItem;
import nationGen.magic.MageGenerator;
import nationGen.magic.SpellGen;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.misc.FlagGen;
import nationGen.misc.GodGen;
import nationGen.misc.ItemSet;
import nationGen.misc.Site;
import nationGen.misc.SiteGenerator;
import nationGen.naming.Summary;
import nationGen.restrictions.NationRestriction;
import nationGen.restrictions.NationRestriction.RestrictionType;
import nationGen.rostergeneration.CommanderGenerator;
import nationGen.rostergeneration.HeroGenerator;
import nationGen.rostergeneration.MonsterGenerator;
import nationGen.rostergeneration.RosterGenerator;
import nationGen.rostergeneration.SacredGenerator;
import nationGen.rostergeneration.ScoutGenerator;
import nationGen.rostergeneration.SpecialCommanderGenerator;
import nationGen.units.ShapeChangeUnit;
import nationGen.units.Unit;


public class Nation {
	
	public int mockid = -2;
	public Color[] colors = new Color[5];
	public int nationid;
	public int era = 2;
	public String name = "UNNAMED";
	public String epithet = "NO EPITHET";
	
	public boolean passed = true;
	public String restrictionFailed = "";
	
	public NationGen nationGen;
	private NationGenAssets assets;
	
	private long seed = 0;
	
	public ItemSet usedItems = new ItemSet();
	public Random random;

	public List<Unit> heroes = new ArrayList<Unit>();

	public List<Command> commands = new ArrayList<Command>();
	public List<CustomItem> usedcustomitems = new ArrayList<CustomItem>();
	public List<ShapeChangeUnit> secondshapes = new ArrayList<ShapeChangeUnit>();
	public List<CustomItem> customitems = new ArrayList<CustomItem>();
	public List<Command> gods = new ArrayList<Command>();
	public List<Site> sites = new ArrayList<Site>();
	public List<Theme> nationthemes = new ArrayList<Theme>();
	public List<Filter> spells = new ArrayList<Filter>();
	
	public LinkedHashMap <String, List<Unit>> unitlists = new LinkedHashMap <String, List<Unit>>();
	public LinkedHashMap <String, List<Unit>> comlists = new LinkedHashMap <String, List<Unit>>();
	
	public List<Race> races = new ArrayList<Race>();
	public String nationalitysuffix;
	
	public List<Filter> castles = new ArrayList<Filter>();
	
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
		
		comlists.put("scouts", new ArrayList<Unit>());
		comlists.put("commanders", new ArrayList<Unit>());
		comlists.put("priests", new ArrayList<Unit>());
		comlists.put("mages", new ArrayList<Unit>());
		
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
			if (!r.tags.contains("secondary")) 
			{
				allRaces.add(r);
			}
		}
		Race race;
		race = chandler.getRandom(allRaces);
				
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
		
		race = Race.getRandom(random, chandler.handleChanceIncs(allRaces));
		races.add(race.getCopy());
		
		// Add themes for secondary race
		addRaceThemes(races.get(1));
		
		// Apply the nation and primary race theme secondary race effects
		for(Theme t : races.get(0).themefilters)
		{
			for(String str : t.secondarynationeffects)
			{
				races.get(1).addOwnLine(str);
			}
			for(String str2 : t.bothnationeffects)
			{
				races.get(1).addOwnLine(str2);
			}
		}
		for(Theme t : this.nationthemes)
		{
			for(String str : t.secondarynationeffects)
			{
				races.get(1).addOwnLine(str);
			}
			for(String str2 : t.bothnationeffects)
			{
				races.get(1).addOwnLine(str2);
			}
		}
		
		
		
		// Add secondaryracecommands to the secondary race
		for(String tag : this.races.get(0).tags)
		{
			List<String> args = Generic.parseArgs(tag);
			if(args.get(0).equals("secondaryracecommand"))
			{
				args.remove(0);
				races.get(1).addCommand(Generic.listToString(args));
			}
		}

	}
	
	
	private void generateMagesAndPriests()
	{
		// Mages and priests
		MageGenerator mageGen = new MageGenerator(nationGen, this, assets);
		comlists.get("mages").addAll(mageGen.generateMages());
		comlists.get("priests").addAll(mageGen.generatePriests());
		mageGen = null;
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
		
		if(races.get(0).hasRole("scout") && !races.get(0).tags.contains("#no_scouts"))
			comlists.get("scouts").add(scoutgen.generateScout(races.get(0)));
		else if(races.get(1).hasRole("scout") && !races.get(1).tags.contains("#no_scouts"))
			comlists.get("scouts").add(scoutgen.generateScout(races.get(1)));
		scoutgen = null;
		System.gc();
	}
	
	private void generateSpecialComs()
	{

		// Special commanders

		double specialcomchance = 0.05;
		if(Generic.containsTag(races.get(0).tags, "specialcommanderchance"))
			specialcomchance = Double.parseDouble(Generic.getTagValue(races.get(0).tags, "specialcommanderchance"));
		
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
		double monsterchance = 0.05;
		if(Generic.containsTag(races.get(0).tags, "monsterchance"))
			monsterchance = Double.parseDouble(Generic.getTagValue(races.get(0).tags, "monsterchance"));
		
		if(random.nextDouble() < monsterchance)
		{
			MonsterGenerator mGen = new MonsterGenerator(this, this.nationGen, assets);
			Unit monster = mGen.generateMonster();
			if(monster != null)
			{
				this.unitlists.put("monsters", new ArrayList<Unit>());
				this.unitlists.get("monsters").add(monster);
			}
			mGen = null;
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
			startaff = Filter.getRandom(random, chandler.handleChanceIncs(posaff));
			
			if(startaff.getCommands().size() > 0)
			{
				for(Command c : startaff.getCommands())
				{
					Command.handleCommand(this.commands, c, nationGen);
				}
				posaff.remove(startaff);
			}
		
			cycles--;
		}
		posaff = null;
		System.gc();
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

		double extraPDMulti = 1;
		if (Generic.containsTag(this.races.get(0).tags, "extrapdmulti"))
			extraPDMulti = Double.parseDouble(Generic.getTagValue(this.races.get(0).tags, "extrapdmulti"));

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
		List<String> freeThemes = Generic.getTagValues(race.tags, "freenationtheme");
		
		for(String str : freeThemes)
		{
			List<Theme> frees = ChanceIncHandler.getFiltersWithType(str, possibleThemes);
			Theme t = null;
			
			if(frees.size() > 0)
			{
				t = Entity.getRandom(random, chandler.handleChanceIncs(frees));
				if(t != null)
				{
				
					possibleThemes.remove(t);
					race.handleTheme(t);
					for(Command c : t.commands)
						this.handleCommand(commands, c);
					this.nationthemes.add(t);
					
			
				}
			}


			if(t == null)
				System.out.println(race + " has #freenationtheme " + str + " but no filters of #type " + str);
			
			
		}
		
		
		int guaranteedthemes = 1;
		if(Generic.getTagValue(race.tags, "guaranteednationthemes") != null)
			guaranteedthemes = Integer.parseInt(Generic.getTagValue(race.tags, "guaranteednationthemes"));
		
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
				Theme t = Theme.getRandom(random, chandler.handleChanceIncs(possibleThemes));
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
		List<String> list = new ArrayList<String>();
		for(Filter f : this.spells)
			for(String str : Generic.getTagValues(f.tags, "spell"))
				if(!list.contains(str))
					list.add(str);
		
		return list;
	}
	
	private void addRaceThemes(Race race)
	{
		
		
		List<Theme> possibleThemes = ChanceIncHandler.retrieveFilters("racethemes", "default_racethemes", assets.themes, null, race);
		ChanceIncHandler chandler = new ChanceIncHandler(this);
		
		List<String> freeThemes = Generic.getTagValues(race.tags, "freetheme");
		
		for(String str : freeThemes)
		{
			List<Theme> frees = ChanceIncHandler.getFiltersWithType(str, possibleThemes);
			Theme t = null;
			
			if(frees.size() > 0)
			{
				t = Entity.getRandom(random, chandler.handleChanceIncs(frees));
				if(t != null)
				{
				
					possibleThemes.remove(t);
					race.themefilters.add(t);
					race.handleTheme(t);
			
				}
			}


			if(t == null)
				System.out.println(race + " has #freetheme " + str + " but no filters of #type " + str);
			
			
		}
		
		
		
		int guaranteedthemes = 1;
		if(Generic.getTagValue(race.tags, "guaranteedthemes") != null)
			guaranteedthemes = Integer.parseInt(Generic.getTagValue(race.tags, "guaranteedthemes"));
		
		boolean getsNonFreeThemes = (race == races.get(0));
		if(!getsNonFreeThemes)
			getsNonFreeThemes = Generic.containsTag(race.tags, "normal_themes_as_secondary");
		
		// Guaranteed themes
		for(int i = 0; i < guaranteedthemes; i++)
		{
	
			possibleThemes = ChanceIncHandler.getValidFilters(possibleThemes, race.themefilters);
			if(possibleThemes.size() > 0)
			{
				Theme t = Theme.getRandom(random, chandler.handleChanceIncs(possibleThemes));
				if(t != null)
				{
					race.themefilters.add(t);
					race.handleTheme(t);
					possibleThemes.remove(t);
					

				}
			}
		}
		
		// 10% chance for second theme
		possibleThemes = ChanceIncHandler.getValidFilters(possibleThemes, race.themefilters);
		if(possibleThemes.size() > 0 && random.nextDouble() < 0.1)
		{
			Theme t = Theme.getRandom(random, chandler.handleChanceIncs(possibleThemes));
			if(t != null)
			{
				race.themefilters.add(t);
				race.handleTheme(t);
			}
		}
		
		
		
	}
	
	public void finalizeUnits()
	{
		List<Command> conditional = new ArrayList<Command>();
		
		for(String tag : this.races.get(0).tags)
		{
			List<String> args = Generic.parseArgs(tag);
			if(args.get(0).equals("secondaryracecommand_conditional"))
			{
				conditional.add(Command.parseCommandFromDefinition(args));
			}
		}
		
		
		
		for(List<Unit> l : this.comlists.values())
			for(Unit u : l)
			{
				u.commands.addAll(u.race.specialcommands);
				giveSecondaryRaceSpecialCommands(u, conditional);
				
				int priest = u.getMagicPicks()[8];
				if(Generic.getAllNationTags(this).contains("priestextracost") && priest > 0)
				{
					List<String> asd = Generic.getTagValues(Generic.getAllNationTags(this), "priestextracost");
					int highest = 0;
					for(String str : asd)
					{
						if(Integer.parseInt(str) > highest)
							highest = Integer.parseInt(str);
					}
					
					
					
					u.commands.add(new Command("#gcost", "+" + (highest * priest)));
				}

					
				
				u.polish();
			}
		for(List<Unit> l : this.unitlists.values())
			for(Unit u : l)
			{
				if(u.tags.contains("elite") || u.tags.contains("sacred"))
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
			Filter f = Entity.getRandom(random, chandler.handleChanceIncs(possibles));
			
			
			for(List<Unit> l : this.comlists.values())
				for(Unit u : l)
				{
					if(((u.race == races.get(0)) == primary || both) && ChanceIncHandler.canAdd(u, f) && !f.tags.contains("trooponly"))
					{
						added++;
						u.appliedFilters.add(f);
					}
						
				}
			for(List<Unit> l : this.unitlists.values())
				for(Unit u : l)
				{
					if(((u.race == races.get(0)) == primary || both) && ChanceIncHandler.canAdd(u, f) && !f.tags.contains("commanderonly"))
					{
						added++;
						u.appliedFilters.add(f);
					}
				}
			

			for(Unit u : heroes)
			{
				if(((u.race == races.get(0)) == primary || both) && ChanceIncHandler.canAdd(u, f) && !f.tags.contains("trooponly"))
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
			
				if(u.tags.contains("schoolmage " + i))
					l.add(u);
			}
			lists.add(l);
		}
		
		List<Unit> l = new ArrayList<Unit>();
		for(Unit u : all)
			if(u.tags.contains("extramage"))
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
			lines.add(cmd.command + " " + Generic.listToString(cmd.args));
		}
		
		
		lines.add("");
		
		// Gods
		for(Command cmd : this.gods)
			lines.add(cmd.command + " " + Generic.listToString(cmd.args));
		
		
		
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
							if(u.tags.contains(tag))
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
							if(u.tags.contains(tag))
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
		
		c = new Command(c.command, c.args);
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
				String arg = c.args.get(0);
				String oldarg = old.args.get(0);
				if(arg.startsWith("+") || arg.startsWith("-"))
				{
					if(arg.startsWith("+"))
					{
						arg = arg.substring(1);
					}
				
					
					
					if(old != null && !uniques.contains(c.command))
					{
						int argint = (Integer.parseInt(oldarg) + Integer.parseInt(arg));
						if(c.command.equals("#idealcold"))
						{
							argint = Math.min(3, argint);
							argint = Math.max(-3, argint);
						}
						oldarg = argint + "";  
						old.args.set(i, oldarg);
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
					if(old != null && !uniques.contains(c.command))
					{
						oldarg = arg;
						old.args.set(i, oldarg);
	
					}
					else
					{
						commands.add(c);
						return;
					}
				}
				
			}
		}	
		else
		{
			// If there's no existing copy and the command starts with +, remove +.
			if(c.args.size() > 0 && c.args.get(0).startsWith("+"))
			{
				c.args.set(0, c.args.get(0).substring(1));
			}
			commands.add(c);
		}

	}
	
	public List<Unit> generateTroopList()
	{
		List<Unit> units = new ArrayList<Unit>();
		
		Iterator<List<Unit>> itr = unitlists.values().iterator();
		while(itr.hasNext())
			units.addAll(itr.next());

		return units;
	}

	public List<Unit> generateUnitList()
	{
		List<Unit> units = new ArrayList<Unit>();
		
		Iterator<List<Unit>> itr = comlists.values().iterator();
		while(itr.hasNext())
			units.addAll(itr.next());
		
		itr = unitlists.values().iterator();
		while(itr.hasNext())
			units.addAll(itr.next());

		return units;
	}
	
	public List<Unit> generateComList()
	{
		List<Unit> units = new ArrayList<Unit>();
		
		Iterator<List<Unit>> itr = comlists.values().iterator();
		while(itr.hasNext())
			units.addAll(itr.next());

		return units;
	}
	public List<Unit> generateUnitList(String type)
	{
		List<Unit> list = new ArrayList<Unit>();
		Iterator<Entry<String, List<Unit>>> itr = unitlists.entrySet().iterator();
		while(itr.hasNext())
		{
			Entry<String, List<Unit>> entry = itr.next();
			if(entry.getKey().startsWith(type))
			{
				for(Unit u : entry.getValue())
					list.add(u);
			}
		}
		return list;
	}
	
	public List<Unit> generateComList(String type)
	{
		List<Unit> list = new ArrayList<Unit>();
		Iterator<Entry<String, List<Unit>>> itr = comlists.entrySet().iterator();
		while(itr.hasNext())
		{
			Entry<String, List<Unit>> entry = itr.next();
			if(entry.getKey().startsWith(type))
			{
				
				for(Unit u : entry.getValue())
					list.add(u);
			}
		}
		return list;
	}
	
	
	public List<List<Unit>> getListsOfType(String type, boolean coms, boolean troops)
	{
		List<List<Unit>> lists = new ArrayList<List<Unit>>();
		
		if(troops)
		{
			Iterator<Entry<String, List<Unit>>> itr = unitlists.entrySet().iterator();
			while(itr.hasNext())
			{
				Entry<String, List<Unit>> entry = itr.next();
				if(entry.getKey().startsWith(type))
				{
					lists.add(entry.getValue());
				}
			}
		}
		
		if(coms)
		{
			Iterator<Entry<String, List<Unit>>> itr = comlists.entrySet().iterator();
			while(itr.hasNext())
			{
				Entry<String, List<Unit>> entry = itr.next();
				if(entry.getKey().startsWith(type))
				{
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
