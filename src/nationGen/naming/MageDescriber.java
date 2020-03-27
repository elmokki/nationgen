package nationGen.naming;

import nationGen.nation.Nation;
import nationGen.units.Unit;

import java.util.List;
import java.util.stream.Collectors;


public class MageDescriber {
	
	
	public static String getCommonNoun(Nation n)
	{
		Name noun = getCommonName(n);
		

		String derp = "";
		if(noun.prefix != null)
			derp = noun.prefix.toString();
		if(noun.prefixprefix != null)
			derp = noun.prefixprefix.toString() + " " + derp;
		if(noun.suffix != null)
			derp = noun.suffix.toString();
		if(noun.suffixprefix != null && noun.prefix == null)
			derp = noun.suffixprefix.toString() + " " + derp;

		return derp.trim();
	}
	

	public static Name getCommonName(Nation n)
	{
		List<Unit> primaryMages = n.selectCommanders()
			.filter(u -> u.tags.contains("schoolmage", 3))
			.collect(Collectors.toList());
		
		Name common;
		if(primaryMages.size() > 0)
		{
			common = primaryMages.get(0).name.getCopy();
			
			primaryMages.forEach(u -> updateCommonNameFromUnit(u, common));
			
			if(common.suffix == null && common.suffixprefix != null)
				common.suffixprefix = null;
			
			if(primaryMages.size() > 1)
				return common;
			
			// secondary mages
			n.selectCommanders()
				.filter(u -> u.tags.contains("schoolmage", 2))
				.forEach(u -> updateCommonNameFromUnit(u, common));

			// tertiary mages
			n.selectCommanders()
				.filter(u -> u.tags.contains("schoolmage", 1))
				.forEach(u -> updateCommonNameFromUnit(u, common));
			
			// compensation mages (commented because it did nothing prior to refactoring)
//			n.selectCommanders()
//				.filter(u -> u.tags.containsName("extramage"))
//				.forEach(u -> updateCommonNameFromUnit(u, common));
		
		} else {
			common = new Name();
		}
		
		if(common.suffix == null && common.suffixprefix != null)
			common.suffixprefix = null;
		
		
		return common;
	}

	private static void updateCommonNameFromUnit(Unit u, Name common) {

		if(u.name.prefix == null || !u.name.prefix.equals(common.prefix))
			common.prefix = null;
		if(u.name.type == null || !u.name.type.equals(common.type))
			common.setType("mage");
		if(u.name.suffixprefix == null || !u.name.suffixprefix.equals(common.suffixprefix))
			common.suffixprefix = null;
		if(u.name.suffix == null || !u.name.suffix.equals(common.suffix))
			common.suffix = null;
	}
}
