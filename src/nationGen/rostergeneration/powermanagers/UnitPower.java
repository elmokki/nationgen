package nationGen.rostergeneration.powermanagers;

import java.util.Random;

public class UnitPower {
    public int power = 0;
    public int powerLeft = 0;
    public int statUpgradeBudget = 0;
    public int statUpgradeBudgetLeft = 0;

    public UnitPower(int power, int statUpgradeBudget) {
        this.power = power;
        this.powerLeft = power;
        this.statUpgradeBudget = statUpgradeBudget;
        this.statUpgradeBudgetLeft = statUpgradeBudget;
    }

    public void modifyPower(int powerChange) {
        this.power += powerChange;
        this.powerLeft += powerChange;
    }

    public void modifyStatUpgradeBudget(int statUpgradeBudgetChange) {
        this.statUpgradeBudget += statUpgradeBudgetChange;
        this.statUpgradeBudgetLeft += statUpgradeBudgetChange;
    }

    public void scaleStatUpgradeBudget(double factor) {
        this.statUpgradeBudget *= factor;
        this.statUpgradeBudgetLeft *= factor;
    }

    public void shiftPowerToStatUpgradeBudget(int powerChange, int statUpgradeBudgetChange) {
        this.modifyPower(powerChange);
        this.modifyStatUpgradeBudget(statUpgradeBudgetChange);
    }

    public boolean rollChanceOfStatUpgradeBudgetChange(Random random, double chance, int change) {
        double roll = random.nextDouble();

        if (roll < chance) {
            this.modifyStatUpgradeBudget(change);
            return true;
        }

        else return false;
    }

    public boolean rollChanceOfBudgetChange(Random random, double chance, int powerChange, int statUpgradeBudgetChange) {
        double roll = random.nextDouble();

        if (roll < chance) {
            this.shiftPowerToStatUpgradeBudget(powerChange, statUpgradeBudgetChange);
            return true;
        }

        else return false;
    }
}
