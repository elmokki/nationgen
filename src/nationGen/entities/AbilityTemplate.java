package nationGen.entities;


import nationGen.NationGen;
import nationGen.misc.Args;
import nationGen.misc.Command;
import nationGen.misc.Tags;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class AbilityTemplate extends Filter {

	public AbilityTemplate(NationGen nationGen) {
		super(nationGen);
		
	}
	
	String desc = "";
	Tags addTags = new Tags();
        
	@Override
	public void handleOwnCommand(Command str)
	{
		
		try
		{
			if(str.command.equals("#desc"))
			{
				desc = str.args.get(0).get();
			}
			else if(str.command.equals("#addtag"))
			{
				addTags.addFromCommand(str.args.get(0).getCommand());
			}

			else
				super.handleOwnCommand(str);
		
		}
		catch(IndexOutOfBoundsException e)
		{
			throw new IllegalArgumentException("WARNING: " + str + " has insufficient arguments (" + this.name + ")", e);
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
		
		for(Args args : this.tags.getAllArgs("possiblecommand"))
		{
			if(n.random.nextDouble() < args.get(1).getDouble())
				f.commands.add(args.get(0).getCommand());
		}
		

		f.name = "TEMPLATE FILTER: " + this.name;
		f.tags.add("description", desc);

		if(f.commands.size() > 0 || !addTags.isEmpty())
		{
			u.appliedFilters.add(f);
		}
	}

}
