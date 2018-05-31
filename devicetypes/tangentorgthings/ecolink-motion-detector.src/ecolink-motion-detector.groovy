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

def getDriverVersion() {
  return "v4.42"
}

def isPlus() {
  return settings.newModel || state.newModel
}

def isLifeLine() {
  return state.isLifeLine
}

def getAssociationGroup () {
  if ( zwaveHubNodeId == 1 || isPlus() || isLifeLine()) {
    return 1
  }

  return 2
}

def getParamater() {
  return 0x63
}

metadata {
  definition (name: "Ecolink Motion Detector", namespace: "TangentOrgThings", author: "Brian Aker", ocfDeviceType: "x.com.st.d.sensor.motion") {
    capability "Battery"
    capability "Motion Sensor"
    capability "Sensor"
    capability "Tamper Alert"
    capability "Temperature Measurement"

    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last Error  messages.

    // Device Specific
    attribute "Lifeline", "string"
    attribute "Repeated", "string"
    attribute "BasicReport", "enum", ["Unconfigured", "On", "Off"]

    // String attribute with name "firmwareVersion"
    attribute "firmwareVersion", "string"
    attribute "zWaveProtocolVersion", "string"
    attribute "driverVersion", "string"
    attribute "MSR", "string"
    attribute "Manufacturer", "string"
    attribute "ManufacturerCode", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"

    attribute "WakeUp", "string"
    attribute "LastActive", "string"

    attribute "NIF", "string"
    attribute "SupportedSensors", "string"

    // zw:S type:2001 mfr:014A prod:0001 model:0001 ver:2.00 zwv:3.40 lib:06 cc:30,71,72,86,85,84,80,70 ccOut:20
    fingerprint type: "2001", mfr: "014A", prod: "0001", model: "0001", deviceJoinName: "Ecolink Motion Sensor PIRZWAVE1" // Ecolink motion //, cc: "30, 71, 72, 86, 85, 84, 80, 70", ccOut: "20"
    fingerprint type: "2001", mfr: "014A", prod: "0004", model: "0001", deviceJoinName: "Ecolink Motion Sensor PIRZWAVE2.5-ECO"  // Ecolink motion + // , cc: "85, 59, 80, 30, 04, 72, 71, 73, 86, 84, 5E", ccOut: "20"
  }

  simulator {
    status "inactive": "command: 3003, payload: 00"
    status "active": "command: 3003, payload: FF"
  }

  preferences {
    input name: "newModel", type: "bool", title: "Newer model", description: "... ", required: false, defaultValue: false
    input name: "followupCheck", type: "bool", title: "Newer model", description: "... ", required: false, defaultValue: false
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false, defaultValue: 3
  }

  tiles {
    standardTile("motion", "device.motion", width: 2, height: 2) {
      state("active", label:'motion', icon:"st.motion.motion.active", backgroundColor:"#53a7c0")
      state("inactive", label:'no motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff")
    }

    valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
      state("battery", label:'${currentValue}', unit:"%")
    }

    valueTile("driverVersion", "device.driverVersion", inactiveLabel: true, decoration: "flat") {
      state("driverVersion", label: getDriverVersion())
    }

    valueTile("associated", "device.Associated", inactiveLabel: false, decoration: "flat") {
      state("device.Associated", label: '${currentValue}')
    }

    valueTile("temperature", "device.temperature", width: 2, height: 2) {
      state("temperature", label:'${currentValue}', unit:"dF",
      backgroundColors:[
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
    details(["motion", "battery", "tamper", "driverVersion", "temperature", "associated", "lastActive"])
  }
}

private deviceCommandClasses() {
  if (isPlus()) {
    return [
      0x20: 1,  // Basic
      0x22: 1,  // Application Status
      0x30: 1,  // Sensor Binary
      0x59: 1,  // Association Grp Info
      // 0x5E: 1,  // Plus
      0x70: 2,  // Configuration
      0x71: 3,  // Notification v5
      0x72: 2,  // Manufacturer Specific
      0x73: 1, // Powerlevel
      0x80: 1, // Battery
      0x84: 2, // Wake Up
      0x85: 2,  // Association  0x85  V1 V2
      0x86: 1,  // Version
      0x01: 1,  // Z-wave command class
      0x25: 1,  //
      0x31: 1,  //
    ]
  } else {
    return [
      0x20: 1,  // Basic
      0x70: 1,  // Configuration
      0x71: 3,  // Notification v2
      0x72: 2,  // Manufacturer Specific V1
      0x80: 1,  // Battery
      0x84: 2,  // Wake Up
      0x85: 2,  // Association  0x85  V1 V2
      0x86: 1,  // Version
      0x01: 1,  // Z-wave command class
      0x22: 1,  // Application Status
    ]
  }
}

def parse(String description) {
  def result = []

  if (description && description.startsWith("Err")) {
    log.error "parse error: ${description}"
    result << createEvent(name: "lastError", value: "Error parse() ${description}", descriptionText: description)

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
    result << createEvent(name: "logMessage", value: "parse() called with NULL description", descriptionText: "$device.displayName")
  } else if (description != "updated") {
    def cmd = zwave.parse(description, deviceCommandClasses())

    if (cmd) {
      def cmds_result = []
      def cmds = checkConfigure()

      if (cmds) {
        result << response( delayBetween ( cmds ))
      }
      zwaveEvent(cmd, result)
    } else {
      logger("zwave.parse(deviceCommandClasses()) failed for: ${description}", "error")
      
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

  if (state.isHappening) { // No lock checking, this is not a critical operation
    def now = new Date().time
    def last = state.lastActive + 300

    log.debug("$device.displayName ... $last < $now")
    if (state.lastActive + 300 < now) {
      sendEvent(name: "motion", value: "inactive", descriptionText: "$device.displayName reset on followupStateCheck", isStateChange: followupCheck, displayed: true)
    }
  }
}

def sensorValueEvent(Boolean happening, result) {
  logger "sensorValueEvent() $happening"

  if (happening) {
    state.lastActive = new Date().time
    sendEvent(name: "LastActive", value: state.lastActive, displayed: false)

    runIn(360, followupStateCheck)
  }

  result << createEvent(name: "motion", value: happening ? "motion" : "inactive", descriptionText: "$device.displayName active", isStateChange: true, displayed: true)
  state.isHappening = happening

  return result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$device.displayName $cmd")

  if (! isLifeLine()) {
    sensorValueEvent((Boolean)cmd.value, result)
  } else {
    result << createEvent(descriptionText: "duplicate event", isStateChange: false, displayed: false)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$device.displayName $cmd")

  if (! isLifeLine()) {
    sensorValueEvent((Boolean)cmd.value, result)
  } else {
    result << createEvent(descriptionText: "duplicate event", isStateChange: false, displayed: false)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, result) {
  logger("$device.displayName $cmd")

  if (! isLifeLine()) {
    sensorValueEvent((Boolean)cmd.value, result)
  } else {
    result << createEvent(descriptionText: "duplicate event", isStateChange: false, displayed: false)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinarySet cmd, result) {
  logger("$device.displayName $cmd")

  if (! isLifeLine()) {
    sensorValueEvent((Boolean)cmd.sensorValue, result)
  } else {
    result << createEvent(descriptionText: "duplicate event", isStateChange: false, displayed: false)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd, result) {
  logger("$device.displayName $cmd")
  sensorValueEvent((Boolean)cmd.sensorValue, result)
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv2.SensorBinarySupportedSensorReport cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "SupportedSensors", value: "$cmd", descriptionText: "$device.displayName", isStateChange: true, displayed: true)
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
    Boolean current_status = cmd.notificationStatus == 255 ? true : false

    switch (cmd.event) {
      case 3:
        sendEvent(name: "tamper", value: current_status ? "detected" : "clear", descriptionText: "$device.displayName covering was removed", isStateChange: true, displayed: true)
        result << createEvent(name: "tamper", value: current_status ? "detected" : "clear", descriptionText: "$device.displayName covering was removed", isStateChange: true, displayed: true)
        break;
      case 8:
        result << sensorValueEvent(true, result)
        break;
      case 0:
        result << sensorValueEvent(false, result)
        break;
      case 2:
        result << sensorValueEvent(current_status, result)
        break;
      default:
        result << createEvent(descriptionText: "$device.displayName unknown event for notification 7: $cmd", isStateChange: true)
        log.error "Unknown state: $cmd"
    }
  } else if (cmd.notificationType == 8) {
    result << createEvent(descriptionText: "$device.displayName unknown notificationType: $cmd", isStateChange: true)
    log.error "Unknown state for notificationType 8: $cmd"
  } else if (cmd.notificationType) {
    def text = "Unknown state for notificationType: $cmd"
    result << createEvent(name: "notification$cmd.notificationType", value: "$text", descriptionText: text, isStateChange: true, displayed: true)
    log.error "Unknown notificationType: $cmd"
  } else {
    def value = cmd.v1AlarmLevel == 255 ? "active" : cmd.v1AlarmLevel ?: "inactive"
    result << createEvent(name: "alarm $cmd.v1AlarmType", value: value, isStateChange: true, displayed: true)
  }

  return result
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationBusy cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationRejectedRequest cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.applicationcapabilityv1.CommandCommandClassNotSupported cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd, result) {
  log.debug("$device.displayName $cmd")

  if (cmd.sensorType == 1) {
    result << createEvent(name: "temperature", value: cmd.scaledSensorValue, unit:"dF", isStateChange: true, displayed: true)
  } else if (cmd.sensorType == 7) {
    Boolean current_status = cmd.notificationStatus == 255 ? true : false
    result << sensorValueEvent(current_status, result)
  } else {
    result << createEvent(descriptionText: "$cmd", isStateChange: true, displayed: true)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd, result) {
  logger("$device.displayName $cmd")

  result << createEvent(descriptionText: "${device.displayName} woke up", isStateChange: false)

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
  log.error("$device.displayName command not implemented: $cmd")
  result << createEvent(descriptionText: "$device.displayName command not implemented: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
  logger("$device.displayName $cmd")

  def manufacturerCode = String.format("%04X", cmd.manufacturerId)
  def productTypeCode = String.format("%04X", cmd.productTypeId)
  def productCode = String.format("%04X", cmd.productId)

  state.manufacturer = "Ecolink"

  Integer[] parameters = [ 63 ]

  def cmds = []
  parameters.each {
    cmds << zwave.configurationV1.configurationGet(parameterNumber: it).format()
  }

  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)

  state.newModel = cmd.productId == 4 ? true : false

  result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
  result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
  result << createEvent(name: "ProductCode", value: productCode)
  result << createEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: false)
  result << createEvent(name: "Manufacturer", value: "${state.manufacturer}", descriptionText: "$device.displayName", isStateChange: false)
  result << response(delayBetween(cmds, 1000))
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

  if (cmd.parameterNumber == 0x63) {
    if (cmd.configurationValue == 0xFF) {
      result << createEvent(name: "BasicReport", value: "On", displayed: false)
      state.isConfigured = true
    } else if (cmd.configurationValue != 0) {
      // Attempting to be On but is misconfigured
      result << createEvent(name: "BasicReport", value: "Off", displayed: false)
      state.isConfigured = false
    } else {
      result << createEvent(name: "BasicReport", value: "Off", displayed: false)
      state.isConfigured = false
    }
  } else {
    result << createEvent(name: "BasicReport", value: "Unconfigured", displayed: false)
  }

  if ( zwaveHubNodeId != 1 && ! isPlus() && device.currentValue("MSR")) {
    if (! state.isConfigured) {
      result << response(delayBetween([
        zwave.configurationV1.configurationSet(parameterNumber: 0x63, scaledConfigurationValue: 0xFF, size: 1).format(),
        zwave.configurationV1.configurationGet(parameterNumber: 0x63).format(),
      ], 1000))
    }
  }

  return result
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd, result ) {
  logger("$device.displayName $cmd")

  state.groups = cmd.supportedGroupings

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
    result << createEvent(descriptionText: "$device.displayName reported no groups", isStateChange: true, displayed: true)
  }

  return result
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$device.displayName $cmd")

  def cmds = []

  String final_string

  if (cmd.nodeId) {
    def string_of_assoc = ""
    cmd.nodeId.each {
      string_of_assoc += "${it}, "
    }
    def lengthMinus2 = string_of_assoc.length() - 3
    final_string = string_of_assoc.getAt(0..lengthMinus2)
  }

  if (cmd.groupingIdentifier == 0x01) { // Lifeline
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      Boolean isStateChange = state.isAssociated ?: false
      state.isLifeLine = true
      result << createEvent(name: "Lifeline",
          value: "${final_string}",
          descriptionText: "${final_string}",
          displayed: true,
          isStateChange: isStateChange)

      state.isAssociated = true
    } else {
      Boolean isStateChange = state.isAssociated ? true : false
      result << createEvent(name: "Lifeline",
          value: "",
          descriptionText: "${final_string}",
          displayed: true,
          isStateChange: isStateChange)
      if (isLifeLine()) {
        state.isAssociated = false
      }
    }
  } else if (cmd.groupingIdentifier == 0x02) { // Repeated
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      Boolean isStateChange = state.isAssociated ?: false
      result << createEvent(name: "Repeated",
      value: "${final_string}",
      displayed: true,
      isStateChange: isStateChange)
      if (isLifeLine()) {
        cmds << zwave.associationV2.AssociationRemove(groupingIdentifier: cmd.groupingIdentifier, nodeId: [zwaveHubNodeId]).format()
      } else {
        state.isAssociated = true
      }
    } else {
      Boolean isStateChange = state.isAssociated ? true : false
      result << createEvent(name: "Repeated",
      descriptionText: "${final_string}",
      displayed: true,
      isStateChange: isStateChange)
    }
  } else {
    result << createEvent(descriptionText: "$device.displayName unknown association: $cmd", isStateChange: true, displayed: true)
    // Error
  }

  if (! state.isAssociated ) {
    cmds << zwave.associationV2.associationSet(groupingIdentifier: getAssociationGroup(), nodeId: [zwaveHubNodeId]).format()
    cmds << zwave.associationV2.associationGet(groupingIdentifier: getAssociationGroup()).format()
  }

  if (cmds.size()) {
    result << response(delayBetween(cmds, 500))
  }
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def checkConfigure() {
  def cmds = []

  if (device.currentValue("Configured") && device.currentValue("Configured").toBoolean() == false) {
    if (! state.lastConfigure || (new Date().time) - state.lastConfigure > 1500) {
      state.lastConfigure = new Date().time

      if (isPlus()) {
        cmds << zwave.manufacturerSpecificV2.manufacturerSpecificGet().format()
        cmds << zwave.configurationV2.configurationGet(parameterNumber: getParamater()).format()
      } else {
        cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
        cmds << zwave.configurationV1.configurationGet(parameterNumber: getParamater()).format()
      }

      if (0) {
        cmds << zwave.wakeUpV1.wakeUpIntervalGet().format()
      }
    }
  }

  if (device.currentValue("isAssociated") && device.currentValue("isAssociated").toBoolean() == false) {
    if (!state.lastAssociated || (new Date().time) - state.lastAssociated > 1500) {
      state.lastAssociated = new Date().time

      cmds << zwave.associationV2.associationGroupingsGet().format()
    }
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
  state.loggingLevelIDE = debugLevel ? debugLevel : 3
  log.info("$device.displayName updated() debug: ${state.loggingLevelIDE}")

  if (0) {
    def zwInfo = getZwaveInfo()
    if ($zwInfo) {
      logger("$device.displayName $zwInfo", "info")
      sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
    }
  }

  sendEvent(name: "tamper", value: "clear")

  // We don't send the prepDevice() because we don't know if the device is awake
  if (0) {
    sendCommands(prepDevice())
  }
  state.isAssociated = false
  state.isConfigured = false
  sendEvent(name: "driverVersion", value: getDriverVersion(), displayed: true, isStateChange: true)
  // sendEvent(name: "motion", value: "inactive", descriptionText: "$device.displayName reset on update", isStateChange: true, displayed: true)

  // Avoid calling updated() twice
  state.updatedDate = Calendar.getInstance().getTimeInMillis()
}

def installed() {
  log.debug "$device.displayName installed()"
  state.loggingLevelIDE = 4

  if (0) {
    def zwInfo = getZwaveInfo()
    if ($zwInfo) {
      logger("$device.displayName $zwInfo", "info")
      sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
    }
  }

  sendEvent(name: "driverVersion", value: getDriverVersion(), displayed: true, isStateChange: true)
  sendCommands(prepDevice())
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
    case "error":
    if (state.loggingLevelIDE >= 1) {
      log.error msg
      sendEvent(name: "lastError", value: "ERROR: ${msg}", displayed: false, isStateChange: true)
    }
    break

    case "warn":
    if (state.loggingLevelIDE >= 2) {
      log.warn msg
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
