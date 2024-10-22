package nationGen.restrictions;

import java.awt.*;
import javax.swing.*;
import nationGen.nation.Nation;

/**
 *
 * @author Flashfire
 * Class intends to filter out nations that have recruit anywhere sacreds.
 */
public class RecAnywhereSacredsRestriction implements NationRestriction {

  public RecAnywhereSacredsRestriction() {}

  @Override
  // No need for a fancy (or even unfancy!) UI for just a simple flag!
  public void getGUI(JPanel panel) {}

  @Override
  public NationRestriction getRestriction() {
    return new RecAnywhereSacredsRestriction();
  }

  @Override
  public LayoutManager getLayout() {
    return new BorderLayout();
  }

  @Override
  public boolean doesThisPass(Nation n) {
    return n.selectTroops("sacred").anyMatch(u -> !u.caponly);
  }

  @Override
  public String toString() {
    return "Recruit Anywhere Sacred";
  }

  @Override
  public RestrictionType getType() {
    return RestrictionType.RecAnywhereSacreds;
  }
}
