package nationGen.naming;

import java.util.ArrayList;
import java.util.List;

import com.elmokki.Generic;

import nationGen.entities.Filter;
import nationGen.items.Item;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class NationDescriber {
	private Nation n;
	
	public NationDescriber(Nation n)
	{
		this.n = n;
		describeTroops();
		describeCommanders();
	}
	
	
	private void describeUnits(DescriptionReplacer dr, List<Unit> units, Filter descf)
	{			
		dr.calibrate(units);
		for(Unit u : units)
		{
			dr.calibrate(u);
			
			String desc = "";
			if(Generic.getTagValue(u.race.tags, "description") != null)
				desc = Generic.getTagValue(u.race.tags, "description") + " ";
			
			if(descf != null)
			{
				desc = desc + descf.name + " ";
			}
			
			
			for(Item i : u.slotmap.values())
			{
			
				if(i != null && Generic.getTagValue(i.tags, "description") != null)
				{
					desc = desc + Generic.getTagValue(i.tags, "description") + " ";
				}
			}
			
			for(Filter f : u.appliedFilters)
			{
				if(f != null && Generic.getTagValue(f.tags, "description") != null)
				{
					desc = desc + Generic.getTagValue(f.tags, "description") + " ";
				}
			}
			
			desc = dr.replace(desc.trim());
			u.commands.add(new Command("#descr", "\"" + desc + "\""));
		}
	}
	
	private void describeTroops()
	{
		String[] roles = {"ranged", "infantry", "mounted", "chariot"};
		
		ChanceIncHandler chandler = new ChanceIncHandler(n);
		List<Filter> descs = ChanceIncHandler.retrieveFilters("troopdescriptions", "troopdescs", n.nationGen.descriptions, null, n.races.get(0));
		
		DescriptionReplacer dr = new DescriptionReplacer(n);
		for(String role : roles)
		{
			
			List<Unit> units = n.generateUnitList(role);
			if(units == null || units.size() == 0)
				continue;
				
				
			if(!role.equals("mounted"))
				dr.descs.put("%role%", role);
			else
				dr.descs.put("%role%", "cavalry");
	
			if(role.equals("chariot"))
				dr.descs.put("%role%", role + "s");
			if(role.equals("ranged"))
				dr.descs.put("%role%", role + " units");
			


			
			List<Filter> possibles = new ArrayList<Filter>();
			for(Filter f : descs)
				if(ChanceIncHandler.suitableFor(units.get(0), f, n))
					possibles.add(f);
			
			Filter descf = null;
			if(possibles.size() > 0)
			{
				descf = Filter.getRandom(n.random, chandler.handleChanceIncs(units, possibles));

			}
			else
			{
				System.out.println("No suitable troop descriptions for " + role + "!");
			}
			
			describeUnits(dr, units, descf);
			
		}
		
	}

	private void describeCommanders()
	{
		String[] roles = {"commanders", "priests"};
		
		ChanceIncHandler chandler = new ChanceIncHandler(n);
		List<Filter> descs = ChanceIncHandler.retrieveFilters("troopdescriptions", "troopdescs", n.nationGen.descriptions, null, n.races.get(0));

		DescriptionReplacer dr = new DescriptionReplacer(n);
		
		for(String role : roles)
		{
			List<Unit> units = n.generateComList(role);
	
			dr.calibrate(units);
			for(Unit u : units)
			{
				dr.calibrate(u);
				
				String desc = "";
				if(Generic.getTagValue(u.race.tags, "description") != null)
					desc = Generic.getTagValue(u.race.tags, "description") + " ";
				
	
	
				Command tmpDesc = null;
				

				for(Command c : u.getCommands())
				{
					if(c.command.equals("#descr"))
						tmpDesc = c;
				}
				
				if(tmpDesc != null)
				{
					desc += tmpDesc.args.get(0);	
				}
			
				for(Filter f : u.appliedFilters)
				{
					if(f != null && Generic.getTagValue(f.tags, "description") != null)
					{
						desc = desc + " " + Generic.getTagValue(f.tags, "description");
					}
				}
				
				
				desc = dr.replace(desc);
				
				if(tmpDesc != null)
					u.setCommandValue("#descr", 0, "\"" + desc + "\"");
				else
					u.commands.add(new Command("#descr", "\"" + desc + "\""));
			}
		}
		
	}

	
}
