package nationGen.magic;

import java.util.ArrayList;
import java.util.List;
import nationGen.NationGen;
import nationGen.entities.Filter;

public class Spell extends Filter {

  public List<Integer> nationids = new ArrayList<Integer>();

  public Spell(NationGen nationGen) {
    super(nationGen);
  }
}
