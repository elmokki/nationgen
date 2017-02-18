package nationGen.items;


import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.elmokki.Generic;



import nationGen.NationGen;
import nationGen.entities.Entity;
import nationGen.entities.MagicItem;



public class CustomItem extends Item {

	public LinkedHashMap<String, String> values = new LinkedHashMap<String, String>();
	public Item olditem = null;
	
	public CustomItem getCopy()
	{
		CustomItem item = this.getCustomItemCopy();
		item.olditem = this.olditem;
		for(String str : values.keySet())
			item.values.put(str, values.get(str));
		
		return item;
	}
	
	public MagicItem magicItem = null;
	
	public CustomItem(NationGen nationGen) {
		super(nationGen);
		this.values.put("rcost", "0");
		this.values.put("att", "0");
		this.values.put("def", "0");
		this.values.put("len", "0");
		this.values.put("dmg", "0");
	}
	
	
	@Override
	public <Entity> void handleOwnCommand(String str)
	{
		List<String> args = Generic.parseArgs(str);
		if(args.get(0).equals("#command") && args.size() > 1)
		{	
			List<String> newargs = Generic.parseArgs(args.get(1), "'");
			String comcommand = newargs.get(0);
			String comarg = "";
			for(int i = 1; i < newargs.size(); i++)
				comarg = comarg + newargs.get(i) + " ";
			
			comarg = comarg.trim();
			
			if(comcommand.equals("#name"))
				comarg = "\"" + comarg + "\"";

			this.values.put(comcommand.substring(1), comarg);
		}
		else
			super.handleOwnCommand(str);

	}
	
	
	public LinkedHashMap<String, String> getHashMap()
	{
		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		map.put("id#", id + "");
		map.put("#att", "1");
		map.put("shots", "0");
		map.put("rng", "0");
		map.put("att", "0");
		map.put("def", "0");
		map.put("lgt", "0");
		map.put("dmg", "0");
		map.put("2h", "0");

		for(String str : values.keySet())
		{
			String arg = values.get(str);
			if(arg == null)
				continue;
			

			if(str.equals("secondaryeffectalways"))
			{
				str = "aeff#";
			}
			if(str.equals("secondaryeffect"))
			{
				str = "eff#";
			}
			
			if(str.equals("twohanded"))
			{
				str = "2h";
				arg = "1";
			}
			
			if(str.equals("charge"))
			{
				arg = "Charge";
			}
			
			if(str.equals("bonus"))
			{
				arg = "Bonus";
			}
		
			if(str.equals("dt_cap"))
			{
				arg = "Max dmg 1";
			}
			
			if(str.equals("magic"))
			{
				arg = "Magic";
				
			}
			
			if(str.equals("ammo"))
			{
				str = "shots";
			}
			
			if(str.equals("armorpiercing"))
			{
				str = "ap";
				arg = "ap";
			}
			
			if(str.equals("armornegating"))
			{
				str = "an";
				arg = "an";
			}
	
			if(str.equals("bonus"))
			{
				arg = "Bonus";
			}
			
			if(str.equals("range"))
			{
				str = "rng";
			}
			
			if(str.equals("len"))
			{
				str = "lgt";
			}
			
			if(str.equals("nratt"))
			{
				str = "#att";
			}
			
			if(str.equals("rcost"))
			{
				str = "res";
			}
			
			
			if(str.equals("name") && this.armor)
			{
				str = "armorname";
				arg = arg.replaceAll("\"", "");
			}
			else if(str.equals("name") && !this.armor)
			{
				str = "weapon_name";	
				arg = arg.replaceAll("\"", "");
			}
			map.put(str, arg);
		}
		
		if(map.get("2h") == null)
			map.put("2h", "0");
		
		
		
		return map;
	}
	
	
	public void write(PrintWriter tw)
	{

		if(armor)
			tw.println("#newarmor " + id);
		else
			tw.println("#newweapon " + id);
		
		List<String> lines = new ArrayList<String>();
		
		for(String command : values.keySet())
		{
			if(command.equals("name"))
			{
				tw.println("#name " + values.get("name"));
			}
		}
		
		
		for(String command : values.keySet())
		{
			String arg = "";
			
			if(command.equals("name"))
				continue;
			
			if(values.get(command) != null)
			{
				arg = " " + values.get(command);
			}

			
			lines.add("#" + command + arg);
				
		}
			
		for(String str : lines)
		{			
			tw.println(str);
		}
		
		
		tw.println("#end");
		tw.println();
	}

}
