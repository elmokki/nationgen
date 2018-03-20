package nationGen.entities;

import java.util.ArrayList;
import java.util.List;

import com.elmokki.Generic;






import nationGen.NationGen;
import nationGen.misc.Command;
import nationGen.misc.ItemSet;

public class Race extends Filter {
	public String longsyllables = "";
	public String shortsyllables = "";
	public String namesuffixes = "";
	public List<Command> nationcommands = new ArrayList<Command>();
	public List<Command> unitcommands = new ArrayList<Command>();
	public List<Command> specialcommands = new ArrayList<Command>();
	public List<Pose> poses = new ArrayList<Pose>();
	public List<Pose> spriteGenPoses = new ArrayList<Pose>();
	
	public List<Theme> themefilters = new ArrayList<Theme>();

	public String visiblename = null;
	

	public Race(NationGen nationGen)
	{

		super(nationGen);

		addCommand("#gcost 10");
		addCommand("#ap 12");
		addCommand("#mapmove 16");
		addCommand("#mor 10");
		addCommand("#mr 10");
		addCommand("#hp 10");
		addCommand("#str 10");
		addCommand("#att 10");
		addCommand("#def 10");
		addCommand("#prec 10");
		addCommand("#enc 3");
		addCommand("#size 2");
		addCommand("#maxage 50");
	}
	

	/**
	 * Adds a new command replacing old one of the same type. Just so nations can have both defaults and 
	 * custom stuff on top of that.
	 * @param str
	 */
	public void addCommand(String str)
	{
		if((str.startsWith("\'") && str.endsWith("\'")) || (str.startsWith("\"") && str.endsWith("\"")))  // do final cleanup on command EA20150604
			str = str.substring(1, str.length()-1);
		
		Command c = Command.parseCommand(str);
		
		for(int i = 0; i < unitcommands.size(); i++)
			if(unitcommands.get(i).command.equals(c.command))
			{
				
				if(c.args.get(0).startsWith("+") || c.args.get(0).startsWith("-") || c.args.get(0).startsWith("*"))
				{
					
				}
				else
				{
					unitcommands.remove(unitcommands.get(i));
				}
			}
			unitcommands.add(c);
		
	}
	
	

	
	
	/**
	 * Adds a new command replacing old one of the same type. Just so nations can have both defaults and 
	 * custom stuff on top of that.
	 * @param str
	 */
	public void addOwnLine(String str)
	{
		List<String> args = Generic.parseArgs(str);
		List<String> toBeRemoved = new ArrayList<String>();
		
		for(int i = 0; i < tags.size(); i++)
		{
			List<String> args2 = Generic.parseArgs(tags.get(i));
			
			if(args.size() != args2.size() || args.size() < 2 || !args.get(1).equals(args2.get(1)))
				continue;
			
			boolean ok = true;
			for(int j = 1; j < args.size() - 1; j++)
			{
				if(args.get(j).startsWith("\'") || args.get(j).startsWith("\""))
					args.set(j, args.get(j).substring(1));
				if(args.get(j).endsWith("\'") || args.get(j).endsWith("\""))
					args.set(j, args.get(j).substring(0, args.get(j).length()-1));	
					
				if(!args.get(j).equals(args2.get(j)))
				{
					ok = false;
					break;
				}

			}
			
			if(ok)
			{
				toBeRemoved.add(tags.get(i));
			}

		}
		
		tags.removeAll(toBeRemoved);

		this.handleOwnCommand(str);
		
	}
	
	public boolean hasRole(String role)
	{
		for(Pose p : poses)
		{
			if(p.roles.contains(role))
				return true;
		}
		return false;
	}
	
	public boolean hasSpecialRole(String role, boolean sacred)
	{
		String str = "elite";
		boolean allok = false;
		if(sacred && this.tags.contains("all_troops_sacred"))
			allok = true;
		else if(!sacred && this.tags.contains("all_troops_elite"))
			allok = true;
		
		
		if(sacred)
			str = "sacred";
		for(Pose p : poses)
		{
			for(String role2 : p.roles)
				if(role2.toLowerCase().equals((str + " " + role).toLowerCase()) || (role2.equals(role) && allok) || (role2.equals(role) && p.roles.contains(str)))
					return true;
		}
		return false;
	}
	
	
	@Override
	public <Entity> void handleOwnCommand(String str)
	{
		List<String> args = Generic.parseArgs(str);
		if(args.size() == 0)
			return;
			
		if(args.get(0).equals("#longsyllables"))
			this.longsyllables = args.get(1);
		else if(args.get(0).equals("#shortsyllables"))
			this.shortsyllables = args.get(1);
		else if(args.get(0).equals("#suffixes"))
			this.namesuffixes = args.get(1);
		else if(args.get(0).equals("#visiblename"))
			this.visiblename = args.get(1);
		else if(args.get(0).equals("#nationcommand"))
		{
			Command c = Command.parseCommandFromDefinition(args);
			this.nationcommands.add(c);
		}
		else if(args.get(0).equals("#unitcommand"))
		{			
			args.remove(0);
			this.addCommand(Generic.listToString(args));
		}
		else if(args.get(0).equals("#specialcommand"))
			this.specialcommands.add(Command.parseCommandFromDefinition(args));
		else if(args.get(0).equals("#pose"))
		{
			List<Pose> set = nationGen.poses.get(args.get(1));
			if(set == null)
			{
				System.out.println("Pose set " + args.get(1) + " was not found.");
			}
			else
			{
				this.poses.addAll(set);
			}
		}
		else if(args.get(0).equals("#spritegenpose"))
		{
			List<Pose> set = nationGen.poses.get(args.get(1));
			if(set == null)
			{
				System.out.println("Pose set " + args.get(1) + " was not found.");
			}
			else
			{
				this.spriteGenPoses.addAll(set);
			}
		}
		else
			super.handleOwnCommand(str);

	}
	
	
	public ItemSet getItems(String slot, String role)
	{
		ItemSet items = new ItemSet();
		for(Pose p : poses)
			if(p.roles.contains(role))
			{
				if(p.getItems(slot) != null)
					items.addAll(p.getItems(slot));
			}
		
		return items;
	}
	
	
	public List<Pose> getPoses(String role)
	{
		List<Pose> poses = new ArrayList<Pose>();
		for(Pose p : this.poses)
		{
			if(p.roles.contains(role))
			{
				poses.add(p);
			}
		}
			
		return poses;
	}
	
	@Override
	protected void finish()
	{
		if(visiblename == null)
			visiblename = name;
	}


	

	public void handleTheme(Theme t)
	{
		// Add race commands
		for(String str2 : t.nationeffects)
		{
			addOwnLine(str2);
		}
		for(String str2 : t.bothnationeffects)
		{
			addOwnLine(str2);
		}
	}
	
	public Race getCopy()
	{
		Race r = new Race(nationGen);
		r.longsyllables = this.longsyllables;
		r.shortsyllables = this.shortsyllables;
		r.namesuffixes = this.namesuffixes;
		r.nationcommands.addAll(this.nationcommands);
		r.unitcommands.addAll(this.unitcommands);
		r.specialcommands.addAll(this.specialcommands);
		r.poses.addAll(this.poses);
		r.spriteGenPoses.addAll(this.spriteGenPoses);
		r.chanceincs.addAll(this.chanceincs);
		r.tags.addAll(this.tags);
		r.visiblename = this.visiblename;
		r.basechance = this.basechance;
		r.commands.addAll(this.commands);
		r.name = this.name;
		r.types.addAll(this.types);
		r.tags.addAll(this.types);
		r.themes.addAll(this.themes);
		
		return r;
	}
	

}
