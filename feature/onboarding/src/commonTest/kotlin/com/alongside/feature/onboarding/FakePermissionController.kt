package com.alongside.feature.onboarding

internal class FakePermissionController(
    initialStatuses: Map<OnboardingPermission, PermissionStatus> = emptyMap(),
    private val requestResult: (OnboardingPermission) -> PermissionStatus = { PermissionStatus.GRANTED },
) : PermissionController {
    private val statuses = initialStatuses.toMutableMap()

    var lastRequestedPermission: OnboardingPermission? = null
        private set
    var openAppSettingsCallCount: Int = 0
        private set

    override fun status(permission: OnboardingPermission): PermissionStatus = statuses[permission] ?: PermissionStatus.NOT_DETERMINED

    /** Simulates the permission changing outside of [request] - e.g. the user granting it from
     * the system Settings screen and returning to the app. */
    fun updateStatus(
        permission: OnboardingPermission,
        status: PermissionStatus,
    ) {
        statuses[permission] = status
    }

    override fun request(
        permission: OnboardingPermission,
        onResult: (PermissionStatus) -> Unit,
    ) {
        lastRequestedPermission = permission
        val result = requestResult(permission)
        statuses[permission] = result
        onResult(result)
    }

    override fun openAppSettings() {
        openAppSettingsCallCount++
    }
}
