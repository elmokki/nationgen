package nationGen.items;

public class ItemDependency {
	public boolean lagged = false;
	public boolean type = false;
	public String slot = null;
	public String target = null;
	
	public ItemDependency(String slot, String target)
	{
		this.slot = slot;
		this.target = target;
	}
	
	public ItemDependency(String slot, String target, boolean type, boolean lagged)
	{
		this.slot = slot;
		this.target = target;
		this.lagged = lagged;
		this.type = type;
	}
}
