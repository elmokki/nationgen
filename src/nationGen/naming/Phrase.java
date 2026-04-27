package nationGen.naming;

public final class Phrase {
  public final String singular;
  public final String plural;

  public Phrase(String singular, String plural) {
    this.singular = singular;
    this.plural = plural;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    
    if (other == null || getClass() != other.getClass()) {
      return false;
    }

    Phrase otherPhrase = (Phrase) other;
    return this.singular.equals(otherPhrase.singular) &&
      this.plural.equals(otherPhrase.plural);
  }
}
