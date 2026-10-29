# Tetris en Java (Swing)

## Fichiers

| Fichier | Rôle |
|---|---|
| `Main.java` | Point d'entrée, crée la fenêtre |
| `GamePanel.java` | Boucle de jeu (Timer), clavier, affichage |
| `Board.java` | Grille 10x20, collisions, suppression des lignes |
| `Piece.java` | Pièce en jeu : position, rotation |
| `PieceType.java` | Les 7 tétriminos (formes et couleurs) |
| `ScoreManager.java` | Score, niveau, vitesse |

## Prérequis

Java 17 ou plus récent (JDK, pas seulement le JRE). Vérifier avec :

```
java -version
javac -version
```

La bibliothèque JLayer, nécessaire pour décoder la musique MP3, n'est pas incluse
dans ce dépôt. Pour activer la musique MP3, télécharge `jlayer-1.0.1.jar`
(Maven Central, `javazoom:jlayer:1.0.1`) et place-le dans `lib/`. JLayer est
distribuée sous licence LGPL 2.1. La lecture des fichiers WAV ne nécessite aucune
bibliothèque supplémentaire.

## Compiler et lancer

Dans le dossier du projet :

```
javac -cp "lib/*" *.java
java -cp ".:lib/*" Main
```

Sous Windows, remplacer `:` par `;` dans la commande `java -cp` :

```
java -cp ".;lib/*" Main
```

Les fichiers MP3 et WAV placés dans `assets/` apparaissent dans le menu déroulant
du menu de démarrage. Choisis un morceau avant de sélectionner une difficulté ; il
sera joué en boucle pendant la partie. Ajoute tes propres fichiers audio dans ce
dossier ; ils ne sont pas inclus dans le dépôt. Choisis « Aucune musique » pour jouer
sans son.

Les bruitages des déplacements, rotations, chutes, stockages et lignes complètes
sont générés par le jeu et ne nécessitent aucun fichier supplémentaire.

## Menu de démarrage

Choisis une difficulté en cliquant sur son bouton ou avec les touches 1, 2 ou 3 :

| Difficulté | Niveau de départ |
|---|---:|
| Easy | 1 |
| Medium | 4 |
| Hard | 8 |

## Commandes

| Touche | Action |
|---|---|
| ← / → (ou A / D) | Déplacer |
| ↑ (ou W / X) | Rotation horaire |
| Z | Rotation anti-horaire |
| ↓ (ou S) | Descente rapide |
| Espace | Chute directe |
| C | Stocker ou échanger la pièce (une fois par pièce) |
| P | Pause |
| M | Revenir au menu |
| R | Recommencer |

## Score

- 1 ligne : 100, 2 lignes : 300, 3 lignes : 500, 4 lignes (Tetris) : 800, multiplié par le niveau
- Descente rapide : +1 par case, chute directe : +2 par case
- Un niveau toutes les 5 lignes, la vitesse augmente à chaque niveau
- Le message « Level Up » s'affiche brièvement avec la police fournie dans `megamax-jonathan-too-font/`
