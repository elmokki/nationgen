package nationGen.restrictions;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/**
 * Class for generic restrictions that contain two text boxes which users use to add entries to list
 * - Contains optional combobox!
 * @author Elmokki
 *
 */
public abstract class DoubleTextBoxListRestriction<E>
  implements NationRestriction, ActionListener, ItemListener {

  protected String text = "";
  protected String name = "Generic text box restriction";

  protected String textFieldLabel = "Undefined label";
  protected String textfieldDefaultText = "";
  protected String textFieldLabel2 = "Undefined label";
  protected String textfieldDefaultText2 = "";

  protected boolean hascombobox = false;
  protected JComboBox<String> combobox = null;
  protected String comboboxlabel = "Undefined label";
  protected String[] comboboxoptions = null;
  public String comboselection = null;

  public DoubleTextBoxListRestriction(String text, String name) {
    this.name = name;
    this.text = text;
  }

  @Override
  public String toString() {
    return name;
  }

  protected DefaultListModel<E> rmodel = new DefaultListModel<>();
  protected JButton addButton;
  protected JButton removeButton;
  protected JList<E> chosen;

  // Extra text field
  protected JTextField textfield = null;
  protected JTextField textfield2 = null;

  @Override
  public void getGUI(JPanel panel) {
    // Text and buttons

    int toprows = 4;

    if (hascombobox) toprows++;

    JPanel top = new JPanel(new GridLayout(toprows, 1, 5, 5));
    top.add(new JLabel(text));
    JPanel combo = new JPanel(new GridLayout(1, 2, 5, 5));

    if (hascombobox) {
      JPanel tpanel2 = new JPanel(new GridLayout(1, 2, 5, 5));
      tpanel2.add(new JLabel(comboboxlabel));
      combobox = new JComboBox<String>(comboboxoptions);
      tpanel2.add(combobox);
      top.add(tpanel2);
      combobox.addItemListener(this);
    }

    JPanel tpanel = new JPanel(new GridLayout(1, 2, 5, 5));
    tpanel.add(new JLabel(textFieldLabel));
    textfield = new JTextField(textfieldDefaultText);
    tpanel.add(textfield);
    top.add(tpanel);

    JPanel tpanel2 = new JPanel(new GridLayout(1, 2, 5, 5));
    tpanel2.add(new JLabel(textFieldLabel2));
    textfield2 = new JTextField(textfieldDefaultText2);
    tpanel2.add(textfield2);
    top.add(tpanel2);

    addButton = new JButton("Add");
    addButton.addActionListener(this);
    removeButton = new JButton("Remove");
    removeButton.addActionListener(this);

    combo.add(addButton);
    combo.add(removeButton);
    top.add(combo);

    // Lists

    JPanel lists = new JPanel(new GridLayout(1, 1, 5, 5));

    chosen = new JList<E>(rmodel);
    lists.add(new JScrollPane(chosen));

    panel.add(top, BorderLayout.PAGE_START);
    panel.add(lists, BorderLayout.CENTER);
  }

  @Override
  public LayoutManager getLayout() {
    return new BorderLayout();
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    // Add button
    if (arg0.getSource().equals(addButton)) {
      if (
        textfield.getText().length() > 0 &&
        textfield2.getText().length() > 0 &&
        !rmodel.contains(textfield.getText() + " - " + textfield2.getText())
      ) {
        // Override this, please.
      }
    }
    // remove button
    if (
      arg0.getSource().equals(removeButton) && chosen.getSelectedIndex() > -1
    ) {
      rmodel.remove(chosen.getSelectedIndex());
    }
  }

  @Override
  public void itemStateChanged(ItemEvent arg0) {
    if (arg0.getStateChange() == 1) this.comboselection =
      (String) combobox.getSelectedItem();
  }
}
