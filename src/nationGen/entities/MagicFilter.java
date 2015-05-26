package nationGen.entities;



import java.util.ArrayList;
import java.util.List;



import nationGen.NationGen;
import nationGen.magic.MagicPattern;
import nationGen.misc.Command;

public class MagicFilter extends Filter {

	public MagicPattern pattern;
	public List<Integer> prio;
	
	public MagicFilter(NationGen nationGen) {
		super(nationGen);
		this.name = "MAGICPICKS";
	}
	
	public List<Command> getCommands()
	{
		List<Command> coms = new ArrayList<Command>();
		coms.addAll(commands);

		// Price
		if(this.name.equals("MAGICPICKS"))
			coms.add(new Command("#gcost", "+" + pattern.getPrice()));
		
		// Magic
		
		if(prio == null || pattern == null)
		{
			System.out.println("WARNING: An unit had incorrect magic specified by NationGen.");
			return coms;
		}
		
		coms.addAll(pattern.getMagicCommands(prio));
		
		return coms;
	}
	
}
	
