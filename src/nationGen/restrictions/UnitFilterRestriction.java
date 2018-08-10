package nationGen.restrictions;

import java.util.List;
import java.util.ArrayList;

import nationGen.NationGenAssets;
import nationGen.entities.Filter;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class UnitFilterRestriction extends TwoListRestrictionWithComboBox<String, String> {
	public List<String> possibleFilterNames = new ArrayList<String>();
	
	private NationGenAssets assets;
	
	private String[] ownoptions = {"All", "Troops", "Commanders", "Sacred troops"};
	public UnitFilterRestriction(NationGenAssets assets)
	{
		super("<html>Nation needs to have at least one unit with a filter on the right box.</html>", "Unit filter");
		this.assets = assets;
		
		this.comboboxlabel = "Units to match:";
		for(String str : assets.filters.keySet())
			for(Filter f : assets.filters.get(str))
				rmodel.addElement(str + ": " + f);
		
		this.comboboxoptions = ownoptions;
		
		
	}
	


	@Override
	public NationRestriction getRestriction() {
		UnitFilterRestriction res = new UnitFilterRestriction(assets);
		for(int i =0; i < chosen.getModel().getSize(); i++)
			res.possibleFilterNames.add(chosen.getModel().getElementAt(i));
		
		res.comboselection = this.comboselection;
		return res;
	}
	
	@Override
	public boolean doesThisPass(Nation n) {
		if(possibleFilterNames.size() == 0)
		{
			System.out.println("Units of filter restriction has no filters set!");
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
			for(String str : possibleFilterNames)
			{
				int index = str.indexOf(": ");
				String comp = str.substring(index + 2);
				for(Filter f : u.appliedFilters)
				{
					if(f.toString().equals(comp))
						return true;
				}
				
			}
		}
		return false;
	}

    @Override
    public RestrictionType getType()
    {
        return RestrictionType.UnitFilter;
    }
	
}
