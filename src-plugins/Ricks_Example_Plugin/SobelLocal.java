

import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice.Type;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLProgram;

public class SobelLocal implements PlugInFilter {
    
    @Override
	public void run(ImageProcessor imp) 
    {
        // TODO: Adapt conditional logic after low level JOCL libs are adapted
	boolean is64bit = System.getProperty("os.arch", "").indexOf("64") >= 0;
	if(is64bit)
        {
	    if (System.getProperty("os.name", "<unknown>").equals("Linux") ) { JNI.loadLibrary( "JOCL-linux-x86_64" ); }
	    if (System.getProperty("os.name", "<unknown>").equals("Mac OS X") ) { JNI.loadLibrary( "JOCL-apple-x86_64" ); }
	    if (System.getProperty("os.name", "<unknown>").equals("Windows") ) { JNI.loadLibrary( "JOCL-windows-x86_64" ); }
	} else 
	{ 
	   if (System.getProperty("os.name", "<unknown>").equals("Windows") ) { JNI.loadLibrary( "JOCL-windows-x86" ); 
	}
	JNI.loadLibrary( "jocl" );
	JNI.loadLibrary( "gluegen-rt" );
		
	float[] testImage = (float[]) imp.getPixels();
	String openCLCodeString = "__kernel void sobel( __global float* input,__global float* output, int width,    int height ){get_global_id(0); int y = get_global_id(1);    int offset = y * width + x;    float p0, p1, p2, p3, p5, p6, p7, p8 = 0;if( x < 1 || y < 1 || x > width - 2 || y > height - 2 ){  output[offset] = 0;}else{ p0 = input[offset - width - 1] ; p1 = input[offset - width] ;p2 = input[offset - width + 1] ;   p3 = input[offset - 1] ; p5 = input[offset + 1] ; p6 = input[offset + width - 1] ; p7 = input[offset + width] ; p8 = input[offset + width + 1] ; float sum1 = p0 + 2*p1 + p2 - p6 - 2*p7 - p8;   float sum2 = p0 + 2*p3 + p6 - p2 - 2*p5 - p8;  output[offset] = sqrt(  sum1*sum1 + sum2*sum2 );}}";

	CLProgram program = null;
	CLContext context;
	CLKernel kernel;
	CLCommandQueue queue;

	FloatBuffer data;
	CLBuffer<FloatBuffer> clFloatBufferDataCopy;
	CLBuffer<FloatBuffer> clFloatBufferData;

	context = CLContext.create(Type.GPU);
	program = context.createProgram( openCLCodeString );
	program.build();
	kernel = program.createCLKernel( "sobel" );
	queue = context.getMaxFlopsDevice().createCommandQueue();
	data = ByteBuffer.allocateDirect(imp.getHeight() * imp.getWidth() * 4).order( ByteOrder.nativeOrder() ).asFloatBuffer();
	clFloatBufferData = context.createBuffer( data, Mem.READ_WRITE );
	clFloatBufferDataCopy = context.createFloatBuffer( imp.getHeight()* imp.getWidth(), Mem.READ_ONLY );

	kernel.setArg( 0, clFloatBufferDataCopy );
	kernel.setArg( 1, clFloatBufferData );
	kernel.setArg( 2, imp.getWidth() );
	kernel.setArg( 3, imp.getHeight() );
	
	for (int i = 0; i < imp.getHeight() * imp.getWidth(); i++) {
	    data.put(i, testImage[i]);
	}

	queue.putWriteBuffer(clFloatBufferData, false);
	queue.putCopyBuffer(clFloatBufferData, clFloatBufferDataCopy);
	queue.put2DRangeKernel(kernel, 0, 0, imp.getWidth(), imp.getHeight(), 0, 0);
	queue.finish();
	
	queue.putReadBuffer(clFloatBufferData, true);
	for (int i = 0; i < imp.getHeight() * imp.getWidth(); i++) {
	    testImage[i] = data.get(i);
	}
	
	context.release();
	imp.setPixels( testImage );
    }

    @Override
	public int setup( String arg0, ImagePlus arg1 ) 
    {
	return DOES_32;
    }

}