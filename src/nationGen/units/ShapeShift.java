package nationGen.units;

import nationGen.NationGen;
import nationGen.entities.Filter;
import nationGen.misc.Command;

public class ShapeShift extends Filter {

  public ShapeShift(NationGen nationGen) {
    super(nationGen);
  }

  boolean nofeedback = false;
  boolean keepFirstFormName = false;
  boolean nogcost = false;

  public ShapeShift getCopy() {
    ShapeShift ss = new ShapeShift(nationGen);
    ss.basechance = basechance;
    ss.name = name;
    ss.types.addAll(types);
    ss.tags.addAll(tags);
    ss.themes.addAll(themes);
    ss.nofeedback = nofeedback;
    ss.keepFirstFormName = keepFirstFormName;
    ss.nogcost = nogcost;
    ss.addCommands(this.getCommands());
    return ss;
  }

  @Override
  public void handleOwnCommand(Command command) {
    if (command.command.equals("#keepfirstformname")) {
      this.keepFirstFormName = true;
      // Overrides filter implementation
    } else if (command.command.equals("#command")) {
      if (command.args.size() != 1) {
        throw new IllegalArgumentException(
          "#command or #define must have a single arg. Surround the command with quotes if needed."
        );
      }
      this.addCommands(command.args.get(0).getCommand());
    } else super.handleOwnCommand(command);
  }
}
