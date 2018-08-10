package nationGen.restrictions;

import java.util.List;
import java.util.ArrayList;

import nationGen.NationGenAssets;
import nationGen.entities.Theme;
import nationGen.nation.Nation;

public class NationThemeRestriction extends TwoListRestriction<String>  {
	public List<String> possibleRaceNames = new ArrayList<String>();
	
	
	private NationGenAssets assets;
	public NationThemeRestriction(NationGenAssets assets)
	{
		super("Nation or primary race needs to have a theme named like one of the themes in the right box", "Nation or primary race theme");
		this.assets = assets;
		
		for(String str : assets.themes.keySet())
			for(Theme t : assets.themes.get(str))
				rmodel.addElement(str + " - " + t.toString());
		
		
		this.extraTextField = true;
	    this.textFieldLabel = "Search:";
	}

	@Override
	public NationRestriction getRestriction() {
		NationThemeRestriction res = new NationThemeRestriction(assets);
		for(int i =0; i < chosen.getModel().getSize(); i++)
			res.possibleRaceNames.add(chosen.getModel().getElementAt(i));
		return res;
	}

	@Override
	public boolean doesThisPass(Nation n) {
		if(possibleRaceNames.size() == 0)
		{
			System.out.println("Nation or primary race theme nation restriction has no races set!");
			return true;
		}
		
		boolean ok = false;
		for(Theme t : n.nationthemes)
		{
			for(String str : possibleRaceNames)
			{
				String name = str.split(" - ")[1];
				if(t.name.equals(name))
				{
					ok = true;
					break;
				}
				
			}
		}
		
		for(Theme t : n.races.get(0).themefilters)
		{
			for(String str : possibleRaceNames)
			{
				String name = str.split(" - ")[1];
				if(t.name.equals(name))
				{
					ok = true;
					break;
				}
				
			}
		}
		
		return ok;
	}

    @Override
    public RestrictionType getType()
    {
        return RestrictionType.NationTheme;
    }
    
    @Override
    protected void textFieldUpdate()
    {
        rmodel.clear();
        for(String str : assets.themes.keySet())
        {
            for(Theme t : assets.themes.get(str))
            {
                String add = str + " - " + t.toString();
                if (add.contains(textfield.getText()) || textfield.getText().length() == 0)
                {
                    rmodel.addElement(add);
                }
            }
        }
    }
}
