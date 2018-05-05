// vim :set ts=2 sw=2 sts=2 expandtab smarttab :
/**
 *  Copyright 2017-2018 Brian Aker <brian@tangent.org>
 *  Copyright 2015 SmartThings
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
  return "v2.88"
}

def getAssociationGroup() {
  return 1
}

metadata {
  definition (name: "Aeon Labs Smart Energy Switch", namespace: "TangentOrgThings", author: "brian@tangent.org", ocfDeviceType: "oic.d.switch") {
    capability "Actuator"
    capability "Acceleration Sensor"
    capability "Energy Meter"
    capability "Health Check"
    capability "Motion Sensor"
    capability "Polling"
    capability "Power Meter"
    capability "Refresh"
    capability "Sensor"
    capability "Outlet"

    command "reset"

    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message

    attribute "Associated", "string"
    attribute "LifeLine", "string"
    attribute "driverVersion", "string"
    attribute "FirmwareMdReport", "string"
    attribute "firmwareVersion", "string"
    attribute "Manufacturer", "string"
    attribute "ManufacturerCode", "string"
    attribute "MSR", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"

    attribute "NIF", "string"

    fingerprint type: "1001", mfr: "0086",  prod: "0003", model: "0006", deviceJoinName: "Aeon Smart Energy Switch 1.43"
    // fingerprint mfr: "1001", prod: "0003", model: "0006", inClusters: "0x20,0x25,0x32,0x27,0x70,0x85,0x72,0x86", ccOut: "0x20,0x82"
  }

  // simulator metadata
  simulator {
    status "on":  "command: 2003, payload: FF"
    status "off": "command: 2003, payload: 00"

    for (int i = 0; i <= 100; i += 10) {
      status "energy  ${i} kWh": new physicalgraph.zwave.Zwave().meterV2.meterReport(
      scaledMeterValue: i, precision: 3, meterType: 0, scale: 0, size: 4).incomingMessage()
    }

    // reply messages
    reply "2001FF,delay 100,2502": "command: 2503, payload: FF"
    reply "200100,delay 100,2502": "command: 2503, payload: 00"

  }

  preferences {
    input "disbableDigitalOff", "bool", title: "Disable Digital Off", description: "Disallow digital turn off", required: false
    input "debugLevel", "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  }

  // tile definitions
	tiles {
		multiAttributeTile(name:"switch", type:"generic", width:6, height:4) {
			tileAttribute("device.acceleration", key: "PRIMARY_CONTROL") {
				attributeState("active", label:'${name}', icon:"st.motion.motion.active", backgroundColor:"#00A0DC")
				attributeState("inactive", label:'${name}', icon:"st.motion.motion.inactive", backgroundColor:"#CCCCCC", defaultState: true)
			}
			tileAttribute("device.switch", key: "SECONDARY_CONTROL") {
				attributeState("on", label:'${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC")
				attributeState("off", label:'${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff")
			}
		}

		valueTile("power", "device.power", decoration: "flat") {
			state "default", label:'${currentValue}', unit:"W"
		}

		valueTile("energy", "device.energy", decoration: "flat") {
			state "default", label:'${currentValue}', unit:"kWh"
		}

		standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat") {
			state "default", label:'reset kWh', action:"reset"
		}

		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		valueTile("driverVersion", "device.driverVersion", width:2, height:2, inactiveLabel: true, decoration: "flat") {
			state "default", label: '${currentValue}'
		}

		main "switch"
		details(["switch", "power", "energy", "reset", "refresh", "driverVersion"])
	}
}

def deviceCommandClasses() { // 25, 31, 32, 27, 70, 85, 72, 86
  [
    0x20: 1,  // Basic
    0x25: 1,  // Switch Binary
    0x27: 1,  // Switch All
    0x31: 5,  // SensorMultilevel V4
    0x32: 3,  // Meter V2
    0x70: 2,  // Configuration V2
    0x72: 2,  // Manufacturer Specific V2
    0x86: 1,  // Version
    0x85: 2,  // Association	0x85	V1 V2
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
    result << createEvent(name: "logMessage", value: "parse() called with NULL description", descriptionText: "$device.displayName")
  } else if (description != "updated") {
    def cmd = zwave.parse(description, deviceCommandClasses())

    if (cmd) {
      def cmds_result = []
      def cmds = checkConfigure()

      if (cmds) {
        result << response( delayBetween ( cmds ))
      }
      zwaveEvent(cmd, result)

    } else {
      log.warn "zwave.parse() failed for: ${description}"
      result << createEvent(name: "lastError", value: "zwave.parse() failed for: ${description}", descriptionText: description)
    }
  }

  return result
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$device.displayName: $cmd");

  String final_string
  if (cmd.nodeId) {
    def string_of_assoc = ""
    cmd.nodeId.each {
      string_of_assoc += "${it}, "
    }
    def lengthMinus2 = string_of_assoc.length() - 3
    final_string = string_of_assoc.getAt(0..lengthMinus2)
  }

  if (cmd.groupingIdentifier == getAssociationGroup()) {
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      Boolean isStateChange = state.isAssociated ?: false
      result << createEvent(name: "Associated",
                            value: "${final_string}",
                            descriptionText: "${final_string}",
                            isStateChange: isStateChange)

      state.isAssociated = true
    } else {
      Boolean isStateChange = state.isAssociated ? true : false
      result << createEvent(name: "Associated",
                          value: "",
                          descriptionText: "${final_string}",
                          isStateChange: isStateChange)
      state.isAssociated = false
    }
  } else if (cmd.groupingIdentifier == 1) {
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      Boolean isStateChange = state.isAssociated ?: false
      result << createEvent(name: "LifeLine",
                            value: "${final_string}",
                            descriptionText: "${final_string}",
                            isStateChange: isStateChange)

      state.isAssociated = true
    } else {
      Boolean isStateChange = state.isAssociated ? true : false
      result << createEvent(name: "LifeLine",
                          value: "${final_string}",
                          descriptionText: "${final_string}",
                          isStateChange: isStateChange)
      state.isAssociated = false
    }
  } else {
    Boolean isStateChange = state.isAssociated ? true : false
    result << createEvent(name: "LifeLine",
                          value: "misconfigured",
                          descriptionText: "misconfigured group ${cmd.groupingIdentifier}",
                          isStateChange: isStateChange)
  }

  if (state.isAssociated == false) {
    result << delayBetween([
                   zwave.associationV1.associationSet(groupingIdentifier: getAssociationGroup(), nodeId: [1,zwaveHubNodeId]),
                   zwave.associationV1.associationSet(groupingIdentifier: 1, nodeId: [1,zwaveHubNodeId]),
                   zwave.associationV1.associationGet(groupingIdentifier: getAssociationGroup())
                 ], 1000)
  }

  return result
}

// def zwaveEvent(physicalgraph.zwave.commands.meterv2.MeterReport cmd) {
def zwaveEvent(physicalgraph.zwave.commands.meterv3.MeterReport cmd, result) {
  logger("$device.displayName: $cmd");

  if (cmd.meterType != 1) {
    result << createEvent(descriptionText: "$device.displayName bad type: $cmd")
    return
  }

  if (cmd.scale == 0) {
    result << createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kWh")
  } else if (cmd.scale == 1) {
    result << createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kVAh")
  } else if (cmd.scale == 2) {
    result << createEvent(name: "power", value: cmd.scaledMeterValue ? Math.round(cmd.scaledMeterValue) : 0, unit: "W")
    result << createEvent(name: "acceleration", value: cmd.scaledMeterValue ? "active" : "inactive")
   } else {
    result << createEvent(descriptionText: "$device.displayName scale not implemented: $cmd")
  }
}

// def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv4.SensorMultilevelReport cmd) {
def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd, result) {
  logger("$device.displayName: $cmd");

  def map = [ descriptionText: "$device.displayName ${cmd.scaledSensorValue}" ]

  map.value = cmd.scaledSensorValue ? cmd.scaledSensorValue.toString() : ""

    Boolean isActive = false
	switch (cmd.sensorType) {
		case 1:
			map.name = "temperature"
			map.unit = cmd.scale == 1 ? "F" : "C"
			break;
		case 2:
			map.name = "value"
			map.unit = cmd.scale == 1 ? "%" : ""
			break;
		case 3:
			map.name = "illuminance"
			map.value = cmd.scaledSensorValue ? cmd.scaledSensorValue.toInteger().toString() : 0
			map.unit = "lux"
			break;
		case 4:
			map.name = "power"
            // map.value = cmd.scaledSensorValue ? cmd.scaledSensorValue.toInteger().toString() : 0
			map.unit = cmd.scale == 1 ? "Btu/h" : "W"
            if (cmd.scale != 1) sendEvent(name: "acceleration", value: cmd.scaledSensorValue ? "active" : "inactive")
			break;
		case 5:
			map.name = "humidity"
			map.value = cmd.scaledSensorValue ? cmd.scaledSensorValue.toInteger().toString() : 0
			map.unit = cmd.scale == 0 ? "%" : ""
			break;
		case 6:
			map.name = "velocity"
			map.unit = cmd.scale == 1 ? "mph" : "m/s"
			break;
		case 8:
		case 9:
			map.name = "pressure"
			map.unit = cmd.scale == 1 ? "inHg" : "kPa"
			break;
		case 0xE:
			map.name = "weight"
			map.unit = cmd.scale == 1 ? "lbs" : "kg"
			break;
		case 0xF:
			map.name = "voltage"
			map.unit = cmd.scale == 1 ? "mV" : "V"
			break;
		case 0x10:
			map.name = "current"
			map.unit = cmd.scale == 1 ? "mA" : "A"
            sendEvent(name: "motion", value: cmd.scaledSensorValue ? "active" : "inactive")
			break;
		case 0x12:
			map.name = "air flow"
			map.unit = cmd.scale == 1 ? "cfm" : "m^3/h"
			break;
		case 0x1E:
			map.name = "loudness"
			map.unit = cmd.scale == 1 ? "dBA" : "dB"
			break;
        default:
            map.descriptionText = cmd.toString()
            break;
	}

  result << createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$device.displayName: $cmd");

  result << createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical")
  result << response(zwave.meterV2.meterGet(scale:02))
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$device.displayName: $cmd");

  result << createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical")
  result << response(zwave.meterV2.meterGet(scale:0))
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, result) {
  logger("$device.displayName: $cmd");

  result << createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "digital")
  result << response(zwave.meterV2.meterGet(scale:0))
}

def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  logger("$device.displayName command not implemented: $cmd")
  result << createEvent(descriptionText: "$device.displayName command not implemented: $cmd", displayed: true)
}

def on() {
  logger("$device.displayName: on()");

  delayBetween([
		zwave.switchBinaryV1.switchBinarySet(value: 0xFF).format(),
        zwave.switchBinaryV1.switchBinaryGet().format(),
	])
}

def off() {
  logger("$device.displayName: off()");

  if (settings.disbableDigitalOff) {
    logger("..off() disabled")

    return zwave.switchBinaryV1.switchBinaryGet().format()
  }

  delayBetween([
    // Lets not turn off the Dryer or the Washer by accident
    // zwave.basicV1.basicSet(value: 0x00),
    zwave.switchBinaryV1.switchBinarySet(value: 0xFF).format(),
    zwave.switchBinaryV1.switchBinaryGet().format(),
  ])
}

def ping() {
  logger("$device.displayName: ping()");
  zwave.switchBinaryV1.switchBinaryGet().format()
}

def poll() {
  logger("$device.displayName: poll()");
  zwave.switchBinaryV1.switchBinaryGet().format()
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd, result) {
  logger("$device.displayName: $cmd");

  result << createEvent(name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: true)
  result << response(zwave.meterV2.meterGet(scale:0))
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
  logger("$device.displayName: $cmd");

  state.manufacturer= cmd.manufacturerName ? cmd.manufacturerName : "Aeon"

  def manufacturerCode = String.format("%04X", cmd.manufacturerId)
  def productTypeCode = String.format("%04X", cmd.productTypeId)
  def productCode = String.format("%04X", cmd.productId)

  result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
  result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
  result << createEvent(name: "ProductCode", value: productCode)

  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  result << createEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName")

  Integer[] parameters = [ 1, 80, 90, 91, 92, 101, 102, 103, 111, 112, 113 ]

  def cmds = []
  parameters.each {
    cmds << zwave.configurationV1.configurationGet(parameterNumber: it).format()
  }

  result << createEvent(name: "Manufacturer", value: "${state.manufacturer}", descriptionText: "$device.displayName", isStateChange: true)

  if (cmds) {
		result << response(delayBetween(cmds, 1000))
	}
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd, result) {
  logger("$device.displayName: $cmd");

  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  result << createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: false)
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
    result << delayBetween([
      zwave.switchAllV1.switchAllSet(mode: 0x00).format(),
      zwave.switchAllV1.switchAllGet().format(),
    ])
  } else {
    result << createEvent(name: "SwitchAll", value: msg, isStateChange: true)
  }
}

def refresh() {
  logger("$device.displayName: refresh()");
  delayBetween([
    zwave.switchBinaryV1.switchBinaryGet().format(),
    zwave.meterV2.meterGet(scale: 0).format(),
    zwave.meterV2.meterGet(scale: 2).format(),
    ])
}

def reset() {
  logger("$device.displayName: reset()");
  sendCommands([
		zwave.meterV2.meterReset(),
		zwave.meterV2.meterGet(scale: 0),
        zwave.meterV2.meterGet(scale: 2),
	])
}

def checkConfigure() {
  return []
}

private prepDevice() {
  [
    zwave.versionV1.versionGet(),
    zwave.manufacturerSpecificV1.manufacturerSpecificGet(),
    zwave.configurationV1.configurationSet(parameterNumber: 1, size: 1, scaledConfigurationValue: 0),
    zwave.configurationV1.configurationSet(parameterNumber: 80, size: 1, scaledConfigurationValue: 2),
    zwave.configurationV1.configurationSet(parameterNumber: 90, size: 1, scaledConfigurationValue: 1),
    zwave.configurationV1.configurationSet(parameterNumber: 91, size: 2, scaledConfigurationValue: 20),
    zwave.configurationV1.configurationSet(parameterNumber: 92, size: 1, scaledConfigurationValue: 10),
    zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 8),   // energy in W
    zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: 3600), // every 5 min
    zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: 0),   // energy in W
    zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: 0), // every 5 min
    zwave.configurationV1.configurationSet(parameterNumber: 103, size: 4, scaledConfigurationValue: 0),
    zwave.configurationV1.configurationSet(parameterNumber: 113, size: 4, scaledConfigurationValue: 0),
    zwave.switchBinaryV1.switchBinaryGet(),
    zwave.meterV2.meterGet(scale: 0),
    zwave.meterV2.meterGet(scale: 2),
    zwave.switchAllV1.switchAllGet(),
    zwave.zwaveCmdClassV1.requestNodeInfo(),
  ]
}

def installed() {
  log.info("$device.displayName installed()")
  state.loggingLevelIDE = 4

  // Device-Watch simply pings if no device events received for 86220 (one day minus 3 minutes)
  // sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])

  if (0) {
  def zwInfo = getZwaveInfo()
  log.debug("$device.displayName $zwInfo")
  sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
  }

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed:true)
  state.driverVersion = getDriverVersion()

  sendCommands( prepDevice(), 2000 )
}

def updated() {
  if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
    return
  }
  log.info("$device.displayName updated() debug: ${debugLevel}")

  state.loggingLevelIDE = debugLevel ? debugLevel : 4

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)

  if (0) {
    def zwInfo = getZwaveInfo()
    log.debug("$device.displayName $zwInfo")
    sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
  }

  // Device-Watch simply pings if no device events received for 86220 (one day minus 3 minutes)
  //sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed:true)

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
 *    messages by sending events for the device's logMessage attribute.
 *    Configured using configLoggingLevelIDE and configLoggingLevelDevice preferences.
 **/
private logger(msg, level = "trace") {
	switch(level) {
		case "error":
		if (state.loggingLevelIDE >= 1) log.error msg
			if (state.loggingLevelDevice >= 1) sendEvent(name: "logMessage", value: "ERROR: ${msg}", displayed: false, isStateChange: true)
				break

		case "warn":
		if (state.loggingLevelIDE >= 2) log.warn msg
			if (state.loggingLevelDevice >= 2) sendEvent(name: "logMessage", value: "WARNING: ${msg}", displayed: false, isStateChange: true)
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
