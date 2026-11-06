package com.example.data.remote

import android.app.Activity
import android.content.Context
import com.example.BuildConfig
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.RecurringTransactionDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import com.example.domain.model.SyncState
import com.example.domain.model.TransactionType
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseManager(
    private val context: Context,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val recurringDao: RecurringTransactionDao,
    private val userDao: UserDao
) {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy {
        val instance = FirebaseFirestore.getInstance()
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            instance.firestoreSettings = settings
        } catch (e: Exception) {
            Log.w("FirebaseManager", "Could not set custom firestore settings: ${e.message}")
        }
        instance
    }

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    private var activeListeners = mutableListOf<ListenerRegistration>()

    val currentUser: FirebaseUser?
        get() = try {
            auth.currentUser
        } catch (e: Exception) {
            null
        }

    init {
        try {
            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null && user.uid.isNotBlank()) {
                    setupRealtimeListeners(user.uid)
                } else {
                    removeRealtimeListeners()
                    _syncState.value = SyncState.IDLE
                }
            }
        } catch (e: Exception) {
            Log.w("FirebaseManager", "Could not register auth state listener: ${e.message}")
        }
    }

    fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun registerUser(name: String, email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: return@withContext Result.failure(Exception("User creation failed"))

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name.trim())
                .build()
            user.updateProfile(profileUpdates).await()

            // Save user entity locally and in Firestore under users/{userId}
            val userEntity = UserEntity(
                userId = user.uid,
                email = user.email ?: email,
                displayName = name.trim(),
                updatedAt = System.currentTimeMillis()
            )
            userDao.insertUser(userEntity)

            try {
                firestore.collection("users").document(user.uid)
                    .set(userEntity, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w("FirebaseManager", "Could not sync new user profile to Firestore immediately: ${e.message}")
            }

            // Migrate any local guest data to this registered account
            migrateLocalGuestData(user.uid)
            syncWithCloud(user.uid)
            setupRealtimeListeners(user.uid)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            Log.d("FirebaseManager", "Starting email login for: $email")
            val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: return@withContext Result.failure(Exception("Login failed: User is null"))
            Log.d("FirebaseManager", "Login successful: ${user.uid}")

            val userEntity = UserEntity(
                userId = user.uid,
                email = user.email ?: email,
                displayName = user.displayName ?: email.substringBefore("@"),
                updatedAt = System.currentTimeMillis()
            )
            userDao.insertUser(userEntity)

            // Migrate any local guest data and sync
            try {
                migrateLocalGuestData(user.uid)
                syncWithCloud(user.uid)
                setupRealtimeListeners(user.uid)
            } catch (e: Exception) {
                Log.e("FirebaseManager", "Post-login sync failed: ${e.message}", e)
            }

            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Login exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            Log.d("FirebaseManager", "Starting Google Sign-In")
            val credentialManager = CredentialManager.create(activity)
            val webClientId = getWebClientId(activity)
                ?: return@withContext Result.failure(
                    Exception("Google Sign-In configuration missing.")
                )

            val googleOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: return@withContext Result.failure(Exception("Google Sign-In failed"))
                Log.d("FirebaseManager", "Google Sign-In successful: ${user.uid}")

                val userEntity = UserEntity(
                    userId = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: "Google User",
                    updatedAt = System.currentTimeMillis()
                )
                userDao.insertUser(userEntity)

                try {
                    firestore.collection("users").document(user.uid)
                        .set(userEntity, SetOptions.merge())
                        .await()
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "Profile sync failed: ${e.message}", e)
                }

                try {
                    migrateLocalGuestData(user.uid)
                    syncWithCloud(user.uid)
                    setupRealtimeListeners(user.uid)
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "Post-Google sync failed: ${e.message}", e)
                }

                Result.success(user)
            } else {
                Result.failure(Exception("Unexpected credential type returned from Google"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("FirebaseManager", "Google Sign-In cancelled: ${e.message}")
            Result.failure(Exception("Sign-in cancelled"))
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Google Sign-In exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun getWebClientId(context: Context): String? {
        // 1. Check if the user provided it via the Secrets panel (BuildConfig)
        val configId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (configId.isNotBlank() && !configId.contains("YOUR_WEB_CLIENT_ID_HERE")) {
            return configId
        }

        // 2. Fallback to the generated resource from google-services.json
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        try {
            removeRealtimeListeners()
            auth.signOut()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val credentialManager = CredentialManager.create(context)
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w("FirebaseManager", "Could not clear credential state: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w("FirebaseManager", "Notice signing out: ${e.message}")
        }
    }

    private fun removeRealtimeListeners() {
        for (listener in activeListeners) {
            listener.remove()
        }
        activeListeners.clear()
    }

    fun setupRealtimeListeners(userId: String) {
        val currentUid = auth.currentUser?.uid
        if (userId.isBlank() || userId == "local_user" || currentUid != userId) {
            removeRealtimeListeners()
            return
        }
        removeRealtimeListeners()

        val scope = CoroutineScope(Dispatchers.IO)

        // 1. Listen to users/{userId}/expenses
        val expenseListener = firestore.collection("users")
            .document(userId)
            .collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w("FirebaseManager", "Expenses listener: permissions pending for $userId")
                        removeRealtimeListeners()
                        _syncState.value = SyncState.PENDING
                    } else {
                        Log.w("FirebaseManager", "Expenses listener notice: ${error.message}")
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch {
                        for (doc in snapshot.documents) {
                            try {
                                val tx = doc.toObject(TransactionEntity::class.java)
                                if (tx != null) {
                                    val local = transactionDao.getTransactionById(tx.transactionId)
                                    if (local == null || tx.updatedAt > local.updatedAt) {
                                        transactionDao.insertTransaction(tx.copy(isSynced = true))
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w("FirebaseManager", "Error parsing expense doc ${doc.id}: ${e.message}")
                            }
                        }
                    }
                }
            }
        activeListeners.add(expenseListener)

        // 2. Listen to users/{userId}/income
        val incomeListener = firestore.collection("users")
            .document(userId)
            .collection("income")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w("FirebaseManager", "Income listener: permissions pending for $userId")
                        removeRealtimeListeners()
                        _syncState.value = SyncState.PENDING
                    } else {
                        Log.w("FirebaseManager", "Income listener notice: ${error.message}")
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch {
                        for (doc in snapshot.documents) {
                            try {
                                val tx = doc.toObject(TransactionEntity::class.java)
                                if (tx != null) {
                                    val local = transactionDao.getTransactionById(tx.transactionId)
                                    if (local == null || tx.updatedAt > local.updatedAt) {
                                        transactionDao.insertTransaction(tx.copy(isSynced = true))
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w("FirebaseManager", "Error parsing income doc ${doc.id}: ${e.message}")
                            }
                        }
                    }
                }
            }
        activeListeners.add(incomeListener)

        // 3. Listen to users/{userId}/budgets
        val budgetListener = firestore.collection("users")
            .document(userId)
            .collection("budgets")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w("FirebaseManager", "Budgets listener: permissions pending for $userId")
                        removeRealtimeListeners()
                        _syncState.value = SyncState.PENDING
                    } else {
                        Log.w("FirebaseManager", "Budgets listener notice: ${error.message}")
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch {
                        for (doc in snapshot.documents) {
                            try {
                                val budget = doc.toObject(BudgetEntity::class.java)
                                if (budget != null) {
                                    budgetDao.insertBudget(budget.copy(isSynced = true))
                                }
                            } catch (e: Exception) {
                                Log.w("FirebaseManager", "Error parsing budget doc ${doc.id}: ${e.message}")
                            }
                        }
                    }
                }
            }
        activeListeners.add(budgetListener)

        // 4. Listen to users/{userId}/recurring_expenses
        val recurringListener = firestore.collection("users")
            .document(userId)
            .collection("recurring_expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w("FirebaseManager", "Recurring listener: permissions pending for $userId")
                        removeRealtimeListeners()
                        _syncState.value = SyncState.PENDING
                    } else {
                        Log.w("FirebaseManager", "Recurring listener notice: ${error.message}")
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch {
                        for (doc in snapshot.documents) {
                            try {
                                val rec = doc.toObject(RecurringTransactionEntity::class.java)
                                if (rec != null) {
                                    recurringDao.insertRecurring(rec.copy(isSynced = true))
                                }
                            } catch (e: Exception) {
                                Log.w("FirebaseManager", "Error parsing recurring doc ${doc.id}: ${e.message}")
                            }
                        }
                    }
                }
            }
        activeListeners.add(recurringListener)
    }

    private suspend fun migrateLocalGuestData(userId: String) = withContext(Dispatchers.IO) {
        try {
            // Check if there are local transactions under "local_user"
            val localTxList = transactionDao.getUnsyncedTransactions()
            for (tx in localTxList) {
                if (tx.userId == "local_user") {
                    transactionDao.updateTransaction(tx.copy(userId = userId, isSynced = false))
                }
            }
        } catch (e: Exception) {
            Log.w("FirebaseManager", "Data migration check: ${e.message}")
        }
    }

    suspend fun syncWithCloud(userId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            _syncState.value = SyncState.OFFLINE
            return@withContext false
        }

        if (userId.isBlank() || userId == "local_user") {
            _syncState.value = SyncState.IDLE
            return@withContext false
        }

        val currentUid = auth.currentUser?.uid
        if (currentUid == null || currentUid != userId) {
            _syncState.value = SyncState.IDLE
            return@withContext false
        }

        try {
            _syncState.value = SyncState.SYNCING
            val userDocRef = firestore.collection("users").document(userId)

            // 1. Upload unsynced local transactions to users/{userId}/expenses or users/{userId}/income
            val unsyncedTransactions = transactionDao.getUnsyncedTransactions()
            for (tx in unsyncedTransactions) {
                if (tx.userId != userId) continue

                val targetCollection = if (tx.type == TransactionType.INCOME.name) {
                    userDocRef.collection("income")
                } else {
                    userDocRef.collection("expenses")
                }

                if (tx.isDeleted) {
                    targetCollection.document(tx.transactionId).delete().await()
                } else {
                    targetCollection.document(tx.transactionId)
                        .set(tx, SetOptions.merge())
                        .await()
                }
                transactionDao.markTransactionSynced(tx.transactionId)
            }

            // 2. Download cloud expenses from users/{userId}/expenses
            val expensesSnapshot = userDocRef.collection("expenses").get().await()
            for (doc in expensesSnapshot.documents) {
                try {
                    val cloudTx = doc.toObject(TransactionEntity::class.java)
                    if (cloudTx != null) {
                        val localTx = transactionDao.getTransactionById(cloudTx.transactionId)
                        if (localTx == null || cloudTx.updatedAt > localTx.updatedAt) {
                            transactionDao.insertTransaction(cloudTx.copy(isSynced = true))
                        }
                    }
                } catch (e: Exception) {
                    Log.w("FirebaseManager", "Error sync expense doc ${doc.id}: ${e.message}")
                }
            }

            // 3. Download cloud income from users/{userId}/income
            val incomeSnapshot = userDocRef.collection("income").get().await()
            for (doc in incomeSnapshot.documents) {
                try {
                    val cloudTx = doc.toObject(TransactionEntity::class.java)
                    if (cloudTx != null) {
                        val localTx = transactionDao.getTransactionById(cloudTx.transactionId)
                        if (localTx == null || cloudTx.updatedAt > localTx.updatedAt) {
                            transactionDao.insertTransaction(cloudTx.copy(isSynced = true))
                        }
                    }
                } catch (e: Exception) {
                    Log.w("FirebaseManager", "Error sync income doc ${doc.id}: ${e.message}")
                }
            }

            // 4. Upload unsynced local budgets to users/{userId}/budgets
            val unsyncedBudgets = budgetDao.getUnsyncedBudgets()
            for (budget in unsyncedBudgets) {
                if (budget.userId != userId) continue
                if (budget.isDeleted) {
                    userDocRef.collection("budgets").document(budget.budgetId).delete().await()
                } else {
                    userDocRef.collection("budgets").document(budget.budgetId)
                        .set(budget, SetOptions.merge())
                        .await()
                }
                budgetDao.markBudgetSynced(budget.budgetId)
            }

            // 5. Download cloud budgets from users/{userId}/budgets
            val budgetsSnapshot = userDocRef.collection("budgets").get().await()
            for (doc in budgetsSnapshot.documents) {
                try {
                    val cloudBudget = doc.toObject(BudgetEntity::class.java)
                    if (cloudBudget != null) {
                        budgetDao.insertBudget(cloudBudget.copy(isSynced = true))
                    }
                } catch (e: Exception) {
                    Log.w("FirebaseManager", "Error sync budget doc ${doc.id}: ${e.message}")
                }
            }

            // 6. Upload unsynced local recurring transactions to users/{userId}/recurring_expenses
            val unsyncedRecurring = recurringDao.getUnsyncedRecurring()
            for (rec in unsyncedRecurring) {
                if (rec.userId != userId) continue
                if (rec.isDeleted) {
                    userDocRef.collection("recurring_expenses").document(rec.recurringId).delete().await()
                } else {
                    userDocRef.collection("recurring_expenses").document(rec.recurringId)
                        .set(rec, SetOptions.merge())
                        .await()
                }
                recurringDao.markRecurringSynced(rec.recurringId)
            }

            // 7. Download cloud recurring from users/{userId}/recurring_expenses
            val recurringSnapshot = userDocRef.collection("recurring_expenses").get().await()
            for (doc in recurringSnapshot.documents) {
                try {
                    val cloudRec = doc.toObject(RecurringTransactionEntity::class.java)
                    if (cloudRec != null) {
                        recurringDao.insertRecurring(cloudRec.copy(isSynced = true))
                    }
                } catch (e: Exception) {
                    Log.w("FirebaseManager", "Error sync recurring doc ${doc.id}: ${e.message}")
                }
            }

            _syncState.value = SyncState.SYNCED
            _lastSyncTime.value = System.currentTimeMillis()
            true
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Log.w("FirebaseManager", "Cloud sync: permissions pending for user $userId")
                _syncState.value = SyncState.PENDING
            } else {
                Log.w("FirebaseManager", "Firestore sync: ${e.message}")
                _syncState.value = SyncState.ERROR
            }
            false
        } catch (e: Exception) {
            Log.w("FirebaseManager", "Sync exception: ${e.message}")
            _syncState.value = SyncState.ERROR
            false
        }
    }
}
