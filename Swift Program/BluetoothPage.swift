//
//  BluetoothPage.swift
//  ATC
//
//  Created by RAVI KANDARPA on 11/29/22.
//

import SwiftUI

struct BluetoothPage: View {
    @ObservedObject private var bluetoothViewModel = BluetoothModel()
    var body: some View {
        NavigationView{
            List(bluetoothViewModel.peripheralNames, id: \.self){ peripheral in
                Text(peripheral)
            }
            .navigationTitle("Devices")
        }
    }
}

struct BluetoothPage_Previews: PreviewProvider {
    static var previews: some View {
        BluetoothPage()
    }
}

