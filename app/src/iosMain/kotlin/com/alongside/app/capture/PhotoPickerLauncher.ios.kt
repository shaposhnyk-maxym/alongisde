package com.alongside.app.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Photos.PHAccessLevelReadWrite
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHPhotoLibrary
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * `PHPickerViewController` (PhotosUI, iOS 14+) - a system framework with auto-generated cinterop,
 * same "no Swift needed" precedent as [com.alongside.feature.onboarding.IosPermissionController]
 * (unlike `GoogleAuthProvider`, which had to be Swift since GIDSignIn is an SPM dependency). Runs
 * out-of-process and doesn't itself require Photos library authorization to let the user pick -
 * only resolving [PHPickerResult.assetIdentifier] back to a real `PHAsset` (which
 * [com.alongside.feature.diary.capture.IosExifPhotoReader]/`IosPhotoByteReader` do) needs that,
 * consistent with the permission flow M7 already built.
 *
 * `.delegate` is a weak reference (standard UIKit delegate pattern) - [holder] keeps a strong
 * Kotlin/ObjC reference alive for the picker's lifetime, otherwise ARC would deallocate the
 * delegate before `picker:didFinishPicking:` fires.
 */
@Composable
internal actual fun rememberPhotoPickerLauncher(onPick: (List<String>) -> Unit): () -> Unit {
    val holder = remember { PickerDelegateHolder() }
    return {
        val status = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite)
        if (status == PHAuthorizationStatusNotDetermined) {
            // Onboarding (M7) normally requests this, but a silently-restored session skips
            // Onboarding entirely - without this, PHPickerResult.assetIdentifier comes back nil
            // for every picked item, silently no-oping the whole capture flow. Presenting the
            // picker regardless of the outcome mirrors Apple's own privacy model - a denial just
            // means assetIdentifier stays nil, same as before.
            //
            // requestAuthorizationForAccessLevel's completion fires on an arbitrary background
            // queue (Apple's documented behavior) - constructing/presenting PHPickerViewController
            // off the main thread hits a hard assertion failure and crashes the app immediately
            // (confirmed live 2026-07-30: "This code must be running on the main thread",
            // -[PHPickerViewController initWithConfiguration:]). dispatch_async back to main first.
            PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelReadWrite) {
                dispatch_async(dispatch_get_main_queue()) {
                    presentPicker(holder, onPick)
                }
            }
        } else {
            presentPicker(holder, onPick)
        }
    }
}

private fun presentPicker(
    holder: PickerDelegateHolder,
    onPick: (List<String>) -> Unit,
) {
    // The parameterless PHPickerConfiguration() deliberately never populates
    // PHPickerResult.assetIdentifier, regardless of Photos permission - that's Apple's
    // library-independent picking mode. Passing the shared PHPhotoLibrary is required to opt into
    // identifier resolution (confirmed live 2026-07-30: assetIdentifier stayed nil for every pick
    // with the parameterless init, even with PHAccessLevelReadWrite freshly granted).
    val configuration =
        PHPickerConfiguration(photoLibrary = PHPhotoLibrary.sharedPhotoLibrary()).apply {
            selectionLimit = 0L
            filter = PHPickerFilter.imagesFilter()
        }
    val picker = PHPickerViewController(configuration = configuration)
    val delegate =
        PickerDelegate { identifiers ->
            holder.delegate = null
            onPick(identifiers)
        }
    holder.delegate = delegate
    picker.delegate = delegate
    topPresentedViewController()?.presentViewController(picker, animated = true, completion = null)
}

private class PickerDelegateHolder {
    var delegate: PickerDelegate? = null
}

@OptIn(ExperimentalForeignApi::class)
private class PickerDelegate(
    private val onResult: (List<String>) -> Unit,
) : NSObject(),
    PHPickerViewControllerDelegateProtocol {
    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>,
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val identifiers = didFinishPicking.mapNotNull { (it as? PHPickerResult)?.assetIdentifier }
        onResult(identifiers)
    }
}

private fun topPresentedViewController(): UIViewController? {
    val keyWindow =
        UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { it.windows.filterIsInstance<UIWindow>() }
            .firstOrNull { it.isKeyWindow() }
    var top = keyWindow?.rootViewController
    while (top?.presentedViewController != null) {
        top = top.presentedViewController
    }
    return top
}
