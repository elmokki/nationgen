package nationGen.entities;


import nationGen.NationGen;
import nationGen.misc.Command;
import nationGen.misc.FileUtil;
import nationGen.misc.Tags;

import java.util.*;


public class Entity {

	
	public NationGen nationGen;
	public String name = null;
	public double basechance = 1;
	public Tags tags = new Tags();
	public List<String> themes = new ArrayList<>();

	public Entity(NationGen nationGen)
	{
		this.nationGen = nationGen;
	}

	
	public static <E extends Entity> List<E> readFile(NationGen nationGen, String file, Class<E> c)
	{
		List<E> list = new ArrayList<>();
		
		E instance = null;
		int line = 0;
		for (String strLine : FileUtil.readLines(file))
		{
			line++;
			try {
				if (strLine.trim().toLowerCase().startsWith("#new")) {
					if (instance != null) {
						throw new IllegalStateException("The previous definition must be closed with #end before starting another with #new");
					}
					try {
						instance = c.getConstructor(NationGen.class).newInstance(nationGen);
					} catch (Exception e) {
						throw new IllegalStateException("Error initializing a new instance of " + c.getCanonicalName(), e);
					}
				} else if (strLine.trim().toLowerCase().startsWith("#end")) {
					if (instance != null) {
						instance.finish();
						list.add(instance);
					}
					instance = null;
				} else if (strLine.startsWith("#")) {
					if (instance == null) {
						throw new IllegalStateException("Dangling command detected");
					}
					instance.handleOwnCommand(Command.parse(strLine));
				}
			} catch (Exception e) {
				throw new IllegalStateException("Error handling file '" + file + "' line " + line + ": " + strLine, e);
			}
		}
		
		return list;
	}
	
	
	public void handleOwnCommand(Command command)
	{
		if(command.command.equals("#id") || command.command.equals("#name"))
		{
			this.name = command.args.get(0).get();
		}
		else if(command.command.equals("#basechance") || command.command.equals("#chance"))
		{
			this.basechance = command.args.get(0).getDouble();
		}
		else if(command.command.equals("#tag"))
		{
			this.tags.addFromCommand(command.args.get(0).getCommand());
		}
		else if(command.command.equals("#theme"))
		{
			this.themes.add(command.args.get(0).get());
		}
		else if(command.command.startsWith("#"))
		{
			this.tags.add(command.command.substring(1), command.args);
		}
	}


	
	public String toString()
	{
		return this.name;
	}
	
	
	/**
	 * Called when #end is read in file. Used at least for race to set visiblename to name if no #visiblename was found
	 */
	protected void finish()
	{
		
	}
}

