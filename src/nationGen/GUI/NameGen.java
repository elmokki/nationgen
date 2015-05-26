package nationGen.GUI;

import java.io.IOException;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.Race;
import nationGen.naming.NameGenerator;

public class NameGen {
	public static void main(String[] args) throws IOException
	{
		NationGen ng = new NationGen();
		NameGenerator nameg = new NameGenerator(ng);
		
		for(Race r : ng.races)
		{
			if(!Generic.containsTag(r.tags, "secondary"))
			{
				System.out.print(r.visiblename + ": ");
				for(int i = 0; i < 10; i++)
				{
					System.out.print(nameg.generateNationName(r, null));
					if(i < 9)
						System.out.print(", ");
				}
				System.out.println();
			}
		}
	}
}
