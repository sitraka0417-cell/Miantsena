# Miantsena

Application Android de liste de courses (design 3D) avec connexion Google et synchronisation automatique (Firebase).

## Mise en place Firebase (une seule fois)
1. Console Firebase → **Authentication → Sign-in method → Google** : activer.
2. **Paramètres du projet → Vos applications → Android** : package `mg.miantsena`, et ajouter les empreintes :
   - SHA-1 : `11:EB:3E:8B:5E:FB:4C:A2:8D:0A:A1:A2:86:25:53:1A:FF:40:F8:CD`
   - SHA-256 : `A6:E6:29:B2:97:90:EE:6E:10:28:E9:F7:02:08:3C:03:8C:A3:BD:BE:B7:69:F8:E9:F1:9A:3F:32:A9:17:E3:51`
3. Télécharger `google-services.json` et le placer dans `app/` (puis commit/push).
4. **Firestore Database** → créer la base, onglet **Règles** → coller le contenu de `firestore.rules` → Publier.
5. Autoriser un compte : Firestore → collection `autorises` → ajouter un document dont l'**ID = l'e-mail en minuscules** (ex. `quelquun@gmail.com`). Supprimer le document = retirer l'accès.

## Compilation
Push sur `main` : le workflow `.github/workflows/build.yml` compile l'APK (Actions → Build APK → Artifacts → Miantsena-APK).
`app/debug.keystore` est une clé fixe, pour que le SHA-1 ne change jamais.
