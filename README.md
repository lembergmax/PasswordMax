# PasswordMax – Passwortmanager mit grafischer Oberfläche

PasswordMax ist ein einfacher, lokal arbeitender Passwortmanager in Java. Er speichert Zugangsdaten (z.B. für Webseiten, E‑Mail-Konten oder andere Dienste) verschlüsselt auf Ihrem Rechner und bietet eine klassische Desktop-Oberfläche auf Basis von Swing.

Das Projekt eignet sich sowohl für den praktischen Einsatz im kleinen Rahmen als auch als Beispielprojekt, um Aufbau, Persistenz und Verschlüsselung in einer Java-Anwendung zu verstehen.

---

## Verwendung (Kurzanleitung)

1. **Programm starten**  
   - Start über `java -jar …` oder direkt aus der IDE.
   - Das Hauptfenster von `PasswordMaxUI` öffnet sich.

2. **Ersten Eintrag anlegen**  
   - Über den entsprechenden Button oder Menüpunkt einen neuen Eintrag/Account erstellen.  
   - In den Dialogfeldern typischerweise auszufüllen:  
     - Bezeichnung oder Dienst (z.B. „E-Mail“, „GitHub“, „Bankkonto“)
     - Benutzername
     - Passwort
     - Optionale Notiz/URL
   - Speichern Sie den Eintrag.

3. **Einträge verwalten**  
   - Bereits gespeicherte Einträge können über die GUI ausgewählt werden.  
   - Je nach Implementierung stehen Funktionen wie **Bearbeiten**, **Löschen** und ggf. **Suchen/Filtern** zur Verfügung.

4. **Daten speichern und laden**  
   - Die Klassen `AccountManager` und `AccountStorage` sorgen im Hintergrund dafür, dass Ihre Einträge in einer verschlüsselten Datei abgelegt und beim Start wieder geladen werden.  
   - Details zu Pfad und Dateinamen können sich je nach Implementierung unterscheiden (siehe Abschnitt *Konfiguration & Speicherort*).

5. **Passwörter anzeigen/verbergen**  
   - Je nach GUI-Implementierung können Passwörter standardmäßig verborgen sein und auf Wunsch eingeblendet werden.  
   - Achten Sie darauf, Passwörter nur dann einzublenden, wenn niemand mitlesen kann.

---

## Sicherheits-Hinweise

PasswordMax verwendet eigene Kryptokomponenten (`Cryptographer`, `CryptoUtils`), um die gespeicherten Passwörter zu verschlüsseln. Dennoch gelten einige wichtige Hinweise:

- **Lern- und Hobbyprojekt-Charakter**  
  - Die Implementierung der Verschlüsselung ist nicht notwendigerweise durch ein professionelles Sicherheits-Audit geprüft.  
  - Nutzen Sie PasswordMax daher vorzugsweise in einer kontrollierten Umgebung und nicht als einzigen Passwortspeicher für hochkritische Anwendungen.

- **Lokale Speicherung**  
  - Alle Daten werden lokal auf Ihrem Rechner gespeichert.  
  - Es gibt **keinen** Server- oder Cloud-Dienst im Hintergrund.

- **Grenzen der Sicherheit**  
  - PasswordMax kann keinen Schutz gegen Keylogger, Screen-Recorder oder andere Malware bieten.  
  - Bei kompromittiertem Betriebssystem könnten Angreifer eventuell dennoch an Ihre Passwörter gelangen.  
  - Auch Speicherauszüge (Memory Dumps) oder Debugger-Angriffe werden nicht speziell abgewehrt.

Verwenden Sie PasswordMax daher mit gesundem Sicherheitsbewusstsein und ggf. zusätzlich zu anderen etablierten Sicherheitsmechanismen.

---

## Konfiguration & Speicherort der Daten

Die genauen Details können Sie im Code von `AccountStorage` nachlesen, typischerweise gelten:

- **Standard-Speicherort**  
  - Eine Datei im Projekt- oder Benutzerverzeichnis, in der alle Accounts/Einträge verschlüsselt gespeichert werden.

- **Konfigurierbarkeit**  
  - Je nach Implementierung kann der Speicherpfad fest eincodiert oder über Einstellungen/Parameter veränderbar sein.

- **Backup & Restore**  
  - Für ein Backup reicht es in der Regel, die verschlüsselte Datendatei an einen sicheren Ort zu kopieren.  
  - Zur Wiederherstellung ersetzen Sie einfach die aktuelle Datei durch Ihre Sicherungskopie (bei geschlossenem Programm).

---

## Lizenz

Geben Sie hier die verwendete Lizenz an, z.B.:

- MIT  
- Apache License 2.0  
- GPLv3  

und verweisen Sie auf eine entsprechende `LICENSE`-Datei im Projektroot.

> Beispiel (bitte anpassen): "Dieses Projekt steht unter der MIT-Lizenz. Weitere Informationen finden Sie in der Datei `LICENSE`."

---

## Haftungsausschluss

Die Nutzung von PasswordMax erfolgt **auf eigene Gefahr**. Es wird **keine Gewähr** für die Richtigkeit, Vollständigkeit, Sicherheit oder Eignung für einen bestimmten Zweck übernommen.

Insbesondere bei Software, die mit sensiblen Daten wie Passwörtern arbeitet, sollten Sie stets zusätzliche Sicherheitsmaßnahmen ergreifen (z.B. regelmäßige Backups, aktuelle Betriebssystem- und Virenschutz-Updates, vorsichtiger Umgang mit Installationen von Drittsoftware).

