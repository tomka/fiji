package fiji.plugin.nperry.tracking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import mpicbg.imglib.algorithm.math.MathLib;

import fiji.plugin.nperry.Spot;

/**
 * <p>This class takes a list of {@link Spots} from time point <code>t</code>, and "links" each Spot to the nearest
 * <code>nLinks</code> Spots in time point <code>t+1</code>. The measure of distance used is Euclidean distance.</p>
 * 
 * <p>The user has the option of specifying a maximum search distance, such that a Spot in time <code>t</code> can only be
 * linked to Spots in <code>t+1</code> that are within this distance.</p>
 * 
 * <p>In the event that the user specifies a maximum search distance, and there are more than <code>nLinks</code> Spots
 * in <code>t+1</code> within this distance for a Spot in <code>t</code>, two rules are used to select which <code>nLink</code>
 * Spots in <code>t+1</code> are used:
 * 
 * <ol>
 * <li>Spots in <code>t+1</code> that have are not linked to an object in <code>t</code> are selected.</li>
 * <li>If there is still contention, the closest Spots are chosen.</li>
 * </ol>
 * 
 * @author nperry
 *
 */
public class NearestNeighborLinker {
	
	/*
	 * FIELDS
	 */
	
	/** The Spots belonging to time point <code>t</code>. */
	private ArrayList<Spot> t0;
	/** The Spots belonging to time point <code>t+1</code>. */
	private ArrayList<Spot> t1;
	/** The maximum number of Spots in time <code>t+1</code> Spots in time <code>t</code> can be linked to. If not specified
	 * 0 is used as a flag. */
	private int nLinks;
	/** The maximum distance away from the center of a Spot in time <code>t</code> that a Spot in time <code>t+1</code>
	 * can be in order to be linked (centroid to centroid Euclidean distance, in physical units). If the user does not specify
	 * a maximum distance, this variable is set to positive infinity to indicate there is no maximum distance. */
	private float maxDist;
	/** Each index represents a Spot in t0 (same index), and holds a list of the Spots in <code>t+1</code> that are linked 
	 * to it. */
	private ArrayList< ArrayList<Spot> > links;
	/** This String will hold any error message incurred during the use of this class. */
	private String errorMessage = "";
	/** Used to store whether checkInput() was run or not. */
	private boolean inputChecked = false;
	
	
	/*
	 * CONSTRUCTORS
	 */
	
	public NearestNeighborLinker(ArrayList<Spot> t0, ArrayList<Spot> t1, int nLinks, float maxDist)
	{
		this.t0 = t0;
		this.t1 = t1;
		this.nLinks = nLinks;
		this.maxDist = maxDist;
		this.links = new ArrayList< ArrayList<Spot> >();
	}
	
	
	public NearestNeighborLinker(ArrayList<Spot> t0, ArrayList<Spot> t1, int nLinks)
	{
		this(t0, t1, nLinks, Float.POSITIVE_INFINITY);
		this.links = new ArrayList< ArrayList<Spot> >();
	}
	
	
	public NearestNeighborLinker(ArrayList<Spot> t0, ArrayList<Spot> t1, float maxDist)
	{
		this(t0, t1, 0, maxDist);
		this.links = new ArrayList< ArrayList<Spot> >();
	}
	
	
	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * Returns the max number of Spots in t+1 to which a Spot in t can be linked.
	 * @return The max number of links.
	 */
	public int getNLinks()
	{
		return this.nLinks;
	}
	
	
	/**
	 * Returns the maximum distance parameter which determines how far away a Spot in t+1 can be from a Spot in t in
	 * order to be linked.
	 * @return The maximum distance between potentially link-able Spots.
	 */
	public float getMaxDist()
	{
		return this.maxDist;
	}
	
	
	/**
	 * Returns a map, where keys are Spots in t, and values are a list of Spots in t+1 that have been linked to the
	 * Spot in t.
	 * @return The computed list of links for each object in time t as a map.
	 */
	public HashMap< Spot, ArrayList<Spot> > getResult()
	{
		HashMap< Spot, ArrayList<Spot> > result = new HashMap< Spot, ArrayList<Spot> >();
		
		for (int i = 0; i < t0.size(); i++) {
			result.put(t0.get(i), links.get(i));
		}
		
		return result;
	}
	
	
	/**
	 * Returns any error messages that develop during the use of this class.
	 * @return The error message.
	 */
	public String getErrorMessage() {
		return this.errorMessage;
	}
	
	
	/**
	 * Call this method before calling process, in order to guarantee that all of the required input is correct.
	 * @return true if no errors, false otherwise.
	 */
	public boolean checkInput()
	{
		// Check that t0 isn't empty
		if (t0.isEmpty()) {
			errorMessage = "The ArrayList for time point t (t0) is empty.";
			return false;
		}
		
		// Check that t1 isn't empty
		if (t1.isEmpty()) {
			errorMessage = "The ArrayList for time point t+1 (t1) is empty.";
			return false;
		}
		
		// Check that maxDist >= 0
		if (maxDist < 0) {
			errorMessage = "The maximum search distance supplied is negative.";
			return false;
		}
		
		// Check that nLinks >= 0 (0 is internal flag signifying no limit)
		if (nLinks < 0) {
			errorMessage = "The number of links to find is negative.";
			return false;
		}
		
		// If we got here, we are fine.
		inputChecked = true;
		return true;
	}

	
	/**
	 * Executes the functionality of the class - link Spots in time point t to Spots in t+1 based on the parameters
	 * passed at construction (only consider Spots within a certain distance, and limit the number of links made).
	 * @return True if the method runs successfully, false otherwise.
	 */
	public boolean process()
	{
		// Ensure that checkInput() was run before executing
		if (!inputChecked) {
			errorMessage = "checkInput() was never run.";
			return false;
		}
		
		//Initialize local vars
		final float maxDistSq = maxDist * maxDist;								// Prevents us from using the costly Math.sqrt() function for Euclidean distance checks
		final HashMap<Spot, Integer> numTimesLinked = new HashMap<Spot, Integer>();	// A HashMap to keep track of how many times Spots in t+1 have been linked to Spots in t. 
		final ArrayList< HashMap<Spot, Float> > distances = new ArrayList< HashMap<Spot, Float> >();  // For the points we add as links in part (1) below, store the distances we calculate for later pruning
		
		//Add all Spots from t1 into the numLinks hashmap, with an initial count of 0 (they are all unlinked at this point).
		for (int i = 0; i < t1.size(); i++) {
			numTimesLinked.put(t1.get(i), 0);
		}
		
		//For each Spot in t, find *all* potential Spots in t+1 within maxDist to link to (could be > nLinks).
		linkAllSpotsWithinMaxDistance(maxDistSq, numTimesLinked, distances);
		
		// Trim down the number of Spots in t+1 linked to Spot in t to <= nLinks, if nLinks specified
		if (nLinks > 0) {
			for (int i = 0; i < links.size(); i++) {	// For all Spots in t...
				if (links.get(i).size() > nLinks) {		// If there are more than nLinks for this Spot...
					reduceNumberOfLinksForSpot(links.get(i), numTimesLinked, distances.get(i));
				}
			}
		}
		
		// Process() finished
		return true;
	}
	
	
	/**
	 * <p>Reduces the number of Spots in t+1 that a Spot in t is linked to, based on the following (ordered by important) 
	 * priorities:<p>
	 * 
	 * <ol>
	 * <li>Spots in t+1 not linked to any other Spots in t are preferentially kept.</li>
	 * <li>The nearest Spots are kept.</li>
	 * </ol>
	 * 
	 * @param linked A list of Spots in t+1 that the current Spot is linked to.
	 * @param numTimesLinked A map pairing Spots in t+1 and how many times they've been linked to Spots in t.
	 * @param distances A map pairing Spots in t+1 and their distance from the Spot in t.
	 */
	private void reduceNumberOfLinksForSpot(ArrayList<Spot> linked, HashMap<Spot, Integer> numTimesLinked, HashMap<Spot, Float> distances)
	{
		// Create a duplicate list, and clear the real one
		ArrayList<Spot> dup = new ArrayList<Spot>(linked);
		linked.clear();
		HashMap<Spot, Float> distMapNotLinkedAnymore = new HashMap<Spot, Float>(distances);
		HashMap<Spot, Float> distMapLinked = new HashMap<Spot, Float>();
		
		// Add back only the Spots that are not linked to anything else
		for (int j = 0; j < dup.size(); j++) {
			Spot s = dup.get(j);
			if (numTimesLinked.get(s) == 1) {	// If == 1, then this Spot in t+1 is only linked to the current Spot in t.
				linked.add(s);
				distMapLinked.put(s, distMapNotLinkedAnymore.get(s));	// Add, so we keep track of the distances of each Spot in case we later have too many
				distMapNotLinkedAnymore.remove(s);						// Remove, so that distMap has only Spots that aren't linked anymore
				
			}
		}
		
		// We might have the right number now.
		if (linked.size() == nLinks) return;	
		
		// If not, check to see if we are over or under quota, and act appropriately.
	
		// Case 1 - Too many links still, so remove those farthest away.
		if (linked.size() > nLinks) {	
			// Make a new map, with keys as floats, and values as Spots, so that there is a natural ordering
			TreeMap<Float, Spot> invertedDistMap = invertMap(distMapLinked);
			
			// Remove farthest spots in t+1
			while (linked.size() > nLinks) {
				Float toRemove = invertedDistMap.lastKey();	// This is the smallest distance
				linked.remove(invertedDistMap.get(toRemove));		// Add the corresponding Spot to the linked list
				invertedDistMap.remove(toRemove);				// So we don't add it again
			}
			
		}
		
		// Case 2 - Not enough links, add the next closest until we have enough
		else {
			// Make a new map, with keys as floats, and values as Spots, so that there is a natural ordering
			TreeMap<Float, Spot> invertedDistMap = invertMap(distMapNotLinkedAnymore);
			
			// Add back closest spots in t+1
			while (linked.size() < nLinks) {
				Float toAdd = invertedDistMap.firstKey();	// This is the smallest distance
				linked.add(invertedDistMap.get(toAdd));		// Add the corresponding Spot to the linked list
				invertedDistMap.remove(toAdd);				// So we don't add it again
			}
		}
	}

	
	/**
	 * Links all the Spots in t+1 that are within maxDist to each Spot in t. Note, this could be more than nLinks.
	 * 
	 * @param maxDistSq The maxDist parameter, provided at construction, squared so we can avoid the costly Math.sqrt.
	 * @param numTimesLinked A HashMap recording for each Spot in t+1 how many times it was linked.
	 * @param distances For each Spot in t, stores a map which holds all Spots it's linked to in t+1 plus their distance away.
	 */
	private void linkAllSpotsWithinMaxDistance(float maxDistSq, HashMap<Spot, Integer> numTimesLinked, ArrayList< HashMap<Spot, Float> > distances)
	{
		// Initialize variables so we don't recreate them every iteration
		float[] currCoords = new float[t0.get(1).getCoordinates().length];		// We are guaranteed the existence of at least one Spot in t0, because of checkInput.
		float[] potentialCoords = new float[t0.get(1).getCoordinates().length];
		float dist = 0;
		
		for (int i = 0; i < t0.size(); i++) {	// For all Spots in t
			currCoords = t0.get(i).getCoordinates();
			HashMap<Spot, Float> distMap = new HashMap<Spot, Float>();	// store the relevant distances we calculate for this Spot to Spots in t+1
			ArrayList<Spot> currLinks = new ArrayList<Spot>();
			for (int j = 0; j < t1.size(); j++) {	// For all Spots in t+1
				potentialCoords = t1.get(j).getCoordinates();
				dist = getEucDistSq(currCoords, potentialCoords);
				if (dist <= maxDistSq) {
					currLinks.add(t1.get(j));	// Add this Spot j in t+1 as a link to our Spot i in t
					incrementCount(numTimesLinked, t1.get(j));
					distMap.put(t1.get(j), dist);
				}
			}
			links.add(currLinks);
			distances.add(distMap);	// Store the distances for each Spot in t+1 linked to the current Spot in t.
		}
	}
	
	
	/**
	 * Takes a hash map, and creates a new tree map with the keys in the hash map as values, and 
	 * the values as keys.
	 * @param hash The hash map to invert.
	 * @return A tree map with the hash map's values as keys, and the hash map's keys as values.
	 */
	private static TreeMap<Float, Spot> invertMap(HashMap<Spot, Float> hash)
	{
		Set<Spot> spots = hash.keySet();
		TreeMap<Float, Spot> inverted = new TreeMap<Float, Spot>();
		Iterator<Spot> itr = spots.iterator();
		while (itr.hasNext()) {
			Spot spot = itr.next();
			inverted.put(hash.get(spot), spot);
		}
		return inverted;
	}
	
	
	/**
	 * Helper method which increments the count of links made to this Spot.
	 * @param numLinks The HashMap storing the number of links made to each Spot.
	 * @param spot	The Spot for which the number of links should be incremented.
	 */
	private static void incrementCount(HashMap<Spot, Integer> numLinks, Spot spot) 
	{
		int count = numLinks.get(spot);
		count++;
		numLinks.put(spot, count);
	}
	
	
	/**
	 * This method returns the (Euclidean distance)^2 between two points.
	 * 
	 * @param a The coordinates of the first point.
	 * @param b The coordinates of the second point.
	 * @return The Euclidean distance squared.
	 */
	private static float getEucDistSq(float[] a, float[] b) {
		float total = 0;
		for (int i = 0; i < a.length; i++) {
			total += ((a[i] - b[i]) * (a[i] - b[i]));
		}
		return total;
	}
	
	
	/**
	 * For testing
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		 * Set up params
		 */
		ArrayList<Spot> t0 = new ArrayList<Spot>();
		ArrayList<Spot> t1 = new ArrayList<Spot>();
		
		// T0
		t0.add(new Spot(new float[] {27.39f, 39.69f, 20f}));
		t0.add(new Spot(new float[] {9.12f, 18.66f, 20f}));
		t0.add(new Spot(new float[] {52.99f, 8.53f, 12f}));
		
		// T1
		t1.add(new Spot(new float[] {30.96f, 43.66f, 18f}));
		t1.add(new Spot(new float[] {27.79f, 33.54f, 18f}));
		t1.add(new Spot(new float[] {52.40f, 10.32f, 10f}));
		t1.add(new Spot(new float[] {50.40f, 10.32f, 10f}));
		t1.add(new Spot(new float[] {48.40f, 10.32f, 10f}));
		t1.add(new Spot(new float[] {46.40f, 10.32f, 10f}));
		t1.add(new Spot(new float[] {44.40f, 10.32f, 10f}));
		
		/*
		 * Execute linker!
		 */
		NearestNeighborLinker linker = new NearestNeighborLinker(t0, t1, 3, 10f);  // max of 3 links, 10f max dist away
		if (!linker.checkInput() || !linker.process()) {
			System.out.println("Linker failed with error: " + linker.getErrorMessage());
		}
		HashMap< Spot, ArrayList<Spot> > links = linker.getResult();
		System.out.println("Results:");
		System.out.println("________________");
		System.out.println();
		for (int i = 0; i < links.size(); i++) {
			ArrayList<Spot> spot = links.get(i);
			System.out.println("Spot at " + MathLib.printCoordinates(t0.get(i).getCoordinates()) + " is linked to: ");
			for (int j = 0; j < spot.size(); j++) {
				System.out.println(MathLib.printCoordinates(spot.get(j).getCoordinates()));
			}
			System.out.println();
		}
		
		
	}
}

