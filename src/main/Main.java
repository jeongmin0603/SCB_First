package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.sun.javafx.print.Units;

import bwapi.Color;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Position;
import bwapi.Race;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.Flag.Enum;
import bwta.BWTA;

public class Main extends DefaultBWListener {

	Mirror mirror = new Mirror();
	Game game;
	List<Unit> minerals;
	List<UnitType> unitQueue = new ArrayList<>();
	List<Integer> selectedMinerals = new ArrayList<Integer>();
	List<Unit> geysers;
	Map<UnitType, List<Unit>> units = new HashMap<UnitType, List<Unit>>();

	public static void main(String[] args) {
		new Main().run();

	}

	public void run() {
		mirror.getModule().setEventListener(this);
		mirror.startGame();
	}

	@Override
	public void onStart() {
		game = mirror.getGame();
		game.enableFlag(Enum.UserInput.getValue());

		BWTA.analyze();
		BWTA.readMap();

		game.setLocalSpeed(10);

		minerals = BWTA.getNearestBaseLocation(game.self().getStartLocation().getPoint()).getMinerals();
		geysers = BWTA.getNearestBaseLocation(game.self().getStartLocation().getPoint()).getGeysers();

		for (int i = 0; i < 12; i++) {
			unitQueue.add(UnitType.Terran_SCV);
		}

		unitQueue.add(UnitType.Terran_Barracks);
		unitQueue.add(UnitType.Terran_Refinery);

	}

	private void setWorkerToMinerals(Unit unit) {
		double minDistance;
		double distance;

		int minIndex = 0;
		Unit mineral;

		minDistance = Double.MAX_VALUE;

		for (int j = 0; j < minerals.size(); j++) {
			if (selectedMinerals.contains(j))
				continue;
			mineral = minerals.get(j);
			distance = unit.getDistance(mineral);

			if (distance < minDistance) {
				minDistance = distance;
				minIndex = j;
			}
		}

		unit.rightClick(minerals.get(minIndex));
		selectedMinerals.add(minIndex);

	}
	int index = 0;
	@Override
	public void onFrame() {
		if(game.getFrameCount() % 6 != 0) return;
		
		if(unitQueue.isEmpty()) return;
		
		if(unitQueue.get(0) == UnitType.Terran_SCV && units.containsKey(UnitType.Terran_Command_Center)) {			
			if(unitQueue.get(0) == UnitType.Terran_SCV && !units.get(UnitType.Terran_Command_Center).get(0).isTraining()) {
				units.get(UnitType.Terran_Command_Center).get(0).train(unitQueue.get(0));
				unitQueue.remove(0);
				System.out.println(++index);
			}
			
		}
	}
//		Unit barracks = null;
//		if(units.containsKey(UnitType.Terran_Barracks)) barracks = units.get(UnitType.Terran_Barracks).get(0);
//		
//		if(barracks != null && game.self().supplyTotal() - game.self().supplyUsed() != 0) {
//			System.out.println(barracks.canTrain(UnitType.Terran_Marine));
//			if(barracks.canTrain(UnitType.Terran_Marine)) {
//				barracks.train(UnitType.Terran_Marine);
//			}				
//		}
//		
//		Unit commandCenter = units.get(UnitType.Terran_Command_Center).get(0);
//		if(commandCenter.canTrain(UnitType.Terran_SCV) && units.get(UnitType.Terran_SCV).size() < minerals.size()) {
//			commandCenter.train(UnitType.Terran_SCV);
//		}
//		
//		Unit scv = units.get(UnitType.Terran_SCV).get(0);
//		game.drawTextMap(scv.getPoint(), Integer.toString(scv.getID()));
//		
//		if(scv.canBuild(UnitType.Terran_Barracks) && !units.containsKey(UnitType.Terran_Barracks)) {
//			TilePosition position = new TilePosition(commandCenter.getTilePosition().getX() - 6 , commandCenter.getTilePosition().getY());
//			boolean ans = scv.build(UnitType.Terran_Barracks, position);
//			if(!ans) position = new TilePosition(commandCenter.getTilePosition().getX() + 6 , commandCenter.getTilePosition().getY());
//			scv.build(UnitType.Terran_Barracks, position);
//		}
//		
//		if(scv.canBuild(UnitType.Terran_Supply_Depot) && units.containsKey(UnitType.Terran_Barracks) && !units.containsKey(UnitType.Terran_Supply_Depot)) {
//			TilePosition position = new TilePosition(units.get(UnitType.Terran_Barracks).get(0).getTilePosition().getX(), units.get(UnitType.Terran_Barracks).get(0).getTilePosition().getY() - 3);
//			scv.build(UnitType.Terran_Supply_Depot, position);
//		}
//	}

	private Unit getNotWorkingWorker() {
		UnitType workerType = null;
		if (game.self().getRace() == Race.Terran) {
			workerType = UnitType.Terran_SCV;
		} else if (game.self().getRace() == Race.Zerg) {
			workerType = UnitType.Zerg_Drone;
		} else {
			workerType = UnitType.Protoss_Probe;
		}

		for (Unit unit : units.get(workerType)) {
			if (!(unit.isBlind() || unit.isGatheringGas() || unit.isGatheringMinerals()))
				return unit;
		}

		for (int i = 0; i < units.get(workerType).size(); i++) {
			selectedMinerals.remove(0);
			return units.get(workerType).get(0);
		}
		
		return null;
	}

	@Override
	public void onUnitComplete(Unit unit) {
		if (unit.getPlayer().equals(game.self())) {
			UnitType type = unit.getType();
			if (type == UnitType.Terran_SCV || type == UnitType.Protoss_Probe || type == UnitType.Zerg_Drone) {
				setWorkerToMinerals(unit);
			}
		}
	}

	@Override
	public void onEnd(boolean win) {
		if (win) {
			System.out.println("WIN!");
		} else {
			System.out.println("LOSE...");
		}
		System.exit(0);
	}

	@Override
	public void onUnitCreate(Unit unit) {
		if (unit.getPlayer().equals(game.self())) {
			UnitType type = unit.getType();
			if (!units.containsKey(type)) {
				ArrayList<Unit> list = new ArrayList<Unit>();
				units.put(type, list);
			}
			units.get(type).add(unit);
		}
	}

	@Override
	public void onReceiveText(Player arg0, String arg1) {
		game.sendText(arg1);
	}

}
