import SwiftUI
import YumShared

struct ComposeView: UIViewControllerRepresentable {
    let payloadType: String?
    let payloadId: String?

    func makeUIViewController(context: Context) -> UIViewController {
        return MainViewControllerKt.MainViewController(payloadType: payloadType, payloadId: payloadId)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var payloadType: String?
    @State private var payloadId: String?
    // Compose reads its payload once at composition, so a new deep link has to rebuild the
    // controller. Changing this id via .id() is what forces that rebuild.
    @State private var reloadToken = UUID()

    var body: some View {
        ComposeView(payloadType: payloadType, payloadId: payloadId)
            .id(reloadToken)
            .ignoresSafeArea(.keyboard)
            .onOpenURL { url in
                guard let route = WidgetRoute(url: url) else { return }
                payloadType = route.payloadType
                payloadId = route.payloadId
                reloadToken = UUID()
            }
    }
}

/// Parses the heyum://widget/... links emitted by the home-screen widget.
struct WidgetRoute {
    let payloadType: String
    let payloadId: String?

    init?(url: URL) {
        guard url.scheme == "heyum", url.host == "widget" else { return nil }
        let parts = url.pathComponents.filter { $0 != "/" }
        guard let first = parts.first else { return nil }
        switch first {
        case "cravings":
            payloadType = "craving_log"
            payloadId = nil
        case "meals":
            payloadType = "meal_log"
            payloadId = nil
        case "session":
            payloadType = "group"
            payloadId = parts.count > 1 ? parts[1] : nil
        default:
            return nil
        }
    }
}
