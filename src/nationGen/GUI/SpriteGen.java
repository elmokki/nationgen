package nationGen.GUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.elmokki.Drawing;
import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.units.Unit;

public class SpriteGen extends JFrame implements ActionListener, ItemListener{

	private static final long serialVersionUID = 1L;

	private class SGEntry implements ActionListener, ItemListener {
		JTextField offsetx = new JTextField("0");
		JTextField offsety = new JTextField("0");
		JComboBox<Item> items = new JComboBox<Item>();
		JLabel lbl = new JLabel();
		private SpriteGen sGen;


		public SGEntry(String slot, SpriteGen sGen)
		{
			lbl.setText(slot);
			this.sGen = sGen;
			
			offsetx.addActionListener(this);
			offsety.addActionListener(this);
			items.addItemListener(this);

		}


		@Override
		public void actionPerformed(ActionEvent e) {
			
			if(sGen.unit.getSlot(this.lbl.getText()) != null && Generic.isNumeric(((JTextField)e.getSource()).getText()))
			{	
				if(e.getSource() == offsetx)
					sGen.unit.getSlot(this.lbl.getText()).setOffsetX(Integer.parseInt(((JTextField)e.getSource()).getText()));
				if(e.getSource() == offsety)
					sGen.unit.getSlot(this.lbl.getText()).setOffsetY(Integer.parseInt(((JTextField)e.getSource()).getText()));
				
				sGen.drawUnit();
			}	
			
		}


		@Override
		public void itemStateChanged(ItemEvent e) {
			
			if(e.getStateChange() == ItemEvent.SELECTED)
			{
				Item i = (Item)this.items.getSelectedItem();
			    this.offsetx.setText(i.getOffsetX() + "");
			    this.offsety.setText(i.getOffsetY() + "");
			    
			    if(i != sGen.dummy)
			    	sGen.unit.setSlot(this.lbl.getText(), i);
			    else
			    	sGen.unit.setSlot(this.lbl.getText(), null);

			    sGen.drawUnit();
			}
		}


	
	}
	
	
	NationGen nGen;
    JComboBox<Race> racecombo = new JComboBox<Race>();
    JComboBox<Pose> posecombo = new JComboBox<Pose>();
    List<SGEntry> slots = new ArrayList<SGEntry>();
    Unit unit = null;
    JPanel itempanel = new JPanel(new GridLayout(20,1));
    JPanel drawpanel = new JPanel();
	Item dummy;
    JTextField greenf = new JTextField("255");
    JTextField redf = new JTextField("255");
    JTextField bluef = new JTextField("255");
    JTextField filename = new JTextField("derp.tga");

	public static void main(String[] args)
	{
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SpriteGen g;
				try {
					g = new SpriteGen();
	                g.setVisible(true);
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
        });
	}
	
    public SpriteGen() throws IOException {
    	nGen = new NationGen();
    	dummy = new Item(nGen);
    	
        setTitle("SpriteGen");
        this.setPreferredSize(new Dimension(1000, 800));
        this.setResizable(false);

        initGUI();
        pack();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
     }
	

    public void drawUnit()
    {

    	
    	System.out.println("Drawing.");

    	Graphics g = drawpanel.getGraphics();
    	
    	if(unit == null || g == null)
    		return;
    	
    	try {
			g.drawImage(unit.render(), 0, 0, null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }

    public void initGUI()
    {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        
        // Race
        JPanel racepanel = new JPanel(new BorderLayout(5,5));
        JLabel racelbl = new JLabel("Race");
        racepanel.add(racelbl, BorderLayout.NORTH);
        for(Race r : nGen.races)
        	racecombo.addItem(r);
        racecombo.addItemListener(this);
        racepanel.add(racecombo, BorderLayout.SOUTH);
        
        
        // Pose
        JPanel posepanel = new JPanel(new BorderLayout(5,5));
        JLabel poselbl = new JLabel("Pose");
        posepanel.add(poselbl, BorderLayout.NORTH);
        Race r = (Race)racecombo.getSelectedItem();
        for(Pose p : r.poses)
      		posecombo.addItem(p);
        posecombo.addItemListener(this);
        posepanel.add(posecombo, BorderLayout.SOUTH);
        
        this.unit = new Unit(nGen, (Race)racecombo.getSelectedItem(), (Pose)posecombo.getSelectedItem());

        // Colors
        JPanel colorpanel = new JPanel(new GridLayout(3,2));
        colorpanel.add(new JLabel("Red"));
        redf.addActionListener(this);
        colorpanel.add(redf);
        
        colorpanel.add(new JLabel("Green"));
        greenf.addActionListener(this);
        colorpanel.add(greenf);

        colorpanel.add(new JLabel("Blue"));
        bluef.addActionListener(this);
        colorpanel.add(bluef);
        

        
        // Combine race, colors and  pose
        JPanel toppanel = new JPanel(new BorderLayout(5,5));
        
        JPanel raceposepanel = new JPanel(new BorderLayout(5,5));
        raceposepanel.add(racepanel, BorderLayout.WEST);
        raceposepanel.add(posepanel, BorderLayout.EAST);
        toppanel.add(raceposepanel, BorderLayout.EAST);
        toppanel.add(colorpanel, BorderLayout.WEST);
        panel.add(toppanel, BorderLayout.NORTH);
        
        // Items
        updateItems();
        panel.add(itempanel, BorderLayout.EAST);
        
        panel.add(drawpanel, BorderLayout.CENTER);
        
        
        // Saving box and button
        JLabel lbl = new JLabel("Filename:");
        JButton derp = new JButton("Save to .tga");
        derp.addActionListener(this);
        
        JPanel bottompanel = new JPanel(new GridLayout(1,3));
        bottompanel.add(lbl);
        bottompanel.add(filename);
        bottompanel.add(derp);
        
        panel.add(bottompanel, BorderLayout.SOUTH);
        
        


        add(panel);
    }
    
    private Color getColor()
    {
    	int r = 255;
    	int g = 255;
    	int b = 255;
    	if(Generic.isNumeric(redf.getText()))
    		r = Integer.parseInt(redf.getText());
    	if(Generic.isNumeric(greenf.getText()))
    		g = Integer.parseInt(greenf.getText());
    	if(Generic.isNumeric(bluef.getText()))
    		b = Integer.parseInt(bluef.getText());
    	
    	return new Color(r, g, b);
    }
    
    private void updateItems()
    {
    	Pose p = (Pose)posecombo.getSelectedItem();
    	if(p == null)
    		return;
    	
    	itempanel.removeAll();
    	slots.clear();
    	
    	for(String str : p.getListOfSlots())
    	{
    		SGEntry se = new SGEntry(str, this);
    		
    		dummy.name = "nothing";
    		se.items.addItem(dummy);
    		
    		for(Item i : p.getItems(str))
    			se.items.addItem(i);
    		
    		slots.add(se);
    	}
    	
    	
        JPanel tp = new JPanel(new GridLayout(1,4));
        tp.add(new JLabel("Slot"));
        tp.add(new JLabel("Item"));
        tp.add(new JLabel("X"));
        tp.add(new JLabel("Y"));
        itempanel.add(tp);
        
        for(SGEntry se : slots)
        {
            JPanel tp2 = new JPanel(new GridLayout(1,4));
        	tp2.add(se.lbl);
        	tp2.add(se.items);
        	tp2.add(se.offsetx);
        	tp2.add(se.offsety);
        	itempanel.add(tp2);

        }
        
        this.validate();
        this.repaint();
    	
 
    }
    
	@Override
	public void itemStateChanged(ItemEvent e) {
		
	    Object source = e.getItemSelectable();
	  

		if(source == this.racecombo)
		{
			this.posecombo.removeAllItems();
			for(Pose p : ((Race)racecombo.getSelectedItem()).poses)
				posecombo.addItem(p);
		}
		if(source == this.posecombo)
		{
			updateItems();
			
		}
		
		if(racecombo.getSelectedItem() != null && posecombo.getSelectedItem() != null)
		{
			this.unit = new Unit(nGen, (Race)racecombo.getSelectedItem(), (Pose)posecombo.getSelectedItem());
			this.unit.color = this.getColor();
		}

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		
		if(source == this.redf || source == this.bluef || source == this.greenf)
		{
			if(this.unit != null)
			{
				unit.color = getColor();
				drawUnit();
			}
		
		}
		else // save
		{
			try {
				Drawing.writeTGA(unit.render(), filename.getText());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
	}

}
