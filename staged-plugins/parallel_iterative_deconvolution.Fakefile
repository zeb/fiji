JAVAVERSION=1.5
JAR=parallel_iterative_deconvolution.jar
JARVERSION=ParallelIterativeDeconvolution/lib/parallel_iterative_deconvolution-1.10.jar
DEFINES=-Dparallelcolt.dir=../../jars -Dparallelcolt.lib.dir=../../jars \
	-Dimagej.dir=../.. -f ParallelIterativeDeconvolution/build.xml
ANTTARGET=jar
all <- $JAR

$JAR <- $JARVERSION

$JARVERSION[../fiji --ant $DEFINES $ANTTARGET] <-

