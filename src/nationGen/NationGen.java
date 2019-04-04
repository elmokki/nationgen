package nationGen;

import com.elmokki.Dom3DB;
import nationGen.Settings.SettingsType;
import nationGen.entities.Filter;
import nationGen.entities.Race;
import nationGen.items.CustomItem;
import nationGen.items.Item;
import nationGen.magic.MagicPath;
import nationGen.magic.Spell;
import nationGen.misc.*;
import nationGen.naming.NameGenerator;
import nationGen.naming.NamingHandler;
import nationGen.naming.NationAdvancedSummarizer;
import nationGen.nation.Nation;
import nationGen.restrictions.NationRestriction;
import nationGen.units.ShapeChangeUnit;
import nationGen.units.ShapeShift;
import nationGen.units.Unit;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;



public class NationGen
{
	public static String version = "0.7.0-RC9";
	public static String date = "22nd of January 2019";
	
	private List<NationRestriction> restrictions;
	
	private NationGenAssets assets;
	
	public Dom3DB weapondb;
	public Dom3DB armordb;
	public Dom3DB units;
	public Dom3DB sites;
	
	public Settings settings;
	private CustomItemsHandler customItemsHandler;
	private IdHandler idHandler;
	
	public List<ShapeChangeUnit> forms = new ArrayList<>();
	private List<Spell> spellsToWrite = new ArrayList<>();
	private List<Spell> freeSpells = new ArrayList<>();
	
	private ReentrantLock pauseLock;
	private boolean shouldAbort = false;
	
	public NationGen()
	{
		// For versions of this that don't need pausing, simply create a dummy lock object to be used.
		this(new ReentrantLock(), new Settings(), new ArrayList<>());
	}
	
	public NationGen(ReentrantLock pauseLock, Settings settings, List<NationRestriction> restrictions)
	{
		this.pauseLock = pauseLock;
		this.settings = settings;
		this.restrictions = restrictions;
		
		//System.out.println("Dominions 4 NationGen version " + version + " (" + date + ")");
		//System.out.println("------------------------------------------------------------------");
		
		System.out.print("Loading Larzm42's Dom5 Mod Inspector database... ");
		loadDom3DB();
		System.out.println("done!");
		System.out.print("Loading definitions... ");
		customItemsHandler = new CustomItemsHandler(
				Item.readFile(this, "./data/items/customitems.txt", CustomItem.class), weapondb, armordb);
		assets = new NationGenAssets(this);
		assets.loadRaces("./data/races/races.txt", this); // ugh.  Looks like *somehow* assets is circularly depended in races.
		// Oh, it's totally because of the getassets method causing dependency shenanigans.
		
		System.out.println("done!");
		
		System.gc();
		//this.writeDebugInfo();
	}
	
	public long seed = 0;
	public String modname = "";
	public boolean manyseeds = false;
	
	public void generate(int amount)
	{
		Random random = new Random();
		generate(amount, random.nextLong(), null);
	}
	
	public void generate(int amount, long seed)
	{
		generate(amount, seed, null);
	}
	
	public void generate(List<Long> seeds)
	{
		Random random = new Random();
		generate(1, random.nextLong(), seeds);
	}
	
	private void generate(int amount, long seed, List<Long> seeds)
	{
		shouldAbort = false; // Don't abort before you even start.
		
		this.seed = seed;
		
		Random random = new Random(seed);
		
		// If there's a list of seeds.
		if(seeds != null && seeds.size() > 0)
		{
			manyseeds = true;
			amount = seeds.size();
			random = new Random(0);
		}
		
		// Start
		idHandler = new IdHandler();
		idHandler.loadFile("/forbidden_ids.txt");
		
		customItemsHandler.UpdateIDHandler(idHandler);
		
		if(!manyseeds)
		{
			System.out.println("Generating " + amount + " nations with seed " + seed + ".");
		}
		else
		{
			System.out.println("Generating " + amount + " nations with predefined seeds.");
			
			if(restrictions.size() > 0)
			{
				restrictions.clear();
				System.out.println("Ignoring nation restrictions due to predefined seeds.");
			}
		}
		
		System.out.println("Generating nations...");
		List<Nation> generatedNations = new ArrayList<>();
		Nation newnation = null;
		long newseed = 0;
		
		int count = 0;
		int failedcount = 0;
		int totalfailed = 0;
		boolean isDebug = settings.get(SettingsType.debug) == 1.0;
		
		while(generatedNations.size() < amount)
		{
			// Anyhow, a simple way to handle pausing is just to acquire a lock (which will be locked in the UI thread).
			pauseLock.lock();
			
			// Then, to avoid hanging the UI thread during generation, we immediately release the lock.
			pauseLock.unlock();
			
			// Check abort after unpausing so that if you pause and then abort, you don't generate a nation.
			if (shouldAbort)
			{
				totalfailed += failedcount;
				break;
			}
			
			count++;
			if(!manyseeds)
			{
				newseed = random.nextLong();
			}
			else
			{
				newseed = seeds.get(generatedNations.size());
			}
			
			if (isDebug)
			{
				System.out.print("- Generating nation " + (generatedNations.size() + 1) + "/" + amount + " (seed " + newseed);
				System.out.print(" / " + ZonedDateTime.now().toLocalTime().truncatedTo(ChronoUnit.SECONDS));
				System.out.print(")... ");
			}
			
			newnation = new Nation(this, newseed, count, restrictions, assets);
			
			if (newnation.passed)
			{
				if (restrictions.size() == 0)
				{
					System.out.format("- Successfully generated nation %d/%d (seed %d)\n",
							generatedNations.size() + 1, amount, newseed);
				}
				else
				{
					System.out.format("- After failing %d attempt(s), successfully generated nation %d/%d (seed %d)\n",
							failedcount, generatedNations.size() + 1, amount, newseed);
				}
				totalfailed += failedcount;
				failedcount = 0;
			}
			else
			{
				++failedcount;
				if (isDebug)
				{
					System.out.println("try " + failedcount + ", FAILED RESTRICTION " + newnation.restrictionFailed);
				}
				else if (failedcount % 250 == 0)
				{
					// This is honestly a workaround to showing progress without destroying the UI.
					// Ideally, there'd be a label that shows the current gen. But, I'm saving a UI
					// rewrite for a future date.
					System.out.format("- Failed to generate nation after %d attempts.\n", failedcount);
				}
				continue;
			}
			
			// Handle loose ends
			newnation.nationid = idHandler.nextNationId();
			polishNation(newnation);
			
			newnation.name = "Nation " + count;
			
			System.gc();
			
			generatedNations.add(newnation);
		}
		
		if (generatedNations.size() == 0)
		{
			System.out.format("Failed to generate any nations while having %d not pass restrictions.\n", totalfailed);
			return;
		}
		
		if(restrictions.size() > 0)
		{
			System.out.println("Total nations that did not pass restrictions: " + String.valueOf(totalfailed));
		}
		
		System.out.print("Giving ids");
		for(Nation n : generatedNations)
		{
			// units
			for(List<Unit> ul : n.unitlists.values())
			{
				for(Unit u : ul)
				{
					if(!u.invariantMonster)
					{
						u.id = idHandler.nextUnitId();
					}
					// Else the monster's ID was set in MonsterGen
				}
			}
			
			for(List<Unit> ul : n.comlists.values())
			{
				for(Unit u : ul)
				{
					u.id = idHandler.nextUnitId();
				}
			}
			
			for(Unit u : n.heroes)
			{
				u.id = idHandler.nextUnitId();
			}
			
			// sites
			for(Site s : n.sites)
			{
				s.id = idHandler.nextSiteId();
			}
			System.out.print(".");
		}
		
		System.out.println(" Done!");
		System.out.print("Naming things");
		
		NameGenerator nGen = new NameGenerator(this);
		NamingHandler nHandler = new NamingHandler(this, assets);
		for(Nation n : generatedNations)
		{
			n.name = nGen.generateNationName(n.races.get(0), n);
			n.nationalitysuffix = nGen.getNationalitySuffix(n, n.name);
			
			
			// troops
			nHandler.nameTroops(n);
			
			// sites
			for(Site s : n.sites)
			{
				s.name = nGen.getSiteName(n.random, s.getPath(), s.getSecondaryPath());
			}
			
			// mages
			nHandler.nameMages(n);
			
			// priests
			nHandler.namePriests(n);
			
			// sacreds and elites
			nHandler.nameSacreds(n);
			
			// Epithet
			nHandler.giveEpithet(n);
			
			// Unit descriptions
			nHandler.describeNation(n);
			
			// Summaries
			n.summary.update();
			
			System.out.print(".");
		}
		
		// Get mod name if not custom
		if(modname.equals(""))
		{
			if(generatedNations.size() > 1)
			{
				modname = nGen.getSiteName(generatedNations.get(0).random, MagicPath.fromInt(generatedNations.get(0).random.nextInt(8)), MagicPath.fromInt(generatedNations.get(0).random.nextInt(8)));
			}
			else
			{
				modname = generatedNations.get(0).name;
			}
		}
		
		System.out.println(" Done!");
		
		this.write(generatedNations);
		
		modname = "";
	}
	
	/**
	 * Loads data from Dom3DB
	 */
	private void loadDom3DB()
	{
		units = new Dom3DB("/db/units.csv");
		armordb = new Dom3DB("/db/armor.csv");
		weapondb = new Dom3DB("/db/weapon.csv");
		sites = new Dom3DB("/db/sites.csv");
	}
	
	/**
	 * Handles spells
	 * @param spells
	 * @param n
	 */
	private void handleSpells(List<Filter> spells, Nation n)
	{
		int id = n.nationid;
		
		for(String s : n.getSpells())
		{
			Spell spell = null;
			
			// check for existing free spell
			for(Spell sp : this.freeSpells)
			{
				if(sp.name.equals(s))
				{
					spell = sp;
				}
			}
			// create a new spell
			
			// check for custom spells first
			if(spell == null)
			{
				for(Filter sf : assets.customspells)
				{
					if(sf.name.equals(s))
					{
						spell = new Spell(this);
						spell.name = s;
						spell.commands.addAll(sf.commands);
						break;
					}
				}
			}
			// copy existing spell
			if(spell == null)
			{
				spell = new Spell(this);
				spell.name = s;
				spell.commands.add(Command.args("#copyspell", s));
				spell.commands.add(Command.args("#name", s));
			}
			
			spell.nationids.add(id);
			
			// Handle existence in the list of spells with free space
			if(!this.freeSpells.contains(spell))
			{
				this.freeSpells.add(spell);
			}
			
			if(spell.nationids.size() >= settings.get(SettingsType.maxrestrictedperspell))
			{
				this.freeSpells.remove(spell);
			}
			
			// Add to spells to write
			if(!this.spellsToWrite.contains(spell))
			{
				this.spellsToWrite.add(spell);
			}
		}
	}
	
	public void writeDebugInfo()
	{
		double total = 0;
		for(Race r : assets.races)
		{
			if(!r.tags.containsName("secondary"))
			{
				total += r.basechance;
			}
		}
		
		for(Race r : assets.races)
		{
			if(!r.tags.containsName("secondary"))
			{
				System.out.println(r.name + ": " + (r.basechance / total));
			}
		}
	}
	
	private void writeDescriptions(List<Nation> nations, String modDirectory)
	{
		NationAdvancedSummarizer nDesc = new NationAdvancedSummarizer(armordb, weapondb);
		if(settings.get(SettingsType.advancedDescs) == 1.0)
		{
			nDesc.writeAdvancedDescriptionFile(nations, modDirectory);
		}
		if(settings.get(SettingsType.basicDescs) == 1.0)
		{
			nDesc.writeDescriptionFile(nations, modDirectory);
		}
	}
	
	private void drawPreviews(List<Nation> nations, String dir)
	{
		if(settings.get(SettingsType.drawPreview) == 1)
		{
			System.out.print("Drawing previews");
			PreviewGenerator pGen = new PreviewGenerator();
			for(Nation n : nations)
			{
				pGen.savePreview(n, "./mods/" + dir + "/preview_" + n.nationid + "_" + n.name.toLowerCase().replaceAll(" ", "_") + ".png");
				System.out.print(".");
			}
			System.out.println(" Done!");
		}
	}
	
	// TODO: Support saving anywhere
	public void write(List<Nation> nations)
	{
		String modDirectory = "nationgen_" + this.modname.replaceAll(" ", "_").toLowerCase();
		
		String modFilename = modDirectory + ".dm";
		// nation.name.toLowerCase().replaceAll(" ", "_")
		
		FileUtil.createDirectory("/mods/" + modDirectory);
		
		// Descriptions
		writeDescriptions(nations, modDirectory);
		
		// Mod file
		FileUtil.writeLines("/mods/" + modFilename, writeModLines(nations, modDirectory));
		
		// Images
		BufferedImage modBanner = generateBanner(nations.get(0).colors[0], this.modname, nations.get(0).flag);
		FileUtil.writeTGA(modBanner, "/mods/" + modDirectory + "/banner.tga");
		for(Nation nation : nations)
		{
			String nationDirectory = getNationDirectory(modDirectory, nation);
			FileUtil.createDirectory("/mods/" + nationDirectory);
			// Flag
			FileUtil.writeTGA(nation.flag, "/mods/" + nationDirectory + "/flag.tga");
			
			nation.writeSprites(nationDirectory);
		}
		
		
		// Draw previews
		drawPreviews(nations, modDirectory);
		
		// Displays mage names
//		System.out.println();
//		for(Nation n : nations)
//		{
//			List<Unit> mages = n.generateComList("mage");
//			List<String> mnames = new ArrayList<String>();
//			for(Unit u : mages)
//					mnames.add(u.name.toString());
//			System.out.println("* " + Generic.listToString(mnames, ","));
//		}
//		System.out.println();
		
		System.out.println("------------------------------------------------------------------");
		System.out.println("Finished writing " + nations.size() + " nations to file " + modFilename + "!");
		
	}
	
	private List<String> writeModLines(List<Nation> nations, String modDirectory)
	{
		List<String> lines = new ArrayList<>();
		// Description!
		lines.add("-- NationGen - " + modDirectory);
		lines.add("-----------------------------------");
		
		lines.add("-- Generated with version " + version + ".");
		lines.add("-- Generation setting code: " + settings.getSettingInteger());
		
		if(!manyseeds)
		{
			lines.add("-- Nation seeds generated with seed " + this.seed + ".");
		}
		else
		{
			lines.add("-- Nation seeds specified by user.");
		}
		
		for(Nation n : nations)
		{
			lines.add("-- Nation " + n.nationid + ": " + n.name + " generated with seed " + n.getSeed());
		}
		lines.add("-----------------------------------");
		lines.add("");
		
		// Actual mod definition
		lines.add("#modname \"NationGen - " + this.modname + "\"");
		lines.add("#description \"A NationGen generated nation!\"");
		
		// Banner!
		lines.add("#icon \"" + modDirectory + "/banner.tga\"");
		lines.add("");
		
		// Write items!
		// This is a relic from Dom3 version, but oh well.
		System.out.print("Writing items and spells");
		lines.addAll(customItemsHandler.writeCustomItemsLines());
		lines.addAll(writeSpellsLines());
		
		System.out.print(".".repeat(nations.size()));
		System.out.println(" Done!");
		
		// Write units!
		System.out.print("Writing units");
		for(Nation nation : nations)
		{
			// Unit definitions
			lines.addAll(nation.writeUnitsLines(getNationDirectory(modDirectory, nation)));
			System.out.print(".");
		}
		System.out.println(" Done!");
		
		// Write sites!
		System.out.print("Writing sites");
		
		for(Nation nation : nations)
		{
			// Site definitions
			lines.addAll(nation.writeSitesLines());
			System.out.print(".");
		}
		System.out.println(" Done!");
		
		// Write nation definitions!
		System.out.print("Writing nations");
		for(Nation nation : nations)
		{
			// Nation definitions
			lines.addAll(nation.writeLines(getNationDirectory(modDirectory, nation)));
			System.out.print(".");
		}
		System.out.println(" Done!");
		
		if(settings.get(SettingsType.hidevanillanations) == 1)
		{
			lines.addAll(writeHideVanillaNationsLines(nations.size()));
		}
		
		return lines;
	}
	
	private String getNationDirectory(String modDirectory, Nation nation) {
		return modDirectory + "/" + nation.nationid + "-" + nation.name.toLowerCase().replaceAll(" ", "_");
	}
	
	private List<String> writeHideVanillaNationsLines(int nationcount)
	{
		System.out.print("Hiding vanilla nations... ");
		
		List<String> lines = new ArrayList<>();
		lines.add("-- Hiding vanilla nations");
		lines.add("-----------------------------------");
		
		if(nationcount > 1)
		{
			lines.add("#disableoldnations");
			lines.add("");
			System.out.println(" Done!");
		}
		else
		{
			System.out.println("Unable to hide vanilla nations with only one random nation!");
		}
		
		return lines;
	}
	
	private List<String> writeSpellsLines()
	{
		List<String> lines = new ArrayList<>();
		if(!spellsToWrite.isEmpty())
		{
			
			lines.add("--- Spells:");
			for(Spell s : this.spellsToWrite)
			{
				lines.add("#newspell");
				for(Command c : s.commands)
				{
					lines.add(c.toModLine());
				}
				for(int id : s.nationids)
				{
					lines.add("#restricted " + id);
				}
				lines.add("#end");
				lines.add("");
			}
		}
		return lines;
	}
	
	public boolean hasShapeShift(Arg id)
	{
		if ("".equals(id.get())) {
			return false;
		}
		
		int realid = id.isNumeric() ? id.getInt() : -1;
		
		return this.forms.stream().anyMatch(scu -> scu.id == realid);
	}
	
	private void polishNation(Nation n)
	{
		n.finalizeUnits();
		handleShapeshifts(n);
		handleSpells(n.spells, n);
	}
	
	private void handleShapeshifts(Nation n)
	{
		
		List<Unit> shapeshiftUnits = n.generateUnitList();
		shapeshiftUnits.addAll(n.heroes);
		List<ShapeChangeUnit> sul = new ArrayList<>();
		
		for(Unit u : shapeshiftUnits)
		{
			for(Command c : u.commands)
			{
				if(c.command.contains("shape") && !hasShapeShift(c.args.get(0)))
				{
					if(c.command.equals("#firstshape") && u.tags.containsName("montagunit"))
					{
						handleMontag(c);
					}
					else
					{
						handleShapeshift(c, u);
					}
				}
				else if(c.command.equals("#montag"))
				{
					handleMontag(c);
				}
			}
		}
		
		for(ShapeChangeUnit su : forms)
		{
			if(shapeshiftUnits.contains(su.otherForm))
			{
				sul.add(su);
			}
		}
		
		for(ShapeChangeUnit su : sul)
		{
			su.polish(this, n);
			
			// Replace command
			for(Command c : su.thisForm.commands)
			{
				// Weapons
				if(c.command.equals("#weapon"))
				{
					Arg realarg = c.args.get(0);
					
					if (!realarg.isNumeric()) {
						c.args.set(0, new Arg(customItemsHandler.getCustomItemId(realarg.get())));
					}
				}
			}
		}
	}
	
	private HashMap<String, Integer> montagmap = new HashMap<>();
	private void handleMontag(Command c)
	{
		Integer montag = montagmap.computeIfAbsent(c.args.get(0).get(), k -> idHandler.nextMontagId());
		
		if(c.command.equals("#firstshape"))
		{
			c.args.set(0, new Arg("-" + montag));
		}
		else if(c.command.equals("#montag"))
		{
			c.args.set(0, new Arg("" + montag));
		}
	}
	
	private void handleShapeshift(Command c, Unit u)
	{
		
		ShapeShift shift = assets.secondshapes.stream()
				.filter(s -> s.name.equals(c.args.get(0).get()))
				.findFirst().orElseThrow(
					() -> new IllegalArgumentException("Shapeshift named " + c.args.get(0) + " could not be found."));
		
		ShapeChangeUnit su = new ShapeChangeUnit(this, assets, u.race, u.pose, u, shift);
		
		su.id = idHandler.nextUnitId();
		
		switch (c.command)
		{
			case "#shapechange":
				su.shiftcommand = "#shapechange";
				break;
			case "#secondshape":
				su.shiftcommand = "#firstshape";
				break;
			case "#firstshape":
				su.shiftcommand = "#secondshape";
				break;
			case "#secondtmpshape":
				su.shiftcommand = "";
				break;
			case "#landshape":
				su.shiftcommand = "#watershape";
				break;
			case "#watershape":
				su.shiftcommand = "#landshape";
				break;
			case "#forestshape":
				su.shiftcommand = "#plainshape";
				break;
			case "#plainshape":
				su.shiftcommand = "#forestshape";
				break;
			default:
				break;
		}
		
		c.args.set(0, new Arg(su.id));
		forms.add(su);
	}
	
	public static BufferedImage generateBanner(Color c, String name, BufferedImage flag)
	{
		BufferedImage combined = new BufferedImage(256, 64, BufferedImage.TYPE_INT_RGB);
		Graphics g = combined.getGraphics();
		
		g.setColor(new Color(0, 0, 0));
		g.fillRect(0, 0, 256, 64);
		g.drawImage(flag, 0, -4, 64, 64, null);
		
		g.setColor(Color.DARK_GRAY);
		
		Font f = g.getFont();
		Font d = f.deriveFont(18f);
		f = f.deriveFont(24f);
		g.setFont(d);
		
		g.drawString("NationGen " + version + ":", 64, 18);
		
		g.setFont(f);
		g.drawString(name, 64, 48);
		
		return combined;
	}
	
	/**
	 * Copies any poses from each race's spriteGenPoses list into its poses list
	 */
	public void setSpriteGenPoses()
	{
		for(Race race : assets.races)
		{
			race.poses.addAll(race.spriteGenPoses);
		}
	}
	
	/**
	 * Aborts the nation generating process. This var is reset when the next set starts to generate.
	 */
	public void abortNationGeneration()
	{
		shouldAbort = true;
	}
	
	public CustomItemsHandler GetCustomItemsHandler()
	{
		return customItemsHandler;
	}
	
	/**
	 * This function *will* be refactored out.  But, race is a pain to do only half measured refactorings.
	 * Eventually, entity/filter will need to be redone.
	 * @return
	 */
	public NationGenAssets getAssets()
	{
		return assets;
	}
}
