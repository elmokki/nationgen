package nationGen.misc;

import java.util.*;
import nationGen.magic.MagicPath;

/**
 * Parses Args.  Has many convenience methods relating to Args.
 */
public class ArgParser extends Parser<Arg> {

  public ArgParser(Collection<Arg> args) {
    super(args);
  }

  public String nextString() {
    return next("text string").get();
  }

  public int nextInt() {
    return next("integer").getInt();
  }

  public double nextDecimal() {
    return next("decimal number").getDouble();
  }

  public MagicPath nextMagicPath() {
    return next("magic path").getMagicPath();
  }

  public Command nextCommand() {
    return next("command/tag").getCommand();
  }

  public boolean nextOptionalFlag(String name) {
    return nextIf(a -> a.get().equalsIgnoreCase(name)).isPresent();
  }

  public Optional<Integer> nextOptionalInt() {
    return nextIf(Arg::isNumeric).map(Arg::getInt);
  }

  @Override
  public Args remaining() {
    return new Args(super.remaining());
  }
}
