// vim :set ts=2 sw=2 sts=2 expandtab smarttab :
/**
 *  Enerwave ZWN BPC PLUS
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


String getDriverVersion() {
  return "v3.43"
}

def getDefaultWakeupInterval() {
  return isPlus() ? 240 : 360
}

def getParamater() {
  return isPlus() ? 1 : 0
}

def getConfigurationOptions() {
  return [ 1 ]
}

def getAssociationGroup () {
  if (isPlus()) {
   return zwaveHubNodeId == 1 ? 1 : 3
  }

  return 1
}

def isPlus() {
  // state.newerModel is set to true if we recieved a Notification packet
  if (settings.isNewerModel || state.newerModel) {
    return true
  }

  return false
}

metadata {
  definition (name: "Enerwave ZWN BPC Plus", namespace: "TangentOrgThings", author: "Brian Aker", ocfDeviceType: "x.com.st.d.sensor.motion") {
    capability "Battery"
    capability "Motion Sensor"
    capability "Sensor"

    attribute "DeviceReset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message
    attribute "parseErrorCount", "number"        // Last error message
    attribute "unknownCommandErrorCount", "number"        // Last error message

    attribute "Configured", "enum", ["false", "true"]
    attribute "supportedGroupings", "string"
    attribute "Lifeline", "string"

    attribute "Group 1", "string"
    attribute "Group 2", "string"
    attribute "Group 3", "string"
    attribute "Group 4", "string"

    attribute "Sensor Basic rep", "string"
    attribute "Sensor notifi rep", "string"
    attribute "Sensor Basic SET", "string"

    attribute "AssociationGroup", "number"
    attribute "isAssociated", "enum", ["false", "true"]

    attribute "driverVersion", "string"
    attribute "firmwareVersion", "string"
    attribute "zWaveProtocolVersion", "string"
    attribute "Power", "string"
    attribute "FirmwareMdReport", "string"
    attribute "NIF", "string"

    attribute "MSR", "string"
    attribute "ManufacturerCode", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"

    attribute "WakeUp", "string"
    attribute "WakeUpInterval", "number"
    attribute "WakeUpNode", "number"

    attribute "lastActive", "string"
    attribute "LastAwake", "string"
    attribute "MotionTimeout", "number"

    attribute "NIF", "string"

    // fingerprint mfr: "011a", prod: "0601", model: "0901", cc: "30,70,72,80,84,85,86", ccOut: "20", deviceJoinName: "Enerwave Motion Sensor"  // Enerwave ZWN-BPC
    fingerprint type: "2001", mfr: "011A", prod: "0601", model: "0901", deviceJoinName: "Enerwave Motion Sensor ZWN-BPC+"  // Enerwave ZWN-BPC
    // fingerprint type: "2001", mfr: "011A", prod: "00FF", model: "0700", deviceJoinName: "Enerwave Motion Sensor ZWN-BPC PLus"  // Enerwave ZWN-BPC
  }

  simulator {
    status "inactive": "command: 3003, payload: 00"
    status "active": "command: 3003, payload: FF"
  }

  tiles {
    multiAttributeTile(name:"motion", type: "generic", width: 6, height: 4) {
      tileAttribute ("device.motion", key: "PRIMARY_CONTROL") {
        attributeState "active", label:'motion', icon:"st.motion.motion.active", backgroundColor:"#00a0dc"
        attributeState "inactive", label:'no motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
      }
      tileAttribute ("battery", key: "SECONDARY_CONTROL") {
        attributeState "battery", label:'${currentValue}% battery', unit:""
      }
    }

    valueTile("driverVersion", "device.driverVersion", width:2, height:2, inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }

    standardTile("reset", "device.DeviceReset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "false", label:'', backgroundColor:"#ffffff"
      state "true", label:'reset', backgroundColor:"#e51426"
    }

    valueTile("configured", "device.Configured", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "false", label:'', backgroundColor:"#ffffff"
      state "true", label:'', backgroundColor:"#e51426"
    }

    valueTile("lastActive", "state.lastActive", width:2, height:2, inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }

    valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
     state("battery", label:'${currentValue}% battery', unit:"")
  }

    main "motion"
    details(["motion", "lastActive", "driverVersion", "reset", "configured", "battery", "lastActive"])
  }

  preferences {
    input name: "wakeupInterval", type: "number", title: "Wakeup Interval", description: "Interval in seconds for the device to wakeup", range: "240..68400"
    input name: "motionTimeout", type: "number", title: "Motion Timeout", description: "Interval in seconds for the device to timeout after motion (plus model is N * Wakeup Interval", range: "1..255"
    input name: "isNewerModel", type: "bool", title: "Temp fix for model", description: "Enter true or false"
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  }
}

private deviceCommandClasses() {
  if (isPlus()) {
    return [
      0x20: 1,  // Basic
      0x59: 1,  // Association Grp Info
      0x5A: 1,  // Device Reset Locally
      // 0x5E: 1,  // Plus
      0x70: 1,  // Configuratin
      0x71: 3,  //     Notification0x8
      0x72: 2,  // Manufacturer Specific
      0x73: 1, // Powerlevel
      0x80: 1, // Battery
      0x84: 2, // Wake Up
      0x85: 2,  // Association  0x85  V1 V2
      0x86: 1,  // Version
      0x01: 1,  // Z-wave command class
      0x25: 1,  // Switch Binary
    ]
  } else {
    return [
      0x20: 1,  // Basic
      0x30: 1,  // Sensor Binary
      0x70: 1,  // Configuratin
      0x72: 2,  // Manufacturer Specific V1
      0x80: 1,  // Battery
      0x84: 2,  // Wake Up V1
      0x85: 2,  // Association  0x85  V1 V2
      0x86: 1,  // Version
      0x01: 1,  // Z-wave command class
    ]
  }
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

    if (1) {
      def check_cmds = checkConfigure()

      if (check_cmds) {
        result << response( delayBetween ( check_cmds ))
      }
    }

    def cmd = zwave.parse(description, deviceCommandClasses())
    if (! cmd) {
      logger( "zwave.parse(getCommandClassVersions()) failed for: ${description}", "parse" )
      cmd = zwave.parse(description)
    }

    if (cmd) {
      zwaveEvent(cmd, result)
    } else {
      logger( "zwave.parse() failed for: ${description}", "error" )
    }
  }

  return result
}

def configure() {
  sendEvent(name: "Configured", value: "false", isStateChange: true)
  sendEvent(name: "isAssociated", value: "false", isStateChange: true)
}

def sensorValueEvent(happened, result) {

  if (happened) {
    logger("  is active", "info")
    
    state.lastActive = new Date().format("MMM dd EEE HH:mm:ss", location.timeZone)
    result << createEvent(name: "lastActive", value: state.lastActive)
  }

  result << createEvent(name: "motion", value: happened ? "active" : "inactive")
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$device.displayName $cmd -- BEING CONTROLLED")
  // The device is sending a Set message device is in Group 4 ( this logic should be checked against associations)
  sensorValueEvent(cmd.value, result)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$device.displayName $cmd")
  sensorValueEvent(cmd.value, result)
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, result) {
  logger("$device.displayName $cmd")
  sensorValueEvent(cmd.value, result)
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd) {
  logger("$device.displayName $cmd")
  sensorValueEvent(cmd.sensorValue, result)
}

// NotificationReport() NotificationReport(event: 8, eventParameter: [], eventParametersLength: 0, notificationStatus: 255, notificationType: 7, reserved61: 0, sequence: false, v1AlarmLevel: 0, v1AlarmType: 0, zensorNetSourceNodeId: 0)
def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd, result) {
  logger("$device.displayName $cmd")
  state.newerModel = true

  if (cmd.notificationType == 0x07) {
    switch (cmd.event) {
      case 0x08:
      sensorValueEvent(true, result)
      break;
      case 0x03:
      result << createEvent(descriptionText: "$device.displayName configure button", displayed: true, isStateChange: true)
      break;
      case 0x00:
      sensorValueEvent(false, result)
      break;
      default:
      log.warn("$device.displayName has unknown state: $cmd")
      result << createEvent(descriptionText: "$device.displayName unknown event state: $cmd", displayed: true)
      return
    }

    return
  }

  log.error("$device.displayName notificationType not implemented: $cmd")
  result << createEvent(descriptionText: "$device.displayName unknown notificationType: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpIntervalReport cmd, result) {
  logger("$device.displayName $cmd")

  result << createEvent(name: "WakeUpNode", value: cmd.nodeid, isStateChange: true, displayed: true)
  result << createEvent(name: "WakeUpInterval", value: cmd.seconds, isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd, result) {
  logger("$device.displayName $cmd")
  state.lastActive = new Date().format("MMM dd EEE HH:mm:ss", location.timeZone)
  result << createEvent(name: "LastAwake", value: state.lastActive, descriptionText: "${device.displayName} woke up", isStateChange: false)
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd, result) {
  logger("$device.displayName $cmd")
  Boolean did_batterylevel_change = state.batteryLevel != cmd.batteryLevel
  state.batteryLevel = cmd.batteryLevel

  int level
  switch(cmd.batteryLevel) {
    case 0x64:
    level = 99
    break
    case 0x10:
    level = 50
    break
    case 0x00:
    level = 25
    break
    case 0xFF:
    level = 1
    break
    default:
    level = 98
    break
  }

  state.lastbat = new Date().time

  result << createEvent(name: "battery", unit: "%", value: level, descriptionText: "Battery level", isStateChange: did_batterylevel_change)
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd, result) {
  logger("$device.displayName $cmd")

  if (cmd.supportedGroupings > 1) {
    state.newerModel = true
  }

  if (cmd.supportedGroupings) {
    def cmds = []

    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      if (isPlus()) {
        if (0) {
        cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: true, refreshCache: true).format();
        cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x).format();
        cmds << zwave.associationGrpInfoV1.associationGroupCommandListGet(groupingIdentifier: x, allowCache: false).format();
        }
        cmds << zwave.associationV2.associationGet(groupingIdentifier: x).format();
      } else {
        cmds << zwave.associationV1.associationGet(groupingIdentifier: x).format();
      }
    }

    result << createEvent(name: "supportedGroupings", value: cmd.supportedGroupings, descriptionText: "$device.displayName", isStateChange: true, displayed: true)
    result << response(delayBetween(cmds, 2000))

    return
  }

  result << createEvent(descriptionText: "$device.displayName AssociationGroupingsReport: $cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupCommandListReport cmd, result) {
  logger("$device.displayName $cmd")

  result << createEvent(descriptionText: "$device.displayName AssociationGroupCommandListReport: $cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(descriptionText: "$device.displayName AssociationGroupInfoReport: $cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd, result) {
  logger("$device.displayName $cmd")

  def name = new String(cmd.name as byte[])
  logger("Association Group #${cmd.groupingIdentifier} has name: ${name}", "info")

  result << createEvent(name: "Group #${cmd.groupingIdentifier}", value: "${name}", isStateChange: true)

  result << response(delayBetween([
    zwave.associationV1.associationGet(groupingIdentifier: cmd.groupingIdentifier).format(),
  ]))
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$device.displayName $cmd")

  if (cmd.groupingIdentifier == getAssociationGroup()) {
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      result << createEvent(name: "isAssociated", value: "true")
      result << response(delayBetween([
        zwave.associationV1.associationSet(groupingIdentifier: 4, nodeId: [zwaveHubNodeId]).format(),
        zwave.associationV1.associationGet(groupingIdentifier: 4).format(),
        ]))
    } else {
      result << createEvent(name: "isAssociated", value: "false")
      result << response(delayBetween([
        zwave.associationV1.associationSet(groupingIdentifier: getAssociationGroup(), nodeId: [zwaveHubNodeId]).format(),
        zwave.associationV1.associationGet(groupingIdentifier: getAssociationGroup()).format(),
        zwave.associationV1.associationSet(groupingIdentifier: 4, nodeId: [zwaveHubNodeId]).format(),
        zwave.associationV1.associationGet(groupingIdentifier: 4).format(),
        ]))
    }
  }

  String final_string = ""
  if (cmd.nodeId) {
    def string_of_assoc = ""
    cmd.nodeId.each {
      string_of_assoc += "${it}, "
    }
    def lengthMinus2 = ( string_of_assoc.length() > 3 ) ? string_of_assoc.length() - 3 : 0
    final_string = lengthMinus2 ? string_of_assoc.getAt(0..lengthMinus2) : string_of_assoc
  }

  String group_name
  switch ( cmd.groupingIdentifier ) {
    case 1:
    group_name = "Lifeline"
    break;
    case 2:
    group_name = "Sensor Basic rep"
    break;
    case 3:
    group_name = "Sensor notifi rep"
    break;
    case 4:
    group_name = "Sensor Basic SET"
    break;
    default:
    log.error "Unknown group ${cmd.groupingIdentifier}";
    group_name = ""
    break;
  }

  updateDataValue("$group_name", "$final_string")
}

def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelReport cmd, result) {
  logger("zwaveEvent(): Powerlevel Report received: ${cmd}","trace")
  def device_power_level = (cmd.powerLevel > 0) ? "minus${cmd.powerLevel}dBm" : "NormalPower"
  logger("Powerlevel Report: Power: ${device_power_level}, Timeout: ${cmd.timeout}","info")
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

def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  logger("$device.displayName command not implemented: $cmd", "error")
  result << createEvent(descriptionText: "$device.displayName command not implemented: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd, result) {
  logger("$device.displayName $cmd", "warn")
  result << createEvent(name: "DeviceReset", value: "true", descriptionText: cmd.toString(), isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
  logger("$device.displayName $cmd")

  String manufacturerCode = String.format("%04X", cmd.manufacturerId)
  String productTypeCode = String.format("%04X", cmd.productTypeId)
  String productCode = String.format("%04X", cmd.productId)

  state.manufacturer = cmd.manufacturerName ? cmd.manufacturerName : "Enerwave"

  result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
  result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
  result << createEvent(name: "ProductCode", value: productCode)

  String msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)
  updateDataValue("manufacturer", state.manufacturer)
  state.MSR = msr

  result << createEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: false)

  if (isPlus()) {
    Integer[] parameters = getConfigurationOptions()

    def cmds = []
    parameters.each {
      cmds << zwave.configurationV1.configurationGet(parameterNumber: it).format()
    }

    result << response(delayBetween(cmds, 1000))
  }

  result << response(delayBetween([
    zwave.versionV1.versionGet().format(),
  ]))
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd, result) {
  logger("$device.displayName $cmd")

  updateDataValue("Configuration #${cmd.parameterNumber}", "${cmd.scaledConfigurationValue}")
  
  int parameterNumber

  if (! state.MSR) { // Don't change an unknown 
    result << createEvent(name: "Configured", value: "false", isStateChange: true)
    return
  }

  if (getParamater() == cmd.parameterNumber) {
    if ( cmd.scaledConfigurationValue != settings.motionTimeout) {
      result << createEvent(name: "Configured", value: "false", isStateChange: true)
      return
    }

    result << createEvent(name: "Configured", value: "true", isStateChange: true, displayed: true)
  } else {
    logger("Unknown parameter $cmd.parameterNumber", "error")
  }
}

def checkConfigure() {
  def cmds = []

  if (! state.MSR ) {
    cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
    return cmds
  }

  if (device.currentValue("Configured") && device.currentValue("Configured").toBoolean() == false) {
    if (!state.lastConfigure || (new Date().time) - state.lastConfigure > 1500) {
      state.lastConfigure = new Date().time

      int MotionTimout = settings.motionTimeout as Integer

      if (isPlus()) {
        cmds << zwave.manufacturerSpecificV2.manufacturerSpecificGet().format();
        cmds << zwave.powerlevelV1.powerlevelGet().format()
        // cmds << zwave.configurationV2.configurationGet(parameterNumber: getParamater()).format()
      } else {
        cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
      }
      cmds << zwave.wakeUpV1.wakeUpIntervalGet().format()
    }
  }

  if (device.currentValue("isAssociated") && device.currentValue("isAssociated").toBoolean() == false) {
    if (!state.lastAssociated || (new Date().time) - state.lastAssociated > 1500) {
      state.lastAssociated = new Date().time

      if (isPlus()) {
        cmds << zwave.associationV2.associationGroupingsGet().format()
      } else {
        cmds << zwave.associationV1.associationGroupingsGet().format()
      }
    }
  }

  return cmds
}

def prepDevice() {
  [
    zwave.manufacturerSpecificV1.manufacturerSpecificGet(),
  ]
}

def installed() {
  log.info("$device.displayName installed()")

  if (0) {
    def zwInfo = getZwaveInfo()
    if ($zwInfo) {
      log.debug("$device.displayName $zwInfo")
      sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
    }
  }

  sendEvent(name: "driverVersion", value: getDriverVersion(), isStateChange: true)
  sendEvent(name: "AssociationGroup", value: getAssociationGroup(), isStateChange: true)

  state.wakeupInterval = getDefaultWakeupInterval()

  sendEvent(name: "isAssociated", value: "false", isStateChange: true)
  sendEvent(name: "Configured", value: "false", isStateChange: true)

  sendCommands(prepDevice())
}

def updated() {
  if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
    return
  }
  log.info("$device.displayName updated() debug: ${settings.debugLevel}")

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)
  sendEvent(name: "parseErrorCount", value: 0, displayed: false)
  sendEvent(name: "unknownCommandErrorCount", value: 0, displayed: false)
  state.parseErrorCount = 0
  state.unknownCommandErrorCount = 0

  if (0) {
    def zwInfo = getZwaveInfo()
    if ($zwInfo) {
      log.debug("$device.displayName $zwInfo")
      sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
    }
  }

  sendEvent(name: "isAssociated", value: "false", isStateChange: true)
  sendEvent(name: "Configured", value: "false", isStateChange: true)

  sendEvent(name: "driverVersion", value: getDriverVersion(), isStateChange: true)
  // sendEvent(name: "motion", value: "inactive", descriptionText: "$device.displayName is being reset")
  sendEvent(name: "AssociationGroup", value: getAssociationGroup(), isStateChange: true)
  state.AssociationGroup = getAssociationGroup()

  // Avoid calling updated() twice
  state.updatedDate = Calendar.getInstance().getTimeInMillis()
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
  } else if (state.useCrc16) {
    return zwave.crc16EncapV1.crc16Encap().encapsulate(cmd)
  } else {
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
 *    messages by sending events for the device's logMessage attribute and lastError attribute.
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
