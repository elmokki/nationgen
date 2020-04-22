package nationGen;


import com.elmokki.Dom3DB;
import nationGen.items.CustomItem;
import nationGen.misc.Arg;
import nationGen.misc.Command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * 
 * This class exists to clean up the custom items from NationGen.java. 
 * Long term, the Dom3DB stuff should probably get culled. Perhaps with its own separate class. We'll see.
 * However, with them as they are now, these changes let this class be not dependent on Nationgen.
 * @author FlashFire
 *
 */
public class CustomItemsHandler
{
    private List<CustomItem> chosenCustomItems; // List of items used by natgen in current runthrough. 
    private List<CustomItem> customItems; // Superset of chosenCustomItems. Has all generated custom items + some which are cached.
    private IdHandler idHandler; // Local ref of id handler to generate new IDs when fresh custom item needs a new ID.
    private Dom3DB weapondb; // Local ref of weapondb to remove nationgen dependency.
    private Dom3DB armordb; // Local ref of armordb to remove nationgen dependency.
    
    public CustomItemsHandler(List<CustomItem> existingItems, Dom3DB weapondb, Dom3DB armordb)
    {
        chosenCustomItems = new ArrayList<>();
        customItems = existingItems;
        idHandler = null;
        this.weapondb = weapondb;
        this.armordb = armordb;
        
        PopulateDB();
    }
    
    /**
     * This populates the DB with predefined custom items.
     */
    private void PopulateDB()
    {
        customItems.forEach((CustomItem ci) -> 
        {
            if (ci.armor) 
            {
                armordb.addToMap(ci.name, ci.getHashMap());
            } 
            else 
            {
                weapondb.addToMap(ci.name, ci.getHashMap());
            }
        });
    }
    
    /**
     * Returns given custom item.  Can't be contained in chosenCustomItems.
     * @param name: Name of the custom item
     * @return the custom item
     */
    public Optional<CustomItem> getCustomItem(String name)
    {
        return customItems.stream()
            .filter(ci -> ci.name.equals(name))
            .filter(ci -> !chosenCustomItems.contains(ci))
            .findFirst();
    }
    
    /**
     *  The idea is that we first see if the name is already chosen, and return its chosen ID if so.
     *  Else, if the item exists in the superset [customItems], we generate its ID & add it to chosen items.
     */
    public String getCustomItemId(String name)
    {
        for(CustomItem ci : chosenCustomItems)
        {
            if(ci.name.equals(name))
            {
                return ci.id;
            }  
        }

        CustomItem citem = null;
        for(CustomItem ci : customItems)
        {
            if(ci.name.equals(name) && !chosenCustomItems.contains(ci))
            {
                citem = ci.getCopy();
                break;
            }
        }

        if(citem == null)
        {
            System.out.println("WARNING: No custom item named " + name + " was found!");
            return "-1";
        }
        
        if(idHandler != null)
        {
            if(citem.armor)
            {
                citem.id = idHandler.nextArmorId() + "";
            }
            else
            {
                citem.id = idHandler.nextWeaponId() + "";
            }
        }
        else
        {
            System.out.println("ERROR: idHandler was not initialized!");
            citem.id = "-1";
        }
        
        // -521978361
        // Check references!
        for(Command command : citem.values)
        {
            if(command.command.equals("#secondaryeffect") || command.command.equals("#secondaryeffectalways"))
            {
                Arg customItemSecondaryEffect = command.args.get(0);
                if (!customItemSecondaryEffect.isNumeric())
                {
                    String id = getCustomItemId(customItemSecondaryEffect.get());
                    command.args.set(0, new Arg(id));
                }
            }
        }
        
        chosenCustomItems.add(citem);
        //this.customitems.remove(citem);

        if(!citem.armor)
        {
            weapondb.addToMap(citem.id, citem.getHashMap());
        }
        else
        {
            armordb.addToMap(citem.id, citem.getHashMap());
        }

        return citem.id;
    }
    
    /**
     * Add a new custom item.
     */
    public void AddCustomItem(CustomItem item)
    {
        customItems.add(item);
    }
    
    /**
     * Updates ID handler to match current handler. This *must* be called before other functions are used.
     * Ideally, this would just be passed in constructor and never changed per instance. C'est la vie.
     */
    public void UpdateIDHandler(IdHandler handler)
    {
        idHandler = handler;
    }
    
    /**
     * Generates the mod lines for the currently chosen custom items.
     */
    public List<String> writeCustomItemsLines()
    {
        List<String> lines = new ArrayList<>();
        if(!chosenCustomItems.isEmpty())
        {
            lines.add("--- Generic custom items:");
            for(CustomItem ci : chosenCustomItems)
            {
                lines.addAll(ci.writeLines());
            }
        }
        return lines;
    }
}
