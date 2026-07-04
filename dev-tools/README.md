# dev-tools — outils de validation manuelle

Ce dossier est **hors du reactor Maven principal** et **hors de example-app**.
Il sert uniquement à valider manuellement chaque fonctionnalité de la librairie
avant publication sur Maven Central.

## Contenu

| Chemin | Rôle |
|---|---|
| `pom.xml` | Application Spring Boot standalone (même structure que example-app) |
| `src/…/PlaygroundController.java` | Un endpoint POST par feature isolée |
| `postman/` | Collection Postman v2.1 importable |
| `test-all-features.ps1` | Script de non-régression PowerShell |

## Pré-requis

Avant d'utiliser cet outillage, le starter doit être dans le dépôt Maven local :

```
# Depuis la racine du repo
mvn clean install
```

## Lancer l'application playground

```
cd dev-tools
mvn spring-boot:run
# ou
mvn package -DskipTests && java -jar target/easy-input-filter-dev-tools-0.1.0-SNAPSHOT.jar
```

L'application démarre sur le port **8081** (pour ne pas entrer en conflit avec example-app sur 8080).

## Lancer le script de non-régression

```powershell
# Depuis la racine du repo
.\dev-tools\test-all-features.ps1
```

Le script compile la librairie, démarre le playground, exécute tous les cas
PASS/FAIL et affiche un tableau coloré. Code de sortie 1 si au moins un test échoue.

## Relation avec le repo public

Ce dossier est versionné (`git` le suit) pour garder les outils disponibles
d'une machine à l'autre. Il n'est **pas** publié sur Maven Central et n'est
**pas** référencé dans le `pom.xml` parent.
