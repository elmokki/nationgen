package nationGen.diagnostics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.Race;
import nationGen.naming.NameGenerator;

public class NameTester {
	
	public static void main(String[] args)
	{
		
		NationGen ng = null;

		try {
			ng = new NationGen();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		NameGenerator ngen = new NameGenerator(ng);

		Race r = null;
		for(Race r2 : ng.races)
		{
			if(r2.name.equals("Van"))
				r = r2;
		}
		
		List<String> names = new ArrayList<String>();
		for(int i = 0; i < 50; i++)
		{
	
				names.add(ngen.generateNationName(r, null));
		}
		
		if(names.size() > 0)
			System.out.println(Generic.listToString(names, ","));
		
		
	}
	
	
}
