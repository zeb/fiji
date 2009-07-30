JAVAVERSION=1.5
JAR=parallel_superresolution.jar
JARVERSION=lib/parallel_superresolution-1.1.jar
DEFINES=-Dparallelcolt.dir=../jars -Dparallelcolt.lib.dir=../jars \
	-Dimagej.dir=..
DISTDIR=dist
ANTTARGET=jar
all <- $JAR

$JAR <- $JARVERSION

$JARVERSION[../fiji --ant $DEFINES $ANTTARGET] <-

