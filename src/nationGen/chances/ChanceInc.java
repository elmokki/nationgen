package nationGen.chances;

import nationGen.misc.Arg;
import nationGen.misc.ArgParser;
import nationGen.misc.Args;

/**
 * A "chanceinc", which modifies the chance of a particular thing being randomly selected.
 */
public class ChanceInc extends ConditionalModifier<ChanceIncData> {

  public ChanceInc(Condition<ChanceIncData> condition, Arg modificationAmount) {
    super(condition, modificationAmount);
  }

  public static ChanceInc from(Args args) {
    ArgParser parser = args.parse();
    Arg mod = parser.last("chance modification");
    ChanceInc chanceInc = new ChanceInc(
      Condition.from(parser.remaining(), ChanceIncConditionType::create),
      mod
    );
    chanceInc.source = args.toString();
    return chanceInc;
  }

  public static ChanceInc parse(String line) {
    ChanceInc chanceInc = from(Args.lex(line));
    chanceInc.source = line;
    return chanceInc;
  }
}
