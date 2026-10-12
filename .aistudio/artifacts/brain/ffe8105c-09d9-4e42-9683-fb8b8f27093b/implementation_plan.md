# Plan d'implémentation : Module EXTRACT Exhaustif, Accès Root & Arborescence Système Complète pour FORGER

Ce plan intègre l'ensemble des besoins d'extraction pour FORGER : l'accès Root renforcé, la cartographie complète de l'arborescence du système d'exploitation du téléphone hôte pour alimenter la compréhension de l'IA, le diagnostic de la chaîne FOD matérielle (Vendor <-> System) et la génération multi-JSON structurée dans `sdcard/FORGER/EXTRACT/`.

---

## 1. Décisions Confirmées & Nouveaux Ajouts (Révision)

> [!IMPORTANT]
> 1. **Accès Root Actif & Multi-Vecteurs** :
>    - Détection et demande interactive de permissions Root (`su -c id`) supportant Magisk, KernelSU, APatch, `/system/bin/su` et `/system/xbin/su`.
>    - Bouton d'acquisition / test d'accès Root direct et exécution de l'extraction avec privilèges élevés pour franchir les restrictions SELinux de `/vendor` et `/sys`.
>    - Mode dégradé propre (fallback userspace sans blocage) si le téléphone n'est pas rooté.
> 2. **Extraction de l'Arborescence Complète du Téléphone (`full_device_tree_map.json`)** :
>    - Parcours récursif exhaustif de toute la hiérarchie de fichiers de l'appareil (`/`, `/system`, `/vendor`, `/product`, `/system_ext`, `/odm`, `/apex`, `/sys/class/drm`, `/dev/input`, etc.).
>    - Enregistrement dans `sdcard/FORGER/EXTRACT/full_device_tree_map.json` et `filesystem_hierarchy.txt` (structure, tailles, types de nœuds, symlinks, permissions POSIX et contextes SELinux) pour permettre à l'IA d'analyser la cohérence globale de l'OS hôte.
> 3. **Navigation Latérale & Ergonomie** :
>    - Intégration dans la barre de navigation latérale (`NavigationRail`) et dans la barre de navigation inférieure (`NavigationBar`) aux côtés de `UNPACK / REPACK`, `Journal` et `Paramètres`.
> 4. **Bouton Unique « Extraire tout (Système + FOD + IA) »** :
>    - Déclenchement en un clic de l'extraction intégrale, du dump des blobs, du scan FOD, de la cartographie de l'arborescence et du diagnostic IA.
> 5. **Multi-JSON & Données Structurées dans `sdcard/FORGER/EXTRACT/`** :
>    - Production de 9 fichiers JSON spécialisés pour les futurs portages.

---

## 2. Architecture Technique & Flux de Données

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ÉCRAN EXTRACT (1 Clic)                          │
│     [Détection Auto : Modèle, SoC, Android, Kernel, Statut Root]       │
│     [Bouton Optionnel : Demander / Tester l'accès Root (su)]           │
│     [Bouton Principal : Extraire tout & Diagnostic IA Complet]         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│            MOTEUR D'EXTRACTION ROOT & ARBORESCENCE SYSTÈME             │
│ 1. Acquisition Root active (Magisk, KernelSU, APatch, su)              │
│ 2. Scan récursif de l'arborescence complète du téléphone (Tree Walker) │
│ 3. Extraction brute de /system, /vendor, /product, /system_ext, /odm   │
│ 4. Dump des HALs (HIDL/AIDL), Blobs ELF64 (.so), VINTF, SELinux, Init  │
│ 5. Dissection de la chaîne FOD (Capteur -> HAL -> Libs -> HBM -> DRM)  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│               ANALYSEUR D'INTERDÉPENDANCES & IA HYBRIDE                │
│ 1. Cartographie de la communication Vendor <-> System                  │
│ 2. Analyse de l'arborescence complète par l'IA (mécanismes de l'OS)   │
│ 3. Synthèse des exigences de portage (shims .so, VNDK, overlays)       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│               RÉPERTOIRE DE SORTIE : sdcard/FORGER/EXTRACT/            │
│ ├── raw_blobs_and_hals/       (Fichiers bruts .so, HALs extraits)      │
│ ├── configs_and_xml/          (VINTF, init.rc, permissions XML, .kl)   │
│ ├── full_device_tree_map.json (Arborescence complète de l'OS hôte)     │
│ ├── filesystem_hierarchy.txt  (Vue texte de la structure des dossiers) │
│ ├── device_dna.json           (Spécifications techniques & props)      │
│ ├── fod_chain.json            (Chaîne technique complète du FOD)       │
│ ├── hals_and_blobs.json       (Inventaire exhaustif des HALs et Blobs) │
│ ├── vintf_matrix.json         (Compatibilité VINTF & manifestes)       │
│ ├── partition_layout.json     (Schéma des partitions & points mount)   │
│ ├── system_vendor_bridge.json (Interdépendances Vendor <-> System)     │
│ ├── interdependencies.json    (Symboles ELF64, DT_NEEDED, shims requis)│
│ ├── ai_porting_dossier.json   (Diagnostic IA et directives de portage) │
│ └── rapport_diagnostic.md     (Rapport lisible avec synthèse FOD)      │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Détail des Développements par Module

### A. Gestion du Root et Cartographie Système (`HybridShellEngine.kt` & `DeviceExtractEngine.kt`)
- **Acquisition Root** :
  - Méthode `requestAndVerifyRootAccess()` : teste l'accès Root via `su -c id` ou `su 0 id`, détecte le fournisseur (`Magisk`, `KernelSU`, `APatch`), affiche la demande d'autorisation à l'utilisateur et met à jour l'état réactif `isRootAvailable` / `activeRootProviderLabel`.
  - Exécution en lecture seule (`su -c "cp -f ..."` et `su -c "find / ..."`) pour ne jamais altérer le téléphone hôte.
- **Cartographie de l'Arborescence Complète (`scanFullDeviceTree`)** :
  - Parcours méthodique et sécurisé des répertoires clés : `/`, `/system`, `/vendor`, `/product`, `/system_ext`, `/odm`, `/apex`, `/etc`, `/usr`, `/sys/class/drm`, `/dev/input`.
  - Collecte de la hiérarchie complète : chemins relatifs, tailles, droits POSIX, liens symboliques (`symlink -> cible`), types MIME / ELF et contextes SELinux.
  - Enregistrement dans `full_device_tree_map.json` pour que l'IA dispose de l'anatomie exacte du système.

### B. Dissection Approfondie de la Chaîne FOD (`HostSystemAndVendorFodProbe.kt`)
- Extraction de tous les maillons de la chaîne d'empreinte digitale sous l'écran :
  - **Pilote Noyau & Device Node** : `/dev/goodix_fp`, `/dev/xiaomi-touch`, nœuds input (`/dev/input/event*`).
  - **HAL & Démons Propriétaires** : `android.hardware.biometrics.fingerprint@2.1..2.3`, `vendor.xiaomi.hardware.fingerprintextension@1.0`, `vendor.goodix.hardware.biometrics.fingerprint@2.1`, `libgf_hal.so`, `hw/biometrics.fingerprint*.so`.
  - **Contrôle Écran & Éclairage HBM** : Ioctl d'illumination haute luminosité (`disp_param`, `displayfeature@1.0`, nœuds sysfs DRM).
  - **Configuration Keylayout** : `uinput-goodix.kl` (keycode 338 / `KEY_FOD_GESTURE` ou 304).
  - **Framework & Overlays SystemUI** : Détection des coordonnées (`ro.hardware.fp.fod.location.X_Y`, `persist.vendor.sys.fp.fod.*`), rayon du capteur et classes UDFPS.
- Analyse claire expliquant :
  - Pourquoi le capteur fonctionne sur la ROM actuelle.
  - Pourquoi il est souvent inopérant sur une GSI non configurée.
  - Quelles liaisons Vendor <-> System doivent être rétablies lors d'un portage.

### C. Moteur IA & Export Multi-JSON (`GastroEngine.kt` / `GeminiExtractorAgent.kt`)
- Structuration de 9 fichiers JSON normalisés dans `sdcard/FORGER/EXTRACT/` :
  1. `full_device_tree_map.json` : Arborescence complète de l'OS avec nœuds et métadonnées.
  2. `device_dna.json` : Propriétés matérielles, build.prop, kernel, partitions.
  3. `fod_chain.json` : Chaîne complète du capteur FOD (Driver -> HAL -> HBM -> SystemUI).
  4. `hals_and_blobs.json` : Cartographie des HALs et bibliothèques `.so`.
  5. `vintf_matrix.json` : Manifeste VINTF, matrices de compatibilité et VNDK.
  6. `partition_layout.json` : Structure des partitions dynamiques (`super`, `dm-0`...).
  7. `system_vendor_bridge.json` : Communication et interdépendances Vendor <-> System.
  8. `interdependencies.json` : Analyse des symboles manquants et besoins en shims.
  9. `ai_porting_dossier.json` : Synthèse complète du diagnostic par l'IA.

### D. Stockage & Intégration UI (`RomForgeStorageManager.kt`, `RomKitchenViewModel.kt`, `ExtractorScreen.kt`, `MainActivity.kt`)
- **Stockage** : Garantie de création et d'écriture dans `sdcard/FORGER/EXTRACT/`.
- **ViewModel** :
  - Méthode `executeFullExtractAndAiDiagnostic()` orchestrant les étapes avec barre de progression de 0 à 100%.
  - Méthode `requestRootAccess()` pour tester et accorder les droits Root.
  - Streaming des logs en direct dans le Journal (`Console`).
- **Interface Utilisateur Compose** :
  - Onglet **EXTRACT** dans la barre latérale et la navigation inférieure.
  - Carte d'identité matérielle en haut (Téléphone détecté, SoC, Android, Kernel, Statut Root avec badge interactif).
  - Bouton d'action principal « Extraire tout (Système + FOD + IA) ».
  - Cartes de statistiques en direct (fichiers extraits, arborescence analysée, chaîne FOD validée, JSON générés).
  - Bouton d'accès direct au Journal pour visualiser le déroulement pas-à-pas.

---

## 4. Stratégie de Vérification

1. **Compilation** : Vérification immédiate du code via `compile_applet`.
2. **Cycle Complet en 1 Clic** : Test du bouton d'extraction complète (détection, dump arborescence, FOD, multi-JSON, synthèse IA).
3. **Contrôle des Fichiers dans `sdcard/FORGER/EXTRACT/`** : Vérification de la présence des fichiers bruts et des 9 fichiers JSON structurés.
4. **Validation de l'UI** : Vérification de la réactivité des boutons, de la navigation latérale et de l'affichage en temps réel dans le Journal.
