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

String getDriverVersion() {
  return "v3.05"
}

Integer getAssociationGroup() {
  return 1
}

Integer getWattMin() {
  return 5
}

metadata {
  definition (name: "Aeon Labs Smart Energy Switch", namespace: "TangentOrgThings", author: "brian@tangent.org", ocfDeviceType: "oic.d.switch") {
    capability "Actuator"
    capability "Acceleration Sensor"
    capability "Energy Meter"
    capability "Polling"
    capability "Power Meter"
    capability "Refresh"
    capability "Sensor"
    capability "Switch"

    command "reset"

    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message
    attribute "parseErrorCount", "number"        // Last error message
    attribute "unknownCommandErrorCount", "number"        // Last error message

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

		valueTile("power", "device.power", width:2, height:2, inactiveLabel: true, decoration: "flat") {
			state "default", label:'${currentValue} W', unit:"W"
		}

		valueTile("energy", "device.energy", width:2, height:2, inactiveLabel: true, decoration: "flat") {
			state "default", label:'${currentValue} kWh', unit:"kWh"
		}

		standardTile("reset", "device.energy", width:2, height:2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'reset kWh', action:"reset"
		}

		standardTile("refresh", "device.switch", width:2, height:2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		valueTile("driverVersion", "device.driverVersion", width:2, height:2, inactiveLabel: true, decoration: "flat") {
			state "default", label: '${currentValue}'
		}

		main "switch"
		details(["switch", "power", "energy", "reset", "refresh", "driverVersion"])
	}
}

def getCommandClassVersions() { // 25, 31, 32, 27, 70, 85, 72, 86
  [
    0x20: 1,  // Basic
    0x25: 1,  // Switch Binary
    0x27: 1,  // Switch All
    0x31: 3,  // SensorMultilevel V3 sensormultilevelv3
    0x32: 3,  // Meter V2
    0x70: 2,  // Configuration V2
    0x72: 2,  // Manufacturer Specific V2
    0x82: 1, // Hail
    0x86: 1,  // Version
    0x85: 2,  // Association	0x85	V1 V2
  ]
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
    logger("description: '$description'")
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

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$device.displayName: $cmd");

  String final_string
  if (cmd.nodeId) {
    def string_of_assoc = ""
    cmd.nodeId.each {
      string_of_assoc += "${it}, "
    }
    Integer lengthMinus2 = ( string_of_assoc.length() > 3 ) ? string_of_assoc.length() - 3 : 0
    final_string = lengthMinus2 ? string_of_assoc.getAt(0..lengthMinus2) : string_of_assoc
  }
  
  updateDataValue("Group Associations #${cmd.groupingIdentifier}", "${final_string}")

  if (cmd.groupingIdentifier == getAssociationGroup()) {
    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      Boolean isStateChange = state.isAssociated ? false : true
      result << createEvent(name: "Associated",
                            value: "${final_string}",
                            descriptionText: "${final_string}",
                            isStateChange: isStateChange)

      state.isAssociated = true
    } else {
      Boolean isStateChange = state.isAssociated ?: false
      result << createEvent(name: "Associated",
                          value: "",
                          descriptionText: "${final_string}",
                          isStateChange: isStateChange)
      state.isAssociated = false
    }
  } else {
    logger("Unknown group ${cmd.groupingIdentifier}", "error")
  }

  if (state.isAssociated == false) {
    result << response(delayBetween([
                   zwave.associationV1.associationSet(groupingIdentifier: getAssociationGroup(), nodeId: [zwaveHubNodeId]),
                   zwave.associationV1.associationGet(groupingIdentifier: getAssociationGroup())
                 ], 1000))
  } 
}

def zwaveEvent(physicalgraph.zwave.commands.meterv3.MeterSupportedReport cmd, result) {
  logger("$device.displayName: $cmd");
}

def zwaveEvent(physicalgraph.zwave.commands.meterv3.MeterReport cmd, result) {
  logger("$device.displayName: $cmd");

  switch (cmd.scale) {
    case 0x00:
    result << createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kWh")
    break;
    case 0x01:
    result << createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kVAh")
    break;    
    case 0x02:
    result << createEvent(name: "power", value: Math.round(cmd.scaledMeterValue), unit: "W")
    result << createEvent(name: "acceleration", value: ( Math.round(cmd.scaledMeterValue) > getWattMin() ) ? "active" : "inactive")
    break;
    default:
    result << createEvent(descriptionText: "$device.displayName scale not implemented: $cmd")
    break;
  }
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelSupportedSensorReport cmd, result) {
  logger("$device.displayName: $cmd", "warn");
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelSupportedScaleReport cmd, result) {
  logger("$device.displayName: $cmd", "warn");
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd, result) {
  logger("$device.displayName: $cmd");

  switch (cmd.sensorType) {
    case 4:
    if (cmd.scale == 0) {
      result << createEvent(name: "power", value: Math.round(cmd.scaledSensorValue), unit: "W", descriptionText: "$device.displayName ${cmd.scaledSensorValue}")
      result << createEvent(name: "acceleration", value: ( Math.round(cmd.scaledSensorValue) > getWattMin() ) ? "active" : "inactive")
      return
    }
    logger("$device.displayName: SensorMultilevelReport Unknown power scale ${cmd.scale}", "error")
    break;
    default:
    if (cmd.precision == 0 && cmd.scaledSensorValue == 0) {
      return
    }
    break;
  }

  logger("$device.displayName: SensorMultilevelReport Unknown sensor type ${cmd.sensorType}", "error")
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv3.SensorMultilevelReport cmd, result) {
  logger("$device.displayName: $cmd");

  switch (cmd.sensorType) {
    case 4:
    if (cmd.scale == 0) {
      result << createEvent(name: "power", value: Math.round(cmd.scaledSensorValue), unit: "W", descriptionText: "$device.displayName ${cmd.scaledSensorValue}")
      result << createEvent(name: "acceleration", value: ( Math.round(cmd.scaledSensorValue) > getWattMin() ) ? "active" : "inactive")
      return
    }
    logger("$device.displayName: SensorMultilevelReport Unknown power scale ${cmd.scale}", "error")
    break;
    default:
    if (cmd.precision == 0 && cmd.scaledSensorValue == 0) {
      return
    }
    break;
  }

  logger("$device.displayName: SensorMultilevelReport Unknown sensor type ${cmd.sensorType}", "error")
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$device.displayName: $cmd");

  result << createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical")
  result << response("delay 1200")
  result << response(zwave.sensorMultilevelV3.sensorMultilevelGet())
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$device.displayName: $cmd");

  result << createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical")
  result << response("delay 1200")
  result << response(zwave.sensorMultilevelV3.sensorMultilevelGet())
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, result) {
  logger("$device.displayName: $cmd");

  result << createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "digital")
  result << response("delay 1200")
  result << response(zwave.sensorMultilevelV3.sensorMultilevelGet())
}

def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  logger("$device.displayName command not implemented: $cmd", "unknownCommand")
}

def on() {
  logger("$device.displayName: on()");

  delayBetween([
		zwave.switchBinaryV1.switchBinarySet(switchValue: 0xFF).format(),
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
    zwave.switchBinaryV1.switchBinarySet(switchValue: 0x00).format(),
    zwave.switchBinaryV1.switchBinaryGet().format(),
  ])
}

def ping() {
  logger("$device.displayName: ping()");
  zwave.switchBinaryV1.switchBinaryGet().format()
}

def poll() {
  logger("$device.displayName: poll()");
  if (0) {
    zwave.switchBinaryV1.switchBinaryGet().format()
  }
  sendCommands([
    zwave.switchBinaryV1.switchBinaryGet(),
    // zwave.sensorMultilevelV3.sensorMultilevelGet(),
    zwave.meterV2.meterGet(scale: 0x00),
    zwave.meterV2.meterGet(scale: 0x02),
  ])
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd, result) {
  logger("$device.displayName: $cmd");

  result << createEvent(name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: true)
  result << response(zwave.switchBinaryV1.switchBinaryGet())
  result << response(zwave.meterV2.meterGet(scale: 0x00))
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd, result) {
  logger("$device.displayName $cmd")

  updateDataValue("Configuration #${cmd.parameterNumber}", "${cmd.scaledConfigurationValue}")
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
  logger("$device.displayName: $cmd");

  state.manufacturer= cmd.manufacturerName ? cmd.manufacturerName : "Aeon"

  String manufacturerCode = String.format("%04X", cmd.manufacturerId)
  String productTypeCode = String.format("%04X", cmd.productTypeId)
  String productCode = String.format("%04X", cmd.productId)

  result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
  result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
  result << createEvent(name: "ProductCode", value: productCode)

  String msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
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

  String text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
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
  logger("Switch All Mode: ${msg}", "info")

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
    // zwave.sensorMultilevelV3.sensorMultilevelGet().format(),
    zwave.meterV2.meterGet(scale: 0x00).format(),
    zwave.meterV2.meterGet(scale: 0x02).format(),
    ])
}

def reset() {
  logger("$device.displayName: reset()");
  sendCommands([
    zwave.meterV2.meterReset(),
    // zwave.sensorMultilevelV3.sensorMultilevelGet(),
    zwave.meterV2.meterGet(scale: 0x00),
    zwave.meterV2.meterGet(scale: 0x02),
  ])
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
    zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 8),   // energy in KW
    zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: 3600), // every 60 min
    zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: 0),
    zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: 0),
    zwave.configurationV1.configurationSet(parameterNumber: 103, size: 4, scaledConfigurationValue: 0),
    zwave.configurationV1.configurationSet(parameterNumber: 113, size: 4, scaledConfigurationValue: 0),
    zwave.switchBinaryV1.switchBinaryGet(),
    // zwave.sensorMultilevelV3.sensorMultilevelGet(),
    zwave.meterV2.meterGet(scale: 0x00),
    zwave.meterV2.meterGet(scale: 0x02),
    zwave.meterV2.meterSupportedGet(),
    zwave.switchAllV1.switchAllGet(),
    zwave.zwaveCmdClassV1.requestNodeInfo(),
  ]
}

def installed() {
  logger("$device.displayName installed()")

  if (0) {
  def zwInfo = getZwaveInfo()
  log.debug("$device.displayName $zwInfo")
  sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
  }

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed:true)

  sendCommands( prepDevice(), 2000 )
}

def updated() {
  if ( state.updatedDate && ((Calendar.getInstance().getTimeInMillis() - state.updatedDate)) < 5000 ) {
    return
  }
  logger("$device.displayName updated() debug: ${settings.debugLevel}")

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)
  state.parseErrorCount = 0
  state.unknownCommandErrorCount = 0

  if (0) {
    def zwInfo = getZwaveInfo()
    log.debug("$device.displayName $zwInfo")
    sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
  }

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

