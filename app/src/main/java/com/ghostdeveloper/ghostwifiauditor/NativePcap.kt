package com.ghostdeveloper.ghostwifiauditor

object NativePcap {
    init { System.loadLibrary("ghostpcap") }
    external fun inspectHeader(bytes: ByteArray): String
    /** JSON summary only; does not return packet contents or credentials. */
    external fun analyzeFile(privatePath: String): String
}
