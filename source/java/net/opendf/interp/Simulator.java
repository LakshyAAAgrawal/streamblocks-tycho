package net.opendf.interp;

public interface Simulator {
	/**
	 * Executes instructions in the actor machine controller until an ICall or IWait instruction is executed.
	 * 
	 * @return - true if an ICall is executed, false if IWait was executed last, 
	 *           i.e. when step() return false there is no need to call it again until other actors has put new tokens in this actors input channels.
	 */
	public boolean step();
	public String scopesToString();
}
