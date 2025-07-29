package nationGen.restrictions;

import com.elmokki.Generic;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import nationGen.magic.MageGenerator;
import nationGen.magic.MagicPath;
import nationGen.magic.MagicPathInts;
import nationGen.nation.Nation;

public class MagicAccessRestriction
  extends TwoListRestrictionWithComboBox<String, String> {

  public List<String> neededPaths = new ArrayList<>();

  public MagicAccessRestriction() {
    super(
      "<html>Nation needs have 1 in 4 (100% random for 4 paths or better) access to at least one of the paths listed in the right box</html>",
      "Magic: Access"
    );
    this.extraTextField = true;
    this.textfieldDefaultText = "1";
    this.textFieldLabel = "Minimum magic level for added paths";

    this.comboboxlabel = "25% probability randoms allowed";
    this.comboboxoptions = new String[] { "True", "False" };

    MagicPath.NON_HOLY
      .stream()
      .sorted(Comparator.comparing(MagicPath::name))
      .forEach(mPath -> rmodel.addElement(mPath.name));
  }

  @Override
  public NationRestriction getRestriction() {
    MagicAccessRestriction res = new MagicAccessRestriction();

    for (int i = 0; i < chosen.getModel().getSize(); i++) res.neededPaths.add(
      chosen.getModel().getElementAt(i)
    );

    res.comboselection = comboselection;
    return res;
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    // Add button
    if (arg0.getSource().equals(addButton) && all.getSelectedIndex() > -1) {
      if (rmodel.getElementAt(all.getSelectedIndex()) != null) {
        if (!rmodel2.contains(rmodel.getElementAt(all.getSelectedIndex()))) {
          if (textfield == null) rmodel2.addElement(
            rmodel.getElementAt(all.getSelectedIndex())
          );
          else if (Generic.isNumeric(textfield.getText())) rmodel2.addElement(
            rmodel.getElementAt(all.getSelectedIndex()) +
            " " +
            textfield.getText()
          );
        }
      }
    }
    // remove button
    if (
      arg0.getSource().equals(removeButton) && chosen.getSelectedIndex() > -1
    ) {
      rmodel2.remove(chosen.getSelectedIndex());
    }
  }

  @Override
  public boolean doesThisPass(Nation n) {
    boolean randoms = comboselection == null || comboselection.equals("True");

    MagicPathInts nonrandom_paths = MageGenerator.getAllPicks(
      n.listCommanders("mage"),
      randoms
    );

    if (neededPaths.size() == 0) {
      System.out.println("Magic access nation restriction has no paths set!");
      return true;
    }

    boolean pass = false;
    for (String p : neededPaths) {
      List<String> args = Generic.parseArgs(p);
      MagicPath path = MagicPath.fromName(args.get(0));
      int level = Integer.parseInt(args.get(1));
      if (nonrandom_paths.get(path) >= level) pass = true;
    }

    return pass;
  }

  @Override
  public RestrictionType getType() {
    return RestrictionType.MagicAccess;
  }
}
