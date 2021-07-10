/**
 *  Device Type Definition File
 *
 *  Device Type:        Fibaro Flood Sensor Mule
 *  File Name:          fibaro-flood-sensor-mule.groovy
 *  Initial Release:    2014-12-10
 *  @author:            Todd Wackford
 *  Email:              todd@wackford.net
 *  @version:           1.0
 *
 *  Copyright Brian Aker
 *  Copyright 2014 SmartThings
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
 
 /**
 * Sets up metadata, simulator info and tile definition. The tamper tile is setup, but 
 * not displayed to the user. We do this so we can receive events and display on device 
 * activity. If the user wants to display the tamper tile, adjust the tile display lines
 * with the following:
 *		main(["water", "temperature"])
 *		details(["water", "temperature", "tamper", "battery"])
 *
 * @param none
 *
 * @return none
 */
metadata {
	definition (name: "Fibaro Flood Sensor Mule", namespace: "TangentOrgThings", author: "Brian Aker and SmartThings") {
		capability "Tamper Alert"
		capability "Water Sensor"
		capability "Temperature Measurement"
		capability "Battery"
		capability "Health Check"
		capability "Sensor"
    
		fingerprint deviceId: "0xA102", inClusters: "0x30,0x9C,0x60,0x85,0x8E,0x72,0x70,0x86,0x80,0x84", deviceJoinName: "Fibaro Water Leak Sensor"
		fingerprint mfr:"010F", prod:"0000", model:"2002", deviceJoinName: "Fibaro Water Leak Sensor"
		fingerprint mfr:"010F", prod:"0000", model:"1002", deviceJoinName: "Fibaro Water Leak Sensor"
		fingerprint mfr:"010F", prod:"0B00", model:"1001", deviceJoinName: "Fibaro Water Leak Sensor"
	}

	simulator {
	}

	tiles(scale:2) {
		multiAttributeTile(name:"water", type: "generic", width: 6, height: 4){
			tileAttribute("device.water", key: "PRIMARY_CONTROL") {
				attributeState("dry", icon:"st.alarm.water.dry", backgroundColor:"#ffffff")
				attributeState("wet", icon:"st.alarm.water.wet", backgroundColor:"#00A0DC")
 			}
 		}
		valueTile("temperature", "device.temperature", inactiveLabel: false, width: 2, height: 2) {
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
		standardTile("tamper", "device.tamper", decoration: "flat", width: 2, height: 2) {			
			state "clear", label: 'tamper clear', backgroundColor: "#ffffff"
			state "detected", label: 'tampered', action:"clearTamper", backgroundColor: "#ff0000"
		}        
		valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "battery", label:'${currentValue}% battery', unit:""
		}

		main(["water"])
		details(["water", "temperature", "battery", "tamper"])
	}
}

def clearTamper() {
  if (state.isTamper) {
    sendEvent(name: "tamper", value: "clear", isStateChange: true)
    state.isTamper= false
  }	
}

// Parse incoming device messages to generate events
def parse(String description) {
	def result = []

	def cmd = zwave.parse(description, getCommandClassVersions())

	if (cmd) {
		result += zwaveEvent(cmd) //createEvent(zwaveEvent(cmd))   
	}

	if ( result[0] != null ) {
		log.debug "Parse returned ${result}"
		result
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
	def encapsulatedCommand = cmd.encapsulatedCommand([0x20: 1, 0x30: 2, 0x31: 2]) // can specify command class versions here like in zwave.parse
	log.debug ("Command from endpoint ${cmd.sourceEndPoint}: ${encapsulatedCommand}")
	if (encapsulatedCommand) {
		return zwaveEvent(encapsulatedCommand)
	}
}

// Event Generation
def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd) {
	def result = [createEvent(descriptionText: "${device.displayName} woke up", isStateChange: false)]
	result
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv2.SensorMultilevelReport cmd) {
	def map = [:]

	switch (cmd.sensorType) {
		case 1:
			// temperature
			def cmdScale = cmd.scale == 1 ? "F" : "C"
			map.value = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmdScale, cmd.precision)
			map.unit = getTemperatureScale()
			map.name = "temperature"
			break;
		case 0:
			// here's our tamper alarm = acceleration
			map.name = "tamper"
			map.isStateChange = true
			if (cmd.sensorState == 255) {
				map.value = "detected"
				state.isTamper= true
				runIn(300, "resetTamper") //device does not send alarm cancelation
			} else {
				map.value = "clear"
				state.isTamper= false
			}
			map.descriptionText = "${device.displayName} SensorMultilevelReport(0) been tampered with: ${map.value}"
			break;
	}
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	def map = [:]
	map.name = "battery"
	map.unit = "%"

	if (cmd.batteryLevel == 0xFF) {  // Special value for low battery alert
		map.value = 1
		map.descriptionText = "${device.displayName} has a low battery"
		map.isStateChange = true
	} else {
		map.value = cmd.batteryLevel
		map.descriptionText = "Current battery level"
	}
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd) {
	def map = [:]
	map.name = "tamper"
	map.value = cmd.sensorValue ? "detected" : "clear"
	map.name = "tamper"

	if (cmd.sensorValue) {
		map.descriptionText = "$device.displayName SensorBinaryReport detected vibration"
		map.value = "detected"
		state.isTamper= true
	}
	else {
		map.descriptionText = "$device.displayName SensorBinaryReport has stopped"
		map.value = "clear"
		state.isTamper= false
	}
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
	log.debug "${device.displayName} cmd: ${cmd.toString()}"
	[:]
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
		map.descriptionText = "${device.displayName} SensorAlarmReport(General Purpose Alarm) : ${map.value}"
		state.isTamper= cmd.sensorState ? true : false
		if (state.isTamper) runIn(300, "resetTamper") //device does not send alarm cancelation

	} else if ( cmd.sensorType == 1) {
		map.name = "tamper"
		map.value = cmd.sensorState ? "detected" : "clear"
		map.descriptionText = "${device.displayName} SensorAlarmReport(1) : ${map.value}"
		state.isTamper= cmd.sensorState ? true : false
		if (state.isTamper) runIn(300, "resetTamper") //device does not send alarm cancelation

	} else {
		map.descriptionText = "${device.displayName}: unknown SensorAlarmReport ${cmd}"
	}
	createEvent(map)
}

def resetTamper() {
	def map = [:]
	state.isTamper= false
	map.name = "tamper"
	map.value = "clear"
	map.descriptionText = "$device.displayName is secure"
	sendEvent(map)
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	log.debug "Catchall reached for cmd: ${cmd.toString()}}"
	[:]
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	def result = []

	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	log.debug "msr: $msr"
	device.updateDataValue(["MSR", msr])

	result << createEvent(descriptionText: "$device.displayName MSR: $msr", isStateChange: false)
	result
}

def getCommandClassVersions() {
	return [
		0x20: 1,  // Basic
		0x31: 2, 
		0x30: 1, 
		0x70: 2, 
		0x71: 1, 
		0x84: 1, 
		0x80: 1, 
		0x9C: 1, 
		0x72: 2, 
		0x56: 2, 
		0x60: 3, 
		0x8E: 2,
	]
}
