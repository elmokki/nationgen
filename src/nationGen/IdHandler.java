package nationGen;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class IdHandler {
	private int nation = 1;
	private int unit = 1;
	private int weapon = 1;
	private int site = 1;
	private int armor = 1;
	private int name = 1;
	private int mock = -2;
	
	public List<Integer> forbiddenNations = new ArrayList<Integer>();
	public List<Integer> forbiddenUnits = new ArrayList<Integer>();
	public List<Integer> forbiddenWeapons = new ArrayList<Integer>();
	public List<Integer> forbiddenSites = new ArrayList<Integer>();
	public List<Integer> forbiddenArmors = new ArrayList<Integer>();
	public List<Integer> forbiddenNames = new ArrayList<Integer>();
	
	public IdHandler()
	{

	}
	
	private List<String> parseLine(String line)
	{
		return parseLine(line, "\"");
	}
	
	
	
	
	private String parseOne(List<String>args, String line, String c)
	{
		line = line.trim();
		boolean extra = false;
		int end = line.indexOf(" ");
		int start = 0;

		
		if(line.startsWith(c) && line.indexOf(c) != -1)
		{
			extra = true;
			start = 0;
			end = line.substring(1).indexOf(c) + 1;
		}	

		if(end < 0 || end > line.length() + 1)
				end = line.length();

		if(!extra)
		{
			args.add(line.substring(start, end));
			line = line.substring(end);
		}
		else
		{
			args.add(line.substring(start + 1, end));
			line = line.substring(end + 1);
		}



		return line;
	}
	
	/**
	 * Parses a line.
	 * @param line
	 * @return
	 */
	private List<String> parseLine(String line, String c)
	{

		List<String> args = new ArrayList<String>();
				
		do
		{
			line = parseOne(args, line, c);
		
		} while(line.trim().length() > 0);
		
		return args;
	}
	
	public void loadFile(String filename)
	{

		
        Scanner file;
		
		try {
			file = new Scanner(new FileInputStream(System.getProperty("user.dir") + "/" + filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		while(file.hasNextLine())
		{
			String line = file.nextLine().toLowerCase();
			if(line.length() == 0 || line.startsWith("-"))
				continue;

			List<String> args = parseLine(line);
			String command = args.get(0);
			args.remove(0);
			
			if(command.equals("nation"))
			{
				if(args.get(0).contains("-"))
				{
					for(int i = Integer.parseInt(args.get(0).split("-")[0]); i <= Integer.parseInt(args.get(0).split("-")[1]); i++)
					{
						forbiddenNations.add(i);
					}
				}
				else if(args.get(0).equals("start"))
				{
					nation = Integer.parseInt(args.get(1)) - 1;
				}
				else
				{
					forbiddenNations.add(Integer.parseInt(args.get(0)));
				}
			}
			if(command.equals("weapon"))
			{
				if(args.get(0).contains("-"))
				{
					for(int i = Integer.parseInt(args.get(0).split("-")[0]); i <= Integer.parseInt(args.get(0).split("-")[1]); i++)
					{
						forbiddenWeapons.add(i);
					}
				}
				else if(args.get(0).equals("start"))
				{
					weapon = Integer.parseInt(args.get(1)) - 1;
				}
				else
				{
					forbiddenWeapons.add(Integer.parseInt(args.get(0)));
				}
			}
			if(command.equals("unit"))
			{
				if(args.get(0).contains("-"))
				{
					for(int i = Integer.parseInt(args.get(0).split("-")[0]); i <= Integer.parseInt(args.get(0).split("-")[1]); i++)
					{
						forbiddenUnits.add(i);
					}
				}
				else if(args.get(0).equals("start"))
				{
					unit = Integer.parseInt(args.get(1)) - 1;
				}
				else
				{
					forbiddenUnits.add(Integer.parseInt(args.get(0)));
				}
			}
			if(command.equals("site"))
			{
				if(args.get(0).contains("-"))
				{
					for(int i = Integer.parseInt(args.get(0).split("-")[0]); i <= Integer.parseInt(args.get(0).split("-")[1]); i++)
					{
						forbiddenSites.add(i);
					}
				}
				else if(args.get(0).equals("start"))
				{
					site = Integer.parseInt(args.get(1)) - 1;
				}
				else
				{
					forbiddenSites.add(Integer.parseInt(args.get(0)));
				}
			}
			
			if(command.equals("armor"))
			{
				if(args.get(0).contains("-"))
				{
					for(int i = Integer.parseInt(args.get(0).split("-")[0]); i <= Integer.parseInt(args.get(0).split("-")[1]); i++)
					{
						forbiddenArmors.add(i);
					}
				}
				else if(args.get(0).equals("start"))
				{
					armor = Integer.parseInt(args.get(1)) - 1;
				}
				else
				{
					forbiddenArmors.add(Integer.parseInt(args.get(0)));
				}
			}

			
			if(command.equals("name"))
			{
				if(args.get(0).contains("-"))
				{
					for(int i = Integer.parseInt(args.get(0).split("-")[0]); i <= Integer.parseInt(args.get(0).split("-")[1]); i++)
					{
						forbiddenNames.add(i);
					}
				}
				else if(args.get(0).equals("start"))
				{
					name = Integer.parseInt(args.get(1)) - 1;
				}
				else
				{
					forbiddenNames.add(Integer.parseInt(args.get(0)));
				}
			}
		}
		
		file.close();
	}
	
	public void addRange(List<Integer> list, int from, int to)
	{
		for(int i = from; i <= to; i++)
		{
			list.add(i);
		}
	}
	
	
	public int nextMockId()
	{
		return mock--;
	}
	
	public int nextNationId()
	{
		nation++;
		while(forbiddenNations.contains(nation))
			nation++;
		return nation;
	}
	
	public int nextUnitId()
	{
		unit++;
		while(forbiddenUnits.contains(unit))
			unit++;
		
		return unit;
	}
	
	public int nextWeaponId()
	{
		weapon++;
		while(forbiddenWeapons.contains(weapon))
			weapon++;
		return weapon;
	}
	
	public int nextSiteId()
	{
		site++;
		while(forbiddenSites.contains(site))
			site++;
		return site;
	}
	
	public int nextArmorId()
	{
		armor++;
		while(forbiddenArmors.contains(armor))
			armor++;
		return armor;
	}
	
	public int nextNameId()
	{
		name++;
		while(forbiddenNames.contains(name))
			name++;
		return name;
	}
	
	
}
