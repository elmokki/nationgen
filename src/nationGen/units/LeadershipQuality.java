package nationGen.units;

import java.util.Optional;
import java.util.stream.Stream;

public enum LeadershipQuality {
  NONE("no", 0, 0, -1),
  POOR("poor", 10, 1, -1),
  OK("ok", 50, 2, 0),
  GOOD("good", 100, 3, 1),
  EXPERT("expert", 150, 4, 2),
  SUPERIOR("superior", 200, 4, 3);

  private String quality;
  private int leadAmount;
  private int squadLimit;
  private int moraleBonus;

  LeadershipQuality(String quality, int leadAmount, int squadLimit, int moraleBonus) {
    this.quality = quality;
    this.leadAmount = leadAmount;
    this.squadLimit = squadLimit;
    this.moraleBonus = moraleBonus;
  }

  public String getQualityString() {
    return this.quality;
  }

  public int getLeadershipAmount() {
    return this.leadAmount;
  }

  public int getSquadLimit() {
    return this.squadLimit;
  }

  public int getMoraleBonus() {
    return this.moraleBonus;
  }

  public LeadershipQuality getNext() {
    if (this.ordinal() == LeadershipQuality.values().length - 1) {
      return this;
    }

    return LeadershipQuality.values()[this.ordinal()+1];
  }

  public LeadershipQuality getPrevious() {
    if (this.ordinal() == 0) {
      return this;
    }

    return LeadershipQuality.values()[this.ordinal()-1];
  }

  static public Optional<LeadershipQuality> fromString(String qualityString) {
    return Stream.of(LeadershipQuality.values())
      .filter(q -> qualityString == q.getQualityString())
      .findFirst();
  }
}
