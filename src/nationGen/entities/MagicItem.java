package nationGen.entities;

import java.util.ArrayList;
import java.util.List;
import nationGen.NationGen;
import nationGen.items.Item;
import nationGen.misc.ArgParser;
import nationGen.misc.Command;

public class MagicItem extends Filter {

  public MagicItem(NationGen nationGen) {
    super(nationGen);
  }

  public List<String> namePrefixes = new ArrayList<>();
  public List<String> nameSuffixes = new ArrayList<>();
  public Item baseitem;
  public String effect = "-1";
  public boolean always = false;

  @Override
  public void handleOwnCommand(Command command) {
    try {
      if (command.command.equals("#unitname")) {
        ArgParser parser = command.args.get(0).getArgs().parse();
        String nameType = parser.nextString();
        switch (nameType) {
          case "prefix":
            namePrefixes.add(parser.nextString());
            break;
          case "suffix":
            nameSuffixes.add(parser.nextString());
            break;
          default:
            throw new IllegalArgumentException(
              "Magic item '" +
              this.name +
              "' has unknown #unitname type '" +
              nameType +
              "'.  Command: " +
              command
            );
        }
      } else if (command.command.equals("#eff")) {
        effect = command.args.get(0).get();
      } else {
        super.handleOwnCommand(command);
      }
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
}
