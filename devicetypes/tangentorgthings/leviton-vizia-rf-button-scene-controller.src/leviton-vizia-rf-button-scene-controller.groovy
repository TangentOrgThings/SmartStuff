// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :
/**
 *
 *  VIZIA RF 1 BUTTON SCENE CONTROLLER
 *  VRCS1 - 1-Button Scene Controller
 *  https://products.z-wavealliance.org/products/316
 *
 *  Leviton VRCS4-M0Z Vizia RF + 4-Button Remote Scene Controller
 *  https://products.z-wavealliance.org/products/318
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


String getDriverVersion () {
  return "v1.69"
}

metadata {
  definition (name: "Leviton Vizia RF Button Scene Controller", namespace: "TangentOrgThings", author: "Brian Aker") {
    capability "Actuator"
    capability "Button"
    capability "Sensor"
    capability "Switch"

    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message
    attribute "parseErrorCount", "number"        // Last error message
    attribute "unknownCommandErrorCount", "number"        // Last error message

    attribute "ManufacturerCode", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"
    attribute "firmwareVersion", "string"
    attribute "zWaveProtocolVersion", "string"
    attribute "driverVersion", "string"

    attribute "hail", "string"

    attribute "Scene", "number"
    attribute "setScene", "enum", ["Unknown", "Set", "Setting"]

    attribute "NIF", "string"

    command "getparamState"

    fingerprint mfr: "001D", prod: "0902", model: "0224", deviceJoinName: "Leviton VRCS1-1LZ Vizia RF + 1-Button Scene Controller"
    fingerprint mfr: "001D", prod: "0802", model: "0261", deviceJoinName: "Leviton VRCS4-M0Z Vizia RF + 4-Button Remote Scene Controller"
  }


  simulator {
    // TODO: define status and reply messages here
  }

  preferences {
    input name: "associatedDevice", type: "number", title: "Associated Device", description: "... ", required: false
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  }

  tiles {
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4) {
      tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label: '${name}', action: "off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC"
        attributeState "off", label: '${name}', icon: "st.switches.switch.off", backgroundColor: "#ffffff"
      }
    }

    valueTile("scene", "device.Scene", width: 2, height: 2, decoration: "flat", inactiveLabel: false) {
      state "default", label: '${currentValue}'
    }

    valueTile("setScene", "device.setScene", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "Set", label: '${name}', nextState: "Setting"
      state "Setting", label: '${name}' //, nextState: "Set_Scene"
    }

    valueTile("driverVersion", "device.driverVersion", width:2, height: 2, decoration: "flat", inactiveLabel: false) {
      state "default", label: '${currentValue}'
    }

    main ("switch")
    details(["switch", "scene", "setScene", "driverVersion"])
  }
}

def getCommandClassVersions() {
  [
    0x20: 1,  // Basic
    0x2D: 1,  // Scene Controller Conf
    0x72: 1,  // Manufacturer Specific
    0x73: 1,  // Powerlevel
    // 0x77: 1,  // Node Naming
    0x82: 1,  // Hail
    0x85: 2,  // Association  0x85  V1 V2
    0x86: 1,  // Version
    0x91: 1,  // Man Prop
    // Note: Controlled but not supported
    0x2B: 1,  // SceneActivation
    0x2C: 1,  // Scene Actuator Conf
    0x25: 1,  //
    0x22: 1,  // Application Status
    0x7C: 1,  // Remote Association Activate
    //    0x56: 1,  // Crc16 Encap
    //    0x25: 1,  // Switch Binary
    //    0x91: 1, // Manufacturer Proprietary
    // Stray commands that show up
    0x54: 1,  // Network Management Primary
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
      logger( "zwave.parse(CC) failed for: ${description}", "parse")

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

def on() {
  logger("$device.displayName on()")
}

def off() {
  logger("$device.displayName off()")

  logger(device.currentValue("switch"))
  if (device.currentValue("switch") == "on") {
    if (0) {
      sendEvent(name: "switch", value: "off", type: "digital", isStateChange: true, displayed: true)
    }
    response( zwave.basicV1.basicSet(value: 0x00) )
  }

  if (0) {
    unsetScene(false)
  }
}

def childOn(String childID) {
  logger("$device.displayName childOn( $childID )")

  Integer buttonId = childID?.split("/")[1] as Integer
}

def childOff(String childID) {
  logger("$device.displayName childOff( $childID )")

  Integer buttonId = childID?.split("/")[1] as Integer
}

def childLevel(String childID, Integer val) {
  logger("$device.displayName childLevel( $childID, $val )")

  Integer buttonId = childID?.split("/")[1] as Integer
}

def handleManufacturerProprietary(String description, result) {
  // log.debug "Handling manufacturer-proprietary command: '${description}'"
  logger("$device.displayName $description")
}

def buttonEvent(String exec_cmd, Integer button_pressed, Boolean isHeld) {
  logger("buttonEvent: $button_pressed  exec: $exec_cmd held: $isHeld")

  String heldType = isHeld ? "held" : "pushed"

  if (button_pressed > 0) {
    sendEvent(name: "button", value: "$heldType", data: [buttonNumber: button_pressed], descriptionText: "$device.displayName $exec_cmd button $button_pressed was pushed", isStateChange: true, type: "physical")
  } else {
    sendEvent(name: "button", value: "default", descriptionText: "$device.displayName $exec_cmd button released", isStateChange: true, type: "physical")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$device.displayName $cmd")
  if ( cmd.value == 0 ) {
    unsetScene(true)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelStartLevelChange cmd, result) {	
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelStopLevelChange cmd, result) {	
  logger("$device.displayName $cmd")
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

  if (cmd.groupId != cmd.sceneId) {
    result << response(delayBetween([
      zwave.sceneControllerConfV1.sceneControllerConfSet(groupId: cmd.groupId, sceneId: cmd.sceneId).format(),
      zwave.sceneControllerConfV1.sceneControllerConfGet(groupId: cmd.groupId).format(),
    ]))
  }

  updateDataValue("Group #${cmd.groupId} Scene", "$cmd.sceneId")
  updateDataValue("Group #${cmd.groupId} Duration", "$cmd.dimmingDuration")
}

def zwaveEvent(physicalgraph.zwave.commands.scenecontrollerconfv1.SceneControllerConfSet cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.scenecontrollerconfv1.SceneControllerConfGet cmd, result) {
  logger("$device.displayName $cmd")
  result << response(zwave.sceneActuatorConfV1.sceneActuatorConfReport(dimmingDuration: 0xFF, level: 0xFF, sceneId: cmd.sceneId))
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

    setScene( cmd.sceneId )
  }
}

def setScene( sceneId ) {
  state.Scene = sceneId

  sendEvent(name: "Scene", value: state.Scene, isStateChange: true)
  sendEvent(name: "setScene", value: "Setting", isStateChange: true)

  if (state.buttons && state.Scene >= 1 && state.Scene <= state.buttons) {
    sendEvent(name: "switch", value: "on", type: "digital", isStateChange: true, displayed: true)
    buttonEvent("setScene", state.Scene, true)
  } else if (state.buttons && state.Scene > state.buttons && state.Scene <= state.buttons * 2) {
    sendEvent(name: "switch", value: "off", type: "digital", isStateChange: true, displayed: true)
    buttonEvent("setScene", state.Scene - state.buttons, false)
  } else {
    buttonEvent("setScene", state.Scene, true)
  }
}

def unsetScene(Boolean isPhysical) {
  if ( state.Scene ) {
    if (state.buttons && state.Scene >= 1 && state.Scene <= state.buttons) {
      if (isPhysical) {
        buttonEvent("unsetScene", state.Scene, false)
      }
      sendEvent(name: "switch", value: "off", type: isPhysical ? "physical" : "digital", isStateChange: true, displayed: true)
    } else if (device.currentValue("switch") == "on") {
      sendEvent(name: "switch", value: "off", type: isPhysical ? "physical" : "digital", isStateChange: true, displayed: true)
    }

    state.Scene = 0
    sendEvent(name: "Scene", value: state.Scene, isStateChange: true)
  } else if (device.currentValue("switch") == "on") {
    sendEvent(name: "switch", value: "off", type: isPhysical ? "physical" : "digital", isStateChange: true, displayed: true)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfGet cmd, result) {
  logger("$device.displayName $cmd")
  logger("$device.displayName lastScene: $state.lastScene")

  result << createEvent(name: "setScene", value: "Set", isStateChange: true, displayed: true)
  result << response(zwave.sceneActuatorConfV1.sceneActuatorConfReport(
    dimmingDuration: 0xFF, 
    level: 0xFF, 
    sceneId: cmd.sceneId == 0 ? state.lastScene : cmd.sceneId
  ))
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd, result) {
  logger("$device.displayName $cmd")
  def cmds = []

  if (cmd.supportedGroupings) {
    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationV1.associationGet(groupingIdentifier: x).format();
    }
  }

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
  String final_string = lengthMinus2 ? string_of_assoc.getAt(0..lengthMinus2) : string_of_assoc

  updateDataValue("Association Group #${cmd.groupingIdentifier}", "${final_string}")

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

  String group_association_name =  "Group ${cmd.groupingIdentifier}"
  updateDataValue("$group_association_name", "${event_value}");
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

  def cmds = []

  String manufacturerCode = String.format("%04X", cmd.manufacturerId)
  String productTypeCode = String.format("%04X", cmd.productTypeId)
  String productCode = String.format("%04X", cmd.productId)
  String manufacturerName = cmd.manufacturerName ? cmd.manufacturerName : "Leviton"
  updateDataValue("manufacturer", manufacturerName)

  String msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)

  state.buttons = 0
  if (msr == "001D-0902-0224") { // VRCS1
    state.buttons = 1

    sendEvent(name: "numberOfButtons", value: state.buttons, isStateChange: true, displayed: false)

    for (def x = 1; x <= state.buttons * 2; x++) {
      cmds << zwave.sceneControllerConfV1.sceneControllerConfGet(groupId: x).format()
    }
  } else if (msr == "001D-0802-0261") { // VRCS4
    state.buttons = 4

    sendEvent(name: "numberOfButtons", value: state.buttons, isStateChange: true, displayed: false)

    for (def x = 1; x <= state.buttons * 2; x++) {
      cmds << zwave.sceneControllerConfV1.sceneControllerConfGet(groupId: x).format()
    }
  }

  if (0 && ! childDevices) {
    createChildDevices(state.buttons)
  }

  if ( cmds.size ) {
    result << response(delayBetween(cmds, 1000))
  }

  result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
  result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
  result << createEvent(name: "ProductCode", value: productCode)

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

private void createChildDevices(Integer numberOfSwitches) {
  // Save the device label for updates by updated()
  state.oldLabel = device.label

  // Add child devices for four button presses
  for ( Integer x in 1..numberOfSwitches ) {
    def childDevice = addChildDevice(
      "smartthings",
      "Child Switch",
      "${device.deviceNetworkId}/$x",
      "",
      [
      label         : "$device.displayName Switch $x",
      completedSetup: true,
      isComponent: true,
      ]);

    childDevice.ignoreDigital()
  }
}

def prepDevice() {
  [
    zwave.powerlevelV1.powerlevelSet(powerLevel: 0, timeout: 0),
    zwave.associationV1.associationGroupingsGet(),
    // zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration: 0xFF, groupId:1, sceneId:1),
    // zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration: 0xFF, groupId:2, sceneId:2),
    zwave.manufacturerSpecificV1.manufacturerSpecificGet(),
    // zwave.remoteAssociationActivateV1.remoteAssociationActivate(groupingIdentifier:1),
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

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  // Device-Watch simply pings if no device events received for 32min(checkInterval)
  // sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])

  sendCommands( prepDevice(), 3000 )
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

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  if ( childDevices ) {
    childDevices.each { logger("${it.deviceNetworkId}") }
  }

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
