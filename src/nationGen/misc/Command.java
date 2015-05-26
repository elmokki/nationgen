package nationGen.misc;

import java.util.ArrayList;
import java.util.List;

import nationGen.NationGen;

import com.elmokki.Generic;


public class Command {
	public String command = "";
	public List<String> args = new ArrayList<String>();
	
	public static void handleCommand(List<Command> commands, Command c, NationGen nationGen)
	{


		// List of commands that may appear more than once per unit
		List<String> uniques = new ArrayList<String>();
		uniques.add("#weapon");
		uniques.add("#custommagic");
		uniques.add("#magicskill");
		
		
		int copystats = -1;

		c = new Command(c.command, c.args);
		Command old = null;
		for(Command cmd : commands)
		{
			if(cmd.command.equals(c.command))
				old = cmd;
			
			if(cmd.command.equals("#copystats"))
				copystats = Integer.parseInt(cmd.args.get(0));
		}
		
		
		if(c.args.size() > 0 && (c.args.get(0).startsWith("+") || c.args.get(0).startsWith("-") || c.args.get(0).startsWith("*")) && copystats != -1 && old == null)
		{
			String value = nationGen.units.GetValue(copystats + "", c.command.substring(1));
			if(!value.equals(""))
			{
				old = new Command(c.command, value);
				commands.add(old);
			}
		}

		if(old != null && !uniques.contains(c.command))
		{
			/*
			if(this.tags.contains("sacred") && c.command.equals("#gcost"))
				System.out.println(c.command + "  " + c.args);
			*/
			for(int i = 0; i < c.args.size(); i++)
			{
			
				String arg = c.args.get(i);
				String oldarg = old.args.get(i);
				if(arg.startsWith("+") || arg.startsWith("-"))
				{
					if(arg.startsWith("+"))
						arg = arg.substring(1);
					

					try
					{
						//if(this.tags.contains("schoolmage 3"))
							//System.out.println(arg + " + " + oldarg + " = " + (int)(Integer.parseInt(oldarg) + Double.parseDouble(arg)));
						
						
						oldarg = "" + (Integer.parseInt(oldarg) + Integer.parseInt(arg));
						old.args.set(i, oldarg);
					}
					catch(NumberFormatException e)
					{
						System.out.println("FATAL ERROR: Argument parsing " + oldarg + " + " + arg + " on " + c.command + " caused crash.");
					}
					continue;
			
				}
				else if(arg.startsWith("*"))
				{
						
					arg = arg.substring(1);
					try
					{
			

						oldarg = "" + (int)(Integer.parseInt(oldarg) * Double.parseDouble(arg));
						old.args.set(i, oldarg);
					}
					catch(Exception e)
					{
						System.out.println("FATAL ERROR: Argument parsing " + oldarg + " * " + arg + " on " + c.command + " caused crash.");
					}
					continue;
				}
				else
				{
					if(!uniques.contains(c.command))
					{
						oldarg = arg;
						old.args.set(i, oldarg);
						continue;
	
					}
					else
					{
						commands.add(c);
						continue;
					}
				}
			}
		}
		else
		{

			
			for(int i = 0; i < c.args.size(); i++)
			{
				if(c.args.get(i).startsWith("+"))
					c.args.set(i, c.args.get(i).substring(1));
				else if(c.args.get(i).startsWith("*"))
					c.args.set(i, 0 + "");

			}
			commands.add(c);
		}
	
		

	}
	
	public static Command parseCommand(String str)
	{
		str = str.replaceAll("'", "\"");
		List<String> args = Generic.parseArgs(str);
		String command = args.get(0);
		args.remove(0);
		
		
		return new Command(command, args);
		
	}
	
	public Command(String cmd, List<String> args)
	{
		this.command = cmd;
		this.args.addAll(args);
	}
	
	public Command(String cmd, String arg)
	{
		this.command = cmd;
		this.args.add(arg);
	}
	
	public Command(String cmd)
	{
		this.command = cmd;
	}
	
	
	public String toString()
	{
		String str = command;
		for(String arg : args)
			str = str + " " + arg;
		
		return str;
	}
}
