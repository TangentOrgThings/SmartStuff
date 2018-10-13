// vim :set ts=2 sw=2 sts=2 expandtab smarttab :
/**
 *  Copyright 2017-2018 SmartThings
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
  return "v4.29"
}

def getConfigurationOptions(Integer model) {
  return [ 3, 4 ]
}

metadata {
  definition (name: "GE Branded Switch", namespace: "TangentOrgThings", author: "brian@tangent.org", ocfDeviceType: "oic.d.switch") {
    capability "Actuator"
    capability "Button"
    capability "Health Check"
    capability "Indicator"
    capability "Light"    
    capability "Refresh"
    capability "Sensor"
    capability "Switch"

    attribute "DeviceReset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message
    attribute "parseErrorCount", "number"        // Last error message
    attribute "unknownCommandErrorCount", "number"        // Last error message

    attribute "NIF", "string"

    attribute "manufacturerId", "string"
    attribute "NodeLocation", "string"
    attribute "NodeName", "string"
    attribute "productType", "string"
    attribute "productId", "string"

    attribute "driverVersion", "string"
    attribute "firmwareVersion", "string"
    attribute "FirmwareMdReport", "string"

    attribute "Group 1", "string"
    attribute "Group 2", "string"
    attribute "Group 3", "string"
    attribute "Group 4", "string"

    attribute "invertedStatus", "enum", ["false", "true"]

    attribute "keyAttributes", "number"

    attribute "Scene", "number"

    attribute "SwitchAll", "string"
    attribute "Power", "string"
    attribute "Protection", "string"

    command "connect"
    command "disconnect"

    fingerprint mfr:"0063", prod: "4952", model:"3032", deviceJoinName: "Jasco/GE 12722 In-Wall On/Off Switch" //, cc: "0x20, 0x25, 0x27, 0x70, 0x72, 0x73, 0x77, 0x86"
    fingerprint mfr:"0063", prod: "4952", model:"3036", deviceJoinName: "Jasco/GE 14291 In-Wall On/Off Switch"
    fingerprint mfr:"0063", prod: "4F50", model:"3032", deviceJoinName: "Jasco/GE Plug-in Outdoor Smart Switch"
    fingerprint mfr:"0063", prod: "5252", model:"3530", deviceJoinName: "GE 45636/ZW1001"
  }

  // simulator metadata
  simulator {
    status "on":  "command: 2003, payload: FF"
    status "off": "command: 2003, payload: 00"

    // reply messages
    reply "2001FF,delay 100,2502": "command: 2503, payload: FF"
    reply "200100,delay 100,2502": "command: 2503, payload: 00"
  }

  preferences {
    input name: "ledIndicator", type: "enum", title: "LED Indicator", description: "Turn LED indicator... ", required: false, options:["off": "When Off", "on": "When On", "never": "Never"]
    input name: "invertSwitch", type: "bool", title: "Invert Switch", description: "Invert switch? ", required: false
    input name: "disbableDigitalOff", type: "bool", title: "Disable Digital Off", description: "Disallow digital turn off", required: false
    input name: "delayOff", type: "bool", title: "Delay Off", description: "Delay Off for three seconds", required: false
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  }

  // tile definitions
  tiles(scale: 2) {
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
      tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label: '${name}', action: "disconnect", icon: "st.switches.switch.on", backgroundColor: "#00A0DC"
        attributeState "off", label: '${name}', action: "connect", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
      }
    }

    standardTile("indicator", "device.indicatorStatus", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "when off", action:"indicator.indicatorWhenOn", icon:"st.indicators.lit-when-off"
      state "when on", action:"indicator.indicatorNever", icon:"st.indicators.lit-when-on"
      state "never", action:"indicator.indicatorWhenOff", icon:"st.indicators.never-lit"
    }

    valueTile("driverVersion", "device.driverVersion", width:2, height: 2) {
      state "val", label: '${currentValue}', defaultState: true
    }

    standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
    }

    main("switch")
    details(["switch", "indicator", "driverVersion", "refresh"])
  }
}

def getCommandClassVersions() {
  switch (state.MSR) {
    case "0063-4952-3032":
    return [
      0x20: 1,  // Basic
      0x22: 1,  // Application Status
      0x25: 1,  // Switch Binary
      0x27: 1,  // Switch All
      0x70: 1,  // Configuration
      0x72: 2,  // Manufacturer Specific V1
      0x73: 1,  // Powerlevel
      0x77: 1,  // Node Naming
      0x86: 1,  // Version
    ]
    break
    case "0063-4F50-3032":
    case "0063-4952-3036":
    return [
      0x20: 1,  // Basic
      0x22: 1,  // Application Status
      0x25: 1,  // Switch Binary
      0x27: 1,  // Switch All
      0x2B: 1,  // SceneActivation
      0x2C: 1,  // Scene Actuator Conf
      0x59: 1,  // Association Grp Info
      0x5A: 1,  // Device Reset Locally
      0x56: 1,  // Crc16Encap
      0x70: 1,  // Configuration
      0x72: 2,  // Manufacturer Specific
      0x73: 1,  // Powerlevel
      0x7A: 2,  // Firmware Update Md
      0x85: 2,  // Association  0x85  V1 V2
      0x86: 1,  // Version
    ]
    break
    default:
    return [
      0x20: 1,  // Basic
      0x22: 1,  // Application Status
      0x25: 1,  // Switch Binary
      0x27: 1,  // Switch All
      0x70: 1,  // Configuration
      0x72: 2,  // Manufacturer Specific V1
      0x73: 1,  // Powerlevel
      0x86: 1,  // Version
    ]
  }
}

def prepDevice() {
  [
    zwave.manufacturerSpecificV1.manufacturerSpecificGet(),
    // zwave.associationV2.associationGroupingsGet(),
    zwave.powerlevelV1.powerlevelGet(),
    zwave.switchAllV1.switchAllGet(),
    // zwave.configurationV1.configurationSet(configurationValue: [ledIndicator == "on" ? 1 : ledIndicator == "never" ? 2 : 0], parameterNumber: 3, size: 1),
    // zwave.configurationV1.configurationSet(configurationValue: [invertSwitch == true ? 1 : 0], parameterNumber: 4, size: 1),
    // zwave.configurationV1.configurationGet(parameterNumber: 3),
    // zwave.configurationV1.configurationGet(parameterNumber: 4),
    zwave.switchBinaryV1.switchBinaryGet(),
    zwave.zwaveCmdClassV1.requestNodeInfo(),
  ]
}

def initialize() {
  if (1) {
    sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
  }
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

  initialize()

  // Check in case the device has been changed
  state.manufacturer = null
  updateDataValue("MSR", null)
  updateDataValue("manufacturer", null)

  sendEvent(name: "driverVersion", value: getDriverVersion(), isStateChange: true)
  sendEvent(name: "Scene", value: 0, isStateChange: true)

  sendCommands(prepDevice(), 3000)

  // Avoid calling updated() twice
  state.updatedDate = Calendar.getInstance().getTimeInMillis()
}

def installed() {
  log.debug ("installed()")

  sendEvent(name: "ledIndicator", value: "when off", displayed: true, isStateChange: true)

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed:true)
  sendEvent(name: "Scene", value: 0, isStateChange: true)

  initialize()

  sendCommands(prepDevice(), 3000)
}

def parse(String description) {
  def result = []

  if (description && description.startsWith("Err")) {
    logger ( "parse() passed error: ${description}", "error" )

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
      // Try it without check for classes
      cmd = zwave.parse(description)

      if (cmd) {
        logger( "zwave.parse(getCommandClassVersions()) failed: ${description}", "parse" )
        zwaveEvent(cmd, result)
      } else {
        logger( "zwave.parse() failed for: ${description}", "parse" )
      }
    }
  }

  return result
}

private switchEvents(Short value, boolean isPhysical, result) {
  if (value == 254) {
    logger("$device.displayName returned Unknown for status.", "warn")
    return
  }

  result << createEvent(name: "switch", value: value ? "on" : "off", type: isPhysical ? "physical" : "digital", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicGet cmd, result) {
  def currentValue = device.currentState("switch").value.equals("on") ? 255 : 0
  result << response(delayBetween([
    zwave.basicV1.basicReport(value: currentValue).format(),
  ]))
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$device.displayName $cmd NIF CHECKED")
  switchEvents(cmd.value, true, result);
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$device.displayName $cmd -- BEING CONTROLLED")

  result << response(zwave.switchBinaryV1.switchBinaryGet())

  if (0) {
    if ( cmd.value ) {
      response( trueOn(false) )
      return
    }

    response( trueOff(false) )
    return
  }

  if (cmd.value == 255) {
    createEvent(name: "button", value: "pushed", data: [buttonNumber: 3], descriptionText: "Double-tap up (button 1) on $device.displayName", isStateChange: true, type: "physical")
  }
  else if (cmd.value == 0) {
    createEvent(name: "button", value: "pushed", data: [buttonNumber: 4], descriptionText: "Double-tap down (button 2) on $device.displayName", isStateChange: true, type: "physical")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, result) {
  logger("$device.displayName $cmd")
  switchEvents(cmd.value, false, result);
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinarySet cmd, result) {
  logger("$device.displayName $cmd -- BEING CONTROLLED")

  result << response(zwave.switchBinaryV1.switchBinaryGet())
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryGet cmd, result) {
  logger("$device.displayName $cmd")

  def currentValue = device.currentState("switch").value.equals("on") ? 255 : 0
  result << response(delayBetween([
    zwave.basicV1.switchBinaryReport(value: currentValue),
  ]))
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd, result) {
  logger("$device.displayName $cmd")

  def name = ""
  def value = ""
  def reportValue = cmd.configurationValue[0]
  switch (cmd.parameterNumber) {
    case 3:
    name = "indicatorStatus"
    value = reportValue == 1 ? "when on" : reportValue == 2 ? "never" : "when off"
    break
    case 4:
    name = "invertSwitch"
    value = reportValue == 1 ? "true" : "false"
    break
    default:
    logger("$device.displayName has unknown configuration parameter $cmd.parameterNumber : $cmd.configurationValue[0]", "error")
    return
    break
  }

  result << createEvent(name: name, value: value)
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
  logger("$device.displayName $cmd")

  state.manufacturerId = cmd.manufacturerId
  state.productTypeId = cmd.productTypeId
  state.productId = cmd.productId
  state.manufacturerName = cmd.manufacturerName ? cmd.manufacturerName : "GE"

  String manufacturerId = String.format("%04X", state.manufacturerId)
  String productTypeId = String.format("%04X", state.productTypeId)
  String productId = String.format("%04X", state.productId)

  result << createEvent(name: "manufacturerId", value: manufacturerId)
  result << createEvent(name: "productType", value: productTypeId)
  result << createEvent(name: "productId", value: productId)

  def cmds = []
  if (state.productId == 0x3036) {
    result << createEvent(name: "numberOfButtons", value: 4, displayed: false)
    sendCommands(
      [
      zwave.associationV2.associationGroupingsGet(),
      ], 2000)
    if (! state.FirmwareMdReport ) {
      sendCommands(
        [
        // zwave.configurationV1.configurationGet(parameterNumber: 3),
        // zwave.configurationV1.configurationGet(parameterNumber: 4),
        zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: 1),
        zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: 2),
        zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: zwaveHubNodeId),
        zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: device.deviceNetworkId),
        zwave.firmwareUpdateMdV2.firmwareMdGet(),
        // zwave.associationV2.associationGroupingsGet(),
        ], 2000)
    }
  }

  String msr = String.format("%04X-%04X-%04X", state.manufacturerId, state.productTypeId, state.productId)
  updateDataValue("MSR", msr)
  state.MSR = "$msr"

  updateDataValue("manufacturer", state.manufacturerName)

  Integer[] parameters = getConfigurationOptions(cmd.productId)

  parameters.each {
    cmds << zwave.configurationV1.configurationGet(parameterNumber: it).format()
  }

  result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
  result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
  result << createEvent(name: "ProductCode", value: productCode)
  
  result << response(delayBetween(cmds, 1000))
  result << response( zwave.versionV1.versionGet() )
}


def zwaveEvent(physicalgraph.zwave.commands.nodenamingv1.NodeNamingNodeNameReport cmd, result) {
  int length = cmd.nodeName.size()
  logger "NodeNamingNodeNameReport: ($length)"
  /*
  String nodeName = cmd.nodeName.each { String.format("%02x ", it) }.join()
  state.nodeName = nodeName
   */
  state.nodeName= "Not Implemented"
  result << createEvent(
    name: "NodeName",
    value: state.nodeName,
    displayed: true,
    isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.nodenamingv1.NodeNamingNodeLocationReport cmd, result) {
  int length = cmd.nodeLocation.size()
  logger "NodeNamingNodeLocationReport:  ($length)"
  /*
  String nodeLocation = cmd.nodeLocation.each { String.format("%02x ", it) }.join()
  state.nodeLocation= nodeLocation
   */
  state.nodeLocation= "Not Implemented"
  result << createEvent(
    name: "NodeLocation",
    value: state.nodeLocation,
    displayed: true,
    isStateChange: true)
}

def buttonEvent(String exec_caller, Integer button, Boolean held, String buttonType) {
  logger("$exec_caller buttonEvent: $button  held: $held  type: $buttonType")

  button = button as Integer
  if (held) {
    sendEvent(name: "button", value: "held", data: [buttonNumber: button], descriptionText: "$device.displayName button $button was pushed", isStateChange: true, type: "$buttonType")
  } else {
    sendEvent(name: "button", value: "pushed", data: [buttonNumber: button], descriptionText: "$device.displayName button $button was pushed", isStateChange: true, type: "$buttonType")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfGet cmd, result) {
  logger("$device.displayName $cmd")
  buttonEvent("SceneActuatorConfGet", cmd.sceneId, false, "digital")

  result << response(zwave.sceneActuatorConfV1.sceneActuatorConfReport(dimmingDuration: 0xFF, level: 0xFF, sceneId: cmd.sceneId))
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfReport cmd, result) {
  logger("$device.displayName $cmd")

  // HomeSeer (ST?) does not implement this scene
  if (cmd.sceneId == 0) {
    result << createEvent(name: "Scene", value: cmd.sceneId, isStateChange: true, displayed: true)
    result << createEvent(name: "switch", value: cmd.level == 0 ? "off" : "on", isStateChange: true, displayed: true)
    return
  }

  def cmds = []
  if (cmd.sceneId == 1) {
    if (cmd.level != 255) {
      cmds << zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: cmd.sceneId, dimmingDuration: 0, level: 255, override: true).format()
    }
  } else if (cmd.sceneId == 2) {
    if (cmd.level) {
      cmds << zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: cmd.sceneId, dimmingDuration: 0, level: 0, override: true).format()
    }
  } else if (cmd.sceneId == zwaveHubNodeId) {
    if (0) {
      if (cmd.level || (cmd.dimmingDuration != 0x88)) {
        cmds << zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: cmd.sceneId, dimmingDuration: 0x88, level: 0, override: true).format()
        cmds << zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: cmd.sceneId).format()
      }
    }
  }

  updateDataValue("Scene #${cmd.sceneId}", "Level: ${cmd.level} Dimming Duration: ${cmd.dimmingDuration}")

  if (cmds.size()) {
    result << response(delayBetween(cmds, 1000))
  }
}

/*
def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd) {
  logger("$device.displayName $cmd")

  Integer button = ((cmd.sceneId + 1) / 2) as Integer
  Boolean held = !(cmd.sceneId % 2)
  buttonEvent(button, held, "digital")
}
*/

def zwaveEvent(physicalgraph.zwave.commands.switchallv1.SwitchAllReport cmd, result) {
  logger("$device.displayName $cmd")

  state.switchAllModeCache = cmd.mode

  def msg = ""
  switch (cmd.mode) {
    case 0:
    msg = "Device is excluded from the all on/all off functionality."
    break

    case 1:
    msg = "Device is excluded from the all on functionality but not all off."
    break

    case 2:
    msg = "Device is excluded from the all off functionality but not all on."
    break

    default:
    msg = "Device is included in the all on/all off functionality."
    break
  }
  logger("Switch All Mode: ${msg}","info")

  if (cmd.mode != 0) {
    sendCommands([
      zwave.switchAllV1.switchAllSet(mode: 0x00),
      zwave.switchAllV1.switchAllGet(),
    ])
  } else {
    result << createEvent(name: "SwitchAll", value: msg, isStateChange: true, displayed: true)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd, result) {
  logger("$device.displayName $cmd")

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

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationSpecificGroupReport cmd, result) {
  log.debug("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd, result) {
  logger("$device.displayName $cmd")
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

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupCommandListReport cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$device.displayName $cmd")
  
  String group_name

  switch (cmd.groupingIdentifier) {
    case 1:
    group_name = "LifeLine"
    break
    case 2:
    group_name = "AssociationSet"
    break
    case 3:
    group_name = "DoubleTap"
    break

    default:
    logger("Unknown group ${cmd.groupingIdentifier}", "error")
      return
  }

  String final_string
  if (cmd.nodeId ) {
    def string_of_assoc = ""
    cmd.nodeId.each {
      string_of_assoc += "${it}, "
    }
    def lengthMinus2 = string_of_assoc.length() - 3
    final_string = string_of_assoc.getAt(0..lengthMinus2)
  }

  Boolean isStateChange
  state.isAssociated = true

  if (cmd.groupingIdentifier == 0x01) { // Lifeline
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      isStateChange = state.hasLifeLine ? false : true
        state.hasLifeLine = true
    } else {
      state.hasLifeLine = false
      result << response(delayBetween([
        zwave.associationV1.associationSet(groupingIdentifier: cmd.groupingIdentifier, nodeId: [zwaveHubNodeId]).format(),
        zwave.associationV1.associationGet(groupingIdentifier: cmd.groupingIdentifier).format(),
      ]))
    }

    state.Lifeline = final_string;

  } else if ( cmd.groupingIdentifier == 0x02 ) { // AssociationSet
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      isStateChange = state.hasAssociationSet ? false : true
      state.hasAssociationSet = true
    } else {
      state.hasAssociationSet = false
      result << response(delayBetween([
        zwave.associationV1.associationSet(groupingIdentifier: cmd.groupingIdentifier, nodeId: [zwaveHubNodeId]).format(),
        zwave.associationV1.associationGet(groupingIdentifier: cmd.groupingIdentifier).format(),
      ]))
    }

    state.AssociationSet = final_string;

  } else if ( cmd.groupingIdentifier == 0x03 ) { // DoubleTap
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      isStateChange = state.hasDoubleTap ? false : true
      state.hasDoubleTap = true
    } else {
      state.DoubleTap = false
      result << response( delayBetween([
        zwave.associationV1.associationRemove(groupingIdentifier: cmd.groupingIdentifier, nodeId: zwaveHubNodeId).format(),
        zwave.associationV1.associationGet(groupingIdentifier: cmd.groupingIdentifier).format(),
      ]))
    }

    state.DoubleTap = final_string;

  } else {
    logger("Association group ${cmd.groupingIdentifier} is unknown", "error");
    return
  }

  updateDataValue("$group_name", "${final_string}")
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd, result) {
  logger("$device.displayName $cmd")

  def versions = commandClassVersions
  def version = versions[cmd.commandClass as Integer]
  def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
  def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
  if (encapsulatedCommand) {
    zwaveEvent(encapsulatedCommand, result)
  }
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

def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelReport cmd, result) {
  logger("zwaveEvent(): Powerlevel Report received: ${cmd}")
  def device_power_level = (cmd.powerLevel > 0) ? "minus${cmd.powerLevel}dBm" : "NormalPower"
  logger("Powerlevel Report: Power: ${device_power_level}, Timeout: ${cmd.timeout}", "info")
  result << createEvent(name: "Power", value: device_power_level)
}

def zwaveEvent(physicalgraph.zwave.commands.controllerreplicationv1.CtrlReplicationTransferGroup cmd, result) {
  logger("$device.displayName $cmd")
  updateDataValue("CtrlReplicationTransferGroup", "$cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.controllerreplicationv1.CtrlReplicationTransferGroupName cmd, result) {
  logger("$device.displayName $cmd")
  updateDataValue("CtrlReplicationTransferGroupName", "$cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.controllerreplicationv1.CtrlReplicationTransferScene cmd, result) {
  logger("$device.displayName $cmd")
  updateDataValue("CtrlReplicationTransferScene", "$cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.controllerreplicationv1.CtrlReplicationTransferSceneName cmd, result) {
  logger("$device.displayName $cmd")
  updateDataValue("CtrlReplicationTransferSceneName", "$cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.protectionv1.ProtectionReport cmd, result) {    
  if (cmd.protectionState == 0) {
    result << createEvent(name: "Protection", value: "disabled", descriptionText: "Protection Mode Disabled")
  } else if (cmd.protectionState == 1) {
    result << createEvent(name: "Protection", value: "sequence", descriptionText: "Protection Mode set to Sequence Control")
  } else if (cmd.protectionState == 2) {
    result << createEvent(name: "Protection", value: "remote", descriptionText: "Protection Mode set to Remote Only")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd, result) {
  logger("$device.displayName command not implemented: $cmd")
  result << createEvent(name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false)
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "DeviceReset", value: "true", descriptionText: cmd.toString(), isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationBusy cmd, result) {
  logger("$device.displayName $cmd")
  switch (cmd.status) {
    case 0:
    logger("Try again later ${cmd.waitTime}")
    break
    case 1:
    logger("Try again in ${cmd.waitTime} seconds")
    break
    case 2:
    logger("Request queued ${cmd.waitTime}")
    break
    default:
    logger("Unknown Status ${cmd.status}", "error")
    break
  }
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationRejectedRequest cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.applicationcapabilityv1.CommandCommandClassNotSupported cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  logger("$device.displayName command not implemented: $cmd", "unknownCommand")
}

def connect() {
  logger("$device.displayName connect()")  
  trueOn(true)
}

def on() {
  logger("$device.displayName on()")
  trueOn(false)
}
  
private trueOn(Boolean physical = true) {
  if (state.lastBounce && (Calendar.getInstance().getTimeInMillis() - state.lastBounce) < 2000 ) {
    logger("$device.displayName bounce", "warn")
    return
  }
  state.lastBounce = Calendar.getInstance().getTimeInMillis()
  
  if (physical) { // Add option to have digital commands execute buttons
    buttonEvent("on()", 1, false, "digital")
  }

  def cmds = []
  cmds << zwave.basicV1.basicSet(value: 0xFF).format()
  cmds << "delay 500"
  cmds << zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0xff, sceneId: zwaveHubNodeId).format();
  cmds << "delay 5000"
  cmds << zwave.switchBinaryV1.switchBinaryGet().format()
  
  delayBetween(cmds, 5000)
}

def off() {
  logger("$device.displayName off()")

  if (settings.disbableDigitalOff) {
    logger("..off() disabled")
    return zwave.switchBinaryV1.switchBinaryGet().format()
  }

  trueOff(false)
}

def disconnect() {
  logger("$device.displayName disconnect()") 
  trueOff(true)
}

private trueOff(Boolean physical = true) {
  if (state.lastBounce && (Calendar.getInstance().getTimeInMillis() - state.lastBounce) < 2000 ) {
    logger("$device.displayName bounce", "warn")
    return
  }
  state.lastBounce = Calendar.getInstance().getTimeInMillis()
  
  if (physical) { // Add option to have digital commands execute buttons
    buttonEvent("off()", 2, false, "digital")
  }

  def cmds = []
  if (settings.delayOff) {
    // cmds << zwave.versionV1.versionGet()
    // cmds << zwave.zwaveCmdClassV1.zwaveCmdNop()
    cmds << "delay 3000";
  }

  cmds << zwave.basicV1.basicSet(value: 0x00).format()
  cmds << "delay 5000"
  cmds << zwave.switchBinaryV1.switchBinaryGet().format()

  delayBetween( cmds ) //, settings.delayOff ? 3000 : 600 )
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
  zwave.switchBinaryV1.switchBinaryGet().format()
}

def poll() {
  response(zwave.switchBinaryV1.switchBinaryGet())
}

def refresh() {
  logger("refresh()")
  
  def cmds = [
    zwave.switchBinaryV1.switchBinaryGet()
  ]

  if ( state.productId == 0x3032 || state.productId == 3036 ) {
    Integer[] parameters = getConfigurationOptions(cmd.productId)

    parameters.each {
      cmds << zwave.configurationV1.configurationGet(parameterNumber: it)
    }
  }

  cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet()

  if (device.currentState('firmwareVersion') == null) {
    cmds << zwave.versionV1.versionGet()
  }

  return sendCommands(cmds, 3000)
}

void indicatorWhenOn() {
  if ( state.productTypeId != 0x4F50) {
    sendEvent(name: "indicatorStatus", value: "when on", displayed: false)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 3, size: 1).format()))
  }
}

void indicatorWhenOff() {
  if ( state.productTypeId != 0x4F50) {
    sendEvent(name: "indicatorStatus", value: "when off", displayed: false)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 3, size: 1).format()))
  }
}

void indicatorNever() {
  if ( state.productTypeId != 0x4F50) {
    sendEvent(name: "indicatorStatus", value: "never", displayed: false)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [2], parameterNumber: 3, size: 1).format()))
  }
}

def invertSwitch(invert=true) {
  if ( state.productTypeId != 0x4F50) {
    state.Invert = invert
    if (invert) {
      sendHubCommand([zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 4, size: 1)])
    } else {
      sendHubCommand([zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 4, size: 1)])
    }
  }
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
private prepCommands(cmds, delay=1000) {
  return response(delayBetween(cmds.collect{ (it instanceof physicalgraph.zwave.Command ) ? encapCommand(it).format() : it }, delay))
}

/**
 *  sendCommands(cmds, delay=200)
 *
 *  Sends a list of commands directly to the device using sendHubCommand.
 *  Uses encapCommand() to apply security or CRC16 encapsulation as needed.
 **/
private sendCommands(cmds, delay=1000) {
  sendHubCommand( cmds.collect{ (it instanceof physicalgraph.zwave.Command ) ? response(encapCommand(it)) : response(it) }, delay)
}

/**
 *  logger()
 *
 *  Wrapper function for all logging:
 *    Logs messages to the IDE (Live Logging), and also keeps a historical log of critical error and warning
 *    messages by sending events for the device's logMessage attribute.
 *    Controlled by loggingLevelIDE and settings.debugLevel
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
    if (settings.debugLevel >= 3) log.info msg
      return

    case "debug":
    if (settings.debugLevel >= 4) log.debug msg
      return

    case "trace":
    if (settings.debugLevel >= 5) log.trace msg
      return

    case "error":
    default:
    break
  }

  log.error msg
  sendEvent(name: "lastError", value: "ERROR: ${msg}", displayed: false, isStateChange: true)
}
