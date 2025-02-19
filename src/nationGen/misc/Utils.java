package nationGen.misc;

public class Utils {
  /**
   * Normalizes a value in a range of minValue to maxValue to a range of 0 to 1
   * @param value
   * @param minValue
   * @param maxValue
   * @return
   */
  public static <T extends Number> float normalize(T value, T minValue, T maxValue) {
    return (value.floatValue() - minValue.floatValue()) / (maxValue.floatValue() - minValue.floatValue());
  }
}
