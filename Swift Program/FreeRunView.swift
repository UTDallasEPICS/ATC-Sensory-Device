//
//  FreeRunView.swift
//  SwiftApp
//
//  Created by UTDesign EPICS Student on 5/9/23.
//

import Foundation
import SwiftUI
import CoreBluetooth

struct FreeRunView: View{
    @State private var sliderValue = 0.0
    @State private var pressureTarget = 0.0
    @State private var isEditing = false
    @State private var formatted = "";
    
    var body : some View{
        NavigationView {
            VStack{
                /*
                Button(action:{
                    //Instantiate Dialog Box to start scanning
                    //isActive = true
                    PairDialog(title: "Scanning...")
                }, label: {
                    Text("Pair Device")
                })*/
                
                NavigationLink(destination: PairDialog(title: "Scanning...")){
                    Text("Pair Device")
                }
                
                Slider(
                    value: $sliderValue,
                    in: 0...10,
                    onEditingChanged: { editing in
                        isEditing = editing
                        pressureTarget = 14.3 + (0.1)*sliderValue
                        formatted = String(format: "%.1f", pressureTarget)
                    }
                )
                
                Text("Target: \(formatted)")
                    .padding()
                
                Button(action:{
                    //Send value to device
                }, label: {
                    Text("Start")
                })
            }
        }
    }
}
