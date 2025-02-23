package nationGen.rostergeneration.statupgradepatterns;

// Cost of upgrades does not matter for the chance of the stat getting upgraded.
public class RandomStatUpgradePattern extends StatUpgradePattern {
    public RandomStatUpgradePattern() {}

    @Override
    public double calculateStatUpgradeWeight(double statUpgradeCost) {
        return 1;
    }
}
