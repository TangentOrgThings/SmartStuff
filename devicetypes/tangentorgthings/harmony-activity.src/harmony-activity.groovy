// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :

/**
 *  Logitech Harmony Activity
 *
 *  Copyright 2018 Brian Aker <brian@tangent.org>
 *  Copyright 2015 Juan Risso
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

def getDriverVersion () {
	return "v0.27"
}

metadata {
	definition (name: "Harmony Activity", namespace: "TangentOrgThings", author: "Juan Risso") {
		capability "Actuator"
		capability "Refresh"
		capability "Sensor"
		capability "Switch"

		command "huboff"
		command "alloff"

		attribute "driverVersion", "string"

		attribute "logMessage", "string"        // Important log messages.
		attribute "lastError", "string"        // Last error message
		attribute "parseErrorCount", "number"        // Last error message
		attribute "unknownCommandErrorCount", "number"        // Last error message
	}

	// simulator metadata
	simulator {
	}

	preferences {
		input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
	}

	// UI tile definitions
	tiles {
		standardTile("button", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "off", label: 'Off', action: "switch.on", icon: "st.harmony.harmony-hub-icon", backgroundColor: "#ffffff", nextState: "on"
			state "on", label: 'On', action: "switch.off", icon: "st.harmony.harmony-hub-icon", backgroundColor: "#00A0DC", nextState: "off"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("forceoff", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:'Force End', action:"switch.off", icon:"st.secondary.off"
		}
		standardTile("huboff", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:'End Hub Action', action:"huboff", icon:"st.harmony.harmony-hub-icon"
		}
		standardTile("alloff", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:'All Actions', action:"alloff", icon:"st.secondary.off"
		}
		main "button"
		details(["button", "refresh", "forceoff", "huboff", "alloff"])
	}
}

def parse(String description) {
	logger("parse() ${description}")
}

def on() {
	sendEvent(name: "switch", value: "on")
	logger( parent.activity(device.deviceNetworkId,"start") )
}

def off() {
	sendEvent(name: "switch", value: "off")
	logger( parent.activity(device.deviceNetworkId,"end") )
}

def huboff() {
	sendEvent(name: "switch", value: "off")
	logger( parent.activity(device.deviceNetworkId,"hub") )
}

def alloff() {
	sendEvent(name: "switch", value: "off")
	log.trace parent.activity("all","end")
}

def refresh() {
	log.debug "Executing 'refresh'"
	log.trace parent.poll()
}

def initialize() {
	sendEvent(name: "DeviceWatch-Enroll", value: JsonOutput.toJson([protocol: "cloud", scheme:"untracked"]), displayed: false)
}

def installed() {
	logger("$device.displayName installed()", "info")
	state.loggingLevelIDE = 4

	sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

	initialize()
}

def updated() {
	if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
		return
	}
	state.loggingLevelIDE = settings.debugLevel ?: 4
	log.info("$device.displayName updated() debug: ${state.loggingLevelIDE}")

	sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

	sendEvent(name: "lastError", value: "", displayed: false)
	sendEvent(name: "logMessage", value: "", displayed: false)
	sendEvent(name: "parseErrorCount", value: 0, displayed: false)
	sendEvent(name: "unknownCommandErrorCount", value: 0, displayed: false)

	initialize()
  state.updatedDate = Calendar.getInstance().getTimeInMillis()
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
		case "unknownCommand":
		state.unknownCommandErrorCount += 1
		sendEvent(name: "unknownCommandErrorCount", value: unknownCommandErrorCount, displayed: false, isStateChange: true)
		break

		case "parse":
		state.parseErrorCount += 1
		sendEvent(name: "parseErrorCount", value: parseErrorCount, displayed: false, isStateChange: true)
		break

		case "warn":
		if (state.loggingLevelIDE >= 2) {
			log.warn msg
			sendEvent(name: "logMessage", value: "WARNING: ${msg}", displayed: false, isStateChange: true)
		}
		return

		case "info":
		if (state.loggingLevelIDE >= 3) log.info msg
			return

		case "debug":
		if (state.loggingLevelIDE >= 4) log.debug msg
			return

		case "trace":
		if (state.loggingLevelIDE >= 5) log.trace msg
			return

		case "error":
		default:
		break
	}

	log.error msg
	sendEvent(name: "lastError", value: "ERROR: ${msg}", displayed: false, isStateChange: true)
}
