package nationGen.naming;

import java.util.List;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.Entity;
import nationGen.entities.Filter;

public class NameFilter extends Filter {

	
	public int rank = -1;
	
	
	public NamePart toPart()
	{
		NamePart np = new NamePart(this.name);
		np.tags.addAll(this.tags);
		return np;
	}
	
	public NameFilter(NationGen nationGen) {
		super(nationGen);
	}

	
	public <E extends Entity> void handleOwnCommand(String str)
	{

		List<String> args = Generic.parseArgs(str);
		
		try
		{
		
		if(args.get(0).equals("#rank"))
			this.rank = Integer.parseInt(args.get(1));
		else
			super.handleOwnCommand(str);
		
		}
		catch(IndexOutOfBoundsException e)
		{
			System.out.println("WARNING: " + str + " has insufficient arguments (" + this.name + ")");
		}
	}
}
