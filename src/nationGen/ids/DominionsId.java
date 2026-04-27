package nationGen.ids;

/**
 * This class is an encapsulation of the two ids that a generated item 
 * 
 * A "Dominions id" is an id above 0 which can or must be written into a mod command, such as #armor
 * or #weapon for items, or #newmonster for creatures. In this class, that is the ingameId.
 * 
 * A "NationGen id" is an id internal to the NationGen program used to identify entities which may
 * or may not eventually get a Dominions id assigned.
 * 
 * For example, a "basesprite" item does not have a Dominions id, but still has a NationGen id (the
 * Entity.name property, usually). A vanilla Dominions item that is used within NationGen will have
 * both a NationGen id (for the defined item that a pose equips), and a Dominions id (11). A custom
 * item, such as a new armor or weapon that does not exist in vanilla Dominions but is
 * defined/generated in NationGen, will have a NationGen id, and eventually will have a Dominions id
 * assigned to it.
 * 
 * Note that "Item" is a misleading word in NationGen, since it can refer both to
 * equipped Dominions items as well as to cosmetic sprite parts, such as "hands" or "shadow".
 */
public class DominionsId {
    private String nationGenId = "";
    private int ingameId = -1;

    public DominionsId() {}

    public DominionsId(String nationGenId) {
        this.nationGenId = nationGenId;
    }

    public DominionsId(int ingameId) {
        this.ingameId = ingameId;
    }

    public String getNationGenId() {
        return this.nationGenId;
    }

    public DominionsId setNationGenId(String name) {
        this.nationGenId = name;
        return this;
    }

    public int getIngameId() {
        return this.ingameId;
    }

    public DominionsId setIngameId(int id) {
        this.ingameId = id;
        return this;
    }

    public boolean isBlank() {
        return this.nationGenId.isBlank() && this.ingameId == -1;
    }

    public boolean isResolved() {
        return this.ingameId > 0;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        if (other.getClass() != this.getClass()) {
            return false;
        }
        
        DominionsId parsed = (DominionsId)other;

        if (!this.getNationGenId().equals(parsed.getNationGenId())) {
            return false;
        }

        if (this.getIngameId() != parsed.getIngameId()) {
            return false;
        }

        return true;
    }
}
