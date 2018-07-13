package nationGen;

import java.io.IOException;

import nationGen.magic.MagicPattern;
import nationGen.misc.ResourceStorage;

/**
 * This class will mirror the constant elements in Nationgen.java but eventually, it'll replace it.
 * This will allow me to incrementally replace files and allow easier testing. 
 * For simplicity of refactoring, they'll start public. But, eventually, better accessors will be made.
 * @author flash-fire
 *
 */
public class NationGenAssets
{
    public ResourceStorage<MagicPattern> patterns;
    
    public NationGenAssets(NationGen gen)
    {
        patterns = new ResourceStorage<>(MagicPattern.class, gen);
        
        Init();
    }
    
    public void Init()
    {
        try
        {
            patterns.load("./data/magic/magicpatterns.txt");
        } catch (IOException e)
        {
            e.printStackTrace();
            System.out.println("Error loading file " + e.getMessage());
        }
    }
}
