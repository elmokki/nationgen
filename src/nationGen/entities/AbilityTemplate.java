package nationGen.entities;

import java.util.ArrayList;
import java.util.List;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class AbilityTemplate extends Filter {

	public AbilityTemplate(NationGen nationGen) {
		super(nationGen);
		
	}
	
	String desc = "";
	List<String> addTags = new ArrayList<String>();
	public <E extends Entity> void handleOwnCommand(String str)
	{
		
		List<String> args = Generic.parseArgs(str);
		try
		{
			if(args.get(0).equals("#desc"))
			{
				desc = args.get(1);
			}
			else if(args.get(0).equals("#addtag"))
			{
				args.remove(0);
				addTags.add(Generic.listToString(args));
			}

			else
				super.handleOwnCommand(str);
		
		}
		catch(IndexOutOfBoundsException e)
		{
			System.out.println("WARNING: " + str + " has insufficient arguments (" + this.name + ")");
		}
	}
	
	public void finish()
	{
		if(desc.equals(""))
			desc = this.name;
	}
	
	public void apply(Unit u, Nation n)
	{
		Filter f = new Filter(nationGen);
		f.commands.addAll(this.commands);
		u.tags.addAll(addTags);
		
		for(String tag : this.tags)
		{
			List<String> args = Generic.parseArgs(tag);
			if(args.get(0).equals("possiblecommand"))
			{
				if(n.random.nextDouble() < Double.parseDouble(args.get(2)))
					f.commands.add(Command.parseCommand(args.get(1)));
			}
		}
		

		f.name = "TEMPLATE FILTER: " + this.name;
		f.tags.add("description " + desc);

		if(f.commands.size() > 0 || addTags.size() > 0)
		{
			u.appliedFilters.add(f);
		}
	}

}
