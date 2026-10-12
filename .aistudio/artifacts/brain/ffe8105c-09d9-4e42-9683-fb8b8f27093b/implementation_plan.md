# Plan d'Implémentation : EXTRACT Intelligent & Universel (Tout Appareil & Toute GSI) avec Désassemblage Binaire & IA Explicative

## 1. Vision & Principes Directeurs

1. **Préservation Totale de l'Existant** :
   * Conserver à 100% les fonctionnalités déjà opérationnelles d'**EXTRACT** : arborescence complète (`full_device_tree_map.json`), détection Superuser (`su`), analyseur ELF des dépendances (`DT_NEEDED`), extraction des blobs `/vendor`, journaux détaillés en temps réel, et stockage dans `/sdcard/FORGER/EXTRACT`.

2. **Universalité & Intelligence Agnostique (Tout Appareil, Tout SoC, Toute GSI)** :
   * Ne pas limiter l'extraction ou le diagnostic à un seul modèle : le moteur détecte dynamiquement les caractéristiques matérielles de **n'importe quel smartphone** (SoC Qualcomm Snapdragon, MediaTek Dimensity/Helio, Samsung Exynos, Google Tensor, Unisoc) et le type de capteur biométrique (Optique sous l'écran, Ultrasonique 3D Sonic, ou capacitif latéral/arrière).
   * Compatible avec **n'importe quelle GSI** (Android 11 à 16+, AOSP, LineageOS, PixelOS, crDroid, etc.) unpackée dans `FORGER/Data/IMGSTRACT` ou présente dans les répertoires UNPACK.

3. **Complétude Système + Vendor** :
   * Puisqu'une GSI réutilise le vendor du téléphone hôte tout en remplaçant la partition `/system`, l'outil extrait désormais **aussi les composants clés du `/system` hôte** (framework, `services.jar`, `SystemUI`, `lib64`, interfaces AIDL/HIDL, `plat_file_contexts`, overlays).

4. **Désassemblage & Comparatif Binaire Approfondi** :
   * Désassemblage et inspection du bytecode Dalvik/ART (DEX) des deux systèmes pour cartographier les classes et méthodes d'intégration matérielle.
   * Extraction des tables de symboles dynamiques ELF (`.dynsym`, `.symtab`, exports/imports JNI) des bibliothèques système et vendor.

5. **IA Explicative Enrichie** :
   * Analyse didactique expliquant pas-à-pas **pourquoi le capteur fonctionne sur le téléphone actuel** (chaîne continue Driver ➔ Vendor HAL ➔ Extension constructeur ➔ Hooks services.jar ➔ Couche affichage / HBM) et **pourquoi il échoue sur la GSI** (rupture des extensions propriétaires AOSP, absence d'illumination optique ou d'accès binder/SELinux), avec des recommandations de portage universelles.

---

## 2. Architecture Technique Détaillée

### A. Moteur d'Extraction Universel (`DeviceExtractEngine.kt`)
* **Détection Dynamique de l'Écosystème Matériel** :
  * Détection du fabricant (`ro.product.manufacturer`, `ro.product.vendor.manufacturer`) : Xiaomi, Samsung, OnePlus, Oppo, Vivo, Motorola, Realme, Asus, Google, etc.
  * Détection du type de capteur et des extensions constructeur :
    * Xiaomi/Redmi : `vendor.xiaomi.hardware.fingerprintextension`, Goodix/Focaltech optical.
    * Samsung : `vendor.samsung.hardware.biometrics.fingerprint`, Ultrasonic/Optical.
    * OnePlus/Oppo/Realme : `vendor.oplus.hardware.biometrics.fingerprint`.
    * Asus/Motorola : HALs Goodix/Egis/FPC.
* **Extraction Système Complète (`SYSTEM`)** :
  * Fichiers DEX/Java clés : `/system/framework/services.jar`, `/system/framework/framework.jar`, `/system/priv-app/SystemUI/SystemUI.apk`.
  * Bibliothèques système : `/system/lib64/libandroid_runtime.so`, `/system/lib64/libbiometrics_fingerprint.so`, `/system/lib64/libhidlbase.so`, `/system/lib64/libbinder.so`.
  * Matrices VINTF et règles SELinux : `/system/etc/vintf/`, `/system/etc/selinux/plat_file_contexts`.
  * Overlays d'affichage et de configuration : `/system/product/overlay/`, `/system/vendor/overlay/`.
* Organisation dans `/sdcard/FORGER/EXTRACT/` :
  * `full_device_tree_map.json` (arborescence intégrale existante préservée)
  * `extracted_system_inventory.json` (catalogue complet des fichiers système)
  * `SYSTEM/` et `VENDOR/` avec les binaires et fichiers de configuration.

### B. Moteur de Désassemblage Universel DEX & ELF (`BinaryDisassemblyEngine.kt`)
* **Parseur de Bytecode DEX Dalvik/ART natif Kotlin** :
  * Lecture des en-têtes DEX (`dex\n035`, `dex\n037`, `dex\n038`, `dex\n039`).
  * Extraction et filtrage intelligent des définitions de classes (`class_def_item`) et prototypes de méthodes (`method_id_item`).
  * Scan des paquetages biométriques universels :
    * Standard AOSP : `android.hardware.biometrics.*`, `com.android.server.biometrics.*`.
    * Extensions constructeurs : `vendor.*.hardware.biometrics.*`, `vendor.*.hardware.fingerprintextension.*`.
    * Couche UI/Keyguard : `com.android.systemui.biometrics.*`.
* **Parseur de Symboles ELF & JNI** :
  * Lecture des segments dynamiques ELF (format ELF32 et ELF64, Little Endian).
  * Extraction des tables de chaînes (`.dynstr`) et symboles (`.dynsym`) : fonctions exportées, fonctions importées non résolues (UNDEF), et passerelles JNI (`Java_*`).
* **Comparateur GSI vs Système Téléphone** :
  * Analyse différentielle identifiant :
    1. Classes et méthodes présentes dans le téléphone hôte mais absentes de la GSI.
    2. Symboles JNI et dépendances natives absentes ou en conflit de version.
    3. Interfaces AIDL/HIDL manquantes.

### C. Moteur d'Analyse IA Approfondie (`FodAiDiagnosticEngine.kt`)
* Analyse automatisée générant un diagnostic structuré multi-niveaux pour n'importe quel combo Appareil / GSI :
  1. **Autopsie Téléphone Hôte (Pourquoi ça fonctionne)** :
     * Explication claire de la chaîne matérielle et logicielle spécifique au constructeur détecté.
     * Rôle des classes propriétaires et du contrôle de la dalle d'affichage (HBM, dimlayer).
  2. **Diagnostic GSI (Pourquoi ça ne fonctionne pas sur la GSI)** :
     * Identification exacte de la rupture (ex: GSI AOSP utilise `Fingerprint21` générique et n'appelle pas l'extension vendor, ou absence de gestion de la lumière optique).
     * Cartographie des classes et symboles manquants trouvés par le désassembleur.
  3. **Plan de Remédiation Universel (Comment faire fonctionner le portage)** :
     * Overlays RRO universels pour Treble / PHH (ex: coordonnées FOD, seuil de luminosité, nœud sysfs HBM).
     * Shims de compatibilité natifs pour les fonctions C++ manquantes.
     * Règles de redirection Binder et correctifs de permissions SELinux.
* Enregistrement dans `/sdcard/FORGER/Data/IMGSTRACT/universal_ai_porting_diagnostic.json`.

### D. Interface Graphique Interactive dans la Carte FOD (`FodVisualComparisonCard.kt`)
* Panneau interactif dépliable sous le graphique Recharts/Canvas :
  * **Sélecteur de Mode** : Vue synthétique / Inspection binaire détaillée.
  * **Bloc Explicatif Didactique "Pourquoi ça marche / Pourquoi ça échoue"** avec badges visuels clairs.
  * **Explorateur de Désassemblage Dépliable** : Liste filtrable des classes DEX et symboles ELF (Filtre par "Écarts uniquement", "Biométrie/FOD", "Tous").
  * **Bouton d'Exportation** : Possibilité de copier le rapport technique ou de l'exporter en JSON/Markdown.

---

## 3. Plan d'Exécution par Étapes

1. **Développement du moteur de désassemblage universel `BinaryDisassemblyEngine.kt`** :
   * Parseur DEX Kotlin léger et rapide (extraction classes, méthodes et interfaces).
   * Extension du parseur ELF pour extraire les symboles dynamiques `.dynsym` et tables JNI.
2. **Mise à jour de `DeviceExtractEngine.kt`** :
   * Intégration de l'extraction de `/system` (framework, services, SystemUI, lib64, configs) tout en préservant 100% de la logique d'extraction `/vendor` existante.
   * Détection dynamique des caractéristiques du téléphone hôte et du format de la GSI sélectionnée.
3. **Création du moteur IA explicatif `FodAiDiagnosticEngine.kt`** :
   * Génération de l'autopsie détaillée et des recommandations de portage universelles.
4. **Mise à jour de `RomKitchenViewModel.kt`** :
   * Liaison entre les résultats de désassemblage, l'analyse IA enrichie et l'état de l'UI.
5. **Enrichissement de `FodVisualComparisonCard.kt` et `ExtractorScreen.kt`** :
   * Intégration du panneau dépliable d'inspection binaire et de l'explication IA pas-à-pas.
6. **Vérification & Compilation** :
   * Tests unitaires Robolectric (`gradle :app:testDebugUnitTest`).
   * Vérification de compilation finale (`compile_applet`).
