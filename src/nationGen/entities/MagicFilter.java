package nationGen.entities;

import java.util.ArrayList;
import java.util.List;
import nationGen.NationGen;
import nationGen.magic.MagicPath;
import nationGen.magic.MagicPattern;
import nationGen.misc.Command;

public class MagicFilter extends Filter {

  public MagicPattern pattern;
  public List<MagicPath> prio;

  public MagicFilter(NationGen nationGen) {
    super(nationGen);
    this.name = "MAGICPICKS";
  }

  public List<Command> getCommands() {
    List<Command> coms = new ArrayList<>(commands);

    // Price
    if (this.name.equals("MAGICPICKS")) coms.add(
      Command.args("#gcost", "+" + pattern.getPrice())
    );

    // Magic

    if (prio == null || pattern == null) {
      throw new IllegalStateException(
        "WARNING: An unit had incorrect magic specified by NationGen."
      );
    }

    coms.addAll(pattern.getMagicCommands(prio));

    return coms;
  }
}
