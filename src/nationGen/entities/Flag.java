package nationGen.entities;

import java.util.ArrayList;
import java.util.List;

import com.elmokki.Generic;

import nationGen.NationGen;


public class Flag extends Drawable {

	public Flag(NationGen nationGen) {
		super(nationGen);
	}
	

	public List<String> allowed = new ArrayList<String>();
	
        @Override
	public <Entity> void handleOwnCommand(String str)
	{

		List<String> args = Generic.parseArgs(str);
		
		try
		{
		

		if(args.get(0).equals("#allowed"))
			this.allowed.add(args.get(1));
		else
			super.handleOwnCommand(str);
		
		}
		catch(IndexOutOfBoundsException e)
		{
			System.out.println("WARNING: " + str + " has insufficient arguments (" + this.name + ")");
		}
	}

}
