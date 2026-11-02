import WidgetKit
import SwiftUI
import AppIntents

// Must match HEYUM_APP_GROUP in WidgetDataStore.ios.kt — a widget runs as a separate binary and
// can only see the app's data through an App Group container.
private let appGroupId = "group.com.pgstorm148.heyum.widget"

// Keys written by WidgetDataStore.ios.kt.
private enum Keys {
    static let cravings = "widget.cravings"
    static let meals = "widget.meals"
    static let sessionCode = "widget.sessionCode"
    static let sessionName = "widget.sessionName"
    static let paneIndex = "widget.paneIndex"
}

// MARK: - Model

struct CravingItem: Identifiable {
    let id = UUID()
    let foodName: String
}

struct MealItem: Identifiable {
    let id = UUID()
    let dishName: String
    let mealType: String
}

/// The three panes, in cycle order. Mirrors WidgetPane in shared Kotlin.
enum Pane: Int, CaseIterable {
    case cravings = 0, meals = 1, session = 2

    var title: String {
        switch self {
        case .cravings: return "CRAVING LOG"
        case .meals: return "MEALS LOGGED"
        case .session: return "GROUP SESSION"
        }
    }
}

struct HeyumEntry: TimelineEntry {
    let date: Date
    let cravings: [CravingItem]
    let meals: [MealItem]
    let sessionCode: String?
    let sessionName: String?
    let pane: Pane

    static func placeholder() -> HeyumEntry {
        HeyumEntry(
            date: Date(),
            cravings: [CravingItem(foodName: "Butter chicken"), CravingItem(foodName: "Cold coffee")],
            meals: [MealItem(dishName: "Masala dosa", mealType: "BREAKFAST")],
            sessionCode: nil,
            sessionName: nil,
            pane: .cravings
        )
    }
}

// MARK: - Data loading

private func loadEntry() -> HeyumEntry {
    let defaults = UserDefaults(suiteName: appGroupId)

    let cravings = (defaults?.array(forKey: Keys.cravings) as? [[String: Any]] ?? [])
        .compactMap { dict -> CravingItem? in
            guard let name = dict["foodName"] as? String, !name.isEmpty else { return nil }
            return CravingItem(foodName: name)
        }

    let meals = (defaults?.array(forKey: Keys.meals) as? [[String: Any]] ?? [])
        .compactMap { dict -> MealItem? in
            guard let name = dict["dishName"] as? String, !name.isEmpty else { return nil }
            return MealItem(dishName: name, mealType: dict["mealType"] as? String ?? "")
        }

    let rawPane = defaults?.integer(forKey: Keys.paneIndex) ?? 0
    let pane = Pane(rawValue: ((rawPane % Pane.allCases.count) + Pane.allCases.count) % Pane.allCases.count) ?? .cravings

    return HeyumEntry(
        date: Date(),
        cravings: cravings,
        meals: meals,
        sessionCode: defaults?.string(forKey: Keys.sessionCode)?.nilIfBlank,
        sessionName: defaults?.string(forKey: Keys.sessionName)?.nilIfBlank,
        pane: pane
    )
}

private extension String {
    var nilIfBlank: String? { trimmingCharacters(in: .whitespaces).isEmpty ? nil : self }
}

// MARK: - Cycle intent

/// Advances the small widget to the next pane.
///
/// WidgetKit has no swipe or gesture support — widgets are static snapshots — so cycling is a
/// tappable button (iOS 17+ AppIntent), which is the closest interaction the platform allows.
struct CyclePaneIntent: AppIntent {
    static var title: LocalizedStringResource = "Next pane"

    func perform() async throws -> some IntentResult {
        let defaults = UserDefaults(suiteName: appGroupId)
        let next = ((defaults?.integer(forKey: Keys.paneIndex) ?? 0) + 1) % Pane.allCases.count
        defaults?.set(next, forKey: Keys.paneIndex)
        return .result()
    }
}

// MARK: - Provider

struct HeyumProvider: TimelineProvider {
    func placeholder(in context: Context) -> HeyumEntry { .placeholder() }

    func getSnapshot(in context: Context, completion: @escaping (HeyumEntry) -> Void) {
        completion(context.isPreview ? .placeholder() : loadEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<HeyumEntry>) -> Void) {
        // The app reloads timelines whenever it writes new data, so a slow refresh here is only a
        // backstop for the case where the app hasn't run in a while.
        let next = Calendar.current.date(byAdding: .minute, value: 30, to: Date()) ?? Date()
        completion(Timeline(entries: [loadEntry()], policy: .after(next)))
    }
}

// MARK: - Shared pieces

private let accent = Color(red: 1.0, green: 0.42, blue: 0.21)
private let bodyText = Color(red: 0.10, green: 0.10, blue: 0.10)
private let mutedText = Color(red: 0.46, green: 0.44, blue: 0.42)

private struct PaneHeader: View {
    let title: String
    var body: some View {
        Text(title)
            .font(.system(size: 10, weight: .bold))
            .foregroundStyle(accent)
    }
}

private struct EmptyHint: View {
    let message: String
    var body: some View {
        Text(message)
            .font(.system(size: 11))
            .foregroundStyle(mutedText)
            .lineLimit(2)
    }
}

private struct CravingList: View {
    let cravings: [CravingItem]
    let maxLines: Int
    var body: some View {
        if cravings.isEmpty {
            EmptyHint(message: "No cravings yet — tap to log one")
        } else {
            VStack(alignment: .leading, spacing: 3) {
                ForEach(cravings.prefix(maxLines)) { craving in
                    Text("• \(craving.foodName)")
                        .font(.system(size: 12))
                        .foregroundStyle(bodyText)
                        .lineLimit(1)
                }
            }
        }
    }
}

private struct MealList: View {
    let meals: [MealItem]
    let maxLines: Int
    var body: some View {
        if meals.isEmpty {
            EmptyHint(message: "Nothing logged today")
        } else {
            VStack(alignment: .leading, spacing: 3) {
                ForEach(meals.prefix(maxLines)) { meal in
                    VStack(alignment: .leading, spacing: 0) {
                        Text("• \(meal.dishName)")
                            .font(.system(size: 12))
                            .foregroundStyle(bodyText)
                            .lineLimit(1)
                        if !meal.mealType.isEmpty && meal.mealType != "ALL" {
                            Text(meal.mealType.lowercased())
                                .font(.system(size: 9))
                                .foregroundStyle(mutedText)
                        }
                    }
                }
            }
        }
    }
}

private struct SessionPane: View {
    let entry: HeyumEntry
    var body: some View {
        if let code = entry.sessionCode {
            VStack(alignment: .leading, spacing: 3) {
                Text(entry.sessionName ?? "Group session")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(bodyText)
                    .lineLimit(1)
                Text("Tap to join · \(code)")
                    .font(.system(size: 10))
                    .foregroundStyle(mutedText)
                    .lineLimit(1)
            }
        } else {
            EmptyHint(message: "No group session running")
        }
    }
}

/// Deep link opened when a pane is tapped, handled by the app's onOpenURL.
private func paneURL(_ pane: Pane, entry: HeyumEntry) -> URL? {
    switch pane {
    case .cravings: return URL(string: "heyum://widget/cravings")
    case .meals: return URL(string: "heyum://widget/meals")
    case .session:
        if let code = entry.sessionCode { return URL(string: "heyum://widget/session/\(code)") }
        return URL(string: "heyum://widget/cravings")
    }
}

// MARK: - Size-specific views

struct SmallWidgetView: View {
    let entry: HeyumEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                PaneHeader(title: entry.pane.title)
                Spacer()
                // Tap to cycle panes (no swipe support in WidgetKit).
                Button(intent: CyclePaneIntent()) {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(mutedText)
                }
                .buttonStyle(.plain)
            }

            switch entry.pane {
            case .cravings: CravingList(cravings: entry.cravings, maxLines: 3)
            case .meals: MealList(meals: entry.meals, maxLines: 3)
            case .session: SessionPane(entry: entry)
            }

            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .widgetURL(paneURL(entry.pane, entry: entry))
    }
}

struct MediumWidgetView: View {
    let entry: HeyumEntry
    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            VStack(alignment: .leading, spacing: 6) {
                PaneHeader(title: Pane.cravings.title)
                CravingList(cravings: entry.cravings, maxLines: 3)
                Spacer(minLength: 0)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            VStack(alignment: .leading, spacing: 6) {
                PaneHeader(title: Pane.meals.title)
                MealList(meals: entry.meals, maxLines: 3)
                Spacer(minLength: 0)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .widgetURL(paneURL(.cravings, entry: entry))
    }
}

struct LargeWidgetView: View {
    let entry: HeyumEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 5) {
                PaneHeader(title: Pane.cravings.title)
                CravingList(cravings: entry.cravings, maxLines: 3)
            }

            VStack(alignment: .leading, spacing: 5) {
                PaneHeader(title: Pane.meals.title)
                MealList(meals: entry.meals, maxLines: 3)
            }

            VStack(alignment: .leading, spacing: 5) {
                PaneHeader(title: Pane.session.title)
                SessionPane(entry: entry)
            }

            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        // The large layout leads with the session so the join link is the primary tap target.
        .widgetURL(paneURL(.session, entry: entry))
    }
}

// MARK: - Widget entry point

struct HeyumWidgetEntryView: View {
    @Environment(\.widgetFamily) var family
    let entry: HeyumEntry

    var body: some View {
        Group {
            switch family {
            case .systemSmall: SmallWidgetView(entry: entry)
            case .systemLarge, .systemExtraLarge: LargeWidgetView(entry: entry)
            default: MediumWidgetView(entry: entry)
            }
        }
        .containerBackground(Color(red: 1.0, green: 0.97, blue: 0.94), for: .widget)
    }
}

struct HeyumWidget: Widget {
    let kind: String = "HeyumWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: HeyumProvider()) { entry in
            HeyumWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("heyum")
        .description("Your craving log, logged meals, and live group session.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

@main
struct HeyumWidgetBundle: WidgetBundle {
    var body: some Widget {
        HeyumWidget()
    }
}
