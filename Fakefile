# TODO: micromanager

# This is a configuration file for Fiji mAKE ("fake")
#
# The syntax of a Fakefile is meant to be very simple.
#
# The first rule is the default rule.
#
# All rules are of the form
#
#	target <- prerequisites
#
# before making "target", all prerequisites will be made (i.e. if there
# exists a rule, for an item on the right side, it will be executed before
# the current rule).
#
# Most rules have implicit actions: if the target is a .jar file, the items
# on the right side are packaged into the target, compiling them first, if
# they are .java files.
#
# If the last item on the right side is a .cxx file, the GNU C++ compiler
# will be invoked to make the target from it.
#
# If an item on the right side is a directory, and a Fakefile or a Makefile
# exists in that directory, "fake" or "make" will be called in that directory.
# The target will be simply copied from that directory after handling all
# dependencies.
#
# There is a special type of rule when "fake" does not know how to produce
# the target from the prerequisites: you can call a program with
#
#	target[program] <- items
#
# This will check if the target is up-to-date, by checking the timestamps of the
# items (if there is no item, the target is deemed _not_ up-to-date). If the target
# is not up-to-date, "fake" will execute the program with all items as parameters.
#
# Variables are defined like this:
#
#	VARIABLE=VALUE
#
# and their values can be accessed with "$VARIABLE" in most places.
#
# You can define variables depending on the platform, the target, and in some
# cases the prerequisite, by adding a tag in parentheses to the name:
#
#	VARIABLE(target)=xyz
#	VARIABLE(platform)=abc

# These variables are special, as they will be interpreted by "fake".

# Do not be verbose
verbose=false

# Usually not necessary
debug=false

# Compile .java files for this Java version
javaVersion=1.5

# When building a .jar file, and a .config file of the same name is found in
# this directory, it will be included as "plugins.config".
pluginsConfigDirectory=staged-plugins

# When a prerequisite is a directory, but contains neither Fakefile nor
# Makefile, just ignore it
ignoreMissingFakefiles=true

# When a submodule could not be made, fall back to copying from this directory
precompiledDirectory=precompiled/

buildDir=build/

FIJI_JAVA_HOME(linux)=java/linux/jdk1.6.0_16/jre
FIJI_JAVA_HOME(linux64)=java/linux-amd64/jdk1.6.0_16/jre
FIJI_JAVA_HOME(win32)=java/win32/jdk1.6.0_16/jre
FIJI_JAVA_HOME(win64)=java/win64/jdk1.6.0_16/jre
FIJI_JAVA_HOME(macosx)=java/macosx-java3d
JAVA_HOME=$FIJI_JAVA_HOME
ENVOVERRIDES(JAVA_HOME)=true

# the main target

SUBMODULE_TARGETS=\
	ij.jar \
	plugins/loci_tools.jar \
	plugins/VIB_.jar \
	jars/VectorString.jar \
	plugins/TrakEM2_.jar \
	plugins/mpicbg_.jar \
	jars/clojure.jar \
	jars/clojure-contrib.jar \
	plugins/ij-ImageIO_.jar \
	jars/jacl.jar \
	jars/batik.jar \
	jars/junit-4.5.jar \
	jars/rsyntaxtextarea.jar \
	jars/autocomplete.jar \
	jars/weka.jar \
	jars/jython.jar \
	jars/imglib.jar \
	jars/appframework.jar \
	jars/visualap/.jar \
	jars/jdom.jar \
	jars/swing-layout.jar \

PLUGIN_TARGETS=plugins/Jython_Interpreter.jar \
	plugins/Clojure_Interpreter.jar \
	plugins/JRuby_Interpreter.jar \
	plugins/BeanShell_Interpreter.jar \
	plugins/bUnwarpJ_.jar \
	plugins/register_virtual_stack_slices.jar \
	plugins/Siox_Segmentation.jar \
	plugins/registration_3d.jar \
	plugins/IO_.jar \
	plugins/CLI_.jar \
	plugins/Javascript_.jar \
	plugins/LSM_Toolbox.jar \
	plugins/SplineDeformationGenerator_.jar \
	plugins/level_sets.jar \
	plugins/Analyze_Reader_Writer.jar \
	plugins/Color_Histogram.jar \
	plugins/Color_Inspector_3D.jar \
	plugins/Image_5D.jar \
	plugins/M_I_P.jar \
	plugins/Interactive_3D_Surface_Plot.jar \
	plugins/View5D_.jar \
	plugins/Volume_Viewer.jar \
	plugins/IJ_Robot.jar \
	plugins/Fiji_Updater.jar \
	plugins/Daltonize_.jar \
	plugins/Stitching_.jar \
	plugins/LSM_Reader.jar \
	plugins/AnalyzeSkeleton_.jar \
	plugins/Skeletonize3D_.jar \
	plugins/TurboReg_.jar \
	plugins/Bug_Submitter.jar \
	plugins/Fiji_Plugins.jar \
	plugins/ToAST_.jar \
	plugins/MTrack2_.jar \
	plugins/Time_Stamper.jar \
	plugins/Statistical_Region_Merging.jar \
	plugins/Refresh_Javas.jar \
	plugins/Auto_Threshold.jar \
	plugins/Arrow_.jar \
	plugins/Stack_Manipulation.jar \
	plugins/FlowJ_.jar \
	plugins/PIV_analyser.jar \
	plugins/Record_Screen.jar \
	plugins/Video_Editing.jar \
	plugins/Sync_Win.jar \
	plugins/Gray_Morphology.jar \
	plugins/Colocalisation_Analysis.jar \
	plugins/LocalThickness_.jar \
	plugins/Fiji_Developer.jar \
	plugins/Script_Editor.jar \
	plugins/Manual_Tracking.jar \
	plugins/Calculator_Plus.jar \
	plugins/3D_Objects_Counter.jar \
	plugins/IsoData_Classifier.jar \
	plugins/RATS_.jar \
	\
	misc/Fiji.jar

all <- fiji $SUBMODULE_TARGETS $PLUGIN_TARGETS third-party-plugins jars/zs.jar

# The "run" rule just executes ./fiji (as long as the file "run" does not exist...)
# It has items on the right side, because these would be passed to the executable.

run[] <- all run-fiji
run-fiji[./fiji] <-
DEBUG_ARGS=-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n
dev[./fiji $DEBUG_ARGS] <-


# JDK

JDK=java/$PLATFORM
JDK(linux64)=java/linux-amd64
JDK(macosx)=java/macosx-java3d

# Call the Jython script to ensure that the JDK is checked out (from Git)
jdk[bin/checkout-jdk.py $JDK] <-

# From submodules
ij.jar <- jars/javac.jar ImageJA/
CLASSPATH(plugins/VIB_.jar)=plugins/LSM_Toolbox.jar
plugins/VIB_.jar <- plugins/LSM_Toolbox.jar VIB/
plugins/mpicbg_.jar <- mpicbg/
jars/clojure.jar <- clojure/
jars/clojure-contrib.jar <- jars/clojure.jar clojure-contrib/
plugins/loci_tools.jar <- bio-formats/
jars/VectorString.jar <- TrakEM2/
CLASSPATH(plugins/TrakEM2_.jar)=plugins/VIB_.jar:plugins/mpicbg_.jar:plugins/loci_tools.jar:plugins/bUnwarpJ_.jar:plugins/level_sets.jar:plugins/Fiji_Plugins.jar
plugins/TrakEM2_.jar <- ij.jar plugins/VIB_.jar plugins/mpicbg_.jar plugins/bUnwarpJ_.jar plugins/level_sets.jar plugins/Fiji_Plugins.jar jars/VectorString.jar TrakEM2/
plugins/ij-ImageIO_.jar <- ij-plugins/
jars/jacl.jar <- tcljava/
jars/batik.jar <- batik/
jars/junit-4.5.jar <- junit/
jars/rsyntaxtextarea.jar <- RSyntaxTextArea/
jars/autocomplete.jar <- AutoComplete/
jars/weka.jar <- weka/
jars/jython.jar <- jython/
jars/imglib.jar <- imglib/
jars/appframework.jar <- appframework/
jars/visualap/.jar <- visualap//
jars/jdom.jar <- jdom/
jars/swing-layout.jar <- swing-layout/

# From source
javaVersion(misc/Fiji.jar)=1.5
mainClass(misc/Fiji.jar)=fiji.Main
misc/Fiji.jar <- src-plugins/Fiji/fiji/*.java icon.png[images/icon.png]

# These classes are common
CLASSPATH(jars/zs.jar)=jars/Jama-1.0.2.jar
jars/zs.jar <- src-plugins/zs/**/*.java

jars/fiji-lib.jar <- src-plugins/fiji-lib/**/*.java

# These classes are common to the scripting plugins
jars/fiji-scripting.jar <- src-plugins/fiji-scripting/**/*.java

CLASSPATH(plugins/Refresh_Javas.jar)=jars/fiji-scripting.jar:jars/fake.jar
CLASSPATH(plugins/Jython_Interpreter.jar)=jars/fiji-scripting.jar:jars/jython.jar
plugins/Jython_Interpreter.jar <- src-plugins/Jython/*.java
CLASSPATH(plugins/Clojure_Interpreter.jar)=jars/fiji-scripting.jar:jars/clojure.jar:jars/clojure-contrib.jar
plugins/Clojure_Interpreter.jar <- src-plugins/Clojure/*.java
CLASSPATH(plugins/JRuby_Interpreter.jar)=jars/fiji-scripting.jar
plugins/JRuby_Interpreter.jar <- src-plugins/JRuby/*.java
CLASSPATH(plugins/BeanShell_Interpreter.jar)=jars/fiji-scripting.jar
plugins/BeanShell_Interpreter.jar <- src-plugins/BSH/*.java
CLASSPATH(plugins/Javascript_.jar)=jars/fiji-scripting.jar
plugins/Javascript_.jar <- src-plugins/Javascript/*.java

plugins/Bug_Submitter.jar <- src-plugins/Bug_Submitter/*.java

CLASSPATH(plugins/register_virtual_stack_slices.jar)=plugins/TrakEM2_.jar:plugins/mpicbg_.jar:plugins/bUnwarpJ_.jar:plugins/Fiji_Plugins.jar

CLASSPATH(plugins/Siox_Segmentation.jar)=jars/fiji-lib.jar

CLASSPATH(plugins/LSM_Toolbox.jar)=plugins/LSM_Reader.jar
MAINCLASS(plugins/LSM_Toolbox.jar)=org.imagearchive.lsm.toolbox.gui.AboutDialog
plugins/LSM_Toolbox.jar <- \
	src-plugins/LSM_Toolbox/**/*.java \
	src-plugins/LSM_Toolbox/**/*.png \
	src-plugins/LSM_Toolbox/**/*.jpg \
	src-plugins/LSM_Toolbox/**/*.htm \
	src-plugins/LSM_Toolbox/**/*.txt
MAINCLASS(plugins/Interactive_3D_Surface_Plot.jar)=Interactive_3D_Surface_Plot
CLASSPATH(plugins/Stitching_.jar)=plugins/loci_tools.jar:plugins/Fiji_Plugins.jar
CLASSPATH(plugins/Fiji_Plugins.jar)=jars/jsch-0.1.37.jar
CLASSPATH(plugins/Fiji_Updater.jar)=jars/jsch-0.1.37.jar:misc/Fiji.jar

plugins/Record_Screen.jar <- src-plugins/Record_Screen/ src-plugins/Record_Screen/**/*

CLASSPATH(plugins/CLI_.jar)=jars/fiji-scripting.jar
plugins/CLI_.jar <- src-plugins/CLI_/CLI/*.java

CLASSPATH(plugins/IO_.jar)=jars/batik.jar
plugins/IO_.jar <- src-plugins/IO_/**/*.java \
	io/df3_scene.pov[src-plugins/IO_/io/df3/df3_scene.pov]

CLASSPATH(plugins/Sync_Win.jar)=plugins/Image_5D.jar
CLASSPATH(plugins/Fiji_Developer.jar)=plugins/Script_Editor.jar:plugins/Fiji_Plugins.jar:plugins/VIB_.jar
MAINCLASS(plugins/Script_Editor.jar)=fiji.scripting.Script_Editor
CLASSPATH(plugins/Script_Editor.jar)=jars/rsyntaxtextarea.jar:\
jars/autocomplete.jar:plugins/Clojure_Interpreter.jar:\
plugins/JRuby_Interpreter.jar:plugins/Javascript_.jar:\
plugins/Jython_Interpreter.jar:plugins/Refresh_Javas.jar:\
plugins/BeanShell_Interpreter.jar:jars/fiji-scripting.jar:\
misc/Fiji.jar:$JAVA_HOME/../lib/tools.jar
NO_COMPILE(plugins/Script_Editor.jar)=src-plugins/Script_Editor/templates/**/*
plugins/Script_Editor.jar <- src-plugins/Script_Editor/**/*.java  \
							icon.png[images/icon.png] \
							var.png[images/var.png]    \
							funtion.png[images/function.png] \
							src-plugins/Script_Editor/templates/**/*

plugins/*_*.jar <- src-plugins/*_*/**/*.java

MAINCLASS(jars/javac.jar)=com.sun.tools.javac.Main
JAVAVERSION(jars/javac.jar)=1.5
jars/javac.jar <- src-plugins/javac/**/*

MAINCLASS(jars/test-fiji.jar)=fiji.Tests
jars/test-fiji.jar <- src-plugins/test-fiji/**/*.java

# Third party plugins

# TODO: compile ij-ImageIO_ as submodule
THIRD_PARTY_PLUGINS= \
	plugins/TransformJ_.jar \
	plugins/ij-ImageIO_.jar \

third-party-plugins[] <- $THIRD_PARTY_PLUGINS
plugins/*.jar <- staged-plugins/*.jar

# Fiji launcher

JAVA_LIB_PATH(linux)=lib/i386/client/libjvm.so
JAVA_LIB_PATH(linux64)=lib/amd64/server/libjvm.so
JAVA_LIB_PATH(win32)=bin/client/jvm.dll
JAVA_LIB_PATH(win64)=bin/server/jvm.dll
JAVA_LIB_PATH(macosx)=

# The variables CFLAGS, CXXFLAGS, LDFLAGS and LIBS will be used for compiling
# C and C++ programs.
CXXFLAGS(*)=-Wall -Iincludes \
	-DJAVA_HOME='"$FIJI_JAVA_HOME"' -DJAVA_LIB_PATH='"$JAVA_LIB_PATH"'
WINOPTS=-mwindows -mno-cygwin -DMINGW32
CXXFLAGS(win32)=$CXXFLAGS $WINOPTS
CXXFLAGS(win64)=$CXXFLAGS $WINOPTS

# Include 64-bit architectures only in ./fiji (as opposed to ./fiji-tiger),
# and only on MacOSX
MACOPTS(osx10.3)=-I/System/Library/Frameworks/JavaVM.Framework/Headers \
	-DMACOSX -arch ppc
MACOPTS(osx10.4)=$MACOPTS(osx10.3) -mmacosx-version-min=10.3 -arch i386
MACOPTS(osx10.5)=$MACOPTS(osx10.4) -arch ppc64 -arch x86_64

CXXFLAGS(linux)=$CXXFLAGS -DIPV6_MAYBE_BROKEN
CXXFLAGS(linux64)=$CXXFLAGS -DIPV6_MAYBE_BROKEN

LDFLAGS(win32)=$LDFLAGS $WINOPTS

CXXFLAGS(fiji)=$CXXFLAGS $MACOPTS
LDFLAGS(fiji)=$LDFLAGS $MACOPTS

LIBS(linux)=-ldl
LIBS(linux64)=-ldl
LIBS(macosx)=-framework CoreFoundation -framework JavaVM

fiji <- fiji.cxx

CXXFLAGS(fiji-tiger)=$CXXFLAGS $MACOPTS(osx10.4)
LDFLAGS(fiji-tiger)=$LDFLAGS $MACOPTS(osx10.4)
fiji-tiger <- fiji.cxx

CXXFLAGS(fiji-panther)=$CXXFLAGS $MACOPTS(osx10.3)
LDFLAGS(fiji-panther)=$LDFLAGS $MACOPTS(osx10.3)
fiji-panther <- fiji.cxx

# Cross-compiling (works only on Linux64 so far)

all-cross[] <- cross-win32 cross-win64 cross-linux
# cross-tiger does not work yet

cross-win64[bin/cross-compiler.py win64 $CXXFLAGS(win64)] <- fiji.cxx
cross-tiger[bin/chrooted-cross-compiler.sh tiger \
	$CXXFLAGS(macosx) $LIBS(macosx)] <- fiji.cxx
cross-*[bin/chrooted-cross-compiler.sh * \
	$CXXFLAGS(*) $LIBS(*)] <- fiji.cxx

# Precompiled stuff

LAUNCHER(*)=precompiled/fiji-$PLATFORM
LAUNCHER(win32)=precompiled/fiji-win32.exe
LAUNCHER(win64)=precompiled/fiji-win64.exe
LAUNCHER(osx10.4)=precompiled/fiji-macosx
LAUNCHER(osx10.5)=precompiled/fiji-macosx precompiled/fiji-tiger
precompile-fiji[] <- $LAUNCHER

precompiled/fiji-tiger[bin/copy-file.py $PRE $TARGET] <- fiji-tiger
# this rule only matches precompiled/fiji-$PLATFORM
precompiled/fiji-*[bin/copy-file.py $PRE $TARGET] <- fiji

precompile-fake[] <- precompiled/fake.jar
precompiled/*[bin/copy-file.py $PRE $TARGET] <- *

precompile-submodules[] <- \
	precompiled/ij.jar \
	precompiled/loci_tools.jar \
	precompiled/TrakEM2_.jar \
	precompiled/VIB_.jar \
	precompiled/mpicbg_.jar \
	precompiled/clojure.jar \
	precompiled/clojure-contrib.jar \
	precompiled/ij-ImageIO_.jar \
	precompiled/jacl.jar \
	precompiled/batik.jar \
	precompiled/junit-4.5.jar \
	precompiled/rsyntaxtextarea.jar \
	precompiled/autocomplete.jar \
	precompiled/weka.jar \
	precompiled/jython.jar \
	precompiled/imglib.jar \
	precompiled/appframework.jar \
	precompiled/.jar \
	precompiled/jdom.jar \
	precompiled/swing-layout.jar \

precompiled/ij.jar <- ij.jar
precompiled/clojure.jar <- jars/clojure.jar
precompiled/clojure-contrib.jar <- jars/clojure.jar jars/clojure-contrib.jar
precompiled/jacl.jar <- jars/jacl.jar
precompiled/batik.jar <- jars/batik.jar
precompiled/junit-4.5.jar <- jars/junit-4.5.jar
precompiled/rsyntaxtextarea.jar <- jars/rsyntaxtextarea.jar
precompiled/autocomplete.jar <- jars/autocomplete.jar
precompiled/weka.jar <- jars/weka.jar
precompiled/jython.jar <- jars/jython.jar
precompiled/imglib.jar <- jars/imglib.jar
precompiled/appframework.jar <- jars/appframework.jar
precompiled/.jar <- jars/visualap/.jar
precompiled/jdom.jar <- jars/jdom.jar
precompiled/swing-layout.jar <- jars/swing-layout.jar
precompiled/* <- plugins/*

precompile[] <- precompile-fiji precompile-fake precompile-submodules

# Portable application/.app

all-apps[] <- app-macosx app-linux app-linux64 app-win32 app-win64
MACOSX_TIGER_LAUNCHER(macosx)=fiji-tiger
app-*[bin/make-app.py * $PLATFORM] <- all $MACOSX_TIGER_LAUNCHER

app-all[bin/make-app.py all $PLATFORM] <- all
app-nojre[bin/make-app.py nojre $PLATFORM] <- all

all-dmgs[] <- fiji-macosx.dmg
fiji-*.dmg[bin/make-dmg.py] <- app-* Fiji.app \
	resources/install-fiji.jpg
dmg[] <- fiji-macosx.dmg

resources/install-fiji.jpg[./fiji bin/generate-finder-background.py] <- \
	bin/generate-finder-background.py

all-tars[] <- fiji-linux.tar.bz2 fiji-linux64.tar.bz2 \
	fiji-all.tar.bz2 fiji-nojre.tar.bz2
fiji-*.tar.bz2[bin/make-tar.py $TARGET Fiji.app] <- app-* Fiji.app
tar[] <- fiji-$PLATFORM.tar.bz2

all-zips[] <- fiji-linux.zip fiji-linux64.zip fiji-win32.zip fiji-win64.zip \
	fiji-all.zip fiji-nojre.zip
fiji-*.zip[bin/make-zip.py $TARGET Fiji.app] <- app-* Fiji.app
zip[] <- fiji-$PLATFORM.zip

all-isos[] <- fiji-linux.iso fiji-linux64.iso fiji-win32.iso fiji-win64.iso \
	fiji-macosx.iso fiji-all.iso fiji-nojre.iso
fiji-*.iso[genisoimage -J -V Fiji -o $TARGET Fiji.app] <- app-*

all-7zs[] <- fiji-linux.7z fiji-linux64.7z fiji-win32.7z fiji-win64.7z \
	fiji-macosx.7z fiji-all.7z fiji-nojre.7z
fiji-*.7z[bin/make-7z.py $TARGET Fiji.app] <- app-*

# Checks

check[] <- check-launchers check-submodules

LAUNCHERS=$LAUNCHER(linux) $LAUNCHER(linux64) \
	$LAUNCHER(win32) $LAUNCHER(win64) $LAUNCHER(macosx)
check-launchers[bin/up-to-date-check.py fiji.cxx $LAUNCHERS] <-

check-submodules[] <- check-ij check-VIB check-TrakEM2 check-mpicbg

check-ij[bin/up-to-date-check.py ImageJA precompiled/ij.jar] <-
check-*[bin/up-to-date-check.py * precompiled/*_.jar] <-

# Fake itself

MAINCLASS(jars/fake.jar)=fiji.build.Fake
JAVAVERSION(jars/fake.jar)=1.3
jars/fake.jar <- src-plugins/fake/**/*.java

# Script_Editor_Stub.jar (to include MainClassForDebugging in the classpath)

MAINCLASS(jars/Script_Editor_Stub.jar)=stub.MainClassForDebugging
CLASSPATH(jars/Script_Editor_Stub.jar)=ij.jar
jars/Script_Editor_Stub.jar <- stub/MainClassForDebugging.java
