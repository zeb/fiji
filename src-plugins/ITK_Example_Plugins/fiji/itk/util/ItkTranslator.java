//
// ItkTranslator.java
//

/*
Fiji Java reflection wrapper for ITK.
Copyright (c) 2010, UW-Madison LOCI.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the UW-Madison LOCI nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY UW-MADISON LOCI ''AS IS'' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL UW-MADISON LOCI BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package fiji.itk.util;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.NumericType;
import mpicbg.imglib.type.numeric.DoubleType;
import mpicbg.imglib.type.numeric.FloatType;
import mpicbg.imglib.type.numeric.LongType;
import mpicbg.imglib.type.numeric.UnsignedByteType;
import mpicbg.imglib.type.numeric.UnsignedShortType;

import org.itk.base.itkImageD2;
import org.itk.base.itkImageD3;
import org.itk.base.itkImageF2;
import org.itk.base.itkImageF3;
import org.itk.base.itkImageUC2;
import org.itk.base.itkImageUC3;
import org.itk.base.itkImageUL2;
import org.itk.base.itkImageUL3;
import org.itk.base.itkImageUS2;
import org.itk.base.itkImageUS3;
import org.itk.base.itkIndex2;
import org.itk.base.itkIndex3;

import fiji.itk.ItkException;
import fiji.itk.base.ItkImage;

/**
 * ItkTranslator provides translation routines between imglib andITK.
 *
 * Because reflected method calls per-pixel are so slow, this class contains
 * hard dependencies to WrapITK classes, with strongly typed methods for each
 * supported combination of ITK image type and dimensionality.
 */
public final class ItkTranslator {
	
	private ItkTranslator() { }
	
	// -- General --
	
	/**
	 * Copies the data from the given imglib image
	 * into the specified ITK image.
	 * @param src The imglib image source.
	 * @param dest The ITK image destination.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends NumericType<T>> void copyToITK(Image<T> src,
		ItkImage dest) throws ItkException
	{
		String type = dest.getType();
		int dim = dest.getDimension();
		if (dim != 2 && dim != 3) {
			throw new ItkException("Unsupported dimension: " + dim);
		}
		Object var = dest.var();
		if (type.equals(ItkImage.TYPE_UINT8)) {
			Image<UnsignedByteType> img = (Image<UnsignedByteType>) src;
			if (dim == 2) copyToITK(img, (itkImageUC2) var);
			else if (dim == 3) copyToITK(img, (itkImageUC3) var);
		}
		else if (type.equals(ItkImage.TYPE_UINT16)) {
			Image<UnsignedShortType> img = (Image<UnsignedShortType>) src;
			if (dim == 2) copyToITK(img, (itkImageUS2) var);
			else if (dim == 3) copyToITK(img, (itkImageUS3) var);
		}
		else if (type.equals(ItkImage.TYPE_UINT64)) {
			Image<LongType> img = (Image<LongType>) src;
			if (dim == 2) copyToITK(img, (itkImageUL2) var);
			else if (dim == 3) copyToITK(img, (itkImageUL3) var);
		}
		else if (type.equals(ItkImage.TYPE_FLOAT)) {
			Image<FloatType> img = (Image<FloatType>) src;
			if (dim == 2) copyToITK(img, (itkImageF2) var);
			else if (dim == 3) copyToITK(img, (itkImageF3) var);
		}
		else if (type.equals(ItkImage.TYPE_DOUBLE)) {
			Image<DoubleType> img = (Image<DoubleType>) src;
			if (dim == 2) copyToITK(img, (itkImageD2) var);
			else if (dim == 3) copyToITK(img, (itkImageD3) var);
		}
		else throw new ItkException("Unsupported type: " + type);
	}
	
	/**
	 * Copies the data from the given ITK image into the specified imglib image.
	 * @param src The ITK image source.
	 * @param dest The imglib image destination.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends NumericType<T>> void copyFromITK(ItkImage src,
		Image<T> dest) throws ItkException
	{
		String type = src.getType();
		int dim = src.getDimension();
		if (dim != 2 && dim != 3) {
			throw new ItkException("Unsupported dimension: " + dim);
		}
		Object var = src.var();
		if (type.equals(ItkImage.TYPE_UINT8)) {
			Image<UnsignedByteType> img = (Image<UnsignedByteType>) dest;
			if (dim == 2) copyFromITK((itkImageUC2) var, img);
			else if (dim == 3) copyFromITK((itkImageUC3) var, img);
		}
		else if (type.equals(ItkImage.TYPE_UINT16)) {
			Image<UnsignedShortType> img = (Image<UnsignedShortType>) dest;
			if (dim == 2) copyFromITK((itkImageUS2) var, img);
			else if (dim == 3) copyFromITK((itkImageUS3) var, img);
		}
		else if (type.equals(ItkImage.TYPE_UINT64)) {
			Image<LongType> img = (Image<LongType>) dest;
			if (dim == 2) copyFromITK((itkImageUL2) var, img);
			else if (dim == 3) copyFromITK((itkImageUL3) var, img);
		}
		else if (type.equals(ItkImage.TYPE_FLOAT)) {
			Image<FloatType> img = (Image<FloatType>) dest;
			if (dim == 2) copyFromITK((itkImageF2) var, img);
			else if (dim == 3) copyFromITK((itkImageF3) var, img);
		}
		else if (type.equals(ItkImage.TYPE_DOUBLE)) {
			Image<DoubleType> img = (Image<DoubleType>) dest;
			if (dim == 2) copyFromITK((itkImageD2) var, img);
			else if (dim == 3) copyFromITK((itkImageD3) var, img);
		}
		else throw new ItkException("Unsupported type: " + type);
	}

	// -- UINT8 --
	
	/**
	 * Copies the data from the given imglib image
	 * into the specified ITK image.
	 * @param src The imglib image source.
	 * @param dest The ITK image destination.
	 */
	public static void copyToITK(Image<UnsignedByteType> src, itkImageUC2 dest) {
		LocalizableByDimCursor<UnsignedByteType> cursor =
			src.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			int value = cursor.getType().get();
			dest.SetPixel(createIndex2(pos), (short) value);
		}
	}

	/**
	 * Copies the data from the given ITK image into the specified imglib image.
	 * @param src The ITK image source.
	 * @param dest The imglib image destination.
	 */
	public static void copyFromITK(itkImageUC2 src, Image<UnsignedByteType> dest) {
		LocalizableByDimCursor<UnsignedByteType> cursor =
			dest.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			int value = src.GetPixel(createIndex2(pos));
			cursor.getType().set(value);
		}
	}

	/**
	 * Copies the data from the given imglib image
	 * into the specified ITK image.
	 * @param src The imglib image source.
	 * @param dest The ITK image destination.
	 */
	public static void copyToITK(Image<UnsignedByteType> src, itkImageUC3 dest) {
		LocalizableByDimCursor<UnsignedByteType> cursor =
			src.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			int value = cursor.getType().get();
			dest.SetPixel(createIndex3(pos), (short) value);
		}
	}

	/**
	 * Copies the data from the given ITK image into the specified imglib image.
	 * @param src The ITK image source.
	 * @param dest The imglib image destination.
	 */
	public static void copyFromITK(itkImageUC3 src, Image<UnsignedByteType> dest) {
		LocalizableByDimCursor<UnsignedByteType> cursor =
			dest.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			int value = src.GetPixel(createIndex3(pos));
			cursor.getType().set(value);
		}
	}
	
	// -- UINT16 --

	/**
	 * Copies the data from the given imglib image
	 * into the specified ITK image.
	 * @param src The imglib image source.
	 * @param dest The ITK image destination.
	 */
	public static void copyToITK(Image<UnsignedShortType> src, itkImageUS2 dest) {
		LocalizableByDimCursor<UnsignedShortType> cursor =
			src.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			int value = cursor.getType().get();
			dest.SetPixel(createIndex2(pos), value);
		}
	}

	/**
	 * Copies the data from the given ITK image into the specified imglib image.
	 * @param src The ITK image source.
	 * @param dest The imglib image destination.
	 */
	public static void copyFromITK(itkImageUS2 src, Image<UnsignedShortType> dest) {
		LocalizableByDimCursor<UnsignedShortType> cursor =
			dest.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			int value = src.GetPixel(createIndex2(pos));
			cursor.getType().set(value);
		}
	}

	/**
	 * Copies the data from the given imglib image
	 * into the specified ITK image.
	 * @param src The imglib image source.
	 * @param dest The ITK image destination.
	 */
	public static void copyToITK(Image<UnsignedShortType> src, itkImageUS3 dest) {
		LocalizableByDimCursor<UnsignedShortType> cursor =
			src.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			int value = cursor.getType().get();
			dest.SetPixel(createIndex3(pos), value);
		}
	}

	/**
	 * Copies the data from the given ITK image into the specified imglib image.
	 * @param src The ITK image source.
	 * @param dest The imglib image destination.
	 */
	public static void copyFromITK(itkImageUS3 src,
		Image<UnsignedShortType> dest)
	{
		LocalizableByDimCursor<UnsignedShortType> cursor =
			dest.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			int value = src.GetPixel(createIndex3(pos));
			cursor.getType().set(value);
		}
	}
	
	// -- UINT64 --

	/**
	 * Copies the data from the given imglib image
	 * into the specified ITK image.
	 * @param src The imglib image source.
	 * @param dest The ITK image destination.
	 */
	public static void copyToITK(Image<LongType> src, itkImageUL2 dest) {
		LocalizableByDimCursor<LongType> cursor =
			src.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			long value = cursor.getType().get();
			dest.SetPixel(createIndex2(pos), value);
		}
	}

	/**
	 * Copies the data from the given ITK image into the specified imglib image.
	 * @param src The ITK image source.
	 * @param dest The imglib image destination.
	 */
	public static void copyFromITK(itkImageUL2 src, Image<LongType> dest) {
		LocalizableByDimCursor<LongType> cursor =
			dest.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			long value = src.GetPixel(createIndex2(pos));
			cursor.getType().set(value);
		}
	}

	/**
	 * Copies the data from the given imglib image
	 * into the specified ITK image.
	 * @param src The imglib image source.
	 * @param dest The ITK image destination.
	 */
	public static void copyToITK(Image<LongType> src, itkImageUL3 dest) {
		LocalizableByDimCursor<LongType> cursor =
			src.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			long value = cursor.getType().get();
			dest.SetPixel(createIndex3(pos), value);
		}
	}

	/**
	 * Copies the data from the given ITK image into the specified imglib image.
	 * @param src The ITK image source.
	 * @param dest The imglib image destination.
	 */
	public static void copyFromITK(itkImageUL3 src, Image<LongType> dest) {
		LocalizableByDimCursor<LongType> cursor =
			dest.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			long value = src.GetPixel(createIndex3(pos));
			cursor.getType().set(value);
		}
	}
	
	// -- FLOAT --

	/**
	 * Copies the data from the given imglib image
	 * into the specified ITK image.
	 * @param src The imglib image source.
	 * @param dest The ITK image destination.
	 */
	public static void copyToITK(Image<FloatType> src, itkImageF2 dest) {
		LocalizableByDimCursor<FloatType> cursor =
			src.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			float value = cursor.getType().get();
			dest.SetPixel(createIndex2(pos), value);
		}
	}

	/**
	 * Copies the data from the given ITK image into the specified imglib image.
	 * @param src The ITK image source.
	 * @param dest The imglib image destination.
	 */
	public static void copyFromITK(itkImageF2 src, Image<FloatType> dest) {
		LocalizableByDimCursor<FloatType> cursor =
			dest.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			float value = src.GetPixel(createIndex2(pos));
			cursor.getType().set(value);
		}
	}

	/**
	 * Copies the data from the given imglib image
	 * into the specified ITK image.
	 * @param src The imglib image source.
	 * @param dest The ITK image destination.
	 */
	public static void copyToITK(Image<FloatType> src, itkImageF3 dest) {
		LocalizableByDimCursor<FloatType> cursor =
			src.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			float value = cursor.getType().get();
			dest.SetPixel(createIndex3(pos), value);
		}
	}

	/**
	 * Copies the data from the given ITK image into the specified imglib image.
	 * @param src The ITK image source.
	 * @param dest The imglib image destination.
	 */
	public static void copyFromITK(itkImageF3 src, Image<FloatType> dest) {
		LocalizableByDimCursor<FloatType> cursor =
			dest.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			float value = src.GetPixel(createIndex3(pos));
			cursor.getType().set(value);
		}
	}

	// -- DOUBLE --

	/**
	 * Copies the data from the given imglib image
	 * into the specified ITK image.
	 * @param src The imglib image source.
	 * @param dest The ITK image destination.
	 */
	public static void copyToITK(Image<DoubleType> src, itkImageD2 dest) {
		LocalizableByDimCursor<DoubleType> cursor =
			src.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			double value = cursor.getType().get();
			dest.SetPixel(createIndex2(pos), value);
		}
	}

	/**
	 * Copies the data from the given ITK image into the specified imglib image.
	 * @param src The ITK image source.
	 * @param dest The imglib image destination.
	 */
	public static void copyFromITK(itkImageD2 src, Image<DoubleType> dest) {
		LocalizableByDimCursor<DoubleType> cursor =
			dest.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			double value = src.GetPixel(createIndex2(pos));
			cursor.getType().set(value);
		}
	}

	/**
	 * Copies the data from the given imglib image
	 * into the specified ITK image.
	 * @param src The imglib image source.
	 * @param dest The ITK image destination.
	 */
	public static void copyToITK(Image<DoubleType> src, itkImageD3 dest) {
		LocalizableByDimCursor<DoubleType> cursor =
			src.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			double value = cursor.getType().get();
			dest.SetPixel(createIndex3(pos), value);
		}
	}

	/**
	 * Copies the data from the given ITK image into the specified imglib image.
	 * @param src The ITK image source.
	 * @param dest The imglib image destination.
	 */
	public static void copyFromITK(itkImageD3 src, Image<DoubleType> dest) {
		LocalizableByDimCursor<DoubleType> cursor =
			dest.createLocalizableByDimCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			int[] pos = cursor.getPosition();
			double value = src.GetPixel(createIndex3(pos));
			cursor.getType().set(value);
		}
	}

	// -- Helper methods --
	
	/** Creates a 2D ITK position index from the given coordinates. */
	private static itkIndex2 createIndex2(int[] pos) {
		itkIndex2 index = new itkIndex2();
		for (int i=0; i<pos.length; i++) {
			index.SetElement(i, pos[i]);
		}
		return index;
	}

	/** Creates a 3D ITK position index from the given coordinates. */
	private static itkIndex3 createIndex3(int[] pos) {
		itkIndex3 index = new itkIndex3();
		for (int i=0; i<pos.length; i++) {
			index.SetElement(i, pos[i]);
		}
		return index;
	}

}
