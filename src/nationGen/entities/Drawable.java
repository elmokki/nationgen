package nationGen.entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.List;

import com.elmokki.Drawing;
import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.misc.FileUtil;


public class Drawable extends Filter {

	public String sprite = "";
	public String mask = "";
	protected int offsetx = 0;
	protected int offsety = 0;
	public String renderslot = "";
	public int renderprio = 5;
	
	
	public Drawable(NationGen nationGen) {
		super(nationGen);
	}
	
	
    @Override
	public <Entity> void handleOwnCommand(String str)
	{

		List<String> args = Generic.parseArgs(str);
		
		
		try
		{
		

		if(args.get(0).equals("#sprite"))
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
		else
			super.handleOwnCommand(str);
		}
		catch(IndexOutOfBoundsException e)
		{
			System.out.println("WARNING: " + str + " has insufficient arguments (" + this.name + ")");
		}
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
	
	
	public void render(Graphics g, Color c)
	{
		render(g, false, 0, 0, c, 0);
	}
	
	
	private BufferedImage convertToAlpha(BufferedImage image)
	{
		image = Drawing.convertImageToRGBA(image);
		image = Drawing.blackToTransparent(image);
	    image = Drawing.purpleToShadow(image);
	    return image;
	}
	
	public void render(Graphics g, boolean useoffsets, int offsetx, int offsety, Color color, int extraX)
	{				
		Drawable i = this;
		if(i == null || i.sprite == null || i.sprite.equals(""))
			return;
		
		int xoff = i.offsetx + offsetx + extraX;
		int yoff = i.offsety + offsety;
		if(!useoffsets)
		{
			xoff = extraX;
			yoff = 0;
		}
		
		if(i != null)
		{
			
			BufferedImage image = FileUtil.readImage(i.sprite);
			
			// Handle "black_to_alpha"
			if(this.tags.contains("convert_to_alpha"))
				image = convertToAlpha(image);
			
			g.drawImage(image, xoff, yoff, null);
			drawRecolorMask(g, this, color, xoff, yoff);
		}
	}
	
	
	private void drawRecolorMask(Graphics g, Drawable i, Color c, int x, int y)
	{
		if(!i.mask.equals(""))
		{
			if(i.mask.equals("self"))
				i.mask = i.sprite;
			
			BufferedImage image = FileUtil.readImage(i.mask);
			BufferedImageOp colorizeFilter;
	
			// Handle "black_to_alpha"
			if(this.tags.contains("convert_to_alpha"))
				image = convertToAlpha(image);

			if(i.tags.contains("alternaterecolor"))
				colorizeFilter =  Drawing.createColorizeOp_alt(c);
			else
				colorizeFilter = Drawing.createColorizeOp(c);
			
			
			BufferedImage targetImage = colorizeFilter.filter(image, image);
			
			
			g.drawImage(targetImage, x, y, null);
		}
	}
	
}
