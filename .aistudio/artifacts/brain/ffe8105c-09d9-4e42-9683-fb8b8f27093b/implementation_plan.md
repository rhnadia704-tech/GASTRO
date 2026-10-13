# Plan d'Implémentation Révisé : Moteur de Portage Intelligent "PORTER", FOD Fix & Protection Anti-Bootloop 100% Factuelle

## Résumé Exécutif
Ce plan révisé intègre l'ensemble des exigences exprimées par l'utilisateur :
1. **Conservation intégrale et enrichissement de l'EXTRACT existant** : aucune fonctionnalité antérieure n'est retirée. L'extraction est rendue plus intelligente et factuelle à 100% (zéro hallucination, scan exhaustif des HALs biometrics, nœuds HBM/OLED réels, VINTF, propriétés vendor et dépendances ELF).
2. **Moteur intelligent de compréhension et de portage** : corrélation entre les extractions du téléphone et l'inspection de la GSI décompressée (`UNPACK/`) pour résoudre la rupture de communication avec le vendor.
3. **Solution intelligente anti-bootloop sans risque de crash** :
   - Préservation stricte des fichiers et APKs d'origine de la GSI.
   - Système de sauvegarde/snapshot automatique pré-patch et rollback immédiat en un clic.
   - Injection déclarative et modulaire (overlays RRO, fichier d'initialisation dédié `/system/etc/init/init.fod.porter.rc`, propriétés vendor).
   - Validateur statique pré-repack vérifiant les permissions (0644/0755), la syntaxe `init.rc` et les contextes SELinux avant tout repack pour garantir le boot sous DSU Sideloader.
4. **Rubrique PORTER avec menu déroulant et action "FOD Fix"**.

---

## Retours & Réponses Utilisateur Confirmées

> [!IMPORTANT]
> - **Conservation d'EXTRACT** : Tout le code d'extraction existant est conservé et enrichi, sans perte de données.
> - **Stratégie Anti-Bootloop "Solution Intelligente"** : Sauvegarde instantanée pré-patch, injection additive isolée (aucun binaire ni APK écrasé), et restauration en un clic.
> - **Validation Pré-Repack (Choix 1 validé)** : Contrôle statique automatique de l'intégrité de la GSI modifiée (syntaxe init, permissions chmod standard, contextes SELinux) avant de valider l'image prête pour DSU Sideloader.
> - **Factualité 100% (Zéro Hallucination)** : Seuls les fichiers, nœuds réels du téléphone et composants réels de la GSI sont inspectés et cités.

---

## 1. Architecture Globale du Système

```
┌────────────────────────────────────────────────────────────────────────┐
│                        TÉLÉPHONE HÔTE & VENDOR                         │
│  • Scan partitions réelles (/vendor, /system, /apex, /odm)             │
│  • HALs réels (android.hardware.biometrics.fingerprint@2.x / AIDL)     │
│  • Nœuds réels sysfs (HBM, OLED display, FOD dimming, /dev/...)        │
│  • VINTF manifest & compatibility matrix, propriétés persist.*         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Scan Factuel 100% Enrichi
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│              EXTRACT & IMGSTRACT HAUTE-FIDÉLITÉ (FACTUEL)              │
│  • DeviceExtractEngine : Collecte existante conservée + enrichie       │
│  • UnpackedImageInspector : Scan miroir du dossier GSI unpacké         │
│  • BinaryDisassemblyEngine : Dépendances réelles DT_NEEDED & .dynsym   │
│  • FodAiDiagnosticEngine : Prompt factuel strict anti-hallucination    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Analyse croisée
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                  MOTEUR DE PORTAGE INTELLIGENT (PORTER)                │
│  • Sélecteur déroulant de GSI décompressée (dossiers UNPACK/)          │
│  • Bouton majeur "FOD Fix"                                             │
│  • Système Anti-Bootloop Intelligent :                                 │
│    - Snapshot & Sauvegarde automatique pré-patch                       │
│    - Injection modulaire isolée (RRO Overlay + init.fod.porter.rc)     │
│    - Validateur statique pré-repack (SELinux, chmod 0644/0755, syntaxe)│
│    - Option de Rollback instantané en cas de doute                     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Dossier GSI patché et certifié
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                  DOSSIER GSI UNPACKÉ SÉCURISÉ POUR DSU                 │
│  • Démarrage préservé à 100% sans risque de bootloop                   │
│  • Prêt pour repack et installation via DSU Sideloader                 │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Détail des Composants & Améliorations

### A. Enrichissement d'EXTRACT (Zéro régression, 100% Factuel)
- Maintien de tous les modules de collecte existants (`DeviceExtractEngine`, inspection des répertoires, props, VINTF, services binder).
- Enrichissement ciblé pour le FOD :
  - Détection automatique des nœuds d'illumination optique sous `/sys/class/drm/`, `/sys/devices/virtual/`, `/sys/class/backlight/` et `/sys/class/touch/`.
  - Extraction des descripteurs de capteur (dimensions d'écran, coordonnées FOD, surface de l'icône empreinte dans les overlays vendor).
  - Détection de la pile HIDL/AIDL et des bibliothèques d'extensions propriétaires (`vendor.display.config`, `vendor.goodix`, etc.).
  - Remplacement de tout texte simulé par les résultats factuels du shell et des lectures directes.

### B. Moteur Anti-Bootloop Intelligent (`BootSafetyEngine.kt`)
Pour garantir qu'une GSI modifiée ne causera aucun crash au boot :
1. **Snapshot Pré-Patch** : Avant toute modification du dossier GSI, création d'un index d'intégrité et sauvegarde des fichiers touchés dans `.porter_backup/`.
2. **Architecture Additive Isolée** :
   - Aucune modification de fichier APK existant (`services.jar`, `SystemUI.apk` restent intacts).
   - Création de fichiers dédiés étanches :
     - `/system/product/overlay/FodVendorOverlay.apk` (ou déclaration RRO) pour positionner le cercle FOD et le flag HBM sans modifier le code.
     - `/system/etc/init/init.fod.porter.rc` : script d'init indépendant qui règle les permissions des nœuds sysfs (`chmod 0666 /sys/class/drm/...`) et initialise les ponts de compatibilité au démarrage.
     - `/system/etc/prop.default` : ajout non-destructif des propriétés vendor requises.
3. **Validateur Statique Pré-Repack (Vérification Statique Complète)** :
   - Contrôle syntaxique du fichier `.rc` (analyse lexicale des blocs `on init`, `on boot`, `service`).
   - Contrôle des permissions Unix : vérification que les fichiers ajoutés ont les masques 0644 (fichiers système, overlays) ou 0755 (scripts exécutables).
   - Contrôle des contextes SELinux virtuels (`u:object_r:system_file:s0`).
   - Détection d'erreurs de syntaxe dans les fichiers de propriétés.
   - Génération d'un badge de certification : **"Boot Safety Validé : 0 risque détecté"**.
4. **Bouton de Restauration / Rollback** : Possibilité de restaurer le dossier GSI à son état initial exact en un clic.

### C. Rubrique PORTER & Interface UI (`AutoPorterScreen.kt`)
- **Sélecteur déroulant** listant les images décompressées dans `UNPACK/`.
- **Card d'Analyse Intelligente** : Affichage comparatif factuel "Pourquoi ça marche sur le téléphone vs Pourquoi ça bloque sur la GSI".
- **Bouton d'Action "FOD Fix"** : Lancement du processus complet (Snapshot -> Injection -> Vérification statique -> Rapport de certification).
- **Console d'Exécution en Direct** : Télémétrie pas-à-pas montrant chaque action réalisée, permissions appliquées et vérifications réussies.
- **Bouton "Rollback / Annuler le patch"** actif après toute modification.

---

## 3. Plan d'Exécution Fichier par Fichier

1. **`app/src/main/java/com/example/modules/porter/BootSafetyEngine.kt` (Nouveau)** :
   - Moteur de snapshot pré-patch, de contrôle statique (SELinux, permissions Unix 0644/0755, syntaxe rc), et de rollback sécurisé.
2. **`app/src/main/java/com/example/modules/porter/AutoPorterEngine.kt` (Mise à jour)** :
   - Enrichissement du moteur avec la méthode `applyIntelligentFodFix(targetDir, extractData)` utilisant `BootSafetyEngine`.
   - Génération sécurisée des configurations FOD (overlay RRO, init.rc isolé, permissions sysfs).
3. **`app/src/main/java/com/example/core/extract/DeviceExtractEngine.kt` & `FodComparativeEngine.kt` (Mise à jour)** :
   - Préservation de toutes les méthodes d'extraction existantes.
   - Enrichissement des nœuds FOD, HBM, et bibliothèques HIDL/AIDL.
   - Élimination absolue des hallucinations et données fictives.
4. **`app/src/main/java/com/example/core/extract/FodAiDiagnosticEngine.kt` (Mise à jour)** :
   - Renforcement du prompt IA pour une explication factuelle rigoureuse ("Téléphone vs GSI") sans inventer de composants.
5. **`app/src/main/java/com/example/ui/screens/AutoPorterScreen.kt` (Mise à jour)** :
   - Intégration du sélecteur déroulant de GSI unpackée, du bouton "FOD Fix", du validateur statique anti-bootloop et du journal interactif.
6. **Vérification & Compilation** :
   - Validation par `compile_applet` pour garantir l'absence de toute erreur de syntaxe ou de régression.
