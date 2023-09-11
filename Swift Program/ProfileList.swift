//
//  ProfileList.swift
//  ATC
//
//  Created by RAVI KANDARPA on 12/11/22.
//

import SwiftUI

struct ProfileList: View {
    @EnvironmentObject var profViewModel: ProfileViewModel
    var body: some View {
        List {
            ForEach(profViewModel.profiles) { p in
                NavigationLink {
                    SwiftUIView()
                }label: {
                    ProfileRowView(p: p)
                }
            }
            .onDelete(perform: profViewModel.deleteProfile)
            .onMove(perform: profViewModel.moveProfile)
        }
        .navigationTitle("Profile List")
        .navigationBarItems(
                leading: EditButton(),
                trailing:
                    NavigationLink("Add", destination: AddView())
                )
    }
    
    
}

struct ProfileList_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            ProfileList()
        }
        .environmentObject(ProfileViewModel())
    }
}


