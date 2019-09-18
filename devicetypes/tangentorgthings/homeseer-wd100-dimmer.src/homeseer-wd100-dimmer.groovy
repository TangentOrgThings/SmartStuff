// vim: set filetype=groovy tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :

/**
 *  HomeSeer HS-WD100+
 *
 *  Copyright 2017-2019 Brian Aker <brian@tangent.org>
 *  Copyright 2016 DarwinsDen.com
 *
 *  For device parameter information and images, questions or to provide feedback on this device handler,
 *  please visit:
 *
 *      darwinsden.com/homeseer100plus/
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
 *  Author: Darwin@DarwinsDen.com
 *  Date: 2016-04-10
 *
 *  Changelog:
 *
 *  0.10 (04/10/2016) - Initial 0.1 Beta.
 *  0.11 (05/28/2016) - Set numberOfButtons attribute for ease of use with CoRE and other SmartApps. Corrected physical/digital states.
 *  0.12 (06/03/2016) - Added press type indicator to display last tap/hold press status
 *  0.13 (06/13/2016) - Added dim level ramp-up option for remote dim commands
 *  0.14 (08/01/2016) - Corrected 60% dim rate limit test that was inadvertently pulled into repository
 *  0.15 (09/06/2016) - Added Firmware version info. Removed unused lit indicator button.
 *  0.16 (09/24/2016) - Added double-tap-up to full brightness option and support for firmware dim rate configuration parameters.
 *  0.17 (10/05/2016) - Added single-tap-up to full brightness option.
 *
 */

import physicalgraph.*

String getDriverVersion() {
  return "v7.53"
}

def getConfigurationOptions(Integer model) {
  if ( model == 0x3036 ) {
    return [ 13, 14, 21, 31, 4, 7, 8, 9, 10, 5 ] // Removed 6, support of this has not arrived
  }

  return [ 4, 7, 8, 9, 10 ]
}

metadata {
  definition (name: "Homeseer WD100 Dimmer", namespace: "TangentOrgThings", author: "brian@tangent.org", ocfDeviceType: "oic.d.light") {
    capability "Actuator"
    capability "Button"
    capability "Indicator"
    capability "Light"
    capability "Refresh"
    capability "Sensor"
    capability "Switch Level"
    capability "Switch"

    attribute "DeviceReset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message

    attribute "driverVersion", "string"

    attribute "firmwareVersion", "string"
    attribute "zWaveProtocolVersion", "string"

    attribute "FirmwareMdReport", "string"
    attribute "NIF", "string"

    attribute "MSR", "string"
    attribute "Manufacturer", "string"
    attribute "ManufacturerCode", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"

    attribute "invertedStatus", "enum", ["false", "true"]

    attribute "Scene", "number"
    attribute "keyAttributes", "number"

    attribute "SwitchAll", "string"
    attribute "Power", "string"
    
    attribute "statusMode", "enum", ["default", "status"]
    attribute "defaultLEDColor", "enum", ["White", "Red", "Green", "Blue", "Magenta", "Yellow", "Cyan"]    
    attribute "statusLEDColor", "enum", ["Off", "Red", "Green", "Blue", "Magenta", "Yellow", "Cyan", "White"]
    attribute "blinkFrequency", "number" 

    command "basicOn"
    command "basicOff"

    command "setStatusLed"
    command "setSwitchModeNormal"
    command "setSwitchModeStatus"
    command "setDefaultColor"
    command "setBlinkDurationMilliseconds"

    // 0 0 0x2001 0 0 0 a 0x30 0x71 0x72 0x86 0x85 0x84 0x80 0x70 0xEF 0x20
    // zw:L type:1101 mfr:0184 prod:4447 model:3034 ver:5.14 zwv:4.24 lib:03 cc:5E,86,72,5A,85,59,73,26,27,70,2C,2B,5B,7A ccOut:5B role:05 ff:8600 ui:8600
    fingerprint mfr: "000C", prod: "4447", model: "3034", deviceJoinName: "HS-WD100+ In-Wall Dimmer" //, cc: "5E, 86, 72, 5A, 85, 59, 73, 26, 27, 70, 2C, 2B, 5B, 7A", ccOut: "5B", deviceJoinName: "HS-WD100+ In-Wall Dimmer"
    fingerprint mfr: "0184", prod: "4447", model: "3034", deviceJoinName: "WD100+ In-Wall Dimmer" // , cc: "5E, 86, 72, 5A, 85, 59, 73, 26, 27, 70, 2C, 2B, 5B, 7A", ccOut: "5B", deviceJoinName: "WD100+ In-Wall Dimmer"
    fingerprint mfr: "000C", prod: "4447", model: "3036", deviceJoinName: "WD200+ In-Wall Dimmer" //
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
    input name: "disableSafetyLevel", type: "bool", title: "Disable Safety Level", description: "Disable Safety Level", required: false,  defaultValue: false
    input name: "startMax", type: "bool", title: "Start at Max", description: "Always Start at Max Power", required: false,  defaultValue: true
    input name: "invertSwitch", type: "bool", title: "Invert Switch", description: "If you oopsed the switch... ", required: false,  defaultValue: false
    input name: "localStepDuration", type: "number", title: "Local Ramp Rate: Duration of each level (1-255)(1=10ms) [default: 3]", range: "1..255", required: false
    input name: "localStepSize", type: "number", title: "Local Ramp Rate: Dim level % to change each duration (1-99) [default: 1]", range: "1..99", required: false
    input name: "remoteStepDuration", type: "number", title: "Remote Ramp Rate: Duration of each level (1-255)(1=10ms) [default: 3]", range: "1..255", required: false
    input name: "remoteStepSize", type: "number", title: "Remote Ramp Rate: Dim level % to change each duration (1-99) [default: 1]", range: "1..99", required: false
    input name: "fastDuration", type: "bool", title: "Fast Duration", description: "Where to quickly change light state", required: false, defaultValue: true
    input name: "disbableDigitalOff", type: "bool", title: "Disable Digital Off", description: "Disallow digital turn off", required: false, defaultValue: false
    input name: "enableDigitalButtons", type: "bool", title: "Enable Digital Buttons", description: "Enable on and off commands to execute digital buttons", required: false, defaultValue: false
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
    input name: "color", type: "enum", title: "Default LED Color", options: ["White", "Red", "Green", "Blue", "Magenta", "Yellow", "Cyan"], description: "Select Color", required: false
  }

  tiles(scale: 2) {
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
      tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff"
        attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
        attributeState "turningOn", label:'${name}', icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff"
        attributeState "turningOff", label:'${name}', icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
      }

      tileAttribute ("device.level", key: "SLIDER_CONTROL") {
        attributeState "level", action: "switch level.setLevel", defaultState: true
      }

      tileAttribute("device.Scene", key: "SECONDARY_CONTROL") {
        attributeState("default", label: '${currentValue}', unit: "")
      }
    }

    standardTile("indicator", "device.indicatorStatus", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "when off", action:"indicator.indicatorWhenOn", icon:"st.indicators.lit-when-off"
      state "when on", action:"indicator.indicatorNever", icon:"st.indicators.lit-when-on"
      state "never", action:"indicator.indicatorWhenOff", icon:"st.indicators.never-lit"
    }

    standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "default", label: '', action: "refresh.refresh", icon: "st.secondary.refresh"
    }

    valueTile("driverVersion", "device.driverVersion", inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }

    standardTile("reset", "device.DeviceReset", inactiveLabel: true, decoration: "flat", width: 2, height: 2) {
      state "false", label:'', backgroundColor:"#ffffff"
      state "true", label:'reset', backgroundColor:"#e51426"
    }

    main "switch"
    details(["switch", "driverVersion", "indicator", "refresh", "reset"])
  }
}

def getCommandClassVersions() { // 26, 27, 2B, 2C, 59, 5A, 5B, 5E, 70, 72, 73, 7A, 85, 86
  if (state.MSR && state.MSR == "000C-4447-3036") {
    return [
      0x20: 1,  // Basic
      0x26: 1,  // SwitchMultilevel
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
      0x01: 1,  // Z-wave command class
      0x25: 1,  // Switch Binary <-- This does seem to happen
      0x56: 1,  // Crc16 Encap	0x56	V1
      // Controlled
      0x22: 1,  // Application Status
    ]
  }

  return [
    0x20: 1,  // Basic
    0x26: 1,  // SwitchMultilevel
    0x27: 1,  // Switch All
    0x2B: 1,  // SceneActivation
    0x2C: 1,  // Scene Actuator Conf
    0x59: 1,  // Association Grp Info
    0x5A: 1,  // Device Reset Locally
    0x5B: 1,  // Central Scene
    0x70: 1,  // Configuration
    0x72: 2,  // Manufacturer Specific
    0x73: 1,  // Powerlevel
    0x7A: 2,  // Firmware Update Md
    0x86: 1,  // Version
    0x85: 2,  // Association  0x85  V1 V2
    0x01: 1,  // Z-wave command class
    0x25: 1,  // Switch Binary <-- This does seem to happen
    0x56: 1,  // Crc16 Encap	0x56	V1
    // Controlled
    0x22: 1,  // Application Status
  ]
}

def parse(String description) {
  def result = []

  if (description && description.startsWith("Err")) {
    logger( "parse error: ${description}", "error")

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
    logger("parse() called with NULL description", "warn")
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
        logger("zwave.parse() failed for: ${description}", "error")
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

def zwaveEvent(physicalgraph.zwave.commands.switchallv1.SwitchAllReport cmd, result) {
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

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelGet cmd, result) {
  logger("$cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd, result) {
  logger("$cmd")
  dimmerEvents(cmd.value, false, result)
}


def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelSet cmd, result) {
  logger("$cmd -- BEING CONTROLLED")
  dimmerEvents(cmd.value, false, result)
}


def zwaveEvent(physicalgraph.zwave.commands.securitypanelmodev1.SecurityPanelModeSupportedGet cmd, result) {
  result << response(zwave.securityPanelModeV1.securityPanelModeSupportedReport(supportedModeBitMask: 0))
}

void buttonEvent(Integer button, Boolean held, String buttonType = "physical") {
  logger("buttonEvent: $button  held: $held  type: $buttonType")

  String heldType = held ? "held" : "pushed"

  if (button > 0) {
    sendEvent(name: "button", value: "$heldType", data: [buttonNumber: button], descriptionText: "$device.displayName button $button was pushed", isStateChange: true, type: "$buttonType")
  } else {
    sendEvent(name: "button", value: "default", descriptionText: "$device.displayName button released", isStateChange: true, type: "$buttonType")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfGet cmd, result) {
  logger("$cmd")
  buttonEvent(cmd.sceneId, false, "digital")

  result << response(zwave.sceneActuatorConfV1.sceneActuatorConfReport(dimmingDuration: fastDuration ? 0x00 : 0xFF, level: 0xFF, sceneId: cmd.sceneId))
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfReport cmd, result) {
  logger("$cmd")

  def cmds = []
  
  switch (cmd.sceneId) {
    case 0:
    result << createEvent(name: "Scene", value: cmd.sceneId, isStateChange: true, displayed: true)
    result << createEvent(name: "level", value: cmd.level, isStateChange: true, displayed: true)
    result << createEvent(name: "switch", value: cmd.level == 0 ? "off" : "on", isStateChange: true, displayed: true)

    return;
    break;
    case 1:
      if (startMax) {
        if (cmd.level != 99) {
          cmds << zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: cmd.sceneId, dimmingDuration: 0, level: 99, override: true).format()
        }
      } else if (cmd.level != 255) {
        cmds << zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: cmd.sceneId, dimmingDuration: fastDuration ? 0x00 : 0xFF, level: 255, override: true).format()
      }
    break;
    case 2:
      if (cmd.level) {
        cmds << zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: cmd.sceneId, dimmingDuration: fastDuration ? 0x00 : 0xFF, level: 0, override: true).format()
      }
    break;
    default:
    break;
  }

  updateDataValue("Scene #${cmd.sceneId}", "Level: ${cmd.level} Dimming Duration: ${cmd.dimmingDuration}")

  if (cmds) {
    result << response(delayBetween(cmds))
  }
}

private dimmerEvents(Integer cmd_value, boolean isPhysical, result) {
  if (cmd_value == 255) {
    logger("returned default value so request current value.", "info")
    result << zwave.basicV1.basicGet().format()
    return
  }

  if (cmd_value == 254) {
    logger("returned Unknown for level.", "info")
    result << zwave.basicV1.basicGet().format()
    return
  }

  if (cmd_value > 99 && cmd_value < 255) {
    logger("returned invalid level value.", "warn")
    result << zwave.basicV1.basicGet().format()
    return
  }

  Integer level = Math.max(Math.min(cmd_value, 99), 0)

  def cmds = []

  if (( ! settings.disableSafetyLevel ) && level >= 1 && level <= 32) {  // Make sure we don't burn anything out
    cmds << zwave.switchMultilevelV1.switchMultilevelSet(value: 33).format()
    cmds << zwave.switchMultilevelV1.switchMultilevelGet().format()
  }

  // state.lastLevel = cmd.value
  if (cmd_value && level < 100) {
    result << createEvent(name: "switch", value: "on", type: isPhysical ? "physical" : "digital", displayed: true )
    result << createEvent(name: "level", value: ( level == 99 ) ? 100 : level, unit: "%", displayed: true)
  } else if (level == 0) {
    result << createEvent(name: "switch", value: "off", type: isPhysical ? "physical" : "digital", displayed: true )
  }

  if (cmds) {
    result << delayBetween(cmds, 1000)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd, result) {
  logger("$cmd")
  result << createEvent(name: "Scene", value: "${cmd.sceneId}", isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd, result) {
  logger("$cmd")

  updateDataValue("Configuration #${cmd.parameterNumber}", "${cmd.scaledConfigurationValue}")

  def cmds = []

  if (1) {
    switch (cmd.parameterNumber) {
      case 3: // Homeseer has an undocumented indicator light in the most recent versions of the firmware
      if (1) {
        def indicatorStatus = "when off"
        if (cmd.configurationValue[0] == 1) { indicatorStatus = "when on" }
        if (cmd.configurationValue[0] == 2) { indicatorStatus = "never" }
        logger("Indicator Light ${indicatorStatus}", "info")
        result << createEvent(name: "indicatorStatus", value: indicatorStatus, display: true)
      }
      break;
      case 4:
      logger("orientation ${cmd.scaledConfigurationValue}", "info")
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
      case 5:
      logger("lowest dimming value ${cmd.scaledConfigurationValue}", "info")
      break;
      case 7:
      logger("remote number of levels ${cmd.scaledConfigurationValue}", "info")
      break;
      case 8:
      logger("remote duration of each level ${cmd.scaledConfigurationValue}", "info")
      break;
      case 9:
      logger("number of levels ${cmd.scaledConfigurationValue}", "info")
      break;
      case 10:
      logger("duration of each level ${cmd.scaledConfigurationValue}", "info")
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
          color = "White";
          break;
          case 1:
          color = "Red";
          break;
          case 2:
          color = "Green"
          break;
          case 3:
          color = "Blue"
          break; 
          case 4:
          color = "Magenta"
          break;
          case 5:
          color = "Yellow"
          break;
          case 6:
          color = "Cyan"
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
        String color
        switch (cmd.configurationValue[0]) {
          case 0: // Off
          color = "Off";
          break;
          case 1:
          color = "Red";
          break;
          case 2:
          color = "Green"
          break;
          case 3:
          color = "Blue"
          break; 
          case 4:
          color = "Magenta"
          break;
          case 5:
          color = "Yellow"
          break;
          case 6:
          color = "Cyan"
          break; 
          case 7:
          color = "White"
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
      break
    }
  }

  updateDataValue("Configuration #${cmd.parameterNumber}", "${cmd.scaledConfigurationValue}")

  if (cmd.parameterNumber == 4) {
    if ( cmd.scaledConfigurationValue != invertSwitch) {
      Integer switch_value = invertSwitch ? 1 : 0
      cmds << zwave.configurationV1.configurationSet(configurationValue: [switch_value], parameterNumber: cmd.parameterNumber, size: 1).format()
      cmds << zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format()
    }

    result << createEvent(name: "invertedStatus", value: invertedStatus, display: true)
  } else if (cmd.parameterNumber == 7) {
    if ( cmd.scaledConfigurationValue != 1) {
      cmds << zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: cmd.parameterNumber, size: 1).format()
      cmds << zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format()
    }
  } else if (cmd.parameterNumber == 9) {
    if ( cmd.scaledConfigurationValue != 1) {
      cmds << zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: cmd.parameterNumber, size: 1).format()
      cmds << zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format()
    }
  } else if (cmd.parameterNumber == 8) {
    if ( cmd.scaledConfigurationValue != 3) {
      cmds << zwave.configurationV1.configurationSet(configurationValue: [0, 1], parameterNumber: cmd.parameterNumber, size: 2).format()
      cmds << zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format()
    }
  } else if (cmd.parameterNumber == 10) {
    if ( cmd.scaledConfigurationValue != 3) {
      cmds << zwave.configurationV1.configurationSet(configurationValue: [0, 3], parameterNumber: cmd.parameterNumber, size: 2).format()
      cmds << zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format()
    }
  }
  
  if (cmds) {
    result << response(delayBetween(cmds))
  }
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd, result) {
  logger("$cmd")

  updateDataValue("Configuration #${cmd.parameterNumber}", "${cmd.scaledConfigurationValue}")

  def cmds = []

  if (1) {
    switch (cmd.parameterNumber) {
      case 4:
      logger("orientation ${cmd.scaledConfigurationValue}", "info")
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
      case 7:
      logger("remote number of levels ${cmd.scaledConfigurationValue}", "info")
      break;
      case 8:
      logger("remote duration of each level ${cmd.scaledConfigurationValue}", "info")
      break;
      case 9:
      logger("number of levels ${cmd.scaledConfigurationValue}", "info")
      break;
      case 10:
      logger("duration of each level ${cmd.scaledConfigurationValue}", "info")
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
          color = "White";
          break;
          case 1:
          color = "Red";
          break;
          case 2:
          color = "Green"
          break;
          case 3:
          color = "Blue"
          break; 
          case 4:
          color = "Magenta"
          break;
          case 5:
          color = "Yellow"
          break;
          case 6:
          color = "Cyan"
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
        String color
        switch (cmd.configurationValue[0]) {
          case 0: // Off
          color = "Off";
          break;
          case 1:
          color = "Red";
          break;
          case 2:
          color = "Green"
          break;
          case 3:
          color = "Blue"
          break; 
          case 4:
          color = "Magenta"
          break;
          case 5:
          color = "Yellow"
          break;
          case 6:
          color = "Cyan"
          break; 
          case 7:
          color = "White"
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
      break
    }
  }

  updateDataValue("Configuration #${cmd.parameterNumber}", "${cmd.scaledConfigurationValue}")

  if (cmd.parameterNumber == 4) {
    if ( cmd.scaledConfigurationValue != invertSwitch) {
      Integer switch_value = invertSwitch ? 1 : 0
      cmds << zwave.configurationV1.configurationSet(configurationValue: [switch_value], parameterNumber: cmd.parameterNumber, size: 1).format()
      cmds << zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format()
    }

    result << createEvent(name: "invertedStatus", value: invertedStatus, display: true)
  } else if (cmd.parameterNumber == 7) {
    if ( cmd.scaledConfigurationValue != 1) {
      cmds << zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: cmd.parameterNumber, size: 1).format()
      cmds << zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format()
    }
  } else if (cmd.parameterNumber == 9) {
    if ( cmd.scaledConfigurationValue != 1) {
      cmds << zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: cmd.parameterNumber, size: 1).format()
      cmds << zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format()
    }
  } else if (cmd.parameterNumber == 8) {
    if ( cmd.scaledConfigurationValue != 3) {
      cmds << zwave.configurationV1.configurationSet(configurationValue: [0, 1], parameterNumber: cmd.parameterNumber, size: 2).format()
      cmds << zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format()
    }
  } else if (cmd.parameterNumber == 10) {
    if ( cmd.scaledConfigurationValue != 3) {
      cmds << zwave.configurationV1.configurationSet(configurationValue: [0, 3], parameterNumber: cmd.parameterNumber, size: 2).format()
      cmds << zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format()
    }
  }
  
  if (cmds) {
    result << response(delayBetween(cmds))
  }
}

def zwaveEvent(zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd, result) {
  logger("$cmd")

  updateDataValue("deviceIdData", "${cmd.deviceIdData}")
  updateDataValue("deviceIdDataFormat", "${cmd.deviceIdDataFormat}")
  updateDataValue("deviceIdDataLengthIndicator", "${cmd.deviceIdDataLengthIndicator}")
  updateDataValue("deviceIdType", "${cmd.deviceIdType}")

  if (cmd.deviceIdType == 1 && cmd.deviceIdDataFormat == 1) {//serial number in binary format
    String serialNumber = "h'"

    cmd.deviceIdData.each{ data ->
      serialNumber += "${String.format("%02X", data)}"
    }

    updateDataValue("serialNumber", serialNumber)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
  logger("$cmd")

  if ( cmd.manufacturerId == 0x000C ) {
    state.manufacturer= "HomeSeer"
  } else if ( cmd.manufacturerId == 0x0184 ) {
    state.manufacturer= "Dragon Tech Industrial, Ltd."
  } else {
    if ( cmd.manufacturerId == 0x0000 ) {
      cmd.manufacturerId = 0x0184
    }

    state.manufacturer= "Unknown Licensed Dragon Tech Industrial, Ltd."
  }

  String manufacturerCode = String.format("%04X", cmd.manufacturerId)
  String productTypeCode = String.format("%04X", cmd.productTypeId)
  String productCode = String.format("%04X", cmd.productId)

  String msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)
  updateDataValue("manufacturer", "${state.manufacturer}")

  Integer[] parameters = getConfigurationOptions(cmd.productId)

  def cmds = []
  parameters.each {
    cmds << zwave.configurationV1.configurationGet(parameterNumber: it).format()
  }

  result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
  result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
  result << createEvent(name: "ProductCode", value: productCode)
  if ( cmds.size ) {
    result << response(delayBetween(cmds, 1000))
  }
  result << response( zwave.versionV1.versionGet() )
}


def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd, result) {
  logger("$cmd")

  def versions = getCommandClassVersions()
  def version = versions[cmd.commandClass as Integer]
  def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
  def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
  if (encapsulatedCommand) {
    zwaveEvent(encapsulatedCommand, result)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelReport cmd, result) {
  logger("zwaveEvent(): Powerlevel Report received: ${cmd}")
  String device_power_level = (cmd.powerLevel > 0) ? "minus${cmd.powerLevel}dBm" : "NormalPower"
  logger("Powerlevel Report: Power: ${device_power_level}, Timeout: ${cmd.timeout}", "info")
  result << createEvent(name: "Power", value: device_power_level)
}

def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelTestNodeReport cmd, result) {
  logger("$cmd")
}

def testPowerLevel() {
  sendCommands([
    zwave.powerlevelV1.powerlevelGet(),
    zwave.powerlevelV1.powerlevelTestNodeSet(powerLevel: 0, testFrameCount: 20, testNodeid: zwaveHubNodeId ),
    zwave.powerlevelV1.powerlevelTestNodeGet(),
  ], 8000)
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd, result) {
  logger("$cmd")

  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  def zWaveProtocolVersion = "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  result << createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: true)
  result << createEvent(name: "zWaveProtocolVersion", value: "${zWaveProtocolVersion}", descriptionText: "${device.displayName} ${zWaveProtocolVersion}", isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelStartLevelChange cmd, result) {
  logger("$cmd")
  dimmerEvents(cmd.startLevel, true, result);
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelStopLevelChange cmd, result) {
  logger("$cmd")
  result << response(zwave.switchMultilevelV1.switchMultilevelGet())
}

def zwaveEvent(physicalgraph.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd, result) {
  logger("$cmd")
  String firmware_report = String.format("%s-%s-%s", cmd.checksum, cmd.firmwareId, cmd.manufacturerId)
  updateDataValue("FirmwareMdReport", firmware_report)
  result << createEvent(name: "FirmwareMdReport", value: firmware_report, descriptionText: "$device.displayName FIRMWARE_REPORT: $firmware_report", displayed: true, isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationBusy cmd, result) {
  logger("$cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationRejectedRequest cmd, result) {
  logger("$cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.applicationcapabilityv1.CommandCommandClassNotSupported cmd, result) {
  logger("$cmd")
}


def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  logger("command not implemented: $cmd", "error")
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd, result) {
  loggesr("$cmd")
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
  response(zwave.basicV1.basicSet(value: 0x63))
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

def on() {
  logger("on()")

  if (settings.enableDigitalButtons) { // Add option to have digital commands execute buttons
    buttonEvent(1, false, "digital")
  }

  delayBetween([
      // zwave.basicV1.basicSet(value: 0xFF).format(),
      zwave.switchMultilevelV1.switchMultilevelSet(value: 0xFF).format(),
      zwave.switchMultilevelV1.switchMultilevelGet().format(),
  ], 5000)
}

def off() {
  logger("off()")
  
  if (settings.enableDigitalButtons) { // Add option to have digital commands execute buttons
    buttonEvent(2, false, "digital")
  }
  
  if (settings.disbableDigitalOff) {
    logger("..off() disabled")
    return zwave.switchMultilevelV1.switchMultilevelGet().format();
  }

  def cmds = []
  if (settings.delayOff) {
    // cmds << zwave.versionV1.versionGet()
    // cmds << zwave.zwaveCmdClassV1.zwaveCmdNop()
    cmds << "delay 3000";
  }

  cmds << zwave.basicV1.basicSet(value: 0x00).format()
  cmds << "delay 1000"
  cmds << zwave.switchMultilevelV1.switchMultilevelGet().format()

  delayBetween( cmds )
}

def setLevel (provided_value) {
  def valueaux = provided_value as Integer
  logger("setLevel() value: $value")

  def level = (valueaux != 255) ? Math.max(Math.min(valueaux, 99), 0) : 99

  if (valueaux <= 1) { // Check for 1 because of bad device on setting non-existant indicator
    level = 0
  }

  // In the future establish if going from on to off
  Integer set_sceen = level ? 1 : 2
  // buttonEvent(set_sceen, false, "digital")
  //  zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 2, sceneId: 2).format(),

  buttonEvent((level == 0) ? 2 : 1, false, "digital")

  delayBetween ([
    // zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0, sceneId: 2).format(),
    zwave.switchMultilevelV1.switchMultilevelSet(value: level).format(),
    zwave.switchMultilevelV1.switchMultilevelGet().format(),
  ], 5000)
}

def setLevel(value, duration) {
  logger("setLevel( value: $value, duration: $duration )")

  /*
  def valueaux = value as Integer

  def level = Math.max(Math.min(valueaux, 99), 0)
  def dimmingDuration = duration < 128 ? duration : 128 + Math.round(duration / 60)
  def getStatusDelay = duration < 128 ? (duration*1000)+2000 : (Math.round(duration / 60)*60*1000)+2000
   */

  return setLevel(value);
}

/*
 *  Set dimmer to status mode, then set the color of the individual LED
 *
 *  led = 1-7
 *  color = 0=0ff
 *          1=red
 *          2=green
 *          3=blue
 *          4=magenta
 *          5=yellow
 *          6=cyan
 *          7=white
 */

def setBlinkDurationMilliseconds (newBlinkDuration) { 
  def cmds= []
  if ( 0 < newBlinkDuration && newBlinkDuration < 25500){
    log.debug "setting blink duration to: ${newBlinkDuration} ms"
    state.blinkDuration = newBlinkDuration.toInteger()/100
    log.debug "blink duration config parameter 30 is: ${state.blinkDuration}"
    cmds << zwave.configurationV2.configurationSet(configurationValue: [state.blinkDuration.toInteger()], parameterNumber: 30, size: 1).format()
  } else {
    log.debug "commanded blink duration ${newBlinkDuration} is outside range 0 .. 25500 ms"
  }
  return cmds
}

def setStatusLed (led,color,blink) {    
  def cmds= []

  if(state.statusled1==null) {      
    state.statusled1=0
    state.statusled2=0
    state.statusled3=0
    state.statusled4=0
    state.statusled5=0
    state.statusled6=0
    state.statusled7=0
    state.blinkval=0
  }

  /* set led # and color */
  switch(led) {
    case 1:
    state.statusled1=color
    break
    case 2:
    state.statusled2=color
    break
    case 3:
    state.statusled3=color
    break
    case 4:
    state.statusled4=color
    break
    case 5:
    state.statusled5=color
    break
    case 6:
    state.statusled6=color
    break
    case 7:
    state.statusled7=color
    break
    case 0:
    case 8:
    // Special case - all LED's
    state.statusled1=color
    state.statusled2=color
    state.statusled3=color
    state.statusled4=color
    state.statusled5=color
    state.statusled6=color
    state.statusled7=color
    break

  }

  if(state.statusled1==0 && state.statusled2==0 && state.statusled3==0 && state.statusled4==0 && state.statusled5==0 && state.statusled6==0 && state.statusled7==0)
  {
    // no LEDS are set, put back to NORMAL mode
    cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 13, size: 1).format()         
  }
  else
  {
    // at least one LED is set, put to status mode
    cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 13, size: 1).format()
  }

  if (led==8 | led==0) 
  {
    for (def ledToChange = 1; ledToChange <= 7; ledToChange++)
    {
      // set color for all LEDs
      cmds << zwave.configurationV2.configurationSet(configurationValue: [color], parameterNumber: ledToChange+20, size: 1).format()
    }
  }
  else
  {
    // set color for specified LED
    cmds << zwave.configurationV2.configurationSet(configurationValue: [color], parameterNumber: led+20, size: 1).format()
  }   

  // check if LED should be blinking
  def blinkval = state.blinkval

  if(blink) {
    switch(led) {
      case 1:
      blinkval = blinkval | 0x1
      break
      case 2:
      blinkval = blinkval | 0x2
      break
      case 3:
      blinkval = blinkval | 0x4
      break
      case 4:
      blinkval = blinkval | 0x8
      break
      case 5:
      blinkval = blinkval | 0x10
      break
      case 6:
      blinkval = blinkval | 0x20
      break
      case 7:
      blinkval = blinkval | 0x40
      break
      case 0:
      case 8:
      blinkval = 0x7F
      break
    }
    cmds << zwave.configurationV2.configurationSet(configurationValue: [blinkval], parameterNumber: 31, size: 1).format()
    state.blinkval = blinkval
    // set blink frequency if not already set, 5=500ms
    if(state.blinkDuration == null | state.blinkDuration < 0 | state.blinkDuration > 255) {
      cmds << zwave.configurationV2.configurationSet(configurationValue: [5], parameterNumber: 30, size: 1).format()
    }
  } else {

    switch(led) {
      case 1:
      blinkval = blinkval & 0xFE
      break
      case 2:
      blinkval = blinkval & 0xFD
      break
      case 3:
      blinkval = blinkval & 0xFB
      break
      case 4:
      blinkval = blinkval & 0xF7
      break
      case 5:
      blinkval = blinkval & 0xEF
      break
      case 6:
      blinkval = blinkval & 0xDF
      break
      case 7:
      blinkval = blinkval & 0xBF
      break
      case 0:  
      case 8:
      blinkval = 0
      break         
    }
    cmds << zwave.configurationV2.configurationSet(configurationValue: [blinkval], parameterNumber: 31, size: 1).format()
    state.blinkval = blinkval
  }     
  delayBetween(cmds, 150)
}

/*
 * Set Dimmer to Normal dimming mode (exit status mode)
 *
 */
def setSwitchModeNormal() {
  def cmds= []
  cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 13, size: 1).format()
  delayBetween(cmds, 500)
}

/*
 * Set Dimmer to Status mode (exit normal mode)
 *
 */
def setSwitchModeStatus() {
  def cmds= []
  cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 13, size: 1).format()
  delayBetween(cmds, 500)
}

/*
 * Set the color of the LEDS for normal dimming mode, shows the current dim level
 */
def setDefaultColor(color) {
  def cmds= []
  cmds << zwave.configurationV2.configurationSet(configurationValue: [color], parameterNumber: 14, size: 1).format()
  delayBetween(cmds, 500)
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 **/
def ping() {
  logger("ping()")
  zwave.switchMultilevelV1.switchMultilevelGet().format()
}

def refresh() {
  logger("refresh()")
  zwave.switchMultilevelV1.switchMultilevelGet().format()
}

def poll() {
  logger("poll()")
  if (0) {
    zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: 0).format()
  }

  zwave.switchMultilevelV1.switchMultilevelGet().format()
}

def indicatorWhenOn() {
	sendEvent(name: "indicatorStatus", value: "when on", display: false)
	zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 3, size: 1).format()
}

def indicatorWhenOff() {
	sendEvent(name: "indicatorStatus", value: "when off", display: false)
	zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 3, size: 1).format()
}

def indicatorNever() {
	sendEvent(name: "indicatorStatus", value: "never", display: false)
	zwave.configurationV1.configurationSet(configurationValue: [2], parameterNumber: 3, size: 1).format()
}

def zwaveEvent(physicalgraph.zwave.commands.centralscenev1.CentralSceneSupportedReport cmd, result) {
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

def zwaveEvent(physicalgraph.zwave.commands.centralscenev1.CentralSceneNotification cmd, result) {
  logger("$cmd")

  if (0) {
    long currenTime = Calendar.getInstance().getTimeInMillis()
    logger("Lasttime ${state.lastSequenceBounce} $currenTime")
    if ( state.lastSequenceBounce ) {
      if (0 && currenTime - state.lastSequenceBounce < 1500 ) {
        state.sequenceNumber= cmd.sequenceNumber
        logger("Bounce, too soon to update.", "warn")
        return
      }
      if ( state.sequenceNumber && cmd.sequenceNumber > 1 && ( cmd.sequenceNumber < state.sequenceNumber ) ) {
        logger("Late sequenceNumber ${cmd.sequenceNumber} < ${state.sequenceNumber}", "warn")
        return
      }
    }
    state.sequenceNumber= cmd.sequenceNumber
    state.lastSequenceBounce = Calendar.getInstance().getTimeInMillis()
  }

  def cmds = []

  switch (cmd.sceneNumber) {
    case 1:
    switch (cmd.keyAttributes) {
      case 2:
      cmds << "delay 2000"
      case 0:
      result << createEvent(name: "switch", value: "on", type: "physical", isStateChange: true, displayed: true)
      cmds << zwave.switchMultilevelV1.switchMultilevelGet().format()
      buttonEvent(cmd.sceneNumber, cmd.keyAttributes == 0 ? false : true, "physical")
      case 1:
      break;
      case 3:
      // 2 Times
      buttonEvent(3, false, "physical")
      cmds << zwave.switchMultilevelV1.switchMultilevelSet(value: 99).format()
      cmds << zwave.switchMultilevelV1.switchMultilevelGet().format()
      break;
      case 4: // 3 Three times
      buttonEvent(5, false, "physical")
      break;
      default:
      logger ("unexpected up press keyAttribute: $cmd", "error")
      break
    }
    break

    case 2: // Down
    switch (cmd.keyAttributes) {
      case 2:
      cmds << "delay 2000"
      case 0:
      result << createEvent(name: "switch", value: "off", type: "physical", isStateChange: true, displayed: true)
      cmds << zwave.switchMultilevelV1.switchMultilevelGet().format()
      // cmds << zwave.basicV1.basicGet().format()
      buttonEvent(cmd.sceneNumber, cmd.keyAttributes == 0 ? false : true, "physical")
      case 1:
      break;
      case 3: // 2 Times
      buttonEvent(4, false, "physical")
      cmds << zwave.basicV1.basicSet(value: 0x00).format()
      cmds << zwave.basicV1.basicGet().format()
      break;
      case 4: // 3 Three times
      buttonEvent(6, false, "physical")
      break;
      default:
      logger ("unexpected down press keyAttribute: $cmd", "error")
    }
    break

    default:
    // unexpected case
    logger ("unexpected scene: $cmd.sceneNumber", "error")
  }

  if (cmds.size) {
    result << response(delayBetween(cmds))
  }

  return result
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd, result) {
  logger("$cmd")

  def cmds = []
  if (cmd.supportedGroupings) {
    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x).format()
      cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: true, refreshCache: true).format()
      cmds << zwave.associationGrpInfoV1.associationGroupCommandListGet(groupingIdentifier: x, allowCache: false).format()
    }

    result << response(delayBetween(cmds, 2000))

    return
  }

  logger("AssociationGroupingsReport: $cmd", "error")
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationSpecificGroupReport cmd, result) {
  log.debug("$cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupCommandListReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd, result) {
  logger("$cmd")

  def name = new String(cmd.name as byte[])
  logger("Association Group #${cmd.groupingIdentifier} has name: ${name}", "info")

  result << createEvent(name: "Group #${cmd.groupingIdentifier}", value: "${name}", isStateChange: true)
  updateDataValue("Group #${cmd.groupingIdentifier}", "${name}")

  result << response(delayBetween([
    zwave.associationV1.associationGet(groupingIdentifier: cmd.groupingIdentifier).format(),
  ]))
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$cmd")

  if (cmd.groupingIdentifier > 2) {
    logger("Unknown Group Identifier", "error");
    return
  }

  // Lifeline
  def string_of_assoc = ""
  cmd.nodeId.each {
    string_of_assoc += "${it}, "
  }
  Integer lengthMinus2 = ( string_of_assoc.length() > 3 ) ? string_of_assoc.length() - 3 : 0
  String final_string = lengthMinus2 ? string_of_assoc.getAt(0..lengthMinus2) : string_of_assoc

  if (cmd.nodeId.any { it == zwaveHubNodeId }) {
    state.isAssociated = true
  } else {
    if (cmd.groupingIdentifier == 1) {
      state.isAssociated = false
      result << response( zwave.associationV1.associationSet(groupingIdentifier: cmd.groupingIdentifier, nodeId: [zwaveHubNodeId]) )
    }
  }


  String group_name = ""
  switch (cmd.groupingIdentifier) {
    case 1:
    group_name = "Lifeline"
      break;
    case 2:
    group_name = "On/Off/Dimming control"
    break;
    default:
    group_name = "Unknown";
    break;
  }

  updateDataValue("$group_name", "${final_string}")
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd, result) {
  logger("$cmd")
  result << createEvent(name: "DeviceReset", value: "true", descriptionText: cmd.toString(), isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd, result) {
  logger("$cmd")
  result << createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def prepDevice() {
  [
    zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
    zwave.firmwareUpdateMdV2.firmwareMdGet(),
    zwave.associationV2.associationGroupingsGet(),
    zwave.centralSceneV1.centralSceneSupportedGet(),
    zwave.switchAllV1.switchAllGet(),
    zwave.powerlevelV1.powerlevelGet(),
    zwave.switchMultilevelV1.switchMultilevelGet(),
    zwave.configurationV1.configurationGet(parameterNumber: 0x00),
  ]
}

def installed() {
  log.debug "installed()"
  setTrace(true)
  /*

  def zwInfo = getZwaveInfo()
  if ($zwInfo) {
  log.debug("$zwInfo")
  sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
  }
   */

  // Device-Watch simply pings if no device events received for 32min(checkInterval)
  // sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID]) //, offlinePingable: "1"])

  // Set Button Number and driver version
  sendEvent(name: "numberOfButtons", value: 6, displayed: false)
  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  sendCommands( prepDevice() + setDimRatePrefs(), 2000 )
}

def setDimRatePrefs() {
  def cmds = []
  if (remoteStepSize) {
  def remoteStepSize = Math.max(Math.min(remoteStepSize, 99), 1)
  cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: remoteStepSize, parameterNumber: 7, size: 1)
  }

  if (remoteStepDuration) {
  def remoteStepDuration = Math.max(Math.min(remoteStepDuration, 255), 1)
  cmds << zwave.configurationV1.configurationSet(configurationValue: [0, remoteStepDuration], parameterNumber: 8, size: 2)
  }


  if (localStepSize) {
  def localStepSize = Math.max(Math.min(localStepSize, 99), 1)
  cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: localStepSize, parameterNumber: 9, size: 1)
  }

  if (localStepDuration) {
  def localStepDuration = Math.max(Math.min(localStepDuration, 255), 1)
  cmds << zwave.configurationV1.configurationSet(configurationValue: [0, localStepDuration], parameterNumber: 10, size: 2)
  }
  return cmds
}

def updated() {
  if ( state.updatedDate && ((Calendar.getInstance().getTimeInMillis() - state.updatedDate)) < 5000 ) {
    return
  }
  logger("updated() debug: ${settings.debugLevel}")

  setTrace(true)

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)

  // Set Button Number and driver version
  sendEvent(name: "numberOfButtons", value: 6, displayed: false)
  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  sendEvent(name: "DeviceReset", value: "false", descriptionText: cmd.toString(), isStateChange: true, displayed: true)

  // Device-Watch simply pings if no device events received for 32min(checkInterval)
  // sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])

  // sendCommands( prepDevice() + setDimRatePrefs(), 2000 )
  sendCommands( prepDevice(), 2000 )
  
  if (0) {
    def zwInfo = getZwaveInfo()
    if ($zwInfo) {
      log.debug("$zwInfo")
      sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
    }
  }

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

def setTrace(Boolean enable) {
  state.isTrace = enable
}

Boolean isTraceEnabled() {
  Boolean is_trace = state.isTrace ?: false
  return is_trace
}

def followupTraceDisable() {
  logger("followupTraceDisable()")

  setTrace(false)
}

private logger(msg, level = "trace") {
  String device_name = "$device.displayName"
  String msg_text = (msg != null) ? "${msg}" : "<null>"

  Integer log_level =  isTraceEnabled() ? 5 : 2

  switch(level) {
    case "warn":
    if (log_level >= 2) {
      log.warn "$device_name ${msg_text}"
    }
    sendEvent(name: "logMessage", value: "${msg_text}", isStateChange: true)
    break;

    case "info":
    if (log_level >= 3) {
      log.info "$device_name ${msg_text}"
    }
    break;

    case "debug":
    if (log_level >= 4) {
      log.debug "$device_name ${msg_text}"
    }
    break;

    case "trace":
    if (log_level >= 5) {
      log.trace "$device_name ${msg_text}"
    }
    break;

    case "error":
    default:
    log.error "$device_name ${msg_text}"
    sendEvent(name: "lastError", value: "${msg_text}", isStateChange: true)
    break;
  }
}
