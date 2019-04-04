package nationGen.misc;



import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import nationGen.nation.Nation;
import nationGen.units.Unit;


public class PreviewGenerator {
	public void savePreview(Nation n, String file)
	{
		int baseX = 64;
		int baseY = 64;
		
		int maxX = 10 - 1;
		int maxY = 1;
		
		int neededBlocks = 0;
		//int totalNeededBlocks = 0;
		//int troopLines = 0;

		List<Unit> troops = n.generateUnitList("ranged");
		troops.addAll(n.generateUnitList("infantry"));
		troops.addAll(n.generateUnitList("mounted"));
		troops.addAll(n.generateUnitList("chariot"));
		troops.addAll(n.generateUnitList("sacred"));
		troops.addAll(n.generateUnitList("montagsacreds"));
		troops.addAll(n.generateUnitList("montagtroops"));


		for(Unit u : troops)
		{
			Dimension d = u.getSpriteDimensions();
			
			int x = (int)Math.ceil(d.getWidth() / 64);
			int y = (int)Math.ceil(d.getHeight() / 64);
			
			neededBlocks += x*y;
		}
		
		//troopLines = (int) Math.ceil((double)neededBlocks / ((double)maxX + 1));
		maxY += (int) Math.ceil((double)neededBlocks / ((double)maxX + 1));
		//totalNeededBlocks += neededBlocks;
		neededBlocks = 0;
		
		for(Unit u : n.generateComList())
		{
			Dimension d = u.getSpriteDimensions();
			
			int x = (int)Math.ceil(d.getWidth() / 64);
			int y = (int)Math.ceil(d.getHeight() / 64);
			
			neededBlocks += x*y;
		}
		//totalNeededBlocks += neededBlocks;


		
		maxY += (int) Math.ceil((double)neededBlocks / ((double)maxX + 1));
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
	
		BufferedImage flag = n.flag;

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
		g.drawImage(flag, 16, 0, 64, 64, null);
			
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
		
		FileUtil.writePng(img, file);
	}
	
	private void drawList(int[][] map, int maxX, int maxY, List<Unit> troops, Graphics g)
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
						Dimension d = u.getSpriteDimensions();
						
						int unitX = (int)Math.ceil(d.getWidth() / 64);
						int unitY = (int)Math.ceil(d.getHeight() / 64);
						

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
}
