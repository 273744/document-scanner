package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Helper object for managing runtime permissions in the Document Scanner app.
 * Handles permission requests for Android 6.0+ (API 23+)
 */
object PermissionHelper {

    // Permission request codes
    const val CAMERA_PERMISSION_REQUEST_CODE = 100
    const val STORAGE_PERMISSION_REQUEST_CODE = 101
    const val ALL_PERMISSIONS_REQUEST_CODE = 102

    /**
     * Get the list of required storage permissions based on Android version
     */
    fun getStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-12 - Scoped storage
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            // Android 9 and below
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * Check if camera permission is granted
     */
    fun isCameraPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if all storage permissions are granted
     */
    fun isStoragePermissionGranted(context: Context): Boolean {
        val permissions = getStoragePermissions()
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if all required permissions are granted
     */
    fun areAllPermissionsGranted(context: Context): Boolean {
        return isCameraPermissionGranted(context) && isStoragePermissionGranted(context)
    }

    /**
     * Request camera permission
     */
    fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Request storage permissions
     */
    fun requestStoragePermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            getStoragePermissions(),
            STORAGE_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Request all required permissions at once
     */
    fun requestAllPermissions(activity: Activity) {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        permissions.addAll(getStoragePermissions())

        ActivityCompat.requestPermissions(
            activity,
            permissions.toTypedArray(),
            ALL_PERMISSIONS_REQUEST_CODE
        )
    }

    /**
     * Check if we should show rationale for camera permission
     */
    fun shouldShowCameraPermissionRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.CAMERA
        )
    }

    /**
     * Check if we should show rationale for storage permissions
     */
    fun shouldShowStoragePermissionRationale(activity: Activity): Boolean {
        return getStoragePermissions().any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    /**
     * Handle permission request results
     * Returns true if all requested permissions were granted
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        grantResults: IntArray
    ): Boolean {
        return when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE,
            STORAGE_PERMISSION_REQUEST_CODE,
            ALL_PERMISSIONS_REQUEST_CODE -> {
                grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            }
            else -> false
        }
    }
}

