// vim: set filetype=groovy tabstop=2 shiftwidth=2 softtabstop=2 expandtab smarttab :

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

import physicalgraph.*

String getDriverVersion() {
  return "v3.43"
}

Integer getAssociationGroup() {
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
    attribute "ManufacturerCode", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"
    attribute "firmwareVersion", "string"
    attribute "zWaveProtocolVersion", "string"
    attribute "Power", "string"
    attribute "SensorMultilevelSupportedScaleReport", "string"
    attribute "SensorMultilevelSupportedSensorReport", "string" 

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

    valueTile("tamperAlert", "device.tamperAlert", inactiveLabel: true, decoration: "flat", width: 2, height: 2) {
      state "detected", backgroundColor:"#00FF00"
      state "clear", backgroundColor:"#e51426"
    }

    valueTile("battery", "device.battery", inactiveLabel: true, decoration: "flat", width: 2, height: 2) {
      state "battery", label:'${currentValue}', unit:"%"
    }

    valueTile("driverVersion", "device.driverVersion", width:2, height:2, inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }

    standardTile("reset", "device.DeviceReset", inactiveLabel: true, decoration: "flat", width: 2, height: 2) {
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
    input name: "associatedDevice", type: "number", title: "Associated Device", description: "... ", required: false
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  }
}
def getCommandClassVersions() {
  return [
    // 0x20: 1, 0x59: 1, 0x5a: 1, 0x5e: 2, 0x71: 4, 0x72: 2, 0x73: 1, 0x80: 1, 0x84: 2, 0x85: 2, 0x86: 2
    0x20: 1,  // Basic
    0x59: 1,  // Association Grp Info
    0x5A: 1,  // Device Reset Locally
    // 0x5E: 1,  // Plus
    0x71: 3,  //     Notification0x8
    0x72: 2,  // Manufacturer Specific
    0x73: 1,  // Powerlevel
    0x80: 1, // Battery
    0x84: 2, // Wake Up
    0x85: 2,  // Association	0x85	V1 V2
    0x86: 1,  // Version
    0x01: 1,  // Z-wave command class
  ]
}

def checkConfigure() {
  def cmds = []

  if (!device.currentValue("Configured") || device.currentValue("Configured").toBoolean() == false) {
    if ( !state.lastConfigure || ((Calendar.getInstance().getTimeInMillis() - state.lastConfigure)) < 5000 ) {
      state.lastConfigure = Calendar.getInstance().getTimeInMillis()

      cmds << zwave.manufacturerSpecificV2.manufacturerSpecificGet().format()
      cmds << zwave.manufacturerSpecificV2.deviceSpecificGet().format()
			cmds << zwave.wakeUpV1.wakeUpIntervalGet().format()
		}
  }

	if (state.isAssociated == false) {
    if ( !state.lastAssociated || ((Calendar.getInstance().getTimeInMillis() - state.lastAssociated)) < 5000 ) {
      state.lastAssociated = Calendar.getInstance().getTimeInMillis()

      cmds << zwave.associationV2.associationGroupingsGet().format()
		}
	}

  return cmds
}

def prepDevice() {
  [
    zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
    zwave.manufacturerSpecificV2.deviceSpecificGet(),
    zwave.associationV2.associationGroupingsGet(),
    zwave.zwaveCmdClassV1.requestNodeInfo(),
  ]
}

def installed() {
  log.info("$device.displayName installed()")

  if (0) {
  def zwInfo = getZwaveInfo()
  if ($zwInfo) {
    log.debug("$device.displayName $zwInfo")
    sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
  }
  }

  sendEvent(name: "driverVersion", value: getDriverVersion(), isStateChange: true)
  sendEvent(name: "Configured", value: "false", isStateChange: true)
  sendEvent(name: "DeviceReset", value: "false", isStateChange: true)

  // state.wakeupInterval = getDefaultWakeupInterval()
  // state.motionTimeout = getDefaultMotionTimeout()

  state.isAssociated = false

  sendCommands(prepDevice())
}

def updated() {
  if ( state.updatedDate && ((Calendar.getInstance().getTimeInMillis() - state.updatedDate)) < 5000 ) {
    return
  }
  log.info("$device.displayName updated() debug: ${settings.debugLevel}")

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

  // Avoid calling updated() twice
  state.updatedDate = Calendar.getInstance().getTimeInMillis()
}

def parse(String description) {
  def result = []

  if (description && description.startsWith("Err")) {
    logger("parse error: ${description}", "warn")

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
    logger("parse() called with NULL description", "warn")
  } else if (description != "updated") {
    if (1) {
      def cmds = checkConfigure()

      if (cmds) {
        result << response( delayBetween ( cmds ))
      }
    }

    def cmd = zwave.parse(description, getCommandClassVersions())

    if (! cmd ) {
      logger("zwave.parse(getCommandClassVersion()) failed for: ${description}", "warn")
      cmd = zwave.parse(description)
    }
	
    if (cmd) {
      zwaveEvent(cmd, result)
    } else {
      logger("zwave.parse() failed for: ${description}", "warn")
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

  state.lastActive = new Date().time
  result << createEvent(name: "LastAwake", value: state.lastActive, descriptionText: "${device.displayName} woke up", isStateChange: false)
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "DeviceReset", value: "true", descriptionText: cmd.toString(), isStateChange: true, displayed: true)
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

  result << createEvent(name: "Configured", value: "true", isStateChange: true, displayed: true)

  result << response( zwave.versionV1.versionGet() )
}

def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelReport cmd, result) {
  logger("zwaveEvent(): Powerlevel Report received: ${cmd}")
  String device_power_level = (cmd.powerLevel > 0) ? "minus${cmd.powerLevel}dBm" : "NormalPower"
  logger("Powerlevel Report: Power: ${device_power_level}, Timeout: ${cmd.timeout}", "info")
  result << createEvent(name: "Power", value: device_power_level)
}

def zwaveEvent(physicalgraph.zwave.commands.powerlevelv1.PowerlevelTestNodeReport cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd, result) {
  logger("$device.displayName $cmd")

  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  def zWaveProtocolVersion = "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  result << createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: true)
  result << createEvent(name: "zWaveProtocolVersion", value: "${zWaveProtocolVersion}", descriptionText: "${device.displayName} ${zWaveProtocolVersion}", isStateChange: true)
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

def zwaveEvent(zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$cmd")

  Short value = cmd.value

  if (value == 0) {
    result << createEvent(name: "motion", value: "inactive", isStateChange: true, displayed: true)
    if (device.displayName.endsWith("Dimmer")) {
      result << createEvent(name: "level", value: 0, isStateChange: true, displayed: true)
    }
  } else if (value < 100 || value == 255) {
    result << createEvent(name: "motion", value: "active", isStateChange: true, displayed: true)
    if (device.displayName.endsWith("Dimmer")) {
      result << createEvent(name: "level", value: 100, isStateChange: true, displayed: true)
    }
  } else if (value < 254) {
    logger("BasicSet returned reserved state ($value)", "warn")
  } else if (value == 254) {
    logger("BasicSet unknown state (254)", "warn")
  } else {
    logger("BasicSet reported value unknown to API ($value)", "warn")
  }
} 


def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, result) {
  logger("$device.displayName $cmd")

  motionEvent(cmd.value, result)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$device.displayName $cmd")

  motionEvent(cmd.value, result)
}

//  payload: 00 00 00 FF 07 00 01 03
def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd, result) {
  logger("$device.displayName $cmd")

  if (cmd.notificationType == 7) { // NOTIFICATION_TYPE_BURGLAR) {
    switch (cmd.event) {
      case 0:
      if ( cmd.eventParametersLength && cmd.eventParameter.size() && cmd.eventParameter[0] ) {
        switch ( cmd.eventParameter[0]) {
          case 3:
          result << createEvent(name: "tamper", value: "clear", descriptionText: "$device.displayName has been activated by the switch.", isStateChange: true)
          break
          case 8:
          motionEvent(0, result)
          break
          default:
          logger("Unknown event parameter", "error")
        }
      } else {
        motionEvent(0, result)
      }
      break;

      case 3:
      // "Tampering, product cover removed"
      result << createEvent(name: "tamper", value: "detected", descriptionText: "$device.displayName has been deactivated by the switch.", isStateChange: true)
      motionEvent(0, result)
      break;

      case 8: // "Motion detected, location unknown"
      motionEvent(255, result)
      break;

      default :
      logger("Unknown event ${cmd.event}", "error")
      break;
    }
  } else {
    logger("Unknown notification type ${cmd.notificationType}", "error")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd, result) {
  logger("$device.displayName $cmd")

  if (cmd.supportedGroupings) {
    def cmds = []

    for (def x = cmd.supportedGroupings; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x).format();
      cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: 0x01).format();
    }

    result << delayBetween(cmds, 1000)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd, result) {
  logger("$device.displayName $cmd")

  def name = new String(cmd.name as byte[])
  logger("Association Group #${cmd.groupingIdentifier} has name: ${name}", "info")
  updateDataValue("Group #${cmd.groupingIdentifier}", "$name")

  result << response( zwave.associationV2.associationGet(groupingIdentifier: cmd.groupingIdentifier) )
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
				zwave.associationV1.associationSet(groupingIdentifier: cmd.groupingIdentifier, nodeId: [zwaveHubNodeId]).format(),
				zwave.associationV1.associationGet(groupingIdentifier: cmd.groupingIdentifier).format(),
			]))
		}

    if ( settings.associatedDevice  && ! cmd.nodeId.any { it == settings.associatedDevice }) {
      associate += settings.associatedDevice
      state.isAssociated = false

      result << response( zwave.associationV1.associationSet(groupingIdentifier: cmd.groupingIdentifier, nodeId: settings.associatedDevice) )
    }
	}

	String final_string = ""
	if (cmd.nodeId) {
		final_string = cmd.nodeId.join(",")
	}

	String group_name = ""
	switch ( cmd.groupingIdentifier ) {
		case 1:
		group_name = "Lifeline"
			break;
		default:
		logger("Unknown group ${cmd.groupingIdentifier}", "error");
    return
		break;
	}

  updateDataValue("$group_name", "$final_string")
}

def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  logger("$device.displayName command not implemented: $cmd", "error")
}

def refresh() {
  logger("refresh()");
  state.refresh = false
}

def configure() {
  sendEvent(name: "Configured", value: "false", isStateChange: true)
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

private logger(msg, level = "trace") {
  String device_name = "$device.displayName"
  String msg_text = (msg != null) ? "${msg}" : "<null>"

  Integer log_level = state.defaultLogLevel ?: settings.debugLevel

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
