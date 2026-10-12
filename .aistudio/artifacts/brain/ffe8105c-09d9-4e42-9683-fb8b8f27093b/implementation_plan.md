# Plan d'Implémentation Révisé : Extraction Exhaustive, Moteur d'Analyse ELF, Comparateur FOD & Visualisation Graphique

Ce plan détaille l'enrichissement complet du module **EXTRACT** dans l'application **FORGER** :
1. **Extraction exhaustive du téléphone** (tous les HALs, blobs `.so`, configurations, services, kernel, arbre complet) et génération de fichiers JSON ultra-riches dans `/sdcard/FORGER/EXTRACT/`.
2. **Analyseur de dépendances binaires ELF (`DT_NEEDED`)** natif en Kotlin pour cartographier les liens dynamiques de chaque blob et les dépendances Vendor <-> System.
3. **Dissection ultra-poussée du fonctionnement FOD** (Fingerprint on Display) de l'appareil hôte (Hardware, Drivers, Sysfs HBM, HIDL/AIDL, Binder, Keylayout, Coordonnées, Propriétés système, Diagnostic IA).
4. **Sélecteur d'image unpackée** dans l'écran EXTRACT (liste déroulante alimentée par `/sdcard/FORGER/UNPACK/`).
5. **Analyse de l'image unpackée ciblée** exportée dans `/sdcard/FORGER/Data/IMGSTRACT/` (si une image est sélectionnée).
6. **Zone de Synthèse Comparative FOD** avec **composant de visualisation graphique dynamique en Jetpack Compose** (graphique de couverture, matrice de compatibilité des dépendances, mise en évidence des écarts de version et des liens manquants).
7. **Journal d'exécution en direct** hautement détaillé et explicite.

---

## 1. Architecture des Données et Répertoires

### A. Données de l'Appareil Hôte (`/sdcard/FORGER/EXTRACT/`)
- `fod_fingerprint/` : Blobs, HALs, XML de permissions, keylayouts, scripts `.rc`, et dump sysfs de l'illumination HBM de l'hôte.
- `raw_blobs_and_hals/` : Scan et copie dynamique de l'ensemble des bibliothèques (`/vendor/lib/`, `/vendor/lib64/`, `hw/`, `/odm/lib64/`).
- `configs_and_xml/` : Manifestes VINTF, matrices de compatibilité, configurations audio, display, caméras, sepolicy.
- `binaries_and_services/` : Démons exécutables sous `/vendor/bin/`.
- `live_diagnostics/` : Sorties textuelles brutes (`lshal.txt`, `service_list.txt`, `getprop.txt`, `dmesg_fod.txt`, `mounts.txt`, `cmdline.txt`).
- `interdependencies/` : Graphique des dépendances ELF et cartographie des ponts système/vendor.
- `reports_json/` : Fichiers JSON complets et exhaustifs :
  - `device_dna.json` : Fiche d'identité matérielle intégrale, SoC, kernel, build.props système et vendor.
  - `fod_chain.json` : Spécification complète du FOD de l'hôte, chemin IPC, coordonnées X/Y/Rayon, nodes HBM.
  - `hals_and_blobs.json` : Inventaire exhaustif des HALs et bibliothèques copiés.
  - `elf_dependency_graph.json` : Dépendances dynamiques `DT_NEEDED` et symboles pour chaque `.so`.
  - `interdependencies.json` : Analyse des liens et shims nécessaires entre Vendor et System.
  - `vintf_matrix.json` : HALs déclarés et interfaces de compatibilité.
  - `partition_layout.json` : Tailles et types de partitions actives.
  - `live_services_map.json` : Services vivants Binder et HALs.
  - `full_device_tree_map.json` : Arborescence complète scannée du système.
  - `ai_porting_dossier.json` : Synthèse complète d'aide au portage (explication détaillée du FOD et des démarches requises).

### B. Données de l'Image Unpackée (`/sdcard/FORGER/Data/IMGSTRACT/`)
Lorsque l'utilisateur sélectionne une image unpackée dans la liste déroulante :
- `unpacked_image_dna.json` : Props extraites (`build.prop`), structure des partitions, versions API/Android de l'image.
- `unpacked_blobs_inventory.json` : Liste des `.so` et binaires présents dans l'image.
- `unpacked_fod_support.json` : Analyse du support FOD dans l'image (présence ou absence de services UDFPS, overlays, keylayouts, etc.).
- `fod_comparative_audit.json` : Audit différentiel complet (Hôte vs Image unpackée) détaillant pourquoi le FOD fonctionne sur l'hôte et pourquoi il échoue sur l'image unpackée.

---

## 2. Nouveaux Composants et Modules Développés

### A. Moteur d'Analyse des Dépendances Binaires ELF (`ElfDependencyAnalyzer.kt`)
- Parseur binaire ELF (32-bit et 64-bit, Little Endian) natif en Kotlin.
- Extraction des entrées dynamiques `DT_NEEDED` (noms de fichiers `.so` requis).
- Extraction de `DT_SONAME` et identification des bibliothèques système vs vendor vs VNDK.
- Détection des bibliothèques introuvables nécessitant des shims pour le portage.

### B. Moteur d'Inspection d'Image Unpackée (`UnpackedImageInspector.kt`)
- Parcours du dossier d'image unpackée sélectionné dans `/sdcard/FORGER/UNPACK/`.
- Détection de la partition cible (ex: `system`, `vendor`, `product`).
- Extraction des `build.prop`, configurations VINTF, `.rc` d'initialisation, et détection du support FOD de l'image.
- Évaluation des écarts critiques avec le téléphone physique.

### C. Moteur de Comparaison FOD Différentielle (`FodComparativeEngine.kt`)
- Compare point par point :
  1. **Blobs & HALs** : HAL Goodix/FPC/Xiaomi présents sur l'hôte vs absents ou génériques sur l'image.
  2. **Services Init & Daemons** : Présence des services HIDL/AIDL fingerprint dans les `.rc`.
  3. **Overlays UDFPS & Framework** : Vérification des dimensions du capteur, de la géométrie de l'écran et des paramètres de rétroéclairage HBM.
  4. **Keylayout & Saisie** : Vérification du fichier de keylayout (ex: `uinput-goodix.kl`) et du keycode (ex: 338).
  5. **Propriétés Système & Drivers** : Props requises pour déclencher le pont d'affichage (`ro.vendor.display.*`, `vendor.touch.*`).
- Production d'un score de compatibilité (0% à 100%) et de la synthèse explicative détaillée.

### D. Visualisation Graphique Compose dans la Zone de Synthèse (`FodDependencyGraphView.kt`)
- **Composant graphique dynamique en Jetpack Compose Canvas / Material 3** :
  - **Graphique en barres / radar de couverture FOD** : Comparaison visuelle entre l'Hôte (100% fonctionnel) et l'Image unpackée (taux de couverture des blobs, scripts init, props et overlays).
  - **Matrice différentielle interactive (Dependency Breakdown)** :
    - Blobs critiques (ex: `libgf_hal.so`, `vendor.xiaomi.hardware.fingerprintextension@1.0.so`, `biometrics.fingerprint@2.1.so`) : Statut Hôte (Vert) vs Statut Image (Rouge / Mismatch).
    - Scripts Init & Daemons (ex: `android.hardware.biometrics.fingerprint@2.1-service.rc`).
    - Drivers & Nodes HBM (ex: `/sys/class/drm/card0-*/disp_param`).
    - Keylayout (`uinput-goodix.kl` / Keycode 338).
    - Propriétés système requises.
  - **Badge de diagnostic global et explications pas-à-pas** pour résoudre chaque dépendance manquante lors du futur portage.

### E. Mise à Jour de l'Écran EXTRACT (`ExtractScreen.kt`)
- Ajout d'un sélecteur déroulant (`ExposedDropdownMenuBox`) :
  - Option par défaut : *"Aucune (Extraction téléphone uniquement)"*.
  - Liste dynamique de tous les dossiers et images trouvés dans `/sdcard/FORGER/UNPACK/`.
- Bouton unique **"Lancer l'Extraction & Diagnostic"** :
  - Si une image unpackée est choisie : effectue l'extraction hôte complète + l'analyse de l'image unpackée vers `sdcard/FORGER/Data/IMGSTRACT/` + affiche la **Zone de Synthèse Comparative** avec le graphique interactif.
  - Si aucune image n'est choisie : effectue l'extraction hôte complète vers `/sdcard/FORGER/EXTRACT/`.
- Affichage de la **Zone de Synthèse Comparative FOD** conditionnelle, avec onglets/cartes dédiés, graphiques visuels et journalisation enrichie en temps réel.

---

## 3. Plan d'Exécution par Fichier

| Étape | Fichier | Action |
|---|---|---|
| 1 | `app/src/main/java/com/example/core/extract/ElfDependencyAnalyzer.kt` | Créer le parseur binaire ELF Kotlin pour extraire `DT_NEEDED`, architectures et graphes de dépendances. |
| 2 | `app/src/main/java/com/example/core/extract/UnpackedImageInspector.kt` | Créer le moteur d'inspection d'images unpackées (scan de dossiers dans `FORGER/UNPACK`, extraction vers `FORGER/Data/IMGSTRACT`). |
| 3 | `app/src/main/java/com/example/core/extract/FodComparativeEngine.kt` | Créer le moteur d'audit différentiel FOD (Hôte vs Image Unpackée), détection des liens manquants et des écarts de version. |
| 4 | `app/src/main/java/com/example/core/extract/DeviceExtractEngine.kt` | Refondre l'extraction hôte pour parcourir dynamiquement tous les blobs/HALs/services, exécuter les diagnostics live approfondis et intégrer l'inspection d'image optionnelle. |
| 5 | `app/src/main/java/com/example/ui/extract/FodVisualComparisonCard.kt` | Créer le composant de visualisation graphique Compose (graphique de couverture, matrice de comparaison des dépendances avec code couleur Vert/Rouge/Orange). |
| 6 | `app/src/main/java/com/example/ui/extract/ExtractScreen.kt` | Intégrer le menu déroulant des images unpackées, la zone de synthèse comparative FOD et la journalisation en direct détaillée. |
| 7 | `app/src/test/java/com/example/ExampleRobolectricTest.kt` | Ajouter les tests unitaires pour le parseur ELF, l'audit différentiel FOD et la sérialisation des nouveaux rapports. |

---

## 4. Validation & Vérification
1. Validation de la compilation avec `compile_applet`.
2. Exécution des tests unitaires Robolectric (`gradle :app:testDebugUnitTest`).
3. Vérification de la fluidité et de la réactivité de l'interface graphique.
