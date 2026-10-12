package com.example.core.extract

import kotlinx.serialization.Serializable
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Serializable
data class ElfBinaryInfo(
    val fileName: String,
    val relativePath: String,
    val sizeBytes: Long,
    val is64Bit: Boolean,
    val architecture: String, // AArch64, ARM, x86_64, etc.
    val soname: String? = null,
    val neededLibraries: List<String> = emptyList(),
    val isHalModule: Boolean = false,
    val isFodRelated: Boolean = false,
    val vendorOrSystemClassification: String = "VENDOR"
)

@Serializable
data class ElfDependencyGraphReport(
    val totalBinariesAnalyzed: Int,
    val totalDependenciesMapped: Int,
    val binaries: List<ElfBinaryInfo>,
    val mostCommonDependencies: Map<String, Int>,
    val systemLibrariesDependedOnByVendor: List<String>,
    val fodRelatedBlobs: List<String>
)

object ElfDependencyAnalyzer {

    private const val ELF_MAGIC = 0x464C457F // 0x7F, 'E', 'L', 'F' little-endian

    private const val PT_LOAD = 1
    private const val PT_DYNAMIC = 2

    private const val DT_NULL = 0L
    private const val DT_NEEDED = 1L
    private const val DT_STRTAB = 5L
    private const val DT_SONAME = 14L
    private const val DT_STRSZ = 10L

    fun isElfFile(file: File): Boolean {
        if (!file.exists() || file.length() < 16) return false
        return try {
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(0)
                val magic = raf.readInt()
                // In big-endian integer read: 0x7F454C46
                magic == 0x7F454C46
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun parseElf(file: File, baseDir: File = file.parentFile ?: file): ElfBinaryInfo? {
        if (!isElfFile(file)) return null

        return try {
            RandomAccessFile(file, "r").use { raf ->
                val channel = raf.channel
                val headerBuf = ByteBuffer.allocate(64)
                channel.position(0)
                channel.read(headerBuf)
                headerBuf.flip()

                // EI_MAG0..3 already verified
                headerBuf.position(4)
                val eiClass = headerBuf.get().toInt() and 0xFF // 1 = 32-bit, 2 = 64-bit
                val eiData = headerBuf.get().toInt() and 0xFF  // 1 = Little-endian, 2 = Big-endian
                val is64Bit = (eiClass == 2)
                val byteOrder = if (eiData == 2) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN

                headerBuf.order(byteOrder)

                // Skip version, osabi, abiversion, pad (up to offset 16)
                headerBuf.position(16)
                val eType = headerBuf.short.toInt() and 0xFFFF
                val eMachine = headerBuf.short.toInt() and 0xFFFF
                val eVersion = headerBuf.int

                val archName = when (eMachine) {
                    0x28 -> "ARM (32-bit)"
                    0xB7 -> "AArch64 (64-bit ARMv8)"
                    0x03 -> "x86 (32-bit)"
                    0x3E -> "x86_64"
                    else -> "Machine(0x${eMachine.toString(16)})"
                }

                val ePhoff: Long
                val eShoff: Long
                val eFlags: Int
                val eEhsize: Int
                val ePhentsize: Int
                val ePhnum: Int
                val eShentsize: Int
                val eShnum: Int
                val eShstrndx: Int

                if (is64Bit) {
                    val eEntry = headerBuf.long
                    ePhoff = headerBuf.long
                    eShoff = headerBuf.long
                    eFlags = headerBuf.int
                    eEhsize = headerBuf.short.toInt() and 0xFFFF
                    ePhentsize = headerBuf.short.toInt() and 0xFFFF
                    ePhnum = headerBuf.short.toInt() and 0xFFFF
                    eShentsize = headerBuf.short.toInt() and 0xFFFF
                    eShnum = headerBuf.short.toInt() and 0xFFFF
                    eShstrndx = headerBuf.short.toInt() and 0xFFFF
                } else {
                    val eEntry = headerBuf.int.toLong() and 0xFFFFFFFFL
                    ePhoff = headerBuf.int.toLong() and 0xFFFFFFFFL
                    eShoff = headerBuf.int.toLong() and 0xFFFFFFFFL
                    eFlags = headerBuf.int
                    eEhsize = headerBuf.short.toInt() and 0xFFFF
                    ePhentsize = headerBuf.short.toInt() and 0xFFFF
                    ePhnum = headerBuf.short.toInt() and 0xFFFF
                    eShentsize = headerBuf.short.toInt() and 0xFFFF
                    eShnum = headerBuf.short.toInt() and 0xFFFF
                    eShstrndx = headerBuf.short.toInt() and 0xFFFF
                }

                // Parse Program Headers to locate PT_DYNAMIC and PT_LOAD
                var dynamicOffset = 0L
                var dynamicSize = 0L
                var dynamicVAddr = 0L

                data class PhLoad(val vaddr: Long, val memsz: Long, val offset: Long)
                val loads = mutableListOf<PhLoad>()

                for (i in 0 until ePhnum) {
                    val phPos = ePhoff + (i.toLong() * ePhentsize)
                    channel.position(phPos)
                    val phBuf = ByteBuffer.allocate(if (is64Bit) 56 else 32).order(byteOrder)
                    channel.read(phBuf)
                    phBuf.flip()

                    val pType = phBuf.int
                    if (is64Bit) {
                        val pFlags = phBuf.int
                        val pOffset = phBuf.long
                        val pVaddr = phBuf.long
                        val pPaddr = phBuf.long
                        val pFilesz = phBuf.long
                        val pMemsz = phBuf.long
                        val pAlign = phBuf.long

                        if (pType == PT_DYNAMIC) {
                            dynamicOffset = pOffset
                            dynamicSize = pFilesz
                            dynamicVAddr = pVaddr
                        } else if (pType == PT_LOAD) {
                            loads.add(PhLoad(pVaddr, pMemsz, pOffset))
                        }
                    } else {
                        val pOffset = phBuf.int.toLong() and 0xFFFFFFFFL
                        val pVaddr = phBuf.int.toLong() and 0xFFFFFFFFL
                        val pPaddr = phBuf.int.toLong() and 0xFFFFFFFFL
                        val pFilesz = phBuf.int.toLong() and 0xFFFFFFFFL
                        val pMemsz = phBuf.int.toLong() and 0xFFFFFFFFL
                        val pFlags = phBuf.int
                        val pAlign = phBuf.int.toLong() and 0xFFFFFFFFL

                        if (pType == PT_DYNAMIC) {
                            dynamicOffset = pOffset
                            dynamicSize = pFilesz
                            dynamicVAddr = pVaddr
                        } else if (pType == PT_LOAD) {
                            loads.add(PhLoad(pVaddr, pMemsz, pOffset))
                        }
                    }
                }

                fun vaddrToOffset(vaddr: Long): Long {
                    for (ld in loads) {
                        if (vaddr >= ld.vaddr && vaddr < (ld.vaddr + ld.memsz)) {
                            return ld.offset + (vaddr - ld.vaddr)
                        }
                    }
                    return vaddr // fallback
                }

                val neededOffsets = mutableListOf<Long>()
                var sonameOffset = -1L
                var strtabVaddr = 0L
                var strsz = 0L

                if (dynamicOffset > 0L && dynamicSize > 0L) {
                    val entrySize = if (is64Bit) 16 else 8
                    val entryCount = (dynamicSize / entrySize).toInt()

                    channel.position(dynamicOffset)
                    val dynBuf = ByteBuffer.allocate(dynamicSize.toInt().coerceAtMost(65536)).order(byteOrder)
                    channel.read(dynBuf)
                    dynBuf.flip()

                    for (i in 0 until entryCount) {
                        if (dynBuf.remaining() < entrySize) break
                        val dTag = if (is64Bit) dynBuf.long else dynBuf.int.toLong() and 0xFFFFFFFFL
                        val dVal = if (is64Bit) dynBuf.long else dynBuf.int.toLong() and 0xFFFFFFFFL

                        if (dTag == DT_NULL) break
                        when (dTag) {
                            DT_NEEDED -> neededOffsets.add(dVal)
                            DT_SONAME -> sonameOffset = dVal
                            DT_STRTAB -> strtabVaddr = dVal
                            DT_STRSZ -> strsz = dVal
                        }
                    }
                }

                val neededLibs = mutableListOf<String>()
                var resolvedSoname: String? = null

                if (strtabVaddr > 0L) {
                    val strtabFileOffset = vaddrToOffset(strtabVaddr)
                    val readLen = if (strsz > 0L) strsz.toInt().coerceIn(128, 65536) else 16384
                    val strBuf = ByteBuffer.allocate(readLen)
                    channel.position(strtabFileOffset)
                    channel.read(strBuf)
                    val strBytes = strBuf.array()

                    fun readAsciiZ(offset: Long): String? {
                        val idx = offset.toInt()
                        if (idx < 0 || idx >= strBytes.size) return null
                        var end = idx
                        while (end < strBytes.size && strBytes[end] != 0.toByte()) {
                            end++
                        }
                        return if (end > idx) String(strBytes, idx, end - idx, Charsets.UTF_8) else null
                    }

                    for (off in neededOffsets) {
                        readAsciiZ(off)?.let { neededLibs.add(it) }
                    }

                    if (sonameOffset >= 0L) {
                        resolvedSoname = readAsciiZ(sonameOffset)
                    }
                }

                val nameLower = file.name.lowercase()
                val isHal = nameLower.contains(".hardware.") || nameLower.contains("hw") || file.parentFile?.name == "hw"
                val isFod = nameLower.contains("fingerprint") || nameLower.contains("fod") ||
                        nameLower.contains("goodix") || nameLower.contains("fpc") ||
                        nameLower.contains("displayfeature") || nameLower.contains("biometrics")

                val relPath = try {
                    file.relativeTo(baseDir).path
                } catch (_: Throwable) {
                    file.name
                }

                ElfBinaryInfo(
                    fileName = file.name,
                    relativePath = relPath,
                    sizeBytes = file.length(),
                    is64Bit = is64Bit,
                    architecture = archName,
                    soname = resolvedSoname,
                    neededLibraries = neededLibs.distinct(),
                    isHalModule = isHal,
                    isFodRelated = isFod,
                    vendorOrSystemClassification = if (relPath.contains("system")) "SYSTEM" else "VENDOR"
                )
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun analyzeAllBlobs(blobsDir: File): ElfDependencyGraphReport {
        if (!blobsDir.exists()) {
            return ElfDependencyGraphReport(0, 0, emptyList(), emptyMap(), emptyList(), emptyList())
        }

        val allElfFiles = blobsDir.walkTopDown()
            .filter { it.isFile && (it.name.endsWith(".so") || !it.name.contains(".")) }
            .toList()

        val parsedInfos = mutableListOf<ElfBinaryInfo>()
        val depFrequencies = mutableMapOf<String, Int>()
        val sysDepsFromVendor = mutableSetOf<String>()
        val fodBlobs = mutableListOf<String>()

        for (f in allElfFiles) {
            val info = parseElf(f, blobsDir) ?: continue
            parsedInfos.add(info)

            if (info.isFodRelated) {
                fodBlobs.add(info.fileName)
            }

            for (dep in info.neededLibraries) {
                depFrequencies[dep] = (depFrequencies[dep] ?: 0) + 1
                if (dep.startsWith("libhidlbase") || dep.startsWith("libutils") ||
                    dep.startsWith("libcutils") || dep.startsWith("libbinder") ||
                    dep.startsWith("libgui") || dep.startsWith("libui") || dep.startsWith("libhardware")
                ) {
                    sysDepsFromVendor.add(dep)
                }
            }
        }

        val totalDeps = parsedInfos.sumOf { it.neededLibraries.size }

        return ElfDependencyGraphReport(
            totalBinariesAnalyzed = parsedInfos.size,
            totalDependenciesMapped = totalDeps,
            binaries = parsedInfos,
            mostCommonDependencies = depFrequencies.entries
                .sortedByDescending { it.value }
                .take(25)
                .associate { it.key to it.value },
            systemLibrariesDependedOnByVendor = sysDepsFromVendor.sorted(),
            fodRelatedBlobs = fodBlobs.distinct()
        )
    }
}
