package nationGen.entities;

import java.util.ArrayList;
import java.util.List;

import com.elmokki.Generic;

import nationGen.NationGen;

/**
 * Class for handling national theme effects
 *
 */
public class Theme extends Filter {

	
	public List<String> nationeffects = new ArrayList<String>();
	public List<String> secondarynationeffects = new ArrayList<String>();
	
	public Theme(NationGen nationGen) {
		super(nationGen);
	}
	
	public <E extends Entity> void handleOwnCommand(String str)
	{

		List<String> args = Generic.parseArgs(str);
		
		try
		{
		

			if(args.get(0).equals("#racedefinition"))
			{
				args.remove(0);
				this.nationeffects.add(Generic.listToString(args));
			}
			else if(args.get(0).equals("#secondaryracedefinition"))
			{
				args.remove(0);
				this.secondarynationeffects.add(Generic.listToString(args));
			}
			else
				super.handleOwnCommand(str);
		
		}
		catch(IndexOutOfBoundsException e)
		{
			System.out.println("WARNING: " + str + " has insufficient arguments (" + this.name + ")");
		}
	}
}
