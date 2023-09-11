//
//  ContentView.swift
//  ATC
//
//  Created by RAVI KANDARPA on 10/5/22.
//

import SwiftUI
import CoreBluetooth

struct ContentView: View {
    var body: some View {
        NavigationView{
            ZStack{
                Color.blue.ignoresSafeArea()
                Circle().scale(1.7).foregroundColor(.white.opacity(0.15))
                Circle().scale(1.35).foregroundColor(.white)
                VStack{
                    NavigationLink {
                        BluetoothPage()
                    } label: {
                        Text("BlueTooth Settings")
                            .font(.headline)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .padding()
                            .padding(.horizontal, 20)
                            .background(Color.blue
                                .cornerRadius(10)
                                .shadow(radius:10)
                            )
                    }
                    .padding()
                    NavigationLink{
                        ProfileList()
                    } label: {
                        Text("Profile List")
                            .font(.headline)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .padding()
                            .padding(.horizontal, 20)
                            .background(Color.blue
                                .cornerRadius(10)
                                .shadow(radius:10)
                            )
                    }
                }
                .navigationTitle("ATC App")
            }
        }
    }
    
    struct ContentView_Previews: PreviewProvider {
        static var previews: some View {
            ContentView()
        }
    }
    
}

