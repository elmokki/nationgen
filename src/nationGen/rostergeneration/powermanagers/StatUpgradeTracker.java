package nationGen.rostergeneration.powermanagers;

import nationGen.rostergeneration.statupgradepatterns.StatUpgradePattern;

class StatUpgradeTracker {
    public final int defaultValue;
    public final int upgradeStepSize;
    public final int maxValue;
    public final int maxNumberOfUpgrades;

    private int currentValue = 0;
    private int timesUpgraded = 0;
    private boolean isLowerStatBetter = false;
    private StatUpgradePattern statUpgradePattern;

    public StatUpgradeTracker(int currentValue, int defaultValue, int upgradeStepSize, int maxUpgradeValue, int maxNumberOfUpgrades, StatUpgradePattern statUpgradePattern) {
        this.currentValue = currentValue;
        this.defaultValue = defaultValue;
        this.upgradeStepSize = upgradeStepSize;
        this.maxValue = maxUpgradeValue;
        this.maxNumberOfUpgrades = maxNumberOfUpgrades;
        this.statUpgradePattern = statUpgradePattern;

        if (upgradeStepSize < 0) {
            isLowerStatBetter = true;
        }
    }

    public int getCurrentValue() {
        return this.currentValue;
    }

    public void addUpgrade(int modifier) {
        this.timesUpgraded++;
        this.currentValue += modifier;
    }

    public int getTimesUpgraded() {
        return this.timesUpgraded;
    }

    public boolean hasReachedStatUpgradeLimit(int currentUnitValue) {
        int valueAfterUpgrade = currentUnitValue + this.upgradeStepSize;

        if (this.isLowerStatBetter) {
            return valueAfterUpgrade < this.maxValue;
        }

        else {
            return valueAfterUpgrade > this.maxValue;
        }
    }
    
    public boolean hasReachedMaxNumberOfUpgrades() {
        return this.timesUpgraded >= this.maxNumberOfUpgrades;
    }

    public double calculateUpgradePrice(int currentValue) {
        double price = 1;
        double divisor = 2;
        double dif = currentValue - this.defaultValue;

        // If a lower value of this stat is better (like encumbrance),
        // then flip the difference between current value and default
        if (this.isLowerStatBetter) {
            dif = -dif;
        }

        // If stat is higher than the default, increase price
        if (dif > 0) {
            // If stat is 3 points higher than default, price is 3.25.
            // If stat is 6 points higher than default, price is 10
            price *= (1 + Math.pow(dif / divisor, 2));
        }

        // Else if stat is lower than the default, reduce price
        else if (dif < 0) {
            // If stat is 3 points lower than default, price is 0.85.
            // If stat is 6 points lower than default, price is 0.828 (never below 0.8)
            price *= 0.8 + 0.2 / (1 + Math.abs(dif));
        }

        return price;
    }

    // Chance of this upgrade being picked over others depends on its cost
    // The higher the cost, the lower the chance of this stat being upgraded
    public double calculateUpgradeChance(double upgradeCost) {
        return statUpgradePattern.calculateStatUpgradeWeight(upgradeCost);
    }
}
