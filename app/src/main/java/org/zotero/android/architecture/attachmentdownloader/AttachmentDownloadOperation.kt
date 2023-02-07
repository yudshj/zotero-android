package org.zotero.android.architecture.attachmentdownloader

import kotlinx.coroutines.Job
import okhttp3.ResponseBody
import org.zotero.android.BuildConfig
import org.zotero.android.api.SyncApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.sync.LibraryIdentifier
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.FileStore

class AttachmentDownloadOperation(
    private val file: File,
    private val download: AttachmentDownloader.Download,
    private val batchProgress: BatchProgress,
    private val userId: Long,
    private val syncApi: SyncApi,
    private val fileStorage: FileStore,
    private var requestJob: Job? = null,
) {
    private enum class State {
        downloading, unzipping, done
    }

    sealed class Error : Exception() {
        object cancelled : Error()
    }

    private var state: State? = null

    var finishedDownload: ((customResult: CustomResult<Unit>) -> Void)? = null

    suspend fun start() {
        if (this.state == null && (requestJob == null || requestJob?.isCancelled == false)) {
            startDownload()
        }
    }

    private suspend fun startDownload() {
        var isCompressed = false //TODO Use WebDavController
        Timber.i("AttachmentDownloadOperation: start downloading ${this.download.key}")
        this.state = State.downloading

        val networkResult = downloadRequest( key = this.download.key, libraryId = this.download.libraryId, userId = this.userId)

        if (networkResult is CustomResult.GeneralError) {
            processError(networkResult)
            return
        }

        try {
            networkResult as CustomResult.GeneralSuccess.NetworkSuccess
            val responseBody = networkResult.value!!
            val byteStream = responseBody.byteStream()

            val input = BufferedInputStream(byteStream)
            val output = FileOutputStream(file)

            val contentLength = responseBody.contentLength()
            var numOfBytesRead: Int
            var totalNumberOfBytesRead: Long = 0
            val byteArray = ByteArray(1024)

            while (input.read(byteArray).also { numOfBytesRead = it } != -1) {
                totalNumberOfBytesRead += numOfBytesRead
                output.write(byteArray, 0, numOfBytesRead)

                //TODO uncompress if necessary

                if (requestJob?.isCancelled == true) {
                    return
                }
                val currentProgressInHundreds =
                    ((totalNumberOfBytesRead / contentLength.toDouble()) * 100).toInt()
                val downloadProgress = Progress(currentProgressInHundreds)
                batchProgress.updateProgress(file, downloadProgress)
            }
            output.flush()
            output.close()
            input.close()

            if (requestJob?.isCancelled == true) {
                return
            }
            requestJob = null
            state = State.done

            //TODO unzip if compressed

            // Finish download
            finish(CustomResult.GeneralSuccess(null))
        } catch (e: Exception) {
            processError(CustomResult.GeneralError.CodeError(e))
        }

    }

    private suspend fun downloadRequest(key: String, libraryId: LibraryIdentifier, userId: Long): CustomResult<ResponseBody> {
        //TODO download from WebDavController
        val url =
            BuildConfig.BASE_API_URL + "/" + libraryId.apiPath(userId = userId) + "/items/$key/file"
        val networkResult = safeApiCall {
            syncApi.downloadFile(url)
        }
        return networkResult
    }

    private fun finish(result: CustomResult<Unit>) {
        Timber.i("AttachmentDownloadOperation: finished downloading ${this.download.key}")
        finishedDownload?.let { it(result)}
    }

    fun cancel() {
        Timber.i("AttachmentDownloadOperation: cancelled ${this.download.key}")
        val localState = state
        if (localState == null) {
            finishedDownload?.let { it(
                CustomResult.GeneralError.CodeError(
                    Error.cancelled
                )
            ) }
            return
        }
        state = null

        when (localState) {
            State.downloading -> {
                requestJob?.cancel()
                requestJob = null
            }
            State.done -> {
            }
            State.unzipping -> {
                //TODO logic for unzip cancelling
            }
        }

        finishedDownload?.let { it(
            CustomResult.GeneralError.CodeError(
                Error.cancelled
            )
        )}
    }

    private fun processError(error: CustomResult.GeneralError) {
        when (error) {
            is CustomResult.GeneralError.CodeError -> Timber.e(error.throwable)
            is CustomResult.GeneralError.NetworkError -> Timber.e(error.stringResponse)
        }
        if (requestJob?.isCancelled == true) {
            return
        }
        if (file.exists()) {
            file.delete()
        }

        requestJob = null
        state = State.done
        finish(error)
    }
}
