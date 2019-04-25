package com.elmokki;

import nationGen.misc.FileUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Dom3DB {
	
	public HashMap<String, List<String>> entryMap = new HashMap<>();
    private List<String> definition;
    private List<String> booleanargs;
    
    public List<String> getDefinition()
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
    	List<String> row = new ArrayList<>(this.definition.size());
    	for (String col : this.definition) {
    		String val = attributes.get(col);
    		
    		row.add(val != null ? val : "id".equals(col) ? id : "");
		}

    	entryMap.put(id, row);
    }
    
    public Dom3DB(String filename)
    {
       // System.out.print("Reading Dom3DB from " + filename + "... ");
        
        List<String> lines = FileUtil.readLines(filename);

		String rawdef = lines.remove(0);
		definition = List.of(rawdef.split(";"));
		boolean convert = false;
		if(definition.size() < 2)
		{
			convert = true;
			rawdef = rawdef.replaceAll("\t", ";");
			definition = List.of(rawdef.split(";"));
		}

        // Let's read the unit data then.

        for (String line : lines)
        {
        	if (!line.isEmpty()) {
				if (convert)
					line = line.replaceAll("\t", ";");
		
				// Set units[id] to line of that unit.
				List<String> row = List.of(line.split(";"));
				this.entryMap.put(row.get(0), row);
			}
        }

       // System.out.println(derp + " definitions loaded!");

        // Find out boolean args
    	booleanargs = new ArrayList<>();
    	for(String str : definition)
    	{
    		boolean isBool = true;
    		for(String id : entryMap.keySet())
    		{
    			String value = this.GetValue(id, str);
				if (!value.equals("") && !value.equals("0") && !value.equals("1")) {
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
    	int integer;
    	try { 
    		integer = Integer.parseInt(GetValue(id, value, defaultvalue + "")); 
    	}
    	catch(Exception e)
    	{
    		integer = defaultvalue;
    	}
    	
    	return integer;
    }
    
    private int getColumnIndex(String columnName) {
		int placeOfValue = -1;
		for (int i = 0; i < definition.size(); i++) {
			if (definition.get(i).equalsIgnoreCase(columnName)) {
				placeOfValue = i;
				break;
			}
		}
		return placeOfValue;
	}

    
    public List<String> getColumn(String name)
    {
    	List<String> list = new ArrayList<>();
    	
    	int placeOfValue = getColumnIndex(name);
        
        if(placeOfValue == -1)
        	return list;
        
        for(List<String> line : entryMap.values())
        	list.add(line.get(placeOfValue));
        
        return list;
    }
    
    
    public void saveToFile(String filename)
    {
		
		StringBuilder def = new StringBuilder();
		for (String s : definition)
			def.append(s).append(";");
		
		List<String> lines = new ArrayList<>();
		lines.add(def.toString());
		
		for(int i = 0; i < 5000; i++)
		{
			if(entryMap.keySet().contains("" + i))
				lines.add(String.join(";", entryMap.get("" + i)));

		}

		FileUtil.writeLines(filename, lines);

    }
    
    public void setValue(String id, String value, String column)
    {
    	
        int index = getColumnIndex(column);
        if (index == -1) {
			index = definition.size();
			definition.add(column);
        }
        
		List<String> line = entryMap.get(id);
	
		while (line.size() <= index) {
        	line.add("");
		}
        
        line.set(index, value);
    }
    
    
    /**
     * Returns specified value for the unit of specified id
     * @param id ID of the unit
     * @param column field name, for example unitname
     * @param defaultvalue value returned if field contains nothing
     * @return Specified value for the unit of specified id
     */
    public String GetValue(String id, String column, String defaultvalue)
    {

    	List<String> line = entryMap.get(id);
    	
    	if(id.equals("-1") || line == null || line.isEmpty())
    	{
    		return defaultvalue;
    	}
    	
        int index = getColumnIndex(column);

        if(index == -1)
        {
        	return defaultvalue;
        }
    
        if (line.size() <= index) {
        	return defaultvalue;
		}
        
        String value = line.get(index);
        
        return "".equals(value) ? defaultvalue : value;
    }
}
