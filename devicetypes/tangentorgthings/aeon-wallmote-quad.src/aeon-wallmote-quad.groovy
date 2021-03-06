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
 * https://products.z-wavealliance.org/products/2130
 *
 */

import physicalgraph.*

String getDriverVersion () {
  return "v0.63"
}

Integer maxButton () {
  return 4
}

def getConfigurationOptions(Integer model) {
  // 1 Touch beep
  // 2 Touch vibration
  // 3 Button slide function
  // 4 WallMote Reports
  return [ 1, 2, 3, 4, 39 ]
}

metadata {
  definition (name: "Aeon Wallmote Quad", namespace: "TangentOrgThings", author: "brian@tangent.org") {
    capability "Actuator"
    capability "Button"
    capability "Battery"
    capability "Configuration"
    capability "Refresh"

    attribute "Configured", "enum", ["false", "true"]
    attribute "isAssociated", "enum", ["false", "true"]

    attribute "DeviceReset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message
    attribute "parseErrorCount", "number"        // Last error message
    attribute "unknownCommandErrorCount", "number"        // Last error message

    attribute "Group 1", "string"
    attribute "Group 2", "string"
    attribute "Group 3", "string"
    attribute "Group 4", "string"
    attribute "Group 5", "string"
    attribute "Group 6", "string"
    attribute "Group 7", "string"
    attribute "Group 8", "string"
    attribute "Group 9", "string"

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

    attribute "epInfo", "string"

    // fingerprint deviceId: "0x1801", inClusters: "0x5E, 0x70, 0x85, 0x2D, 0x8E, 0x80, 0x84, 0x8F, 0x5A, 0x59, 0x5B, 0x73, 0x86, 0x72", outClusters: "0x20, 0x5B, 0x26, 0x27, 0x2B, 0x60"
    // fingerprint deviceId: "0x1202", inClusters: "0x5E, 0x8F, 0x73, 0x98, 0x86, 0x72, 0x70, 0x85, 0x2D, 0x8E, 0x80, 0x84, 0x5A, 0x59, 0x5B", outClusters:  "0x20, 0x5B, 0x26, 0x27, 0x2B, 0x60"         
    fingerprint mfr: "0184", prod: "4447", model: "3033", deviceJoinName: "Aeon Wallmote Quad" // cc: "5E, 86, 72, 5A, 85, 59, 73, 25, 27, 70, 2C, 2B, 5B, 7A", ccOut: "5B",
    fingerprint mfr: "0208", prod: "4447", model: "0201", deviceJoinName: "Aeon Wallmote Quad" // cc: "5E, 86, 72, 5A, 85, 59, 73, 25, 27, 70, 2C, 2B, 5B, 7A", ccOut: "5B",
  }

  simulator {
    status "button 1 pushed":  "command: 9881, payload: 00 5B 03 DE 00 01"

    // need to redo simulator commands

  }

  preferences {
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
    input name: "createChildren", type: "bool", title: "Create Children", description: "Create children devices for buttons.", required: true,  defaultValue: false
  }

  tiles {
    multiAttributeTile(name: "rich-control", type: "generic", width: 6, height: 4, canChangeIcon: true) {
      tileAttribute("device.button", key: "PRIMARY_CONTROL") {
        attributeState "default", label: ' ', action: "", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffffff"
      }
    }
    standardTile("battery", "device.battery", inactiveLabel: false, width: 6, height: 2) {
      state "battery", label:'${currentValue}% battery', unit:""
    }
    childDeviceTiles("outlets")

    main "rich-control"
    details(["button", "battery"])
  }

}

def getCommandClassVersions() { // 5E,85,59,8E,60,86,70,72,5A,73,84,80,5B,71,7A ccOut:25,26
  [
    0x20: 1,  // Basic
    0x59: 1,  // Association Grp Info
    0x5A: 1,  // Device Reset Locally
    0x5B: 1,  // Central Scene
    // 0x5E: 2, //
    0x60: 3,  // Multi-Channel Association
    // 0x6C: 2, // Supervision
    0x70: 2,  // Configuration
    0x71: 3,  // Notification
    0x72: 2,  // Manufacturer Specific
    0x73: 1,  // Powerlevel
    0x7A: 2,  // Firmware Update Md HS-200 V4
    0x80: 1,  // Battery
    0x84: 2,  // Wake Up
    0x85: 2,  // Association  0x85  V1 V2    
    0x86: 1,  // Version
    0x8E: 1,  // Multi Channel Association
    0x96: 1,  // Security
    0x25: 1,  // 
    0x26: 1,  // Switch Multilevel
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
    logger("parse() called with NULL description")
  } else if (description != "updated") {

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

def zwaveEvent(zwave.commands.securityv1.SecurityMessageEncapsulation cmd, result) {
  logger("$cmd")

  def encapsulatedCommand = cmd.encapsulatedCommand(getCommandClassVersions())
  //    log.debug("UnsecuredCommand: $encapsulatedCommand")
  // can specify command class versions here like in zwave.parse
  if (encapsulatedCommand) {
    //  log.debug("UnsecuredCommand: $encapsulatedCommand")
    zwaveEvent(encapsulatedCommand, result)
    return
  }

  logger("Unable to extract security encapsulated cmd from $cmd", "error")
}

def childOn(String childID) {
  logger("$childID")

  Integer buttonId = childID?.split("/")[1] as Integer

  def child = getChildDeviceForEndpoint( buttonId )
  if ( child ) {
    child.off()
  }

  if ( ! state.isCentralScene ) {
    buttonEvent(buttonId, false)
  }

  if ( child ) {
    child.on()
  }
}

def childOff(String childID) {
  logger("$childID")

  Integer buttonId = childID?.split("/")[1] as Integer

  if ( ! state.isCentralScene ) {
    buttonEvent(0, false)
  }

  def child = getChildDeviceForEndpoint( buttonId )
  if ( child ) {
    child?.sendEvent(name: "switch", value: "off")
  }
}

void buttonEvent(Integer buttonId, Boolean isHeld, Boolean isPhysical = false /* false ? "digital" : "physical" */) {
  logger("buttonEvent: $buttonId  held: $isHeld  isPhysical: $isPhysical")

  if ( buttonId ) {
    String heldType = isHeld ? "held" : "pushed"
    String buttonType = isPhysical ? "physical" : "digital"

    if ( isPhysical ) {
      def child = getChildDeviceForEndpoint( buttonId )

      if ( child ) {
        child?.sendEvent(name: "switch", value: "on")
        child?.sendEvent(name: "switch", value: "off")
      }
    }

    sendEvent(name: "button", value: "$heldType", data: [buttonNumber: buttonId], descriptionText: "$device.displayName $buttonId", isStateChange: true, type: buttonType)
  }
}

// NotificationReport(eventParametersLength: 0, eventParameter: [], zensorNetSourceNodeId: 0, v1AlarmType: 0, reserved61: 0, notificationStatus: 255, sequence: false, event: 0, notificationType: 8, v1AlarmLevel: 0)
def zwaveEvent(zwave.commands.notificationv3.NotificationReport cmd, result) {
  logger("$cmd")

  if (cmd.notificationType == 8) { // Numbers are just picked
    result << createEvent(name: "battery", unit: "%", value: cmd.event == 0xFF ? 1 : 90, descriptionText: "Battery level", isStateChange: did_batterylevel_change)
  }
}

def zwaveEvent(zwave.commands.centralscenev1.CentralSceneSupportedReport cmd, result) {
  logger("$cmd")
  result << createEvent(name: "numberOfButtons", value: maxButton(), isStateChange: true)
}

def zwaveEvent(zwave.commands.centralscenev1.CentralSceneSupportedReport cmd, Short endPoint, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.centralscenev1.CentralSceneNotification cmd, result) {
  logger("$cmd")
  logger("keyAttributes: $cmd.keyAttributes")

  state.lastActive = new Date().time

  state.isCentralScene = true

    switch (cmd.keyAttributes) {
      case 0: // Key Attributes is unreliable for Mote, so just count them all as one
      case 1:
      case 2: 
      buttonEvent(cmd.sceneNumber, false, true)
      break;
      default:
      logger("Unknown keyAttributes ${cmd.keyAttributes}")
      break;
    }
}

def zwaveEvent(zwave.commands.wakeupv2.WakeUpIntervalCapabilitiesReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.wakeupv2.WakeUpIntervalReport cmd, result) {
  logger("$cmd")
  result << createEvent(name: "WakeUp", value: "$cmd")
}

def zwaveEvent(zwave.commands.wakeupv2.WakeUpNotification cmd, result) {
  logger("$cmd")

  def request = []

  if (!state.lastBatteryReport || (now() - state.lastBatteryReport) / 60000 >= 60 * 24) {
    logger("Over 24hr since last battery report. Requesting report")
    request << response( zwave.batteryV1.batteryGet() )
  }
}

def zwaveEvent(zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.basicv1.BasicReport cmd, Short endPoint, result) {
  logger(":${endPoint} $cmd")
}

def zwaveEvent(zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.basicv1.BasicSet cmd, Short	endPoint, result) {
  logger(":${endPoint} $cmd")

  if ( ! state.isCentralScene ) {
    buttonEvent(endPoint, false, true)
  }
}

def zwaveEvent(zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.switchmultilevelv1.SwitchMultilevelSet cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.switchmultilevelv1.SwitchMultilevelSet cmd, Short	endPoint, result) {
  logger(":${endPoint} $cmd")
}

def zwaveEvent(zwave.commands.switchmultilevelv1.SwitchMultilevelStartLevelChange cmd, Short	endPoint, result) {
  logger(":${endPoint} $cmd")
}

def zwaveEvent(zwave.commands.switchmultilevelv1.SwitchMultilevelStartLevelChange cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.switchmultilevelv1.SwitchMultilevelStopLevelChange cmd, Short	endPoint, result) {
  logger(":${endPoint} $cmd")
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

def zwaveEvent(zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
  logger("$cmd")

  if ( state.manufacturerId != cmd.manufacturerId && state.productTypeId != cmd.productTypeId && state.productId != cmd.productId) {
    log.warn("has changed")
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


def zwaveEvent(zwave.commands.versionv1.VersionReport cmd, result) {
  logger("$cmd")

  String text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  String zWaveProtocolVersion = "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"

  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  result << createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: true)
  result << createEvent(name: "zWaveProtocolVersion", value: "${zWaveProtocolVersion}", descriptionText: "${device.displayName} ${zWaveProtocolVersion}", isStateChange: true)
}

def zwaveEvent(zwave.commands.batteryv1.BatteryReport cmd, result) {
  logger("$cmd")

  def map = [ name: "battery", unit: "%" ]
  if (cmd.batteryLevel == 0xFF) {
    map.value = 1
    map.descriptionText = "${device.displayName} battery is low"
    map.isStateChange = true
  } else {
    map.value = cmd.batteryLevel
  }

  state.lastBatteryReport = now()
  if (state.lastBatteryValue != map.value) {
    state.lastBatteryValue = map.value
    map.isStateChange = true
  }
  result << createEvent(map)
}

def zwaveEvent(zwave.commands.associationv2.AssociationGroupingsReport cmd, result) {
  logger("$cmd")

  state.groups = cmd.supportedGroupings

  if (cmd.supportedGroupings) {
    def cmds = []
    for (Integer x = 1; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x);
      cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: 0x00);
      cmds << zwave.associationGrpInfoV1.associationGroupCommandListGet(allowCache: true, groupingIdentifier: x);
    }

    sendCommands(cmds, 2000)
  } else {
    logger("reported no groups", "error")
  }
}

def zwaveEvent(zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd, result) {
  logger("$cmd")

  String name = new String(cmd.name as byte[])
  logger("Association Group #${cmd.groupingIdentifier} has name: ${name}", "info")

  updateDataValue("Association Group #${cmd.groupingIdentifier} Name", "${name}")

  result << response(delayBetween([
    zwave.associationV1.associationGet(groupingIdentifier: cmd.groupingIdentifier).format()
  ]))
}

def zwaveEvent(zwave.commands.associationgrpinfov1.AssociationGroupCommandListReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$cmd")

  result << createEvent(name: "isAssociated", value: "true")

	String final_string = ""
	if (cmd.nodeId) {
		final_string = cmd.nodeId.join(",")
	}

  updateDataValue("Association Group #${cmd.groupingIdentifier}", "${final_string}")
}

def zwaveEvent(zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd, result) {
  logger("$cmd")

  def firmware_report = String.format("%s-%s-%s", cmd.checksum, cmd.firmwareId, cmd.manufacturerId)
  updateDataValue("FirmwareMdReport", firmware_report)
  result << createEvent(name: "FirmwareMdReport", value: firmware_report, descriptionText: "$device.displayName FIRMWARE_REPORT: $firmware_report", displayed: true, isStateChange: true)
}

def zwaveEvent(zwave.commands.multichannelv3.MultiChannelCmdEncap cmd, result) {
  logger("$cmd")

  def encapsulatedCommand = cmd.encapsulatedCommand(getCommandClassVersions())
  if (encapsulatedCommand) {
    zwaveEvent(encapsulatedCommand, cmd.destinationEndPoint, result)
    return
  }

  logger("Unable to extract encapsulated cmd from $cmd", "error")
}

private List loadEndpointInfo() {
  if (state.endpointInfo) {
    state.endpointInfo
  } else if (device.currentValue("epInfo")) {
    util.parseJson((device.currentValue("epInfo")))
  } else {
    []
  }
}

def zwaveEvent(zwave.commands.multichannelv3.MultiInstanceCmdEncap cmd, result) {
  logger("$cmd")

  def encapsulatedCommand = cmd.encapsulatedCommand(getCommandClassVersions())
  if (encapsulatedCommand) {
    zwaveEvent(encapsulatedCommand, cmd.destinationEndPoint, result)
    return
  }

  logger("Unable to extract encapsulated cmd from $cmd", "error")
}

def zwaveEvent(zwave.commands.multichannelv3.MultiChannelCapabilityReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.multichannelv3.MultiChannelEndPointFindReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.multichannelv3.MultiInstanceReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.multichannelv3.MultiChannelEndPointReport cmd, result) {
  logger("$cmd")

  updateDataValue("endpoints", cmd.endPoints.toString())
  if (!state.endpointInfo) {
    state.endpointInfo = loadEndpointInfo()
  }

  if (state.endpointInfo.size() > cmd.endPoints) {
    cmd.endpointInfo
  }

  state.endpointInfo = [null] * cmd.endPoints

  //response(zwave.associationV2.associationGroupingsGet())
  result << createEvent(name: "epInfo", value: util.toJson(state.endpointInfo), displayed: false, descriptionText:"")
  result << response(zwave.multiChannelV3.multiChannelCapabilityGet(endPoint: 1))
}

def zwaveEvent(zwave.Command cmd, result) {
  logger("command not implemented: $cmd", "error")
}

private getChildDeviceForEndpoint(Integer button) {
  def children = childDevices
  def child

  if (children && button) {
    String childDni = "${device.deviceNetworkId}/${button}"

    logger("${childDni}")
    child = childDevices.find{ it.deviceNetworkId == childDni }

    if (! child) {
      logger("Child device $childDni not found", "error")
    }
  }

  return child
}

def ping() {
  logger("ping()")
}

def configure() {
  logger("Resetting Sensor Parameters to SmartThings Compatible Defaults", "debug")
  def cmds = []

  cmds << zwave.configurationV1.configurationSet(parameterNumber: 1, configurationValue: [0], size: 1).format()
  cmds << zwave.configurationV1.configurationSet(parameterNumber: 2, configurationValue: [1], size: 1).format()
  cmds << zwave.configurationV1.configurationSet(parameterNumber: 3, configurationValue: [1], size: 1).format()
  cmds << zwave.configurationV1.configurationSet(parameterNumber: 39, configurationValue: [40], size: 1).format()
  cmds << zwave.configurationV1.configurationSet(parameterNumber: 4, configurationValue: [1], size: 1).format()
  
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

def checkConfigure() {
  def cmds = []

  if (device.currentValue("Configured") && device.currentValue("Configured").toBoolean() == false) {
    if (!state.lastConfigure || (new Date().time) - state.lastConfigure > 1500) {
      state.lastConfigure = new Date().time
      cmds << zwave.manufacturerSpecificV2.manufacturerSpecificGet().format()
    }
  }

  if (device.currentValue("isAssociated") && device.currentValue("isAssociated").toBoolean() == false) {
    if (!state.lastAssociated || (new Date().time) - state.lastAssociated > 1500) {
      state.lastAssociated = new Date().time

      cmds << zwave.associationV2.associationGroupingsGet().format()
      cmds << zwave.multiChannelAssociationV2.multiChannelAssociationGroupingsGet().format()
      cmds << zwave.multiChannelV3.multiChannelEndPointGet().forma()
    }
  }

  return cmds
}

private void createChildDevices() {
  if ( settings.createChildren ) {
    // Save the device label for updates by updated()
    state.oldLabel = device.label

    // Add child devices for four button presses
    for ( Integer x in 1..4 ) {
      addChildDevice(
        "smartthings",
        "Child Switch",
        "${device.deviceNetworkId}/$x",
        "",
        [
        label         : "$device.displayName Switch $x",
        completedSetup: true,
        isComponent: true,
        ])
    }
  }
}

def prepDevice() {
  [
    zwave.zwaveCmdClassV1.requestNodeInfo(),
    zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
    zwave.configurationV2.configurationGet(parameterNumber: 4),
    zwave.firmwareUpdateMdV1.firmwareMdGet(),
    zwave.associationV2.associationGroupingsGet(),
    zwave.multiChannelAssociationV2.multiChannelAssociationGroupingsGet(),
    zwave.centralSceneV1.centralSceneSupportedGet(),
    zwave.multiChannelV3.multiChannelEndPointGet(),
  ]
}

def installed() {
  log.info("$device.displayName installed()")

  sendEvent(name: "numberOfButtons", value: maxButton(), displayed: true, isStateChange: true)

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  createChildDevices()

  sendCommands( prepDevice(), 5000 )
}

def updated() {
  if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
    return
  }
  log.info("$device.displayName updated() debug: ${settings.debugLevel}")

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)

  state.manufacturer = null
  updateDataValue("MSR", null)
  updateDataValue("manufacturer", null)
  sendEvent(name: "numberOfButtons", value: maxButton(), displayed: true, isStateChange: true)

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  if (! childDevices) {
    createChildDevices()
  }

  if ( childDevices ) {
    childDevices.each { logger("${it.deviceNetworkId}") }
  }

  sendCommands( prepDevice(), 5000 )

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
  String msg_text = (msg != null) ? "${msg}" : "<null>"

  Integer log_level = state.defaultLogLevel ?: settings.debugLevel

  switch(level) {
    case "warn":
    if (log_level >= 2) {
      log.warn "$device_name ${msg_text}"
    }
    sendEvent(name: "logMessage", value: "${msg_text}", isStateChange: true)
    break;

    case "info":
    if (log_level >= 3) {
      log.info "$device_name ${msg_text}"
    }
    break;

    case "debug":
    if (log_level >= 4) {
      log.debug "$device_name ${msg_text}"
    }
    break;

    case "trace":
    if (log_level >= 5) {
      log.trace "$device_name ${msg_text}"
    }
    break;

    case "error":
    default:
    log.error "$device_name ${msg_text}"
    sendEvent(name: "lastError", value: "${msg_text}", isStateChange: true)
    break;
  }
}
