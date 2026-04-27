package nationGen.units;

import java.util.Optional;
import java.util.stream.Stream;

import nationGen.misc.Command;

public enum LeadershipAbility {
  // Normal unit leadership types
  NO_NORMAL(LeadershipQuality.NONE, LeadershipType.NORMAL),
  POOR_NORMAL(LeadershipQuality.POOR, LeadershipType.NORMAL),
  OK_NORMAL(LeadershipQuality.OK, LeadershipType.NORMAL),
  GOOD_NORMAL(LeadershipQuality.GOOD, LeadershipType.NORMAL),
  EXPERT_NORMAL(LeadershipQuality.EXPERT, LeadershipType.NORMAL),
  SUPERIOR_NORMAL(LeadershipQuality.SUPERIOR, LeadershipType.NORMAL),

  // Undead/Demon unit leadership types
  NO_UNDEAD(LeadershipQuality.NONE, LeadershipType.UNDEAD),
  POOR_UNDEAD(LeadershipQuality.POOR, LeadershipType.UNDEAD),
  OK_UNDEAD(LeadershipQuality.OK, LeadershipType.UNDEAD),
  GOOD_UNDEAD(LeadershipQuality.GOOD, LeadershipType.UNDEAD),
  EXPERT_UNDEAD(LeadershipQuality.EXPERT, LeadershipType.UNDEAD),
  SUPERIOR_UNDEAD(LeadershipQuality.SUPERIOR, LeadershipType.UNDEAD),

  // Magic Being unit leadership types
  NO_MAGIC_BEING(LeadershipQuality.NONE, LeadershipType.MAGIC_BEING),
  POOR_MAGIC_BEING(LeadershipQuality.POOR, LeadershipType.MAGIC_BEING),
  OK_MAGIC_BEING(LeadershipQuality.OK, LeadershipType.MAGIC_BEING),
  GOOD_MAGIC_BEING(LeadershipQuality.GOOD, LeadershipType.MAGIC_BEING),
  EXPERT_MAGIC_BEING(LeadershipQuality.EXPERT, LeadershipType.MAGIC_BEING),
  SUPERIOR_MAGIC_BEING(LeadershipQuality.SUPERIOR, LeadershipType.MAGIC_BEING);

  public LeadershipQuality quality;
  public LeadershipType type;

  LeadershipAbility(LeadershipQuality quality, LeadershipType leadershipType) {
    this.quality = quality;
    this.type = leadershipType;
  }

  public String toModCommand() {
    return "#" + this.quality.getQualityString() + this.type.toModCommandType() + "leader";
  }

  public static Optional<LeadershipAbility> fromModCommand(Command command) {
    if (command == null) {
      return Optional.empty();
    }

    return LeadershipAbility.fromModCommand(command.command);
  }

  public static Optional<LeadershipAbility> fromModCommand(String modCommand) {
    return Stream.of(LeadershipAbility.values())
      .filter(l -> modCommand == l.toModCommand())
      .findFirst();
  }

  public static LeadershipAbility fromTypeAndQuality(LeadershipType type, LeadershipQuality quality) {
    return Stream.of(LeadershipAbility.values())
      .filter(l -> l.type == type && l.quality == quality)
      .findFirst()
      .get();
  }

  public static LeadershipAbility getNoLeadership(LeadershipType type) {
    return Stream.of(LeadershipAbility.values())
      .filter(l -> l.type == type)
      .findFirst()
      .orElse(LeadershipAbility.NO_NORMAL);
  }
}
