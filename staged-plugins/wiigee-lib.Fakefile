JAVAVERSION=1.5
BUILDXML=trunk/wiigee-lib/build.xml
ANTTARGET=trunk/wiigee-lib/dist/wiigee-lib.jar
JAR=wiigee-lib.jar
all <- $JAR

$JAR <- $ANTTARGET

$ANTTARGET[../fiji --ant -f $BUILDXML jar] <-

