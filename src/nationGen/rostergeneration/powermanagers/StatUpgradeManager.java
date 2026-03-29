package nationGen.rostergeneration.powermanagers;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nationGen.NationGen;
import nationGen.Settings.SettingsType;
import nationGen.chances.EntityChances;
import nationGen.misc.Command;
import nationGen.rostergeneration.statupgradepatterns.BalancedStatUpgradePattern;
import nationGen.rostergeneration.statupgradepatterns.LowerFirstStatUpgradePattern;
import nationGen.rostergeneration.statupgradepatterns.RandomStatUpgradePattern;
import nationGen.rostergeneration.statupgradepatterns.StatUpgradePattern;
import nationGen.rostergeneration.statupgradepatterns.StatUpgradePatternEnum;
import nationGen.units.Unit;
import nationGen.entities.Filter;

enum UpgradableStats {
    HP("#hp"),
    ATTACK("#att"),
    DEFENSE("#def"),
    STRENGTH("#str"),
    PRECISION("#prec"),
    ENCUMBRANCE("#enc"),
    MR("#mr");

    private final String name;
    private static final Map<String, UpgradableStats> ENUM_MAP;

    UpgradableStats(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    // Build a map of the enum type values keyed by their string name,
    // so that it is easy to retrieve them by string with the fromString() method
    static {
        Map<String, UpgradableStats> map = new ConcurrentHashMap<String, UpgradableStats>();
        for (UpgradableStats instance : UpgradableStats.values()) {
            map.put(instance.getName().toLowerCase(),instance);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    public static UpgradableStats fromString (String name) {
        return ENUM_MAP.get(name.toLowerCase());
    }

    @Override
    public String toString() {
        return name;
    }
}

class StatUpgradeTracker {
    public final int defaultValue;
    public final int upgradeStepSize;
    public final int maxValue;
    public final int maxNumberOfUpgrades;

    private int timesUpgraded = 0;
    private boolean isLowerStatBetter = false;
    private StatUpgradePattern statUpgradePattern;

    public StatUpgradeTracker(int defaultValue, int upgradeStepSize, int maxUpgradeValue, int maxNumberOfUpgrades, StatUpgradePattern statUpgradePattern) {
        this.defaultValue = defaultValue;
        this.upgradeStepSize = upgradeStepSize;
        this.maxValue = maxUpgradeValue;
        this.maxNumberOfUpgrades = maxNumberOfUpgrades;
        this.statUpgradePattern = statUpgradePattern;

        if (upgradeStepSize < 0) {
            isLowerStatBetter = true;
        }
    }

    public void addUpgrade() {
        this.timesUpgraded++;
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

/**
 * The main class to handle the workflow of spending sacredpower on upgrading a unit's stats.
 * Not the most performant solution - it is currently creating a lot of complex type instances
 * for every unit in the pack of nations being generated. It would benefit from being rewritten
 * more as an API or with a State Design Pattern.
 */
public class StatUpgradeManager {
    private NationGen nationGen;
    private Unit unit;
    public double budgetLeft;
    private Random random;
    private StatUpgradePattern statUpgradePattern;
    public boolean cannotAffordMoreUpgrades = false;

    private StatUpgradePattern[] statUpgradePatterns = {
        new LowerFirstStatUpgradePattern(),
        new BalancedStatUpgradePattern(),
        new RandomStatUpgradePattern()
    };

    private int extraGoldCostCounter = 0;

    // Stats that can be randomly upgraded. Their defaultValue determines how expensive the upgrade will be.
    // If the unit's current stat is already higher than the default, it will be less likely to be chosen as an upgrade.
    // This should be a data structure that guarantees ordering to not mess with seeds (different order every time would
    // not result in the same results with the same seed).
    private LinkedHashMap<UpgradableStats, StatUpgradeTracker> statUpgradeCalculators = new LinkedHashMap<>();

    public StatUpgradeManager(NationGen nationGen, Unit unitToUpgrade, int statUpgradeBudget, Random random) {
        this.nationGen = nationGen;
        this.unit = unitToUpgrade;
        this.budgetLeft = statUpgradeBudget;
        this.random = random;

        int statUpgradePatternSetting = nationGen.settings.get(SettingsType.statUpgradePattern).intValue();

        // Pick a random pattern of the three
        if (statUpgradePatternSetting == StatUpgradePatternEnum.AnyStatUpgradePattern.getNumVal()) {
            int randomRoll = random.nextInt(statUpgradePatterns.length);
            this.statUpgradePattern = statUpgradePatterns[randomRoll];
        }

        // Otherwise pick the stat upgrade distribution pattern chosen in the settings
        else {
            this.statUpgradePattern = statUpgradePatterns[statUpgradePatternSetting-1];
        }

        //System.out.println("Stat Upgrade Pattern for this unit: " + this.statUpgradePattern.toString());
        
        // Determine default hp value based on the unit hp
        int unitHp = unit.getTotalCommandValue("#hp", 10);
        int defaultHpValue = unitHp;

        // If less than 10 hp, take lowest of 3 or 10 - hp
        if (unitHp < 10) {
            defaultHpValue = unitHp - Math.min(3, 10 - unitHp);
        }

        // Upgrades to hp will be 5% of default hp, minimum 1
        int hpUpgradeAmount = (int) Math.round(Math.max(defaultHpValue * 0.05, 1));

        // Create calculators for each stat with their default values
        StatUpgradeTracker hpUpgradeCalculator = new StatUpgradeTracker(defaultHpValue, hpUpgradeAmount, 999, 99, this.statUpgradePattern);
        StatUpgradeTracker attackUpgradeCalculator = new StatUpgradeTracker(10, 1, 99, 99, this.statUpgradePattern);
        StatUpgradeTracker defenseUpgradeCalculator = new StatUpgradeTracker(10, 1, 99, 99, this.statUpgradePattern);
        StatUpgradeTracker strengthUpgradeCalculator = new StatUpgradeTracker(10, 1, 99, 99, this.statUpgradePattern);
        StatUpgradeTracker encumbranceUpgradeCalculator = new StatUpgradeTracker(3, -1, 1, 1, this.statUpgradePattern);
        StatUpgradeTracker mrUpgradeCalculator = new StatUpgradeTracker(10, 1, 18, 2, this.statUpgradePattern);

        // Add to hashmap for ease of access later
        statUpgradeCalculators.put(UpgradableStats.HP, hpUpgradeCalculator);
        statUpgradeCalculators.put(UpgradableStats.ATTACK, attackUpgradeCalculator);
        statUpgradeCalculators.put(UpgradableStats.DEFENSE, defenseUpgradeCalculator);
        statUpgradeCalculators.put(UpgradableStats.STRENGTH, strengthUpgradeCalculator);
        statUpgradeCalculators.put(UpgradableStats.ENCUMBRANCE, encumbranceUpgradeCalculator);
        statUpgradeCalculators.put(UpgradableStats.MR, mrUpgradeCalculator);

        // Only add precision as an upgrade option if unit is ranged
        if (unit.isRanged() == true) {
            StatUpgradeTracker precisionUpgradeCalculator = new StatUpgradeTracker(10, 1, 99, 99, this.statUpgradePattern);
            statUpgradeCalculators.put(UpgradableStats.PRECISION, precisionUpgradeCalculator);
        }
    }

    // Select a random stat to upgrade based on its price
    // and the upgrade pattern chosen and add it to the unit
    public void buyStatUpgrade() {
        List<Filter> possibleUpgrades = new ArrayList<>();
        
        // Calculate price and chance weights for each upgradable stat based on the
        // current stat values of the unit. Stats that are already higher than the default
        // values (usually 10 for most stats, or 3 for encumbrance) will cost more to upgrade
        statUpgradeCalculators.forEach((statKey, statTracker) -> {
            int currentUnitValue;
            double upgradePrice;
            double upgradeChance;
            Filter upgradeFilter;

            // Find current stat value and calculate its price based on it
            currentUnitValue = this.unit.getTotalCommandValue(statKey.toString(), statTracker.defaultValue);
            upgradePrice = statTracker.calculateUpgradePrice(currentUnitValue);

            // Upgrade would push past upgradable limit, set exorbitant price
            if (statTracker.hasReachedStatUpgradeLimit(currentUnitValue) || statTracker.hasReachedMaxNumberOfUpgrades()) {
                upgradePrice = 100;
            }

            // Add special encumbrance price modifiers
            if (statKey == UpgradableStats.ENCUMBRANCE) {
                // If this unit already had encumbrance upgraded at least once, or its
                // current encumbrance is less than 2, set an exorbitant price
                if (statTracker.getTimesUpgraded() > 0 || currentUnitValue < 2) {
                    upgradePrice = 100;
                }

                // Otherwise set a slightly less exorbitant price. Encumbrance be expensive yo
                else {
                    upgradePrice *= 1.5;
                }
            }

            // Strength upgrades should get more and more expensive with each of them.
            // Don't want markatas with 16 strength because it was the cheapest stat.
            else if (statKey == UpgradableStats.STRENGTH) {
                upgradePrice *= statTracker.getTimesUpgraded() + 1;
            }

            // If we can afford the price of this stat upgrade, then make a Filter out of it
            if (upgradePrice > 0 && upgradePrice < this.budgetLeft) {
                // Calculate the chance weight for this particular stat upgrade. This
                // is based on the price of the upgrade and gets weighed differently
                // depending on which stat upgrade pattern was selected in the settings
                // (see StatUpgradePattern.java and the classes that extend it)
                upgradeChance = statTracker.calculateUpgradeChance(upgradePrice);
                upgradeFilter = new Filter(nationGen);
                upgradeFilter.name = statKey.toString();
                upgradeFilter.basechance = upgradeChance;
                upgradeFilter.power = upgradePrice;
                possibleUpgrades.add(upgradeFilter);
            }
        });

        // If there are no upgrades that we can afford then we're done spending our budget
        if (possibleUpgrades.size() == 0) {
            this.cannotAffordMoreUpgrades = true;

            // Increase gcost of unit by 1/3 of the number of stat upgrades we've granted it
            unit.commands.add(Command.args("#gcost", "+" + (int) Math.round(this.extraGoldCostCounter)));
            return;
        }

        // Sort the possibleUpgrades list by a deterministic rule every time, so that seeds are not impacted by a different order of items
        possibleUpgrades.sort(Comparator.comparing(Filter::getName));

        // If there are upgradable stats, select one at random based on their weights
        Filter selectedStatUpgrade = EntityChances.baseChances(possibleUpgrades).getRandom(random);
        StatUpgradeTracker statUpgradeCalculator = statUpgradeCalculators.get(UpgradableStats.fromString(selectedStatUpgrade.name));
        String amountToUpgrade = statUpgradeCalculator.upgradeStepSize + "";

        // If the stat is positive (i.e. attack +1), add the + sign so natgen
        // knows to add it on the existing stat, rather than replace it
        if (statUpgradeCalculator.upgradeStepSize > 0) {
            amountToUpgrade = "+" + amountToUpgrade;
        }

        // Add stat upgrade, spend budget, track number of upgrades for this stat
        unit.commands.add(Command.args(selectedStatUpgrade.name, amountToUpgrade));
        this.budgetLeft -= selectedStatUpgrade.power;
        statUpgradeCalculator.addUpgrade();

        // Every stat upgrade will make the unit slightly more expensive
        extraGoldCostCounter += 0.33;

        //DecimalFormat twoPlaces = new DecimalFormat("#.##");
        //System.out.println("Upgraded " + unit.pose + "'s " + selectedStatUpgrade.name + " by " + amountToUpgrade + " (cost " + twoPlaces.format(selectedStatUpgrade.power) + ", basechance " + twoPlaces.format(selectedStatUpgrade.basechance) + ")");
    }
}
