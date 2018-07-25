// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :

import groovy.json.JsonOutput
/**
 *  Logitech Harmony Hub
 *
 *  Author: SmartThings
 */

def getDriverVersion () {
	return "v0.23"
}


metadata {
	definition (name: "Logitech Harmony Hub C2C Custom", namespace: "TangentOrgThings", author: "SmartThings") {
		capability "Actuator"
		capability "Media Controller"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
		capability "Switch"

		command "activityoff"
		command "alloff"

		attribute "driverVersion", "string"

		attribute "logMessage", "string"        // Important log messages.
		attribute "lastError", "string"        // Last error message
		attribute "parseErrorCount", "number"        // Last error message
		attribute "unknownCommandErrorCount", "number"        // Last error message
	}

	simulator {
	}

	preferences {
		input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
	}

	tiles {
		valueTile("currentActivity", "device.currentActivity", decoration: "flat", height: 2, width: 2, inactiveLabel: false) {
			state "default", label:'${currentValue}'
		}

		valueTile("status", "device.status", decoration: "flat", height: 2, width: 2, inactiveLabel: false) {
			state "default", label:'${currentValue}'
		}

		standardTile("huboff", "device.switch", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'End Activity', action:"activityoff", icon:"st.harmony.harmony-hub-icon"
		}

		standardTile("alloff", "device.switch", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'All Activities', action:"alloff", icon:"st.secondary.off"
		}

		standardTile("refresh", "device.refresh", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

    valueTile("driverVersion", "device.driverVersion", width: 2, height: 2, inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }

		main (["currentActivity"])
		details(["currentActivity", "status", "huboff", "alloff", "driverVersion", "refresh"])
	}
}

def parse(String description) {
	if (description) {
		log.debug("parse() $description")
	}
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
	state.loggingLevelIDE = debugLevel ? debugLevel : 4
	log.info("$device.displayName updated() debug: ${state.loggingLevelIDE}")

	sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

	sendEvent(name: "lastError", value: "", displayed: false)
	sendEvent(name: "logMessage", value: "", displayed: false)
	sendEvent(name: "parseErrorCount", value: 0, displayed: false)
	sendEvent(name: "unknownCommandErrorCount", value: 0, displayed: false)

	initialize()
}

def startActivity(String activityId) {
	log.debug "Executing 'Start Activity'"
	log.trace parent.activity("$device.deviceNetworkId-$activityId", "start")
}

def on() {
	log.debug "off()"
	log.trace parent.poll()
}

def off() {
	log.debug "off()"
	log.trace parent.activity(device.deviceNetworkId, "hub")
	log.trace parent.activity("all","end")
}

def activityoff() {
	log.debug "Executing 'Activity Off'"
	log.trace parent.activity(device.deviceNetworkId,"hub")
}

def alloff() {
	log.debug "Executing 'All Off'"
	log.trace parent.activity("all","end")
}

def poll() {
	log.debug "Executing 'Poll'"
	log.trace parent.poll()
}

def ping() {
	refresh()
}

def refresh() {
	log.debug "Executing 'Refresh'"
	log.trace parent.poll()
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
