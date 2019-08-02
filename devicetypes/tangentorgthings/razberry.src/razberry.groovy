// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :

/**
 *  Copyright 2017-2019 Brian Aker <brian@tangent.org>
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

def getDriverVersion() {
  return "v1.81"
}

metadata {
  definition (name: "Razberry", namespace: "TangentOrgThings", author: "Brian Aker") {
    capability "Refresh"
    capability "Sensor"

    attribute "Lifeline", "string"
    attribute "driverVersion", "string"
    attribute "NIF", "string"
    attribute "FirmwareMdReport", "string"
    attribute "Manufacturer", "string"
    attribute "ManufacturerCode", "string"
    attribute "MSR", "string"
    attribute "NodeName", "string"
    attribute "NodeLocation", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"
    attribute "WakeUp", "string"
    attribute "WirelessConfig", "string"

    fingerprint type: "0207", mfr: "0115", prod: "0001", model: "0001", deviceJoinName: "Z-Wave.Me Razberry"
  }

  simulator {
  }

  tiles {
    standardTile("state", "device.state", width: 2, height: 2) {
      state 'connected', icon: "st.unknown.zwave.static-controller", backgroundColor:"#ffffff"
    }
    standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
      state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
    }

    main "state"
    details (["state", "refresh"])
  }
}

def getCommandClassVersions() {
  [
    0x20: 1,
    0x25: 1,
    0x27: 1,
    0x2b: 1,
    0x2c: 1,
    0x56: 1,
    0x59: 1,
    0x5a: 1,
    // 0x5e: 2,
    0x60: 3, // V4
    0x70: 2, // V1
    0x71: 3, // V4
    0x72: 2,
    0x73: 1,
    0x77: 1,
    0x7a: 2,
    0x85: 2,
    0x86: 1, // V2
    0x8e: 2, // V3
    0x31: 5, // Note: this one shows up from logs
  ]
}

def parse(String description) {
  def result = []

  logger "parse(${description})"

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
      // logger( "zwave.parse(getCommandClassVersions()) failed for: ${description}", "warn" )
      logger "zwave.parse(getCommandClassVersions()) failed for: ${description}"
      // Try it without check for classes
      cmd = zwave.parse(description)

      if (cmd) {
        zwaveEvent(cmd, result)
      } else {
        // logger( "zwave.parse() failed for: ${description}", "error" )
        logger "zwave.parse() failed for: ${description}"
      }
    }
  }

  return result
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd, result) {
	result << createEvent(descriptionText: "${device.displayName} woke up", isStateChange:true)
	result << response(["delay 2000", zwave.wakeUpV1.wakeUpNoMoreInformation().format()])
}

private List loadEndpointInfo() {
	if (state.endpointInfo) {
		state.endpointInfo
	} else if (device.currentValue("epInfo")) {
		fromJson(device.currentValue("epInfo"))
	} else {
		[]
	}
}

def zwaveEvent(zwave.commands.multichannelv3.MultiChannelEndPointGet cmd, result) {
}

def zwaveEvent(zwave.commands.multichannelv3.MultiChannelEndPointReport cmd, result) {
  logger("$cmd")
  updateDataValue("endpoints", cmd.endPoints.toString())

	def cmds = []
  for (def x = 1; x <= cmd.endPoints; x++) {
    cmds << zwave.multiChannelV3.multiChannelCapabilityGet(endPoint: x).format()
  }

  result << response(delayBetween(cmds, 4000))
}

def zwaveEvent(zwave.commands.multichannelv3.MultiChannelCapabilityReport cmd, result) {
  logger("$cmd")
  updateDataValue("MultiChannelCapabilityReport ", cmd.endPoint.toString())

	def endP = cmd.endPoint
	
  if (childDevices) {
    if (!childDevices.find{ it.deviceNetworkId.endsWith("-ep${endP}") || !childDevices}) {
      // createChildDevices( cmd.commandClass, endP )
    }
  }
}

def zwaveEvent(zwave.commands.multichannelv3.MultiChannelCmdEncap cmd, result) {
  logger("$cmd")

  if (childDevice) {
    def ep = cmd.sourceEndPoint
    def childDevice = null

    childDevices.each {
      if (it.deviceNetworkId =="${device.deviceNetworkId}-ep${ep}") {
        childDevice = it
      }
    }
    logger("Parse ${childDevice.deviceNetworkId}, cmd: ${cmd}", "info")
    childDevice.parse(cmd.encapsulatedCommand().format())
  } else {
    logger( "Child device not found.cmd: ${cmd}", "warn")

    def encapsulatedCommand = cmd.encapsulatedCommand(getCommandClassVersions())
    if (encapsulatedCommand) {
      zwaveEvent(encapsulatedCommand, result)
      return
    }

    logger("Unknown MultiChannelCmdEncap: $cmd", "warn")
  }
}

def zwaveEvent(zwave.commands.multichannelassociationv2.MultiChannelAssociationGroupingsReport cmd, result) {
  logger("$cmd")

  if (cmd.supportedGroupings) {
    def cmds = []

    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.multiChannelAssociationV2.multiChannelAssociationGet( groupingIdentifier: x );
    }

    sendCommands(cmds, 1000)

    return
  }

  logger("No groups reported", "warn")
}

def zwaveEvent(zwave.commands.multichannelassociationv2.MultiChannelAssociationReport cmd, result) {
  logger("$cmd")

  String nodes = cmd.nodeId.join(", ")
  updateDataValue("MultiGroup #${cmd.groupingIdentifier}", "${name}")
}


def zwaveEvent(zwave.commands.associationv2.AssociationGroupingsReport cmd, result) {
  logger("$cmd")

  if (cmd.supportedGroupings) {
    def cmds = []

    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: 0x01, refreshCache: true);
      cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x);
    }

    sendCommands(cmds, 1000)

    return
  }

  logger("No groups reported", "error")
}

def zwaveEvent(zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$cmd")

  if (cmd.groupingIdentifier > 3) {
    logger("Unknown Group Identifier", "error");
    return
  }

  // Lifeline
  String nodes = cmd.nodeId.join(", ")

  String group_name = ""
  switch (cmd.groupingIdentifier) {
    case 1:
    group_name = "Lifeline"
      break;
    case 2:
    group_name = "Basic Set"
    break;
    case 3:
    group_name = "Basic Set Double Tap"
    break;
    default:
    group_name = "Unknown";
    break;
  }

  updateDataValue("$group_name", "$nodes")
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
    zwave.associationV2.associationGet(groupingIdentifier: cmd.groupingIdentifier).format(),
  ]))
}

def zwaveEvent(zwave.commands.associationgrpinfov1.AssociationGroupCommandListReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.languagev1.LanguageReport cmd, result) {
  log.debug "LanguageReport: $cmd"
  result << createEvent(descriptionText: "$device.displayName LanguageReport: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd, result) {
  // 5E,86,60,8F,81,46,98,26,25,72,8A,2B,77,22,5B,56,73,85,59,8E
  def encapsulatedCommand = cmd.encapsulatedCommand([0x20: 1, 0x22: 1, 0x25: 1, 0x26: 1, 0x2b: 1, 0x46: 1, 0x56: 1, 0x59: 1, 0x5a: 1, 0x5b: 1, 0x5e: 2, 0x60: 4, 0x72: 2, 0x73: 1, 0x77: 1, 0x81: 1, 0x85: 2, 0x86: 2, 0x8a: 2, 0x8e: 3, 0x8f: 1,  0x98: 1, 0x89: 1])
  if (encapsulatedCommand) {
    if (! state.sec) {
      state.sec = 1; // Fix this to be the real value by checking Commands Supported
    }
    zwaveEvent(encapsulatedCommand, result)
    result += result.collect {
      if (it instanceof physicalgraph.device.HubAction && !it.toString().startsWith("9881")) {
        response(cmd.CMD + "00" + it.toString())
      } else {
        it
      }
    }
    return result
  }
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd, result) {
  log.debug("NodeInfo: $cmd")
  result << createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd")
}

/*
def zwaveEvent(physicalgraph.zwave.commands.securityv1.NetworkKeyVerify cmd) {
  createEvent(name:"secureInclusion", value:"success", descriptionText:"Secure inclusion was successful")
}
*/

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityCommandsSupportedReport cmd, result) {
  state.sec = cmd.commandClassSupport.collect { String.format("%02X ", it) }.join()
  if (cmd.commandClassControl) {
    state.secCon = cmd.commandClassControl.collect { String.format("%02X ", it) }.join()
  }
  log.debug "Security command classes: $state.sec"
  result << createEvent(name:"secureInclusion", value:"success", descriptionText:"Lock is securely included")
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd, result) {
  logger("$cmd")

  def encapsulatedCommand = zwave.commandClass(cmd.commandClass)?.command(cmd.command)?.parse(cmd.data)
  if (encapsulatedCommand) {
    zwaveEvent(encapsulatedCommand, result)
    return
  }

  logger("zwaveEvent(): Could not extract command from ${cmd}")
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationBusy cmd, result) {
  state.status = cmd.status
  def msg = cmd.status == 0 ? "try again later" :
            cmd.status == 1 ? "try again in $cmd.waitTime seconds" :
            cmd.status == 2 ? "request queued" : "sorry"
  // result << createEvent(displayed: true, descriptionText: "$device.displayName is busy, $msg")
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationRejectedRequest cmd, result) {
  logger("$cmd")
  state.status = cmd.status
}

def zwaveEvent(zwave.commands.nodenamingv1.NodeNamingNodeNameReport cmd, result) {
}

def zwaveEvent(zwave.commands.nodenamingv1.NodeNamingNodeLocationReport cmd, result) {
}

def zwaveEvent(zwave.commands.basicv1.BasicGet cmd, result) {
  logger("$cmd")

  result << zwave.basicV1.basicReport(value: 0x255).format()
}

def zwaveEvent(zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$cmd")
}

def zwaveEvent(zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$cmd")
} 

def zwaveEvent(zwave.commands.powerlevelv1.PowerlevelGet cmd, result) {
  logger("$cmd")

  result << zwave.powerlevelV1.powerlevelReport(powerLevel: 0, timeout: 0).format()
}

def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  log.debug "ERROR: $cmd"
  result << createEvent(descriptionText: "$device.displayName command not implemented: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
  def manufacturerCode = String.format("%04X", cmd.manufacturerId)
  def productTypeCode = String.format("%04X", cmd.productTypeId)
  def productCode = String.format("%04X", cmd.productId)
  def wirelessConfig = "ZWP"
  
  result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
  result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
  result << createEvent(name: "ProductCode", value: productCode)
  result << createEvent(name: "WirelessConfig", value: wirelessConfig)

  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)
  updateDataValue("manufacturer", cmd.manufacturerName)
  if (!state.manufacturer) {
    state.manufacturer= cmd.manufacturerName
  }
  
  result << createEvent([name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: false])
  result << createEvent([name: "Manufacturer", value: "${cmd.manufacturerName}", descriptionText: "$device.displayName", isStateChange: false])
  
  return result
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd, result) {
  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion 
  result << createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: false)
}

def zwaveEvent(physicalgraph.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd, result) {
  def firmware_report = String.format("%s-%s-%s", cmd.checksum, cmd.firmwareId, cmd.manufacturerId)
  updateDataValue("FirmwareMdReport", firmware_report)
  result << createEvent(name: "FirmwareMdReport", value: firmware_report, descriptionText: "$device.displayName FIRMWARE_REPORT: $firmware_report", isStateChange: false)
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd, result) {
  state.reset = true
  result << createEvent(descriptionText: cmd.toString(), isStateChange: true, displayed: true)
}

def refresh() {
	sendCommands([zwave.basicV1.basicGet()])
}

def prepDevice() {
  [
    zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
    zwave.zwaveCmdClassV1.requestNodeInfo(),
  ]
}

def installed() {
  sendEvent(name: "driverVersion", value: getDriverVersion(), isStateChange: true)
  sendCommands( prepDevice(), 1000 )
}

def updated() { 
  log.debug "updated()"
  
  // Check in case the device has been changed
  state.manufacturer = null
  updateDataValue("MSR", null)
  updateDataValue("manufacturer", null)
  
  sendEvent([name: "driverVersion", value: getDriverVersion(), isStateChange: true])
  
  sendCommands( prepDevice(), 1000 )
}

def epCmd(Integer ep, String cmds) {
	def result
	if (cmds) {
		def header = state.sec ? "988100600D00" : "600D00"
		result = cmds.split(",").collect { cmd -> (cmd.startsWith("delay")) ? cmd : String.format("%s%02X%s", header, ep, cmd) }
	}
	result
}

def enableEpEvents(enabledEndpoints) {
	state.enabledEndpoints = enabledEndpoints.split(",").findAll()*.toInteger()
	null
}

private encap(cmd, endpoint) {
	if (endpoint) {
		command(zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint:endpoint).encapsulate(cmd))
	} else {
		command(cmd)
	}
}

private encapWithDelay(commands, endpoint, delay=200) {
	delayBetween(commands.collect{ encap(it, endpoint) }, delay)
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
