// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :
/**
 *  Eaton Cooper ASPIRE RF Accessory Switch (RF9517)
 *  https://products.z-wavealliance.org/products/742
 *  https://products.z-wavealliance.org/products/479 ( Older Version )
 *
 *  Copyright 2017-2018 Brian Aker
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


def getDriverVersion () {
  return "v1.51"
}

def getConfigurationOptions(Integer model) {
  if (1) {
    return [ 1 ]
  }
  return [ 1, 2, 3, 4, 5, 6, 7 ]
}

metadata {
  definition (name: "ASPIRE RF Accessory Switch", namespace: "TangentOrgThings", author: "Brian Aker") {
    capability "Actuator"
    capability "Button"
    capability "Indicator"
    capability "Sensor"
    capability "Switch"

    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message

    attribute "Manufacturer", "string"
    attribute "ManufacturerCode", "string"
    attribute "MSR", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"
    attribute "firmwareVersion", "string"
    attribute "zWaveProtocolVersion", "string"
    attribute "driverVersion", "string"

    attribute "Associated", "string"

    attribute "Scene", "number"
    attribute "setScene", "enum", ["Unknown", "Set", "Setting"]
    
    attribute "SwitchAll", "string"
    attribute "Power", "string"
    attribute "Protection", "string"

    attribute "NIF", "string"

    fingerprint mfr: "001A", prod: "5352", model: "0000", deviceJoinName: "Eaton Cooper Aspire RF Accessory Switch (RF9517)"
  }


  simulator {
    // TODO: define status and reply messages here
  }

  preferences {
    input name: "DelayedOFF", type: "number", title: "Associated Device", description: "... ", range: "1..127", required: false
    input name: "associatedDevice", type: "number", title: "Associated Device", description: "... ", required: false
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  }

  tiles {
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
      tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label: '${name}', action: "disconnect", icon: "st.switches.switch.on", backgroundColor: "#00A0DC"
        attributeState "off", label: '${name}', action: "connect", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
      }

      /*
      tileAttribute("device.indicatorStatus", key: "SECONDARY_CONTROL") {
        attributeState("when off", label:'${currentValue}', icon:"st.indicators.lit-when-off")
        attributeState("when on", label:'${currentValue}', icon:"st.indicators.lit-when-on")
        attributeState("never", label:'${currentValue}', icon:"st.indicators.never-lit")
      } */
    }
    
    standardTile("tap", "device.button", width: 2, height: 2, decoration: "flat") {
      state "default", label: "", backgroundColor: "#D3D3D3"
      state "pushed", label: "${currentValue}", backgroundColor: "#79b821"
    }

    valueTile("scene", "device.Scene", width: 2, height: 2, decoration: "flat", inactiveLabel: false) {
      state "default", label: '${currentValue}'
    }

    valueTile("setScene", "device.setScene", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "Set", label: '${name}', nextState: "Setting"
      state "Setting", label: '${name}' //, nextState: "Set_Scene"
    }

    main (["switch"])
    details(["switch", "tap", "scene", "setScene"])
  }
}

def getCommandClassVersions() {
  [
    0x20: 1,  // Basic
    0x27: 1,  // Switch All
    0x2B: 1,  // SceneActivation
    0x2C: 1,  // Scene Actuator Conf
    0x70: 1,  // Configuration
    0x72: 2,  // Manufacturer Specific v1
    0x73: 1,  // Powerlevel
    0x75: 1,  // Protection
    // 0x77: 1,  // Node Naming
    0x84: 2, // Wake Up 
    0x85: 2,  // Association  0x85  V1
    0x86: 1,  // Version
    0x87: 1,  // Indicator
  ]
}

// parse events into attributes
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
    logger( "parse() called with NULL description", "warn")
  } else if (description != "updated") {
    def cmd = zwave.parse(description, getCommandClassVersions())

    if (cmd) {
      zwaveEvent(cmd, result)
    } else {
      logger( "zwave.parse(CC) failed for: ${description}", "error")

      cmd = zwave.parse(description)
      if (cmd) {
        zwaveEvent(cmd, result)
      } else {
        logger( "zwave.parse() failed for: ${description}", "error")
      }
    }
  }

  return result
}

def buttonEvent(String exec_cmd, Integer button, Boolean held, result) {
  logger("buttonEvent: $button  held: $held  exec: $exec_cmd")

  String heldType = held ? "held" : "pushed"

  if (button > 0) {
    result << createEvent(name: "button", value: heldType, data: [buttonNumber: 1, status: (button == 1 ? "on" : "off")], descriptionText: "$device.displayName $exec_cmd button $button was pushed", isStateChange: true)
  } else {
    result << createEvent(name: "button", value: "", descriptionText: "$device.displayName $exec_cmd button released", isStateChange: true)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "switch", value: cmd.value ? "on" : "off", isStateChange: true, type: "digital")
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "switch", value: cmd.value ? "on" : "off", isStateChange: true, type: "physical")
}


def zwaveEvent(physicalgraph.zwave.commands.indicatorv1.IndicatorReport cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "switch", value: cmd.value ? "on" : "off", isStateChange: true, type: "digital")
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd, result) {
  logger("$device.displayName $cmd")

  if (state.lastScene == cmd.sceneId && (state.repeatCount < 4) && (now() - state.repeatStart < 3000)) {
    logger("Button was repeated")
    state.repeatCount = state.repeatCount + 1
  } else {
    state.lastScene = cmd.sceneId
    state.lastLevel = 0
    state.repeatCount = 0
    state.repeatStart = now()

    buttonEvent("SceneActivationSet", cmd.sceneId, false, result)
    result <<  createEvent(name: "Scene", value: cmd.sceneId, isStateChange: true)
    result <<  createEvent(name: "setScene", value: "Setting", isStateChange: true)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfGet cmd, result) {
  logger("$device.displayName $cmd")
  buttonEvent("SceneActuatorConfGet", cmd.sceneId, false, result)

  result <<  createEvent(name: "setScene", value: "Set", isStateChange: true, displayed: true)
  result << response(zwave.sceneActuatorConfV1.sceneActuatorConfReport(dimmingDuration: 0xFF, level: 0xFF, sceneId: state.lastScene ? state.lastScene : 0))
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
  if (cmd.sceneId == zwaveHubNodeId) {
    if (cmd.level != 255) {
      cmds << zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: cmd.sceneId, dimmingDuration: 0, level: 255, override: true).format()
    }
  }

  state.lastScene = cmd.sceneId

  String scene_name = "Scene_$cmd.sceneId"
  String scene_duration_name = String.format("Scene_%d_Duration", cmd.sceneId)

  result << createEvent(name: "$scene_name", value: cmd.level, isStateChange: true, displayed: true)
  result << createEvent(name: "$scene_duration_name", value: cmd.dimmingDuration, isStateChange: true, displayed: true)
  result << response(cmds)
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd, result) {
  logger("$device.displayName $cmd")
  result <<  createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd, result) {
  logger("$device.displayName $cmd")
  def cmds = []

  if (cmd.supportedGroupings) {
    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationV1.associationGet(groupingIdentifier: x).format()
    }
  }

  result << createEvent(name: "numberOfButtons", value: cmd.supportedGroupings, isStateChange: true, displayed: true)
  result << response( delayBetween(cmds, 2000) )
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd, result) {
  logger("$device.displayName $cmd")

  def reportValue = cmd.configurationValue[0]
  switch (cmd.parameterNumber) {
    case 1:
    logger("DelayedOFF $reportValue")
    if (settings.DelayedOFF != null && settings.DelayedOFF != reportValue) {
      result << response( delayBetween([
        zwave.configurationV1.configurationSet(scaledConfigurationValue: settings.DelayedOFF.toInteger(), parameterNumber: cmd.parameterNumber, size: 1).format(),
        // zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format(),
      ], 2000) )
    }
    break
    default:
    logger("Unknown Configuration Parameter: ${cmd.parameterNumber}")
    break
  }
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$device.displayName $cmd")

  Integer[] associate =  []
  if (0) {
    associate +=  1 // Add SIS (assumption)
  }

  Boolean isStateChange
  String event_value
  String event_descriptionText

  def string_of_assoc = ""
  cmd.nodeId.each {
    string_of_assoc += "${it}, "
  }
  def lengthMinus2 = string_of_assoc.length() ? string_of_assoc.length() - 3 : 0
  def final_string = lengthMinus2 ? string_of_assoc.getAt(0..lengthMinus2) : string_of_assoc

  event_value = final_string

  isStateChange = state.isAssociated ?: false

  state.isAssociated = true
  if (! cmd.nodeId.any { it == zwaveHubNodeId }) {
    isStateChange = true
    associate += zwaveHubNodeId
    state.isAssociated = false
    event_descriptionText = "Hub is not associated"

    result << response( zwave.associationV1.associationSet(groupingIdentifier: cmd.groupingIdentifier, nodeId: zwaveHubNodeId) )
  }

  if ( associatedDevice  && ! cmd.nodeId.any { it == associatedDevice }) {
    associate += associatedDevice
    state.isAssociated = false
    isStateChange = true

    result << response( zwave.associationV1.associationSet(groupingIdentifier: cmd.groupingIdentifier, nodeId: associatedDevice) )
  }

  result << createEvent(name: "Associated",
    value: "${event_value}",
    descriptionText: "${event_descriptionText}",
    displayed: true,
    isStateChange: isStateChange)
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd, result) {
  logger("$device.displayName $cmd")

  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  def zWaveProtocolVersion = "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  result << createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: true)
  result << createEvent(name: "zWaveProtocolVersion", value: "${zWaveProtocolVersion}", descriptionText: "${device.displayName} ${zWaveProtocolVersion}", isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
  logger("$device.displayName $cmd")

  String manufacturerCode = String.format("%04X", cmd.manufacturerId)
  String productTypeCode = String.format("%04X", cmd.productTypeId)
  String productCode = String.format("%04X", cmd.productId)
  String manufacturerName = cmd.manufacturerName ? cmd.manufacturerName : "Leviton"
  updateDataValue("manufacturer", manufacturerName)

  String msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)
  
  Integer[] parameters = getConfigurationOptions(cmd.productId)

  def cmds = []
  parameters.each {
    cmds << zwave.configurationV1.configurationGet(parameterNumber: it).format()
  }

  result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
  result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
  result << createEvent(name: "ProductCode", value: productCode)

  result << createEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: true)
  result << createEvent(name: "Manufacturer", value: "${manufacturerName}", descriptionText: "$device.displayName", isStateChange: true)
  result << response(delayBetween(cmds, 1000))
  result << response(zwave.versionV1.versionGet())
}

def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelReport cmd, result) {
  logger("zwaveEvent(): Powerlevel Report received: ${cmd}")
  def device_power_level = (cmd.powerLevel > 0) ? "minus${cmd.powerLevel}dBm" : "NormalPower"
  logger("Powerlevel Report: Power: ${device_power_level}, Timeout: ${cmd.timeout}", "info")
  result << createEvent(name: "Power", value: device_power_level)
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

def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  logger("$device.displayName no implementation of $cmd", "error")
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
  logger ("$device.displayName ping()")
  delayBetween([
    zwave.manufacturerSpecificV1.manufacturerSpecificGet().format(),
  ])
}

def refresh () {
  logger ("$device.displayName refresh()")
  delayBetween([
    zwave.manufacturerSpecificV1.manufacturerSpecificGet().format(),
  ])
}

def poll() {
  logger ("$device.displayName poll()")
  response( delayBetween([
    zwave.manufacturerSpecificV1.manufacturerSpecificGet().format(),
  ]))
}

def prepDevice() {
  [
    zwave.associationV1.associationSet(groupingIdentifier: 0xFF, nodeId: 0x01),
    zwave.associationV1.associationGet(groupingIdentifier: 0xFF),
    zwave.associationV1.associationGroupingsGet(),
    zwave.manufacturerSpecificV1.manufacturerSpecificGet(),
    zwave.switchAllV1.switchAllGet(),
    zwave.powerlevelV1.powerlevelGet(),
    zwave.protectionV1.protectionGet(),
    zwave.indicatorV1.indicatorGet(),
    zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: zwaveHubNodeId),
    zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: 0),
    zwave.zwaveCmdClassV1.requestNodeInfo(),
    // zwave.associationV1.associationGet(groupingIdentifier: 255),
    // zwave.basicV1.basicGet(),
  ]
}

def installed() {
  log.info("$device.displayName installed()")

  state.loggingLevelIDE = settings.debugLevel ? settings.debugLevel : 4

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  // Device-Watch simply pings if no device events received for 32min(checkInterval)
  // sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])

  sendCommands( prepDevice(), 3000 )
}

def updated() {
  if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
    return
  }
  state.loggingLevelIDE = settings.debugLevel ? settings.debugLevel : 4
  log.info("$device.displayName updated() debug: ${state.loggingLevelIDE}")

  sendEvent(name: "lastError", value: "")
  sendEvent(name: "Scene", value: 0)
  sendEvent(name: "setScene", value: "Unknown")

  // Check in case the device has been changed
  state.manufacturer = null
  updateDataValue("MSR", null)
  updateDataValue("Manufacturer", null)

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  sendCommands( prepDevice(), 3000 )

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
