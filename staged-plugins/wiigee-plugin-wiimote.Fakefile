JAVAVERSION=1.5
BUILDXML=trunk/wiigee-plugin-wiimote/build.xml
ANTTARGET=trunk/wiigee-plugin-wiimote/dist/wiigee-plugin-wiimote.jar
JAR=wiigee-plugin-wiimote.jar
all <- $JAR

$JAR <- $ANTTARGET

$ANTTARGET[sh -c "../fiji --ant -f $BUILDXML -Djavac.classpath=../../../jars/wiigee-lib.jar:../../../jars/bluecove.jar -Djavac.includes=.java -k jar || true"] <-
