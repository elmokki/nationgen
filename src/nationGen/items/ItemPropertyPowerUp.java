package nationGen.items;

public class ItemPropertyPowerUp {
  private ItemProperty itemProperty;
  private Integer powerUpCost = 1;
  private int powerUpIncrease = 1;
  private double powerUpChance = 1;
  private Boolean isBoolean = false;

  ItemPropertyPowerUp(ItemProperty property) {
    this.itemProperty = property;
  }

  ItemPropertyPowerUp(ItemProperty property, Boolean isBoolean) {
    this.itemProperty = property;
    this.isBoolean = isBoolean;
  }

  public ItemProperty getProperty() {
    return this.itemProperty;
  }

  public Integer getCost() {
    return this.powerUpCost;
  }

  public ItemPropertyPowerUp setCost(Integer powerUpCost) {
    this.powerUpCost = powerUpCost;
    return this;
  }

  public Integer getIncrease() {
    return this.powerUpIncrease;
  }

  public ItemPropertyPowerUp setIncrease(Integer powerUpIncrease) {
    this.powerUpIncrease = powerUpIncrease;
    return this;
  }

  public double getChance() {
    return this.powerUpChance;
  }

  public ItemPropertyPowerUp setChance(double powerUpChance) {
    this.powerUpChance = powerUpChance;
    return this;
  }

  public Boolean isBoolean() {
    return this.isBoolean;
  }
}
