Aufgabe & Zielsetzung:
Die ursprüngliche Aufgabenstellung sah lediglich die Erstellung eines statischen Mockups (visueller Entwurf) vor. 
Ich habe dieses Projekt eigenständig zu einem funktionalen Konzept weiterentwickelt, das auf einem konsequenten 
„Offline-First-Ansatz“ basiert. Ziel war es, eine Kalender-Lösung zu entwerfen, die ohne externe Cloud-Anbindungen 
(wie Google Calendar) auskommt, um maximale Datensouveränität zu gewährleisten.

Struktur & Software-Design:
Bei der technischen Konzeption habe ich mich bewusst gegen die Implementierung eines komplexen MVC-Patterns 
(Model-View-Controller) entschieden. Da es sich um ein fokussiertes, schlankes Projekt handelt, wurde zugunsten 
einer direkten und effizienten Struktur auf diese zusätzliche Abstraktionsebene verzichtet, um die Komplexität 
gering zu halten und eine performante Lösung zu realisieren.

Bedienkonzept & Funktionalität:
Das Interface zeichnet sich durch ein haptisches und intuitives Steuerungssystem aus:
Navigation: Die Datumsauswahl erfolgt flexibel über das Mausrad sowie die linke und rechte Maustaste.
Interaktive Steuerung: Ein zentrales Bedienelement (Hebel) öffnet per Interaktion ein Detailfenster für die Terminverwaltung.
Eintragsmanagement: In diesem Fenster können Termine eingesehen, editiert oder neu angelegt werden.
Automatisierte Duplizierung: Das System unterstützt intelligente Wiederholungsregeln für Einträge – wahlweise 
jährlich (Geburtstage), monatlich oder spezifisch für die Arbeitswoche (Mo–Fr).
