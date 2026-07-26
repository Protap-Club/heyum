import SwiftUI
import UIKit
import UserNotifications
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore
import YumShared

class SwiftAuthDelegate: NSObject, IosAuthDelegate {
    func watchAuthState(onUserChanged: @escaping (String?, String?) -> Void) {
        Auth.auth().addStateDidChangeListener { (_, user) in
            onUserChanged(user?.uid, user?.email)
        }
    }

    func verifyPhoneNumber(phoneNumber: String, onCodeSent: @escaping (String) -> Void, onFailed: @escaping (String) -> Void) {
        PhoneAuthProvider.provider().verifyPhoneNumber(phoneNumber, uiDelegate: nil) { verificationID, error in
            if let error = error {
                onFailed(error.localizedDescription)
            } else if let verificationID = verificationID {
                onCodeSent(verificationID)
            } else {
                onFailed("Couldn't send the verification code. Please try again.")
            }
        }
    }

    func signInWithCode(verificationId: String, verificationCode: String, completion: @escaping (String?, String?, String?) -> Void) {
        let credential = PhoneAuthProvider.provider().credential(
            withVerificationID: verificationId,
            verificationCode: verificationCode
        )
        Auth.auth().signIn(with: credential) { (result, error) in
            if let error = error {
                completion(nil, nil, error.localizedDescription)
            } else if let user = result?.user {
                completion(user.uid, user.email, nil)
            } else {
                completion(nil, nil, "Unknown verification error")
            }
        }
    }

    func signIn(email: String, password: String, completion: @escaping (String?, String?, String?) -> Void) {
        Auth.auth().signIn(withEmail: email, password: password) { (result, error) in
            if let error = error {
                completion(nil, nil, error.localizedDescription)
            } else if let user = result?.user {
                completion(user.uid, user.email, nil)
            } else {
                completion(nil, nil, "Unknown login error")
            }
        }
    }
    
    func signUp(email: String, password: String, completion: @escaping (String?, String?, String?) -> Void) {
        Auth.auth().createUser(withEmail: email, password: password) { (result, error) in
            if let error = error {
                completion(nil, nil, error.localizedDescription)
            } else if let user = result?.user {
                completion(user.uid, user.email, nil)
            } else {
                completion(nil, nil, "Unknown signup error")
            }
        }
    }
    
    func signOut(completion: @escaping (String?) -> Void) {
        do {
            try Auth.auth().signOut()
            completion(nil)
        } catch let error {
            completion(error.localizedDescription)
        }
    }
}

class SwiftFirestoreDelegate: NSObject, IosFirestoreDelegate {
    func observeCravings(userId: String, onUpdate: @escaping ([Craving]?, String?) -> Void) -> () -> Void {
        let query = Firestore.firestore().collection("cravings")
            .whereField("userId", isEqualTo: userId)
            .order(by: "timestamp", descending: true)
            
        let listener = query.addSnapshotListener { (snapshot, error) in
            if let error = error {
                onUpdate(nil, error.localizedDescription)
                return
            }
            guard let documents = snapshot?.documents else {
                onUpdate([], nil)
                return
            }
            
            let cravings: [Craving] = documents.map { doc in
                let data = doc.data()
                return Craving(
                    id: doc.documentID,
                    userId: data["userId"] as? String ?? "",
                    foodName: data["foodName"] as? String ?? "",
                    mood: data["mood"] as? String ?? "",
                    timestamp: data["timestamp"] as? Int64 ?? 0,
                    notes: data["notes"] as? String ?? ""
                )
            }
            onUpdate(cravings, nil)
        }
        
        return {
            listener.remove()
        }
    }
    
    func saveCraving(id: String, userId: String, foodName: String, mood: String, timestamp: Int64, notes: String, completion: @escaping (String?) -> Void) {
        let db = Firestore.firestore()
        let data: [String: Any] = [
            "userId": userId,
            "foodName": foodName,
            "mood": mood,
            "timestamp": timestamp,
            "notes": notes
        ]
        
        if !id.isEmpty {
            db.collection("cravings").document(id).setData(data) { error in
                completion(error?.localizedDescription)
            }
        } else {
            let _ = db.collection("cravings").addDocument(data: data) { error in
                completion(error?.localizedDescription)
            }
        }
    }
    
    func deleteCraving(id: String, completion: @escaping (String?) -> Void) {
        Firestore.firestore().collection("cravings").document(id).delete { error in
            completion(error?.localizedDescription)
        }
    }
}

// Mirrors AndroidUserRepository's Firestore shape exactly (collection "users", document id =
// userId). Without this, IosUserRepository silently no-op'd every write (including
// onboardingCompleted), so the app could never remember that onboarding was finished — it asked
// for preferences again on every cold launch.
class SwiftUserRepositoryDelegate: NSObject, IosUserRepositoryDelegate {
    private let db = Firestore.firestore()

    private func cuisine(from raw: String?) -> Cuisine {
        guard let raw = raw else { return .comfort }
        return Cuisine.entries.first(where: { $0.name == raw }) ?? .comfort
    }
    private func timeOfDay(from raw: String?) -> TimeOfDay {
        guard let raw = raw else { return .morning }
        return TimeOfDay.entries.first(where: { $0.name == raw }) ?? .morning
    }
    private func mood(from raw: String?) -> Mood? {
        guard let raw = raw else { return nil }
        return Mood.entries.first(where: { $0.name == raw })
    }
    private func dietaryRestriction(from raw: String) -> DietaryRestriction? {
        return DietaryRestriction.entries.first(where: { $0.name == raw })
    }
    private func notificationFrequency(from raw: String?) -> NotificationFrequency {
        guard let raw = raw else { return .standard }
        return NotificationFrequency.entries.first(where: { $0.name == raw }) ?? .standard
    }

    private func contextFromDict(_ dict: [String: Any]) -> MealContext {
        return MealContext(
            timeOfDay: timeOfDay(from: dict["timeOfDay"] as? String),
            weather: dict["weather"] as? String ?? "",
            mood: mood(from: dict["mood"] as? String),
            groupSize: Int32((dict["groupSize"] as? NSNumber)?.intValue ?? 1),
            availableIngredients: dict["availableIngredients"] as? [String],
            hadHeavyLunch: dict["hadHeavyLunch"] as? Bool ?? false,
            todayCuisines: dict["todayCuisines"] as? [String] ?? [],
            todayDishIds: dict["todayDishIds"] as? [String] ?? [],
            mealTypeStr: dict["mealTypeStr"] as? String ?? "ALL",
            rejectedDishIds: dict["rejectedDishIds"] as? [String] ?? []
        )
    }

    private func contextToDict(_ context: MealContext) -> [String: Any] {
        var map: [String: Any] = [
            "timeOfDay": context.timeOfDay.name,
            "weather": context.weather,
            "groupSize": Int(context.groupSize)
        ]
        if let mood = context.mood {
            map["mood"] = mood.name
        }
        if let ingredients = context.availableIngredients {
            map["availableIngredients"] = ingredients
        }
        return map
    }

    private func mealHistoryFromDict(_ dict: [String: Any]) -> MealHistoryEntry? {
        guard let contextDict = dict["context"] as? [String: Any] else { return nil }
        return MealHistoryEntry(
            id: dict["id"] as? String ?? "",
            dishName: dict["dishName"] as? String ?? "",
            cuisine: cuisine(from: dict["cuisine"] as? String),
            accepted: dict["accepted"] as? Bool ?? true,
            timestamp: (dict["timestamp"] as? NSNumber)?.int64Value ?? 0,
            context: contextFromDict(contextDict)
        )
    }

    private func mealHistoryToDict(_ entry: MealHistoryEntry) -> [String: Any] {
        return [
            "id": entry.id,
            "dishName": entry.dishName,
            "cuisine": entry.cuisine.name,
            "accepted": entry.accepted,
            "timestamp": entry.timestamp,
            "context": contextToDict(entry.context)
        ]
    }

    private func profileFromDocument(_ doc: DocumentSnapshot) -> UserProfile? {
        guard let data = doc.data() else { return nil }
        let dietaryRestrictions = (data["dietaryRestrictions"] as? [String] ?? []).compactMap { dietaryRestriction(from: $0) }
        let favoriteCuisines = (data["favoriteCuisines"] as? [String] ?? []).map { cuisine(from: $0) }
        let mealHistory = (data["mealHistory"] as? [[String: Any]] ?? []).compactMap { mealHistoryFromDict($0) }
        var tasteVector: [String: KotlinFloat] = [:]
        if let rawVector = data["tasteVector"] as? [String: Any] {
            for (key, value) in rawVector {
                if let num = value as? NSNumber {
                    tasteVector[key] = KotlinFloat(float: num.floatValue)
                }
            }
        }

        return UserProfile(
            id: doc.documentID,
            name: data["name"] as? String ?? "",
            dietaryRestrictions: dietaryRestrictions,
            spiceTolerance: Int32((data["spiceTolerance"] as? NSNumber)?.intValue ?? 3),
            favoriteCuisines: favoriteCuisines,
            dislikedIngredients: data["dislikedIngredients"] as? [String] ?? [],
            mealHistory: mealHistory,
            tasteVector: tasteVector,
            onboardingCompleted: data["onboardingCompleted"] as? Bool ?? false,
            notificationFrequency: notificationFrequency(from: data["notificationFrequency"] as? String),
            quietHoursStart: data["quietHoursStart"] as? String ?? "23:00",
            quietHoursEnd: data["quietHoursEnd"] as? String ?? "08:00",
            quietHoursEnabled: data["quietHoursEnabled"] as? Bool ?? false,
            mealRemindersEnabled: data["mealRemindersEnabled"] as? Bool ?? true,
            reorderSuggestionsEnabled: data["reorderSuggestionsEnabled"] as? Bool ?? true,
            wasteAlertsEnabled: data["wasteAlertsEnabled"] as? Bool ?? true,
            groupInvitesEnabled: data["groupInvitesEnabled"] as? Bool ?? true
        )
    }

    private func profileToDict(_ profile: UserProfile) -> [String: Any] {
        var tasteVectorDict: [String: Float] = [:]
        for (key, value) in profile.tasteVector {
            tasteVectorDict[key] = value.floatValue
        }
        return [
            "name": profile.name,
            "dietaryRestrictions": profile.dietaryRestrictions.map { $0.name },
            "spiceTolerance": Int(profile.spiceTolerance),
            "favoriteCuisines": profile.favoriteCuisines.map { $0.name },
            "dislikedIngredients": profile.dislikedIngredients,
            "mealHistory": profile.mealHistory.map { mealHistoryToDict($0) },
            "tasteVector": tasteVectorDict,
            "onboardingCompleted": profile.onboardingCompleted,
            "notificationFrequency": profile.notificationFrequency.name,
            "quietHoursStart": profile.quietHoursStart,
            "quietHoursEnd": profile.quietHoursEnd,
            "quietHoursEnabled": profile.quietHoursEnabled,
            "mealRemindersEnabled": profile.mealRemindersEnabled,
            "reorderSuggestionsEnabled": profile.reorderSuggestionsEnabled,
            "wasteAlertsEnabled": profile.wasteAlertsEnabled,
            "groupInvitesEnabled": profile.groupInvitesEnabled
        ]
    }

    func getUserProfile(userId: String, onUpdate: @escaping (UserProfile?, String?) -> Void) -> () -> Void {
        let ref = db.collection("users").document(userId)
        let listener = ref.addSnapshotListener { doc, error in
            if let error = error {
                onUpdate(nil, error.localizedDescription)
                return
            }
            guard let doc = doc, doc.exists else {
                onUpdate(nil, nil)
                return
            }
            onUpdate(self.profileFromDocument(doc), nil)
        }
        return {
            listener.remove()
        }
    }

    func saveUserProfile(profile: UserProfile, completion: @escaping (String?) -> Void) {
        db.collection("users").document(profile.id).setData(profileToDict(profile)) { error in
            completion(error?.localizedDescription)
        }
    }

    func updateMealHistory(userId: String, entry: MealHistoryEntry, completion: @escaping (String?) -> Void) {
        db.collection("users").document(userId).updateData([
            "mealHistory": FieldValue.arrayUnion([mealHistoryToDict(entry)])
        ]) { error in
            completion(error?.localizedDescription)
        }
    }

    func updateTasteVector(userId: String, tasteVector: [String: KotlinFloat], completion: @escaping (String?) -> Void) {
        var dict: [String: Float] = [:]
        for (key, value) in tasteVector {
            dict[key] = value.floatValue
        }
        db.collection("users").document(userId).updateData([
            "tasteVector": dict
        ]) { error in
            completion(error?.localizedDescription)
        }
    }
}

// Mirrors AndroidDishRepository's Firestore shape exactly (collection "dishes", optional cuisine
// filter). Without this, IosDishRepository always returned an empty list on iOS — invisible on
// the main swipe screen (which falls back to a local dish pool via SuggestionEngineClient), but
// GroupSessionScreen calls this repository directly with no such fallback, so a group could never
// find a match: every member submitting kept it stuck on the selection screen forever.
class SwiftDishRepositoryDelegate: NSObject, IosDishRepositoryDelegate {
    private let db = Firestore.firestore()

    private func cuisine(from raw: String?) -> Cuisine {
        guard let raw = raw else { return .comfort }
        return Cuisine.entries.first(where: { $0.name == raw }) ?? .comfort
    }

    private func difficulty(from raw: String?) -> Difficulty {
        guard let raw = raw else { return .medium }
        return Difficulty.entries.first(where: { $0.name == raw }) ?? .medium
    }

    private func dietaryTag(from raw: String) -> DietaryRestriction? {
        return DietaryRestriction.entries.first(where: { $0.name == raw })
    }

    private func dishFromDocument(_ doc: QueryDocumentSnapshot) -> Dish {
        let data = doc.data()
        return Dish(
            id: doc.documentID,
            name: data["name"] as? String ?? "",
            cuisine: cuisine(from: data["cuisine"] as? String),
            description: data["description"] as? String ?? "",
            imageUrl: data["imageUrl"] as? String ?? "",
            spiceLevel: Int32((data["spiceLevel"] as? NSNumber)?.intValue ?? 3),
            prepTimeMinutes: Int32((data["prepTimeMinutes"] as? NSNumber)?.intValue ?? 30),
            difficulty: difficulty(from: data["difficulty"] as? String),
            ingredients: data["ingredients"] as? [String] ?? [],
            instructions: data["instructions"] as? [String] ?? [],
            dietaryTags: (data["dietaryTags"] as? [String] ?? []).compactMap { dietaryTag(from: $0) },
            occasions: data["occasions"] as? [String] ?? [],
            orderingAvailable: data["orderingAvailable"] as? Bool ?? false,
            orderingUrl: data["orderingUrl"] as? String,
            mealType: data["mealType"] as? [String] ?? []
        )
    }

    private func dishToDict(_ dish: Dish) -> [String: Any] {
        var map: [String: Any] = [
            "id": dish.id,
            "name": dish.name,
            "cuisine": dish.cuisine.name,
            "description": dish.description_,
            "imageUrl": dish.imageUrl,
            "spiceLevel": Int(dish.spiceLevel),
            "prepTimeMinutes": Int(dish.prepTimeMinutes),
            "difficulty": dish.difficulty.name,
            "ingredients": dish.ingredients,
            "instructions": dish.instructions,
            "dietaryTags": dish.dietaryTags.map { $0.name },
            "occasions": dish.occasions,
            "orderingAvailable": dish.orderingAvailable,
            "mealType": dish.mealType
        ]
        if let url = dish.orderingUrl {
            map["orderingUrl"] = url
        }
        return map
    }

    func getDishesByContext(cuisine: Cuisine?, timeOfDay: TimeOfDay, weather: String,
                            onUpdate: @escaping ([Dish]?, String?) -> Void) -> () -> Void {
        var query: Query = db.collection("dishes")
        if let cuisine = cuisine {
            query = query.whereField("cuisine", isEqualTo: cuisine.name)
        }
        let listener = query.addSnapshotListener { snapshot, error in
            if let error = error {
                onUpdate([], error.localizedDescription)
                return
            }
            let dishes = (snapshot?.documents ?? []).map { self.dishFromDocument($0) }
            onUpdate(dishes, nil)
        }
        return {
            listener.remove()
        }
    }

    func saveDish(dish: Dish, completion: @escaping (String?) -> Void) {
        db.collection("dishes").document(dish.id).setData(dishToDict(dish)) { error in
            completion(error?.localizedDescription)
        }
    }
}

// Mirrors AndroidGroupSessionRepository's Firestore shape exactly (collection "sessions", looked
// up by the "joinCode" field, members stored as an array, per-user selections as dot-path map
// updates) so a join code works interchangeably whether the host is on Android or iOS.
class SwiftGroupSessionDelegate: NSObject, IosGroupSessionRepositoryDelegate {
    private let db = Firestore.firestore()

    private func memberToDict(_ member: GroupMember) -> [String: Any] {
        return [
            "userId": member.userId,
            "name": member.name,
            "tasteProfile": member.tasteProfile,
            "joinedAt": member.joinedAt,
            "isOrganizer": member.isOrganizer,
            "dietaryRestrictions": member.dietaryRestrictions,
            "spiceTolerance": Int(member.spiceTolerance),
            "favoriteCuisines": member.favoriteCuisines
        ]
    }

    private func memberFromDict(_ dict: [String: Any]) -> GroupMember {
        return GroupMember(
            userId: dict["userId"] as? String ?? "",
            name: dict["name"] as? String ?? "",
            tasteProfile: dict["tasteProfile"] as? String ?? "",
            joinedAt: (dict["joinedAt"] as? NSNumber)?.int64Value ?? 0,
            isOrganizer: dict["isOrganizer"] as? Bool ?? false,
            dietaryRestrictions: dict["dietaryRestrictions"] as? [String] ?? [],
            spiceTolerance: Int32((dict["spiceTolerance"] as? NSNumber)?.intValue ?? 3),
            favoriteCuisines: dict["favoriteCuisines"] as? [String] ?? []
        )
    }

    private func sessionToDict(_ session: GroupSession) -> [String: Any] {
        return [
            "id": session.id,
            "organizerId": session.organizerId,
            "organizerName": session.organizerName,
            "sessionName": session.sessionName,
            "createdAt": session.createdAt,
            "mealType": session.mealType,
            "members": session.members.map { memberToDict($0) },
            "selectedCuisines": session.selectedCuisines,
            "selectedDishes": session.selectedDishes,
            "status": session.status,
            "finalDish": session.finalDish,
            "joinCode": session.joinCode
        ]
    }

    private func sessionFromDocument(_ doc: DocumentSnapshot) -> GroupSession? {
        guard let data = doc.data() else { return nil }
        let members = (data["members"] as? [[String: Any]] ?? []).map { memberFromDict($0) }
        return GroupSession(
            id: doc.documentID,
            organizerId: data["organizerId"] as? String ?? "",
            organizerName: data["organizerName"] as? String ?? "",
            sessionName: data["sessionName"] as? String ?? "",
            createdAt: (data["createdAt"] as? NSNumber)?.int64Value ?? 0,
            mealType: data["mealType"] as? String ?? "LUNCH",
            members: members,
            selectedCuisines: data["selectedCuisines"] as? [String: String] ?? [:],
            selectedDishes: data["selectedDishes"] as? [String: String] ?? [:],
            status: data["status"] as? String ?? "WAITING",
            finalDish: data["finalDish"] as? String ?? "",
            joinCode: data["joinCode"] as? String ?? ""
        )
    }

    private func updateFirstMatch(code: String, data: [String: Any], completion: @escaping (String?) -> Void) {
        db.collection("sessions").whereField("joinCode", isEqualTo: code).getDocuments { snapshot, error in
            if let error = error {
                completion(error.localizedDescription)
                return
            }
            guard let doc = snapshot?.documents.first else {
                completion(nil)
                return
            }
            doc.reference.updateData(data) { error in
                completion(error?.localizedDescription)
            }
        }
    }

    func observeSession(code: String, onUpdate: @escaping (GroupSession?, String?) -> Void) -> () -> Void {
        let query = db.collection("sessions").whereField("joinCode", isEqualTo: code)
        let listener = query.addSnapshotListener { snapshot, error in
            if let error = error {
                onUpdate(nil, error.localizedDescription)
                return
            }
            guard let doc = snapshot?.documents.first else {
                onUpdate(nil, nil)
                return
            }
            onUpdate(self.sessionFromDocument(doc), nil)
        }
        return {
            listener.remove()
        }
    }

    func createSession(session: GroupSession, completion: @escaping (String?) -> Void) {
        db.collection("sessions").document(session.id).setData(sessionToDict(session)) { error in
            completion(error?.localizedDescription)
        }
    }

    func joinSession(code: String, member: GroupMember, completion: @escaping (String?) -> Void) {
        db.collection("sessions").whereField("joinCode", isEqualTo: code).getDocuments { snapshot, error in
            if let error = error {
                completion(error.localizedDescription)
                return
            }
            guard let doc = snapshot?.documents.first else {
                completion("Couldn't find a session with this code.")
                return
            }
            let existingMembers = (doc.data()["members"] as? [[String: Any]] ?? []).map { self.memberFromDict($0) }
            if existingMembers.contains(where: { $0.userId == member.userId }) {
                completion(nil)
                return
            }
            doc.reference.updateData([
                "members": FieldValue.arrayUnion([self.memberToDict(member)])
            ]) { error in
                completion(error?.localizedDescription)
            }
        }
    }

    func makeDecision(code: String, dishId: String, completion: @escaping (String?) -> Void) {
        updateFirstMatch(code: code, data: ["finalDish": dishId, "status": "COMPLETED"], completion: completion)
    }

    func updateSelection(code: String, userId: String, cuisine: String, dish: String, completion: @escaping (String?) -> Void) {
        updateFirstMatch(code: code, data: [
            "selectedCuisines.\(userId)": cuisine,
            "selectedDishes.\(userId)": dish
        ], completion: completion)
    }

    func startCollaboration(code: String, completion: @escaping (String?) -> Void) {
        updateFirstMatch(code: code, data: ["status": "ACTIVE"], completion: completion)
    }

    func endSession(code: String, completion: @escaping (String?) -> Void) {
        updateFirstMatch(code: code, data: ["status": "ENDED"], completion: completion)
    }

    func leaveSession(code: String, userId: String, completion: @escaping (String?) -> Void) {
        db.collection("sessions").whereField("joinCode", isEqualTo: code).getDocuments { snapshot, error in
            if let error = error {
                completion(error.localizedDescription)
                return
            }
            guard let doc = snapshot?.documents.first, let session = self.sessionFromDocument(doc) else {
                completion(nil)
                return
            }
            let remaining = session.members.filter { $0.userId != userId }.map { self.memberToDict($0) }
            doc.reference.updateData(["members": remaining]) { error in
                completion(error?.localizedDescription)
            }
        }
    }
}

private enum MealSlot: CaseIterable {
    case breakfast, brunch, lunch, eveningSnacks, dinner, lateNightSnacks

    var identifier: String {
        switch self {
        case .breakfast: return "heyum.meal.breakfast"
        case .brunch: return "heyum.meal.brunch"
        case .lunch: return "heyum.meal.lunch"
        case .eveningSnacks: return "heyum.meal.eveningSnacks"
        case .dinner: return "heyum.meal.dinner"
        case .lateNightSnacks: return "heyum.meal.lateNightSnacks"
        }
    }

    // Matches MealNotificationWorker's meal windows exactly (app/src/main/kotlin/.../notifications/).
    var hour: Int {
        switch self {
        case .breakfast: return 9
        case .brunch: return 11
        case .lunch: return 13
        case .eveningSnacks: return 16
        case .dinner: return 19
        case .lateNightSnacks: return 1
        }
    }

    var minute: Int {
        switch self {
        case .breakfast: return 0
        default: return 30
        }
    }

    var label: String {
        switch self {
        case .breakfast: return "breakfast"
        case .brunch: return "brunch"
        case .lunch: return "lunch"
        case .eveningSnacks: return "evening snacks"
        case .dinner: return "dinner"
        case .lateNightSnacks: return "late-night snacks"
        }
    }

    var popularMeals: [String] {
        switch self {
        case .breakfast: return ["Pancakes", "Oatmeal", "Eggs", "Toast", "Cereal"]
        case .brunch: return ["Avocado Toast", "Waffle", "Benedict Eggs", "French Toast"]
        case .lunch: return ["Pasta", "Sandwich", "Salad", "Rice Bowl", "Burger"]
        case .eveningSnacks: return ["Samosa", "Cookies", "Cake", "Chips", "Fruit"]
        case .dinner: return ["Biryani", "Dal Makhani", "Roti", "Curry", "Steak"]
        case .lateNightSnacks: return ["Maggi", "Popcorn", "Nuts", "Yogurt", "Ice Cream"]
        }
    }

    var favoriteMeals: [String] {
        switch self {
        case .breakfast: return ["Idli", "Dosa", "Paratha"]
        case .brunch: return ["Chilla", "Upma", "Dhokla"]
        case .lunch: return ["Biryani", "Butter Chicken", "Tandoori"]
        case .eveningSnacks: return ["Chai & Pakora", "Tea & Biscuits"]
        case .dinner: return ["Paneer Tikka", "Naan", "Raita"]
        case .lateNightSnacks: return ["Chai", "Cha Paratha"]
        }
    }

    var titleOptions: [String] {
        switch self {
        case .breakfast: return ["☀️ Rise & dine", "Good morning, hungry?"]
        case .brunch: return ["🥂 Brunch o'clock", "Lazy brunch vibes?"]
        case .lunch: return ["🍴 Lunch, decided", "Midday fuel time"]
        case .eveningSnacks: return ["🫖 Snack o'clock", "Chai & something?"]
        case .dinner: return ["🌆 Dinner, sorted", "What's for dinner?"]
        case .lateNightSnacks: return ["🌙 Late-night cravings", "Midnight munchies?"]
        }
    }
}

// Mirrors Android's MealNotificationWorker, which runs as an hourly WorkManager job checking the
// current time window and firing a notification. iOS has no background-polling primitive
// available without extra entitlements, so this schedules native repeating local notifications
// instead — one per meal window, firing daily via UNCalendarNotificationTrigger even while the
// app stays closed. Content (which dish to suggest) is refreshed each time the app comes to the
// foreground, matching Android's recommendation mix: recent cravings + popular + favorite dishes.
private enum MealNotificationScheduler {
    static func requestAuthorization() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
    }

    static func rescheduleAll() {
        guard let userId = Auth.auth().currentUser?.uid else { return }
        fetchPastCravings(userId: userId) { pastCravings in
            let center = UNUserNotificationCenter.current()
            center.removePendingNotificationRequests(withIdentifiers: MealSlot.allCases.map { $0.identifier })
            for slot in MealSlot.allCases {
                schedule(slot: slot, pastCravings: pastCravings, center: center)
            }
        }
    }

    private static func fetchPastCravings(userId: String, completion: @escaping ([String]) -> Void) {
        Firestore.firestore().collection("cravings")
            .whereField("userId", isEqualTo: userId)
            .order(by: "timestamp", descending: true)
            .limit(to: 20)
            .getDocuments { snapshot, _ in
                let names = (snapshot?.documents ?? []).compactMap { $0.data()["foodName"] as? String }
                var seen = Set<String>()
                completion(names.filter { seen.insert($0).inserted })
            }
    }

    private static func recommendation(for slot: MealSlot, pastCravings: [String]) -> String {
        var pool: [String] = []
        pastCravings.randomElement().map { pool.append($0) }
        slot.popularMeals.randomElement().map { pool.append($0) }
        slot.favoriteMeals.randomElement().map { pool.append($0) }
        return pool.randomElement() ?? slot.popularMeals.randomElement() ?? slot.label
    }

    private static func schedule(slot: MealSlot, pastCravings: [String], center: UNUserNotificationCenter) {
        let dish = recommendation(for: slot, pastCravings: pastCravings)
        let content = UNMutableNotificationContent()
        content.title = slot.titleOptions.randomElement() ?? "Hungry?"
        content.body = messageBody(slot: slot, dish: dish)
        content.sound = .default

        var dateComponents = DateComponents()
        dateComponents.hour = slot.hour
        dateComponents.minute = slot.minute
        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)

        center.add(UNNotificationRequest(identifier: slot.identifier, content: content, trigger: trigger))
    }

    private static func messageBody(slot: MealSlot, dish: String) -> String {
        let templates = [
            "Craving \(dish)? Tap to lock it in 😋",
            "\(dish) for \(slot.label)? One swipe and you're sorted.",
            "How about \(dish) right now? 🍽️",
            "\(dish) is calling your name — yum or nah?",
            "Can't decide on \(slot.label)? We're feeling \(dish) today."
        ]
        return templates.randomElement() ?? "Time for \(slot.label)!"
    }
}

// Firebase phone auth needs a UIApplicationDelegate to forward remote notifications to
// Auth.canHandleNotification. A pure SwiftUI app has none, which is why verification failed the
// "notification forwarding" self-check. This delegate provides that bridge (and the APNs token +
// reCAPTCHA URL callback for real-device auth later).
class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        // Configure Firebase HERE (not in the SwiftUI App.init) so it runs after this delegate is
        // installed. That's what lets Firebase's notification swizzling hook the delegate, which
        // phone-number auth's forwarding self-check depends on.
        FirebaseApp.configure()

        // Test-number sign-in without APNs/reCAPTCHA. DEBUG-only, never ships in Release.
        #if DEBUG
        let authSettings = AuthSettings()
        authSettings.isAppVerificationDisabledForTesting = true
        Auth.auth().settings = authSettings
        #endif

        // Start Koin and inject the Swift Firebase delegates into the KMP bridge.
        KoinKt.doInitKoin()
        IosFirebaseBridge.shared.authDelegate = SwiftAuthDelegate()
        IosFirebaseBridge.shared.firestoreDelegate = SwiftFirestoreDelegate()
        IosFirebaseBridge.shared.dishRepositoryDelegate = SwiftDishRepositoryDelegate()
        IosFirebaseBridge.shared.userRepositoryDelegate = SwiftUserRepositoryDelegate()
        IosFirebaseBridge.shared.groupSessionRepositoryDelegate = SwiftGroupSessionDelegate()

        MealNotificationScheduler.requestAuthorization()
        // didFinishLaunching can run before Firebase restores the signed-in user, in which case
        // rescheduleAll() no-ops (no uid yet) — this listener catches that case as soon as auth
        // state resolves, instead of waiting for the next foreground.
        Auth.auth().addStateDidChangeListener { _, user in
            if user != nil {
                MealNotificationScheduler.rescheduleAll()
            }
        }
        return true
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        MealNotificationScheduler.rescheduleAll()
    }

    func application(_ application: UIApplication,
                     didReceiveRemoteNotification userInfo: [AnyHashable: Any],
                     fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        if Auth.auth().canHandleNotification(userInfo) {
            completionHandler(.noData)
            return
        }
        completionHandler(.newData)
    }

    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Auth.auth().setAPNSToken(deviceToken, type: .unknown)
    }

    func application(_ app: UIApplication, open url: URL,
                     options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool {
        return Auth.auth().canHandle(url)
    }
}

@main
struct iosApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
