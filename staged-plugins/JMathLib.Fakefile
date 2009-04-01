all <- bin compile JMathLib.jar

bin[mkdir -p bin] <-

ANT_TARGETS=compile mfiles resources images
compile[../fiji --ant -Djar.dest=. $ANT_TARGETS] <-

JMathLib.jar <- bin/ bin/**/*
