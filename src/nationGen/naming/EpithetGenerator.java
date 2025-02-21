package nationGen.naming;

import com.elmokki.Generic;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import nationGen.NationGenAssets;
import nationGen.chances.EntityChances;
import nationGen.misc.ChanceIncHandler;
import nationGen.nation.Nation;

public class EpithetGenerator {

  private NationGenAssets assets;

  List<NamePart> mageepithetparts = new ArrayList<NamePart>();

  public EpithetGenerator(NationGenAssets assets) {
    this.assets = assets;
  }

  public void giveEpithet(Nation n) {
    Random random = new Random(n.random.nextInt());
    mageepithetparts = ChanceIncHandler.retrieveFilters(
      "epithet_era_names",
      "default_epithet_parts",
      assets.miscnames,
      null,
      n.races.get(0)
    );

    String epithet = "";

    double r = random.nextDouble();

    if (
      r > 0.55 && !MageDescriber.getCommonNoun(n).equals("")
    ) { // Second condition is relatively rare
      NamePart part = EntityChances.baseChances(mageepithetparts).getRandom(
        random
      );

      if (
        random.nextDouble() < 0.5 &&
        MageDescriber.getCommonNoun(n).split(" ").length < 2
      ) {
        epithet = MageDescriber.getCommonNoun(n) + " " + part.name;
      } else epithet = part.name + " of the " + MageDescriber.getCommonNoun(n);
    } else if (
      r > 0.3 && !MageDescriber.getCommonName(n).toString().equals("Mage")
    ) {
      NamePart part = EntityChances.baseChances(mageepithetparts).getRandom(
        random
      );
      Name common = MageDescriber.getCommonName(n);
      common.setType(Generic.plural(common.type.toString()));

      if (common.suffix != null || common.suffixprefix != null) if (
        common.suffixprefix == null
      ) epithet = part.name + " of the " + common.suffix;
      else epithet = part.name +
      " of the " +
      common.suffixprefix +
      " " +
      common.suffix;
      else epithet = part.name + " of the " + common.toString();
    } else {
      epithet = n.sites.get(0).name;
    }

    epithet = Generic.capitalize(epithet);
    n.epithet = epithet;
    //n.commands.add(new Command("#epithet", "\"" + epithet + "\""));
  }
}
