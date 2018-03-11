package com.elmokki;

import java.io.*;
import java.util.*;



public class Dom3DB {
	
	private boolean convert = false;
	public HashMap<String, String> entryMap = new HashMap<String, String>();
    private String[] definition;
    private List<String> booleanargs;
    
    public String[] getDefinition()
    {
    	return definition;
    }
    
    public List<String> getBooleanArgs()
    {
    	return booleanargs;
    }
    
    public int getSize()
    {
    	return entryMap.size();
    }
    
    /***
     * Adds to the entry map a value converted from a HashMap.
     * @param id id of the new item
     * @param attributes entry map
     */
    public void addToMap(String id, HashMap<String, String> attributes)
    {
    	String line = "";
    	for(String attr : definition)
    	{
    		String value = attributes.get(attr);
    		if(attr.equals("id") && value == null)
    			value = id + "";
    		
    		if(value == null)
    			value = "";
    		
    		
    		line = line + value + ";";
    	}
    	

    	entryMap.put(id, line);
    	
    }
    
    



    
    public Dom3DB(String filename) throws FileNotFoundException
    {
       // System.out.print("Reading Dom3DB from " + filename + "... ");
        
        Scanner file;

	
		file = new Scanner(new FileInputStream(System.getProperty("user.dir") + "/" + filename));

		String rawdef = file.nextLine();
		definition = rawdef.split(";");
		if(definition.length < 2)
		{
			convert = true;
			rawdef = rawdef.replaceAll("\t", ";");
			definition = rawdef.split(";");
		}

        String line;

        // Let's read the unit data then.

        while (file.hasNextLine())
        {
            // Read line
            line = file.nextLine();
            
            if(convert)
            	line = line.replaceAll("\t", ";");
            
            // Set units[id] to line of that unit.
            if(line.length() > 0 && line.split(";").length > 0)
            	entryMap.put(line.split(";")[0], line);
            
     
        }

       // System.out.println(derp + " definitions loaded!");      
        file.close();
        

        // Find out boolean args
    	booleanargs = new ArrayList<String>();
    	for(String str : definition)
    	{
    		boolean isBool = true;
    		for(String id : entryMap.keySet())
    		{
    			if(this.GetValue(id, str).equals("") || this.GetValue(id, str).equals("0") || this.GetValue(id, str).equals("1"))
    			{
    				// Do nothing
    			}
    			else
    			{
    				isBool = false;
    				break;
    			}
    		}
    		
    		if(isBool)
    			booleanargs.add(str);
    	}
    }
    
    

    /**
     * Returns specified value for the unit of specified id
     * @param id ID of the unit
     * @param value field name, for example "unitname"
     * @return Specified value for the unit of specified id
     */
    
    public String GetValue(String id, String value)
    {
    	return GetValue(id, value, "");
    }
    
    
    
    public int GetInteger(String id, String value)
    {
    	return GetInteger(id, value, 0);
    }
    
    public int GetInteger(String id, String value, int defaultvalue)
    {
    	int integer = 0;
    	try { 
    		integer = Integer.parseInt(GetValue(id, value, defaultvalue + "")); 
    	}
    	catch(Exception e)
    	{
    		integer = defaultvalue;
    	}
    	
    	return integer;
    }
    
    

    
    public List<String> getColumn(String name)
    {
    	List<String> list = new ArrayList<String>();
    	
    	// Gets the column
        int placeOfValue = -1;
        for (int i = 0; i < definition.length; i++)
        {
            if (definition[i].toLowerCase().equals(name.toLowerCase()))
            {
                placeOfValue = i;
                break;
            }
        }
        
        if(placeOfValue == -1)
        	return list;
        
        for(String line : entryMap.values())
        	list.add(this.getValue(line, placeOfValue));
        
        return list;
    }
    
    
    public void saveToFile(String filename) throws IOException
    {
		
		FileWriter fstream = new FileWriter(filename);
		PrintWriter tw = new PrintWriter(fstream);
		
		String def = "";
		for(int i = 0; i < definition.length; i++)
			def = def + definition[i] + ";";
		
		tw.println(def);
		
		for(int i = 0; i < 5000; i++)
		{
			if(entryMap.keySet().contains("" + i))
				tw.println(entryMap.get("" + i).replaceAll("\t", ";"));

		}

		tw.flush();
		tw.close();
		fstream.close();

    }
    
    public void setValue(String id, String value, String column)
    {
    	String line = entryMap.get(id);

       
    	// Gets the column
        int placeOfValue = -1;
        for (int i = 0; i < definition.length; i++)
        {
            if (definition[i].toLowerCase().equals(column.toLowerCase()))
            {
                placeOfValue = i;
                break;
            }
        }
        if(placeOfValue == -1)
        {
        	String[] newdef = new String[definition.length + 1];
        	for(int i = 0; i < definition.length; i++)
        		newdef[i] = definition[i];
        	newdef[definition.length] = column;
        	placeOfValue = definition.length;
        	definition = newdef;
        }
        
        
        String newline = "";
        String[] crap = new String[definition.length];
        String[] stuff = line.split(";");

        for(int i = 0; i < stuff.length; i++)
        	crap[i] = stuff[i];


        for(int i = 0; i < placeOfValue; i++)
        {
        	if(crap[i] == null)
        		crap[i] = "";
        	
        	newline = newline + crap[i] + ";";
        }
        newline = newline + value + ";";
        
        for(int i = placeOfValue + 1; i < crap.length; i++)
        {
        	if(crap[i] == null)
        		crap[i] = "";
        	
        	newline = newline + crap[i] + ";";
        }

        entryMap.put(id, newline);

        
    }
    
    
    /**
     * Returns specified value for the unit of specified id
     * @param id ID of the unit
     * @param value field name, for example unitname
     * @param defaultvalue value returned if field contains nothing
     * @return Specified value for the unit of specified id
     */
    public String GetValue(String id, String value, String defaultvalue)
    {

    	String line = entryMap.get(id);
    	
    	if(id.equals("-1") || line == null || line.equals(""))
    	{
    		return defaultvalue;
    	}
    	
    	// Gets the column
        int placeOfValue = -1;
        for (int i = 0; i < definition.length; i++)
        {
            if (definition[i].toLowerCase().equals(value.toLowerCase()))
            {
                placeOfValue = i;
                break;
            }
        }
        

        if(placeOfValue == -1)
        {
        	return defaultvalue;
        }
    
        
       try
       {
        if(getValue(line, placeOfValue).equals(""))
        	 return defaultvalue; 
       }
       catch(Exception e)
       {
    	   System.out.println("Dom3DB error: id " + id + " index " + placeOfValue + " value " + value + ". This happened probably because an attribute that does not exist was requested.");
       }
       	
        return getValue(line, placeOfValue);
    }
    
    
    private String getValue(String line, int index)
    {

    	int firstindex = 0;
    	int lastindex = line.toCharArray().length;
    	int passed = 0;
    	int at = 0;
    	for(char c : line.toCharArray())
    	{	
    		if(c == ';' || at == 0)
    		{
    			if(passed == index)
    				firstindex = at;
    			if(passed == index + 1)
    			{
    				lastindex = at;
    				break;
    			}
    			passed++;
    		}		
    	    at++;
    	}
    	


    	return line.substring(firstindex + 1, lastindex);
    	
    }

	public String getDom3DBname(String attrib) {
		String derp = this.GetValue(attrib, "unitname");
		return derp;
	}
    
    
}