package nationGen.entities;


import nationGen.NationGen;
import nationGen.misc.Command;

import java.util.ArrayList;
import java.util.List;


/**
 * Class for handling national theme effects
 *
 */
public class Theme extends Filter {

	
	public List<Command> nationeffects = new ArrayList<>();
	public List<Command> secondarynationeffects = new ArrayList<>();
	public List<Command> bothnationeffects = new ArrayList<>();

	public Theme(NationGen nationGen) {
		super(nationGen);
	}
	
	@Override
	public void handleOwnCommand(Command command)
	{
		try
		{
			switch (command.command) {
				case "#racedefinition":
					this.nationeffects.add(command.args.get(0).getCommand());
					break;
				case "#secondaryracedefinition":
					this.secondarynationeffects.add(command.args.get(0).getCommand());
					break;
				case "#bothracedefinition":
					this.bothnationeffects.add(command.args.get(0).getCommand());
					break;
				default:
					super.handleOwnCommand(command);
					break;
			}
		}
		catch(IndexOutOfBoundsException e)
		{
			throw new IllegalArgumentException("Command [" + command + "] has insufficient arguments (" + this.name + ")", e);
		}
	}
}
