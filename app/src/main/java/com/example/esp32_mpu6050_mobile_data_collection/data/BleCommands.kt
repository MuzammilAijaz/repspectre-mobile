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
        object StopBle : BleCommand.Control()
        object DisconnectDevice : BleCommand.Control()
        object DiscoverServices : BleCommand.Control()
    }

    // Access : no parameters, yes returns
    sealed class Access : BleCommand() {
        object GetService : BleCommand.Access()
        object GetCharacteristic : BleCommand.Access()
        object GetSensorData : BleCommand.Access()
        object GetMtu : BleCommand.Access()
        object GetConnectionState : BleCommand.Access()
        object ReadCharacteristic : BleCommand.Access()
    }

    // Change : yes parameters, no returns
    sealed class Change : BleCommand() {
        data class ChangeMtu(val mtu: Int) : BleCommand.Change()
    }
}
