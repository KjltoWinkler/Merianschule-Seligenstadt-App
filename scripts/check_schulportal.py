#!/usr/bin/env python3
"""
Schnelltest-Skript für den Vertretungsplan des Schulportal Hessen (SPH / LANIS).
Schule: Merianschule Seligenstadt (Schulnummer: 4322)
"""

import getpass
import json
import re
import sys
from html.parser import HTMLParser
from datetime import datetime
import requests


class VTableParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.tables = {}
        self.current_table_id = None
        self.in_thead = False
        self.in_tbody = False
        self.in_th = False
        self.in_td = False
        self.current_row = []
        self.current_headers = []
        self.current_cell_text = ""
        self.current_cell_attrs = {}

    def handle_starttag(self, tag, attrs):
        attr_dict = dict(attrs)
        if tag == "table":
            tid = attr_dict.get("id", "")
            if tid.startswith("vtable"):
                self.current_table_id = tid
                self.tables[tid] = {"headers": [], "rows": []}
        elif self.current_table_id:
            if tag == "thead":
                self.in_thead = True
                self.current_headers = []
            elif tag == "tbody":
                self.in_tbody = True
            elif tag == "tr":
                self.current_row = []
            elif tag == "th":
                self.in_th = True
                self.current_cell_text = ""
                self.current_cell_attrs = attr_dict
            elif tag == "td":
                self.in_td = True
                self.current_cell_text = ""
                self.current_cell_attrs = attr_dict

    def handle_endtag(self, tag):
        if self.current_table_id:
            if tag == "th" and self.in_th:
                header_name = self.current_cell_attrs.get("data-field") or self.current_cell_text.strip()
                self.current_headers.append(header_name)
                self.in_th = False
            elif tag == "td" and self.in_td:
                if "colspan" not in self.current_cell_attrs:
                    self.current_row.append(self.current_cell_text.strip())
                self.in_td = False
            elif tag == "tr":
                if self.in_tbody and self.current_row:
                    headers = self.tables[self.current_table_id]["headers"]
                    row_dict = {}
                    for i, val in enumerate(self.current_row):
                        key = headers[i] if i < len(headers) else f"col_{i}"
                        row_dict[key] = val
                    self.tables[self.current_table_id]["rows"].append(row_dict)
            elif tag == "thead":
                self.tables[self.current_table_id]["headers"] = self.current_headers
                self.in_thead = False
            elif tag == "tbody":
                self.in_tbody = False
            elif tag == "table":
                self.current_table_id = None

    def handle_data(self, data):
        if self.in_th or self.in_td:
            self.current_cell_text += data


def login_and_fetch_vplan(school_id: str, username: str, password: str):
    session = requests.Session()
    session.headers.update({
        "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept-Language": "de-DE,de;q=0.9,en;q=0.8"
    })

    print(f"\n[1/4] Sende Anmeldedaten an Schulportal SSO (Schule {school_id})...")
    
    login_url = f"https://login.schulportal.hessen.de/?i={school_id}"
    clean_username = username.strip()
    if clean_username.startswith(f"{school_id}."):
        full_user = clean_username
        user2 = clean_username[len(school_id) + 1:]
    else:
        full_user = f"{school_id}.{clean_username}"
        user2 = clean_username

    post_data = {
        "url": "",
        "timezone": "",
        "skin": "sp",
        "user": full_user,
        "user2": user2,
        "password": password
    }

    try:
        r1 = session.post(login_url, data=post_data, allow_redirects=False)
    except Exception as e:
        print(f"[-] Verbindungsfehler bei Schritt 1: {e}")
        return False

    if r1.status_code != 302:
        print(f"\n[-] Kein Redirect von login.schulportal erhalten (HTTP {r1.status_code}).")
        
        lock_match = re.search(r'id=[\"\']authErrorLocktime[\"\'][^>]*>(\d+)<', r1.text)
        if lock_match:
            seconds = lock_match.group(1)
            print(f"[!] Temporäre Anmeldesperre aktiv: Bitte warte noch {seconds} Sekunden vor dem nächsten Versuch!")
            return False

        if "falsch" in r1.text.lower() or "ungültig" in r1.text.lower() or "benutzername" in r1.text.lower():
            print("[-] Benutzername oder Passwort ungültig.")
        else:
            err_match = re.search(r'class="alert alert-danger[^>]*>(.*?)</div>', r1.text, re.DOTALL)
            if err_match:
                clean_err = re.sub(r'<[^>]+>', '', err_match.group(1)).strip()
                print(f"[-] Meldung vom Schulportal: {clean_err}")
            else:
                print("[-] Bitte prüfe Benutzername und Passwort.")
        return False

    print(f"[+] Schritt 1 erfolgreich (HTTP {r1.status_code}).")

    # 2. Handshake über connect.schulportal.hessen.de
    print("[2/4] Führe SSO-Handshake über connect.schulportal.hessen.de durch...")
    try:
        r2 = session.get("https://connect.schulportal.hessen.de", allow_redirects=False)
    except Exception as e:
        print(f"[-] Verbindungsfehler bei Schritt 2: {e}")
        return False

    final_portal_url = r2.headers.get("Location") or r2.headers.get("location")
    if not final_portal_url:
        final_portal_url = "https://start.schulportal.hessen.de/"

    print(f"[+] Schritt 2 erfolgreich. Ziel-Portal: {final_portal_url}")

    # 3. Session im Hauptportal aktivieren
    print("[3/4] Aktiviere Portal-Session...")
    try:
        session.get(final_portal_url, allow_redirects=True)
    except Exception as e:
        print(f"[-] Verbindungsfehler bei Schritt 3: {e}")
        return False

    print("[+] Portal-Session aktiv!")

    # 4. Vertretungsplan-Hauptseite abrufen
    print("[4/4] Lade Vertretungsplan (vertretungsplan.php)...")
    vplan_url = "https://start.schulportal.hessen.de/vertretungsplan.php"
    vplan_page = session.get(vplan_url)

    if vplan_page.status_code != 200:
        print(f"[-] Fehler beim Laden von vertretungsplan.php (HTTP {vplan_page.status_code})")
        return False

    # HTML Tabellen parsen
    parser = VTableParser()
    parser.feed(vplan_page.text)

    all_days_data = {}
    total_entries = 0

    for table_id, data in parser.tables.items():
        # table_id ist z.B. "vtable22_10_2026"
        date_match = re.search(r'vtable(\d{2})_(\d{2})_(\d{4})', table_id)
        if date_match:
            formatted_date = f"{date_match.group(1)}.{date_match.group(2)}.{date_match.group(3)}"
        else:
            formatted_date = table_id

        rows = data["rows"]
        all_days_data[formatted_date] = rows
        total_entries += len(rows)

    print(f"\n{'='*60}")
    print(f"ERFOLG: Insgesamt {total_entries} Vertretungseinträge über {len(all_days_data)} Tag(e) gefunden!")
    print(f"{'='*60}\n")

    for date_str, rows in all_days_data.items():
        print(f"\n📅 Tag: {date_str} ({len(rows)} Vertretungseinträge):")
        print("-" * 55)
        for i, row in enumerate(rows, start=1):
            stunde = row.get("Stunde", "?")
            klasse = row.get("Klasse", "") or row.get("Klasse_alt", "-")
            fach = row.get("Fach", "") or row.get("Fach_alt", "-")
            vertreter = row.get("Vertretung", "") or row.get("Vertreter", "-")
            lehrer = row.get("Lehrkraft", "") or row.get("Lehrer", "-")
            raum = row.get("Raum", "-")
            hinweis = row.get("Hinweis", "")
            hinweis2 = row.get("Hinweis2", "")
            full_hinweis = f"{hinweis} {hinweis2}".strip()

            print(f"  [{i:02d}] Std: {stunde} | Klasse: {klasse} | Fach: {fach} | Raum: {raum}")
            if vertreter and vertreter != "-":
                print(f"       Vertretung: {vertreter} (statt {lehrer})")
            else:
                print(f"       Lehrkraft: {lehrer}")
            if full_hinweis:
                print(f"       Hinweis: {full_hinweis}")
            print("  " + "." * 50)

    # Als JSON speichern für deine Android App
    import os
    script_dir = os.path.dirname(os.path.abspath(__file__))
    out_file = os.path.join(script_dir, "vplan_output.json")
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(all_days_data, f, ensure_ascii=False, indent=2)
    print(f"\n[+] Komplette Daten als JSON gespeichert in:\n    {out_file}")

    return True


def main():
    print("=" * 60)
    print(" Schulportal Hessen - Vertretungsplan Schnelltest")
    print("=" * 60)

    default_school_id = "4322"
    school_id_input = input(f"Schulnummer [{default_school_id}]: ").strip()
    school_id = school_id_input if school_id_input else default_school_id

    username = input("Benutzername (z.B. vorname.nachname): ").strip()
    if not username:
        print("[-] Benutzername darf nicht leer sein.")
        sys.exit(1)

    password = getpass.getpass("Passwort (Eingabe wird nicht angezeigt): ").strip()
    if not password:
        print("[-] Passwort darf nicht leer sein.")
        sys.exit(1)

    login_and_fetch_vplan(school_id, username, password)


if __name__ == "__main__":
    main()
