package nationGen;

import com.elmokki.NationGenDB;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import nationGen.Settings.SettingsType;
import nationGen.entities.Filter;
import nationGen.entities.Race;
import nationGen.ids.CustomItemsHandler;
import nationGen.ids.IdHandler;
import nationGen.magic.MagicPath;
import nationGen.magic.Spell;
import nationGen.misc.*;
import nationGen.naming.NameGenerator;
import nationGen.naming.NamingHandler;
import nationGen.naming.NationAdvancedSummarizer;
import nationGen.nation.Nation;
import nationGen.restrictions.NationRestriction;
import nationGen.units.MountUnit;
import nationGen.units.ShapeChangeUnit;
import nationGen.units.ShapeShift;
import nationGen.units.Unit;

public class NationGen {

  public static final String version = "0.13.6";
  public static final String date = "3rd May 2026";
  public static final String appPropertiesPath = "/app.properties";
  private static Properties appProperties;

  private List<NationRestriction> restrictions;

  private NationGenAssets assets;

  public NationGenDB weapondb;
  public NationGenDB armordb;
  public NationGenDB units;
  public NationGenDB sites;

  public long seed = 0;
  public String modname = "";
  public boolean manyseeds = false;

  public Settings settings;
  private CustomItemsHandler customItemsHandler;
  private IdHandler idHandler;

  public List<ShapeChangeUnit> forms = new ArrayList<>();
  private List<Spell> spellsToWrite = new ArrayList<>();
  private List<Spell> freeSpells = new ArrayList<>();

  private ReentrantLock pauseLock;
  private boolean shouldAbort = false;
  private static boolean isDebug = false;

  private Instant start;

  public NationGen() {
    // For versions of this that don't need pausing, simply create a dummy lock object to be used.
    this(new ReentrantLock(), new Settings(), new ArrayList<>());
    NationGen.appProperties = FileUtil.readProperties(NationGen.appPropertiesPath);
  }

  public NationGen(
    ReentrantLock pauseLock,
    Settings settings,
    List<NationRestriction> restrictions
  ) {
    this.pauseLock = pauseLock;
    this.settings = settings;
    this.restrictions = restrictions;
    NationGen.isDebug = settings.get(SettingsType.debug) == 1;

    //System.out.println("Dominions 4 NationGen version " + version + " (" + date + ")");
    //System.out.println("------------------------------------------------------------------");

    this.start = Instant.now();

    System.out.print("Loading Larzm42's Dom6 Mod Inspector database... ");
    loadNationGenDB();
    System.out.println("done!");
    System.out.print("Loading definitions... ");
    customItemsHandler = new CustomItemsHandler(this, weapondb, armordb);
    assets = new NationGenAssets();
    assets.load(this);
    //		assets.loadRaces("./data/races/races.txt", this); // ugh.  Looks like *somehow* assets is circularly depended in races.
    // Oh, it's totally because of the getassets method causing dependency shenanigans.

    System.out.println("done!");

    System.gc();
    //this.writeDebugInfo();
  }

  public void generate(int amount) {
    Random random = new Random();
    generate(amount, random.nextLong(), null);
  }

  public void generate(int amount, long seed) {
    generate(amount, seed, null);
  }

  public void generate(List<Long> seeds) {
    Random random = new Random();
    generate(1, random.nextLong(), seeds);
  }

  private void generate(int amount, long seed, List<Long> seeds) {
    shouldAbort = false; // Don't abort before you even start.

    this.seed = seed;

    Random random = new Random(seed);

    // If there's a list of seeds.
    if (seeds != null && seeds.size() > 0) {
      manyseeds = true;
      amount = seeds.size();
      random = new Random(0);
    }

    // Start
    idHandler = new IdHandler();
    customItemsHandler.UpdateIDHandler(idHandler);

    if (!manyseeds) {
      System.out.println(
        "Generating " + amount + " nations with seed " + seed + "."
      );
    } else {
      System.out.println(
        "Generating " + amount + " nations with predefined seeds."
      );

      if (restrictions.size() > 0) {
        restrictions.clear();
        System.out.println(
          "Ignoring nation restrictions due to predefined seeds."
        );
      }
    }

    System.out.println("Generating nations...");
    List<Nation> generatedNations = new ArrayList<>();

    int count = 0;
    int failedcount = 0;
    int totalfailed = 0;

    while (generatedNations.size() < amount) {
      // Anyhow, a simple way to handle pausing is just to acquire a lock (which will be locked in the UI thread).
      pauseLock.lock();

      // Then, to avoid hanging the UI thread during generation, we immediately release the lock.
      pauseLock.unlock();

      // Check abort after unpausing so that if you pause and then abort, you don't generate a nation.
      if (shouldAbort) {
        totalfailed += failedcount;
        break;
      }

      count++;
      long newseed = this.manyseeds
        ? seeds.get(generatedNations.size())
        : random.nextLong();

      if (NationGen.isInDebugMode()) {
        System.out.print(
          "- Generating nation " +
          (generatedNations.size() + 1) +
          "/" +
          amount +
          " (seed " +
          newseed
        );
        System.out.print(
          " / " +
          ZonedDateTime.now().toLocalTime().truncatedTo(ChronoUnit.SECONDS)
        );
        System.out.print(")... ");
      }

      Nation newnation;
      try {
        newnation = new Nation(this, newseed, count, restrictions, assets);
      } catch (Exception e) {
        throw new IllegalStateException(
          "Error generating nation with seed: " + newseed,
          e
        );
      }

      if (newnation.passed) {
        if (restrictions.size() == 0) {
          System.out.format(
            "- Successfully generated nation %d/%d (seed %d)\n",
            generatedNations.size() + 1,
            amount,
            newseed
          );
        } else {
          System.out.format(
            "- After failing %d attempt(s), successfully generated nation %d/%d (seed %d)\n",
            failedcount,
            generatedNations.size() + 1,
            amount,
            newseed
          );
        }
        totalfailed += failedcount;
        failedcount = 0;
      } else {
        ++failedcount;
        if (isDebug) {
          System.out.println(
            "try " +
            failedcount +
            ", FAILED RESTRICTION " +
            newnation.restrictionFailed
          );
        } else if (failedcount % 250 == 0) {
          // This is honestly a workaround to showing progress without destroying the UI.
          // Ideally, there'd be a label that shows the current gen. But, I'm saving a UI
          // rewrite for a future date.
          System.out.format(
            "- Failed to generate nation after %d attempts.\n",
            failedcount
          );
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

    if (generatedNations.size() == 0) {
      System.out.format(
        "Failed to generate any nations while having %d not pass restrictions.\n",
        totalfailed
      );
      return;
    }

    if (restrictions.size() > 0) {
      System.out.println(
        "Total nations that did not pass restrictions: " +
        String.valueOf(totalfailed)
      );
    }

    System.out.print("Giving ids");

    for (Nation n : generatedNations) {
      // units
      for (List<Unit> ul : n.unitlists.values()) {
        for (Unit u : ul) {
          if (!u.invariantMonster) {
            u.resolveId();
          }
          // Else the monster's ID was set in MonsterGen
        }
      }

      for (List<Unit> ul : n.comlists.values()) {
        for (Unit u : ul) {
          u.resolveId();
        }
      }

      for (Unit u : n.heroes) {
        u.resolveId();
      }

      // sites
      for (Site s : n.sites) {
        s.id = idHandler.nextSiteId();
      }
      System.out.print(".");
    }

    System.out.println(" Done!");
    System.out.print("Naming things");

    NameGenerator nGen = new NameGenerator(this);
    NamingHandler nHandler = new NamingHandler(this, assets);
    for (Nation n : generatedNations) {
      n.name = nGen.generateNationName(n.races.get(0), n);
      n.nationalitysuffix = nGen.getNationalitySuffix(n, n.name);

      // troops
      nHandler.nameTroops(n);

      // sites
      for (Site s : n.sites) {
        if (s.selectSiteName == null) {
          s.name = nGen.getSiteName(n.random, s.getPath(), s.getSecondaryPath());
        }
      }

      // mages
      nHandler.nameMages(n);

      // priests
      nHandler.namePriests(n);

      // sacreds and elites
      nHandler.nameSacreds(n);

      // heroes
      nHandler.nameHeroes(n);

      // Epithet
      nHandler.giveEpithet(n);

      // Unit descriptions
      nHandler.describeNation(n);

      // Summaries
      n.summary.update();

      System.out.print(".");
    }

    // Get mod name if not custom
    if (modname.equals("")) {
      if (generatedNations.size() > 1) {
        modname = nGen.getSiteName(
          generatedNations.get(0).random,
          MagicPath.fromInt(generatedNations.get(0).random.nextInt(8)),
          MagicPath.fromInt(generatedNations.get(0).random.nextInt(8))
        );
      } else {
        modname = generatedNations.get(0).name;
      }
    }

    System.out.println(" Done!");

    this.write(generatedNations);

    Instant end = Instant.now();
    Duration timeElapsed = Duration.between(start, end);
    System.out.println(
      "Finished generation in " +
      timeElapsed.toMinutes() +
      " minutes and " +
      timeElapsed.toSecondsPart() +
      " seconds."
    );
    modname = "";
  }

  public static Properties getAppProperties() {
    if (NationGen.appProperties == null) {
      NationGen.appProperties = FileUtil.readProperties(NationGen.appPropertiesPath);
    }

    return NationGen.appProperties;
  }

  /**
   * Loads data from NationGenDB
   */
  private void loadNationGenDB() {
    units = new NationGenDB("/db/nationgen/units.csv");
    armordb = new NationGenDB("/db/nationgen/armors.csv");
    weapondb = new NationGenDB("/db/nationgen/weapons.csv");
    sites = new NationGenDB("/db/nationgen/sites.csv");
  }

  public int getNextUnitId() {
    return idHandler.nextUnitId();
  }

  /**
   * Handles spells
   * @param spells
   * @param n
   */
  private void handleSpells(List<Filter> spells, Nation nation) {
    int id = nation.nationid;
    List<String> spellNames = nation.getSpells();
    List<String> sitesReqIds = nation.getSpellSitesRequired();

    for (String spellName : spellNames) {
      Spell spell = null;

      // check for existing free spell
      for (Spell sp : this.freeSpells) {
        if (sp.name.equals(spellName)) {
          spell = sp;
        }
      }
      // create a new spell

      // check for custom spells first
      if (spell == null) {
        for (Filter sf : assets.customspells.getAllValues()) {
          if (sf.name.equals(spellName)) {
            spell = new Spell(this);
            spell.name = spellName;
            spell.addCommands(sf.getCommands());
            break;
          }
        }
      }
      // copy existing spell
      if (spell == null) {
        spell = new Spell(this);
        spell.name = spellName;
        spell.addCommands(Command.args("#copyspell", spellName));
        spell.addCommands(Command.args("#name", spellName + " "));
      }

      spell.nationids.add(id);

      // Handle existence in the list of spells with free space
      if (!this.freeSpells.contains(spell)) {
        this.freeSpells.add(spell);
      }

      if (
        spell.nationids.size() >=
        settings.get(SettingsType.maxrestrictedperspell)
      ) {
        this.freeSpells.remove(spell);
      }

      // Add to spells to write
      if (!this.spellsToWrite.contains(spell)) {
        this.spellsToWrite.add(spell);
      }
    }

    // Get the required sites that these spells need to be cast; i.e.
    // Pact of Rhuax requires the Roots of the Earth site. These sites
    // will be copied from vanilla, but their benefits will be cleared
    for (String siteId : sitesReqIds) {
      Site siteReq = new Site(siteId, true);
      siteReq.addCommands(new Command("#selectsite", new Arg(siteId)));
      siteReq.addCommands(new Command("#clear"));
      nation.sites.add(siteReq);
    }
  }

  public void writeDebugInfo() {
    double total = 0;
    for (Race r : assets.races.getAllValues()) {
      if (!r.tags.containsName("secondary")) {
        total += r.basechance;
      }
    }

    for (Race r : assets.races.getAllValues()) {
      if (!r.tags.containsName("secondary")) {
        System.out.println(r.name + ": " + (r.basechance / total));
      }
    }
  }

  private void writeDescriptions(List<Nation> nations, String modDirectory) {
    NationAdvancedSummarizer nDesc = new NationAdvancedSummarizer();
    if (settings.get(SettingsType.advancedDescs) == 1.0) {
      nDesc.writeAdvancedDescriptionFile(nations, modDirectory);
    }
    if (settings.get(SettingsType.basicDescs) == 1.0) {
      nDesc.writeDescriptionFile(nations, modDirectory);
    }
  }

  private void drawPreviews(List<Nation> nations, String dir) {
    if (settings.get(SettingsType.drawPreview) == 1) {
      System.out.print("Drawing previews");
      PreviewGenerator pGen = new PreviewGenerator();
      for (Nation n : nations) {
        pGen.savePreview(
          n,
          "./mods/" +
          dir +
          "/preview_" +
          n.nationid +
          "_" +
          n.name.toLowerCase().replaceAll(" ", "_") +
          ".png"
        );
        System.out.print(".");
      }
      System.out.println(" Done!");
    }
  }

  private String sanitizeForFilenames(String in) {
    return in
      .toLowerCase()
      .replaceAll(" ", "_")
      .replaceAll("å", "a")
      .replaceAll("ä", "a")
      .replaceAll("ö", "o");
  }

  // TODO: Support saving anywhere
  public void write(List<Nation> nations) {
    String og_modDirectory = "nationgen_" + sanitizeForFilenames(this.modname);
    String modDirectory = og_modDirectory;

    int suffix = 0;
    while (FileUtil.directoryExists("/mods/" + modDirectory)) {
      suffix++;
      modDirectory = og_modDirectory + "_" + suffix;
    }

    String modFilename = modDirectory + ".dm";

    FileUtil.createDirectory("/mods/" + modDirectory);

    // Descriptions
    writeDescriptions(nations, modDirectory);

    // Mod file
    FileUtil.writeLines(
      "/mods/" + modDirectory + "/" + modFilename,
      writeModLines(nations, "")
    );

    // Images
    BufferedImage modBanner = generateBanner(
      nations.get(0).colors[0],
      this.modname,
      nations.get(0).flag
    );
    FileUtil.writeTGA(modBanner, "/mods/" + modDirectory + "/banner.tga");
    for (Nation nation : nations) {
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

    System.out.println(
      "------------------------------------------------------------------"
    );
    System.out.println(
      "Finished writing " +
      nations.size() +
      " nations to file " +
      modFilename +
      "!"
    );
  }

  private List<String> writeModLines(
    List<Nation> nations,
    String modDirectory
  ) {
    List<String> lines = new ArrayList<>();
    // Description!
    lines.add("-- NationGen - " + modDirectory);
    lines.add("-----------------------------------");

    lines.add("-- Generated with version " + version + ".");
    lines.add("-- Generation setting code: " + settings.getSettingInteger());

    if (!manyseeds) {
      lines.add("-- Nation seeds generated with seed " + this.seed + ".");
    } else {
      lines.add("-- Nation seeds specified by user.");
    }

    for (Nation n : nations) {
      lines.add(
        "-- Nation " +
        n.nationid +
        ": " +
        n.name +
        " generated with seed " +
        n.getSeed()
      );
    }
    lines.add("-----------------------------------");
    lines.add("");

    // Actual mod definition
    lines.add("#modname \"NationGen - " + this.modname + "\"");
    lines.add("#description \"A NationGen generated nation!\"");

    // Banner!
    lines.add("#icon \"banner.tga\"");
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
    for (Nation nation : nations) {
      // Unit definitions
      lines.addAll(
        nation.writeUnitsLines(getNationDMDirectory(modDirectory, nation))
      );
      System.out.print(".");
    }
    System.out.println(" Done!");

    // Write sites!
    System.out.print("Writing sites");

    for (Nation nation : nations) {
      // Site definitions
      lines.addAll(nation.writeSitesLines());
      System.out.print(".");
    }
    System.out.println(" Done!");

    // Write nation definitions!
    System.out.print("Writing nations");
    for (Nation nation : nations) {
      // Nation definitions
      lines.addAll(
        nation.writeLines(getNationDMDirectory(modDirectory, nation))
      );
      System.out.print(".");
    }
    System.out.println(" Done!");

    if (settings.get(SettingsType.hidevanillanations) == 1) {
      lines.addAll(writeHideVanillaNationsLines(nations.size()));
    }

    return lines;
  }

  private String getNationDMDirectory(String modDirectory, Nation nation) {
    return nation.nationid + "-" + sanitizeForFilenames(nation.name);
  }

  private String getNationDirectory(String modDirectory, Nation nation) {
    return (
      modDirectory +
      "/" +
      nation.nationid +
      "-" +
      sanitizeForFilenames(nation.name)
    );
  }

  private List<String> writeHideVanillaNationsLines(int nationcount) {
    System.out.print("Hiding vanilla nations... ");

    List<String> lines = new ArrayList<>();
    lines.add("-- Hiding vanilla nations");
    lines.add("-----------------------------------");

    if (nationcount > 1) {
      lines.add("#disableoldnations");
      lines.add("");
      System.out.println(" Done!");
    } else {
      System.out.println(
        "Unable to hide vanilla nations with only one random nation!"
      );
    }

    return lines;
  }

  private List<String> writeSpellsLines() {
    List<String> lines = new ArrayList<>();
    if (!spellsToWrite.isEmpty()) {
      lines.add("--- Spells:");
      for (Spell s : this.spellsToWrite) {
        lines.add("#newspell");
        for (Command c : s.getCommands()) {
          lines.add(c.toModLine());
        }
        for (int id : s.nationids) {
          lines.add("#restricted " + id);
        }
        lines.add("#end");
        lines.add("");
      }
    }
    return lines;
  }

  public boolean hasShapeShift(Arg id) {
    if ("".equals(id.get())) {
      return false;
    }

    int realid = id.isNumeric() ? id.getInt() : -1;

    return this.forms.stream().anyMatch(scu -> scu.id == realid);
  }

  private void polishNation(Nation n) {
    n.finalizeUnits();
    handleShapeshifts(n);
    handleMounts(n);
    handleSpells(n.spells, n);
  }

  private void handleShapeshifts(Nation n) {
    List<Unit> shapeshiftUnits = n.listUnitsAndHeroes();

    // Do montags first
    for (Unit u : shapeshiftUnits) {
      for (Command c : u.getCommands()) {
        if (c.command.equals("#montag")) {
          handleMontag(c);
        }
      }
    }

    // Do montag templates and shapeshifters next
    for (Unit u : shapeshiftUnits) {
      for (Command c : u.getCommands()) {
        if (
          c.command.contains("shape") &&
          !c.command.equals("#cleanshape") &&
          !hasShapeShift(c.args.get(0))
        ) {
          if (c.command.equals("#firstshape") && u.tags.containsName("montagunit")) {
            handleMontag(c);
          }
          
          else {
            handleShapeshift(c, u);
          }
        }
      }
    }

    forms
      .stream()
      .filter(su -> shapeshiftUnits.contains(su.otherForm))
      .forEach(su -> {
        su.polish(this, n);

        // Replace command
        su.thisForm.getCommands()
          .stream()
          .filter(c -> c.command.equals("#weapon"))
          .forEach(c -> {
            Arg weaponId = c.args.get(0);

            if (!weaponId.isNumeric()) {
              c.args.set(
                0,
                new Arg(customItemsHandler.getCustomItemId(weaponId.get()))
              );
            }
          });
      });
  }

  private void handleMounts(Nation nation) {
    List<MountUnit> mountUnits = nation.getMountUnits();

    mountUnits
      .stream()
      .forEach(mu -> {
        mu.polish(this, nation);

        // Look for custom #weapon commands defined in the mount form with a
        // non-numerical id (i.e. #command "#armor 'meteorite_barding'").
        // These are defined in customitems.txt, and this code replaces their
        // NationGen id with the generated Dominions custom id
        mu.mount.getCommands()
          .stream()
          .filter(c -> c.command.equals("#weapon"))
          .forEach(c -> {
            Arg weaponId = c.args.get(0);

            if (!weaponId.isNumeric()) {
              c.args.set(
                0,
                new Arg(customItemsHandler.getCustomItemId(weaponId.get()))
              );
            }
          });

        // Look for custom #armor commands defined in the mount form (
        // like custom bardings) with a non-numerical id (i.e. #command
        // "#armor 'meteorite_barding'"). These are defined in customitems.txt,
        // and this code replaces their NationGen id with the generated
        // Dominions custom id
        mu.mount.getCommands()
          .stream()
          .filter(c -> c.command.equals("#armor"))
          .forEach(c -> {
            Arg armorId = c.args.get(0);

            if (!armorId.isNumeric()) {
              c.args.set(
                0,
                new Arg(customItemsHandler.getCustomItemId(armorId.get()))
              );
            }
          });
      });
  }

  private HashMap<String, Integer> montagmap = new HashMap<>();

  private void handleMontag(Command c) {
    Integer montag = montagmap.computeIfAbsent(c.args.get(0).get(), k ->
      idHandler.nextMontagId()
    );

    if (c.command.equals("#firstshape")) {
      c.args.set(0, new Arg("-" + montag));
    } else if (c.command.equals("#montag")) {
      c.args.set(0, new Arg("" + montag));
    }
  }

  private void handleShapeshift(Command c, Unit u) {
    ShapeShift shift = assets.secondshapes
      .getAllValues()
      .stream()
      .filter(s -> s.name.equals(c.args.get(0).get()))
      .findFirst()
      .orElseThrow(() ->
        new IllegalArgumentException(
          "Shapeshift named " + c.args.get(0) + " could not be found."
        )
      );

    ShapeChangeUnit su = new ShapeChangeUnit(
      this,
      assets,
      u.race,
      u.pose,
      u,
      shift
    );

    su.resolveId();

    switch (c.command) {
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
      case "#foreignshape":
        su.shiftcommand = "#homeshape";
        break;
      case "#homeshape":
        su.shiftcommand = "#foreignshape";
        break;
      case "#domshape":
        su.shiftcommand = "#notdomshape";
        break;
      case "#notdomshape":
        su.shiftcommand = "#domshape";
        break;
      case "#springshape":
        su.shiftcommand = "#summershape";
        break;
      case "#summershape":
        su.shiftcommand = "#autumnshape";
        break;
      case "#autumnshape":
        su.shiftcommand = "#wintershape";
        break;
      case "#wintershape":
        su.shiftcommand = "#springshape";
        break;
      default:
        break;
    }

    c.args.set(0, new Arg(su.id));
    forms.add(su);
  }

  public static BufferedImage generateBanner(
    Color c,
    String name,
    BufferedImage flag
  ) {
    BufferedImage combined = new BufferedImage(
      256,
      64,
      BufferedImage.TYPE_INT_RGB
    );
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
   * Aborts the nation generating process. This var is reset when the next set starts to generate.
   */
  public void abortNationGeneration() {
    shouldAbort = true;
  }

  public CustomItemsHandler GetCustomItemsHandler() {
    return customItemsHandler;
  }

  /**
   * This function *will* be refactored out.  But, race is a pain to do only half measured refactorings.
   * Eventually, entity/filter will need to be redone.
   * @return
   */
  public NationGenAssets getAssets() {
    return assets;
  }

  public static Boolean isInDebugMode() {
    return NationGen.isDebug;
  }
}
