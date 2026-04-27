package at.wenzina.model

data class Project(
    val id: Int,
    val dataResource: String,
    val hasChanged: Boolean
)

data class SyncLogEntry(
    val projectId: Int,
    val dataResource: String,
    val errorCode: Int,
    val uuid: String,
    val processedAt: String
)