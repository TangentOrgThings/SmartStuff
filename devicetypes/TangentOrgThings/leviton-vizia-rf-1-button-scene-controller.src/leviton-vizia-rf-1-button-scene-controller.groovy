// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :
/**
 *  VIZIA RF 1 BUTTON SCENE CONTROLLER
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
  return "v1.31"
}

metadata {
  definition (name: "Leviton Vizia RF 1 Button Scene Controller", namespace: "TangentOrgThings", author: "Brian Aker") {
    capability "Actuator"
    capability "Button"
    capability "Health Check"
    capability "Momentary"

    command "tapThat"

    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message

    attribute "Manufacturer", "string"
    attribute "ManufacturerCode", "string"
    attribute "MSR", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"
    attribute "NIF", "string"
    attribute "firmwareVersion", "string"
    attribute "zWaveProtocolVersion", "string"
    attribute "driverVersion", "string"

    attribute "Associated", "string"

    attribute "Scene", "number"
    attribute "Scene_1", "number"
    attribute "Scene_1_Duration", "number"
    attribute "Scene_2", "number"
    attribute "Scene_2_Duration", "number"

    attribute "NIF", "string"

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
        state "default", label: "TAP", backgroundColor: "#ffffff", action: "tapThat", icon: "st.Home.home30"
            state "button 1", label: "1", backgroundColor: "#79b821", icon: "st.Home.home30"
      }
  }
}

def getCommandClassVersions() {
  [
    0x20: 1,  // Basic
    0x2D: 1,  // Scene Controller Conf
    0x72: 1,  // Manufacturer Specific
    // 0x77: 1,  // Node Naming
    0x82: 1,  // Hail
    0x85: 1,  // Association  0x85  V1 V2
    0x86: 1,  // Version
    // Note: Controlled but not supported
    //    0x2B: 1,  // SceneActivation
    //    0x2C: 1,  // Scene Actuator Conf
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
    result << createEvent(name: "logMessage", value: "parse() called with NULL description", descriptionText: "$device.displayName")
  } else if (description != "updated") {
    def cmd = zwave.parse(description, getCommandClassVersions())

    if (cmd) {
      zwaveEvent(cmd, result)
    } else {
      log.warn "zwave.parse() failed for: ${description}"
      result << createEvent(name: "lastError", value: "zwave.parse() failed for: ${description}", descriptionText: description)

      cmd = zwave.parse(description)
      if (cmd) {
        log.warn "Trying ${cmd}"
        zwaveEvent(cmd, result)
      } else {
        log.warn "zwave.parse() failed for: ${description}"
        result << createEvent(name: "lastError", value: "zwave.parse() failed for: ${description}", descriptionText: description)
      }
    }
  }

  return result
}

def push () {
  log.debug("$device.displayName push()")
  def result = []

  result += sendHubCommand(new physicalgraph.device.HubAction(zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0, sceneId: 1).format()))

  return result
}

def tapThat() {
  log.debug("$device.displayName tapThat()")

  response(sendCommands( [
    zwave.basicV1.basicSet(value: 0xFF).format(),
    // zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0, sceneId: 1),
  ]))

  buttonEvent("tapThat()", 1, false, "digital")
}

def buttonEvent(String exec_cmd, Integer button, held, buttonType = "physical") {
  logger("buttonEvent: $button  held: $held  type: $buttonType")

  button = button as Integer
  String heldType = held ? "held" : "pushed"

  if (button > 0) {
    sendEvent(name: "button", value: "$heldType", data: [buttonNumber: button], descriptionText: "$device.displayName $exec_cmd button $button was pushed", isStateChange: true, type: "$buttonType")
  } else {
    sendEvent(name: "button", value: "default", descriptionText: "$device.displayName $exec_cmd button released", isStateChange: true, type: "$buttonType")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationBusy cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.scenecontrollerconfv1.SceneControllerConfReport cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.scenecontrollerconfv1.SceneControllerConfSet cmd, result) {
  logger("$device.displayName $cmd")

  result << createEvent(descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd, result) {
  logger("$device.displayName $cmd")
  Integer set_sceen = ((cmd.sceneId + 1) / 2) as Integer
  buttonEvent("SceneActivationSet", set_sceen, false, "digital")
  result <<  createEvent(name: "setScene", value: "Setting", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfGet cmd, result) {
  logger("$device.displayName $cmd")
  buttonEvent("SceneActuatorConfGet", cmd.sceneId, false, "digital")

  result << response(zwave.sceneActuatorConfV1.sceneActuatorConfReport(dimmingDuration: 0xFF, level: 0xFF, sceneId: cmd.sceneId))
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfReport cmd) {
  logger("$device.displayName $cmd")
  result <<  createEvent(ndescriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd) {
  logger("$device.displayName $cmd")
  result <<  createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.associationv1.AssociationGroupingsReport cmd, result) {
  logger("$device.displayName $cmd")
  def cmds = []

  if (cmd.supportedGroupings) {
    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.sceneControllerConfV1.sceneControllerConfGet(groupId: x).format()
      cmds << zwave.associationV1.associationGet(groupingIdentifier: x).format()
    }
  }

  result << createEvent(name: "numberOfButtons", value: cmd.supportedGroupings, isStateChange: true, displayed: true)
  result << response( delayBetween(cmds, 2000) )
}

def zwaveEvent(physicalgraph.zwave.commands.associationv1.AssociationReport cmd, result) {
  logger("$device.displayName $cmd")

  Integer[] associate =  []
  if (0) {
    associate +=  1 // Add SIS (assumption)
  }

  Boolean isStateChange
  String event_value
  String event_descriptionText

  String string_of_assoc
  cmd.nodeid.each {
    string_of_assoc += "${it}, "
  }

  // def lengthMinus2 = string_of_assoc.length() - 3
  // String final_string = string_of_assoc.getAt(0..lengthMinus2)
  event_value = string_of_assoc

  isStateChange = state.isAssociated ?: false

  state.isAssociated = true
  if (! cmd.nodeid.any { it == zwaveHubNodeId }) {
    isStateChange = true
    associate += zwaveHubNodeId
    state.isAssociated = false
    event_descriptionText = "Hub is not associated"
  }

  if ( associatedDevice  && ! cmd.nodeid.any { it == associatedDevice }) {
    associate += associatedDevice
    state.isAssociated = false
  }

  if (! state.isAssociated ) {
    // sendCommands( [ zwave.associationV1.associationSet(groupingIdentifier: cmd.groupingIdentifier, nodeId: associate) ] )
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

  def manufacturerCode = String.format("%04X", cmd.manufacturerId)
  def productTypeCode = String.format("%04X", cmd.productTypeId)
  def productCode = String.format("%04X", cmd.productId)
  def manufacturerName = cmd.manufacturerName ? cmd.manufacturerName : "Leviton"

  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)


  result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
  result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
  result << createEvent(name: "ProductCode", value: productCode)

  result << createEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: false)
  result << createEvent(name: "Manufacturer", value: "${manufacturerName}", descriptionText: "$device.displayName", isStateChange: false)
  result << response(zwave.versionV1.versionGet())
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(descriptionText: "$device.displayName command not implemented: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(descriptionText: "$device.displayName command not implemented: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  log.error("$device.displayName no implementation of $cmd")
  result << createEvent(descriptionText: "$device.displayName command not implemented: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd, result) {
  logger("$device.displayName command not implemented: $cmd")
  result << createEvent(name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false)
}

def zwaveEvent(physicalgraph.zwave.commands.networkmanagementprimaryv1.ControllerChangeStatus cmd, result) {
  logger("$device.displayName command not implemented: $cmd", "error")
  result << createEvent(descriptionText: "$device.displayName command not implemented: $cmd", displayed: true)
}

def prepDevice() {
  [
    zwave.associationV1.associationGroupingsGet(),
    // zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration: 0xFF, groupId:1, sceneId:1),
    // zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration: 0xFF, groupId:2, sceneId:2),
    zwave.manufacturerSpecificV1.manufacturerSpecificGet(),
    // zwave.versionV1.versionGet(),
    // zwave.associationV1.associationGet(groupingIdentifier: 0x01),
    // zwave.zwaveCmdClassV1.requestNodeInfo(),
  ]
}

def installed() {
  log.info("$device.displayName installed()")

  state.loggingLevelIDE = 4

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  // Device-Watch simply pings if no device events received for 32min(checkInterval)
  // sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])

  sendCommands( prepDevice(), 3000 )
}

def updated() {
  if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
    return
  }
  state.loggingLevelIDE = debugLevel ? debugLevel : 4
  log.info("$device.displayName updated() debug: ${state.loggingLevelIDE}")

  sendEvent(name: "lastError", value: "")
  sendEvent(name: "logMessage", value: "")

  // Check in case the device has been changed
  state.manufacturer = null
  updateDataValue("MSR", null)
  updateDataValue("Manufacturer", null)
  // sendEvent(name: "numberOfButtons", value: 1, displayed: false)

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  // Device-Watch simply pings if no device events received for 32min(checkInterval)
  // sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])

  sendCommands( prepDevice(), 3000 )

  // Avoid calling updated() twice
  state.updatedDate = Calendar.getInstance().getTimeInMillis()
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
  logger ("$device.displayName ping()")
  zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
}

def refresh () {
  logger ("$device.displayName refresh()")
  zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
}

def poll() {
  logger ("$device.displayName poll()")
  zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
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
