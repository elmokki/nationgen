package nationGen.rostergeneration.statupgradepatterns;

// Chance of stat upgrade is lower the higher the cost of the upgrade.
public class BalancedStatUpgradePattern extends StatUpgradePattern {
    public BalancedStatUpgradePattern() {}

    @Override
    public double calculateStatUpgradeWeight(double statUpgradeCost) {
        return 1 / statUpgradeCost;
    }
}
