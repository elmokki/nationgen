package nationGen.items;

import java.util.ArrayList;
import java.util.List;
import nationGen.NationGen;
import nationGen.chances.ThemeInc;
import nationGen.entities.Drawable;
import nationGen.entities.Filter;
import nationGen.misc.Command;

public class Item extends Drawable {

  public String id = "-1";
  public boolean armor = false;
  public Filter filter = null;

  public ArrayList<ItemDependency> dependencies = new ArrayList<>();
  //public LinkedHashMap<String, String> dependencies = new LinkedHashMap<>();
  //public LinkedHashMap<String, String> typedependencies = new LinkedHashMap<>();

  public List<Command> commands = new ArrayList<>();
  public String slot = "";
  public String set = "";

  public Item(NationGen nationGen) {
    super(nationGen);
  }

  public CustomItem getCustomItemCopy() {
    CustomItem item = new CustomItem(nationGen);
    item.sprite = sprite;
    item.mask = mask;
    item.id = id;
    item.armor = armor;
    item.offsetx = offsetx;
    item.offsety = offsety;
    item.dependencies.addAll(dependencies);
    item.commands.addAll(commands);
    item.slot = slot;
    item.set = set;
    item.renderslot = renderslot;
    item.renderprio = renderprio;
    item.name = this.name;
    item.filter = this.filter;
    item.basechance = this.basechance;
    item.tags.addAll(tags);
    return item;
  }

  public Item getCopy() {
    return this.getCustomItemCopy();
  }

  @Override
  public void handleOwnCommand(Command command) {
    try {
      switch (command.command) {
        case "#gameid":
          this.id = command.args.get(0).get();
          break;
        case "#armor":
          this.armor = true;
          break;
        case "#addthemeinc":
          if (this.filter == null) {
            this.filter = new Filter(nationGen);
            filter.tags.addName("do_not_show_in_descriptions");
            if (this.name != null) filter.name = "Item " +
            this.name +
            " generation effects";
          }
          this.filter.themeincs.add(ThemeInc.from(command.args));
          break;
        case "#name":
          if (filter != null) filter.name = "Item " +
          command.args.get(0).get() +
          " generation effects";
          super.handleOwnCommand(command);
          break;
        case "#needs":
          this.dependencies.add(
              new ItemDependency(
                command.args.get(0).get(),
                command.args.get(1).get(),
                false,
                false
              )
            );
          break;
        case "#needstype":
          this.dependencies.add(
              new ItemDependency(
                command.args.get(0).get(),
                command.args.get(1).get(),
                true,
                false
              )
            );
          break;
        case "#forceslot":
          this.dependencies.add(
              new ItemDependency(
                command.args.get(0).get(),
                command.args.get(1).get(),
                false,
                true
              )
            );
          break;
        case "#forceslottype":
          this.dependencies.add(
              new ItemDependency(
                command.args.get(0).get(),
                command.args.get(1).get(),
                true,
                true
              )
            );
          break;
        case "#command":
        case "#define":
          if (command.args.size() != 1) {
            throw new IllegalArgumentException(
              "#command or #define must have a single arg. Surround the command with quotes if needed."
            );
          }
          this.commands.add(command.args.get(0).getCommand());
          break;
        default:
          super.handleOwnCommand(command);
          break;
      }
    } catch (IndexOutOfBoundsException e) {
      throw new IllegalArgumentException(
        "Command [" +
        command +
        "] has insufficient arguments (" +
        this.name +
        ")",
        e
      );
    }
  }
}
