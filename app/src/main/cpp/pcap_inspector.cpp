#include <jni.h>
#include <cstdint>
#include <string>
#include "pcap_core.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_ghostdeveloper_ghostwifiauditor_NativePcap_inspectHeader(
        JNIEnv* env, jobject /*thiz*/, jbyteArray buffer) {
    if (!buffer) return env->NewStringUTF("Archivo vacío");
    const jsize length = env->GetArrayLength(buffer);
    if (length < 4) return env->NewStringUTF("Cabecera demasiado corta");
    jbyte b[32]{};
    const jsize count = length < 32 ? length : 32;
    env->GetByteArrayRegion(buffer, 0, count, b);
    const auto u = [&](int i) -> uint8_t { return static_cast<uint8_t>(b[i]); };
    std::string result;
    if (u(0)==0x0A && u(1)==0x0D && u(2)==0x0D && u(3)==0x0A) {
        result = "PCAPNG reconocido. Análisis de bloques disponible mediante importar.";
    } else if ((u(0)==0xD4 && u(1)==0xC3 && u(2)==0xB2 && u(3)==0xA1) ||
               (u(0)==0xA1 && u(1)==0xB2 && u(2)==0xC3 && u(3)==0xD4) ||
               (u(0)==0x4D && u(1)==0x3C && u(2)==0xB2 && u(3)==0xA1) ||
               (u(0)==0xA1 && u(1)==0xB2 && u(2)==0x3C && u(3)==0x4D)) {
        result = length < 24 ? "Cabecera PCAP incompleta" :
            "PCAP clásico reconocido. Análisis de registros disponible mediante importar.";
    } else {
        result = "No se reconoce una cabecera PCAP/PCAPNG estándar";
    }
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ghostdeveloper_ghostwifiauditor_NativePcap_analyzeFile(
        JNIEnv* env, jobject /*thiz*/, jstring path) {
    if (!path) return env->NewStringUTF("{\"ok\":false,\"error\":\"No path\"}");
    const char* chars = env->GetStringUTFChars(path, nullptr);
    if (!chars) return env->NewStringUTF("{\"ok\":false,\"error\":\"No path memory\"}");
    const auto result = ghost::inspectCapture(chars).json();
    env->ReleaseStringUTFChars(path,chars);
    return env->NewStringUTF(result.c_str());
}
