package com.example.esp32_mpu6050_mobile_data_collection.data


/* All Commands required to Control/Access the Service
 *
 * Access Methods : tells the repository to call a function from service to fetch something
 *      -> things to fetch : ex. service, characteristic, state, device details, name, message
 *
 * Control Methods : controls the service in some way (ex. stopping the service) but does not require any parameter passing
 *
 * Change Methods : changes the services operation in some way like changing mtu values
 */
sealed class BleCommand {
    // Control : no parameters, no return
    sealed class Control : BleCommand() {
//        object StartBle : BleCommand()
        object StopBle : BleCommand()
        object DisconnectDevice : BleCommand()
        object DiscoverServices : BleCommand()
    }

    // Access : no parameters, yes returns
    sealed class Access : BleCommand() {
        object GetService : BleCommand()
        object GetCharacteristic : BleCommand()
        object GetSensorData : BleCommand()
        object GetMtu : BleCommand()
        object GetConnectionState : BleCommand()
        object ReadCharacteristic : BleCommand()
    }

    // Change : yes parameters, no returns
    sealed class Change : BleCommand() {
        data class ChangeMtu(val mtu: Int) : BleCommand()
    }
}
