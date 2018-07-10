// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :
/**
 *  VIZIA RF 1 BUTTON SCENE CONTROLLER
 *  VRCS1 - 1-Button Scene Controller
 *  https://products.z-wavealliance.org/products/316
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
  return "v1.53"
}

metadata {
  definition (name: "Leviton Vizia RF 1 Button Scene Controller", namespace: "TangentOrgThings", author: "Brian Aker") {
    capability "Actuator"
    capability "Button"
    capability "Sensor"

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

    attribute "hail", "string"

    attribute "Scene", "number"
    attribute "setScene", "enum", ["Unknown", "Set", "Setting"]

    attribute "Group 1", "number"
    attribute "Group 1 Duration", "number"
    attribute "Group 2", "number"
    attribute "Group 2 Duration", "number"

    attribute "NIF", "string"

    command "getparamState"

    fingerprint mfr: "001D", prod: "0902", model: "0224", deviceJoinName: "Leviton VRCS1-1LZ Vizia RF + 1-Button Scene Controller"
  }


  simulator {
    // TODO: define status and reply messages here
  }

  preferences {
    input name: "associatedDevice", type: "number", title: "Associated Device", description: "... ", required: false
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  }

  tiles {
    standardTile("tap", "device.button", width: 2, height: 2, decoration: "flat") {
      state "default", label: "", backgroundColor: "#D3D3D3"
      state "pushed", label: "${currentValue}", backgroundColor: "#79b821"
    }

    valueTile("tapme", "device.button", width: 2, height: 2, decoration: "flat", inactiveLabel: false) {
      state "default", label: '${currentValue}'
    }

    valueTile("scene", "device.Scene", width: 2, height: 2, decoration: "flat", inactiveLabel: false) {
      state "default", label: '${currentValue}'
    }

    valueTile("setScene", "device.setScene", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "Set", label: '${name}', nextState: "Setting"
      state "Setting", label: '${name}' //, nextState: "Set_Scene"
    }

    main (["tap"])
    details(["tap", "scene", "setScene", "tapme"])
  }
}

def getCommandClassVersions() {
  [
    0x20: 1,  // Basic
    0x2D: 1,  // Scene Controller Conf
    0x72: 1,  // Manufacturer Specific
    // 0x77: 1,  // Node Naming
    0x82: 1,  // Hail
    0x85: 2,  // Association  0x85  V1 V2
    0x86: 1,  // Version
    0x91: 1,  // Man Prop
    // Note: Controlled but not supported
    0x2B: 1,  // SceneActivation
    0x2C: 1,  // Scene Actuator Conf
    0x22: 1,  // Application Status
    //    0x56: 1,  // Crc16 Encap
    //    0x25: 1,  // Switch Binary
    //    0x91: 1, // Manufacturer Proprietary
    // Stray commands that show up
    0x54: 1,  // Application Status
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
  } else if (description.contains("command: 9100")) {
    logger("PROP $description", "ERROR")
    //handleManufacturerProprietary(description, result)
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

def getparamState() {
  logger("$device.displayName getparamState()")
  // sendHubCommand(new physicalgraph.device.HubAction("91001D0D01FF01180508D3"))
  def cmds = []
  cmds << new physicalgraph.device.HubAction("91001D0D01FF01180508D3")
	return response(delayBetween(cmds, 1000))
}

def handleManufacturerProprietary(String description, result) {
  // log.debug "Handling manufacturer-proprietary command: '${description}'"
  logger("$device.displayName $description")

  switch (description) {
    case "zw device: ${device.deviceNetworkId}, command: 9100, payload: 1D 0C 01 00 ":
    case "1":
    // log.debug "Turning button #1 on"
    logger("button 1 ON")
    updateState("currentButton", "1")
    // result << createEvent(name: "button", value: "on", data: [button: 1, status: "on"], isStateChange: true)
    buttonEvent("handleManufacturerProprietary", 1, false, result)
    break
    case "zw device: ${device.deviceNetworkId}, command: 9100, payload: 1D 0C 05 00 ":
    case "5":
    // log.debug "Turning button #1 off"
    logger("button 1 OFF")
    updateState("currentButton", "1")
    // result << createEvent(name: "button", value: "off", data: [button: 1, status: "off"], isStateChange: true)
    buttonEvent("handleManufacturerProprietary", 2, false, result)
    break
    case "zw device: ${device.deviceNetworkId}, command: 9100, payload: 1D 0C 02 00 ":
    case "2":
    // log.debug "Turning button #2 on"
    logger("button 2 ON")
    updateState("currentButton", "2")
    // result << createEvent(name: "button", value: "on", data: [button: 2, status: "on"], isStateChange: true)
    break
    case "zw device: ${device.deviceNetworkId}, command: 9100, payload: 1D 0C 06 00 ":
    case "6":
    // log.debug "Turning button #2 off"
    logger("button 2 OFF")
    updateState("currentButton", "2")
    // result << createEvent(name: "button", value: "off", data: [button: 2, status: "off"], isStateChange: true)
    break
    case "zw device: ${device.deviceNetworkId}, command: 9100, payload: 1D 0C 03 00 ":
    case "3":
    logger("button 3 ON")
    // log.debug "Turning button #3 on"
    updateState("currentButton", "3")
    // result << createEvent(name: "button", value: "on", data: [button: 3, status: "on"], isStateChange: true)
    break
    case "zw device: ${device.deviceNetworkId}, command: 9100, payload: 1D 0C 07 00 ":        
    case "7":
    // log.debug "Turning button #3 off"
    logger("button 3 OFF")
    updateState("currentButton", "3")
    // result << createEvent(name: "button", value: "off", data: [button: 3, status: "off"], isStateChange: true)
    break
    case "zw device: ${device.deviceNetworkId}, command: 9100, payload: 1D 0C 04 00 ":
    case "4":
    // log.debug "Turning button #4 on"
    logger("button 4 ON")
    updateState("currentButton", "4")
    // result << createEvent(name: "button", value: "on", data: [button: 4, status: "on"], isStateChange: true)
    break
    case "zw device: ${device.deviceNetworkId}, command: 9100, payload: 1D 0C 08 00 ":
    case "8":
    // log.debug "Turning button #4 off"
    logger("button 4 OFF")
    updateState("currentButton", "4")
    // result << createEvent(name: "button", value: "off", data: [button: 4, status: "off"], isStateChange: true)
    break
    default:
    logger("Could not parse vendor hidden command: $description", "error")
    // log.debug "Fell through to default switch case"
    break
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

def zwaveEvent(physicalgraph.zwave.commands.scenecontrollerconfv1.SceneControllerConfReport cmd, result) {
  logger("$device.displayName $cmd")

  if (cmd.groupId == 1 && cmd.sceneId != zwaveHubNodeId) {
    result << response(delayBetween([
      zwave.sceneControllerConfV1.sceneControllerConfSet(groupId: cmd.groupId, sceneId: zwaveHubNodeId).format(),
      zwave.sceneControllerConfV1.sceneControllerConfGet(groupId: cmd.groupId).format(),
    ]))
  } else if (cmd.groupId == 2 && cmd.sceneId != zwaveHubNodeId +1) {
    result << response(delayBetween([
      zwave.sceneControllerConfV1.sceneControllerConfSet(groupId: cmd.groupId, sceneId: zwaveHubNodeId +1).format(),
      zwave.sceneControllerConfV1.sceneControllerConfGet(groupId: cmd.groupId).format(),
    ]))
  }

  String group_scene_name =  "Group ${cmd.groupId}"
  String group_duration_name =  "Group ${cmd.groupId} Duration"
  result <<  createEvent(name: group_scene_name, value: cmd.sceneId, isStateChange: true, displayed: true)
  result <<  createEvent(name: group_duration_name, value: cmd.dimmingDuration, isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.scenecontrollerconfv1.SceneControllerConfSet cmd, result) {
  logger("$device.displayName $cmd")
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

  for (def x = 1; x <= 2; x++) {
    cmds << zwave.sceneControllerConfV1.sceneControllerConfGet(groupId: x).format()
  }

  result << createEvent(name: "numberOfButtons", value: cmd.supportedGroupings, isStateChange: true, displayed: true)
  result << response( delayBetween(cmds, 2000) )
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

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv1.ManufacturerSpecificReport cmd, result) {
  logger("$device.displayName $cmd")

  String manufacturerCode = String.format("%04X", cmd.manufacturerId)
  String productTypeCode = String.format("%04X", cmd.productTypeId)
  String productCode = String.format("%04X", cmd.productId)
  String manufacturerName = cmd.manufacturerName ? cmd.manufacturerName : "Leviton"
  updateDataValue("manufacturer", manufacturerName)

  String msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)

  result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
  result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
  result << createEvent(name: "ProductCode", value: productCode)

  result << createEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: true)
  result << createEvent(name: "Manufacturer", value: "${manufacturerName}", descriptionText: "$device.displayName", isStateChange: true)
  result << response(zwave.versionV1.versionGet())
}

def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  logger("$device.displayName no implementation of $cmd", "error")
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd, result) {
  logger("$device.displayName command not implemented: $cmd")
  result << createEvent(name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false)
}

def zwaveEvent(physicalgraph.zwave.commands.networkmanagementprimaryv1.ControllerChangeStatus cmd, result) {
  logger("$device.displayName command not implemented: $cmd", "error")
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
    zwave.associationV1.associationGroupingsGet(),
    // zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration: 0xFF, groupId:1, sceneId:1),
    // zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration: 0xFF, groupId:2, sceneId:2),
    zwave.manufacturerSpecificV1.manufacturerSpecificGet(),
    // zwave.sceneControllerConfV1.sceneControllerConfGet(groupId: 1),
    // zwave.sceneControllerConfV1.sceneControllerConfGet(groupId: 2),
    // zwave.versionV1.versionGet(),
    // zwave.associationV1.associationGet(groupingIdentifier: 0x01),
    // zwave.basicV1.basicGet(),
    zwave.zwaveCmdClassV1.requestNodeInfo(),
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
