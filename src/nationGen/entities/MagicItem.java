package nationGen.entities;

import java.util.ArrayList;
import java.util.List;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.items.Item;


public class MagicItem extends Filter {

	public MagicItem(NationGen nationGen) {
		super(nationGen);
	}

	public List<String> names = new ArrayList<String>();
	public Item baseitem;
	public String effect = "-1";
	public boolean always = false;
	
        @Override
	public <Entity> void handleOwnCommand(String str)
	{

		List<String> args = Generic.parseArgs(str);
		
		try
		{
		
		if(args.get(0).equals("#unitname"))
			names.add(args.get(1));
		else if(args.get(0).equals("#eff"))
			effect = args.get(1);
		else
			super.handleOwnCommand(str);
		
		}
		catch(IndexOutOfBoundsException e)
		{
			System.out.println("WARNING: " + str + " has insufficient arguments (" + this.name + ")");
		}
	}


}
