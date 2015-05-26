package nationGen.misc;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import nationGen.units.Unit;




public class Site {
	public HashMap<Integer, Integer> gemMap = new HashMap<Integer, Integer>();
	public List<Unit> troops = new ArrayList<Unit>();
	public List<Unit> coms = new ArrayList<Unit>();
	public List<String> othercommands = new ArrayList<String>();
	public int id = -1;
	public String name = "UNNAMED";
	public int level = 0;
	
	public Site()
	{
		for(int i = 0; i < 8; i++)
			gemMap.put(i, 0);
	}
	
	public void write(PrintWriter tw)
	{
		if(id == -1)
		{
			System.out.println("TERRIBLE SITE ID! PROGRAMMING ERROR! ARRRRGH! EVERYTHING FALLS APART!");
			return;
		}
		else if(name.equals("UNNAMED"))
		{
			//System.out.println("TERRIBLE SITE NAME! PROGRAMMING ERROR! ARRRRGH! EVERYTHING DOES NOT FALL APART THOUGH!");
		}
		
		tw.println("#newsite " + id);
		tw.println("#level " + level);
		tw.println("#rarity 5");
		tw.println("#path " + getPath()); 
		tw.println("#name \"" + name + "\"");
		
		writeFeatures(tw);
		
		tw.println("#end");
		tw.println("");
	}
	
	private void writeFeatures(PrintWriter tw)
	{
		for(Unit u : coms)
			tw.println("#homecom " + u.id + " --- " + u.name);
		for(Unit u : troops)
			tw.println("#homemon " + u.id + " --- " + u.name);
		for(String str : othercommands)
			tw.println(str);
		Iterator<Entry<Integer, Integer>> itr = gemMap.entrySet().iterator();
		while(itr.hasNext())
		{
			Entry<Integer, Integer> entry = itr.next();
			if(entry.getValue() > 0)
				tw.println("#gems " + entry.getKey() + " " + entry.getValue());
		}
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
