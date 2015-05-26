package com.elmokki;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.awt.image.LookupOp;
import java.awt.image.LookupTable;
import java.awt.image.RescaleOp;
import java.awt.image.ShortLookupTable;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;




public class Drawing {
	

	public static BufferedImage greyscale(BufferedImage image, int units)
	{
	
		
		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
		ColorConvertOp op = new ColorConvertOp(cs, null);
		BufferedImage newimage = op.filter(image, null);
		BufferedImage n = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics g = n.getGraphics();
		g.drawImage(newimage, 0, 0, null);
		

		for(int x = 0; x < image.getWidth(); x++)
			for(int y = 0; y < image.getHeight(); y++)
			{
				  int clr =  image.getRGB(x,y); 
				  int  red   = (clr & 0x00ff0000) >> 16;
				  int  green = (clr & 0x0000ff00) >> 8;
				  int  blue  =  clr & 0x000000ff;
	
				  if(red > 247 && blue > 247 && green == 0)
				  {
				
					  n.setRGB(x, y, clr);	
					  
				  }
			}
		

		
		return darken(n, units);
	}
	
	public static BufferedImage recolor(BufferedImage image, Color c) throws IOException
	{
	
		BufferedImageOp colorizeFilter = createColorizeOp(c);
		BufferedImage targetImage = colorizeFilter.filter(image, image);
		return targetImage;
		
	}
	
	
	public static BufferedImage recolor_alternate(BufferedImage image, Color c) throws IOException
	{
	
		BufferedImageOp colorizeFilter = createColorizeOp(c);
		BufferedImage targetImage = colorizeFilter.filter(image, image);
		return targetImage;
		
	}
	
	
	public static BufferedImage removeBlack(BufferedImage image)
	{
		BufferedImageOp colorizeFilter = createRemoveBlackOp();
		BufferedImage targetImage = colorizeFilter.filter(image, image);
		return targetImage;
	}
	
	public static BufferedImage darken(BufferedImage image, int units)
	{
		float un = (float)units;

		float[] factors = new float[] {
			    1.0f, 1.0f, 1.0f, 1.0f
			};
		float[] offsets = new float[] {
			    un, un, un, 0.0f
			};
			
		BufferedImage dstImage = null;
		RescaleOp op = new RescaleOp(factors, offsets, null);
		dstImage = op.filter(image, null);

		for(int x = 0; x < image.getWidth(); x++)
			for(int y = 0; y < image.getHeight(); y++)
			{
				  int clr =  image.getRGB(x,y); 
				  int  red   = (clr & 0x00ff0000) >> 16;
				  int  green = (clr & 0x0000ff00) >> 8;
				  int  blue  =  clr & 0x000000ff;
				
				  if(red > 247 && blue > 247 && green == 0)
					  dstImage.setRGB(x, y, clr);		  
				  
				  if(red == 0 && blue == 0 && green == 0)
					  dstImage.setRGB(x, y, Color.BLACK.getRGB());	
			}
		

		
		return dstImage;
	}
	
	public static LookupOp createColorizeOp(Color c) {
	    short[] alpha = new short[256];
	    short[] red = new short[256];
	    short[] green = new short[256];
	    short[] blue = new short[256];

	    short R1 = (short) c.getRed();
	    short B1 = (short) c.getBlue();
	    short G1 = (short) c.getGreen();
	    

	    for (short i = 0; i < 256; i++) {
	        alpha[i] = i;
	        red[i] = (short) ((double)((double)i/(double)255) * (double)R1);
	        green[i] = (short) ((double)((double)i/(double)255) * (double)B1);
	        blue[i] = (short) ((double)((double)i/(double)255) * (double)G1);
	    }

	    short[][] data = new short[][] {
	            red, green, blue, alpha
	    };

	    LookupTable lookupTable = new ShortLookupTable(0, data);
	    return new LookupOp(lookupTable, null);
	}

	
	public static short calcColor(short i, short R1)
	{
		
        double diff = ((double)i / 255 - 0.5) * 2; // -1 to 1
        
        
        double max = 255 - R1; // 200 -> 55, 100 -> 155
        if(diff < 0)
        {
        	max = R1;
        }

        

		return (short)((max * diff) + R1);
	}
	
	
	public static LookupOp createColorizeOp_alt(Color c) {
	    short[] alpha = new short[256];
	    short[] red = new short[256];
	    short[] green = new short[256];
	    short[] blue = new short[256];

	    short R1 = (short) c.getRed();
	    short B1 = (short) c.getBlue();
	    short G1 = (short) c.getGreen();
	    

	    for (short i = 0; i < 256; i++) {
	        alpha[i] = i;
	        
	        red[i] = calcColor(i, R1);
	        green[i] = calcColor(i, G1);
	        blue[i] = calcColor(i, B1);

	        
	    }	

	    short[][] data = new short[][] {
	            red, green, blue, alpha
	    };

	    LookupTable lookupTable = new ShortLookupTable(0, data);
	    return new LookupOp(lookupTable, null);
	}
	
	private static short limitRange(int c)
	{
		if(c > 255)
			c = 255;
		if(c < 1)
			c = 1;
		
		return (short)c;
	}
	
	

	private static LookupOp createRemoveBlackOp() {
	    short[] alpha = new short[256];
	    short[] red = new short[256];
	    short[] green = new short[256];
	    short[] blue = new short[256];
	    
	    for (short i = 0; i < 256; i++) {
	        alpha[i] = i;
	        red[i] = i;
	        green[i] = i;
	        blue[i] = i;
	        
	        if(red[i] < 15)
	        	red[i] = 15;
	        if(green[i] < 15)
	        	green[i] = 15;
	        if(blue[i] < 15)
	        	blue[i] = 15;

	    }

	    short[][] data = new short[][] {
	            red, green, blue, alpha
	    };

	    LookupTable lookupTable = new ShortLookupTable(0, data);
	    return new LookupOp(lookupTable, null);
	}
	
	
	private static short flipEndian(short signedShort) {
		int input = signedShort & 0xFFFF;
		return (short) (input << 8 | (input & 0xFF00) >>> 8);
	}
	
	public static void writeTGA(BufferedImage image, String output) throws IOException
	{
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(output))));
		

		// ID Length
		out.writeByte((byte) 0);

		// Color Map
		out.writeByte((byte) 0);

		// Image Type
		out.writeByte((byte) 2);

		// Color Map - Ignored
		out.writeShort(flipEndian((short) 0));
		out.writeShort(flipEndian((short) 0));
		out.writeByte((byte) 0);

		// X, Y Offset
		out.writeShort(flipEndian((short) 0));
		out.writeShort(flipEndian((short) 0));

		// Width, Height, Depth
		out.writeShort(flipEndian((short) image.getWidth()));
		out.writeShort(flipEndian((short) image.getHeight()));

		out.writeByte((byte) 24);
		out.writeByte((byte) 0);
	
		

		// Write out the image data
		Color c;


		for (int y = image.getHeight()-1; y >= 0; y--) {
			
			for (int x = 0; x < image.getWidth(); x++) {
				
				c = new Color(image.getRGB(x, y));
				out.writeByte((byte) (c.getBlue()));
				out.writeByte((byte) (c.getGreen()));
				out.writeByte((byte) (c.getRed()));
			}
			
			
		}

		out.close();
		
	}
	
	
	public static Color getColor(Random random)
	{
		int red = random.nextInt(255) + 1;
		int blue = random.nextInt(255) + 1;
		int green = random.nextInt(255) + 1;
		
		while((red + blue + green) > 704 || (red + blue + green) < 96)
		{
			red = random.nextInt(256);
			blue = random.nextInt(256);
			green = random.nextInt(256);
		}
		
		return new Color(red, green, blue);
	}
}
