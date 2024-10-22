package nationGen.naming;

import com.elmokki.Generic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import nationGen.NationGen;
import nationGen.entities.Filter;
import nationGen.magic.MagicPath;
import nationGen.misc.Arg;
import nationGen.misc.Args;
import nationGen.misc.Command;
import nationGen.units.Unit;

public class NamePart extends Filter {

  public boolean weak = false;
  public List<MagicPath> elements = new ArrayList<>();
  public int minimumelements = 0;
  public int rank = -1;

  public NamePart(NationGen nationGen) {
    super(nationGen);
  }

  public static NamePart newNamePart(String text, NationGen nationGen) {
    NamePart np = new NamePart(nationGen);
    np.name = text;
    return np;
  }

  public String toString(Unit u) {
    if (u == null) return this.toString();

    String str = this.name;

    for (Args args : this.tags.getAllArgs("commandvariant")) {
      String command = args.get(0).get();
      for (Command c : u.getCommands()) if (c.command.equals(command)) {
        if (
          !(args.contains(new Arg("negative")) &&
            c.args.get(0).getInt() >= 0) &&
          !(args.contains(new Arg("positive")) && c.args.get(0).getInt() <= 0)
        ) {
          str = args.get(args.size() - 1).get();
          break;
        }
      }
    }
    for (Args args : this.tags.getAllArgs("posetagvariant")) {
      String command = args.get(0).get();
      if (u.pose.tags.containsName(command)) {
        str = args.get(1).get();
        break;
      }
    }
    for (Args args : this.tags.getAllArgs("racetagvariant")) {
      String command = args.get(0).get();
      if (u.race.tags.containsName(command)) {
        str = args.get(1).get();
        break;
      }
    }

    for (Args args : this.tags.getAllArgs("racevariant")) {
      if (u.race.name.equals(args.get(0).get())) {
        str = args.get(1).get();
        break;
      }
    }

    return str;
  }

  public String toString() {
    return this.name;
  }

  @Override
  public void handleOwnCommand(Command command) {
    try {
      if (command.command.equals("#rank")) this.rank = command.args
        .get(0)
        .getInt();
      else super.handleOwnCommand(command);
    } catch (IndexOutOfBoundsException e) {
      throw new IllegalArgumentException(
        "WARNING: " +
        command +
        " has insufficient arguments (" +
        this.name +
        ")",
        e
      );
    }
  }

  public NamePart getCopy() {
    NamePart part = new NamePart(this.nationGen);
    part.name = this.name;
    part.weak = this.weak;
    part.elements.addAll(this.elements);
    part.minimumelements = this.minimumelements;
    part.types.addAll(this.types);
    part.tags.addAll(this.tags);
    part.themes.addAll(this.themes);

    return part;
  }

  public static NamePart fromLine(String line, NationGen ng) {
    if (line.startsWith("-") || line.equals("")) return null;

    NamePart part = new NamePart(ng);
    List<String> args = Generic.parseArgs(line);

    part.name = args.get(0);
    if (args.size() > 1) {
      part.minimumelements = Integer.parseInt(args.get(1));
    }
    // If part isn't longer it's minimum elements must be 0, as elements will be defined later
    if (args.size() < 3) {
      part.minimumelements = 0;
      return part;
    }

    args.remove(0);
    args.remove(0);

    for (String str : args) {
      if (str.equals("weak")) part.weak = true;
      else {
        Optional<MagicPath> path = MagicPath.findFromName(str);
        if (path.isPresent()) part.elements.add(path.get());
        else part.tags.addName(str);
      }
    }

    return part;
  }
}
