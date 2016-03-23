package nationGen.diagnostics;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import nationGen.NationGen;
import nationGen.entities.Pose;
import nationGen.items.Item;

public class Spritechecker {
	public static void main(String[] args)
	{
		NationGen ng = null;
		try {
			ng = new NationGen();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		for(String str : ng.poses.keySet())
		{



			List<Pose> pl = ng.poses.get(str);
			for(Pose p : pl)
			{
				for(String slot : p.renderorder.split(" "))
				{
					if(p.getItems(slot) == null)
						continue;
					
					for(Item i : p.getItems(slot))
					{
						if(i.sprite.equals(""))
							continue;
						
						File f = new File("./", i.sprite);

						try {
							ImageIO.read(f);
						} catch (IOException e) {
							System.out.println("ERROR IN SPRITE DEFINITIONS!");
							System.out.println("Set " + str + ", pose " + p + ", item " + i + ", slot " + slot + " -> " + i.sprite + " not found.");
						}
						
						if(i.mask.equals("") || i.mask.equals("self"))
							continue;
		
						f = new File("./", i.mask);

						try {
							ImageIO.read(f);
						} catch (IOException e) {
							System.out.println("ERROR IN MASK DEFINITIONS!");
							System.out.println("Set " + str + ", pose " + p + ", item " + i + ", slot " + slot + " -> " + i.mask + " not found.");
						}

					}
				}
					
			}
		}
		System.out.println("-------------------------------");
		System.out.println("All files checked");

	}
}
