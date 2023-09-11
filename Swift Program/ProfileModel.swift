//
//  ProfileModel.swift
//  ATC
//
//  Created by RAVI KANDARPA on 12/11/22.
//

import Foundation

struct ProfileModel: Identifiable {
    let id: String = UUID().uuidString
    let name: String
    let pressure: Double
}
