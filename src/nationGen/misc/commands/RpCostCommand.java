package nationGen.misc.commands;

import nationGen.misc.Arg;
import nationGen.misc.Args;
import nationGen.misc.Command;
import nationGen.misc.CommandFactory;
import nationGen.misc.Operator;

public class RpCostCommand extends Command {
  public RpCostCommand(String cmd, Args args, String comment) {
    super(cmd, args, comment);
  }

  public RpCostCommand(Command commandToCopy) {
    super(commandToCopy);
  }

  /**
   * Combine the arg values of this command with that of another one of the same type.
   * "This" command command will be used as the base (for example, if neither commands
   * have arg values with specific operators such as ADD or SUBTRACT, the default behaviour
   * will be that "this" command's values will be SET, returning a new command with the same
   * values as "this" one).
   *
   * @param other - another same-type command
   * @return Commmand - a combined, new Command instance
   */
  @Override
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
  @Override
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

  @Override
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
   * Define what happens when a command needs to be combined with no other
   * commands (i.e. in a list that has no other command of the same type).
   * @return Commmand - a combined, new Command instance
   */
  public Command applyArgOperatorsToNothing() {
    return CommandFactory.copy(this);
  }

  /**
   * For Recruitment Points, values at 1000 or above are special values. They trigger Dominions' AutoCalc.
   * AutoCalc recpoints values work roughly in steps of 499. When going over or under these steps, they
   * wrap around. For example, if 8000 autocalcs a unit's RPs to 13, then 8499 autocalcs to 512, but
   * 8500 autocals to 2 (the minimum RP value). To apply a multiplier to these autocalc values, we can
   * take the thousands (i.e. for 8000, that is 8) and apply the multiplier to that, then add/subtract
   * that resulting value from the total autocalc. It's not perfect, but it should approximate well enough.
   * Can't do it more accurately without working out the exact Dominions' AutoCalc formula.
   *
   * @param multiplierArg - the arg with the MULTIPLY operator; e.g. #rpcost *0.8
   * @param baseArg - the arg to which the multiply is applied; e.g. #rpcost 10000
   * @return Commmand - a combined, new Command instance
   */
  @Override
  public Arg multiplyArg(Arg multiplierArg, Arg baseArg) {
    try {
      int value;
      Arg result;
      double multiplier;
      int recPoints;
      
      if (baseArg.get().startsWith("%")) {
        value = 0;
      }

      else {
        multiplier = multiplierArg.getDouble();
        recPoints = baseArg.getInt();

        if (recPoints >= 1000) {
          double inverse = multiplier - 1;
          int modifier = (int) Math.round(10 * inverse);
          value = recPoints + modifier;
        }

        /**
         * Values that are not AutoCalc will simply be multiplied as usual. For example, 30 RPs * 0.75 multiplier.
         */
        else {
          value = (int) Math.round(recPoints * multiplier);
        }
      }

      if (baseArg.getOperator().isPresent()) {
        result = Arg.asModifier(value);
      }

      else {
        result = new Arg(value);
      }

      return result;
    }
    
    catch (Exception e) {
      throw new IllegalArgumentException(
        "RpCostcommand MULTIPLY: " +
        multiplierArg +
        " * " +
        baseArg +
        " on '" +
        this.command +
        "' error",
        e
      );
    }
  }
}
