#!/bin/sh

for package in libmcpp-dev libbz2-dev libdb4.8-java \
	libjgoodies-looks-java libjgoodies-forms-java
do
	dpkg -l $package > /dev/null 2>&1 && continue
	sudo apt-get install $package
done

cd "$(dirname "$0")/.." || {
	echo "Could not switch to Fiji root directory" >&2
	exit 1
}

# make sure the Fiji launcher is in the path
FIJI_HOME="$(pwd)" &&
PATH="$FIJI_HOME:$PATH" &&
export PATH &&

mkdir -p 3rdparty &&
cd 3rdparty &&
if test ! -d omero
then
	git clone git://pacific.mpi-cbg.de/omero/.git
fi &&
cd omero &&

# make Ice

ICE_VERSION=3.4.1 &&
ICE_SHORT_VERSION=3.4 &&
ICE_HOME="$(pwd)/Ice-$ICE_VERSION" &&
if test ! -d "$ICE_HOME"
then
	if test ! -f Ice-$ICE_VERSION.tar.gz
	then
		curl -O http://zeroc.com/download/Ice/$ICE_SHORT_VERSION/Ice-$ICE_VERSION.tar.gz
	fi &&
	tar xzvf Ice-$ICE_VERSION.tar.gz
fi &&
if test ! -f "$ICE_HOME"/java/lib/Ice.jar
then
	(cd "$ICE_HOME" &&
	 cat << EOF > ant &&
#!/bin/sh

fiji --ant-bare --cp /usr/share/java/libdb4.8-java.jar:/usr/share/java/forms.jar:/usr/share/java/looks.jar
EOF
	 chmod a+x ant &&
	 PATH="$(pwd):$PATH" &&
	 export PATH &&
	 make cpp java)
fi &&
export ICE_HOME &&
PATH="$ICE_HOME/cpp/bin:$PATH" &&
export PATH &&
LD_LIBRARY_PATH="$ICE_HOME/cpp/lib${LD_LIBRARY_PATH:+:}$LD_LIBRARY_PATH" &&
export LD_LIBRARY_PATH &&
fiji --ant-bare -Djavac.maxmem=2g --cp "$FIJI_HOME"/jars/ij.jar --jar-path "$ICE_HOME/java/lib"
