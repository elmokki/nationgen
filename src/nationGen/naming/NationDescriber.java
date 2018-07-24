package nationGen.naming;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.elmokki.Generic;

import nationGen.NationGenAssets;
import nationGen.entities.Filter;
import nationGen.items.Item;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class NationDescriber {
	private Nation n;
	private Random random;
	private NationGenAssets assets;
	
	public NationDescriber(Nation n, NationGenAssets assets)
	{
		random = new Random(n.random.nextInt());
		this.n = n;
		this.assets = assets;
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
			
			
			Command tmpDesc = null;
			

			for(Command c : u.getCommands())
			{
				if(c.command.equals("#descr"))
					tmpDesc = c;
			}
			
			
			if(tmpDesc != null)
			{
				desc += " " + tmpDesc.args.get(0).replaceAll("\"", "");	
			}
			
			for(Filter f : u.appliedFilters)
			{
				if(f != null && Generic.getTagValue(f.tags, "description") != null)
				{
					desc = desc + " " + Generic.getTagValue(f.tags, "description");
				}
			}
			
			
			if(Generic.containsTag(u.tags, "montagunit"))
			{
				if(desc.length() > 0)
					desc = desc + "\n\n";
				
				desc = desc + "When recruited, one unit of this category will appear instead of the unit shown here.";
			}
			
			desc = dr.replace(desc);
			desc = desc.replaceAll("\"", "");

			if(tmpDesc != null)
				u.setCommandValue("#descr", 0, "\"" + desc + "\"");
			else
				u.commands.add(new Command("#descr", "\"" + desc + "\""));

		}
	}
	
	private void describeTroops()
	{
		String[] roles = {"ranged", "infantry", "mounted", "chariot", "sacred"};
		
		ChanceIncHandler chandler = new ChanceIncHandler(n);
		List<Filter> descs = ChanceIncHandler.retrieveFilters("troopdescriptions", "troopdescs", assets.descriptions, null, n.races.get(0));
		
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
			if(role.equals("ranged") || role.equals("sacred"))
				dr.descs.put("%role%", role + " units");
			


			
			List<Filter> possibles = new ArrayList<Filter>();
			for(Filter f : descs)
				if(ChanceIncHandler.suitableFor(units.get(0), f, n))
					possibles.add(f);
			
			Filter descf = null;
			if(possibles.size() > 0)
			{
				descf = Filter.getRandom(random, chandler.handleChanceIncs(units.get(0), possibles));

			}

			
			describeUnits(dr, units, descf);
			
		}
		
	}

	private void describeCommanders()
	{
		String[] roles = {"commanders", "priests"};
		
		//ChanceIncHandler chandler = new ChanceIncHandler(n);
		//List<Filter> descs = ChanceIncHandler.retrieveFilters("troopdescriptions", "troopdescs", n.nationGen.descriptions, null, n.races.get(0));

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
				desc = desc.replaceAll("\"", "");

				if(tmpDesc != null)
					u.setCommandValue("#descr", 0, "\"" + desc + "\"");
				else
					u.commands.add(new Command("#descr", "\"" + desc + "\""));
			}
		}
		
	}

	
}
