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
  return "v6.55"
}

metadata {
  definition (name: "WS-100 Switch", namespace: "TangentOrgThings", author: "brian@tangent.org", ocfDeviceType: "oic.d.switch") {
    capability "Actuator"
    capability "Health Check"
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
    attribute "zWaveProtocolVersion", "string"
    attribute "Power", "string"
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
    fingerprint mfr: "0184", prod: "4447", model: "3033", deviceJoinName: "WS-100" // cc: "5E, 86, 72, 5A, 85, 59, 73, 25, 27, 70, 2C, 2B, 5B, 7A", ccOut: "5B",
    fingerprint mfr: "000C", prod: "4447", model: "3033", deviceJoinName: "HS-WS100+" // cc: "5E, 86, 72, 5A, 85, 59, 73, 25, 27, 70, 2C, 2B, 5B, 7A", ccOut: "5B",
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
    input name: "delayOff", type: "bool", title: "Delay Off", description: "Delay Off for three seconds", required: false
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  }

  tiles(scale: 2) {
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
      tileAttribute ("device.Switch", key: "PRIMARY_CONTROL") {
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
      state "Set", label: '${name}', nextState: "Setting"
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
    0x73: 1,  // Powerlevel
    0x7A: 2,  // Firmware Update Md
    0x86: 1,  // Version
    0x85: 2,  // Association  0x85  V1 V2
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

private switchEvents(Short value, boolean isPhysical, result) {
  if (value == 254) {
    logger("$device.displayName returned Unknown for status.", "warn")
    result << createEvent(descriptionText: "$device.displayName returned Unknown for status.", displayed: true)
    return
  }

  result << createEvent(name: "switch", value: value ? "on" : "off", type: isPhysical ? "physical" : "digital", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$device.displayName $cmd (duplicate)")
  switchEvents(cmd.value, true, result);
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$device.displayName $cmd -- BEING CONTROLLED")
  switchEvents(cmd.value, true, result);
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, result) {
  logger("$device.displayName $cmd")
  switchEvents(cmd.value, false, result);
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinarySet cmd, result) {
  logger("$device.displayName $cmd -- BEING CONTROLLED")
  switchEvents(cmd.switchValue, false, result)
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
  result << response(cmds)
}

/*
def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd, result) {
  log.debug("$device.displayName $cmd")
  Integer set_sceen = ((cmd.sceneId + 1) / 2) as Integer
  buttonEvent("SceneActivationSet()", set_sceen, false, "digital")
  [ createEvent(name: "setScene", value: "Setting", isStateChange: true, displayed: true) ]
}
*/

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd, result) {
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

    result << createEvent(name: "indicatorStatus", value: value, display: false)
    return
  } else if (cmd.parameterNumber == 4) {
    if ( cmd.configurationValue[0] != invertSwitch) {
      return response( [
        zwave.configurationV1.configurationSet(scaledConfigurationValue: invertSwitch ? 1 : 0, parameterNumber: cmd.parameterNumber, size: 1).format(),
        zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format(),
      ])
    }

    result << createEvent(name: "invertedState", value: invertedStatus, display: true)
    return
  }

  result << createEvent(descriptionText: "$device.displayName has unknown configuration parameter $cmd.parameterNumber : $cmd.configurationValue[0]", isStateChange: false)
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
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

  def manufacturerCode = String.format("%04X", cmd.manufacturerId)
  def productTypeCode = String.format("%04X", cmd.productTypeId)
  def productCode = String.format("%04X", cmd.productId)

  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)
  updateDataValue("manufacturer", "${state.manufacturer}")

  Integer[] parameters = [ 3, 4 ]

  def cmds = []
  parameters.each {
    cmds << zwave.configurationV1.configurationGet(parameterNumber: it).format()
  }

  result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
  result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
  result << createEvent(name: "ProductCode", value: productCode)
  result << createEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: false)
  result << createEvent(name: "Manufacturer", value: "${state.manufacturer}", descriptionText: "$device.displayName", isStateChange: false)
  result << response(delayBetween(cmds, 1000))
  result << response( zwave.versionV1.versionGet() )
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

def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelReport cmd, result) {
  logger("zwaveEvent(): Powerlevel Report received: ${cmd}","trace")
  def device_power_level = (cmd.powerLevel > 0) ? "minus${cmd.powerLevel}dBm" : "NormalPower"
  logger("Powerlevel Report: Power: ${device_power_level}, Timeout: ${cmd.timeout}","info")
  result << createEvent(name: "Power", value: device_power_level)
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

  state.lastActive = new Date().time

  if (0) { // Add option to have digital commands execute buttons
    buttonEvent("on()", 1, false, "digital")
  }

  sendEvent(name: "setScene", value: "Setting", isStateChange: true, displayed: true)

  def cmds = []
  cmds << zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0xFF, sceneId: 1);
  cmds << zwave.basicV1.basicSet(value: 0xFF);
  cmds << zwave.basicV1.basicGet();
  
  return sendCommands(cmds)
}

def off() {
  logger("$device.displayName off()")

  state.lastActive = new Date().time

  if (0) { // Add option to have digital commands execute buttons
    buttonEvent("off()", 2, false, "digital")
  }

  if (settings.disbableDigitalOff) {
    logger("..off() disabled")
    return response(zwave.basicV1.basicGet())
  }

  def cmds = []
  if (settings.delayOff) {
    cmds << zwave.versionV1.versionGet()
  }

  sendEvent(name: "setScene", value: "Setting", isStateChange: true, displayed: true)

  cmds << zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0xff, sceneId: 2);
  cmds << zwave.basicV1.basicSet(value: 0x00);
  cmds << zwave.basicV1.basicGet();

  return sendCommands( cmds, settings.delayOff ? 3000 : 600 )
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
  logger "ping()"
  if (0) {
    zwave.switchBinaryV1.switchBinaryGet().format()
  }
  zwave.basicV1.basicGet().format()
}

def refresh() {
  logger("refresh()")
  
  if (0) {
    response( zwave.switchBinaryV1.switchBinaryGet() )
  }
  
  delayBetween([
  	zwave.basicV1.basicGet().format(),
    zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0xFF, sceneId: 0).format()
  ])
}

def poll() {
  logger("poll()")
  if (0) {
    response( zwave.switchBinaryV1.switchBinaryGet() )
  }
  if (0) {
    zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: 0x00).format()
  }

  response ( zwave.basicV1.basicGet() )
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

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "DeviceReset", value: "true", descriptionText: cmd.toString(), isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.centralscenev1.CentralSceneSupportedReport cmd, result) {
  logger("$device.displayName $cmd")

  def cmds = []

  for (def x = 1; x <= cmd.supportedScenes; x++) {
    cmds << zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: x)
  }

  result << createEvent(descriptionText:"CentralScene report $cmd", isStateChange: true, displayed: true)
  result << sendCommands(cmds)
}

def zwaveEvent(physicalgraph.zwave.commands.centralscenev1.CentralSceneNotification cmd, result) {
  log.debug("$device.displayName $cmd")

  if ( cmd.sequenceNumber > 1 && cmd.sequenceNumber < state.sequenceNumber ) {
    result << createEvent(descriptionText: "Late sequenceNumber  $cmd", isStateChange: false, displayed: true)
    return
  }
  state.sequenceNumber= cmd.sequenceNumber

  def cmds = []
  
  state.lastActive = new Date().time

  switch (cmd.sceneNumber) {
    case 1:
    // Up
    switch (cmd.keyAttributes) {
      case 2:
      case 0:
      buttonEvent("CentralSceneNotification()", cmd.sceneNumber, cmd.keyAttributes == 0 ? false : true, "physical")
      case 1:
      result << createEvent(name: "setScene", value: "Setting", isStateChange: true, displayed: true)
      result << createEvent(name: "switch", value: cmd.sceneNumber == 1 ? "on" : "off", type: "physical")
      result << response( zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0xFF, sceneId: 0) )
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
      log.error ("unexpected up press keyAttribute: $cmd")
    }
    break

    case 2:
    // Down
    switch (cmd.keyAttributes) {
      case 2:
      case 0:
      buttonEvent("CentralSceneNotification()", cmd.sceneNumber, cmd.keyAttributes == 0 ? false : true, "physical")
      case 1:
      result << createEvent(name: "setScene", value: "Setting", isStateChange: true, displayed: true)
      result << createEvent(name: "switch", value: cmd.sceneNumber == 1 ? "on" : "off", type: "physical")
      result << response( zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0xFF, sceneId: 0) )
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
      log.error ("unexpected up press keyAttribute: $cmd")
    }
    break

    default:
    // unexpected case
    log.debug ("unexpected scene: $cmd.sceneNumber")
  }

  if (0) {
  result << createEvent(name: "keyAttributes", value: cmd.keyAttributes, isStateChange: true, displayed: true)
  result << createEvent(name: "Scene", value: cmd.sceneNumber, isStateChange: true, displayed: true)
  }

  if ( 0 ) { // cmd.keyAttributes ) {
    result << response(zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0, sceneId: cmd.sceneNumber))
  }
  
  cmds << "delay 2000"
  cmds << zwave.basicV1.basicGet().format()

  if (cmds.size) {
    result << response(delayBetween(cmds))
  }

  return result
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

  Boolean isStateChange
  String event_value
  String event_descriptionText

  // Lifeline
  if (cmd.groupingIdentifier == 0x01) {
    def string_of_assoc = ""
    cmd.nodeId.each {
      string_of_assoc += "${it}, "
    }
    def lengthMinus2 = string_of_assoc.length() ? string_of_assoc.length() - 3 : 0
    def final_string = lengthMinus2 ? string_of_assoc.getAt(0..lengthMinus2) : string_of_assoc

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
    result << createEvent(name: "Lifeline",
        value: "${event_value}",
        descriptionText: "${event_descriptionText}",
        displayed: true,
        isStateChange: isStateChange)
      sendCommands( [ zwave.associationV2.associationSet(groupingIdentifier: cmd.groupingIdentifier, nodeId: [zwaveHubNodeId]) ] )
  } else if (state.isAssociated == true && cmd.groupingIdentifier == 0x01) {
    result << createEvent(name: "Lifeline",
        value: "${event_value}",
        descriptionText: "${event_descriptionText}",
        displayed: true,
        isStateChange: isStateChange)
  } else {
    result << createEvent(descriptionText: "$device.displayName is not associated to ${cmd.groupingIdentifier}", displayed: true)
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

def prepDevice() {
  [
    zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
    // zwave.versionV1.versionGet(),
    zwave.firmwareUpdateMdV2.firmwareMdGet(),
    zwave.associationV2.associationGroupingsGet(),
    zwave.centralSceneV1.centralSceneSupportedGet(),
    zwave.switchAllV1.switchAllGet(),
    zwave.powerlevelV1.powerlevelGet(),
    // zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: 0),
    // zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: 1, dimmingDuration: 0, level: 255, override: true),
    // zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: 2, dimmingDuration: 0, level: 0, override: true),
    zwave.zwaveCmdClassV1.requestNodeInfo(),
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
  state.loggingLevelIDE = debugLevel ? debugLevel : 4
  log.info("$device.displayName updated() debug: ${state.loggingLevelIDE}")

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
  
  // Device-Watch simply pings if no device events received for 32min(checkInterval)
  sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID]) //, offlinePingable: "1"])

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
