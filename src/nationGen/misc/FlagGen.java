package nationGen.misc;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;

import java.io.IOException;
import java.util.ArrayList;

import java.util.List;
import java.util.Random;


import javax.imageio.ImageIO;

import com.elmokki.Drawing;
import com.elmokki.Generic;

import nationGen.entities.Flag;
import nationGen.nation.Nation;

public class FlagGen {
	
	
	public List<Flag> top = new ArrayList<Flag>();
	public List<Flag> mid = new ArrayList<Flag>();
	public List<Flag> recolor = new ArrayList<Flag>();
	public List<Flag> border = new ArrayList<Flag>();
	public List<Flag> bases = new ArrayList<Flag>();

    ChanceIncHandler chandler;

	public FlagGen(Nation n)
	{
		chandler = new ChanceIncHandler(n);
		// Load data

		for(String tag : n.races.get(0).tags)
		{
			List<String> args = Generic.parseArgs(tag);
			if(args.get(0).equals("flagtop") && args.size() > 1)
				top.addAll(n.nationGen.flagparts.get(args.get(1)));
			if(args.get(0).equals("flagmid") && args.size() > 1)
				mid.addAll(n.nationGen.flagparts.get(args.get(1)));
			if(args.get(0).equals("flagrecolor") && args.size() > 1)
				recolor.addAll(n.nationGen.flagparts.get(args.get(1)));
			if(args.get(0).equals("flagborder") && args.size() > 1)
				border.addAll(n.nationGen.flagparts.get(args.get(1)));
			if(args.get(0).equals("flagbases") && args.size() > 1)
				bases.addAll(n.nationGen.flagparts.get(args.get(1)));
		}
		
		if(top.size() == 0)
			top.addAll(n.nationGen.flagparts.get("defaulttopicons"));
		if(mid.size() == 0)
			mid.addAll(n.nationGen.flagparts.get("defaultmidicons"));
		if(recolor.size() == 0)
			recolor.addAll(n.nationGen.flagparts.get("defaultrecolors"));
		if(border.size() == 0)
			border.addAll(n.nationGen.flagparts.get("defaultborders"));
		if(bases.size() == 0)
			bases.addAll(n.nationGen.flagparts.get("defaultbaseflags"));

	}
	
	
	public BufferedImage generateFlag(Nation n)
	{
		BufferedImage image = null;
		try
		{
			image = getFlag(n);
		}
		catch(Exception e)
		{
			System.out.println("Error creating flag: " + e.getMessage() + " - CHECK FILENAMES IN FLAG DEFINITIONS!");
			e.printStackTrace();
		}
		return image;
	}
	  
	   
	
	
	

		
	   private BufferedImage getFlag(Nation n) throws IOException
	   {
			Flag baseflag = Flag.getRandom(n.random, chandler.handleChanceIncs(bases));
			
			BufferedImage image = ImageIO.read(new File("./", baseflag.sprite));
			BufferedImage mask = ImageIO.read(new File("./", baseflag.recolormask));
			
			
			BufferedImage combined = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
			// paint both images, preserving the alpha channels
			Graphics g = combined.getGraphics();
			g.setColor(Color.black);
			g.fillRect(0, 0, image.getHeight(), image.getWidth());
			
			mask = Drawing.recolor(mask, n.colors[0]);
			g.drawImage(image, 0, 0, null);
			g.drawImage(mask, 0, 0, null);
			
			Random r = n.random;
			
			int features = 0;
			
			
			boolean hasmid = false;
			if(r.nextDouble() > 0.5)
			{
				if(drawIcon(combined, n, recolor, baseflag.name))
					features++;
			}
			
			// Border
			if(r.nextDouble() > 0.2 || features == 0)
			{
				drawIcon(combined, n, border, baseflag.name);
			}
			
			// Middle
			if(r.nextDouble() > 0.5 + features * 0.4)
			{
				if(drawIcon(combined, n, mid, baseflag.name))
				{
					features++;
					hasmid = true;
				}
			}
			
			// Shading
			if(!baseflag.shading.equals(""))
			{
				BufferedImage shading = ImageIO.read(new File("./", baseflag.shading));
				g.drawImage(shading, 0, 0, null);

			}
			
			// Top icon
			if(r.nextDouble() > 0.2 + features * 0.2 && !hasmid)
			{
				if(drawIcon(combined, n, top, baseflag.name))
				{
					features++;
				}
			}

			
			
			return combined;
	   }
	   
	   private boolean drawIcon(BufferedImage image, Nation n, List<Flag> stuff, String basename) throws IOException
	   {
		   
		   boolean isBorder = false;
		   if(stuff == border)
			   isBorder = true;
		   
			Graphics g = image.getGraphics();
			
			List<Flag> possible = new ArrayList<Flag>();
			for(Flag f : stuff)
			{
				if(f.allowed.size() == 0 || f.allowed.contains(basename))
					possible.add(f);
			}
			
			if(possible.size() == 0)
				return false;
			
	
			Flag part = Flag.getRandom(n.random, chandler.handleChanceIncs(possible));

			//System.out.println("FLAG PART: " + part.sprite + " / " + part.recolormask);

		   	BufferedImage topiconimg = ImageIO.read(new File(part.sprite));
			g.drawImage(topiconimg, 0, 0, null);
			
			// Mask for top icon
			BufferedImage iconmask;
			
			if(!part.recolormask.equals(""))
			{
				String maskfile = part.recolormask;
				if(maskfile.equals("self"))
					maskfile = part.sprite;
				
				iconmask = ImageIO.read(new File(maskfile));
				
				Color col = n.colors[1];
				if(n.random.nextDouble() > 0.5 && isBorder)
				{
					int red = Math.max(0, Math.min(col.getRed() - 30, n.colors[0].getRed() - 30));
					int green = Math.max(0, Math.min(col.getGreen() - 30, n.colors[0].getGreen() - 30));
					int blue = Math.max(0, Math.min(col.getBlue() - 30, n.colors[0].getBlue() - 30));
					col = new Color(red, green, blue);
				}
				else if(n.random.nextDouble() > 0.75)
					col = n.colors[2];
				
				iconmask = Drawing.recolor(iconmask, col);
				g.drawImage(iconmask, 0, 0, null);
				
			}
			return true;
	   }
	 
}
