// vim :set ts=2 sw=2 sts=2 expandtab smarttab :
/**
 *  Copyright 2017 Brian Aker <brian@tangent.org>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

import physicalgraph.*

def getDriverVersion () {
  return "v0.25"
}

metadata {
  definition (name: "Z-Wave Unknown Device", namespace: "TangentOrgThings", author: "brian@tangent.org") {
    capability "Actuator"
    capability "Configuration"
    capability "Switch"
    capability "Refresh"
    capability "Sensor"

    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message

    attribute "Manufacturer", "string"
    attribute "ManufacturerCode", "string"
    attribute "MSR", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"
    attribute "NIF", "string"
    attribute "firmwareVersion", "string"
    attribute "driverVersion", "string"

    attribute "NumberOfAssociatedGroups", "number"
  }

  simulator {
    status "on":  "command: 2003, payload: FF"
    status "off": "command: 2003, payload: 00"
  }

  preferences {
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false,  defaultValue: 5
  }

  tiles(scale: 2) {
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
      tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label: '${name}', action: "switch.off", icon: "st.Home.home30", backgroundColor: "#79b821"
        attributeState "off", label: '${name}', action: "switch.on", icon: "st.Home.home30", backgroundColor: "#ffffff"
      }
      tileAttribute("device.status", key: "SECONDARY_CONTROL") {
        attributeState("default", label:'${currentValue}', unit:"")
      }
    }

    standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
    }

    valueTile("firmwareVersion", "device.firmwareVersion", width:2, height: 2, decoration: "flat", inactiveLabel: false) {
      state "default", label: '${currentValue}'
    }

    valueTile("driverVersion", "device.driverVersion", width:2, height:2, inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }

    standardTile("configure", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "default", label:'', action:"configuration.configure", icon:"st.secondary.configure"
    }

    main "switch"
    details(["switch", "firmwareVersion", "driverVersion", "refresh", "configure"])
  }
}

def getCommandClassVersions() {
  [
    0x20: 1,  // Basic
    0x72: 1,  // Manufacturer Specific
    0x80: 1,  // Battery
    0x84: 1,  // Wake Up
    0x85: 1,  // Association  0x85  V1 V2
    0x86: 1,  // Version
  ]
}

def parse(String description) {
  def result = []

  if (description && description.startsWith("Err")) {
    logger("parse error: ${description}", "error")

    if (description.startsWith("Err 106")) {
      result << createEvent(
        descriptionText: "Security, possible key exchange error.",
        eventType: "ALERT",
        name: "secureInclusion",
        value: "failed",
        isStateChange: true,
      )
    }
  } else if (! description) {
    logger("$device.displayName parse() called with NULL description", "info")
  } else if (description != "updated") {
    def cmd = zwave.parse(description, getCommandClassVersions())

    if (! cmd ) {
      cmd = zwave.parse(description)
    }

    if (cmd) {
      zwaveEvent(cmd, result)
    } else {
      logger( "zwave.parse() failed for: ${description}", "parse" )
    }
  }

  return result
}

def prepDevice() {
  [
    zwave.basicV1.basicSet(value: 0xFF),
    zwave.manufacturerSpecificV1.manufacturerSpecificGet(),
    zwave.versionV1.versionGet(),
    zwave.associationV1.associationGroupingsGet(),
    //zwave.zwaveCmdClassV1.requestNodeInfo(),
  ]
}

def installed() {
  log.info("$device.displayName installed()")

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  sendCommands( prepDevice(), 2000 )
}

def updated() {
  log.info("$device.displayName updated()")
  logger("$device.displayName updated() debug: ${debugLevel}", "info")

  state.loggingLevelIDE = debugLevel ? debugLevel : 4
  sendEvent(name: "lastError", value: "", isStateChange: true)
  sendEvent(name: "logMessage", value: "", isStateChange: true)

  // Check in case the device has been changed
  state.manufacturer = null
  updateDataValue("MSR", null)
  updateDataValue("Manufacturer", null)

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  sendCommands( prepDevice(), 2000 )
}

def zwaveEvent(zwave.commands.associationv1.AssociationGroupingsReport cmd, result) {
  logger("$cmd")
  def cmds = []

  if (cmd.supportedGroupings) {
    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationV1.associationGet(groupingIdentifier: x).format()
    }
  }

  result << createEvent(name: "NumberOfAssociatedGroups", value: cmd.supportedGroupings, descriptionText: "$device.displayName", isStateChange: true, displayed: true)
  response(delayBetween(cmds))
}

def zwaveEvent(zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.associationv1.AssociationReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.wakeupv1.WakeUpNotification cmd, result) {
  logger("$cmd")
  response(zwave.wakeUpV1.wakeUpNoMoreInformation())
}

def zwaveEvent(zwave.commands.basicv1.BasicGet cmd, result) {
  logger("$cmd")

  def currentValue = device.currentState("switch").value.equals("on") ? 255 : 0
  result << zwave.basicV1.basicReport(value: currentValue).format()
}

def zwaveEvent(zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$cmd")

  Short value = cmd.value

  if (value == 0) {
    result << createEvent(name: "switch", value: "off", isStateChange: true, displayed: true)
    if (device.displayName.endsWith("Dimmer")) {
      result << createEvent(name: "level", value: 0, isStateChange: true, displayed: true)
    }
  } else if (value < 100 || value == 255) {
    result << createEvent(name: "switch", value: "on", isStateChange: true, displayed: true)
    if (device.displayName.endsWith("Dimmer")) {
      result << createEvent(name: "level", value: basic == 255 ? 100 : value, isStateChange: true, displayed: true)
    }
  } else if (value < 254) {
    logger("BasicReport returned reserved state ($value)", "warn")
  } else if (value == 254) {
    logger("BasicReport unknown state (254)", "warn")
  } else {
    logger("BasicReport reported value unknown to API ($value)", "warn")
  }
}

def zwaveEvent(zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$cmd")

  Short value = cmd.value

  if (value == 0) {
    result << createEvent(name: "switch", value: "off", isStateChange: true, displayed: true)
    if (device.displayName.endsWith("Dimmer")) {
      result << createEvent(name: "level", value: 0, isStateChange: true, displayed: true)
    }
  } else if (value < 100 || value == 255) {
    result << createEvent(name: "switch", value: "on", isStateChange: true, displayed: true)
    if (device.displayName.endsWith("Dimmer")) {
      result << createEvent(name: "level", value: 100, isStateChange: true, displayed: true)
    }
  } else if (value < 254) {
    logger("BasicSet returned reserved state ($value)", "warn")
  } else if (value == 254) {
    logger("BasicSet unknown state (254)", "warn")
  } else {
    logger("BasicSet reported value unknown to API ($value)", "warn")
  }
} 

def zwaveEvent(zwave.commands.securityv1.SecurityMessageEncapsulation cmd, result) {
  logger("$cmd")

  def encapsulatedCommand = cmd.encapsulatedCommand(getCommandClassVersions())
  if (encapsulatedCommand) {
    state.sec = 1
    def resultsof = zwaveEvent(encapsulatedCommand, result)
    def sec_result = resultsof.collect {
      if (it instanceof physicalgraph.device.HubAction && !it.toString().startsWith("9881")) {
        response(cmd.CMD + "00" + it.toString())
      } else {
        it
      }
    }

    result += sec_result
  }
}

def zwaveEvent(zwave.commands.crc16encapv1.Crc16Encap cmd, result) {
  logger("$cmd")

  def versions = getCommandClassVersions()
  // def encapsulatedCommand = cmd.encapsulatedCommand(versions)
  def version = versions[cmd.commandClass as Integer]
  def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
  def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
  if (encapsulatedCommand) {
    zwaveEvent(encapsulatedCommand, result)
  }
}

def zwaveEvent(zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd, result) {
  logger("$cmd")

  updateDataValue("deviceIdData", "${cmd.deviceIdData}")
  updateDataValue("deviceIdDataFormat", "${cmd.deviceIdDataFormat}")
  updateDataValue("deviceIdDataLengthIndicator", "${cmd.deviceIdDataLengthIndicator}")
  updateDataValue("deviceIdType", "${cmd.deviceIdType}")

  if (cmd.deviceIdType == 1 && cmd.deviceIdDataFormat == 1) {//serial number in binary format
    String serialNumber = "h'"

    cmd.deviceIdData.each{ data ->
      serialNumber += "${String.format("%02X", data)}"
    }

    updateDataValue("serialNumber", serialNumber)
  }
}

def zwaveEvent(zwave.commands.manufacturerspecificv1.ManufacturerSpecificReport cmd, result) {
  logger("$cmd")

  state.manufacturer= cmd.manufacturerName
  state.manufacturerId = cmd.manufacturerId
  state.productTypeId = cmd.productTypeId
  state.productId= cmd.productId

  String manufacturerCode = String.format("%04X", cmd.manufacturerId)
  String productTypeCode = String.format("%04X", cmd.productTypeId)
  String productCode = String.format("%04X", cmd.productId)
  String wirelessConfig = "ZWP"

  sendEvent(name: "ManufacturerCode", value: manufacturerCode)
  sendEvent(name: "ProduceTypeCode", value: productTypeCode)
  sendEvent(name: "ProductCode", value: productCode)
  sendEvent(name: "WirelessConfig", value: wirelessConfig)

  String msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)
  updateDataValue("manufacturer", "${state.manufacturer}")

  sendEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: false)
  result << createEvent(name: "Manufacturer", value: "${state.manufacturer}", descriptionText: "$device.displayName", isStateChange: false)
}

def zwaveEvent(zwave.commands.versionv1.VersionReport cmd, result) {
  logger("$cmd")

  String text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  String zWaveProtocolVersion = "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  updateDataValue("firmwareVersion", "${state.firmwareVersion}")
  updateDataValue("zWaveProtocolVersion", "${zWaveProtocolVersion}")
}

def zwaveEvent(zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd, result) {
  logger("$cmd")
  String firmware_report = String.format("%s-%s-%s", cmd.checksum, cmd.firmwareId, cmd.manufacturerId)
  updateDataValue("FirmwareMdReport", "$firmware_report")
}

def zwaveEvent(zwave.Command cmd, result) {
  logger("command not implemented: $cmd", "warn")
}

def on() {
  zwave.basicV1.basicSet(value: 0xFF).format()
}

def off() {
  zwave.basicV1.basicSet(value: 0xFF).format()
}

def refresh() {
  // sendCommands([zwave.basicV1.basicGet().format()])
    zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
}

/*****************************************************************************************************************
 *  Private Helper Functions:
 *****************************************************************************************************************/

/**
 *  encapCommand(cmd)
 *
 *  Applies security or CRC16 encapsulation to a command as needed.
 *  Returns a zwave.Command.
 **/
private encapCommand(zwave.Command cmd) {
  if (state.useSecurity) {
    return zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd)
  }
  else if (state.useCrc16) {
    return zwave.crc16EncapV1.crc16Encap().encapsulate(cmd)
  }
  else {
    return cmd
  }
}

/**
 *  prepCommands(cmds, delay=200)
 *
 *  Converts a list of commands (and delays) into a HubMultiAction object, suitable for returning via parse().
 *  Uses encapCommand() to apply security or CRC16 encapsulation as needed.
 **/
private prepCommands(cmds, delay=200) {
  return response(delayBetween(cmds.collect{ (it instanceof zwave.Command ) ? encapCommand(it).format() : it }, delay))
}

/**
 *  sendCommands(cmds, delay=200)
 *
 *  Sends a list of commands directly to the device using sendHubCommand.
 *  Uses encapCommand() to apply security or CRC16 encapsulation as needed.
 **/
private sendCommands(cmds, delay=200) {
  sendHubCommand( cmds.collect{ (it instanceof zwave.Command ) ? response(encapCommand(it)) : response(it) }, delay)
}

private logger(msg, level = "trace") {
  String device_name = "$device.displayName"

  switch(level) {
    case "warn":
    if (settings.debugLevel >= 2) {
      log.warn "$device_name ${msg}"
    }
    sendEvent(name: "logMessage", value: "${msg}", displayed: false, isStateChange: true)
    break;

    case "info":
    if (settings.debugLevel >= 3) {
      log.info "$device_name ${msg}"
    }
    break;

    case "debug":
    if (settings.debugLevel >= 4) {
      log.debug "$device_name ${msg}"
    }
    break;

    case "trace":
    if (settings.debugLevel >= 5) {
      log.trace "$device_name ${msg}"
    }
    break;

    case "error":
    default:
    log.error "$device_name ${msg}"
    sendEvent(name: "lastError", value: "${msg}", displayed: false, isStateChange: true)
    break;
  }
}
