package de.kjltowinkler.msapp.ui.tasks

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.data.task.*
import de.kjltowinkler.msapp.theme.getGoogleSansFlexFontFamily
import de.kjltowinkler.msapp.theme.getQuicksandFontFamily
import de.kjltowinkler.msapp.ui.account.AccountAvatar
import de.kjltowinkler.msapp.ui.account.getInitialsForName
import de.kjltowinkler.msapp.ui.components.AppSnackbarManager
import de.kjltowinkler.msapp.ui.components.PredictiveBackContainer
import de.kjltowinkler.msapp.ui.components.handCursor
import de.kjltowinkler.msapp.ui.components.textCursor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Vollbild-Screen zum Freigeben von Aufgabenlisten exakt nach den Figma-Designs:
 * - `tasks#listeteilen#1.png`: Hauptansicht mit Top-Bar, Eingabezeile & "Zugriff verwalten" unten
 * - `tasks#listeteilen#2.png`: Live-Suche nach Nutzern/Mitschülern mit Initialen-Badge
 * - `tasks#listeteilen#3.png`: Nutzer-Chip, Mitarbeiter-Rolle, "Personen benachrichtigen" & Nachricht
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareListScreen(
    list: TaskList,
    onDismissRequest: () -> Unit,
    useMaterialYou: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val colorScheme = MaterialTheme.colorScheme

    // Farben: Dynamisch mit Material You Support oder Figma-Theme
    val screenBg = if (useMaterialYou) colorScheme.background else Color(0xFF111612)
    val textPrimary = if (useMaterialYou) colorScheme.onBackground else Color(0xFFE8EDE5)
    val textSecondary = if (useMaterialYou) colorScheme.onSurfaceVariant else Color(0xFF909A8D)
    val dividerColor = if (useMaterialYou) colorScheme.outlineVariant.copy(alpha = 0.5f) else Color(0xFF232B22)
    val accentGreen = if (useMaterialYou) colorScheme.primary else Color(0xFFABD0AA)
    val chipBg = Color.Transparent
    val chipBorder = if (useMaterialYou) colorScheme.outlineVariant else Color(0xFF2E3E4B)
    val badgeBg = if (useMaterialYou) colorScheme.secondaryContainer else Color(0xFF455A63)
    val badgeText = if (useMaterialYou) colorScheme.onSecondaryContainer else Color.White
    val actionCircleBg = if (useMaterialYou) colorScheme.surfaceContainerHigh else Color(0xFF222B22)

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var shareInfo by remember { mutableStateOf<SharedTaskListInfo?>(null) }

    // Freigabe-Einstellungen
    var currentPrivacyMode by remember { mutableStateOf(TaskPrivacyMode.RESTRICTED) }
    var currentLinkRole by remember { mutableStateOf(TaskMemberRole.VIEWER) }
    var membersList by remember { mutableStateOf<List<SharedListMember>>(emptyList()) }

    // Such- und Einladungszustand
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserSearchResultDto>>(emptyList()) }
    var suggestedUsers by remember { mutableStateOf<List<UserSearchResultDto>>(emptyList()) }
    var isLoadingSuggestions by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var selectedUsersToAdd by remember { mutableStateOf<List<UserSearchResultDto>>(emptyList()) }

    // Formularzustand bei ausgewähltem Nutzer (tasks#listeteilen#3.png)
    var selectedMemberRole by remember { mutableStateOf(TaskMemberRole.EDITOR) }
    var notifyPerson by remember { mutableStateOf(true) }
    var inviteMessage by remember { mutableStateOf("") }
    var isSendingInvite by remember { mutableStateOf(false) }

    // Dropdown- und Dialog-Zustände
    var showTopOverflowMenu by remember { mutableStateOf(false) }
    var showRoleBottomSheet by remember { mutableStateOf(false) }
    var showManageAccessSheet by remember { mutableStateOf(false) }

    // Daten beim Start laden
    LaunchedEffect(list.id) {
        isLoading = true
        errorMessage = null
        val result = TasksRepository.getShareInfo(list.id)
        if (result.isSuccess) {
            val info = result.getOrNull()
            if (info != null) {
                shareInfo = info
                currentPrivacyMode = info.privacyMode
                currentLinkRole = info.linkRole
                membersList = info.members
            } else {
                errorMessage = "Freigabeinformationen konnten nicht geladen werden."
            }
        } else {
            errorMessage = result.exceptionOrNull()?.message ?: "Fehler beim Laden der Freigabe."
        }
        isLoading = false

        // Vorschläge für Personen laden
        isLoadingSuggestions = true
        val sugResult = TasksRepository.getSuggestedUsers()
        val currentUserId = BackendApiClient.currentUser?.id
        suggestedUsers = sugResult.getOrDefault(emptyList()).filter { u ->
            u.id != currentUserId && membersList.none { it.userId == u.id }
        }
        isLoadingSuggestions = false
    }

    // Automatisch fokussieren
    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Benutzersuche mit Debounce
    LaunchedEffect(searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.length >= 1) {
            searchJob?.cancel()
            searchJob = coroutineScope.launch {
                delay(200)
                isSearching = true
                val res = TasksRepository.searchUsers(trimmed)
                val currentUserId = BackendApiClient.currentUser?.id
                searchResults = res.getOrDefault(emptyList()).filter { u ->
                    u.id != currentUserId && 
                    membersList.none { it.userId == u.id } &&
                    selectedUsersToAdd.none { it.id == u.id }
                }
                isSearching = false
            }
        } else {
            searchResults = emptyList()
            isSearching = false
        }
    }

    fun copyShareLink() {
        val info = shareInfo
        val token = info?.shareToken ?: list.shareToken ?: ""
        val url = if (info?.shareUrl?.isNotBlank() == true) info.shareUrl else "https://veloschulplaner.de/share/$token"
        clipboardManager.setText(AnnotatedString(url))
        AppSnackbarManager.showMessage("Link in die Zwischenablage kopiert")
    }

    fun sendInvite() {
        if (selectedUsersToAdd.isEmpty()) return
        val currentInfo = shareInfo ?: return

        isSendingInvite = true
        coroutineScope.launch {
            val newMembersToAdd = selectedUsersToAdd.map { user ->
                SharedListMember(
                    userId = user.id,
                    username = user.username,
                    fullName = user.fullName,
                    className = user.className,
                    avatarType = user.avatarType,
                    avatarColorHex = user.avatarColorHex,
                    role = selectedMemberRole
                )
            }
            val updatedMembers = membersList.filter { m -> selectedUsersToAdd.none { it.id == m.userId } } + newMembersToAdd

            val request = UpdateShareSettingsRequest(
                privacyMode = currentPrivacyMode,
                linkRole = currentLinkRole,
                members = updatedMembers.map { SharedListMemberUpdate(it.userId, it.role) }
            )

            val res = TasksRepository.updateShareSettings(currentInfo.listId, request)
            isSendingInvite = false
            if (res.isSuccess) {
                membersList = updatedMembers
                val count = selectedUsersToAdd.size
                val msg = if (count == 1) {
                    val name = selectedUsersToAdd.first().fullName.ifBlank { selectedUsersToAdd.first().username }
                    "$name hinzugefügt"
                } else {
                    "$count Personen hinzugefügt"
                }
                selectedUsersToAdd = emptyList()
                inviteMessage = ""
                AppSnackbarManager.showMessage(msg)
            } else {
                AppSnackbarManager.showMessage("Fehler beim Freigeben: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    PredictiveBackContainer(
        enabled = true,
        onBack = {
            if (showManageAccessSheet) {
                showManageAccessSheet = false
            } else {
                onDismissRequest()
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        AnimatedContent(
            targetState = showManageAccessSheet,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally(
                        initialOffsetX = { fullWidth -> (fullWidth * 0.28f).toInt() },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(240))) togetherWith
                    (slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -(fullWidth * 0.2f).toInt() },
                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(180)))
                } else {
                    (slideInHorizontally(
                        initialOffsetX = { fullWidth -> -(fullWidth * 0.2f).toInt() },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(240))) togetherWith
                    (slideOutHorizontally(
                        targetOffsetX = { fullWidth -> (fullWidth * 0.28f).toInt() },
                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(180)))
                }
            },
            label = "ScreenTransition",
            modifier = Modifier.fillMaxSize()
        ) { isManageAccess ->
            if (isManageAccess) {
                GeneralAccessScreen(
                    listTitle = list.title,
                    shareInfo = shareInfo,
                    privacyMode = currentPrivacyMode,
                    linkRole = currentLinkRole,
                    members = membersList,
                    onPrivacyModeChange = { newMode ->
                        currentPrivacyMode = newMode
                        val currentInfo = shareInfo
                        if (currentInfo != null) {
                            coroutineScope.launch {
                                val req = UpdateShareSettingsRequest(
                                    privacyMode = newMode,
                                    linkRole = currentLinkRole,
                                    members = membersList.map { SharedListMemberUpdate(it.userId, it.role) }
                                )
                                TasksRepository.updateShareSettings(currentInfo.listId, req)
                            }
                        }
                    },
                    onLinkRoleChange = { newRole ->
                        currentLinkRole = newRole
                        val currentInfo = shareInfo
                        if (currentInfo != null) {
                            coroutineScope.launch {
                                val req = UpdateShareSettingsRequest(
                                    privacyMode = currentPrivacyMode,
                                    linkRole = newRole,
                                    members = membersList.map { SharedListMemberUpdate(it.userId, it.role) }
                                )
                                TasksRepository.updateShareSettings(currentInfo.listId, req)
                            }
                        }
                    },
                    onUpdateMembers = { newMembers ->
                        membersList = newMembers
                        val currentInfo = shareInfo
                        if (currentInfo != null) {
                            coroutineScope.launch {
                                val req = UpdateShareSettingsRequest(
                                    privacyMode = currentPrivacyMode,
                                    linkRole = currentLinkRole,
                                    members = newMembers.map { SharedListMemberUpdate(it.userId, it.role) }
                                )
                                TasksRepository.updateShareSettings(currentInfo.listId, req)
                            }
                        }
                    },
                    onCopyLink = { copyShareLink() },
                    onDismissRequest = { showManageAccessSheet = false },
                    useMaterialYou = useMaterialYou
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(top = 10.dp)
                ) {
                    // --- TOP APP BAR: [✕] Freigeben     [Senden-Flugzeug] [⋮] ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.size(40.dp).handCursor()
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_close),
                                contentDescription = "Schließen",
                                tint = textPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Freigeben",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = getQuicksandFontFamily(),
                            color = textPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        // Senden-Icon (Flugzeug): Animiert aktiviert
                        val canSend = selectedUsersToAdd.isNotEmpty() && !isSendingInvite
                        val sendIconTint by animateColorAsState(
                            targetValue = if (canSend) accentGreen else textSecondary.copy(alpha = 0.45f),
                            animationSpec = tween(200)
                        )
                        IconButton(
                            onClick = { sendInvite() },
                            enabled = canSend,
                            modifier = Modifier.size(40.dp).handCursor()
                        ) {
                            if (isSendingInvite) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = accentGreen
                                )
                            } else {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_send),
                                    contentDescription = "Freigeben / Einladen",
                                    tint = sendIconTint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                // Dreipunkte-Menü (⋮)
                Box {
                    IconButton(
                        onClick = { showTopOverflowMenu = true },
                        modifier = Modifier.size(40.dp).handCursor()
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_more_vert),
                            contentDescription = "Optionen",
                            tint = textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showTopOverflowMenu,
                        onDismissRequest = { showTopOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Link kopieren") },
                            onClick = {
                                showTopOverflowMenu = false
                                copyShareLink()
                            },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_link), contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Zugriff verwalten") },
                            onClick = {
                                showTopOverflowMenu = false
                                showManageAccessSheet = true
                            },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_lock), contentDescription = null)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- EINGABEZEILE: [Person-Icon] [Chips...] [Eingabefeld] ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_person_add),
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    selectedUsersToAdd.forEach { user ->
                        val initials = getInitialsForName(user.fullName.ifBlank { user.username })
                        Surface(
                            shape = CircleShape,
                            color = chipBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, chipBorder),
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .clickable {
                                    selectedUsersToAdd = selectedUsersToAdd.filter { it.id != user.id }
                                }
                                .handCursor()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 3.dp, end = 12.dp, top = 3.dp, bottom = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(badgeBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = getGoogleSansFlexFontFamily(),
                                        color = badgeText
                                    )
                                }

                                Text(
                                    text = user.fullName.ifBlank { user.username },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = getGoogleSansFlexFontFamily(),
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    val searchInputTextStyle = TextStyle(
                        fontFamily = getGoogleSansFlexFontFamily(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = textPrimary
                    )

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = searchInputTextStyle,
                        cursorBrush = SolidColor(accentGreen),
                        modifier = Modifier
                            .widthIn(min = 120.dp)
                            .wrapContentWidth()
                            .focusRequester(focusRequester)
                            .textCursor()
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Backspace && searchQuery.isEmpty() && selectedUsersToAdd.isNotEmpty()) {
                                    selectedUsersToAdd = selectedUsersToAdd.dropLast(1)
                                    true
                                } else {
                                    false
                                }
                            },
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = if (selectedUsersToAdd.isEmpty()) "Personen oder Gruppen hinzufügen" else "Weitere hinzufügen",
                                        style = searchInputTextStyle.copy(
                                            color = textSecondary.copy(alpha = 0.65f)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }

            // Horizontale Trennlinie unter der Eingabezeile
            HorizontalDivider(
                color = dividerColor,
                thickness = 1.dp
            )

            // --- INHALTSBEREICH JE NACH ZUSTAND ---
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accentGreen)
                }
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFE57373),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null
                                val r = TasksRepository.getShareInfo(list.id)
                                if (r.isSuccess) {
                                    shareInfo = r.getOrNull()
                                    currentPrivacyMode = shareInfo?.privacyMode ?: TaskPrivacyMode.RESTRICTED
                                    currentLinkRole = shareInfo?.linkRole ?: TaskMemberRole.VIEWER
                                    membersList = shareInfo?.members ?: emptyList()
                                } else {
                                    errorMessage = r.exceptionOrNull()?.message
                                }
                                isLoading = false
                            }
                        }
                    ) {
                        Text("Erneut versuchen")
                    }
                }
            } else if (searchQuery.isNotEmpty()) {
                // --- ZUSTAND 2: LIVE-SUCHERGEBNISSE (tasks#listeteilen#2.png) ---
                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = accentGreen
                        )
                    }
                } else if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Kein Nutzer gefunden",
                            fontSize = 14.sp,
                            color = textSecondary
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(searchResults) { user ->
                            val initials = getInitialsForName(user.fullName.ifBlank { user.username })
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedUsersToAdd.none { it.id == user.id }) {
                                            selectedUsersToAdd = selectedUsersToAdd + user
                                        }
                                        searchQuery = ""
                                        searchResults = emptyList()
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp)
                                    .handCursor(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                    Text(
                                        text = user.fullName.ifBlank { user.username },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textPrimary
                                    )
                                    if (user.username.isNotBlank() || user.className.isNotBlank()) {
                                        Text(
                                            text = if (user.username.contains("@")) user.username else "@${user.username}${if (user.className.isNotBlank()) " • Klasse ${user.className}" else ""}",
                                            fontSize = 14.sp,
                                            color = textSecondary
                                        )
                                    }
                                }

                                // Initialen-Badge auf der rechten Seite (Screenshot 2)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(badgeBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = badgeText
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = dividerColor,
                                thickness = 1.dp
                            )
                        }
                    }
                }
            } else if (selectedUsersToAdd.isNotEmpty()) {
                // --- ZUSTAND 3: NUTZER AUSGEWÄHLT (tasks#listeteilen#3.png) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Zeile 1: "Mitarbeiter" / Rollen-Auswahl (Screenshots 3, 4 & 5)
                    val roleLabel = when (selectedMemberRole) {
                        TaskMemberRole.EDITOR -> "Mitarbeiter"
                        TaskMemberRole.COMMENTER -> "Kommentator"
                        TaskMemberRole.VIEWER -> "Betrachter"
                        TaskMemberRole.OWNER -> "Eigentümer"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRoleBottomSheet = true }
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .handCursor(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = roleLabel,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = textPrimary
                        )

                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_drop_down),
                            contentDescription = "Rolle ändern",
                            tint = textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    // Zeile 2: "Personen benachrichtigen" (mit Checkbox)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { notifyPerson = !notifyPerson }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .handCursor(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Checkbox(
                            checked = notifyPerson,
                            onCheckedChange = { notifyPerson = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = accentGreen,
                                checkmarkColor = if (useMaterialYou) colorScheme.onPrimary else Color(0xFF0F1A10),
                                uncheckedColor = textSecondary
                            )
                        )
                        Text(
                            text = "Personen benachrichtigen",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    // Zeile 3: Nachricht
                    val messageInputTextStyle = TextStyle(
                        fontFamily = getGoogleSansFlexFontFamily(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = textPrimary
                    )
                    BasicTextField(
                        value = inviteMessage,
                        onValueChange = { inviteMessage = it },
                        textStyle = messageInputTextStyle,
                        cursorBrush = SolidColor(accentGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .textCursor(),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (inviteMessage.isEmpty()) {
                                    Text(
                                        text = "Nachricht",
                                        style = messageInputTextStyle.copy(
                                            color = textSecondary.copy(alpha = 0.65f)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            } else {
                // --- ZUSTAND 1: HAUPTANSICHT MIT VORSCHLÄGE-BEREICH ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val availableSuggestions = suggestedUsers.filter { u ->
                        membersList.none { it.userId == u.id } && selectedUsersToAdd.none { it.id == u.id }
                    }

                    if (availableSuggestions.isNotEmpty()) {
                        Text(
                            text = "Vorschläge",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = getQuicksandFontFamily(),
                            color = textSecondary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(availableSuggestions) { user ->
                                val initials = getInitialsForName(user.fullName.ifBlank { user.username })
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedUsersToAdd = selectedUsersToAdd + user
                                            searchQuery = ""
                                        }
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                        .handCursor(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        AccountAvatar(
                                            accountName = user.fullName.ifBlank { user.username },
                                            avatarType = user.avatarType,
                                            avatarColorHex = user.avatarColorHex,
                                            avatarUrl = user.avatarUrl,
                                            modifier = Modifier.size(40.dp)
                                        )

                                        Column {
                                            Text(
                                                text = user.fullName.ifBlank { user.username },
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = textPrimary
                                            )
                                            if (user.username.isNotBlank() || user.className.isNotBlank()) {
                                                Text(
                                                    text = if (user.username.contains("@")) user.username else "@${user.username}${if (user.className.isNotBlank()) " • Klasse ${user.className}" else ""}",
                                                    fontSize = 13.sp,
                                                    color = textSecondary
                                                )
                                            }
                                        }
                                    }

                                    Icon(
                                        painter = painterResource(Res.drawable.ic_person_add),
                                        contentDescription = "Hinzufügen",
                                        tint = accentGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                HorizontalDivider(
                                    color = dividerColor,
                                    thickness = 1.dp
                                )
                            }
                        }
                    } else if (isLoadingSuggestions) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = accentGreen
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // Unten: "Zugriff verwalten"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = "Zugriff verwalten",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val isRestricted = currentPrivacyMode == TaskPrivacyMode.RESTRICTED
                    val hasMembers = membersList.isNotEmpty()
                    val accessTitle = when {
                        currentPrivacyMode == TaskPrivacyMode.ANYONE_WITH_LINK -> "Jeder mit dem Link"
                        currentPrivacyMode == TaskPrivacyMode.SCHOOL_USERS -> "Merianschule / Nutzer"
                        hasMembers -> "Freigegeben für ${membersList.size} Person${if (membersList.size == 1) "" else "en"}"
                        else -> "Nicht freigegeben"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showManageAccessSheet = true }
                            .padding(vertical = 4.dp)
                            .handCursor(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(actionCircleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(
                                    when (currentPrivacyMode) {
                                        TaskPrivacyMode.ANYONE_WITH_LINK -> Res.drawable.ic_language
                                        TaskPrivacyMode.SCHOOL_USERS -> Res.drawable.ic_school
                                        TaskPrivacyMode.RESTRICTED -> Res.drawable.ic_lock
                                    }
                                ),
                                contentDescription = null,
                                tint = if (!isRestricted || hasMembers) accentGreen else textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = accessTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = textPrimary
                        )
                    }
                }
            }
        }
    }

    // --- BOTTOM SHEET: ROLLENAUSWAHL (Screenshot 5) ---
    if (showRoleBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRoleBottomSheet = false },
            containerColor = if (useMaterialYou) colorScheme.surfaceContainerLow else Color(0xFF0F1820),
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val roles = listOf(
                    TaskMemberRole.VIEWER to "Betrachter",
                    TaskMemberRole.COMMENTER to "Kommentator",
                    TaskMemberRole.EDITOR to "Mitarbeiter"
                )

                roles.forEach { (role, label) ->
                    val isSelected = selectedMemberRole == role
                    val itemBg = if (isSelected) {
                        if (useMaterialYou) colorScheme.secondaryContainer else Color(0xFF004A77)
                    } else {
                        Color.Transparent
                    }
                    val textColor = if (isSelected) {
                        if (useMaterialYou) colorScheme.onSecondaryContainer else Color(0xFFC2E7FF)
                    } else {
                        textPrimary
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(itemBg)
                            .clickable {
                                selectedMemberRole = role
                                showRoleBottomSheet = false
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .handCursor(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_check),
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        } else {
                            Spacer(modifier = Modifier.width(36.dp))
                        }

                        Text(
                            text = label,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}
}
}

/**
 * Vollbild-Screen für "Allgemeiner Zugriff" exakt nach den Google Docs Screenshots:
 * - Screenshot 1: Optionen-Auswahl ("Eingeschränkt", "Jeder, der über den Link verfügt", "Merianschule") mit Haken
 * - Screenshots 2 & 3: Übersicht mit Titel "Allgemeiner Zugriff", Link-Icon rechts und klickbarer Statuszeile mit `>`
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralAccessScreen(
    listTitle: String,
    shareInfo: SharedTaskListInfo?,
    privacyMode: TaskPrivacyMode,
    linkRole: TaskMemberRole,
    members: List<SharedListMember>,
    onPrivacyModeChange: (TaskPrivacyMode) -> Unit,
    onLinkRoleChange: (TaskMemberRole) -> Unit,
    onUpdateMembers: (List<SharedListMember>) -> Unit,
    onCopyLink: () -> Unit,
    onDismissRequest: () -> Unit,
    useMaterialYou: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme
    val screenBg = if (useMaterialYou) colorScheme.background else Color(0xFF111612)
    val textPrimary = if (useMaterialYou) colorScheme.onBackground else Color(0xFFE8EDE5)
    val textSecondary = if (useMaterialYou) colorScheme.onSurfaceVariant else Color(0xFF909A8D)
    val dividerColor = if (useMaterialYou) colorScheme.outlineVariant.copy(alpha = 0.5f) else Color(0xFF232B22)
    val accentGreen = if (useMaterialYou) colorScheme.primary else Color(0xFFABD0AA)

    var isSelectingOption by remember { mutableStateOf(false) }
    var showLinkRoleBottomSheet by remember { mutableStateOf(false) }
    var activeMemberMenuId by remember { mutableStateOf<String?>(null) }

    PredictiveBackContainer(
        enabled = true,
        onBack = {
            if (isSelectingOption) isSelectingOption = false
            else onDismissRequest()
        },
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(top = 10.dp)
        ) {
            // --- TOP BAR: [←] Allgemeiner Zugriff            [ic_link] ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isSelectingOption) isSelectingOption = false
                        else onDismissRequest()
                    },
                    modifier = Modifier.size(40.dp).handCursor()
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_back),
                        contentDescription = "Zurück",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Allgemeiner Zugriff",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = getQuicksandFontFamily(),
                    color = textPrimary,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onCopyLink,
                    modifier = Modifier.size(40.dp).handCursor()
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_link),
                        contentDescription = "Link kopieren",
                        tint = textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            HorizontalDivider(color = dividerColor, thickness = 1.dp)

            AnimatedContent(
                targetState = isSelectingOption,
                transitionSpec = {
                    if (targetState) {
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> (fullWidth * 0.28f).toInt() },
                            animationSpec = tween(260, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(220))) togetherWith
                        (slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -(fullWidth * 0.2f).toInt() },
                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(180)))
                    } else {
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> -(fullWidth * 0.2f).toInt() },
                            animationSpec = tween(260, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(220))) togetherWith
                        (slideOutHorizontally(
                            targetOffsetX = { fullWidth -> (fullWidth * 0.28f).toInt() },
                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(180)))
                    }
                },
                label = "GeneralAccessStateTransition",
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { selectingOption ->
                if (selectingOption) {
                    // --- ZUSTAND 2: OPTIONEN-LISTE (Screenshot 1) ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                    // Option 1: Eingeschränkt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPrivacyModeChange(TaskPrivacyMode.RESTRICTED)
                                isSelectingOption = false
                            }
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .handCursor(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (privacyMode == TaskPrivacyMode.RESTRICTED) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_check),
                                contentDescription = null,
                                tint = accentGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        } else {
                            Spacer(modifier = Modifier.width(38.dp))
                        }

                        Column {
                            Text(
                                text = "Eingeschränkt",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Kann nur von hinzugefügten Personen geöffnet werden",
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    // Option 2: Jeder, der über den Link verfügt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPrivacyModeChange(TaskPrivacyMode.ANYONE_WITH_LINK)
                                isSelectingOption = false
                            }
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .handCursor(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (privacyMode == TaskPrivacyMode.ANYONE_WITH_LINK) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_check),
                                contentDescription = null,
                                tint = accentGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        } else {
                            Spacer(modifier = Modifier.width(38.dp))
                        }

                        Column {
                            Text(
                                text = "Jeder, der über den Link verfügt",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary
                            )
                            if (privacyMode == TaskPrivacyMode.ANYONE_WITH_LINK) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Jeder im Internet, der über den Link verfügt, kann zugreifen",
                                    fontSize = 13.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    // Option 3: Merianschule / Nutzer allgemein
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPrivacyModeChange(TaskPrivacyMode.SCHOOL_USERS)
                                isSelectingOption = false
                            }
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .handCursor(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (privacyMode == TaskPrivacyMode.SCHOOL_USERS) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_check),
                                contentDescription = null,
                                tint = accentGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        } else {
                            Spacer(modifier = Modifier.width(38.dp))
                        }

                        Column {
                            Text(
                                text = "Merianschule / Nutzer allgemein",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary
                            )
                            if (privacyMode == TaskPrivacyMode.SCHOOL_USERS) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Jeder angemeldete Schüler/Lehrer der Schule kann zugreifen",
                                    fontSize = 13.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)
                }
            } else {
                // --- ZUSTAND 1: ÜBERSICHT (Screenshots 2 & 3) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    val accessTitle = when (privacyMode) {
                        TaskPrivacyMode.RESTRICTED -> "Eingeschränkt"
                        TaskPrivacyMode.ANYONE_WITH_LINK -> "Jeder, der über den Link verfügt"
                        TaskPrivacyMode.SCHOOL_USERS -> "Merianschule / Nutzer allgemein"
                    }
                    val accessSubtitle = when (privacyMode) {
                        TaskPrivacyMode.RESTRICTED -> "Kann nur von hinzugefügten Personen geöffnet werden"
                        TaskPrivacyMode.ANYONE_WITH_LINK -> "Jeder im Internet, der über den Link verfügt, kann zugreifen"
                        TaskPrivacyMode.SCHOOL_USERS -> "Jeder angemeldete Schüler/Lehrer der Schule kann zugreifen"
                    }

                    // Klickbare Zeile mit Chevron > (Screenshots 2 & 3)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSelectingOption = true }
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                            .handCursor(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                text = accessTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = accessSubtitle,
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                        }

                        Icon(
                            painter = painterResource(Res.drawable.ic_chevron_right),
                            contentDescription = "Zugriffsart ändern",
                            tint = textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    // Falls nicht eingeschränkt: Link-Rolle (Mitarbeiter / Kommentator / Betrachter)
                    if (privacyMode != TaskPrivacyMode.RESTRICTED) {
                        val linkRoleLabel = when (linkRole) {
                            TaskMemberRole.EDITOR -> "Mitarbeiter"
                            TaskMemberRole.COMMENTER -> "Kommentator"
                            else -> "Betrachter"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLinkRoleBottomSheet = true }
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                                .handCursor(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Rolle für Link-Zugriff",
                                    fontSize = 13.sp,
                                    color = textSecondary
                                )
                                Text(
                                    text = linkRoleLabel,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textPrimary
                                )
                            }

                            Icon(
                                painter = painterResource(Res.drawable.ic_arrow_drop_down),
                                contentDescription = "Rolle ändern",
                                tint = textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        HorizontalDivider(color = dividerColor, thickness = 1.dp)
                    }

                    // Personen mit Zugriff (Eigentümer + Mitglieder)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Personen mit Zugriff",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentGreen,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Eigentümer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AccountAvatar(
                            accountName = shareInfo?.ownerName ?: "Du",
                            modifier = Modifier.size(38.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${shareInfo?.ownerName ?: "Du"} (Du)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary
                            )
                            Text(
                                text = "Eigentümer",
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                        }
                        Text(
                            text = "Eigentümer",
                            fontSize = 13.sp,
                            color = textSecondary
                        )
                    }

                    // Mitglieder
                    members.forEach { member ->
                        HorizontalDivider(
                            color = dividerColor.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            AccountAvatar(
                                accountName = member.fullName,
                                avatarType = member.avatarType,
                                avatarColorHex = member.avatarColorHex,
                                modifier = Modifier.size(38.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.fullName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textPrimary
                                )
                                Text(
                                    text = if (member.status == "PENDING") "@${member.username} • Eingeladen" else "@${member.username}",
                                    fontSize = 13.sp,
                                    color = textSecondary
                                )
                            }

                            Box {
                                TextButton(
                                    onClick = { activeMemberMenuId = member.userId },
                                    modifier = Modifier.handCursor()
                                ) {
                                    Text(
                                        text = when (member.role) {
                                            TaskMemberRole.EDITOR -> "Mitarbeiter"
                                            TaskMemberRole.COMMENTER -> "Kommentator"
                                            else -> "Betrachter"
                                        },
                                        fontSize = 13.sp,
                                        color = textPrimary
                                    )
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_keyboard_arrow_down),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = textSecondary
                                    )
                                }

                                DropdownMenu(
                                    expanded = activeMemberMenuId == member.userId,
                                    onDismissRequest = { activeMemberMenuId = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Betrachter") },
                                        onClick = {
                                            onUpdateMembers(members.map {
                                                if (it.userId == member.userId) it.copy(role = TaskMemberRole.VIEWER) else it
                                            })
                                            activeMemberMenuId = null
                                        },
                                        trailingIcon = if (member.role == TaskMemberRole.VIEWER) {
                                            { Icon(painterResource(Res.drawable.ic_check), contentDescription = null, tint = accentGreen) }
                                        } else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Kommentator") },
                                        onClick = {
                                            onUpdateMembers(members.map {
                                                if (it.userId == member.userId) it.copy(role = TaskMemberRole.COMMENTER) else it
                                            })
                                            activeMemberMenuId = null
                                        },
                                        trailingIcon = if (member.role == TaskMemberRole.COMMENTER) {
                                            { Icon(painterResource(Res.drawable.ic_check), contentDescription = null, tint = accentGreen) }
                                        } else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Mitarbeiter") },
                                        onClick = {
                                            onUpdateMembers(members.map {
                                                if (it.userId == member.userId) it.copy(role = TaskMemberRole.EDITOR) else it
                                            })
                                            activeMemberMenuId = null
                                        },
                                        trailingIcon = if (member.role == TaskMemberRole.EDITOR) {
                                            { Icon(painterResource(Res.drawable.ic_check), contentDescription = null, tint = accentGreen) }
                                        } else null
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Zugriff entfernen", color = colorScheme.error) },
                                        onClick = {
                                            onUpdateMembers(members.filter { it.userId != member.userId })
                                            activeMemberMenuId = null
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(Res.drawable.ic_delete), contentDescription = null, tint = colorScheme.error)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // BottomSheet für Link-Rolle (Screenshot 5)
    if (showLinkRoleBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLinkRoleBottomSheet = false },
            containerColor = if (useMaterialYou) colorScheme.surfaceContainerLow else Color(0xFF0F1820),
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val roles = listOf(
                    TaskMemberRole.VIEWER to "Betrachter",
                    TaskMemberRole.COMMENTER to "Kommentator",
                    TaskMemberRole.EDITOR to "Mitarbeiter"
                )

                roles.forEach { (role, label) ->
                    val isSelected = linkRole == role
                    val itemBg = if (isSelected) {
                        if (useMaterialYou) colorScheme.secondaryContainer else Color(0xFF004A77)
                    } else {
                        Color.Transparent
                    }
                    val textColor = if (isSelected) {
                        if (useMaterialYou) colorScheme.onSecondaryContainer else Color(0xFFC2E7FF)
                    } else {
                        textPrimary
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(itemBg)
                            .clickable {
                                onLinkRoleChange(role)
                                showLinkRoleBottomSheet = false
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .handCursor(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_check),
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        } else {
                            Spacer(modifier = Modifier.width(36.dp))
                        }

                        Text(
                            text = label,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}
}

/**
 * Kompatibilitäts-Alias für bestehende Aufrufe
 */
@Composable
fun ShareListBottomSheet(
    list: TaskList,
    onDismissRequest: () -> Unit,
    useMaterialYou: Boolean = true
) {
    ShareListScreen(
        list = list,
        onDismissRequest = onDismissRequest,
        useMaterialYou = useMaterialYou
    )
}
