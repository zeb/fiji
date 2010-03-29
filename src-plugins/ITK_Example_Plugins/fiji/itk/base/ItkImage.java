//
// ItkImage.java
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

package fiji.itk.base;

import fiji.itk.ItkException;
import fiji.itk.ItkProxy;

/** A proxy for the itkImage* classes (F2, F3, UC2, UC3, etc.). */
public class ItkImage extends ItkProxy {

	public static final String BASE_CLASS = "org.itk.base.itkImage";
	
	// -- Constructors --

	public ItkImage(String type, int dim) throws ItkException {
		this(newDelegate(BASE_CLASS, type, dim), type, dim);
	}

	public ItkImage(Object delegate, String type, int dim) throws ItkException {
		super(delegate, type, dim);
	}

	// -- ItkImage methods --
	
	public void allocate() throws ItkException {
		invoke("Allocate");
	}

	public ItkImageRegion getLargestPossibleRegion() throws ItkException {
		return wrap(invoke("GetLargestPossibleRegion"), ItkImageRegion.class, dim);
	}

	public Number getPixel(ItkIndex index) throws ItkException {
		return (Number) invoke("GetPixel", index);
	}

	public void setPixel(ItkIndex index, float value) throws ItkException {
		invoke("SetPixel", index, convertType(type, value));
	}

	public void setRegions(ItkImageRegion region) throws ItkException {
		invoke("SetRegions", region);
	}

	public void update() throws ItkException {
		invoke("Update");
	}
	
	// -- Helper methods --
	
}
