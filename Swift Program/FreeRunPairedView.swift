//
//  FreeRunView.swift
//  SwiftApp
//
//  Created by UTDesign EPICS Student on 5/9/23.
//

import Foundation
import SwiftUI
import CoreBluetooth

struct FreeRunPairedView: View{
    @State private var sliderValue: Float = 5.0
    @State private var pressureTarget: Float = 0.0
    @State private var isEditing = false
    @State private var formatted = "";
    
    var controller: BLEController
    
    public var receivedDevice: CBPeripheral
    
    var body : some View{
        
        let _ = controller.connect(peripheral: receivedDevice)
        
        NavigationView {
            VStack{
                Text(receivedDevice.name ?? "N/A").padding()
                
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
                    controller.sendMessage(value: Float(pressureTarget))
                }, label: {
                    Text("Start")
                })
            }
        }
    }
}
