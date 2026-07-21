package nationGen.misc;

import com.elmokki.Generic;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Command {

  public final String command;
  public final Args args;
  public final String comment;

  public Command(String cmd, Args args, String comment) {
    if (Generic.containsSpace(cmd)) {
      throw new IllegalArgumentException("Command name can't contain a space.");
    }

    this.command = cmd;
    this.args = args;
    this.comment = comment;
  }

  public Command(Command commandToCopy) {
    this.command = commandToCopy.command;
    this.args = new Args(commandToCopy.args);
    this.comment = commandToCopy.comment;
  }

  public Optional<CommandType> getType() {
    CommandType type = CommandType.fromRaw(this.command);

    if (type == null) {
      return Optional.empty();
    }

    return Optional.of(type);
  }

  public boolean isOfType(CommandType type) {
    return this.getType().equals(Optional.of(type));
  }

  public boolean sameTypeAs(Command other) {
    return this.getType().equals(other.getType());
  }

  public static String parseValueToAddString(int value) {
    String operator = (value > 0) ? "+" : "";
    return operator + value;
  }

  /**
   * Combine the arg values of this command with that of another one of the same type.
   * "This" command command will be used as the base (for example, if neither commands
   * have arg values with specific operators such as ADD or SUBTRACT, the default behaviour
   * will be that "this" command's values will be SET, returning a new command with the same
   * values as "this" one).
   * @param other - another same-type command
   * @return Commmand - a combined, new Command instance
   */
  public Command combine(Command other) {
    Command combinedCommand = CommandFactory.copy(this);

    if (!this.sameTypeAs(other)) {
      throw new IllegalArgumentException(
        "Cannot combine commands of different types ('" +
        this.command +
        "' and '" +
        other.command +
        "'"
      );
    }

    for (int i = 0; i < this.args.size(); i++) {
      Arg arg = combinedCommand.args.get(i);
      Arg otherArg = other.args.get(i);
      Arg combinedValue = this.combineArgs(arg, otherArg);
      combinedCommand.args.set(i, combinedValue);
    }

    return combinedCommand;
  }

  /**
   * Combine this command with raw numerical values. Note that
   * unless this command's arguments have operators such as ADD
   * or SUBTRACT, this will essentially set the commands args to
   * the provided int values.
   * @param rawValues - raw int values, in the order of the command's args
   * @return - the new, combined command
   */
  public Command combine(int... rawValues) {
    Command combinedCommand = CommandFactory.copy(this);

    if (this.args.isEmpty()) {
      return CommandFactory.copy(this);
    }

    for (int i = 0; i < rawValues.length; i++) {
      Arg arg = this.args.get(i);
      Arg otherArg = new Arg(rawValues[i]);
      Arg combinedValue = this.combineArgs(arg, otherArg);
      combinedCommand.args.set(i, combinedValue);
    }

    return combinedCommand;
  }

  public Arg combineArgs(Arg modifierArg, Arg baseArg) {
    // By default, if no specific operator is given, set the value of this command
    Operator operator = modifierArg.getOperator().orElse(Operator.SET);
    Arg combinedValue = new Arg(modifierArg.get());

    if (operator == Operator.ADD || operator == Operator.SUBTRACT) {
      combinedValue = this.addArg(modifierArg, baseArg);
    }
    
    else if (operator == Operator.MULTIPLY) {
      combinedValue = this.multiplyArg(modifierArg, baseArg);
    }

    return combinedValue;
  }

  /**
   * When an #rpcost command needs to be combined alone, we should leave the operator values intact
   * instead of trying to resolve them. The Unit code to autocalc the unit's RP will kick in, and
   * then it can take into account the modifier left dangling here.
   * @return Commmand - a combined, new Command instance
   */
  public Command applyArgOperatorsToNothing() {
    // return CommandFactory.copy(this);

    Args args = new Args();

    for (Arg arg : this.args) {
      args.add(arg.applyOperatorToNothing());
    }

    return CommandFactory.create(this.command, args, this.comment);
  }

  public Arg addArg(Arg ownArg, Arg otherArg) {
    try {
      int value;

      if (otherArg.get().startsWith("%")) {
        value = ownArg.getInt();
      }

      else {
        value = ownArg.getInt() + (otherArg.getInt());
      }

      return new Arg(value);
    }
    
    catch (NumberFormatException error) {
      throw new IllegalArgumentException(
        "Error combining: " +
        ownArg +
        " + " +
        otherArg +
        " on '" +
        this.command +
        "' error",
        error
      );
    }
  }

  public Arg multiplyArg(Arg ownArg, Arg otherArg) {
    try {
      int value;
      
      if (otherArg.get().startsWith("%")) {
        value = 0;
      }

      else {
        value = ((int) Math.round((ownArg.getDouble() * otherArg.getInt())));
      }

      // If the multiplier is not explicitly *0, don't let the new value be less than 1
      // Example is #rpcost *0.8 from the sickly filter that can set commanders to 0 RPs
      if (ownArg.getDouble() > 0 && ownArg.getDouble() < 1 && value == 0) {
        return new Arg(1);
      }

      else {
        return new Arg(value);
      }
    }
    
    catch (Exception e) {
      throw new IllegalArgumentException(
        "Error combining: " +
        ownArg +
        " * " +
        otherArg +
        " on '" +
        this.command +
        "' error",
        e
      );
    }
  }

  /**
   * Writes a String for how Dominions expects a mod command line to look like, with strings in quotes and comments
   * following dashes.
   * @return A string suitable to be written to a Dominions mod file.
   */
  public String toModLine() {
    return (
      this.command +
      (this.args.isEmpty() ? "" : " ") +
      this.args.stream()
        .map(a ->
          a.isNumeric()
            ? a.get()
            : Generic.quote(a.get().replaceAll("\"", "''"), '"')
        )
        .collect(Collectors.joining(" ")) +
      (this.comment != null ? (" --- " + this.comment) : "")
    );
  }

  public Boolean isShapeshiftCommand() {
    return this.command.equals("#shapechange") ||
      this.command.equals("#firstshape") ||
      this.command.equals("#secondshape") ||
      this.command.equals("#secondtmpshape");
  }

  public Boolean hasArgs() {
    return !this.args.isEmpty() && !this.args.getString(0).isBlank();
  }

  public Boolean hasCombinatoryArgs() {
    return this.hasArgs() &&
      this.args.stream()
      .map(a -> a.getOperator().orElse(Operator.SET))
      .filter(op ->
        op.equals(Operator.ADD) ||
        op.equals(Operator.SUBTRACT) ||
        op.equals(Operator.MULTIPLY) ||
        op.equals(Operator.DIVIDE)
      )
      .findFirst()
      .isPresent();
  }

  public Boolean isBoolean() {
    return this.args.isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Command command1 = (Command) o;
    return command.equals(command1.command) && args.equals(command1.args);
  }

  // Tries to match only the base command string, regardless of actual value
  // i.e. #coldres contains the #coldres 5 command
  public Boolean contains(Command other) {
    return this.command.equals(other.command);
  }

  @Override
  public int hashCode() {
    return Objects.hash(command, args);
  }

  public String toString() {
    return this.command + (this.args.isEmpty() ? "" : " ") + this.args;
  }

  public static Optional<Command> lastInStream(Stream<Command> commandStream) {
    return commandStream.reduce((first, second) -> second);
  }
}
