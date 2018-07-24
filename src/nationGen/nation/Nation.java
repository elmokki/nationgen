package nationGen.nation;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;



















import com.elmokki.Drawing;
import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.NationGenAssets;
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
import nationGen.restrictions.MagicAccessRestriction;
import nationGen.restrictions.MageWithAccessRestriction;
import nationGen.restrictions.MagicDiversityRestriction;
import nationGen.restrictions.NationRestriction;
import nationGen.restrictions.NationThemeRestriction;
import nationGen.restrictions.NoPrimaryRaceRestriction;
import nationGen.restrictions.NoUnitOfRaceRestriction;
import nationGen.restrictions.PrimaryRaceRestriction;
import nationGen.restrictions.SacredRaceRestriction;
import nationGen.restrictions.UnitCommandRestriction;
import nationGen.restrictions.UnitFilterRestriction;
import nationGen.restrictions.UnitOfRaceRestriction;
import nationGen.restrictions.RecAnywhereSacredsRestriction;
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
	//public List<Filter> appliedfilters = new ArrayList<Filter>();
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
		this.era = (int)Math.round(nationGen.settings.get("era"));
		this.assets = assets;
		
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

                for(Race r : nationGen.races)
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
		allRaces.addAll(nationGen.races);
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
		GodGen gg = new GodGen(this);
		this.gods.addAll(gg.giveGods());
		gg = null;
	}
	
	private void getForts()
	{
		ChanceIncHandler chandler = new ChanceIncHandler(this);

		// Forts
		List<Filter> possibleForts = ChanceIncHandler.retrieveFilters("forts", "default_forts", nationGen.miscdef, null, races.get(0));
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
		fg = null;
		
	}
	
	private void getStartAffinity()
	{
		ChanceIncHandler chandler = new ChanceIncHandler(this);

		// Start affinity
		int cycles = 2;
		
		List<Filter> posaff = ChanceIncHandler.retrieveFilters("startaffinities", "startaffinities", nationGen.miscdef, null, races.get(0));

		
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
            //
            // Easiest way I found to get the Class information for doing a comparsion; might be an easier way?
            //
            MageWithAccessRestriction mageWithAccessRestriction = new MageWithAccessRestriction(nationGen);
            MagicAccessRestriction magicAccessRestriction = new MagicAccessRestriction(nationGen);
            MagicDiversityRestriction magicDiversityRestriction = new MagicDiversityRestriction(nationGen);
            NationThemeRestriction nationThemeRestriction = new NationThemeRestriction(nationGen, assets);
            NoPrimaryRaceRestriction noPrimaryRaceRestriction = new NoPrimaryRaceRestriction(nationGen);
            NoUnitOfRaceRestriction noUnitOfRaceRestriction = new NoUnitOfRaceRestriction(nationGen);
            PrimaryRaceRestriction primaryRaceRestriction = new PrimaryRaceRestriction(nationGen);
            SacredRaceRestriction sacredRaceRestriction = new SacredRaceRestriction(nationGen);
            RecAnywhereSacredsRestriction recAnywhereSacredRestriction = new RecAnywhereSacredsRestriction(nationGen);
            UnitCommandRestriction unitCommandRestriction = new UnitCommandRestriction(nationGen);
            UnitFilterRestriction unitFilterRestriction = new UnitFilterRestriction(nationGen);
            UnitOfRaceRestriction unitOfRaceRestriction = new UnitOfRaceRestriction(nationGen);
            	
            
            @SuppressWarnings("rawtypes")
			List<Class> restrictionTypes = new ArrayList<>();
            restrictionTypes.add(primaryRaceRestriction.getClass());
            restrictionTypes.add(noPrimaryRaceRestriction.getClass());
            restrictionTypes.add(nationThemeRestriction.getClass());
            
            getColors();
            getRaces();
            
            if (!checkRestrictions(restrictions, restrictionTypes)) 
            {
                return;
            }
            restrictionTypes.clear();
            restrictionTypes.add(mageWithAccessRestriction.getClass());
            restrictionTypes.add(magicAccessRestriction.getClass());
            restrictionTypes.add(magicDiversityRestriction.getClass());
            restrictionTypes.add(primaryRaceRestriction.getClass());
            
            generateMagesAndPriests();
            
            if (!checkRestrictions(restrictions, restrictionTypes)) 
            {
                return;
            }
            restrictionTypes.clear();
            restrictionTypes.add(sacredRaceRestriction.getClass());
            restrictionTypes.add(recAnywhereSacredRestriction.getClass());
            
            generateTroops();
            generateSacreds();
            
            if (!checkRestrictions(restrictions, restrictionTypes)) 
            {
                return;
            }
            restrictionTypes.clear();
            restrictionTypes.add(unitCommandRestriction.getClass());
            restrictionTypes.add(unitFilterRestriction.getClass());
            restrictionTypes.add(unitOfRaceRestriction.getClass());
            restrictionTypes.add(noUnitOfRaceRestriction.getClass());
            
            generateScouts();
            generateSpecialComs();
            generateGods();
            getForts();
            generateHeroes();
            applyNationWideFilter();
            generateComs();
            
            if (!checkRestrictions(restrictions, restrictionTypes)) 
            {
                return;
            }
            
            generateMonsters();
            SiteGenerator.generateSites(this);
            generateSpells();
            generateFlag();	
            getStartAffinity();

            double extraPDMulti = 1;
    		if(Generic.containsTag(this.races.get(0).tags, "extrapdmulti"))
    			extraPDMulti = Double.parseDouble(Generic.getTagValue(this.races.get(0).tags, "extrapdmulti"));

    		if(random.nextDouble() < 0.1 * extraPDMulti)
            {
            	if(random.nextDouble() < 0.02 * extraPDMulti)
            		PDRanks = 4;
            	else
            		PDRanks = 3;
            }
            
            //finalizeUnits();
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
		
		List<Filter> possibles = ChanceIncHandler.retrieveFilters("nationwidefilters", "default_nationwidefilters", nationGen.filters, null, races.get(0));

		ChanceIncHandler chandler = new ChanceIncHandler(this);
		boolean primary = true;
		boolean both = false;
		
		if(random.nextDouble() < this.percentageOfRace(races.get(1)))
		{
			possibles.retainAll(ChanceIncHandler.retrieveFilters("nationwidefilters", "default_nationwidefilters", nationGen.filters, null, races.get(1)));
			if(possibles.size() > 0 && random.nextDouble() < 0.1)
			{
				both = true;
			}
			else
			{
				primary = false;
				possibles = ChanceIncHandler.retrieveFilters("nationwidefilters", "default_nationwidefilters", nationGen.filters, null, races.get(1));
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
	
	/*
	private void addFilters()
	{
		ChanceIncHandler chandler = new ChanceIncHandler(this);
		List<Filter> filters = ChanceIncHandler.retrieveFilters("nationfilters", "default_nationfilters", this.nationGen.filters, null, this.races.get(0));
		
		int count = 0;
		if(random.nextDouble() > 0.5)
		{	
			count++;
			if(random.nextDouble() > 0.75)
			{	
				count++;
			}
		}
		
		for(int i = 0; i < count; i++)
		{
			if(filters.size() == 0)
				break;
			
			Filter f = Filter.getRandom(random, chandler.handleChanceIncs(filters));
			chandler.removeRelated(f, filters);
			this.appliedfilters.add(f);
			for(Command c : f.getCommands())
				this.handleCommand(this.commands, c);

		}
		
	}
	*/
	
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
     * @param tw
     * @throws IOException
     */
    public void write(PrintWriter tw, String spritedir) throws IOException
    {

        Color c = colors[0];

        double r = (double)(Math.round((double)c.getRed()/255*10)) / 10;
        double b = (double)(Math.round((double)c.getBlue()/255*10)) / 10;
        double g = (double)(Math.round((double)c.getGreen()/255*10)) / 10;
    

        tw.println();
        tw.println("-- Nation " + nationid + ": " + this.name + ", " + this.epithet);
        tw.println("---------------------------------------------------------------");
        tw.println("-- Generated with themes: " + this.nationthemes);
        tw.println("-- Generated with " + races.get(0) + " race themes: " + races.get(0).themefilters);
        tw.println("-- Generated with " + races.get(1) + " race themes: " + races.get(1).themefilters);
        tw.println("---------------------------------------------------------------");

        //writeNationInfo(tw);
        tw.println("#selectnation " + nationid);
        tw.println("#clear");
        tw.println("#era " + era);
        tw.println("#name \"" + name + "\"");
        tw.println("#epithet \"" + epithet + "\"");
        
        
		tw.println("#descr \"" + "A glorious NationGen nation! Generated from seed " + seed + " with settings integer " + 
				   nationGen.settings.getSettingInteger() + "\"");
		
        tw.println("#summary \"" + summary + "\"");
        tw.println("#brief \"No description\"");
        tw.println("#color " + (r) + " " + (b) + " " + (g)); 
        
        //Generic.generateFlag(this, spritedir + "/flag.tga");
        

        tw.println("#flag \"" + spritedir + "/flag.tga\"");
        tw.println("");
        // Sites
        tw.println("#clearsites");
 
       
        for(Site site : this.sites)
        {
        	tw.println("#startsite \"" + site.name +  "\"");
        }
        tw.println("");
       
        
        // Add recruitables
        tw.println("#clearrec");
        
        writeRecs(false, unitlists, tw);
        writeRecs(true, comlists, tw);

        
        // PD
        PDSelector pds = new PDSelector(this, nationGen);
                
      
        tw.println("");
        tw.println("#defcom1 " + pds.getIDforPD(pds.getPDCommander(1)));
        tw.println("#defunit1 " + pds.getIDforPD(pds.getMilitia(1, 1)));
        tw.println("#defmult1 " + pds.getMilitiaAmount(pds.getMilitia(1, 1)));
        tw.println("#defunit1b " + pds.getIDforPD(pds.getMilitia(2, 1)));
        tw.println("#defmult1b " + pds.getMilitiaAmount(pds.getMilitia(2, 1)));
        if(PDRanks > 2)
        {
            tw.println("#defunit1c " + pds.getIDforPD(pds.getMilitia(3, 1)));
            tw.println("#defmult1c " + pds.getMilitiaAmount(pds.getMilitia(3, 1)));
            
            if(PDRanks > 3)
            {
                tw.println("#defunit1d " + pds.getIDforPD(pds.getMilitia(4, 1)));
                tw.println("#defmult1d " + pds.getMilitiaAmount(pds.getMilitia(4, 1)));
            }
        }
        tw.println("#defcom2 " + pds.getIDforPD(pds.getPDCommander(2)));
        tw.println("#defunit2 " + pds.getIDforPD(pds.getMilitia(1, 2)));
        tw.println("#defmult2 " + pds.getMilitiaAmount(pds.getMilitia(1, 2)));
        tw.println("#defunit2b " + pds.getIDforPD(pds.getMilitia(2, 2)));
        tw.println("#defmult2b " + pds.getMilitiaAmount(pds.getMilitia(2, 2)));
        tw.println("");
    
        // Wall units
        
        tw.println("#wallcom " + pds.getIDforPD(pds.getPDCommander(1)));
        tw.println("#wallunit " + pds.getIDforPD(pds.getWallUnit(true)));
        tw.println("#wallmult " + pds.getMilitiaAmount(pds.getWallUnit(true)));

        
        // Start army
        tw.println("#startcom " + pds.getStartArmyCommander().id);
        if(comlists.get("scouts").size() > 0)
        	tw.println("#startscout " + comlists.get("scouts").get(0).id);
        else
        	tw.println("#startscout " + pds.getPDCommander(2).id);
    
        
        tw.println("#startunittype1 " + pds.getMilitia(1, 1, false).id);
        tw.println("#startunittype2 " + pds.getMilitia(1, 2, false).id);
        int amount1 = (int)(pds.getStartArmyAmount(pds.getMilitia(1, 1, false)));
        tw.println("#startunitnbrs1 " + amount1);
        int amount2 = (int)(pds.getStartArmyAmount(pds.getMilitia(1, 2, false)));
        tw.println("#startunitnbrs2 " + amount2);
        tw.println("");
        
        // Heroes
        for(int i = 0; i < heroes.size(); i++)
        {
        	tw.println("#hero" + (i+1) + " " + heroes.get(i).id);
        }

        
        tw.println("");

        // Custom coms
        
        for(Command cmd : this.getCommands())
        {
            tw.println(cmd.command + " " + Generic.listToString(cmd.args));
        }
        
        
        tw.println("");
        
        // Gods
        for(Command cmd : this.gods)
        	tw.println(cmd.command + " " + Generic.listToString(cmd.args));
     
 
        
        tw.println("");
        
        
        tw.println("#templepic " + random.nextInt(19));
        tw.println("");
        

        tw.println("#end");
        tw.println("");
        
        tw.println("");
        

        


    }
    
    
	public List<Unit> combineTroopsToList(String role)
	{
		List<Unit> unitlist = new ArrayList<Unit>();
		Iterator<Entry<String, List<Unit>>> itr = unitlists.entrySet().iterator();
		
		if(itr.hasNext())
		{
			Entry<String, List<Unit>> entry = itr.next();
	
			do
			{
				if(entry.getKey().startsWith(role))
				{
					for(Unit u : entry.getValue())
						unitlist.add(u);
				}
				entry = itr.next();
	
			} while(itr.hasNext());
		}
		return unitlist;
	}
    



	
    
	public void writeUnits(PrintWriter tw, String spritedir) throws IOException
	{
	
        tw.println("");
        tw.println("--- Unit definitions for " + this.name);	
        tw.println("");
        
    	List<Unit> units = new ArrayList<Unit>();
    	for(List<Unit> list : this.comlists.values())
    		units.addAll(list);
    	for(List<Unit> list : this.unitlists.values())
    		units.addAll(list);
    	units.addAll(heroes);
    
    	List<ShapeChangeUnit> sus = new ArrayList<ShapeChangeUnit>();
        for(ShapeChangeUnit su : nationGen.forms)
        {
        	if(units.contains(su.otherForm))
        		sus.add(su);
        }
        
        for(ShapeChangeUnit su : sus)
        {
        	su.write(tw, spritedir);
        }
   
	    Iterator<List<Unit>> itr = unitlists.values().iterator();
	    while(itr.hasNext())
	    {
	    	List<Unit> list = (List<Unit>)itr.next();
        	for(Unit u : list)
        	{
        		if(u.color == Color.white)
        			u.color = colors[0];
	
        		//if(nationGen.settings.get("skipSprites") == 0)
        		
				if(!u.invariantMonster)
				{
	        		u.draw(spritedir);
	        		u.write(tw, spritedir);
				}
				else  // The monster uses in-game graphics and stats; just print out name, ID, and costs
				{
					tw.println("--- " + u.name + " (Unit ID " + u.id + "), Gold: " + u.getGoldCost() + ", Resources: " + u.getResCost(true) + "\n");
				}
        	}
	    }


	    itr = comlists.values().iterator();
	    while(itr.hasNext())
	    {
	    	List<Unit> list = (List<Unit>)itr.next();
        	for(Unit u : list)
        	{
        		if(u.color == Color.white)
        			u.color = colors[0];
        		
        		//if(nationGen.settings.get("skipSprites") == 0)
        			u.draw(spritedir);
        		u.write(tw, spritedir);
        	}
	    }
	    
	    for(Unit u : heroes)
	    {
	    	u.draw(spritedir);
	    	u.write(tw, spritedir);
	    }
	    
	    



        
	}
	

	private void writeRecs(boolean coms, LinkedHashMap<String, List<Unit>> unitlists, PrintWriter tw)
	{
		List<String> order = Generic.parseArgs("ranged infantry mounted chariot special sacred monsters");
		if(coms)
			order = Generic.parseArgs("scouts commanders specialcoms priests mages");
		

		
		String line = "#addreccom";
		if(!coms)
			line = "#addrecunit";
		
		List<String> foreigntags = new ArrayList<String>();
		foreigntags.add("forestrec");
		foreigntags.add("mountainrec");
		foreigntags.add("swamprec");
		foreigntags.add("wasterec");
		foreigntags.add("caverec");
		

		List<String> listnames = new ArrayList<String>();
		listnames.addAll(unitlists.keySet());
		for(String str : order)
		{
			if(true)
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
										tw.println("#" + tag.substring(0, tag.length() - 3) + "com " + u.id);
									}
									else
										tw.println("#" + tag+ " " + u.id);
								}
							
							if(!u.caponly)
								tw.println(line + " " + u.id);
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
										tw.println("#" + tag.substring(0, tag.length() - 3) + "com " + u.id);
									else
										tw.println("#" + tag+ " " + u.id);
								}
							
							if(!u.caponly)
								tw.println(line + " " + u.id);
						}
				}
			}
			
		}
		
	}
    
	public List<Command> getCommands()
	{
		List<Command> coms  = new ArrayList<Command>();
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
	
	public void writeSites(PrintWriter tw)
	{
		tw.println("--- Sites for nation " + nationid + ": " + name);
		for(Site site : sites)
			site.write(tw);
	}
	
    public boolean checkRestrictions(List restrictions, List<Class> restrictionTypes)
    {
        for(int index = 0; index < restrictions.size(); index++)
        {
            NationRestriction n = (NationRestriction) restrictions.get(index);
            boolean isRestrictionType = false;
            
            for(Class restrictionType : restrictionTypes)
            {
                if (n.getClass() == restrictionType) 
                {
                    isRestrictionType = true;
                }
            }
            
            if (!isRestrictionType) 
            {
                continue;
            }
            if (!n.doesThisPass(this)) 
            {
                this.passed = false;
                this.restrictionFailed = n.toString().toUpperCase();
                return false;
            }
        }
        return true;
    }
}
