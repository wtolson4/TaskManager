package com.beyondnull.flexibletodos.manager

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

class PermissionRequester(private val activity: ComponentActivity) {
    private var requestPermissionContinuation: CancellableContinuation<Boolean>? = null

    private val requestPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            requestPermissionContinuation?.resumeWith(Result.success(isGranted))
        }

    suspend operator fun invoke(permission: String) =
        suspendCancellableCoroutine<Boolean> { continuation ->
            requestPermissionContinuation = continuation
            if (activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission granted, no-op.
                requestPermissionContinuation?.resumeWith(Result.success(true))
            } else {
                if (activity.shouldShowRequestPermissionRationale(permission)
                ) {
                    // TODO: (P3) show rationale first before launching launcher to request permission
                    requestPermissionLauncher.launch(permission)
                } else {
                    // first request or forever denied case
                    requestPermissionLauncher.launch(permission)
                }
            }

            continuation.invokeOnCancellation {
                requestPermissionContinuation = null
            }
        }
}