// vim :set ts=2 sw=2 sts=2 expandtab smarttab :
/**
 *  Ecolink Motion Sensor
 *
 *  Copyright 2016-2018 Brian Aker
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

String getDriverVersion() {
  return "v4.99"
}

Boolean isPlus() {
  return settings.newModel || state.discoveredNewModel
}

Boolean isLifeLine() {
  return state.isLifeLine ? state.isLifeLine : false
}

def getAssociationGroup () {
  if ( zwaveHubNodeId == 1 || isPlus() ) {
    return 1
  }

  return 2
}

def getConfigurationOptions(Integer model) {
  if ( model == 0x0004 || isPlus() ) {
    return [ 0x01, 0x02 ]
  }
  
  return [ 0x63 ]
}

metadata {
  definition (name: "Ecolink Motion Detector", namespace: "TangentOrgThings", author: "Brian Aker", ocfDeviceType: "x.com.st.d.sensor.motion") {
    capability "Battery"
    capability "Motion Sensor"
    capability "Sensor"
    capability "Tamper Alert"
    capability "Temperature Measurement"

    attribute "DeviceReset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message
    attribute "parseErrorCount", "number"        // Last error message
    attribute "unknownCommandErrorCount", "number"        // Last error message

    // Device Specific
    attribute "Lifeline", "string"
    attribute "Repeated", "string"

    attribute "Basic Report", "enum", ["Unconfigured", "On", "Off"]
    attribute "Lifeline Sensor Binary Report", "enum", ["Unconfigured", "On", "Off"]
    attribute "Basic Set Off", "enum", ["Unconfigured", "On", "Off"]

    // String attribute with name "firmwareVersion"
    attribute "firmwareVersion", "string"
    attribute "zWaveProtocolVersion", "string"
    attribute "driverVersion", "string"
    attribute "MSR", "string"
    attribute "Manufacturer", "string"
    attribute "Manufacturer ID", "string"
    attribute "Product Type", "string"
    attribute "Product ID", "string"

    attribute "WakeUp", "string"
    attribute "LastActive", "string"

    attribute "NIF", "string"
    attribute "SupportedSensors", "string"
    attribute "Events Supported", "string"
    attribute "Notifications Supported", "string"

    attribute "isAssociated", "enum", ["Unconfigured", "false", "true"]
    attribute "isConfigured", "enum", ["Unconfigured", "false", "true"]

    // zw:S type:2001 mfr:014A prod:0001 model:0001 ver:2.00 zwv:3.40 lib:06 cc:30,71,72,86,85,84,80,70 ccOut:20
    fingerprint type: "2001", mfr: "014A", prod: "0001", model: "0001", deviceJoinName: "Ecolink Motion Sensor PIRZWAVE1" // Ecolink motion //, cc: "30, 71, 72, 86, 85, 84, 80, 70", ccOut: "20"
    fingerprint type: "2001", mfr: "014A", prod: "0004", model: "0001", deviceJoinName: "Ecolink Motion Sensor PIRZWAVE2.5-ECO"  // Ecolink motion + // , cc: "85, 59, 80, 30, 04, 72, 71, 73, 86, 84, 5E", ccOut: "20"
  }

  simulator {
    status "inactive": "command: 3003, payload: 00"
    status "active": "command: 3003, payload: FF"
  }

  preferences {
    input name: "followupCheck", type: "bool", title: "Follow up check", description: "Follow up check to turn off after 300 seconds.", required: false, defaultValue: false
    input name: "resetUpdate", type: "bool", title: "Reset on update", description: "Reset when active on update.", required: false, defaultValue: false
    input name: "newModel", type: "bool", title: "Newer model", description: "... ", required: false, defaultValue: false
    input name: "extraDevice", type: "number", title: "Extra Device", description: "Send direct z-wave message to device", range: "1..232", displayDuringSetup: false
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false, defaultValue: 3
  }

  tiles {
    standardTile("motion", "device.motion", width: 2, height: 2) {
      state("active", label: 'motion', icon: "st.motion.motion.active", backgroundColor: "#53a7c0")
      state("inactive", label:'no motion', icon: "st.motion.motion.inactive", backgroundColor: "#ffffff")
    }

    valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
      state("battery", label: '${currentValue}', unit: "%")
    }

    valueTile("driverVersion", "device.driverVersion", width: 2, height: 2, decoration: "flat") {
      state "default", label: '${currentValue}', defaultState: true
    }

    valueTile("temperature", "device.temperature", width: 2, height: 2) {
      state("temperature", label: '${currentValue}', unit: "dF",
      backgroundColors: [
      [value: 31, color: "#153591"],
      [value: 44, color: "#1e9cbb"],
      [value: 59, color: "#90d2a7"],
      [value: 74, color: "#44b621"],
      [value: 84, color: "#f1d801"],
      [value: 95, color: "#d04e00"],
      [value: 96, color: "#bc2323"]
      ]
      )
    }

    valueTile("tamper", "device.tamper", inactiveLabel: false, decoration: "flat") {
      state "clear", backgroundColor:"#00FF00"
      state("detected", label: "detected", backgroundColor:"#e51426")
    }

    valueTile("lastActive", "device.LastActive", width:2, height:2, inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }

    main "motion"
    details(["motion", "battery", "tamper", "driverVersion", "temperature", "lastActive"])
  }
}

def getCommandClassVersions() {
  if (isPlus()) {
    return [
      0x20: 1,  // Basic
      0x22: 1,  // Application Status
      0x30: 2,  // Sensor Binary
      0x59: 1,  // Association Grp Info
      0x5A: 1,  // Device Reset Locally
      // 0x5E: 1,  // Plus
      0x70: 2,  // Configuration
      0x71: 3,  // Notification v5
      0x72: 2,  // Manufacturer Specific
      0x73: 1,  // Powerlevel
      0x80: 1,  // Battery
      0x84: 2,  // Wake Up
      0x85: 2,  // Association  0x85  V1 V2
      0x86: 1,  // Version
      0x01: 1,  // Z-wave command class
      0x25: 1,  //
      0x31: 5,  // Sensor MultLevel V1
    ]
  }

  return [
    0x20: 1,  // Basic
    0x30: 2,  // Sensor Binary
    0x70: 2,  // Configuration V1
    0x71: 2,  // Notification v2 ( Alarm V2 )
    0x72: 2,  // Manufacturer Specific V1
    0x80: 1,  // Battery
    0x84: 2,  // Wake Up
    0x85: 2,  // Association  0x85  V1 V2
    0x86: 1,  // Version
    0x01: 1,  // Z-wave command class
    0x22: 1,  // Application Status
    0x31: 1,  // Sensor MultLevel V1
  ]
}

def parse(String description) {
  def result = []

  if (description && description.startsWith("Err")) {
    logger( "Error parse() ${description}", "error" )

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
    logger("parse() called with NULL description", "warn")
  } else if (description != "updated") {
    // Z-wave event
    def timeDate = location.timeZone ? new Date().format("MMM dd EEE h:mm:ss a", location.timeZone) : new Date().format("yyyy MMM dd EEE h:mm:ss")
    state.lastActive = "$timeDate"
  
    if (1) {
      def cmds_result = []
      def cmds = checkConfigure()

      if (cmds) {
        result << response( delayBetween ( cmds ))
      }      
    }
    
    def cmd = zwave.parse(description, getCommandClassVersions())

    if (cmd) {
      zwaveEvent(cmd, result)
    } else {
      logger("zwave.parse(deviceCommandClasses()) failed for: ${description}", "parse")
      
      cmd = zwave.parse(description)
      if (cmd) {
      } else {
        logger("zwave.parse() failed for: ${description}", "error")
      }
    }
  }

  return result
}

def followupStateCheck() {
  logger("$device.displayName followupStateCheck")

  if (device.currentState("motion").value.equals("active")) {
    sendEvent(name: "motion", value: "inactive", descriptionText: "$device.displayName reset on followupStateCheck $last < $now", isStateChange: true, displayed: true)
  }
}

def sensorValueEvent(Boolean happening, result) {
  logger "sensorValueEvent() $happening"

  sendEvent(name: "LastActive", value: "${state.lastActive}")
  if (happening) {
    if (settings.followupCheck) {
      runIn(360, followupStateCheck)
    }
  }

  result << createEvent(name: "motion", value: happening ? "motion" : "inactive", isStateChange: true, displayed: true)
}

def zwaveEvent(zwave.commands.basicv1.BasicGet cmd, result) {
  logger("$cmd")

  def currentValue = device.currentState("${state.Basic}").value.equals("${state.BaiscOn}") ? 255 : 0
  result << zwave.basicV1.basicReport(value: currentValue).format()
}

def zwaveEvent(zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$cmd")

  Short value = cmd.value

  if (value == 0) {
    result << createEvent(name: "${state.Basic}", value: "${state.BasicOff}", isStateChange: true, displayed: true)
    if (device.displayName.endsWith("Dimmer")) {
      result << createEvent(name: "level", value: 0, isStateChange: true, displayed: true)
    }
  } else if (value < 100 || value == 255) {
    result << createEvent(name: "${state.Basic}", value: "${state.BasicOn}", isStateChange: true, displayed: true)
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
    result << createEvent(name: "${state.Basic}", value: "${state.BasicOff}", isStateChange: true, displayed: true)
    if (device.displayName.endsWith("Dimmer")) {
      result << createEvent(name: "level", value: 0, isStateChange: true, displayed: true)
    }
  } else if (value < 100 || value == 255) {
    result << createEvent(name: "${state.Basic}", value: "${state.BasicOn}", isStateChange: true, displayed: true)
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

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, result) {
  logger("$device.displayName $cmd")

  sensorValueEvent((Boolean)cmd.value, result)
  if (isLifeLine()) {
    logger("duplicate SwitchBinaryReport", "warn")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinarySet cmd, result) {
  logger("$device.displayName $cmd")

  sensorValueEvent((Boolean)cmd.switchValue, result)
  if (isLifeLine()) {
    logger("duplicate SwitchBinarySet", "warn")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd, result) {
  logger("$device.displayName $cmd")
  sensorValueEvent((Boolean)cmd.sensorValue, result)
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv2.SensorBinarySupportedSensorReport cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "SupportedSensors", value: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.securitypanelmodev1.SecurityPanelModeSupportedGet cmd, result) {
  result << response(zwave.securityPanelModeV1.securityPanelModeSupportedReport(supportedModeBitMask: 0))
}

// Older devices
def zwaveEvent(physicalgraph.zwave.commands.alarmv2.AlarmReport cmd, result) {
  logger("$device.displayName $cmd")

  if (cmd.alarmLevel == 0x11) {
    result << createEvent(name: "tamper", value: "detected", descriptionText: "$device.displayName covering was removed", isStateChange: true, displayed: true)
  } else {
    result << createEvent(name: "tamper", value: "clear", descriptionText: "$device.displayName is clear", isStateChange: true, displayed: true)
  }

  return result
}

// NotificationReport() NotificationReport(event: 0, eventParameter: [], eventParametersLength: 0, notificationStatus: 255, notificationType: 7, reserved61: 0, sequence: false, v1AlarmLevel: 0, v1AlarmType: 0, zensorNetSourceNodeId: 0)
def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd, result) {
  logger("$device.displayName $cmd")

  if (cmd.notificationType == 7) {
    Boolean current_status = cmd.notificationStatus == 0xFF ? true : false

    switch (cmd.event) {
      case 3:
        // sendEvent(name: "tamper", value: current_status ? "detected" : "clear", descriptionText: "$device.displayName covering was removed", isStateChange: true, displayed: true)
        result << createEvent(name: "tamper", value: current_status ? "detected" : "clear", descriptionText: "$device.displayName covering was removed", isStateChange: true, displayed: true)
        break;
      case 8:
        sensorValueEvent(true, result)
        break;
      case 0:
        sensorValueEvent(false, result)
        break;
      case 2:
        sensorValueEvent(current_status, result)
        break;
      default:
        logger("$device.displayName unknown event for notification 7: $cmd", "error")
        return
    }
  } else if (cmd.notificationType == 8) {
    logger("$device.displayName unknown notificationType: $cmd", "warn")
    result << createEvent(name: "battery", unit: "%", value: cmd.event == 0xFF ? 1 : 90, descriptionText: "Battery level", isStateChange: did_batterylevel_change)
  } else if (cmd.notificationType) {
    logger("$device.displayName unknown event for notificationType: $cmd", "error")
  } else {
    def value = cmd.v1AlarmLevel == 255 ? "active" : cmd.v1AlarmLevel ?: "inactive"
    logger("$device.displayName unknown event for v1AlarmType: $cmd", "error")
    // result << createEvent(name: "alarm $cmd.v1AlarmType", value: value, isStateChange: true, displayed: true)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.EventSupportedReport cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "Events Supported", value: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationSupportedReport cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "Notifications Supported", value: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationBusy cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationRejectedRequest cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.applicationcapabilityv1.CommandCommandClassNotSupported cmd, result) {
  logger("$device.displayName $cmd")
}

// SensorMultilevelReport(precision: 0, scale: 0, scaledSensorValue: 27, sensorType: 5, sensorValue: [27], size: 1)
def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd, result) {
  log.debug("$device.displayName $cmd")

	switch (cmd.sensorType) {
		case 1:
    result << createEvent(name: "temperature", value: cmd.scaledSensorValue, unit:"dF", isStateChange: true, displayed: true)
    break
		case 3:
    logger("Unknown Illum value ${cmd.scaledSensorValue}", "warn")
    break
		case 5:
    logger("Unknown Relative Humidity value ${cmd.scaledSensorValue}", "warn")
    break
		case 7:
    sensorValueEvent(( cmd.scaledSensorValue.toInteger() ? true : false ) , result)
    break
    default:
    logger("Unkown SensorMultilevelReport(v5) type ${cmd.sensorType}", "error")
    break;
  }
}

// SensorMultilevelReport(precision: 0, scale: 0, scaledSensorValue: 28, sensorType: 5, sensorValue: [28], size: 1)
def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv1.SensorMultilevelReport cmd, result) {
  log.debug("$device.displayName $cmd")

	switch (cmd.sensorType) {
		case 1:
    result << createEvent(name: "temperature", value: cmd.scaledSensorValue, unit:"dF", isStateChange: true, displayed: true)
    break
		case 2:
    sensorValueEvent(( cmd.scaledSensorValue.toInteger() ? true : false ) , result)
    break
		case 3:
    logger("Unknown Illum value ${cmd.scaledSensorValue}", "warn")
    break
    default:
    logger("Unkown SensorMultilevelReport(v1) type ${cmd.sensorType}", "error")
    break;
  }
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd, result) {
  logger("$device.displayName $cmd")

  if (state.tamper == "clear") {
    result << createEvent(name: "tamper", value: "clear", descriptionText: "$device.displayName is clear", isStateChange: true, displayed: true)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd, result) {
  logger("$device.displayName $cmd")

  Boolean did_batterylevel_change = state.batteryLevel != cmd.batteryLevel
  state.batteryLevel = cmd.batteryLevel

  int level
  switch(cmd.batteryLevel) {
    case 0xFF:
    level = 01
    break
    default:
    level = cmd.batteryLevel
    break
  }

  state.lastbat = new Date().time

  result << createEvent(name: "battery", unit: "%", value: level, descriptionText: "Battery level", isStateChange: did_batterylevel_change)
}

def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  logger("$device.displayName command not implemented: $cmd", "unknownCommand")
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
  logger("$device.displayName $cmd")

  String manufacturerCode = String.format("%04X", cmd.manufacturerId)
  String productTypeCode = String.format("%04X", cmd.productTypeId)
  String productCode = String.format("%04X", cmd.productId)

  state.manufacturer = "Ecolink"

  String msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)
  state.MSR = "$msr"
  
  /*
    ManufacturerCode: 014A
    productTypeId -> ProduceTypeCode: 0004
    productId -> ProductCode: 0001
  */

  state.discoveredNewModel = cmd.productTypeId == 0x0004 ? true : false
  
  if (isPlus()) {
    updateDataValue("isNewerModel", "true")
    result << createEvent(name: "Lifeline Sensor Binary Report", value: "Unknown")
    result << createEvent(name: "Basic Set Off", value: "Unknown")
    result << response(delayBetween([
      zwave.notificationV3.eventSupportedGet(notificationType: 7).format(),
      zwave.notificationV3.notificationSupportedGet().format(),
    ], 700))
    state.isLifeLine = true
  } else {
    updateDataValue("isNewerModel", "false")
    result << createEvent(name: "Basic Report", value: "Unknown")
  }
  
  Integer[] parameters = getConfigurationOptions(cmd.productTypeId)
  def cmds = []
  parameters.each {
    cmds << zwave.configurationV1.configurationGet(parameterNumber: it).format()
  }
  
  result << createEvent(name: "Manufacturer ID", value: manufacturerCode)
  result << createEvent(name: "Product Type", value: productTypeCode)
  result << createEvent(name: "Product ID", value: productCode)
  result << createEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: false)
  result << createEvent(name: "Manufacturer", value: "${state.manufacturer}", descriptionText: "$device.displayName", isStateChange: false)
  result << response( delayBetween(cmds, 1000) )
  result << response( zwave.versionV1.versionGet() )
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd, result) {
  logger("$device.displayName $cmd")

  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  def zWaveProtocolVersion = "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  result << createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: true)
  result << createEvent(name: "zWaveProtocolVersion", value: "${zWaveProtocolVersion}", descriptionText: "${device.displayName} ${zWaveProtocolVersion}", isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd, result) {
  logger("$device.displayName $cmd")

  Boolean _isConfigured = false

  if (cmd.parameterNumber == 0x63) {
    result << createEvent(name: "Basic Report", value: cmd.configurationValue == 0xFF ? "On" : "Off", displayed: true)
    _isConfigured = true
  } else if (cmd.parameterNumber == 0x01) {
  / *
      0x00 (Default) Sensor does NOT send Basic Sets to Node IDs in Association Group 2 when the sensor is restored (i.e. Motion Not Detected ).
      0xFF Sensor sends Basic Sets of 0x00 to nodes in Association Group2 when sensor is restored.
    */
    result << createEvent(name: "Basic Set Off", value: cmd.configurationValue == 0xFF ? "On" : "Off", displayed: true)
    _isConfigured = true
  } else if (cmd.parameterNumber == 0x02) {
  /*
      0x00 (Default) Sensor sends Sensor Binary Reports when sensor is faulted and restored for backwards compatibility in addition to Notification Reports.
      0xFF Sensor will send only Notification Reports and NOT Sensor Binary Reports when the sensor is faulted and restored.
   */
    result << createEvent(name: "Lifeline Sensor Binary Report", value: cmd.configurationValue == 0xFF ? "Off" : "On", displayed: true);

    if (0) { // Disable default setting of...
      if ( cmd.configurationValue == 0x00 ) {
        result << response(delayBetween([
        zwave.configurationV1.configurationSet(parameterNumber: cmd.parameterNumber, scaledConfigurationValue: 0xFF, size: 1).format(),
        zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format(),
        ], 800))
      }
    }
    _isConfigured = true
  } else {
    logger("$device.displayName Unknown parameterNumber number ${cmd.parameterNumber}", "error")
    return
  }

  if ( zwaveHubNodeId != 1 && ! isPlus() && device.currentValue("MSR")) {
    if (! _isConfigured) {
      result << response(delayBetween([
        zwave.configurationV1.configurationSet(parameterNumber: 0x63, scaledConfigurationValue: 0xFF, size: 1).format(),
        zwave.configurationV1.configurationGet(parameterNumber: 0x63).format(),
      ], 1000))
    }
  }

  if (_isConfigured) {
    setConfigured()
  } else {
    unConfigured()
  }
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd, result ) {
  logger("$device.displayName $cmd")

  if (cmd.supportedGroupings) {
    def cmds = []
    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      if (0) {
        cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x);
        cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: 0x00);
        cmds << zwave.associationGrpInfoV1.associationGroupCommandListGet(allowCache: true, groupingIdentifier: x);
      }
      cmds << zwave.associationV2.associationGet(groupingIdentifier: x);
    }

    sendCommands(cmds, 2000)
  } else {
    logger("$device.displayName reported no groups", "error")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$device.displayName $cmd")

  def cmds = []

  String final_string = ""
  
  // Association
  if (cmd.nodeId) {
    def string_of_assoc = ""
    cmd.nodeId.each {
      string_of_assoc += "${it}, "
    }
    def lengthMinus2 = ( string_of_assoc.length() > 3 ) ? string_of_assoc.length() - 3 : 0
    final_string = lengthMinus2 ? string_of_assoc.getAt(0..lengthMinus2) : string_of_assoc
  }

  updateDataValue("Group #${cmd.groupingIdentifier}", "${final_string}")

  Boolean isAssociated = false

  if (cmd.groupingIdentifier == 0x01) { // Lifeline
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      isAssociated = true
    }
    result << createEvent(name: "Lifeline",
          value: "${final_string}",
          displayed: true,
          isStateChange: true)
  } else if (cmd.groupingIdentifier == 0x02) { // Repeated
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      if (getAssociationGroup() == 1) {
        cmds << zwave.associationV1.associationRemove(groupingIdentifier: cmd.groupingIdentifier, nodeId: zwaveHubNodeId).format()
      }

      if (getAssociationGroup() == 2) {
        isAssociated = true
      }
    }

    if (settings.extraDevice) {
      if (cmd.nodeId.any { it == settings.extraDevice }) {
      } else {
        cmds << zwave.associationV1.associationSet(groupingIdentifier: cmd.groupingIdentifier, nodeId: [ settings.extraDevice ]).format();
        cmds << zwave.associationV1.associationGet(groupingIdentifier: cmd.groupingIdentifier).format();
      }
    }

    result << createEvent(name: "Repeated",
      value: "${final_string}",
      displayed: true,
      isStateChange: true)
  } else {
    logger("$device.displayName unknown association: $cmd", "error")
    return
  }

  if (! isAssociated ) {
    if (getAssociationGroup() == cmd.groupingIdentifier ) {
      unAssociated()
      cmds << zwave.associationV1.associationSet(groupingIdentifier: getAssociationGroup(), nodeId: [zwaveHubNodeId]).format()
      cmds << zwave.associationV1.associationGet(groupingIdentifier: 1).format()
      cmds << zwave.associationV1.associationGet(groupingIdentifier: 2).format()
    }
  } else {
    if (getAssociationGroup() == cmd.groupingIdentifier ) {
      setAssociated()
    }
  }

  if (cmds.size()) {
    result << response(delayBetween(cmds, 700))
  }
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "DeviceReset", value: "true", descriptionText: cmd.toString(), isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def checkConfigure() {
  def cmds = []

  if (state.checkConfigure && ((new Date().time) - state.checkConfigure) < 120 ) {
    return
  }
  state.checkConfigure = new Date().time

  if (isConfigured()) {
  } else {
    if (! state.lastConfigure || (new Date().time) - state.lastConfigure > 1500) {
      if (isPlus()) {
        cmds << zwave.manufacturerSpecificV2.manufacturerSpecificGet().format()
      } else {
        cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
      }

      if (0) {
        cmds << zwave.wakeUpV1.wakeUpIntervalGet().format()
      }
    }

    state.lastConfigure = new Date().time
    updateDataValue("Last Configured", "${state.lastActive}")
  }

  if (isAssociated()) {
  } else {
    if (!state.lastAssociated || (new Date().time) - state.lastAssociated > 1500) {
      cmds << zwave.associationV2.associationGroupingsGet().format()
    }

    state.lastAssociated = new Date().time
    updateDataValue("Last Associated", "${state.lastActive}")
  }

  if (0) { // Misconfigure fix
    cmds << zwave.associationV1.associationRemove(groupingIdentifier: 2, nodeId: 1).format();
    cmds << zwave.associationV1.associationGet(groupingIdentifier: 1).format();
    cmds << zwave.associationV1.associationGet(groupingIdentifier: 2).format();
  }

  return cmds
}

def prepDevice() {
  [
    zwave.manufacturerSpecificV1.manufacturerSpecificGet(),
    zwave.associationV2.associationGroupingsGet(),
    zwave.zwaveCmdClassV1.requestNodeInfo(),
    // zwave.configurationV1.configurationGet(parameterNumber: 0x63),
  ]
}

def updated() {
  if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
    return
  }
  log.info("$device.displayName updated() debug: ${settings.debugLevel}")

  state.Basic = "motion"
  state.BasicOn = "active"
  state.BasicOff = "inactive"

  if (0) {
    def zwInfo = getZwaveInfo()
    if ($zwInfo) {
      logger("$device.displayName $zwInfo", "info")
      sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
    }
  }

  sendEvent(name: "tamper", value: "clear")

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)
  sendEvent(name: "parseErrorCount", value: 0, displayed: false)
  sendEvent(name: "unknownCommandErrorCount", value: 0, displayed: false)
  state.parseErrorCount = 0
  state.unknownCommandErrorCount = 0

  if (settings.resetUpdate) {
    sendEvent(name: "motion", value: "inactive", isStateChange: true, displayed: true)
  }

  sendEvent(name: "driverVersion", value: getDriverVersion(), displayed: true, isStateChange: true)
  initIsAssociated()
  initIsConfigured()

  // Avoid calling updated() twice
  state.updatedDate = Calendar.getInstance().getTimeInMillis()
}

def installed() {
  log.debug "$device.displayName installed()"

  state.Basic = "motion"
  state.BasicOn = "active"
  state.BasicOff = "inactive"

  if (0) {
    def zwInfo = getZwaveInfo()
    if ($zwInfo) {
      logger("$device.displayName $zwInfo", "info")
      sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
    }
  }

  initIsAssociated()
  initIsConfigured()

  sendEvent(name: "driverVersion", value: getDriverVersion(), displayed: true, isStateChange: true)
  sendCommands(prepDevice())
}

/*****************************************************************************************************************
 *  Private Helper Functions:
 *****************************************************************************************************************/

private initIsConfigured() {
  sendEvent(name: "isConfigured", value: "Unconfigured", displayed: true)
}

private unConfigured() {
  sendEvent(name: "isConfigured", value: "false", displayed: true)
}

private setConfigured() {
  sendEvent(name: "isConfigured", value: "true", displayed: true)
}

private isConfigured() {
  if (device.currentValue("isConfigured") && device.currentValue("isConfigured").toBoolean() == true) {
    return true
  }

  return false
}

private initIsAssociated() {
  sendEvent(name: "isAssociated", value: "Unconfigured", displayed: true)
}

private unAssociated() {
  sendEvent(name: "isAssociated", value: "false", displayed: true)
}

private setAssociated() {
  sendEvent(name: "isAssociated", value: "true", displayed: true)
}

private isAssociated() {
  if (device.currentValue("isAssociated") && device.currentValue("isAssociated").toBoolean() == true) {
    return true
  }

  return false
}

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
private prepCommands(cmds, delay=200) {
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
 *    messages by sending events for the device's logMessage attribute.
 *    Configured using configLoggingLevelIDE and configLoggingLevelDevice preferences.
 **/
private logger(msg, level = "trace") {
  switch(level) {
    case "unknownCommand":
    state.unknownCommandErrorCount += 1
    sendEvent(name: "unknownCommandErrorCount", value: unknownCommandErrorCount, displayed: false, isStateChange: true)
    break

    case "parse":
    state.parseErrorCount += 1
    sendEvent(name: "parseErrorCount", value: parseErrorCount, displayed: false, isStateChange: true)
    break

    case "warn":
    if (settings.debugLevel >= 2) {
      log.warn msg
      sendEvent(name: "logMessage", value: "WARNING: ${msg}", displayed: false, isStateChange: true)
    }
    return

    case "info":
    if (settings.debugLevel >= 3) {
      log.info msg
    }
    return

    case "debug":
    if (settings.debugLevel >= 4) {
      log.debug msg
    }
    return

    case "trace":
    if (settings.debugLevel >= 5) {
      log.trace msg
    }
    return

    case "error":
    default:
    break
  }

  log.error msg
  sendEvent(name: "lastError", value: "ERROR: ${msg}", displayed: false, isStateChange: true)
}
