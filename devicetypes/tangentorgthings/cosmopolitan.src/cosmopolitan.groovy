// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :
/**
 *  Cosmopolitan
 *
 *  Copyright 2018 Brian Aker
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

def getDriverVersion () {
  return "v0.03"
}

metadata {
	definition (name: "Cosmopolitan", namespace: "TangentOrgThings", author: "Brian Aker") {
		capability "Actuator"
		capability "Button"
		capability "Illuminance Measurement"
		capability "Motion Sensor"
		capability "Sensor"
		capability "Switch"
    capability "Switch Level"

		attribute "logMessage", "string"
		attribute "lastError", "string"
		attribute "driverVersion", "string"
	}

  preferences {
    input name: "isaSwitch", type: "bool", title: "Switch?", description: "Is it a Switch?", defaultValue: false
    input name: "isaMotionSensor", type: "bool", title: "Motion Sensor?", description: "Is it a Motion Sensor?", defaultValue: false
    input name: "isaIlluminanceMeasurement", type: "bool", title: "Illuminance Measurement?", description: "Is it a Illuminance Measurement?", defaultValue: false
    input name: "isaLevelSwitch`", type: "bool", title: "Level Switch?", description: "Is it a Level Switch?", defaultValue: false
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  }


	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
      tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label: '${name}', action: "disconnect", icon: "st.switches.switch.on", backgroundColor: "#00A0DC"
        attributeState "off", label: '${name}', action: "connect", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
      }
    }

    valueTile("driverVersion", "device.driverVersion", width: 2, height: 2, inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }

    main "switch"
    details(["switch", "driverVersion" ])
		// TODO: define your main and details tiles here
	}
}

// parse events into attributes
def parse(String description) {
	logger("Parsing '${description}'")
	// TODO: handle 'button' attribute
	// TODO: handle 'numberOfButtons' attribute
	// TODO: handle 'supportedButtonValues' attribute
	// TODO: handle 'illuminance' attribute
	// TODO: handle 'motion' attribute
	// TODO: handle 'switch' attribute
	// TODO: handle 'logMessage' attribute
	// TODO: handle 'lastError' attribute
	// TODO: handle 'driverVersion' attribute

}

// handle commands
def on() {
	logger("on()")
	// TODO: handle 'on' command
}

def off() {
	logger("off()")
	// TODO: handle 'off' command
}

def installed() {
  logger("$device.displayName installed()")
  sendEvent(name: "numberOfButtons", value: 8, displayed: false)
  state.loggingLevelIDE = 4
}

def updated() {
  if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
    return
  }
  state.loggingLevelIDE = debugLevel ? debugLevel : 4
  log.info("$device.displayName updated() debug: ${state.loggingLevelIDE}")

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)

  // Avoid calling updated() twice
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
  String msg_text = (msg != null) ? "$msg" : "<null>"

  switch(level) {
    case "error":
    if (state.loggingLevelIDE >= 1) {
      log.error "$msg_text"
      sendEvent(name: "lastError", value: "${msg_text}", displayed: false, isStateChange: true)
    }
    break

    case "warn":
    if (state.loggingLevelIDE >= 2) {
      log.warn "$msg_text"
      sendEvent(name: "logMessage", value: "${msg_text}", displayed: false, isStateChange: true)
    }
    break

    case "info":
    if (state.loggingLevelIDE >= 3) {
      log.info "$msg_text"
    }
    break

    case "debug":
    if (state.loggingLevelIDE >= 4) {
      log.debug "$msg_textmsg"
    }
    break

    case "trace":
    if (state.loggingLevelIDE >= 5) {
      log.trace "$msg_text"
    }
    break

    default:
    log.debug "$msg_text"
    break
  }
}
