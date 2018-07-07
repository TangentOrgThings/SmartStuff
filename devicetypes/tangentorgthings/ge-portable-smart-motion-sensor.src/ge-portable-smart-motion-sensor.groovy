// vim :set ts=2 sw=2 sts=2 expandtab smarttab :
/**
 *  GE Portable Smart Motion Sensor
 *
 *  Copyright 2018 Brian Aker <brian@tangent.org>
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
 *  Author: SmartThings
 *  Date: 2013-11-25
 */

 def getDriverVersion () {
  return "v1.05"
}

def getConfigurationOptions(Integer model) {
  return [ 13, 18, 20, 28 ]
}

metadata {
  definition (name: "GE Portable Smart Motion Sensor", namespace: "TangentOrgThings", author: "brian@tangent.org", ocfDeviceType: "x.com.st.d.sensor.motion") {
    capability "Motion Sensor"
    capability "Sensor"
    capability "Battery"

    attribute "Lifeline", "string"

    attribute "configured", "enum", ["false", "true"]
    attribute "driverVersion", "string"
    attribute "firmwareVersion", "string"
    attribute "zWaveProtocolVersion", "string"
    attribute "FirmwareMdReport", "string"
    attribute "Manufacturer", "string"
    attribute "ManufacturerCode", "string"
    attribute "MSR", "string"
    attribute "NIF", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"

    attribute "reset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message

    attribute "Power", "string"

    fingerprint mfr: "0063", prod: "4953", model: "3133", deviceJoinName: "GE Portable Smart Motion Sensor"
  }

  simulator {
    status "inactive": "command: 3003, payload: 00"
    status "active": "command: 3003, payload: FF"
  }

  preferences {
    input name: "parameterThirteen", type: "number", title: "PIR Sensitivity", description: "PIR Sensitivity: default=3, 1=Low Sensitivity, 2=Medium Sensitivity, 3=High Sensitivity",  defaultValue: 3, range: "1..3", required: false, displayDuringSetup: true
    input name: "parameterEighteen", type: "number", title: "PIR Timeout Duration", description: "PIR Timeout Duration (minutes): default=4, 1-60",  defaultValue: 4, range: "1..60", required: false, displayDuringSetup: true
    input name: "parameterTwenty", type: "number", title: "Controlled Class", description:"Basic Set, Notification and Basic Report: default=1, 1=Notification, 2=Basic Set, 3=Basic Report",  defaultValue: 1, range: "1..3", required: false, displayDuringSetup: true
    input name: "parameterTwentyEight", type: "number", title: "LED Flash Indicator" , description: "Enable/Disable LED Flash Indicator: default=1, 0=Disable, 1=Enable",  defaultValue: 1, range: "0..1", required: false, displayDuringSetup: true
  }

  tiles(scale: 2) {
    multiAttributeTile(name:"motion", type: "generic", width: 6, height: 4) {
      tileAttribute("device.motion", key: "PRIMARY_CONTROL") {
        attributeState("active", label:'motion', icon:"st.motion.motion.active", backgroundColor:"#00A0DC")
        attributeState("inactive", label:'no motion', icon:"st.motion.motion.inactive", backgroundColor:"#CCCCCC")
      }
    }

    valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state("battery", label:'${currentValue}% battery', unit:"")
    }

    main "motion"
    details(["motion", "battery"])
  }
}

private getCommandClassVersions() {
  [
    0x20: 1,  // Basic
    0x30: 1,  //
    0x31: 5,  //
    0x59: 1,  // Association Grp Info
    0x5A: 1,  // Device Reset Locally
    // 0x5E: 2, //
    0x70: 1,  // Configuration
    0x71: 3,  // Notification
    0x72: 2,  // Manufacturer Specific
    0x73: 1,  // Powerlevel
    0x7A: 2,  // Firmware Update Md HS-200 V4
    0x80: 1,  //
    0x84: 1,  //
    0x85: 2,  // Association  0x85  V1 V2    
    0x86: 1,  // Version
    0x9C: 1,  //
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
    logger("$device.displayName parse() called with NULL description", "info")
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

def sensorValueEvent(value, result) {
  if (value) {
    result << createEvent(name: "motion", value: "active", descriptionText: "$device.displayName detected motion")
  } else {
    result << createEvent(name: "motion", value: "inactive", descriptionText: "$device.displayName motion has stopped")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$device.displayName $cmd")
  sensorValueEvent(cmd.value, result)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$device.displayName $cmd")
  sensorValueEvent(cmd.value, result)
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, result) {
  logger("$device.displayName $cmd")
  sensorValueEvent(cmd.value, result)
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd, result) {
  logger("$device.displayName $cmd")
  sensorValueEvent(cmd.sensorValue, result)
}

def zwaveEvent(physicalgraph.zwave.commands.sensoralarmv1.SensorAlarmReport cmd, result) {
  logger("$device.displayName $cmd")
  sensorValueEvent(cmd.sensorState, result)
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd, result) {
  logger("$device.displayName $cmd")

  if (cmd.notificationType == 0x07) {
    if (cmd.v1AlarmType == 0x07) {  // special case for nonstandard messages from Monoprice ensors
      result << sensorValueEvent(cmd.v1AlarmLevel)
    } else if (cmd.event == 0x01 || cmd.event == 0x02 || cmd.event == 0x07 || cmd.event == 0x08) {
      result << sensorValueEvent(1)
    } else if (cmd.event == 0x00) {
      result << sensorValueEvent(0)
    } else if (cmd.event == 0x03) {
      result << createEvent(name: "tamper", value: "detected", descriptionText: "$device.displayName covering was removed", isStateChange: true)
      result << response(zwave.batteryV1.batteryGet())
    } else if (cmd.event == 0x05 || cmd.event == 0x06) {
      result << createEvent(descriptionText: "$device.displayName detected glass breakage", isStateChange: true)
    }
  } else if (cmd.notificationType) {
    def text = "Notification $cmd.notificationType: event ${([cmd.event] + cmd.eventParameter).join(", ")}"
    result << createEvent(name: "notification$cmd.notificationType", value: "$cmd.event", descriptionText: text, isStateChange: true, displayed: false)
  } else {
    def value = cmd.v1AlarmLevel == 255 ? "active" : cmd.v1AlarmLevel ?: "inactive"
    result << createEvent(name: "alarm $cmd.v1AlarmType", value: value, isStateChange: true, displayed: false)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd, result) {
  logger("$device.displayName $cmd")

  if (!state.lastbat || (new Date().time) - state.lastbat > 53*60*60*1000) {
    result << response(zwave.batteryV1.batteryGet())
  }
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd, result) {
  logger("$device.displayName $cmd")

  def map = [ name: "battery", unit: "%" ]
  if (cmd.batteryLevel == 0xFF) {
    map.value = 1
    map.descriptionText = "${device.displayName} has a low battery"
    map.isStateChange = true
  } else {
    map.value = cmd.batteryLevel
  }
  state.lastbat = new Date().time

  result << createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd, result) {
  def map = [ displayed: true, value: cmd.scaledSensorValue.toString() ]
  switch (cmd.sensorType) {
    case 1:
      map.name = "temperature"
      map.unit = cmd.scale == 1 ? "F" : "C"
      break;
    case 3:
      map.name = "illuminance"
      map.value = cmd.scaledSensorValue.toInteger().toString()
      map.unit = "lux"
      break;
    case 5:
      map.name = "humidity"
      map.value = cmd.scaledSensorValue.toInteger().toString()
      map.unit = cmd.scale == 0 ? "%" : ""
      break;
    case 0x1E:
      map.name = "loudness"
      map.unit = cmd.scale == 1 ? "dBA" : "dB"
      break;
  }

  result << createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
  logger("$device.displayName $cmd")

  if ( cmd.manufacturerName ) {
    state.manufacturer= cmd.manufacturerName
  } else {
    state.manufacturer= "GE"
  }

  state.manufacturer= cmd.manufacturerName

  def manufacturerCode = String.format("%04X", cmd.manufacturerId)
  def productTypeCode = String.format("%04X", cmd.productTypeId)
  def productCode = String.format("%04X", cmd.productId)

  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)
  updateDataValue("manufacturer", "${state.manufacturer}")

  Integer[] parameters = getConfigurationOptions(cmd.productId)

  def cmds = []
  parameters.each {
    cmds << zwave.configurationV1.configurationGet(parameterNumber: it).format()
  }

  result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
  result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
  result << createEvent(name: "ProductCode", value: productCode)
  result << createEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: false)
  result << createEvent(name: "Manufacturer", value: "${state.manufacturer}", descriptionText: "$device.displayName", isStateChange: false)
  result << response(delayBetween(cmds, 1000))
  result << response( zwave.versionV1.versionGet() )
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd, result) {
  logger("$device.displayName $cmd")

  switch (cmd.parameterNumber) {
    case 13:
    logger("PIR Sensitiivity Setting ${cmd.configurationValue[0]}")
    break;
    case 18:
    logger("PIR Timeout Duration ${cmd.configurationValue[0]}")
    break;
    case 20:
    logger("Basic Set, Notification and Basic Report ${cmd.configurationValue[0]}")
    break;
    case 28:
    logger("Enable/Disable the LED indication when PIR is triggered ${cmd.configurationValue[0]}")
    break;
    default:
    logger("$device.displayName has unknown configuration parameter $cmd.parameterNumber : $cmd.configurationValue[0]", "error")
    break;
  }
}

def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelReport cmd, result) {
  logger("zwaveEvent(): Powerlevel Report received: ${cmd}")
  def device_power_level = (cmd.powerLevel > 0) ? "minus${cmd.powerLevel}dBm" : "NormalPower"
  logger("Powerlevel Report: Power: ${device_power_level}, Timeout: ${cmd.timeout}", "info")
  result << createEvent(name: "Power", value: device_power_level)
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd, result) {
  logger("$device.displayName $cmd")

  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  def zWaveProtocolVersion = "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  result << createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: true)
  result << createEvent(name: "zWaveProtocolVersion", value: "${zWaveProtocolVersion}", descriptionText: "${device.displayName} ${zWaveProtocolVersion}", isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd, result) {
  logger("$device.displayName $cmd")
  def firmware_report = String.format("%s-%s-%s", cmd.checksum, cmd.firmwareId, cmd.manufacturerId)
  updateDataValue("FirmwareMdReport", firmware_report)
  result << createEvent(name: "FirmwareMdReport", value: firmware_report, descriptionText: "$device.displayName FIRMWARE_REPORT: $firmware_report", displayed: true, isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "DeviceReset", value: "true", descriptionText: cmd.toString(), isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd, result) {
  logger("$device.displayName $cmd")

  state.groups = cmd.supportedGroupings

  if (cmd.supportedGroupings) {
    def cmds = []
    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x).format()
      cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: 0x00).format()
      cmds << zwave.associationGrpInfoV1.associationGroupCommandListGet(allowCache: true, groupingIdentifier: x).format()
    }

    result << response(delayBetween(cmds, 2000))

    return
  }

  logger("$device.displayName AssociationGroupingsReport: $cmd", "error")
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd, result) {
  logger("$device.displayName $cmd")

  def name = new String(cmd.name as byte[])
  logger("Association Group #${cmd.groupingIdentifier} has name: ${name}", "info")

  result << response( zwave.associationV1.associationGet(groupingIdentifier: cmd.groupingIdentifier) )
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupCommandListReport cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$device.displayName $cmd")

  Boolean isStateChange
  String event_value
  String event_descriptionText

  if (cmd.groupingIdentifier != 1) {
    logger("Unknown Group Identifier", "error");
    return
  }

  // Lifeline
  def string_of_assoc = ""
  cmd.nodeId.each {
    string_of_assoc += "${it}, "
  }
  def lengthMinus2 = ( string_of_assoc.length() > 3 ) ? string_of_assoc.length() - 3 : 0
  def final_string = lengthMinus2 ? string_of_assoc.getAt(0..lengthMinus2) : string_of_assoc

  event_value = "${final_string}"

  if (cmd.nodeId.any { it == zwaveHubNodeId }) {
    isStateChange = state.isAssociated == true ? false : true
    event_descriptionText = "Device is associated"
    state.isAssociated = true
  } else {
    isStateChange = state.isAssociated == false ? false : true
    event_descriptionText = "Hub was not found in lifeline"
    state.isAssociated = false

    result << response( zwave.associationV1.associationSet(groupingIdentifier: cmd.groupingIdentifier, nodeId: zwaveHubNodeId) )
  }

  // result << createEvent(name: "Lifeline",
  sendEvent(name: "Lifeline",
      value: event_value,
      descriptionText: event_descriptionText,
      displayed: true,
      isStateChange: true) // isStateChange)
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  logger("$device.displayName command not implemented: $cmd", "error")
}

def prepDevice() {
  [
    zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
    zwave.firmwareUpdateMdV2.firmwareMdGet(),
    zwave.associationV2.associationGroupingsGet(),
    zwave.powerlevelV1.powerlevelGet(),
  ]
}

def installed() {
  log.debug ("installed()")
  state.loggingLevelIDE = settings.debugLevel ? settings.debugLevel : 4

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  sendCommands(prepDevice())
}

def updated() {
  state.loggingLevelIDE = settings.debugLevel ? settings.debugLevel : 4
  log.info("$device.displayName updated() debug: ${state.loggingLevelIDE}")

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  sendCommands(prepDevice())
}


def configure() {
  log.debug("Adjusting parameters...")
  sendCommands([
    zwave.configurationV1.configurationSet(configurationValue: [parameterThirteen], parameterNumber: 13, size: 3),
    zwave.configurationV1.configurationSet(configurationValue: [parameterEighteen], parameterNumber: 18, size: 1),
    zwave.configurationV1.configurationSet(configurationValue: [parameterTwenty], parameterNumber: 20, size: 4),
    zwave.configurationV1.configurationSet(configurationValue: [parameterTwentyEight], parameterNumber: 28, size: 1),
  ])
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
/**
 *  logger()
 *
 *  Wrapper function for all logging:
 *    Logs messages to the IDE (Live Logging), and also keeps a historical log of critical error and warning
 *    messages by sending events for the device's logMessage attribute and lastError attribute.
 *    Configured using configLoggingLevelIDE and configLoggingLevelDevice preferences.
 **/
private logger(msg, level = "debug") {
  switch(level) {
    case "error":
    if (state.loggingLevelIDE >= 1) {
      log.error msg
    }
    if (state.loggingLevelDevice >= 1) {
      sendEvent(name: "lastError", value: "ERROR: ${msg}", displayed: false, isStateChange: true)
    }
    break

    case "warn":
    if (state.loggingLevelIDE >= 2) {
      log.warn msg
    }
    if (state.loggingLevelDevice >= 2) {
      sendEvent(name: "logMessage", value: "WARNING: ${msg}", displayed: false, isStateChange: true)
    }
    break

    case "info":
    if (state.loggingLevelIDE >= 3) log.info msg
      break

    case "debug":
    if (state.loggingLevelIDE >= 4) log.debug msg
      break

    case "trace":
    if (state.loggingLevelIDE >= 5) log.trace msg
      break

    default:
    log.debug msg
    break
  }
}
