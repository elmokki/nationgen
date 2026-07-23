package nationGen.units;

import com.elmokki.Generic;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nationGen.NationGen;
import nationGen.Settings.SettingsType;
import nationGen.entities.Filter;
import nationGen.entities.MagicFilter;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.entities.Theme;
import nationGen.items.DominionsItemSlot;
import nationGen.items.Item;
import nationGen.items.ItemData;
import nationGen.items.ItemDependency;
import nationGen.items.ItemProperty;
import nationGen.items.ItemType;
import nationGen.magic.MageGenerator;
import nationGen.magic.MagicPath;
import nationGen.magic.MagicPathInts;
import nationGen.misc.Arg;
import nationGen.misc.Args;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.misc.CommandFactory;
import nationGen.misc.CommandType;
import nationGen.misc.FileUtil;
import nationGen.misc.Operator;
import nationGen.misc.Tags;
import nationGen.misc.Utils;
import nationGen.naming.Name;
import nationGen.nation.Nation;

public class Unit {

  public Name name = new Name();
  public Race race;
  public Pose pose;
  public MountUnit mountUnit;
  public SlotMap slotmap = new SlotMap();
  public Color color = Color.white;
  public int id = -1;
  private List<Command> commands = new ArrayList<>();
  public NationGen nationGen;
  private Boolean caponly = false;
  public double capOnlyChance;
  public double survivability;
  public Tags tags = new Tags();
  public List<Filter> appliedFilters = new ArrayList<>();
  public Boolean polished = false;
  public Boolean invariantMonster = false; // Is the unit a monster that must be used as-is instead of copying? (E.g., hydras)
  
  static final public int HUMAN_SIZE = 3;

  private Nation nation = null;

  public Unit(NationGen nationGen, Race race, Pose pose) {
    this.nationGen = nationGen;
    this.pose = pose;
    this.race = race;
  }

  public Unit(NationGen nationGen, Race race, Pose pose, Nation n) {
    this(nationGen, race, pose);
    this.nation = n;
  }

  public Unit(Unit unit) {
    this.nationGen = unit.nationGen;
    this.race = unit.race;
    this.pose = unit.pose;
    this.slotmap = new SlotMap(unit.slotmap);
    this.color = unit.color;
    this.nation = unit.nation;
    this.polished = unit.polished;
    this.invariantMonster = unit.invariantMonster;
    this.caponly = unit.caponly;
    this.tags = new Tags(unit.tags);
    this.commands = new ArrayList<>(unit.commands)
      .stream()
      .map(c -> CommandFactory.copy(c))
      .collect(Collectors.toList());

    this.appliedFilters = new ArrayList<>(unit.appliedFilters)
      .stream()
      .map(f -> new Filter(f))
      .collect(Collectors.toList());

    this.mountUnit = (unit.mountUnit != null) ? new MountUnit(unit.mountUnit, this) : null;
  }

  public int getId() {
    return this.id;
  }

  public int getRootId() {
    int rootId = -1;

    if (this.isMontag()) {
      rootId = this.getCommand("#firstshape")
        .map(c -> c.args.getFirst().getInt())
        .orElse(-1);
    }

    if (rootId == -1) {
      rootId = this.getId();
    }

    return rootId;
  }

  /**
   * Returns a shallow copy of the Unit.commands field.
   * @return List<Command>
   */
  public List<Command> getCommands() {
    return new ArrayList<>(this.commands);
  }

  boolean isGetCommandsRunning = false;

  private void preventGetCommandsRecursion() {
    if (isGetCommandsRunning == true) {
      throw new IllegalCallerException("Unit.getCommands() is being called recursively for " + this.toString() + "!");
    }
  }

  /**
   * Gets all of the Unit commands from itself and its associated properties,
   * such as race, pose, items, etc. These are "handled"; i.e. there is extra
   * processing done after gathering them all to condense them and calculate all
   * the command modifiers.
   * 
   * Note that after Unit.polish(), all commands end up already handled and condensed
   * and assigned to the Unit.command list. At that point, this will just return that
   * Unit.commands list to avoid re-doing work and returning duplicate lists.
   * @return List<Command>
   */
  public List<Command> getAllHandledCommands() {
    this.preventGetCommandsRecursion();
    isGetCommandsRunning = true;

    List<Command> allCommands = new ArrayList<>();
    List<Command> raceCommands = new ArrayList<>();
    List<Command> poseCommands = new ArrayList<>();
    List<Command> itemCommands = new ArrayList<>();
    List<Command> filterCommands = new ArrayList<>();
    List<Command> tagRelatedCommands = new ArrayList<>();

    Tags unitTags = this.getAllTags();
    List<Arg> adjustmentValues = unitTags.getAllValues("adjustmentcommand");
    Optional<Arg> fixedResCostValue = unitTags.getValue("fixedrescost");

    if (this.polished) {
      isGetCommandsRunning = false;
      return this.commands;
    }

    // Shapechangeunits aren't really of the race/pose their other form is
    if (
      this.getClass() != ShapeChangeUnit.class &&
      this.getClass() != MountUnit.class
    ) {
      // Race and Pose commands
      raceCommands.addAll(race.unitcommands);
      poseCommands.addAll(pose.getCommands());
    }

    // Item commands
    slotmap.items().forEach(item -> 
      itemCommands.addAll(item.getCommands())
    );

    // Filter commands
    for (Filter f : this.appliedFilters) {
      for (Command c : f.getCommands()) {
        Command tc = c;
        final int tier = tags.getInt("schoolmage").orElse(2);

        if (c.args.size() > 0 && c.args.getFirst().get().contains("%value%")) {
          int multi = f.tags.getInt("valuemulti").orElse(10);
          int base = f.tags.getInt("basevalue").orElse(-5);
          int result = base + multi * tier;
          String resstring = "" + result;

          if (result > 0) {
            resstring = "+" + resstring;
          }

          if (result != 0) {
            tc = CommandFactory.create(c.command, resstring);
          }
        }

        // Shape change units handle #spr1/#spr2 separately
        if (this.getClass() == ShapeChangeUnit.class) {
          if (!tc.command.equals("#spr1") && !tc.command.equals("#spr2")) {
            filterCommands.add(tc);
          }
        }
        
        else {
          filterCommands.add(tc);
        }
      }
    }

    // Add all commands together so far; they are needed for the following calculations
    allCommands.addAll(raceCommands);
    allCommands.addAll(poseCommands);
    allCommands.addAll(itemCommands);
    allCommands.addAll(filterCommands);
    allCommands.addAll(this.commands);

    // Handle "#adjustmentcommand" tags
    for (Arg adjustmentArg : adjustmentValues) {
      tagRelatedCommands.add(adjustmentArg.getCommand());
    }

    // Handle possible "#fixedrescost" tag
    fixedResCostValue.ifPresent(arg -> {
      int fixedCost = arg.getInt();
      int currentCost = getResCost(true, true, allCommands);
      int costDiff = fixedCost - currentCost;
      Arg costArg = Arg.asModifier(costDiff);
      Command rcostCommand = CommandFactory.create("#rcost", costArg.get());
      tagRelatedCommands.add(rcostCommand);
    });

    // Add adjustments
    allCommands.addAll(tagRelatedCommands);

    // Move copy and clear commands to the top of the list, with #copystats first:
    List<Command> addToTop = new ArrayList<>();
    Stream.of(
      "#copystats",
      "#clear",
      "#clearweapons",
      "#cleararmor",
      "#clearmagic",
      "#clearspec"
    ).forEach(command -> {
      Iterator<Command> it = allCommands.iterator();
      while (it.hasNext()) {
        Command next = it.next();
        if (next.command.equals(command)) {
          it.remove();
          addToTop.add(next);
        }
      }
    });
    
    // Reverse so the first thing in the list is the last to be added to the top
    Collections.reverse(addToTop);
    addToTop.forEach(command -> allCommands.addFirst(command));

    // Now handle them!
    List<Command> multiCommands = new ArrayList<>();
    List<Command> tempCommands = new ArrayList<>();

    for (Command c : allCommands) {
      if (
        c.args.size() > 0 &&
        c.args.getFirst().get().startsWith("*") &&
        c.args.getFirst().isNumeric()
      ) {
        multiCommands.add(c);
      }

      else {
        handleCommand(tempCommands, c);
      }
    }

    // Handle resource cost autocalc - every unit's baseline resource cost is
    // their gold cost x 1000. Any previously handled #rpcost commands will be
    // used to modify this autocalc value.
    tempCommands.stream()
      .filter(c -> c.command.equals("#gcost"))
      // Don't use pure modifier gcosts for rpcost autocalc (i.e. -40 gcost)
      // TODO: These would come from things like ShapeChangeUnit or MountUnit
      // instances that only end up with modifier gcosts to the main Unit.
      // They should probably override this method.
      .filter(c -> !c.hasCombinatoryArgs())
      .findFirst()
      .ifPresent(gcostCommand -> {
        Optional<Command> rpcostCommand = tempCommands.stream()
          .filter(c -> c.isOfType(CommandType.RPCOST))
          .findFirst();

        Command autocalcRpCost = CommandFactory.create(
          CommandType.RPCOST.toString(),
          String.valueOf(this.getAutocalcRps(gcostCommand))
        );

        if (rpcostCommand.isPresent()) {
          Command combined = rpcostCommand.get().combine(autocalcRpCost);
          handleCommand(tempCommands, combined);
        }

        else {
          handleCommand(tempCommands, autocalcRpCost);
        }
      });

    // Percentual cost increases
    for (Command c : multiCommands) {
      handleCommand(tempCommands, c);
    }

    isGetCommandsRunning = false;
    return tempCommands;
  }

  public Command getHandledCommand(String command) {
    return this.getAllHandledCommands()
      .stream()
      .filter(c -> c.command.equals(command))
      .findFirst()
      .orElse(null);
  }

  public List<Command> getMountCommands() {
    List<Command> list = new ArrayList<>();

    if (this.isMounted() == false) {
      return list;
    }

    return this.mountUnit.gatherCommands();
  }

  public void addCommands(Command... commandsToAdd) {
    this.addCommands(List.of(commandsToAdd));
  }

  public void addCommands(List<Command> commandsToAdd) {
    for (Command c : commandsToAdd) {
      this.commands.add(c);
    }
  }

  public boolean removeCommand(Command command) {
    return this.commands.remove(command);
  }

  public boolean removeCommand(String command) {
    return this.commands.remove(CommandFactory.parse(command));
  }

  /**
   * Gets the commands from the Unit and its associated properties, like
   * pose, race, filters and items. Note this performs no processing on
   * them, and so the final list is not suitable for calculations like
   * adding a total (many commands might be repeated in different sources).
   * @return List<Comand>
   */
  public List<Command> gatherCommands() {
    // Don't re-check all command sources if Unit is already polished
    if (this.polished == true) {
      return new ArrayList<>(this.commands);
    }
    
    Stream<Command> unitCommands = this.commands.stream();
    Stream<Command> poseCommands = this.pose.getCommands().stream();
    Stream<Command> raceCommands = this.race.unitcommands.stream();
    Stream<Command> filterCommands = this.appliedFilters.stream().flatMap(f -> f.getCommands().stream());
    Stream<Command> itemCommands = this.slotmap.items().flatMap(i -> i.getCommands().stream());
    List<Command> gatheredCommands = Stream.of(unitCommands, poseCommands, raceCommands, filterCommands, itemCommands)
      .flatMap(s -> s)
      .collect(Collectors.toList());

    return gatheredCommands;
  }

  public Boolean hasCommand(String cmd) {
    Command parsed = CommandFactory.parse(cmd);
    return this.hasCommand(parsed);
  }

  public Boolean hasCommand(Command cmd) {
    return this.gatherCommands()
      .stream()
      .filter(c -> c.equals(cmd))
      .findAny()
      .isPresent();
  }

  private Stream<Command> getCommandFilter(String commandString) {
    Command parsedCommand = CommandFactory.parse(commandString);
    List<Command> allCommands = this.gatherCommands();

    return allCommands
      .stream()
      .filter(c -> parsedCommand.contains(c));
  }

  public Optional<Command> getCommand(String commandString) {
    return this.getCommandFilter(commandString).findFirst();
  }

  public int getFirstCommandValue(String command, int defaultv) {
    List<Command> commands = this.gatherCommands();

    for (Command c : commands) {
      if (c.command.equals(command) && c.args.size() > 0) {
        return c.args.getInt(0);
      }
    }

    return defaultv;
  }

  public int getTotalCommandValue(String command, int defaultv) {
    List<Command> commands = this.gatherCommands();
    int total = defaultv;
    
    for (Command c : commands) {
      if (c.command.equals(command) && c.args.size() > 0) {
        total = Generic.handleModifier(c.args.getFirst(), total);
      }
    }

    return total;
  }

  public String getStringCommandValue(String command, String defaultv) {
    List<Command> commands = this.gatherCommands();
    String value = defaultv;
    
    for (Command c : commands) {
      if (c.command.equals(command) && c.args.size() > 0) value = c.args
        .getFirst()
        .get();
    }

    return value;
  }

  // 20150522EA : my OOP prof back in undergrad would probably shoot me for this method...
  public void setCommandValue(String command, String newValue) {
    for (Command cmd : this.commands) {
      if (cmd.command.equals(command)) {
        cmd.args.set(0, new Arg(newValue));
      }
    }
  }

  public Optional<Command> getOwnCommand(String commandString) {
    Command parsedCommand = CommandFactory.parse(commandString);
    return this.commands
      .stream()
      .filter(c -> c.equals(parsedCommand))
      .findFirst();
  }

  public Item getSlot(String s) {
    return slotmap.get(s);
  }

  public Boolean isSlotEmpty(String slot) {
    return slotmap.get(slot) == null;
  }

  public Boolean isIdResolved() {
    return this.id > 0;
  }

  /**
   * Gets unit's magic picks as int[8] without randoms
   * @return
   */
  public MagicPathInts getMagicPicks() {
    return getMagicPicks(false);
  }

  /**
   * Gets unit's magic picks as int[8]
   * @param randoms if true, randoms at 25% chance are included
   * @return
   */
  public MagicPathInts getMagicPicks(Boolean randoms) {
    MagicPathInts picks = new MagicPathInts();

    double prob = 1;
    if (randoms) prob = 0.25;

    for (Filter f : appliedFilters) {
      if (f.name.equals("MAGICPICKS") || f.name.equals("PRIESTPICKS")) {
        MagicFilter m = (MagicFilter) f;
        MagicPathInts withRandoms = m.pattern.getPathsWithRandoms(m.prio, prob);
        for (MagicPath path : MagicPath.values()) {
          picks.add(path, withRandoms.get(path));
        }
      }
    }

    return picks;
  }

  public int getHolyPicks() {
    return getMagicPicks().get(MagicPath.HOLY);
  }

  public double getMagicAmount(double prob) {
    double n = 0;
    for (Filter f : appliedFilters) if (
      f.name.equals("MAGICPICKS") && !f.tags.containsName("holy")
    ) {
      MagicFilter m = (MagicFilter) f;
      n += m.pattern.getPicks(prob);
    }

    return n;
  }

  public double getRandoms(double prob) {
    double n = 0;
    for (Filter f : appliedFilters) if (
      f.name.equals("MAGICPICKS") && !f.tags.containsName("holy")
    ) {
      MagicFilter m = (MagicFilter) f;
      n += m.pattern.getRandoms(prob);
    }

    return n;
  }

  public List<MagicFilter> getMagicFilters() {
    List<MagicFilter> fs = new ArrayList<MagicFilter>();
    for (Filter f : appliedFilters) if (f.name.equals("MAGICPICKS")) {
      MagicFilter m = (MagicFilter) f;
      fs.add(m);
    }

    return fs;
  }

  public double getMagicAtHighest(double prob) {
    double n = 0;
    for (Filter f : appliedFilters) if (
      f.name.equals("MAGICPICKS") && !f.tags.containsName("holy")
    ) {
      MagicFilter m = (MagicFilter) f;
      n += m.pattern.getHighestReachable(prob);
    }

    return n;
  }

  public double getMagicSpread(double prob) {
    double n = 0;
    for (Filter f : appliedFilters) if (
      f.name.equals("MAGICPICKS") && !f.tags.containsName("holy")
    ) {
      MagicFilter m = (MagicFilter) f;
      n += m.pattern.getPathsAtleastAt(1, prob);
    }

    return n;
  }

  public void resolveId() {
    if (this.isIdResolved()) {
      return;
    }

    if (this.mountUnit != null) {
      this.mountUnit.resolveId();
    }

    this.id = this.nationGen.getNextUnitId();
  }

  // Decides on bodytype tags based on the itemslots of the unit (see #itemslots in modding manual for the bitmask table of values)
  protected Optional<String> writeBodytypeLine() {
    String[] coms = {
      "#lizard",
      "#quadruped",
      "#bird",
      "#snake",
      "#djinn",
      "#miscshape",
      "#humanoid",
      "#mountedhumanoid",
      "#troglodyte",
      "#naga",
      "#copystats",
    };
    for (String str : coms) if (this.hasCommand(str)) {
      return Optional.empty();
    }

    int slots = this.getItemSlots();

    // has feet and an arm
    if (
      Generic.containsBitmask(slots, 131072) &&
      Generic.containsBitmask(slots, 2)
    ) {
      // has head
      if (Generic.containsBitmask(slots, 8192)) return Optional.of("#humanoid");
      else return Optional.of("#troglodyte");
    }
    // no feet, but arm
    else if (Generic.containsBitmask(slots, 2)) {
      Boolean mounted = this.getSlot("mount") != null;
      if (mounted) return Optional.of("#mountedhumanoid");
      else return Optional.of("#naga");
    }
    // feet, no arm
    else if (Generic.containsBitmask(slots, 131072)) {
      return Optional.of("#quadruped");
    }
    // no feet nor arm
    else {
      return Optional.of("#miscshape");
    }
  }

  public int getHandSlots() {
    int itemSlotsMask = this.getItemSlots();
    int handSlots = DominionsItemSlots.getHandSlots(itemSlotsMask);
    return handSlots;
  }

  public int getItemSlots() {
    Command existingItemslots = this.getHandledCommand("#itemslots");
    HashMap<DominionsItemSlot, Integer> existingItemslotsMap;
    HashMap<DominionsItemSlot, Integer> itemslots;
    int encodedSlots;
    
    if (existingItemslots != null && !existingItemslots.hasCombinatoryArgs()) {
      itemslots = DominionsItemSlots.decode(existingItemslots.args.getInt(0));
    }

    else {
      Tags itemTags = new Tags();
      Tags unitTags = Generic.getAllUnitTags(this);
      Item basesprite = this.slotmap.get("basesprite");
      int existingItemslotsValue = (existingItemslots == null ? 0 : existingItemslots.args.getInt(0));

      existingItemslotsMap = DominionsItemSlots.decode(existingItemslotsValue);
      itemslots = DominionsItemSlots.add(
        List.of(
          existingItemslotsMap,
          DominionsItemSlots.defaultSlots()
        )
      );

      if (basesprite != null) {
        itemTags.addAll(basesprite.tags);
      }

      this.slotmap.items()
        .filter(i -> i != basesprite)
        .forEach(i -> itemTags.addAll(i.tags));

      // #baseitemslot tags will override the base amount of slots
      for (Args args : unitTags.getAllArgs("baseitemslot")) {
        String slotName = args.getFirst().get();
        DominionsItemSlot slot = DominionsItemSlot.fromString(slotName);
        Arg modifier = args.get(1);
        int newAmount = Generic.handleModifier(modifier, itemslots.get(slot));
        itemslots.put(slot, newAmount);
      }

      // Seearch for #itemslot tags that modifies each specific slot
      for (Args args : itemTags.getAllArgs("itemslot")) {
        String slotName = args.getFirst().get();
        DominionsItemSlot slot = DominionsItemSlot.fromString(slotName);
        Arg modifier = args.get(1);
        int newAmount = Generic.handleModifier(modifier, itemslots.get(slot));
        itemslots.put(slot, newAmount);
      }
    }

    DominionsItemSlots.enforceMin(itemslots);
    DominionsItemSlots.enforceMax(itemslots);
    encodedSlots = DominionsItemSlots.encode(itemslots);
    return encodedSlots;
  }

  public List<Unit> getMontagShapes() {
    List<Unit> montagUnits = new ArrayList<>();
    Optional<Command> firstshapeCommand = this.getCommand("#firstshape");

    if (firstshapeCommand.isEmpty()) {
      return montagUnits;
    }

    Command parsedFirstshape = firstshapeCommand.get();
    Integer negativeMontagNumber = parsedFirstshape.args.getInt(0);
    String montag = Math.abs(negativeMontagNumber) + "";
    Command montagCmd = CommandFactory.create("#montag", montag);

    this.nation.listTroops().forEach(t -> {
      if (t.hasCommand(montagCmd)) {
        montagUnits.add(t);
      }
    });

    return montagUnits;
  }

  public Boolean isCapOnly() {
    return this.caponly;
  }

  public Boolean isMontag() {
    return this.hasCommand("#montag") ||
      this.pose.roles.contains("montagtroops") ||
      this.pose.tags.containsName("montagpose");
  }

  public Boolean isMontagRecruitableTemplate() {
    Optional<Command> firstshapeCommand = this.getCommand("#firstshape");

    if (firstshapeCommand.isEmpty()) {
      return false;
    }

    String firstshapeId = this.getStringCommandValue("#firstshape", "");
    return !firstshapeId.isBlank() && this.tags.containsName("montagunit");
  }

  public Boolean isUndead() {
    return this.hasCommand("#undead");
  }

  public Boolean isAlmostUndead() {
    return this.hasCommand("#almostundead");
  }

  public Boolean isDemon() {
    return this.hasCommand("#demon");
  }

  public Boolean requiresUndeadLeadership() {
    return this.isUndead() || this.isAlmostUndead() || this.isDemon();
  }

  public Boolean isMagicBeing() {
    return this.hasCommand("#magicbeing");
  }

  public Boolean isMage() {
    return this.commands.stream()
      .filter(c -> {
        return c.command.equals("#magicskill") ||
          c.command.equals("#custommagic");
      })
      .findAny()
      .isPresent();
  }

  public Boolean isWarriorMage() {
    return this.tags.containsName("warriormage");
  }

  public Boolean isPriest() {
    return this.tags.containsName("priest");
  }

  public Boolean isCommander() {
    return this.nation != null && this.nation.selectCommanders().anyMatch(com -> com.equals(this));
  }

  public Boolean isSacred() {
    return this.hasCommand("holy");
  }

  public Boolean isMounted() {
    return this.mountUnit != null;
  }

  public Boolean isImmobile() {
    return pose.tags.containsName("immobile");
  }

  public Boolean isDualWielding() {
    Item weapon = this.getSlot("weapon");
    Item offhand = this.getSlot("offhand");
    Boolean isOffhandMelee = offhand != null && offhand.isMeleeWeapon();
    Boolean isOffhandDominionsItem = offhand != null && offhand.isDominionsEquipment();
    return weapon != null && weapon.isMeleeWeapon() && isOffhandMelee && isOffhandDominionsItem;
  }

  public Boolean isRanged() {
    Item weapon = getSlot("weapon");

    if (weapon == null) {
      return false;
    }

    return weapon.getIntegerFromDb(ItemProperty.RANGE.toDBColumn(), 0) > 0;
  }

  public Boolean isSecondaryRanged() {
    Item bonusWeapon = getSlot("bonusweapon");

    if (bonusWeapon == null) {
      return false;
    }

    return bonusWeapon.getIntegerFromDb(ItemProperty.RANGE.toDBColumn(), 0) > 0;
  }

  public Boolean hasRangeOfAtLeast(int range) {
    Item weapon = getSlot("weapon");

    if (weapon == null) {
      return false;
    }

    return weapon.getIntegerFromDb(ItemProperty.RANGE.toDBColumn(), 0) >= range;
  }

  public Boolean hasSecondaryRangeOfAtLeast(int range) {
    Item bonusWeapon = getSlot("bonusweapon");

    if (bonusWeapon == null) {
      return false;
    }

    return bonusWeapon.getIntegerFromDb(ItemProperty.RANGE.toDBColumn(), 0) >= range;
  }

  public Boolean isShapeshifter() {
    boolean hasShapeshiftCommand = this.gatherCommands()
      .stream()
      .anyMatch(Command::isShapeshiftCommand);

    if (!hasShapeshiftCommand) {
      return false;
    }

    return hasShapeshiftCommand && !isMontagRecruitableTemplate();
  }

  public int getNumberOfHandsRequiredForWeapons() {
    Stream<Item> equippedWeapons = this.slotmap.getEquippedWeapons();
    Stream<Item> equippedShields = this.slotmap.getEquippedShields();
    List<Command> handledCommands = this.getAllHandledCommands();

    int handsForEquipment = Stream.of(equippedWeapons, equippedShields)
      .flatMap(s -> s)
      .mapToInt(Item::getHandsRequiredToUse)
      .sum();

    // Get hands needed for weapons that were added directly through templates, like
    // natural weapons from bases (such as nagas)
    int handsForBaseWeapons = handledCommands.stream()
      .filter(c -> c.command.equals("#weapon"))
      .filter(c -> !Generic.isNumeric(c.args.getString(0)) || c.args.getInt(0) > 0)
      .mapToInt(c -> {
        String id = c.args.getString(0);
        boolean isIntrinsic = this.nationGen.weapondb.GetInteger(id, ItemProperty.INTRINSIC.toDBColumn(), 0) == 1;
        boolean isRanged = this.nationGen.weapondb.GetInteger(id, ItemProperty.RANGE.toDBColumn(), 0) != 0;
        boolean isTwoHanded = this.nationGen.weapondb.GetInteger(id, ItemProperty.IS_2H.toDBColumn(), 0) == 1;
        int handsNeeded = isIntrinsic || isRanged ? 0 : !isTwoHanded ? 1 : 2;
        return handsNeeded;
      }).sum();

    // Get hands needed for shields that were added directly through templates
    int handsForBaseShields = handledCommands.stream()
      .filter(c -> c.command.equals("#armor"))
      .filter(c -> !Generic.isNumeric(c.args.getString(0)) || c.args.getInt(0) > 0)
      .mapToInt(c -> {
        String id = c.args.getString(0);
        boolean isShield = this.nationGen.armordb.GetInteger(id, ItemProperty.TYPE.toDBColumn(), 0) == 4;
        boolean isIntrinsic = this.nationGen.armordb.GetInteger(id, ItemProperty.INTRINSIC.toDBColumn(), 0) == 1;
        int handsNeeded = !isShield ? 0 : isIntrinsic ? 0 : 1;
        return handsNeeded;
      }).sum();

    int totalUsedHands = handsForEquipment + handsForBaseWeapons + handsForBaseShields;
    return totalUsedHands;
  }

  public int getNumberOfFreeHands() {
    int usedHands = this.getNumberOfHandsRequiredForWeapons();
    int totalHandSlots = this.getHandSlots();
    int freeHandSlots = totalHandSlots - usedHands;
    return freeHandSlots;
  }

  public Boolean hasFreeHandSlot() {
    return this.getNumberOfFreeHands() > 0;
  }

  private void handleRemoveDependency(Item i) {
    if (i == null) return;

    for (ItemDependency d : i.dependencies) {
      if (d.lagged) continue;

      String slot = d.slot;

      Item old = slotmap.pop(slot);

      handleSlotChange(slot, old, slotmap.get(slot));
    }
  }

  private void handleDependency(String slotname, Boolean lagged) {
    if (getSlot(slotname) == null) {
      return;
    }

    ChanceIncHandler chandler = null;
    Random r = null;
    if (nation != null) {
      chandler = new ChanceIncHandler(nation);
      r = new Random(nation.random.nextInt());
    }

    // This handles #needs
    for (ItemDependency d : getSlot(slotname).dependencies) {
      if (d.lagged != lagged) {
        continue;
      }

      String target = d.target;
      String slot = d.slot;

      if (!d.type) {
        // Handle needs and setslot
        String command = lagged ? "#forceslot" : "#needs";

        if (pose.getItems(slot) == null) {
          throw new IllegalStateException(
            getSlot(slotname).name +
            " in slot " +
            slotname +
            " tried to link to " +
            target +
            " on list " +
            slot +
            ", but that list wasn't found. " +
            "(race: " +
            this.race.name +
            ", pose: " +
            pose.name +
            ", roles: " +
            this.pose.roles +
            ")"
          );
        }

        Item item = pose.getItems(slot).getItemWithName(target, slot);

        if (item == null) {
          throw new IllegalStateException(
            getSlot(slotname).name +
            " in slot " +
            slotname +
            " tried to link to " +
            target +
            " on list " +
            slot +
            ", but it wasn't found. " +
            "(race: " +
            this.race.name +
            ", pose: " +
            pose.name +
            ", roles: " +
            this.pose.roles +
            ")"
          );
        }
        setSlot(slot, item);
      } else if (chandler != null) { // Handle needstype and setslottype
        String command = lagged ? "#forceslottype" : "#needstype";

        if (pose.getItems(slot) == null) {
          System.out.println(
            command +
            " for " +
            slotname +
            ", type " +
            target +
            " and item " +
            getSlot(slotname).name +
            " on slot " +
            slot +
            " failed due to the slot having no items: " +
            pose +
            ", " +
            this.pose.roles +
            ", race " +
            race.name
          );
          break;
        }

        List<Item> possibles = ChanceIncHandler.getFiltersWithType(
          target,
          pose.getItems(slot)
        );
        Item item = chandler.handleChanceIncs(this, possibles).getRandom(r);

        if (item != null) {
          setSlot(slot, item);
        }
      }
    }
  }

  private void handleRemoveThemeinc(Item item) {
    if (item == null) return;

    if (item.filter != null) {
      appliedFilters.remove(item.filter);
    }
  }

  private void handleAddThemeinc(Item item) {
    if (item == null) return;

    if (item.filter != null && !appliedFilters.contains(item.filter)) {
      appliedFilters.add(item.filter);
    }
  }

  public void setSlot(String slotname, Item newitem) {
    Item olditem = getSlot(slotname);

    // HIC SUNT DRACONIS
    // Do not pop the slot here. Even if it feels like a new item
    // should replace the previous existing item in the same slot,
    // the slotmap is very specifically designed so that each slot
    // operates as a stack. Only the top item of the stack is relevant.
    // But if that item gets subsequently removed due to downstream code,
    // such as in the process of removing an item dependency, we want the
    // slot stack to have the previously equipped item so that things are
    // as they initially were, rather than ending up with a null slot.
    slotmap.push(slotname, newitem);
    handleSlotChange(slotname, olditem, newitem);
  }

  public void setCapOnly(Boolean shouldBeCapOnly) {
    this.caponly = shouldBeCapOnly;
  }

  private void handleSlotChange(String slotName, Item oldItem, Item newItem) {
    if (oldItem != null) {
      handleRemoveThemeinc(oldItem);
      handleRemoveDependency(oldItem);

      // Remove cached mountItem if the item containing the mount is unequipped
      // This assumes that any given Unit will only ever had a single mount
      if (oldItem.isMountItem()) {
        this.mountUnit = null;
      }
    }

    if (newItem != null) {
      handleDependency(slotName, false);
      handleAddThemeinc(newItem);

      if (newItem.isMountItem()) {
        this.mountUnit = new MountUnit(newItem, this, this.nationGen);
      }
    }
  }

  public MountUnit getMountUnit() {
    return this.mountUnit;
  }

  /**
   * Calculates all of the #price_per_command tags of the unit.
   * @param unit The unit to price
   * @param unitTags The unit Tags
   * @return Total extra gold cost of all #price_per_command tags.
   */
  public static int calculatePricePerCommands(Unit unit, Tags unitTags) {
    int total = 0;
    List<Args> pricePerCommandArgs = unitTags.getAllArgs("price_per_command");

    for (Args args : pricePerCommandArgs) {
      String commandToPrice = args.getFirst().get();
      int commandValue = unit.getTotalCommandValue(commandToPrice, 0);
      double costPerCommandPoint = args.get(1).getDouble();
      int commandValueThreshold = 0;

      if (args.size() > 2) {
        commandValueThreshold = args.get(2).getInt();
      }

      // No extra price to calculate on this command
      if (commandValue <= commandValueThreshold) {
        continue;
      }

      // Add price of every command point above the threshold
      int valueOverThreshold = commandValue - commandValueThreshold;
      int modifiedCost = (int) Math.round((double) valueOverThreshold * costPerCommandPoint);
      total += modifiedCost;
    }

    return total;
  }

  /**
   * Calculates all of the #price_per_applied_filter tags of the unit.
   * @param unit The unit to price
   * @param unitTags The unit Tags
   * @return Total extra gold cost of all #price_per_applied_filter tags.
   */
  public static int calculatePricePerAppliedFilters(Unit unit, Tags unitTags) {
    int total = 0;
    Stream<Filter> appliedFiltersStream = unit.appliedFilters.stream();
    List<Args> pricePerAppliedFilterArgs = unitTags.getAllArgs("price_per_applied_filter");

    for (Args args : pricePerAppliedFilterArgs) {
      int filterPower = args.getFirst().getInt();
      int numberOfAppliedFilters = (int) appliedFiltersStream.filter(f -> f.power >= filterPower).count();
      double costPerFilter = args.get(1).getDouble();
      int filterPowerThreshold = 0;

      if (args.size() > 2) {
        filterPowerThreshold = args.get(2).getInt();
      }

      if (numberOfAppliedFilters <= filterPowerThreshold) {
        continue;
      }

      int numberOfFiltersOverThreshold = numberOfAppliedFilters - filterPowerThreshold;
      int modifiedCost = (int) Math.round((double) numberOfFiltersOverThreshold * costPerFilter);
      total += modifiedCost;
    }

    return total;
  }

  /** Calculates all of the #price_if_command tags of the unit.
   * @param unit The unit to price
   * @param unitTags The unit's Tags object
   * @return Total extra gold cost of all #price_if_command tags.
   */
  public static int calculatePriceIfCommandTags(Unit unit, Tags unitTags) {
    int total = 0;
    List<Args> priceIfCommandArgs = unitTags.getAllArgs("price_if_command");

    for (Args args : priceIfCommandArgs) {
      String commandToPrice = args.get(args.size() - 3).get();
      int commandValue = unit.getTotalCommandValue(commandToPrice, 0);
      int target = args.get(args.size() - 2).getInt();
      int costIfValueBeyondTarget = args.get(args.size() - 1).getInt();

      Boolean at = args.contains(new Arg("at"));
      Boolean below = args.contains(new Arg("below"));
      Boolean above = args.contains(new Arg("above"));

      if (
        (commandValue > target && above) ||
        (commandValue == target && at) ||
        ((commandValue < target) && below)
      ) {
        total += costIfValueBeyondTarget;
      }
    }

    return total;
  }

  /**
   * Resolves the %cost placeholder values of commands which depend on the unit's final gold cost.
   * For example, #chaosrec commands typically reduce the unit's cost by a given percentage of its
   * final cost, such as 5 or 10%. They will initially be expressed in the data files as something
   * like "#chaosrec %cost10". If the unit would cost 50 gold, this function will resolve that
   * command into "#chaosrec 5" (a 5% of 50 gold).
   * 
   * @param unit The Unit on which to apply the resolved command.
   * @param unitGoldCost The gold cost of said Unit (more efficient than recalculating it here).
   * @param commands The list of commands that needs to be parsed for unresolved %cost arguments.
   */
  public static void resolvePercentageCostCommands(Unit unit, int unitGoldCost, List<Command> commands) {
    // Find all commands with an unresolved %cost argument
    List<Command> percentCostCommands = commands.stream()
      .filter(c -> {
        return c.args.size() > 0 &&
          c.args.getFirst().get().startsWith("%cost") &&
          Generic.isNumeric(c.args.getFirst().get().substring(5));
      })
      .collect(Collectors.toList());

    if (percentCostCommands.isEmpty()) {
      return;
    }

    for (Command c : percentCostCommands) {
      double percentage = Double.parseDouble(c.args.getFirst().get().substring(5));
      double multiplier = percentage / 100;

      // Calculate the %cost based on the unit's gold cost
      int resolvedCommandCost = (int) Math.round((double) unitGoldCost * multiplier);

      // Re-add the command with the final, resolved value instead of the %cost
      Command d = CommandFactory.create(c.command, resolvedCommandCost + "");
      unit.handleCommand(commands, d);
    }
  }

  public static UnitCost calculateMeanCostOfUnits(List<Unit> units) {
    int goldSum = 0;
    int resourceSum = 0;
    int unitCount = units.size();

    if (unitCount == 0) {
      return UnitCost.zero();
    }
    
    for (Unit unit : units) {
      resourceSum += unit.getResCost(true, true);
      goldSum += unit.getGoldCost(true);
    }

    int meanGoldCost = (int) Math.round((double) goldSum / (double) unitCount);
    int meanResourceCost = (int) Math.round((double) resourceSum / (double) unitCount);
    return new UnitCost(meanGoldCost, meanResourceCost, 0);
  }

  public static List<Command> calculateMeanStatsOfUnits(List<Unit> units) {
    List<String> stats = List.of("#hp", "#size", "#prot", "#mr", "#mor", "#str", "#att", "#def", "#prec", "#ap", "#mapmove", "#enc");
    List<List<Integer>> scores = new ArrayList<>();
    List<Command> means = new ArrayList<>();
    int unitCount = units.size();

    if (units.isEmpty()) {
      return means;
    }
    
    for (int i = 0; i < units.size(); i++) {
      Unit unit = units.get(i);
      scores.add(new ArrayList<>());

      for (int j = 0; j < stats.size(); j++) {
        String stat = stats.get(j);
        Integer statValue = 0;

        if (stat.equals("#prot")) {
          statValue = unit.getTotalProt(true);
        }

        else if (stat.equals("#att")) {
          statValue = unit.getTotalAtt();
        }

        else if (stat.equals("#def")) {
          statValue = unit.getTotalDef();
        }
        
        else {
          statValue = unit.getTotalCommandValue(stat, 0);
        }

        scores.get(i).add(statValue);
      }
    }

    for (int i = 0; i < stats.size(); i++) {
      String stat = stats.get(i);
      int totalStatScores = 0;
      int mean = 0;

      for (int j = 0; j < scores.size(); j++) {
        int unitScore = scores.get(j).get(i);
        totalStatScores += unitScore;
      }

      mean = totalStatScores / unitCount;
      means.add(CommandFactory.parse(stat + " " + mean));
    }

    return means;
  }

  /**
   * Gathers the tags related to a unit from its race, pose, filters and items.
   * @param unit
   * @return A Tags object with all related tags.
   */
  public static Tags gatherAllTags(Unit unit) {
    Tags tags = new Tags();
    tags.addAll(unit.race.tags);
    tags.addAll(unit.pose.tags);

    for (Filter f : unit.appliedFilters) {
      tags.addAll(f.tags);
    }

    for (Theme t : unit.race.themefilters) {
      tags.addAll(t.tags);
    }

    unit.slotmap.items().forEach(i -> tags.addAll(i.tags));
    return tags;
  }

  public int getGoldCost(Boolean includeMountCost) {
    List<Command> commands = this.getAllHandledCommands();
    Tags unitTags = Generic.getAllUnitTags(this);
    int copyStatsTarget = this.getCopyStats();
    double sacredMultiplier = 1;
    double slowRecMultiplier = 1;

    int totalCost = 0;
    int pricePerCommandsCost = Unit.calculatePricePerCommands(this, unitTags);
    int pricePerAppliedFiltersCost = Unit.calculatePricePerAppliedFilters(this, unitTags);
    int pricePerIfCommandTags = Unit.calculatePriceIfCommandTags(this, unitTags);
    totalCost += pricePerCommandsCost + pricePerAppliedFiltersCost + pricePerIfCommandTags;

    // If Unit has a #copystats command and no pricing tags have modified cost yet,
    // we start assuming that the cost of the #copystats target should be final
    Boolean shouldUseCopyStatsFinalCost = copyStatsTarget > -1 && totalCost == 0;

    for (Command c : commands) {
      if (c.command.equals("#gcost")) {
        shouldUseCopyStatsFinalCost = false;
        totalCost += c.args.getFirst().getInt();
      }

      if (c.command.equals("#holy")) {
        shouldUseCopyStatsFinalCost = false;
        sacredMultiplier = this.nationGen.settings.get(SettingsType.goldSacredCostMultiplier);
      }

      if (c.command.equals("#slowrec")) {
        shouldUseCopyStatsFinalCost = false;
        slowRecMultiplier = this.nationGen.settings.get(SettingsType.goldSlowRecCostMultiplier);
      }
    }

    // Only apply the multipliers if the unit wasn't already polished
    if (!this.polished) {
      totalCost *= sacredMultiplier;
      totalCost *= slowRecMultiplier;

      if (includeMountCost == true) {
        totalCost += this.getMountGoldCost();
      }
    }

    // Unit is using #copystats, so figure out the target's cost to account for it
    if (copyStatsTarget > -1 && shouldUseCopyStatsFinalCost) {
      int copyStatsBasecost = this.nationGen.units.GetInteger("" + copyStatsTarget, "basecost");

      if (copyStatsBasecost >= 10000) {
        copyStatsBasecost -= 10000;
      }

      // If the Unit doesn't have #gcost tags but has #copystats, we assume it's a final cost
      return copyStatsBasecost;
    }

    // TODO: if no other cost logic applies to montag template, should move this to an earlier step to break out early
    if (this.isMontagRecruitableTemplate()) {
      Boolean shouldUseMontagMeanCost = !this.pose.tags.containsName("no_montag_mean_costs");
      
      if (shouldUseMontagMeanCost) {
        // Find the parent montag id associated with these poses
        String montagId = this.getFirstshapeIdForMontag();
        List<Unit> montagChildren = this.nation.getMontagUnits(montagId);
        UnitCost meanMontagCosts = Unit.calculateMeanCostOfUnits(montagChildren);
        totalCost = meanMontagCosts.gold;
      }
    }

    return totalCost;
  }

  public int getMountGoldCost() {
    if (this.mountUnit == null) {
      return 0;
    }

    return this.mountUnit.getGoldCost();
  }

  public String getName() {
    int stats = this.getCopyStats();

    if (!this.name.isUnnamed()) {
      return this.name.toString(this);
    }

    else if (stats > -1) {
      return this.nationGen.units.GetValue("" + stats, "name");
    }

    return Name.UNNAMED;
  }

  public String getFirstshapeIdForMontag() {
    String montagId = this.getStringCommandValue("#firstshape", "");

    if (Generic.isNumeric(montagId) && montagId.startsWith("-")) {
      montagId = montagId.substring(1);
    }

    return montagId;
  }

  public LeadershipAbility getLeadership(LeadershipType type) {
    Command leadershipCommand = this.getAllHandledCommands()
      .stream()
      .filter(c -> c.command.endsWith("leader"))
      .findAny()
      .orElse(null);

    return LeadershipAbility
      .fromModCommand(leadershipCommand)
      .orElse(LeadershipAbility.getNoLeadership(type));
  }

  public Boolean hasLeadership(LeadershipType type) {
    return this.getLeadership(type)
      .equals(LeadershipAbility.getNoLeadership(type)) == false;
  }

  public Boolean hasAnyLeadership() {
    return List.of(LeadershipType.values())
      .stream().anyMatch(t -> this.hasLeadership(t) != false);
  }

  public int getResCost(Boolean useSize, Boolean includeMount) {
    return getResCost(useSize, includeMount, this.getAllHandledCommands());
  }

  private int getResCost(Boolean useSize, Boolean includeMountCost, List<Command> commands) {
    int rcost = 0;
    int size = this.getSize();
    int ressize = this.getTotalCommandValue("#ressize", -1);
    int copyStatsTarget = this.getCopyStats();

    if (this.polished && this.isMontagRecruitableTemplate()) {
      return this.getFirstCommandValue("#rcost", 1);
    }

    int extraResources = commands.stream()
      .filter(c -> c.command.equals("#rcost"))
      .mapToInt(c -> c.args.getInt(0))
      .sum();

    int equipmentResources = this.slotmap.items()
      .filter(i -> i.isDominionsEquipment() && i.dominionsId.isResolved())
      .mapToInt(i -> i.getIntegerFromDb(ItemProperty.RESOURCE_COST.toDBColumn(), 0))
      .sum();

    if (useSize) {
      if (ressize > 0) {
        rcost = (ressize * equipmentResources) / Unit.HUMAN_SIZE;
      }

      else {
        rcost = (size * equipmentResources) / Unit.HUMAN_SIZE;
      }
    }

    // If Unit has a #copystats command and no pricing tags have modified cost yet,
    // we start assuming that the cost of the #copystats target should be final
    Boolean shouldUseCopyStatsFinalCost = copyStatsTarget > -1 && rcost == 0;

    for (Command c : commands) {
      if (c.command.equals("#rcost")) {
        shouldUseCopyStatsFinalCost = false;
        rcost += c.args.getFirst().getInt();
      }
    }

    // Unit is using #copystats, and it doesn't have an explicit #rcost command
    if (copyStatsTarget > -1 && shouldUseCopyStatsFinalCost) {
      int copyStatsResCost = this.nationGen.units.GetInteger(String.valueOf(copyStatsTarget), "rcost");

      // Thus we assume it's a final cost
      return copyStatsResCost;
    }

    // TODO: if no other cost logic applies to montag template, should move this to an earlier step to break out early
    if (this.isMontagRecruitableTemplate()) {
      Boolean shouldUseMontagMeanCost = !this.pose.tags.containsName("no_montag_mean_costs");
      
      if (shouldUseMontagMeanCost) {
        // Find the parent montag id associated with these poses
        String montagId = this.getStringCommandValue("#firstshape", "");
        List<Unit> montagChildren = this.nation.getMontagUnits(montagId);
        UnitCost meanMontagCosts = Unit.calculateMeanCostOfUnits(montagChildren);
        rcost = meanMontagCosts.resources;
      }
    }

    if (includeMountCost) {
      rcost += this.getMountResCost();
    }

    // Dom minimum res amount is 1.
    rcost = Math.max(rcost + extraResources, 1);
    return rcost;
  }

  public int getMountResCost() {
    if (this.mountUnit == null) {
      return 0;
    }

    return this.mountUnit.getResCost();
  }

  public Tags getAllTags() {
    Tags allTags = new Tags();
    allTags.addAll(this.tags);
    allTags.addAll(this.pose.tags);
    allTags.addAll(this.race.tags);

    this.race.themefilters.forEach(f -> {
      allTags.addAll(f.tags);
    });

    this.slotmap.items().forEach(i -> {
      allTags.addAll(i.tags);
    });

    this.appliedFilters.forEach(f -> {
      allTags.addAll(f.tags);
    });

    return allTags;
  }

  public List<String> getAllThemes() {
    List<String> unitThemes = new ArrayList<>();
    unitThemes.addAll(this.race.themes);
    unitThemes.addAll(this.pose.themes);
    this.race.themefilters.forEach(f -> unitThemes.addAll(f.themes));
    this.appliedFilters.forEach(f -> unitThemes.addAll(f.themes));
    this.slotmap.items().forEach(i -> unitThemes.addAll(i.themes));
    return unitThemes;
  }

  private Boolean handleLowEncCommandPolish(Tags tags) {
    if (!tags.containsName("lowencthreshold")) return false;

    int treshold = tags.getValue("lowencthreshold").orElseThrow().getInt();
    Item armor = this.getSlot("armor");
    Item offhand = this.getSlot("offhand");
    Item helmet = this.getSlot("helmet");

    int enc = 0;
    if (armor != null) {
      enc += armor.getIntegerFromDb(ItemProperty.ENCUMBRANCE.toDBColumn(), 0);
    }

    if (offhand != null && offhand.isArmor()) {
      enc += offhand.getIntegerFromDb(ItemProperty.ENCUMBRANCE.toDBColumn(), 0);
    }

    if (helmet != null) {
      enc += helmet.getIntegerFromDb(ItemProperty.ENCUMBRANCE.toDBColumn(), 0);
    }

    if (enc <= treshold && tags.containsName("lowenccommand")) {
      Command fullCommand = tags.getCommand("lowenccommand").orElseThrow();
      String command = fullCommand.command;
      for (Command c : this.gatherCommands()) {
        if (command.equals(c.command)) {
          return false;
        }
      }
      this.commands.add(fullCommand);
      return true;
    }

    return false;
  }

  protected Boolean hasCopyStats() {
    Boolean copystats = false;
    for (Command c : this.gatherCommands()) {
      if (c.command.equals("#copystats")) copystats = true;
    }

    return copystats;
  }

  protected int getCopyStats() {
    return this.getFirstCommandValue("#copystats", -1);
  }

  public String guessRole() {
    String r = "infantry";
    for (String role : pose.roles) if (role.contains("ranged")) {
      r = "ranged";
      break;
    }
    for (String role : pose.roles) if (role.contains("mounted")) {
      r = "mounted";
      break;
    }
    return r;
  }

  public void polish() {
    if (this.polished) {
      return;
    }

    final Unit unit = this;
    Item weapon = unit.getSlot("weapon");
    Item offhand = unit.getSlot("offhand");

    handleLowEncCommandPolish(unit.pose.tags);
    handleLowEncCommandPolish(unit.race.tags);

    for (Filter f : appliedFilters) {
      handleLowEncCommandPolish(f.tags);
    }

    if (unit.name.toString(this).equals("\"\"")) {
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

    // Resolve all custom items that the Unit will have equipped in-game through
    // #armor and #weapon commands.
    this.resolveCustomWeaponsAndArmor();

    // +2hp to mounted
    if (this.isMounted() != null) {
      this.commands.add(CommandFactory.create("#hp", "+2"));
      this.tags.addArgs("itemslot", "feet", -1);
    }

    // Ambidextrous. Should be after custom equipment handling and before command cleanup
    if (this.isDualWielding()) {
      int totalLength = Integer.parseInt(offhand.getValueFromDb(ItemProperty.LENGTH.toDBColumn(), "0"));

      if (weapon != null) {
        totalLength += Integer.parseInt(weapon.getValueFromDb(ItemProperty.LENGTH.toDBColumn(), "0"));
      }

      if (totalLength > 0) {
        this.commands.add(CommandFactory.create("#ambidextrous", "+" + Math.max(1, totalLength)));
      }
    }

    // Fist for things without proper weapons
    if (this.lacksMeleeWeapon()) {
      int fistWeaponDominionsId = 92;
      Arg fistArg = new Arg(fistWeaponDominionsId);
      String commandDescription = "Fist given to units that could otherwise only kick.";
      Command fistWeaponCommand = CommandFactory.create("#weapon", Args.of(fistArg), commandDescription);
      this.commands.add(fistWeaponCommand);
    }
                                                        
    // Clean up commands
    List<Command> polishedCommands = unit.getAllHandledCommands();

    for (Command c : polishedCommands) {
      if (!c.hasArgs()) {
        continue;
      }

      // Mapmove must at least be 1 if not immobile
      if (c.command.equals("#mapmove") && !this.isImmobile()) {
        int mapMove = c.args.getFirst().getInt();

        if (mapMove < 1) {
          c.args.set(0, new Arg(1));
        }
      }

      // Assign ingame ids to nationgen weapon references
      if (c.command.equals("#weapon")) {
        Arg weaponId = c.args.getFirst();

        if (!weaponId.isNumeric()) {
          int ingameId = nationGen.GetCustomItemsHandler().getCustomItemId(weaponId.get());
          c.args.set(0, new Arg(ingameId));
        }
      }
    }

    /* Cost calculation */
    int gcost = this.getGoldCost(true);

    if (gcost > 30) {
      gcost = Utils.roundInGroupsOf(gcost, 5);
    }

    Command gcostCommand = CommandFactory.create("#gcost", Integer.toString(gcost));
    handleCommand(polishedCommands, gcostCommand);

    // Resources are autocalculated ingame. We only need to assign them manually to montag templates
    // that don't have any equipment in the recruitment screen until they appear into the game
    if (this.isMontagRecruitableTemplate()) {
      int rcost = this.getResCost(true, true);
      Command rcostCommand = CommandFactory.create("#rcost", Integer.toString(rcost));
      handleCommand(polishedCommands, rcostCommand);

      String montagId = this.getFirstshapeIdForMontag();
      List<Unit> montagChildren = this.nation.getMontagUnits(montagId);
      calculateMeanStatsOfUnits(montagChildren).forEach(meanStatCommand ->
        handleCommand(polishedCommands, meanStatCommand)
      );
    }

    // Handle mounts that should make the unit have more than #holycost 1
    if (unit.isMounted() && unit.isSacred()) {
      int unitHolyCost = unit.getFirstCommandValue("#holycost", 0);
      int mountHolyCost = unit.mountUnit.getFirstCommandValue("#holycost", 0);
      int holyCost = Math.max(unitHolyCost, mountHolyCost);

      if (unit.mountUnit.getSize() >= 7) {
        holyCost = Math.max(holyCost, 2);
      }

      Command holyCostCommand = CommandFactory.create("#holycost", Integer.toString(holyCost));
      handleCommand(polishedCommands, holyCostCommand);
    }

    // Handle %cost commands such as #chaosrec (these need the full gold cost calculated first)
    Unit.resolvePercentageCostCommands(this, gcost, polishedCommands);
    /* ------------------------------------------------------------------------------------------- */

    // Set all gathered and polished commands to become the unit's own commands now.
    // This is why polish() should ALWAYS be done at the end of generation
    unit.commands = polishedCommands;

    // Check for morale over 50
    for (Command c : polishedCommands) {
      if (c.args.size() == 0) {
        continue;
      }

      // morale 50 if over 50
      if (c.command.equals("#mor")) {
        int mor = c.args.getFirst().getInt();
        if (mor > 50) {
          c.args.set(0, new Arg(50));
        } else if (mor <= 0) {
          c.args.set(0, new Arg(1));
        }
      }
    }

    if (this.isMage()) {
      MageGenerator.ensureSpecialLeadership(unit, false);
    }

    polished = true;
  }

  public Boolean lacksMeleeWeapon() {
    Item weapon = this.getSlot("weapon");
    Item bonusWeapon = this.getSlot("bonusweapon");
    Boolean cannotHaveFreeFist = pose.tags.containsName("no_free_fist");
    Boolean isShapeChangeClass = getClass() == ShapeChangeUnit.class;

    if (cannotHaveFreeFist) {
      return false;
    }

    if (isShapeChangeClass) {
      return false;
    }

    if (this.hasCopyStats()) {
      return false;
    }

    if (weapon != null && weapon.isDominionsEquipment() && weapon.isMeleeWeapon()) {
      return false;
    }

    if (bonusWeapon != null && bonusWeapon.isDominionsEquipment() && bonusWeapon.isMeleeWeapon()) {
      return false;
    }

    return true;
  }

  /**
   * Add or combine a command within a given list of commands. Note that if multiple commands of the same
   * type already exist , this function will combine the command passed in with the last occurrence of
   * the same command type within the list, using Operator logic (SET, ADD, SUBTRACT, etc). The goal of
   * this function is to fill a new list of commands one by one, inserting or combining a new command
   * each time. If done this way, theoretically, there should never be more than one other command of
   * the same type (besides non-unique commands that can exist in parallel, such as #weapon or #armor).
   * @param existingCommands - the list of commands in which to insert or combine a new command
   * @param command - the command to insert
   */
  protected void handleCommand(List<Command> existingCommands, Command command) {
    int copystats = this.getCopyStats();
    Optional<CommandType> type = command.getType();
    Command lastExistingCommand = existingCommands.stream()
      .filter(c -> c.contains(command))
      .reduce((first, second) -> second)
      .orElse(null);

    // If the command handled is combinatory (i.e. has an operator like + or - that will combine its value
    // with another command of the same type) and the unit uses copystats, we need to find the value of the
    // same command within the database stats of the unit
    if (copystats != -1 && lastExistingCommand == null && command.hasCombinatoryArgs()) {
      String value = this.nationGen.units.GetValue(copystats + "", command.command.substring(1));
      
      if (value.isBlank()) {
        value = "0";
      }

      lastExistingCommand = CommandFactory.create(command.command, value);
      existingCommands.add(lastExistingCommand);
    }

    // If the command being handled does not exist within the given set of commands,
    // or if the command being handled is one of a type that can exist multiple times
    // on a unit (such as multiple independent #weapon commands), then add the handled
    // command as an entirely new command in the passed list
    if (lastExistingCommand == null || (type.isPresent() && type.get().canMultipleExist)) {
      existingCommands.add(command.applyArgOperatorsToNothing());
    }

    // If a command of the same type already exists, then combine them (add, subtract, set...) depending
    // on the argument operators (+X, -X, =X, X...) by updating the value of the existing one
    else if (!command.equals(lastExistingCommand)) {
      Command combined = command.combine(lastExistingCommand);
      lastExistingCommand.args.clear();
      lastExistingCommand.args.addAll(combined.args);
    }
  }

  /**
   * Assign a Dominions id to all custom nationgen items that are meant to be equipped in-game;
   * a custom #weapon stronbite will be resolved to #weapon 1001, for example.
   */
  public void resolveCustomWeaponsAndArmor() {
    // Resolve items that are equipped in the Unit's slotmap
    this.slotmap.resolveItems();

    // Resolve all custom items that may be hard-coded commands (i.e. piranhid's strongbite defined in the pose)
    this.gatherCommands()
      .stream()
      .filter(c -> {
        return
          (c.command.equals("#weapon") || c.command.equals("#armor")) &&
          !Generic.isNumeric(c.args.getString(0));
      })
      .forEach(c -> {
        String id = c.args.getString(0);
        Integer resolvedId = this.nationGen.GetCustomItemsHandler().getCustomItemId(id);
        c.args.set(0, new Arg(resolvedId));
      });
  }

  /**
   * Returns unit hp, only basesprite and race count.
   * @return
   */
  public int getHP() {
    Unit u = this;
    int hp = 0;
    for (Command c : u.race.unitcommands) if (c.command.equals("#hp")) hp +=
    c.args.getFirst().getInt();

    for (Command c : u.getSlot("basesprite").getCommands()) if (
      c.command.equals("#hp")
    ) hp += c.args.getFirst().getInt();

    if (hp > 0) return hp;
    else return 10;
  }

  public Boolean isLight() {
    if (this.getTotalProt(true) > 10) return false;

    return true;
  }

  public Boolean isHeavy() {
    if (this.getTotalProt(true) <= 12) return false;

    return true;
  }

  public int getSize() {
    int size = this.getTotalCommandValue("#size", -1);
    int copyStats = this.getCopyStats();
    int id = (copyStats > 0) ? copyStats : this.getId();

    if (size == -1) {
      size = this.nationGen.units.GetInteger(String.valueOf(id), "size", -1);
    }

    if (size == -1) {
      size = Unit.HUMAN_SIZE;
    }

    return size;
  }

  public int getArmorProt() {
    Item armor = this.getSlot("armor");

    if (armor == null) {
      return 0;
    }

    return armor.getIntegerFromDb(ItemProperty.PROTECTION.toDBColumn(), 0);
  }

  public int getTotalProt() {
    return getTotalProt(true);
  }

  public int getTotalProt(Boolean naturalprot) {
    int armorprot = 0;
    int helmetprot = 0;
    int natural = 0;
    Item armor = this.getSlot("armor");
    Item helmet = this.getSlot("helmet");

    for (Command c : this.getAllHandledCommands()) {
      if (c.command.equals("#prot")) {
        natural += c.args.getFirst().getInt();
      }
    }

    for (Item equippedArmor : this.slotmap.getEquippedArmors().toList()) {
      Integer prot = equippedArmor.getIntegerFromDb(ItemProperty.PROTECTION.toDBColumn(), 0);

      if (equippedArmor.hasHelmet()) {
        helmetprot += prot;
      }
      
      else if (equippedArmor.isBodyArmor()) {
        armorprot += prot;
      }
    }

    // To not make special stuff that may not be handled considered light
    if (
      armorprot == 0 &&
      armor != null &&
      !armor.isDominionsEquipment()
    ) {
      armorprot = 10;
    }

    if (
      helmetprot == 0 &&
      helmet != null &&
      !helmet.isDominionsEquipment()
    ) {
      helmetprot = 10;
    }

    double prot = (0.2 * (double) helmetprot + 0.8 * (double) armorprot);

    if (!naturalprot) {
      return (int) Math.round(prot);
    }

    return (int) Math.round(natural + prot - ((natural * prot) / 40));
  }

  public int getTotalEnc() {
    int equippedEnc = 0;
    int naturalEnc = 0;

    for (Command c : this.getAllHandledCommands()) {
      if (c.command.equals("#enc")) {
        naturalEnc += c.args.getFirst().getInt();
      }
    }

    for (Item equippedArmor : this.slotmap.getEquippedArmors().toList()) {
      if (equippedArmor.isDominionsEquipment()) {
        equippedEnc += equippedArmor.getIntegerFromDb(ItemProperty.ENCUMBRANCE.toDBColumn(), 0);
      }
    }

    return (int) (equippedEnc + naturalEnc);
  }

  public int getTotalAtt() {
    int naturalAtt = this.getTotalCommandValue("#att", 0);
    int equippedAtt = 0;

    for (Item item : this.slotmap.items().toList()) {
      if (item.isDominionsEquipment()) {
        equippedAtt += item.getIntegerFromDb(ItemProperty.DEFENCE.toDBColumn(), 0);
      }
    }

    return (int) (equippedAtt + naturalAtt);
  }

  public int getTotalDef() {
    int naturalDef = 0;
    int equippedDef = 0;

    for (Command c : this.getAllHandledCommands()) {
      if (c.command.equals("#def")) {
        naturalDef += c.args.getFirst().getInt();
      }

      if (c.command.equals("#mounted")) {
        equippedDef += 3;
      }
    }

    for (Item item : this.slotmap.items().toList()) {
      if (item.isDominionsEquipment()) {
        equippedDef += item.getIntegerFromDb(ItemProperty.DEFENCE.toDBColumn(), 0);
      }
    }
    return (int) (equippedDef + naturalDef);
  }

  public int getParry() {
    int parry = 0;

    for (Item shield : this.slotmap.getEquippedShields().toList()) {
      parry += shield.getIntegerFromDb(ItemProperty.DEFENCE.toDBColumn(), 0);
    }

    return (int) parry;
  }

  public int getShieldProt() {
    int shieldProt = 0;
    
    for (Item shield : this.slotmap.getEquippedShields().toList()) {
      shieldProt += shield.getIntegerFromDb(ItemProperty.PROTECTION.toDBColumn(), 0);
    }

    return (int) shieldProt;
  }

  public List<String> writeLines(String spritedir) {
    List<String> lines = new ArrayList<>();
    boolean isMontagTemplate = this.isMontagRecruitableTemplate();
    
    if (this.mountUnit != null) {
      lines.addAll(this.mountUnit.writeLines(spritedir));
    }

    lines.add(
      "--- " +
      getName() +
      " (" +
      race.name +
      "), Gold (mount included): " +
      getGoldCost(true) +
      ", Resources (mount included): " +
      getResCost(true, true) +
      ", Roles: " +
      pose.roles +
      " (" +
      pose.name +
      ")"
    );

    if (isMontagTemplate) {
      lines.add("------");
      lines.addAll(this.writeMontagTemplateHeader());
    }

    lines.add("------");
    lines.add("--- OFFSET DEBUG: ");
    if (this.getSlot("weapon") != null) {
      lines.add(
        "-- Weapon: " +
        this.getSlot("weapon").getOffsetX() +
        ", " +
        this.getSlot("weapon").getOffsetY()
      );
    }
    if (this.getSlot("armor") != null) {
      lines.add(
        "-- Armor: " +
        this.getSlot("armor").getOffsetX() +
        ", " +
        this.getSlot("armor").getOffsetY()
      );
    }
    if (this.getSlot("offhand") != null) {
      lines.add(
        "-- Offhand: " +
        this.getSlot("offhand").getOffsetX() +
        ", " +
        this.getSlot("offhand").getOffsetY()
      );
    }
    lines.add("------");
    lines.add("--- Generation tags: " + this.tags);

    lines.add(
      "--- Applied filters: " +
      this.appliedFilters.stream()
        .map(f ->
          f.name + (f.name.equals("MAGICPICKS") ?
            " (" + ((MagicFilter) f).pattern.getPrice() + ")":
            ""
          )
        )
        .collect(Collectors.joining(", "))
    );

    if (this.survivability != 0.0) {
      lines.add("--- Unit has a " +
        String.format("%.2f", this.survivability) +
        " survivability rating."
      );
    }

    if (this.capOnlyChance != 0.0) {
      lines.add("--- Unit had a " +
        String.format("%.2f", this.capOnlyChance * 100) +
        "% chance of being cap-only."
      );
    }
    lines.add("------");

    lines.add("#newmonster " + id);

    if (this.getSlot("basesprite") != null) {
      lines.add("#spr1 \"" + (spritedir + "/unit_" + this.id + "_a.tga\""));
      lines.add("#spr2 \"" + (spritedir + "/unit_" + this.id + "_b.tga\""));
    }

    // Write commands before weapons/armor so that #clearweapons/#cleararmor (after #copystats) doesn't clear them
    lines.addAll(writeCommandLines());
    lines.addAll(writeWeaponLines(this.getEquippedWeapons()));
    lines.addAll(writeArmorLines(this.getEquippedArmors()));

    if (!this.name.toString(this).equals("UNNAMED")) {
      lines.add("#name \"" + name.toString(this) + "\"");
    }

    if (!isMontagTemplate) {
      lines.add("");
      lines.add("DEBUG INFORMATION:");
      lines.addAll(writeDebugSlotMapLines(this.slotmap));
    }

    lines.add("#end");
    lines.add("");

    return lines;
  }

  private List<String> writeMontagTemplateHeader() {
    List<String> lines = new ArrayList<>();
    String montagId = this.getFirstshapeIdForMontag();
    lines.add("--- MONTAG TEMPLATE (" + montagId + ")");
    lines.add("-- This is a recruitable template that will spawn one of several possible variant creatures once recruited.");
    lines.add("-- The stats below are a mean of each score of all variants, so as to give an idea of the possible results.");
    lines.add("-- This template's possible spawns are all of the units with the #montag " + montagId + " command.");
    return lines;
  }

  /**
   * Calculates recruitment point cost as gcost from race+pose+basesprite
   */
  protected Optional<String> writeRecpointsLine() {
    int recPointsCost = this.getRecPointsCost();

    if (recPointsCost == -1) {
      return Optional.empty();
    }

    return Optional.of("#rpcost " + recPointsCost);
  }

  private int getRecPointsCost() {
    Optional<Command> explicitRpCost = this.getCommand(CommandType.RPCOST.toString());

    if (explicitRpCost.isPresent()) {
      Command command = explicitRpCost.get();
      Arg rpcostArg = command.args.getFirst();
      Operator operator = rpcostArg.getOperator().orElse(Operator.SET);
      int gcost;
      int rpcost;

      // If a set amount of RPs exist, such as #rpcost 10000, return that
      if (operator == Operator.SET) {
        rpcost = rpcostArg.getInt();
      }

      else {
        // If the existing #rpcost command is a modifier, autocalc RPs and apply the modifier to them
        gcost = this.getGoldCost(true);
        rpcost = command.combine(this.getAutocalcRps(gcost)).args.getFirst().getInt();
      }

      return rpcost;
    }

    else if (this.hasCopyStats()) {
      return -1;
    }

    int gcost = this.getGoldCost(true);
    return this.getAutocalcRps(gcost);
  }

  private int getAutocalcRps(int gcost) {
    // Per modding manual, the RP autocalc value should be the unit's gold value * 1000
    return gcost * 1000;
  }

  private int getAutocalcRps(Command gcost) {
    return this.getAutocalcRps(gcost.args.getInt(0));
  }

  protected List<ItemData> getEquippedWeapons() {
    List<ItemData> weapons = new ArrayList<>();

    // Get weapons that were added directly through templates, like
    // natural weapons from bases (such as nagas)
    this.gatherCommands()
      .stream()
      .filter(c -> c.command.equals("#weapon") && c.args.getInt(0) > 0)
      .forEach(c -> {
        String id = c.args.getString(0);
        ItemData weaponData = new ItemData(id, "", this.nationGen, ItemType.WEAPON);
        weapons.add(weaponData);
      });

    // Get weapon items from the unit's slot map, and do a check
    // to make sure the custom ones are resolved into ids by now
    this.slotmap
      .getEquippedWeapons()
      .forEach(weapon -> {
        ItemData weaponData = new ItemData(weapon);
        weapons.add(weaponData);
      });

    return weapons;
  }

  protected List<ItemData> getEquippedArmors() {
    List<ItemData> armors = new ArrayList<>();

    // Get armors that were added directly through data templates as comands
    this.gatherCommands()
      .stream()
      .filter(c -> c.command.equals("#armor") && c.args.getInt(0) > 0)
      .forEach(c -> {
        String id = c.args.getString(0);
        ItemData armorData = new ItemData(id, "", this.nationGen, ItemType.ARMOR);
        armors.add(armorData);
      });

    // Get armors equipped in the Unit's slotmap
    this.slotmap.getEquippedArmors()
    .forEach(armor -> {
      ItemData armorData = new ItemData(armor);
      armors.add(armorData);
    });

    return armors;
  }

  private List<String> writeCommandLines() {
    List<String> lines = new ArrayList<>();

    writeBodytypeLine().ifPresent(lines::add);
    writeRecpointsLine().ifPresent(lines::add);

    for (Command c : this.commands) {
      // Exceptionally handled above
      if (c.command.equals("#rpcost")) {
        continue;
      }

      if (c.args.size() > 0) {
        if (c.command.equals("#mountmnr")) {
          if (this.mountUnit.isIdResolved() == false) {
            throw new IllegalStateException(
              "Id for MountUnit " +
              c.args.getString(0) +
              " in Unit with pose '" +
              this.pose.getName() +
              "' is not resolved!"
            );
          }

          lines.add(c.command + " " + this.mountUnit.id);
        }

        else if (
          // Item lines will be written later
          !c.command.equals("#itemslots") &&
          !c.command.equals("#weapon") &&
          !c.command.equals("#armor")
        ) {
          lines.add(c.toModLine());
        }
      }
      
      else {
        lines.add(c.command);
      }
    }

    lines.add("#itemslots " + this.getItemSlots());
    return lines;
  }

  public List<String> writeWeaponLines(List<ItemData> weaponData) {
    List<String> lines = new ArrayList<>();

    // Sort weapons by descending range so that weapons with higher range appear higher.
    // When units with multiple ranged weapons have the shorter range defined first,
    // they will close into battle to fire with the shortest range (i.e. naga archers with a spit).
    weaponData.sort((a, b) -> {
      int unitStrength = this.getTotalCommandValue("#str", 0);
      int rangeA = a.getWeaponRange(unitStrength);
      int rangeB = b.getWeaponRange(unitStrength);
      return rangeB - rangeA;
    });

    // Get id and name of weapons and add to the list of #weapon lines
    weaponData.forEach(itemData -> {
      if (itemData.isCustomIdResolved() == false) {
        throw new IllegalArgumentException(
          this.name +
          " unit (pose: " +
          this.pose.name +
          ", race: " +
          this.race.name +
          ", nation: " +
          this.nation.name + " with seed " + this.nation.getSeed() +
          ") has a custom weapon whose id was not resolved: " +
          itemData.getId()
        );
      };
      
      lines.add(
        "#weapon " +
        itemData.getId() +
        " --- " +
        itemData.getDisplayName() +
        ((itemData.hasName()) ? " / " + itemData.getName() : "")
      );
    });

    return lines;
  }

  public List<String> writeArmorLines(List<ItemData> armors) {
    List<String> lines = new ArrayList<>();

    armors.forEach(itemData -> {
      if (itemData.isCustomIdResolved() == false) {
        throw new IllegalArgumentException(
          this.name +
          " unit (pose: " +
          this.pose.name +
          ", race: " +
          this.race.name +
          ", nation: " +
          this.nation.name + " with seed " + this.nation.getSeed() +
          ") has a custom armor whose id was not resolved: " +
          itemData.getId()
        );
      }

      lines.add(
        "#armor " +
        itemData.getId() +
        " --- " +
        itemData.getDisplayName() +
        ((itemData.hasName()) ? " / " + itemData.getName() : "")
      );
    });

    return lines;
  }

  public List<String> writeDebugSlotMapLines(SlotMap slotMap) {
    List<String> lines = new ArrayList<>();

    lines.add("---- Slotmap (most recently assigned; i.e. effectively equipped <- oldest assigned; i.e. effectively unused)");

    for (String slotName : slotMap.getSlots()) {
      String slotDescription = "-- " + slotName + ": ";
      List<Item> itemsInSlot = slotMap.getItemTrailInSlotStack(slotName);

      for (Item item : itemsInSlot) {
        String itemName = (item == null) ? "null" : item.getName();
        slotDescription += itemName + " <- ";
      }

      if (itemsInSlot.size() > 0) {
        slotDescription = slotDescription.substring(0, slotDescription.length() - 4);
      }

      lines.add(slotDescription);
    }

    lines.add("----");
    return lines;
  }

  public void writeSprites(String spritedir) {
    if (getSlot("basesprite") == null) {
      throw new IllegalStateException(
        "Unit " + this.name + " has no basesprite and can't be rendered!"
      );
    }

    FileUtil.writeTGA(
      this.render(0),
      "/mods/" + spritedir + "/unit_" + this.id + "_a.tga"
    );

    // The super awesome attack sprite generation:
    FileUtil.writeTGA(
      this.render(-5),
      "/mods/" + spritedir + "/unit_" + this.id + "_b.tga"
    );
  }

  public BufferedImage render() {
    return render(0);
  }

  public String getMountOffsetSlot() {
    return Generic.getAllUnitTags(this)
      .getString("mount_offset_slot")
      .orElse("mount");
  }

  public BufferedImage render(int offsetX) {
    if (this.getClass() == ShapeChangeUnit.class) return null;

    // Get width and height;
    Dimension d = getSpriteDimensions();

    BufferedImage combined = new BufferedImage(
      d.width,
      d.height,
      BufferedImage.TYPE_3BYTE_BGR
    );
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

    for (String s : pose.renderorder) {
      if (
        s.equals(mountslot) ||
        (!pose.tags.containsName("non_mount_overlay") &&
          s.equals("overlay") &&
          getSlot(s) != null &&
          getSlot("overlay").getOffsetX() == 0 &&
          getSlot("overlay").getOffsetY() == 0)
      ) {
        renderSlot(g, this, s, false);
      }
      
      else if (
        s.equals("basesprite") && slotmap.get(mountslot) == null
      ) {
        renderSlot(g, this, s, false);
      }

      else if (
        s.equals("offhandw") &&
        (getSlot("offhand") != null && !getSlot("offhand").isArmor())
      ) {
        renderSlot(g, this, "offhand", true);
      }

      else if (
        s.equals("offhanda") &&
        (getSlot("offhand") != null && getSlot("offhand").isArmor())
      ) {
        renderSlot(g, this, "offhand", true);
      }

      else {
        renderSlot(g, this, s, true);
      }
    }
  }

  private void renderSlot(Graphics g, Unit u, String slot, Boolean useoffset) {
    List<Item> possibleitems = slotmap
      .items()
      .filter(i -> {
        String itemRenderSlot = i.renderslot;
        boolean isRenderedOffhand = itemRenderSlot.equals("offhanda") || itemRenderSlot.equals("offhandw");

        if (isRenderedOffhand && slot.equals("offhand")) {
          itemRenderSlot = "offhand";
        }

        return slot.equals(itemRenderSlot);
      })
      .collect(Collectors.toList());

    for (int i = 10; i >= 1; i--) {
      for (Item item : possibleitems) {
        if (item.renderprio == i) {
          renderItem(g, item, useoffset);
          return;
        }
      }
    }
  }

  private void renderItem(Graphics g, Item i, Boolean useoffset) {
    if (i == null) return;

    if (useoffset) {
      String mountslot = getMountOffsetSlot();

      int offsetx = 0;
      int offsety = 0;
      if (this.getSlot(mountslot) != null) {
        offsetx += getSlot(mountslot).getOffsetX();
        offsety += getSlot(mountslot).getOffsetY();
      }

      i.render(g, true, offsetx, offsety, this.color);
    } else i.render(g, false, 0, 0, this.color);
  }

  public Dimension getSpriteDimensions() {
    int w = 64;
    int h = 64;

    String mountslot = getMountOffsetSlot();
    if (this.slotmap.get(mountslot) != null) {
      BufferedImage base = FileUtil.readImage(
        this.slotmap.get(mountslot).sprite
      );
      w = Math.max(w, base.getWidth());
      h = Math.max(h, base.getHeight());
    } else if (this.slotmap.get("basesprite") != null) {
      BufferedImage base = FileUtil.readImage(
        this.slotmap.get("basesprite").sprite
      );
      w = Math.max(w, base.getWidth());
      h = Math.max(h, base.getHeight());
    } else {
      System.out.println(
        "No base sprite for " +
        pose.roles +
        " unit of " +
        race.name +
        ", id " +
        id +
        "."
      );
    }

    return new Dimension(w, h);
  }
}
