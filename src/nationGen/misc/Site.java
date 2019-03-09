package nationGen.misc;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import nationGen.entities.Filter;
import nationGen.units.Unit;




public class Site {
	public HashMap<Integer, Integer> gemMap = new HashMap<Integer, Integer>();
	public List<Unit> troops = new ArrayList<Unit>();
	public List<Unit> coms = new ArrayList<Unit>();
	public List<Command> othercommands = new ArrayList<Command>();
	public List<Filter> appliedfilters = new ArrayList<Filter>();

	public int id = -1;
	public String name = "UNNAMED";
	public int level = 0;
	
	public Site()
	{
		for(int i = 0; i < 8; i++)
			gemMap.put(i, 0);
	}
	
	public List<String> writeLines()
	{
		if(id == -1)
		{
			throw new IllegalStateException("Site ID is -1");
		}
//		if(name.equals("UNNAMED"))
//		{
//			throw new IllegalStateException("Site name is 'UNNAMED'");
//		}
		
		List<String> lines = new ArrayList<>();
		
		lines.add("#newsite " + id);
		lines.add("#level " + level);
		lines.add("#rarity 5");
		lines.add("#path " + getPath());
		lines.add("#name \"" + name + "\"");
		
		lines.addAll(writeFeatureLines());
		
		lines.add("#end");
		lines.add("");
		
		return lines;
	}
	
	private List<String> writeFeatureLines()
	{
		List<String> lines = new ArrayList<>();
		
		for(Unit u : coms)
			lines.add("#homecom " + u.id + " --- " + u.name);
		for(Unit u : troops)
			lines.add("#homemon " + u.id + " --- " + u.name);
		for(Command str : othercommands)
			lines.add(str.toString());
		for(Filter f : this.appliedfilters)
			for(Command str : f.commands)
				lines.add(str.toString());
			
		for (Entry<Integer, Integer> entry : gemMap.entrySet())
		{
			if(entry.getValue() > 0)
				lines.add("#gems " + entry.getKey() + " " + entry.getValue());
		}
		return lines;
	}
	
	
	public double[] getSitePathDistribution()
	{
        double[] paths = new double[9];

		for(Unit u : coms)
		{
			for(String tag : u.tags)
			{
				if(tag.startsWith("sitepath") && tag.split(" ").length == 3)
				{
					int path = Integer.parseInt(tag.split(" ")[1]);
					int power = Integer.parseInt(tag.split(" ")[2]);
					paths[path] = paths[path] + power;
				}
				
				int[] unitpaths = u.getMagicPicks();
				for(int i = 0; i < unitpaths.length; i++)
				{
				
					paths[i] += unitpaths[i];



				}
			}
			
			
		}
		
		
		for(Unit u : troops)
		{
			for(String tag : u.tags)
			{
				if(tag.startsWith("sitepath") && tag.split(" ").length == 3)
				{
					int path = Integer.parseInt(tag.split(" ")[1]);
					int power = Integer.parseInt(tag.split(" ")[2]);
					paths[path] = paths[path] + power;

				}
			}
				
		}
		
		for(Filter f : appliedfilters)
		{
			for(String tag : f.tags)
			{
				if(tag.startsWith("sitepath") && tag.split(" ").length == 3)
				{
					int path = Integer.parseInt(tag.split(" ")[1]);
					int power = Integer.parseInt(tag.split(" ")[2]);
					paths[path] = paths[path] + power;

				}
				
			}
			
			
		}
		
		Iterator<Entry<Integer, Integer>> itr = gemMap.entrySet().iterator();
		while(itr.hasNext())
		{
			Entry<Integer, Integer> entry = itr.next();
			if(entry.getValue() > 0)
			{
				paths[entry.getKey()] = paths[entry.getKey()] + entry.getValue() * 2;
			}
		}
		
		return paths;
	}
	
    /** 
     * Gets magic path for a site
     * @return
     */
    public int getPath()
    {
        double[] paths = getSitePathDistribution();

        double highestvalue = -1;
        int highest = -1;

        for(int i = 0; i < 9; i++)
        {
            if(paths[i] > highestvalue)
            {
                highestvalue = paths[i];
                highest = i;
            }
        }
        int path = 8;
        if(highest != -1 || paths[highest] == 0) 
            path = highest;

        // If there were no paths, it's a holy site.
        if(highestvalue == 0)
        	path = 8;
        
        return path;
    }
    
    /** 
     * Gets magic path for a site
     * @return
     */
    public int getSecondaryPath()
    {
        double[] paths = getSitePathDistribution();

        double highestvalue = -1;
        int highest = -1;

        for(int i = 0; i < 9; i++)
        {
            if(paths[i] > highestvalue)
            {
                highestvalue = paths[i];
                highest = i;
            }
        }
        int path = 8;
        if(highest != -1 && paths[highest] != 0) 
            path = highest;
        
        paths[highest] = 0;
        
        highestvalue = -1;
        int secondhighest = -1;

        for(int i = 0; i < 9; i++)
        {
            if(paths[i] > highestvalue)
            {
                highestvalue = paths[i];
                secondhighest = i;
            }
        }
        
        if(secondhighest != -1 && paths[secondhighest] != 0) 
            path = secondhighest;

        return path;
    }


}
