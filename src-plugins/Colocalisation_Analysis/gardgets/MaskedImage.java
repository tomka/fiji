package gadgets;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.LocalizablePlaneCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.numeric.NumericType;
import imglib.mpicbg.imglib.cursor.special.MaskCursor;
import imglib.mpicbg.imglib.cursor.special.MaskLocalizableCursor;

/**
 * A MaskedImage decorates an ImgLib Image in a way that cursors on it
 * will only walk through the region of interest specified. Since not
 * all cursors can handle that in an easy way, some are not usable
 * with that class yet.
 *
 * For now the mask must be of the same type of the image and is bound
 * to numeric types.
 */
public class MaskedImage<T extends NumericType<T>> extends Image<T> {
	// the image to operate on
	Image<T> image;
	// the mask te use for the image
	final Image<T> mask;
	// the offValue of the image (see MaskCursor)
	T offValue;
	// a factory to create MaskImage objects
	MaskedImageFactory<T> maskedImageFactory;

	/**
	 * Creates a new MaskedImage to decorate the passed image. Cursors
	 * created through that class will refer only to the ROI.
	 * 
	 * @param img The image to decorate.
	 * @param mask The mask for the image.
	 */
	public MaskedImage( Image<T> img, final Image<T> mask ) {
		super(img.getContainer(), img.createType(), img.getName());

		this.image = img;	
		this.mask = mask;
		// create the offValue of the mask
		offValue = mask.createType();
		offValue.setZero();
		// create a new factory
		maskedImageFactory = new MaskedImageFactory(mask, img.createType(), img.getContainerFactory());
	}

	@Override
	public ImageFactory<T> getImageFactory() {
		return maskedImageFactory;
	}
	
	@Override
	public Cursor<T> createCursor() {
		Cursor<T> cursor = image.createCursor();
		Cursor<T> maskCursor = mask.createCursor();
		return new MaskCursor(cursor, maskCursor, offValue);
	}

	@Override
	public LocalizableCursor<T> createLocalizableCursor() {
		LocalizableCursor<T> cursor = image.createLocalizableCursor();
		LocalizableCursor<T> maskCursor = mask.createLocalizableCursor();
		return new MaskLocalizableCursor(cursor, maskCursor, offValue);
	}
	
	@Override
	public LocalizableByDimCursor<T> createLocalizableByDimCursor() {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}
	
	/* Not implemented/needed methods follow */
	
	@Override
	public LocalizablePlaneCursor<T> createLocalizablePlaneCursor() {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}

	@Override
	public LocalizableByDimCursor<T> createLocalizableByDimCursor(
			OutOfBoundsStrategyFactory<T> factory) {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}

	@Override
	public Interpolator<T> createInterpolator(InterpolatorFactory<T> factory) {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}
}
