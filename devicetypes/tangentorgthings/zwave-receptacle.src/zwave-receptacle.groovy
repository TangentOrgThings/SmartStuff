// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :
/**
 *  Copyright 2017 Brian Aker <brian@tangent.org>
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
  return "v4.27"
}

metadata {
  definition (name: "Z-Wave Receptacle", namespace: "TangentOrgThings", author: "brian@tangent.org", ocfDeviceType: "oic.d.switch") {
    capability "Actuator"
    capability "Button"
    capability "Energy Meter"
    capability "Indicator"
    capability "Outlet"
    capability "Polling"
    capability "Refresh"
    capability "Sensor"
    capability "Switch"
    capability "Power Meter"
    // capability "Health Check"
    
    command "reset"
    
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
    
    attribute "Lifeline", "string"
    
    attribute "NIF", "string"
    
    attribute "setScene", "enum", ["Set", "Setting"]
    attribute "keyAttributes", "number"

    attribute "Scene", "number"
    attribute "Scene_1", "number"
    attribute "Scene_1_Duration", "number"
    attribute "Scene_2", "number"
    attribute "Scene_2_Duration", "number"
    
    attribute "SwitchAll", "string"
    
     /*
	  manufacturerId value="011a"
	  productType value="0101"
	  productId value="0103"
    */
    fingerprint type: "1001", mfr: "011A", prod: "0101", model: "0103", deviceJoinName: "ZW15R"
    
    /*
      vendorId: 282 (22:49)
      productId: 1539 (22:49)
      productType: 257 (22:49)
      manufacturerId="011a"
      productType value="0101"
	  productId value="0603"
    */
    fingerprint type: "1001", mfr: "011A", prod: "0101", model: "0603", deviceJoinName: "ZW15R"
    
	// ?
    fingerprint type: "1001", mfr: "011A", prod: "1516", model: "3545", deviceJoinName: "ZW15R"
    /*
      manufacturerId value="0063
      productType value="4952"
      productId value="3031"
    */
    fingerprint type: "1001", mfr: "0063", prod: "4952", model: "3031", deviceJoinName: "GE Branded"
    /*
      Z-Wave Product ID: 0x3134
      Product Type: 0x4952
      */
    fingerprint type: "1001", mfr: "0063", prod: "4952", model: "3134", deviceJoinName: "14288/ZW1002"
    
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
    input "ledIndicator", "enum", title: "LED Indicator", description: "Turn LED indicator... ", required: false, options:["on": "When On", "off": "When Off", "never": "Never"]
    input "disbableDigitalOff", "bool", title: "Disable Digital Off", description: "Disallow digital turn off", required: false
    input "debugLevel", "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  }

  // tile definitions
  tiles(scale: 2) {
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
      tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
        attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
      }
      tileAttribute ("device.power", key: "SECONDARY_CONTROL") {
        attributeState "power", label:'Power level: ${currentValue}W', icon: "st.Appliances.appliances17"
      }
    }

    standardTile("indicator", "device.indicatorStatus", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "when off", action:"indicator.indicatorWhenOn", icon:"st.indicators.lit-when-off"
      state "when on", action:"indicator.indicatorNever", icon:"st.indicators.lit-when-on"
      state "never", action:"indicator.indicatorWhenOff", icon:"st.indicators.never-lit"
    }
    
    valueTile("driverVersion", "device.driverVersion", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
      state "driverVersion", label:'${currentValue}'
    }
    
    valueTile("scene", "device.Scene", width:2, height: 2, decoration: "flat", inactiveLabel: false) {
      state "default", label: '${currentValue}'
    }
    
    valueTile("setScene", "device.setScene", width: 2, height: 1, inactiveLabel: false, decoration: "flat") {
      state "Set", label: '${name}', action:"configScene", nextState: "Setting_Scene"
      state "Setting", label: '${name}' //, nextState: "Set_Scene"
    }
    
    valueTile("power", "device.power", decoration: "flat") {
      state "default", label:'${currentValue} W'
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
    details(["switch","indicator", "driverVersion", "power", "energy", "reset", "refresh"])
  }
}

def getCommandClassVersions() {
  [ 
    0x20: 1,  // Basic
    0x25: 1,  // Switch Binary
    0x27: 1,  // Switch All    
    0x2B: 1,  // SceneActivation
    0x2C: 1,  // Scene Actuator Conf
    0x32: 2,  // Meter
    0x56: 1,  // Crc16Encap
    0x59: 1,  // Association Grp Info    
    0x70: 1,  // Configuration
    0x72: 1,  // Manufacturer Specific ManufacturerSpecificReport
    0x7A: 1,  // Firmware Update Md
    0x85: 1,  // Association	0x85	V1 V2
    0x86: 1,  // Version
  ]
}

def parse(String description) {
  def result = null

  if (description && description.startsWith("Err")) {
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
  } else if (! description) {
    result = createEvent(name: "logMessage", value: "parse() called with NULL description", descriptionText: "$device.displayName")
  } else if (description != "updated") {
    def cmd = zwave.parse(description) // , [0x20: 1, 0x25: 1, 0x27: 1, 0x32: 1, 0x70: 1, 0x72: 2, 0x86: 1])

    if (cmd) {
      result = zwaveEvent(cmd)

      if (! result) {
        log.warn "zwaveEvent() failed to return a value for command ${cmd}"
        result = createEvent(name: "lastError", value: "$cmd", descriptionText: description)
      } else {
        // If we displayed the result
        // log.debug "zwave.parse() debug: ${description}"
        // logger("Parsed $result")
      }
    } else {
      log.warn "zwave.parse() failed for: ${description}"
      result = createEvent(name: "lastError", value: "zwave.parse() failed for: ${description}", descriptionText: description)
    }
  }	
    
  return result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
  logger("$device.displayName $cmd")
  
  [createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical") ]
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
  logger("$device.displayName $cmd")
  
  [ createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical") ]
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
  logger("$device.displayName $cmd")
  
  [ createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "digital") ]
}

def buttonEvent(button, held, buttonType = "physical") {
  logger("buttonEvent: $button  held: $held  type: $buttonType")

  button = button as Integer
  String heldType = held ? "held" : "pushed"
  if (button > 0) {
    sendEvent(name: "button", value: "$heldType", data: [buttonNumber: button], descriptionText: "$device.displayName button $button was pushed", isStateChange: true, type: "$buttonType")
  } else {
    sendEvent(name: "button", value: "default", isStateChange: true, type: "$buttonType")
  }
}

// A scene command was received -- it's probably scene 0, so treat it like a button release
def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfGet cmd) {
  logger("$device.displayName $cmd")
  buttonEvent(cmd.sceneId, false, "digital")
  
  response(zwave.sceneActuatorConfV1.sceneActuatorConfReport(dimmingDuration: 0xFF, level: 0xFF, sceneId: cmd.sceneId))
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfReport cmd) {
  logger("$device.displayName $cmd")
  
  // HomeSeer (ST?) does not implement this scene
  if (cmd.sceneId == 0) {
    return [
      createEvent(name: "Scene", value: cmd.sceneId, isStateChange: true, displayed: true),
      createEvent(name: "level", value: cmd.level, isStateChange: true, displayed: true),
      createEvent(name: "switch", value: cmd.level == 0 ? "off" : "on", isStateChange: true, displayed: true),
    ]
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
  
  def result = [ 
    createEvent(name: "$scene_name", value: cmd.level, isStateChange: true, displayed: true),
    createEvent(name: "$scene_duration_name", value: cmd.dimmingDuration, isStateChange: true, displayed: true),
    createEvent(name: "Scene", value: cmd.sceneId, isStateChange: true, displayed: true),
  ]
  
  if (cmds) {
    result << response(delayBetween(cmds, 1000))
  }
  
  return result
}



def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd) {
  logger("$device.displayName $cmd")
  Integer set_sceen = ((cmd.sceneId + 1) / 2) as Integer
  buttonEvent(set_sceen, false, "digital")
  [ createEvent(name: "setScene", value: "Setting", isStateChange: true, displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.meterv2.MeterReport cmd) {
  logger("$device.displayName: $cmd");
  
  state.hasMeter = true
  
  if (cmd.meterType != 1) {
    return [ createEvent(descriptionText: "$device.displayName bad type: $cmd", displayed: true) ]
  }
  
  if (cmd.scale == 0) {
    [ createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kWh", displayed: true, isStateChange: true) ]
  } else if (cmd.scale == 1) {
    [ createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kVAh", displayed: true, isStateChange: true) ]
  } else if (cmd.scale == 2) {
    [ createEvent(name: "power", value: Math.round(cmd.scaledMeterValue), unit: "W", displayed: true, isStateChange: true) ]
  } else {
    [ createEvent(descriptionText: "$device.displayName scale not implemented: $cmd", displayed: true) ]
  }
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
  logger("$device.displayName $cmd")
  
  if (cmd.parameterNumber <= 1 || cmd.parameterNumber == 3) {
    def value = "when off"
    
    if (cmd.configurationValue[cmd.parameterNumber] == 1) {
      value = "when on"
    } else if (cmd.configurationValue[cmd.parameterNumber] == 3) {
      value = "never"
    }
     
    return  [ createEvent(name: "indicatorStatus", value: value, displayed: true) ]
  }
  
  [ createEvent(descriptionText: "Unknown parameterNumber $cmd.parameterNumber", displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd) {
  logger("$device.displayName command not implemented: $cmd")
  [ createEvent(name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false) ]
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  logger("$device.displayName $cmd")
  
  def manufacturerCode = String.format("%04X", cmd.manufacturerId)
  def productTypeCode = String.format("%04X", cmd.productTypeId)
  def productCode = String.format("%04X", cmd.productId)
  
  sendEvent(name: "ManufacturerCode", value: manufacturerCode)
  sendEvent(name: "ProduceTypeCode", value: productTypeCode)
  sendEvent(name: "ProductCode", value: productCode)
  
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
  
  state.manufacturerId = cmd.manufacturerId
  state.productTypeId = cmd.productTypeId
  state.productId= cmd.productId

  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)
  updateDataValue("manufacturer", state.manufacturer)
  sendEvent(name: "Manufacturer", value: "${state.manufacturer}", descriptionText: "$device.displayName", displayed: true, isStateChange: true)
  
  sendEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", displayed: true, isStateChange: true)
  
  if ( cmd.manufacturerId == 0x0184 ) {
    return response(delayBetween([
      zwave.configurationV1.configurationGet(parameterNumber: 3).format(),
      zwave.firmwareUpdateMdV1.firmwareMdGet().format(),
      ]))
  }
  
  return response(zwave.configurationV1.configurationGet(parameterNumber: 1))
}

def zwaveEvent(physicalgraph.zwave.commands.dcpconfigv1.DcpListSupportedReport cmd) {
  logger("$device.displayName $cmd")
  logger("$device.displayName has not implemented: $cmd", "warn")
  [ createEvent(descriptionText: "$device.displayName no implementation: $cmd", displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
  logger("$device.displayName $cmd")
  
  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  [ createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd) {
  logger("$device.displayName $cmd")
  state.reset = true
  [ createEvent(name: "DeviceReset", value: state.reset, descriptionText: cmd.toString(), isStateChange: true, displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd) {
  logger("$device.displayName $cmd")
  
  def firmware_report = String.format("%s-%s-%s", cmd.checksum, cmd.firmwareId, cmd.manufacturerId)
  updateDataValue("FirmwareMdReport", firmware_report)
  [ createEvent(name: "FirmwareMdReport", value: firmware_report, descriptionText: "$device.displayName FIRMWARE_REPORT: $firmware_report", isStateChange: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
  logger("$device.displayName $cmd")

  state.groups = cmd.supportedGroupings

  if (cmd.supportedGroupings) {
    def cmds = []
    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: true, refreshCache: true).format()
      cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x).format()
      cmds << zwave.associationGrpInfoV1.associationGroupCommandListGet(groupingIdentifier: x, allowCache: false).format()
      cmds << zwave.associationV1.associationGet(groupingIdentifier: x).format()
    }
    
    return [
      createEvent(name: "supportedGroupings", value: cmd.supportedGroupings, descriptionText: "$device.displayName", isStateChange: true, displayed: true),
      response(delayBetween(cmds, 2000)),
    ]
  }
  
  [ createEvent(descriptionText: "$device.displayName AssociationGroupingsReport: $cmd", isStateChange: true, displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupCommandListReport cmd) {
  logger("$device.displayName $cmd")
  [ createEvent(descriptionText: "$device.displayName AssociationGroupCommandListReport: $cmd", isStateChange: true, displayed: true) ]
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

  String final_string
  if (cmd.nodeId ) {
    def string_of_assoc = ""
    cmd.nodeId.each {
      string_of_assoc += "${it}, "
    }
    def lengthMinus2 = string_of_assoc.length() - 3
    final_string = string_of_assoc.getAt(0..lengthMinus2)
  }
  
  Boolean isStateChange
  state.isAssociated = true
  
  if (cmd.groupingIdentifier == 0x01) { // Lifeline
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      isStateChange = state.hasLifeLine ? false : true
      state.hasLifeLine = true
    } else {
      state.hasLifeLine = false
    }
    
    state.Lifeline = final_string;

    sendEvent(name: "LifeLine",
      value: "${final_string}",
      displayed: true,
      isStateChange: isStateChange)
  } else {
    logger("Unknown group ${cmd.groupingIdentifier}", "warn")
    return [ createEvent(
               descriptionText: "Association group ${cmd.groupingIdentifier} is not handled",
               displayed: true) ]
  }

  if ( state.hasLifeLine == true ) {
    state.isAssociated = true
  } else { 
    state.isAssociated = false
  }

  if (! state.isAssociated ) {
    return response(sendCommands([ 
          zwave.associationV1.associationSet(groupingIdentifier: 0x01, nodeId: [zwaveHubNodeId]).format(),
        ]))
  } else {
    [ createEvent(descriptionText: "$device.displayName assoc: $cmd", displayed: true) ]
  }
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
  def versions = commandClassVersions
  def version = versions[cmd.commandClass as Integer]
  def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
  def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
  if (encapsulatedCommand) {
    zwaveEvent(encapsulatedCommand)
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

def zwaveEvent(physicalgraph.zwave.Command cmd) {
  logger("$device.displayName $cmd", "error")
  [ createEvent(descriptionText: "$device.displayName command not implemented: $cmd", displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd) {
  logger("$device.displayName $cmd")
  [ createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true) ]
}

def on() {
  delayBetween([
    zwave.basicV1.basicSet(value: 0xFF).format(),
    zwave.switchBinaryV1.switchBinaryGet().format()
  ])
}

def off() {
  delayBetween([
    zwave.basicV1.basicSet(value: 0x00).format(),
    zwave.switchBinaryV1.switchBinaryGet().format()
  ])
}

def poll() {
  zwave.switchBinaryV1.switchBinaryGet().format()
}

/**
  * PING is used by Device-Watch in attempt to reach the Device
**/
def ping() {
  zwave.switchBinaryV1.switchBinaryGet().format()
}

def refresh() {
  zwave.switchBinaryV1.switchBinaryGet().format()
}

def reset() {
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
    zwave.switchBinaryV1.switchBinaryGet(),
    zwave.versionV1.versionGet(),
    zwave.manufacturerSpecificV1.manufacturerSpecificGet(),
    zwave.switchAllV1.switchAllGet(),
    // zwave.associationV1.associationGroupingsGet(),
    zwave.zwaveCmdClassV1.requestNodeInfo(),
  ]
}

def installed() {
  log.info("$device.displayName installed()")
  state.loggingLevelIDE = 4
  
  /*
  if (device.rawDescription) {
    def zwInfo = getZwaveInfo()
    if ($zwInfo) {
      log.debug("$device.displayName $zwInfo")
      sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
    }
  }
  */
  
  // Device-Watch simply pings if no device events received for 32min(checkInterval) 
  sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  sendCommands(prepDevice())
}

def updated() {
  log.info("$device.displayName updated() debug: ${debugLevel}")
  state.loggingLevelIDE = debugLevel ? debugLevel : 4
  
  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)

  if (! state.reset) {
    sendEvent(name: "reset", value: false, isStateChange: true, displayed: true)
  } else {
    sendEvent(name: "reset", value: true, isStateChange: true, displayed: true)
  }
  
  /*
  if (device.rawDescription) {
    def zwInfo = getZwaveInfo()
    if ($zwInfo) {
      log.debug("$device.displayName $zwInfo")
      sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
    }
  }
  */
  
  // Check in case the device has been changed
  //state.manufacturer = null
  //updateDataValue("MSR", "000-000-000")
  //updateDataValue("manufacturer", "")
  
  // Device-Watch simply pings if no device events received for 32min(checkInterval) 
  sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
  
  sendCommands(prepDevice())
}

void indicatorWhenOn() {
  sendEvent(name: "indicatorStatus", value: "when on", displayed: false)
  sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 1, size: 1).format()))
}

void indicatorWhenOff() {
  sendEvent(name: "indicatorStatus", value: "when off", displayed: false)
  sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 1, size: 1).format()))
}

void indicatorNever() {
  sendEvent(name: "indicatorStatus", value: "never", displayed: false)
  sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [2], parameterNumber: 1, size: 1).format()))
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
    case "error":
    if (state.loggingLevelIDE >= 1) {
      log.error msg
    }
    if (state.loggingLevelDevice >= 1) {
      sendEvent(name: "lastError", value: "ERROR: ${msg}", displayed: false, isStateChange: true)
    }
    break

    case "warn":
    if (state.loggingLevelIDE >= 2) {
      log.warn msg
    }
    if (state.loggingLevelDevice >= 2) {
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
