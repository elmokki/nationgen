package nationGen.misc;

import com.elmokki.Generic;
import java.util.Objects;
import java.util.Optional;
import nationGen.magic.MagicPath;

/**
 * Represents an argument from a command line, which can easily be converted to several different types.
 */
public class Arg {

  private final String string;

  private Integer integer;
  private Double dbl;
  private Operator operator;
  private MagicPath path;
  private Args args;
  private Command command;
  private Args parenthesis;

  public Arg(String string) {
    if (string == null) {
      throw new IllegalArgumentException("Arg string can't be null");
    }
    this.string = string;
  }

  public Arg(int integer) {
    this.string = String.valueOf(integer);
    this.integer = integer;
    this.dbl = this.integer.doubleValue();
  }

  public Arg(double dbl) {
    this.string = String.valueOf(dbl);
    this.dbl = dbl;
    this.integer = (int) dbl;
  }

  public Arg(MagicPath path) {
    if (path == null) {
      throw new IllegalArgumentException("Arg path can't be null");
    }
    this.string = path.name;
    this.path = path;
    this.integer = path.number;
  }

  public Arg(Command command) {
    if (command == null) {
      throw new NullPointerException("Arg command can't be null");
    }
    this.string = command.toString();
    this.command = command.copy(); // defensive copy to prevent this object's copy being modified
  }

  public Arg(Args parenthesis) {
    this.parenthesis = parenthesis.copy(); // defensive copy to prevent this object's copy being modified
    this.string = this.parenthesis.toString();
  }

  public String get() {
    return this.string;
  }

  public boolean isNumeric() {
    try {
      getDouble();
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public int getInt() {
    if (this.integer == null) {
      if (this.string.contains(".")) {
        this.integer = (int) getDouble();
      } else if (
        getOperator().filter(o -> o != Operator.SUBTRACT).isPresent()
      ) {
        this.integer = Integer.parseInt(this.string.substring(1));
      } else {
        this.integer = Integer.parseInt(this.string);
      }
    }
    return this.integer;
  }

  public double getDouble() {
    if (this.dbl == null) {
      if (getOperator().filter(o -> o != Operator.SUBTRACT).isPresent()) {
        this.dbl = Double.parseDouble(this.string.substring(1));
      } else {
        this.dbl = Double.parseDouble(this.string);
      }
    }
    return this.dbl;
  }

  public Optional<Operator> getOperator() {
    if (this.operator == null && !this.string.isEmpty()) {
      this.operator = Operator.fromChar(this.string.charAt(0));
    }
    return Optional.ofNullable(this.operator);
  }

  public MagicPath getMagicPath() {
    if (this.path == null) {
      char c = this.string.charAt(0);
      if (c >= '0' && c <= '9') {
        this.path = MagicPath.fromInt(getInt());
      } else {
        this.path = MagicPath.fromName(this.string);
      }
    }
    return this.path;
  }

  public Command getCommand() {
    if (this.command == null) {
      this.command = Command.parse(this.string);
    }
    return this.command.copy(); // defensive copy to prevent this object's copy being modified
  }

  public Args getArgs() {
    if (this.args == null) {
      this.args = Args.lex(this.string);
    }
    return this.args.copy(); // defensive copy to prevent this object's copy being modified
  }

  /**
   * A parenthesis is an arg that was surrounded in parentheses in the command line, such as in:
   * {@code hello (one two)}.  These are currently only used in chanceincs and themeincs.
   * @return If this {@link Arg} represents a parenthesis.
   */
  public boolean isParenthesis() {
    return this.parenthesis != null;
  }

  /**
   * A parenthesis is an arg that was surrounded in parentheses in the command line, such as in:
   * {@code hello (one two)}.  These are currently only used in chanceincs and themeincs.
   * @return The parenthesis args.
   */
  public Args getParenthesis() {
    return this.parenthesis == null ? null : this.parenthesis.copy(); // defensive copy to prevent this object's copy being modified
  }

  public double applyModTo(double value) {
    if (getOperator().isPresent()) {
      switch (getOperator().get()) {
        case ADD:
        case SUBTRACT:
          return value + getDouble();
        case MULTIPLY:
          return value * getDouble();
        case DIVIDE:
          return value / getDouble();
        default:
          throw new UnsupportedOperationException(
            "Unknown how to apply mod operator [" + getOperator().get() + "]."
          );
      }
    } else {
      return value + getDouble();
    }
  }

  public Arg applyModToNothing() {
    Optional<Operator> operator = getOperator();
    if (operator.isPresent()) {
      if (operator.get() == Operator.ADD) {
        return new Arg(this.string.substring(1));
      } else if (
        operator.get() == Operator.MULTIPLY || operator.get() == Operator.DIVIDE
      ) {
        return new Arg(0);
      }
    }
    return new Arg(this.string);
  }

  /**
   * @return A representation of this arg as it might appear on a command line.
   */
  public String unparse() {
    if (this.isParenthesis()) {
      return "(" + this.parenthesis + ")";
    } else if (
      Generic.containsSpace(this.string) ||
      this.string.startsWith("\"") ||
      this.string.startsWith("'")
    ) {
      char quote = this.string.startsWith("\"") ? '\'' : '"';
      return Generic.quote(this.string, quote);
    } else {
      return this.string;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Arg arg = (Arg) o;
    return string.equals(arg.string);
  }

  @Override
  public int hashCode() {
    return Objects.hash(string);
  }

  @Override
  public String toString() {
    return this.string;
  }
}
