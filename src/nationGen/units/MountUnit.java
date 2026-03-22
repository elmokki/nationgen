package nationGen.units;

import com.elmokki.Drawing;
import com.elmokki.Generic;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import nationGen.NationGen;
import nationGen.entities.Filter;
import nationGen.items.Item;
import nationGen.misc.Arg;
import nationGen.misc.Command;
import nationGen.misc.FileUtil;
import nationGen.naming.Name;
import nationGen.naming.NamePart;
import nationGen.nation.Nation;

public class MountUnit extends Unit {

  public Unit rider;
  public Mount mount;
  boolean sacred = false;
  private int gcost = 0;

  /**
   * A MountUnit instance is the object that connects a cavalry Unit (the rider)
   * and the Mount instance (which contains the mount's stats themselves) together.
   * 
   * Thus, a MountUnit has a circular reference to its rider Unit, since they both point
   * to each other. Note that the rider Unit is effectively a parent, and the MountUnit is
   * a child in this circular relationship.
   * 
   * A Unit may exist without a MountUnit, but the reverse is not true. As a result of this,
   * a MountUnit should never be allowed to create a new rider Unit, even when copying itself!
   * 
   * A MountUnit is expected to be created when a Unit gets equipped with an Item that happens
   * to contain a mount (i.e. a #mountmnr command). Note that the item's #mountmnr will point to
   * the Mount to use, but will also contain further customization of it (like a barding, or a new
   * sprite).
   * 
   * The Mount reference is the data-parsed mount definition (mounts.txt), which contains all
   * of the mount creature's baseline stats.
   */
  public MountUnit(
    Item mountItem,
    Unit rider,
    NationGen nationGen
  ) {
    super(nationGen, rider.race, rider.pose);
    this.rider = rider;
    this.loadMountItemData(mountItem);
  }

  /**
   * HIC SUNT DRACONIS
   * The copy constructor of a MountUnit should always pass a reference to
   * the other MountUnit's rider Unit. A MountUnit should never be allowed
   * to create a new rider, or we will incur in reference problems.
   * @param mountUnit
   * @param parentUnit
   */
  public MountUnit(MountUnit mountUnit, Unit parentUnit) {
    super(mountUnit);
    this.rider = parentUnit;
    this.mount = new Mount(mountUnit.mount);
    this.sacred = mountUnit.sacred;
    this.gcost = mountUnit.gcost;
  }

  @Override
  public List<Command> getCommands() {
    List<Command> commands = super.getCommands();
    commands.addAll(this.mount.getCommands());
    return commands;
  }

  public void loadMountItemData(Item mountItem) {
    if (mountItem.isMountItem() == false) {
      throw new IllegalArgumentException("Expected mountItem to have a #mountmnr command but found none!");
    }

    mountItem.getCommands()
      .stream()
      .filter(c -> c.command.equals("#mountmnr"))
      .findFirst()
      .ifPresent(mountMnrCommand -> {
        String mountId = mountMnrCommand.args.get(0).get();
        this.mount = this.nationGen.getAssets().getMount(mountId);

        if (mountItem.sprite.isBlank() == false) {
          this.mount.commands.removeIf(c -> {
            return c.command.equals("#spr1") || c.command.equals("#spr2") || c.command.equals("#copyspr");
          });
          this.mount.commands.add(Command.args("#spr1", "." + mountItem.sprite));
          this.mount.commands.add(Command.args("#spr2", "shift"));
        }

        // If the mount item also contains a barding, add it to the mount instance
        if (mountItem.isBarding()) {
          this.mount.commands.add(Command.args("#armor", mountItem.getBardingId()));
        }
      });
  }

  public int getGoldCost() {
    if (this.polished) {
      return this.gcost;
    }

    return this.calculateGoldCost();
  }

  public int getResCost() {
    return this.getResCost(false, false);
  }

  public int calculateGoldCost() {
    int gcost = 0;

    for (Command c : this.mount.getCommands()) {
      if (c.command.equals("#gcost")) {
        gcost += c.args.get(0).getInt();
      }
    }

    return gcost;
  }

  public void polish(NationGen n, Nation nation) {
    if (this.polished) {
      return;
    }

    Filter polishFilter = new Filter(n);
    polishFilter.name = "Mount unit";

    // Copy sacredness from main form
    List<Command> mountedRiderCommands = rider.getCommands();

    // Inherit relevant commands from rider
    for (Command c : mountedRiderCommands) {
      if (c.command.equals("#holy")) {
        this.sacred = true;
      }
      
      // Copy rider's fire resistance if the rider has a heat aura and the mount's resistance is worse
      else if (c.command.equals("#heat")) {
        String resistanceTag = "#fireres";
        int inheritableResistance = getResistanceFromRiderToInherit(mountedRiderCommands, resistanceTag);
        polishFilter.commands.add(Command.parse(resistanceTag + " " + inheritableResistance));
      }
      
      // Copy rider's cold resistance if the rider has a chill aura and the mount's resistance is worse
      else if (c.command.equals("#cold")) {
        String resistanceTag = "#coldres";
        int inheritableResistance = getResistanceFromRiderToInherit(mountedRiderCommands, resistanceTag);
        polishFilter.commands.add(Command.parse(resistanceTag + " " + inheritableResistance));
      }
      
      // Copy rider's poison resistance if the rider has a poison aura and the mount's resistance is worse
      else if (c.command.equals("#poisoncloud")) {
        String resistanceTag = "#poisonres";
        int inheritableResistance = getResistanceFromRiderToInherit(mountedRiderCommands, resistanceTag);
        polishFilter.commands.add(Command.parse(resistanceTag + " " + inheritableResistance));
      }
    }

    // And remember to copy commands from the actual base mount stats
    for (Command c : this.mount.getCommands()) {
      // Set mount name
      if (c.command.equals("#name") && c.args.size() > 0) {
        c.args.set(0, new Arg(Generic.capitalize(c.args.get(0).get())));
        this.name = new Name();

        this.name.type = NamePart.newNamePart(
          Generic.capitalize(c.args.get(0).get()),
          null
        );
      }
    }

    if (polishFilter.commands.size() > 0) {
      this.appliedFilters.add(polishFilter);
    }

    this.gcost = this.calculateGoldCost();
    this.polished = true;
  }

  /**
   * Checks the rider's commands for their related elemental resistance tag and
   * compares it to the mount's resistance. Then returns the higher resistance.
   * @param riderCommands
   * @param resTag
   * @return higher resistance between rider and mount for mount to inherit
   */
  private int getResistanceFromRiderToInherit(List<Command> riderCommands, String resTag) {
    Optional<Command> potentialResistance = riderCommands
      .stream()
      .filter(mfc -> mfc.command.equals(resTag))
      .findFirst();

    if (potentialResistance.isPresent() == false) {
      return 0;
    }

    int riderResistance = potentialResistance.get().args.getInt(0);
    List<Command> mountCommands = this.mount.getCommands();
    Optional<Command> potentialMountResistance = mountCommands
      .stream()
      .filter(mfc -> mfc.command.equals(resTag))
      .findFirst();

    if (potentialMountResistance.isPresent()) {
      int mountResistance = potentialMountResistance.get().args.getInt(0);
      int combinedResistance = Math.max(riderResistance, mountResistance);
      return combinedResistance;
    }

    else {
      return riderResistance;
    }
  }

  private BufferedImage copyImage(BufferedImage image, int xoffset) {
    BufferedImage base = new BufferedImage(
      image.getWidth(),
      image.getHeight(),
      BufferedImage.TYPE_3BYTE_BGR
    );
    Graphics g = base.getGraphics();
    g.drawImage(image, xoffset, 0, null);

    this.mount.tags
      .getString("recolormask")
      .ifPresent(file -> {
        BufferedImage mask = FileUtil.readImage(file);
        Drawing.recolor(mask, this.color);

        g.drawImage(mask, xoffset, 0, null);
      });

    return base;
  }

  private String getSpritePath(String spritedir, String spriteCommand) {
    return spritedir + "/" + this.getSpriteFilename(spriteCommand);
  }

  private String getSpriteFilename(String spriteCommand) {
    String letter = (spriteCommand.equals("#spr1")) ? "a" : "b";
    return "mount_" + this.id + "_" + letter + ".tga";
  }

  @Override
  public void writeSprites(String spritedir) {
    // Handle sprites

    BufferedImage spr1 = null;
    for (Command c : this.mount.commands) {
      // First sprite
      if (c.command.equals("#spr1")) {
        if (c.args.get(0).get().equals("greyscale")) {
          int greyscaleunits = 0;
          if (c.args.size() > 1) greyscaleunits = c.args.get(1).getInt();

          spr1 = Drawing.greyscale(rider.render(), greyscaleunits);
        } else {
          spr1 = copyImage(FileUtil.readImage(c.args.get(0).get()), 0);
        }
        FileUtil.writeTGA(
          spr1,
          "/mods/" + this.getSpritePath(spritedir, c.command)
        );
      }
    }

    for (Command c : this.mount.commands) {
      if (c.command.equals("#spr2")) {
        BufferedImage spr2;
        if (c.args.get(0).get().equals("shift")) {
          if (spr1 == null) {
            throw new IllegalStateException(
              "Can't shift attack sprite because no #spr1 command found for mount unit!"
            );
          }

          spr2 = copyImage(spr1, -5);
        } else {
          spr2 = copyImage(FileUtil.readImage(c.args.get(0).get()), 0);
        }
        FileUtil.writeTGA(
          spr2,
          "/mods/" + this.getSpritePath(spritedir, c.command)
        );
      }
    }
  }

  @Override
  public List<String> writeLines(String spritedir) {
    List<String> lines = new ArrayList<>();
    lines.add("--- Mount form for " + rider.getName());
    lines.add("#newmonster " + this.id);

    List<Command> commands = this.getCommands();
    boolean hasItemSlots = false;

    // Own non-gcost commands first due to #copystats
    for (Command c : commands) {
      if (c.command.equals("#gcost")) {
        continue;
      }

      if (c.command.startsWith("#spr")) {
        continue;
      }
      
      if (c.command.equals("#itemslots")) {
        hasItemSlots = true;
      }
      
      else if (
        // Leave weapons and armors to be handled below
        !c.command.equals("#weapon") &&
        !c.command.equals("#armor")
      ) {
        lines.add(c.toModLine());
      }
    }

    if (this.mount.commands.stream().anyMatch(c -> c.command.equals("#spr1"))) {
      lines.add("#spr1 \"" + this.getSpritePath(spritedir, "#spr1") + "\"");
    }
  
    if (this.mount.commands.stream().anyMatch(c -> c.command.equals("#spr2"))) {
      lines.add("#spr2 \"" + this.getSpritePath(spritedir, "#spr2") + "\"");
    }

    if (this.mount.isNamed()) {
      lines.add("#name \"" + this.mount.getName() + "\"");
    }
    
    if (sacred) {
      lines.add("#holy");
    }

    if (gcost != 0) {
      lines.add("#gcost " + gcost);
    }

    // If there's no #copystats or defined body type, define body type (as probably humanoid unless shenanigans have been done)
    writeBodytypeLine().ifPresent(lines::add);

    // Write weapons and armor
    lines.addAll(this.writeWeaponLines(this.getEquippedWeapons()));
    lines.addAll(this.writeArmorLines(this.getEquippedArmors()));

    // Write itemslots if they were skipped before
    if (hasItemSlots) {
      lines.add("#itemslots " + this.getItemSlots());
    }

    lines.add("#end");
    lines.add("");

    return lines;
  }
}
