package nationGen.GUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.Settings;
import nationGen.Settings.SettingsType;

public class GUI extends JFrame implements ActionListener, ItemListener, ChangeListener 
{
    private static final long serialVersionUID = 1L;
	private static final String startText = "Start!";
	private static final String abortText = "Stop and Export";
	private static final String pauseText = "Pause";
	private static final String unpauseText = "Unpause";
    
    JTextPane textPane = new JTextPane();
    JProgressBar progress = new JProgressBar(0, 100);
    JButton startButton;
    JButton pauseButton;
    JTabbedPane tabs = new JTabbedPane();
    
    JTextField settingtext = new JTextField("0");
    JTextArea amount = new JTextArea("1");
    JTextArea modname = new JTextArea("Random");
    JTextArea seed = new JTextArea("Random");
    JCheckBox seedRandom = new JCheckBox("Random");
    JCheckBox modNameRandom = new JCheckBox("Random");
    JTextArea seeds = new JTextArea("1337, 715517, 80085");
    JCheckBox advDesc = new JCheckBox("Write advanced descriptions");
    JCheckBox basicDesc = new JCheckBox("Write basic descriptions");
    JCheckBox preview = new JCheckBox("Draw sprite review image");
    List<JCheckBox> optionChecks = new ArrayList<>();
    Settings settings = new Settings();
    JCheckBox seedcheckbox = new JCheckBox("Use predefined nation seeds (separate by line change and/or comma)");
    RestrictionPane rpanel;
    JCheckBox hideVanillaNations = new JCheckBox("Hide vanilla nations");
    JSlider eraSlider = new JSlider(JSlider.HORIZONTAL, 1, 3, 2);
    JSlider spowerSlider = new JSlider(JSlider.HORIZONTAL, 1, 3, 1);
    
    private ReentrantLock pauseLock;
    private  NationGen n = null;
    private NationGenAssets assets;
    
    public static void main(String[] args) throws Exception
    {
    	if(args.length > 0 && args[0].equals("-commandline"))
        {
            new CommandLine(args);
        }
    	else
    	{
            SwingUtilities.invokeLater(() -> {
                GUI g = new GUI();
                g.setVisible(true);
            });
    	}
    }
    
    private void initGUI()
    {
    	optionChecks.add(advDesc);
    	optionChecks.add(basicDesc);
    	optionChecks.add(preview);
    	optionChecks.add(seedcheckbox);
    	optionChecks.add(hideVanillaNations);

    	seedRandom.setSelected(true);
    	modNameRandom.setSelected(true);
    	seed.setEnabled(false);
    	modname.setEnabled(false);
    	
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel options = new JPanel(new GridLayout(2,2));
        JPanel advoptions = new JPanel(new GridLayout(8,1));
        
        pauseLock = new ReentrantLock();
        
        // Restrictions need nationgen;
    	try 
        {
            n = new NationGen(pauseLock, settings, new ArrayList<>());
        } 
        catch (IOException e) 
        {
            System.out.println("Error initializing NationGen.");
            startButton.setEnabled(false);
            return;
        }
    	
    	assets = new NationGenAssets(n);
        rpanel = new RestrictionPane(n, assets);

        // Main
        tabs.addTab("Main", panel);
        
        startButton = new JButton(startText);
        startButton.addActionListener(this);
        pauseButton = new JButton(pauseText);
        pauseButton.setEnabled(false);
        pauseButton.addActionListener(this);
        seedRandom.addItemListener(this);
        modNameRandom.addItemListener(this);
        advDesc.addItemListener(this);
        preview.addItemListener(this);
        basicDesc.addItemListener(this);
        
        startButton.setPreferredSize(new Dimension(100, 50));

        textPane.setPreferredSize(new Dimension(600, 300));
        textPane.setBorder(BorderFactory.createLineBorder(Color.black));
        JScrollPane sp = new JScrollPane(textPane);
        
        progress.setPreferredSize(new Dimension(-1, 22));
        progress.setStringPainted(true);
        
        redirectSystemStreams();
        
        JPanel east = new JPanel(new GridLayout(12, 1));
        JPanel ncount = new JPanel(new GridLayout(1, 3));
        ncount.add(new JLabel("Nation amount"));
        amount.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.black));
        ncount.add(amount);
        ncount.add(new JLabel(""));
        
        JPanel seedpanel = new JPanel(new GridLayout(1, 3));
        seedpanel.add(new JLabel("Seed"));
        seed.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.black));
        seedpanel.add(seed);
        seedpanel.add(seedRandom);
        
        JPanel modnamepanel = new JPanel(new GridLayout(1, 3));
        modnamepanel.add(new JLabel("Mod name"));
        modname.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.black));
        modnamepanel.add(modname);
        modnamepanel.add(modNameRandom);
        
        east.add(ncount);
        east.add(seedpanel);
        east.add(modnamepanel);
        east.add(startButton);
        east.add(pauseButton);
     
        panel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
        //panel.add(progress, BorderLayout.SOUTH);
        panel.add(sp, BorderLayout.WEST);
        panel.add(east, BorderLayout.CENTER);
        
        // Options
        tabs.addTab("Options", options);
        
        // Descriptions
        JPanel descs = new JPanel(new GridLayout(2, 1));
        descs.add(advDesc);
        descs.add(basicDesc);
        descs.add(preview);
        descs.add(hideVanillaNations);
        this.hideVanillaNations.addItemListener(this);

        options.add(descs, BorderLayout.NORTH);

        // Seeds
        JPanel predefinedSeeds = new JPanel(new BorderLayout(5,5));
        seedcheckbox.addItemListener(this);
        this.seeds.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.black));
        predefinedSeeds.add(seedcheckbox, BorderLayout.NORTH);
        predefinedSeeds.add(this.seeds, BorderLayout.CENTER);
        this.seeds.setEnabled(false);
        options.add(predefinedSeeds);
        
        // Advanced options
        tabs.addTab("Advanced options", advoptions);
        advoptions.add(new JLabel("WARNING! Changing these options changes the setting code. Seeds produce same nations only under the same setting code and program version."));

        JPanel scodep = new JPanel(new GridLayout(1,2));
        settingtext.setText("" + settings.getSettingInteger());
        settingtext.addActionListener(this);

        settingtext.getDocument().addDocumentListener(new DocumentListener() 
        {
        	  
            @Override
            public void changedUpdate(DocumentEvent e) {}

            @Override
            public void insertUpdate(DocumentEvent arg0) 
            {
                handleUpdate();
            }
            @Override
            public void removeUpdate(DocumentEvent arg0) 
            {
                handleUpdate();
            }
			
            private void handleUpdate()
            {
                // Change color when non-numeric settingtext
                if(Generic.isNumeric(settingtext.getText()))
                {
                    settingtext.setForeground(Color.BLACK);
                }
                if(!Generic.isNumeric(settingtext.getText()))
                {
                    settingtext.setForeground(Color.RED);
                }
                if(settingtext.getText().length() > 0 && Generic.isNumeric(settingtext.getText()))
                {
                    int i = Integer.parseInt(settingtext.getText());
                    settings.setSettingInteger(i);
                    updateAdvancedSettings();
                }				
            }
        });
        
        scodep.add(new JLabel("Setting code:"));
        scodep.add(this.settingtext);
        advoptions.add(scodep);
        
        // Era
        JPanel era = new JPanel(new GridLayout(1,2));
        eraSlider.addChangeListener(this);
        Hashtable<Integer, JLabel> eraLabelTable = new Hashtable<>();
        eraLabelTable.put(1, new JLabel("Early"));
        eraLabelTable.put(2, new JLabel("Middle"));
        eraLabelTable.put(3, new JLabel("Late"));
        eraSlider.setLabelTable(eraLabelTable);
        eraSlider.setPaintLabels(true);

        era.add(new JLabel("Era:"));
        era.add(eraSlider);
        
        advoptions.add(era);
        
        // Sacred power
        JPanel spower = new JPanel(new GridLayout(1,2));
        spowerSlider.addChangeListener(this);
        Hashtable<Integer, JLabel> spowerLabelTable = new Hashtable<>();
        spowerLabelTable.put(1, new JLabel("Normal"));
        spowerLabelTable.put(2, new JLabel("High"));
        spowerLabelTable.put(3, new JLabel("Batshit Insane"));
        spowerSlider.setLabelTable(spowerLabelTable);
        spowerSlider.setPaintLabels(true);
        
        updateAdvancedSettings();
        
        spower.add(new JLabel("Sacred Power:"));
        spower.add(spowerSlider);
        
        advoptions.add(spower);
        
        // Restrictions
        tabs.addTab("Nation restrictions", rpanel);
        add(tabs);

        pack();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
		
        System.out.println("Dominions 5 NationGen version " + NationGen.version + " (" + NationGen.date + ")");
        System.out.println("------------------------------------------------------------------");
    }
    
    
    private void updateAdvancedSettings()
    {
    	this.eraSlider.setValue(settings.get(SettingsType.era).intValue());
    	this.spowerSlider.setValue(settings.get(SettingsType.sacredpower).intValue());

    }
    
    boolean hasRun = false;
    public GUI() {
        setTitle("NationGen GUI");
        this.setPreferredSize(new Dimension(1000, 450));
        this.setResizable(true);
        initGUI();
    	
    	if(this.settings.get(SettingsType.drawPreview) == 1.0)
        {
            preview.setSelected(true);
        }
    	if(this.settings.get(SettingsType.advancedDescs) == 1.0)
        {
            advDesc.setSelected(true);
        }
    	if(this.settings.get(SettingsType.basicDescs) == 1.0)
        {
            basicDesc.setSelected(true);
        }
    	if(this.settings.get(SettingsType.hidevanillanations) == 1.0)
        {
            hideVanillaNations.setSelected(true);
        }
    	this.eraSlider.setValue((int)Math.round(this.settings.get(SettingsType.era)));
     }
    
    private void updateTextPane(final String text) 
    {
        SwingUtilities.invokeLater(() -> 
        {
            Document doc = textPane.getDocument();
            try
            {
                doc.insertString(doc.getLength(), text, null);
            }
            catch (BadLocationException e)
            {
                throw new RuntimeException(e);
            }
            textPane.setCaretPosition(doc.getLength() - 1);
        });
    }
    
    private List<Long> parseSeeds()
    {
    	List<Long> l = new ArrayList<>();
    	String text = seeds.getText();
    	
    	String[] parts = text.split(",");
    	for(String str : parts)
    	{
            String[] parts2 = str.split("\n");
            for(String str2 : parts2)
            {
                if(Generic.isNumeric(str2.trim()))
                {
                    l.add(Long.parseLong(str2.trim()));
                }
            }
    	}
    	return l;
    }
    
    private void redirectSystemStreams() 
    {
        OutputStream out = new OutputStream() 
        {
    	    @Override
    	    public void write(final int b) throws IOException 
            {
                updateTextPane(String.valueOf((char) b));
    	    }
    	 
    	    @Override
    	    public void write(byte[] b, int off, int len) throws IOException 
            {
                updateTextPane(new String(b, off, len));
    	    }
    	 
    	    @Override
    	    public void write(byte[] b) throws IOException 
            {
                write(b, 0, b.length);
    	    }
        };
    	 
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }


    private void process()
    {
    	try 
        {
            n = new NationGen(pauseLock, settings, rpanel.getRestrictions());
        } 
        catch (IOException e) 
        {
            System.out.println("Error initializing NationGen.");
            startButton.setEnabled(false);
            return;
        }
        Thread thread = new Thread() 
        {
            @Override
            public void run() 
            {
                startButton.setText(abortText);
                pauseButton.setEnabled(true);
                
                if(!modNameRandom.isSelected())
                {
                    n.modname = modname.getText();
                }
                try 
                {
                    if(seedcheckbox.isSelected())
                    {
                        n.generate(parseSeeds());
                    }
                    else
                    {
                        if(!seedRandom.isSelected())
                        {
                            n.generate(Integer.parseInt(amount.getText()), Long.parseLong(seed.getText()));
                        }
                        else
                        {
                            n.generate(Integer.parseInt(amount.getText()));
                        }
                    }
                }
                catch (NumberFormatException | IOException e) 
                {
                    e.printStackTrace();
                }
                finally
                {
                    // Reset start button to default state.
                    startButton.setText(startText);
                    startButton.setEnabled(true);
                    
                    // Reset pause button to default state.
                    pauseButton.setEnabled(false);
                    pauseButton.setText(pauseText);
                }
            }
        };
        thread.start();
        hasRun = true;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) 
    {
        if(e.getSource().equals(startButton))
        {
            if (startButton.getText().equals(startText))
            {
                if(modname.getText().length() == 0)
                {
                    System.out.println("Please enter a mod name.");
                    //modNameRandom.setSelected(true);
                    return;
                }
    
                if(!seedcheckbox.isSelected())
                {
                        if(!(Generic.isNumeric(seed.getText()) || seed.getText().equals("Random")))
                        {
                                System.out.println("Please enter a numeric seed.");
                                //seedRandom.setSelected(true);
                                return;
                        }
    
                        if(!Generic.isNumeric(amount.getText()) || Integer.parseInt(amount.getText()) < 1)
                        {
                        System.out.println("Please enter a numeric nation amount.");
                        return;
                        }
                }
                else if(this.seedcheckbox.isSelected() && parseSeeds().isEmpty())
                {
                        System.out.println("Please specify numeric seeds or disable predefined seeds.");
                        return;
                }
                settingtext.setText(settings.getSettingInteger() + "");
                process();
            }
            else /*if (startButton.getText().equals(abortText)) */
            {
                // Abort! But, we must unpause before we abort, else the other thread will be RIP
                if (pauseLock.isHeldByCurrentThread())
                {
                    pauseLock.unlock();
                }
                n.abortNationGeneration();
            }
        }
        if(e.getSource().equals(settingtext))
        {
            settingtext.setText(settings.getSettingInteger() + "");
        }
        if(e.getSource().equals(pauseButton))
        {
            if (pauseButton.getText().equals(pauseText))
            {
                pauseButton.setText(unpauseText);
                pauseLock.lock();
            }
            else
            {
                pauseButton.setText(pauseText);
                pauseLock.unlock();
            }
        }
    }
	
    @Override
    public void itemStateChanged(ItemEvent e)
    {
        Object source = e.getItemSelectable();

        // Main screen settings
        JTextArea target = null;
        if(source == this.modNameRandom)
        {
            target = this.modname;
        }
        else if(source == this.seedRandom)
        {
            target = this.seed;
        }

        if(target != null)
        {
            if (e.getStateChange() == ItemEvent.DESELECTED) 
            {
                target.setEnabled(true);
                if(target == this.seed)
                {
                    Random r = new Random();
                    target.setText(r.nextInt() + "");
                }
            }
            else if (e.getStateChange() == ItemEvent.SELECTED) 
            {
                target.setEnabled(false);
                target.setText("Random");
            }
        }

        // Options settings
        if(this.optionChecks.contains(source))
        {
            double value = 0;
            if(e.getStateChange() == ItemEvent.SELECTED) 
            {
                value = 1;
            }
            if(source == this.preview)
            {
                settings.put(SettingsType.drawPreview, value);
            }
            if(source == this.advDesc)
            {
                settings.put(SettingsType.advancedDescs, value);
            }
            if(source == this.basicDesc)
            {
                settings.put(SettingsType.basicDescs, value);
            }
            if(source == this.hideVanillaNations)
            {
                settings.put(SettingsType.hidevanillanations, value);
            }
            if(source == this.seedcheckbox)
            {
                this.seeds.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
                this.seedRandom.setSelected(true);
                this.seedRandom.setEnabled(e.getStateChange() != ItemEvent.SELECTED);
                this.amount.setEnabled(e.getStateChange() != ItemEvent.SELECTED);
            }
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) 
    {
        Object source = e.getSource();
        if(source == this.eraSlider && eraSlider.getValueIsAdjusting())
        {
            settings.put(SettingsType.era, eraSlider.getValue());
            settingtext.setText(settings.getSettingInteger() + "");
        }
        if(source == this.spowerSlider && spowerSlider.getValueIsAdjusting())
        {
            settings.put(SettingsType.sacredpower, spowerSlider.getValue());
            settingtext.setText(settings.getSettingInteger() + "");
        }
    }
}