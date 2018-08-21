// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :
/**
 *  Yamaha Network Receiver
 *     Works on RX-V*
 *    SmartThings driver to connect your Yamaha Network Receiver to SmartThings
 *
 *  Loosely based on: https://github.com/BirdAPI/yamaha-network-receivers
 *   and: http://openremote.org/display/forums/Controlling++RX+2065+Yamaha+Amp
 */

String getDriverVersion () {
  return "v1.05"
}

metadata {
  definition (name: "Yamaha Network Receiver", namespace: "TangentOrgThings", author: "kristopher@acm.org", ocfDeviceType: "x.com.st.d.remotecontroller") {
    capability "Actuator"
    capability "Sensor"
    capability "Switch"
    capability "Switch Level"    
    capability "Polling"
    capability "Refresh"
    capability "Media Playback"
    capability "Music Player" // Added in order to enable ABC
    capability "Audio Mute"
    capability "Audio Volume"

    attribute "driverVersion", "string"

    attribute "input", "string"
    attribute "inputChan", "enum"

    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message
    attribute "parseErrorCount", "number"        // Last error message
    attribute "unknownCommandErrorCount", "number"        // Last error message

    command "inputSelect", ["string"]
    command "inputNext"
  }

  simulator {
    // TODO-: define status and reply messages here
  }


  preferences {
    input("destIp", "text", title: "IP", description: "The device IP")
    input("destPort", "number", title: "Port", description: "The port you wish to connect")
    input("inputChan","enum", title: "Input Control", description: "Select the inputs you want to use", options: ["TUNER","MULTI CH","PHONO","HDMI1","HDMI2","HDMI3","HDMI4","HDMI5","HDMI6","AV1","AV2","AV3","AV4","V-AUX","AUDIO1","AUDIO2","NET","Rhapsody","SIRIUS IR","Pandora","SERVER","NET RADIO","USB","iPod (USB)","AirPlay"],multiple: true,required: true)
    input("Zone","enum", title: "Zone", description: "Select the Zone you want to use", options: ["Main_Zone","Zone_2"],multiple: false,required: true)
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  }

  tiles(scale: 2) {
    multiAttributeTile(name: "state", type:"generic", width:6, height:4) {
      tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
        attributeState("on", label:'${name}', action:"switch.off", backgroundColor: "#79b821", icon:"st.Electronics.electronics16")
        attributeState("off", label: '${name}', action:"switch.on", backgroundColor: "#ffffff", icon:"st.Electronics.electronics16")
      }
      tileAttribute ("device.volume", key: "SLIDER_CONTROL") {
        attributeState("volume", action:"Audio Volume.setVolume")
      }
      tileAttribute ("device.mute", key: "SECONDARY_CONTROL") {
        attributeState("unmuted", action:"Audio Mute.muted", nextState: "muted")
        attributeState("muted", action:"Audio Mute.unmuted", nextState: "unmuted")
      }
    }

    standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: false, canChangeBackground: true) {
      state "on", label: '${name}', action:"switch.off", backgroundColor: "#79b821", icon:"st.Electronics.electronics16"
      state "off", label: '${name}', action:"switch.on", backgroundColor: "#ffffff", icon:"st.Electronics.electronics16"
    }

    controlTile("levelSliderControl", "device.volume", "slider", inactiveLabel: false, width: 1, height: 1, range: "(0..100)") {
      state "volume", action:"Audio Volume.setVolume"
    }

    childDeviceTile("volumeChild", "yamahaVolume", height: 2, width: 2, childTileName: "Volume")

    valueTile("volume", "device.volume", width: 1, height: 1) {
      state "volume", label: '${currentValue}'
    }

    standardTile("refresh", "device.refresh", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
      state "poll", label: "", action: "Refresh.refresh", icon: "st.secondary.refresh", backgroundColor: "#FFFFFF"
    }

    standardTile("input", "device.input", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
      state "input", label: '${currentValue}', action: "inputNext", icon: "", backgroundColor: "#FFFFFF"
    }

    standardTile("mute", "device.mute", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
      state "muted", label: '${name}', action:"Audio Mute.unmute", backgroundColor: "#79b821", icon:"st.Electronics.electronics13"
      state "unmuted", label: '${name}', action:"Audio Mute.mute", backgroundColor: "#ffffff", icon:"st.Electronics.electronics13"
    }

    standardTile("playback", "device.playbackStatus", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
      state "pause", label: "play", action: "Media Playback.play", backgroundColor: "#79b821"
      state "play", label: "pause", action: "Media Playback.pause", backgroundColor: "#FFFFFF", defaultState: true
      state "stop", label: "play", action: "Media Playback.play", backgroundColor: "#79b821"
    }

    main "state"
    details(["switch", "levelSliderControl", "volume", "volumeChild", "input", "mute", "refresh", "playback"])
  }
}



def parse(String description) {
  def map = stringToMap(description)
  log.debug ("Parse started")
  if (! map.body) { 
    log.info ("No body parsed")
    return 
  }
  def body = new String(map.body.decodeBase64())

  def statusrsp = new XmlSlurper().parseText(body)
  log.debug ("Parse got body ${body}...")

  if (statusrsp.Main_Zone.Basic_Status.Power_Control.Power.text()) {
    def power = statusrsp.Main_Zone.Basic_Status.Power_Control.Power.text()
    log.debug ("$Zone Power - ${power}")
    if(power == "On") {
      sendEvent(name: "switch", value: 'on')
    }

    if(power != "" && power != "On") {
      sendEvent(name: "switch", value: 'off')
    }
  }

  if (statusrsp.Main_Zone.Basic_Status.Input.Input_Sel.text()) {
    def inputChan = statusrsp.Main_Zone.Basic_Status.Input.Input_Sel.text()
    log.debug ("$Zone Input - ${inputChan}")
    if(inputChan != "") {
      sendEvent(name: "input", value: inputChan)
    }
  }

  if (statusrsp.Main_Zone.Basic_Status.Volume.Mute.text()) {
    def muteLevel = statusrsp.Main_Zone.Basic_Status.Volume.Mute.text()
    log.debug ("$Zone Mute - ${muteLevel}")
    if (muteLevel == "On") {
      sendEvent(name: "mute", value: 'muted')
    } else {
      sendEvent(name: "mute", value: 'unmuted')
    }
  }

  if (0) {
    if (statusrsp.Main_Zone.Basic_Status.Volume.Lvl.Val.text()) {
      def volLevel = statusrsp.Main_Zone.Basic_Status.Volume.Lvl.Val.text()
      log.debug ("$Zone VolumeOrig - $volLevel") 
      def Integer volLevelAdj = ((1000 + volLevel.toInteger()) / 10)
      log.debug ("$Zone Volume - $volLevelAdj")
      sendEvent(name: "volume", value: volLevelAdj)
      sendEvent(name: "level", value: volLevelAdj)
    }
  }

  if(statusrsp.Main_Zone.Basic_Status.Volume.Lvl.Val.text()) { 
    def int volLevel = statusrsp.Main_Zone.Basic_Status.Volume.Lvl.Val.toInteger() ?: -250        
    volLevel = ((((volLevel + 800) / 9)/5)*5).intValue()
    def int curLevel = 65
    try {
      curLevel = device.currentValue("level")
    } catch(NumberFormatException nfe) { 
      curLevel = 65
    }
    if(curLevel != volLevel) {
      log.debug ("$Zone level - ${volLevel}")
      sendEvent(name: "level", value: volLevel)
      sendEvent(name: "volume", value: volLevel)
    }
  }
  /*
  if(statusrsp.Main_Zone.Basic_Status.Volume.Lvl.Val.text()) {
  def int volLevel = statusrsp.Main_Zone.Basic_Status.Volume.Lvl.Val.toInteger() ?: -250
  volLevel = ((((volLevel + 800) / 9)/5)*5).intValue()
  def int curLevel = 65
  try {
  curLevel = device.currentValue("level")
  } catch(NumberFormatException nfe) {
  curLevel = 65
  }
  if(curLevel != volLevel) {
  log.debug ("$Zone level - ${volLevel}")
  sendEvent(name: "level", value: volLevel)
  }
  }
   */

}

def setVolume(value) {
  if (0) {
    sendEvent(name: "mute", value: "unmuted")
    def Integer result =  ( val * 10 ) - 1000
    //sendCommand("<YAMAHA_AV cmd=\"PUT\"><${getZone()}><Volume><Lvl><Val>${result}</Val><Exp>1</Exp><Unit>dB</Unit></Lvl></Volume></${getZone()}></YAMAHA_AV>")
    // sendCommand("<YAMAHA_AV cmd=\"PUT\"><${Zone}><Volume><Lvl><Val>${(value * 10).intValue()}</Val><Exp>1</Exp><Unit>dB</Unit></Lvl></Volume></${Zone}></YAMAHA_AV>")
    sendCommand("<YAMAHA_AV cmd=\"PUT\"><${Zone}><Volume><Lvl><Val>${result.intValue()}</Val><Exp>1</Exp><Unit>dB</Unit></Lvl></Volume></${Zone}></YAMAHA_AV>")
    sendEvent(name: "volume", value: value)
  }

  setLevel(value)
}

def setLevel(value) {
  sendEvent(name: "mute", value: "unmuted")     
  sendEvent(name: "level", value: value)
  sendEvent(name: "volume", value: value)

  def scaledVal = (value * 9 - 800).toInteger()//sprintf("%d",val * 9 - 800)
  scaledVal = (((scaledVal as Integer)/5) as Integer) * 5
  request("<YAMAHA_AV cmd=\"PUT\"><$Zone><Volume><Lvl><Val>$scaledVal</Val><Exp>1</Exp><Unit>dB</Unit></Lvl></Volume></$Zone></YAMAHA_AV>")
  // sendEvent(name: "volume", value: scaledVal)
}

def volumeUp() {
}

def volumeDown() {
}

def on() {
  log.debug "on"
  sendEvent(name: "switch", value: 'on')
  request("<YAMAHA_AV cmd=\"PUT\"><$Zone><Power_Control><Power>On</Power></Power_Control></$Zone></YAMAHA_AV>")
}

def off() {
  log.debug "off"
  sendEvent(name: "switch", value: 'off')
  request("<YAMAHA_AV cmd=\"PUT\"><$Zone><Power_Control><Power>Standby</Power></Power_Control></$Zone></YAMAHA_AV>")
}

def setMute(state) {
  if (state == "muted") {
    mute()
  } else {
    unmute()
  }
}

def mute() {
  logger("mute()")
  sendEvent(name: "mute", value: "muted")
  request("<YAMAHA_AV cmd=\"PUT\"><$Zone><Volume><Mute>On</Mute></Volume></$Zone></YAMAHA_AV>")
}

def unmute() {
  logger("unmute()")
  sendEvent(name: "mute", value: "unmuted")
  request("<YAMAHA_AV cmd=\"PUT\"><$Zone><Volume><Mute>Off</Mute></Volume></$Zone></YAMAHA_AV>")
}

def inputNext() {

  def cur = device.currentValue("input")
  // modify your inputs right here!
  def selectedInputs = ["HDMI1","HDMI2","Pandora","HDMI1"]


  def semaphore = 0
  for(selectedInput in selectedInputs) {
    if(semaphore == 1) {
      return inputSelect(selectedInput)
    }
    if(cur == selectedInput) {
      semaphore = 1
    }
  }
}


def inputSelect(channel) {
  sendEvent(name: "input", value: channel )
  log.debug "Input $channel"
  request("<YAMAHA_AV cmd=\"PUT\"><$Zone><Input><Input_Sel>$channel</Input_Sel></Input></$Zone></YAMAHA_AV>")
}

def setPlaybackStatus(status) {
  if (status == "play") {
    play()
  } else if (status == "pause") {
    pause()
  } else if (status == "stop") {
    stop()
  }
}

def play() {
  logger("play()")
  sendEvent(name: "playbackStatus", value: "play")
  request("<YAMAHA_AV cmd=\"PUT\"><$Zone><Play_Control><Playback>Play</Playback></Play_Control></$Zone></YAMAHA_AV>")
}

def stop() {
  logger("stop()")
  sendEvent(name: "playbackStatus", value: "stop")
  request("<YAMAHA_AV cmd=\"PUT\"><$Zone><Play_Control><Playback>Stop</Playback></Play_Control></$Zone></YAMAHA_AV>")
}

def pause() {
  logger("pause()")
  sendEvent(name: "playbackStatus", value: "pause")
  request("<YAMAHA_AV cmd=\"PUT\"><$Zone><Play_Control><Playback>Pause</Playback></Play_Control></$Zone></YAMAHA_AV>")
}

def poll() {
  refresh()
}

def refresh() {
  log.debug ("Refresh")
  request("<YAMAHA_AV cmd=\"GET\"><$Zone><Basic_Status>GetParam</Basic_Status></$Zone></YAMAHA_AV>")
}

def request(body) {

  def hosthex = convertIPtoHex(destIp)
  def porthex = convertPortToHex(destPort)
  device.deviceNetworkId = "$hosthex:$porthex"

  def hubAction = new physicalgraph.device.HubAction(
    'method': 'POST',
    'path': "/YamahaRemoteControl/ctrl",
    'body': body,
    'headers': [ HOST: "$destIp:$destPort" ]
  )

  // sendHubCommand(hubAction)

  hubAction
}


private String convertIPtoHex(ipAddress) {
  String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
  return hex
}

private String convertPortToHex(port) {
  String hexport = port.toString().format( '%04X', port.toInteger() )
  return hexport
}

private void createChildDevices() {
  // Save the device label for updates by updated()
  state.oldLabel = device.label

  // Add child devices for four button presses
  addChildDevice(
    "Yamaha Network Receiver Volume",
    "${device.displayName}/volume",
    "",
    [
    componentName: "yamahaVolume",
    label         : "$device.displayName Volume",
    completedSetup: true,
    isComponent: true,
    ])
}

def installed() {
  log.info("$device.displayName installed()")
  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
  createChildDevices()
}

def updated() {
  if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
    return
  }
  log.info("$device.displayName updated() debug: ${settings.debugLevel}")

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)
  sendEvent(name: "parseErrorCount", value: 0, displayed: false)
  sendEvent(name: "unknownCommandErrorCount", value: 0, displayed: false)
  state.parseErrorCount = 0
  state.unknownCommandErrorCount = 0

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  if (! childDevices) {
    createChildDevices()
  }

  childDevices.each { logger("${it.deviceNetworkId}") }

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
void logger(msg, level = "trace") {
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
    if (settings.debugLevel >= 2) {
      log.warn msg
      sendEvent(name: "logMessage", value: "WARNING: ${msg}", displayed: false, isStateChange: true)
    }
    return

    case "info":
    if (settings.debugLevel >= 3) {
      log.info msg
    }
    return

    case "debug":
    if (settings.debugLevel >= 4) {
      log.debug msg
    }
    return

    case "trace":
    if (settings.debugLevel >= 5) {
      log.trace msg
    }
    return

    case "error":
    default:
    break
  }

  log.error msg
  sendEvent(name: "lastError", value: "ERROR: ${msg}", displayed: false, isStateChange: true)
}
