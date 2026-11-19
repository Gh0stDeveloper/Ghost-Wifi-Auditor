#include <jni.h>
#include <cstdint>
#include <string>

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
        result = "PCAPNG reconocido (Section Header Block). El archivo debe validarse por bloques antes de analizar paquetes.";
    } else if ((u(0)==0xD4 && u(1)==0xC3 && u(2)==0xB2 && u(3)==0xA1) ||
               (u(0)==0xA1 && u(1)==0xB2 && u(2)==0xC3 && u(3)==0xD4) ||
               (u(0)==0x4D && u(1)==0x3C && u(2)==0xB2 && u(3)==0xA1) ||
               (u(0)==0xA1 && u(1)==0xB2 && u(2)==0x3C && u(3)==0x4D)) {
        if (length < 24) result = "Cabecera PCAP incompleta";
        else result = "PCAP clásico reconocido. Análisis detallado de paquetes: próximo hito.";
    } else {
        result = "No se reconoce una cabecera PCAP/PCAPNG estándar";
    }
    return env->NewStringUTF(result.c_str());
}
