//
//  ProfileViewModel.swift
//  ATC
//
//  Created by RAVI KANDARPA on 12/11/22.
//

import Foundation

class ProfileViewModel: ObservableObject {
    @Published var profiles: [ProfileModel] = []
    
    init() {
        getItems()
    }
    func getItems() {
        let newProfiles = [
            ProfileModel(name: "John", pressure: 5),
            ProfileModel(name: "Sam", pressure: 4),
            ProfileModel(name: "Max", pressure: 3.2),
        ]
        profiles.append(contentsOf: newProfiles)
    }
    func deleteProfile(indexSet: IndexSet) {
        profiles.remove(atOffsets: indexSet)
    }
    func moveProfile(from: IndexSet, to: Int) {
        profiles.move(fromOffsets: from, toOffset: to)
    }
    func addProfile(name: String){
        let newProfile = ProfileModel(name: name, pressure: 5)
        profiles.append(newProfile)
    }
}
