package com.ghostdeveloper.ghostwifiauditor

object NativePcap {
    init { System.loadLibrary("ghostpcap") }
    external fun inspectHeader(bytes: ByteArray): String
}
