JAVA_HOME=/usr/lib/jvm/default-java
SYSTEM= $(shell uname | awk '{print tolower($$0)}')
JAVAH=javah
CC=gcc
CFLAGS=-flto -Wall -Werror -Wextra -pedantic -fPIC
CPPFLAGS=-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(SYSTEM)
LDFLAGS=-shared


all: libsetuid.so

club_wpia_gigi_natives_SetUID.h: ../src/club/wpia/gigi/natives/SetUID.java
	$(JAVAH) -classpath ../bin/ -jni club.wpia.gigi.natives.SetUID

libsetuid.so: club_wpia_gigi_natives_SetUID.c club_wpia_gigi_natives_SetUID.h
	$(CC) $(CPPFLAGS) $(CFLAGS) $(LDFLAGS) -o $@ $<

clean:
	-$(RM) *.so *.h
