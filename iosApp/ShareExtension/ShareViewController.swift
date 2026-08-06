import UIKit
import UniformTypeIdentifiers
import UserNotifications
import os

// NSLog's dynamic (%@) content gets redacted as `<private>` in Console.app on a real device
// (iOS's unified logging privacy default, confirmed live 2026-08-06 - every interpolated string
// showed up blank when copied out of Console.app, while a literal/static value didn't) - os.Logger
// with an explicit `privacy: .public` on every interpolated value is the only way to actually see
// this content off-device.
private let shareLogger = Logger(subsystem: "com.alongside.max", category: "ShareExtension")

/// The Share Extension's entire job (docs/roadmap.md M7): pull the shared URL/text out of the
/// extension item, hand it to the main app via the shared App Group container, schedule a local
/// notification, and dismiss immediately - no visible UI of its own (deliberately not
/// `SLComposeServiceViewController`, which would show Apple's "compose a post" chrome).
///
/// The notification, not the extension's own UI, is what gets the user back into Alongside:
/// there is no supported way for a Share Extension to open its containing app directly
/// (`NSExtensionContext.open(_:)` is Today/widget-extension only per Apple's docs, confirmed live
/// 2026-08-02 - the call flashes an "Open in Alongside?" prompt that never completes, matching
/// openradar rdar://17551744; the classic `UIResponder`-chain `openURL:` private-API hack also
/// confirmed live to find no reachable `UIApplication` instance from this process). Tapping a
/// local notification, by contrast, is a fully standard, always-reliable way to foreground an
/// app - `AlongsideiOSApp.swift`'s existing `scenePhase == .active` handler already picks up
/// `pendingShareText` and navigates to `PlaceImportScreen` on any foreground, notification-tap
/// included, so no extra plumbing is needed on the app side. The actual link resolve/import
/// pipeline (`PlaceImportPipeline`) deliberately still runs only in the main app, not duplicated
/// here - this extension stays plain Swift/UIKit, no KMP/Compose/Koin dependency (extensions are
/// separate, memory-constrained processes).
final class ShareViewController: UIViewController {
    private static let appGroupSuiteName = "group.com.alongside.max"
    private static let pendingShareTextKey = "pendingShareText"
    private var hasStartedExtraction = false

    override func viewDidLoad() {
        super.viewDidLoad()
        shareLogger.log("viewDidLoad")
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        // Starting extraction (and therefore completeRequest) only after the extension's own
        // presentation animation has finished - not in viewDidLoad - avoids racing the system's
        // "slide up the share sheet" transition. Plain-text items can load fast enough that
        // completeRequest was sometimes winning that race, leaving the host app's dimming overlay
        // stuck mid-teardown (confirmed live 2026-08-07: intermittent, not every time - exactly
        // the signature of a timing race, not a deterministic bug).
        guard !hasStartedExtraction else { return }
        hasStartedExtraction = true
        shareLogger.log("viewDidAppear - starting extraction")
        extractSharedText { [weak self] text in
            // `NSItemProvider.loadItem`'s completion handler fires on an arbitrary background
            // queue, not necessarily main (confirmed by Apple's own docs) - completeRequest and
            // any other extensionContext/UIKit work must hop back to main explicitly, or the
            // extension's dismissal can get stuck, leaving the host app (e.g. Google Maps)
            // dimmed and unresponsive underneath a presentation that never finishes tearing down
            // (confirmed live 2026-08-06: exactly this symptom, reported right after this gap
            // was still in place).
            DispatchQueue.main.async {
                shareLogger.log("extractSharedText completion: \(text ?? "nil", privacy: .public)")
                if let text {
                    let defaults = UserDefaults(suiteName: Self.appGroupSuiteName)
                    shareLogger.log(
                        "UserDefaults(suiteName: \(Self.appGroupSuiteName, privacy: .public)) = \(defaults == nil ? "NIL - app group entitlement missing/not provisioned" : "ok", privacy: .public)"
                    )
                    defaults?.set(text, forKey: Self.pendingShareTextKey)
                    let readback = defaults?.string(forKey: Self.pendingShareTextKey)
                    shareLogger.log("wrote pendingShareText, readback = \(readback ?? "nil", privacy: .public)")
                    self?.scheduleImportReadyNotification()
                }
                self?.extensionContext?.completeRequest(returningItems: nil)
            }
        }
    }

    private func scheduleImportReadyNotification() {
        let content = UNMutableNotificationContent()
        content.title = "Місце готове до імпорту"
        content.body = "Натисни, щоб додати його в Alongside"
        content.sound = .default
        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request)
    }

    private func extractSharedText(completion: @escaping (String?) -> Void) {
        shareLogger.log("inputItems count: \(self.extensionContext?.inputItems.count ?? -1, privacy: .public)")
        guard let item = extensionContext?.inputItems.first as? NSExtensionItem else {
            shareLogger.log("first inputItem is not an NSExtensionItem")
            completion(nil)
            return
        }
        let attachments = item.attachments ?? []
        shareLogger.log("attachments count: \(attachments.count, privacy: .public)")
        loadFirstNonEmptyText(from: attachments, index: 0, completion: completion)
    }

    // `NSExtensionItem.attachments` is an array, not a single provider - Maps (and other apps) can
    // hand over more than one representation per share, and the first one isn't guaranteed to be
    // the useful one (confirmed live 2026-08-06: attachment[0]'s plainText loaded as an empty
    // string, not nil - a "successful" load with no usable content). Walks every attachment,
    // preferring a URL representation over plain text, and skips empty results instead of trusting
    // the first thing that loads without an error.
    private func loadFirstNonEmptyText(
        from attachments: [NSItemProvider],
        index: Int,
        completion: @escaping (String?) -> Void
    ) {
        guard index < attachments.count else {
            shareLogger.log("no attachment yielded non-empty text")
            completion(nil)
            return
        }
        let provider = attachments[index]
        shareLogger.log(
            "attachment[\(index, privacy: .public)] registeredTypeIdentifiers: \(provider.registeredTypeIdentifiers, privacy: .public)"
        )

        let tryNext = { [weak self] in
            self?.loadFirstNonEmptyText(from: attachments, index: index + 1, completion: completion)
        }

        if provider.hasItemConformingToTypeIdentifier(UTType.url.identifier) {
            provider.loadItem(forTypeIdentifier: UTType.url.identifier) { result, error in
                let text = (result as? URL)?.absoluteString
                shareLogger.log(
                    "attachment[\(index, privacy: .public)] url: \(text ?? "nil", privacy: .public), error: \(String(describing: error), privacy: .public)"
                )
                if let text, !text.isEmpty {
                    completion(text)
                } else {
                    tryNext()
                }
            }
        } else if provider.hasItemConformingToTypeIdentifier(UTType.plainText.identifier) {
            provider.loadItem(forTypeIdentifier: UTType.plainText.identifier) { result, error in
                let text = result as? String
                shareLogger.log(
                    "attachment[\(index, privacy: .public)] plainText: \(text ?? "nil", privacy: .public), error: \(String(describing: error), privacy: .public)"
                )
                if let text, !text.isEmpty {
                    completion(text)
                } else {
                    tryNext()
                }
            }
        } else {
            shareLogger.log("attachment[\(index, privacy: .public)] matches neither url nor plainText UTType")
            tryNext()
        }
    }
}
