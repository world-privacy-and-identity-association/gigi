SYSTEM= `uname | awk '{print tolower($0)}'`


all: libsetuid.so

libsetuid.so:
	javah -classpath ../bin/ -jni org.cacert.gigi.natives.SetUID	
	gcc -o libsetuid.so -shared -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(SYSTEM) org_cacert_gigi_natives_SetUID.c

clean:
	rm -f *.so
	rm -f *.h
