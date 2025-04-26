package nationGen.units;

import com.elmokki.Drawing;
import com.elmokki.Generic;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.entities.Filter;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.misc.Arg;
import nationGen.misc.Command;
import nationGen.misc.FileUtil;
import nationGen.naming.Name;
import nationGen.naming.NamePart;
import nationGen.nation.Nation;

public class MountUnit extends Unit {

  public Unit otherForm;
  public Mount mountForm;
  boolean sacred = false;
  private int gcost = 0;
  private NationGenAssets assets;

  // otherForm is the mounted rider form, whereas mountForm is the actual, riderless mount
  public MountUnit(
    NationGen nationGen,
    NationGenAssets assets,
    Race race,
    Pose pose,
    Unit otherForm,
    Mount mountForm
  ) {
    super(nationGen, race, pose);
    this.assets = assets;
    this.otherForm = otherForm;
    this.mountForm = mountForm.getCopy();
  }

  public void polish(NationGen n, Nation nation) {
    Filter sf = new Filter(n);
    sf.name = "Special unit";

    // Copy sacredness from main form
    if (otherForm != null) {
      List<Command> mountedFormCommands = otherForm.getCommands();

      for (Command c : mountedFormCommands) {
        if (c.command.equals("#holy") && !mountForm.tags.containsName("mount")) {
          sacred = true;
        }
        
        else if (c.command.equals("#holy") && mountForm.tags.containsName("mount")) {
          sacred = true;
        }
        
        // Copy rider's fire resistance if the rider has a heat aura and the mount's resistance is worse
        else if (c.command.equals("#heat")) {
          String resistanceTag = "#fireres";
          int inheritableResistance = getResistanceFromRiderToInherit(mountedFormCommands, resistanceTag);
          sf.commands.add(Command.parse(resistanceTag + " " + inheritableResistance));
        }
        
        // Copy rider's cold resistance if the rider has a chill aura and the mount's resistance is worse
        else if (c.command.equals("#cold")) {
          String resistanceTag = "#coldres";
          int inheritableResistance = getResistanceFromRiderToInherit(mountedFormCommands, resistanceTag);
          sf.commands.add(Command.parse(resistanceTag + " " + inheritableResistance));
        }
        
        // Copy rider's poison resistance if the rider has a poison aura and the mount's resistance is worse
        else if (c.command.equals("#poisoncloud")) {
          String resistanceTag = "#poisonres";
          int inheritableResistance = getResistanceFromRiderToInherit(mountedFormCommands, resistanceTag);
          sf.commands.add(Command.parse(resistanceTag + " " + inheritableResistance));
        }
      }
    }

    // Copy commands from this form
    for (Command c : mountForm.commands) {
      if (c.command.equals("#name") && c.args.size() > 0) {
        c.args.set(0, new Arg(Generic.capitalize(c.args.get(0).get())));
        name = new Name();

        name.type = NamePart.newNamePart(
          Generic.capitalize(c.args.get(0).get()),
          null
        );
      }

      if (!c.command.startsWith("#spr")) {
        sf.commands.add(c);
      }

      if (c.command.equals("#gcost")) {
        sf.commands.add(c);
        gcost = c.args.get(0).getInt();
      }
    }

    // ...and other Form
    if (otherForm != null) {
      // Inherit nametype and maxage
      boolean maxagefound = false;
      for (Command c : otherForm.getCommands()) if (
        c.command.equals("#maxage") || c.command.equals("#nametype")
      ) {
        sf.commands.add(c);
        if (c.command.equals("#maxage")) maxagefound = true;
      }

      if (!maxagefound) {
        sf.commands.add(new Command("#maxage", new Arg(50)));
        otherForm.commands.add(new Command("#maxage", new Arg(50)));
      }

      // Inherit from race/pose
      // Careful here since this stuff generally is definite instead of relative definitions
      List<Command> clist = new ArrayList<Command>();
      if (!otherForm.race.tags.containsName("noinheritance")) clist.addAll(
        otherForm.race.unitcommands
      );
      if (!otherForm.pose.tags.containsName("noinheritance")) clist.addAll(
        otherForm.pose.getCommands()
      );
      for (Command c : clist) {
        if (assets.secondShapeRacePoseCommands.contains(c.command)) {
          sf.commands.add(c);
        }
      }

      // Inheritance from filter bonus abilities
      for (Filter f : otherForm.appliedFilters) {
        boolean shape = false;
        for (Command c : f.getCommands()) if (
          c.command.contains("shape")
        ) shape = true;

        if (f.tags.containsName("noinheritance") != shape) continue;

        // Add filters
        for (Command c : f.getCommands()) {
          if (
            assets.secondShapeNonMountCommands.contains(c.command) &&
            !mountForm.tags.containsName("mount")
          ) {
            sf.commands.add(c);
            //handleCommand(commands, c);
          }

          if (
            assets.secondShapeMountCommands.contains(c.command) &&
            mountForm.tags.containsName("mount")
          ) {
            sf.commands.add(c);
          }
        }
      }
    }

    if (sf.commands.size() > 0) {
      this.appliedFilters.add(sf);
    }
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
    List<Command> mountCommands = this.mountForm.getCommands();
    Optional<Command> potentialMountResistance = mountCommands
      .stream()
      .filter(mfc -> mfc.command.equals(resTag))
      .findFirst();

    if (potentialMountResistance.isPresent()) {
      int mountResistance = potentialMountResistance.get().args.getInt(0);
      int higherResistance = Math.max(riderResistance, mountResistance);
      return higherResistance;
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

    mountForm.tags
      .getString("recolormask")
      .ifPresent(file -> {
        BufferedImage mask = FileUtil.readImage(file);
        Drawing.recolor(mask, this.color);

        g.drawImage(mask, xoffset, 0, null);
      });

    return base;
  }

  @Override
  public void writeSprites(String spritedir) {
    // Handle sprites

    BufferedImage spr1 = null;
    for (Command c : mountForm.commands) {
      // First sprite
      if (c.command.equals("#spr1")) {
        if (c.args.get(0).get().equals("greyscale")) {
          int greyscaleunits = 0;
          if (c.args.size() > 1) greyscaleunits = c.args.get(1).getInt();

          spr1 = Drawing.greyscale(otherForm.render(), greyscaleunits);
        } else {
          spr1 = copyImage(FileUtil.readImage(c.args.get(0).get()), 0);
        }
        FileUtil.writeTGA(
          spr1,
          "/mods/" + spritedir + "/mount_" + id + "_a.tga"
        );
      }
    }

    for (Command c : mountForm.commands) {
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
          "/mods/" + spritedir + "/mount_" + id + "_b.tga"
        );
      }
    }
  }

  @Override
  public List<String> writeLines(String spritedir) {
    List<String> lines = new ArrayList<>();

    // Write the unit text
    if (otherForm != null) lines.add(
      "--- Mount form for " + otherForm.getName()
    );
    else lines.add("--- Special unit " + getName());

    lines.add("#newmonster " + id);

    List<Command> commands = getCommands();
    boolean hasItemSlots = false;

    // Own non-gcost commands first due to #copystats
    for (Command c : commands) {
      if (c.command.equals("#gcost")) {
        continue;
      }
      
      else if (c.command.equals("#itemslots")) {
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

    if (mountForm.commands.stream().anyMatch(c -> c.command.equals("#spr1"))) {
      lines.add("#spr1 \"" + spritedir + "/mount_" + id + "_a.tga" + "\"");
    }
  
    if (mountForm.commands.stream().anyMatch(c -> c.command.equals("#spr2"))) {
      lines.add("#spr2 \"" + spritedir + "/mount_" + id + "_b.tga" + "\"");
    }

    if (mountForm.keepname && otherForm != null) lines.add(
      "#name \"" + otherForm.name + "\""
    );
    
    if (sacred) lines.add("#holy");

    if (gcost != 0) lines.add("#gcost " + gcost);

    // If there's no #copystats or defined body type, define body type (as probably humanoid unless shenanigans have been done)
    writeBodytypeLine().ifPresent(lines::add);

    // Write weapons and armor
    lines.addAll(writeWeaponLines());
    lines.addAll(writeArmorLines());

    // Write itemslots if they were skipped before
    if (hasItemSlots) lines.add("#itemslots " + this.getItemSlots());

    lines.add("#end");
    lines.add("");

    return lines;
  }
}
