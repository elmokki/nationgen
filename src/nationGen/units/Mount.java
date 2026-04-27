package nationGen.units;

import java.util.Optional;

import nationGen.NationGen;
import nationGen.entities.Filter;
import nationGen.misc.Command;

public class Mount extends Filter {
  private String barding;

  public Mount(NationGen nationGen) {
    super(nationGen);
  }

  public Mount(Mount mount) {
    super(mount);
  }

  public Boolean hasBarding() {
    return super.hasCommand("#armor");
  }

  public Boolean isNamed() {
    return this.hasCommand("#name");
  }

  @Override
  public String getName() {
    Optional<Command> nameCommand = this.getCommand("#name");

    if (nameCommand.isPresent()) {
      return nameCommand.get().args.getString(0);
    }

    // This is the Filter name; this would be a placeholder
    return this.name;
  }

  public String getBardingId() {
    return this.barding;
  }
}
