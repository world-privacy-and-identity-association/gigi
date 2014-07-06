SYSTEM= $(shell uname | awk '{print tolower($$0)}')
JAVAH?=javah
CC?=gcc
CFLAGS?=-O3 -g -flto -Wall -Werror -Wextra -pedantic -fsanitize=address -fPIC
CFLAGS+=-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(SYSTEM)
LDFLAGS?=-shared


all: libsetuid.so

libsetuid.so: org_cacert_gigi_natives_SetUID.c
	$(JAVAH) -classpath ../bin/ -jni org.cacert.gigi.natives.SetUID && \
	$(CC) $(CFLAGS) $(LDFLAGS) -o libsetuid.so org_cacert_gigi_natives_SetUID.c

clean:
	rm -f *.so
	rm -f *.h
