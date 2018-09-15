 // vim :set ts=2 sw=2 sts=2 expandtab smarttab :
/**
 *  Copyright 2015 Brian Aker
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
	return "v1.69"
}

metadata {
	definition (name: "Logitech Harmony Home Hub Extender", namespace: "TangentOrgThings", author: "Brian Aker") {
		capability "Actuator"
		capability "Switch"
		capability "Refresh"
		capability "Configuration"
		capability "Sensor"

		attribute "Lifeline", "string"
		attribute "driverVersion", "string"
		attribute "FirmwareMdReport", "string"
		attribute "Manufacturer", "string"
		attribute "ManufacturerCode", "string"
		attribute "MSR", "string"
		attribute "ProduceTypeCode", "string"
		attribute "ProductCode", "string"
		attribute "WakeUp", "string"
		attribute "WirelessConfig", "string"

		fingerprint type: "0207", mfr: "007F", prod: "0001", model: "0001", deviceJoinName: "Logitech Harmony Home Hub Extender" // cc: "20,22,56,59,72,73,7A,85,86,98,5E", role: "00", ff: "8500", ui: "8500"
	}

	simulator {
	}

	tiles {
		standardTile("state", "device.state", width: 2, height: 2) {
			state 'connected', icon: "st.unknown.zwave.static-controller", backgroundColor:"#ffffff"
		}
        valueTile("driverVersion", "device.driverVersion", width:2, height:2, inactiveLabel: true, decoration: "flat") {
          state "default", label: '${currentValue}'
	    }
        valueTile("status", "state.status", width:2, height:2, inactiveLabel: true, decoration: "flat") {
          state "default", label: '${state.status}'
	    }
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}

		main "state"
		details (["state", "driverVersion", "status", "refresh", "configure"])
	}
}

def parse(String description){
	def result = null

	log.debug "PARSE: ${description}"
	if (description.startsWith("Err"))
	{
		if (description.startsWith("Err 106")) 
		{
			if (state.sec) {
				log.debug description
			} else {
				result = createEvent(
					descriptionText: "This device failed to complete the network security key exchange. If you are unable to control it via SmartThings, you must remove it from your network and add it again.",
					eventType: "ALERT",
					name: "secureInclusion",
					value: "failed",
					isStateChange: true,
				)
			}
		} else {
			result = createEvent(value: description, descriptionText: description)
		}
	} else if (description != "updated") {
		def cmd = zwave.parse(description)

		if (cmd)
		{
			result = zwaveEvent(cmd)

			if (!result)
			{
				log.warning "Parse Failed and returned ${result} for command ${cmd}"
				result = createEvent(value: description, descriptionText: description)
			} else {
				log.debug "RESULT: ${result}"
			}
		} else {
			log.info "zwave.parse() failed: ${description}"
			result = createEvent(value: description, descriptionText: description)
		}
	}

	return result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	if (cmd.value == 0) {
		sendEvent(name: "switch", value: "off")
	} else {
		sendEvent(name: "switch", value: "on")
	}
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	log.debug "BasicSet()"
	createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical")
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationBusy cmd) {
  state.status = cmd.status
  def msg = cmd.status == 0 ? "try again later" :
            cmd.status == 1 ? "try again in $cmd.waitTime seconds" :
            cmd.status == 2 ? "request queued" : "sorry"
  createEvent(displayed: true, descriptionText: "$device.displayName is busy, $msg")
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationRejectedRequest cmd) {
  state.status = cmd.status
  createEvent(displayed: true, descriptionText: "$device.displayName rejected the last request")
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
	state.groups = cmd.supportedGroupings
	if (cmd.supportedGroupings > 1) {
		sendCommands(zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: 0x02, listMode:1))
	} else if (cmd.supportedGroupings == 1) {
		sendCommands(zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: 0x01, listMode:1))
	}
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd) {
  sendCommands(zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier: 0x01, nodeId: zwaveHubNodeId))
  createEvent(descriptionText: "$device.displayName AssociationGroupInfoReport: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd) {
	createEvent(descriptionText: "$device.displayName AssociationGroupNameReport: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
  def encapsulatedCommand = cmd.encapsulatedCommand([0x20: 1, 0x22: 2, 0x72: 1, 0x73: 1, 0x56: 2, 0x57: 1, 0x59: 2, 0x5A: 1, 0x5E: 1, 0x85: 2, 0x86: 1, 0x98: 1])
  
  if (encapsulatedCommand) {
    if (! state.sec) {
      state.sec = 1; // Fix this to be the real value by checking Commands Supported
    }
    def result = zwaveEvent(encapsulatedCommand)
    result = result.collect {
      if (it instanceof physicalgraph.device.HubAction && !it.toString().startsWith("9881")) {
        response(cmd.CMD + "00" + it.toString())
      } else {
        it
      }
    }
    return result
  }
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.NetworkKeyVerify cmd) {
  createEvent(name:"secureInclusion", value:"success", descriptionText:"Secure inclusion was successful")
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityCommandsSupportedReport cmd) {
  state.sec = cmd.commandClassSupport.collect { String.format("%02X ", it) }.join()
  if (cmd.commandClassControl) {
    state.secCon = cmd.commandClassControl.collect { String.format("%02X ", it) }.join()
  }
  log.debug "Security command classes: $state.sec"
  createEvent(name:"secureInclusion", value:"success", descriptionText:"Lock is securely included")
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
	def versions = [0x31: 2, 0x30: 1, 0x84: 1, 0x9C: 1, 0x70: 2]
	// def encapsulatedCommand = cmd.encapsulatedCommand(versions)
	def version = versions[cmd.commandClass as Integer]
	def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
	def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
	if (encapsulatedCommand) {
		zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	log.debug "ERROR: $cmd"
	createEvent(descriptionText: "$device.displayName command not implemented: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd) {
	log.debug "NodeInfo: $cmd"
	createEvent(descriptionText: "$device.displayName NodeInfo: $cmd", displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	def manufacturerCode = String.format("%04X", cmd.manufacturerId)
	def productTypeCode = String.format("%04X", cmd.productTypeId)
	def productCode = String.format("%04X", cmd.productId)
	def wirelessConfig = "ZWP"

	sendEvent(name: "ManufacturerCode", value: manufacturerCode)
	sendEvent(name: "ProduceTypeCode", value: productTypeCode)
	sendEvent(name: "ProductCode", value: productCode)
	sendEvent(name: "WirelessConfig", value: wirelessConfig)

	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	updateDataValue("MSR", msr)
	updateDataValue("manufacturer", cmd.manufacturerName)
	state.manufacturer= cmd.manufacturerName

	sendEvent(name: "Manufacturer", value: "${cmd.manufacturerName}", descriptionText: "$device.displayName", isStateChange: false)

	createEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: false)
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
	def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
	state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion 
	createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: false)
}

def zwaveEvent(physicalgraph.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd) {
	def firmware_report = String.format("%s-%s-%s", cmd.checksum, cmd.firmwareId, cmd.manufacturerId)
	updateDataValue("FirmwareMdReport", firmware_report)
	createEvent(name: "FirmwareMdReport", value: firmware_report, descriptionText: "$device.displayName FIRMWARE_REPORT: $firmware_report", isStateChange: false)
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd) {
	def result = []
	log.debug ("DeviceResetLocallyNotification()")

	result << createEvent(descriptionText: cmd.toString(), isStateChange: true, displayed: true)
	// result << response(command(zwave.associationV2.associationGet(groupingIdentifier: 1)))

	return result
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
	def result = []

	log.debug ("AssociationReport()")

	if (cmd.groupingIdentifier == 0x01) {
		def string_of_assoc = ""
		cmd.nodeId.each {
			string_of_assoc += "${it}, "
		}
		def lengthMinus2 = string_of_assoc.length() - 3
		def final_string = string_of_assoc.getAt(0..lengthMinus2)

		if (cmd.nodeId.any { it == zwaveHubNodeId }) 
		{
			Boolean isStateChange = state.isAssociated ?: false
			result << createEvent(name: "Lifeline",
			value: "${final_string}", 
			descriptionText: "${final_string}",
			displayed: true,
			isStateChange: isStateChange)

			state.isAssociated = true
		} else {
			Boolean isStateChange = state.isAssociated ? true : false
			result << createEvent(name: "Lifeline",
			value: "",
			descriptionText: "${final_string}",
			displayed: true,
			isStateChange: isStateChange)
		}
		state.isAssociated = false
	} else {
		Boolean isStateChange = state.isAssociated ? true : false
		result << createEvent(name: "Lifeline",
		value: "misconfigured",
		descriptionText: "misconfigured group ${cmd.groupingIdentifier}",
		displayed: true,
		isStateChange: isStateChange)
	}
	
	if (state.isAssociated == false) {
      def cmds = [
                   zwave.associationV2.associationSet(groupingIdentifier: 0x01, nodeId: zwaveHubNodeId),
                   zwave.associationV2.associationGet(groupingIdentifier: 0x01),
                 ]
      sendCommands(cmds)         
	}
	
	return result
}

def refresh() {
  sendCommands(zwave.basicV1.basicGet())
}

def installed() {
  createEvent(name: "driverVersion", value: getDriverVersion(), isStateChange: true)
}

def updated() {
  sendEvent([name: "driverVersion", value: getDriverVersion(), isStateChange: true])
  def cmds = [
	zwave.basicV1.basicSet(value: 0xFF),
	zwave.basicV1.basicGet(),
	zwave.versionV1.versionGet(),
	zwave.associationV2.associationGroupingsGet(),
	zwave.associationV2.associationSet(groupingIdentifier: 0x01, nodeId: zwaveHubNodeId),
	zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
	zwave.associationV2.associationGet(groupingIdentifier: 0x01),
    zwave.zwaveCmdClassV1.requestNodeInfo(),
    ]
  sendCommands(cmds)
}

def configure() {
  def cmds = [
		// zwave.associationV2.associationSet(groupingIdentifier:0x01, nodeId:zwaveHubNodeId),
		zwave.associationV2.associationGet(groupingIdentifier: 0x01),
		zwave.associationGrpInfoV1.associationGroupCommandListGet(),
		// zwave.associationGrpInfoV1.associationGroupInfoGet(),
		zwave.associationGrpInfoV1.associationGroupNameGet(),
		zwave.associationV2.associationGroupingsGet()
		// zwave.versionv1.VersionGet(),
		// zwave.zwavecmdclassv1.CmdSetSuc([0x01])//,
		// zwave.zwaveCmdClassV1.requestNodeInfo(),
		// zwave.zwaveCmdClassV1.cmdSucNodeId([0x01]),
		// zwave.securityV1.securityNonceGet(),
		// zwave.securityV1.securitySchemeInherit()
		// zwave.manufacturerSpecificV2.manufacturerSpecificGet()
  ]
  sendCommands(cmds)
}

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
