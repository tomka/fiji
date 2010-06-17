import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

/**
 * A {@link Result} is the abstract base for various kinds
 * of information generated by the colocalisation algorithms.
 * The {@link ResultContainer} keeps track of those and knows
 * only that interface.
 */
public abstract class Result {
	// The name of the result
	String name;
	
	/**
	 * Creates a new result with the specified name.
	 * @param name The name of the result
	 */
	protected Result(String name) {
		super();
		this.name = name;
	}
	
	/**
	 * A {@link CompositeResult} can contain of several other
	 * results and hence allows the encapsulation of Results
	 * connected semantically.
	 */
	static class CompositeResult extends Result implements Iterable<Result> {

		/* A list of composite results. It allows a combination
		 * of results to be encapsulated within one object. 
		 */
		List<Result> compositeResults = new ArrayList<Result>();
		
		/**
		 * Creates a new {@link CompositeResult} with a name and a list
		 * of initial results to be contained in it.
		 * 
		 * @param name The name of the {@link CompositeResult}
		 * @param results The results to be contained in the {@link CompositeResult}
		 */
		public CompositeResult(String name, final Collection<Result> results) {
			super(name);
			addResults(results);
		}
		
		/**
		 * Creates an empty {@link CompositeResult}.
		 * @param name The name of the new {@link CompositeResult}
		 */
		public CompositeResult(String name) {
			super(name);
		}
		
		/**
		 * Adds a {@link Collection} of results to the {@link CompositeResult}. 
		 * 
		 * @param results The results to add
		 */
		public void addResults(final Collection<Result> results) {
			compositeResults.addAll(results);
		}
		
		/**
		 * Adds one {@link Result} to the {@link CompositeResult}
		 * @param result The result to add
		 */
		public void add(final Result result) {
			compositeResults.add(result);
		}

		/**
		 * Gets an iterator over the contents.
		 */
		public Iterator<Result> iterator() {
			return compositeResults.iterator();
		}
	
	}

	/**
	 * Represents a {@link Result} that contains an {@link Image}.
	 * 
	 * @param <T>
	 */
	static class ImageResult <T extends RealType<T>> extends Result {
		Image<T> data;

		public ImageResult(String name, Image<T> data) {
			super(name);
			this.data = data;
		}
	}
	
	/**
	 * Represents a calibrated image result. Additional meta information
	 * is kept in here. A typical example is the colocalisation image
	 * where calibration information is available.
	 *
	 * @param <T>
	 */
	static class CalibratedImageResult <T extends RealType<T>> extends ImageResult<T> {

		//TODO Add calibration/meta information
		public CalibratedImageResult(String name, Image<T> data) {
			super(name, data);
		}
	}
	
	/**
	 * Represents a 2D Histogram result. This is basically an image and
	 * axis label information. A scatterplot could be stored in here.
	 * 
	 * @param <T>
	 */
	static class Histogram2DResult <T extends RealType<T>> extends ImageResult<T>{
		// Axis labels
		String xLabel, yLabel;
		
		public Histogram2DResult(String name, Image<T> data, String xLabel, String yLabel) {
			super(name, data);
			this.xLabel = xLabel;
			this.yLabel = yLabel;
		}
	}
	
	static class ValueArrayResults extends Result{
		double[] values;
		double[] time;
		double[] thresholds;
		
		public ValueArrayResults(String name, double[] values, double[] time, double[] thresholds) {
			super(name);
			this.values = values;
			this.time = time;
			this.thresholds = thresholds;
		}
	}
	
	static class ValueResult extends Result {
		double value;
		double time;
		double thresholds;
		int decimalPlaces;
		
		public ValueResult(String name, double value, double time, double thresholds, int decimalPlaces) {
			super(name);
			this.value = value;
			this.time = time;
			this.thresholds = thresholds;
			this.decimalPlaces = decimalPlaces;
		}
	}
	
	/**
	 * A simple result type which stores a double value along
	 * with a number of decimal places.
	 */
	static class SimpleValueResult extends Result {
		double value;
		int decimalPlaces;

		/**
		 * Creates a new {@link SimpleValueResult} with a default number
		 * of 6 decimal places.
		 * 
		 * @param name The name of the new result
		 * @param value The value of the new result
		 */
		public SimpleValueResult(String name, double value) {
			this(name, value, 6);
		}
		
		/**
		 * Creates a new {@link SimpleValueResult}.
		 * 
		 * @param name The name of the new result
		 * @param value The value of the new result
		 * @param decimalPlaces The number of decimal places
		 */
		public SimpleValueResult(String name, double value, int decimalPlaces) {
			super(name);
			this.value = value;
			this.decimalPlaces = decimalPlaces;
		}
		
		public double getValue() {
			return value;
		}
		
		public int getDecimalPlaces() {
			return decimalPlaces;
		}
	}
}