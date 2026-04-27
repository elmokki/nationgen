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

  public static <T extends Number> int roundInGroupsOf(T value, int inGroupsOf) {
    return (int) Math.round( value.doubleValue() / inGroupsOf ) * inGroupsOf;
  }
}
