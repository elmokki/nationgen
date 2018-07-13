package nationGen;

import java.io.IOException;

import nationGen.entities.Filter;
import nationGen.magic.MagicPattern;
import nationGen.misc.ResourceStorage;

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
    
    public ResourceStorage<Filter> spells;
    
    public NationGenAssets(NationGen gen)
    {
        patterns = new ResourceStorage<>(MagicPattern.class, gen);
        spells = new ResourceStorage<>(Filter.class, gen);
        
        Init();
    }
    
    public void Init()
    {
        try
        {
            patterns.load("./data/magic/magicpatterns.txt");
            spells.load("./data/spells/spells.txt");
        } catch (IOException e)
        {
            e.printStackTrace();
            System.out.println("Error loading file " + e.getMessage());
        }
    }
}
