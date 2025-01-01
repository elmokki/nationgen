package nationGen.rostergeneration.powermanagers;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

class StatUpgradeCalculator {
    public int defaultValue;
    public int upgradeStepSize;
    public int upgradeCount = 0;
    private boolean isLowerStatBetter = false;
    private StatUpgradePattern statUpgradePattern;

    public StatUpgradeCalculator(int defaultValue, int upgradeStepSize, StatUpgradePattern statUpgradePattern) {
        this.defaultValue = defaultValue;
        this.upgradeStepSize = upgradeStepSize;
        this.statUpgradePattern = statUpgradePattern;

        if (upgradeStepSize < 0) {
            isLowerStatBetter = true;
        }
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

    // Stats that can be randomly upgraded. Their defaultValue
    // determines how expensive the upgrade will be. If the unit's
    // current stat is already higher than the default, it will
    // be less likely to be chosen as an upgrade.
    private HashMap<UpgradableStats, StatUpgradeCalculator> statUpgradeCalculators = new HashMap<>();

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
        int unitHp = unit.getCommandValue("#hp", 10);
        int defaultHpValue = unitHp;

        // If less than 10 hp, take lowest of 3 or 10 - hp
        if (unitHp < 10) {
            defaultHpValue = unitHp - Math.min(3, 10 - unitHp);
        }

        // Upgrades to hp will be 5% of default hp, minimum 1
        int hpUpgradeAmount = (int) Math.round(Math.max(defaultHpValue * 0.05, 1));

        // Create calculators for each stat with their default values
        StatUpgradeCalculator hpUpgradeCalculator = new StatUpgradeCalculator(defaultHpValue, hpUpgradeAmount, this.statUpgradePattern);
        StatUpgradeCalculator attackUpgradeCalculator = new StatUpgradeCalculator(10, 1, this.statUpgradePattern);
        StatUpgradeCalculator defenseUpgradeCalculator = new StatUpgradeCalculator(10, 1, this.statUpgradePattern);
        StatUpgradeCalculator encumbranceUpgradeCalculator = new StatUpgradeCalculator(3, -1, this.statUpgradePattern);
        StatUpgradeCalculator mrUpgradeCalculator = new StatUpgradeCalculator(10, 1, this.statUpgradePattern);

        // Add to hashmap for ease of access later
        statUpgradeCalculators.put(UpgradableStats.HP, hpUpgradeCalculator);
        statUpgradeCalculators.put(UpgradableStats.ATTACK, attackUpgradeCalculator);
        statUpgradeCalculators.put(UpgradableStats.DEFENSE, defenseUpgradeCalculator);
        statUpgradeCalculators.put(UpgradableStats.ENCUMBRANCE, encumbranceUpgradeCalculator);
        statUpgradeCalculators.put(UpgradableStats.MR, mrUpgradeCalculator);

        // Only add precision as an upgrade option if unit is ranged
        if (unit.isRanged() == true) {
            StatUpgradeCalculator precisionUpgradeCalculator = new StatUpgradeCalculator(10, 1, this.statUpgradePattern);
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
        statUpgradeCalculators.forEach((statKey, statCalculator) -> {
            int currentUnitValue;
            double upgradePrice;
            double upgradeChance;
            Filter upgradeFilter;

            // Find current stat value and calculate its price based on it
            currentUnitValue = this.unit.getCommandValue(statKey.toString(), statCalculator.defaultValue);
            upgradePrice = statCalculator.calculateUpgradePrice(currentUnitValue);

            // Add special encumbrance price modifiers
            if (statKey == UpgradableStats.ENCUMBRANCE) {
                // If this unit already had encumbrance upgraded at least once, or its
                // current encumbrance is less than 2, set an exorbitant price
                if (statCalculator.upgradeCount > 0 || currentUnitValue < 2) {
                    upgradePrice = 100;
                }

                // Otherwise set a slightly less exorbitant price. Encumbrance be expensive yo
                else {
                    upgradePrice *= 1.5;
                }
            }

            // If we can afford the price of this stat upgrade, then make a Filter out of it
            if (upgradePrice > 0 && upgradePrice < this.budgetLeft) {
                // Calculate the chance weight for this particular stat upgrade. This
                // is based on the price of the upgrade and gets weighed differently
                // depending on which stat upgrade pattern was selected in the settings
                // (see StatUpgradePattern.java and the classes that extend it)
                upgradeChance = statCalculator.calculateUpgradeChance(upgradePrice);
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
            //System.out.println("----------------");
            return;
        }

        // If there are upgradable stats, select one at random based on their weights
        Filter selectedStatUpgrade = EntityChances.baseChances(possibleUpgrades).getRandom(random);
        StatUpgradeCalculator statUpgradeCalculator = statUpgradeCalculators.get(UpgradableStats.fromString(selectedStatUpgrade.name));
        String amountToUpgrade = statUpgradeCalculator.upgradeStepSize + "";

        // If the stat is positive (i.e. attack +1), add the + sign so natgen
        // knows to add it on the existing stat, rather than replace it
        if (statUpgradeCalculator.upgradeStepSize > 0) {
            amountToUpgrade = "+" + amountToUpgrade;
        }

        // Add stat upgrade, spend budget, track number of upgrades for this stat
        unit.commands.add(Command.args(selectedStatUpgrade.name, amountToUpgrade));
        this.budgetLeft -= selectedStatUpgrade.power;
        statUpgradeCalculator.upgradeCount++;

        // Every stat upgrade will make the unit slightly more expensive
        extraGoldCostCounter += 0.33;

        //DecimalFormat twoPlaces = new DecimalFormat("#.##");
        //System.out.println("Upgraded " + unit.pose + "'s " + selectedStatUpgrade.name + " by " + amountToUpgrade + " (cost " + twoPlaces.format(selectedStatUpgrade.power) + ", basechance " + twoPlaces.format(selectedStatUpgrade.basechance) + ")");
    }
}
