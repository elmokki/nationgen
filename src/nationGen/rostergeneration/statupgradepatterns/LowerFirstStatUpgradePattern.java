package nationGen.rostergeneration.statupgradepatterns;

// Chance of stat upgrade is much lower the higher the cost of the upgrade.
// Lower stat upgrades have much higher chance of getting picked.
public class LowerFirstStatUpgradePattern extends StatUpgradePattern {
    public LowerFirstStatUpgradePattern() {}

    @Override
    public double calculateStatUpgradeWeight(double statUpgradeCost) {
        return 1 / (Math.pow(statUpgradeCost, 2));
    }
}
