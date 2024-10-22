package nationGen.chances;

import java.util.Set;
import nationGen.entities.Race;
import nationGen.magic.MagicPath;
import nationGen.magic.MagicPathInts;
import nationGen.nation.Nation;
import nationGen.units.Unit;

/**
 * The data that {@link ChanceInc} conditions consume to return a boolean.
 */
public class ChanceIncData {

  public String moduleId;
  public Nation n;
  public Unit u;
  public Race race;
  public double avgres;
  public double avggold;
  public int diversity;
  public int[] at;
  public MagicPathInts nonrandom_paths;
  public Set<MagicPath> atHighest;
}
