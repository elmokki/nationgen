package nationGen.rostergeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.Filter;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.naming.NamePart;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class SpecialCommanderGenerator {

	Nation n;
	NationGen ng;
	ChanceIncHandler chandler;
	Random r;
	
	public SpecialCommanderGenerator(Nation n, NationGen ng)
	{
		this.n = n;
		this.ng = ng;
		this.chandler = new ChanceIncHandler(n);
		this.r = new Random(n.random.nextInt());
	}
	
	
	public void generate()
	{
		
		Race race = n.races.get(0);
		
		// Chance to use secondary race is third of the percentage of secondary race units in nation so far
		if(n.percentageOfRace(n.races.get(1)) / 3 > n.random.nextDouble())
		{
			race = n.races.get(1);
		}
		
		Unit u = null;
		
		List<Filter> possibles = ChanceIncHandler.retrieveFilters("specialcommanderfilters", "default_specialcommanderfilters", ng.filters, null, race);
		while(u == null)
		{
			if(chandler.countPossibleFilters(possibles) == 0)
				return;
			
			Filter f = chandler.getRandom(possibles);
			possibles.remove(f);


			List<Pose> possiblePoses = getPossiblePoses(f, race);
			if(chandler.countPossibleFilters(possiblePoses) > 0)
			{
				// get pose
				Pose p = chandler.getRandom(possiblePoses);
				
				// generate unit
				UnitGen ug = new UnitGen(ng, n);
				u = ug.generateUnit(race, p);
				u.appliedFilters.add(f);
				
				boolean mage = p.roles.contains("mage") || p.roles.contains("priest");
				String targettag = Generic.getTagValue(f.tags, "equipmenttargettag");

				ug.armorUnit(u, null, null, targettag, mage);
				ug.armUnit(u, null, null, targettag, mage);
				ug = null;
				
				// change color
				double d = r.nextDouble();
				if(d < 0.3)
					u.color = n.colors[2];
				else if(d < 0.4)
					u.color = n.colors[3];

				// get name
				List<String> names = Generic.getTagValues(f.tags, "unitname");
				String name = names.get(r.nextInt(names.size()));
				u.name.type = NamePart.newNamePart(name, ng);
				
				// leadership
				boolean hasleader = false;
				for(Command c : u.getCommands())
					if(c.command.equals("#noleader") ||
					   c.command.equals("#poorleader") ||
					   c.command.equals("#okleader") ||
					   c.command.equals("#goodleader") ||
					   c.command.equals("#expertleader") ||
					   c.command.equals("#superiorleader"))
						hasleader = true;
				
				if(!hasleader)
				{
					if(r.nextBoolean())
						u.commands.add(new Command("#noleader"));
					else
						u.commands.add(new Command("#poorleader"));
				}
				
				// cap only
				double chance = 1;
				if(Generic.containsTag(f.tags, "caponlychance"))
					chance = Double.parseDouble(Generic.getTagValue(f.tags, "caponlychance"));
				
				if(r.nextDouble() < chance)
					u.caponly = true;
				
				// put to lists
				if(f.tags.contains("troop"))
				{
					if(n.unitlists.get("special") == null)
						n.unitlists.put("special", new ArrayList<Unit>());
					
					n.unitlists.get("special").add(u);
				}
				else
				{
					u.commands.add(new Command("#gcost", "+30"));
					if(n.comlists.get("specialcoms") == null)
						n.comlists.put("specialcoms", new ArrayList<Unit>());
					
					n.comlists.get("specialcoms").add(u);
				}
				
				
				
			}
			else
			{
				continue;
			}
			
		}
		chandler = null;
		
		
	}
	
	
	private boolean fillsRequirements(Pose p, Filter f)
	{
		boolean ok = true;
		
		String req = Generic.getTagValue(f.tags, "requiredposetag");
		if(req != null && !Generic.containsTag(p.tags, req))
			ok = false;
		
		req = Generic.getTagValue(f.tags, "requiredposetheme");
		if(req != null && !Generic.containsTag(p.themes, req))
			ok = false;
		
		
		return ok;
		
	}
	
	private List<Pose> getPossiblePoses(Filter f, Race race)
	{
		List<Pose> poses = new ArrayList<Pose>();
		
		for(String str : f.tags)
		{
			List<String> args = Generic.parseArgs(str);
			if(args.get(0).equals("pose"))
			{
				String goal = args.get(2);
				if(args.get(1).equals("role"))
				{
					for(Pose p : race.poses)
					{
						if(p.roles.contains(goal) && !poses.contains(p) && fillsRequirements(p, f))
							poses.add(p);
					}
				}
				else if(args.get(1).equals("name"))
				{
					for(Pose p : race.poses)
					{
						if(p.name.equals(goal) && !poses.contains(p) && fillsRequirements(p, f))
							poses.add(p);
					}
				}
			}
		}
		
		return poses;
	}
	
}
