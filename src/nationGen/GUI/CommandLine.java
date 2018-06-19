package nationGen.GUI;

import java.util.ArrayList;
import java.util.List;

import com.elmokki.Generic;

import nationGen.NationGen;

public class CommandLine {
	public CommandLine (String[] args) throws Exception
	{
		Long seed = null;
		int amount = 0;
		List<Long> seeds = new ArrayList<Long>();
		int settings = 0;
		boolean nextseed = false;
		boolean nextamount = false;
		boolean nextsettings = false;
		boolean nextnseed = false;
		boolean nextname = false;
		String modname = null;
		
		for(String str : args)
		{
			if(str.startsWith("-") && !Generic.isNumeric(str))
			{
				nextnseed = false;
				nextseed = false;
				nextamount = false;
				nextsettings = false;
				nextname = false;
			}
			if(str.equals("-seed"))
			{
				nextseed = true;
			}
			else if(str.equals("-amount"))
			{
				nextamount = true;
			}
			else if(str.equals("-settings"))
			{
				nextsettings = true;
			}
			else if(str.equals("-modname"))
			{
				nextname = true;
			}
			else if(str.equals("-nationseeds"))
			{
				nextnseed = true;
			}
			else if(Generic.isNumeric(str))
			{
				if(nextseed)
					seed = Long.parseLong(str);
				if(nextamount)
					amount = Integer.parseInt(str);
				if(nextsettings)
					settings = Integer.parseInt(str);
				if(nextnseed)
					seeds.add(Long.parseLong(str));
					
			}
			else if(nextname)
			{
				if(modname == null)
					modname = str;
				else
					modname = modname + " " + str;
			}
			else if(!str.equals("-commandline"))
			{
				System.out.println("Invalid argument: " + str);
				return;
			}
				
		}
		
        System.out.println("Dominions 4 NationGen version " + NationGen.version + " (" + NationGen.date + ")");
        System.out.println("------------------------------------------------------------------");
        NationGen ng = new NationGen();
        
        if(settings != 0)
        {
        	ng.settings.setSettingInteger(settings);
        	System.out.println("Settings were set to " + settings);
        }
        if(amount != 0 && seeds.size() == 0)
        {
        	System.out.println("Nation amount was set to " + amount);
        }
        if(seed != null && seeds.size() == 0)
        {
        	System.out.println("Seed was set to " + seed);
        }
        if(seeds.size() > 0)
        {
        	System.out.println(seeds.size() + " nation seeds were inputed.");
        }
        if(modname != null)
        {
        	System.out.println("Mod name was set to " + modname);
            ng.modname = modname;
        }
        	
        if(amount < 1)
        	amount = 1;
        
        
        if(seeds.size() > 0)
        	ng.generate(seeds);
        else if(seed != null)
        	ng.generate(amount, seed);
        else 
        	ng.generate(amount);
        
        
	}
}
