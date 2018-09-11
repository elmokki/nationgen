package nationGen.GUI;

import java.util.ArrayList;
import java.util.List;

import nationGen.NationGen;
import nationGen.Settings.SettingsType;

public class ConsoleGUI {
	
	
	public ConsoleGUI(NationGen gen) {

	}

	public static void main(String[] args) throws Exception
	{
		
		NationGen nationGen = new NationGen();

		List<Long> seeds = new ArrayList<Long>();
		seeds.add(-216802392L);
		
		//nationGen.settings.put("era", 2.0);
		nationGen.settings.put(SettingsType.drawPreview, 1.0);
		nationGen.settings.put(SettingsType.debug, 1.0);
		nationGen.generate(seeds);
		//nationGen.generate(10, 403);
	}




}
