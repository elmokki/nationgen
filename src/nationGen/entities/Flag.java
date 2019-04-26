package nationGen.entities;


import nationGen.NationGen;
import nationGen.misc.Command;

import java.util.ArrayList;
import java.util.List;


public class Flag extends Drawable {

	public Flag(NationGen nationGen) {
		super(nationGen);
	}
	

	public List<String> allowed = new ArrayList<String>();
	
	@Override
	public void handleOwnCommand(Command command)
	{
		try
		{
			if(command.command.equals("#allowed"))
				this.allowed.add(command.args.get(0).get());
			else
				super.handleOwnCommand(command);
		}
		catch(IndexOutOfBoundsException e)
		{
			throw new IllegalArgumentException("WARNING: " + command + " has insufficient arguments (" + this.name + ")", e);
		}
	}

}
