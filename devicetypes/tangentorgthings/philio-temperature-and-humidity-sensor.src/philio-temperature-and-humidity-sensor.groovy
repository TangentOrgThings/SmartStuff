// vim: set filetype=groovy tabstop=2 shiftwidth=2 softtabstop=2 expandtab smarttab :

/**
 *  Philio Temperature and Humidity Sensor PAT02-B
 *
 *  Copyright Brian Aker
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
 
import physicalgraph.*

String getDriverVersion () {
  return "v0.03"
}

metadata {
	definition (name: "Philio Temperature and Humidity Sensor", namespace: "TangentOrgThings", author: "Brian Aker", vid: "generic-leak-5", ocfDeviceType: "x.com.st.d.sensor.moisture") {
		capability "Battery"
		capability "Tamper Alert"
		capability "Temperature Measurement"
    capability "Relative Humidity Measurement"
    capability "Tamper Alert"
    capability "Configuration"

    attribute "DeviceReset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message

    fingerprint mfr: "013C", prod: "0020", model: "0002", deviceJoinName: "Philio 2-in-1 Sensor - Temperature and Humidity"
  }


	simulator {
		// TODO: define status and reply messages here
	}

  tiles {
    valueTile("temperature", "device.temperature", inactiveLabel: false) {
      state "temperature", label:'${currentValue}°',
      backgroundColors:[
        [value: 31, color: "#153591"],
        [value: 44, color: "#1e9cbb"],
        [value: 59, color: "#90d2a7"],
        [value: 74, color: "#44b621"],
        [value: 84, color: "#f1d801"],
        [value: 95, color: "#d04e00"],
        [value: 96, color: "#bc2323"]
      ]
    }
    valueTile("humidity", "device.humidity", inactiveLabel: false) {
      state "humidity", label:'${currentValue}% humidity', unit: "",
      backgroundColors:[
        [value: 31, color: "#153591"],
        [value: 44, color: "#1e9cbb"],
        [value: 59, color: "#90d2a7"],
        [value: 74, color: "#44b621"],
        [value: 84, color: "#f1d801"],
        [value: 95, color: "#d04e00"],
        [value: 96, color: "#bc2323"]
      ]
    }        
    standardTile("tamper", "device.tamper", decoration: "flat", width: 1, height: 1) {			
      state "clear", label: 'tamper clear', backgroundColor: "#ffffff"
      state "detected", label: 'tampered', action:"clearTamper", backgroundColor: "#ff0000"
    }        
    valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "battery", label: '${currentValue}% battery'
    }
    main(["temperature", "humidity"])
    details(["temperature", "humidity", "battery", "tamper"])
  }
}

def getCommandClassVersions() {
  // 
  return [
    0x20: 1,  // Basic
    0x30: 2,  // Sensor Binary
    0x31: 5,  // Sensor Multilevel
    0x59: 1,  // Association Grp Info
    0x5A: 1,  // Device Reset Locally
    // 0x5E: 2, //
    // 0x6C: 2, // Supervision
    0x70: 2,  // Configuration
    0x71: 3,  // Notification
    0x72: 2,  // Manufacturer Specific
    0x73: 1,  // Powerlevel
    0x7A: 2,  // Firmware Update Md HS-200 V4
    0x80: 1,  // Battery
    0x84: 2,  // Wake Up
    0x85: 2,  // Association  0x85  V1 V2
    0x86: 1,  // Version
    // 0x8F: 1,  // Multi Cmd 
    // 0x56: 1,  // Crc16 Encap
    // 0x9F: 1,  // Security 2 Command Class
    // Controlled
    0x98: 1,  // Application Status
  ]
}

def getConfigurationOptions(Integer model) {
  /*
  Parameter 5: Operation Mode
    1  	Disable the Flood function.
    8	  Setting the temperature scale. 0: Fahrenheit, 1:Celsius
    32	Disable the temperature report after event triggered. (1:Disable, 0:Enable)

  Parameter 7: Customer Function
    8  	Disable send out BASIC OFF after the flood event cleared. (1:Disable, 0:Enable)
    16	Notification Type, 0: Using Notification Report. 1: Using Sensor Binary Report.
    32	Disable Multi CC in auto report. (1:Disable, 0:Enable)
    64  Disable to report battery state when the device triggered. (1:Disable, 0:Enable)

  Parameter 10: Auto Report Battery Time
    0 - 127	The interval time for auto report the battery level.

  Parameter 13: Auto Report Temperature Time
    0 - 127	The interval time for auto report the temperature.

  Parameter 14: Auto Report Humidity Time
    0 - 127	The interval time for auto report the humidity.

  Parameter 20: Auto Report Tick Interval
    0 - 255	The interval time for auto report each tick.

  Parameter 21: Temperature Differential Report
    0 - 127	The temperature differential to report. 0 means turn off this function.

  Parameter 23: Humidity Differential Report
    0 - 60	The humidity differential to report.

   */
  return [ 5, 7, 10, 13, 14, 20, 21, 23 ]
}

def parse(String description) {
  def result = []
  
  clearTamper()

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

def zwaveEvent(physicalgraph.zwave.commands.multicmdv1.MultiCmdEncap cmd, result) {
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd, result) {
  def encapsulatedCommand = cmd.encapsulatedCommand(getCommandClassVersions())
  state.sec = 1
  log.debug "encapsulated: ${encapsulatedCommand}"
  if (encapsulatedCommand) {
    zwaveEvent(encapsulatedCommand, result)
  } else {
    log.warn "Unable to extract encapsulated cmd from $cmd"
    result << createEvent(descriptionText: cmd.toString())
  }
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityCommandsSupportedReport cmd, result) {
  log.info "Executing zwaveEvent 98 (SecurityV1): 03 (SecurityCommandsSupportedReport) with cmd: $cmd"
  state.sec = 1
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.NetworkKeyVerify cmd, result) {
  state.sec = 1
  log.info "Executing zwaveEvent 98 (SecurityV1): 07 (NetworkKeyVerify) with cmd: $cmd (node is securely included)"
  result << createEvent(name:"secureInclusion", value:"success", descriptionText:"Secure inclusion was successful", isStateChange: true)
}

def zwaveEvent(zwave.commands.notificationv3.NotificationReport cmd, result) {
  if (cmd.notificationType == 7) {
    switch (cmd.event) {
      case 0:
      result << createEvent(name: "tamper", value: "clear")
      break
      case 3:
      result << createEvent(name: "tamper", value: "detected", descriptionText: "$device.displayName was tampered")
      break
      default :
      break
    }
  } else {
    logger("Need to handle this cmd.notificationType: ${cmd.notificationType}", "warn")
  }
}

def zwaveEvent(zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd, result) {
  def map = [ displayed: true, value: cmd.scaledSensorValue.toString() ]
  switch (cmd.sensorType) {
    case 1:
    map.name = "temperature"
    map.unit = cmd.scale == 1 ? "F" : "C"
    break;
    case 5:
    map.name = "humidity"
    map.value = cmd.scaledSensorValue.toInteger().toString()
    map.unit = cmd.scale == 0 ? "%" : ""
    break;
  }

  result << createEvent(map)
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

  if (cmd.groupingIdentifier > 2) {
    logger("Unknown Group Identifier", "error");
    return
  }

  // Lifeline
  String string_of_assoc = ""
  if (cmd.nodeId) {
    string_of_assoc = cmd.nodeId.join(",")
  }

  String event_value = "${string_of_assoc}"

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
    if (0) {
      if (cmd.nodeId.any { it == zwaveHubNodeId }) {
        result << response( delayBetween([
          zwave.associationV1.associationRemove(groupingIdentifier: cmd.groupingIdentifier, nodeId: zwaveHubNodeId).format(),
          zwave.associationV1.associationGet(groupingIdentifier: cmd.groupingIdentifier).format(),
        ]))
      }
    }
  }

  String group_name = ""
  switch (cmd.groupingIdentifier) {
    case 1:
    group_name = "Lifeline"
      break;
    case 2:
    group_name = "On/Off Control"
    break;
    default :
    group_name = "Unknown";
    break;
  }

  updateDataValue("$group_name", "$event_value")
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

def zwaveEvent(zwave.commands.versionv1.VersionReport cmd, result) {
  logger("$cmd")

  String text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  String zWaveProtocolVersion = "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  updateDataValue("firmwareVersion", "${state.firmwareVersion}")
  updateDataValue("zWaveProtocolVersion", "${zWaveProtocolVersion}")
}

def zwaveEvent(zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd, result) {
  logger("$cmd")
  String firmware_report = String.format("%s-%s-%s", cmd.checksum, cmd.firmwareId, cmd.manufacturerId)
  updateDataValue("FirmwareMdReport", "$firmware_report")
}

def zwaveEvent(zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  String manufacturerCode = String.format("%04X", cmd.manufacturerId)
  String productTypeCode = String.format("%04X", cmd.productTypeId)
  String productCode = String.format("%04X", cmd.productId)

  updateDataValue("ManufacturerCode", manufacturerCode)
  updateDataValue("ProduceTypeCode", productTypeCode)
  updateDataValue("ProductCode", productCode)

  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)

  Integer[] parameters = getConfigurationOptions(cmd.productId)

  def cmds = []
  parameters.each {
    cmds << zwave.configurationV1.configurationGet(parameterNumber: it).format()
  }

  cmds << zwave.manufacturerSpecificV2.deviceSpecificGet().format();

  result << response(delayBetween(cmds, 1000))
  result << response( zwave.versionV1.versionGet() )
}

def zwaveEvent(zwave.commands.powerlevelv1.PowerlevelReport cmd, result) {
  logger("zwaveEvent(): Powerlevel Report received: ${cmd}")

  String device_power_level = (cmd.powerLevel > 0) ? "minus${cmd.powerLevel}dBm" : "NormalPower"
  logger("Powerlevel Report: Power: ${device_power_level}, Timeout: ${cmd.timeout}", "info")
  updateDataValue("Powerlevel Report", "Power: ${device_power_level}, Timeout: ${cmd.timeout}")
}

def zwaveEvent(zwave.commands.powerlevelv1.PowerlevelTestNodeReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.configurationv2.ConfigurationReport cmd, result) {
	logger("ConfigurationReport: $cmd")
}

def zwaveEvent(zwave.commands.wakeupv1.WakeUpNotification cmd, result) {
	logger("${device.displayName} woke up")

  def cmds = []
  cmds << zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 0x05).format()
  cmds << zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 0x01).format()
  result << response(delayBetween(cmds, 1000))
}

def zwaveEvent(zwave.commands.batteryv1.BatteryReport cmd, result) {
	logger("BatteryReport: $cmd")

	def map = [name: "battery", unit: "%", isStateChange: true]
	state.lastbatt = now()
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "$device.displayName battery is low!"
	} else {
		map.value = cmd.batteryLevel
		map.value = cmd.batteryLevel > 0 ? cmd.batteryLevel.toString() : 1
	}
	result << createEvent(map)
}

def zwaveEvent(zwave.commands.sensorbinaryv2.SensorBinaryReport cmd, result) {
	logger("SensorBinaryReport: $cmd")

	if (cmd.sensorType == 8) {
		logger("Device Moved", "info")

		if (cmd.sensorValue) {
			result << createEvent(name: "tamper", value: "detected", descriptionText: "$device.displayName was tampered")
			state.isTamper= true
		}
	} else {
    logger("Sensor Binary provided unknown type: $cmd", "warn")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, result) {
  logger("Basic Report: $cmd", "warn")
}

def zwaveEvent(zwave.commands.basicv1.BasicSet cmd, result) {
  logger("Device Set Associate 2 Basic Message: $cmd", "info")
}

def zwaveEvent(zwave.Command cmd, result) {
  logger("command not implemented: $cmd", "warn")
}

def prepDevice() {
  [
    zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
    zwave.firmwareUpdateMdV2.firmwareMdGet(),
    zwave.associationV2.associationGroupingsGet(),
    zwave.powerlevelV1.powerlevelGet(),
    zwave.batteryV1.batteryGet(),
  ]
}

def installed() {
  logger("installed()")

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  sendEvent(name: "tamper", value: "clear", displayed: false)

  // Set timer turning off trace output
  setTrace(true)

  sendCommands( prepDevice(), 2000 )
}

def updated() {
  configure()
}

def configure() {
  if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
    return
  }
  logger("updated() debug: ${settings.debugLevel}")
  setTrace(true)

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)

  // Device-Watch simply pings if no device events received for 32min(checkInterval)
  // sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID]) //, offlinePingable: "1"])

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  sendCommands( prepDevice(), 2000 )
  response(refresh())

  // Avoid calling updated() twice
  state.updatedDate = Calendar.getInstance().getTimeInMillis()
}

def clearTamper() {
  if (state.isTamper) {
    sendEvent(name: "tamper", value: "clear", isStateChange: true)
    state.isTamper= false
  }	
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
	// ? if (zwaveInfo.zw.contains("s"))
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

def setTrace(Boolean enable) {
	state.isTrace = enable

	if ( enable ) {
		runIn(60*5, followupTraceDisable)
	}
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

  // Integer log_level =  isTraceEnabled() ? 5 : 2
  Integer log_level = 5

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
