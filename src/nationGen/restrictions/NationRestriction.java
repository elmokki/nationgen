package nationGen.restrictions;

import java.awt.LayoutManager;

import javax.swing.JPanel;

import nationGen.nation.Nation;

public interface NationRestriction 
{
    /*
     * Add your restriction types here.
     */
    public enum RestrictionType
    {
        MageWithAccess,
        MagicAccess,
        MagicDiversity,
        NationTheme,
        NoPrimaryRace,
        NoUnitOfRace,
        PrimaryRace,
        RecAnywhereSacreds,
        SacredRace,
        UnitCommand,
        UnitFilter,
        UnitRace,
    }
    
	public void getGUI(JPanel panel);
	
	/**
	 * Parses UI element to generate the relevant restriction. That's distinction between getInstanceOf.
	 * Should I merge the methods to make it less confusing?
	 * @return
	 */
	public NationRestriction getRestriction();
	public LayoutManager getLayout(); 
	public boolean doesThisPass(Nation n);
	public String toString();
	public NationRestriction getInstanceOf();
	public RestrictionType getType();
}
