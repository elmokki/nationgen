package nationGen.restrictions;

import java.awt.LayoutManager;

import javax.swing.JPanel;

import nationGen.nation.Nation;

public abstract class NationRestriction {
	
	public abstract void getGUI(JPanel panel);
	public abstract NationRestriction getRestriction();
	public abstract LayoutManager getLayout(); 
	public abstract boolean doesThisPass(Nation n);
	public abstract String toString();
	public abstract NationRestriction getInstanceOf();
}
