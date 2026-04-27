package nationGen.restrictions;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ItemEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/**
 * Class for generic restrictions that contain a text box which users use to add entries to list
 * - Contains optional combobox!
 * @author Elmokki
 *
 */
public abstract class TextBoxListRestrictionWithCheckboxes
  extends TextBoxListRestriction {

  protected JCheckBox leftCheckBox = null;
  protected JCheckBox rightCheckBox = null;

  protected String leftCheckboxLabel = "Undefined label";
  protected String rightCheckboxLabel = "Undefined label";

  public Boolean isLeftCheckboxMarked = false;
  public Boolean isRightCheckboxMarked = false;

  private int index = 0;

  public TextBoxListRestrictionWithCheckboxes(String text, String name) {
    super(text, name);
  }

  @Override
  public void getGUI(JPanel panel) {
    // Text and buttons

    int toprows = 4;

    if (this.hascombobox) {
      toprows++;
    }

    JPanel top = new JPanel(new GridLayout(toprows, 1, 5, 5));
    top.add(new JLabel(text));
    JPanel combo = new JPanel(new GridLayout(1, 2, 5, 5));

    if (this.hascombobox) {
      JPanel tpanel2 = new JPanel(new GridLayout(1, 2, 5, 5));
      tpanel2.add(new JLabel(comboboxlabel));
      this.combobox = new JComboBox<String>(comboboxoptions);
      this.combobox.setSelectedIndex(this.index);
      this.combobox.addItemListener(this);
      tpanel2.add(this.combobox);
      top.add(tpanel2);
    }

    JPanel tpanel = new JPanel(new GridLayout(1, 2, 5, 5));
    tpanel.add(new JLabel(this.textFieldLabel));
    textfield = new JTextField(textfieldDefaultText);
    tpanel.add(textfield);
    top.add(tpanel);

    JPanel tpanel3 = new JPanel(new GridLayout(1, 2));
    this.leftCheckBox = new JCheckBox(this.leftCheckboxLabel, this.isLeftCheckboxMarked);
    this.rightCheckBox = new JCheckBox(this.rightCheckboxLabel, this.isRightCheckboxMarked);
    this.leftCheckBox.addItemListener(this);
    this.rightCheckBox.addItemListener(this);
    tpanel3.add(this.leftCheckBox);
    tpanel3.add(this.rightCheckBox);
    top.add(tpanel3);

    this.addButton = new JButton("Add");
    this.addButton.addActionListener(this);
    this.removeButton = new JButton("Remove");
    this.removeButton.addActionListener(this);

    combo.add(this.addButton);
    combo.add(this.removeButton);
    top.add(combo);

    // Lists
    JPanel lists = new JPanel(new GridLayout(1, 1, 5, 5));

    this.chosen = new JList<String>(rmodel);
    lists.add(new JScrollPane(chosen));

    panel.add(top, BorderLayout.PAGE_START);
    panel.add(lists, BorderLayout.CENTER);
  }

  @Override
  public LayoutManager getLayout() {
    return new BorderLayout();
  }

  @Override
  public void itemStateChanged(ItemEvent e) {
    Object source = e.getItemSelectable();
    int stateChange = e.getStateChange();

    if (source == this.combobox && stateChange == 1) {
      this.comboselection = (String) combobox.getSelectedItem();
      this.index = combobox.getSelectedIndex();
    }

    // Options settings
    else if (source == this.leftCheckBox) {
      this.isLeftCheckboxMarked = stateChange == ItemEvent.SELECTED;
    }

    else if (source == this.rightCheckBox) {
      this.isRightCheckboxMarked = stateChange == ItemEvent.SELECTED;
    }
  }
}
