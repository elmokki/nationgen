package nationGen.chances;

import nationGen.misc.Arg;
import nationGen.misc.ArgParser;
import nationGen.misc.Args;

/**
 * A "themeinc", which modifies the chance of certain other things being selected.
 */
public class ThemeInc extends ConditionalModifier<ThemeIncData> {

  public ThemeInc(Condition<ThemeIncData> condition, Arg modificationAmount) {
    super(condition, modificationAmount);
  }

  public static ThemeInc from(Args args) {
    ArgParser parser = args.parse();
    Arg mod = parser.last("chance modification");
    ThemeInc themeInc = new ThemeInc(
      Condition.from(parser.remaining(), ThemeIncConditionType::create),
      mod
    );
    themeInc.source = args.toString();
    return themeInc;
  }

  public static ThemeInc parse(String line) {
    ThemeInc themeInc = from(Args.lex(line));
    themeInc.source = line;
    return themeInc;
  }
}
