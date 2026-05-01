package com.example.attit.data

import android.content.Context
import com.example.attit.model.AttendanceLog
import com.example.attit.model.Friend
import com.example.attit.model.Subject
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.attit.utils.CryptoUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class SubjectRepository(
    private val subjectDao: SubjectDao,
    private val logDao: AttendanceLogDao,
    private val friendDao: FriendDao,
    private val userId: String,
    private val context: Context
) {
    private val db = Firebase.firestore

    // PATH A: Private Data (Subjects & Logs) -> Uses UID
    private val cloudPath = db.collection("students").document(userId).collection("subjects")

    // PATH B: Public Data (Profiles & Search) -> Uses Email
    private val usersPath = db.collection("users")

    private val prefs = context.getSharedPreferences("attit_prefs", Context.MODE_PRIVATE)

    // --- Main Data Flow ---
    val allSubjects: Flow<List<Subject>> = subjectDao.getAllSubjects()
    val allFriends: Flow<List<Friend>> = friendDao.getAllFriends()

    // ==========================================
    // 1. SUBJECTS & LOGS (Using UID - Private)
    // ==========================================

    suspend fun insert(subject: Subject) {
        val newDocRef = cloudPath.document()
        val firebaseDocId = newDocRef.id
        val subjectWithFirebaseId = subject.copy(id = 0, firebaseId = firebaseDocId)
        val localId = subjectDao.insertSubject(subjectWithFirebaseId)

        // Update Cloud with the local ID mapping
        // ENCRYPT DATA FOR CLOUD
        val cloudSubject = subjectWithFirebaseId.copy(
            id = localId.toInt(),
            name = subjectWithFirebaseId.name
        )
        newDocRef.set(cloudSubject).await()
    }

    suspend fun update(subject: Subject) {
        subjectDao.updateSubject(subject)
        subject.firebaseId?.let { id -> 
            val cloudSubject = subject
            cloudPath.document(id).set(cloudSubject).await() 
        }
    }

    suspend fun delete(subject: Subject) {
        // 1. Delete Local Data
        logDao.deleteLogsForSubject(subject.id)
        subjectDao.deleteSubject(subject)

        // 2. Delete Cloud Data
        subject.firebaseId?.let { id ->
            val docRef = cloudPath.document(id)

            // OPTIONAL: Try to clean up sub-collection logs to prevent orphans
            // (Firestore doesn't auto-delete subcollections, so we do a quick sweep)
            try {
                val logs = docRef.collection("logs").get().await()
                for (doc in logs) {
                    doc.reference.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Delete the subject document itself
            docRef.delete().await()
        }
    }

    fun getLogs(subjectId: Int): Flow<List<AttendanceLog>> = logDao.getLogsForSubject(subjectId)
    suspend fun getLastLog(subjectId: Int): AttendanceLog? = logDao.getLastLog(subjectId)

    suspend fun addLog(subjectId: Int, status: String) {
        val subject = subjectDao.getSubjectById(subjectId) ?: return
        val cloudId = subject.firebaseId ?: return

        // Exact timestamp used as the ID for consistency
        val timestamp = System.currentTimeMillis()

        val logData = hashMapOf(
            "timestamp" to timestamp,
            "status" to status,
            "dateString" to java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        )

        // Save to Cloud
        try {
            cloudPath.document(cloudId).collection("logs").add(logData).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Save Locally
        val log = AttendanceLog(
            subjectId = subjectId,
            timestamp = timestamp,
            status = status
        )
        logDao.insertLog(log)
    }

    // --- SYNC ENGINE ---
    suspend fun syncSubjectsFromCloud() = withContext(Dispatchers.IO) { // 1. Run on IO Thread (Prevents UI stutter)
        println("ATTIT_SYNC: Starting optimized parallel sync...")
        try {
            val snapshot = cloudPath.get().await()
            val rawCloudSubjects = snapshot.toObjects(Subject::class.java)
            val cloudSubjects = rawCloudSubjects
            
            val localSubjects = subjectDao.getAllSubjectsOnce()
            val localSubjectsByFirebaseId = localSubjects.filter { it.firebaseId != null }.associateBy { it.firebaseId!! }

            // 2. PARALLEL EXECUTION: Launch a separate "worker" for each subject
            val syncJobs = cloudSubjects.map { cloudSubject ->
                async {
                    if (cloudSubject.firebaseId != null) {
                        var currentLocalId = 0
                        val existingLocalSubject = localSubjectsByFirebaseId[cloudSubject.firebaseId]

                        // A. Sync the Subject itself
                        if (existingLocalSubject != null) {
                            val updatedLocalSubject = cloudSubject.copy(id = existingLocalSubject.id)
                            subjectDao.updateSubject(updatedLocalSubject)
                            currentLocalId = existingLocalSubject.id
                        } else {
                            val newId = subjectDao.insertSubject(cloudSubject.copy(id = 0))
                            currentLocalId = newId.toInt()
                        }

                        // B. Sync Logs (This network call now happens in parallel!)
                        val logsSnapshot = cloudPath.document(cloudSubject.firebaseId!!)
                            .collection("logs").get().await()

                        if (!logsSnapshot.isEmpty) {
                            val localLogs = logDao.getLogsSync(currentLocalId)
                            val newLogs = mutableListOf<AttendanceLog>()

                            for (logDoc in logsSnapshot.documents) {
                                val timestamp = logDoc.getLong("timestamp") ?: 0L
                                val rawStatus = logDoc.getString("status") ?: "Present"
                                val status = rawStatus
                                val exists = localLogs.any { it.timestamp == timestamp }

                                if (!exists) {
                                    newLogs.add(
                                        AttendanceLog(
                                            subjectId = currentLocalId,
                                            timestamp = timestamp,
                                            status = status
                                        )
                                    )
                                }
                            }
                            // Batch insert is faster if your DAO supports it, otherwise loops are fine here
                            newLogs.forEach { logDao.insertLog(it) }
                        }
                    }
                }
            }

            // 3. Wait for all workers to finish
            syncJobs.awaitAll()
            println("ATTIT_SYNC: Optimized sync complete.")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- UNDO / DELETE LOGIC ---
    suspend fun deleteLogAndRevert(log: AttendanceLog, passedSubject: Subject) {
        val subject = subjectDao.getSubjectById(passedSubject.id) ?: passedSubject
        val isPresent = log.status.trim().equals("Present", ignoreCase = true)

        // 1. Calculate new stats
        val newAttended = if (isPresent) (subject.attended - 1).coerceAtLeast(0) else subject.attended
        val newTotal = (subject.total - 1).coerceAtLeast(0)

        // 2. Update Subject (Cloud & Local)
        update(subject.copy(attended = newAttended, total = newTotal))

        // 3. Delete Log Locally
        logDao.deleteLog(log)

        // 4. Delete Log from Cloud
        // We must query by timestamp since we don't store the Cloud Document ID locally
        subject.firebaseId?.let { uid ->
            try {
                val query = cloudPath.document(uid).collection("logs")
                    .whereEqualTo("timestamp", log.timestamp)
                    .get().await()

                for (doc in query.documents) {
                    doc.reference.delete() // Deletes the match from Cloud
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ==========================================
    // 2. SOCIAL & PROFILE (Using Email - Public)
    // ==========================================

    suspend fun saveUser(email: String) {
        val userData = hashMapOf("uid" to userId, "email" to email)
        usersPath.document(email).set(userData, com.google.firebase.firestore.SetOptions.merge())
    }

    suspend fun saveUsername(email: String, username: String) {
        val data = hashMapOf("username" to username, "uid" to userId, "email" to email)
        usersPath.document(email).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
        prefs.edit().putString("saved_username", username).apply()
    }

    suspend fun getUsername(email: String): String? {
        val snapshot = usersPath.document(email).get().await()
        val name = snapshot.getString("username")
        if (name != null) {
            prefs.edit().putString("saved_username", name).apply()
        } else {
            prefs.edit().remove("saved_username").apply()
        }
        return name
    }

    fun getCachedUsername(): String? = prefs.getString("saved_username", null)

    suspend fun isUsernameTaken(username: String): Boolean {
        val query = usersPath.whereEqualTo("username", username).get().await()
        return !query.isEmpty
    }

    // Avatar Logic
    suspend fun saveAvatar(email: String, avatarId: String) {
        val data = hashMapOf("avatarId" to avatarId, "uid" to userId, "email" to email)
        usersPath.document(email).set(data, com.google.firebase.firestore.SetOptions.merge())
        prefs.edit().putString("saved_avatar", avatarId).apply()
    }

    suspend fun fetchAvatar(email: String): String? {
        val snapshot = usersPath.document(email).get().await()
        val avatarId = snapshot.getString("avatarId")
        if (avatarId != null) {
            prefs.edit().putString("saved_avatar", avatarId).apply()
        } else {
            prefs.edit().remove("saved_avatar").apply()
        }
        return avatarId
    }

    fun clearProfileData() {
        prefs.edit().remove("saved_username").remove("saved_avatar").apply()
    }

    fun getCachedAvatar(): String? = prefs.getString("saved_avatar", null)

    // ==========================================
    // 3. SEARCH & FRIENDS
    // ==========================================

    suspend fun findUserSimple(input: String): SimpleSearchResult? {
        var searchTerm = input.trim()
        try {
            var foundDoc: com.google.firebase.firestore.DocumentSnapshot? = null

            // 1. Try Email
            val docRef = usersPath.document(searchTerm).get().await()
            if (docRef.exists()) foundDoc = docRef

            // 2. Try Username
            else {
                if (searchTerm.startsWith("@")) searchTerm = searchTerm.substring(1)
                val query = usersPath.whereEqualTo("username", searchTerm).get().await()
                if (!query.isEmpty) foundDoc = query.documents[0]
            }

            // 3. Fetch Subjects using UID
            if (foundDoc != null) {
                val email = foundDoc.id
                val username = foundDoc.getString("username") ?: "Unknown"
                val uid = foundDoc.getString("uid") ?: ""

                val subjectsSnapshot = db.collection("students")
                    .document(uid).collection("subjects").get().await()

                val list = subjectsSnapshot.toObjects(Subject::class.java)
                return SimpleSearchResult(username, email, uid, list)
            }
            return null
        } catch (e: Exception) {
            return null
        }
    }

    suspend fun saveFriendLocal(nickname: String, email: String, uid: String) {
        val username = getUsername(email)
        val avatarId = fetchAvatar(email)
        val friend = Friend(nickname = nickname, email = email, uid = uid, username = username, avatarId = avatarId)
        friendDao.insertFriend(friend)
    }

    suspend fun deleteFriendLocal(friend: Friend) = friendDao.deleteFriend(friend)
    suspend fun getFriendSubjects(friendUid: String): List<Subject> {
        val snapshot = db.collection("students").document(friendUid).collection("subjects").get().await()
        return snapshot.toObjects(Subject::class.java)
    }
}

data class SimpleSearchResult(
    val username: String,
    val email: String,
    val uid: String,
    val subjects: List<Subject>
)