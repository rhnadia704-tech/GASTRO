# GASTRO OMEGA-FORGE AI • Plan d'Architecture : Cerveau IA Autonome, Support Universel d'Images `.img`, Désassemblage Code Source AOSP & Portage 100 % Fonctionnel

Ce plan intègre un **Cerveau IA Architecte AOSP (`GeminiOsArchitectAgent`)** directement connecté aux moteurs binaires de l'application (`UNPACK`, `EXTRACTME`, `CREATION`, `SIGN PRO`) ainsi qu'un **Décodeur Universel d'Images `.img`**. L'application peut ainsi **ouvrir et comprendre n'importe quel fichier `.img`**, le décompiler en **Code Source Android AOSP**, dialoguer automatiquement avec l'IA pour résoudre chaque incompatibilité matérielle (HALs, Blobs, VNDK, SELinux, VINTF, Overlays) et générer une **image `.img` ou Custom ROM `.zip` 100 % fonctionnelle du premier coup**.

---

## User Review & Critical Innovations

> [!IMPORTANT]
> **Architecture Autonome Pilotée par l'IA + Ingénierie Bas-Niveau Universelle :**
> 1. **Communication Directe & Automatique avec l'IA (`GeminiOsArchitectAgent` via `gemini-3.1-pro-preview`)** :
>    - À chaque étape critique (**Unpack/Désassemblage**, **Extraction Root ExtractMe**, **Portage CREATION**, **Simulation Pré-Boot**), l'outil transmet automatiquement à l'IA la télémétrie structurée (symboles ELF64 manquants, contrats `.hal`/`.aidl`, erreurs `SELinux .cil`, conflits `VINTF`, classes DEX `UdfpsController`/`DisplayPowerController` et Device Tree du téléphone).
>    - **Action autonome de l'IA (Structured JSON Function-Execution)** : l'IA ne se contente pas de conseiller ; elle génère et applique directement :
>      - Le code des **Shims C/ELF64** et la configuration `ld.config.txt` pour réconcilier les blobs `.so` propriétaires.
>      - Les règles **SELinux `.cil`** exactes et les fragments **VINTF (`manifest.xml` / `compatibility_matrix.xml`)**.
>      - Les fichiers **`res/values/config.xml`** de l'Overlay RRO sur-mesure (coordonnées exactes FOD/UDFPS, HBM, découpe écran, fréquences 120Hz, audio, batterie).
>      - Les scripts **`init.<device>.rc`** et propriétés multi-partitions (`build.prop`) adaptés au SoC et au kernel du téléphone.
>      - **Mode Hybride IA + Déterministe** : si la connexion réseau est indisponible, le moteur bascule sans interruption sur son solveur heuristique embarqué pour garantir 100 % de continuité.
> 2. **Support Universel de N'importe Quelle Image `.img` (`UniversalImgDecoderEngine`)** :
>    - Détection et prise en charge automatique de **tous les formats d'images Android** :
>      - **Android Sparse Image** (`0xED26FF3A`) avec reconstruction streaming chunk-par-chunk.
>      - **Super Partition (`super.img`)** avec lecture de la géométrie `lp_metadata` (`system_a`, `vendor_a`, `product_a`, `system_ext_a`, `odm_a`).
>      - **EXT4** (avec ou sans journal, blocs 4K, extents profonds) et **EROFS** (`0xE0F5E1E2` avec compression LZ4 / LZ4HC / MicroLZMA).
>      - **Boot / Vendor_Boot / Recovery `.img`** (`ANDROID!` v0 à v4, ramdisk gzip/lz4/cpio, DTB/DTBO).
>      - **Images compressées ou encapsulées** (`.img.lz4`, `.img.gz`, `.img.zst`, `payload.bin`).
> 3. **Désassemblage de Niveau Code Source Android (`AospSourceDecompilerEngine`)** :
>    - Décompile l'image `.img` en un véritable **Arbre Code Source AOSP (`ROM_FORGE/AOSP_SOURCE_TREE/`)** :
>      - **DEX -> Code Source Java/Kotlin** (`framework.jar`, `services.jar`, `SystemUI.apk`).
>      - **ELF64 ARM64 -> Pseudo-Code C/C++ (`.cpp`, `.h`) & Contrats `.hal`/`.aidl`** pour tous les binaires et librairies `.so`.
>      - **AXML / `resources.arsc` -> XML AOSP & `Android.bp`** + **Graphe Synaptique de toutes les interdépendances de l'OS**.
> 4. **ExtractMe v4 & Accès Root Multi-Vecteurs (`HybridShellEngine`)** :
>    - Acquisition Root réelle (`Magisk`, `KernelSU`, `APatch`, `/system/bin/su`, `/sbin/su`) + extraction du **Device Tree AOSP complet** et des **binaires bruts (`.so`, HALs)** du téléphone.
> 5. **Boucle Auto-Correctrice Pré-Boot & Streaming Anti-OOM (`PreBootEmulatorEngine` & `SignProEngine`)** :
>    - Le simulateur teste les 5 stades du démarrage Android (`Init Stage 1`, `SELinux`, `VINTF`, `Linker64`, `Zygote/ART`). Tant qu'un score de **100 % Bootable** n'est pas atteint, l'orchestrateur IA + Natif corrige automatiquement les anomalies avant de compiler l'image `.img` ou la Custom ROM `.zip` en streaming disque pur (zéro `OutOfMemoryError`).

---

## 1. Overview & Core Concept

- **What It Does** : Une forge d'OS Android autonome assistée par IA qui prend en entrée **n'importe quel fichier `.img`** et l'ADN matériel de votre téléphone, et livre en sortie une **image `.img` ou une Custom ROM `.zip` 100 % spécifique à votre appareil et garantie fonctionnelle** :
  1. **Universal Unpack & AOSP Source Decompiler** : Décode n'importe quel format `.img` et reconstitue son code source Android (`Java/Kotlin`, `C/C++ .cpp/.h`, `.hal/.aidl`, `Android.bp`, `XML`, `init.rc`, `SELinux .cil`).
  2. **ExtractMe Root Hardware DNA** : Extrait en Root réel le Device Tree AOSP complet et les binaires bruts du téléphone hôte.
  3. **AI-Driven Omega Porting & Auto-Healing Loop** : L'IA (`gemini-3.1-pro-preview`) analyse conjointement le code source désassemblé de l'image et le Device Tree du téléphone, génère les ponts HAL/Blobs/VNDK/SELinux/Overlays et pilote la recompilation jusqu'à validation totale des 5 stades de boot.
- **Target Audience** : Développeurs et passionnés souhaitant convertir automatiquement n'importe quelle image `.img` ou GSI en un système 100 % natif et fonctionnel pour leur téléphone.
- **Key Value** : Alliance unique d'un **décompilateur AOSP bas-niveau** et d'un **agent IA ingénieur système Android** qui automatise 100 % du portage et élimine tout risque de bootloop.

---

## 2. User Experience & Visual Design

- **Key User Flows** :
  1. **Flux 1 — Unpack Universel & Studio Code Source AOSP + Analyse IA (`UNPACK`)** :
     - Ouverture de **n'importe quel `.img`** -> Détection automatique du format (`Sparse`, `EXT4`, `EROFS`, `Super`, `Boot`, `LZ4`) -> Déballage + **Décompilation Code Source AOSP** + **Audit IA automatique de l'architecture de l'image**.
     - **Explorateur Code Source AOSP intégré** pour parcourir les fichiers `.java`, `.cpp`, `.h`, `.hal`, `.aidl`, `Android.bp` et `config.xml`.
  2. **Flux 2 — ExtractMe Root Multi-Vecteurs & ADN Matériel (`EXTRACTOR`)** :
     - **Bouton d'Acquisition Root Actif** (`Magisk`, `KernelSU`, `APatch`, `su`) + **Extraction Unifiée ExtractMe** (Partitions, Binaires bruts `.so`, HALs et Device Tree AOSP complet dans `ROM_FORGE/EXTRACT/`).
  3. **Flux 3 — CREATION Pilotée par l'IA : Portage Automatique 100 % Fonctionnel (`CREATION`)** :
     - **Bouton « Auto-Pilot AI Port & Build (100% Working IMG / ROM) »** :
       1. L'agent IA compare le **Code Source désassemblé de l'IMG** et le **Device Tree du téléphone**.
       2. Il synthétise et injecte en temps réel les correctifs matériels (HALs, Blobs, Shims ELF64, Overlays RRO 4K, SELinux `.cil`, VINTF, `init.rc`).
       3. Le **Simulateur Pré-Boot à 5 Stades** vérifie l'image ; si une anomalie subsiste, l'IA la répare automatiquement en boucle fermée.
       4. Compilation finale via **`Make IMG`** (image système `.img` flashable) ou **`Make ROM`** (archive Custom ROM `.zip` complète).
- **Visual Identity & Theme** :
  - *Aesthetic Direction* : **Precision Cyber-Engineering, AI Neural Telemetry & AOSP Source Studio** (interface sombre haute précision avec console de dialogue IA en direct, inspecteur de code source AOSP et radar de validation de boot en 5 stades).
  - *Color Palette* : Fond carbone profond (`#0B0F17`), panneaux d'ingénierie (`#131B26`), Emeraude (`#00E676`) pour les validations de boot et Root, Cyan (`#00B0FF`) pour le code source AOSP, Violet/Indigo (`#7C4DFF`) pour le copilote IA autonome `GeminiOsArchitect`, et Ambre/Corail (`#FFB300` / `#FF5252`) pour les anomalies auto-corrigées.
  - *Typography & Hierarchy* : Titres techniques structurés et police `Monospace` pour l'explorateur de code source AOSP et les journaux de synthèse IA.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 : Agent IA Architecte AOSP en Boucle Fermée (`GeminiOsArchitectAgent`)**
  - *Chosen Approach* : Intégrer l'API REST Gemini (`gemini-3.1-pro-preview` pour l'architecture/synthèse de code de portage et `gemini-3.5-flash` pour les diagnostics rapides en streaming) avec `responseSchema` JSON strict (`BuildConfig.GEMINI_API_KEY` via `.env.example`).
  - L'IA reçoit le résumé structuré du code source désassemblé de l'image `.img` et du Device Tree du téléphone, et retourne directement un **Plan d'Exécution Exécutable (`AiPortingBlueprint`)** contenant les règles SELinux `.cil`, les mappings de symboles ELF64/Shims, les fragments XML VINTF, les valeurs exactes de l'Overlay RRO et les commandes `init.rc`, que le moteur applique automatiquement.
  - Un **moteur expert local de secours** garantit que toutes les fonctions restent 100 % opérationnelles même hors-ligne ou sans clé configurée.
  - *Why* : Chaque image `.img` et chaque constructeur (Xiaomi, Samsung, OnePlus, Google, Realme, etc.) possède des particularités uniques ; la combinaison du désassemblage code source et du raisonnement de Gemini permet de résoudre automatiquement même les cas matériels les plus complexes.

- **Decision 2 : Décodeur & Reconstructeur Universel d'Images `.img` (`UniversalImgEngine`)**
  - *Chosen Approach* : Supporter en lecture et en reconstruction l'ensemble des formats Android (`Sparse`, `EXT4`, `EROFS`, `Super`, `Boot/Vendor_Boot`, `LZ4/Gzip`) en streaming disque pur, avec calcul exact de l'espace libre (+ marge de sécurité anti-ENOSPC), préservation des symlinks/xattrs SELinux et signature `AVB 2.0`.
  - *Why* : Garantit que l'application ne rejette aucune image `.img` fournie par l'utilisateur et produit toujours un fichier `.img` ou `.zip` flashable et démarrable.

- **Decision 3 : Streaming Disque Pur Anti-OOM (`SignProEngine`) & Sanctuarisation du Framework**
  - *Chosen Approach* : Remplacer tous les tampons mémoire par du streaming disque (`FileChannel` / buffers 64 Ko) dans **Sign Pro** et **Generator**, et injecter toutes les adaptations matérielles via des **Overlays RRO 4K signés** et des **modules Vendor/System_Ext isolés** sans jamais corrompre la signature des APKs système AOSP d'origine.
  - *Why* : Zéro fermeture soudaine (`OutOfMemoryError`) et zéro bootloop lié au `PackageManagerService` ou à `dm-verity`.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                          INTERFACE UTILISATEUR (JETPACK COMPOSE)                       │
│  ┌──────────────────────────┐  ┌────────────────────────────┐  ┌────────────────────┐  │
│  │  UNPACK & SOURCE STUDIO  │  │  CREATION : AI OMEGA-FORGE │  │ EXTRACTME & SIGN   │  │
│  │ • Décodeur Universel IMG │  │ • Auto-Pilot AI Port & Fix │  │ • Root Multi-Su    │  │
│  │ • Code Source AOSP (.java│  │ • Simulateur Boot 5 Stades │  │ • Streaming 4K Zip │  │
│  │   .cpp, .hal, Android.bp)│  │ • Make IMG & Make ROM 100% │  │ • Zéro Crash OOM   │  │
│  └────────────┬─────────────┘  └─────────────┬──────────────┘  └─────────┬──────────┘  │
└───────────────┼──────────────────────────────┼───────────────────────────┼─────────────┘
                ▼                              ▼                           ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│           CERVEAU IA AUTONOME + MOTEUR D'INGÉNIERIE AOSP (KOTLIN + JNI RUST)           │
│                                                                                        │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│  │ 1. GeminiOsArchitectAgent (`gemini-3.1-pro-preview` + Fallback Déterministe)     │  │
│  │    • Analyse croisée Code Source IMG <-> Device Tree Téléphone                   │  │
│  │    • Génération automatique : Shims C/ELF64, SELinux `.cil`, VINTF, RRO, `init`  │  │
│  │    • Auto-correction en boucle fermée avec le Simulateur de Boot à 5 Stades      │  │
│  └────────────────────────────────────────┬─────────────────────────────────────────┘  │
│                                           ▼                                            │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│  │ 2. UniversalImgDecoder & AospSourceDecompilerEngine                              │  │
│  │    • Formats : Sparse, EXT4, EROFS, Super (`lp_metadata`), Boot/Vendor_Boot, LZ4 │  │
│  │    • Décompilation : DEX -> `.java`, ELF64 -> `.cpp`/`.h`, `.hal`/`.aidl`, `.bp` │  │
│  └────────────────────────────────────────┬─────────────────────────────────────────┘  │
│                                           ▼                                            │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│  │ 3. HybridShellEngine v4 & HardwareDnaExtractor (`ROM_FORGE/EXTRACT/`)            │  │
│  │    • Session Root réelle (`su -c`) Magisk/KernelSU/APatch + Lecture `/vendor`    │  │
│  │    • Extraction Device Tree complet (`BoardConfig.mk`, `device.mk`) + Blobs bruts│  │
│  └────────────────────────────────────────┬─────────────────────────────────────────┘  │
│                                           ▼                                            │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│  │ 4. OmegaPortingOrchestrator, PreBootEmulatorEngine & OmegaImgRomBuilder          │  │
│  │    • Greffe 6-Couches + Validation 5 Stades (Init -> SELinux -> VINTF -> Zygote) │  │
│  │    • Compilation `Make IMG` (1:1 & From Scratch EXT4/EROFS + AVB) & `Make ROM`   │  │
│  └──────────────────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### Plan d'Exécution Technique Fichier par Fichier

1. **Configuration Gemini API & Secret Management (`.env.example`, `app/build.gradle.kts`, `GeminiOsArchitectAgent.kt`)** :
   - Activation de `GEMINI_API_KEY` dans `.env.example` (conformément au skill `android-secret-management`) et configuration du client Retrofit/OkHttp (timeouts 60s) pointant sur `gemini-3.1-pro-preview` et `gemini-3.5-flash`.
   - Création de **`GeminiOsArchitectAgent.kt`** : agent IA autonome qui reçoit le rapport de décompilation AOSP de l'image `.img` et l'ADN matériel extrait du téléphone, et produit un `AiPortingBlueprint` JSON structuré (règles SELinux `.cil`, définitions de Shims ELF64, fragments VINTF, ressources XML Overlay RRO, scripts `init.<device>.rc` et correctifs `build.prop`), doublé d'un moteur heuristique hors-ligne complet.
2. **Accès Root Multi-Chemins & ExtractMe ADN Matériel (`HybridShellEngine.kt`, `GastroEngine.kt`, `ExtractorScreen.kt`)** :
   - Sonde et session Root réelle multi-vecteurs (`Magisk`, `KernelSU`, `APatch`, `/system/bin/su`, `/sbin/su`) avec panneau de diagnostic Root en direct.
   - Extraction en direct des partitions nécessaires, des binaires bruts (`.so`, démons HAL) et génération du **Device Tree AOSP complet** (`BoardConfig.mk`, `device.mk`, `proprietary-files.txt`, `fstab`, `init.*.rc`, `vintf`) dans `ROM_FORGE/EXTRACT/`.
3. **Décodeur Universel d'Images `.img` & Désassemblage Code Source AOSP (`AospSourceDecompilerEngine.kt`, `UnpackScreen.kt`)** :
   - Support universel de tout fichier `.img` (`Sparse`, `EXT4`, `EROFS`, `Super`, `Boot/Vendor_Boot`, `LZ4/Gzip`).
   - Génération de l'arborescence **`ROM_FORGE/AOSP_SOURCE_TREE/<nom_img>/`** (`DEX -> .java`, `ELF64 ARM64 -> .cpp/.h`, `.hal/.aidl`, `resources.arsc -> XML AOSP`, `Android.bp` et Graphe d'interdépendances de l'OS) avec un **Explorateur Code Source AOSP** intégré dans l'interface.
4. **Portage Automatique 6-Couches Assisté par IA, Simulateur Pré-Boot & Compilation `Make IMG` / `Make ROM` (`OmegaPortingOrchestrator.kt`, `ImgCompilerEngine.kt`, `CreationScreen.kt`, `SignProEngine.kt`)** :
   - Exécution automatique de la greffe matérielle à 6 couches guidée par `GeminiOsArchitectAgent`.
   - Validation en boucle fermée par le **Simulateur Pré-Boot à 5 Stades** (`First-Stage Init`, `SELinux .cil`, `VINTF`, `Linker64`, `Zygote/ART`) jusqu'à obtenir un score de démarrage de 100 %.
   - Compilation finale via **`Make IMG`** (Chirurgie 1:1 + From Scratch EXT4/EROFS avec `AVB 2.0`) et **`Make ROM`** (Custom ROM `.zip` flashable), avec signature/alignement 4K en **streaming disque pur** dans `SignProEngine.kt` (zéro crash OOM).
