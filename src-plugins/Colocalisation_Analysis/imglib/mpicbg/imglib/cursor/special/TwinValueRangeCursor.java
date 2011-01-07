package imglib.mpicbg.imglib.cursor.special;

import mpicbg.imglib.container.Container;
import mpicbg.imglib.image.Image;
import imglib.mpicbg.imglib.cursor.special.meta.AlwaysTruePredicate;
import imglib.mpicbg.imglib.cursor.special.meta.Predicate;
import mpicbg.imglib.type.Type;

import java.util.Arrays;
import java.util.Iterator;

import mpicbg.imglib.cursor.Cursor;

/**
 * A class that walks over two images in a constrained way. Cursor
 * specific functionality that could provide only one type (like
 * iterator()  refer to the the first cursor that is passed to this
 * class. A pendent (abbended a "2") for the second cursor is available.
 *
 * Author: Tom Kazimiers
 */
public class TwinValueRangeCursor< T extends Type<T> & Comparable<T> > extends ConstraintCursor<T> {
	/**
	 * Creates a TwinValueRangeCursor without any restrictions in values.
	 * It allows iteration over all values of both cursors.
	 */
	public TwinValueRangeCursor(Cursor<T> cursor1, Cursor<T> cursor2) {
		this( cursor1, cursor2,
			new AlwaysTruePredicate<T>(),
			new AlwaysTruePredicate<T>() );
	}

	/**
	 * Creates a new TwinValueRangeCursor that limits access to the values.
	 * It will only iterate over values where both predicates relate
	 * according to the forward mode set.
	 *
	 * @param cursor1 The cursor for channel one
	 * @param cursor2 The cursor for channel two
	 * @param predicate1 The predicate for channel one
	 * @param predicate2 The predicate for channel two
	 */
	@SuppressWarnings("unchecked")
	public TwinValueRangeCursor(Cursor<T> cursor1, Cursor<T> cursor2, Predicate<T> predicate1, Predicate<T> predicate2 ) {
		super( cursor1, cursor2, predicate1, predicate2 );
	}

	@Override
	public Iterator<T> iterator() 
	{
		return cursor1.iterator();
	}
	
	public Iterator<T> iterator2() 
	{
		return cursor2.iterator();
	}

	@Override
	public T getType() {
		return getChannel1Type();
	}

	public T getType2() {
		return getChannel2Type();
	}

	@Override
	public Image<T> getImage() {
		return cursor1.getImage();
	}

	public Image<T> getImage2() {
		return cursor2.getImage();
	}

	@Override
	public int getArrayIndex() {
		return cursor1.getArrayIndex();
	}

	public int getArrayIndex2() {
		return cursor2.getArrayIndex();
	}

	@Override
	public int getStorageIndex() {
		return cursor1.getStorageIndex();
	}

	public int getStorageIndex2() {
		return cursor2.getStorageIndex();
	}

	@Override
	public Container<T> getStorageContainer() {
		return cursor1.getStorageContainer();
	}

	public Container<T> getStorageContainer2() {
		return cursor2.getStorageContainer();
	}

	@Override
	public int[] createPositionArray() {
		return cursor1.createPositionArray();
	}	
	
	public int[] createPositionArray2() {
		return cursor2.createPositionArray();
	}	
	
	@Override
	public int getNumDimensions() {
		return cursor1.getNumDimensions();
	}
	
	public int getNumDimensions2() {
		return cursor2.getNumDimensions();
	}
	
	@Override
	public int[] getDimensions() {
		return cursor1.getDimensions();
	}
	
	public int[] getDimensions2() {
		return cursor2.getDimensions();
	}
	
	@Override
	public void getDimensions( int[] position ) {
		cursor1.getDimensions( position );
	}

	public void getDimensions2( int[] position ) {
		cursor2.getDimensions( position );
	}
}

