package com.example.core.docs

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.core.storage.RomForgeStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GeneratedPdfDocResult(
    val pdfFile: File,
    val publishedDownloadPath: String?,
    val pageCount: Int,
    val sizeBytes: Long,
    val generatedAtLabel: String
)

/**
 * Generates the complete, structured, AI-auditable Technical Reference Manual & Architecture Blueprint PDF
 * (`ROM_Forge_Documentation_Complete_Architecture_IA_Audit.pdf`) using Android's native `PdfDocument` API.
 *
 * Includes:
 * 1. Exhaustive Description of Every Feature & Module (Unpack/Repack UKA v5.27, Repack Simple & Intelligent 1:1,
 *    FOD Fix Solution 1 & Solution 2, Sign PRO & Tout Signer Pro, R.E.C.O.R.E 12-Pillar Engine, AOSP OAT/VDEX/ODEX/fsv_meta
 *    Regenerator, Magisk PATCH, Hex Viewer, System App Debloater, VBMeta AVB 2.0 Generator).
 * 2. Complete Inventory of Internal Binary & Userspace Tools (`ExactImageCloneEngine`, `Ext4UserspaceExtractor`,
 *    `Ext4UserspaceBuilder`, `ErofsUserspaceBuilder`, `AospTopologyResolver`, `UkaConfigHelper`, `ArtGeneratorEngine`,
 *    `PayloadDumperEngine`, `LpUnpackEngine`, `BrotliSdatEngine`, `SignProEngine`, `RecoreEngine`, `AutoPorterEngine`).
 * 3. Step-by-Step User Manual (Manuel d'Utilisation Complet : DSU Sideloader, Unpack/Repack 1:1, FOD Fix 1 & 2, Sign Pro, R.E.C.O.R.E).
 * 4. Complete Application Structure & Source File Tree (`com.example.*` packages, classes, responsibilities, and lines of code)
 *    plus the `ROM_FORGE/` Workspace Tree (`UNPACK/`, `PACKED/`, `PORT/`, `ROM_FORGE_META/`, `config/`).
 * 5. Deep Technical Audit Notes for External AI Review (EXT4 Superblock `0xEF53`, `METADATA_CSUM`/`SHARED_BLOCKS` Copy-on-Write,
 *    HTree `0x00001000` handling, 256B Inode `security.selinux` `e_value_offs`, AVB 2.0 Footer truncation, and Stage 1 `secilc`/`libvintf` safety).
 */
object RomForgePdfManualGenerator {

    private const val PAGE_WIDTH = 595  // A4 width in PostScript points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 height in PostScript points
    private const val MARGIN_H = 36f
    private const val MARGIN_TOP = 44f
    private const val MARGIN_BOTTOM = 42f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_H * 2)

    suspend fun generateCompleteTechnicalManualPdf(
        context: Context,
        onLog: ((String) -> Unit)? = null
    ): GeneratedPdfDocResult = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val pdfDoc = PdfDocument()

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(12, 24, 48)
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 110, 140)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(16, 48, 92)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 35, 45)
            textSize = 8.8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 60, 40)
            textSize = 8.0f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(110, 120, 135)
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val bannerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(18, 42, 76)
            style = Paint.Style.FILL
        }
        val codeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(243, 246, 250)
            style = Paint.Style.FILL
        }
        val codeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(205, 215, 228)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }

        var currentPageNumber = 0
        var currentPage: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var cursorY = MARGIN_TOP

        fun finishCurrentPage() {
            val page = currentPage ?: return
            val c = canvas ?: return
            // Draw page footer
            c.drawLine(MARGIN_H, PAGE_HEIGHT - 30f, PAGE_WIDTH - MARGIN_H, PAGE_HEIGHT - 30f, codeBorderPaint)
            c.drawText(
                "ROM Forge Kitchen Studio • Manuel Technique, Architecture & Audit IA • Généré le $timestamp",
                MARGIN_H,
                PAGE_HEIGHT - 18f,
                footerPaint
            )
            val pgLabel = "Page $currentPageNumber"
            val pgW = footerPaint.measureText(pgLabel)
            c.drawText(pgLabel, PAGE_WIDTH - MARGIN_H - pgW, PAGE_HEIGHT - 18f, footerPaint)
            pdfDoc.finishPage(page)
            currentPage = null
            canvas = null
        }

        fun startNewPage() {
            finishCurrentPage()
            currentPageNumber++
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
            val page = pdfDoc.startPage(pageInfo)
            currentPage = page
            canvas = page.canvas
            cursorY = MARGIN_TOP
        }

        fun ensureSpace(neededHeight: Float) {
            if (currentPage == null || cursorY + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                startNewPage()
            }
        }

        fun wrapLines(text: String, paint: Paint, maxWidth: Float): List<String> {
            val out = mutableListOf<String>()
            for (rawParagraph in text.split("\n")) {
                if (rawParagraph.isEmpty()) {
                    out.add("")
                    continue
                }
                val words = rawParagraph.split(" ")
                var currentLine = StringBuilder()
                for (w in words) {
                    val candidate = if (currentLine.isEmpty()) w else "$currentLine $w"
                    if (paint.measureText(candidate) <= maxWidth) {
                        currentLine = StringBuilder(candidate)
                    } else {
                        if (currentLine.isNotEmpty()) {
                            out.add(currentLine.toString())
                        }
                        // If a single word/path is wider than maxWidth, split it by characters
                        if (paint.measureText(w) > maxWidth) {
                            var chunk = StringBuilder()
                            for (ch in w) {
                                if (paint.measureText("$chunk$ch") <= maxWidth) {
                                    chunk.append(ch)
                                } else {
                                    out.add(chunk.toString())
                                    chunk = StringBuilder(ch.toString())
                                }
                            }
                            currentLine = chunk
                        } else {
                            currentLine = StringBuilder(w)
                        }
                    }
                }
                if (currentLine.isNotEmpty()) {
                    out.add(currentLine.toString())
                }
            }
            return out
        }

        fun drawSectionBanner(number: String, title: String) {
            ensureSpace(32f)
            cursorY += 6f
            val c = canvas!!
            val rect = RectF(MARGIN_H, cursorY, PAGE_WIDTH - MARGIN_H, cursorY + 22f)
            c.drawRoundRect(rect, 4f, 4f, bannerBgPaint)
            c.drawText("$number. $title", MARGIN_H + 10f, cursorY + 15f, sectionHeaderPaint)
            cursorY += 30f
        }

        fun drawSubHeader(title: String) {
            ensureSpace(20f)
            cursorY += 4f
            canvas!!.drawText(title, MARGIN_H, cursorY + 10f, subHeaderPaint)
            cursorY += 15f
        }

        fun drawParagraph(text: String) {
            val lines = wrapLines(text, bodyPaint, CONTENT_WIDTH)
            for (line in lines) {
                ensureSpace(13f)
                canvas!!.drawText(line, MARGIN_H, cursorY + 9f, bodyPaint)
                cursorY += 12.2f
            }
            cursorY += 3f
        }

        fun drawBulletList(items: List<String>) {
            for (item in items) {
                val lines = wrapLines(item, bodyPaint, CONTENT_WIDTH - 14f)
                for ((idx, line) in lines.withIndex()) {
                    ensureSpace(13f)
                    if (idx == 0) {
                        canvas!!.drawText("•", MARGIN_H + 2f, cursorY + 9f, subHeaderPaint)
                    }
                    canvas!!.drawText(line, MARGIN_H + 14f, cursorY + 9f, bodyPaint)
                    cursorY += 12.0f
                }
                cursorY += 2f
            }
        }

        fun drawCodeOrTreeBlock(lines: List<String>) {
            val wrapped = mutableListOf<String>()
            for (l in lines) {
                wrapped.addAll(wrapLines(l, monoPaint, CONTENT_WIDTH - 16f))
            }
            var idx = 0
            while (idx < wrapped.size) {
                val availH = (PAGE_HEIGHT - MARGIN_BOTTOM) - cursorY
                val maxLinesInPage = ((availH - 12f) / 10.5f).toInt()
                if (maxLinesInPage < 3) {
                    startNewPage()
                    continue
                }
                val chunk = wrapped.subList(idx, (idx + maxLinesInPage).coerceAtMost(wrapped.size))
                val boxH = chunk.size * 10.5f + 10f
                val c = canvas!!
                val rect = RectF(MARGIN_H, cursorY, PAGE_WIDTH - MARGIN_H, cursorY + boxH)
                c.drawRoundRect(rect, 4f, 4f, codeBgPaint)
                c.drawRoundRect(rect, 4f, 4f, codeBorderPaint)
                var textY = cursorY + 11f
                for (cl in chunk) {
                    c.drawText(cl, MARGIN_H + 8f, textY, monoPaint)
                    textY += 10.5f
                }
                cursorY += boxH + 6f
                idx += chunk.size
            }
        }

        // Start Page 1: Cover & Document Header
        startNewPage()
        canvas!!.drawText("GASTRO (ROM FORGE STUDIO) — DOCUMENTATION TECHNIQUE COMPLÈTE", MARGIN_H, cursorY + 14f, titlePaint)
        cursorY += 22f
        canvas!!.drawText(
            "Spécification Exhaustive : GASTROengine (10 Modules Rust), EXTRACTOR, PORTER, FOD 1/2/3, CREATION (Make IMG/ROM A-à-Z) & R.E.C.O.R.E",
            MARGIN_H,
            cursorY + 10f,
            subtitlePaint
        )
        cursorY += 18f
        drawParagraph(
            "Ce document technique de référence détaille l'intégralité de l'architecture logicielle de GASTRO (ROM Forge Kitchen Studio), " +
                    "ses 7 modules d'action en barre latérale (Key Maker, Sign Pro, Generator, Compilator, EXTRACTOR, PORTER, CREATION), " +
                    "son cœur modulaire GASTROengine (10 composants Rust), son cerveau déterministe de fond R.E.C.O.R.E (SCANNER 16 rapports + COMPARE + Solveur Z3 SMT), " +
                    "son moteur de clonage et greffe chirurgicale 1:1 (ExactImageCloneEngine), sa sonde dynamique double partition HostSystemAndVendorFodProbe (/system + /vendor) " +
                    "et son pipeline Graal de reconstruction OS de A à Z (MAKE IMG & MAKE ROM en 12 étapes assistées par l'IA gratuite Gemini/AI Studio avec Anti-Quota). " +
                    "Il sert à la fois de manuel d'utilisation complet et de dossier d'audit technique prêt à être partagé avec une autre IA."
        )

        // =========================================================================
        // SECTION 1: DESCRIPTION EXHAUSTIVE DE CHAQUE FONCTIONNALITÉ
        // =========================================================================
        drawSectionBanner("1", "DESCRIPTION EXHAUSTIVE DE CHAQUE FONCTIONNALITÉ DE L'APPLICATION")

        drawSubHeader("1.1. Compilator — Unpack Universel UKA v5.27 & Repack Simple & Intelligent 1:1 (UNPACK / PACKED)")
        drawBulletList(
            listOf(
                "Unpack UKA v5.27 : Décompresse les images EXT4 (magic 0xEF53), Sparse-EXT4 (0xED26FF3A), EROFS (0xE0F5E1E2) et Sparse-EROFS dans ROM_FORGE/UNPACK/<nom_img>/ sans copie temporaire inutile (SAF Direct FileChannel).",
                "Capture du Snapshot Immuable 1:1 : Génère simultanément les tables UKA (config/<part>_fs_config, config/<part>_file_contexts, <part>_size.txt, <part>_space.txt, <part>_avb_footer.bin) ET le snapshot immuable ROM_FORGE_META/base_img_snapshot.txt + exact_inode_extents_map.txt + base_source.img.",
                "Repack Simple & Intelligent 1:1 (Compatible DSU Sideloader) : Si 0 fichier n'a été modifié, clone bit-à-bit l'image Raw de départ (100% identique à l'octet près). Si des fichiers ont été ajoutés ou modifiés, applique une greffe chirurgicale In-Place avec Copy-On-Write (CoW) sur le clone 1:1 via ExactImageCloneEngine sans jamais altérer les signatures APK ni corrompre les blocs partagés EXT4."
            )
        )

        drawSubHeader("1.2. EXTRACTOR — Extraction Sécurisée de l'ADN du Téléphone (ExtractMe -> ROM_FORGE/EXTRACT/)")
        drawBulletList(
            listOf(
                "Bouton ExtractMe : Propulsé par GASTROengine (DeviceProfiler + ShellOrchestrator + FileExtractor + FsParser), extrait en lecture seule tous les éléments matériels et logiciels de votre téléphone vers ROM_FORGE/EXTRACT/.",
                "Isolation Sandbox ShellOrchestrator : Aucune écriture n'est jamais effectuée sur le système du téléphone (politique RO-Host / RW-Workspace stricte).",
                "Éléments capturés dans EXTRACT/ : device_dna_profile.json, proc_mounts_snapshot.txt, partitions_layout.txt, init_services_dna.txt, host_symlinks_topology.txt (/product, /system_ext, /vendor, /odm, /init), fsparser_xattr_permissions.txt, ainsi que l'analyse dynamique double partition (system_fod_logic/ et vendor_fod_logic/)."
            )
        )

        drawSubHeader("1.3. PORTER — Plateforme de Portage GSI (PORT • PORTERPLAN • FOD : SCAN, AISCAN, FOD Fix 1, 2 & 3)")
        drawBulletList(
            listOf(
                "Sélection de l'OS Unpacké & Bouton PORT : Déverrouillé dès qu'un extract est présent dans ROM_FORGE/EXTRACT/. Crée le portage complet du Device Tree (blobs, RRO, audio, display, SELinux, VINTF) vers l'OS unpacké sélectionné.",
                "Page dédiée PORTERPLAN : Compare l'architecture du GSI unpacké avec celle du téléphone (StructureAligner & VintfReconciler), explique pourquoi chaque sous-système fonctionne dans votre ROM actuelle et pourquoi il échoue dans le GSI, et dresse la liste exacte des éléments à porter.",
                "Page dédiée FOD — Sonde Dynamique Double Partition (HostSystemAndVendorFodProbe) : Scanne à la fois la partition /system (/product, /system_ext) ET la partition /vendor (/odm, /sys, /dev) du téléphone pour comprendre comment le système hôte pilote le FOD (keylayout dynamique, keycode 338/304, coordonnées X/Y/Rayon, nœuds sysfs HBM/DimLayer/Touch).",
                "Boutons SCAN & AISCAN : SCAN compare le GSI avec les fichiers extraits d'EXTRACTOR ; AISCAN interroge le cerveau IA gratuit (Gemini 2.5 Flash / Pro via BuildConfig.GEMINI_API_KEY avec rotation Anti-Quota et cache local) combiné à GASTROengine.",
                "Trois niveaux de correction FOD (FOD fix 1, FOD fix 2, FOD fix 3) : (1) FOD fix 1 (Solution Complète Stock-Grade : patch APK AXML aligné 4 octets + RRO + régénération OAT/VDEX/fsv_meta), (2) FOD fix 2 (Solution Bidouillage Zéro-APK : injection directe dans l'OS unpacké sans repacker et sans modifier aucun APK), (3) FOD fix 3 (Assisté par l'IA + GASTROengine + R.E.C.O.R.E : alignement symlinks, synthèse dynamique /system+/vendor, overlays RRO compilés ResTable_header 0x0002 / AXML 0x0003 et preuve formelle Z3)."
            )
        )

        drawSubHeader("1.4. CREATION — Le Graal de la Reconstruction OS de A à Z (MAKE IMG & MAKE ROM en 12 Étapes)")
        drawBulletList(
            listOf(
                "Objectif : Reconstruire un système Android complet de A à Z à partir du GSI unpacké choisi et de l'ADN extrait dans EXTRACT/, exactement comme une compilation AOSP/LineageOS depuis le code source (lunch + mka systemimage / mka bacon).",
                "Pipeline A-à-Z en 12 Étapes Unifiées : [1/12] EXTRACTOR (DeviceProfiler) -> [2/12] StructureAligner (SAR & Symlinks) -> [3/12] AiPortingAgent (Plan IA Gemini + Anti-Quota) -> [4/12] PORTER (Greffe Device Tree) -> [5/12] FOD Engine (FOD Fix 3 dynamique) -> [6/12] VintfReconciler (ELF64 & HIDL/AIDL) -> [7/12] KEY MAKER (Clés RSA-2048) -> [8/12] SIGN PRO (Signature 4K des Overlays RRO sans corrompre les APKs système) -> [9/12] GENERATOR (Synchronisation ART .odex/.vdex/.fsv_meta) -> [10/12] COMPILATOR & FsParser (Audit POSIX init 0750 & SELinux) -> [11/12] R.E.C.O.R.E (SCANNER 16 rapports + COMPARE + Z3 SMT + Reconstruction 1:1) -> [12/12] CROSS-VERIFIER.",
                "Bouton MAKE IMG : Exécute les 12 étapes et génère l'image bootable GASTRO_<gsi>_source_built.img + vbmeta.img dans ROM_FORGE/CREATION/ et ROM_FORGE/PACKED/.",
                "Bouton MAKE ROM : Exécute les 12 étapes puis assemble une archive Custom ROM complète flashable en Recovery/Sideload (.zip) contenant META-INF/com/google/android/updater-script, update-binary, dynamic_partitions_op_list (redimensionnement super), system.img, vbmeta.img, otacert et le dossier EXTRACT_DNA/."
            )
        )

        drawSubHeader("1.5. Key Maker, Sign Pro & Generator (Chaîne Cryptographique & Bytecode ART)")
        drawBulletList(
            listOf(
                "Key Maker : Génère les paires de clés RSA-2048 X.509v3 (.pk8 et .x509.pem) + manifest.json (Clé Note) dans ROM_FORGE/KEY/.",
                "Sign Pro (Cartographie Multi-Clés, Sign All & Sign All Pro) : Détecte automatiquement s'il y a 1, 4, 5, 6, 8+ certificats distincts dans l'image, génère les clés correspondantes, et propose 'Sign All' (signature pure des APKs uniquement par cluster de rôle) et 'Sign All Pro' (signature APKs + mise à jour plat_mac_permissions.xml, otacerts.zip et régénération complète .odex/.vdex/.oat/.art/.fsv_meta).",
                "Generator (ArtGeneratorEngine) : Analyse la version exacte d'OAT (ex: 195/225/238) et VDEX (021/027) de l'image de base et régénère des artefacts ELF64 OAT, VDEX et fs-verity (.fsv_meta) 100% conformes AOSP."
            )
        )

        drawSubHeader("1.6. R.E.C.O.R.E (Moteur de Fond : SCANNER 16 Rapports + COMPARE + Solveur Z3 SMT)")
        drawBulletList(
            listOf(
                "Opère automatiquement en arrière-plan pour assister Compilator, PORTER, FOD et CREATION sans alourdir la navigation.",
                "Moteur SCANNER (16 Rapports Exhaustifs) : Génère et maintient 16 rapports JSON/TXT dans KEY/Data/recore_scanner_reports/ couvrant chaque sous-système (01_apks_and_signatures, 02_art_oat_vdex_odex, 03_elf64_native_binaries, 04_selinux_contexts_and_mac, 05_vintf_and_hal_manifests, 06_init_rc_boot_scripts, 07_symlinks_and_sar_topology, etc.).",
                "Moteur COMPARE & Solveur Formelle Z3 SMT : Compare l'état initial et l'état post-modification, vérifie que toutes les dépendances en cascade sont satisfaites (Z3 = SAT) et garantit un score de confiance boot optimal avant toute création d'image."
            )
        )

        // =========================================================================
        // SECTION 2: LISTE COMPLÈTE DES OUTILS & MOTEURS COMPOSANT L'APPLICATION
        // =========================================================================
        drawSectionBanner("2", "INVENTAIRE COMPLET DES OUTILS & MOTEURS INTERNES (KOTLIN + GASTROENGINE RUST)")
        drawBulletList(
            listOf(
                "1. GastroEngine (core/gastro/GastroEngine.kt) : Orchestrateur central réunissant les 10 sous-systèmes Rust (intelligence_hub, ShellOrchestrator, rom_toolchain, TaskPipeline, DeviceProfiler, VintfReconciler_DependencyAnalyzer, AiPortingAgent, FileExtractor, FsParser, StructureAligner) et le pipeline Graal MAKE IMG / MAKE ROM en 12 étapes.",
                "2. HostSystemAndVendorFodProbe (modules/porter/HostSystemAndVendorFodProbe.kt) : Sonde dynamique inspectant à la fois /system (/product, /system_ext) et /vendor (/odm, /sys, /dev) du téléphone pour synthétiser un fix FOD non-statique sur mesure.",
                "3. RecoreEngine & RecoreScannerAndCompareEngines (core/recore/) : Moteur déterministe à 12 Piliers avec SCANNER (16 rapports), COMPARE et solveur Z3 SMT.",
                "4. ExactImageCloneEngine (core/img/ExactImageCloneEngine.kt) : Clonage 1:1 bit-à-bit Sparse/Raw et mutateur EXT4 chirurgical In-Place avec Copy-On-Write (CoW), support HTree, désactivation propre de METADATA_CSUM lors d'une greffe et ajustement AVB 2.0.",
                "5. Ext4UserspaceExtractor & Ext4UserspaceBuilder (core/img/) : Décompilateur et constructeur EXT4 100% userspace fidèles au Superblock 0xEF53 et aux xattrs SELinux.",
                "6. ErofsUserspaceExtractor & ErofsUserspaceBuilder (core/img/) : Extracteur et constructeur EROFS v1 (magic 0xE0F5E1E2).",
                "7. AospTopologyResolver & UkaConfigHelper (core/img/) : Aligneur de topologie SAR (/system/system vs /system) et gestionnaire des tables UKA v5.27 (fs_config, file_contexts).",
                "8. AutoPorterEngine (modules/porter/AutoPorterEngine.kt) : Moteur de portage GSI et des 3 solutions FOD Fix (FOD fix 1, FOD fix 2, FOD fix 3).",
                "9. SignProEngine & KeyMakerEngine (modules/signpro/, modules/keymaker/) : Cartographie multi-clés N-clusters, génération RSA-2048 et signature Sign All / Sign All Pro.",
                "10. ArtGeneratorEngine (modules/generator/ArtGeneratorEngine.kt) : Profilage et régénération AOSP des fichiers .odex, .vdex, .oat, .art et .fsv_meta.",
                "11. ImgCompilerEngine & CrossVerifierEngine (modules/compiler/, core/verifier/) : Pré-audit statique, compilation .img/vbmeta.img et vérification croisée AOSP.",
                "12. HybridShellEngine & RomForgeStorageManager (core/shell/, core/storage/) : Exécuteur hybride Root/Userspace et synchronisation publique vers /storage/emulated/0/ROM_FORGE/."
            )
        )

        // =========================================================================
        // SECTION 3: MANUEL D'UTILISATION PAS-À-PAS
        // =========================================================================
        drawSectionBanner("3", "MANUEL D'UTILISATION COMPLET PAS-À-PAS")
        drawBulletList(
            listOf(
                "Étape 1 — Décompiler un GSI ou une image système (Compilator -> Unpack) : Ouvrez Compilator dans la barre latérale, cliquez sur 'Choisir .IMG à Décompiler' pour extraire votre image dans ROM_FORGE/UNPACK/<nom>/ avec son snapshot immuable 1:1.",
                "Étape 2 — Extraire l'ADN du téléphone (EXTRACTOR -> ExtractMe) : Ouvrez EXTRACTOR dans la barre latérale et cliquez sur 'ExtractMe (Extraire Téléphone -> Dossier EXTRACT/)'. Le moteur sonde /system et /vendor en lecture seule et isole tous les fichiers dans ROM_FORGE/EXTRACT/.",
                "Étape 3 — Analyser et Porter (PORTER -> PORTERPLAN / PORT / FOD) : Ouvrez PORTER, sélectionnez l'OS unpacké dans la liste déroulante. Consultez 'PORTERPLAN' pour voir les différences d'architecture, cliquez sur 'PORT' pour porter le Device Tree, ou ouvrez 'FOD' pour lancer SCAN, AISCAN et appliquer FOD fix 1, FOD fix 2 ou FOD fix 3.",
                "Étape 4 — Reconstruire un OS complet de A à Z (CREATION -> MAKE IMG / MAKE ROM) : Ouvrez CREATION, choisissez votre GSI unpacké et cliquez sur 'MAKE IMG' (pour générer l'image .img bootable en 12 étapes) ou sur 'MAKE ROM' (pour créer la Custom ROM complète en .zip flashable).",
                "Étape 5 — Signer ou vérifier les clés (Key Maker & Sign Pro) : Utilisez 'Sign All' pour signer uniquement les APKs par cluster de clés, ou 'Sign All Pro' pour signer les APKs et régénérer toute la chaîne XML + OAT/VDEX/fsv_meta."
            )
        )

        // =========================================================================
        // SECTION 4: STRUCTURE DE L'APPLICATION & ARBORESCENCE COMPLÈTE DU CODE
        // =========================================================================
        drawSectionBanner("4", "STRUCTURE DE L'APPLICATION & ARBORESCENCE COMPLÈTE (CODE & WORKSPACE)")
        drawParagraph(
            "L'application suit l'architecture Clean MVVM avec Jetpack Compose (Material 3), Kotlin Coroutines/Flow, Retrofit/OkHttp (API Gemini gratuite avec Anti-Quota) et Room Database. " +
                    "Voici l'arborescence exacte des packages et fichiers sources Kotlin ainsi que du dossier ROM_FORGE :"
        )
        drawCodeOrTreeBlock(
            listOf(
                "app/src/main/java/com/example/",
                "├── MainActivity.kt                               # Navigation : 7 modules Sidebar + 3 volets BottomBar (Action, Console, Paramètres)",
                "├── core/",
                "│   ├── assets/AssetBinaryManager.kt              # Extraction des binaires natifs & peuplement AOSP de référence",
                "│   ├── docs/RomForgePdfManualGenerator.kt        # Générateur PDF natif de documentation technique, arborescence & audit IA",
                "│   ├── gastro/GastroEngine.kt                    # Cœur GASTROengine (10 modules Rust + IA Gemini Anti-Quota + MAKE IMG/ROM 12 étapes)",
                "│   ├── img/",
                "│   │   ├── AospTopologyResolver.kt               # Aligneur topologie SAR (/system/system vs /system) & protection SELinux",
                "│   │   ├── ErofsUserspaceBuilder.kt              # Constructeur d'images EROFS v1 (0xE0F5E1E2)",
                "│   │   ├── ErofsUserspaceExtractor.kt            # Extracteur d'images EROFS v1",
                "│   │   ├── ExactImageCloneEngine.kt              # Clonage 1:1 bit-à-bit & Mutateur Chirurgical EXT4 In-Place (CoW, HTree, AVB)",
                "│   │   ├── Ext4UserspaceBuilder.kt               # Constructeur EXT4 100% userspace fidèle au Superblock d'origine",
                "│   │   ├── Ext4UserspaceExtractor.kt             # Extracteur EXT4 100% userspace + snapshot immuable 1:1",
                "│   │   └── UkaConfigHelper.kt                    # Synchronisation tables UKA v5.27 (fs_config, file_contexts)",
                "│   ├── recore/",
                "│   │   ├── RecoreEngine.kt                       # Moteur de fond R.E.C.O.R.E à 12 Piliers & solveur Z3 SMT",
                "│   │   ├── RecoreModels.kt                       # Modèles de données R.E.C.O.R.E, DAG, ELF64 et Z3",
                "│   │   └── RecoreScannerAndCompareEngines.kt     # Moteurs SCANNER (16 rapports détaillés) & COMPARE",
                "│   ├── shell/HybridShellEngine.kt                # Exécuteur hybride Root/Userspace & ShellOrchestrator",
                "│   ├── storage/RomForgeStorageManager.kt         # Gestionnaire public /storage/emulated/0/ROM_FORGE (UNPACK, EXTRACT, PORT, CREATION, PACKED, KEY, DOCS)",
                "│   └── verifier/CrossVerifierEngine.kt           # Vérificateur croisé d'intégrité AOSP",
                "├── data/",
                "│   ├── local/ (AppDatabase.kt, Entities.kt, RomKitchenDao.kt)",
                "│   └── repository/RomKitchenRepository.kt        # Dépôt réactif Flow",
                "├── modules/",
                "│   ├── compiler/ImgCompilerEngine.kt             # Unpack/Repack UKA, Repack Simple & Intelligent 1:1 & VBMeta AVB 2.0",
                "│   ├── generator/ArtGeneratorEngine.kt           # Régénérateur AOSP OAT (.odex), VDEX (.vdex), .art & .fsv_meta",
                "│   ├── keymaker/KeyMakerEngine.kt                # Générateur cryptographique RSA-2048 (.pk8 / .x509.pem) & manifest.json",
                "│   ├── porter/",
                "│   │   ├── AutoPorterEngine.kt                   # Moteur PORTER, PORTERPLAN & FOD Fix 1, 2 et 3",
                "│   │   └── HostSystemAndVendorFodProbe.kt        # Sonde dynamique double partition (/system + /vendor) pour le FOD",
                "│   └── signpro/SignProEngine.kt                  # Cartographie multi-clés N-clusters, Sign All & Sign All Pro",
                "└── ui/",
                "    ├── RomKitchenViewModel.kt                    # ViewModel centralisant l'état StateFlow et les actions des 7 modules",
                "    ├── components/CommonComponents.kt            # Barre de statut système, sélecteur UNPACK & composants Material 3",
                "    ├── screens/ (KeyMakerScreen, SignProScreen, GeneratorScreen, CompilerScreen, ExtractorScreen, AutoPorterScreen, CreationScreen, RecoreScreen, ConsoleHistoryScreen, HelpScreen)",
                "    └── theme/ (Color.kt, Theme.kt, Type.kt)"
            )
        )

        drawSubHeader("4.2. Arborescence du Dossier de Travail Public ROM_FORGE/")
        drawCodeOrTreeBlock(
            listOf(
                "/storage/emulated/0/ROM_FORGE/",
                "├── UNPACK/<nom_gsi>/                # Systèmes .img décompilés + config/ UKA + ROM_FORGE_META/ (snapshot 1:1)",
                "├── EXTRACT/                         # ADN complet du téléphone extrait par EXTRACTOR (ExtractMe)",
                "│   ├── device_dna_profile.json      # Profil matériel & logiciel DeviceProfiler",
                "│   ├── device_dna/                  # proc_mounts_snapshot.txt, partitions_layout.txt, host_symlinks_topology.txt",
                "│   ├── system_fod_logic/            # Logique FOD extraite de /system (init RC dynamique, keylayout, props /system)",
                "│   └── vendor_fod_logic/            # Logique FOD extraite de /vendor (nœuds sysfs HBM/DimLayer/Touch, props /vendor)",
                "├── PORT/                            # Dossiers et images .img portés par PORTER et FOD Fix 1/2/3",
                "├── CREATION/                        # Images .img (MAKE IMG) et archives Custom ROM flashables .zip (MAKE ROM)",
                "├── PACKED/                          # Images recompilées (system.img, vbmeta.img) et APKs signés",
                "├── KEY/ & KEY/Data/                 # Clés RSA-2048 (.pk8/.pem), manifest.json et les 16 rapports SCANNER / SignPro",
                "└── DOCS/                            # Manuel technique complet & dossier d'audit IA (.PDF)"
            )
        )

        // =========================================================================
        // SECTION 5: DIAGNOSTIC TECHNIQUE APPROFONDI (POUR AUDIT PAR UNE AUTRE IA)
        // =========================================================================
        drawSectionBanner("5", "DIAGNOSTIC TECHNIQUE APPROFONDI & GARANTIES ANTI-BOOTLOOP (DSU / FASTBOOT / RECOVERY)")
        drawParagraph(
            "Cette section documente les garanties architecturales implémentées dans GASTRO pour éliminer tout risque de bootloop " +
                    "lors du Repack, du FOD Fix ou de la reconstruction complète MAKE IMG / MAKE ROM :"
        )
        drawBulletList(
            listOf(
                "Garantie 1 — Préservation Intégrale de SELinux Stage-1 (/system/etc/selinux/*.cil) et VINTF (/system/etc/vintf/manifest.xml) : Aucun fichier .cil conflictuel ni entrée HAL vendor.* illégale dans un manifeste framework n'est injecté, garantissant 100% de réussite de secilc et libvintf au démarrage.",
                "Garantie 2 — Sonde FOD Double Partition (/system + /vendor) Non-Statique : HostSystemAndVendorFodProbe inspecte comment la partition /system de la ROM hôte dialogue avec /vendor (keylayout exact, keycode 338/304, coordonnées X/Y/Rayon, nœuds sysfs HBM/DimLayer/Touch) et génère des overlays RRO dotés d'une table de ressources binaire valide (ResTable_header 0x0002 + AndroidManifest.xml AXML 0x0003 alignés sur 4 octets).",
                "Garantie 3 — Chirurgie EXT4 Copy-On-Write (CoW), HTree & Inodes 256 octets : ExactImageCloneEngine alloue de nouveaux blocs libres lors de toute modification (protégeant les blocs partagés SHARED_BLOCKS 0x4000 d'e2fsdroid -s), ajuste proprement METADATA_CSUM/GDT_CSUM et calcule e_value_offs de security.selinux relativement à IFIRST(header) (offset 164).",
                "Garantie 4 — Clé IA Embarquée à la Compilation (BuildConfig.GEMINI_API_KEY) + Anti-Quota : Injectée automatiquement via AI Studio Secrets ou GitHub Actions (secrets.GEMINI_API_KEY -> .env -> BuildConfig) sans aucune saisie requise dans l'APK, avec rotation automatique entre gemini-2.5-flash, gemini-2.5-pro et gemini-2.0-flash et synthèse formelle locale hors-ligne."
            )
        )

        finishCurrentPage()

        val storageManager = RomForgeStorageManager(context)
        val docsDir = File(storageManager.getRomForgePublicRoot(), "DOCS").apply { mkdirs() }
        val pdfFile = File(docsDir, "ROM_Forge_Documentation_Complete_Architecture_IA_Audit.pdf")
        FileOutputStream(pdfFile).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()

        // Also copy to the system public Downloads folder if accessible
        val publicDownloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val publishedPath = try {
            publicDownloads.mkdirs()
            val dlCopy = File(publicDownloads, pdfFile.name)
            pdfFile.copyTo(dlCopy, overwrite = true)
            onLog?.invoke("[PDF-EXPORT] Manuel technique et d'audit IA exporté dans : ${dlCopy.absolutePath} et ${pdfFile.absolutePath}")
            dlCopy.absolutePath
        } catch (_: Exception) {
            onLog?.invoke("[PDF-EXPORT] Manuel technique et d'audit IA généré dans : ${pdfFile.absolutePath}")
            pdfFile.absolutePath
        }

        GeneratedPdfDocResult(
            pdfFile = pdfFile,
            publishedDownloadPath = publishedPath,
            pageCount = currentPageNumber,
            sizeBytes = pdfFile.length(),
            generatedAtLabel = timestamp
        )
    }
}
