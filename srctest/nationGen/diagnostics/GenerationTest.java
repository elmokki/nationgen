package nationGen.diagnostics;

import com.elmokki.Generic;
import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.entities.Race;
import nationGen.entities.Theme;
import nationGen.misc.ChanceIncHandler;
import nationGen.naming.NameGenerator;
import org.junit.Test;

import java.util.*;


/**
 * Tests features of random generation.
 */
public class GenerationTest {
	
	/**
	 * Tests nation name generation for each possible combination of themes on each non-secondary race.  The theme
	 * combinations are to ensure that no combination leaves the nation without name syllable files defined.
	 */
	@Test
	public void testNationNameGeneration() {
		
		NationGen ng = new NationGen();
		NationGenAssets assets = ng.getAssets();
		
		NameGenerator ngen = new NameGenerator(ng);

		for (Race baseRace : assets.races) {
			if (!baseRace.tags.containsName("secondary")) {
				List<Theme> possibleThemes = ChanceIncHandler.retrieveFilters("racethemes", "default_racethemes", assets.themes, null, baseRace);
				
				Map<String, List<Theme>> themeChoiceMap = new LinkedHashMap<>();
				for (String freeThemeType : baseRace.tags.getAllStrings("freetheme")) {
					List<Theme> freeThemeChoices = ChanceIncHandler.getFiltersWithType(freeThemeType, possibleThemes);
					if (!freeThemeChoices.isEmpty()) {
						themeChoiceMap.put(freeThemeType, freeThemeChoices);
					}
				}
				
				for (Map<String, Theme> themeSet : generateCombinations(themeChoiceMap)) {
					Race race = baseRace.getCopy();
					for (Theme theme : themeSet.values()) {
						race.handleTheme(theme);
					}
							
					System.out.print(race.visiblename + " (" + race.name + ") " + themeSet + ": ");
					List<String> names = new ArrayList<>();
					for (int i = 0; i < 10; i++) {
						names.add(ngen.generateNationName(race, null));
					}
					System.out.println(Generic.listToString(names, ","));
				}
			}
		}
	}
	
	/**
	 * Rough probably-inefficient way of getting all combinations of choosing 1 value in each list in the map.
	 * LinkedHashMap keeps the values in order.
	 *
	 * @param choices The ordered map of choice lists to generate all combinations from.
	 * @param <K> The key of the map of choices.  These simply label the choice
	 * @param <V> The type of the thing we are generating combinations of
	 * @return The list of all combinations of choosing one value from each key
	 */
	private static <K, V> List<Map<K, V>> generateCombinations(Map<K, List<V>> choices) {
		List<Map<K, V>> combos = new ArrayList<>();
		if (choices.isEmpty()) {
			combos.add(Collections.emptyMap());
		} else {
			Map.Entry<K, List<V>> choiceEntry = choices.entrySet().iterator().next();
			Map<K, List<V>> nextChoices = new LinkedHashMap<>(choices);
			nextChoices.remove(choiceEntry.getKey());
			
			List<Map<K, V>> nextChosens = generateCombinations(nextChoices);
			for (V choice : choiceEntry.getValue()) {
				for (Map<K, V> nextChosen : nextChosens) {
					Map<K, V> chosen = new LinkedHashMap<>();
					chosen.put(choiceEntry.getKey(), choice);
					chosen.putAll(nextChosen);
					combos.add(chosen);
				}
			}
		}
		return combos;
	}
}
