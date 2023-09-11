//
//  AddView.swift
//  ATC
//
//  Created by RAVI KANDARPA on 12/11/22.
//

import SwiftUI

struct AddView: View {
    @Environment(\.presentationMode) var presentationMode
    @EnvironmentObject var profViewModel: ProfileViewModel
    @State var textFieldText: String = ""
    @State var alertTitle: String = ""
    @State var showAlert: Bool = false
    
    var body: some View {
        ScrollView {
            VStack {
                TextField("Type Profile name here", text: $textFieldText)
                    .padding(.horizontal)
                    .frame(height: 55)
                    .background(Color(.white))
                    .cornerRadius(10)
                
                Button(action: AddButtonPressed, label: {
                    Text("Add".uppercased())
                        .foregroundColor(.white)
                        .font(.headline)
                        .frame(height: 55)
                        .frame(maxWidth: .infinity)
                        .background(Color.accentColor)
                        .cornerRadius(10)
                })
            }
            .padding(14)
        }
        .navigationTitle("Add Profile")
        .alert(isPresented: $showAlert, content: getAlert)
        
    }
    func AddButtonPressed() {
        if textIsApproved() == true {
            profViewModel.addProfile(name: textFieldText)
            presentationMode.wrappedValue.dismiss()
        }
    }
    func textIsApproved() -> Bool {
        if(textFieldText.count == 0){
            alertTitle = "Please enter Profile name"
            showAlert.toggle()
            return false
        }
        return true
    }
    func getAlert() -> Alert {
        return Alert(title: Text(alertTitle))
    }
}

struct AddView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView{
            AddView()
        }
        .environmentObject(ProfileViewModel())
    }
}
