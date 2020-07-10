package nationGen.naming;

import nationGen.NationGenAssets;
import nationGen.entities.Filter;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;


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
			
			StringBuilder desc = new StringBuilder(u.race.tags.getString("description").map(s -> s + " ").orElse(""));

			
			if(descf != null)
			{
				desc.append(descf.name).append(" ");
			}
			
			
			desc.append(u.slotmap.items()
				.map(i -> i.tags.getString("description"))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.joining(" "))).append(" ");
				
			
			desc = new StringBuilder(dr.replace(desc.toString().trim()));
			
			
			Command tmpDesc = null;
			

			for(Command c : u.getCommands())
			{
				if(c.command.equals("#descr"))
					tmpDesc = c;
			}
			
			
			if(tmpDesc != null)
			{
				desc.append(" ").append(tmpDesc.args.get(0));
			}
			
			for(Filter f : u.appliedFilters)
			{
				if(f != null)
				{
					desc.append(f.tags.getString("description").map(s -> " " + s).orElse(""));
				}
			}
			
			
			if(u.tags.containsName("montagunit"))
			{
				if(desc.length() > 0)
					desc.append("\n\n");
				
				desc.append("When recruited, one unit of this category will appear instead of the unit shown here.");
			}
			
			String description = dr.replace(desc.toString()).replaceAll("\"", "");

			if(tmpDesc != null)
				u.setCommandValue("#descr", description);
			else
				u.commands.add(Command.args("#descr", description));

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
			
			List<Unit> units = n.listTroops(role);
			if(units.isEmpty())
				continue;
				
				
			if(!role.equals("mounted"))
				dr.descs.put("%role%", role);
			else
				dr.descs.put("%role%", "cavalry");
	
			if(role.equals("chariot"))
				dr.descs.put("%role%", role + "s");
			if(role.equals("ranged") || role.equals("sacred"))
				dr.descs.put("%role%", role + " units");
			


			
			List<Filter> possibles = new ArrayList<>();
			for(Filter f : descs)
				if(ChanceIncHandler.suitableFor(units.get(0), f, n))
					possibles.add(f);
			
			Filter descf = null;
			if(possibles.size() > 0)
			{
				descf = chandler.handleChanceIncs(units.get(0), possibles).getRandom(random);

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
			List<Unit> units = n.listCommanders(role);
	
			dr.calibrate(units);
			for(Unit u : units)
			{
				dr.calibrate(u);
				
				StringBuilder desc = new StringBuilder(u.race.tags.getString("description").map(s -> s + " ").orElse(""));
	
	
				Command tmpDesc = null;
				

				for(Command c : u.getCommands())
				{
					if(c.command.equals("#descr"))
						tmpDesc = c;
				}
				
				if(tmpDesc != null)
				{
					desc.append(tmpDesc.args.get(0));
				}
			
				for(Filter f : u.appliedFilters)
				{
					if(f != null)
					{
						desc.append(f.tags.getValue("description").map(s -> " " + s).orElse(""));
					}
				}
				
				
				String description = dr.replace(desc.toString()).replaceAll("\"", "");

				if(tmpDesc != null)
					u.setCommandValue("#descr", description);
				else
					u.commands.add(Command.args("#descr", description));
			}
		}
		
	}

	
}
