# NationGen
Dominions 6 procedural content generation program.

## To-do
###### List of priority fixes and improvements

- **Refine pretender templates for all gods without realms**

	Should be done in `data/gods/individual/gods-without-realm.txt`. They are mostly nation-specific gods, like the Risen Oracle. Some of them should be fleshed out with more detailed `#chanceinc`, like additional race checks, or theme checks (i.e. cold nations, heat nations, etc).

- **Give mounts the resists of their riders**

	This should probably only be done for riders that have heat/chill/poison auras, so that the mount doesn't die under them, but could also be done more rarely in other cases.

- **Temperature protection**

	Temperature protection in forts for nations with `#idealcold`. The modding command for this does not exist yet.

- **New barded cavalry sprites**

	Many don't have proper sprites yet.


## Template ideas

- **`#xpshape` and `#labxpshape` units**

	For example, carp units that grow bigger and bigger after killing enough units. This should be done programmatically so that different kinds of xpshapes can be applied to different units without having to manually define the templates every time. There are different flavours of `#xpshape` effects that could be done, i.e. growing in size, or getting more of a certain stat, or changing into more mature versions (like dragon hatchlings to dragons), or gaining more military discipline (formation fighter, morale, leadership, etc).

- **Generally, work out all the new modding commands added in Dom6 and make something out of them.**

- **Use thematic commands to buff some underpowered nations.**

	For example, pygmies or other tribal/primitive nations. An example of this would be to use the spirit guardian tag, or using montags to give them special allies, or using tattoos.
