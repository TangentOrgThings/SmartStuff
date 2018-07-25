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
  return "v2.89"
}

metadata {
  definition (name: "GE Branded Dimmer", namespace: "TangentOrgThings", author: "brian@tangent.org", ocfDeviceType: "oic.d.light") {
    capability "Actuator"
    capability "Button"
    capability "Indicator"
    capability "Light"
    capability "Polling"
    capability "Refresh"
    capability "Sensor"
    capability "Switch Level"
    capability "Switch"

    attribute "DeviceReset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message
    attribute "parseErrorCount", "number"        // Last error message
    attribute "unknownCommandErrorCount", "number"        // Last error message

    attribute "driverVersion", "string"
    attribute "firmwareVersion", "string"
    attribute "FirmwareMdReport", "string"
    attribute "Manufacturer", "string"
    attribute "manufacturerId", "string"
    attribute "MSR", "string"
    attribute "productType", "string"
    attribute "productId", "string"

    attribute "invertedStatus", "enum", ["false", "true"]

    attribute "NIF", "string"

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

    fingerprint mfr:"0063", prod:"4944", deviceJoinName: "GE In-Wall Dimmer" // model:"3031", 
    fingerprint mfr:"0063", prod:"4457", deviceJoinName: "GE 4457 Z-Wave Wall Dimmer"
    fingerprint mfr:"0063", prod:"4944", deviceJoinName: "GE 4944 Z-Wave Wall Dimmer"
    fingerprint mfr:"0063", prod:"5044", deviceJoinName: "GE 5044 Z-Wave Plug-In Dimmer"
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
    input name: "ledIndicator", type: "enum", title: "LED Indicator", description: "Turn LED indicator... ", required: true, options:["on": "When On", "off": "When Off", "never": "Never"], defaultValue: "off"
    input name: "invertSwitch", type: "bool", title: "Invert Switch", description: "Invert switch? ", required: true, defaultValue: false
    input name: "zwaveSteps", type: "number", title: "Z-Wave Dim Steps (1-99)", description: "Z-Wave Dim Steps ", required: true, range: "1..99", defaultValue: 1
    input name: "zwaveDelay", type: "number", title: "Z-Wave Dim Delay (10ms Increments, 1-255)", description: "Z-Wave Dim Delay (10ms Increments) ", required: true, range: "1..255", defaultValue: 3
    input name: "manualSteps", type: "number", title: "Manual Dim Steps (1-99)", description: "Manual Dim Steps ", required: true, range: "1..99", defaultValue: 1
    input name: "manualDelay", type: "number", title: "Manual Dim Delay (10ms Increments, 1-255)", description: "Manual Dim Delay (10ms Increments) ", required: true, range: "1..255", defaultValue: 3
    input name: "allonSteps", type: "number", title: "All-On/All-Off Dim Steps (1-99)", description: "All-On/All-Off Dim Steps ", required: true, range: "1..99", defaultValue: 1
    input name: "allonDelay", type: "number", title: "All-On/All-Off Dim Delay (10ms Increments, 1-255)", description: "All-On/All-Off Dim Delay (10ms Increments) ", required: true, range: "1..255", defaultValue: 3
    input name: "fastDuration", type: "bool", title: "Fast Duration", description: "Where to quickly change light state", required: false, defaultValue: true
    input name: "disbableDigitalOff", type: "bool", title: "Disable Digital Off", description: "Disallow digital turn off", required: false, defaultValue: false
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false, defaultValue: 3 
  }

  tiles(scale: 2) {
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
      tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label:'${name}', action:"disconnect", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff"
        attributeState "off", label:'${name}', action:"connect", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
        attributeState "turningOn", label:'${name}', action:"disconnect", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff"
        attributeState "turningOff", label:'${name}', action:"connect", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
      }
      tileAttribute ("device.level", key: "SLIDER_CONTROL") {
        attributeState "level", action:"switch level.setLevel"
      }
      tileAttribute("device.indicatorStatus", key: "SECONDARY_CONTROL") {
        attributeState("when off", label:'${currentValue}', icon:"st.indicators.lit-when-off")
        attributeState("when on", label:'${currentValue}', icon:"st.indicators.lit-when-on")
        attributeState("never", label:'${currentValue}', icon:"st.indicators.never-lit")
      }
    }

    standardTile("indicator", "device.indicatorStatus", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "when off", action:"indicator.indicatorWhenOn", icon:"st.indicators.lit-when-off"
      state "when on", action:"indicator.indicatorNever", icon:"st.indicators.lit-when-on"
      state "never", action:"indicator.indicatorWhenOff", icon:"st.indicators.never-lit"
    }

    valueTile("scene", "device.Scene", width:2, height: 2, decoration: "flat", inactiveLabel: false) {
      state "default", label: '${currentValue}'
    }

    standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
    }
    
    valueTile("driverVersion", "device.driverVersion", width: 2, height: 2, inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }

    main "switch"
    details(["switch", "scene", "indicator", "driverVersion", "refresh"])
    // details(["switch", "level", "refresh"])
  }
}

def getCommandClassVersions() { // 26, 27, 2B, 2C, 59, 5A, 5B, 5E, 70, 72, 73, 7A, 85, 86
[
  0x20: 1,  // Basic
  0x26: 3,  // SwitchMultilevel
  0x27: 1,  // Switch All
  0x2B: 1,  // SceneActivation
  0x2C: 1,  // Scene Actuator Conf
  0x56: 1,  // Crc16 Encap V1
  0x59: 1,  // Association Grp Info
  0x5A: 1,  // Device Reset Locally
  // 0x5E: 2,  // 
  0x70: 2,  // Configuration V1
  0x72: 2,  // Manufacturer Specific
  0x73: 1, // Powerlevel
  0x7A: 2,  // Firmware Update Md
  0x85: 2,  // Association  0x85  V1 V2
  0x86: 1,  // Version V2
  0x01: 1,  // Z-wave command class
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
    result << createEvent(name: "logMessage", value: "parse() called with NULL description", descriptionText: "$device.displayName")
  } else if (description != "updated") {
    def cmd = zwave.parse(description, getCommandClassVersions())

    if (cmd) {
      zwaveEvent(cmd, result)

    } else {
      logger("zwave.parse(getCommandClassVersions()) failed for: ${description}", "error")
      // Try it without check for classes
      cmd = zwave.parse(description)

      if (cmd) {
        zwaveEvent(cmd, result)
      } else {
        logger("zwave.parse(description) failed for: ${description}", "error")
      }
    }
  }

  return result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$device.displayName $cmd")

  dimmerEvents(cmd.value, false, result);
}

// Physical press of the switch
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

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
  logger("$device.displayName $cmd")

  state.manufacturerId = cmd.manufacturerId
  state.productTypeId = cmd.productTypeId
  state.productId= cmd.productId
  state.manufacturer= cmd.manufacturerName ? cmd.manufacturerName : "Jasco"

  String manufacturerId = String.format("%04X", cmd.manufacturerId)
  String productType = String.format("%04X", cmd.productTypeId)
  String productId = String.format("%04X", cmd.productId)

  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)
  updateDataValue("manufacturerName", state.manufacturer)

  Integer[] parameters = [ 10, 11, 12, 3, 4, 7, 8, 9 ]

  def cmds = []
  parameters.each {
    cmds << zwave.configurationV1.configurationGet(parameterNumber: it).format()
  }
  
  result << createEvent(name: "manufacturerId", value: manufacturerId)
  result << createEvent(name: "productType", value: productType)
  result << createEvent(name: "productId", value: productId)
  result << createEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: false)
  result << createEvent(name: "Manufacturer", value: "${state.manufacturer}", descriptionText: "$device.displayName", isStateChange: false)
  result << response(delayBetween(cmds, 1000))
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
      cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x).format()
      cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: true, refreshCache: true).format()
      cmds << zwave.associationGrpInfoV1.associationGroupCommandListGet(allowCache: true, groupingIdentifier: x).format()
    }

    result << response(delayBetween(cmds, 2000))

    return
  }
  
  logger("$device.displayName AssociationGroupingsReport: $cmd", "error")
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd, result) {
  logger("$device.displayName $cmd")

  def name = new String(cmd.name as byte[])
  logger("Association Group #${cmd.groupingIdentifier} has name: ${name}", "info")

  result << response( zwave.associationV2.associationGet(groupingIdentifier: cmd.groupingIdentifier) )
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupCommandListReport cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "DeviceReset", value: "true", descriptionText: cmd.toString(), isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelReport cmd, result) {
  logger("zwaveEvent(): Powerlevel Report received: ${cmd}")
  def device_power_level = (cmd.powerLevel > 0) ? "minus${cmd.powerLevel}dBm" : "NormalPower"
  logger("Powerlevel Report: Power: ${device_power_level}, Timeout: ${cmd.timeout}", "info")
  result << createEvent(name: "Power", value: device_power_level)
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true)
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

  delayBetween([
      zwave.basicV1.basicSet(value: 0xFF).format(),
      zwave.switchMultilevelV1.switchMultilevelGet().format(),
  ], 5000)
}

def off() {
  logger("$device.displayName off()")
  
  if (settings.disbableDigitalOff) {
    logger("..off() disabled")
    return zwave.switchMultilevelV1.switchMultilevelGet().format();
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
    
  delayBetween([
    zwave.basicV1.basicSet(value: 0x00).format(),
    zwave.switchMultilevelV1.switchMultilevelGet().format(),
  ], 5000)
}

def setLevel (value) {
  logger ("setLevel >> value: $value", "debug")
  def valueaux = value as Integer
  def level = Math.max(Math.min(valueaux, 99), 0)

  delayBetween([
    zwave.switchMultilevelV2.switchMultilevelSet(value: level).format(),
    zwave.switchMultilevelV1.switchMultilevelGet().format(),
  ], 5000)
}


def setLevel(value, duration) {
  logger("setLevel >> value: $value, duration: $duration")

  def valueaux = value as Integer
  def level = Math.max(Math.min(valueaux, 99), 0)
  def dimmingDuration = duration < 128 ? duration : 128 + Math.round(duration / 60)
  def getStatusDelay = duration < 128 ? (duration*1000)+2000 : (Math.round(duration / 60)*60*1000)+2000

  delayBetween([
    zwave.switchMultilevelV2.switchMultilevelSet(value: level, dimmingDuration: dimmingDuration).format(),
    zwave.switchMultilevelV2.switchMultilevelGet().format(),
  ], getStatusDelay)
}

def poll() {
  logger("poll()")
  zwave.switchMultilevelV2.switchMultilevelGet().format()
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
  logger("ping()")
  zwave.switchMultilevelV2.switchMultilevelGet().format()
}

def refresh() {
  logger("refresh()")

  def commands = []

  commands << zwave.switchMultilevelV2.switchMultilevelGet().format()

  if (0) {
    commands << zwave.configurationV1.configurationGet(parameterNumber: 3).format()
    commands << zwave.configurationV1.configurationGet(parameterNumber: 4).format()
    commands << zwave.configurationV1.configurationGet(parameterNumber: 7).format()
    commands << zwave.configurationV1.configurationGet(parameterNumber: 8).format()
    commands << zwave.configurationV1.configurationGet(parameterNumber: 9).format()
    commands << zwave.configurationV1.configurationGet(parameterNumber: 10).format()
  }

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
  if (0) {
    [
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
    ]
  }
  [
    // zwave.switchBinaryV1.switchBinaryGet(),
    zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
    zwave.associationV2.associationGroupingsGet(),
    zwave.firmwareUpdateMdV2.firmwareMdGet(),
    zwave.powerlevelV1.powerlevelGet(),
    zwave.switchAllV1.switchAllGet(),
    zwave.switchMultilevelV1.switchMultilevelGet(),
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
  sendCommands(prepDevice(), 3000)
}

def updated() {
  if ( state.updatedDate && ((Calendar.getInstance().getTimeInMillis() - state.updatedDate)) < 5000 ) {
    return
  }

  state.loggingLevelIDE = debugLevel ? debugLevel : 4
  log.info("$device.displayName updated() debug: ${state.loggingLevelIDE}")

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)
  sendEvent(name: "parseErrorCount", value: 0, displayed: false)
  sendEvent(name: "unknownCommandErrorCount", value: 0, displayed: false)

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
  sendCommands(prepDevice(), 3000)

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
    case "unknownCommand":
    state.unknownCommandErrorCount += 1
    sendEvent(name: "unknownCommandErrorCount", value: unknownCommandErrorCount, displayed: false, isStateChange: true)
    break

    case "parse":
    state.parseErrorCount += 1
    sendEvent(name: "parseErrorCount", value: parseErrorCount, displayed: false, isStateChange: true)
    break

    case "warn":
    if (state.loggingLevelIDE >= 2) {
      log.warn msg
      sendEvent(name: "logMessage", value: "WARNING: ${msg}", displayed: false, isStateChange: true)
    }
    return

    case "info":
    if (state.loggingLevelIDE >= 3) {
      log.info msg
    }
    return

    case "debug":
    if (state.loggingLevelIDE >= 4) {
      log.debug msg
    }
    return

    case "trace":
    if (state.loggingLevelIDE >= 5) {
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
