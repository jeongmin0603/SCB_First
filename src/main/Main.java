package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
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
	Map<UnitType, List<Unit>> units = new HashMap<UnitType, List<Unit>>();
	UnitType currentType;

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

		for (int j = 0; j < minerals.size() - selectedMinerals.size(); j++) {
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
		
		currentType = unitQueue.get(0);
	}

	@Override
	public void onFrame() {
		if (game.getFrameCount() % 12 != 0)
			return;

		if (unitQueue.isEmpty())
			return;

		System.out.println(unitQueue);

		if (game.self().supplyTotal() - game.self().supplyUsed() == 0
				&& unitQueue.get(0) != UnitType.Terran_Supply_Depot) {
			unitQueue.add(0, UnitType.Terran_Supply_Depot);
		}

		if (currentType == UnitType.Terran_SCV && units.containsKey(UnitType.Terran_Command_Center)) {
			if (currentType == UnitType.Terran_SCV && !units.get(UnitType.Terran_Command_Center).get(0).isTraining()) {
				units.get(UnitType.Terran_Command_Center).get(0).train(currentType);
				if(unitQueue.indexOf(currentType) + 1 < unitQueue.size()) {					
					currentType = unitQueue.get(unitQueue.indexOf(currentType) + 1);
				} else {
					currentType = null;
				}
			}
		}

		if (units.containsKey(UnitType.Terran_Barracks)) {
			for (Unit barrack : units.get(UnitType.Terran_Barracks)) {
				barrack.train(UnitType.Terran_Marine);
			}
		}

		if (currentType == UnitType.Terran_Supply_Depot || currentType == UnitType.Terran_Barracks
				|| currentType == UnitType.Terran_Refinery) {
			Unit worker = getNotWorkingWorker();
			if (worker.canBuild(currentType)) {
				TilePosition position = null;

				if (game.self().getStartLocation().getX() < 50)
					position = new TilePosition(game.self().getStartLocation().getX() + 6,
							game.self().getStartLocation().getY());
				else
					position = new TilePosition(game.self().getStartLocation().getX() - 6,
							game.self().getStartLocation().getY());

				if (units.containsKey(currentType)) {
					position = units.get(currentType).get(units.get(currentType).size() - 1).getTilePosition();
				}

				worker.build(currentType, getPositionCanBuild(currentType, worker, position));
				
				if(unitQueue.indexOf(currentType) + 1 < unitQueue.size()) {					
					currentType = unitQueue.get(unitQueue.indexOf(currentType) + 1);
				} else {
					currentType = null;
				}
			}
		}
	}

	private TilePosition getPositionCanBuild(UnitType type, Unit unit, TilePosition position) {

		if (UnitType.Terran_Refinery == type) {
			Unit gas = BWTA.getNearestBaseLocation(game.self().getStartLocation().getPoint()).getGeysers().get(0);
			return gas.getTilePosition();
		}

		for (int i = 1; i <= 128; i++) {
			if (unit.canBuild(type, new TilePosition(position.getX() + i, position.getY()))) {
				position = new TilePosition(position.getX() + i, position.getY());
				break;
			} else if (unit.canBuild(type, new TilePosition(position.getX() - i, position.getY()))) {
				position = new TilePosition(position.getX() - i, position.getY());
				break;
			} else if (unit.canBuild(type, new TilePosition(position.getX(), position.getY() + i))) {
				position = new TilePosition(position.getX(), position.getY() + i);
				break;
			} else if (unit.canBuild(type, new TilePosition(position.getX(), position.getY() - i))) {
				position = new TilePosition(position.getX(), position.getY() - i);
				break;
			}
		}

		return position;
	}

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
			if (!(unit.isMoving() || unit.isGatheringGas() || unit.isGatheringMinerals()))
				return unit;
		}

		for (int i = 0; i < units.get(workerType).size(); i++) {
			if (units.get(workerType).get(i).isGatheringMinerals()) {
				return units.get(workerType).get(i);
			}
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
			unitQueue.remove(unit.getType());
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
