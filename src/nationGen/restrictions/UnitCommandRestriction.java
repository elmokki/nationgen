package nationGen.restrictions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class UnitCommandRestriction extends TextBoxListRestrictionWithCheckboxes {

  private List<String> commandRestrictions = new ArrayList<>();

  public UnitCommandRestriction() {
    super(
      "<html>Nation needs to have at least one unit with a command on the right box. If you leave arguments empty, arguments on unit do not matter. " +
      "If you specify arguments, they need to match exactly.</html>",
      "Unit command"
    );
    this.hascombobox = true;
    this.comboboxoptions = new String[] {
      "All",
      "Troops",
      "Commanders",
      "Sacred troops",
      "Cap-only",
      "Normal-rec",
    };
    this.comboboxlabel = "Units to match:";
    this.leftCheckboxLabel = "Match all commands";
    this.rightCheckboxLabel = "Check mount commands";
    this.textFieldLabel = "Command to add:";
    this.textfieldDefaultText = "#flying";
  }

  @Override
  public NationRestriction getRestriction() {
    UnitCommandRestriction res = new UnitCommandRestriction();
    for (
      int i = 0;
      i < chosen.getModel().getSize();
      i++
    ) res.commandRestrictions.add(chosen.getModel().getElementAt(i));

    res.comboselection = this.comboselection;
    res.isLeftCheckboxMarked = this.isLeftCheckboxMarked;
    res.isRightCheckboxMarked = this.isRightCheckboxMarked;
    return res;
  }

  @Override
  public boolean doesThisPass(Nation n) {
    if (commandRestrictions.size() == 0) {
      System.out.println("Unit command nation restriction has no races set!");
      return true;
    }

    if (comboselection == null) comboselection = "All";

    if (comboselection.equals("Cap-only")) {
      return n.selectUnits().filter(Unit::isCapOnly).anyMatch(this::checkUnit);
    }

    if (comboselection.equals("Normal-rec")) {
      return n.selectUnits().filter(Predicate.not(Unit::isCapOnly)).anyMatch(this::checkUnit);
    }

    return (
      ((comboselection.equals("Troops") || comboselection.equals("All")) &&
        n.selectTroops().anyMatch(this::checkUnit)) ||
      ((comboselection.equals("Commanders") || comboselection.equals("All")) &&
        n.selectCommanders().anyMatch(this::checkUnit)) ||
      (comboselection.equals("Sacred troops") &&
        n.selectTroops("sacred").anyMatch(this::checkUnit))
    );
  }

  private boolean checkUnit(Unit u) {
    List<Command> unitCommands = u.gatherCommands();
    List<Command> requiredCommands = commandRestrictions.stream().map(str -> Command.parse(str)).toList();

    if (this.shouldCheckMountCommands()) {
      unitCommands.addAll(u.getMountCommands());
    }

    if (this.shouldMatchAllCommands()) {
      return requiredCommands.stream().allMatch(reqCommand -> {
        return unitCommands.stream().anyMatch(unitCommand -> {
          return unitCommand.command.equals(reqCommand.command) &&
            (reqCommand.args.isEmpty() || unitCommand.equals(reqCommand));
        });
      });
    }

    else {
      return requiredCommands.stream().anyMatch(reqCommand -> {
        return unitCommands.stream().anyMatch(unitCommand -> {
          return unitCommand.command.equals(reqCommand.command) &&
            (reqCommand.args.isEmpty() || unitCommand.equals(reqCommand));
        });
      });
    }
  }

  private Boolean shouldMatchAllCommands() {
    return this.isLeftCheckboxMarked;
  }

  private Boolean shouldCheckMountCommands() {
    return this.isRightCheckboxMarked;
  }

  @Override
  public RestrictionType getType() {
    return RestrictionType.UnitCommand;
  }
}
