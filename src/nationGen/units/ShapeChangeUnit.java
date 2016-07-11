package nationGen.units;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.elmokki.Drawing;
import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.Filter;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.misc.Command;
import nationGen.naming.Name;
import nationGen.naming.NamePart;
import nationGen.nation.Nation;



public class ShapeChangeUnit extends Unit {

	public Unit otherForm;
	public ShapeShift thisForm;
	boolean sacred = false;
	public String shiftcommand = "#firstshape";
	private int gcost = 0;
	
	public ShapeChangeUnit(NationGen nationGen, Race race, Pose pose, Unit otherForm, ShapeShift thisForm)
	{
		super(nationGen, race, pose);
		this.otherForm = otherForm;
		this.thisForm = thisForm.getCopy();

	}


	public void polish(NationGen n, Nation nation)
	{
		Filter sf = new Filter(n);
		sf.name = "Special unit";
		
		// Copy sacredness and gcost from main form
		if(otherForm != null)
		{
			for(Command c : otherForm.getCommands())
			{
				if(c.command.equals("#holy") && !thisForm.tags.contains("mount"))
				{
					sacred = true;
				}
				else if(c.command.equals("#holy") && thisForm.tags.contains("mount"))
				{
					if(otherForm.tags.contains("sacredmount"))
						sacred = true;
				}
				
				if(c.command.equals("#gcost") && !thisForm.tags.contains("nogcost"))
				{
					
					//System.out.println(c.args.get(0) + " ADDED " + " / " + otherForm.getGoldCost_DEBUG());
					gcost = Integer.parseInt(c.args.get(0));


				}
				


			}
		}
		
		// Handle custom weapons
		for(Command c : thisForm.commands)
		{
			// Weapons
			if(c.command.equals("#weapon"))
			{
				String realarg = c.args.get(0);
				if(realarg.contains(" "))
					realarg = realarg.split(" ")[0];
				
				try
				{
					Integer.parseInt(realarg);
				}
				catch(Exception e)
				{
					c.args.set(0, n.getCustomItemId(c.args.get(0)) + "");
				}
			}
		}
		

		
		// Copy commands from this form
		for(Command c : thisForm.commands)
		{
			if(c.command.equals("#name") && c.args.size() > 0)
			{
				c.args.set(0, "\"" + Generic.capitalize(c.args.get(0).replaceAll("\"", "")) + "\"");
				name = new Name();
				
				name.type = NamePart.newNamePart(Generic.capitalize(c.args.get(0).replaceAll("\"", "")), null);
			}
			
			if(!c.command.startsWith("#spr"))
				sf.commands.add(c);
		
		}
		
		
			
		// ...and other Form
		if(otherForm != null)
		{

			
			// Inherit nametype and maxage
			
			boolean maxagefound = false;
			for(Command c : otherForm.getCommands())
				if(c.command.equals("#maxage") || c.command.equals("#nametype"))
				{
					sf.commands.add(c);
					if(c.command.equals("#maxage"))
						maxagefound = true;
				}
			
			if(!maxagefound)
			{
				sf.commands.add(new Command("#maxage", "50"));
				otherForm.commands.add(new Command("#maxage", "50"));
			}
			
		

			
			// Inherit from race/pose
			// Careful here since this stuff generally is definite instead of relative definitions
			List<Command> clist = new ArrayList<Command>();
			if(!otherForm.race.tags.contains("noinheritance"))
				clist.addAll(otherForm.race.unitcommands);
			if(!otherForm.pose.tags.contains("noinheritance"))
				clist.addAll(otherForm.pose.getCommands());
			for(Command c : clist)
			{
				if(n.secondShapeRacePoseCommands.contains(c.command))
				{	
					sf.commands.add(c);
					//handleCommand(commands, c);
				}
			}
			
			// Inheritance from filter bonus abilities
			for(Filter f : otherForm.appliedFilters)
			{
				boolean shape = false;
				for(Command c : f.getCommands())
					if(c.command.contains("shape"))
						shape = true;
				
				if(f.tags.contains("noinheritance") != shape)
					continue;
				
				
				// Add filters
				for(Command c : f.getCommands())
				{
			
					if(n.secondShapeNonMountCommands.contains(c.command) && !thisForm.tags.contains("mount"))
					{	
						sf.commands.add(c);
						//handleCommand(commands, c);
					}
					
					if(n.secondShapeMountCommands.contains(c.command) && thisForm.tags.contains("mount"))
					{
						sf.commands.add(c);
						//handleCommand(commands, c);
					}
			
				}
				
			}
			
	

	
			
			/*
			// Add filters
			for(Command c : f.commands)
			{
		
				if(n.secondShapeNonMountCommands.contains(c.command) && !thisForm.tags.contains("mount"))
				{	
					handleCommand(commands, c, nation);
				}
				
				if(n.secondShapeMountCommands.contains(c.command) && thisForm.tags.contains("mount"))
				{
					//System.out.println(f.name + " -> " + c.command + " " + c.argument);
					handleCommand(commands, c, nation);
				}
		
			}
				*/
		
		
		}
		
		if(sf.commands.size() > 0)
		{
			appliedFilters.add(sf);
		}

	}
	
	
	

	
	private void copyImage(String orig, String dest, int xoffset)
	{

		dest = "./mods/" + dest;
		BufferedImage image = null;
		try
		{
			image = ImageIO.read(new File("./", orig));
		}
		catch(IOException e)
		{
			System.out.println("CRITICAL FAILURE, IMAGE FILE " + orig + " CANNOT BE FOUND.");
			return;
		}
		
		BufferedImage base = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		Graphics g = base.getGraphics();
		g.drawImage(image, xoffset, 0, null);
		

		
		if(Generic.containsTag(thisForm.tags, "recolormask"))
		{
			BufferedImage mask = null;
			String file = Generic.getTagValue(thisForm.tags, "recolormask");
			try {
				mask = ImageIO.read(new File("./", file));
				Drawing.recolor(mask, this.color);

			} catch (IOException e) {
				System.out.println("CRITICAL FAILURE, IMAGE FILE " + file + " CANNOT BE FOUND.");
				return;
			}
			g.drawImage(mask, xoffset, 0, null);

			

		}
		
		try
		{
			Drawing.writeTGA(base, dest);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.out.println("Unable to write to " + dest + " from " + orig + "!");
		}
		
	}
	
	public void write(PrintWriter tw, String spritedir)
	{

		
		// Handle sprites
		String spr1path = "";
		boolean doShift = false;
		boolean greyscale = false;
		int greyscaleunits = 0;
		List<Command> ignore = new ArrayList<Command>();
		for(Command c : thisForm.commands)
		{
			// First sprite
			if(c.command.equals("#spr1"))
			{
				if(c.args.get(0).equals("greyscale"))
				{
					greyscale = true;
					
					
					if(c.args.size() > 1)
						greyscaleunits = Integer.parseInt(c.args.get(1));
					
			
					try {
						BufferedImage image = otherForm.render();
						Drawing.writeTGA(Drawing.greyscale(image, greyscaleunits), "./mods/" + spritedir + "/shapechange_" + id + "_a.tga");
					} catch (IOException e) {
						e.printStackTrace();
					}
					
				}
				else
				{
					spr1path = c.args.get(0);
					copyImage(spr1path, spritedir + "/shapechange_" + id + "_a.tga", 0);
				}
				ignore.add(c);
				
				this.commands.add(new Command(c.command, "\"" + spritedir + "/shapechange_" + id + "_a.tga" + "\""));
			}
			else if(c.command.equals("#spr2"))
			{
				if(c.args.get(0).equals("shift"))
				{
					doShift = true;
					ignore.add(c);
					this.commands.add(new Command(c.command, "\"" + spritedir + "/shapechange_" + id + "_b.tga" + "\""));
				}
				else
				{
					copyImage(c.args.get(0), spritedir + "/shapechange_" + id + "_b.tga", 0);
					ignore.add(c);
					this.commands.add(new Command(c.command, "\"" + spritedir + "/shapechange_" + id + "_b.tga" + "\""));
				}
			}
		}
		
		// To handle #spr2 even if #spr1 wasn't there yet:
		if(doShift && !spr1path.equals("") && !greyscale)
			copyImage(spr1path, spritedir + "/shapechange_" + id + "_b.tga", -5);
		else if(doShift && greyscale)
		{
			try {
				BufferedImage image = otherForm.render(-5);
				Drawing.writeTGA(Drawing.greyscale(image, greyscaleunits), "./mods/" + spritedir + "/shapechange_" + id + "_b.tga");
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}

		// Write the unit text
		if(otherForm != null)
			tw.println("--- Shapechange form for " + otherForm.getName());
		else
			tw.println("--- Special unit " + getName());
		
		tw.println("#newmonster " + id);


		List<Command> commands = getCommands();
		
		// Own non-gcost commands first due to #copystats
		for(Command c : commands)
		{
			if(!c.command.equals("#gcost"))
			{
				tw.println(c.command + " " + Generic.listToString(c.args));
			}
		
		}
		

		tw.println("#descr \"No description\"");		

		//tw.println("#maxage 1000");
		
		if(thisForm.keepname && otherForm != null)
			tw.println("#name \"" + otherForm.name + "\"");
		

		if(!shiftcommand.equals("") && !thisForm.tags.contains("nowayback") && otherForm != null)
		{
			tw.println(shiftcommand + " " + otherForm.id);
		}
		if(sacred)
			tw.println("#holy");
		
		if(gcost != 0)
			tw.println("#gcost " + gcost);

		
		tw.println("#end");
		tw.println("");	
	}
	

	
}
