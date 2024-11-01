package nationGen.magic;

import com.elmokki.Generic;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import nationGen.NationGen;
import nationGen.entities.Filter;
import nationGen.misc.Arg;
import nationGen.misc.Command;

public class MagicPattern extends Filter {

  public LinkedHashMap<Integer, Integer> picks = new LinkedHashMap<>();
  public List<RandomEntry> randoms = new ArrayList<>();
  public List<Integer> levels = new ArrayList<>();
  public List<Integer> primaries = new ArrayList<>();
  public int premium = 0;
  public int gcost = 0;

  public MagicPattern(NationGen nationGen) {
    super(nationGen);
  }

  public String toString() {
    return toString(null);
  }

  public String toString(List<MagicPath> prio) {
    String str = "";
    int high = this.getHighestReachable(1);

    int at = 0;

    for (int i = high; i > 0; i--) {
      for (int j = 0; j < this.getPathsAt(i); j++) {
        String a = "";
        if (prio != null) {
          a = prio.get(at).letter;
          at++;
        }
        str = str + i + a + "-";
      }
    }

    double linkedrandom = 0;
    double random = 0;
    for (RandomEntry e : randoms) {
      Double d = e.amount;
      if (d > 1) linkedrandom += d;
      else random += d;
    }

    if (linkedrandom > 0) str = str + linkedrandom + "?l-";
    if (random > 0) str = str + random + "?";

    if (str.endsWith("-")) str = str.substring(0, str.length() - 1);

    return str;
  }

  public List<Command> getMagicCommands(List<MagicPath> prio) {
    List<Command> coms = new ArrayList<>();

    int high = this.getHighestReachable(1);

    int at = 0;

    for (int i = high; i > 0; i--) {
      for (int j = 0; j < this.getPathsAt(i); j++) {
        if (prio != null) {
          coms.add(
            new Command("#magicskill", new Arg(prio.get(at).number), new Arg(i))
          );
          at++;
        }
      }
    }

    for (RandomEntry e : randoms) {
      double d = e.amount;
      coms.add(
        new Command(
          "#custommagic",
          new Arg(pathsToMask(e.paths, prio)),
          new Arg((int) (d * 100))
        )
      );
    }

    return coms;
  }

  private int pathsToMask(int paths, List<MagicPath> prio) {
    int mask = 0;

    for (int i = 0; i < 10; i++) {
      int num = (int) Math.pow(2, i);
      if (Generic.containsBitmask(paths, num)) {
        mask += prio.get(i).mask;
      }
    }
    return mask;
  }

  public int getPicks(double probability) {
    int[] picks = this.getPathLevels(probability);
    int total = 0;

    for (int i = 0; i < picks.length; i++) total += picks[i];

    return total;
  }

  public int getPathsAt(int level) {
    int paths = 0;
    for (Integer i : this.picks.values()) if (i == level) paths++;

    return paths;
  }

  public int getPathsAtleastAt(int level) {
    int paths = 0;
    for (Integer i : this.picks.values()) if (i >= level) paths++;

    return paths;
  }

  public int getPathsAtleastAt(int level, double probability) {
    int c = 0;
    int[] derp = getPathLevels(probability);
    for (int i = 0; i < derp.length; i++) if (derp[i] >= level) c++;

    return c;
  }

  public int getRandoms(double probability) {
    int r = 0;
    for (RandomEntry e : randoms) {
      double realamount = 1;
      double amount = e.amount;
      if (amount > 1) realamount = amount;

      if (amount >= probability) r += realamount;
    }
    return r;
  }

  public int[] getPaths(List<Integer> prio) {
    int[] paths = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    int high = this.getHighestReachable(1);

    int at = 0;

    for (int i = high; i > 0; i--) {
      for (int j = 0; j < this.getPathsAt(i); j++) {
        if (prio != null) {
          paths[prio.get(at)] += i;
          at++;
        }
      }
    }

    return paths;
  }

  public MagicPathInts getPathsWithRandoms(List<MagicPath> prio, double prob) {
    int[] newpaths = getPathLevels(prob);
    MagicPathInts paths = new MagicPathInts();

    for (int i = 0; i < prio.size(); i++) {
      paths.set(prio.get(i), newpaths[i]);
    }

    return paths;
  }

  public int getPrice() {
    // Get premium
    double premium = this.premium;

    // Autocalc algorithm from now on
    int[] cheap = getPathLevels(1);
    int[] expensive = getPathLevels(1);

    cheap[8] = 0;
    expensive[8] = 0;

    // Add randoms in the cheapest and the most expensive ways
    int[] masks = { 1, 2, 4, 8, 16, 32, 64, 128, 256 };
    for (RandomEntry e : randoms) {
      List<Integer> possibles = new ArrayList<Integer>();
      for (int i = 0; i < masks.length; i++) {
        if (Generic.containsBitmask(e.paths, masks[i])) {
          if (e.amount >= 1) {
            possibles.add(i);
          }
        }
      }

      if (possibles.size() > 0) {
        // Get the lowest and highest values and their place out of the possible increases
        int lowestvalue = 140;
        int highestvalue = -1;
        int lowestid = -1;
        int highestid = -1;
        for (int i = 0; i < possibles.size(); i++) {
          int id = possibles.get(i);
          if (cheap[id] < lowestvalue) {
            lowestvalue = cheap[id];
            lowestid = id;
          }
          if (expensive[id] > highestvalue) {
            highestvalue = expensive[id];
            highestid = id;
          }
        }

        expensive[highestid] += e.amount;
        cheap[lowestid] += e.amount;
      }
    }

    double lowp = (double) pricePaths(cheap);
    double highp = (double) pricePaths(expensive);
    int price = (int) Math.round(0.25 * lowp + 0.8 * highp);

    /*
		System.out.print("HIGH:");
		for(int i = 0; i < expensive.length; i++)
			System.out.print(expensive[i] + " ");
		System.out.println();
		System.out.print("LOW:");
		for(int i = 0; i < cheap.length; i++)
			System.out.print(cheap[i] + " ");
		System.out.println();
		*/

    return price + (int) premium;
  }

  private int pricePaths(int[] paths) {
    int price = 0;

    int highpricing = 84;
    int lowpricing = 56;

    // Price the highest path
    int highestvalue = -1;
    int highestid = -1;
    for (int i = 0; i < paths.length; i++) {
      if (paths[i] > highestvalue) {
        highestvalue = paths[i];
        highestid = i;
      }
    }

    if (highestid > -1 && highestvalue > 0) {
      price += 42 + highpricing * (highestvalue - 1);
      paths[highestid] = 0;
    }

    // Price the rest
    for (int i = 0; i < 8; i++) {
      if (paths[i] > 0) {
        price += 28 + lowpricing * (paths[i] - 1);
      }
    }

    // EXTRA STUFF
    // Having blood is +28 gold.
    if (paths[7] > 0) price += 28;

    return price;
  }

  public int[] getPathLevels(double probability) {
    int[] derp = new int[10];
    int[] masks = { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512 };
    for (int i = 0; i < masks.length; i++) {
      for (Integer p : picks.keySet()) if (
        Generic.containsBitmask(p, masks[i])
      ) derp[i] += picks.get(p);

      int rand = 0;
      double prob = 1;
      for (RandomEntry e : randoms) {
        if (Generic.containsBitmask(e.paths, masks[i])) {
          // Realamount = actual increase in path
          // At amount <= 1 it is 1
          // At amount > 1 (ie linked randoms) it is amount
          double realamount = 1;
          double amount = e.amount;

          if (amount > 1) {
            realamount = amount;
            amount = 1;
          }
          double chance = amount / (double) Generic.AmountOfVariables(e.paths);

          prob *= chance;

          if (prob < probability) continue;
          else rand += realamount;
        }
      }

      derp[i] += rand;
    }

    return derp;
  }

  public int getPicksAtHighest() {
    int hi = getHighestReachable(1);
    return getPathsAt(hi);
  }

  public int getHighestReachable(double probability) {
    int[] levels = getPathLevels(probability);

    int level = 0;
    for (int i = 0; i < levels.length; i++) {
      if (levels[i] >= level) level = levels[i];
    }

    return level;
  }

  @Override
  public void handleOwnCommand(Command command) {
    switch (command.command) {
      case "#picks": {
        int amount = command.args.get(0).getInt();
        int path = command.args.get(1).getInt();

        int old = 0;
        if (picks.get(path) != null) old = picks.get(path);

        picks.put(path, old + amount);

        break;
      }
      case "#random": {
        double amount = command.args.get(0).getDouble();
        int path = command.args.get(1).getInt();
        randoms.add(new RandomEntry(path, amount));
        break;
      }
      case "#level": {
        int num = command.args.get(0).getInt();
        if (!levels.contains(num)) levels.add(num);
        break;
      }
      case "#primarymages": {
        int num = command.args.get(0).getInt();
        if (!primaries.contains(num)) primaries.add(num);
        break;
      }
      case "#pricepremium":
        this.premium = command.args.get(0).getInt();
        break;
      default:
        super.handleOwnCommand(command);
        break;
    }
  }
}
