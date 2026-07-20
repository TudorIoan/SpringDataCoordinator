package app.springdata.coordinator.repo

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import app.springdata.coordinator.BuildConfig
import app.springdata.coordinator.model.AppSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun fetchAppSettings(): Result<AppSettings?> {
        return try {
            val document = firestore.collection("app_settings")
                .document(BuildConfig.APPLICATION_ID)
                .get()
                .await()

            if (!document.exists()) {
                return Result.success(null)
            }

            val settings = AppSettings(
                minAppVersion = document.getLong("min_app_version")?.toInt() ?: 0,
                minAppMessage = document.getString("min_app_message").orEmpty()
            )

            Result.success(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
