To enable ITK functionality within Fiji:

1) Download and install ITK and WrapITK binaries from:
     http://code.google.com/p/wrapitk/

2) Launch Fiji with the path to your installed WrapITK libraries:
     ./fiji -Djava.library.path=/usr/lib/InsightToolkit/WrapITK/lib --

   On some systems (e.g., Mac OS X 10.5), you may need to force a
   32-bit architecture to match the installed native libraries:
     arch -i386 ./fiji -Djava.library.path=/usr/lib/InsightToolkit/WrapITK/lib --