package nationGen.chances;


import nationGen.entities.Race;
import nationGen.magic.MagicPath;
import nationGen.magic.MagicPathInts;
import nationGen.nation.Nation;
import nationGen.units.Unit;

import java.util.Set;


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
