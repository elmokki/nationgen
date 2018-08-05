package nationGen.restrictions;

import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.restrictions.NationRestriction.RestrictionType;

public class NationRestrictionFactory
{
    public static NationRestriction getInstance(NationGen gen, NationGenAssets assets, RestrictionType type)
    {
        switch (type)
        {
        case MageWithAccess: return new MageWithAccessRestriction(gen);
        case MagicAccess: return new MagicAccessRestriction(gen);
        case MagicDiversity: return new MagicDiversityRestriction(gen);
        case NationTheme: return new NationThemeRestriction(gen, assets);
        case NoPrimaryRace: return new NoPrimaryRaceRestriction(gen, assets);
        case NoUnitOfRace: return new NoUnitOfRaceRestriction(gen, assets);
        case PrimaryRace: return new PrimaryRaceRestriction(gen, assets);
        case RecAnywhereSacreds: return new RecAnywhereSacredsRestriction(gen);
        case SacredRace: return new SacredRaceRestriction(gen, assets);
        case UnitCommand: return new UnitCommandRestriction(gen);
        case UnitFilter: return new UnitFilterRestriction(gen, assets);
        case UnitRace: return new UnitOfRaceRestriction(gen, assets);
        default: return null;
        }
    }
    
    public static NationRestriction getSameTypedInstance(NationGen gen, NationGenAssets assets, NationRestriction restriction)
    {
        return getInstance(gen, assets, restriction.getType());
    }
}
