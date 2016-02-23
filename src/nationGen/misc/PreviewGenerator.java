package nationGen.misc;



import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import com.elmokki.Generic;

import nationGen.nation.Nation;
import nationGen.units.Unit;


public class PreviewGenerator {
	public void savePreview(Nation n, String file) throws IOException
	{
		int baseX = 64;
		int baseY = 64;
		
		int maxX = 10 - 1;
		int maxY = 1;
		
		int neededBlocks = 0;
		int totalNeededBlocks = 0;
		int troopLines = 0;

		List<Unit> troops = n.generateUnitList("ranged");
		troops.addAll(n.generateUnitList("infantry"));
		troops.addAll(n.generateUnitList("mounted"));
		troops.addAll(n.generateUnitList("chariot"));
		troops.addAll(n.generateUnitList("sacred"));
		troops.addAll(n.generateUnitList("montagsacreds"));
		troops.addAll(n.generateUnitList("montagtroops"));


		for(Unit u : troops)
		{
			int x = getSize(u, true);
			int y = getSize(u, false);
			
			x = (int)Math.ceil((double)x / 64);
			y = (int)Math.ceil((double)y / 64);
			
			neededBlocks += x*y;
		}
		
		troopLines = (int) Math.ceil((double)neededBlocks / ((double)maxX + 1));
		maxY += (int) Math.ceil((double)neededBlocks / ((double)maxX + 1));
		totalNeededBlocks += neededBlocks;
		neededBlocks = 0;
		
		for(Unit u : n.generateComList())
		{
			int x = getSize(u, true);
			int y = getSize(u, false);
			
			x = (int)Math.ceil((double)x / 64);
			y = (int)Math.ceil((double)y / 64);
			
			neededBlocks += x*y;
		}
		totalNeededBlocks += neededBlocks;


		
		maxY += (int) Math.ceil((double)neededBlocks / ((double)maxX + 1));
		neededBlocks = 0;
		maxY++;
		

		
		BufferedImage img = new BufferedImage((1 + maxX)*baseX, (1 + maxY)*baseY, BufferedImage.TYPE_3BYTE_BGR);
		Graphics g = img.getGraphics();
		g.setColor(Color.black);
		g.fillRect(0, 0, maxX*baseX, maxY*baseY);
		
		
		int[][] map = new int[maxY + 1][maxX + 1];



		// Nation text and troop header
		for(int x = 0; x < maxX; x++)
		{
			for(int y = 0; y < 1; y++)
			{
				map[y][x] = 1;
			}
		}
	
		FlagGen fg = new FlagGen(n);
		BufferedImage flag = fg.generateFlag(n);

		g.setColor(new Color(0, 0, 0));
		g.fillRect(0, 0, 256, 64);
		g.setColor(Color.white);
		
		Font f = g.getFont();
		Font d = f.deriveFont(32f);
		g.setFont(d);
		//g.setColor(c);
		g.drawString(n.name, 96, 48);
		g.setFont(f);
		g.setColor(Color.white);
		g.drawImage(flag, 16, 0, null);
			
		drawList(map, maxX, maxY, troops, g);
		
		/*
		for(int y = 0; y < maxY; y++)
		{
			if(map[y][0] == 1)
				for(int x = 0; x < maxX; x++)
				{
					map[y][x] = 1;
			
				}
			
		}
		*/
		drawList(map, maxX, maxY, n.generateComList(), g);
		
		
		javax.imageio.ImageIO.write(img, "png", new File(file));
	}
	
	private void drawList(int[][] map, int maxX, int maxY, List<Unit> troops, Graphics g) throws IOException
	{
		
		for(Unit u : troops)
		{
			boolean foundFit = false;
			for(int y = 0; y < maxY; y++)
			{

				for(int x = 0; x < maxX; x++)
				{

					if(map[y][x] == 0)
					{
						int unitX = getSize(u, true);
						int unitY = getSize(u, false);
						
						unitX = (int)Math.ceil((double)unitX / 64);
						unitY = (int)Math.ceil((double)unitY / 64);
						

						boolean fit = true;
				
	
						for(int tX = x; tX < x + unitX; tX++)
						{
							for(int tY = y; tY < y + unitY; tY++)
							{
								if(tY > maxY || tX > maxX)
								{
									fit = false;
									break;
								}
								else if(map[tY][tX] != 0)
								{
									fit = false;
									break;
								}
								else
								{
								}
							}
						}
						
						if(!fit)
						{
							
						}
						else
						{
							for(int tX = x; tX < x + unitX; tX++)
							{
								for(int tY = y; tY < y + unitY; tY++)
								{
									map[tY][tX] = 1;

								}
							}
							
							g.drawImage(u.render(), x*64, y*64, null);
							foundFit = true;
							break;
						}
						
					}
					else
					{

					}
					
				}
				if(foundFit)
					break;
			}
		}
	}
	
	private int getSize(Unit u, boolean width) throws IOException
	{
		BufferedImage base;
		try
		{ 
			base = ImageIO.read(new File("./", u.slotmap.get("basesprite").sprite));
		}
		catch(Exception e)
		{
			base = new BufferedImage(64, 64, BufferedImage.TYPE_3BYTE_BGR);
		}

		// create the new image, canvas size is the max of both image sizes
		int w = Math.max(64, base.getWidth());
		int h = Math.max(64, base.getHeight());
		

		if(u.slotmap.get("mount") != null)
		{
			base = ImageIO.read(new File("./", u.slotmap.get("mount").sprite));
			w = Math.max(64, base.getWidth());
			h = Math.max(64, base.getHeight());
		}
		
		if(width)
			return w;
		else
			return h;
	}
}
