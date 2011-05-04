JAVAVERSION=1.5
all <- jacl.jar

JAVALOCK=tcl/lang/library/java/javalock.tcl
TCL1=tcl/lang/library/history.tcl
TCL2=tcl/lang/library/init.tcl
TCL3=tcl/lang/library/ldAout.tcl
TCL4=tcl/lang/library/parray.tcl
TCL5=tcl/lang/library/safe.tcl
TCL6=tcl/lang/library/word.tcl
MAINCLASS(jacl.jar)=tcl.lang.Shell
jacl.jar <- src/jacl/**/*.java src/tcljava/**/*.java \
	$JAVALOCK[src/tcljava/$JAVALOCK] \
	$TCL1[src/jacl/$TCL1] \
	$TCL2[src/jacl/$TCL2] \
	$TCL3[src/jacl/$TCL3] \
	$TCL4[src/jacl/$TCL4] \
	$TCL5[src/jacl/$TCL5] \
	$TCL6[src/jacl/$TCL6]
