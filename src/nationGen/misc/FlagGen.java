package nationGen.misc;

import nationGen.NationGenAssets;
import nationGen.entities.Flag;
import nationGen.nation.Nation;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FlagGen {
	
	
	public List<Flag> top = new ArrayList<>();
	public List<Flag> poles = new ArrayList<>();
	public List<Flag> border = new ArrayList<>();
	public List<Flag> bases = new ArrayList<>();

    private ChanceIncHandler chandler;

	public FlagGen(Nation n, NationGenAssets assets)
	{
		chandler = new ChanceIncHandler(n);
		// Load data
		n.races.get(0).tags.getAllStrings("flagtops").forEach(set -> top.addAll(assets.flagparts.get(set)));
		n.races.get(0).tags.getAllStrings("flagborders").forEach(set -> border.addAll(assets.flagparts.get(set)));
		n.races.get(0).tags.getAllStrings("flagbases").forEach(set -> bases.addAll(assets.flagparts.get(set)));
		n.races.get(0).tags.getAllStrings("flagpoles").forEach(set -> poles.addAll(assets.flagparts.get(set)));
		
		if(top.size() == 0)
			top.addAll(assets.flagparts.get("topicons"));
		if(poles.size() == 0)
			poles.addAll(assets.flagparts.get("poles"));
		if(border.size() == 0)
			border.addAll(assets.flagparts.get("borders"));
		if(bases.size() == 0)
			bases.addAll(assets.flagparts.get("baseflags"));

	}
	
	
	public BufferedImage generateFlag(Nation n)
	{
		
		Flag basepole = chandler.handleChanceIncs(poles).getRandom(n.random);
		Flag baseflag = chandler.handleChanceIncs(bases).getRandom(n.random);
	
		
		BufferedImage combined = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
		
		// paint both images, preserving the alpha channels
		Graphics g = combined.getGraphics();
		g.setColor(new Color(0,0,0,0));
		g.fillRect(0, 0, combined.getHeight(), combined.getWidth());
		
	
		basepole.render(g, n.colors[0]);
		baseflag.render(g, n.colors[0]);
	
		Random r = n.random;
		
		// Always add border
		drawIcon(combined, n, border, baseflag.name);
	
		// Always add top icon until there's something interesting to compensate
		if(r.nextDouble() > 0)
		{
			drawIcon(combined, n, top, baseflag.name);
			
		}
	
		
		
		return combined;
	}
	
	private boolean drawIcon(BufferedImage image, Nation n, List<Flag> stuff, String basename)
	{
	   
	   boolean isBorder = false;
	   if(stuff == border)
		   isBorder = true;
	   
		Graphics g = image.getGraphics();
		
		List<Flag> possible = new ArrayList<>();
		for(Flag f : stuff)
		{
			if(f.allowed.size() == 0 || f.allowed.contains(basename))
				possible.add(f);
		}
		
		if(possible.size() == 0)
			return false;
		
	
		Flag part = chandler.handleChanceIncs(possible).getRandom(n.random);
	
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
		
		part.render(g, col);
		
	
		return true;
	}
	 
}
