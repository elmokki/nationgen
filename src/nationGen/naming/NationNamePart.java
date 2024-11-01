package nationGen.naming;

import com.elmokki.Generic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NationNamePart {

  public String text = "";
  public List<String> cantBeAfter = new ArrayList<String>();
  public List<String> cantBeBefore = new ArrayList<String>();

  public boolean canBeAfter(NationNamePart part) {
    for (String str : cantBeAfter) {
      if (part.text.endsWith(str)) return false;
    }
    return true;
  }

  public boolean canBeBefore(NationNamePart part) {
    for (String str : cantBeBefore) {
      if (part.text.startsWith(str)) return false;
    }
    return true;
  }

  public static NationNamePart fromLine(String line) {
    String[] vowels = {
      "a",
      "e",
      "i",
      "o",
      "u",
      "y",
      "\u00E5"/* a with circle above */,
      "\u00E4"/* a with umlaut */,
      "\u00F6"/* o with umlaut */,
      "\u00FC",
      /* u with umlaut */
    };
    String[] consonants = {
      "b",
      "c",
      "d",
      "f",
      "g",
      "h",
      "j",
      "k",
      "l",
      "m",
      "n",
      "p",
      "q",
      "r",
      "s",
      "t",
      "w",
      "v",
      "x",
      "z",
    };
    String[] softconsonants = {
      "f",
      "h",
      "j",
      "l",
      "m",
      "n",
      "r",
      "s",
      "w",
      "v",
      "x",
      "z",
    };
    String[] hardconsonants = { "b", "c", "d", "g", "k", "p", "q", "t" };

    HashMap<String, String[]> crap = new HashMap<String, String[]>();
    crap.put("vowel", vowels);
    crap.put("consonant", consonants);
    crap.put("hardconsonant", hardconsonants);
    crap.put("softconsonant", softconsonants);

    List<String> args = Generic.parseArgs(line);

    NationNamePart part = new NationNamePart();

    if (args.size() == 0 || args.get(0).startsWith("-")) return null;
    else part.text = args.get(0);

    List<String> list;
    for (int i = 1; i < args.size(); i++) {
      if (args.get(i).toLowerCase().startsWith("cantbeafter")) {
        list = part.cantBeAfter;
      } else if (args.get(i).toLowerCase().startsWith("cantbebefore")) {
        list = part.cantBeBefore;
      } else {
        continue;
      }

      List<String> newstuff = new ArrayList<String>();
      for (int j = 1; j < Generic.parseArgs(args.get(i)).size(); j++) {
        String arg = Generic.parseArgs(args.get(i)).get(j);
        if (crap.get(arg) != null) {
          List<String> temp = new ArrayList<String>();
          for (String s : crap.get(arg)) {
            if (newstuff.size() > 0) for (String s2 : newstuff) temp.add(
              s2 + s
            );
            else temp.add(s);
          }
          newstuff.clear();
          newstuff.addAll(temp);
        } else {
          List<String> temp = new ArrayList<String>();
          if (newstuff.size() > 0) for (String s2 : newstuff) {
            temp.remove(s2);
            temp.add(s2 + arg);
          }
          else temp.add(arg);

          newstuff.clear();
          newstuff.addAll(temp);
        }
      }
      list.addAll(newstuff);
    }

    return part;
  }
}
