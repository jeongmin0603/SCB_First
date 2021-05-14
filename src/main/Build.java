package main;

import bwapi.UnitType;

public class Build {
	//build train search
	
	String act;
	
	UnitType unit;
	
	public Build(String act, UnitType unit) {
		this.act = act;
		this.unit = unit;
	}
	public Build(String act) {
		this.act = act;
	}
	public String getAct() {
		return act;
	}

	public UnitType getUnit() {
		return unit;
	}
	
	
}
