package nationGen.unrelated;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.elmokki.Dom3DB;

public class SiteRarity {
	public static void main(String[] args) throws IOException
	{
		FileWriter fstream = new FileWriter("derp.txt");
		PrintWriter tw = new PrintWriter(fstream);
		
		Dom3DB sites = new Dom3DB("sites.csv");
		
		for(int i = 1; i < 1027; i++)
		{
			if(sites.GetInteger(i + "", "frq") == 0 && sites.GetInteger(i + "", "lvl") < 2)
			{
				int frq = sites.GetInteger(i + "", "frq") + 1;
		
				tw.println("#selectsite " + i);
				tw.println("#rarity " + frq);
				
				String[] paths = {"F", "A", "W", "E", "S", "D", "N", "B"};
				for(int j = 0; j < 8; j++)
				{
					String path = paths[j];
					if(sites.GetInteger(i + "", path) > 0)
						tw.println("#gems " + j + " " + sites.GetInteger(i + "", path));
				}
			
				if(sites.GetInteger(i + "", "gold") > 0)
					tw.println("#gold " + sites.GetInteger(i + "", "gold"));
				if(sites.GetInteger(i + "", "res") > 0)
					tw.println("#res " + sites.GetInteger(i + "", "res"));
				if(sites.GetInteger(i + "", "sup") > 0)
					tw.println("#supply " + sites.GetInteger(i + "", "sup"));
				if(sites.GetInteger(i + "", "exp") > 0)
					tw.println("#xp " + sites.GetInteger(i + "", "exp"));

				
				tw.println("#end");
				tw.println();
		
			}
		}
		
		for(int i = 1; i < 1027; i++)
		{
			if(sites.GetInteger(i + "", "frq") == 11)
			{
				int frq = 5;
		
				tw.println("#selectsite " + i);
				tw.println("#rarity " + frq);
				tw.println("#end");
				tw.println();
		
			}
		}
		
		tw.flush();
		tw.close();
	}
}
