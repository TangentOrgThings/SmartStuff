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

def getDriverVersion() {
  return "v4.01"
}

metadata {
  definition (name: "GE Branded Switch", namespace: "TangentOrgThings", author: "brian@tangent.org", ocfDeviceType: "oic.d.switch") {
    capability "Actuator"
    capability "Button"
    // capability "Health Check"
    capability "Indicator"
    capability "Light"    
    capability "Polling"
    capability "Refresh"
    capability "Sensor"
    capability "Switch"

    attribute "DeviceReset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message

    attribute "MSR", "string"
    attribute "Manufacturer", "string"
    attribute "ManufacturerCode", "string"
    attribute "NIF", "string"
    attribute "NodeLocation", "string"
    attribute "NodeName", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"
    attribute "driverVersion", "string"
    attribute "firmwareVersion", "string"
    attribute "FirmwareMdReport", "string"

    attribute "LifeLine", "string"
    attribute "AssociationSet", "string"
    attribute "DoubleTap", "string"

    attribute "invertedStatus", "enum", ["false", "true"]

    attribute "keyAttributes", "number"

    attribute "Scene", "number"
    attribute "Scene_1", "number"
    attribute "Scene_1_Duration", "number"
    attribute "Scene_2", "number"
    attribute "Scene_2_Duration", "number"

    attribute "SwitchAll", "string"
    attribute "Power", "string"

    command "connect"
    command "disconnect"

    fingerprint mfr:"0063", prod: "4952", model:"3031", deviceJoinName: "Jasco/GE 12721 In-Wall Duplex Receptacle" //, cc: "0x20, 0x25, 0x27, 0x70, 0x72, 0x73, 0x75, 0x77, 0x86" // OUTLET has CC PROTECTION
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

    valueTile("driverVersion", "device.driverVersion", width:2, height: 2, decoration: "flat", inactiveLabel: false) {
      state "default", label: '${currentValue}'
    }

    standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
    }

    main("switch")
    details(["switch", "indicator", "driverVersion", "refresh"])
  }
}

def getCommandClassVersions() {
  [
    0x20: 1,  // Basic
    0x25: 1,  // Switch Binary
    0x27: 1,  // Switch All
    0x2B: 1,  // SceneActivation
    0x2C: 1,  // Scene Actuator Conf
    0x59: 1,  // Association Grp Info
    0x5A: 1,  // Device Reset Locally
    0x56: 1,  // Crc16Encap
    0x70: 1,  // Configuration
    0x72: 2,  // Manufacturer Specific
    0x73: 1, // Powerlevel
    0x7A: 2,  // Firmware Update Md
    0x85: 2,  // Association  0x85  V1 V2
    0x86: 1,  // Version
  ]
}

def prepDevice() {
  [
    zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
    zwave.firmwareUpdateMdV2.firmwareMdGet(),
    zwave.associationV2.associationGroupingsGet(),
    zwave.powerlevelV1.powerlevelGet(),
    zwave.switchAllV1.switchAllGet(),
    // zwave.configurationV1.configurationSet(configurationValue: [ledIndicator == "on" ? 1 : ledIndicator == "never" ? 2 : 0], parameterNumber: 3, size: 1),
    // zwave.configurationV1.configurationSet(configurationValue: [invertSwitch == true ? 1 : 0], parameterNumber: 4, size: 1),
    // zwave.configurationV1.configurationGet(parameterNumber: 3),
    // zwave.configurationV1.configurationGet(parameterNumber: 4),
    zwave.switchBinaryV1.switchBinaryGet(),
  ]
}

def updated() {
  state.loggingLevelIDE = debugLevel ? debugLevel : 4
  log.info("$device.displayName updated() debug: ${state.loggingLevelIDE}")

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)

  if ($zwInfo) {
    log.debug("$device.displayName $zwInfo")
    sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
  } else {
    log.debug("$device.displayName has no ZwaveInfo")
  }

  // Device-Watch simply pings if no device events received for 32min(checkInterval)
  sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])

  // Check in case the device has been changed
  state.manufacturer = null
  updateDataValue("MSR", null)
  updateDataValue("manufacturer", null)

  sendEvent(name: "driverVersion", value: getDriverVersion(), isStateChange: true)
  sendEvent(name: "ledIndicator", value: "when off", displayed: true, isStateChange: true)

  sendEvent(name: "numberOfButtons", value: 4, displayed: false)

  sendCommands(prepDevice())
}

def installed() {
  log.debug ("installed()")

  def zwInfo = getZwaveInfo()

  if ($zwInfo) {
    log.debug("$device.displayName $zwInfo")
    sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
  } else {
    log.debug("$device.displayName has no ZwaveInfo")
  }

  // Device-Watch simply pings if no device events received for 86220 (one day minus 3 minutes)
  sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])

  sendEvent(name: "ledIndicator", value: "when off", displayed: true, isStateChange: true)

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed:true)

  sendCommands(prepDevice())
}

def parse(String description) {
  def result = null

  if (description && description.startsWith("Err")) {
    log.error "parse error: ${description}"
    result = []
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
    logger("parse() called with NULL description", "warn")
  } else if (description != "updated") {
    def cmd = zwave.parse(description)

    if (cmd) {
      result = zwaveEvent(cmd)

      if (! result) {
        logger("zwave.parse() retured an empty list", "warn")
      } else {
        if (provideResults) {
          log.debug "RESULT: ${result}"
        }
      }
    } else {
      log.warn "zwave.parse() failed for: ${description}"
      result = createEvent(name: "lastError", value: "zwave.parse() failed for: ${description}", descriptionText: description)
    }
  }

  return result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
  logger ("$device.displayName $cmd")
  [ createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical") ]
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
  logger ("$device.displayName $cmd")
  def result = []
  
  result << createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical")
  if (cmd.value == 255) {
    result << createEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "Double-tap up (button 1) on $device.displayName", isStateChange: true, type: "physical")
  } else if (cmd.value == 0) {
    result << createEvent(name: "button", value: "pushed", data: [buttonNumber: 2], descriptionText: "Double-tap down (button 2) on $device.displayName", isStateChange: true, type: "physical")
  }
  
  return result
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinarySet cmd) {
  logger("$device.displayName $cmd")
  [ createEvent(name: "switch", value: cmd.switchValue ? "on" : "off", type: "physical") ]
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
  logger ("$device.displayName $cmd")
  [ createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "digital") ]
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
  logger "ConfigurationReport() $cmd"

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
    name = "Unknown"
    value= "VALUE"
    break
  }

  [ createEvent(name: name, value: value) ]
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
  logger "ConfigurationReport() $cmd"

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
    name = "Unknown"
    value= "VALUE"
    break
  }

  [ createEvent(name: name, value: value) ]
}
/*
def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv1.ManufacturerSpecificReport cmd) {
setManufacturerSpecificReport(cmd.manufacturerId, cmd.productTypeId, cmd.productId, cmd.manufacturerName)
}
 */

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  setManufacturerSpecificReport(cmd.manufacturerId, cmd.productTypeId, cmd.productId, cmd.manufacturerName)
}

def zwaveEvent(physicalgraph.zwave.commands.nodenamingv1.NodeNamingNodeNameReport cmd) {
  int length = cmd.nodeName.size()
  logger "NodeNamingNodeNameReport: ($length)"
  /*
  String nodeName = cmd.nodeName.each { String.format("%02x ", it) }.join()
  state.nodeName = nodeName
   */
  state.nodeName= "Not Implemented"
  [ createEvent(
    name: "NodeName",
    value: state.nodeName,
    displayed: true,
    isStateChange: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.nodenamingv1.NodeNamingNodeLocationReport cmd) {
  int length = cmd.nodeLocation.size()
  logger "NodeNamingNodeLocationReport:  ($length)"
  /*
  String nodeLocation = cmd.nodeLocation.each { String.format("%02x ", it) }.join()
  state.nodeLocation= nodeLocation
   */
  state.nodeLocation= "Not Implemented"
  [ createEvent(
    name: "NodeLocation",
    value: state.nodeLocation,
    displayed: true,
    isStateChange: true) ]
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

def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfGet cmd) {
  logger("$device.displayName $cmd")
  buttonEvent("SceneActuatorConfGet", cmd.sceneId, false, "digital")

  response(zwave.sceneActuatorConfV1.sceneActuatorConfReport(dimmingDuration: 0xFF, level: 0xFF, sceneId: cmd.sceneId))
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfReport cmd) {
  logger("$device.displayName $cmd")

  // HomeSeer (ST?) does not implement this scene
  if (cmd.sceneId == 0) {
    return [
      createEvent(name: "Scene", value: cmd.sceneId, isStateChange: true, displayed: true),
      createEvent(name: "level", value: cmd.level, isStateChange: true, displayed: true),
      createEvent(name: "switch", value: cmd.level == 0 ? "off" : "on", isStateChange: true, displayed: true),
    ]
  }

  def cmds = []

  if (cmd.sceneId == 1) {
    if (cmd.level != 255) {
      cmds << zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: cmd.sceneId, dimmingDuration: 0xFF, level: 255, override: true).format()
    }
  } else if (cmd.sceneId == 2) {
    if (cmd.level) {
      cmds << zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: cmd.sceneId, dimmingDuration: 0xFF, level: 0, override: true).format()
    }
  }

  String scene_name = "Scene_$cmd.sceneId"
  String scene_duration_name = String.format("Scene_%d_Duration", cmd.sceneId)

  def result = [
    createEvent(name: "$scene_name", value: cmd.level, isStateChange: true, displayed: true),
    createEvent(name: "$scene_duration_name", value: cmd.dimmingDuration, isStateChange: true, displayed: true),
    createEvent(name: "Scene", value: cmd.sceneId, isStateChange: true, displayed: true),
  ]

  if (cmds) {
    result << response(delayBetween(cmds, 1000))
  }

  return result
}

/*
def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd) {
  logger("$device.displayName $cmd")

  Integer button = ((cmd.sceneId + 1) / 2) as Integer
  Boolean held = !(cmd.sceneId % 2)
  buttonEvent(button, held, "digital")
}
*/

def zwaveEvent(physicalgraph.zwave.commands.switchallv1.SwitchAllReport cmd) {
  logger("$device.displayName $cmd")
  
  def result = []

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
    result << response(delayBetween([
      zwave.switchAllV1.switchAllSet(mode: 0x00).format(),
      zwave.switchAllV1.switchAllGet().format(),
    ], 5000))
  } else {
    result << createEvent(name: "SwitchAll", value: msg, isStateChange: true, displayed: true)
  }
  
  return result
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
  logger("$device.displayName $cmd")
  
  def result = []

  state.groups = cmd.supportedGroupings

  if (cmd.supportedGroupings) {
    def cmds = []
    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: true, refreshCache: true).format()
      cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x).format()
    }
    
    result << response(delayBetween(cmds, 2000))

    return result
  }
  
  logger("$device.displayName AssociationGroupingsReport: $cmd", "error")
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd) {
  logger("$device.displayName $cmd")

  def name = new String(cmd.name as byte[])
  logger("Association Group #${cmd.groupingIdentifier} has name: ${name}", "info")

  def result = []
  result << response( zwave.associationV2.associationGet(groupingIdentifier: cmd.groupingIdentifier) )
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
  logger("$device.displayName $cmd")
  
  def result = []

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
    }

    state.Lifeline = final_string;

    result << createEvent(name: "LifeLine",
        value: "${final_string}",
        displayed: true,
        isStateChange: isStateChange)
  } else if ( cmd.groupingIdentifier == 0x02 ) { // AssociationSet
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      isStateChange = state.hasAssociationSet ? false : true
      state.hasAssociationSet = true
    } else {
      state.hasAssociationSet = false
    }

    state.AssociationSet = final_string;

    result << createEvent(name: "AssociationSet",
      value: "${final_string}",
      displayed: true,
      isStateChange: isStateChange)
  } else if ( cmd.groupingIdentifier == 0x03 ) { // DoubleTap
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      isStateChange = state.hasDoubleTap ? false : true
      state.hasDoubleTap = true
    } else {
      state.DoubleTap = false
    }

    state.DoubleTap = final_string;

    result << createEvent(name: "DoubleTap",
      value: "${final_string}",
      displayed: true,
      isStateChange: isStateChange)
  } else {
    logger("Association group ${cmd.groupingIdentifier} is unknown", "error")
  }

  if ( state.hasAssociationSet == true && state.hasDoubleTap == true && state.hasLifeLine == true ) {
    state.isAssociated = true
  } else {
    state.isAssociated = false
  }

  if (! state.isAssociated ) {
    result << response(delayBetween([
      zwave.associationV1.associationSet(groupingIdentifier: 0x01, nodeId: [zwaveHubNodeId]).format(),
      zwave.associationV1.associationSet(groupingIdentifier: 0x02, nodeId: [zwaveHubNodeId]).format(),
      zwave.associationV1.associationSet(groupingIdentifier: 0x03, nodeId: [zwaveHubNodeId]).format(),
      // zwave.associationV1.associationGet(groupingIdentifier: 0x01).format(),
      // zwave.associationV1.associationGet(groupingIdentifier: 0x02).format(),
      // zwave.associationV1.associationGet(groupingIdentifier: 0x03).format(),
    ], 5000))
  }
  
  return result
}

private setManufacturerSpecificReport(manufacturerId, productTypeId, productId, manufacturerName) {
  logger("$device.displayName $cmd")

  state.manufacturerId = manufacturerId
  state.productTypeId = productTypeId
  state.productId = productId
  state.manufacturerName = manufacturerName ? manufacturerName : "GE"

  def manufacturerCode = String.format("%04X", manufacturerId)
  def productTypeCode = String.format("%04X", productTypeId)
  def productCode = String.format("%04X", productId)

  sendEvent(name: "ManufacturerCode", value: manufacturerCode)
  sendEvent(name: "ProduceTypeCode", value: productTypeCode)
  sendEvent(name: "ProductCode", value: productCode)

  if (state.productId == 0x3036) {
    sendEvent(name: "numberOfButtons", value: 4, displayed: false)
    if (! state.FirmwareMdReport ) {
      sendCommands(
        [
        // zwave.configurationV1.configurationGet(parameterNumber: 3),
        // zwave.configurationV1.configurationGet(parameterNumber: 4),
        // zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: 1),
        // zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: 2),
        zwave.firmwareUpdateMdV1.firmwareMdGet(),
        // zwave.associationV2.associationGroupingsGet(),
        ], 2000)
    }
  } else if (state.productId == 0x3031) {
    sendCommands(
      [
      zwave.configurationV1.configurationGet(parameterNumber: 3),
      ], 2000)
  }

  def msr = String.format("%04X-%04X-%04X", manufacturerId, productTypeId, productId)
  updateDataValue("MSR", msr)

  updateDataValue("manufacturer", state.manufacturerName)

  sendEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: false)
  [ createEvent(name: "Manufacturer", value: "${state.manufacturerName}", descriptionText: "$device.displayName", isStateChange: false) ]
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
  logger("$device.displayName $cmd")

  def versions = getCommandClassVersions()
  def version = versions[cmd.commandClass as Integer]
  def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
  def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
  if (encapsulatedCommand) {
    zwaveEvent(encapsulatedCommand)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
  logger("VersionReport() $cmd")
  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  [ createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd) {
  logger("$device.displayName $cmd")

  def firmware_report = String.format("%s-%s-%s", cmd.checksum, cmd.firmwareId, cmd.manufacturerId)
  state.FirmwareMdReport = firmware_report
  updateDataValue("FirmwareMdReport", firmware_report)
  [ createEvent(name: "FirmwareMdReport", value: firmware_report, descriptionText: "$device.displayName FIRMWARE_REPORT: $firmware_report", isStateChange: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelReport cmd) {
  logger("zwaveEvent(): Powerlevel Report received: ${cmd}")
  
  def result = []
  
  def device_power_level = (cmd.powerLevel > 0) ? "minus${cmd.powerLevel}dBm" : "NormalPower"
  logger("Powerlevel Report: Power: ${device_power_level}, Timeout: ${cmd.timeout}", "info")
  result << createEvent(name: "Power", value: device_power_level)
}

def zwaveEvent(physicalgraph.zwave.commands.controllerreplicationv1.CtrlReplicationTransferScene cmd) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.protectionv1.ProtectionReport cmd) {
  logger("$device.displayName $cmd")

  state.protectionState= cmd.protectionState
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd) {
  logger("$device.displayName command not implemented: $cmd")
  [ createEvent(name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false) ]
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd) {
  logger("$device.displayName $cmd")
  [ createEvent(name: "DeviceReset", value: "true", descriptionText: cmd.toString(), isStateChange: true, displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd) {
  logger("$device.displayName $cmd")
  [ createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationBusy cmd) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
  logger("$device.displayName command not implemented: $cmd", "error")
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
  state.lastBounce = new Date().time
  
  if (physical) { // Add option to have digital commands execute buttons
    buttonEvent("on()", 1, false, "digital")
  }

  def cmds = []
  cmds << zwave.switchBinaryV1.switchBinarySet(switchValue: 0xFF).format()
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
  state.lastBounce = new Date().time
  
  if (physical) { // Add option to have digital commands execute buttons
    buttonEvent("off()", 2, false, "digital")
  }

  sendEvent(name: "switch", value: "off");
  def cmds = []
  if (settings.delayOff) {
    // cmds << zwave.versionV1.versionGet()
    // cmds << zwave.zwaveCmdClassV1.zwaveCmdNop()
    cmds << "delay 3000";
  }

  // cmds << zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0xff, sceneId: 2).format();
  // cmds << physical ? zwave.basicV1.basicSet(value: 0x00).format() : zwave.switchBinaryV1.switchBinarySet(switchValue: 0x00).format();
  cmds << zwave.switchBinaryV1.switchBinarySet(switchValue: 0x00).format()
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
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 3)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 4)
  } else if ( state.productId == 0x3031 ) {
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 3)
  }

  if (getDataValue("MSR") == null) {
    cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet()
  }

  if (device.currentState('firmwareVersion') == null) {
    cmds << zwave.versionV1.versionGet()
  }

  return sendCommands(cmds)
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
