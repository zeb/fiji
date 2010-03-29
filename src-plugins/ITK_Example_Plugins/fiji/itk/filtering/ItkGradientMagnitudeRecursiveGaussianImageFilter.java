//
// ItkGradientMagnitudeRecursiveGaussianImageFilter.java
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

package fiji.itk.filtering;

import fiji.itk.ItkException;
import fiji.itk.ItkProxy;
import fiji.itk.base.ItkImage;

/**
 * A proxy for the itkGradientMagnitudeRecursiveGaussianImageFilter* classes
 * (IF2IF2, IUC2IUC2, etc.).
 */
public class ItkGradientMagnitudeRecursiveGaussianImageFilter extends ItkProxy {
	
	public static final String BASE_CLASS =
		"org.itk.filtering.itkGradientMagnitudeRecursiveGaussianImageFilter";
	
	// -- Constructors --
	
	public ItkGradientMagnitudeRecursiveGaussianImageFilter(String inType,
		int inDim, String outType, int outDim) throws ItkException
	{
		this(newDelegate(BASE_CLASS, inType, inDim, outType, outDim, true),
			inType, inDim, outType, outDim);
	}
	
	public ItkGradientMagnitudeRecursiveGaussianImageFilter(Object delegate,
		String inType, int inDim, String outType, int outDim) throws ItkException
	{
		super(delegate, inType, inDim, outType, outDim);
	}
	
	// -- ItkGradientMagnitudeRecursiveGaussianImageFilter methods --
	
	public ItkImage getOutput() throws ItkException {
		return wrap(invoke("GetOutput"), ItkImage.class, outType, outDim);
	}
	
	public void setInput(ItkImage image) throws ItkException {
		invoke("SetInput", image);
	}
	
	public void setSigma(double sigma) throws ItkException {
		invoke("SetSigma", sigma);
	}
	
	public void update() throws ItkException {
		invoke("Update");
	}
	
}
