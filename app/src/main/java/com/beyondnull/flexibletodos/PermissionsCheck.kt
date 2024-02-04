package com.beyondnull.flexibletodos

import android.content.Context
import java.security.Permission

fun checkAndRequestPermissions(context: Context, permission: Permission) {
//    // Register the permissions callback, which handles the user's response to the
//    // system permissions dialog. Save the return value, an instance of
//    // ActivityResultLauncher. You can use either a val, as shown in this snippet,
//    // or a lateinit var in your onAttach() or onCreate() method.
//    val requestPermissionLauncher = registerForActivityResult(
//        ActivityResultContracts.RequestPermission()
//    ) { isGranted: Boolean ->
//        if (isGranted) {
//            // Permission is granted. Continue the action or workflow in your
//            // app.
//            sendNotification(this)
//        } else {
//            // Explain to the user that the feature is unavailable because the
//            // features requires a permission that the user has denied. At the
//            // same time, respect the user's decision. Don't link to system
//            // settings in an effort to convince the user to change their
//            // decision.
//        }
//    }
//    if (ContextCompat.checkSelfPermission(
//            context,
//            Manifest.permission.POST_NOTIFICATIONS
//        ) == PackageManager.PERMISSION_GRANTED
//    ) {
//        // permission granted
//    } else {
//        if (shouldShowRequestPermissionRationale(context, Manifest.permission.POST_NOTIFICATIONS)) {
//            // show rationale and then launch launcher to request permission
//        } else {
//            // first request or forever denied case
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
//            }
//        }
//    }
}