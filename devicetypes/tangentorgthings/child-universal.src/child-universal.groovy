// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :
/**
 *  Child Universal
 *
 *  Copyright 2018-2019 Brian Aker <brian@tangent.org>
 *
 *  For device parameter information and images, questions or to provide feedback on this device handler,
 *  please visit:
 *
 *      github.com/TangentOrgThings/
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
 *  Author: Brian Aker <brian@tangent.org>
 *  Date: 2018
 *
 *  Changelog:
 *
 *
 *
 *
 */

String getDriverVersion () {
  return "v1.03"
}

def getConfigurationOptions(Integer model) {
  return [ ]
}

metadata {
  definition (name: "Child Universal", namespace: "TangentOrgThings", author: "brian@tangent.org", ocfDeviceType: "oic.d.switch") {
    capability "Actuator"
    capability "Sensor"
    capability "Switch"

    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message
    attribute "parseErrorCount", "number"        // Last error message
    attribute "unknownCommandErrorCount", "number"        // Last error message

    attribute "driverVersion", "string"

    attribute "firmwareVersion", "string"

    attribute "Scene", "number"
    attribute "keyAttributes", "number"

    attribute "SwitchAll", "string"
    attribute "Power", "string"
    
    command "basicOn"
    command "basicOff"
  }

  // simulator metadata
  simulator {
  }

  preferences {
    input name: "disbableDigitalOff", type: "bool", title: "Disable Digital Off", description: "Disallow digital turn off", required: false
    input name: "delayOff", type: "bool", title: "Delay Off", description: "Delay Off for three seconds", required: false
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false,  defaultValue: 3
  }

  tiles(scale: 2) {
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00a0dc", nextState: "turningOff"
				attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
				attributeState "turningOn", label: '${name}', icon: "st.switches.switch.on", backgroundColor: "#00a0dc", nextState: "turningOff"
				attributeState "turningOff", label: '${name}', icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
			}
    }

    valueTile("scene", "device.Scene", width: 2, height: 2, decoration: "flat") {
      state "default", label: '${currentValue}', defaultState: true
    }

    standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "default", label:'', action:"refresh.refresh", icon: "st.secondary.refresh"
    }

    valueTile("driverVersion", "device.driverVersion", width: 2, height: 2, decoration: "flat") {
      state "default", label: '${currentValue}', defaultState: true
    }

    standardTile("basicOn", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "on", label:'test on', action:"basicOn", icon: "st.switches.switch.on"
    }

    standardTile("basicOff", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "off", label:'test off', action:"basicOff", icon: "st.switches.switch.off"
    }

    main "switch"
    details(["switch", "scene", "driverVersion", "refresh", "basicOn", "basicOff"])
  }
}

def getCommandClassVersions() {
  [
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
  logger("$device.displayName $cmd")
  switchEvents(cmd.value, true, result);
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$device.displayName $cmd -- BEING CONTROLLED")
  result << response(zwave.switchBinaryV1.switchBinaryGet())
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

  result << response(delayBetween([
    zwave.basicV1.switchBinaryReport(value: device.currentValue("switch")).format(),
  ]))
}

// These will show up from time to time, handle them as control
def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd, result) {
  logger("$device.displayName $cmd -- BEING CONTROLLED")
  if (cmd.sensorValue) {
    on()
    return
  }

  off()
}

def buttonEvent(String exec_cmd, Integer button, Boolean held, buttonType = "physical") {
  logger("buttonEvent: $button  held: $held  type: $buttonType")

  String heldType = held ? "held" : "pushed"

  if (button > 0) {
    sendEvent(name: "button", value: "$heldType", data: [buttonNumber: button], descriptionText: "$device.displayName $exec_cmd button $button was pushed", isStateChange: true, type: "$buttonType")
  } else {
    sendEvent(name: "button", value: "default", descriptionText: "$device.displayName $exec_cmd button released", isStateChange: true, type: "$buttonType")
  }
}

// A scene command was received -- it's probably scene 0, so treat it like a button release
def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfGet cmd, result) {
  logger("$device.displayName $cmd")
  buttonEvent("SceneActuatorConfGet()", cmd.sceneId, false, "digital")
  result << response(zwave.sceneActuatorConfV1.sceneActuatorConfReport(dimmingDuration: 0xFF, level: 0xFF, sceneId: cmd.sceneId))
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfReport cmd, result) {
  logger("$device.displayName $cmd")
  updateDataValue("Scene #${cmd.sceneId}", "Level: ${cmd.level} Dimming Duration: ${cmd.dimmingDuration}")
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "Scene", value: "${cmd.sceneId}", isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd, result) {
  logger("$device.displayName $cmd")
  updateDataValue("Configuration #${cmd.parameterNumber}", "${cmd.scaledConfigurationValue}")
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

  String text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}"
  result << createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationBusy cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationRejectedRequest cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.applicationcapabilityv1.CommandCommandClassNotSupported cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  logger("$device.displayName command not implemented: $cmd", "warn")
}

def basicOn() {
  response(zwave.basicV1.basicSet(value: 0xFF))
}

def basicOff() {
  response(zwave.basicV1.basicSet(value: 0x00))
}

def on() {
  logger("$device.displayName on()")

  if (1) { // Add option to have digital commands execute buttons
    buttonEvent("on()", 1, false, "digital")
  }

  delayBetween([
    zwave.basicV1.basicSet(value: 0xFF).format(),
    zwave.switchBinaryV1.switchBinaryGet().format(),
  ], 2000)
}

def off() {
  logger("$device.displayName off()")

  if (1) { // Add option to have digital commands execute buttons
    buttonEvent("off()", 2, false, "digital")
  }

  if (settings.disbableDigitalOff) {
    logger("..off() disabled")
    return zwave.switchBinaryV1.switchBinaryGet().format();
  }

  def cmds = []
  if (settings.delayOff) {
    cmds << "delay 3000";
  }

  // cmds << zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0xff, sceneId: 2).format();
  // cmds << physical ? zwave.basicV1.basicSet(value: 0x00).format() : zwave.switchBinaryV1.switchBinarySet(switchValue: 0x00).format();
  cmds << zwave.basicV1.basicSet(value: 0x00).format()
  cmds << "delay 1000"
  cmds << zwave.switchBinaryV1.switchBinaryGet().format()

  delayBetween( cmds )
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
  logger "ping()"
  zwave.switchBinaryV1.switchBinaryGet().format()
}

def refresh() {
  logger("refresh()")
  
  delayBetween([
    zwave.switchBinaryV1.switchBinaryGet().format(),
    zwave.manufacturerSpecificV1.manufacturerSpecificGet().format(),
  ])
}

def poll() {
  logger("poll()")

  if (0) {
    zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: 0x00).format()
  }

  zwave.switchBinaryV1.switchBinaryGet().format()
}

def zwaveEvent(physicalgraph.zwave.commands.centralscenev1.CentralSceneSupportedReport cmd, result) {
  logger("$device.displayName $cmd")

  def cmds = []

  for (def x = 1; x <= cmd.supportedScenes; x++) {
    cmds << zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: x).format()
  }
  cmds << zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: integerHex(device.deviceNetworkId)).format()

  if (cmds.size) {
    result << response(delayBetween(cmds))
  }
}

def zwaveEvent(physicalgraph.zwave.commands.centralscenev1.CentralSceneNotification cmd, result) {
  log.debug("$device.displayName $cmd")

  if (0) {
    if ( cmd.sequenceNumber > 1 && cmd.sequenceNumber < state.sequenceNumber ) {
      logger(descriptionText: "Late sequenceNumber  ${state.sequenceNumber} < $cmd", "info")
      return
    }
    state.sequenceNumber= cmd.sequenceNumber
  }

  def cmds = []
  
  switch (cmd.sceneNumber) {
    case 1:
    // Up
    switch (cmd.keyAttributes) {
      case 2:
      case 0:
      result << createEvent(name: "switch", value: "on", type: "physical", isStateChange: true, displayed: true)
      buttonEvent("CentralSceneNotification()", cmd.sceneNumber, cmd.keyAttributes == 0 ? false : true, "physical")
      case 1:
      break;
      case 3:
      // 2 Times
      buttonEvent("CentralSceneNotification()", 3, false, "physical")
      break;
      case 4:
      // 3 Three times
      buttonEvent("CentralSceneNotification()", 5, false, "physical")
      break;
      default:
      logger("unexpected up press keyAttribute: $cmd", "error")
    }
    break

    case 2:
    // Down
    switch (cmd.keyAttributes) {
      case 2:
      case 0:
      result << createEvent(name: "switch", value: "off", type: "physical", isStateChange: true, displayed: true)
      buttonEvent("CentralSceneNotification()", cmd.sceneNumber, cmd.keyAttributes == 0 ? false : true, "physical")
      case 1:
      break;
      case 3:
      // 2 Times
      buttonEvent("CentralSceneNotification()", 4, false, "physical")
      break;
      case 4:
      // 3 Three times
      buttonEvent("CentralSceneNotification()", 6, false, "physical")
      break;
      default:
      logger("unexpected down press keyAttribute: $cmd", "error")
    }
    break

    default:
    // unexpected case
    logger("unexpected scene: $cmd.sceneNumber", "error")
  }

  if (cmds.size) {
    result << response(delayBetween(cmds))
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
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd, result) {
  logger("$device.displayName $cmd")

  def name = new String(cmd.name as byte[])
  logger("Association Group #${cmd.groupingIdentifier} has name: ${name}", "info")

  updateDataValue("Group #${cmd.groupingIdentifier}", "${name}")

  result << response(delayBetween([
    zwave.associationV1.associationGet(groupingIdentifier: cmd.groupingIdentifier).format(),
  ]))
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupCommandListReport cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$device.displayName $cmd")

  String nodes = ""
  
  if (cmd.nodeId) {
    nodes = cmd.nodeId.join(",")
  }

  updateDataValue("Group #${cmd.groupingIdentifier}", "$nodes")
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
  }
}


def prepDevice() {
  [
  ]
}

def installed() {
  logger("$device.displayName installed()")

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
  sendEvent(name: "Scene", value: 0, isStateChange: true)

  sendCommands( prepDevice(), 2000 )
  response(refresh())
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

  sendEvent(name: "Scene", value: 0, isStateChange: true)

  // Device-Watch simply pings if no device events received for 32min(checkInterval)
  // sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID]) //, offlinePingable: "1"])

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  sendCommands( prepDevice(), 2000 )
  response(refresh())

  // Avoid calling updated() twice
  state.updatedDate = Calendar.getInstance().getTimeInMillis()
}

/*****************************************************************************************************************
 *  Private Helper Functions:
 *****************************************************************************************************************/

// convert a hex string to integer 
def integerHex(String v) { 
  if (v == null) { 
    return 0 
  } 

  return Integer.parseInt(v, 16) 
} 

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
    case "parse":
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
