#!/bin/sh

# openbsd 4.9
# gcc 4.2.1
# openjdk 1.7.0

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:.
mkdir -p target/classes
javac src/main/java/org/hwbot/cpuid/CpuId.java -d target/classes/
javah -d src/main/c/ -classpath target/classes/ org.hwbot.cpuid.CpuId
gcc -I/Library/Java/JavaVirtualMachines/jdk1.7.0_07.jdk/Contents/Home/include/ -I/Library/Java/JavaVirtualMachines/jdk1.7.0_07.jdk/Contents/Home/include/darwin/ -shared src/main/c/CpuId.c -o target/libCpuId$1
# java -Djava.library.path=/Users/frederik/dev/git/benchbot/bench-common/src/main/c/hello HelloWorld