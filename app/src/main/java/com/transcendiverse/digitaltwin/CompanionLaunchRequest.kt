package com.transcendiverse.digitaltwin

import android.content.Intent

enum class CompanionDestination {
    Today,
    CheckIn,
    Quest,
    Journal,
    Ask,
    Me,
}

data class CompanionLaunchRequest(
    val destination: CompanionDestination = CompanionDestination.Today,
    val payload: String? = null,
    val requestId: Long = 0L,
)

fun Intent.putCompanionLaunchRequest(request: CompanionLaunchRequest): Intent {
    putExtra(EXTRA_COMPANION_DESTINATION, request.destination.name)
    val cleanPayload = request.payload?.takeIf { it.isNotBlank() }
    if (cleanPayload == null) {
        removeExtra(EXTRA_COMPANION_PAYLOAD)
    } else {
        putExtra(EXTRA_COMPANION_PAYLOAD, cleanPayload)
    }
    return this
}

fun Intent?.toCompanionLaunchRequest(requestId: Long = 0L): CompanionLaunchRequest {
    val destinationName = this?.getStringExtra(EXTRA_COMPANION_DESTINATION).orEmpty()
    val destination = CompanionDestination.entries.firstOrNull { it.name == destinationName }
        ?: CompanionDestination.Today
    val payload = this?.getStringExtra(EXTRA_COMPANION_PAYLOAD)?.takeIf { it.isNotBlank() }

    return CompanionLaunchRequest(
        destination = destination,
        payload = payload,
        requestId = requestId,
    )
}

const val EXTRA_COMPANION_DESTINATION = "com.transcendiverse.digitaltwin.extra.COMPANION_DESTINATION"
const val EXTRA_COMPANION_PAYLOAD = "com.transcendiverse.digitaltwin.extra.COMPANION_PAYLOAD"
