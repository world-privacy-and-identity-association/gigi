JAVA_HOME=/usr/lib/jvm/default-java
SYSTEM= $(shell uname | awk '{print tolower($$0)}')
JAVAH=javah
CC=gcc
CFLAGS=-flto -Wall -Werror -Wextra -pedantic -fPIC
CPPFLAGS=-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(SYSTEM)
LDFLAGS=-shared


all: libsetuid.so

org_cacert_gigi_natives_SetUID.h: ../src/org/cacert/gigi/natives/SetUID.java
	$(JAVAH) -classpath ../bin/ -jni org.cacert.gigi.natives.SetUID

libsetuid.so: org_cacert_gigi_natives_SetUID.c org_cacert_gigi_natives_SetUID.h
	$(CC) $(CPPFLAGS) $(CFLAGS) $(LDFLAGS) -o $@ $<

clean:
	-$(RM) *.so *.h
