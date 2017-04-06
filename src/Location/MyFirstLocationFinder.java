package Location;


import Utils.MacRssiPair;
import Utils.Position;
import Utils.Utils;

import java.awt.Rectangle;
import java.util.*;

/**
 * Simple Location finder that returns the first known APs location from the list of received MAC addresses
 * @author Bernd
 *
 */
public class MyFirstLocationFinder implements LocationFinder{

	private HashMap<String, Position> knownLocations; //Contains the known locations of APs. The long is a MAC address.
	private Double myReferenceValue = -40.0;
	private Double mySignalExp = 3.4;
	public MyFirstLocationFinder(){
		knownLocations = Utils.getKnownLocations5GHz(); //Put the known locations in our hashMap
		//knownLocations.putAll(Utils.getKnownLocations());
	}

	@Override
	public Position locate(MacRssiPair[] data) {
		printMacs(data); //print all the received data
		return getAveragePosition(data); //return the first known APs location
	}
	
	/**
	 * Returns the position of the strongest known AP found in the list of MacRssi pairs
	 * @param data
	 * @return
	 */


	private Position getStrongestFromList(MacRssiPair[] data){
		int bestRssi = -100;
		String correspondingMac = "";
		Position myPos = new Position(0,0);
		for(int i=0; i<data.length; i++){
			if(knownLocations.containsKey(data[i].getMacAsString())){
				if( data[i].getRssi() > bestRssi){
					bestRssi = data[i].getRssi();
					correspondingMac = data[i].getMacAsString();
				}
			}
		}

		if (!correspondingMac.equals("")) {
			myPos = knownLocations.get(correspondingMac);
		}
		return myPos;
	}


	private Position getAveragePosition(MacRssiPair[] data){
		List<Position> measuredPositions = new ArrayList<>();
		List<Integer> correspondingRSSI = new ArrayList<>();
		for(int i=0; i<data.length; i++){
			if(knownLocations.containsKey(data[i].getMacAsString())) {
				measuredPositions.add(knownLocations.get(data[i].getMacAsString()));
				correspondingRSSI.add(data[i].getRssi());
			}
		}

		if (measuredPositions.size() > 0) {
			if (measuredPositions.size() > 1) {
				return calculatePosition(measuredPositions, correspondingRSSI);
			} else {
				return measuredPositions.get(0);
			}
		}

		return new Position(0,0);
	}
	private Position trilaterationPosition(MacRssiPair[] data){
		List<Position> measuredPositions = new ArrayList<>();
		List<Integer> correspondingRSSI = new ArrayList<>();
		HashMap<Position, Integer> allBeacons = new HashMap<>();
		for(int i=0; i<data.length; i++){
			if(knownLocations.containsKey(data[i].getMacAsString())) {
				Position pos = knownLocations.get(data[i].getMacAsString());
				int rssi = data[i].getRssi();
				allBeacons.put(pos, rssi);
			}
		}
		if(allBeacons.size() > 2) {
			//List<Integer> top3 = getTop3Index(correspondingRSSI);
			List<Rectangle> rectList = new ArrayList<>();
			for (Map.Entry<Position, Integer> entry : allBeacons.entrySet()) {
				rectList.add(createRect(entry.getKey(), entry.getValue()));
			}

			Rectangle rectInter = getIntersection(rectList.get(0), rectList.get(1));
			for (int i = 2; i < rectList.size(); i++){
				rectInter = getIntersection(rectInter, rectList.get(i));
			}
			/*Rectangle rect1 = createRect(beacon1, correspondingRSSI.get(top3.get(0)));
			Rectangle rect2 = createRect(beacon2, correspondingRSSI.get(top3.get(1)));
			Rectangle rect3 = createRect(beacon3, correspondingRSSI.get(top3.get(2)));
			Rectangle inters12 = getIntersection(rect1, rect2);
			Rectangle finalInter = getIntersection(inters12, rect3);*/
			System.out.println("final rect " + rectInter.toString());
			return getCenter(rectInter);
		}
		else {
			return calculatePosition(measuredPositions, correspondingRSSI);
		}
	}

	private Position calculatePosition(List<Position> positions, List<Integer> values ){
		List<Double> weights = new ArrayList<>();
		Double sum = 0.0;
		for (int value : values){
			Double x = (myReferenceValue/value);
			weights.add(Math.pow(Math.E, (5*x)));
		}
		for (Double weight : weights){
			sum += weight;
		}
		int totalWeightedX = 0;
		int totalWeightedY = 0;
		for (Position pos : positions) {
			Double weight = weights.get(positions.indexOf(pos));
			totalWeightedX += (pos.getX()*weight);
			totalWeightedY += (pos.getY()*weight);
		}
		return new Position(totalWeightedX/sum , totalWeightedY/sum);
	}
	private ArrayList<Integer> getTop3Index(List<Integer> list){
		List<Integer> mySortedList = list;
		Collections.sort(mySortedList, Collections.reverseOrder());
		ArrayList<Integer> myIndex = new ArrayList<>();
		List<Integer> top3 = new ArrayList<>(mySortedList.subList(0,3));
		System.out.println(top3);
		for ( int i : top3){
			myIndex.add(list.indexOf(i));
		}
		return myIndex;
	}

	private int calculateD(int rssi){
		//Double dist = Math.pow(10,(myReferenceValue - rssi)/(10*mySignalExp));
		int distInt = (int) (Math.pow(10,(myReferenceValue - rssi)/(10*mySignalExp))*8.6); // *8.6 to rescale
		//System.out.println("double " + dist + " " + distInt);
		return distInt;
	}

	private Rectangle createRect(Position pos, int rssi){
		int d = calculateD(rssi);
		int wh = 2 * d;
		int x = (int) pos.getX() - d;
		int y = (int) pos.getY() - d;
		return new Rectangle(x,y,wh,wh);
	}
	private Rectangle getIntersection(Rectangle r1, Rectangle r2){
		Rectangle inters = r1.intersection(r2);
		return inters;
	}
	private Position getCenter(Rectangle r1){
		return new Position(r1.getX() + (r1.getWidth()/2), r1.getY() + (r1.getHeight()/2));
	}

	private void printMacs(MacRssiPair[] data) {
		for (MacRssiPair pair : data) {
			if(knownLocations.containsKey(pair.getMacAsString())) {
				System.out.println(pair + " " + knownLocations.get(pair.getMacAsString()));
			}
		}
	}

	public class Circle {
		public double x, y, radius;

		public Circle(double x, double y, double radius) {
			this.x = x;
			this.y = y;
			this.radius = radius;
		}
	}
}
