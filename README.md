# 🎓 Music Organizer

A simple desktop music player made using Java. Music Organizer helps you manage and play your own MP3 files through a clean, Spotify-inspired interface. It works offline and does not require an account or streaming service.

## 📌 About the Project

Music Organizer is a Java desktop application for importing, organising and playing local music files. You can manage your music library, create playlists, search and sort songs, and control playback from one place.

## ✨ Features

- ➕ Import one or multiple MP3 files
- 🎵 Browse, search and sort your music library
- 📂 Create, rename and manage playlists
- ▶️ Play, pause, skip and seek through songs
- 🔊 Adjust volume, mute and use repeat modes
- 🔗 Relink songs if their files are moved
- 📊 View basic information and statistics about your library

## 🛠️ Technologies Used

- **Programming Language:** Java
- **GUI:** Java Swing
- **Audio Playback:** JavaFX MediaPlayer
- **Build Tool:** Apache Maven
- **Concepts:** Object-Oriented Programming (OOP), file handling and event handling
- **Data Storage:** Text files

## 📁 Project Structure

```text
src/
├── main/
│   ├── java/com/moodify/
│   │   ├── Main.java
│   │   ├── model/
│   │   ├── data/
│   │   ├── audio/
│   │   └── ui/
│   └── resources/images/
│       └── cover.png
└── test/java/com/moodify/
    ├── PlaylistStoreTest.java
    └── SongLibraryTest.java

run.bat
run.sh
pom.xml
```

## 🚀 How to Run

1. Download or clone this repository.
2. Make sure JDK 17 or newer and Maven 3.8+ are installed.
3. On **Windows**, run `run.bat`.
4. On **Linux or macOS**, run `./run.sh`.
5. The first run needs an internet connection to download Maven dependencies.

You can also build and run it manually:

```bash
mvn package
java -jar target/moodify.jar
```

To run the tests:

```bash
mvn test
```

## 🎯 Project Objective

The main objective of this project is to apply Java and Object-Oriented Programming concepts by building a desktop application. It also helped me work with Java Swing, JavaFX audio playback, file handling and managing data without using a database.

## 💾 Data Storage

The app saves its data in a `data` folder:

- `songs.txt` stores song details and file paths.
- `playlists.txt` stores playlist names and song order.

The app stores the paths to MP3 files instead of copying the files into the project. Base64 is used to encode text fields; it is not encryption.

---

⭐ Thanks for checking out my project!
