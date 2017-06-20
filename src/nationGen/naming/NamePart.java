package nationGen.naming;

import java.util.ArrayList;
import java.util.List;

import nationGen.NationGen;
import nationGen.entities.Entity;
import nationGen.entities.Filter;
import nationGen.misc.Command;
import nationGen.units.Unit;

import com.elmokki.Generic;


public class NamePart extends Filter {

	public boolean weak = false;
	public List<String> elements = new ArrayList<String>();
	public int minimumelements = 0;
	public int rank = -1;
	
	

	
	public NamePart(NationGen nationGen) {
		super(nationGen);
	}
	
	public static NamePart newNamePart(String text, NationGen nationGen)
	{
		NamePart np = new NamePart(nationGen);
		np.name = text;
		return np;
	}
	
	public String toString(Unit u)
	{
		if(u == null)
			return this.toString();
		
		String str = this.name;
		
		if(Generic.containsTag(tags, "commandvariant"))
		{
			for(String tag : this.tags)
			{
				List<String> args = Generic.parseArgs(tag);
				if(args.get(0).equals("commandvariant"))
				{
					String command = args.get(1);
					for(Command c : u.getCommands())
						if(c.command.equals(command))
						{
							boolean ok = true;
							
							if(args.contains("negative") && Integer.parseInt(c.args.get(0)) >= 0)
								ok = false;
							if(args.contains("positive") && Integer.parseInt(c.args.get(0)) <= 0)
								ok = false;
							
							if(ok)
							{
								str = args.get(args.size() - 1);
								break;
							}
						}
				}
			}
		}
		if(Generic.containsTag(tags, "posetagvariant"))
			for(String tag : this.tags)
			{
				List<String> args = Generic.parseArgs(tag);
				if(args.get(0).equals("posetagvariant"))
				{
					String command = args.get(1);
					if(u.pose.tags.contains(command))
					{
						str = args.get(2);
						break;
					}
				}
			}
		if(Generic.containsTag(tags, "racetagvariant"))
			for(String tag : this.tags)
			{
				List<String> args = Generic.parseArgs(tag);
				if(args.get(0).equals("racetagvariant"))
				{
					String command = args.get(1);
					if(u.race.tags.contains(command))
					{
						str = args.get(2);
						break;
					}
				}
			}
		
		if(Generic.containsTag(tags, "racevariant"))
			for(String tag : this.tags)
			{
				List<String> args = Generic.parseArgs(tag);
				if(args.get(0).equals("racevariant"))
				{
					if(u.race.name.equals(args.get(1)))
					{
						str = args.get(2);
						break;
					}
				}
			}
			
		return str;
	}
	
	public String toString()
	{
		return this.name;
	}
	
        @Override
	public <Entity> void handleOwnCommand(String str)
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
	
	public NamePart getCopy()
	{
		NamePart part = new NamePart(this.nationGen);
		part.name = this.name;
		part.weak = this.weak;
		part.elements.addAll(this.elements);
		part.minimumelements = this.minimumelements;
		part.types.addAll(this.types);
		part.tags.addAll(this.tags);
		part.themes.addAll(this.themes);

		return part;
	}
	
	public static NamePart fromLine(String line, NationGen ng)
	{
		if(line.startsWith("-") || line.equals(""))
			return null;
		
		NamePart part = new NamePart(ng);
		List<String> args = Generic.parseArgs(line);
		

		part.name = args.get(0);
		if(args.size() > 1)
		{
			part.minimumelements = Integer.parseInt(args.get(1));
		}
		// If part isn't longer it's minimum elements must be 0, as elements will be defined later
		if(args.size() < 3)
		{
			part.minimumelements = 0;
			return part;
		}
		
		args.remove(0);
		args.remove(0);
		
		for(String str : args)
		{
			if(str.equals("weak"))
				part.weak = true;
			else if(Generic.PathToInteger(str) != -1)
				part.elements.add(str);
			else
				part.tags.add(str);
		}
		
		return part;
	}
	
}
