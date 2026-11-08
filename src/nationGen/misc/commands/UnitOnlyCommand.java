package nationGen.misc.commands;

import nationGen.misc.Args;
import nationGen.misc.Command;

public class UnitOnlyCommand extends Command {
  public UnitOnlyCommand(String cmd, Args args, String comment) {
    super(cmd, args, comment);
  }

  public UnitOnlyCommand(Command commandToCopy) {
    super(commandToCopy);
  }
}
