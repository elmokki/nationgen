package nationGen.entities;


import nationGen.NationGen;
import nationGen.misc.Command;
import nationGen.misc.ItemSet;

import java.util.ArrayList;
import java.util.List;

public class Race extends Filter {
	public String longsyllables = "";
	public String shortsyllables = "";
	public String namesuffixes = "";
	public List<Command> nationcommands = new ArrayList<>();
	public List<Command> unitcommands = new ArrayList<>();
	public List<Command> specialcommands = new ArrayList<>();
	public List<Pose> poses = new ArrayList<>();
	public List<Pose> spriteGenPoses = new ArrayList<>();
	
	public List<Theme> themefilters = new ArrayList<>();

	public String visiblename = null;
	

	public Race(NationGen nationGen)
	{

		super(nationGen);

		addCommand(Command.parse("#gcost 10"));
		addCommand(Command.parse("#ap 12"));
		addCommand(Command.parse("#mapmove 16"));
		addCommand(Command.parse("#mor 10"));
		addCommand(Command.parse("#mr 10"));
		addCommand(Command.parse("#hp 10"));
		addCommand(Command.parse("#str 10"));
		addCommand(Command.parse("#att 10"));
		addCommand(Command.parse("#def 10"));
		addCommand(Command.parse("#prec 10"));
		addCommand(Command.parse("#enc 3"));
		addCommand(Command.parse("#size 2"));
		addCommand(Command.parse("#maxage 50"));
	}
	

	/**
	 * Adds a new command replacing old one of the same type. Just so nations can have both defaults and 
	 * custom stuff on top of that.
	 * @param c
	 */
	public void addCommand(Command c)
	{
		for(int i = 0; i < unitcommands.size(); i++)
			if(unitcommands.get(i).command.equals(c.command))
			{
				String arg = c.args.get(0).get();
				if (!arg.startsWith("+") && !arg.startsWith("-") && !arg.startsWith("*")) {
					unitcommands.remove(unitcommands.get(i));
				}
			}
			unitcommands.add(c);
		
	}
	
	

	
	
	/**
	 * Adds a new command replacing old one of the same type. Just so nations can have both defaults and 
	 * custom stuff on top of that.
	 * @param command
	 */
	public void addOwnLine(Command command)
	{
		tags.remove(command);

		this.handleOwnCommand(command);
		
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
		if(sacred && this.tags.containsName("all_troops_sacred"))
			allok = true;
		else if(!sacred && this.tags.containsName("all_troops_elite"))
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
	public void handleOwnCommand(Command command)
	{
		switch (command.command) {
			case "#longsyllables":
				this.longsyllables = command.args.get(0).get();
				break;
			case "#shortsyllables":
				this.shortsyllables = command.args.get(0).get();
				break;
			case "#suffixes":
				this.namesuffixes = command.args.get(0).get();
				break;
			case "#visiblename":
				this.visiblename = command.args.get(0).get();
				break;
			case "#nationcommand":
				this.nationcommands.add(command.args.get(0).getCommand());
				break;
			case "#unitcommand":
				this.addCommand(command.args.get(0).getCommand());
				break;
			case "#specialcommand":
				this.specialcommands.add(command.args.get(0).getCommand());
				break;
			case "#pose": {
				List<Pose> set = nationGen.getAssets().poses.get(command.args.get(0).get());
				if (set == null) {
					throw new IllegalArgumentException("Pose set " + command.args.get(0).get() + " was not found.");
				} else {
					this.poses.addAll(set);
				}
				break;
			}
			case "#spritegenpose": {
				List<Pose> set = nationGen.getAssets().poses.get(command.args.get(0).get());
				if (set == null) {
					System.out.println("Pose set " + command.args.get(0).get() + " was not found.");
				} else {
					this.spriteGenPoses.addAll(set);
				}
				break;
			}
			default:
				super.handleOwnCommand(command);
				break;
		}

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
		List<Pose> poses = new ArrayList<>();
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
		for(Command str2 : t.nationeffects)
		{
			addOwnLine(str2);
		}
		for(Command str2 : t.bothnationeffects)
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
		r.tags.addAllNames(this.types);
		r.themes.addAll(this.themes);
		
		return r;
	}
	

}
