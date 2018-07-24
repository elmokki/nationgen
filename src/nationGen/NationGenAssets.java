package nationGen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nationGen.entities.Filter;
import nationGen.entities.Flag;
import nationGen.entities.MagicItem;
import nationGen.entities.Pose;
import nationGen.entities.Theme;
import nationGen.items.Item;
import nationGen.magic.MagicPattern;
import nationGen.misc.ResourceStorage;
import nationGen.naming.NamePart;
import nationGen.units.ShapeShift;

/**
 * This class will mirror the constant elements in Nationgen.java but eventually, it'll replace it.
 * This will allow me to incrementally replace files and allow easier testing. 
 * For simplicity of refactoring, they'll start public. But, eventually, better accessors will be made.
 * Ideally, NationGen would NOT be depended upon at all. That's the end goal.  But, one step at a time.
 * @author flash-fire
 *
 */
public class NationGenAssets
{
    public ResourceStorage<MagicPattern> patterns;
    
    public ResourceStorage<Filter> templates;
    public ResourceStorage<Filter> descriptions;
    public ResourceStorage<ShapeShift> monsters;
    public ResourceStorage<Pose> poses;
    public ResourceStorage<Theme> themes;
    public ResourceStorage<Filter> spells;
    public List<Filter> customspells;
    public ResourceStorage<NamePart> magenames;
    public ResourceStorage<NamePart> miscnames;
    public ResourceStorage<Filter> miscdef;
    public ResourceStorage<Flag> flagparts;
    public ResourceStorage<MagicItem> magicitems;
    
    public NationGenAssets(NationGen gen)
    {
        patterns = new ResourceStorage<>(MagicPattern.class, gen);
        templates = new ResourceStorage<>(Filter.class, gen);
        descriptions = new ResourceStorage<>(Filter.class, gen);
        monsters = new ResourceStorage<>(ShapeShift.class, gen);
        poses = new ResourceStorage<>(Pose.class, gen);
        themes = new ResourceStorage<>(Theme.class, gen);
        spells = new ResourceStorage<>(Filter.class, gen);
        customspells = new ArrayList<>();
        magenames = new ResourceStorage<>(NamePart.class, gen);
        miscnames = new ResourceStorage<>(NamePart.class, gen);
        miscdef = new ResourceStorage<>(Filter.class, gen);
        flagparts = new ResourceStorage<>(Flag.class, gen);
        magicitems = new ResourceStorage<>(MagicItem.class, gen);

        Init(gen);
    }
    
    public void Init(NationGen gen)
    {
        try
        {
            patterns.load("./data/magic/magicpatterns.txt");
            templates.load("./data/templates/templates.txt");
            descriptions.load("./data/descriptions/descriptions.txt");
            monsters.load("./data/monsters/monsters.txt");
            poses.load("./data/poses/poses.txt");
            themes.load("./data/themes/themes.txt");
            spells.load("./data/spells/spells.txt");
            customspells.addAll(Item.readFile(gen, "./data/spells/custom_spells.txt", Filter.class)); // ugh why you break pattern?
            
            magenames.load("./data/names/magenames/magenames.txt");
            miscnames.load("./data/names/naming.txt");
            miscdef.load("./data/misc/miscdef.txt");
            flagparts.load("./data/flags/flagdef.txt");
            magicitems.load("./data/items/magicweapons.txt");
        } catch (IOException e)
        {
            e.printStackTrace();
            System.out.println("Error loading file " + e.getMessage());
        }
    }
}
