package nationGen.items;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import java.util.LinkedHashMap;
import java.util.List;

import javax.imageio.ImageIO;




import com.elmokki.Drawing;
import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.Entity;
import nationGen.entities.Filter;
import nationGen.misc.Command;

public class Item extends Filter {
	public String sprite = "";
	public String mask = "";
	public String id = "-1";
	public boolean armor = false;
	
	
	protected int offsetx = 0;
	protected int offsety = 0;
	
	public ArrayList<ItemDependency> dependencies = new ArrayList<ItemDependency>();
	//public LinkedHashMap<String, String> dependencies = new LinkedHashMap<String, String>();
	//public LinkedHashMap<String, String> typedependencies = new LinkedHashMap<String, String>();

	public List<Command> commands = new ArrayList<Command>();
	public String slot = "";
	public String set = "";
	public String renderslot = "";
	public int renderprio = 5;
	
	public Item(NationGen nationGen)
	{
		super(nationGen);
	}
	
	public int getOffsetX()
	{
		return offsetx;
	}
	
	public void setOffsetX(int x)
	{

		this.offsetx = x;
	}
	
	public int getOffsetY()
	{
		return offsety;
	}
	
	public void setOffsetY(int y)
	{


		this.offsety = y;
	}
	
	
	public CustomItem getCustomItemCopy()
	{
		CustomItem item = new CustomItem(nationGen);
		item.sprite = sprite;
		item.mask = mask;
		item.id = id;
		item.armor = armor;
		item.offsetx = offsetx;
		item.offsety = offsety;
		dependencies.addAll(dependencies);
		item.commands.addAll(commands);
		item.slot = slot;
		item.set = set;
		item.renderslot = renderslot;
		item.renderprio = renderprio;
		item.name = this.name;
		item.basechance = this.basechance;
		item.tags.addAll(tags);
		return item;
	}
	
	public Item getCopy()
	{

		return (Item) this.getCustomItemCopy();
	}
	
	public <E extends Entity> void handleOwnCommand(String str)
	{

		List<String> args = Generic.parseArgs(str);
		
		
		try
		{
		
		if(args.get(0).equals("#gameid"))
			this.id = args.get(1);
		else if(args.get(0).equals("#armor"))
			this.armor = true;
		else if(args.get(0).equals("#sprite"))
			this.sprite = args.get(1);
		else if(args.get(0).equals("#renderslot"))
			this.renderslot = args.get(1);
		else if(args.get(0).equals("#renderprio"))
			this.renderprio = Integer.parseInt(args.get(1));
		else if(args.get(0).equals("#recolormask") || args.get(0).equals("#mask"))
			this.mask = args.get(1);
		else if(args.get(0).equals("#offsetx"))
			this.offsetx = Integer.parseInt(args.get(1));
		else if(args.get(0).equals("#offsety"))
			this.offsety = Integer.parseInt(args.get(1));
		else if(args.get(0).equals("#needs"))
			this.dependencies.add(new ItemDependency(args.get(1), args.get(2), false, false));
		else if(args.get(0).equals("#needstype"))
		{
			this.dependencies.add(new ItemDependency(args.get(1), args.get(2), true, false));
		}
		else if(args.get(0).equals("#forceslot"))
			this.dependencies.add(new ItemDependency(args.get(1), args.get(2), false, true));
		else if(args.get(0).equals("#forceslottype"))
		{
			this.dependencies.add(new ItemDependency(args.get(1), args.get(2), true, true));
		}
		else if(args.get(0).equals("#command") || args.get(0).equals("#define"))
			this.commands.add(Command.parseCommand(args.get(1)));
		else
			super.handleOwnCommand(str);
		}
		catch(IndexOutOfBoundsException e)
		{
			System.out.println("WARNING: " + str + " has insufficient arguments (" + this.name + ")");
		}
	}
	
	public void render(Graphics g, boolean useoffsets, int offsetx, int offsety, Color color, int extraX) throws IOException
	{				
		Item i = this;
		if(i == null || i.sprite == null || i.sprite.equals(""))
			return;
		
		int xoff = i.offsetx + offsetx + extraX;
		int yoff = i.offsety + offsety;
		if(!useoffsets)
		{
			xoff = extraX;
			yoff = 0;
		}
		String path = "./";
		BufferedImage image = null;
		
		if(i != null)
		{
			
			// Draw image
			try
			{
				image = ImageIO.read(new File(path, i.sprite));
			}
			catch(IOException e)
			{
				System.out.println("CRITICAL FAILURE, IMAGE FILE " + i.sprite + " CANNOT BE FOUND.");
				return;
			}
			g.drawImage(image, xoff, yoff, null);


			drawRecolorMask(g, this, color, xoff, yoff);
			image = null;
		}
	}
	
	
	private void drawRecolorMask(Graphics g, Item i, Color c, int x, int y) throws IOException
	{
		if(!i.mask.equals(""))
		{
			if(i.mask.equals("self"))
				i.mask = i.sprite;
			
			BufferedImage image;
			BufferedImageOp colorizeFilter;
			BufferedImage targetImage = null;
			try
			{
				image = ImageIO.read(new File("./", i.mask));
				
	
				if(i.tags.contains("alternaterecolor"))
					colorizeFilter =  Drawing.createColorizeOp_alt(c);
				else
					colorizeFilter = Drawing.createColorizeOp(c);
				
				
				targetImage = colorizeFilter.filter(image, image);
			}
			catch(Exception e)
			{
				System.out.println("RECOLORMASK " + i.mask + " COULD NOT BE READ!");
			}
			
			g.drawImage(targetImage, x, y, null);
			targetImage = null;
			image = null;
		}
	}
	
	

	
}
