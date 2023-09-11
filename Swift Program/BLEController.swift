//
//  BLEController.swift
//  SwiftApp
//
//  Created by UTDesign EPICS Student on 5/10/23.
//

import Foundation
import CoreBluetooth

class BLEController: NSObject, ObservableObject {
    @Published var centralManager: CBCentralManager!
    @Published var device: CBPeripheral
    
    override init() {
        super.init()
        self.centralManager = CBCentralManager(delegate: self, queue: .main)
    }
    
    func connect(){
        centralManager?.connect(device, options: nil)
    }
}

extension BluetoothScanModule: CBCentralManagerDelegate {
    //Called when the Central Manager's state is updated
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if central.state == .poweredOn {
            
        }
    }
    
    
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        peripheral.delegate = self
        peripheral.discoverServices(nil)
    }
}

extension BluetoothScanModule: CBPeripheralDelegate {
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        guard let services = peripheral.services else { return }
        for service in services {
            peripheral.discoverCharacteristics(nil, for: service)
        }
    }
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        guard let characteristics = service.characteristics else { return }
        for characteristic in characteristics {
            if characteristic.properties.contains(.read) {
                peripheral.readValue(for: characteristic)
            }
            if characteristic.properties.contains(.notify) {
                peripheral.setNotifyValue(true, for: characteristic)
            }
        }
    }
    
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        if let value = characteristic.value {
            let stringFromData = String(data: value, encoding: .utf8)
            print("Received: \(stringFromData ?? "")")
        }
    }
}
