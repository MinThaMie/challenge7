package Location;

import Utils.MacRssiPair;
import Utils.Position;
import Utils.Utils;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple Location finder that returns the first known APs location from the list of received MAC addresses
 * @author Bernd
 *
 */
public class MyFirstLocationFinder implements LocationFinder{

	private HashMap<String, Position> knownLocations; //Contains the known locations of APs. The long is a MAC address.
	private Position myPosition = new Position(0,0);
	private String myRouter = "";
	private int rssi = -100;
	public MyFirstLocationFinder(){
		knownLocations = Utils.getKnownLocations(); //Put the known locations in our hashMap
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

	private Position calculatePosition(List<Position> positions, List<Integer> values ){

		if (list.size() == 2){
			Position pos1 = list.get(0);
			Position pos2 = list.get(1);
			int x = (int) (pos1.getX() + pos2.getX()) / 2;
			int y = (int) (pos1.getY() + pos2.getY()) / 2;
			return new Position(x,y);
		} else {
			Position pos1 = list.get(top3.get(0));
			Position pos2 = list.get(top3.get(1));
			Position pos3 = list.get(top3.get(2));
			int x = (int) (pos1.getX() + pos2.getX() + pos3.getX()) / 3;
			int y = (int) (pos1.getY() + pos2.getY() + pos3.getY()) / 3;
			return new Position(x,y);
		}
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
	/**
	 * Outputs all the received MAC RSSI pairs to the standard out
	 * This method is provided so you can see the data you are getting
	 * @param data
	 */
	private void printMacs(MacRssiPair[] data) {
		for (MacRssiPair pair : data) {
			if(knownLocations.containsKey(pair.getMacAsString())) {
				System.out.println(pair + " " + knownLocations.get(pair.getMacAsString()));
			}
		}
	}
}
