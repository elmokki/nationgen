package nationGen.misc;

import java.util.ArrayList;
import java.util.List;
import nationGen.NationGenAssets;
import nationGen.chances.EntityChances;
import nationGen.entities.Filter;
import nationGen.nation.Nation;

public class GodGen extends ChanceIncHandler {

  private Nation nation;
  private List<Filter> gods = new ArrayList<>();
  private List<Filter> additionalgods = new ArrayList<>();

  // The chance, in %, that an individual god will be added to the nation on the first roll
  private static final int FIRST_EXTRA_GOD_CHANCE = 100;

  // The reduction in the chances of rolling another additional #individualgods assigned to the nation
  private static final float CHANCE_REDUCTION_FOR_EVERY_EXTRA_GOD_ASSSIGNED = 1.5f;

  public GodGen(Nation nation, NationGenAssets assets) {
    super(nation);
    this.nation = nation;

    // All of the #gods tags (homerealm pantheons) that are in this nation's definition
    List<Arg> godsTags = nation.races.get(0).tags.getAllValues("gods");

    // All of the #additionalgods tags (individual gods) that are in this nation's definition
    List<Arg> additionalgodsTags = nation.races.get(0).tags.getAllValues("additionalgods");

    // Fetch the asset data of the above #gods tags (their actual definition with all their commands)
    for (Arg godsTag : godsTags) {
      gods.addAll(assets.gods.get(godsTag.get()));
    }

    // Fetch the asset data of the above #additionalgods tags (their actual definition with all their commands)
    for (Arg additionalgodsTag : additionalgodsTags) {
      additionalgods.addAll(assets.gods.get(additionalgodsTag.get()));
    }

    // If no #gods are defined in the nation, then load the default_gods list
    // (defined in data/gods/pantheons/gods-default.txt and loaded in data/gods/gods.txt)
    if (gods.isEmpty()) {
      gods.addAll(assets.gods.get("default_gods"));
    }
  }

  // Selects a given amount of gods to actually grant the nation
  public List<Command> giveGods() {
    ChanceIncHandler chandler = new ChanceIncHandler(nation);

    // Select a random #gods tag from the nation and pick that as their main pantheon 
    Filter pantheon = chandler.handleChanceIncs(gods).getRandom(nation.random);

    // The final list of gods that will be assigned to this nation, from #gods and #additionalgods
    List<Command> assignedGods = new ArrayList<>(pantheon.getCommands());

    // The default #homerealm that most nations have access to in vanilla
    Command defaultRealm = Command.args("#homerealm", "10");
    assignedGods.add(defaultRealm);

    if (additionalgods.size() > 0) {
      // Filter the additionalgods to only keep those that are allowed in this nation's pantheon type
      List<Filter> allowedAdditionalgods = new ArrayList<>();
      for (Filter additionalgod : additionalgods) {
        // Get all #allowed <pantheon name> commands in this additionalgod
        List<String> allowed = additionalgod.tags.getAllStrings("allowed");

        // If there are no #allowed tags, allow the god automatically
        // If there are some, make sure the nation's pantheon name matches one of them
        if (allowed.size() == 0 || allowed.contains(pantheon.name)) {
          allowedAdditionalgods.add(additionalgod);
        }
      }

      // If no gods are left to pick from, return early with just the pantheon gods
      if (allowedAdditionalgods.size() <= 0) {
        return assignedGods;
      }

      // Feed the list of allowed #additionalgods to handleChanceIncs(), which will process
      // their #basechance, #chanceinc, #themeinc, etc. and create a list of all these gods
      // each with their own weight based on those tags
      EntityChances<Filter> additionalgodsWeights = chandler.handleChanceIncs(allowedAdditionalgods);

      // The chance that another individual god will be added to the nation. Starts at 100
      // to guarantee at least one additional god added, if the nation had any defined/allowed
      int chanceOfAnotherGodAdded = FIRST_EXTRA_GOD_CHANCE;
      int safetyLoopLimit = 100;

      while (safetyLoopLimit > 0) {
        // Roll a d100 to see if another individualgod gets assigned to the nation
        int roll = nation.random.nextInt(100);

        // If roll fails, stop the process entirely. No more individual gods will get assigned
        if (roll > chanceOfAnotherGodAdded) {
          break;
        }

        // If the roll succeeds, select one random allowed god from the list of weights that was created above
        Filter randomAdditionalGod = additionalgodsWeights.getRandom(nation.random);

        // Remove the selected god from the list of possible gods and assign it to the nation
        allowedAdditionalgods.remove(randomAdditionalGod);
        assignedGods.addAll(randomAdditionalGod.getCommands());

        // Reduce the chance that an extra god will get added again in the next loop iteration
        chanceOfAnotherGodAdded /= CHANCE_REDUCTION_FOR_EVERY_EXTRA_GOD_ASSSIGNED;
        safetyLoopLimit--;
      }
    }

    // Return all the nation's given gods, both from a pantheon and the rolled individual gods
    return assignedGods;
  }
}
