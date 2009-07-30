JAVAVERSION=1.5
JAR=parallel_spectral_deconvolution.jar
JARVERSION=ParallelSpectralDeconvolution/lib/parallel_spectral_deconvolution-1.10.jar
DEFINES=-Dparallelcolt.dir=../../jars -Dparallelcolt.lib.dir=../../jars \
	-Dimagej.dir=../.. -f ParallelSpectralDeconvolution/build.xml
ANTTARGET=jar
all <- $JAR

$JAR <- $JARVERSION

$JARVERSION[../fiji --ant $DEFINES $ANTTARGET] <-

