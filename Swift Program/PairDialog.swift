//
//  PairDialog.swift
//  SwiftApp
//
//  Created by UTDesign EPICS Student on 5/9/23.
//

import Foundation
import SwiftUI
import CoreBluetooth

struct PairDialog: View{
    @Environment(\.defaultMinListRowHeight) var minRowHeight
    
    //@State var scanStatus = "Scanning..."
    let title: String
    
    @State private var offset: CGFloat = 1000
    
    @ObservedObject private var bluetoothViewModel = BLEController()
    
    var body: some View {
        NavigationView {
            VStack {
                //make the device item clickable and be able to transfer cbperipheral object to freerun class
               
                Text(title)
                    .foregroundColor(.black)
                    .padding()
                
                //Scan and Display Scan Results Here
                ScrollView{
                    List {
                        ForEach(bluetoothViewModel.peripherals, id: \.self){ device in
                            let peripheralName: String = device.name ?? "<nil>"
                            NavigationLink(destination: FreeRunPairedView(controller: bluetoothViewModel, receivedDevice: device)){
                                Text(peripheralName)
                            }
                        }
                    }.frame(minHeight: minRowHeight * 10)
                }
                
                /*
                Button {
                    //Go back to page that called dialog
                    FreeRunView()
                } label: {
                    ZStack {
                        RoundedRectangle(cornerRadius: 20)
                            .foregroundColor(.red)
                        
                        Text("Cancel")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                            .padding()
                    }
                    .padding()
                }*/
                NavigationLink(destination: FreeRunView()){
                    Text("Close")
                }
            }
            .fixedSize(horizontal: false, vertical: true)
            .padding()
            .background(.white)
            .clipShape(RoundedRectangle(cornerRadius: 20))
            .shadow(radius: 20)
            .padding(30)
            .offset(x: 0, y: offset)
            .onAppear(){
                withAnimation(.spring()){
                    offset = 0
                }
            }
        }
        }
    
    /*
    func close() {
        withAnimation(.spring()) {
            offset = 1000
            isActive = false
        }
    }*/
}
