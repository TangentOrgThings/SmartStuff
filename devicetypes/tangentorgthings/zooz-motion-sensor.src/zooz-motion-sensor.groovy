// vim :set ts=2 sw=2 sts=2 expandtab smarttab :
/**
 *  Zooz ZSE02 Motion Sensor
 *
 *  Copyright 2016-2018 Brian Aker
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
  return "v3.19"
}

def getAssociationGroup() {
  return 1
}

metadata {
  definition (name: "Zooz Motion Sensor", namespace: "TangentOrgThings", author: "Brian Aker") {
    capability "Battery"
    capability "Motion Sensor"
    capability "Refresh"
    capability "Sensor"
    capability "Tamper Alert"

    attribute "DeviceReset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message

    attribute "Configured", "enum", ["false", "true"]
    attribute "Lifeline", "string"

    attribute "driverVersion", "string"
    attribute "MSR", "string"
    attribute "Manufacturer", "string"
    attribute "ManufacturerCode", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"
    attribute "WirelessConfig", "string"
    attribute "firmwareVersion", "string"
    attribute "AlarmTypeSupportedReport", "string"
    attribute "SensorMultilevelSupportedScaleReport", "string"

    attribute "WakeUp", "string"
    attribute "WakeUpInterval", "number"
    attribute "WakeUpNode", "number"

    attribute "LastActive", "string"
    attribute "LastAwake", "string"
    attribute "MotionTimeout", "number"

    attribute "NIF", "string"
  }

  /*
    vendorId: 338 (2017-06-17)
    vendor: (2017-06-17)
    productId: 3 (2017-06-17)
    productType: 1280 (2017-06-17)
    */

  // zw:S type:0701 mfr:0152 prod:0500 model:0003 ver:0.01 zwv:3.95 lib:06 cc:5E,85,59,71,80,5A,73,84,72,86 role:06 ff:8C07 ui:8C07
  fingerprint type: "0701", mfr: "0152", prod: "0500", model: "0003", deviceJoinName: "Zooz Motion Sensor ZSE02"
  // fingerprint type: "0701", mfr: "027A", prod: "0500", model: "0003", deviceJoinName: "Zooz Motion Sensor ZSE02"
  // fingerprint type: "0701", mfr: "0152", prod: "0003", model: "0500", deviceJoinName: "Zooz Motion Sensor ZSE02"


  simulator
  {
    // TODO: define status and reply messages here
  }

  tiles (scale: 2)
  {
    multiAttributeTile(name:"main", type: "generic", width: 6, height: 4)
    {
      tileAttribute ("device.motion", key: "PRIMARY_CONTROL") {
        attributeState "active", label:'motion', icon:"st.motion.motion.active", backgroundColor:"#53a7c0"
        attributeState "inactive", label:'no motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
      }
    }

    valueTile("tamperAlert", "device.tamperAlert", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "detected", backgroundColor:"#00FF00"
      state "clear", backgroundColor:"#e51426"
    }

    valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "battery", label:'${currentValue}', unit:"%"
    }

    valueTile("driverVersion", "device.driverVersion", width:2, height:2, inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }

    standardTile("reset", "device.DeviceReset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "false", label:'', backgroundColor:"#ffffff"
      state "true", label:'reset', backgroundColor:"#e51426"
    }

    valueTile("configured", "device.Configured", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "false", label:'', backgroundColor:"#ffffff"
      state "true", label:'', backgroundColor:"#e51426"
    }

    valueTile("lastActive", "device.LastActive", width:2, height:2, inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }
    main(["main"])

    details(["main", "tamperAlert", "battery", "driverVersion", "reset", "configured", "lastActive"])
  }

  preferences {
    input name: "wakeupInterval", type: "number", title: "Wakeup Interval", description: "Interval in seconds for the device to wakeup", range: "240..68400"
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  }
}

private deviceCommandClasses () {
  return [
    // 0x20: 1, 0x30: 1, 0x70: 2, 0x72: 1, 0x80: 1, 0x84: 2, 0x85: 2, 0x86: 1
    0x20: 1,  // Basic
    0x59: 1,  // Association Grp Info
    0x5A: 1,  // Device Reset Locally
    // 0x5E: 1,  // Plus
    0x71: 3,  //     Notification0x8
    0x72: 2,  // Manufacturer Specific
    0x80: 1, // Battery
    0x84: 2, // Wake Up
    0x85: 2,  // Association	0x85	V1 V2
    0x86: 1,  // Version
    0x01: 1,  // Z-wave command class
  ]
}

def checkConfigure() {
  def cmds = []

  if (device.currentValue("Configured") && device.currentValue("Configured").toBoolean() == false) {
		if (!state.lastConfigure || (new Date().time) - state.lastConfigure > 1500) {
			state.lastConfigure = new Date().time

      cmds << zwave.manufacturerSpecificV2.manufacturerSpecificGet().format()
			cmds << zwave.versionV1.versionGet().format()
			cmds << zwave.wakeUpV1.wakeUpIntervalGet().format()
		}
  }

	if (state.isAssociated == false) {
		if (!state.lastAssociated || (new Date().time) - state.lastAssociated > 1500) {
			state.lastAssociated = new Date().time

      cmds << zwave.associationV2.associationGroupingsGet().format()
		}
	}

  return cmds
}

def prepDevice() {
  [
    zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
    zwave.versionV1.versionGet(),
    zwave.associationV2.associationGroupingsGet(),
    zwave.zwaveCmdClassV1.requestNodeInfo(),
  ]
}

def installed() {
  log.info("$device.displayName installed()")
  state.loggingLevelIDE = 4

  if (0) {
  def zwInfo = getZwaveInfo()
  if ($zwInfo) {
    log.debug("$device.displayName $zwInfo")
    sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
  }
  }

  sendEvent(name: "driverVersion", value: getDriverVersion(), isStateChange: true)
  sendEvent(name: "AssociationGroup", value: getAssociationGroup(), isStateChange: true)
  sendEvent(name: "Configured", value: false, isStateChange: true)
  sendEvent(name: "DeviceReset", value: false, isStateChange: true)

  // state.wakeupInterval = getDefaultWakeupInterval()
  // state.motionTimeout = getDefaultMotionTimeout()

  state.isAssociated = false
  state.isConfigured = false
  sendEvent(name: "Configured", value: "false", isStateChange: true)

  sendCommands(prepDevice())
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
  if ($zwInfo) {
    log.debug("$device.displayName $zwInfo")
    sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
  }
  }

  state.isAssociated = false
  sendEvent(name: "Configured", value: "false", isStateChange: true)

  sendEvent(name: "driverVersion", value: getDriverVersion(), isStateChange: true)
  // sendEvent(name: "motion", value: "inactive", descriptionText: "$device.displayName is being reset")
  sendEvent(name: "AssociationGroup", value: getAssociationGroup(), isStateChange: true)
  state.AssociationGroup = getAssociationGroup()

  // Avoid calling updated() twice
  state.updatedDate = Calendar.getInstance().getTimeInMillis()
}

def parse(String description) {
  log.debug "parse() ${description}"

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
    def cmd = zwave.parse(description, deviceCommandClasses())// , [0x20: 1, 0x85: 2, 0x59: 1, 0x71: 3, 0x80: 1, 0x5A: 1, 0x84: 2, 0x72: 2, 0x86: 1, 0x31: 5])
	
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

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpIntervalReport cmd, result) {
  logger("$device.displayName $cmd")

  result << createEvent(name: "WakeUpNode", value: cmd.nodeid, isStateChange: true, displayed: true)
  result << createEvent(name: "WakeUpInterval", value: cmd.seconds, isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd, result) {
  logger("$device.displayName $cmd")

  def cmds = []

  if (!state.lastbat || (new Date().time) - state.lastbat > 53*60*60*1000) {
    cmds << zwave.batteryV1.batteryGet().format()
  }

  state.lastActive = new Date().time
  result << createEvent(name: "LastAwake", value: state.lastActive, descriptionText: "${device.displayName} woke up", isStateChange: false)

  if (cmds) {
    result << response( delayBetween ( cmds ))
  }

  return result
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd, result) {
  logger("$device.displayName $cmd")
  state.reset = true
  result << createEvent(name: "reset", value: state.reset, descriptionText: cmd.toString(), isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
	logger("$device.displayName $cmd")

	String manufacturerCode = String.format("%04X", cmd.manufacturerId)
	String productTypeCode = String.format("%04X", cmd.productTypeId)
	String productCode = String.format("%04X", cmd.productId)

	state.manufacturer = cmd.manufacturerName ? cmd.manufacturerName : "Zooz"

	result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
	result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
	result << createEvent(name: "ProductCode", value: productCode)

	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	updateDataValue("MSR", msr)
	updateDataValue("manufacturer", state.manufacturer)

	result << createEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: false)

  result << createEvent(name: "Configured", value: "true", isStateChange: true, displayed: true)

	result << createEvent(name: "Manufacturer", value: "${state.manufacturer}", descriptionText: "$device.displayName", isStateChange: false)
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd, result) {
  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  result << createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: false)
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd, result) {
  log.debug("$device.displayName $cmd")

  def map = [ name: "battery", unit: "%" ]
  if (cmd.batteryLevel == 0xFF) {
    map.value = 1
    map.descriptionText = "${device.displayName} has a low battery"
    map.isStateChange = true
  } else {
    map.value = cmd.batteryLevel
  }
  state.lastbat = new Date().time

  result << createEvent(map)

	return
}

def motionEvent(value, result) {
  def map = [name: "motion"]
  if (value != 0) {
    map.value = "active"
    map.descriptionText = "$device.displayName detected motion"
    state.lastActive = new Date().time
		result << createEvent(name: "LastActive", value: state.lastActive, displayed: true)
	} else {
    map.value = "inactive"
    map.descriptionText = "$device.displayName motion has stopped"
  }

  result << createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$device.displayName $cmd")

  motionEvent(cmd.value, result)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$device.displayName $cmd")

  motionEvent(cmd.value, result)
}

def zwaveEvent(physicalgraph.zwave.commands.alarmv2.AlarmTypeSupportedReport cmd, result) {
  logger("$device.displayName $cmd")

  state.AlarmTypeSupportedReport= "$cmd"
  result << createEvent(name: "AlarmTypeSupportedReport", value: cmd.toString(), descriptionText: "$device.displayName recieved: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelSupportedScaleReport cmd, result) {
  logger("$device.displayName $cmd")

  state.SensorMultilevelSupportedScaleReport= "${cmd}"
  result << createEvent(name: "SensorMultilevelSupportedScaleReport", value: cmd.toString(), descriptionText: "$device.displayName recieved: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd, result) {
  logger("$device.displayName $cmd")

  switch (cmd.sensorType) {
    case 27:
      result << motionEvent(cmd.scale)
      break;
  default:
      result << createEvent(descriptionText: "$device.displayName unsupported sensortype: $cmd.sensorType", displayed: true)
      break;
  }

  return result
}


//  payload: 00 00 00 FF 07 00 01 03
def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd, result) {
  logger("$device.displayName $cmd")

  if (cmd.notificationType == NOTIFICATION_TYPE_BURGLAR) {
    if (cmd.event == 0x00) {
      if (cmd.eventParameter == [8]) {
        motionEvent(0, result)
      } else if (cmd.eventParameter == [3]) { // payload : 00 00 00 FF 07 00 01 03
        result << createEvent(name: "tamper", value: "detected", descriptionText: "$device.displayName covering was removed", isStateChange: true)
      } else {
        result << createEvent(name: "tamper", value: "clear", descriptionText: "$device.displayName has been deactivated by the switch.")
        motionEvent(0, result)
      }
    } else if (cmd.event == 0x03) {
      result << createEvent(name: "tamper", value: "detected", descriptionText: "$device.displayName has been deactivated by the switch.", isStateChange: true)
    } else if (cmd.event == 0x08) {
			motionEvent(255, result)
    }
  } else {
    result << createEvent(descriptionText: cmd.toString(), isStateChange: true)
  }

  /*
  def cmds = [
    zwave.alarmV2.alarmTypeSupportedGet(),
    zwave.manufacturerSpecificV1.manufacturerSpecificGet(),
    // zwave.sensorMultilevelV4.sensorMultilevelSupportedGetSensor(),
    zwave.versionV1.versionGet()
  ]

  if (! state.AlarmTypeSupportedReport) {
    return [ event, sendCommands(cmds)]
  } else {
    return [ event ]
  }
  */

  return
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd, result) {
  logger("$device.displayName $cmd")

  state.groups = cmd.supportedGroupings

  if (cmd.supportedGroupings) {
    def cmds = []

    for (def x = cmd.supportedGroupings; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: 0x01).format();
      cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x).format();
      cmds << zwave.associationV2.associationGet(groupingIdentifier: x).format();
    }

    response(delayBetween(cmds, 1000))
  } else {
    result << createEvent(descriptionText: "$device.displayName reported no groups", isStateChange: true, displayed: true)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(descriptionText: "$device.displayName AssociationGroupInfoReport: $cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd, result) {
  logger("$device.displayName $cmd")

  result << createEvent(descriptionText: "$device.displayName AssociationGroupNameReport: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$device.displayName $cmd")

	if (cmd.groupingIdentifier == getAssociationGroup()) {
		if (cmd.nodeId.any { it == zwaveHubNodeId }) {
			state.isAssociated = true
			result << createEvent(name: "isAssociated", value: "true")
		} else {
			state.isAssociated = false
			result << createEvent(name: "isAssociated", value: "false")
			result << response(delayBetween([
				zwave.associationV1.associationSet(groupingIdentifier: getAssociationGroup(), nodeId: [zwaveHubNodeId]).format(),
				zwave.associationV1.associationGet(groupingIdentifier: getAssociationGroup()).format(),
			]))
		}
	}

	String final_string
	if (cmd.nodeId) {
		def string_of_assoc = ""
		cmd.nodeId.each {
			string_of_assoc += "${it}, "
		}
		def lengthMinus2 = string_of_assoc.length() - 3
		final_string = string_of_assoc.getAt(0..lengthMinus2)
	}

	String group_name
	switch ( cmd.groupingIdentifier ) {
		case 1:
		group_name = "Lifeline"
			break;
		case 2:
		group_name = "Sensor Basic rep"
		break;
		case 3:
		group_name = "Sensor notifi rep"
			break;
		case 4:
		group_name = "Sensor Basic SET"
		break;
		default:
		log.error "Unknown group ${cmd.groupingIdentifier}";
		group_name = ""
		break;
	}

	result << createEvent(
		name: group_name,
		value: "${final_string}",
		descriptionText: "Association group ${cmd.groupingIdentifier}",
		displayed: true,
		isStateChange: true);

	result << createEvent(descriptionText: "$device.displayName assoc: ${cmd.groupingIdentifier}", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.controllerreplicationv1.CtrlReplicationTransferScene cmd, result) {
  logger("$device.displayName $cmd")

  result << createEvent(descriptionText: "$device.displayName told to CtrlReplicationTransferScene: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.transportservicev1.CommandSubsequentFragment cmd, result) {
  logger("$device.displayName $cmd")

  result << createEvent(descriptionText: "$device.displayName told to CommandSubsequentFragment: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  log.error "ERROR: $cmd"
  result << createEvent(descriptionText: "$device.displayName command not implemented: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd, result) {
  logger("$device.displayName $cmd")

  result << createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd")
}

/*
private askIt()
{
  log.debug "askIt() is called"

  def cmds = []

  if (state.refresh) {
    cmds << zwave.notificationv3.NotificationGet()
    cmds << zwave.batteryV1.batteryGet()
    if (state.configure) {
      cmds << zwave.associationV2.associationSet(groupingIdentifier:cmd.groupingIdentifier, nodeId:zwaveHubNodeId)
    }
    cmds << zwave.associationV2.associationGet(groupingIdentifier:cmd.groupingIdentifier)

    if (getDataValue("MSR") == null) {
      cmds << zwave.manufacturerSpecificV2.manufacturerSpecificGet()
    }

    if (device.currentState('firmwareVersion') == null) {
      cmds << zwave.versionv1.VersionGet()
    }
  } else if (state.configure) {
    cmds << zwave.associationV2.associationSet(groupingIdentifier:cmd.groupingIdentifier, nodeId:zwaveHubNodeId)
    cmds << zwave.associationV2.associationGet(groupingIdentifier:cmd.groupingIdentifier)
  }


  return response(commands(cmds))
}
*/

def refresh() {
  log.debug "refresh() is called"
  state.refresh = false
  createEvent(descriptionText: "refresh will be called during next wakeup", displayed: true)
}

def configure() {
  state.configured = false
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
private prepCommands(cmds, delay=1000) {
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
