//
//  BluetoothModel.swift
//  ATC
//
//  Created by RAVI KANDARPA on 11/29/22.
//
import Foundation
import CoreBluetooth

class BLEController: NSObject, ObservableObject {
    @Published var centralManager: CBCentralManager!
    @Published var peripherals: [CBPeripheral] = []
    private var device: CBPeripheral!
    private var txCharacteristic: CBCharacteristic!
    private var rxCharacteristic: CBCharacteristic!
    
    override init() {
        super.init()
        self.centralManager = CBCentralManager(delegate: self, queue: .main)
    }
    
    func stopScanning(){
        centralManager?.stopScan();
    }
    
    func connect(peripheral: CBPeripheral){
        device = peripheral
        centralManager?.connect(peripheral, options: nil)
    }
    
    func disconnect () {
        if device != nil {
            centralManager?.cancelPeripheralConnection(device!)
        }
    }
    
    func sendMessage(value: Float){
        print(value)
        var valueBytes = toUint32Array(value: value)
        let data = Data(buffer: UnsafeBufferPointer(start: &valueBytes, count: 1))
        //let valueBytes = NSData.init(bytes: data, length: 4)
        if let device = device {
            if let txCharacteristic = txCharacteristic {
                device.writeValue(data, for: txCharacteristic, type: CBCharacteristicWriteType.withResponse)
            }
        }
    }
    
    private func toUint32Array(value: Float) -> [UInt32] {
        var byteArray = [UInt32](repeating: 0, count: 4)
        var intBits = value.bitPattern
        var i = 0
        for _ in byteArray {
            byteArray[byteArray.count-1-i] = UInt32(intBits)
            i+=1
            intBits = intBits >> 8
        }
        return byteArray
    }
}
extension BLEController: CBCentralManagerDelegate {
    //Called when the Central Manager's state is updated
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if central.state == .poweredOn {
            self.centralManager?.scanForPeripherals(withServices: [CBUUIDs.BLEService_UUID])
        }
    }
    
    //Called when a peripheral is discovered
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        
        if !peripherals.contains(peripheral){
            self.peripherals.append(peripheral)
        }
    }
    
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        print("Connected to \(peripheral.name ?? "unnamed device")")
        stopScanning()
        peripheral.delegate = self
        peripheral.discoverServices([CBUUIDs.BLEService_UUID])
    }
}

extension BLEController: CBPeripheralDelegate {
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        print("*******************************************************")
        if ((error) != nil) {
            print("Error discovering services: \(error!.localizedDescription)")
            return
        }
        guard let services = peripheral.services else {
            return
        }
        //We need to discover the all characteristic
        for service in services {
            peripheral.discoverCharacteristics(nil, for: service)
        }
        print("Discovered Services: \(services)")
    }
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        guard let characteristics = service.characteristics else {
            return
        }

        print("Found \(characteristics.count) characteristics.")

          for characteristic in characteristics {
            if characteristic.uuid.isEqual(CBUUIDs.BLE_Characteristic_uuid_Rx)  {
              rxCharacteristic = characteristic
              peripheral.setNotifyValue(true, for: rxCharacteristic!)
              peripheral.readValue(for: characteristic)
              print("RX Characteristic: \(rxCharacteristic.uuid)")
                
            }

            if characteristic.uuid.isEqual(CBUUIDs.BLE_Characteristic_uuid_Tx){
              txCharacteristic = characteristic
              print("TX Characteristic: \(txCharacteristic.uuid)")
            }
        }
    }
}
