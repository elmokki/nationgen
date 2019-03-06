package nationGen.diagnostics;

import java.util.*;
import java.util.stream.Collectors;

import com.elmokki.Dom3DB;
import com.elmokki.Generic;
import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.entities.Pose;
import nationGen.items.Item;
import nationGen.misc.FileUtil;
import org.junit.Assert;
import org.junit.Test;


/**
 * Tests the integrity of data files regarding things like missing references.
 */
public class DataIntegrityTest {
	
	/**
	 * Verifies that all possible spell names can be located in either the vanilla spell database or custom spell list.
	 */
	@Test
	public void checkSpellNames()
	{
		NationGen ng = new NationGen();
		NationGenAssets assets = new NationGenAssets(ng);
		
		Dom3DB spells = new Dom3DB("/db/spells.csv");
		
		Set<String> names = new HashSet<>(spells.getColumn("name"));
		names.addAll(assets.customspells.stream().map(f -> f.name).collect(Collectors.toList()));
		
		List<String> notFound = assets.spells.values().stream()
				.flatMap(List::stream)
				.flatMap(f -> Generic.getTagValues(f.tags, "spell").stream())
				.filter(spell -> !names.contains(spell))
				.collect(Collectors.toList());
		
		Assert.assertTrue("Spells not found: " + notFound, notFound.isEmpty());
	}
	
	/**
	 * Verifies that all sprite filenames on items point to actual files.
	 */
	@Test
	public void checkSpritePaths()
	{
		NationGen ng = new NationGen();
		NationGenAssets assets = new NationGenAssets(ng);
		
		List<String> notFound = new ArrayList<>();
		for(String str : assets.poses.keySet())
		{
			List<Pose> pl = assets.poses.get(str);
			for(Pose p : pl)
			{
				for(String slot : p.getListOfSlots())
				{
					if(p.getItems(slot) == null)
						continue;
					
					for(Item i : p.getItems(slot))
					{
						if(i.sprite.equals(""))
							continue;

						if (FileUtil.isMissing(i.sprite)) {
							notFound.add("[Set " + str + ", pose " + p + ", item " + i + ", slot " + slot + " sprite -> " + i.sprite + "]");
						}
						
						if(i.mask.equals("") || i.mask.equals("self"))
							continue;
		
						if (FileUtil.isMissing(i.mask)) {
							notFound.add("[Set " + str + ", pose " + p + ", item " + i + ", slot " + slot + " mask -> " + i.mask + "]");
						}
					}
				}
			}
		}

		Assert.assertTrue("Image files not found: " + notFound, notFound.isEmpty());
	}
}
