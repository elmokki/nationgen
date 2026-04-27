package nationGen.nation.pd;

public enum PDUnitNumber {
  FIRST("", 0),
  SECOND("b", 1),
  THIRD("c", 2),
  FOURTH("d", 3);

  private String modCommandDesignator;
  private int unitNumber;

  PDUnitNumber(String modCommandDesignator, int unitNumber) {
    this.modCommandDesignator = modCommandDesignator;
    this.unitNumber = unitNumber;
  }

  public String getModCommandDesignator() {
    return this.modCommandDesignator;
  }

  public int getUnitNumber() {
    return this.unitNumber;
  }
}
