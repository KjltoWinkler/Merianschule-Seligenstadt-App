package de.kjltowinkler.msapp.server.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.adminWebPanel() {
    route("/admin") {
        get {
            call.respondText(ADMIN_PANEL_HTML, ContentType.Text.Html)
        }
    }
}

private const val ADMIN_PANEL_HTML = """<!DOCTYPE html>
<html lang="de">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Merianschule - Admin Web-Panel</title>

  <!-- Google Fonts: Roboto & Material Symbols -->
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;500;700&family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,400,0,0&display=swap" rel="stylesheet">

  <!-- Material 3 Web Components (Official Google M3 Web Components) -->
  <script type="module" src="https://cdn.jsdelivr.net/npm/@material/web/all.js/+esm"></script>

  <style>
    :root {
      /* Material 3 Dark Color System Tokens */
      --md-sys-color-primary: #a8c7fa;
      --md-sys-color-on-primary: #062e6f;
      --md-sys-color-primary-container: #1b4587;
      --md-sys-color-on-primary-container: #d3e3fd;
      --md-sys-color-secondary: #c2c7cf;
      --md-sys-color-on-secondary: #2c3137;
      --md-sys-color-surface: #111318;
      --md-sys-color-surface-dim: #0b0d11;
      --md-sys-color-surface-container-lowest: #0c0e12;
      --md-sys-color-surface-container-low: #171a20;
      --md-sys-color-surface-container: #1b1e25;
      --md-sys-color-surface-container-high: #252830;
      --md-sys-color-surface-container-highest: #30333c;
      --md-sys-color-on-surface: #e2e2e8;
      --md-sys-color-on-surface-variant: #c4c7d0;
      --md-sys-color-outline: #8e909a;
      --md-sys-color-outline-variant: #44474f;
      --md-sys-color-error: #ffb4ab;
      --md-sys-color-on-error: #690005;
      --md-sys-color-error-container: #93000a;
      --md-sys-color-on-error-container: #ffdad6;
      --md-sys-typescale-body-large-font: 'Roboto', sans-serif;
      --md-sys-typescale-title-large-font: 'Roboto', sans-serif;
      --md-sys-typescale-headline-small-font: 'Roboto', sans-serif;
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }

    body {
      font-family: 'Roboto', sans-serif;
      background-color: var(--md-sys-color-surface);
      color: var(--md-sys-color-on-surface);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }

    .top-app-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 64px;
      padding: 0 24px;
      background-color: var(--md-sys-color-surface-container);
      border-bottom: 1px solid var(--md-sys-color-outline-variant);
      position: sticky;
      top: 0;
      z-index: 10;
    }

    .logo-container {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .logo-badge {
      width: 38px;
      height: 38px;
      border-radius: 10px;
      background: linear-gradient(135deg, #1b65a4, #395a3c);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-weight: 700;
      font-size: 18px;
    }

    .app-title {
      font-size: 20px;
      font-weight: 500;
      color: var(--md-sys-color-on-surface);
    }

    .app-subtitle {
      font-size: 13px;
      color: var(--md-sys-color-on-surface-variant);
    }

    .main-container {
      max-width: 1200px;
      width: 100%;
      margin: 0 auto;
      padding: 24px;
      flex: 1;
    }

    /* Cards */
    .m3-card {
      background-color: var(--md-sys-color-surface-container);
      border-radius: 16px;
      padding: 24px;
      border: 1px solid var(--md-sys-color-outline-variant);
      margin-bottom: 24px;
    }

    /* Login Box */
    .login-wrapper {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: calc(100vh - 120px);
    }

    .login-card {
      width: 100%;
      max-width: 440px;
      background-color: var(--md-sys-color-surface-container-high);
      border-radius: 20px;
      padding: 32px;
      border: 1px solid var(--md-sys-color-outline-variant);
      box-shadow: 0 10px 30px rgba(0,0,0,0.5);
    }

    .login-card h2 {
      font-size: 24px;
      margin-bottom: 8px;
    }

    .login-card p {
      font-size: 14px;
      color: var(--md-sys-color-on-surface-variant);
      margin-bottom: 24px;
    }

    /* Form Grids */
    .form-group {
      display: flex;
      flex-direction: column;
      gap: 16px;
      margin-bottom: 20px;
    }

    .grid-2 {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    @media (max-width: 600px) {
      .grid-2 {
        grid-template-columns: 1fr;
      }
    }

    /* User Table / List */
    .toolbar-row {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 20px;
    }

    .search-field {
      flex: 1;
      min-width: 260px;
    }

    .stats-chips {
      display: flex;
      gap: 12px;
      margin-bottom: 20px;
      flex-wrap: wrap;
    }

    .stat-chip {
      background-color: var(--md-sys-color-surface-container-highest);
      padding: 8px 16px;
      border-radius: 12px;
      font-size: 13px;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .stat-chip strong {
      color: var(--md-sys-color-primary);
      font-size: 15px;
    }

    .user-table-wrap {
      overflow-x: auto;
      border-radius: 12px;
      border: 1px solid var(--md-sys-color-outline-variant);
    }

    table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
    }

    th {
      background-color: var(--md-sys-color-surface-container-high);
      color: var(--md-sys-color-on-surface-variant);
      font-size: 13px;
      font-weight: 500;
      padding: 14px 16px;
      border-bottom: 1px solid var(--md-sys-color-outline-variant);
    }

    td {
      padding: 14px 16px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.05);
      font-size: 14px;
      vertical-align: middle;
    }

    tr:hover td {
      background-color: rgba(255, 255, 255, 0.02);
    }

    .user-name-cell {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .avatar-circle {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-weight: 500;
      font-size: 14px;
      flex-shrink: 0;
    }

    .badge-class {
      display: inline-block;
      padding: 3px 8px;
      border-radius: 6px;
      background-color: var(--md-sys-color-surface-container-highest);
      font-size: 12px;
      font-weight: 500;
    }

    .badge-role {
      display: inline-block;
      padding: 3px 8px;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.5px;
    }

    .badge-admin {
      background-color: rgba(168, 199, 250, 0.2);
      color: #a8c7fa;
      border: 1px solid rgba(168, 199, 250, 0.4);
    }

    .badge-user {
      background-color: rgba(255, 255, 255, 0.08);
      color: var(--md-sys-color-on-surface-variant);
    }

    .actions-cell {
      display: flex;
      gap: 4px;
      justify-content: flex-end;
    }

    /* Toast Notification */
    .toast {
      position: fixed;
      bottom: 24px;
      right: 24px;
      padding: 14px 20px;
      background-color: var(--md-sys-color-surface-container-highest);
      border-radius: 10px;
      border: 1px solid var(--md-sys-color-outline-variant);
      box-shadow: 0 4px 16px rgba(0,0,0,0.6);
      display: none;
      align-items: center;
      gap: 12px;
      z-index: 100;
      font-size: 14px;
    }

    .toast.success {
      border-left: 4px solid #a8c7fa;
    }

    .toast.error {
      border-left: 4px solid #ffb4ab;
      color: #ffb4ab;
    }

    .tab-content {
      margin-top: 24px;
    }

    .hidden {
      display: none !important;
    }
  </style>
</head>
<body>

  <!-- Top App Bar -->
  <header class="top-app-bar">
    <div class="logo-container">
      <div class="logo-badge">M</div>
      <div>
        <div class="app-title">Merianschule Seligenstadt</div>
        <div class="app-subtitle">Admin Web-Panel</div>
      </div>
    </div>
    <div id="authActions" class="hidden">
      <md-text-button id="btnLogout">
        <md-icon slot="icon">logout</md-icon>
        Abmelden
      </md-text-button>
    </div>
  </header>

  <!-- Login View -->
  <div id="loginView" class="login-wrapper">
    <div class="login-card">
      <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 12px;">
        <span class="material-symbols-outlined" style="font-size: 32px; color: var(--md-sys-color-primary);">shield_person</span>
        <h2>Admin-Anmeldung</h2>
      </div>
      <p>Melde dich mit deinem Administrator-Konto oder dem Admin-Secret-Key an.</p>

      <form id="loginForm" class="form-group" onsubmit="handleLogin(event)">
        <md-outlined-text-field id="loginIdentifier" label="Benutzername oder E-Mail" required value="admin">
          <md-icon slot="leading-icon">person</md-icon>
        </md-outlined-text-field>

        <md-outlined-text-field id="loginPassword" label="Passwort" type="password" required>
          <md-icon slot="leading-icon">key</md-icon>
        </md-outlined-text-field>

        <div style="margin-top: 10px;">
          <md-filled-button type="submit" style="width: 100%;">
            <md-icon slot="icon">login</md-icon>
            Anmelden
          </md-filled-button>
        </div>
      </form>
    </div>
  </div>

  <!-- Dashboard View (Protected) -->
  <main id="dashboardView" class="main-container hidden">

    <!-- Tabs Navigation -->
    <md-tabs id="mainTabs" active-tab-index="0">
      <md-primary-tab id="tabUsers">
        <md-icon slot="icon">group</md-icon>
        Nutzerverwaltung
      </md-primary-tab>
      <md-primary-tab id="tabSystem">
        <md-icon slot="icon">dns</md-icon>
        Server & Status
      </md-primary-tab>
      <md-primary-tab id="tabSchulportal">
        <md-icon slot="icon">sync</md-icon>
        Schulportal Sync
      </md-primary-tab>
    </md-tabs>

    <!-- Tab 1: Nutzerverwaltung -->
    <section id="panelUsers" class="tab-content">
      <div class="toolbar-row">
        <div class="search-field">
          <md-outlined-text-field id="userSearch" placeholder="Nutzer durchsuchen (Name, E-Mail, Klasse)..." style="width: 100%;" oninput="filterUsers()">
            <md-icon slot="leading-icon">search</md-icon>
          </md-outlined-text-field>
        </div>
        <div style="display: flex; gap: 12px;">
          <md-outlined-button onclick="loadUsers()">
            <md-icon slot="icon">refresh</md-icon>
            Aktualisieren
          </md-outlined-button>
          <md-filled-button id="btnOpenAddUser" onclick="openAddUserDialog()">
            <md-icon slot="icon">person_add</md-icon>
            Nutzer hinzufügen
          </md-filled-button>
        </div>
      </div>

      <!-- Stats Chips -->
      <div class="stats-chips">
        <div class="stat-chip">
          <span class="material-symbols-outlined" style="font-size: 18px; color: var(--md-sys-color-primary);">group</span>
          <span>Gesamt: <strong id="totalUsersCount">0</strong></span>
        </div>
        <div class="stat-chip">
          <span class="material-symbols-outlined" style="font-size: 18px; color: #81c784;">school</span>
          <span>Schüler: <strong id="totalStudentsCount">0</strong></span>
        </div>
        <div class="stat-chip">
          <span class="material-symbols-outlined" style="font-size: 18px; color: #ffb74d;">admin_panel_settings</span>
          <span>Admins: <strong id="totalAdminsCount">0</strong></span>
        </div>
      </div>

      <!-- User Table -->
      <div class="user-table-wrap">
        <table>
          <thead>
            <tr>
              <th>Benutzer</th>
              <th>E-Mail</th>
              <th>Klasse</th>
              <th>Schulform</th>
              <th>Klassenlehrer</th>
              <th>Rolle</th>
              <th style="text-align: right;">Aktionen</th>
            </tr>
          </thead>
          <tbody id="userTableBody">
            <tr>
              <td colspan="7" style="text-align: center; padding: 32px;">
                <md-circular-progress indeterminate></md-circular-progress>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Tab 2: Server & Status -->
    <section id="panelSystem" class="tab-content hidden">
      <div class="m3-card">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
          <h3 style="font-size: 18px; display: flex; align-items: center; gap: 8px;">
            <md-icon>dns</md-icon> Server-Metriken
          </h3>
          <md-outlined-button onclick="loadStats()">
            <md-icon slot="icon">refresh</md-icon> Aktualisieren
          </md-outlined-button>
        </div>
        <div class="grid-2">
          <div class="stat-chip" style="padding: 16px; border-radius: 12px;">
            <div>
              <div style="color: var(--md-sys-color-on-surface-variant); font-size: 13px;">Server-Status</div>
              <div id="statStatus" style="font-size: 20px; font-weight: 700; color: #81c784; margin-top: 4px;">ONLINE</div>
            </div>
          </div>
          <div class="stat-chip" style="padding: 16px; border-radius: 12px;">
            <div>
              <div style="color: var(--md-sys-color-on-surface-variant); font-size: 13px;">Server Uptime</div>
              <div id="statUptime" style="font-size: 20px; font-weight: 700; margin-top: 4px;">-</div>
            </div>
          </div>
          <div class="stat-chip" style="padding: 16px; border-radius: 12px;">
            <div>
              <div style="color: var(--md-sys-color-on-surface-variant); font-size: 13px;">Registrierte Benutzer</div>
              <div id="statUsers" style="font-size: 20px; font-weight: 700; color: var(--md-sys-color-primary); margin-top: 4px;">-</div>
            </div>
          </div>
          <div class="stat-chip" style="padding: 16px; border-radius: 12px;">
            <div>
              <div style="color: var(--md-sys-color-on-surface-variant); font-size: 13px;">Vertretungseinträge im Cache</div>
              <div id="statSubstitutions" style="font-size: 20px; font-weight: 700; margin-top: 4px;">-</div>
            </div>
          </div>
          <div class="stat-chip" style="padding: 16px; border-radius: 12px;">
            <div>
              <div style="color: var(--md-sys-color-on-surface-variant); font-size: 13px;">Synchronisierte Aufgaben</div>
              <div id="statTasks" style="font-size: 20px; font-weight: 700; margin-top: 4px;">-</div>
            </div>
          </div>
          <div class="stat-chip" style="padding: 16px; border-radius: 12px;">
            <div>
              <div style="color: var(--md-sys-color-on-surface-variant); font-size: 13px;">JVM Speicher (Frei / Gesamt)</div>
              <div id="statMemory" style="font-size: 20px; font-weight: 700; margin-top: 4px;">-</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Tab 3: Schulportal Sync -->
    <section id="panelSchulportal" class="tab-content hidden">
      <div class="m3-card">
        <h3 style="font-size: 18px; display: flex; align-items: center; gap: 8px; margin-bottom: 12px;">
          <md-icon>sync</md-icon> Automatischer Schulportal-Abgleich
        </h3>
        <p style="color: var(--md-sys-color-on-surface-variant); margin-bottom: 20px; line-height: 1.5;">
          Der Server synchronisiert den Vertretungsplan der Merianschule Seligenstadt (Schul-ID 4322) automatisch alle 15 Minuten im Hintergrund.
        </p>
        <div style="display: flex; gap: 16px; align-items: center;">
          <md-filled-button id="btnTriggerSync" onclick="triggerSchulportalSync()">
            <md-icon slot="icon">cloud_sync</md-icon>
            Jetzt sofort synchronisieren
          </md-filled-button>
          <span id="syncStatusText" style="font-size: 14px; color: var(--md-sys-color-on-surface-variant);"></span>
        </div>
      </div>
    </section>

  </main>

  <!-- Modal Dialog: Nutzer hinzufügen -->
  <md-dialog id="addUserDialog">
    <div slot="headline" style="display: flex; align-items: center; gap: 8px;">
      <md-icon>person_add</md-icon>
      Neuen Nutzer anlegen
    </div>
    <form id="addUserForm" slot="content" method="dialog" class="form-group" style="padding-top: 8px;">
      <div class="grid-2">
        <md-outlined-text-field id="addUsername" label="Benutzername*" required placeholder="z. B. max.mustermann">
          <md-icon slot="leading-icon">account_circle</md-icon>
        </md-outlined-text-field>
        <md-outlined-text-field id="addEmail" label="E-Mail*" type="email" required placeholder="z. B. max.mustermann@mssof.onmicrosoft.com">
          <md-icon slot="leading-icon">email</md-icon>
        </md-outlined-text-field>
      </div>

      <div class="grid-2">
        <md-outlined-text-field id="addFullName" label="Vollständiger Name" placeholder="z. B. Max Mustermann">
          <md-icon slot="leading-icon">badge</md-icon>
        </md-outlined-text-field>
        <md-outlined-text-field id="addPassword" label="Initiales Passwort*" type="password" required placeholder="Mindestens 6 Zeichen">
          <md-icon slot="leading-icon">lock</md-icon>
        </md-outlined-text-field>
      </div>

      <div class="grid-2">
        <md-outlined-text-field id="addClassName" label="Klasse" value="9aR" placeholder="z. B. 9aR">
          <md-icon slot="leading-icon">meeting_room</md-icon>
        </md-outlined-text-field>
        <md-outlined-select id="addSchoolType" label="Schulform">
          <md-select-option value="Realschule" selected><div slot="headline">Realschule</div></md-select-option>
          <md-select-option value="Gymnasium"><div slot="headline">Gymnasium</div></md-select-option>
          <md-select-option value="Hauptschule"><div slot="headline">Hauptschule</div></md-select-option>
          <md-select-option value="Verwaltung"><div slot="headline">Verwaltung</div></md-select-option>
        </md-outlined-select>
      </div>

      <div class="grid-2">
        <md-outlined-text-field id="addClassTeacher" label="Klassenlehrer" value="Hr. Thomas" placeholder="z. B. Hr. Thomas">
          <md-icon slot="leading-icon">person_outline</md-icon>
        </md-outlined-text-field>
        <md-outlined-select id="addRole" label="Rolle">
          <md-select-option value="USER" selected><div slot="headline">Benutzer (Schüler/Lehrer)</div></md-select-option>
          <md-select-option value="ADMIN"><div slot="headline">Administrator</div></md-select-option>
        </md-outlined-select>
      </div>
    </form>
    <div slot="actions">
      <md-text-button onclick="closeAddUserDialog()">Abbrechen</md-text-button>
      <md-filled-button id="btnSubmitAddUser" onclick="submitAddUser()">
        <md-icon slot="icon">check</md-icon>
        Nutzer anlegen
      </md-filled-button>
    </div>
  </md-dialog>

  <!-- Modal Dialog: Passwort zurücksetzen -->
  <md-dialog id="resetPasswordDialog">
    <div slot="headline">Passwort zurücksetzen</div>
    <form slot="content" method="dialog" class="form-group" style="padding-top: 8px;">
      <p id="resetPasswordDesc" style="color: var(--md-sys-color-on-surface-variant); font-size: 14px; margin-bottom: 12px;"></p>
      <input type="hidden" id="resetPasswordUserId">
      <md-outlined-text-field id="resetPasswordInput" label="Neues Passwort" type="password" required placeholder="Neues Passwort eingeben">
        <md-icon slot="leading-icon">key</md-icon>
      </md-outlined-text-field>
    </form>
    <div slot="actions">
      <md-text-button onclick="document.getElementById('resetPasswordDialog').close()">Abbrechen</md-text-button>
      <md-filled-button onclick="submitResetPassword()">Speichern</md-filled-button>
    </div>
  </md-dialog>

  <!-- Modal Dialog: Nutzer löschen bestätigen -->
  <md-dialog id="deleteUserDialog">
    <div slot="headline" style="color: var(--md-sys-color-error);">Benutzer löschen</div>
    <div slot="content">
      <p id="deleteUserDesc" style="font-size: 14px; line-height: 1.5;"></p>
      <input type="hidden" id="deleteUserId">
    </div>
    <div slot="actions">
      <md-text-button onclick="document.getElementById('deleteUserDialog').close()">Abbrechen</md-text-button>
      <md-filled-button style="--md-sys-color-primary: #ffb4ab; --md-sys-color-on-primary: #690005;" onclick="submitDeleteUser()">
        Unwiderruflich löschen
      </md-filled-button>
    </div>
  </md-dialog>

  <!-- Toast Notification -->
  <div id="toast" class="toast">
    <span id="toastIcon" class="material-symbols-outlined">check_circle</span>
    <span id="toastMessage">Aktion erfolgreich</span>
  </div>

  <script>
    let authToken = localStorage.getItem('merian_admin_token') || '';
    let allUsers = [];

    // Avatar Farb-Palette
    const AVATAR_COLORS = [
      '#1b65a4', '#395a3c', '#6b4fa2', '#007a78', '#b25e00', '#a83232', '#455a64'
    ];

    function getAvatarColor(username) {
      let hash = 0;
      for (let i = 0; i < username.length; i++) {
        hash = username.charCodeAt(i) + ((hash << 5) - hash);
      }
      return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length];
    }

    function getInitials(name, fallback) {
      const clean = (name && name.trim()) || fallback || '?';
      const parts = clean.split(' ').filter(Boolean);
      if (parts.length >= 2) {
        return (parts[0][0] + parts[1][0]).toUpperCase();
      }
      return clean.substring(0, 2).toUpperCase();
    }

    // UI Initialisierung
    document.addEventListener('DOMContentLoaded', () => {
      // Tab switching
      const tabs = document.getElementById('mainTabs');
      tabs.addEventListener('change', (e) => {
        const index = tabs.activeTabIndex;
        document.getElementById('panelUsers').classList.toggle('hidden', index !== 0);
        document.getElementById('panelSystem').classList.toggle('hidden', index !== 1);
        document.getElementById('panelSchulportal').classList.toggle('hidden', index !== 2);

        if (index === 0) loadUsers();
        if (index === 1) loadStats();
      });

      // Logout handler
      document.getElementById('btnLogout').addEventListener('click', () => {
        localStorage.removeItem('merian_admin_token');
        authToken = '';
        updateAuthView();
      });

      // Auth prüfen
      if (authToken) {
        updateAuthView();
        loadUsers();
      } else {
        updateAuthView();
      }
    });

    function updateAuthView() {
      const loggedIn = Boolean(authToken);
      document.getElementById('loginView').classList.toggle('hidden', loggedIn);
      document.getElementById('dashboardView').classList.toggle('hidden', !loggedIn);
      document.getElementById('authActions').classList.toggle('hidden', !loggedIn);
    }

    // Toast
    function showToast(message, isError = false) {
      const toast = document.getElementById('toast');
      const toastIcon = document.getElementById('toastIcon');
      const toastMsg = document.getElementById('toastMessage');

      toast.className = 'toast ' + (isError ? 'error' : 'success');
      toastIcon.textContent = isError ? 'error' : 'check_circle';
      toastMsg.textContent = message;
      toast.style.display = 'flex';

      setTimeout(() => {
        toast.style.display = 'none';
      }, 4000);
    }

    // Login
    async function handleLogin(e) {
      e.preventDefault();
      const identifier = document.getElementById('loginIdentifier').value.trim();
      const password = document.getElementById('loginPassword').value.trim();

      if (!identifier || !password) return;

      try {
        const res = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ usernameOrEmail: identifier, password: password })
        });

        const data = await res.json();
        if (res.ok && data.token) {
          if (data.user && data.user.role !== 'ADMIN') {
            showToast('Nur Administratoren haben Zugriff auf dieses Web-Panel.', true);
            return;
          }
          authToken = data.token;
          localStorage.setItem('merian_admin_token', authToken);
          updateAuthView();
          loadUsers();
          showToast('Erfolgreich als Administrator angemeldet.');
        } else {
          showToast(data.message || 'Anmeldung fehlgeschlagen.', true);
        }
      } catch (err) {
        showToast('Verbindung zum Server fehlgeschlagen: ' + err.message, true);
      }
    }

    // API Helper mit Auth
    async function apiFetch(url, options = {}) {
      options.headers = options.headers || {};
      options.headers['Authorization'] = 'Bearer ' + authToken;
      const res = await fetch(url, options);
      if (res.status === 401) {
        authToken = '';
        localStorage.removeItem('merian_admin_token');
        updateAuthView();
        throw new Error('Sitzung abgelaufen. Bitte erneut anmelden.');
      }
      return res;
    }

    // Nutzer laden
    async function loadUsers() {
      const tbody = document.getElementById('userTableBody');
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:32px;"><md-circular-progress indeterminate></md-circular-progress></td></tr>`;

      try {
        const res = await apiFetch('/api/admin/users');
        if (!res.ok) throw new Error('Nutzer konnten nicht geladen werden.');
        allUsers = await res.json();
        renderUsers(allUsers);
      } catch (err) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:24px; color:var(--md-sys-color-error);">${'$'}{err.message}</td></tr>`;
      }
    }

    function renderUsers(users) {
      const tbody = document.getElementById('userTableBody');
      const totalCount = users.length;
      const adminCount = users.filter(u => u.role === 'ADMIN').length;
      const studentCount = totalCount - adminCount;

      document.getElementById('totalUsersCount').textContent = totalCount;
      document.getElementById('totalAdminsCount').textContent = adminCount;
      document.getElementById('totalStudentsCount').textContent = studentCount;

      if (users.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:32px; color:var(--md-sys-color-on-surface-variant);">Keine Benutzer gefunden.</td></tr>`;
        return;
      }

      tbody.innerHTML = users.map(user => {
        const initials = getInitials(user.fullName, user.username);
        const color = getAvatarColor(user.username);
        const isAdmin = user.role === 'ADMIN';

        return `
          <tr>
            <td>
              <div class="user-name-cell">
                <div class="avatar-circle" style="background-color: ${'$'}{color};">${'$'}{initials}</div>
                <div>
                  <div style="font-weight: 500;">${'$'}{escapeHtml(user.fullName || user.username)}</div>
                  <div style="font-size: 12px; color: var(--md-sys-color-on-surface-variant);">@${'$'}{escapeHtml(user.username)}</div>
                </div>
              </div>
            </td>
            <td>${'$'}{escapeHtml(user.email)}</td>
            <td><span class="badge-class">${'$'}{escapeHtml(user.className || '-')}</span></td>
            <td>${'$'}{escapeHtml(user.schoolType || '-')}</td>
            <td>${'$'}{escapeHtml(user.classTeacher || '-')}</td>
            <td>
              <span class="badge-role ${'$'}{isAdmin ? 'badge-admin' : 'badge-user'}">
                ${'$'}{user.role}
              </span>
            </td>
            <td>
              <div class="actions-cell">
                <md-icon-button title="Passwort ändern" onclick="openResetPasswordDialog('${'$'}{user.id}', '${'$'}{escapeHtml(user.fullName || user.username)}')">
                  <md-icon>key</md-icon>
                </md-icon-button>
                <md-icon-button title="Benutzer löschen" onclick="openDeleteUserDialog('${'$'}{user.id}', '${'$'}{escapeHtml(user.fullName || user.username)}')">
                  <md-icon style="color: var(--md-sys-color-error);">delete</md-icon>
                </md-icon-button>
              </div>
            </td>
          </tr>
        `;
      }).join('');
    }

    function filterUsers() {
      const q = document.getElementById('userSearch').value.toLowerCase().trim();
      if (!q) {
        renderUsers(allUsers);
        return;
      }
      const filtered = allUsers.filter(u => 
        (u.username && u.username.toLowerCase().includes(q)) ||
        (u.fullName && u.fullName.toLowerCase().includes(q)) ||
        (u.email && u.email.toLowerCase().includes(q)) ||
        (u.className && u.className.toLowerCase().includes(q))
      );
      renderUsers(filtered);
    }

    // Add User Dialog
    function openAddUserDialog() {
      document.getElementById('addUserForm').reset();
      document.getElementById('addClassName').value = '9aR';
      document.getElementById('addClassTeacher').value = 'Hr. Thomas';
      document.getElementById('addUserDialog').show();
    }

    function closeAddUserDialog() {
      document.getElementById('addUserDialog').close();
    }

    async function submitAddUser() {
      const username = document.getElementById('addUsername').value.trim();
      const email = document.getElementById('addEmail').value.trim();
      const fullName = document.getElementById('addFullName').value.trim();
      const password = document.getElementById('addPassword').value.trim();
      const className = document.getElementById('addClassName').value.trim();
      const schoolType = document.getElementById('addSchoolType').value;
      const classTeacher = document.getElementById('addClassTeacher').value.trim();
      const role = document.getElementById('addRole').value;

      if (!username || !email || !password) {
        showToast('Benutzername, E-Mail und Passwort sind Pflichtfelder.', true);
        return;
      }

      if (password.length < 6) {
        showToast('Das Passwort muss mindestens 6 Zeichen lang sein.', true);
        return;
      }

      const payload = {
        username,
        email,
        fullName: fullName || username,
        password,
        className: className || '9aR',
        schoolType: schoolType || 'Realschule',
        classTeacher: classTeacher || 'Hr. Thomas',
        role: role || 'USER'
      };

      try {
        const res = await apiFetch('/api/admin/users', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (res.ok) {
          closeAddUserDialog();
          showToast(`Benutzer "${'$'}{payload.fullName}" erfolgreich angelegt!`);
          loadUsers();
        } else {
          showToast(data.message || 'Fehler beim Anlegen des Benutzers.', true);
        }
      } catch (err) {
        showToast(err.message, true);
      }
    }

    // Reset Password Dialog
    function openResetPasswordDialog(userId, userName) {
      document.getElementById('resetPasswordUserId').value = userId;
      document.getElementById('resetPasswordDesc').textContent = `Neues Passwort für "${'$'}{userName}" festlegen:`;
      document.getElementById('resetPasswordInput').value = '';
      document.getElementById('resetPasswordDialog').show();
    }

    async function submitResetPassword() {
      const userId = document.getElementById('resetPasswordUserId').value;
      const newPassword = document.getElementById('resetPasswordInput').value.trim();

      if (!newPassword || newPassword.length < 6) {
        showToast('Das neue Passwort muss mindestens 6 Zeichen lang sein.', true);
        return;
      }

      try {
        const res = await apiFetch(`/api/admin/users/${'$'}{userId}/password`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ newPassword })
        });
        const data = await res.json();
        if (res.ok) {
          document.getElementById('resetPasswordDialog').close();
          showToast('Passwort erfolgreich zurückgesetzt!');
        } else {
          showToast(data.message || 'Fehler beim Zurücksetzen.', true);
        }
      } catch (err) {
        showToast(err.message, true);
      }
    }

    // Delete User Dialog
    function openDeleteUserDialog(userId, userName) {
      document.getElementById('deleteUserId').value = userId;
      document.getElementById('deleteUserDesc').textContent = `Möchtest du das Benutzerkonto von "${'$'}{userName}" wirklich unwiderruflich löschen? Alle zugehörigen Aufgaben und Kurse werden ebenfalls entfernt.`;
      document.getElementById('deleteUserDialog').show();
    }

    async function submitDeleteUser() {
      const userId = document.getElementById('deleteUserId').value;
      try {
        const res = await apiFetch(`/api/admin/users/${'$'}{userId}`, {
          method: 'DELETE'
        });
        const data = await res.json();
        if (res.ok) {
          document.getElementById('deleteUserDialog').close();
          showToast('Benutzer erfolgreich gelöscht!');
          loadUsers();
        } else {
          showToast(data.message || 'Fehler beim Löschen.', true);
        }
      } catch (err) {
        showToast(err.message, true);
      }
    }

    // Stats
    async function loadStats() {
      try {
        const res = await apiFetch('/api/admin/stats');
        if (!res.ok) throw new Error('Stats konnten nicht geladen werden.');
        const data = await res.json();

        document.getElementById('statStatus').textContent = data.status;
        document.getElementById('statUsers').textContent = data.totalUsers;
        document.getElementById('statTasks').textContent = data.totalTasks;
        document.getElementById('statSubstitutions').textContent = data.totalSubstitutions;
        document.getElementById('statMemory').textContent = `${'$'}{data.freeMemoryMb} MB / ${'$'}{data.totalMemoryMb} MB`;

        const uptimeMinutes = Math.floor(data.uptimeMillis / 60000);
        const uptimeHours = Math.floor(uptimeMinutes / 60);
        document.getElementById('statUptime').textContent = `${'$'}{uptimeHours}h ${'$'}{uptimeMinutes % 60}m`;
      } catch (err) {
        showToast(err.message, true);
      }
    }

    // Schulportal Sync
    async function triggerSchulportalSync() {
      const btn = document.getElementById('btnTriggerSync');
      const statusText = document.getElementById('syncStatusText');
      btn.disabled = true;
      statusText.textContent = 'Synchronisiere mit Schulportal Hessen...';

      try {
        const res = await apiFetch('/api/admin/schulportal/sync', { method: 'POST' });
        const data = await res.json();
        if (res.ok) {
          showToast(data.message || 'Schulportal erfolgreich synchronisiert!');
          statusText.textContent = 'Letzte Synchronisation: Gerade eben erfolgreich.';
        } else {
          showToast(data.message || 'Synchronisation fehlgeschlagen.', true);
          statusText.textContent = 'Fehler: ' + (data.message || 'Unbekannt');
        }
      } catch (err) {
        showToast(err.message, true);
        statusText.textContent = 'Fehler: ' + err.message;
      } finally {
        btn.disabled = false;
      }
    }

    function escapeHtml(str) {
      if (!str) return '';
      return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
    }
  </script>
</body>
</html>
"""
