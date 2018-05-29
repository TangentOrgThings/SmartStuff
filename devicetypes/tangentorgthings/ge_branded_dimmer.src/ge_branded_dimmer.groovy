// vim :set ts=2 sw=2 sts=2 expandtab smarttab :
/**
 *  Copyright 2017-2018 Brian Aker <brian@tangent.org>
 *  Original version derived from Smartthings Dimmer Device
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
  return "v2.73"
}

metadata {
  definition (name: "GE Branded Dimmer", namespace: "TangentOrgThings", author: "brian@tangent.org", ocfDeviceType: "oic.d.light") {
    capability "Switch Level"
    capability "Actuator"
    capability "Button"
    capability "Light"
    capability "Switch"
    capability "Refresh"
    capability "Sensor"
    capability "Health Check"

    attribute "DeviceReset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message

    attribute "driverVersion", "string"
    attribute "firmwareVersion", "string"
    attribute "FirmwareMdReport", "string"
    attribute "Manufacturer", "string"
    attribute "ManufacturerCode", "string"
    attribute "MSR", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"

    attribute "invertedStatus", "enum", ["false", "true"]

    attribute "NIF", "string"

    attribute "setScene", "enum", ["Set", "Setting"]
    attribute "keyAttributes", "number"

    attribute "Scene", "number"
    attribute "Scene_1", "number"
    attribute "Scene_1_Duration", "number"
    attribute "Scene_2", "number"
    attribute "Scene_2_Duration", "number"

    attribute "SwitchAll", "string"

    fingerprint mfr:"0063", prod:"4944", model:"3031", deviceJoinName: "GE In-Wall Dimmer"
  }

  simulator {
    status "on":  "command: 2003, payload: FF"
    status "off": "command: 2003, payload: 00"
    status "09%": "command: 2003, payload: 09"
    status "10%": "command: 2003, payload: 0A"
    status "33%": "command: 2003, payload: 21"
    status "66%": "command: 2003, payload: 42"
    status "99%": "command: 2003, payload: 63"

    // reply messages
    reply "2001FF,delay 5000,2602": "command: 2603, payload: FF"
    reply "200100,delay 5000,2602": "command: 2603, payload: 00"
    reply "200119,delay 5000,2602": "command: 2603, payload: 19"
    reply "200132,delay 5000,2602": "command: 2603, payload: 32"
    reply "20014B,delay 5000,2602": "command: 2603, payload: 4B"
    reply "200163,delay 5000,2602": "command: 2603, payload: 63"
  }

  preferences {
    input name: "ledIndicator", type: "enum", title: "LED Indicator", description: "Turn LED indicator... ", required: true, options:["on": "When On", "off": "When Off", "never": "Never"]
    input name: "invertSwitch", type: "bool", title: "Invert Switch", description: "Invert switch? ", required: true
    input name: "zwaveSteps", type: "number", title: "Z-Wave Dim Steps (1-99)", description: "Z-Wave Dim Steps ", required: true, range: "1..99"
    input name: "zwaveDelay", type: "number", title: "Z-Wave Dim Delay (10ms Increments, 1-255)", description: "Z-Wave Dim Delay (10ms Increments) ", required: true, range: "1..255"
    input name: "manualSteps", type: "number", title: "Manual Dim Steps (1-99)", description: "Manual Dim Steps ", required: true, range: "1..99"
    input name: "manualDelay", type: "number", title: "Manual Dim Delay (10ms Increments, 1-255)", description: "Manual Dim Delay (10ms Increments) ", required: true, range: "1..255"
    input name: "allonSteps", type: "number", title: "All-On/All-Off Dim Steps (1-99)", description: "All-On/All-Off Dim Steps ", required: true, range: "1..99"
    input name: "allonDelay", type: "number", title: "All-On/All-Off Dim Delay (10ms Increments, 1-255)", description: "All-On/All-Off Dim Delay (10ms Increments) ", required: true, range: "1..255"
    input name: "disbableDigitalOff", type: "bool", title: "Disable Digital Off", description: "Disallow digital turn off", required: false
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  }

  tiles(scale: 2) {
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
      tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
        attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
        attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
        attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
      }
      tileAttribute ("device.level", key: "SLIDER_CONTROL") {
        attributeState "level", action:"switch level.setLevel"
      }
    }

    standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
    }

    
    valueTile("driverVersion", "device.driverVersion", width: 2, height: 2, inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }

    main "switch"
    details(["switch", "driverVersion", "refresh"])
    // details(["switch", "level", "refresh"])
  }
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
    result << createEvent(name: "logMessage", value: "parse() called with NULL description", descriptionText: "$device.displayName")
  } else if (description != "updated") {
    def cmd = zwave.parse(description) //, getCommandClassVersions())

    if (cmd) {
      zwaveEvent(cmd, result)

    } else {
      log.warn "zwave.parse() failed for: ${description}"
      result << createEvent(name: "lastError", value: "zwave.parse() failed for: ${description}", descriptionText: description)
      // Try it without check for classes
      cmd = zwave.parse(description)

      if (cmd) {
        zwaveEvent(cmd, result)
      }
    }
  }

  return result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$device.displayName $cmd")

  dimmerEvents(cmd.value, false, result);
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$device.displayName $cmd")

  dimmerEvents(cmd.value, true, result);

  if (cmd.value == 255) {
    buttonEvent("SceneActuatorConfGet()", 1, false, "physical")
  }
  else if (cmd.value == 0) {
    buttonEvent("SceneActuatorConfGet()", 2, false, "physical")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, result) {
  logger("$device.displayName $cmd")

  dimmerEvents(cmd.value, false, result);
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinarySet cmd, result) {
  logger("$device.displayName $cmd -- BEING CONTROLLED")

  dimmerEvents(cmd.switchValue, true, result);
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd, result) {
  logger("$device.displayName $cmd")

  dimmerEvents(cmd.value, false, result)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelSet cmd, result) {
  logger("$device.displayName $cmd")

  dimmerEvents(cmd.value, false, result)
}

def zwaveEvent(physicalgraph.zwave.commands.nodenamingv1.NodeNamingNodeNameReport cmd, result) {
  logger("$device.displayName $cmd")
  state.nodeName = cmd.nodeName
}

def zwaveEvent(physicalgraph.zwave.commands.nodenamingv1.NodeNamingNodeLocationReport cmd, result) {
  logger("$device.displayName $cmd")
  state.nodeLocation = cmd.nodeLocation
}

private dimmerEvents(Integer dimmer_level, boolean isPhysical, result) {
  def switch_value = (dimmer_level ? "on" : "off")
  result << createEvent(name: "switch", value: switch_value, type: isPhysical ? "physical" : "digital", isStateChange: true, displayed: true)

  if (dimmer_level && dimmer_level <= 100) {
    result << createEvent(name: "level", value: dimmer_level, unit: "%", isStateChange: true, displayed: true)
  }

  return result
}


def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd, result) {
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
    case 7:
    name = "zwaveSteps"
    value = reportValue
    break
    case 8:
    name = "zwaveDelay"
    value = reportValue
    break
    case 9:
    name = "manualSteps"
    value = reportValue
    break
    case 10:
    name = "manualDelay"
    value = reportValue
    break
    default:
    break
  }

  result <<  createEvent(name: name, value: value)
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd, result) {
  result << createEvent(name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false)
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
  logger("$device.displayName $cmd")

  state.manufacturerId = cmd.manufacturerId
  state.productTypeId = cmd.productTypeId
  state.productId= cmd.productId
  state.manufacturer= cmd.manufacturerName ? cmd.manufacturerName : "Jasco"

  def manufacturerCode = String.format("%04X", cmd.manufacturerId)
  def productTypeCode = String.format("%04X", cmd.productTypeId)
  def productCode = String.format("%04X", cmd.productId)

  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)
  updateDataValue("manufacturerName", state.manufacturer)
  updateDataValue("manufacturerId", manufacturerCode)
  updateDataValue("productId", productCode)
  updateDataValue("productTypeId", productTypeCode)

  result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
  result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
  result << createEvent(name: "ProductCode", value: productCode)
  result << createEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: false)
  result << createEvent(name: "Manufacturer", value: "${state.manufacturer}", descriptionText: "$device.displayName", isStateChange: false)
  // result << response(delayBetween(cmds, 1000))
  result << response( zwave.versionV1.versionGet() )
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd, result) {
  logger("zwaveEvent(): CRC-16 Encapsulation Command received: ${cmd}")

  def encapsulatedCommand = zwave.commandClass(cmd.commandClass)?.command(cmd.command)?.parse(cmd.data)
  if (!encapsulatedCommand) {
    log.debug("zwaveEvent(): Could not extract command from ${cmd}")
  } else {
    return zwaveEvent(encapsulatedCommand)
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

def buttonEvent(String exec_cmd, Integer button, held, buttonType = "physical") {
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
  result << createEvent(name: "Scene", value: cmd.sceneId, isStateChange: true, displayed: true)
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
  }

  String scene_name = "Scene_$cmd.sceneId"
  String scene_duration_name = String.format("Scene_%d_Duration", cmd.sceneId)

  result << createEvent(name: "$scene_name", value: cmd.level, isStateChange: true, displayed: true)
  result << createEvent(name: "$scene_duration_name", value: cmd.dimmingDuration, isStateChange: true, displayed: true)
  result << createEvent(name: "Scene", value: cmd.sceneId, isStateChange: true, displayed: true)
  result << response(cmds)
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd, result) {
  logger("$device.displayName $cmd")

  state.groups = cmd.supportedGroupings

  if (cmd.supportedGroupings) {
    def cmds = []
    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x);
      cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: 0x00);
      cmds << zwave.associationGrpInfoV1.associationGroupCommandListGet(allowCache: true, groupingIdentifier: x);
    }

    sendCommands(cmds, 2000)
  } else {
    result << createEvent(descriptionText: "$device.displayName reported no groups", isStateChange: true, displayed: true)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(descriptionText: "$device.displayName AssociationGroupInfoReport: $cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd, result) {
  logger("$device.displayName $cmd")

  def name = new String(cmd.name as byte[])
  logger("Association Group #${cmd.groupingIdentifier} has name: ${name}", "info")

  result << createEvent(descriptionText: "$device.displayName AssociationGroupNameReport: $cmd", displayed: true)
  result << response( zwave.associationV2.associationGet(groupingIdentifier: cmd.groupingIdentifier) )
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupCommandListReport cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(descriptionText: "$device.displayName AssociationGroupCommandListReport: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$device.displayName $cmd")
  [ createEvent(descriptionText: "$device.displayName AssociationReport: $cmd", isStateChange: true, displayed: true) ]
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
  result << createEvent(descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationRejectedRequest cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.applicationcapabilityv1.CommandCommandClassNotSupported cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  logger("$device.displayName command not implemented: $cmd", "error")
  result << createEvent(descriptionText: "$device.displayName command not implemented: $cmd", displayed: true)
} 

def on() {
  logger("$device.displayName on()")

  def result = []

  state.lastActive = new Date().time

  result << createEvent(name: "Scene", value: 1, displayed: true)
  result << createEvent(name: "setScene", value: "Setting", isStateChange: true, displayed: true)

  result << delayBetween([
    // zwave.basicV1.basicSet(value: 0xFF).format(),
    zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0, sceneId: 1).format(),
    zwave.switchMultilevelV2.switchMultilevelGet().format(),
  ])

  return result
}

def off() {
  logger("$device.displayName off()")

  def result = []

  if (settings.disbableDigitalOff) {
    log.debug "..off() disabled"
    return response(zwave.switchBinaryV1.switchBinaryGet())
  }

  result << createEvent(name: "Scene", value: 2, displayed: true)
  result << createEvent(name: "setScene", value: "Setting", isStateChange: true, displayed: true)

  result << delayBetween([
    zwave.basicV1.basicSet(value: 0x00).format(),
    zwave.switchMultilevelV2.switchMultilevelGet().format(),
  ])

  return result
}

def setLevel (value) {
  logger ("setLevel >> value: $value", "debug")
  def valueaux = value as Integer
  def level = Math.max(Math.min(valueaux, 99), 0)

  def result = []

  if (level > 0) {
    result << createEvent(name: "switch", value: "on")
    result << createEvent(name: "Scene", value: 1, displayed: true)
    result << createEvent(name: "setScene", value: "Setting", isStateChange: true, displayed: true)
  } else {
    result << createEvent(name: "switch", value: "off")
    result << createEvent(name: "Scene", value: 2, displayed: true)
    result << createEvent(name: "setScene", value: "Setting", isStateChange: true, displayed: true)
  }

  result << createEvent(name: "level", value: level, unit: "%")
  result << delayBetween ([
    zwave.switchMultilevelV2.switchMultilevelSet(value: level).format(),
    zwave.switchMultilevelV2.switchMultilevelGet().format()
  ], 5000)

  return result
}


def setLevel(value, duration) {
  log.debug "setLevel >> value: $value, duration: $duration"

  def valueaux = value as Integer
  def level = Math.max(Math.min(valueaux, 99), 0)
  def dimmingDuration = duration < 128 ? duration : 128 + Math.round(duration / 60)
  def getStatusDelay = duration < 128 ? (duration*1000)+2000 : (Math.round(duration / 60)*60*1000)+2000

  if (level > 0) {
    sendEvent(name: "switch", value: "on")
  } else {
    sendEvent(name: "switch", value: "off")
  }

  delayBetween ([
    zwave.switchMultilevelV2.switchMultilevelSet(value: level, dimmingDuration: dimmingDuration).format(),
    zwave.switchMultilevelV2.switchMultilevelGet().format()
  ], getStatusDelay)
}

def poll() {
  zwave.switchMultilevelV2.switchMultilevelGet().format()
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
  zwave.switchMultilevelV2.switchMultilevelGet().format()
}

def refresh() {
  logger "refresh() is called"

  def commands = []

  commands << zwave.switchMultilevelV2.switchMultilevelGet().format()

  if (getDataValue("MSR") == null) {
    commands << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
  }

  commands << zwave.configurationV1.configurationGet(parameterNumber: 3).format()
  commands << zwave.configurationV1.configurationGet(parameterNumber: 4).format()
  commands << zwave.configurationV1.configurationGet(parameterNumber: 7).format()
  commands << zwave.configurationV1.configurationGet(parameterNumber: 8).format()
  commands << zwave.configurationV1.configurationGet(parameterNumber: 9).format()
  commands << zwave.configurationV1.configurationGet(parameterNumber: 10).format()

  delayBetween(commands)
}

void indicatorWhenOn() {
  sendEvent(name: "indicatorStatus", value: "when on", displayed: false)
  sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 3, size: 1).format()))
}

void indicatorWhenOff() {
  sendEvent(name: "indicatorStatus", value: "when off", displayed: false)
  sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 3, size: 1).format()))
}

void indicatorNever() {
  sendEvent(name: "indicatorStatus", value: "never", displayed: false)
  sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [2], parameterNumber: 3, size: 1).format()))
}

def invertSwitch(invert=true) {
  if (invert) {
    zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 4, size: 1).format()
  } else {
    zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 4, size: 1).format()
  }
}

def prepDevice() {
  [
    // zwave.switchBinaryV1.switchBinaryGet(),
    zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
    zwave.versionV1.versionGet(),
    zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: 1),
    zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: 2),
    zwave.associationV2.associationGroupingsGet(),
    // zwave.configurationV1.configurationSet(configurationValue: [ledIndicator == "on" ? 1 : ledIndicator == "never" ? 2 : 0], parameterNumber: 3, size: 1),
    zwave.configurationV1.configurationSet(configurationValue: [invertSwitch == true ? 1 : 0], parameterNumber: 4, size: 1),
    zwave.configurationV1.configurationSet(configurationValue: [zwaveSteps], parameterNumber: 7, size: 1),
    zwave.configurationV1.configurationSet(configurationValue: [zwaveDelay], parameterNumber: 8, size: 1),
    zwave.configurationV1.configurationSet(configurationValue: [manualSteps], parameterNumber: 9, size: 1),
    zwave.configurationV1.configurationSet(configurationValue: [manualDelay], parameterNumber: 10, size: 1),
    zwave.configurationV1.configurationSet(configurationValue: [allonSteps], parameterNumber: 11, size: 1),
    zwave.configurationV1.configurationSet(configurationValue: [allonDelay], parameterNumber: 12, size: 1),
    zwave.configurationV1.configurationGet(parameterNumber: 3),
    zwave.configurationV1.configurationGet(parameterNumber: 4),
    zwave.configurationV1.configurationGet(parameterNumber: 7),
    zwave.configurationV1.configurationGet(parameterNumber: 8),
    zwave.configurationV1.configurationGet(parameterNumber: 9),
    zwave.configurationV1.configurationGet(parameterNumber: 10),
    zwave.configurationV1.configurationGet(parameterNumber: 11),
    zwave.configurationV1.configurationGet(parameterNumber: 12),
    zwave.firmwareUpdateMdV2.firmwareMdGet(),
    zwave.zwaveCmdClassV1.requestNodeInfo(),
    ]
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
  // sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])

  sendEvent(name: "driverVersion", value: getDriverVersion(), isStateChange: true)
  sendCommands(prepDevice())
}

def updated() {
  log.info("$device.displayName updated() debug: ${debugLevel}")
  state.loggingLevelIDE = debugLevel ? debugLevel : 4

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)

  /*
  def zwInfo = getZwaveInfo()
  if ($zwInfo) {
  log.debug("$device.displayName $zwInfo")
  sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
  } else {
  log.debug("$device.displayName has no ZwaveInfo")
  }
   */

  // Device-Watch simply pings if no device events received for 86220 (one day minus 3 minutes)
  // sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])

  sendEvent(name: "driverVersion", value: getDriverVersion(), isStateChange: true)
  sendEvent(name: "numberOfButtons", value: 4, displayed: false)
  sendCommands(prepDevice())
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
