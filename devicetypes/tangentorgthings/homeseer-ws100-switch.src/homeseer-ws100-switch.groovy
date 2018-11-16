// vim: set filetype=groovy tabstop=2 shiftwidth=2 softtabstop=2 expandtab smarttab :

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
 *  Date: 2017-2018
 *
 *  Changelog:
 *
 *
 *
 *
 */

import physicalgraph.*

String getDriverVersion () {
  return "v7.35"
}

def getConfigurationOptions(Integer model) {
  if ( model == 12341 ) {
    return [ 13, 14, 21, 3, 31, 4 ] // Removed 6, support of this has not arrived
  }
  return [ 3, 4 ]
}

metadata {
  definition (name: "Homeseer WS100 Switch", namespace: "TangentOrgThings", author: "brian@tangent.org", ocfDeviceType: "oic.d.switch") {
    capability "Actuator"
    capability "Button"
    capability "Indicator"
    capability "Light"    
    capability "Notification"    
    capability "Refresh"
    capability "Polling"
    capability "Sensor"
    capability "Switch"

    attribute "DeviceReset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message

    attribute "invertedState", "enum", ["false", "true"]

    attribute "driverVersion", "string"

    attribute "firmwareVersion", "string"
    attribute "zWaveProtocolVersion", "string"

    attribute "FirmwareMdReport", "string"
    attribute "ManufacturerCode", "string"
    attribute "NIF", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"

    attribute "Scene", "number"
    attribute "keyAttributes", "number"

    attribute "SwitchAll", "string"
    attribute "Power", "string"
    
    attribute "statusMode", "enum", ["default", "status"]
    attribute "defaultLEDColor", "enum", ["white", "red", "green", "blue", "magenta", "yellow", "cyan"]    
    attribute "statusLEDColor", "enum", ["off", "red", "green", "blue", "magenta", "yellow", "cyan", "white"]
    attribute "blinkFrequency", "number" 
    
    command "basicOn"
    command "basicOff"
    command "codeRed"

    // zw:L type:1001 mfr:000C prod:4447 model:3033 ver:5.14 zwv:4.05 lib:03 cc:5E,86,72,5A,85,59,73,25,27,70,2C,2B,5B,7A ccOut:5B role:05 ff:8700 ui:8700
    fingerprint mfr: "0184", prod: "4447", model: "3033", deviceJoinName: "WS-100" // cc: "5E, 86, 72, 5A, 85, 59, 73, 25, 27, 70, 2C, 2B, 5B, 7A", ccOut: "5B",
    fingerprint mfr: "000C", prod: "4447", model: "3033", deviceJoinName: "HS-WS100+" // cc: "5E, 86, 72, 5A, 85, 59, 73, 25, 27, 70, 2C, 2B, 5B, 7A", ccOut: "5B",
    fingerprint mfr: "000C", prod: "4447", model: "3035", deviceJoinName: "HS-WS200+" // cc: "5E, 86, 72, 5A, 85, 59, 73, 25, 27, 70, 2C, 2B, 5B, 7A", ccOut: "5B",
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
    input name: "invertSwitch", type: "bool", title: "Invert Switch", description: "If you oopsed the switch... ", required: false,  defaultValue: false
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

    standardTile("indicator", "device.indicatorStatus", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "when off", action: "indicator.indicatorWhenOn", icon: "st.indicators.lit-when-off"
      state "when on", action: "indicator.indicatorNever", icon: "st.indicators.lit-when-on"
      state "never", action: "indicator.indicatorWhenOff", icon: "st.indicators.never-lit"
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

    standardTile("reset", "device.DeviceReset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "false", label: '', backgroundColor:"#ffffff", defaultState: true
      state "true", label: 'reset', backgroundColor:"#e51426"
    }

    standardTile("basicOn", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "on", label:'test on', action:"basicOn", icon: "st.switches.switch.on"
    }

    standardTile("basicOff", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "off", label:'test off', action:"basicOff", icon: "st.switches.switch.off"
    }

    standardTile("codeRed", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "default", label:'', action: "device.codeRed", icon: "st.secondary.refresh"
    }

    main "switch"
    details(["switch", "indicator", "scene", "driverVersion", "refresh", "reset", "basicOn", "basicOff", "codeRed"])
  }
}

def getCommandClassVersions() {
  if (state.MSR && state.MSR == "000C-4447-3035") {
    return [
      0x20: 1,  // Basic
      0x25: 1,  // Switch Binary
      0x27: 1,  // Switch All
      0x2B: 1,  // SceneActivation
      0x2C: 1,  // Scene Actuator Conf
      0x59: 1,  // Association Grp Info
      0x5A: 1,  // Device Reset Locally
      0x5B: 1,  // Central Scene
      // 0x5E: 2, //
      // 0x6C: 2, // Supervision
      0x70: 2,  // Configuration
      0x72: 2,  // Manufacturer Specific
      0x73: 1,  // Powerlevel
      0x7A: 2,  // Firmware Update Md HS-200 V4
      0x85: 2,  // Association  0x85  V1 V2    
      0x86: 1,  // Version
      // 0x55: 1,  // Transport Service Command Class
      // 0x9F: 1,  // Security 2 Command Class  
      // Controlled
      0x21: 1,  // Application Status
    ]
  }

  // 000C-4447-3033
  return [
    0x20: 1,  // Basic
    0x25: 1,  // Switch Binary
    0x27: 1,  // Switch All
    0x2B: 1,  // SceneActivation
    0x2C: 1,  // Scene Actuator Conf
    0x59: 1,  // Association Grp Info
    0x5A: 1,  // Device Reset Locally
    0x5B: 1,  // Central Scene
    // 0x5E: 2, //
    // 0x6C: 2, // Supervision
    0x70: 2,  // Configuration
    0x72: 2,  // Manufacturer Specific
    0x73: 1,  // Powerlevel
    0x7A: 2,  // Firmware Update Md HS-200 V4
    0x85: 2,  // Association  0x85  V1 V2    
    0x86: 1,  // Version
    // 0x55: 1,  // Transport Service Command Class
    // 0x9F: 1,  // Security 2 Command Class  
    // Controlled
    0x21: 1,  // Application Status
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

def zwaveEvent(zwave.commands.switchbinaryv1.SwitchBinaryGet cmd, result) {
  logger("$cmd")

  def value = device.currentState("switch").value.equals("on") ? 255 : 0
  result << zwave.basicV1.switchBinaryReport(value: value).format()
}

def zwaveEvent(zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, result) {
  logger("$cmd")

  Short value = cmd.value

  if (value == 0) {
    result << createEvent(name: "switch", value: "off", isStateChange: true, displayed: true)
    if (device.displayName.endsWith("Dimmer")) {
      result << createEvent(name: "level", value: 0, isStateChange: true, displayed: true)
    }
  } else if (value == 255) {
    result << createEvent(name: "switch", value: "on", isStateChange: true, displayed: true)
    if (device.displayName.endsWith("Dimmer")) {
      result << createEvent(name: "level", value: 100, isStateChange: true, displayed: true)
    }
  } else if (value < 254) {
    logger("SwitchBinaryReport returned reserved state ($value)", "warn")
  } else if (value == 254) {
    logger("SwitchBinaryReport unknown state (254)", "warn")
  } else {
    logger("SwitchBinaryReport reported value unknown to API ($value)", "warn")
  }
}

def zwaveEvent(zwave.commands.switchbinaryv1.SwitchBinarySet cmd, result) {
  logger("$cmd")

  Short value = cmd.switchValue

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
    logger("SwitchBinarySet returned reserved state ($value)", "warn")
  } else if (value == 254) {
    logger("SwitchBinarySet unknown state (254)", "warn")
  } else {
    logger("SwitchBinarySet reported value unknown to API ($value)", "warn")
  }
} 

// These will show up from time to time, handle them as control
def zwaveEvent(zwave.commands.sensorbinaryv2.SensorBinaryReport cmd, result) {
  logger("$device.displayName $cmd -- BEING CONTROLLED")
  if (cmd.sensorValue) {
    on()
    return
  }

  off()
}

def zwaveEvent(zwave.commands.securitypanelmodev1.SecurityPanelModeSupportedGet cmd, result) {
  logger("$cmd")
  result << response(zwave.securityPanelModeV1.securityPanelModeSupportedReport(supportedModeBitMask: 0))
}

def zwaveEvent(zwave.commands.securitypanelmodev1.SecurityPanelModeReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.securitypanelmodev1.SecurityPanelModeSupportedReport cmd, result) {
  logger("$cmd")
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
def zwaveEvent(zwave.commands.sceneactuatorconfv1.SceneActuatorConfGet cmd, result) {
  logger("$cmd")
  buttonEvent("SceneActuatorConfGet()", cmd.sceneId, false, "digital")
  result << response(zwave.sceneActuatorConfV1.sceneActuatorConfReport(dimmingDuration: 0xFF, level: 0xFF, sceneId: cmd.sceneId))
}

def zwaveEvent(zwave.commands.sceneactuatorconfv1.SceneActuatorConfReport cmd, result) {
  logger("$cmd")

  // HomeSeer (ST?) does not implement this scene
  if (cmd.sceneId == 0) {
    if (0) {
    result << createEvent(name: "Scene", value: cmd.sceneId, isStateChange: true, displayed: true)
    result << createEvent(name: "switch", value: cmd.level == 0 ? "off" : "on", isStateChange: true, displayed: true)
    }
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
  } else if (cmd.sceneId == zwaveHubNodeId) {
    if (cmd.level || (cmd.dimmingDuration != 0x88)) {
      cmds << zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: cmd.sceneId, dimmingDuration: 0x88, level: 0, override: true).format()
      cmds << zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: cmd.sceneId).format()
    }
  }

  updateDataValue("Scene #${cmd.sceneId}", "Level: ${cmd.level} Dimming Duration: ${cmd.dimmingDuration}")

  if (cmds.size()) {
    result << response(delayBetween(cmds, 1000))
  }
}

def zwaveEvent(zwave.commands.sceneactivationv1.SceneActivationSet cmd, result) {
  logger("$cmd")
  result << createEvent(name: "Scene", value: "${cmd.sceneId}", isStateChange: true)
}

def zwaveEvent(zwave.commands.configurationv2.ConfigurationReport cmd, result) {
  logger("$cmd")

  updateDataValue("Configuration #${cmd.parameterNumber}", "${cmd.scaledConfigurationValue}")

  switch (cmd.parameterNumber) {
    case 3:
    if (1) {
      switch (cmd.configurationValue[0]) {
        case 0:
        result << createEvent(name: "indicatorStatus", value: "when off", display: false, isStateChange: true)
        break;
        case 1:
        result << createEvent(name: "indicatorStatus", value: "when on", display: false, isStateChange: true)
        break;
        case 2:
        result << createEvent(name: "indicatorStatus", value: "never", display: false, isStateChange: true)
        break;
        default:
        indicatorWhenOff()
        break;
      }
      return
    }
    break;
    case 4:
    if (1) {
      if ( cmd.configurationValue[0] != invertSwitch) {
        return response( delayBetween(
          [
          zwave.configurationV1.configurationSet(scaledConfigurationValue: invertSwitch ? 1 : 0, parameterNumber: cmd.parameterNumber, size: 1).format(),
          zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format(),
          ]
        ))
      }

      result << createEvent(name: "invertedState", value: invertedStatus, display: true)
      return
    } 
    break;
    case 13: // Display mode
    result << createEvent(name: "statusMode", value: cmd.configurationValue[0] ? "status" : "default")
    return
    break;
    case 14: // Sets the default LED color
    if (1) {
      String color
      switch (cmd.configurationValue[0]) {
        case 0: // White
        color = "white";
        break;
        case 1:
        color = "red";
        break;
        case 2:
        color = "green"
        break;
        case 3:
        color = "blue"
        break; 
        case 4:
        color = "magenta"
        break;
        case 5:
        color = "yellow"
        break;
        case 6:
        color = "cyan"
        break;    
        default:
        logger("Unknown default color ${cmd.configurationValue[0]}", "error")
        return;
        break;
      }
      result << createEvent(name: "defaultLEDColor", value: color);
    }
    break;
    case 21: // Sets the Color of the LED indicator in Status mode
    if (1) {
      String color = "error"
      switch (cmd.configurationValue[0]) {
        case 0: // Off
        color = "off";
        break;
        case 1:
        color = "red";
        break;
        case 2:
        color = "green"
        break;
        case 3:
        color = "blue"
        break; 
        case 4:
        color = "magenta"
        break;
        case 5:
        color = "yellow"
        break;
        case 6:
        color = "cyan"
        break; 
        case 7:
        color = "white"
        break;    
        default:
        logger("Unknown status color ${cmd.configurationValue[0]}", "error")
        return
        break;
      }
      result << createEvent(name: "statusLEDColor", value: color);
    }
    break;
    case 31: // Sets Blink Frequency of LED in Status mode
    result << createEvent(name: "blinkFrequency", value: cmd.configurationValue[0])
    break;
    default:
    logger("$device.displayName has unknown configuration parameter $cmd.parameterNumber", "warn")
    break;
  }
}

def zwaveEvent(zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
  logger("$cmd")

  if ( cmd.manufacturerId == 0x000C ) {
    updateDataValue("manufacturer", "HomeSeer")

    if (! cmd.manufacturerName ) {
      state.manufacturer= "HomeSeer"
    }

    if (cmd.productId == 0x3035) {
      if (! childDevices) {
        createChildDevices()
      }
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

  String manufacturerCode = String.format("%04X", cmd.manufacturerId)
  String productTypeCode = String.format("%04X", cmd.productTypeId)
  String productCode = String.format("%04X", cmd.productId)

  String msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)
  state.MSR = "$msr"
  updateDataValue("manufacturer", "${state.manufacturer}")

  Integer[] parameters = getConfigurationOptions(cmd.productId)

  def cmds = []
  parameters.each {
    cmds << zwave.configurationV1.configurationGet(parameterNumber: it).format()
  }

  result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
  result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
  result << createEvent(name: "ProductCode", value: productCode)
  result << response(delayBetween(cmds, 1000))
  result << response( zwave.versionV1.versionGet() )
}

def zwaveEvent(zwave.commands.crc16encapv1.Crc16Encap cmd, result) {
  logger("$cmd")

  def versions = commandClassVersions
  def version = versions[cmd.commandClass as Integer]
  def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
  def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
  if (encapsulatedCommand) {
    zwaveEvent(encapsulatedCommand, result)
  }
}

def zwaveEvent(zwave.commands.powerlevelv1.PowerlevelReport cmd, result) {
  logger("zwaveEvent(): Powerlevel Report received: ${cmd}")
  String device_power_level = (cmd.powerLevel > 0) ? "minus${cmd.powerLevel}dBm" : "NormalPower"
  logger("Powerlevel Report: Power: ${device_power_level}, Timeout: ${cmd.timeout}", "info")
  result << createEvent(name: "Power", value: device_power_level)
}

def zwaveEvent(zwave.commands.powerlevelv1.PowerlevelTestNodeReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.controllerreplicationv1.CtrlReplicationTransferGroup cmd, result) {
  logger("$cmd")
  updateDataValue("CtrlReplicationTransferGroup", "$cmd")
}

def zwaveEvent(zwave.commands.controllerreplicationv1.CtrlReplicationTransferGroupName cmd, result) {
  logger("$cmd")
  updateDataValue("CtrlReplicationTransferGroupName", "$cmd")
}

def zwaveEvent(zwave.commands.controllerreplicationv1.CtrlReplicationTransferScene cmd, result) {
  logger("$cmd")
  updateDataValue("CtrlReplicationTransferScene", "$cmd")
}

def zwaveEvent(zwave.commands.controllerreplicationv1.CtrlReplicationTransferSceneName cmd, result) {
  logger("$cmd")
  updateDataValue("CtrlReplicationTransferSceneName", "$cmd")
}

def zwaveEvent(zwave.commands.versionv1.VersionReport cmd, result) {
  logger("$cmd")

  String text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  String zWaveProtocolVersion = "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  result << createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: true)
  result << createEvent(name: "zWaveProtocolVersion", value: "${zWaveProtocolVersion}", descriptionText: "${device.displayName} ${zWaveProtocolVersion}", isStateChange: true)
}

def zwaveEvent(zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd, result) {
  logger("$cmd")
  String firmware_report = String.format("%s-%s-%s", cmd.checksum, cmd.firmwareId, cmd.manufacturerId)
  updateDataValue("FirmwareMdReport", firmware_report)
  result << createEvent(name: "FirmwareMdReport", value: firmware_report, descriptionText: "$device.displayName FIRMWARE_REPORT: $firmware_report", displayed: true, isStateChange: true)
}

def zwaveEvent(zwave.commands.applicationstatusv1.ApplicationBusy cmd, result) {
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

def zwaveEvent(zwave.commands.applicationstatusv1.ApplicationRejectedRequest cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.applicationcapabilityv1.CommandCommandClassNotSupported cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.Command cmd, result) {
  logger("command not implemented: $cmd", "warn")
}

def childOn(String childID) {
  logger("childOn($childID)") 

  return response( delayBetween(
    [
    zwave.configurationV1.configurationSet(scaledConfigurationValue: 1, parameterNumber: 21, size: 1).format(),
    zwave.configurationV1.configurationGet(parameterNumber: 21).format(),
    ]
  ))
}

def basicOn() {
  response(zwave.basicV1.basicSet(value: 0xFF))
}

def basicOff() {
  response(zwave.basicV1.basicSet(value: 0x00))
}

def childOff(String childID) {
  logger("childOff($childID)") 

  return response( delayBetween(
    [
    zwave.configurationV1.configurationSet(scaledConfigurationValue: 0, parameterNumber: 21, size: 1).format(),
    zwave.configurationV1.configurationGet(parameterNumber: 21).format(),
    ]
  ))
}

def codeRed() {
  deviceNotification("red")
}

def deviceNotification(String notification) {
  logger("deviceNotification()");

  Integer colorNum = 0

  String notificationLower = notification.toLowerCase()

  switch (notificationLower) {
    case "off": // Off
    colorNum = 0;
    break;

    case "red":
    colorNum = 1
    break;

    case "green":
    colorNum = 2
    break;

    case "blue":
    colorNum = 3
    break; 

    case "magenta":
    colorNum = 4
    break;

    case "yellow":
    colorNum = 5
    break;

    case "cyan":
    colorNum = 6
    break; 

    case "white":
    colorNum = 7
    break;    

    default:
    logger("Unknown notification ${notification}", "error")
    return
    break;
  }

  sendEvent(name: "statusLEDColor", value: color);

  return response( delayBetween(
    [
    zwave.configurationV1.configurationSet(scaledConfigurationValue: 1, parameterNumber: 13, size: 1).format(),
    zwave.configurationV1.configurationSet(scaledConfigurationValue: colorNum, parameterNumber: 21, size: 1).format(),
    zwave.configurationV1.configurationGet(parameterNumber: 13).format(),
    zwave.configurationV1.configurationGet(parameterNumber: 21).format(),
    ]
  ))
}

def on() {
  logger("on()")

  if (1) { // Add option to have digital commands execute buttons
    buttonEvent("on()", 1, false, "digital")
  }

  delayBetween([
    zwave.basicV1.basicSet(value: 0xFF).format(),
    zwave.switchBinaryV1.switchBinaryGet().format(),
  ], 2000)
}

def off() {
  logger("off()")

  if (1) { // Add option to have digital commands execute buttons
    buttonEvent("off()", 2, false, "digital")
  }

  if (settings.disbableDigitalOff) {
    logger("..off() disabled")
    return zwave.switchBinaryV1.switchBinaryGet().format();
  }

  def cmds = []
  if (settings.delayOff) {
    // cmds << zwave.versionV1.versionGet()
    // cmds << zwave.zwaveCmdClassV1.zwaveCmdNop()
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

void indicatorWhenOn() {
  logger("indicatorWhenOn()")
  // sendEvent(name: "indicatorStatus", value: "when on", displayed: false)

  sendHubCommand([
    zwave.configurationV1.configurationSet(scaledConfigurationValue: 1, parameterNumber: 3, size: 1).format(),
    zwave.configurationV1.configurationGet(parameterNumber: 3).format(),
  ])
}

void indicatorWhenOff() {
  logger("indicatorWhenOff()")

  sendHubCommand([
    zwave.configurationV1.configurationSet(scaledConfigurationValue: 0, parameterNumber: 3, size: 1).format(),
    zwave.configurationV1.configurationGet(parameterNumber: 3).format(),
  ])
}

void indicatorNever() {
  logger("indicatorNever()")
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

def zwaveEvent(zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd, result) {
  logger("$cmd")
  result << createEvent(name: "DeviceReset", value: "true", descriptionText: cmd.toString(), isStateChange: true, displayed: true)
}

def zwaveEvent(zwave.commands.zwavecmdclassv1.NodeInfo cmd, result) {
  logger("$cmd")
  result << createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(zwave.commands.centralscenev1.CentralSceneSupportedReport cmd, result) {
  logger("$cmd")

  def cmds = []

  for (def x = 1; x <= cmd.supportedScenes; x++) {
    cmds << zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: x).format()
  }
  cmds << zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: integerHex(device.deviceNetworkId)).format()

  if (cmds.size) {
    result << response(delayBetween(cmds))
  }
}

def zwaveEvent(zwave.commands.centralscenev1.CentralSceneNotification cmd, result) {
  log.debug("$cmd")

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

def zwaveEvent(zwave.commands.associationv2.AssociationGroupingsReport cmd, result) {
  logger("$cmd")

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

  logger("AssociationGroupingsReport: $cmd", "error")
}

def zwaveEvent(zwave.commands.associationv2.AssociationSpecificGroupReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd, result) {
  logger("$cmd")

  def name = new String(cmd.name as byte[])
  logger("Association Group #${cmd.groupingIdentifier} has name: ${name}", "info")

  updateDataValue("Group #${cmd.groupingIdentifier}", "${name}")

  result << response(delayBetween([
    zwave.associationV1.associationGet(groupingIdentifier: cmd.groupingIdentifier).format(),
  ]))
}

def zwaveEvent(zwave.commands.associationgrpinfov1.AssociationGroupCommandListReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$cmd")

  String event_value

  if (cmd.groupingIdentifier > 2) {
    logger("Unknown Group Identifier", "error");
    return
  }

  // Lifeline
  def string_of_assoc = ""
  cmd.nodeId.each {
    string_of_assoc += "${it}, "
  }
  def lengthMinus2 = ( string_of_assoc.length() > 3 ) ? string_of_assoc.length() - 3 : 0
  def final_string = lengthMinus2 ? string_of_assoc.getAt(0..lengthMinus2) : string_of_assoc

  event_value = "${final_string}"

  if (cmd.groupingIdentifier == 1) {
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      state.isAssociated = true
    } else {
      state.isAssociated = false
      result << response( delayBetween([
        zwave.associationV1.associationSet(groupingIdentifier: cmd.groupingIdentifier, nodeId: zwaveHubNodeId).format(),
        zwave.associationV1.associationGet(groupingIdentifier: cmd.groupingIdentifier).format(),
      ]))
    }
  }

  if (cmd.groupingIdentifier == 2) {
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      result << response( delayBetween([
        zwave.associationV1.associationRemove(groupingIdentifier: cmd.groupingIdentifier, nodeId: zwaveHubNodeId).format(),
        zwave.associationV1.associationGet(groupingIdentifier: cmd.groupingIdentifier).format(),
      ]))
    }
  }

  String group_name = ""
  switch (cmd.groupingIdentifier) {
    case 1:
    group_name = "Lifeline"
      break;
    case 2:
    group_name = "On/Off/Dimming Aux Control"
    break;
    default:
    group_name = "Unknown";
    break;
  }

  updateDataValue("$group_name", "$event_value")
}

def zwaveEvent(zwave.commands.switchallv1.SwitchAllReport cmd, result) {
  logger("$cmd")

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

private void createChildDevices() {
  // Save the device label for updates by updated()
  state.oldLabel = device.label

  // Add child devices for four button presses
  addChildDevice(
    "TangentOrgThings",
    "Homeseer WS200 Child",
    "${device.deviceNetworkId}/Status",
    "",
    [
    label         : "$device.displayName Status",
    completedSetup: true,
    isComponent: true,
    ])
}

def prepDevice() {
  [
    zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
    zwave.firmwareUpdateMdV2.firmwareMdGet(),
    zwave.associationV2.associationGroupingsGet(),
    zwave.centralSceneV1.centralSceneSupportedGet(),
    zwave.switchAllV1.switchAllGet(),
    zwave.powerlevelV1.powerlevelGet(),
    // zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: 0),
    // zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: 1, dimmingDuration: 0, level: 255, override: true),
    // zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: 2, dimmingDuration: 0, level: 0, override: true),
    // zwave.switchBinaryV1.switchBinaryGet(),
  ]
}

def installed() {
  logger("installed()")
  sendEvent(name: "numberOfButtons", value: 6, displayed: false)

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
  sendEvent(name: "Scene", value: 0, isStateChange: true)
  indicatorWhenOff()

  sendCommands( [
    zwave.configurationV1.configurationSet(scaledConfigurationValue: 0, parameterNumber: 3, size: 1),
    ] + prepDevice(), 2000 )
  response(refresh())
}

def updated() {
  if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
    return
  }
  logger("updated() debug: ${settings.debugLevel}")

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)

  sendEvent(name: "numberOfButtons", value: 6, displayed: true, isStateChange: true)
  sendEvent(name: "Scene", value: 0, isStateChange: true)

  if (1) {
    switch ( device.currentValue("indicatorStatus") ) {
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
      indicatorWhenOff()
      break
    }
  }
  
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
 *  Returns a zwave.Command.
 **/
private encapCommand(zwave.Command cmd) {
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
  return response(delayBetween(cmds.collect{ (it instanceof zwave.Command ) ? encapCommand(it).format() : it }, delay))
}

/**
 *  sendCommands(cmds, delay=200)
 *
 *  Sends a list of commands directly to the device using sendHubCommand.
 *  Uses encapCommand() to apply security or CRC16 encapsulation as needed.
 **/
private sendCommands(cmds, delay=200) {
  sendHubCommand( cmds.collect{ (it instanceof zwave.Command ) ? response(encapCommand(it)) : response(it) }, delay)
}


private logger(msg, level = "trace") {
  String device_name = "$device.displayName"

  switch(level) {
    case "warn":
    if (settings.debugLevel >= 2) {
      log.warn "$device_name ${msg}"
    }
    sendEvent(name: "logMessage", value: " ${msg}", displayed: false, isStateChange: true)
    break;

    case "info":
    if (settings.debugLevel >= 3) {
      log.info "$device_name ${msg}"
    }
    break;

    case "debug":
    if (settings.debugLevel >= 4) {
      log.debug "$device_name ${msg}"
    }
    break;

    case "trace":
    if (settings.debugLevel >= 5) {
      log.trace "$device_name ${msg}"
    }
    break;

    case "error":
    default:
    log.error "$device_name ${msg}"
    sendEvent(name: "lastError", value: "${msg}", displayed: false, isStateChange: true)
    break;
  }
}
