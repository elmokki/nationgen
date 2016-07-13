package nationGen.restrictions;

public class RestrictionMagicEntry {

		public int paths = 1;
		public int level = 1;
		public boolean randoms = false;
		public RestrictionMagicEntry(int paths, int level, boolean randoms)
		{
			this.randoms = randoms;
			this.paths = paths;
			this.level = level;
		}
		
		public String toString()
		{
			String str = "At least " + paths + " paths at " + level + " or above";
			
			if(randoms)
				str = str + " with randoms";
			else
				str = str + " without randoms";

			return str;
		}
	
}
