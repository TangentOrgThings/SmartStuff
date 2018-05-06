// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :
/**
 *  WS-100+ Dragon Tech Industrial, Ltd.
 *
 *  Copyright 2017-2018 Brian Aker <brian@tangent.org>, DarwinsDen.com
 *
 *  For device parameter information and images, questions or to provide feedback on this device handler,
 *  please visit:
 *
 *      github.com/TangentOrgThings/ws100plus/
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
 *  Date: 2017
 *
 *  Changelog:
 *
 *
 *
 *
 */

def getDriverVersion () {
  return "v6.24"
}

metadata {
  definition (name: "WS-100 Switch", namespace: "TangentOrgThings", author: "brian@tangent.org", ocfDeviceType: "oic.d.switch") {
    capability "Actuator"
    // capability "Health Check"
    capability "Button"
    capability "Indicator"
    capability "Light"
    capability "Polling"
    capability "Refresh"
    capability "Sensor"
    capability "Switch"

    attribute "DeviceReset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message

    attribute "invertedState", "enum", ["false", "true"]

    attribute "Lifeline", "string"
    attribute "configured", "enum", ["false", "true"]
    attribute "driverVersion", "string"
    attribute "firmwareVersion", "string"
    attribute "FirmwareMdReport", "string"
    attribute "Manufacturer", "string"
    attribute "ManufacturerCode", "string"
    attribute "MSR", "string"
    attribute "NIF", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"

    attribute "setScene", "enum", ["Set", "Setting"]
    attribute "keyAttributes", "number"

    attribute "Scene", "number"
    attribute "Scene_1", "number"
    attribute "Scene_1_Duration", "number"
    attribute "Scene_2", "number"
    attribute "Scene_2_Duration", "number"

    attribute "SwitchAll", "string"

    // zw:L type:1001 mfr:000C prod:4447 model:3033 ver:5.14 zwv:4.05 lib:03 cc:5E,86,72,5A,85,59,73,25,27,70,2C,2B,5B,7A ccOut:5B role:05 ff:8700 ui:8700
    fingerprint type: "1001", mfr: "0184", prod: "4447", model: "3033", deviceJoinName: "WS-100" // cc: "5E, 86, 72, 5A, 85, 59, 73, 25, 27, 70, 2C, 2B, 5B, 7A", ccOut: "5B",
    fingerprint type: "1001", mfr: "000C", prod: "4447", model: "3033", deviceJoinName: "HS-WS100+" // cc: "5E, 86, 72, 5A, 85, 59, 73, 25, 27, 70, 2C, 2B, 5B, 7A", ccOut: "5B",
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
    input name: "ledIndicator", type: "enum", title: "LED Indicator", description: "Turn LED indicator... ", required: false, options: ["When Off", "When On", "Never"]
    input name: "invertSwitch", type: "bool", title: "Invert Switch", description: "If you oopsed the switch... ", required: false,  defaultValue: false
    input name: "disbableDigitalOff", type: "bool", title: "Disable Digital Off", description: "Disallow digital turn off", required: false
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  }

  tiles(scale: 2) {
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
      tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC"
        attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
      }
      tileAttribute("device.indicatorStatus", key: "SECONDARY_CONTROL") {
        attributeState("when off", label:'${currentValue}', icon:"st.indicators.lit-when-off")
        attributeState("when on", label:'${currentValue}', icon:"st.indicators.lit-when-on")
        attributeState("never", label:'${currentValue}', icon:"st.indicators.never-lit")
      }
    }

    valueTile("scene", "device.Scene", width: 2, height: 2, decoration: "flat", inactiveLabel: false) {
      state "default", label: '${currentValue}'
    }

    valueTile("setScene", "device.setScene", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "Set", label: '${name}', action:"configScene", nextState: "Setting_Scene"
      state "Setting", label: '${name}' //, nextState: "Set_Scene"
    }

    standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
    }

    valueTile("firmwareVersion", "device.firmwareVersion", width: 2, height: 2, decoration: "flat", inactiveLabel: false) {
      state "default", label: '${currentValue}'
    }

    valueTile("driverVersion", "device.driverVersion", width: 2, height: 2, inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }

    standardTile("reset", "device.DeviceReset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "false", label:'', backgroundColor:"#ffffff"
      state "true", label:'reset', backgroundColor:"#e51426"
    }

    main "switch"
    details(["switch", "scene", "setScene", "firmwareVersion", "driverVersion", "refresh", "reset"])
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
    0x5B: 1,  // Central Scene
    0x70: 2,  // Configuration
    0x72: 2,  // Manufacturer Specific
    // 0x73: 1, // Powerlevel
    0x7A: 2,  // Firmware Update Md
    0x86: 1,  // Version
    0x85: 2,  // Association  0x85  V1 V2
  ]
}

def parse(String description) {
  def result = null

  //log.debug "PARSE: ${description}"
  if (description.startsWith("Err")) {
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
  } else if (description != "updated") {
    def cmd = zwave.parse(description, getCommandClassVersions())

    if (cmd) {
      result = zwaveEvent(cmd)

      if (! result) {
        log.warning "zwaveEvent() failed to return a value for command ${cmd}"
        result = createEvent(name: "lastError", value: "$cmd", descriptionText: description)
      } else {
        // If we displayed the result
        // log.debug "zwave.parse() debug: ${description}"
        // logger("Parsed $result")
      }
    } else {
      log.warning "zwave.parse() failed for: ${description}"
      result = createEvent(name: "lastError", value: "zwave.parse() failed for: ${description}", descriptionText: description)
    }
  } else {
    result = createEvent(name: "logMessage", value: "DESC: ${description}", descriptionText: description)
  }

  return result
}

private switchEvents(Short value, boolean isPhysical = true) {
  if (value == 254) {
    logger("$device.displayName returned Unknown for status.", "warn")
    return createEvent(descriptionText: "$device.displayName returned Unknown for status.", displayed: true)
  }

  return [ createEvent(name: "switch", value: value ? "on" : "off", type: isPhysical ? "physical" : "digital") ]
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
  log.debug("$device.displayName $cmd (duplicate)")
  if (0) {
    return switchEvents(cmd.value, true);
  }
  [ createEvent(descriptionText: "$device.displayName basic duplicate.", isStateChange: false, displayed: false) ]
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
  log.debug("$device.displayName $cmd -- BEING CONTROLLED")
  return switchEvents(cmd.value, true);
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
  log.debug("$device.displayName $cmd")
  return switchEvents(cmd.value, false);
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinarySet cmd) {
  log.debug("$device.displayName $cmd -- BEING CONTROLLED")
  return switchEvents(cmd.switchValue, false)
}

def buttonEvent(button, held, buttonType = "physical") {
  log.debug("buttonEvent: $button  held: $held  type: $buttonType")

  button = button as Integer
  String heldType = held ? "held" : "pushed"
  sendEvent(name: "button", value: "$heldType", data: [buttonNumber: button], descriptionText: "$device.displayName button $button was pushed", isStateChange: true, type: "$buttonType")
}

// A scene command was received -- it's probably scene 0, so treat it like a button release
def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfGet cmd) {
  log.debug("$device.displayName $cmd")
  buttonEvent(cmd.sceneId, false, "digital")
  [
    createEvent(name: "Scene", value: cmd.sceneId, isStateChange: true, displayed: true),
  ]
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfReport cmd) {
  log.debug("$device.displayName $cmd")

  // HomeSeer (ST?) does not implement this scene
  if (cmd.sceneId == 0) {
    return [
      createEvent(name: "Scene", value: cmd.sceneId, isStateChange: true, displayed: true),
      createEvent(name: "switch", value: cmd.level == 0 ? "off" : "on", isStateChange: true, displayed: true),
    ]
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
  }

  String scene_name = "Scene_$cmd.sceneId"
  String scene_duration_name = String.format("Scene_%d_Duration", cmd.sceneId)

  [ createEvent(name: "$scene_name", value: cmd.level, isStateChange: true, displayed: true),
    createEvent(name: "$scene_duration_name", value: cmd.dimmingDuration, isStateChange: true, displayed: true),
    createEvent(name: "Scene", value: cmd.sceneId, isStateChange: true, displayed: true),
    response(cmds),
  ]
}

/*
def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd) {
  log.debug("$device.displayName $cmd")
  Integer set_sceen = ((cmd.sceneId + 1) / 2) as Integer
  buttonEvent(set_sceen, false, "digital")
  [ createEvent(name: "setScene", value: "Setting", isStateChange: true, displayed: true) ]
}
*/

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
  logger("$device.displayName $cmd")

  if (cmd.parameterNumber == 3) {
    def value = "when off"

    if (cmd.configurationValue[0] == 0) {
      value = "when on"
    } else if (cmd.configurationValue[0] == 1) {
      value = "when on"
    } else if (cmd.configurationValue[0] == 2) {
      value = "never"
    }

    return [ createEvent(name: "indicatorStatus", value: value, display: false) ]
  } else if (cmd.parameterNumber == 4) {
    if ( cmd.configurationValue[0] != invertSwitch) {
      return response( [
        zwave.configurationV1.configurationSet(scaledConfigurationValue: invertSwitch ? 1 : 0, parameterNumber: cmd.parameterNumber, size: 1).format(),
        zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format(),
      ])
    }

    return [ createEvent(name: "invertedState", value: invertedStatus, display: true) ]
  }

  [ createEvent(descriptionText: "$device.displayName has unknown configuration parameter $cmd.parameterNumber : $cmd.configurationValue[0]", isStateChange: false) ]
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  logger("$device.displayName $cmd")

  if ( cmd.manufacturerId == 0x000C ) {
    updateDataValue("manufacturer", "HomeSeer")
    if (! cmd.manufacturerName ) {
      state.manufacturer= "HomeSeer"
    }
  } else if ( cmd.manufacturerId == 0x0184 ) {
    updateDataValue("manufacturer", "Dragon Tech Industrial, Ltd.")
    if (! cmd.manufacturerName ) {
      state.manufacturer= "Dragon Tech Industrial, Ltd."
    }
  } else {
    if ( cmd.manufacturerId == 0x0000 ) {
      cmd.manufacturerId = 0x0184
    }

    updateDataValue("manufacturer", "Unknown Licensed Dragon Tech Industrial, Ltd.")
    state.manufacturer= "Dragon Tech Industrial, Ltd."
  }

  if ( ! state.manufacturer ) {
    state.manufacturer= cmd.manufacturerName
  }

  state.manufacturer= cmd.manufacturerName
  state.manufacturerId = cmd.manufacturerId
  state.productTypeId = cmd.productTypeId
  state.productId= cmd.productId

  def manufacturerCode = String.format("%04X", cmd.manufacturerId)
  def productTypeCode = String.format("%04X", cmd.productTypeId)
  def productCode = String.format("%04X", cmd.productId)
  def wirelessConfig = "ZWP"

  sendEvent(name: "ManufacturerCode", value: manufacturerCode)
  sendEvent(name: "ProduceTypeCode", value: productTypeCode)
  sendEvent(name: "ProductCode", value: productCode)

  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)
  updateDataValue("manufacturer", "${state.manufacturer}")

  sendEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: false)
  [ createEvent(name: "Manufacturer", value: "${state.manufacturer}", descriptionText: "$device.displayName", isStateChange: false) ]
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
  logger("$device.displayName $cmd")

  def versions = commandClassVersions
  def version = versions[cmd.commandClass as Integer]
  def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
  def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
  if (encapsulatedCommand) {
    zwaveEvent(encapsulatedCommand)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
  logger("$device.displayName $cmd")

  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  [ createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", displayed: true, isStateChange: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd) {
  logger("$device.displayName $cmd")
  def firmware_report = String.format("%s-%s-%s", cmd.checksum, cmd.firmwareId, cmd.manufacturerId)
  updateDataValue("FirmwareMdReport", firmware_report)
  [ createEvent(name: "FirmwareMdReport", value: firmware_report, descriptionText: "$device.displayName FIRMWARE_REPORT: $firmware_report", displayed: true, isStateChange: true) ]
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
  logger("$device.displayName command not implemented: $cmd", "error")
  [ createEvent(descriptionText: "$device.displayName command not implemented: $cmd", displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd) {
  logger("$device.displayName command not implemented: $cmd")
  [ createEvent(name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false) ]
}

def on() {
  log.debug("$device.displayName on()")

  state.lastActive = new Date().time

  if (0) { // Add option to have digital commands execute buttons
    buttonEvent(1, false, "digital")
  }

  delayBetween([
    zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0xFF, sceneId: 1).format(),
    // zwave.switchBinaryV1.switchBinaryGet().format(),
  ])
}

def off() {
  log.debug("$device.displayName off()")

  state.lastActive = new Date().time

  if (0) { // Add option to have digital commands execute buttons
    buttonEvent(2, false, "digital")
  }

  if (settings.disbableDigitalOff) {
    logger("..off() disabled")
    return zwave.switchBinaryV1.switchBinaryGet().format()
  }

  delayBetween([
    // zwave.switchBinaryV1.switchBinarySet(switchValue: 0x00).format(),
    zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0xFF, sceneId: 2).format(),
    // zwave.switchBinaryV1.switchBinaryGet().format(),
  ])
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
  log.debug "ping()"
  zwave.switchBinaryV1.switchBinaryGet().format()
}

def refresh() {
  logger("refresh()")
  if (0) {
  response( zwave.switchBinaryV1.switchBinaryGet() )
  }
  zwave.switchBinaryV1.switchBinaryGet().format()
}

def poll() {
  logger("poll()")
  zwave.switchBinaryV1.switchBinaryGet().format()
  if (0) {
    zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: 0x00).format()
  }
}

void indicatorWhenOn() {
  logger("$device.displayName indicatorWhenOn()")
  // sendEvent(name: "indicatorStatus", value: "when on", displayed: false)

  sendHubCommand([
    zwave.configurationV1.configurationSet(scaledConfigurationValue: 1, parameterNumber: 3, size: 1).format(),
    zwave.configurationV1.configurationGet(parameterNumber: 3).format(),
  ])
}

void indicatorWhenOff() {
  logger("$device.displayName indicatorWhenOff()")

  sendHubCommand([
    zwave.configurationV1.configurationSet(scaledConfigurationValue: 0, parameterNumber: 3, size: 1).format(),
    zwave.configurationV1.configurationGet(parameterNumber: 3).format(),
  ])
}

void indicatorNever() {
  logger("$device.displayName indicatorNever()")
  // sendEvent(name: "indicatorStatus", value: "never", displayed: false)

  sendHubCommand([
    zwave.configurationV1.configurationSet(scaledConfigurationValue: 2, parameterNumber: 3, size: 1).format(),
    zwave.configurationV1.configurationGet(parameterNumber: 3).format(),
  ])
}

def invertSwitch(invert=true) {
  if (invert) {
    sendCommands([
      zwave.configurationV1.configurationSet(scaledConfigurationValue: 1, parameterNumber: 4, size: 1),
      zwave.configurationV1.configurationGet(parameterNumber: 4)
    ])
  }
  else {
    sendCommands([
      zwave.configurationV1.configurationSet(scaledConfigurationValue: 0, parameterNumber: 4, size: 1),
      zwave.configurationV1.configurationGet(parameterNumber: 4)
    ])
  }
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd) {
  logger("$device.displayName $cmd")
  [ createEvent(name: "DeviceReset", value: "true", descriptionText: cmd.toString(), isStateChange: true, displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd) {
  logger("$device.displayName $cmd")
  [ createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.centralscenev1.CentralSceneSupportedReport cmd) {
  logger("$device.displayName $cmd")

  def cmds = []

  for (def x = 1; x <= cmd.supportedScenes; x++) {
    cmds << zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: x)
  }

  sendCommands(cmds)

  [ createEvent(descriptionText:"CentralScene report $cmd", isStateChange: true, displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.centralscenev1.CentralSceneNotification cmd) {
  log.debug("$device.displayName $cmd")

  if ( cmd.sequenceNumber > 1 && cmd.sequenceNumber < state.sequenceNumber ) {
    return [ createEvent(descriptionText: "Late sequenceNumber  $cmd", isStateChange: false, displayed: true) ]
  }
  state.sequenceNumber= cmd.sequenceNumber

  def result = []

  state.lastActive = new Date().time

  switch (cmd.sceneNumber) {
    case 1:
    // Up
    switch (cmd.keyAttributes) {
      case 2:
      case 0:
      buttonEvent(cmd.sceneNumber, cmd.keyAttributes == 0 ? false : true, "physical")
      case 1:
      result << createEvent(name: "switch", value: cmd.sceneNumber == 1 ? "on" : "off", type: "physical")
      break;
      case 3:
      // 2 Times
      buttonEvent(3, false, "physical")
      break;
      case 4:
      // 3 Three times
      buttonEvent(5, false, "physical")
      break;
      default:
      log.error ("unexpected up press keyAttribute: $cmd")
    }
    break

    case 2:
    // Down
    switch (cmd.keyAttributes) {
      case 2:
      case 0:
      buttonEvent(cmd.sceneNumber, cmd.keyAttributes == 0 ? false : true, "physical")
      case 1:
      result << createEvent(name: "switch", value: cmd.sceneNumber == 1 ? "on" : "off", type: "physical")
      break;
      case 3:
      // 2 Times
      buttonEvent(4, false, "physical")
      break;
      case 4:
      // 3 Three times
      buttonEvent(6, false, "physical")
      break;
      default:
      log.error ("unexpected up press keyAttribute: $cmd")
    }
    break

    default:
    // unexpected case
    log.debug ("unexpected scene: $cmd.sceneNumber")
  }

  result << createEvent(name: "keyAttributes", value: cmd.keyAttributes, isStateChange: true, displayed: true)
  result << createEvent(name: "Scene", value: cmd.sceneNumber, isStateChange: true, displayed: true)

  if ( 0 ) { // cmd.keyAttributes ) {
    result << response(zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0, sceneId: cmd.sceneNumber))
  }

  return result
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
  logger("$device.displayName $cmd")

  state.groups = cmd.supportedGroupings

  if (cmd.supportedGroupings) {
    def cmds = []
    for (def x = cmd.supportedGroupings; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: 0x01);
      cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x);
      cmds << zwave.associationV2.associationGet(groupingIdentifier: x);
    }

    sendCommands(cmds, 2000)
  } else {
    [ createEvent(descriptionText: "$device.displayName reported no groups", isStateChange: true, displayed: true) ]
  }
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd) {
  logger("$device.displayName $cmd")
  [ createEvent(descriptionText: "$device.displayName AssociationGroupInfoReport: $cmd", isStateChange: true, displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd) {
  logger("$device.displayName $cmd")
  [ createEvent(descriptionText: "$device.displayName AssociationGroupNameReport: $cmd", displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
  logger("$device.displayName $cmd")

  Boolean isStateChange
  String event_value
  String event_descriptionText

  // Lifeline
  if (cmd.groupingIdentifier == 0x01) {
    def string_of_assoc = ""
    cmd.nodeId.each {
      string_of_assoc += "${it}, "
    }
    def lengthMinus2 = string_of_assoc.length() - 2
    String final_string = string_of_assoc.getAt(0..lengthMinus2)

    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      isStateChange = state.isAssociated ?: false
      event_value = "${final_string}"
      event_descriptionText = "${final_string}"
      state.isAssociated = true
    } else {
      isStateChange = state.isAssociated ? true : false
      event_value = ""
      event_descriptionText = "Hub was not found in lifeline: ${final_string}"
      state.isAssociated = false
    }
  } else {
    isStateChange = state.isAssociated ? true : false
    event_value = "misconfigured"
    event_descriptionText = "misconfigured group ${cmd.groupingIdentifier}"
  }

  if (state.isAssociated == false && cmd.groupingIdentifier == 0x01) {
    sendEvent(name: "Lifeline",
        value: "${event_value}",
        descriptionText: "${event_descriptionText}",
        displayed: true,
        isStateChange: isStateChange)
      sendCommands( [ zwave.associationV2.associationSet(groupingIdentifier: cmd.groupingIdentifier, nodeId: [zwaveHubNodeId]) ] )
  } else if (state.isAssociated == true && cmd.groupingIdentifier == 0x01) {
    [ createEvent(name: "Lifeline",
        value: "${event_value}",
        descriptionText: "${event_descriptionText}",
        displayed: true,
        isStateChange: isStateChange) ]
  } else {
    [ createEvent(descriptionText: "$device.displayName is not associated to ${cmd.groupingIdentifier}", displayed: true) ]
  }
}

def zwaveEvent(physicalgraph.zwave.commands.switchallv1.SwitchAllReport cmd) {
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
    [
      createEvent(name: "SwitchAll", value: msg, isStateChange: true, displayed: true),
    ]
  }
}

def prepDevice() {
  [
    zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
    zwave.configurationV1.configurationGet(parameterNumber: 3),
    zwave.configurationV1.configurationGet(parameterNumber: 4),
    zwave.versionV1.versionGet(),
    zwave.firmwareUpdateMdV1.firmwareMdGet(),
    //zwave.associationV2.associationGet(groupingIdentifier: 0x01),
    zwave.associationV2.associationGroupingsGet(),
    zwave.centralSceneV1.centralSceneSupportedGet(),
    zwave.zwaveCmdClassV1.requestNodeInfo(),
    zwave.switchAllV1.switchAllGet(),
    zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: 0x00),
    // zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: 1, dimmingDuration: 0, level: 255, override: true),
    // zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: 2, dimmingDuration: 0, level: 0, override: true),
  ]
}

def installed() {
  logger("$device.displayName installed()")
  sendEvent(name: "numberOfButtons", value: 8, displayed: false)
  state.loggingLevelIDE = 4

  if (0) {
    def zwInfo = getZwaveInfo()
    log.debug("$device.displayName $zwInfo")
    sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
  }

  // Device-Watch simply pings if no device events received for 32min(checkInterval)
  // sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
  indicatorWhenOff()

  sendCommands( [
    zwave.configurationV1.configurationSet(scaledConfigurationValue: 0, parameterNumber: 3, size: 1),
    ] + prepDevice(), 2000 )
}

def updated() {
  if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
    return
  }
  log.info("$device.displayName updated() debug: ${debugLevel}")
  state.loggingLevelIDE = debugLevel ? debugLevel : 4

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)
  sendEvent(name: "numberOfButtons", value: 8, displayed: true, isStateChange: true)

  if (0) {
    if (! state.indicatorStatus) {
      settings.indicatorStatus = state.indicatorStatus
    } else {
      settings.indicatorStatus = "when off"
      state.indicatorStatus = settings.indicatorStatus
    }
  }

  if (0) {
  switch (settings.indicatorStatus) {
    case "when on":
    indicatorWhenOn()
    break
    case "when off":
    indicatorWhenOff()
    break
    case "never":
    indicatorNever()
    break
    default:
    indicatorWhenOn()
    break
  }
  }

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  sendCommands( prepDevice(), 2000 )

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
private logger(msg, level = "debug") {
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
