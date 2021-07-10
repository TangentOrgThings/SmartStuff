/**
 *	Fibaro Flood Sensor ZW5 Mule
 */
metadata {
	definition(name: "Fibaro Flood Sensor ZW5 Mule", namespace: "TangentOrgThings", author: "Brian Aker and Fibar Group", ocfDeviceType: "x.com.st.d.sensor.moisture") {
		capability "Battery"
		capability "Tamper Alert"
		capability "Temperature Measurement"
		capability "Water Sensor"
		capability "Health Check"

		attribute "lastAlarmDate", "string"

		fingerprint mfr: "010F", prod: "0B01", model: "1002", deviceJoinName: "Fibaro Water Leak Sensor"
		fingerprint mfr: "010F", prod: "0B01", model: "1003", deviceJoinName: "Fibaro Water Leak Sensor"
		fingerprint mfr: "010F", prod: "0B01", model: "2002", deviceJoinName: "Fibaro Water Leak Sensor"
		fingerprint mfr: "010F", prod: "0B01", deviceJoinName: "Fibaro Water Leak Sensor"
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"water", type: "generic", width: 6, height: 4){
			tileAttribute("device.water", key: "PRIMARY_CONTROL") {
				attributeState("dry", icon:"st.alarm.water.dry", backgroundColor:"#ffffff")
				attributeState("wet", icon:"st.alarm.water.wet", backgroundColor:"#00A0DC")
 			}
 		}

		valueTile("temperature", "device.temperature", inactiveLabel: false, width: 2, height: 2) {
			state "temperature", label: '${currentValue}°',
					backgroundColors: [
							[value: 31, color: "#153591"],
							[value: 44, color: "#1e9cbb"],
							[value: 59, color: "#90d2a7"],
							[value: 74, color: "#44b621"],
							[value: 84, color: "#f1d801"],
							[value: 95, color: "#d04e00"],
							[value: 96, color: "#bc2323"]
					]
		}

		valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat", width: 4, height: 2) {
			state "battery", label:'${currentValue}% battery', unit:""
		}

		main(["water"])
		details(["water", "temperature", "battery", "tamper"])
	}

	preferences {
	}
}

def clearTamper() {
  if (state.isTamper) {
    sendEvent(name: "tamper", value: "clear", isStateChange: true)
    state.isTamper= false
  }	
}

def installed() {
	sendEvent(name: "checkInterval", value: (21600*2)+10*60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
}

def updated() {
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd) {
	log.debug "WakeUpNotification"
	def event = createEvent(descriptionText: "${device.displayName} woke up", displayed: false)
	def cmds = []
	def cmdsSet = []
	def cmdsGet = []
	def cmdCount = 0
	def results = [createEvent(descriptionText: "$device.displayName woke up", isStateChange: true)]

	cmdsGet << zwave.batteryV1.batteryGet()
	cmdsGet << zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 1, scale: 0)

	if (cmdsSet) {
		cmds = encapSequence(cmdsSet, 500)
		cmds << "delay 500"
	}

	cmds = cmds + encapSequence(cmdsGet, 1000)
	cmds << "delay " + (5000 + cmdCount * 1500)
	results = results + response(cmds)

	return results
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	log.debug "ManufacturerSpecificReport"
	log.debug "manufacturerId:	 ${cmd.manufacturerId}"
	log.debug "manufacturerName: ${cmd.manufacturerName}"
	log.debug "productId:		 ${cmd.productId}"
	log.debug "productTypeId:	 ${cmd.productTypeId}"
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd) {
	log.debug "DeviceSpecificReport"
	log.debug "deviceIdData:				${cmd.deviceIdData}"
	log.debug "deviceIdDataFormat:			${cmd.deviceIdDataFormat}"
	log.debug "deviceIdDataLengthIndicator: ${cmd.deviceIdDataLengthIndicator}"
	log.debug "deviceIdType:				${cmd.deviceIdType}"
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
	log.debug "VersionReport"
	log.debug "applicationVersion:		${cmd.applicationVersion}"
	log.debug "applicationSubVersion:	${cmd.applicationSubVersion}"
	log.debug "zWaveLibraryType:		${cmd.zWaveLibraryType}"
	log.debug "zWaveProtocolVersion:	${cmd.zWaveProtocolVersion}"
	log.debug "zWaveProtocolSubVersion: ${cmd.zWaveProtocolSubVersion}"
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	log.debug "BatteryReport"
	log.debug "cmd: "+cmd
	log.debug "location: "+location
	def timeDate = location.timeZone ? new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone) : new Date().format("yyyy MMM dd EEE h:mm:ss")

	if (cmd.batteryLevel == 0xFF) {  // Special value for low battery alert
		sendEvent(name: "battery", value: 1, descriptionText: "${device.displayName} has a low battery", isStateChange: true)
	} else {
		sendEvent(name: "battery", value: cmd.batteryLevel, descriptionText: "Current battery level")
	}
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd) {
	log.debug "NotificationReport: ${cmd.notificationType} : ${cmd.event}"
	def map = [:]
	def alarmInfo = "Last alarm detection: "
	if (cmd.notificationType == 5) {
		switch (cmd.event) {
			case 2:
				map.name = "water"
				map.value = "wet"
				map.descriptionText = "${device.displayName} is ${map.value}"
				state.lastAlarmDate = "\n"+new Date().format("yyyy MMM dd EEE HH:mm:ss")
				//state.lastAlarmDate = "\n"+new Date().format("yyyy MMM dd EEE HH:mm:ss", location.timeZone)
				break

			case 0:
				map.name = "water"
				map.value = "dry"
				map.descriptionText = "${device.displayName} is ${map.value}"
				break
		}
	} else if (cmd.notificationType == 7) {
		switch (cmd.event) {
			case 0:
				state.isTamper= false
				map.name = "tamper"
				map.value = "clear"
				map.descriptionText = "${device.displayName}: tamper alarm has been deactivated"
				break

			case 3:
				state.isTamper= true
				map.name = "tamper"
				map.value = "detected"
				map.descriptionText = "${device.displayName}: tamper alarm activated"
				break
		}
	} else {
		log.warn "NotificationReport did not handle this cmd.notificationType: ${cmd.notificationType}"
	}
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) {
	log.debug "SensorMultilevelReport"
	def map = [:]
	if (cmd.sensorType == 1) {
		// temperature
		def cmdScale = cmd.scale == 1 ? "F" : "C"
		map.value = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmdScale, 1)
		map.unit = getTemperatureScale()
		map.name = "temperature"
		map.displayed = true
		log.debug "Temperature:" + map.value
		createEvent(map)
	} else {
		log.warn "SensorMultilevelReport unknown report: $cmd"
	}
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd) {
	log.warn "Test10: DeviceResetLocallyNotification"
	log.info "${device.displayName}: received command: $cmd - device has reset itself"
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpIntervalReport cmd) {
	log.warn cmd
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	log.debug "BasicSet with CMD = ${cmd}"

	// Allows Basic to represent Alarm state
	if (1) {
		def result = []
		def map = [:]

		map.name = "water"
		map.value = cmd.value ? "wet" : "dry"
		map.descriptionText = "${device.displayName} is ${map.value}"

		result << createEvent(map)

		result
	}
}

def zwaveEvent(physicalgraph.zwave.commands.sensoralarmv1.SensorAlarmReport cmd)
{
	def map = [:]

	if (cmd.sensorType == 0x05) {
		map.name = "water"
		map.value = cmd.sensorState ? "wet" : "dry"
		map.descriptionText = "${device.displayName} is ${map.value}"

		log.debug "CMD = SensorAlarmReport: ${cmd}"
	} else if ( cmd.sensorType == 0) {
		map.name = "tamper"
		map.isStateChange = true
		map.value = cmd.sensorState ? "detected" : "clear"
		map.descriptionText = "${device.displayName} has been tampered with"
		state.isTamper= cmd.sensorState ? true : false
		runIn(30, "resetTamper") //device does not send alarm cancelation

	} else if ( cmd.sensorType == 1) {
		map.name = "tamper"
		map.value = cmd.sensorState ? "detected" : "clear"
		map.descriptionText = "${device.displayName} has been tampered with"
		state.isTamper= cmd.sensorState ? true : false
		runIn(30, "resetTamper") //device does not send alarm cancelation

	} else {
		map.descriptionText = "${device.displayName}: ${cmd}"
	}
	createEvent(map)
}

def resetTamper() {
	def map = [:]
	map.name = "tamper"
	map.value = "clear"
	map.descriptionText = "$device.displayName is secure"
	state.isTamper= false
	sendEvent(map)
}

/*
####################
## Z-Wave Toolkit ##
####################
*/

def parse(String description) {
	def result = []
	logging("${device.displayName} - Parsing: ${description}")
	if (description.startsWith("Err 106")) {
		result = createEvent(
				descriptionText: "Failed to complete the network security key exchange. If you are unable to receive data from it, you must remove it from your network and add it again.",
				eventType: "ALERT",
				name: "secureInclusion",
				value: "failed",
				displayed: true,
		)
	} else if (description == "updated") {
		return null
	} else {
		def cmd = zwave.parse(description, cmdVersions())
		if (cmd) {
			logging("${device.displayName} - Parsed: ${cmd}")
			zwaveEvent(cmd)
		}
	}
}

//event handlers related to configuration and sync
def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
	log.debug cmd
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand(cmdVersions())
	if (encapsulatedCommand) {
		logging("${device.displayName} - Parsed SecurityMessageEncapsulation into: ${encapsulatedCommand}")
		zwaveEvent(encapsulatedCommand)
	} else {
		log.warn "Unable to extract Secure command from $cmd"
	}
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
	def version = cmdVersions()[cmd.commandClass as Integer]
	def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
	def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
	if (encapsulatedCommand) {
		logging("${device.displayName} - Parsed Crc16Encap into: ${encapsulatedCommand}")
		zwaveEvent(encapsulatedCommand)
	} else {
		log.warn "Unable to extract CRC16 command from $cmd"
	}
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	if (cmd.commandClass == 0x6C && cmd.parameter.size >= 4) { // Supervision encapsulated Message
		// Supervision header is 4 bytes long, two bytes dropped here are the latter two bytes of the supervision header
		cmd.parameter = cmd.parameter.drop(2)
		// Updated Command Class/Command now with the remaining bytes
		cmd.commandClass = cmd.parameter[0]
		cmd.command = cmd.parameter[1]
		cmd.parameter = cmd.parameter.drop(2)
	}
	def encapsulatedCommand = cmd.encapsulatedCommand(cmdVersions())
	if (encapsulatedCommand) {
		logging("${device.displayName} - Parsed MultiChannelCmdEncap ${encapsulatedCommand}")
		zwaveEvent(encapsulatedCommand)
		// zwaveEvent(encapsulatedCommand, cmd.sourceEndPoint as Integer)
	} else {
		log.warn "Unable to extract MultiChannel command from $cmd"
	}
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	log.debug "Unhandled: ${cmd.toString()}"
	[:]
}

private logging(text, type = "debug") {
	if (settings.logging == "true") {
		log."$type" text
	}
}

private secEncap(physicalgraph.zwave.Command cmd) {
	logging("${device.displayName} - encapsulating command using Secure Encapsulation, command: $cmd", "info")
	zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private crcEncap(physicalgraph.zwave.Command cmd) {
	logging("${device.displayName} - encapsulating command using CRC16 Encapsulation, command: $cmd", "info")
	zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
}

private multiEncap(physicalgraph.zwave.Command cmd, Integer ep) {
	logging("${device.displayName} - encapsulating command using MultiChannel Encapsulation, ep: $ep command: $cmd", "info")
	zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint: ep).encapsulate(cmd)
}

private encap(physicalgraph.zwave.Command cmd, Integer ep) {
	encap(multiEncap(cmd, ep))
}

private encap(List encapList) {
	encap(encapList[0], encapList[1])
}

private encap(Map encapMap) {
	encap(encapMap.cmd, encapMap.ep)
}

private encap(physicalgraph.zwave.Command cmd) {
	if (zwaveInfo.zw.contains("s")) {
		secEncap(cmd)
	} else if (zwaveInfo?.cc?.contains("56")) {
		crcEncap(cmd)
	} else {
		logging("${device.displayName} - no encapsulation supported for command: $cmd", "info")
		cmd.format()
	}
}

private encapSequence(cmds, Integer delay = 250) {
	delayBetween(cmds.collect { encap(it) }, delay)
}

private List intToParam(Long value, Integer size = 1) {
	def result = []
	size.times {
		result = result.plus(0, (value & 0xFF) as Short)
		value = (value >> 8)
	}
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationRejectedRequest cmd) {
	log.debug cmd
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.NetworkKeyVerify cmd) {
	log.debug cmd
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecuritySchemeReport cmd) {
	log.debug cmd
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityCommandsSupportedReport cmd) {
	log.debug cmd
}

/*
##########################
## Device Configuration ##
##########################
*/

/*	0x31 : 5 - Sensor Multilevel
	0x56 : 1 - Crc16 Encap
	0x71 : 2 - Notification ST supported V3
	0x72 : 2 - Manufacturer Specific
	0x80 : 1 - Battery
	0x84: 2 - Wake Up
	0x85: 2 - Association
	0x86: 1 - Version
	0x98: 1 - Security */

private Map cmdVersions() {
	[0x31: 5, 0x56: 1, 0x71: 3, 0x72: 2, 0x80: 1, 0x84: 2, 0x85: 2, 0x86: 1, 0x98: 1]
}
