# easy-input-filter — example-app

Application Spring Boot de démonstration montrant easy-input-filter en action sur un
endpoint `/products` typique d'une marketplace.

## Démarrage rapide

**Étape 1** — Depuis la **racine du repo**, installer la librairie dans `~/.m2` :

```
mvn clean install
```

**Étape 2** — Depuis le dossier **`example-app/`**, démarrer l'application :

```
cd example-app
mvn spring-boot:run
```

L'application démarre sur le port **8080**.

> **Note Windows PowerShell** : exécutez les deux étapes séparément.
> Ne les collez pas en un seul bloc — PowerShell les interprèterait en mode
> multi-ligne et lancerait `spring-boot:run` depuis la racine (où le plugin
> n'existe pas).

### Exemple de requête

```
POST http://localhost:8080/products
Content-Type: application/json

{
  "title": "Canape en cuir",
  "description": "Tres bon etat, contactez-moi au 06 12 34 56 78",
  "internalNote": "  <b>VIP</b>   client    fidele  "
}
```

→ **400 Bad Request** : champ `description`, détecteur `phone`.

Renvoyez la requête avec une description propre : vous obtiendrez un **200** et
`internalNote` sera nettoyé en `"VIP client fidele"`.

---

## Manual testing

Les outils de test manuel (PlaygroundController, collection Postman, script PowerShell)
sont dans **`dev-tools/`** à la racine du repo.

### Importer la collection Postman

1. Ouvrez Postman → **Import** → glissez-déposez
   `dev-tools/postman/easy-input-filter-playground.postman_collection.json`.
2. Démarrez le playground (port 8081) :
   ```bash
   # Depuis la racine du repo
   mvn -f dev-tools/pom.xml spring-boot:run
   ```
3. Dans Postman, cliquez **Run collection** (icône ▶ à côté du nom de la collection)
   pour ouvrir le **Collection Runner**.
4. Cliquez **Run easy-input-filter playground** — toutes les assertions s'exécutent
   d'un coup et le résultat PASS/FAIL apparaît ligne par ligne.

### Lancer le script de non-régression PowerShell

```powershell
# Depuis la racine du repo
.\dev-tools\test-all-features.ps1
```

Le script :
- Compile la librairie et le playground
- Démarre automatiquement le playground en arrière-plan
- Exécute tous les cas PASS/FAIL
- Affiche un tableau coloré (vert/rouge) et arrête l'application
- Retourne `exit 1` si au moins un test échoue (intégrable en CI)

### Tester le webhook avec webhook.site

1. Allez sur **https://webhook.site** — une URL unique vous est attribuée
   gratuitement (ex : `https://webhook.site/abc-123…`). Laissez la page ouverte :
   chaque requête entrante apparaît en temps réel sans rien installer.
2. Copiez votre URL unique.
3. Editez `dev-tools/src/main/resources/application.yml` et décommentez :
   ```yaml
   easy-input-filter:
     webhook:
       enabled: true
       url: https://webhook.site/VOTRE-URL-UNIQUE
   ```
4. Redémarrez le playground.
5. Envoyez via Postman la requête **"FAIL — téléphone détecté"** sur
   `/playground/webhook-trigger` (ou n'importe quel endpoint FAIL).
6. Observez le payload JSON apparaître instantanément sur webhook.site :
   champ, détecteur, message, timestamp et nom de l'application.
