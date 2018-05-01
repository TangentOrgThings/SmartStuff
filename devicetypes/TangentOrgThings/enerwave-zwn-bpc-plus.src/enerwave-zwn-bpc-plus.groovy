// vim :set ts=2 sw=2 sts=2 expandtab smarttab :
/**
 *  Enerwave ZWN BPC
 *
 *  Copyright 2016 Brian Aker
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
  return "v3.15"
}

def getDefaultMotionTimeout() {
  return isPlus() ? 5 : 5
}

def getDefaultWakeupInterval() {
  return isNewerModel ? 60 : 360
}

def getParamater() {
  return isPlus() ? 1 : 0
}

def getAssociationGroup () {
  if (isPlus()) {
   return zwaveHubNodeId == 1 ? 1 : 3
  }
  
  return 1
}

def isPlus() {
  return true
}

metadata {
  definition (name: "Enerwave ZWN BPC Plus", namespace: "TangentOrgThings", author: "Brian Aker", ocfDeviceType: "x.com.st.d.sensor.motion") {
    capability "Battery"
    capability "Motion Sensor"
    capability "Sensor"
    
    attribute "DeviceReset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message    

    attribute "Configured", "enum", ["false", "true"]
    attribute "supportedGroupings", "string"
    attribute "Lifeline", "string"
    attribute "Sensor Basic rep", "string"
    attribute "Sensor notifi rep", "string"
    attribute "Sensor Basic SET", "string"
    
    attribute "AssociationGroup", "number"
    attribute "isAssociated", "enum", ["false", "true"]
    
    attribute "driverVersion", "string"
    attribute "firmwareVersion", "string"
    attribute "FirmwareMdReport", "string"
    attribute "Manufacturer", "string"
    attribute "ManufacturerCode", "string"
    attribute "MSR", "string"
    attribute "NIF", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"
    attribute "WirelessConfig", "string"
    
    attribute "WakeUp", "string"
    attribute "WakeUpInterval", "number"
    attribute "WakeUpNode", "number"
    
    attribute "lastActive", "string"
    attribute "LastAwake", "string"
    attribute "MotionTimeout", "number"
    
    attribute "NIF", "string"
    
    // fingerprint mfr: "011a", prod: "0601", model: "0901", cc: "30,70,72,80,84,85,86", ccOut: "20", deviceJoinName: "Enerwave Motion Sensor"  // Enerwave ZWN-BPC
    fingerprint type: "2001", mfr: "011A", prod: "0601", model: "0901", deviceJoinName: "Enerwave Motion Sensor ZWN-BPC+"  // Enerwave ZWN-BPC
    // fingerprint type: "2001", mfr: "011A", prod: "00FF", model: "0700", deviceJoinName: "Enerwave Motion Sensor ZWN-BPC PLus"  // Enerwave ZWN-BPC
  }

  simulator {
    status "inactive": "command: 3003, payload: 00"
    status "active": "command: 3003, payload: FF"
  }

  tiles {
    multiAttributeTile(name:"motion", type: "generic", width: 6, height: 4) {
      tileAttribute ("device.motion", key: "PRIMARY_CONTROL") {
        attributeState "active", label:'motion', icon:"st.motion.motion.active", backgroundColor:"#00a0dc"
        attributeState "inactive", label:'no motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
      }
      tileAttribute ("battery", key: "SECONDARY_CONTROL") {
        attributeState "battery", label:'${currentValue}% battery', unit:""
      }
    }
    
    valueTile("firmwareVersion", "device.firmwareVersion", width:2, height: 2, decoration: "flat", inactiveLabel: false) {
      state "default", label: '${currentValue}'
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
    
    valueTile("lastActive", "state.LastActive", width:2, height:2, inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }
    
    valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
	   state("battery", label:'${currentValue}% battery', unit:"")
	}

    main "motion"
    details(["motion", "lastActive", "driverVersion", "firmwareVersion", "reset", "configured", "battery", "lastActive"])
  }

  preferences {
    input name: "wakeupInterval", type: "number", title: "Wakeup Interval", description: "Interval in seconds for the device to wakeup", range: "240..68400"
    input name: "isNewerModel", type: "bool", title: "Temp fix for model", description: "Enter true or false"
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false    
  }
}

private deviceCommandClasses () {
  if (isPlus()) {
    return [
      // 0x20: 1, 0x30: 1, 0x70: 2, 0x72: 1, 0x80: 1, 0x84: 2, 0x85: 2, 0x86: 1
      0x20: 1,  // Basic
      0x59: 1,  // Association Grp Info
      0x5A: 1,  // Device Reset Locally
      // 0x5E: 1,  // Plus
      0x70: 1,  // Configuratin
      0x71: 3,  //     Notification0x8
      0x72: 2,  // Manufacturer Specific
      0x80: 1, // Battery
      0x84: 1, // Wake Up
      0x85: 2,  // Association	0x85	V1 V2      
      0x86: 1,  // Version
      0x01: 1,  // Z-wave command class
    ]
  } else {
    return [
      0x20: 1,  // Basic
      0x30: 1,  // Sensor Binary 
      // 0x5E: 1,  // Plus
      0x70: 1,  // Configuratin
      0x72: 1,  // Manufacturer Specific
      0x80: 1,  // Battery
      0x84: 1,  // Wake Up
      0x85: 1,  // Association	0x85	V1 V2      
      0x86: 1,  // Version
      0x01: 1,  // Z-wave command class
    ]
  }
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
    def cmd = zwave.parse(description, [ 0x20: 1, 0x30: 1, 0x59: 1, 0x70: 2, 0x71: 3, 0x72: 2, 0x80: 1, 0x84: 2, 0x85: 2, 0x86: 1 ]) // , [0x20: 1, 0x30: 1, 0x70: 1, 0x72: 1, 0x80: 1, 0x84: 2, 0x85: 1, 0x86: 1]) // 30,70,72,80,84,85,86
    
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

def configure() {
  setMotionTimeout()
  state.isAssociated = false
  state.isConfigured = false

  sendEvent(name: "Configured", value: "false", isStateChange: true)
	sendEvent(name: "isAssociated", value: "false", isStateChange: true)
}

def setMotionTimeout() {

}

def sensorValueEvent(happened, result) {

  if (happened) {
    logger("  is active", "info")
    state.lastActive = new Date().time
    result << createEvent(name: "LastActive", value: state.lastActive, isStateChange: true, displayed: true)
  } 
    
  result << createEvent(name: "motion", value: happened ? "active" : "inactive", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$device.displayName $cmd")
  sensorValueEvent(cmd.value, result)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$device.displayName $cmd")
  sensorValueEvent(cmd.value, result)
} 

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd, result) {
  logger("$device.displayName $cmd")
  sensorValueEvent(cmd.sensorValue, result)
}

def zwaveEvent(physicalgraph.zwave.commands.sensoralarmv1.SensorAlarmReport cmd, result) {
  logger("$device.displayName $cmd")
  sensorValueEvent(cmd.sensorState, result)
}

// NotificationReport() NotificationReport(event: 8, eventParameter: [], eventParametersLength: 0, notificationStatus: 255, notificationType: 7, reserved61: 0, sequence: false, v1AlarmLevel: 0, v1AlarmType: 0, zensorNetSourceNodeId: 0)
def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd, result) {
	logger("$device.displayName $cmd")
	state.newerModel = true

	Boolean isActive = false
	Boolean isError = false

	if (cmd.notificationType == 0x07) {
		switch (cmd.event) {
			case 0x08:
			isActive = true
			break;
			case 0x00:
			isActive =  false
			break;
			default:
			log.error("$device.displayName has unknown state: $cmd")
			result << createEvent(descriptionText: "$device.displayName unknown state: $cmd", displayed: true)
      return
		} 

		sensorValueEvent( ( isActive ? 1 :0 ), result)
		return
	}

	log.error("$device.displayName notificationType not implemented: $cmd")
	[ createEvent(descriptionText: "$device.displayName unknown notificationType: $cmd", displayed: true) ]
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

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd, result) {
  log.debug "BatteryReport() $cmd"
  Boolean did_batterylevel_change = state.batteryLevel != cmd.batteryLevel
  state.batteryLevel = cmd.batteryLevel

  int level
  switch(cmd.batteryLevel) {
    case 0x64:
    level = 99
    break
    case 0x10:
    level = 50
    break
    case 0x00:
    level = 25
    break
    case 0xFF:
    level = 1
    break
    default:
    level = 98
    break
  }
  
  state.lastbat = new Date().time
  
  result << createEvent(name: "battery", unit: "%", value: level, descriptionText: "Battery level", isStateChange: did_batterylevel_change)
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd, result) {
  logger("$device.displayName $cmd")

  if (cmd.supportedGroupings) {
    def cmds = []

		for (def x = 1; x <= cmd.supportedGroupings; x++) {
			if (isPlus()) {
				cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: true, refreshCache: true).format();
				cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x).format();
				cmds << zwave.associationGrpInfoV1.associationGroupCommandListGet(groupingIdentifier: x, allowCache: false).format();
				cmds << zwave.associationV2.associationGet(groupingIdentifier: x).format();
			} else {
				cmds << zwave.associationV1.associationGet(groupingIdentifier: x).format();
			}
		}

    result << createEvent(name: "supportedGroupings", value: cmd.supportedGroupings, descriptionText: "$device.displayName", isStateChange: true, displayed: true)
    result << response(delayBetween(cmds, 2000))

		return
  }
  
  result << createEvent(descriptionText: "$device.displayName AssociationGroupingsReport: $cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupCommandListReport cmd, result) {
  logger("$device.displayName $cmd")

  result << createEvent(descriptionText: "$device.displayName AssociationGroupCommandListReport: $cmd", isStateChange: true, displayed: true)
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
				zwave.associationV1.associationSet(groupingIdentifier: 4, nodeId: [zwaveHubNodeId]).format(),
				zwave.associationV1.associationGet(groupingIdentifier: 4).format(),
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

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd, result) {
  logger("$device.displayName $cmd")

  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  result << createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", displayed: true, isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  logger("$device.displayName command not implemented: $cmd", "error")
  result << createEvent(descriptionText: "$device.displayName command not implemented: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd, result) {
  logger("$device.displayName $cmd", "warn")
  state.reset = true
  result << createEvent(name: "DeviceReset", value: state.reset, descriptionText: cmd.toString(), isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
	logger("$device.displayName $cmd")

	String manufacturerCode = String.format("%04X", cmd.manufacturerId)
	String productTypeCode = String.format("%04X", cmd.productTypeId)
	String productCode = String.format("%04X", cmd.productId)

	state.manufacturer = cmd.manufacturerName ? cmd.manufacturerName : "Enerwave"

	sendEvent(name: "ManufacturerCode", value: manufacturerCode)
	sendEvent(name: "ProduceTypeCode", value: productTypeCode)
	sendEvent(name: "ProductCode", value: productCode)

	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	updateDataValue("MSR", msr)
	updateDataValue("manufacturer", state.manufacturer)

	sendEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: false)

	result << createEvent(name: "Manufacturer", value: "${state.manufacturer}", descriptionText: "$device.displayName", isStateChange: false)
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd, result) {
  logger("$device.displayName $cmd")
  int parameterNumber

  if (getParamater() == cmd.parameterNumber) {
    state.motionTimeout = cmd.scaledConfigurationValue
    sendEvent(name: "MotionTimeout", value: state.motionTimeout, isStateChange: true, displayed: true)
  }
  
  switch ( cmd.parameterNumber) {
    case 0:
    parameterNumber = 0
    break;
    case 1:
    parameterNumber = 1
    break;
    default:
    result << createEvent(descriptionText: "$device.displayName recieved unknown parameter $cmd.parameterNumber", isStateChange: false)
		return
  }

/*
  if (cmd.configurationValue[parameterNumber] != state.motionTimeout) {
    state.isConfigured = false
    int MotionTimout = state.motionTimeout as Integer
    sendHubCommand([
      zwave.configurationV1.configurationSet(parameterNumber: getParamater(), configurationValue: [(MotionTimout)]),
      zwave.configurationV1.configurationGet(parameterNumber: getParamater()),
      zwave.wakeUpV1.wakeUpIntervalGet(),
    ])
  } else {
    state.isConfigured = true
    [createEvent(name: "Configured", value: false, isStateChange: true)]
  }
  */
  
   state.isConfigured = true
     
  result << createEvent(name: "Configured", value: state.isConfigured, isStateChange: true, displayed: true)
}

def checkConfigure() {
  def cmds = []
  
  if (state.isConfigured == false) {
		if (!state.lastConfigure || (new Date().time) - state.lastConfigure > 1500) {
			state.lastConfigure = new Date().time

			if (isPlus()) {
				cmds << zwave.manufacturerSpecificV2.manufacturerSpecificGet().format()
					cmds << zwave.configurationV1.configurationGet(parameterNumber: getParamater()).format()
					// cmds << zwave.configurationV2.configurationGet(parameterNumber: getParamater()).format()
			} else {
				cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
					cmds << zwave.configurationV1.configurationGet(parameterNumber: getParamater()).format()
			}
			cmds << zwave.versionV1.versionGet().format()
			cmds << zwave.wakeUpV1.wakeUpIntervalGet().format()
		}
  }

	if (state.isAssociated == false) {
		if (!state.lastAssociated || (new Date().time) - state.lastAssociated > 1500) {
			state.lastAssociated = new Date().time

			if (isPlus()) {
				cmds << zwave.associationV2.associationGroupingsGet().format()
			} else {
				cmds << zwave.associationV1.associationGroupingsGet().format()
			}
		}
	}
  
  return cmds
}

def prepDevice() {
  [
    zwave.versionV1.versionGet(),
    zwave.manufacturerSpecificV1.manufacturerSpecificGet(),
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

  state.wakeupInterval = getDefaultWakeupInterval()
  state.motionTimeout = getDefaultMotionTimeout()

  state.isAssociated = false
  state.isConfigured = false
	sendEvent(name: "isAssociated", value: "false", isStateChange: true)
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
  state.isConfigured = false
	sendEvent(name: "isAssociated", value: "false", isStateChange: true)
  sendEvent(name: "Configured", value: "false", isStateChange: true)

  sendEvent(name: "driverVersion", value: getDriverVersion(), isStateChange: true)
  // sendEvent(name: "motion", value: "inactive", descriptionText: "$device.displayName is being reset")
  sendEvent(name: "AssociationGroup", value: getAssociationGroup(), isStateChange: true)
  state.AssociationGroup = getAssociationGroup()
  
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
