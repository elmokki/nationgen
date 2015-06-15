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
		seeds.add(1306265258);
		
		//nationGen.generate(seeds);
		nationGen.generate(100, 45456);
	}




}
