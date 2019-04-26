package com.elmokki;


import nationGen.misc.FileUtil;

import java.util.*;


public class Dom3DB {
	
	public HashMap<String, List<String>> entryMap = new HashMap<>();
    private List<String> definition;
    private List<String> booleanargs;
    
    public List<String> getDefinition()
    {
    	return definition;
    }
    
    public void setDefinition(List<String> columnNames) {
    	if (columnNames.size() != this.definition.size()) {
    		throw new IllegalArgumentException(String.format("Definition must be the same size (old = %s, new = %s)",
				this.definition.size(), columnNames.size()));
		}
    	if (!"id".equals(columnNames.get(0))) {
    		throw new IllegalArgumentException(String.format("First column name must be 'id' (found '%s' instead)",
				columnNames.get(0)));
		}
    	
    	this.definition = new ArrayList<>(columnNames);
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
		definition = new ArrayList<>(List.of(rawdef.split(";")));
		boolean convert = false;
		if(definition.size() < 2)
		{
			convert = true;
			rawdef = rawdef.replaceAll("\t", ";");
			definition = new ArrayList<>(List.of(rawdef.split(";")));
		}

        // Let's read the unit data then.

        for (String line : lines)
        {
        	if (!line.isEmpty()) {
				if (convert)
					line = line.replaceAll("\t", ";");
		
				// Set units[id] to line of that unit.
				List<String> row = new ArrayList<>(List.of(line.split(";")));
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
    
    public Map<String, String> getColumnAsMap(String name) {
    	
    	Map<String, String> column = new LinkedHashMap<>();
    	
    	int index = getColumnIndex(name);
    	
    	if (index != -1) {
			for (Map.Entry<String, List<String>> entry : this.entryMap.entrySet()) {
				column.put(entry.getKey(), entry.getValue().get(index));
			}
		}
    	
    	return column;
	}
	
	public void setColumn(String name, Map<String, String> column) {
    	
    	int index = getColumnIndex(name);
    	
    	if (index != -1) {
    		for (Map.Entry<String, String> entry : column.entrySet()) {
    			if (this.entryMap.containsKey(entry.getKey())) {
    				this.entryMap.get(entry.getKey()).set(index, entry.getValue());
				}
			}
		}
	}
	
	public void removeColumn(String name) {
		int index = getColumnIndex(name);
		
		if (index == -1) {
			return;
		}
		
		this.definition.remove(index);
		
		for (List<String> line : entryMap.values()) {
			if (index < line.size()) {
				line.remove(index);
			}
		}
	}
	
	
	public void saveToFile(String filename)
    {
		List<String> lines = new ArrayList<>();
		
		lines.add(String.join(";", this.definition));
		
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
