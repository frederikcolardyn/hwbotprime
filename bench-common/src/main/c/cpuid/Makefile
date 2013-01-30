.PHONY: all clean

all: cpuid

cpuid: cpuid.c cpuid.h tool.c
	$(CC) $(CFLAGS) -o cpuid cpuid.c tool.c

clean:
	rm -f cpuid

CC=gcc
CFLAGS=-I. -std=c99 -pedantic -Wall -W -Wundef -Wendif-labels -Wshadow -Wpointer-arith -Wbad-function-cast -Wcast-align -Wwrite-strings -Wstrict-prototypes -Wmissing-prototypes -Wnested-externs -Winline -Wdisabled-optimization -fstrict-aliasing -O2 -pipe -Wno-parentheses
