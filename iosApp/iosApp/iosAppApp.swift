import SwiftUI
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

@main
struct iosApp: App {
    init() {
        FirebaseApp.configure()
        
        // Start Koin
        KoinKt.doInitKoin()
        
        // Inject Swift Delegates to KMP bridge
        IosFirebaseBridge.shared.authDelegate = SwiftAuthDelegate()
        IosFirebaseBridge.shared.firestoreDelegate = SwiftFirestoreDelegate()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
