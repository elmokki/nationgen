package nationGen.restrictions;



import java.util.List;
import java.util.ArrayList;



import nationGen.NationGen;
import nationGen.entities.Filter;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class UnitFilterRestriction extends TwoListRestrictionWithComboBox<String, String> {
	public List<String> possibleRaceNames = new ArrayList<String>();
	
	private NationGen ng;
	private String[] ownoptions = {"All", "Troops", "Commanders", "Sacred troops"};
	public UnitFilterRestriction(NationGen ng)
	{
		super(ng, "<html>Nation needs to have at least one unit with a filter on the right box.</html>", "Unit filter");
		this.ng = ng;
		
		this.comboboxlabel = "Units to match:";
		for(String str : ng.filters.keySet())
			for(Filter f : ng.filters.get(str))
				rmodel.addElement(str + ": " + f);
		
		this.comboboxoptions = ownoptions;
		
		
	}
	


	@Override
	public NationRestriction getRestriction() {
		UnitFilterRestriction res = new UnitFilterRestriction(ng);
		for(int i =0; i < chosen.getModel().getSize(); i++)
			res.possibleRaceNames.add(chosen.getModel().getElementAt(i));
		
		res.comboselection = this.comboselection;
		return res;
	}
	
	@Override
	public boolean doesThisPass(Nation n) {
		if(possibleRaceNames.size() == 0)
		{
			System.out.println("Units of filter nation restriction has no races set!");
			return true;
		}
		
		if(comboselection == null)
			comboselection = "All";
		
		boolean pass = false;
		if(comboselection.equals("Troops") || comboselection.equals("All"))
			pass = checkUnits(n.generateTroopList());
		
		if(!pass && (comboselection.equals("Commanders") || comboselection.equals("All")))
			pass = checkUnits(n.generateComList());
			
		if(!pass && comboselection.equals("Sacred troops"))
			pass = checkUnits(n.generateUnitList("sacred"));
		
		

		return pass;
	}
	
	private boolean checkUnits(List<Unit> list)
	{
		for(Unit u : list)
		{
			for(String str : possibleRaceNames)
			{
				String[] args = str.split(": ");
				for(Filter f : u.appliedFilters)
					if(f.toString().equals(args[1]))
						return true;
				
				
			}
		}
		return false;
	}
	
	@Override
	public NationRestriction getInstanceOf() {
		return new UnitFilterRestriction(ng);
	}
	
}
