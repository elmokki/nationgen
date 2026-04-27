package nationGen.units;

/**
 * Encapsulates the different types of costs that a Dominions Unit can have.
 */
public class UnitCost {
    public final int gold;
    public final int resources;
    public final int recPoints;

    public UnitCost(int goldCost, int resourceCost, int recPointCost) {
        if (goldCost < 0) {
            throw new IllegalArgumentException("Unit's gold cost cannot be negative!");
        }

        if (resourceCost < 0) {
            throw new IllegalArgumentException("Unit's resource cost cannot be negative!");
        }

        if (recPointCost < 0) {
            throw new IllegalArgumentException("Unit's recruitment points cost cannot be negative!");
        }

        this.gold = goldCost;
        this.resources = resourceCost;
        this.recPoints = recPointCost;
    }

    public static UnitCost zero() {
        return new UnitCost(0, 0, 0);
    }
}
