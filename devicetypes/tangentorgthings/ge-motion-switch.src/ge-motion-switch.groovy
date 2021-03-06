// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :
/**
 *  GE Motion Switch
 *  Author: Brian Aker <brian@tangent.org)>
 *  Author: Matt lebaugh (@mlebaugh)
 *
 * Copyright (C) Brian Aker
 * Copyright (C) Matt LeBaugh
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

def getDriverVersion () {
  return "v2.11"
}

def getConfigurationOptions(String model) {
  return [ 1, 13, 14, 15, 19, 2, 3, 4, 5, 6 ]
}

metadata {
  definition (name: "GE Motion Switch", namespace: "TangentOrgThings", author: "Brian Aker", ocfDeviceType: "oic.d.switch", executeCommandsLocally: true) {
    capability "Motion Sensor"
    capability "Actuator"
    capability "Switch"
    capability "Polling"
    capability "Refresh"
    capability "Sensor"
    // capability "Health Check"
    capability "Light"
    capability "Button"

    command "toggleMode"
    command "Occupancy"
    command "Vacancy"
    command "Manual"
    command "LightSenseOn"
    command "LightSenseOff"

		command "parentCommand"

    attribute "operatingMode", "enum", ["Manual", "Vacancy", "Occupancy"]

    attribute "DeviceReset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message

    attribute "driverVersion", "string"
    attribute "Manufacturer", "string"
    attribute "NIF", "string"
    attribute "FirmwareMdReport", "string"
    attribute "FirmwareVersion", "string"

    attribute "setScene", "enum", ["Set", "Setting"]
    attribute "keyAttributes", "number"

    attribute "Scene", "number"

    attribute "SwitchAll", "string"
    attribute "Power", "string"

    fingerprint mfr:"0063", prod:"494D", model: "3032", deviceJoinName: "GE Z-Wave Plus Motion Wall Switch"
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
    input title: "", description: "Select your prefrences here, they will be sent to the device once updated.\n\nTo verify the current settings of the device, they will be shown in the 'recently' page once any setting is updated", displayDuringSetup: false, type: "paragraph", element: "paragraph"
      input (
        name: "operationmode",
        title: "Operating Mode",
        description: "Occupancy: Automatically turn on and off the light with motion\nVacancy: Manually turn on, automatically turn off light with no motion.",
        type: "enum",
        options: [
        "1" : "Manual",
        "2" : "Vacancy (auto-off)",
        "3" : "Occupancy (auto-on/off)",
        ],
        required: false
      )
      input (
        name: "timeoutduration",
        title: "Timeout Duration",
        description: "Length of time after no motion for the light to shut off in Occupancy/Vacancy modes",
        type: "enum",
        options: [
        "0" : "Test (5s)",
        "1" : "1 minute",
        "5" : "5 minutes (default)",
        "15" : "15 minutes",
        "30" : "30 minutes",
        "255" : "disabled"
        ],
        required: false
      )
      input (
        name: "motionsensitivity",
        title: "Motion Sensitivity",
        description: "Motion Sensitivity",
        type: "enum",
        options: [
        "1" : "High",
        "2" : "Medium (default)",
        "3" : "Low"
        ],
        required: false
      )
      input (
        name: "lightsense",
        title: "Light Sensing",
        description: "If enabled, Occupancy mode will only turn light on if it is dark",
        type: "enum",
        options: [
        "0" : "Disabled",
        "1" : "Enabled",
        ],
        required: false
      )

      input (
        name: "motion",
        title: "Motion Sensor",
        description: "Enable/Disable Motion Sensor.",
        type: "enum",
        options: [
        "0" : "Disable",
        "1" : "Enable",
        ],
        required: false
      )
      input (
        name: "invertSwitch",
        title: "Switch Orientation",
        type: "enum",
        options: [
        "0" : "Normal",
        "1" : "Inverted",
        ],
        required: false
      )
      input (
        name: "resetcycle",
        title: "Reset Cycle",
        type: "enum",
        options: [
        "0" : "Disabled",
        "1" : "10 sec",
        "2" : "20 sec (default)",
        "3" : "30 sec",
        "4" : "45 sec",
        "110" : "27 mins",
        ],
        required: false
      )
      input (
        type: "paragraph",
        element: "paragraph",
        title: "Configure Association Groups:",
        description: "Devices in association group 2 will receive Basic Set commands directly from the switch when it is turned on or off. Use this to control another device as if it was connected to this switch.\n\n" +
        "Devices in association group 3 will receive Basic Set commands directly from the switch when it is double tapped up or down.\n\n" +
        "Devices are entered as a comma delimited list of IDs in hexadecimal format."
      )

      input (
        name: "requestedGroup2",
        title: "Association Group 2 Members (Max of 5):",
        type: "text",
        required: false
      )

      input (
        name: "requestedGroup3",
        title: "Association Group 3 Members (Max of 4):",
        type: "text",
        required: false
      )

      input (
        name: "debugLevel",
        type: "number",
        title: "Debug Level",
        description: "Adjust debug level for log",
        range: "1..5",
        displayDuringSetup: false,
        defaultValue: 3
      )
  }

  tiles(scale: 2) {
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
      tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC"
        attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
      }
    }

    standardTile("motion", "device.motion", inactiveLabel: false, width: 2, height: 2) {
      state "inactive",label:'no motion',icon:"st.motion.motion.inactive",backgroundColor:"#ffffff"
      state "active",label:'motion',icon:"st.motion.motion.active",backgroundColor:"#53a7c0"
    }

    standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
    }

    standardTile("operatingMode", "device.operatingMode", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "default", label:'Mode toggle: ${currentValue}', unit:"", action:"toggleMode"
    }

    valueTile("driverVersion", "device.driverVersion", width: 2, height: 2, decoration: "flat") {
      state "default", label: '${currentValue}', defaultState: true
    }

    main(["switch"])
    details(["switch", "motion", "operatingMode", "refresh", "driverVersion"])
  }
}

def getCommandClassVersions() {
  [
    0x20: 1,
    0x25: 1,
    0x27: 1,
    0x2b: 1,
    0x2c: 1,
    0x56: 1,
    0x59: 1,
    0x5a: 1,
    // 0x5e: 2,
    0x60: 3, // V4
    0x70: 2, // V1
    0x71: 3, // V4
    0x72: 2,
    0x73: 1,
    0x7a: 2,
    0x85: 2,
    0x86: 1, // V2
    0x8e: 2, // V3
    0x31: 5, // Note: this one shows up from logs
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
    logger("parse() called with NULL description", "info")
  } else if (description != "updated") {
    def cmd = zwave.parse(description, getCommandClassVersions())

    if (cmd) {
      zwaveEvent(cmd, result)

    } else {
      logger( "zwave.parse(getCommandClassVersions()) failed for: ${description}", "warn" )
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

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd, result) {
  logger("$cmd")

  def encapsulatedCommand = zwave.commandClass(cmd.commandClass)?.command(cmd.command)?.parse(cmd.data)
  if (encapsulatedCommand) {
    zwaveEvent(encapsulatedCommand, result)
    return
  }

  logger("zwaveEvent(): Could not extract command from ${cmd}")
}

def zwaveEvent(zwave.commands.multichannelv3.MultiChannelEndPointReport cmd, result) {
  logger("$cmd")
  updateDataValue("endpoints", cmd.endPoints.toString())

	def cmds = []
  for (def x = 1; x <= cmd.endPoints; x++) {
    cmds << zwave.multiChannelV3.multiChannelCapabilityGet(endPoint: x).format()
  }

  result << response(delayBetween(cmds, 4000))
}

def zwaveEvent(zwave.commands.multichannelv3.MultiChannelCapabilityReport cmd, result) {
  logger("$cmd")
  updateDataValue("MultiChannelCapabilityReport ", cmd.endPoint.toString())

	def endP = cmd.endPoint
	
  if (childDevices) {
    if (!childDevices.find{ it.deviceNetworkId.endsWith("-ep${endP}") || !childDevices}) {
      createChildDevices( cmd.commandClass, endP )
    }
  }
}

def zwaveEvent(zwave.commands.multichannelv3.MultiChannelCmdEncap cmd, result) {
  logger("$cmd")

  def ep = cmd.sourceEndPoint
  def childDevice = null

  if (childDevices) {
    childDevices.each {
      if (it.deviceNetworkId =="${device.deviceNetworkId}-ep${ep}") {
        childDevice = it
      }
    }
  }

  if (childDevice) {
    logger("Parse ${childDevice.deviceNetworkId}, cmd: ${cmd}", "info")
    childDevice.parse(cmd.encapsulatedCommand().format())
  } else {
    logger( "Child device not found.cmd: ${cmd}", "warn")

    def encapsulatedCommand = cmd.encapsulatedCommand(getCommandClassVersions())
    if (encapsulatedCommand) {
      zwaveEvent(encapsulatedCommand, result)
      return
    }

    logger("Unknown MultiChannelCmdEncap: $cmd", "warn")
  }
}

def zwaveEvent(zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$cmd")
  result << createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical")
}

def zwaveEvent(zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$cmd")

  result << createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical")

  if (cmd.value == 255) {
    result << createEvent(name: "button", value: "pushed", data: [buttonNumber: "1"], descriptionText: "On/Up on (button 1) $device.displayName was pushed", isStateChange: true, type: "physical")
  }
  else if (cmd.value == 0) {
    result << createEvent(name: "button", value: "pushed", data: [buttonNumber: "2"], descriptionText: "Off/Down (button 2) on $device.displayName was pushed", isStateChange: true, type: "physical")
  }
}

def zwaveEvent(zwave.commands.multichannelassociationv2.MultiChannelAssociationGroupingsReport cmd, result) {
  logger("$cmd")

  if (cmd.supportedGroupings) {
    def cmds = []

    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.multiChannelAssociationV2.multiChannelAssociationGet( groupingIdentifier: x );
    }

    sendCommands(cmds, 1000)

    return
  }

  logger("No groups reported", "warn")
}

def zwaveEvent(zwave.commands.multichannelassociationv2.MultiChannelAssociationReport cmd, result) {
  logger("$cmd")

  String nodes = cmd.nodeId.join(", ")
  updateDataValue("MultiGroup #${cmd.groupingIdentifier}", "${name}")
}

def zwaveEvent(zwave.commands.associationv2.AssociationGroupingsReport cmd, result) {
  logger("$cmd")

  if (cmd.supportedGroupings) {
    def cmds = []

    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: 0x01, refreshCache: true);
      cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x);
      cmds << zwave.associationV2.associationGet(groupingIdentifier: x);
    }

    sendCommands(cmds, 1000)

    return
  }

  logger("No groups reported", "error")
}

def zwaveEvent(zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$cmd")

  if (cmd.groupingIdentifier > 3) {
    logger("Unknown Group Identifier", "error");
    return
  }

  // Lifeline
  String nodes = cmd.nodeId.join(", ")

  String group_name = ""
  switch (cmd.groupingIdentifier) {
    case 1:
    group_name = "Lifeline"
      break;
    case 2:
    group_name = "Basic Set"
    break;
    case 3:
    group_name = "Basic Set Double Tap"
    break;
    default:
    group_name = "Unknown";
    break;
  }

  updateDataValue("$group_name", "$nodes")
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
    zwave.associationV2.associationGet(groupingIdentifier: cmd.groupingIdentifier).format(),
  ]))
}

def zwaveEvent(zwave.commands.associationgrpinfov1.AssociationGroupCommandListReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.configurationv2.ConfigurationReport cmd, result) {
  logger("$cmd")

  def config = cmd.scaledConfigurationValue
  if (cmd.parameterNumber == 1) {
    def value = config == 0 ? "Test 5s" : config == 1 ? "1 minute" : config == 5 ? "5 minute" : config == 15 ? "15 minute" : config == 30 ? "30 minute" : "255 minute"
    result << createEvent(name:"TimeoutDuration", value: value, displayed:true, isStateChange:true)
  } else if (cmd.parameterNumber == 13) {
    def value = config == 1 ? "High" : config == 2 ? "Medium" : "Low"
    result << createEvent(name:"MotionSensitivity", value: value, displayed:true, isStateChange:true)
  } else if (cmd.parameterNumber == 14) {
    def value = config == 0 ? "Disabled" : "Enabled"
    result << createEvent(name:"LightSense", value: value, displayed:true, isStateChange:true)
  } else if (cmd.parameterNumber == 15) {
    def value = config == 0 ? "Disabled" : config == 1 ? "10 sec" : config == 2 ? "20 sec" : config == 3 ? "30 sec" : config == 4 ? "45 sec" : "27 minute"
    result << createEvent(name:"ResetCycle", value: value, displayed:true, isStateChange:true)
  } else if (cmd.parameterNumber == 3) {
    if (config == 1 ) {
      result << createEvent(name:"operatingMode", value: "Manual", displayed:true, isStateChange:true)
    } else if (config == 2 ) {
      result << createEvent(name:"operatingMode", value: "Vacancy", displayed:true, isStateChange:true)
    } else if (config == 3 ) {
      result << createEvent(name:"operatingMode", value: "Occupancy", displayed:true, isStateChange:true)
    }
  } else if (cmd.parameterNumber == 6) {
    def value = config == 0 ? "Disabled" : "Enabled"
    result << createEvent(name:"MotionSensor", value: value, displayed:true, isStateChange:true)
  } else if (cmd.parameterNumber == 5) {
    def value = config == 0 ? "Normal" : "Inverted"
    result << createEvent(name:"SwitchOrientation", value: value, displayed:true, isStateChange:true)
  }
}

private switchEvents(Short value, boolean isPhysical, result) {
  if (value == 254) {
    logger("returned Unknown for status.", "warn")
    return
  }

  result << createEvent(name: "switch", value: value ? "on" : "off", type: isPhysical ? "physical" : "digital", isStateChange: true, displayed: true)
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

def buttonEvent(button, held, buttonType = "physical") {
  log.debug("buttonEvent: $button  held: $held  type: $buttonType")

  button = button as Integer
  String heldType = held ? "held" : "pushed"
  sendEvent(name: "button", value: "$heldType", data: [buttonNumber: button], descriptionText: "$device.displayName button $button was pushed", isStateChange: true, type: "$buttonType")
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

def zwaveEvent(zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
  logger("$cmd")

  if ( cmd.manufacturerName ) {
    state.manufacturer= cmd.manufacturerName
  } else {
    state.manufacturer= "GE/Jasco"
  }

  String manufacturerCode = String.format("%04X", cmd.manufacturerId)
  String productTypeCode = String.format("%04X", cmd.productTypeId)
  String productCode = String.format("%04X", cmd.productId)

  String msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", "$msr")
  updateDataValue("manufacturer", "${state.manufacturer}")

  Integer[] parameters = getConfigurationOptions(productCode)

  def cmds = []
  parameters.each {
    cmds << zwave.configurationV1.configurationGet(parameterNumber: it).format()
  }

  cmds << zwave.versionV1.versionGet().format()

  result << response(delayBetween(cmds, 1000))
  result << response( zwave.versionV1.versionGet() )
}

def zwaveEvent(zwave.commands.versionv1.VersionReport cmd, result) {
  logger("$cmd")

  String text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  String zWaveProtocolVersion = "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  result << createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: true)
  result << createEvent(name: "zWaveProtocolVersion", value: "${zWaveProtocolVersion}", descriptionText: "${device.displayName} ${zWaveProtocolVersion}", isStateChange: true)
}

def zwaveEvent(zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd, result) {
  logger("$cmd")
  result << createEvent(name: "DeviceReset", value: "true", descriptionText: cmd.toString(), isStateChange: true, displayed: true)
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

def zwaveEvent(zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd, result) {
  logger("$cmd")

  String firmware_report = String.format("%s-%s-%s", cmd.checksum, cmd.firmwareId, cmd.manufacturerId)
  updateDataValue("FirmwareMdReport", firmware_report)
  result << createEvent(name: "FirmwareMdReport", value: firmware_report, descriptionText: "$device.displayName FIRMWARE_REPORT: $firmware_report", displayed: true, isStateChange: true)
}

def zwaveEvent(zwave.commands.hailv1.Hail cmd, result) {
  logger("$cmd")
  result << createEvent(name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false)
}

// SensorMultilevelReport(scale: 1, sensorValue: [2, 234], precision: 1, sensorType: 1, scaledSensorValue: 74.6, size: 2)
def zwaveEvent(zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd, result) {
  logger("$cmd")

  switch (sensorType) {
    case SENSOR_TYPE_TEMPERATURE_VERSION_1:
    logger("Temperature Report: ${cmd.scaledSensorValue}", "info")
    break;
    default:
    logger("Unknown SensorReport(${cmd.sensorType}: ${cmd.scaledSensorValue}", "info")
    break;
  }
}

def zwaveEvent(zwave.commands.notificationv3.NotificationReport cmd, result) {
  logger("$cmd")

  if (cmd.notificationType == 0x07) {
    if ((cmd.event == 0x00)) {
      result << createEvent(name: "motion", value: "inactive", descriptionText: "$device.displayName motion has stopped")
    } else if (cmd.event == 0x08) {
      result << createEvent(name: "motion", value: "active", descriptionText: "$device.displayName detected motion")
    }
  }
  result
}

def zwaveEvent(zwave.commands.applicationstatusv1.ApplicationBusy cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.applicationstatusv1.ApplicationRejectedRequest cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.Command cmd, result) {
  logger("command not implemented: $cmd", "error")
}

def on() {
  log.debug("$device.displayName on()")

  state.lastActive = new Date().time
  if (0) {
    buttonEvent(1, false, "digital")
  }

  delayBetween([
    zwave.switchBinaryV1.switchBinarySet(switchValue: 0xFF).format(),
    // zwave.sceneActivationV1.sceneActivationSet(dimmingDuration: 0, sceneId: 1).format(),
    zwave.switchBinaryV1.switchBinaryGet().format(),
  ])
}

def off() {
  logger("off()")

  state.lastActive = new Date().time
  if (0) {
    buttonEvent(2, false, "digital")
  }

  if (settings.disbableDigitalOff) {
    logger("..off() disabled")
    return zwave.switchBinaryV1.switchBinaryGet().format()
  }

  delayBetween([
    zwave.switchBinaryV1.switchBinarySet(switchValue: 0x00).format(),
    zwave.switchBinaryV1.switchBinaryGet().format(),
  ])
}

def poll() {
  delayBetween([
    zwave.switchBinaryV1.switchBinaryGet().format(),
  ])
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 **/
def ping() {
  refresh()
}

def refresh() {
  log.debug "refresh() is called"

  delayBetween([
    zwave.switchBinaryV1.switchBinaryGet().format(),
    zwave.notificationV3.notificationGet(notificationType: 7).format()
  ])
}

def toggleMode() {
  log.debug("Toggling Mode")

  def cmds = []
  if (device.currentValue("operatingMode") == "Manual") {
    cmds << zwave.configurationV1.configurationSet(configurationValue: [2] , parameterNumber: 3, size: 1)
  }
  else if (device.currentValue("operatingMode") == "Vacancy") {
    cmds << zwave.configurationV1.configurationSet(configurationValue: [3], parameterNumber: 3, size: 1)
  }
  else if (device.currentValue("operatingMode") == "Occupancy") {
    cmds << zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 3, size: 1)
  }
  cmds << zwave.configurationV1.configurationGet(parameterNumber: 3)

  sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 1000)

}

def SetModeNumber(value) {
  log.debug("Setting mode by number: ${value}")

  def cmds = []
  cmds << zwave.configurationV1.configurationSet(configurationValue: [value] , parameterNumber: 3, size: 1)
  cmds << zwave.configurationV1.configurationGet(parameterNumber: 3)
  sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 1000)

}

def Occupancy() {
  def cmds = []
  cmds << zwave.configurationV1.configurationSet(configurationValue: [3] , parameterNumber: 3, size: 1)
  cmds << zwave.configurationV1.configurationGet(parameterNumber: 3)
  sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 1000)

}

def Vacancy() {
  def cmds = []
  cmds << zwave.configurationV1.configurationSet(configurationValue: [2] , parameterNumber: 3, size: 1)
  cmds << zwave.configurationV1.configurationGet(parameterNumber: 3)
  sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 1000)

}

def Manual() {
  def cmds = []
  cmds << zwave.configurationV1.configurationSet(configurationValue: [1] , parameterNumber: 3, size: 1)
  cmds << zwave.configurationV1.configurationGet(parameterNumber: 3)
  sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 1000)

}

def LightSenseOn() {
  log.debug("Setting Light Sense On")
  def cmds = []
  cmds << zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 14, size: 1)
  sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 1000)
}

def LightSenseOff() {
  log.debug("Setting Light Sense Off")
  def cmds = []
  cmds << zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 14, size: 1)
  sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 1000)
}

def parentCommand() {
}

def prepDevice() {
  [
    zwave.manufacturerSpecificV1.manufacturerSpecificGet(),
    zwave.associationV2.associationGroupingsGet(),
    zwave.zwaveCmdClassV1.requestNodeInfo(),
    zwave.multiChannelV3.multiChannelEndPointGet(),
    zwave.switchAllV1.switchAllGet(),
    zwave.powerlevelV1.powerlevelGet(),
  ]
}

def setTrace(Boolean enable) {
  state.isTrace = enable
}

Boolean isTraceEnabled() {
  Boolean is_trace = ( state.isTrace == null || !( state.isTrace instanceof Boolean )) ? false : state.isTrace
  return is_trace
}

def followupTraceDisable() {
  logger("followupTraceDisable()")
  
  setTrace(false)
}

def installed() {
  // Device-Watch simply pings if no device events received for 32min(checkInterval)
  // sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
}

def updated() {
  log.info("updated()")
  setTrace(true)

  // sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
  sendEvent(name: "numberOfButtons", value: 2, displayed: false)

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)

  if (state.lastUpdated && now() <= state.lastUpdated + 3000) {
    return
  }
  state.lastUpdated = now()

  if ( 0 ) {
    def cmds = []

    //switch and dimmer settings
    if (0) {
      if (settings.timeoutduration) {cmds << zwave.configurationV1.configurationSet(configurationValue: [settings.timeoutduration.toInteger()], parameterNumber: 1, size: 1)}
      cmds << zwave.configurationV1.configurationGet(parameterNumber: 1)
    }
    if (settings.motionsensitivity) {cmds << zwave.configurationV1.configurationSet(configurationValue: [settings.motionsensitivity.toInteger()], parameterNumber: 13, size: 1)}
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 13)
    if (settings.lightsense) {cmds << zwave.configurationV1.configurationSet(configurationValue: [settings.lightsense.toInteger()], parameterNumber: 14, size: 1)}
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 14)
    if (settings.resetcycle) {cmds << zwave.configurationV1.configurationSet(configurationValue: [settings.resetcycle.toInteger()], parameterNumber: 15, size: 1)}
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 15)
    if (settings.operationmode) {cmds << zwave.configurationV1.configurationSet(configurationValue: [settings.operationmode.toInteger()], parameterNumber: 3, size: 1)}
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 3)
    if (settings.motion) {cmds << zwave.configurationV1.configurationSet(configurationValue: [settings.motion.toInteger()], parameterNumber: 6, size: 1)}
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 6)
    if (settings.invertSwitch) {cmds << zwave.configurationV1.configurationSet(configurationValue: [settings.invertSwitch.toInteger()], parameterNumber: 5, size: 1)}
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 5)

    // Make sure lifeline is associated - was missing on a dimmer:
    /*
    cmds << zwave.associationV1.associationSet(groupingIdentifier:0, nodeId:zwaveHubNodeId)
    cmds << zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId)
    cmds << zwave.associationV1.associationSet(groupingIdentifier:2, nodeId:zwaveHubNodeId)
    cmds << zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:zwaveHubNodeId)
     */

    //association groups
    /*
    def nodes = []
    if (settings.requestedGroup2 != state.currentGroup2) {
    nodes = parseAssocGroupList(settings.requestedGroup2, 2)
    cmds << zwave.associationV2.associationRemove(groupingIdentifier: 2, nodeId: [])
    cmds << zwave.associationV2.associationSet(groupingIdentifier: 2, nodeId: nodes)
    cmds << zwave.associationV2.associationGet(groupingIdentifier: 2)
    state.currentGroup2 = settings.requestedGroup2
    }

    if (settings.requestedGroup3 != state.currentGroup3) {
    nodes = parseAssocGroupList(settings.requestedGroup3, 3)
    cmds << zwave.associationV2.associationRemove(groupingIdentifier: 3, nodeId: [])
    cmds << zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: nodes)
    cmds << zwave.associationV2.associationGet(groupingIdentifier: 3)
    state.currentGroup3 = settings.requestedGroup3
    }
     */

    cmds += prepDevice()

    // Set timer turning off trace output
    if (0) {
      runIn(60*5, followupTraceDisable)
    }

    sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 5000)
  }
}

def Up() {
  sendEvent(name: "button", value: "pushed", data: [buttonNumber: "1"], descriptionText: "On/Up (button 1) on $device.displayName was pushed", isStateChange: true, type: "digital")
  on()
}

def Down() {
  sendEvent(name: "button", value: "pushed", data: [buttonNumber: "2"], descriptionText: "Off/Down (button 2) on $device.displayName was pushed", isStateChange: true, type: "digital")
  off()
}

def configure() {
  /*
  def cmds = []
  // Make sure lifeline is associated - was missing on a dimmer:
  cmds << zwave.associationV1.associationSet(groupingIdentifier:0, nodeId:zwaveHubNodeId)
  cmds << zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId)
  cmds << zwave.associationV1.associationSet(groupingIdentifier:2, nodeId:zwaveHubNodeId)
  cmds << zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:zwaveHubNodeId)
  sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 1000)
   */
}

private parseAssocGroupList(list, group) {
  def nodes = group == 2 ? [] : [zwaveHubNodeId]
  if (list) {
    def nodeList = list.split(',')
    def max = group == 2 ? 5 : 4
    def count = 0

    nodeList.each { node ->
      node = node.trim()
      if ( count >= max) {
        log.warn "Association Group ${group}: Number of members is greater than ${max}! The following member was discarded: ${node}"
      }
      else if (node.matches("\\p{XDigit}+")) {
        def nodeId = Integer.parseInt(node,16)
        if (nodeId == zwaveHubNodeId) {
          log.warn "Association Group ${group}: Adding the hub as an association is not allowed (it would break double-tap)."
        }
        else if ( (nodeId > 0) & (nodeId < 256) ) {
          nodes << nodeId
          count++
        }
        else {
          log.warn "Association Group ${group}: Invalid member: ${node}"
        }
      }
      else {
        log.warn "Association Group ${group}: Invalid member: ${node}"
      }
    }
  }

  return nodes
}

// handle commands
void createChildDevices(def cc, def ep) {
  try {
    String deviceCCHandler = ""
    String deviceCCType = ""

    for (def i = 0; i < cc.size() && 0; i++) {
      switch (cc[i]) {
        case 0x26: 
        deviceCCType = "Multilevel Switch"
        deviceCCHandler = "Child Multilevel Switch"
        break

        case 0x25: 
        deviceCCType =  "Binary Switch"
        deviceCCHandler = "Child Binary Switch"
        break

        case 0x31: 
        deviceCCType = "Multilevel Sensor"
        deviceCCHandler = "Child Multilevel Sensor"
        break

        case 0x32:
        deviceCCType = "Meter"
        deviceCCHandler = "Child Meter"
        break

        case 0x71: 
        deviceCCType = "Notification";
        deviceCCHandler = "Child Notification";
        break

        case 0x40:					
        case 0x43: 
        deviceCCType = "Thermostat"
        deviceCCHandler = "Child Thermostat"
        break

        default:
        logger("No Child Device Handler case for command class: '$cc'", "debug")
      }

      // stop on the first matched CC
      if (deviceCCHandler != "") {
        break
      }
    }

    if (deviceCCHandler == "") {
      deviceCCType = "Universal"
      deviceCCHandler = "Child Universal"
    }

    if (deviceCCHandler != "") {
      try {
        addChildDevice(deviceCCHandler, "${device.deviceNetworkId}-ep${ep}", null,
        [completedSetup: true, label: "${deviceCCType}-${ep}", 
        isComponent: false, componentName: "${deviceCCType}-${ep}", componentLabel: "${deviceCCType}-${ep}"])
      } catch (e) {
        log.error "Creation child devices failed with error = ${e}"
      }
    }

    // associationSet()
  } catch (e) {
    logger("Child device creation failed with error = ${e}", "warn")
  }
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
private sendCommands(cmds, delay=1000) {
  sendHubCommand( cmds.collect{ (it instanceof physicalgraph.zwave.Command ) ? response(encapCommand(it)) : response(it) }, delay)
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
