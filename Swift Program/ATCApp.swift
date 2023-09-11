//
//  ATCApp.swift
//  ATC
//
//  Created by RAVI KANDARPA on 10/5/22.
//

import SwiftUI

@main
struct ATCApp: App {
    @StateObject var profViewModel: ProfileViewModel = ProfileViewModel()
    var body: some Scene {
        WindowGroup {
            NavigationView {
                ContentView()
            }
            .environmentObject(profViewModel)
        }
    }
}
