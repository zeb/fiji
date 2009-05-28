all <- JMathLib.jar

CLASSPATH(JMathLib-standalone.jar)=src/jmathlib/plugins/dynjava/dynamicjava.jar:libs/servlet-api.jar:../jars/junit-4.5.jar
JMathLib-standalone.jar <- src/**/*.java src/jmathlib/**/*.m \
	src/**/*.properties src/**/*.gif

bin[mkdir -p bin] <-

# JMathLib-standalone.jar is just a precondition to make sure that the classes
# are built.  createfunctionslist() needs to run with classpath=src/ though, to
# be able to discover the available classes.
bin/webFunctionsList.dat[../fiji --cp src --main-class jmathlib.ui.text.TextUI createfunctionslist();quit();] <- JMathLib-standalone.jar bin

JMathLib.jar <- JMathLib-standalone.jar/ bin/ bin/webFunctionsList.dat
