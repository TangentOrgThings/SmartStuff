// vim :set ts=2 sw=2 sts=2 expandtab smarttab :
/**
 *  Copyright 2017-2018 Brian Aker <brian@tangent.org>
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
 *
 *  Version : see getDriverVersion()
 *  Author: Brian Aker
 *  Date: 2017-
 *
 * https://products.z-wavealliance.org/products/2017
 */

import physicalgraph.*

def getDriverVersion () {
  return "v0.43"
}

def maxButton () {
  return 5
}

metadata {
  definition (name: "FIBARO Button", namespace: "TangentOrgThings", author: "brian@tangent.org") {
    capability "Actuator"
    capability "Button"
    capability "Battery"
    capability "Configuration"
    capability "Refresh"

    attribute "DeviceReset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message
    attribute "parseErrorCount", "number"        // Last error message
    attribute "unknownCommandErrorCount", "number"        // Last error message

    attribute "buttonPressed", "number"
    attribute "keyAttributes", "number"
    attribute "Scene", "number"

    attribute "driverVersion", "string"
    attribute "firmwareVersion", "string"
    attribute "FirmwareMdReport", "string"
    attribute "Manufacturer", "string"
    attribute "ManufacturerCode", "string"
    attribute "MSR", "string"
    attribute "NIF", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"
    attribute "WakeUp", "string"
    attribute "WirelessConfig", "string"

    // fingerprint deviceId: "0x1801", inClusters: "0x5E, 0x70, 0x85, 0x2D, 0x8E, 0x80, 0x84, 0x8F, 0x5A, 0x59, 0x5B, 0x73, 0x86, 0x72", outClusters: "0x20, 0x5B, 0x26, 0x27, 0x2B, 0x60"
    // fingerprint deviceId: "0x1202", inClusters: "0x5E, 0x8F, 0x73, 0x98, 0x86, 0x72, 0x70, 0x85, 0x2D, 0x8E, 0x80, 0x84, 0x5A, 0x59, 0x5B", outClusters:  "0x20, 0x5B, 0x26, 0x27, 0x2B, 0x60"         
  }

  simulator {
    status "button 1 pushed":  "command: 9881, payload: 00 5B 03 DE 00 01"

    // need to redo simulator commands

  }

  preferences {
    input "debugLevel", "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  }

  tiles {
    standardTile("button", "device.button", width: 2, height: 2) {
      state "default", label: "", icon: "st.Home.home30", backgroundColor: "#ffffff"
      state "held", label: "holding", icon: "st.Home.home30", backgroundColor: "#C390D4"
    }
    valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
      tileAttribute ("device.battery", key: "PRIMARY_CONTROL"){
        state "battery", label:'${currentValue}% battery', unit:""
      }
    }
    standardTile("configure", "device.button", width: 1, height: 1, decoration: "flat") {
      state "default", label: "configure", backgroundColor: "#ffffff", action: "configure"
    }

    main "button"
    details(["button", "battery", "configure"])
  }

}

def getCommandClassVersions() {
  [
    0x20: 1,  // Basic
    0x26: 3,  // Switch Multilevel
    0x2B: 1,  // SceneActivation
    0x2C: 1,  // Scene Actuator Conf
    0x56: 1,  // Crc
    0x59: 1,  // Association Grp Info
    0x5A: 1,  // Device Reset Locally
    0x5B: 1,  // Central Scene
    0x60: 3,  // Multi-Channel Association
    // 0x5E: 2, //
    // 0x6C: 2, // Supervision
    0x70: 2,  // Configuration
    0x71: 3,  // Notification
    0x72: 2,  // Manufacturer Specific
    0x73: 1,  // Powerlevel
    0x7A: 2,  // Firmware Update Md HS-200 V4
    0x85: 2,  // Association  0x85  V1 V2    
    0x86: 1,  // Version
    0x96: 1,  // Security
    0x9C: 1, // Sensor Alarm
    // 0x55: 1,  // Transport Service Command Class
    // 0x9F: 1,  // Security 2 Command Class  
  ]
}

def parse(String description) {
  def result = []

  if (description && description.startsWith("Err")) {
    logger ( "parse error: ${description}", "error" )

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
    logger("parse() called with NULL description", "info")
  } else if (description != "updated") {
    def cmd = zwave.parse(description, getCommandClassVersions())

    if (cmd) {
      zwaveEvent(cmd, result)

    } else {
      logger( "zwave.parse(getCommandClassVersions()) failed for: ${description}", "error" )
      // Try it without check for classes
      cmd = zwave.parse(description)

      if (cmd) {
        zwaveEvent(cmd, result)
      } else {
        logger( "zwave.parse() failed for: ${description}", "error" )
      }
    }
  }

  return result
}

def prepDevice() {
  [
    zwave.zwaveCmdClassV1.requestNodeInfo(),
    zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
    zwave.configurationV2.configurationGet(parameterNumber: 4),
    zwave.firmwareUpdateMdV1.firmwareMdGet(),
    zwave.associationV2.associationGroupingsGet(),
    zwave.centralSceneV1.centralSceneSupportedGet(),
  ]
}

def installed() {
  log.info("$device.displayName installed()")

  sendEvent(name: "numberOfButtons", value: maxButton(), displayed: true, isStateChange: true)
  state.loggingLevelIDE = 4

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  sendCommands( prepDevice(), 2000 )
}

def updated() {
  if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
    return
  }
  logger("updated() debug: ${settings.debugLevel}")

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)
  sendEvent(name: "parseErrorCount", value: 0, displayed: false)
  sendEvent(name: "unknownCommandErrorCount", value: 0, displayed: false)
  state.parseErrorCount = 0
  state.unknownCommandErrorCount = 0

  state.manufacturer = null
  updateDataValue("MSR", null)
  updateDataValue("manufacturer", null)
  sendEvent(name: "numberOfButtons", value: maxButton(), displayed: true, isStateChange: true)
  state.loggingLevelIDE = 4

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  sendCommands( prepDevice(), 2000 )

  // Avoid calling updated() twice
  state.updatedDate = Calendar.getInstance().getTimeInMillis()
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd, result) {
  logger("$cmd")

  def encapsulatedCommand = cmd.encapsulatedCommand(getCommandClassVersions())
  //    log.debug("UnsecuredCommand: $encapsulatedCommand")
  // can specify command class versions here like in zwave.parse
  if (encapsulatedCommand) {
    //  log.debug("UnsecuredCommand: $encapsulatedCommand")
    return zwaveEvent(encapsulatedCommand, result)
  }

  logger("Unable to extract security encapsulated cmd from $cmd", "error")
}

def buttonEvent(button, held, buttonType = "physical") {
  log.debug("buttonEvent: $button  held: $held  type: $buttonType")
  button = button as Integer
  String heldType = held ? "held" : "pushed"

  sendEvent(name: "buttonPressed", value: button, displayed: true, isStateChange: true)
  sendEvent(name: "button", value: "$heldType", data: [buttonNumber: button], descriptionText: "$device.displayName", isStateChange: true, type: "$buttonType")
}

def zwaveEvent(physicalgraph.zwave.commands.centralscenev1.CentralSceneSupportedReport cmd, result) {
  logger("$cmd")

  sendEvent(name: "numberOfButtons", value: maxButton(), displayed: true, isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.centralscenev1.CentralSceneNotification cmd, result) {
  logger("$cmd")
  logger("keyAttributes: $cmd.keyAttributes")

  state.lastActive = new Date().time

  switch (cmd.sceneNumber) {
    case 1:
    // Up
    switch (cmd.keyAttributes) {
      case 0:
      case 1: // Capture 0 and 1 as same;
      buttonEvent(1, false, "physical")
      break;
      default:
      buttonEvent(cmd.keyAttributes -1, false, "physical")
      break;
    }
    break;

    default:
    // unexpected case
    logger ("unexpected scene: $cmd.sceneNumber", "warn")
    break;
  }


  return result
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd, result) {
  logger("$cmd")
  result << response(zwave.wakeUpV2.wakeUpNoMoreInformation())
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$cmd")
  switch( cmd.value ) {
    case 3:
    buttonEvent(3, false, "physical")
    break;
    case 2:
    buttonEvent(2, false, "physical")
    break;
    case 1:
    default:
    buttonEvent(1, false, "physical")
    break;
  }
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd, result) {
  logger("$cmd")
  // buttonEvent(2, false, "physical"),
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelSet cmd, result) {
  logger("$cmd")
  buttonEvent(3, false, "physical")
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelStartLevelChange cmd, result) {
  logger("$cmd")
  buttonEvent(1, true, "physical")
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelStopLevelChange cmd, result) {
  logger("$cmd")
  buttonEvent(1, true, "physical")
}

def zwaveEvent(physicalgraph.zwave.commands.sensoralarmv1.SensorAlarmReport cmd, result) {
  logger("$cmd")
  buttonEvent(4, false, "physical")
  result << response(configure())
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd, result) {
  logger("$cmd")
  def map = [ name: "battery", unit: "%" ]
  if (cmd.batteryLevel == 0xFF) {  // Special value for low battery alert
    map.value = 1
    map.descriptionText = "${device.displayName} has a low battery"
    map.isStateChange = true
  } else {
    map.value = cmd.batteryLevel
    log.debug ("Battery: $cmd.batteryLevel")
  }
  // Store time of last battery update so we don't ask every wakeup, see WakeUpNotification handler
  state.lastbatt = new Date().time
  result << createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd, result) {
  logger("$cmd")
  /*
  if (cmd.notificationType == 0x07) {
  if (cmd.v1AlarmType == 0x07) {  // special case for nonstandard messages from Monoprice ensors
  } else if (cmd.event == 0x01 || cmd.event == 0x02 || cmd.event == 0x07 || cmd.event == 0x08) {
  } else if (cmd.event == 0x00) {
  } else if (cmd.event == 0x03) {
  } else if (cmd.event == 0x05 || cmd.event == 0x06) {
  }
  } else if (cmd.notificationType) {
  } else {
  }
   */
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd, result) {
  logger("$cmd")
  def versions = getCommandClassVersions()
  // def encapsulatedCommand = cmd.encapsulatedCommand(versions)
  def version = versions[cmd.commandClass as Integer]
  def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
  def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
  if (!encapsulatedCommand) {
    log.debug "Could not extract command from $cmd"
  } else {
    zwaveEvent(encapsulatedCommand, result)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd) {
  logger("$cmd")
  logger( "Button code: $cmd.sceneId", "debug")
}

def zwaveEvent(zwave.commands.configurationv2.ConfigurationReport cmd, result) {
  logger("$cmd")

  updateDataValue("Configuration #${cmd.parameterNumber}", "${cmd.scaledConfigurationValue}")
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd, result) {
  logger("$cmd")

  def encapsulatedCommand = cmd.encapsulatedCommand(getCommandClassVersions())
  if (encapsulatedCommand) {
    zwaveEvent(encapsulatedCommand, result)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelEndPointReport cmd, result) {
  logger("$cmd")
  updateDataValue("endpoints", cmd.endPoints.toString())
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCapabilityReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd, result) {
  logger("$cmd")
  result << createEvent(name: "DeviceReset", value: "true", descriptionText: cmd.toString(), isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd, result) {
  logger("$cmd")
  result << createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
  logger("$cmd")

  if ( state.manufacturerId != cmd.manufacturerId && state.productTypeId != cmd.productTypeId && state.productId != cmd.productId) {
    log.warn("$device.displayName has changed")
  }

  state.manufacturer= cmd.manufacturerName
  state.manufacturerId= cmd.manufacturerId
  state.productTypeId= cmd.productTypeId
  state.productId= cmd.productId

  String manufacturerCode = String.format("%04X", cmd.manufacturerId)
  String productTypeCode = String.format("%04X", cmd.productTypeId)
  String productCode = String.format("%04X", cmd.productId)

  String msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)
  updateDataValue("manufacturer", "${state.manufacturer}")
  updateDataValue("manufacturerName", cmd.manufacturerName)
  updateDataValue("manufacturerId", manufacturerCode)
  updateDataValue("productId", productCode)
  updateDataValue("productTypeId", productTypeCode)

  result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
  result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
  result << createEvent(name: "ProductCode", value: productCode)
  result << response(delayBetween(cmds, 1000))
  result << response( zwave.versionV1.versionGet() )
}


def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd, result) {
  logger("$cmd")

  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  def zWaveProtocolVersion = "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  result << createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: true)
  result << createEvent(name: "zWaveProtocolVersion", value: "${zWaveProtocolVersion}", descriptionText: "${device.displayName} ${zWaveProtocolVersion}", isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd, result) {
  logger("$cmd")

  state.groups = cmd.supportedGroupings

  if (cmd.supportedGroupings) {
    def cmds = []
    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x);
      cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: 0x00);
      cmds << zwave.associationGrpInfoV1.associationGroupCommandListGet(allowCache: true, groupingIdentifier: x);
    }

    sendCommands(cmds, 2000)
  } else {
    logger("reported no groups", "error")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd, result) {
  logger("$cmd")

  def name = new String(cmd.name as byte[])
  logger("Association Group #${cmd.groupingIdentifier} has name: ${name}", "info")

  result << response( zwave.associationV1.associationGet(groupingIdentifier: cmd.groupingIdentifier) )
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupCommandListReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd, result) {
  logger("$cmd")
  String firmware_report = String.format("%s-%s-%s", cmd.checksum, cmd.firmwareId, cmd.manufacturerId)
  updateDataValue("FirmwareMdReport", "$firmware_report")
}

def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  logger("command not implemented: $cmd", "error")
}

private getChildDeviceForEndpoint(Integer endpoint) {
  def children = childDevices
  if (children && endpoint) {
    return children.find{ it.deviceNetworkId.endsWith("ep$endpoint") }
  }
}

def configure() {
  logger("Resetting Sensor Parameters to SmartThings Compatible Defaults", "debug")
  def cmds = []

  cmds << zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 10, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 11, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 12, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [2], parameterNumber: 13, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 14, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [3], parameterNumber: 15, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 20, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 21, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 22, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [2], parameterNumber: 23, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 24, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [3], parameterNumber: 25, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [2], parameterNumber: 29, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [8], parameterNumber: 30, size: 1).format()
  /*
  cmds << zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 1, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 2, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [4], parameterNumber: 11, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [4], parameterNumber: 12, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [4], parameterNumber: 13, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [4], parameterNumber: 14, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 21, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 22, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [2], parameterNumber: 24, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 25, size: 1).format()
  cmds << zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 30, size: 1).format()
   */

  delayBetween(cmds, 500)
}

/*****************************************************************************************************************
 *  Private Helper Functions:
 *****************************************************************************************************************/

/**
 *  encapCommand(cmd)
 *
 *  Applies security or CRC16 encapsulation to a command as needed.
 *  Returns a physicalgraph.zwave.Command.
 **/
private encapCommand(physicalgraph.zwave.Command cmd) {
  if (state.sec) {
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
private prepCommands(cmds, delay) {
  return response(delayBetween(cmds.collect{ (it instanceof physicalgraph.zwave.Command ) ? encapCommand(it).format() : it }, delay))
}

/**
 *  sendCommands(cmds, delay=200)
 *
 *  Sends a list of commands directly to the device using sendHubCommand.
 *  Uses encapCommand() to apply security or CRC16 encapsulation as needed.
 **/
private sendCommands(cmds, delay=200) {
  sendHubCommand( cmds.collect{ (it instanceof physicalgraph.zwave.Command ) ? response(encapCommand(it)) : response(it) }, delay)
}

private logger(msg, level = "trace") {
  String device_name = "$device.displayName"
  String msg_text = (msg != null) ? "$msg" : "<null>"

  Integer log_level = state.defaultLogLevel ?: settings.debugLevel

  switch(level) {
    case "warn":
    if (log_level >= 2) {
      log.warn "$device_name ${msg_txt}"
    }
    sendEvent(name: "logMessage", value: "${msg_txt}", displayed: false, isStateChange: true)
    break;

    case "info":
    if (log_level >= 3) {
      log.info "$device_name ${msg_txt}"
    }
    break;

    case "debug":
    if (log_level >= 4) {
      log.debug "$device_name ${msg_txt}"
    }
    break;

    case "trace":
    if (log_level >= 5) {
      log.trace "$device_name ${msg_txt}"
    }
    break;

    case "error":
    default:
    log.error "$device_name ${msg_txt}"
    sendEvent(name: "lastError", value: "${msg}", displayed: false, isStateChange: true)
    break;
  }
}
