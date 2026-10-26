# Merianschule App Backend

Modernes, ressourcenschonendes und sicheres Backend für die **Merianschule Seligenstadt App**, entwickelt mit **Kotlin Ktor**, **SQLite** und **Docker**.

## 🚀 Funktionen

- **Sichere Accounts & Authentifizierung:**
  - Registrierung & Login mit E-Mail oder Benutzername.
  - Passwortverschlüsselung mit PBKDF2 (100.000 Runden, individueller Salt).
  - Sichere stateless JWT-Authentifizierung für Android, iOS und Desktop.
  - Benutzerprofil mit Schulklasse, Schulform, Klassenleitung und individuellem Avatar.
- **Geräteübergreifende Aufgaben-Synchronisation (`/api/tasks`):**
  - CRUD-Endpunkte für Aufgaben und benutzerdefinierte Aufgabenlisten.
  - Dedizierter `/api/tasks/sync`-Endpunkt für nahtlose 2-Wege-Synchronisation nach Reconnect.
  - Strikte Benutzer-Isolation (jeder Schüler hat nur Zugriff auf die eigenen Daten).
- **Stundenplan & Vertretungsplan (`/api/substitutions` & `/api/timetable`):**
  - Schneller Abruf des Vertretungsplans (filterbar nach Klasse oder alle Klassen).
  - Stundenplan nach Klassen aufgeschlüsselt.
  - Lehrer-Verzeichnis mit Sprechzeiten und E-Mail-Adressen.
- **Admin-REST-Endpunkt für manuelle JSON-Updates:**
  - Da das Schulportal keine API für Vertretungspläne anbietet, können Pläne direkt per Admin-REST-API (`POST /api/admin/substitutions`) oder Datei (`data/substitutions.json`) gepflegt werden.
  - Geschützt durch Admin-Secret-Key (`X-Admin-Key`) oder Admin-JWT.

---

## 🛠️ Schnellstart

### 1. Lokal starten (Gradle)

```bash
# Aus dem Hauptverzeichnis des Projekts:
./gradlew :server:run
```
Der Server startet standardmäßig auf Port `8080`.

### 2. Mit Docker starten (Self-Hosting auf Server / VPS)

```bash
# Im server-Verzeichnis:
cd server
docker compose up -d --build
```

Container stoppen:
```bash
docker compose down
```

Die SQLite-Datenbank (`merianschule.db`) und die JSON-Dateien bleiben persistent im gemounteten Verzeichnis `server/data/` erhalten.

---

## ⚙️ Umgebungsvariablen

| Variable | Beschreibung | Standardwert |
| :--- | :--- | :--- |
| `PORT` | HTTP-Port des Servers | `8080` |
| `HOST` | Bind-Adresse | `0.0.0.0` |
| `DATA_DIR` | Speicherort für DB & Backups | `data` |
| `DB_PATH` | Pfad zur SQLite-Datenbankdatei | `data/merianschule.db` |
| `ADMIN_SECRET_KEY` | Geheimer Schlüssel für Admin-REST-Endpunkte | `merian-admin-secret-2026` |
| `JWT_SECRET` | Geheimer Schlüssel zur Signierung der JWTs | `merianschule-seligenstadt-secret-key-2026-safe-production` |
| `ADMIN_DEFAULT_PASSWORD`| Initiales Passwort für den `admin`-Account | `MerianAdmin2026!` |

---

## 📡 API-Dokumentation & Beispiele

### 1. Healthcheck
```bash
curl http://localhost:8080/health
```

---

### 2. Vertretungsplan manuell aktualisieren (Admin-Endpunkt)

Du kannst den Vertretungsplan als JSON-Array direkt an den Server senden:

```bash
curl -X POST http://localhost:8080/api/admin/substitutions \
  -H "X-Admin-Key: merian-admin-secret-2026" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "id": "sph_2026-10-22_1",
      "date": "2026-10-22",
      "dayLabel": "Heute, 22. Oktober",
      "period": "2. Std",
      "subject": "Biologie",
      "className": "09AR",
      "type": "Entfall",
      "details": "Tho entfällt ersatzlos",
      "roomInfo": "Entfall (geplant C202) - fällt aus",
      "time": "22.10.2026",
      "isNew": true,
      "isMarked": false
    }
  ]'
```

*Alternativ:* Du kannst auch direkt eine Datei hochladen:
```bash
curl -X POST http://localhost:8080/api/admin/substitutions \
  -H "X-Admin-Key: merian-admin-secret-2026" \
  -H "Content-Type: application/json" \
  --data-binary @substitutions.json
```

---

### 3. Vertretungsplan abrufen (Öffentlich)

```bash
# Alle Einträge
curl http://localhost:8080/api/substitutions

# Gefiltert nach Klasse (z.B. 09AR)
curl "http://localhost:8080/api/substitutions?class=09AR"

# Exakte Roh-JSON-Datei
curl http://localhost:8080/api/substitutions/raw
```

---

### 4. Stundenplan manuell aktualisieren (Admin-Endpunkt)

```bash
curl -X POST http://localhost:8080/api/admin/timetable \
  -H "X-Admin-Key: merian-admin-secret-2026" \
  -H "Content-Type: application/json" \
  -d '{
    "className": "09AR",
    "days": {
      "mon": [
        {
          "id": "mon_1",
          "periodNumber": 1,
          "timeRange": "07:40 - 08:25",
          "subject": "Deutsch",
          "room": "Raum B208",
          "teacherCode": "Af",
          "teacherName": "Fr. Affeldt",
          "teacherEmail": "affeldt@merianschule-seligenstadt.de",
          "breakAfterMinutes": 5,
          "breakLabel": "5 Min. Wechselpause"
        }
      ]
    }
  }'
```

---

### 5. Account & Authentifizierung

#### Registrieren:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "max",
    "email": "max@merianschule.de",
    "password": "SicheresPasswort123!",
    "fullName": "Max Mustermann",
    "className": "09AR",
    "schoolType": "Realschule",
    "classTeacher": "Hr. Thomas"
  }'
```

#### Anmelden:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "max",
    "password": "SicheresPasswort123!"
  }'
```
Antwort:
```json
{
  "token": "eyJhbGciOi...",
  "user": {
    "id": "...",
    "username": "max",
    "email": "max@merianschule.de",
    "className": "09AR"
  }
}
```

---

### 6. Aufgaben synchronisieren

```bash
curl -X POST http://localhost:8080/api/tasks/sync \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "tasks": [
      {
        "id": "task_1",
        "title": "Deutsch Hausaufgaben S. 42",
        "isCompleted": false,
        "listId": "my_tasks"
      }
    ],
    "lists": [
      {
        "id": "my_tasks",
        "title": "Meine Aufgaben"
      }
    ]
  }'
```
