package nationGen.chances;

import nationGen.NationGen;
import nationGen.entities.Filter;

/**
 * The data which a {@link ThemeInc} consumes to determine if a chance should be modified or not.
 */
public class ThemeIncData {

  public NationGen nationGen;
  public final Filter f;

  public ThemeIncData(NationGen nationGen, Filter f) {
    this.nationGen = nationGen;
    this.f = f;
  }
}
