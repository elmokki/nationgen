package nationGen.misc;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A {@link List} of {@link Arg}s with some convenience methods.
 */
public class Args extends ArrayList<Arg> {

  public Args() {
    super();
  }

  public Args(Collection<Arg> args) {
    super(args);
  }

  public static Args of(Arg[] args) {
    return new Args(List.of(args));
  }

  public static Args of(Arg one) {
    Args args = new Args();
    args.add(one);
    return args;
  }

  public static Args of(Arg one, Arg two) {
    Args args = new Args();
    args.add(one);
    args.add(two);
    return args;
  }

  /**
   * @return An {@link ArgParser} which allows for easy sequential and conditional consumption of these args.
   */
  public ArgParser parse() {
    return new ArgParser(this);
  }

  /**
   * @return A copy of these args, which may be needed since {@link Args} is a mutable {@link List}.
   */
  public Args copy() {
    return new Args(this);
  }

  public String getString(int index) {
    return get(index).get();
  }

  public int getInt(int index) {
    return get(index).getInt();
  }

  public double getDouble(int index) {
    return get(index).getDouble();
  }

  public Command getCommand(int index) {
    return get(index).getCommand();
  }

  public Args getArgs(int index) {
    return get(index).getArgs();
  }

  @Override
  public String toString() {
    return stream().map(Arg::unparse).collect(Collectors.joining(" "));
  }

  /**
   * Lexes a string of characters into a string of generic args, which can then be interpreted by a
   * {@link #parse parser} if desired. Parentheses outside of quotes must be evenly matched. Quotation marks must also
   * be evenly matched unless escaped with a backslash or within an argument which doesn't start with a quotation
   * mark. Both double and single quotes are supported and can be nested, but must be escaped if nested more than
   * once. The number of backslashes required to escape deeply nested quotes grows exponentially.  But it is doable.
   * <p>
   * Example: {@code "quote one 'two \"three\"'"}
   * @param line The source line to lex.
   * @return The resulting Args
   */
  public static Args lex(String line) {
    char quote = 0;
    StringBuilder arg = new StringBuilder();
    boolean escape = false;
    boolean inArg = false;

    final Deque<Args> argStack = new LinkedList<>();
    argStack.push(new Args());

    for (int i = 0; i < line.length(); i++) {
      final char c = line.charAt(i);

      if (inArg) {
        if (quote != 0) {
          if (c == quote && !escape) {
            argStack.element().add(new Arg(arg.toString()));
            quote = 0;
            arg = new StringBuilder();
            inArg = false;
          } else if (!escape && c == '\\') {
            escape = true;
          } else {
            if (escape && c != quote && c != '\\') {
              arg.append('\\');
            }
            escape = false;
            arg.append(c);
          }
        } else {
          if (c == '(' || c == ')' || Character.isWhitespace(c)) {
            argStack.element().add(new Arg(arg.toString()));
            arg = new StringBuilder();
            inArg = false;
            if (c == '(') {
              argStack.push(new Args());
            } else if (c == ')') {
              if (argStack.size() == 1) {
                throw new IllegalArgumentException(
                  String.format(
                    "Misplaced closing parentheses [)] in string [%s].",
                    line
                  )
                );
              }
              final Arg parenthesis = new Arg(argStack.pop());
              argStack.element().add(parenthesis);
            }
          } else {
            arg.append(c);
          }
        }
      } else if (!Character.isWhitespace(c)) {
        if (c == '(') {
          argStack.push(new Args());
        } else if (c == ')') {
          if (argStack.size() == 1) {
            throw new IllegalArgumentException(
              String.format(
                "Misplaced closing parentheses [)] in string [%s].",
                line
              )
            );
          }
          final Arg parenthesis = new Arg(argStack.pop());
          argStack.element().add(parenthesis);
        } else if (c == '\'' || c == '"') {
          quote = c;
          inArg = true;
        } else {
          arg.append(c);
          inArg = true;
        }
      } // else ignore the space
    }
    if (quote != 0) {
      throw new IllegalArgumentException(
        String.format(
          "Ambiguous quote [%s] in string [%s]. Unclear if argument [%s] is meant to have matching " +
          "terminal quote or if initial quote is meant to be part of the string literal.",
          quote,
          line,
          arg
        )
      );
    }
    if (inArg && arg.length() > 0) {
      argStack.element().add(new Arg(arg.toString()));
    }
    if (argStack.size() > 1) {
      throw new IllegalArgumentException(
        String.format(
          "Not enough closing parentheses [)] in string [%s].",
          line
        )
      );
    }

    return argStack.pop();
  }

  /**
   * Prepares a line for parsing.  This is just a shortcut for calling {@code lex(line).parse();}.
   * @param line The line to parse
   * @return The resulting {@link ArgParser}
   */
  public static ArgParser parse(String line) {
    return lex(line).parse();
  }
}
