package nationGen.magic;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Represents each of the different magic paths in Dominions.
 */
public enum MagicPath {
	
	FIRE("F", "fire", 128),
	AIR("A", "air", 256),
	WATER("W", "water", 512),
	EARTH("E", "earth", 1024),
	ASTRAL("S", "astral", 2048),
	DEATH("D", "death", 4096),
	NATURE("N", "nature", 8192),
	BLOOD("B", "blood", 16384),
	HOLY("H", "holy", 32768);
	
	public static final List<MagicPath> NON_HOLY = List.of(FIRE, AIR, WATER, EARTH, ASTRAL, DEATH, NATURE, BLOOD);
	
	public final String letter;
	public final String name;
	public final int number;
	public final int mask;
	
	MagicPath(String letter, String name, int mask) {
		this.letter = letter;
		this.name = name;
		this.number = ordinal();
		this.mask = mask;
	}
	
	public static MagicPath fromInt(int integer) {
		return MagicPath.values()[integer];
	}
	
	public static MagicPath fromName(String pathName) {
		return findFromName(pathName).orElseThrow(() -> new IllegalArgumentException("Illegal magic path name: " + pathName));
	}
	
	public static Optional<MagicPath> findFromName(String pathName) {
		return Arrays.stream(MagicPath.values()).filter(p -> p.name.equals(pathName)).findFirst();
	}
	
	public static List<MagicPath> listFromMask(int mask) {
		return Arrays.stream(MagicPath.values()).filter(p -> (p.mask & mask) != 0).collect(Collectors.toList());
	}
	
	public static String integerToPath(int integer)
	{
		String[] paths = { "fire", "air", "water", "earth", "astral", "death", "nature", "blood", "holy" };
		return paths[integer];
	}
	
	public static int PathToInteger(String path)
	{
		String[] paths = { "fire", "air", "water", "earth", "astral", "death", "nature", "blood", "holy" };
		path = path.toLowerCase().trim();
		for(int i = 0; i < 9; i++)
		{
			if(paths[i].equals(path))
			{
				return i;
			}
		}
		
		return -1;
	}
	
	public static String integerToShortPath(int integer)
	{
		String[] paths = { "F", "A", "W", "E", "S", "D", "N", "B", "H" };
		return paths[integer];
	}
	
	public static List<String> getListOfPathsInMask(int mask)
	{
		int[] masks = {128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768};
		String[] paths = { "fire", "air", "water", "earth", "astral", "death", "nature", "blood", "holy" };
		
		List<String> list = new ArrayList<String>();
		for(int i = masks.length - 1; i >= 0; i--)
		{
			if(mask >= masks[i])
			{
				list.add(paths[i]);
				mask = mask - masks[i];
			}
		}
		
		
		return list;
	}
	
}
