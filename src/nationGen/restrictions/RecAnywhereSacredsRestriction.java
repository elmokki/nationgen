package nationGen.restrictions;

import java.awt.BorderLayout;
import java.awt.LayoutManager;

import javax.swing.JPanel;

import nationGen.NationGen;
import nationGen.nation.Nation;
import nationGen.units.Unit;

/**
 * 
 * @author Flashfire
 * Class intends to filter out nations that have recruit anywhere sacreds.
 */
public class RecAnywhereSacredsRestriction implements NationRestriction {

	private NationGen ng; //everyone else needs one of these boys. May as well have it I suppose.
	public RecAnywhereSacredsRestriction(NationGen nationGen)
	{
		this.ng = nationGen;
	}
	
	@Override
	// No need for a fancy (or even unfancy!) UI for just a simple flag!
	public void getGUI(JPanel panel) {
	}

	@Override
	public NationRestriction getRestriction() {
		return new RecAnywhereSacredsRestriction(ng);
	}

	@Override
	public LayoutManager getLayout() {
		return new BorderLayout();
	}

	@Override
	public boolean doesThisPass(Nation n) {
		for(Unit u : n.generateUnitList("sacred"))
		{
			if(!u.caponly)
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "Recruit Anywhere Sacred";
	}

	@Override
	public NationRestriction getInstanceOf() {
		return new RecAnywhereSacredsRestriction(ng);
	}

    @Override
    public RestrictionType getType()
    {
        return RestrictionType.RecAnywhereSacreds;
    }

}
