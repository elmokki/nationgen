package nationGen.naming;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class PriestNamer extends MageNamer {

	public PriestNamer(NationGen ng) {
		super(ng);
		
	}

	public void execute(Nation n)
	{
		this.n = n;
		this.random = new Random(n.random.nextInt());
		loadNameData();
		
		List<Unit> priests = n.comlists.get("priests");
		
		List<Unit> primaries = new ArrayList<Unit>();
		List<Unit> secondaries = new ArrayList<Unit>();
		List<Unit> tertiaries = new ArrayList<Unit>();
		
		List<List<Unit>> all = new ArrayList<List<Unit>>();
		all.add(tertiaries);
		all.add(secondaries);
		all.add(primaries);
		
		int maxTier = 0;
		for(Unit u : priests)
		{
			for(int i = 1; i < 4; i++)
			{
				if(u.tags.contains("priest " + i))
				{
					all.get(i - 1).add(u);
					if(maxTier < i)
						maxTier = i;
				}	
			}
		}
		
		
		// prefixrank = <rank> <fixed name>
		// other = <rank based name>
		boolean prefixrank = n.random.nextBoolean();
		
		// whether extra special fun stuff should be suffix or prefix
		// ie apprentice storm druid or apprentice druid of astral graves
		// ie golden storm master or master of golden storm
		boolean suffix = n.random.nextBoolean();
		
		
		for(int i = 2; i >= 0; i--)
		{
			if(all.get(i).size() == 0)
				continue;
			
			if(i < 2 && all.get(i + 1).size() > 0)
			{
				this.deriveNames(all.get(i + 1), all.get(i), (i + 1), prefixrank, suffix);
			}
			else
			{
				if(i < 2)
					this.generateNewNames(all.get(i), 3, prefixrank, suffix);
				else
					this.generateNewNames(all.get(i), (i + 1), prefixrank, suffix);

			}
		}
		
		

	}
	
	protected void loadNameData()
	{
		
		for(String tag : n.races.get(0).tags)
		{
			List<String> args = Generic.parseArgs(tag);
			if(args.get(0).equals("priestnames"))
			{
				names.addAll(n.nationGen.magenames.get(args.get(1)));
			}
			else if(args.get(0).equals("priestadjectives"))
			{
				adjectives.addAll(n.nationGen.magenames.get(args.get(1)));
			}
			else if(args.get(0).equals("priestnouns"))
			{
				nouns.addAll(n.nationGen.magenames.get(args.get(1)));
			}
			else if(args.get(0).equals("tieredpriestnames"))
			{
				tieredmage.addAll(n.nationGen.magenames.get(args.get(1)));
			}
			else if(args.get(0).equals("priestrankedprefixes"))
			{
				rankedprefix.addAll(n.nationGen.magenames.get(args.get(1)));
			}
		}
		
		if(names.size() == 0)
			names = n.nationGen.magenames.get("defaultnames");
		if(adjectives.size() == 0)
			adjectives = n.nationGen.magenames.get("defaultadjectives");
		if(nouns.size() == 0)
			nouns = n.nationGen.magenames.get("defaultnouns");
		if(tieredmage.size() == 0)
			tieredmage = n.nationGen.magenames.get("defaulttieredpriestnames");
		if(rankedprefix.size() == 0)
			rankedprefix = n.nationGen.magenames.get("defaultprefixranks");
		
	
	}
}
