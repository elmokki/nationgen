package nationGen.units;

public enum LeadershipType {
  NORMAL(""),
  UNDEAD("undead"),
  MAGIC_BEING("magic");

  private String type;

  LeadershipType(String type) {
    this.type = type;
  }

  public String toModCommandType() {
    return this.type;
  }
}
