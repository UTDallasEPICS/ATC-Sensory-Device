//
//  ProfileRowView.swift
//  ATC
//
//  Created by RAVI KANDARPA on 12/11/22.
//

import SwiftUI

struct ProfileRowView: View {
    let p: ProfileModel
    var body: some View {
        HStack {
            Image(systemName: "checkmark.circle")
            Text(p.name)
            Spacer()
        }
    }
}

struct ProfileRowView_Previews: PreviewProvider {
    static var profile1 = ProfileModel(name: "John", pressure: 5)
    static var profile2 = ProfileModel(name: "Sam", pressure: 4)
    static var previews: some View {
        Group {
            ProfileRowView(p: profile1)
            ProfileRowView(p: profile2)
        }
        .previewLayout(.sizeThatFits)
    }
}
