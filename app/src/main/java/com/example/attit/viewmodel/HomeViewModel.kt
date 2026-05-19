package com.example.attit.viewmodel

import android.app.Application // <--- NEW IMPORT
import android.content.Context // <--- NEW IMPORT
import androidx.lifecycle.AndroidViewModel // <--- CHANGED FROM ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attit.data.SubjectRepository
import com.example.attit.model.AttendanceLog
import com.example.attit.model.Friend
import com.example.attit.model.Subject
import com.example.attit.screens.GyroThemes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// 1. CHANGED: Inherits from AndroidViewModel to access SharedPreferences
class HomeViewModel(
    private val repository: SubjectRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _savedUsername = MutableStateFlow("")
    val savedUsername = _savedUsername.asStateFlow()

    private val _usernameTakenError = MutableStateFlow(false)
    val usernameTakenError = _usernameTakenError.asStateFlow()

    fun resetUsernameError() {
        _usernameTakenError.value = false
    }

    // 2. THEME STATE
    private val _currentTheme = MutableStateFlow(GyroThemes.BUBBLES)
    val currentTheme = _currentTheme.asStateFlow()

    private val _currentAvatar = MutableStateFlow<String?>(null)
    val currentAvatar = _currentAvatar.asStateFlow()

    init {
        viewModelScope.launch { repository.syncSubjectsFromCloud() }
        // 3. LOAD THEME ON STARTUP
        loadSavedTheme()
    }

    val subjects: StateFlow<List<Subject>> = repository.allSubjects.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val friends: StateFlow<List<Friend>> = repository.allFriends.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- 1. NEW: SHARED STATE FOR THE DIALOG ---
    private val _isAddDialogVisible = MutableStateFlow(false)
    val isAddDialogVisible = _isAddDialogVisible.asStateFlow()

    fun openAddDialog() { _isAddDialogVisible.value = true }
    fun closeAddDialog() { _isAddDialogVisible.value = false }

    // Friend Search State
    private val _friendSubjects = MutableStateFlow<List<Subject>>(emptyList())
    val friendSubjects = _friendSubjects.asStateFlow()

    private val _friendStatus = MutableStateFlow<String>("")
    val friendStatus = _friendStatus.asStateFlow()

    private val _foundFriendEmail = MutableStateFlow("")
    val foundFriendEmail = _foundFriendEmail.asStateFlow()

    private var lastFoundUid: String? = null
    private var undoJob: kotlinx.coroutines.Job? = null

    // --- ATTENDANCE ACTIONS ---
    fun markPresent(subject: Subject) {
        viewModelScope.launch {
            val updatedSubject = subject.copy(attended = subject.attended + 1, total = subject.total + 1)
            repository.update(updatedSubject)
            repository.addLog(subject.id, "Present")
        }
    }

    fun markAbsent(subject: Subject) {
        viewModelScope.launch {
            val updatedSubject = subject.copy(total = subject.total + 1)
            repository.update(updatedSubject)
            repository.addLog(subject.id, "Absent")
        }
    }

    fun undoLastEntry(subject: Subject) {
        if (undoJob?.isActive == true) return
        undoJob = viewModelScope.launch {
            val lastLog = repository.getLastLog(subject.id)
            if (lastLog != null) {
                repository.deleteLogAndRevert(lastLog, subject)
            }
        }
    }

    // --- SUBJECT ACTIONS ---
    fun addSubject(name: String, goal: Int) {
        val exists = subjects.value.any { it.name.equals(name, ignoreCase = true) }
        if (exists) return
        viewModelScope.launch {
            repository.insert(Subject(name = name, attended = 0, total = 0, goal = goal))
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch { repository.delete(subject) }
    }

    fun getLogsForSubject(subjectId: Int): Flow<List<AttendanceLog>> = repository.getLogs(subjectId)

    fun deleteLog(log: AttendanceLog, subject: Subject) {
        viewModelScope.launch { repository.deleteLogAndRevert(log, subject) }
    }

    // --- USER PROFILE ACTIONS ---
    fun saveMyProfile(email: String) {
        viewModelScope.launch { repository.saveUser(email) }
    }

    fun saveUsername(email: String, username: String) {
        viewModelScope.launch {
            _usernameTakenError.value = false
            if (username.length < 3 || !username.matches(Regex("^[a-zA-Z0-9_]*$"))) return@launch
            if (repository.isUsernameTaken(username)) {
                _usernameTakenError.value = true
                return@launch
            }

            repository.saveUsername(email, username)
            _savedUsername.value = username
        }
    }

    fun loadUsername(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cachedName = repository.getCachedUsername()
            if (!cachedName.isNullOrEmpty()) _savedUsername.value = cachedName

            val cloudName = repository.getUsername(email)
            if (!cloudName.isNullOrEmpty()) {
                _savedUsername.value = cloudName
            } else {
                // AUTO-GENERATE DEFAULT USERNAME
                val defaultUsername = email.substringBefore("@").replace(".", "").lowercase()
                repository.saveUsername(email, defaultUsername)
                _savedUsername.value = defaultUsername
            }
        }
    }

    fun loadUserProfile(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cachedName = repository.getCachedUsername()
            if (!cachedName.isNullOrEmpty()) _savedUsername.value = cachedName

            val cachedAvatar = repository.getCachedAvatar()
            if (cachedAvatar != null) _currentAvatar.value = cachedAvatar

            val cloudName = repository.getUsername(email)
            if (!cloudName.isNullOrEmpty()) {
                _savedUsername.value = cloudName
            } else {
                val defaultUsername = email.substringBefore("@").replace(".", "").lowercase()
                repository.saveUsername(email, defaultUsername)
                _savedUsername.value = defaultUsername
            }

            val cloudAvatar = repository.fetchAvatar(email)
            if (cloudAvatar != null) {
                _currentAvatar.value = cloudAvatar
            } else {
                _currentAvatar.value = null
            }
        }
    }

    fun clearUserData() {
        _savedUsername.value = ""
        _currentAvatar.value = null
        _friendSubjects.value = emptyList()
        _friendStatus.value = ""
        _foundFriendEmail.value = ""
    }

    fun updateAvatar(email: String, avatarId: String) {
        _currentAvatar.value = avatarId
        viewModelScope.launch { repository.saveAvatar(email, avatarId) }
    }

    // --- FRIEND ACTIONS ---
    fun findFriend(input: String) {
        viewModelScope.launch {
            _friendStatus.value = "Searching..."
            _friendSubjects.value = emptyList()
            lastFoundUid = null


            val searchQuery = if (input.contains("@")) {
                input.substringBefore("@").replace(".", "").lowercase()
            } else {
                input.removePrefix("@").lowercase().trim()
            }

            val result = repository.findUserSimple(searchQuery)
            if (result != null) {
                _friendStatus.value = "Viewing ${result.username}"
                _foundFriendEmail.value = result.email
                _friendSubjects.value = result.subjects
                lastFoundUid = result.uid
            } else {
                _friendStatus.value = "User not found"
            }
        }
    }

    fun clearSearch() {
        _friendSubjects.value = emptyList()
        _friendStatus.value = ""
        _foundFriendEmail.value = ""
        lastFoundUid = null
    }

    fun saveCurrentFriend(nickname: String, email: String) {
        viewModelScope.launch {
            lastFoundUid?.let { uid ->
                repository.saveFriendLocal(nickname, email, uid)
                clearSearch()
            }
        }
    }

    fun deleteFriend(friend: Friend) {
        viewModelScope.launch { repository.deleteFriendLocal(friend) }
    }

    fun loadSavedFriend(friend: Friend) {
        viewModelScope.launch {
            _friendStatus.value = "Loading..."
            try {
                val data = repository.getFriendSubjects(friend.uid)
                _friendSubjects.value = data
                _friendStatus.value = "Viewing ${friend.nickname}"
            } catch (e: Exception) {
                _friendStatus.value = "Error"
            }
        }
    }

    // --- THEME LOGIC ---
    fun updateTheme(newTheme: String) {
        _currentTheme.value = newTheme
        saveThemeToDisk(newTheme)
    }

    private fun saveThemeToDisk(theme: String) {
        // Now safely using getApplication()
        val sharedPref = getApplication<Application>().getSharedPreferences("ATTit_Prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("saved_theme", theme)
            apply()
        }
    }

    private fun loadSavedTheme() {
        viewModelScope.launch(Dispatchers.IO) {
            val sharedPref = getApplication<Application>().getSharedPreferences("ATTit_Prefs", Context.MODE_PRIVATE)
            val saved = sharedPref.getString("saved_theme", GyroThemes.BUBBLES) ?: GyroThemes.BUBBLES
            _currentTheme.value = saved
        }
    }
}