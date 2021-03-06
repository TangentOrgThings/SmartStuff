// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :
/**
 *  Copyright 2017-2018 Brian Aker <brian@tangent.org>
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

String getDriverVersion() {
  return "v4.75"
}

def getIndicatorParam() {
  return state.indicatorParam
}

def setIndicatorParam(Integer set_param) {
  state.indicatorParam = set_param
}

metadata {
  definition (name: "Z-Wave Receptacle", namespace: "TangentOrgThings", author: "brian@tangent.org", ocfDeviceType: "oic.d.switch") {
    capability "Actuator"
    capability "Button"
    capability "Energy Meter"
    capability "Indicator"
    capability "Outlet"
    capability "Polling"
    capability "Power Meter"
    capability "Refresh"
    capability "Sensor"
    capability "Switch"

    command "reset"

    attribute "DeviceReset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message
    attribute "parseErrorCount", "number"        // Last error message
    attribute "unknownCommandErrorCount", "number"        // Last error message

    attribute "driverVersion", "string"

    attribute "firmwareVersion", "string"
    attribute "zWaveProtocolVersion", "string"

    attribute "FirmwareMdReport", "string"

    attribute "MSR", "string"
    attribute "Manufacturer", "string"
    attribute "ManufacturerCode", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"

    attribute "Lifeline", "string"
    attribute "BasicSet control", "string"
    attribute "Double Tap", "string"

    attribute "NIF", "string"

    attribute "keyAttributes", "number"

    attribute "Scene", "number"
    attribute "Scene_1", "number"
    attribute "Scene_1_Duration", "number"
    attribute "Scene_2", "number"
    attribute "Scene_2_Duration", "number"

    attribute "SwitchAll", "string"
    attribute "Power", "string"
    attribute "Protection", "string"

    command "connect"
    command "disconnect"
    
     /*
		manufacturerId value="011a"
		productType value="0101"
		productId value="0103"
    */
    fingerprint type: "1001", mfr: "011A", prod: "0101", model: "0103", deviceJoinName: "ZW15R"

    /* Original Enerwave
      vendorId: 282 (22:49)
      productId: 1539 (22:49)
      productType: 257 (22:49)
      manufacturerId="011a"
      productType value="0101"
		productId value="0603"
    */
    fingerprint type: "1001", mfr: "011A", prod: "0101", model: "0603", deviceJoinName: "Enerwave ZW15R"
    fingerprint type: "1001", mfr: "011A", prod: "0101", model: "0103", deviceJoinName: "Enerwave ZW15R"
    fingerprint type: "1001", mfr: "011A", prod: "1516", model: "3545", deviceJoinName: "Enerwave ZW15R"
    fingerprint type: "1001", mfr: "011A", prod: "0111", model: "0105", deviceJoinName: "Enerwave ZW15RM Receptacle"

    /*
      manufacturerId value="0063
      productType value="4952"
      productId value="3031"
    */
    fingerprint mfr:"0063", prod: "4952", model:"3031", deviceJoinName: "Jasco/GE 12721 In-Wall Duplex Receptacle" //, cc: "0x20, 0x25, 0x27, 0x70, 0x72, 0x73, 0x75, 0x77, 0x86" // OUTLET has CC PROTECTION
    

    /* 14288
      Z-Wave Product ID: 0x3134
      Product Type: 0x4952
      */
    fingerprint type: "1001", mfr: "0063", prod: "4952", model: "3134", deviceJoinName: "GE 14288/ZW1002"

    /*
      vendorId: 388 (26.06.2017)
      vendor: (26.06.2017)
      productId: 12337 (26.06.2017)
      productType: 17479 (26.06.2017)
    */
    fingerprint type: "1001", mfr: "0184", prod: "4447", model: "3031", deviceJoinName: "PA-100"
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
    input name: "ledIndicator", type: "enum", title: "LED Indicator", description: "Turn LED indicator... ", required: false, options:["on": "When On", "off": "When Off", "never": "Never"]
    input name: "disbableDigitalOff", type: "bool", title: "Disable Digital Off", description: "Disallow digital turn off", required: false
    input name: "delayOff", type: "bool", title: "Delay Off", description: "Delay Off for three seconds", required: false
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  }

  // tile definitions
  tiles(scale: 2) {
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
      tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label: '${name}', action: "disconnect", icon: "st.switches.switch.on", backgroundColor: "#79b821"
        attributeState "off", label: '${name}', action: "connect", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
      }
      tileAttribute ("device.power", key: "SECONDARY_CONTROL") {
        attributeState "power", label:'${currentValue}', unit: "W", icon: "st.Appliances.appliances17"
      }
    }

    standardTile("indicator", "device.indicatorStatus", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "when off", action:"indicator.indicatorWhenOn", icon:"st.indicators.lit-when-off"
      state "when on", action:"indicator.indicatorNever", icon:"st.indicators.lit-when-on"
      state "never", action:"indicator.indicatorWhenOff", icon:"st.indicators.never-lit"
    }

    valueTile("driverVersion", "device.driverVersion", width:2, height: 2) {
      state "val", label: '${currentValue}', defaultState: true
    }

    valueTile("scene", "device.Scene", width:2, height: 2, decoration: "flat", inactiveLabel: false) {
      state "default", label: '${currentValue}'
    }

    valueTile("energy", "device.energy", decoration: "flat") {
      state "default", label:'${currentValue} kWh'
    }

    standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat") {
      state "default", label:'reset kWh', action:"reset"
    }

    standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
    }

    main "switch"
    details(["switch", "indicator", "driverVersion", "energy", "reset", "refresh"])
  }
}

// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :
def getCommandClassVersions() {
  String msr = state.MSR ?: device.currentValue("MSR")
  
  switch (msr) {
    case "011A-0101-0603":
    return [
    0x20: 1,  // Basic
    0x25: 1,  // Switch Binary
    0x27: 1,  // Switch All
    0x70: 2,  // Configuration V1
    0x72: 2,  // Manufacturer Specific ManufacturerSpecificReport V1
    0x86: 1,  // Version
    ]
    break;
    case "011A-0101-0103":
    return [
    0x20: 1,  // Basic
    0x25: 1,  // Switch Binary
    0x27: 1,  // Switch All
    0x70: 2,  // Configuration V1
    0x72: 2,  // Manufacturer Specific ManufacturerSpecificReport V1
    0x85: 2,  // Association  0x85  V1
    0x86: 1,  // Version
    ]
    break;
    case "0063-4952-3133": // Alcove Lamp    
    case "0184-4447-3031": // PA-110 aka Dragon
    return [
    0x20: 1,  // Basic
    0x25: 1,  // Switch Binary
    0x27: 1,  // Switch All
    0x59: 1,  // Association Grp Info
    0x5A: 1,  // Device Reset Locally
    // 0x5E: 2,  //
    0x70: 2,  // Configuration V1
    0x72: 2,  // Manufacturer Specific ManufacturerSpecificReport
    0x7A: 2,  // Firmware Update Md
    0x85: 2,  // Association  0x85  V1 V2
    0x86: 1,  // Version
    ]
    break;
    case "0063-4952-3031":  // 25,27,75,73,70,86,72,77
        return [
    0x20: 1,  // Basic
    0x25: 1,  // Switch Binary
    0x27: 1,  // Switch All
    0x70: 2,  // Configuration V1
    0x72: 2,  // Manufacturer Specific V1
    0x73: 1,  //
    0x75: 1,  //
    0x77: 1,  //
    0x86: 1,  // Version
    ]
    default:
    return [
    0x20: 1,  // Basic
    0x25: 1,  // Switch Binary
    0x27: 1,  // Switch All
    0x2B: 1,  // SceneActivation
    0x2C: 1,  // Scene Actuator Conf
    0x32: 2,  // Meter
    0x56: 1,  // Crc16Encap
    0x59: 1,  // Association Grp Info
    0x5A: 1,  // Device Reset Locally
    0x70: 2,  // Configuration
    0x72: 2,  // Manufacturer Specific ManufacturerSpecificReport
    0x7A: 2,  // Firmware Update Md
    0x85: 2,  // Association  0x85  V1 V2
    0x86: 1,  // Version
    ]
    break;
  }
}

def parse(String description) {
  def result = []

  if (description && description.startsWith("Err")) {
    logger("parse error: ${description}", "error")

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
        logger("TRYING parse() ${cmd}", "warn")
        zwaveEvent(cmd, result)
      } else {
        logger("zwave.parse() failed for: ${description}", "error")
      }
    }
  }

  return result
}

def zwaveEvent(physicalgraph.zwave.commands.switchtogglebinaryv1.SwitchToggleBinaryReport cmd, result) {
  logger("$device.displayName $cmd")

  result << createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical")
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

// This should not ever be called, but it is
def zwaveEvent(physicalgraph.zwave.commands.thermostatfanmodev1.ThermostatFanModeGet cmd, result) {
  logger("$device.displayName $cmd")
  result << response(delayBetween([
    physicalgraph.zwave.commands.thermostatfanmodev1.ThermostatFanModeReport(0x00, 0x2),
  ]))
}

// NotificationReport
def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd, result) {
  logger("$device.displayName $cmd")

  if (cmd.notificationType == 7) {
    Boolean current_status = cmd.notificationStatus == 255 ? true : false

    switch (cmd.event) {
      case 8:
      sendCommands([
        zwave.switchBinaryV1.switchBinarySet(switchValue: 0xFF),
        zwave.switchBinaryV1.switchBinaryGet(),
      ]) 
      break;
      case 0:
      sendCommands([
        zwave.switchBinaryV1.switchBinarySet(switchValue: 0xFF),
        zwave.switchBinaryV1.switchBinaryGet(),
      ]) 
      break;
      default:
      logger("$device.displayName unknown event for notification 7: $cmd", "error")
    }
  }
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
    result << createEvent(name: "Scene", value: cmd.sceneId, isStateChange: true, displayed: true)
    return
  }

  def cmds = []

  if (cmd.sceneId == 1) {
    if (cmd.level != 255) {
      cmds << zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: cmd.sceneId, dimmingDuration: 0xFF, level: 255, override: true).format()
    }
  } else if (cmd.sceneId == 2) {
    if (cmd.level) {
      cmds << zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: cmd.sceneId, dimmingDuration: 0xFF, level: 0, override: true).format()
    }
  }

  String scene_name = "Scene_$cmd.sceneId"
  String scene_duration_name = String.format("Scene_%d_Duration", cmd.sceneId)

  result << createEvent(name: "$scene_name", value: cmd.level, isStateChange: true, displayed: true)
  result << createEvent(name: "$scene_duration_name", value: cmd.dimmingDuration, isStateChange: true, displayed: true)

  if (cmds) {
    result << response(delayBetween(cmds, 1000))
  }

  return result
}

/*
def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd, result) {
  logger("$device.displayName $cmd")
  buttonEvent(cmd.sceneId, false, "digital")
} */

def zwaveEvent(physicalgraph.zwave.commands.meterv2.MeterReport cmd, result) {
  logger("$device.displayName: $cmd");

  state.hasMeter = true

  if (cmd.meterType != 1) {
    logger("$device.displayName bad type: $cmd", "error")
    return
  }

  if (cmd.scale == 0) {
    result << createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kWh", displayed: true, isStateChange: true)
  } else if (cmd.scale == 1) {
    result << createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kVAh", displayed: true, isStateChange: true)
  } else if (cmd.scale == 2) {
    result << createEvent(name: "power", value: Math.round(cmd.scaledMeterValue), unit: "W", displayed: true, isStateChange: true)
  } else {
    logger("$device.displayName scale not implemented: $cmd", "error")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd, result) {
  logger("$device.displayName $cmd")

  if (cmd.parameterNumber <= 1 || cmd.parameterNumber == 3) {
    def value = "when off"

    if (cmd.scaledConfigurationValue == 1) {
      value = "when on"
    } else if (cmd.scaledConfigurationValue == 2) {
      value = "never"
    }

    result << createEvent(name: "indicatorStatus", value: value, displayed: true)

    return
  } else if (cmd.parameterNumber == 2) {
    logger("Physical Button state ${cmd.scaledConfigurationValue}", "warn")
    return
  }

  logger("Unknown parameterNumber $cmd.parameterNumber", "error")
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd, result) {
  logger("$device.displayName command not implemented: $cmd", "error")
}

def zwaveEvent(physicalgraph.zwave.commands.protectionv1.ProtectionReport cmd, result) {    
  if (cmd.protectionState == 0) {
    result << createEvent(name: "Protection", value: "disabled", descriptionText: "Protection Mode Disabled")
  } else if (cmd.protectionState == 1) {
    result << createEvent(name: "Protection", value: "sequence", descriptionText: "Protection Mode set to Sequence Control")
  } else if (cmd.protectionState == 2) {
    result << createEvent(name: "Protection", value: "remote", descriptionText: "Protection Mode set to Remote Only")
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
  logger("$device.displayName $cmd")

  String manufacturerCode = String.format("%04X", cmd.manufacturerId)
  String productTypeCode = String.format("%04X", cmd.productTypeId)
  String productCode = String.format("%04X", cmd.productId)

  result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
  result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
  result << createEvent(name: "ProductCode", value: productCode)

  if (cmd.manufacturerName) {
    state.manufacturer = "${cmd.manufacturerName}"
  } else  if (cmd.manufacturerId == 0x0063) {
    state.manufacturer = "GE Branded"
  } else if ( cmd.manufacturerId == 0x0184 ) {
    state.manufacturer = "Dragon Tech Industrial, Ltd."
  } else if ( cmd.manufacturerId == 0x011A ) {
    state.manufacturer = "Enerwave"
  } else {
    state.manufacturer = String.format("Unknown Vendor %04X", cmd.manufacturerId)
  }

  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)
  state.MSR = "$msr"

  result << createEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", displayed: true, isStateChange: true)

  def cmds = []
  cmds << zwave.versionV1.versionGet().format()

  switch (msr) {
    case "UNUSED":
    cmds << zwave.configurationV1.configurationGet(parameterNumber: setIndicatorParam(1)).format()
    cmds << zwave.associationV2.associationGroupingsGet().format()
    break;

    case "011A-0101-0103":
    cmds << zwave.configurationV1.configurationGet(parameterNumber: setIndicatorParam(1)).format()
    cmds << zwave.associationV1.associationGet(groupingIdentifier: 1).format()
    break;

    case "0063-4952-3031":
    cmds << zwave.powerlevelV1.powerlevelGet().format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: setIndicatorParam(3)).format()
    cmds << zwave.protectionV1.protectionGet().format()
    break

    case "0063-4F50-3032":
    cmds << zwave.powerlevelV1.powerlevelGet().format()
    cmds << zwave.associationV2.associationGroupingsGet().format()
    cmds << zwave.manufacturerSpecificV2.deviceSpecificGet().format()
    cmds << zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: 1).format()
    cmds << zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: 2).format()
    cmds << zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: zwaveHubNodeId).format()
    break;

    case "0063-4952-3133":
    case "0184-4447-3031":
    cmds << zwave.configurationV1.configurationGet(parameterNumber: setIndicatorParam(3)).format()
    cmds << zwave.associationV2.associationGroupingsGet().format()
    cmds << zwave.manufacturerSpecificV2.deviceSpecificGet().format()
    cmds << zwave.powerlevelV1.powerlevelGet().format()
    cmds << zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: 1).format()
    cmds << zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: 2).format()
    cmds << zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId: zwaveHubNodeId).format()
    break;

    case "011A-0101-0603":
    cmds << zwave.configurationV1.configurationGet(parameterNumber: setIndicatorParam(1)).format()
    break
    default:
    cmds << zwave.configurationV1.configurationGet(parameterNumber: setIndicatorParam(1)).format()
    break;
  }

  result << response( delayBetween(cmds, 1000) )
  
}

def zwaveEvent(physicalgraph.zwave.commands.dcpconfigv1.DcpListSupportedReport cmd, result) {
  logger("$device.displayName $cmd")
  logger("$device.displayName has not implemented: $cmd", "warn")
}

def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelReport cmd, result) {
  logger("zwaveEvent(): Powerlevel Report received: ${cmd}")
  def device_power_level = (cmd.powerLevel > 0) ? "minus${cmd.powerLevel}dBm" : "NormalPower"
  logger("Powerlevel Report: Power: ${device_power_level}, Timeout: ${cmd.timeout}", "info")
  result << createEvent(name: "Power", value: device_power_level)
}

def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelTestNodeReport cmd, result) {
  logger("$device.displayName $cmd")
  result << response( zwave.commands.powerlevelv1.PowerlevelGet() )
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd, result) {
  logger("$device.displayName $cmd")

  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  def zWaveProtocolVersion = "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  result << createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: true)
  result << createEvent(name: "zWaveProtocolVersion", value: "${zWaveProtocolVersion}", descriptionText: "${device.displayName} ${zWaveProtocolVersion}", isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "DeviceReset", value: "true", descriptionText: cmd.toString(), isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationBusy cmd, result) {
  logger("$device.displayName $cmd no implemented", "warn")
}

def zwaveEvent(physicalgraph.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd, result) {
  logger("$device.displayName $cmd")

  def firmware_report = String.format("%s-%s-%s", cmd.checksum, cmd.firmwareId, cmd.manufacturerId)
  updateDataValue("FirmwareMdReport", firmware_report)
  result << createEvent(name: "FirmwareMdReport", value: firmware_report, descriptionText: "$device.displayName FIRMWARE_REPORT: $firmware_report", isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd, result) {
  logger("$device.displayName $cmd")

  state.groups = cmd.supportedGroupings

  if (cmd.supportedGroupings) {
    Boolean isVersion2 = false

    switch (device.currentValue("MSR")) {
      case "011A-0101-0603":
      logger("$device.displayName should have no association class", "error")
      return
      break;
      case "011A-0101-0103":
      isVersion2 = false
      break
      case "0184-4447-3031":
      default:
      isVersion2 = true
      break
    }

    def cmds = []
    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      if (isVersion2) {
        cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: true, refreshCache: true).format()
        cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x).format()
        cmds << zwave.associationGrpInfoV1.associationGroupCommandListGet(groupingIdentifier: x, allowCache: false).format()
        // cmds << zwave.associationV2.associationGet(groupingIdentifier: x).format()
      } else {
        cmds << zwave.associationV1.associationGet(groupingIdentifier: x).format()
      }
    }

    result << createEvent(name: "supportedGroupings", value: cmd.supportedGroupings, descriptionText: "$device.displayName", isStateChange: true, displayed: true)
    result << response(delayBetween(cmds, 2000))

    return
  }
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupCommandListReport cmd, result) {
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
    zwave.associationV2.associationGet(groupingIdentifier: cmd.groupingIdentifier).format(),
  ]))
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$device.displayName $cmd")

  if (cmd.groupingIdentifier > 3) { // Lifeline
    logger("$device.displayName unknown assoc: $cmd", "error")
    return
  }

  String final_string = ""
  if (cmd.nodeId ) {
    def string_of_assoc = ""
    cmd.nodeId.each {
      string_of_assoc += "${it}, "
    }
    def lengthMinus2 = string_of_assoc.length() - 3
    final_string = string_of_assoc.getAt(0..lengthMinus2)
  }

  Boolean isStateChange = true
  if (cmd.groupingIdentifier == 1) {
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      isStateChange = state.isAssociated ? false : true
      state.isAssociated = true
    } else {
      state.isAssociated = false
    }

    if (! state.isAssociated ) {
      result << zwave.associationV1.associationSet(groupingIdentifier: cmd.groupingIdentifier, nodeId: [zwaveHubNodeId]).format()
    }
  }

  if (cmd.groupingIdentifier == 2) {
    // A check for model should be added here
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      zwave.associationV1.associationRemove(groupingIdentifier: cmd.groupingIdentifier, nodeId: zwaveHubNodeId).format()
    } else {
    }
  }

  if (cmd.groupingIdentifier == 3) {
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
    } else {
      result << zwave.associationV1.associationSet(groupingIdentifier: cmd.groupingIdentifier, nodeId: [zwaveHubNodeId]).format()
    }
  }

  String association_name = "Lifeline";

  switch ( cmd.groupingIdentifier ) {
    case 1: // Lifeline
    break;
    case 2:
    association_name = "BasicSet control";
    break;
    case 3:
    association_name = "Double Tap"
    break;
    default:
    association_name = "Unknown"
    break;
  }

  updateDataValue("$association_name", "$final_string")
  return
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd, result) {
  def versions = commandClassVersions
  def version = versions[cmd.commandClass as Integer]
  def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
  def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
  if (encapsulatedCommand) {
    zwaveEvent(encapsulatedCommand)
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
    result << sendCommands([
      zwave.switchAllV1.switchAllSet(mode: 0x00),
      zwave.switchAllV1.switchAllGet(),
    ])
  } else {
    result << createEvent(name: "SwitchAll", value: msg, isStateChange: true, displayed: true)
  }
}

def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  logger("$device.displayName command not implemented: $cmd", "error")
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true)
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
    logger("$device.displayName bounce on()", "warn")
    return
  }
  state.lastBounce = Calendar.getInstance().getTimeInMillis()

  if (physical) { // Add option to have digital commands execute buttons
    buttonEvent("on()", 1, false, "digital")
  }
  
  String active_time = new Date(state.lastBounce).format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  sendEvent(name: "lastActive", value: active_time, isStateChange: true);

  delayBetween([
    zwave.basicV1.basicSet(value: 0xFF).format(),
    zwave.switchBinaryV1.switchBinaryGet().format(),
  ], 3000)
}

def off() {
  logger("$device.displayName off()")
  
  if (settings.disbableDigitalOff) {
    logger("..off() disabled")
    return response(zwave.switchBinaryV1.switchBinaryGet())
  }
  
  trueOff(false)
}

def disconnect() {
  logger("$device.displayName disconnect()") 
  trueOff(true)
}

private trueOff(Boolean physical = true) {
  if (state.lastBounce && (Calendar.getInstance().getTimeInMillis() - state.lastBounce) < 2000 ) {
    logger("$device.displayName bounce off()", "warn")
    return
  }
  state.lastBounce = Calendar.getInstance().getTimeInMillis()
  
  if (physical) { // Add option to have digital commands execute buttons
    buttonEvent("off()", 2, false, "digital")
  }

  String active_time = new Date(state.lastBounce).format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  sendEvent(name: "lastActive", value: active_time, isStateChange: true);

  def cmds = []
  if (settings.delayOff) {
    // cmds << zwave.versionV1.versionGet()
    // cmds << zwave.zwaveCmdClassV1.zwaveCmdNop()
    cmds << "delay 3000";
  }

  // cmds << zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0xff, sceneId: 2).format();
  // cmds << physical ? zwave.basicV1.basicSet(value: 0x00).format() : zwave.switchBinaryV1.switchBinarySet(switchValue: 0x00).format();
  cmds << zwave.basicV1.basicSet(value: 0x00).format()
  cmds << "delay 5000"
  cmds << zwave.switchBinaryV1.switchBinaryGet().format()

  delayBetween( cmds ) //, settings.delayOff ? 3000 : 600 )
  // sendCommands(cmds)
}

def poll() {
  logger("$device.displayName poll()")
  
  zwave.switchBinaryV1.switchBinaryGet().format()
}

/**
  * PING is used by Device-Watch in attempt to reach the Device
**/
def ping() {
  logger("$device.displayName ping()")
  
  zwave.switchBinaryV1.switchBinaryGet().format()
}

def refresh() {
  logger("$device.displayName refresh()")
 
  delayBetween([
    zwave.switchBinaryV1.switchBinaryGet().format(),
    zwave.manufacturerSpecificV1.manufacturerSpecificGet().format(),
  ])
}

def reset() {
  logger("$device.displayName reset()")

  if (state.hasMeter) {
    return response(delayBetween([
      zwave.meterV2.meterReset().format(),
      zwave.meterV2.meterGet(scale: 0).format(),
      zwave.meterV2.meterGet(scale: 2).format(),
    ]))
  }
}

def prepDevice() {
  [
    zwave.manufacturerSpecificV1.manufacturerSpecificGet(),
    zwave.switchAllV1.switchAllGet(),
    zwave.switchBinaryV1.switchBinaryGet(),
    // zwave.zwaveCmdClassV1.requestNodeInfo(),
  ]
}

def installed() {
  log.info("$device.displayName installed()")

  /*
  if (device.rawDescription) {
    def zwInfo = getZwaveInfo()
    if ($zwInfo) {
      log.debug("$device.displayName $zwInfo")
      sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
    }
  }
  */
  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  sendCommands(prepDevice(), 3000)
}

def updated() {
  if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
    return
  }
  log.info("$device.displayName updated() debug: ${settings.debugLevel}")

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)
  state.parseErrorCount = 0
  state.unknownCommandErrorCount = 0

  // Check in case the device has been changed
  //state.manufacturer = null
  //updateDataValue("MSR", "000-000-000")
  //updateDataValue("manufacturer", "")

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  sendCommands(prepDevice(), 3000)
  
  // Avoid calling updated() twice
  state.updatedDate = Calendar.getInstance().getTimeInMillis()
}

void indicatorWhenOn() {
  sendEvent(name: "indicatorStatus", value: "when on", displayed: false)
  sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: getIndicatorParam(), size: 1).format()))
}

void indicatorWhenOff() {
  sendEvent(name: "indicatorStatus", value: "when off", displayed: false)
  sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: getIndicatorParam(), size: 1).format()))
}

void indicatorNever() {
  sendEvent(name: "indicatorStatus", value: "never", displayed: false)
  sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [2], parameterNumber: getIndicatorParam(), size: 1).format()))
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
