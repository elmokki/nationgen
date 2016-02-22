package nationGen.GUI;

import java.util.ArrayList;
import java.util.List;

import nationGen.NationGen;

public class ConsoleGUI {
	
	
	public ConsoleGUI(NationGen gen) {

	}

	public static void main(String[] args) throws Exception
	{
		
		NationGen nationGen = new NationGen();

		List<Integer> seeds = new ArrayList<Integer>();
		seeds.add(-421273726);
		
		//nationGen.settings.put("era", 2.0);
		nationGen.settings.put("drawPreview", 1.0);

		nationGen.generate(seeds);
		//nationGen.generate(1);
	}




}
