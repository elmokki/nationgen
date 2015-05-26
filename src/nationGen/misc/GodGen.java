package nationGen.misc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.elmokki.Generic;


import nationGen.entities.Filter;
import nationGen.nation.Nation;




public class GodGen extends ChanceIncHandler {
	
	Nation n;
	List<Filter> orig;
	List<Filter> add;
	
	public GodGen(Nation n) {
		super(n);
		this.n = n;
		
		orig = new ArrayList<Filter>();
		add = new ArrayList<Filter>();
		for(String tag : n.races.get(0).tags)
		{
			List<String> args = Generic.parseArgs(tag);
			if(args.get(0).equals("gods"))
			{
				orig.addAll(n.nationGen.miscdef.get(args.get(1)));
			}
			if(args.get(0).equals("additionalgods"))
			{
				add.addAll(n.nationGen.miscdef.get(args.get(1)));
			}
		}
		if(orig.size() == 0)
			orig.addAll(n.nationGen.miscdef.get("defaultgods"));

	}
	
	

	public List<Command> giveGods()
	{
		List<Command> filters = new ArrayList<Command>();
		ChanceIncHandler chandler = new ChanceIncHandler(n);
		
		LinkedHashMap<Filter, Double> possibles = chandler.handleChanceIncs(orig);
		Filter pantheon = Filter.getRandom(n.random, possibles);
		filters.addAll(pantheon.commands);
		
		
		filters.add(new Command("#homerealm", "10"));
		
		if(add.size() > 0)
		{
			List<Filter> mores = new ArrayList<Filter>();
			for(Filter g : add)
			{
				List<String> allowed = new ArrayList<String>();
				for(String t : g.tags)
				{
					List<String> args = Generic.parseArgs(t);
					if(args.get(0).equals("allowed"))
						allowed.add(args.get(1));
				}
				
				if(allowed.size() == 0 || allowed.contains(pantheon.name))
					mores.add(g);
			}
			
			possibles = chandler.handleChanceIncs(mores);
			
			// 0 to 4 extra Filters
			int moreFilters = n.random.nextInt(Math.min(mores.size() + 1, 5));


			for(int i = 0; i < moreFilters; i++)
			{
				Filter newFilter = Filter.getRandom(n.random, possibles);
				mores.remove(newFilter);
				filters.addAll(newFilter.commands);
			}
		}
		return filters;
		
	}
}
