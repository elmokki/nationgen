package nationGen;

import com.elmokki.Generic;
import nationGen.entities.Entity;
import nationGen.entities.Filter;
import nationGen.entities.Flag;
import nationGen.entities.MagicItem;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.entities.Theme;
import nationGen.magic.MagicPattern;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.FileUtil;
import nationGen.misc.ResourceStorage;
import nationGen.naming.NamePart;
import nationGen.units.ShapeShift;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class will mirror the constant elements in Nationgen.java but eventually, it'll replace it.
 * This will allow me to incrementally replace files and allow easier testing. 
 * For simplicity of refactoring, they'll start public. But, eventually, better accessors will be made.
 * Ideally, NationGen would NOT be depended upon at all. That's the end goal.  But, one step at a time.
 * @author flash-fire
 *
 */
public class NationGenAssets {
    public ResourceStorage<MagicPattern> patterns = new ResourceStorage<>(MagicPattern.class);
    
    public ResourceStorage<Filter> templates = new ResourceStorage<>(Filter.class);
    public ResourceStorage<Filter> descriptions = new ResourceStorage<>(Filter.class);
    public List<Race> races = new ArrayList<>();
    public ResourceStorage<ShapeShift> monsters = new ResourceStorage<>(ShapeShift.class);
    public ResourceStorage<Pose> poses = new ResourceStorage<>(Pose.class);
    public ResourceStorage<Filter> filters = new ResourceStorage<>(Filter.class);
    public ResourceStorage<Theme> themes = new ResourceStorage<>(Theme.class);
    public ResourceStorage<Filter> spells = new ResourceStorage<>(Filter.class);
    public List<Filter> customspells = new ArrayList<>();
    public ResourceStorage<NamePart> magenames = new ResourceStorage<>(NamePart.class);
    public ResourceStorage<NamePart> miscnames = new ResourceStorage<>(NamePart.class);
    public ResourceStorage<Filter> miscdef = new ResourceStorage<>(Filter.class);
    public ResourceStorage<Flag> flagparts = new ResourceStorage<>(Flag.class);
    public ResourceStorage<MagicItem> magicitems = new ResourceStorage<>(MagicItem.class);
    
    public List<ShapeShift> secondshapes = new ArrayList<>();
    public List<String> secondShapeMountCommands = new ArrayList<>();
    public List<String> secondShapeNonMountCommands = new ArrayList<>();
    public List<String> secondShapeRacePoseCommands = new ArrayList<>();
    
    public void load(NationGen gen)
    {
        patterns.load(gen, "/data/magic/magicpatterns.txt");
        templates.load(gen, "/data/templates/templates.txt");
        descriptions.load(gen, "/data/descriptions/descriptions.txt");
        monsters.load(gen, "/data/monsters/monsters.txt");
        poses.load(gen, "/data/poses/poses.txt");
        filters.load(gen, "/data/filters/filters.txt");
        themes.load(gen, "/data/themes/themes.txt");
        spells.load(gen, "/data/spells/spells.txt");
        customspells.addAll(Entity.readFile(gen, "/data/spells/custom_spells.txt", Filter.class)); // ugh why you break pattern?
        
        magenames.load(gen, "/data/names/magenames/magenames.txt");
        miscnames.load(gen, "/data/names/naming.txt");
        miscdef.load(gen, "/data/misc/miscdef.txt");
        flagparts.load(gen, "/data/flags/flagdef.txt");
        magicitems.load(gen, "/data/items/magicweapons.txt");
        
        secondshapes = Entity.readFile(gen, "/data/shapes/secondshapes.txt", ShapeShift.class);
        loadSecondShapeInheritance("/data/shapes/secondshapeinheritance.txt");
        loadRaces(gen, "./data/races/races.txt");
        
        initializeAllFilters();
    }
    
    private void loadRaces(NationGen gen, String file)
    {
        for (String strLine : FileUtil.readLines(file))
        {
            List<String> args = Generic.parseArgs(strLine);
            if(args.isEmpty())
            {
                continue;
            }

            if(args.get(0).equals("#load"))
            {
                races.addAll(Entity.readFile(gen, args.get(1), Race.class));
            }
        }
    }
    
    /**
     * Loads the list of commands that second shapes should inherit from the primary shape
     * @param filename
     * @return
     */
    private int loadSecondShapeInheritance(String filename)
    {
        int amount = 0;

        for(String line : FileUtil.readLines(filename))
        {
            if(line.startsWith("-"))
            {
                continue; 
            }

            List<String> args = Generic.parseArgs(line);
            if(args.isEmpty())
            {
                continue;
            }
    
            switch (args.get(0)) {
                case "all":
                    secondShapeMountCommands.add(args.get(1));
                    secondShapeNonMountCommands.add(args.get(1));
                    amount++;
                    break;
                case "mount":
                    secondShapeMountCommands.add(args.get(1));
                    amount++;
                    break;
                case "nonmount":
                    secondShapeNonMountCommands.add(args.get(1));
                    amount++;
                    break;
                case "racepose":
                    secondShapeRacePoseCommands.add(args.get(1));
                    amount++;
                    break;
            }
        }  
        
        return amount;
    }
    
    /**
     * Copies any poses from each race's theme's poses into its poses list
     */
    public void addThemePoses() {
        for(Race race : races) {
            ChanceIncHandler.retrieveFilters("racethemes", "default_racethemes", themes, null, race).stream()
                .flatMap(theme -> theme.nationeffects.stream())
                .filter(command -> "#pose".equals(command.command))
                .map(command -> command.args.get(0).get())
                .distinct()
                .forEach(poseSetName -> {
                    List<Pose> set = poses.get(poseSetName);
                    if (set == null) {
                        throw new IllegalArgumentException("Pose set " + poseSetName + " was not found.");
                    } else {
                        race.poses.addAll(set);
                    }
                });
        }
    }
    
    public void initializeFilters(List<Filter> newFilters)
    {
    	List<Filter> newDescriptions = new ArrayList<>();  
    	
    	for (String s : descriptions.keySet())
    		newDescriptions.addAll(descriptions.get(s));
    	
    	for (Filter f : newFilters)
    	{
    		if (f.tags.containsName("filterdesc"))    			
    			f.description = newDescriptions.stream().filter(pf -> pf.name.equals(f.tags.getString("filterdesc").orElse(""))).findFirst().orElse(f);
    		
    		if (f.tags.containsName("prev"))
    			f.prevDesc = newDescriptions.stream().filter(pf -> pf.descSet.equals(f.tags.getString("prev").orElse(""))).collect(Collectors.toList());
    		
    		if (f.tags.containsName("next"))
    			f.nextDesc = newDescriptions.stream().filter(nf -> nf.descSet.equals(f.tags.getString("next").orElse(""))).collect(Collectors.toList());
    		
    		if (f.tags.containsName("bridge"))
    			f.bridgeDesc = newDescriptions.stream().filter(bf -> bf.descSet.equals(f.tags.getString("bridge").orElse(""))).collect(Collectors.toList());    		
    	}
    }
    
    public void initializeAllFilters()
    {
    	List<Filter> newFilters = new ArrayList<>();

    	for (String s : descriptions.keySet())
    		newFilters.addAll(descriptions.get(s));
    	
    	for (String s : filters.keySet())
    		newFilters.addAll(filters.get(s));
    	
    	for (String s : templates.keySet())
    		newFilters.addAll(templates.get(s)); 
    	
    	initializeFilters(newFilters);
    }
}
