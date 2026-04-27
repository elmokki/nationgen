package nationGen.nation.pd;

import java.util.Optional;

public enum PDUnitType {
  FORTED_OR_WITH_20PD_FIRST(PDProvinceType.FORTED_OR_WITH_20PD, PDUnitNumber.FIRST),
  FORTED_OR_WITH_20PD_SECOND(PDProvinceType.FORTED_OR_WITH_20PD, PDUnitNumber.SECOND),
  FORTED_OR_WITH_20PD_THIRD(PDProvinceType.FORTED_OR_WITH_20PD, PDUnitNumber.THIRD),
  FORTED_OR_WITH_20PD_FOURTH(PDProvinceType.FORTED_OR_WITH_20PD, PDUnitNumber.FOURTH),
  FORTED_WITH_20PD_FIRST(PDProvinceType.FORTED_WITH_20PD, PDUnitNumber.FIRST),
  FORTED_WITH_20PD_SECOND(PDProvinceType.FORTED_WITH_20PD, PDUnitNumber.SECOND);

  public PDProvinceType pdProvinceType;
  public PDUnitNumber pdUnitNumber;

  PDUnitType(PDProvinceType pdProvinceType, PDUnitNumber pdUnitNumber) {
    this.pdProvinceType = pdProvinceType;
    this.pdUnitNumber = pdUnitNumber;
  }

  public String getModCommand() {
    return this.pdProvinceType.getBaseModCommand() +
      this.pdUnitNumber.getModCommandDesignator();
  }

  public String getMultiplierModCommand() {
    return this.pdProvinceType.getMultiplierModCommand() +
      this.pdUnitNumber.getModCommandDesignator();
  }

  public Optional<PDUnitType> getByNumber(PDProvinceType provinceType, PDUnitNumber number) {
    for (PDUnitType pdUnitType : PDUnitType.values()) {
      if (pdUnitType.pdProvinceType == provinceType && pdUnitType.pdUnitNumber == number) {
        return Optional.of(pdUnitType);
      }
    }

    return Optional.empty();
  }
}
