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

String getDriverVersion () {
  return "v6.95"
}

def getConfigurationOptions(Integer model) {
  if ( model == 12341 ) {
    return [ 13, 14, 21, 3, 31, 4 ]
  }
  return [ 3, 4 ]
}

metadata {
  definition (name: "WS-100 Switch", namespace: "TangentOrgThings", author: "brian@tangent.org", ocfDeviceType: "oic.d.switch") {
    capability "Actuator"
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
    attribute "parseErrorCount", "number"        // Last error message
    attribute "unknownCommandErrorCount", "number"        // Last error message

    attribute "invertedState", "enum", ["false", "true"]

    attribute "Lifeline", "string"

    attribute "Group 1", "string"
    attribute "Group 2", "string"
    attribute "Group 3", "string"
    attribute "Group 4", "string"

    attribute "driverVersion", "string"
    attribute "firmwareVersion", "string"
    attribute "zWaveProtocolVersion", "string"
    attribute "FirmwareMdReport", "string"
    attribute "Manufacturer", "string"
    attribute "ManufacturerCode", "string"
    attribute "MSR", "string"
    attribute "NIF", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"

    attribute "keyAttributes", "number"

    attribute "Scene", "number"
    attribute "Scene_1", "number"
    attribute "Scene_1_Duration", "number"
    attribute "Scene_2", "number"
    attribute "Scene_2_Duration", "number"

    attribute "SwitchAll", "string"
    attribute "Power", "string"
    
    attribute "statusMode", "enum", ["default", "status"]
    attribute "defaultLEDColor", "enum", ["White", "Red", "Green", "Blue", "Magenta", "Yellow", "Cyan"]    
    attribute "statusLEDColor", "enum", ["Off", "Red", "Green", "Blue", "Magenta", "Yellow", "Cyan", "White"]
    attribute "blinkFrequency", "number" 
    
    command "connect"
    command "disconnect"

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
    input name: "ledIndicator", type: "enum", title: "LED Indicator", description: "Turn LED indicator... ", required: false, options: ["When Off", "When On", "Never"]
    input name: "invertSwitch", type: "bool", title: "Invert Switch", description: "If you oopsed the switch... ", required: false,  defaultValue: false
    input name: "disbableDigitalOff", type: "bool", title: "Disable Digital Off", description: "Disallow digital turn off", required: false
    input name: "delayOff", type: "bool", title: "Delay Off", description: "Delay Off for three seconds", required: false
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false,  defaultValue: 3
  }

  tiles(scale: 2) {
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
      tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label: '${name}', action: "disconnect", icon: "st.switches.switch.on", backgroundColor: "#00A0DC"
        attributeState "off", label: '${name}', action: "connect", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
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

    standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
    }

    valueTile("driverVersion", "device.driverVersion", width: 2, height: 2, inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }

    standardTile("reset", "device.DeviceReset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "false", label:'', backgroundColor:"#ffffff"
      state "true", label:'reset', backgroundColor:"#e51426"
    }

    main "switch"
    details(["switch", "scene", "driverVersion", "refresh", "reset"])
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
    // 0x5E: 2, //
    // 0x6C: 2, // Supervision
    0x70: 1,  // Configuration
    0x72: 2,  // Manufacturer Specific
    0x73: 1,  // Powerlevel
    0x7A: 2,  // Firmware Update Md HS-200 V4
    0x85: 2,  // Association  0x85  V1 V2    
    0x86: 1,  // Version
    // 0x55: 1,  // Transport Service Command Class
    // 0x9F: 1,  // Security 2 Command Class  
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

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$device.displayName $cmd")
  switchEvents(cmd.value, true, result);
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$device.displayName $cmd -- BEING CONTROLLED")
  if (cmd.value) {
    trueOn(false)
    return
  }

  trueOff(false)
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, result) {
  logger("$device.displayName $cmd")
  switchEvents(cmd.value, false, result);
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinarySet cmd, result) {
  logger("$device.displayName $cmd -- BEING CONTROLLED")
  if (cmd.switchValue) {
    trueOn(false)
    return
  }

  trueOff(false)
}

def zwaveEvent(physicalgraph.zwave.commands.securitypanelmodev1.SecurityPanelModeSupportedGet cmd, result) {
  result << response(zwave.securityPanelModeV1.securityPanelModeSupportedReport(supportedModeBitMask: 0))
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

  String scene_name = "Scene_$cmd.sceneId"
  String scene_duration_name = String.format("Scene_%d_Duration", cmd.sceneId)

  result << createEvent(name: "$scene_name", value: cmd.level, isStateChange: true, displayed: true)
  result << createEvent(name: "$scene_duration_name", value: cmd.dimmingDuration, isStateChange: true, displayed: true)
  result << createEvent(name: "Scene", value: cmd.sceneId, isStateChange: true, displayed: true)
  if (cmds.size()) {
    result << response(delayBetween(cmds, 1000))
  }
}

/*
def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd, result) {
  log.debug("$device.displayName $cmd")
  Integer set_sceen = ((cmd.sceneId + 1) / 2) as Integer
  buttonEvent("SceneActivationSet()", set_sceen, false, "digital")
  [ createEvent(name: "setScene", value: "Setting", isStateChange: true, displayed: true) ]
}
*/

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd, result) {
  logger("$device.displayName $cmd")

  switch (cmd.parameterNumber) {
    case 3:
    if (1) {
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
    }
    break;
    case 4:
    if (1) {
      if ( cmd.configurationValue[0] != invertSwitch) {
        return response( [
          zwave.configurationV1.configurationSet(scaledConfigurationValue: invertSwitch ? 1 : 0, parameterNumber: cmd.parameterNumber, size: 1).format(),
          zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format(),
        ])
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
    logger("$device.displayName has unknown configuration parameter $cmd.parameterNumber : $cmd.configurationValue[0]", "error")
    break;
  }
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
  logger("zwaveEvent(): Powerlevel Report received: ${cmd}")
  String device_power_level = (cmd.powerLevel > 0) ? "minus${cmd.powerLevel}dBm" : "NormalPower"
  logger("Powerlevel Report: Power: ${device_power_level}, Timeout: ${cmd.timeout}", "info")
  result << createEvent(name: "Power", value: device_power_level)
}

def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelTestNodeReport cmd, result) {
  logger("$device.displayName $cmd")
  result << response( zwave.commands.powerlevelv1.PowerlevelGet() )
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd, result) {
  logger("$device.displayName $cmd")

  String text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  String zWaveProtocolVersion = "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  result << createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: true)
  result << createEvent(name: "zWaveProtocolVersion", value: "${zWaveProtocolVersion}", descriptionText: "${device.displayName} ${zWaveProtocolVersion}", isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd, result) {
  logger("$device.displayName $cmd")
  String firmware_report = String.format("%s-%s-%s", cmd.checksum, cmd.firmwareId, cmd.manufacturerId)
  updateDataValue("FirmwareMdReport", firmware_report)
  result << createEvent(name: "FirmwareMdReport", value: firmware_report, descriptionText: "$device.displayName FIRMWARE_REPORT: $firmware_report", displayed: true, isStateChange: true)
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
  if (state.lastOnBounce && (Calendar.getInstance().getTimeInMillis() - state.lastOnBounce) < 2000 ) {
    logger("$device.displayName bounce", "warn")
    return
  }
  state.lastOnBounce = Calendar.getInstance().getTimeInMillis()

  if (physical) { // Add option to have digital commands execute buttons
    buttonEvent("on()", 1, false, "digital")
  }
  
  String active_time = new Date(state.lastOnBounce).format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  sendEvent(name: "lastActive", value: active_time, isStateChange: true);
  sendEvent(name: "switch", value: "on", isStateChange: true);

  delayBetween([
    // zwave.switchBinaryV1.switchBinarySet(switchValue: 0xFF).format(),
    zwave.basicV1.basicSet(value: 0xFF).format(),
    zwave.switchBinaryV1.switchBinaryGet().format(),
  ], 5000)
}

def off() {
  logger("$device.displayName off()")

  if (settings.disbableDigitalOff) {
    logger("..off() disabled")
    return zwave.switchBinaryV1.switchBinaryGet().format();
  }
  
  trueOff(false)
}

def disconnect() {
  logger("$device.displayName disconnect()") 
  trueOff(true)
}

private trueOff(Boolean physical = true) {
  if (state.lastOffBounce && (Calendar.getInstance().getTimeInMillis() - state.lastOffBounce) < 2000 ) {
    logger("$device.displayName bounce", "warn")
    return
  }
  state.lastOffBounce = Calendar.getInstance().getTimeInMillis()
  
  if (physical) { // Add option to have digital commands execute buttons
    buttonEvent("off()", 2, false, "digital")
  }

  String active_time = new Date(state.lastOffBounce).format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  sendEvent(name: "lastActive", value: active_time, isStateChange: true);
  sendEvent(name: "switch", value: "off", isStateChange: true);
  def cmds = []
  if (settings.delayOff) {
    // cmds << zwave.versionV1.versionGet()
    // cmds << zwave.zwaveCmdClassV1.zwaveCmdNop()
    cmds << "delay 3000";
  }

  // cmds << zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0xff, sceneId: 2).format();
  // cmds << physical ? zwave.basicV1.basicSet(value: 0x00).format() : zwave.switchBinaryV1.switchBinarySet(switchValue: 0x00).format();
  //cmds << zwave.switchBinaryV1.switchBinarySet(switchValue: 0x00).format()
  cmds << zwave.basicV1.basicSet(value: 0x00).format()
  cmds << "delay 5000"
  cmds << zwave.switchBinaryV1.switchBinaryGet().format()

  delayBetween( cmds ) //, settings.delayOff ? 3000 : 600 )
  // sendCommands(cmds)
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
    // zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0xFF, sceneId: 0).format(),
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

  result << sendCommands(cmds)
}

def zwaveEvent(physicalgraph.zwave.commands.centralscenev1.CentralSceneNotification cmd, result) {
  log.debug("$device.displayName $cmd")

  if ( cmd.sequenceNumber > 1 && cmd.sequenceNumber < state.sequenceNumber ) {
    logger(descriptionText: "Late sequenceNumber  $cmd", "error")
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
      log.error ("unexpected up press keyAttribute: $cmd")
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
    cmds << "delay 2000"
    cmds << zwave.switchBinaryV1.switchBinaryGet().format();
  }

  if (cmds.size) {
    result << response(delayBetween(cmds))
  }
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd, result) {
  logger("$device.displayName $cmd")

  state.groups = cmd.supportedGroupings

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

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd, result) {
  logger("$device.displayName $cmd")

  def name = new String(cmd.name as byte[])
  logger("Association Group #${cmd.groupingIdentifier} has name: ${name}", "info")

  result << createEvent(name: "Group #${cmd.groupingIdentifier}", value: "${name}", isStateChange: true)
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

  Boolean isStateChange
  String event_value
  String event_descriptionText

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

  if (cmd.nodeId.any { it == zwaveHubNodeId }) {
    isStateChange = state.isAssociated == true ? false : true
    event_descriptionText = "Device is associated"
    if (cmd.groupingIdentifier == 1) {
      state.isAssociated = true
    }
  } else {
    isStateChange = state.isAssociated == false ? false : true
    event_descriptionText = "Hub was not found in lifeline"
    if (cmd.groupingIdentifier == 1) {
      state.isAssociated = false
    }

    result << response( zwave.associationV1.associationSet(groupingIdentifier: cmd.groupingIdentifier, nodeId: zwaveHubNodeId) )
  }

  String group_name = getDataValue("Group #${cmd.groupingIdentifier}")

  if (group_name) {
    sendEvent(name: "$group_name",
        value: event_value,
        descriptionText: event_descriptionText,
        displayed: true,
        isStateChange: true) // isStateChange)
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
  logger("$device.displayName installed()")
  sendEvent(name: "numberOfButtons", value: 6, displayed: false)

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

  sendEvent(name: "numberOfButtons", value: 6, displayed: true, isStateChange: true)

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
