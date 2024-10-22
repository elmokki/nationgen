package nationGen.misc;

import java.util.ArrayList;
import java.util.List;
import nationGen.NationGenAssets;
import nationGen.chances.EntityChances;
import nationGen.entities.Filter;
import nationGen.nation.Nation;

public class GodGen extends ChanceIncHandler {

  private Nation n;
  private List<Filter> orig = new ArrayList<>();
  private List<Filter> add = new ArrayList<>();

  public GodGen(Nation n, NationGenAssets assets) {
    super(n);
    this.n = n;

    for (Arg arg : n.races.get(0).tags.getAllValues("gods")) {
      orig.addAll(assets.miscdef.get(arg.get()));
    }

    for (Arg arg : n.races.get(0).tags.getAllValues("additionalgods")) {
      add.addAll(assets.miscdef.get(arg.get()));
    }

    if (orig.isEmpty()) orig.addAll(assets.miscdef.get("defaultgods"));
  }

  public List<Command> giveGods() {
    ChanceIncHandler chandler = new ChanceIncHandler(n);

    Filter pantheon = chandler.handleChanceIncs(orig).getRandom(n.random);
    List<Command> filters = new ArrayList<>(pantheon.getCommands());

    filters.add(Command.args("#homerealm", "10"));

    if (add.size() > 0) {
      List<Filter> mores = new ArrayList<>();
      for (Filter g : add) {
        List<String> allowed = g.tags.getAllStrings("allowed");

        if (allowed.size() == 0 || allowed.contains(pantheon.name)) mores.add(
          g
        );
      }

      EntityChances<Filter> possibles = chandler.handleChanceIncs(mores);

      // 0 to 4 extra Filters
      int moreFilters = n.random.nextInt(Math.min(mores.size() + 1, 5));

      for (int i = 0; i < moreFilters; i++) {
        Filter newFilter = possibles.getRandom(n.random);
        mores.remove(newFilter);
        filters.addAll(newFilter.getCommands());
      }
    }
    return filters;
  }
}
