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
  return "v3.95"
}

metadata {
  definition (name: "Z-Wave Receptacle", namespace: "TangentOrgThings", author: "brian@tangent.org", ocfDeviceType: "oic.d.switch") {
    capability "Actuator"
    capability "Energy Meter"
    capability "Indicator"
    capability "Outlet"
    // capability "Polling"
    capability "Refresh"
    capability "Sensor"
    capability "Switch"
    capability "Power Meter"
    // capability "Health Check"
    
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
    attribute "Scene_2", "number"
    
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

    standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
    }

    main "switch"
    details(["switch","indicator", "driverVersion", "refresh"])
  }
}
def prepDevice() {
  [
    zwave.switchBinaryV1.switchBinaryGet(),
    zwave.versionV1.versionGet(),
    zwave.manufacturerSpecificV1.manufacturerSpecificGet(),
    zwave.zwaveCmdClassV1.requestNodeInfo(),
  ]
}

def installed() {
  log.debug "$device.displayName installed()"
  
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
  log.debug "$device.displayName updated"
  
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

def parse(String description) {
  def result = null

  log.debug "PARSE: ${description}"
  if (description.startsWith("Err")) {
    if (description.startsWith("Err 106")) {
      if (state.sec) {
        log.debug description
      } else {
        result = createEvent(
          descriptionText: "This device failed to complete the network security key exchange. If you are unable to control it via SmartThings, you must remove it from your network and add it again.",
          eventType: "ALERT",
          name: "secureInclusion",
          value: "failed",
          isStateChange: true,
        )
      }
    } else {
      result = createEvent(value: description, descriptionText: description, isStateChange: true)
    }
  } else if (description != "updated") {
    def cmd = zwave.parse(description) // , [0x20: 1, 0x25: 1, 0x27: 1, 0x32: 1, 0x70: 1, 0x72: 2, 0x86: 1])
	
    if (cmd) {
        result = zwaveEvent(cmd)
      
      
      if (!result) {
        log.warning "Parse Failed and returned ${result} for command ${cmd}"
        result = createEvent(value: description, descriptionText: description)
      } else {
        // log.debug "RESULT: ${result}"
      }
    } else {
      log.info "zwave.parse() failed: ${description}"
      result = createEvent(value: description, descriptionText: description)
    }
  }
    
  return result
}

def getCommandClassVersions() {
  [ 
    0x20: 1,  // Basic
    0x56: 1,  // Crc16Encap
    0x70: 1,  // Configuration
  ]
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
  log.debug("$device.displayName $cmd")
  
  [createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical") ]
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
  log.debug("$device.displayName $cmd")
  
  [ createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical") ]
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
  log.debug("$device.displayName $cmd")
  
  [ createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "digital") ]
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
      createEvent(name: "level", value: cmd.level, isStateChange: true, displayed: true),
      createEvent(name: "switch", value: cmd.level == 0 ? "on" : "off", isStateChange: true, displayed: true),
    ]
  }

  if (cmd.sceneId == 1) {
    if (cmd.level != 255) {
      sendCommands([
        zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: cmd.sceneId, dimmingDuration: 0, level: 255, override: true),
      ])
    }
  } else if (cmd.sceneId == 2) {
    if (cmd.level) {
      sendCommands([
        zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId: cmd.sceneId, dimmingDuration: 0, level: 0, override: true),
      ])
    }
  }

  String scene_name = "Scene_$cmd.sceneId"
  
  [ createEvent(name: "$scene_name", value: cmd.level, isStateChange: true, displayed: true),
    createEvent(name: "Scene", value: cmd.sceneId, isStateChange: true, displayed: true),
  ]
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd) {
  log.debug("$device.displayName $cmd")
  Integer set_sceen = ((cmd.sceneId + 1) / 2) as Integer
  buttonEvent(set_sceen, false, "digital")
  [ createEvent(name: "setScene", value: "Setting", isStateChange: true, displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.meterv3.MeterReport cmd) {
  log.debug("$device.displayName $cmd")
  
	if (cmd.scale == 0) {
		[ createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kWh") ]
	} else if (cmd.scale == 1) {
		[ createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kVAh") ]
	}
	else {
		[ createEvent(name: "power", value: Math.round(cmd.scaledMeterValue), unit: "W") ]
	}
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
  log.debug("$device.displayName $cmd")
  
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
  log.debug("$device.displayName command not implemented: $cmd")
  [ createEvent(name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false) ]
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  log.debug("$device.displayName $cmd")
  
  def manufacturerCode = String.format("%04X", cmd.manufacturerId)
  def productTypeCode = String.format("%04X", cmd.productTypeId)
  def productCode = String.format("%04X", cmd.productId)
  
  sendEvent(name: "ManufacturerCode", value: manufacturerCode)
  sendEvent(name: "ProduceTypeCode", value: productTypeCode)
  sendEvent(name: "ProductCode", value: productCode)
  
  if (cmd.manufacturerName ) {
    state.manufacturer == String.format("${state.manufacturer}")
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
    sendCommands( [
      zwave.configurationV1.configurationGet(parameterNumber: 3),
      zwave.firmwareUpdateMdV1.firmwareMdGet(),
      zwave.associationV2.associationGroupingsGet(),
      ])
  } else {
    sendCommands( [
      zwave.configurationV1.configurationGet(parameterNumber: 1),
      ] )
  }
}

def zwaveEvent(physicalgraph.zwave.commands.dcpconfigv1.DcpListSupportedReport cmd) {
  log.debug "$device.displayName has not implemented: $cmd"
  [ createEvent(descriptionText: "$device.displayName no implementation: $cmd", displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
  log.debug("$device.displayName $cmd")
  
  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  [ createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd) {
  log.debug("$device.displayName $cmd")
  state.reset = true
  [ createEvent(name: "DeviceReset", value: state.reset, descriptionText: cmd.toString(), isStateChange: true, displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd) {
  log.debug ("$device.displayName $cmd")
  
  def firmware_report = String.format("%s-%s-%s", cmd.checksum, cmd.firmwareId, cmd.manufacturerId)
  updateDataValue("FirmwareMdReport", firmware_report)
  [ createEvent(name: "FirmwareMdReport", value: firmware_report, descriptionText: "$device.displayName FIRMWARE_REPORT: $firmware_report", isStateChange: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
  log.debug ("$device.displayName $cmd")

  state.groups = cmd.supportedGroupings

  if (cmd.supportedGroupings) {
    def cmds = []
    for (def x = 1; x <= cmd.supportedGroupings; x++) {
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
  log.debug ("$device.displayName $cmd")
  [ createEvent(descriptionText: "$device.displayName AssociationGroupInfoReport: $cmd", isStateChange: true, displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd) {
  log.debug ("$device.displayName $cmd")
  [ createEvent(descriptionText: "$device.displayName AssociationGroupNameReport: $cmd", displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
  log.debug ("$device.displayName $cmd")

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
    log.error "Unknown group ${cmd.groupingIdentifier}"
    return [ createEvent(
               value: "${final_string}",
               descriptionText: "Association group ${cmd.groupingIdentifier} is unknown",
               displayed: true) ]
  }

  if ( state.hasLifeLine == true ) {
    state.isAssociated = true
  } else { 
    state.isAssociated = false
  }

  if (! state.isAssociated ) {
    return sendCommands([ 
          zwave.associationV1.associationSet(groupingIdentifier: 0x01, nodeId: [zwaveHubNodeId]),
        ])
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
  log.error("$device.displayName $cmd")
  [ createEvent(descriptionText: "$device.displayName command not implemented: $cmd", displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd) {
  log.debug("$device.displayName $cmd")
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
  delayBetween([
    zwave.switchBinaryV1.switchBinaryGet().format(),
    zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
  ])
}

/**
  * PING is used by Device-Watch in attempt to reach the Device
**/
def ping() {
  refresh()
}

def refresh() {
  delayBetween([
    zwave.switchBinaryV1.switchBinaryGet().format(),
    zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
  ])
}

def reset() {
	return [
		zwave.meterV2.meterReset().format(),
		zwave.meterV2.meterGet(scale: 0).format()
	]
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

private command(physicalgraph.zwave.Command cmd) {
  if (state.sec) {
    zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
  } else {
    cmd.format()
  }
}

private commands(commands, delay=200) {
  sendCommands(commands.collect{ command(it) }, delay)
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
