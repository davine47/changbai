// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
// demo_jni.c — minimal JNI demo: native function called from Scala/SpinalSim
//
// Compile:
//   macOS: gcc -shared -o libdemojni.dylib -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin" demo_jni.c
//   Linux: gcc -shared -o libdemojni.so    -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux"   demo_jni.c

#include <jni.h>
#include <stdio.h>

// JNI function names encode Scala object suffix '$' as '_00024'.
// Scala:  object DemoJniBridge  →  JVM class: DemoJniBridge$
// JNI:    Java_v1_demo_1dpi_DemoJniBridge_00024_compute
//         └─ package _ → _1 ─┘ └class┘ └$→_00024┘ └method┘

JNIEXPORT jint JNICALL
Java_v1_demo_1dpi_DemoJniBridge_00024_compute(JNIEnv *env, jclass cls, jint a, jint b) {
    int result = a + b;
    printf("[JNI C]  compute(%d, %d) = %d\n", a, b, result);
    fflush(stdout);
    return result;
}

JNIEXPORT jint JNICALL
Java_v1_demo_1dpi_DemoJniBridge_00024_isLegal(JNIEnv *env, jclass cls, jint inst) {
    int opcode = inst & 0x7F;
    int result = (opcode != 0) ? 1 : 0;
    printf("[JNI C]  isLegal(0x%08X) = %d  (opcode=0x%02X)\n", inst, result, opcode);
    fflush(stdout);
    return result;
}
