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
import nationGen.entities.Entity;
import nationGen.entities.Filter;
import nationGen.entities.Race;
import nationGen.entities.Theme;
import nationGen.items.CustomItem;
import nationGen.magic.MageGenerator;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.misc.FlagGen;
import nationGen.misc.GodGen;
import nationGen.misc.ItemSet;
import nationGen.misc.Site;
import nationGen.misc.SiteGenerator;
import nationGen.naming.Summary;
import nationGen.rostergeneration.CommanderGenerator;
import nationGen.rostergeneration.HeroGenerator;
import nationGen.rostergeneration.MonsterGenerator;
import nationGen.rostergeneration.RosterGenerator;
import nationGen.rostergeneration.SacredGenerator;
import nationGen.rostergeneration.ScoutGenerator;
import nationGen.units.ShapeChangeUnit;
import nationGen.units.Unit;


public class Nation {
	public Color[] colors = new Color[5];
	public int nationid = 92;
	public int era = 2;
	public String name = "UNNAMED";
	public String epithet = "NO EPITHET";
	
	public NationGen nationGen;
	public int seed = 0;
	
	public ItemSet usedItems = new ItemSet();
	public Random random;

	public List<Unit> heroes = new ArrayList<Unit>();

	public List<Command> commands = new ArrayList<Command>();
	public List<Filter> appliedfilters = new ArrayList<Filter>();
	public List<CustomItem> usedcustomitems = new ArrayList<CustomItem>();
	public List<ShapeChangeUnit> secondshapes = new ArrayList<ShapeChangeUnit>();
	public List<CustomItem> customitems = new ArrayList<CustomItem>();
	public List<Command> gods = new ArrayList<Command>();
	public List<Site> sites = new ArrayList<Site>();
	
	
	public LinkedHashMap <String, List<Unit>> unitlists = new LinkedHashMap <String, List<Unit>>();
	public LinkedHashMap <String, List<Unit>> comlists = new LinkedHashMap <String, List<Unit>>();
	
	public List<Race> races = new ArrayList<Race>();
	public String nationalitysuffix;
	
	public List<Filter> castles = new ArrayList<Filter>();
	
	public Summary summary = new Summary(this);
	
	public BufferedImage flag = null;

	public Nation(NationGen ngen, int id, int seed)
	{
		this.nationid = id;
		this.nationGen = ngen;
		this.random = new Random(seed);
		this.seed = seed;
		this.era = (int)Math.round(nationGen.settings.get("era"));
		
		comlists.put("scouts", new ArrayList<Unit>());
		comlists.put("commanders", new ArrayList<Unit>());
		comlists.put("priests", new ArrayList<Unit>());
		comlists.put("mages", new ArrayList<Unit>());
		
		generate();
		
		
	}
	
	
	public void generate()
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
		
		


		ChanceIncHandler chandler = new ChanceIncHandler(this);

		// choose primary race
		List<Race> allRaces = new ArrayList<Race>();
		allRaces.addAll(nationGen.races);

		Race race;
		do
		{
			race = chandler.getRandom(allRaces);
			allRaces.remove(race);

		} while(race.tags.contains("secondary"));

		races.add(race.getCopy());
		
		for(Command c : races.get(0).nationcommands)
			this.handleCommand(commands, c);
		

		
		// Add themes
		addThemes(races.get(0));
		
		// Restart chanceinc handler after adding themes.
		chandler = new ChanceIncHandler(this);
		
		
		// Secondary race after themes since themes may affect it
		allRaces.clear();
		allRaces.addAll(nationGen.races);
		allRaces.remove(race);
		
		race = Race.getRandom(random, chandler.handleChanceIncs(allRaces));
		races.add(race.getCopy());
		
		// Add themes for secondary race
		addThemes(races.get(1));
		
		// Apply the theme primary race effects for the secondary race as well
		for(Theme t : races.get(0).themefilters)
		{

			for(String str : t.secondarynationeffects)
			{
				races.get(1).addOwnLine(str);
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
		
		// Restart chanceinc handler after adding themes to secondary race as well.
		chandler = new ChanceIncHandler(this);
		

		
		
		// Mages and priests
		MageGenerator mageGen = new MageGenerator(nationGen, this);
		comlists.get("mages").addAll(mageGen.generateMages());
		comlists.get("priests").addAll(mageGen.generatePriests());
		

		// Troops
		RosterGenerator g = new RosterGenerator(nationGen, this);
		g.execute();

		//// Sacreds and elites
		SacredGenerator sacGen = new SacredGenerator(nationGen, this);
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
		

		// Scouts
		
		ScoutGenerator scoutgen = new ScoutGenerator(nationGen, this);
		if(races.get(0).hasRole("scout"))
			comlists.get("scouts").add(scoutgen.generateScout(races.get(0)));
		


		// Gods
		GodGen gg = new GodGen(this);
		this.gods.addAll(gg.giveGods());
		

	
		// Heroes
		
		HeroGenerator hg = new HeroGenerator(nationGen, this);
		int heroes = 1 + random.nextInt(3); // 1-3
		this.heroes.addAll(hg.generateHeroes(heroes));

		
		// Nation wide filters
		int count = 0;
		if(random.nextDouble() < 0.33)
		{
			count++;
			if(random.nextDouble() < 0.33)
				count++;
		}

		applyNationWideFilter(count);
		

		// Commanders
		CommanderGenerator comgen = new CommanderGenerator(nationGen, this);
		comgen.generateComs();
		

		
		// Monsters
		double monsterchance = 0.05;
		if(Generic.containsTag(races.get(0).tags, "monsterchance"))
			monsterchance = Double.parseDouble(Generic.getTagValue(races.get(0).tags, "monsterchance"));
		
		if(random.nextDouble() < monsterchance)
		{
			MonsterGenerator mGen = new MonsterGenerator(this, this.nationGen);
			Unit monster = mGen.generateMonster();
			if(monster != null)
			{
				this.unitlists.put("monsters", new ArrayList<Unit>());
				this.unitlists.get("monsters").add(monster);
			}
			
		}
		
		// Sites
		SiteGenerator.generateSites(this);
		
		// Nation filters
		addFilters();
		
		// Flag
		FlagGen fg = new FlagGen(this);
		this.flag = fg.generateFlag(this);
		
		// Start affinity
		int cycles = 2;
		
		List<Filter> posaff = ChanceIncHandler.retrieveFilters("startaffinities", "startaffinities", nationGen.miscdef, null, races.get(0));
		while(cycles > 0)
		{
			Filter startaff = Filter.getRandom(random, chandler.handleChanceIncs(posaff));
			
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
		
		
		
		// Finalizing
		finalizeUnits();
	
		
		List<ShapeChangeUnit> sul = new ArrayList<ShapeChangeUnit>();
    	List<Unit> units = new ArrayList<Unit>();
    	for(List<Unit> list : this.comlists.values())
    		units.addAll(list);
    	for(List<Unit> list : this.unitlists.values())
    		units.addAll(list);
    	units.addAll(this.heroes);
    	
        for(ShapeChangeUnit su : nationGen.forms)
        {
        	if(units.contains(su.otherForm))
        		sul.add(su);
        }
        
        for(ShapeChangeUnit su : sul)
        {	
    			su.polish(nationGen, this);
        }
        
       
	}
	
	

	
	private void addThemes(Race race)
	{
		Random r = new Random(this.random.nextInt());
		
		
		List<Theme> possibleThemes = ChanceIncHandler.retrieveFilters("nationthemes", "default_themes", nationGen.themes, null, race);
		ChanceIncHandler chandler = new ChanceIncHandler(this);
		
		List<String> freeThemes = Generic.getTagValues(race.tags, "freetheme");
		
		for(String str : freeThemes)
		{
			List<Theme> frees = ChanceIncHandler.getFiltersWithType(str, possibleThemes);
			Theme t = null;
			
			if(frees.size() > 0)
			{
				t = Entity.getRandom(r, chandler.handleChanceIncs(frees));
				if(t != null)
				{
				
					possibleThemes.remove(t);
					race.addTheme(t);
			
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
			Generic.containsTag(race.tags, "normal_themes_as_secondary");
		
		// Guaranteed themes
		for(int i = 0; i < guaranteedthemes; i++)
		{
	
			possibleThemes = ChanceIncHandler.getValidFilters(possibleThemes, race.themefilters);
			if(possibleThemes.size() > 0)
			{
				Theme t = Theme.getRandom(r, chandler.handleChanceIncs(possibleThemes));
				if(t != null)
				{
					race.addTheme(t);
					possibleThemes.remove(t);
					

				}
			}
		}
		
		// 10% chance for second theme
		possibleThemes = ChanceIncHandler.getValidFilters(possibleThemes, race.themefilters);
		if(possibleThemes.size() > 0 && r.nextDouble() < 0.1)
		{
			Theme t = Theme.getRandom(r, chandler.handleChanceIncs(possibleThemes));
			if(t != null)
			{
				race.addTheme(t);
			}
		}
		
		
		
	}
	
	private void finalizeUnits()
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

	
	private void applyNationWideFilter(int count)
	{

		
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
	
	
	private void addFilters()
	{
		ChanceIncHandler chandler = new ChanceIncHandler(this);
		List<Filter> filters = ChanceIncHandler.retrieveFilters("nationfilters", "default_nationfilters", this.nationGen.filters, null, this.races.get(0));
		
		int count = 0;
		if(random.nextDouble() > 0.65)
		{	
			count++;
			if(random.nextDouble() > 0.65)
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
        tw.println("-- Generated with filters: " + this.appliedfilters);
        tw.println("-- Generated with " + races.get(0) + " race themes: " + races.get(0).themes);
        tw.println("-- Generated with " + races.get(1) + " race themes: " + races.get(1).themes);
        tw.println("---------------------------------------------------------------");

        //writeNationInfo(tw);
        tw.println("#selectnation " + nationid);
        tw.println("#clear");
        tw.println("#era " + era);
        tw.println("#name \"" + name + "\"");
        tw.println("#epithet \"" + epithet + "\"");
        
        
		tw.println("#descr \"" + "A glorious NationGen nation!" + "\"");
		
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

        
        
        tw.println("");
        tw.println("#defcom1 " + comlists.get("commanders").get(0).id);
        tw.println("#defunit1 " + getMilitia(1, 1).id);
        tw.println("#defmult1 " + militiaAmount(getMilitia(1, 1)));
        tw.println("#defunit1b " + getMilitia(2, 1).id);
        tw.println("#defmult1b " + militiaAmount(getMilitia(2, 1)));
        tw.println("#defcom2 " + comlists.get("commanders").get(1).id);
        tw.println("#defunit2 " + getMilitia(1, 2).id);
        tw.println("#defmult2 " + militiaAmount(getMilitia(1, 2)));
        tw.println("#defunit2b " + getMilitia(2, 2).id);
        tw.println("#defmult2b " + militiaAmount(getMilitia(2, 2)));
        tw.println("");
        // Start army
        
     
        tw.println("#startcom " + comlists.get("commanders").get(0).id);
        if(comlists.get("scouts").size() > 0)
        	tw.println("#startscout " + comlists.get("scouts").get(0).id);
        else
        	tw.println("#startscout " + comlists.get("commanders").get(1).id);
    
        
        tw.println("#startunittype1 " + getMilitia(1, 1).id);
        tw.println("#startunittype2 " + getMilitia(1, 2).id);
        int amount1 = (int)(militiaAmount(getMilitia(1, 1)) * 1.2);
        tw.println("#startunitnbrs1 " + amount1);
        int amount2 = (int)(militiaAmount(getMilitia(1, 2)) * 1.2);
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
     
        // Forts
        int forts = this.era;
        if(random.nextDouble() > 0.8)
        	forts++;
        else if(random.nextDouble() > 0.8)
        	forts--;
        if(random.nextDouble() > 0.9 && forts < this.era)
        	forts--;
        if(random.nextDouble() > 0.9 && forts > this.era)
        	forts++;
        if(forts == 0 && random.nextDouble() > 0.2)
        	forts++;

        forts = Math.min(3, Math.max(0, forts));
        tw.println("#fortera " + forts);
        
        tw.println("");
        
        
        
        
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
		while(itr.hasNext())
		{
			Entry<String, List<Unit>> entry = itr.next();
			if(entry.getKey().startsWith(role))
			{
				for(Unit u : entry.getValue())
					unitlist.add(u);
			}
		}
		return unitlist;
	}
    
	/**
	 * Gets the rank:th best militia unit of tier:th tier
	 * @param rank rankth best (1-2)
	 * @param tier tier increases possible resource cost.
	 * @return
	 */
	public Unit getMilitia(int rank, int tier)
	{
		
		if(rank < 1)
			rank = 1;
		if(rank > 2)
			rank = 2;
		
		double targetgcost = 10;
		double targetrcost = 10;
		
		List<List<Unit>> militia = new ArrayList<List<Unit>>();

		List<Unit> units = combineTroopsToList("infantry");
		units.addAll(combineTroopsToList("mounted"));
		units.addAll(combineTroopsToList("ranged"));
		
		//System.out.println(nationid + " has " + units.size() + " units for militia");
		
		
		
		List<Unit> units2 = new ArrayList<Unit>();
		units2.addAll(units);
		
		for(int i = 0; i < tier; i++)
		{
			if(units.size() == 0)
			{
				//System.out.println("NOT ENOUGH UNITS FOR MILITIA?! WHAT IS THIS MADNESS? (Nation " + this.nationid + ")");
				
				units = combineTroopsToList("infantry");
				units.addAll(combineTroopsToList("mounted"));
				units.addAll(combineTroopsToList("ranged"));
				//break;
			}
			
			List<Unit> rankunits = new ArrayList<Unit>();
			
			targetrcost = targetrcost * 1.2;
			while(rankunits.size() < 2 && units.size() > 0)
			{	
				boolean canBeRanged = true;
				if(i > 0 && militia.get(i - 1).get(0).isRanged())
					canBeRanged = false;
				if(rankunits.size() > 0 && rankunits.get(0).isRanged())
					canBeRanged = false;
				
				Unit best = units.get(0);
				double bestscore = scoreForMilitia(best, targetrcost, targetgcost);
				for(Unit u : units)
				{
					if(!u.isRanged() || (u.isRanged() && canBeRanged))
					{
						double score = scoreForMilitia(u, targetrcost, targetgcost);
						if(bestscore >= score)
						{
							bestscore = score;
							best = u;
						}		
					}
				}
				units.remove(best);
				rankunits.add(best);
			}
			

			if(rankunits.size() < 2)
			{
				//System.out.println("MILITIA PROBLEM - FAILSAFE INITIATED FOR RACE " + this.races.get(0).name + " NATION " + this.nationid);
				rankunits.add(rankunits.get(0));
			}

			militia.add(i, rankunits);
		}

		
		return militia.get(tier - 1).get(rank - 1);
	}
	
	private double scoreForMilitia(Unit u, double targetres, double targetgold)
	{

		double goldscore = 0;
		double resscore = 0;
		
		
		goldscore = Math.abs(u.getGoldCost() - targetgold);
		if(u.getGoldCost() < targetgold * 0.7)
			goldscore = goldscore * 2;
		if(u.getGoldCost() > targetgold * 2)
			goldscore = goldscore * 2;
		
		resscore = Math.abs(u.getResCost(false) - targetres);
		if(u.getResCost(false) < targetres * 0.7)
			resscore = resscore * 2;
		if(u.getResCost(false) > targetres * 2.25)
			resscore = resscore * 2;
	
		if(u.isRanged() && u.getResCost(false) < 20)
			resscore = resscore * 0.25;
		
		//System.out.println("TARGET: " + targetres + " " + targetgold);
		//System.out.println("UNIT  : " + u.getResCost() + " " + u.getGoldCost() + " - " + u.id + " / " + u.name);
		//System.out.println("--> " + (0.75*goldscore + resscore));
		return 0.75*goldscore + resscore;
	}
	
	private int militiaAmount(Unit u)
	{
		int res = u.getResCost(true);
		int gold = u.getGoldCost();
		
		if(res >= nationGen.settings.get("resUpperTreshold"))
			res += nationGen.settings.get("resUpperTresholdChange");
		if(res <= nationGen.settings.get("resLowerTreshold"))
			res += nationGen.settings.get("resLowerTresholdChange");
		if(gold >= nationGen.settings.get("goldUpperTreshold"))
			gold += nationGen.settings.get("goldUpperTresholdChange");
		if(gold <= nationGen.settings.get("goldLowerTreshold"))
			gold += nationGen.settings.get("goldLowerTresholdChange");
		
		if(res >= nationGen.settings.get("resMultiTreshold"))
			res *= nationGen.settings.get("resMulti");
		
		int score = res + gold;
		double multi = nationGen.settings.get("militiaDivider") / score;
		double result = Math.round(multi * 10);
		result = result * nationGen.settings.get("militiaMultiplier");
		
		return (int)result;
		
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
			order = Generic.parseArgs("scouts commanders priests mages");
		
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
			if(str.equals("special"))
			{
				/*
				for(ShapeChangeUnit su : this.specialmonsters)
					if(!su.thisForm.tags.contains("caponly"))
						tw.println(line + " " + su.id);
				*/
			}
			else
			{
				for(int i = 1; i <= 10; i++)
				{
					if(listnames.contains(str + "-" + i))
					{
						for(Unit u : unitlists.get(str + "-" + i))
						{
							boolean foreign = false;
							for(String tag : foreigntags)
								if(u.tags.contains(tag))
								{
									if(coms)
									{
										tw.println("#" + tag.substring(0, tag.length() - 3) + "com " + u.id);
									}
									else
										tw.println("#" + tag+ " " + u.id);
									foreign = true;
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
							boolean foreign = false;
							for(String tag : foreigntags)
								if(u.tags.contains(tag))
								{
									if(coms)
										tw.println("#" + tag.substring(0, tag.length() - 3) + "com " + u.id);
									else
										tw.println("#" + tag+ " " + u.id);
									foreign = true;
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
		List<String> uniques = new ArrayList<String>();
		
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
						arg = arg.substring(1);
					
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
	

	
	

}
