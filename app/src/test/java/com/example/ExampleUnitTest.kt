package com.example

import com.example.core.gastro.GastroJniRustBlobBridge
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun gastroJniRustBlobBridge_synthesizesAndPatchesElf64Binary() {
        val elfBytes = GastroJniRustBlobBridge.synthesizeAarch64ElfSharedLibraryBytes(
            soname = "libgastro_engine_jni.so",
            exportedSymbols = GastroJniRustBlobBridge.getExportedJniAbiSignatures(),
            neededLibs = listOf("libhidltransport.so", "libhidlbase.so", "liblog.so", "libc.so")
        )
        assertTrue(elfBytes.size > 128)
        assertEquals(0x7F.toByte(), elfBytes[0])
        assertEquals('E'.code.toByte(), elfBytes[1])
        assertEquals('L'.code.toByte(), elfBytes[2])
        assertEquals('F'.code.toByte(), elfBytes[3])

        val tmpFile = File.createTempFile("test_gastro_blob", ".so")
        try {
            tmpFile.writeBytes(elfBytes)
            val patchedSlots = GastroJniRustBlobBridge.patchElfDtNeededInPlace(
                elfFile = tmpFile,
                oldSoname = "libhidltransport.so",
                newSoname = "libhidlbase.so"
            )
            assertEquals(1, patchedSlots)

            val rodata = GastroJniRustBlobBridge.scanBinaryRodataHardwareTelemetry(tmpFile)
            assertEquals(tmpFile.name, rodata.blobFileName)
            assertEquals(7, GastroJniRustBlobBridge.getExportedJniAbiSignatures().size)
        } finally {
            tmpFile.delete()
        }
    }
}
