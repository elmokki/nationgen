package nationGen.restrictions;

import nationGen.NationGenAssets;
import nationGen.restrictions.NationRestriction.RestrictionType;

public class NationRestrictionFactory {

  public static NationRestriction getInstance(
    NationGenAssets assets,
    RestrictionType type
  ) {
    switch (type) {
      case MageWithAccess:
        return new MageWithAccessRestriction();
      case MagicAccess:
        return new MagicAccessRestriction();
      case MagicDiversity:
        return new MagicDiversityRestriction();
      case NationTheme:
        return new NationThemeRestriction(assets);
      case NoPrimaryRace:
        return new NoPrimaryRaceRestriction(assets);
      case NoUnitOfRace:
        return new NoUnitOfRaceRestriction(assets);
      case PrimaryRace:
        return new PrimaryRaceRestriction(assets);
      case RecAnywhereSacreds:
        return new RecAnywhereSacredsRestriction();
      case SacredRace:
        return new SacredRaceRestriction(assets);
      case UnitCommand:
        return new UnitCommandRestriction();
      case UnitFilter:
        return new UnitFilterRestriction(assets);
      case UnitRace:
        return new UnitOfRaceRestriction(assets);
      default:
        return null;
    }
  }

  public static NationRestriction getSameTypedInstance(
    NationGenAssets assets,
    NationRestriction restriction
  ) {
    return getInstance(assets, restriction.getType());
  }
}
