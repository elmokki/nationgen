package nationGen;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

import com.elmokki.Dom3DB;
import com.elmokki.Drawing;
import com.elmokki.Generic;

import nationGen.entities.Entity;
import nationGen.entities.Filter;
import nationGen.entities.Flag;
import nationGen.entities.MagicItem;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.entities.Theme;
import nationGen.items.CustomItem;
import nationGen.items.Item;
import nationGen.magic.MagicPattern;
import nationGen.magic.Spell;
import nationGen.misc.Command;
import nationGen.misc.PreviewGenerator;
import nationGen.misc.ResourceStorage;
import nationGen.misc.Site;
import nationGen.naming.NamePart;
import nationGen.naming.NameGenerator;
import nationGen.naming.NamingHandler;
import nationGen.naming.NationAdvancedSummarizer;
import nationGen.nation.Nation;
import nationGen.restrictions.NationRestriction;
import nationGen.units.ShapeChangeUnit;
import nationGen.units.ShapeShift;
import nationGen.units.Unit;



public class NationGen 
{
    public static String version = "0.7.0-RC7";
    public static String date = "6th of May 2018";

    public List<NationRestriction> restrictions = new ArrayList<>();

    private NationGenAssets assets;
    public ResourceStorage<Filter> filters = new ResourceStorage<>(Filter.class, this);
    
    public List<String> secondShapeMountCommands = new ArrayList<>();
    public List<String> secondShapeNonMountCommands = new ArrayList<>();
    public List<String> secondShapeRacePoseCommands = new ArrayList<>();

    public Dom3DB weapondb;
    public Dom3DB armordb;
    public Dom3DB units;
    public Dom3DB sites;

    public Settings settings;
    private CustomItemsHandler customItemsHandler;
    public List<ShapeShift> secondshapes = new ArrayList<>();
    public List<Race> races = new ArrayList<>();
    public IdHandler idHandler;

    public List<ShapeChangeUnit> forms = new ArrayList<>();
    public List<Spell> spellsToWrite = new ArrayList<>();
    public List<Spell> freeSpells = new ArrayList<>();

    private ReentrantLock pauseLock;
    private boolean shouldAbort = false;
    
    public NationGen() throws IOException
    {
        // For versions of this that don't need pausing, simply create a dummy lock object to be used.
        this(new ReentrantLock());
    }
    
    public NationGen(ReentrantLock pauseLock) throws IOException
    {
        this.pauseLock = pauseLock;
        //System.out.println("Dominions 4 NationGen version " + version + " (" + date + ")");
        //System.out.println("------------------------------------------------------------------");

        System.out.print("Loading settings... ");
        settings = new Settings();
        System.out.println("done!");
                
        try 
        {
            System.out.print("Loading Larzm42's Dom5 Mod Inspector database... ");
            loadDom3DB();
            System.out.println("done!");
            System.out.print("Loading definitions... ");
            customItemsHandler = new CustomItemsHandler(
                    Item.readFile(this, "./data/items/customitems.txt", CustomItem.class), weapondb, armordb);
            assets = new NationGenAssets(this);
            filters.load("./data/filters/filters.txt");
            loadRaces("./data/races/races.txt");
            secondshapes = Entity.readFile(this, "./data/shapes/secondshapes.txt", ShapeShift.class);
            loadSecondShapeInheritance("/data/shapes/secondshapeinheritance.txt");

            System.out.println("done!");
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            System.out.println("Error loading file " + e.getMessage());
        }

        System.gc();
        //this.writeDebugInfo();
    }
	
    public long seed = 0;
    public String modname = "";
    public boolean manyseeds = false;

    public void generate(int amount) throws IOException
    {
        Random random = new Random();
        generate(amount, random.nextLong(), null);
    }
    
    public void generate(int amount, long seed) throws IOException
    {
        generate(amount, seed, null);
    }
    
    public void generate(List<Long> seeds) throws IOException
    {
        Random random = new Random();
        generate(1, random.nextLong(), seeds);
    }
	
    private void generate(int amount, long seed, List<Long> seeds) throws IOException
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
        idHandler.loadFile("forbidden_ids.txt");
        
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
        boolean isDebug = settings.get("debug") == 1.0;
        
        while(generatedNations.size() < amount)
        {  
            // Anyhow, a simple way to handle pausing is just to acquire a lock (which will be locked in the UI thread).
            pauseLock.lock();
            
            // Then, to avoid hanging the UI thread during generation, we immediately release the lock.
            pauseLock.unlock();
            
            // Check abort after unpausing so that if you pause and then abort, you don't generate a nation.
            if (shouldAbort)
            {
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
                modname = nGen.getSiteName(generatedNations.get(0).random, generatedNations.get(0).random.nextInt(8), generatedNations.get(0).random.nextInt(8));
            }
            else
            {
                modname = generatedNations.get(0).name;
            }
        }

        System.out.println(" Done!");

        String filename = modname.replaceAll(" ", "_").toLowerCase();
        try 
        {
            this.write(generatedNations, filename);
        } 
        catch (IOException e) 
        {
            System.out.println("Error writing mod: " + e.getMessage());
        }

        System.out.println("------------------------------------------------------------------");
        System.out.println("Finished generating " + generatedNations.size() + " nations to file nationgen_" + filename + ".dm!");

        modname = "";
    }

    /**
     * Loads data from Dom3DB
     */
    private void loadDom3DB() throws Exception
    {
            units = new Dom3DB("units.csv");
            armordb = new Dom3DB("armor.csv");
            weapondb = new Dom3DB("weapon.csv");
            sites = new Dom3DB("sites.csv");
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
                spell.commands.add(new Command("#copyspell", "\"" + s + "\""));
                spell.commands.add(new Command("#name", "\"" + s + " \""));
            }

            spell.nationids.add(id);

            // Handle existence in the list of spells with free space
            if(!this.freeSpells.contains(spell))
            {
                this.freeSpells.add(spell);
            }

            if(spell.nationids.size() >= settings.get("maxrestrictedperspell"))
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

    private void loadRaces(String file) throws IOException
    {
        FileInputStream fstream = new FileInputStream(file);
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        String strLine;

        while ((strLine = br.readLine()) != null)   
        {
            List<String> args = Generic.parseArgs(strLine);
            if(args.isEmpty())
            {
                continue;
            }

            if(args.get(0).equals("#load"))
            {
                List<Race> items = new ArrayList<>();
                items.addAll(Item.readFile(this, args.get(1), Race.class));
                races.addAll(items);
            }
        }
        in.close();
    }
	
    public void writeDebugInfo()
    { 
        double total = 0;
        for(Race r : races)
        {
            if(!r.tags.contains("secondary"))
            {
                total += r.basechance;
            }
        }

        for(Race r : races)
        {
            if(!r.tags.contains("secondary"))
            {
                System.out.println(r.name + ": " + (r.basechance / total));
            }
        }
    }

    private void writeDescriptions(PrintWriter tw, List<Nation> nations, String modname) throws IOException
    {
        NationAdvancedSummarizer nDesc = new NationAdvancedSummarizer(armordb, weapondb);
        if(settings.get("advancedDescs") == 1.0)
        {
            nDesc.writeAdvancedDescriptionFile(nations, modname);
        }
        if(settings.get("basicDescs") == 1.0)
        {
            nDesc.writeDescriptionFile(nations, modname);
        }   
    }
	
    private void drawPreviews(List<Nation> nations, String dir) throws IOException
    {
        if(settings.get("drawPreview") == 1)
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
	
    public void write(List<Nation> nations, String modname) throws IOException
    {
        String dir = "nationgen_" + modname.toLowerCase().replaceAll(" ", "_") + "/"; // nation.name.toLowerCase().replaceAll(" ", "_")
        new File("./mods/" + dir).mkdirs();
        
        FileWriter fstream = new FileWriter("./mods/nationgen_" + modname.toLowerCase().replaceAll(" ", "_") + ".dm");
        PrintWriter tw = new PrintWriter(fstream, true);

        // Descriptions
        writeDescriptions(tw, nations, modname);

        // Description!
        tw.println("-- NationGen - " + modname);
        tw.println("-----------------------------------");

        tw.println("-- Generated with version " + version + ".");
        tw.println("-- Generation setting code: " + settings.getSettingInteger());

        if(!manyseeds)
        {
            tw.println("-- Nation seeds generated with seed " + this.seed + ".");
        }
        else
        {
            tw.println("-- Nation seeds specified by user.");
        }

        for(Nation n : nations)
        {
            tw.println("-- Nation " + n.nationid + ": " + n.name + " generated with seed " + n.getSeed());
        }
        tw.println("-----------------------------------");
        tw.println();

        // Actual mod definition
        tw.println("#modname \"NationGen - " + this.modname + "\"");
        tw.println("#description \"A NationGen generated nation!\"");
        
        // Banner!
        generateBanner(nations.get(0).colors[0], this.modname, dir + "/banner.tga", nations.get(0).flag);
        tw.println("#icon \"" + dir + "banner.tga\"");
        tw.println("");
        
        // Write items!
        // This is a relic from Dom3 version, but oh well.
        System.out.print("Writing items and spells");
        customItemsHandler.writeCustomItems(tw);
        this.writeSpells(tw);
        for (Nation nation : nations) 
        {
            System.out.print(".");
        }
        System.out.println(" Done!");
	
        // Write units!
        System.out.print("Writing units");
        for(Nation nation : nations)
        {
            new File("./mods/" + dir + "/" + nation.nationid + "-" + nation.name.toLowerCase().replaceAll(" ", "_") + "/").mkdir();

            // Unit definitions
            nation.writeUnits(tw, dir + "/" + nation.nationid + "-" + nation.name.toLowerCase().replaceAll(" ", "_") + "/");
            System.out.print(".");
        }
        System.out.println(" Done!");

        // Write sites!
        System.out.print("Writing sites");
        
        for(Nation nation : nations)
        {
            // Site definitions
            nation.writeSites(tw);
            System.out.print(".");
        }
        System.out.println(" Done!");
      
        // Write nation definitions!
        System.out.print("Writing nations");
        for(Nation nation : nations)
        {
            // Flag
            Drawing.writeTGA(nation.flag, "mods/" + dir + "/" + nation.nationid + "-" + nation.name.toLowerCase().replaceAll(" ", "_") + "/flag.tga");

            // Nation definitions
            nation.write(tw, dir + "/" + nation.nationid + "-" + nation.name.toLowerCase().replaceAll(" ", "_") + "/");
            System.out.print(".");
        }
        System.out.println(" Done!");
        
        // Draw previews
        drawPreviews(nations, dir);
        
        if(settings.get("hidevanillanations") == 1)
        {
            hideVanillaNations(tw, nations.size());
        }
		
        tw.flush();
        tw.close();
        fstream.close();
        
        // Displays mage names
        /*
        System.out.println();
        for(Nation n : nations)
        {
            List<Unit> mages = n.generateComList("mage");
            List<String> mnames = new ArrayList<String>();
            for(Unit u : mages)
                    mnames.add(u.name.toString());
            System.out.println("* " + Generic.listToString(mnames, ","));
        }
        System.out.println();
       */
    }
	
    private void hideVanillaNations(PrintWriter tw, int nationcount)
    {
        System.out.print("Hiding vanilla nations... ");
        tw.println("-- Hiding vanilla nations");
        tw.println("-----------------------------------");

        if(nationcount > 1)
        {
            tw.println("#disableoldnations");
            tw.println();
            System.out.println(" Done!");
        }
        else
        {
            System.out.println("Unable to hide vanilla nations with only one random nation!");
        }
    }
	
    private void writeSpells(PrintWriter tw)
    {
        if(spellsToWrite.isEmpty())
        {
            return;
        }

        tw.println("--- Spells:");
        for(Spell s : this.spellsToWrite)
        {	
            tw.println("#newspell");
            for(Command c : s.commands)
            {
                tw.println(c);
            }
            for(int id : s.nationids)
            {
                tw.println("#restricted " + id);
            }
            tw.println("#end");
            tw.println();
        }
    }
    
    public boolean hasShapeShift(String id)
    {
        int realid = -1;
        if (id.isEmpty()) 
        {
            return false;
        }
        else
        {
            boolean isNumeric = id.chars().allMatch( Character::isDigit );
            if (isNumeric) 
            {
                realid = Integer.parseInt(id);    
            }
        }

        for(ShapeChangeUnit su : this.forms)
        {
            if(su.id == realid)
            {
                return true;
            }
        }
        return false;
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
                    if((c.command.equals("#firstshape") && u.tags.contains("montagunit")))
                    {
                        handleMontag(c, u, shapeshiftUnits);
                    }
                    else
                    {
                        handleShapeshift(c, u);
                    }
                }
                else if(c.command.equals("#montag"))
                {
                    handleMontag(c, u, shapeshiftUnits);
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
                    String realarg = c.args.get(0);
                    if(realarg.contains(" "))
                    {
                        realarg = realarg.split(" ")[0];
                    }
                            
                    if (realarg.isEmpty()) 
                    {
                        c.args.set(0, customItemsHandler.getCustomItemId(c.args.get(0)) + "");
                    }
                    else
                    {
                        boolean isNumeric = realarg.chars().allMatch( Character::isDigit );
                        if (isNumeric) 
                        {
                            Integer.parseInt(realarg);
                        }
                    }
                }
            }
        }
    }
	
    private HashMap<String, Integer> montagmap = new HashMap<>();
    private void handleMontag(Command c, Unit u, List<Unit> units)
    {
        Integer montag = montagmap.get(c.args.get(0));
        if(montag == null)
        {
            montag = idHandler.nextMontagId();
            montagmap.put(c.args.get(0), montag);
            // System.out.println("Added "  + montag + " for " + c.args.get(0));
        }

        if(c.command.equals("#firstshape"))
        {
            c.args.set(0, "-" + montag);
        }
        else if(c.command.equals("#montag"))
        {
            c.args.set(0, "" + montag);
        }
    }
	
    private void handleShapeshift(Command c, Unit u)
    {		
        ShapeShift shift;
        shift = null;
        
        for(ShapeShift s : secondshapes)
        {
            if(s.name.equals(c.args.get(0)))
            {
                shift = s;
                break;
            }
        }
		
        if(shift == null)
        {
            System.out.println("Shapeshift named " + c.args.get(0) + " could not be found.");
            return;
        }
        ShapeChangeUnit su = new ShapeChangeUnit(this, u.race, u.pose, u, shift);

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

        c.args.set(0, "" + su.id);
        forms.add(su);
    }
	
    /**
     * Loads the list of commands that second shapes should inherit from the primary shape
     * @param filename
     * @return
     */
    public int loadSecondShapeInheritance(String filename)
    {
        int amount = 0;
		
        Scanner file;
		
        try 
        {
            file = new Scanner(new FileInputStream(System.getProperty("user.dir") + "/" + filename));
        } 
        catch (FileNotFoundException e) 
        {
            e.printStackTrace();
            return 0;
        }

        while(file.hasNextLine())
        {
            String line = file.nextLine();
            if(line.startsWith("-"))
            {
                continue; 
            }

            List<String> args = Generic.parseArgs(line);
            if(args.isEmpty())
            {
                continue;
            }

            if(args.get(0).equals("all") && args.size() > 0)
            {
                secondShapeMountCommands.add(args.get(1));
                secondShapeNonMountCommands.add(args.get(1));
                amount++;
            }
            else if(args.get(0).equals("mount") && args.size() > 0)
            {
                secondShapeMountCommands.add(args.get(1));
                amount++;
            }
            else if(args.get(0).equals("nonmount") && args.size() > 0)
            {
                secondShapeNonMountCommands.add(args.get(1));
                amount++;
            }
            else if(args.get(0).equals("racepose") && args.size() > 0)
            {
                secondShapeRacePoseCommands.add(args.get(1));
                amount++;
            }
        }  
        file.close();
        return amount;
    }

    public static void generateBanner(Color c, String name, String output, BufferedImage flag) throws IOException
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
        
        Drawing.writeTGA(combined, "./mods/" + output);
    }
	
    /**
     * Copies any poses from each race's spriteGenPoses list into its poses list
     * @param filename
     * @return
     */
    public void setSpriteGenPoses()
    {
        for(Race race : races)
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
