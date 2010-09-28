.PHONY : all clean

CLASSPATH=.:../../jars/ij.jar:../../jars/VIB-lib.jar

all : fastpngtest fastjpegtest

clean :
	rm -vf */*.so */*.class

%.class : %.java
	javac -cp $(CLASSPATH) "$<"

# Native PNG writing:

fastpng/fastpng_Native_PNG_Writer.h : fastpng/Native_PNG_Writer.class
	javah -classpath $(CLASSPATH) -jni -d fastpng fastpng.Native_PNG_Writer

fastpng/libfastpng.so : fastpng/fastpng_Native_PNG_Writer.h fastpng/fastpng_Native_PNG_Writer.c
	( cd fastpng && gcc -Wall -O3 -o libfastpng.so fastpng_Native_PNG_Writer.c -shared -fPIC -I/usr/lib/jvm/java-6-sun/include/ -I/usr/lib/jvm/java-6-sun/include/linux/ `libpng-config --libs` )

fastpngtest : fastpng/libfastpng.so fastpng/Test_Native_PNG_Writer.class fastpng/Native_PNG_Writer.class
	LD_LIBRARY_PATH=fastpng java -classpath $(CLASSPATH) fastpng.Test_Native_PNG_Writer

# Native JPEG writing:

fastjpeg/fastjpeg_Native_JPEG_Writer.h : fastjpeg/Native_JPEG_Writer.class
	javah -classpath $(CLASSPATH) -jni -d fastjpeg fastjpeg.Native_JPEG_Writer

fastjpeg/libfastjpeg.so : fastjpeg/fastjpeg_Native_JPEG_Writer.h fastjpeg/fastjpeg_Native_JPEG_Writer.c
	( cd fastjpeg && gcc -Wall -O3 -o libfastjpeg.so fastjpeg_Native_JPEG_Writer.c -shared -fPIC -I/usr/lib/jvm/java-6-sun/include/ -I/usr/lib/jvm/java-6-sun/include/linux/ -ljpeg )

fastjpegtest : fastjpeg/libfastjpeg.so fastjpeg/Test_Native_JPEG_Writer.class fastjpeg/Native_JPEG_Writer.class
	LD_LIBRARY_PATH=fastjpeg java -classpath $(CLASSPATH) fastjpeg.Test_Native_JPEG_Writer

