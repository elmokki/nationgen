package nationGen.diagnostics;


import com.elmokki.Dom3DB;
import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.misc.FileUtil;
import nationGen.units.Unit;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


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
		NationGenAssets assets = ng.getAssets();
		
		Dom3DB spells = new Dom3DB("/db/spells.csv");
		
		Set<String> names = new HashSet<>(spells.getColumn("name"));
		names.addAll(assets.customspells.stream().map(f -> f.name).collect(Collectors.toList()));
		
		List<String> notFound = assets.spells.values().stream()
				.flatMap(List::stream)
				.flatMap(f -> f.tags.getAllStrings("spell").stream())
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
		NationGenAssets assets = new NationGen().getAssets();
		
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
	
	@Test
	public void checkItemsCanRenderInPose() {
		NationGenAssets assets = new NationGen().getAssets();
		
		List<String> errors = new ArrayList<>();
		
		for (String str : assets.poses.keySet()) {
			List<Pose> poseList = assets.poses.get(str);
			for (Pose pose : poseList) {
				for (String slot : pose.getListOfSlots()) {
					if (pose.getItems(slot) == null)
						continue;
					
					// Check which render slots are defined
					Set<String> spriteRenderSlots = new HashSet<>();
					for (Item i : pose.getItems(slot)) {
						if (!"".equals(i.sprite)) {
							String renderslot = "".equals(i.renderslot) ? slot : i.renderslot;
							if ("offhand".equals(renderslot)) {
								renderslot = i.armor ? "offhanda" : "offhandw";
							}
							spriteRenderSlots.add(renderslot);
						}
					}
					// Check if this itemset that has a sprite defined is in the renderorder.
					for (String renderslot : spriteRenderSlots) {
						if ("offhanda".equals(renderslot) || "offhandw".equals(renderslot)) {
							if (!pose.renderorder.contains("offhand") && !pose.renderorder.contains(renderslot)) {
								errors.add("Pose list " + str + ": pose " + pose.name + ": slot " + slot + ": renderslot offhand - slot offhand or " + renderslot + " not found in #renderorder");
							}
						} else if (!pose.renderorder.contains(renderslot)) {
							errors.add("Pose list " + str + ": pose " + pose.name + ": slot " + slot + ": renderslot " + renderslot + " not found in #renderorder!");
						}
					}
				}
			}
		}
		
		errors.forEach(System.out::println);
		Assert.assertTrue("Some items with sprites don't have their slot defined in the #renderorder", errors.isEmpty());
	}
	
	@Test
	public void checkNeeds() {
		NationGen ng = new NationGen();
		NationGenAssets assets = ng.getAssets();
		assets.addThemePoses();
		
		Race emptyRace = new Race(ng);
		
		List<String> notFound = new ArrayList<>();
		for(String str : assets.poses.keySet()) {
			List<Pose> poseList = assets.poses.get(str);
			for(Pose pose : poseList) {
				// find the first suitable race for the pose
				Optional<Race> race = assets.races.stream()
					.filter(r -> r.poses.contains(pose))
					.findFirst();
				
				if (race.isEmpty()) {
					System.out.println("no race found for pose " + pose.name + " " + pose.roles);
				}
				
				Unit unit = new Unit(ng, race.orElse(emptyRace), pose);
				
				for(String slot : pose.getListOfSlots()) {
					if (pose.getItems(slot) == null)
						continue;
					
					for (Item i : pose.getItems(slot)) {
						if (!i.dependencies.isEmpty()) {
							try {
								unit.setSlot(slot, i);
							} catch (IllegalStateException e) {
								notFound.add(e.getMessage());
							}
						}
					}
				}
			}
		}
		System.out.println();
		
		notFound.forEach(System.out::println);
		Assert.assertTrue("Some item #needs couldn't be met.", notFound.isEmpty());
	}
}
