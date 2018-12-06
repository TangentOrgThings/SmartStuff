// vim :set ts=2 sw=2 sts=2 expandtab smarttab :
/**
 *  ZWN-SC7 Enerwave 7 Button Scene Controller
 *
 *  Copyright 2017-2018 Brian Aker <brian@tangent.org>
 *
 *  Author: Matt Frank based on VRCS Button Controller by Brian Dahlem, based on SmartThings Button Controller
 *  Date Created: 2014-12-18
 *  Last Updated: 2015-02-13
 *  Updated: 2016-08-15 https://github.com/ady624/ZWN-SC7-Enerwave-7-Button-Scene-Controller
 *  Updated: 2016-09-17 https://github.com/AlohaHausThings/ZWN-SC7-Enerwave-7-Button-Scene-Controller
 *    * Fixed fingerprint for device.
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

def getDriverVersion() {
  return "v0.47"
}

metadata {
  definition (name: "Enerwave ZWNSC7 Scene Controller", namespace: "TangentOrgThings", author: "Brian Aker") {
    capability "Actuator"
    capability "Button"
    capability "Configuration"
    capability "Sensor"
    capability "Switch"

    attribute "currentButton", "STRING"

    attribute "Manufacturer", "string"
    attribute "ManufacturerCode", "string"
    attribute "MSR", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"

    attribute "Group 1", "string"
    attribute "Group 2", "string"
    attribute "Group 3", "string"
    attribute "Group 4", "string"
    attribute "Group 5", "string"
    attribute "Group 6", "string"
    attribute "Group 7", "string"

    attribute "Group Scene 1", "string"
    attribute "Group Scene 2", "string"
    attribute "Group Scene 3", "string"
    attribute "Group Scene 4", "string"
    attribute "Group Scene 5", "string"
    attribute "Group Scene 6", "string"
    attribute "Group Scene 7", "string"

    attribute "Scene", "number"
    attribute "setScene", "enum", ["Set", "Setting"]

    attribute "firmwareVersion", "string"
    attribute "zWaveProtocolVersion", "string"

    attribute "driverVersion", "string"

    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message
    attribute "parseErrorCount", "number"        // Last error message
    attribute "unknownCommandErrorCount", "number"        // Last error message

    // zw:L type:0202 mfr:011A prod:0801 model:0B03 ver:1.05 zwv:3.42 lib:02 cc:2D,85,86,72
    // fingerprint deviceId: "0x0202", inClusters:"0x21, 0x2D, 0x85, 0x86, 0x72"
    // fingerprint deviceId: "0x0202", inClusters:"0x2D, 0x85, 0x86, 0x72"
    fingerprint type:"0202", mfr: "011A", model: "0B03", prod: "0801", cc: "2D, 85, 86, 72", deviceJoinName: "ZWN-SC7 Enerwave 7 Button Scene Controller"
    fingerprint type:"0202", mfr: "011A", model: "0B03", prod: "0803", cc: "2D, 85, 86, 72", deviceJoinName: "ZWN-SC7 Enerwave 7 Button Scene Controller"
  }

  simulator {
    status "button 1 pushed":  "command: 2B01, payload: 01 FF"
    status "button 2 pushed":  "command: 2B01, payload: 02 FF"
    status "button 3 pushed":  "command: 2B01, payload: 03 FF"
    status "button 4 pushed":  "command: 2B01, payload: 04 FF"
    status "button 5 pushed":  "command: 2B01, payload: 05 FF"
    status "button 6 pushed":  "command: 2B01, payload: 06 FF"
    status "button 7 pushed":  "command: 2B01, payload: 07 FF"
    status "button released":  "command: 2C02, payload: 00"
  }

  preferences {
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  }

  tiles {
    standardTile("button", "device.button", width: 2, height: 2) {
      state "default", label: " ", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffffff"
      state "button 1", label: "1", icon: "st.Weather.weather14", backgroundColor: "#79b821"
      state "button 2", label: "2", icon: "st.Weather.weather14", backgroundColor: "#79b821"
      state "button 3", label: "3", icon: "st.Weather.weather14", backgroundColor: "#79b821"
      state "button 4", label: "4", icon: "st.Weather.weather14", backgroundColor: "#79b821"
      state "button 5", label: "5", icon: "st.Weather.weather14", backgroundColor: "#79b821"
      state "button 6", label: "6", icon: "st.Weather.weather14", backgroundColor: "#79b821"
      state "button 7", label: "7", icon: "st.Weather.weather14", backgroundColor: "#79b821"
    }

    valueTile("scene", "device.Scene", width: 2, height: 2, decoration: "flat", inactiveLabel: false) {
      state "default", label: '${currentValue}'
    }

    valueTile("setScene", "device.setScene", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "Set", label: '${name}', nextState: "Setting"
      state "Setting", label: '${name}' //, nextState: "Set_Scene"
    }

    main "button"
    details (["button", "scene", "setScene"])
  }
}

def getCommandClassVersions() {
  [
    0x20: 1,  // Basic
    0x2D: 1,  // Scene Controller Conf
    0x72: 1,  // Manufacturer Specific
    0x85: 1,  // Association  0x85  V1 V2
    0x86: 1,  // Version
    // Controlled
    0x2B: 1,  // Scene Activation
  ]
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
    logger("$device.displayName parse() called with NULL description", "info")
  } else if (description != "updated") {
    def cmd = zwave.parse(description, getCommandClassVersions())

    if (cmd) {
      zwaveEvent(cmd, result)

    } else {
      logger( "zwave.parse(getCommandClassVersions()) failed for: ${description}", "parse" )
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

// Handle a button being pressed
def buttonEvent(String exec_cmd, Integer button, Boolean held, Boolean buttonType, result) {

  if (button > 0) {
    // update the device state, recording the button press
    result << createEvent(name: "button", value: "pushed", data: [buttonNumber: button], descriptionText: "$device.displayName button $button was pushed", isStateChange: true)

    // turn off the button LED
    result << response(zwave.sceneActuatorConfV1.sceneActuatorConfReport(dimmingDuration: 255, level: 255, sceneId: 0))
  }
  else {
    // update the device state, recording the button press
    result << createEvent(name: "button", value: "default", descriptionText: "$device.displayName button was released", isStateChange: true)

    result << response(zwave.sceneActuatorConfV1.sceneActuatorConfReport(dimmingDuration: 255, level: 255, sceneId: 0))
  }
}

def zwaveEvent(zwave.commands.basicv1.BasicGet cmd, result) {
  logger("$cmd")

  def currentValue = device.currentState("switch").value.equals("on") ? 255 : 0
  result << zwave.basicV1.basicReport(value: currentValue).format()
}

def zwaveEvent(zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$cmd")

  Short value = cmd.value

  if (value == 0) {
    result << createEvent(name: "switch", value: "off", isStateChange: true, displayed: true)
    if (device.displayName.endsWith("Dimmer")) {
      result << createEvent(name: "level", value: 0, isStateChange: true, displayed: true)
    }
  } else if (value < 100 || value == 255) {
    result << createEvent(name: "switch", value: "on", isStateChange: true, displayed: true)
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
    result << createEvent(name: "switch", value: "off", isStateChange: true, displayed: true)
    if (device.displayName.endsWith("Dimmer")) {
      result << createEvent(name: "level", value: 0, isStateChange: true, displayed: true)
    }
  } else if (value < 100 || value == 255) {
    result << createEvent(name: "switch", value: "on", isStateChange: true, displayed: true)
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

// A zwave command for a button press was received
def zwaveEvent(zwave.commands.sceneactivationv1.SceneActivationSet cmd, result) {
  logger("$device.displayName $cmd")

  // The controller likes to repeat the command... ignore repeats
  if (state.lastScene == cmd.sceneId && (state.repeatCount < 4) && (now() - state.repeatStart < 2000)) {
    log.debug "Button ${cmd.sceneId} repeat ${state.repeatCount}x ${now()}"
    state.repeatCount = state.repeatCount + 1
    createEvent([:])
  }
  else {
    // If the button was really pressed, store the new scene and handle the button press
    state.lastScene = cmd.sceneId
    state.lastLevel = 0
    state.repeatCount = 0
    state.repeatStart = now()

    buttonEvent("SceneActivationSet", cmd.sceneId, false, true, result)
    result <<  createEvent(name: "Scene", value: "${cmd.sceneId}", isStateChange: true, displayed: true)
    result <<  createEvent(name: "setScene", value: "Setting", isStateChange: true, displayed: true)
  }
}

// A scene command was received -- it's probably scene 0, so treat it like a button release
def zwaveEvent(zwave.commands.sceneactuatorconfv1.SceneActuatorConfGet cmd, result) {
  logger("$device.displayName $cmd")

  buttonEvent("SceneActuatorConfGet", cmd.sceneId, false, true, result)
  result <<  createEvent(name: "setScene", value: "Set", isStateChange: true, displayed: true)

  result << response(zwave.sceneActuatorConfV1.sceneActuatorConfReport(dimmingDuration: 0xFF, level: 0xFF, sceneId: cmd.sceneId))
}

/*
def zwaveEvent(zwave.commands.sceneactuatorconfv1.SceneActuatorConfReport cmd, result) {
  logger("$device.displayName $cmd")
  result <<  createEvent(ndescriptionText: "$cmd", isStateChange: true, displayed: true)
}
*/

// Update manufacturer information when it is reported
def zwaveEvent(zwave.commands.manufacturerspecificv1.ManufacturerSpecificReport cmd, result) {
  logger("$device.displayName $cmd")

  if ( cmd.manufacturerName ) {
    state.manufacturer= cmd.manufacturerName
  } else {
    state.manufacturer= "Enerwave"
  }

  def manufacturerCode = String.format("%04X", cmd.manufacturerId)
  def productTypeCode = String.format("%04X", cmd.productTypeId)
  def productCode = String.format("%04X", cmd.productId)

  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)
  updateDataValue("manufacturer", "${state.manufacturer}")

  result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
  result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
  result << createEvent(name: "ProductCode", value: productCode)
  result << createEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: false)
  result << createEvent(name: "Manufacturer", value: "${state.manufacturer}", descriptionText: "$device.displayName", isStateChange: false)
  result << response(zwave.versionV1.versionGet())
}

def zwaveEvent(zwave.commands.versionv1.VersionReport cmd, result) {
  logger("$device.displayName $cmd")

  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  def zWaveProtocolVersion = "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  result << createEvent(name: "firmwareVersion", value: "v${state.firmwareVersion}", descriptionText: "$text", isStateChange: true)
  result << createEvent(name: "zWaveProtocolVersion", value: "${zWaveProtocolVersion}", descriptionText: "${device.displayName} ${zWaveProtocolVersion}", isStateChange: true)
}

// Association Groupings Reports tell us how many groupings the device supports.  This equates to the number of
// buttons/scenes in the VRCS
def zwaveEvent(zwave.commands.associationv1.AssociationGroupingsReport cmd, result) {
  logger("$device.displayName $cmd")

  sendEvent(name: "numberOfButtons", value: cmd.supportedGroupings, isStateChange: true, displayed: true)

  if (cmd.supportedGroupings) {
    def cmds = []
    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationV1.associationGet(groupingIdentifier: x).format();
      cmds << zwave.sceneControllerConfV1.sceneControllerConfGet(groupId: x).format();
    }

    result << response(delayBetween(cmds))
  } else {
    result << createEvent(descriptionText: "$device.displayName reported no groups", isStateChange: true, displayed: true)
  }
}

def zwaveEvent(zwave.commands.associationv1.AssociationReport cmd, result) {
  logger("$device.displayName $cmd")

	String final_string = ""
	if (cmd.nodeid) {
		final_string = cmd.nodeid.join(",")
	}

  Boolean isAssociated = false
  if (cmd.nodeid.any { it == zwaveHubNodeId }) {
    isAssociated = true
  }
  result << createEvent(name: "Group ${cmd.groupingIdentifier}", value:  "${final_string}", isStateChange: true, displayed: true);

  switch (cmd.groupingIdentifier) {
    case 1:
    state.Associated_01 = "${final_string}"
    result << createEvent(name: "Group 1", value:  "${final_string}", isStateChange: true, displayed: true);
    break

    case 2:
    state.Associated_02 = "${final_string}"
    result << createEvent(name: "Group 2", value:  "${final_string}", isStateChange: true, displayed: true);
    break

    case 3:
    state.Associated_03 = "${final_string}"
    result << createEvent(name: "Group 3", value:  "${final_string}", isStateChange: true, displayed: true);
    break

    case 4:
    state.Associated_04 = "${final_string}"
    result << createEvent(name: "Group 4", value:  "${final_string}", isStateChange: true, displayed: true);
    break

    case 5:
    state.Associated_05 = "${final_string}"
    result << createEvent(name: "Group 5", value:  "${final_string}", isStateChange: true, displayed: true);
    break

    case 6:
    state.Associated_06 = "${final_string}"
    result << createEvent(name: "Group 6", value:  "${final_string}", isStateChange: true, displayed: true);
    break

    case 7:
    state.Associated_07 = "${final_string}"
    result << createEvent(name: "Group 7", value:  "${final_string}", isStateChange: true, displayed: true);
    break
  }

  if (isAssociated == false) {
    result << response(zwave.associationV1.associationSet(groupingIdentifier: cmd.groupingIdentifier, nodeId: zwaveHubNodeId))
  }
}

def zwaveEvent(zwave.commands.scenecontrollerconfv1.SceneControllerConfReport cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(descriptionText: "$cmd", isStateChange: true, displayed: true)
  result << createEvent(name: "Group Scene ${cmd.groupId}", value:  "${cmd.sceneId}", isStateChange: true, displayed: true);

  if (cmd.sceneId && cmd.groupId) {
    result <<  createEvent(name: "Scene", value: "${cmd.sceneId}", isStateChange: true, displayed: true)
  }
}

def zwaveEvent(zwave.commands.scenecontrollerconfv1.SceneControllerConfSet cmd, result) {
  logger("$device.displayName $cmd")

  result << createEvent(descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(zwave.commands.applicationstatusv1.ApplicationBusy cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(zwave.commands.zwavecmdclassv1.NodeInfo cmd, result) {
  logger("$device.displayName $cmd")
  result <<  createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(zwave.Command cmd, result) {
  logger("$device.displayName command not implemented: $cmd", "error")
  result << createEvent(descriptionText: "$device.displayName command not implemented: $cmd", displayed: true)
}

// handle commands

// Create a list of the configuration commands to send to the device
def configurationCmds() {
  // Always check the manufacturer and the number of groupings allowed
  def cmds = [
    zwave.manufacturerSpecificV1.manufacturerSpecificGet(),
    zwave.associationV1.associationGroupingsGet(),
    zwave.sceneControllerConfV1.sceneControllerConfSet(groupId:1, sceneId:1),
    zwave.sceneControllerConfV1.sceneControllerConfSet(groupId:2, sceneId:2),
    zwave.sceneControllerConfV1.sceneControllerConfSet(groupId:3, sceneId:3),
    zwave.sceneControllerConfV1.sceneControllerConfSet(groupId:4, sceneId:4),
    zwave.sceneControllerConfV1.sceneControllerConfSet(groupId:5, sceneId:5),
    zwave.sceneControllerConfV1.sceneControllerConfSet(groupId:6, sceneId:6),
    zwave.sceneControllerConfV1.sceneControllerConfSet(groupId:7, sceneId:7),
  ]

  cmds << associateHub()

  delayBetween(cmds)
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
  logger ("$device.displayName ping()")
  delayBetween([
    zwave.sceneControllerConfV1.sceneControllerConfGet(groupId: 0).format(),
  ])
}

def refresh () {
  logger ("$device.displayName refresh()")
  delayBetween([
    zwave.sceneControllerConfV1.sceneControllerConfGet(groupId: 0).format(),
  ])
}

def poll() {
  logger ("$device.displayName poll()")
  response( delayBetween([
    zwave.sceneControllerConfV1.sceneControllerConfGet(groupId: 0).format(),
  ]))
}

def prepDevice() {
  [
    zwave.manufacturerSpecificV1.manufacturerSpecificGet(),
    zwave.sceneControllerConfV1.sceneControllerConfSet(groupId:1, sceneId:1),
    zwave.sceneControllerConfV1.sceneControllerConfSet(groupId:2, sceneId:2),
    zwave.sceneControllerConfV1.sceneControllerConfSet(groupId:3, sceneId:3),
    zwave.sceneControllerConfV1.sceneControllerConfSet(groupId:4, sceneId:4),
    zwave.sceneControllerConfV1.sceneControllerConfSet(groupId:5, sceneId:5),
    zwave.sceneControllerConfV1.sceneControllerConfSet(groupId:6, sceneId:6),
    zwave.sceneControllerConfV1.sceneControllerConfSet(groupId:7, sceneId:7),
    zwave.associationV1.associationSet(groupingIdentifier: 1, nodeId: zwaveHubNodeId),
    zwave.associationV1.associationSet(groupingIdentifier: 2, nodeId: zwaveHubNodeId),
    zwave.associationV1.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId),
    zwave.associationV1.associationSet(groupingIdentifier: 4, nodeId: zwaveHubNodeId),
    zwave.associationV1.associationSet(groupingIdentifier: 5, nodeId: zwaveHubNodeId),
    zwave.associationV1.associationSet(groupingIdentifier: 6, nodeId: zwaveHubNodeId),
    zwave.associationV1.associationSet(groupingIdentifier: 7, nodeId: zwaveHubNodeId),
    zwave.associationV1.associationGroupingsGet(),
    zwave.sceneControllerConfV1.sceneControllerConfGet(groupId: 0),
    zwave.zwaveCmdClassV1.requestNodeInfo(),
  ]
}

def updated() {
  if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
    return
  }
  state.loggingLevelIDE = debugLevel ? debugLevel : 4
  log.info("$device.displayName updated() debug: ${state.loggingLevelIDE}")

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)
  sendEvent(name: "parseErrorCount", value: 0, displayed: false)
  sendEvent(name: "unknownCommandErrorCount", value: 0, displayed: false)
  state.parseErrorCount = 0
  state.unknownCommandErrorCount = 0

  // Check in case the device has been changed
  state.manufacturer = null
  updateDataValue("MSR", null)
  updateDataValue("Manufacturer", null)
  sendEvent(name: "numberOfButtons", value: 7, displayed: false)

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
  
  sendCommands( prepDevice(), 1000 )

  // Avoid calling updated() twice
  state.updatedDate = Calendar.getInstance().getTimeInMillis()
}

def installed() {
  log.info("$device.displayName installed()")

  state.loggingLevelIDE = 4

  sendEvent(name: "numberOfButtons", value: 7, displayed: false)
  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed:true)

  sendCommands( prepDevice(), 1000 )
}

//
// Associate the hub with the buttons on the device, so we will get status updates
def associateHub() {
  def cmds = []

  // Loop through all the buttons on the controller
  for (def buttonNum = 1; buttonNum <= integer(getDataByName("numButtons")); buttonNum++) {

    // Associate the hub with the button so we will get status updates
    cmds << zwave.associationV1.associationSet(groupingIdentifier: buttonNum, nodeId: zwaveHubNodeId)

  }

  return cmds
}

// Update State
// Store mode and settings
def updateState(String name, String value) {
  state[name] = value
  device.updateDataValue(name, value)
}

// Get Data By Name
// Given the name of a setting/attribute, lookup the setting's value
def getDataByName(String name) {
  state[name] ?: device.getDataValue(name)
}

//Stupid conversions

// convert a double to an integer
def integer(double v) {
  return v.toInteger()
}

// convert a hex string to integer
def integerhex(String v) {
  if (v == null) {
    return 0
  }

  return Integer.parseInt(v, 16)
}

// convert a hex string to integer
def integer(String v) {
  if (v == null) {
    return 0
  }

  return Integer.parseInt(v)
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

private logger(msg, level = "trace") {
  String device_name = "$device.displayName"
  String msg_text = (msg != null) ? "$msg" : "<null>"

  Integer log_level = state.defaultLogLevel ?: settings.debugLevel

  switch(level) {
    case "warn":
    if (log_level >= 2) {
      log.warn "$device_name ${msg_txt}"
    }
    sendEvent(name: "logMessage", value: "${msg_txt}", displayed: false, isStateChange: true)
    break;

    case "info":
    if (log_level >= 3) {
      log.info "$device_name ${msg_txt}"
    }
    break;

    case "debug":
    if (log_level >= 4) {
      log.debug "$device_name ${msg_txt}"
    }
    break;

    case "trace":
    if (log_level >= 5) {
      log.trace "$device_name ${msg_txt}"
    }
    break;

    case "error":
    default:
    log.error "$device_name ${msg_txt}"
    sendEvent(name: "lastError", value: "${msg}", displayed: false, isStateChange: true)
    break;
  }
}
