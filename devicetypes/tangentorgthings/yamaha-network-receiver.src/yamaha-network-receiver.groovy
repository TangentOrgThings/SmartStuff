// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :
/**
 *  Yamaha Network Receiver
 *     Works on RX-V*
 *    SmartThings driver to connect your Yamaha Network Receiver to SmartThings
 *
 *  Loosely based on: https://github.com/BirdAPI/yamaha-network-receivers
 *   and: http://openremote.org/display/forums/Controlling++RX+2065+Yamaha+Amp
 */

import groovy.json.JsonSlurper
import groovy.util.XmlSlurper
import physicalgraph.*

String getDriverVersion () {
  return "v1.15"
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
    capability "Media Track Control"
    capability "Audio Mute"
    capability "Audio Volume"

    attribute "driverVersion", "string"

    attribute "input", "string"
    attribute "inputChan", "enum"

    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message

    command "setInputSource", ["string"]

    attribute "inputSource", "string"
    attribute "supportedInputSources", "string"

    attribute "dB", "number"
  }

  simulator {
    // TODO-: define status and reply messages here
  }


  preferences {
    input(name: "destIp", type: "text", title: "IP", description: "The device IP")
    input(name: "destPort", type: "number", title: "Port", description: "The port you wish to connect")
    input(name: "inputChan", type: "enum", title: "Input Control", description: "Select the inputs you want to use", options: ["TUNER","MULTI CH","PHONO","HDMI1","HDMI2","HDMI3","HDMI4","HDMI5","HDMI6","AV1","AV2","AV3","AV4","V-AUX","AUDIO1","AUDIO2","NET","Rhapsody","SIRIUS IR","Pandora","SERVER","NET RADIO","USB","iPod (USB)","AirPlay"],multiple: true,required: true)
    input(name: "Zone", type: "enum", title: "Zone", description: "Select the Zone you want to use", options: ["Main_Zone","Zone_2"],multiple: false,required: true)
    input(name: "volumeStep", type: "decimal", range: 0.5..10, title: "Volume Step", description: "Enter the amount the volume up and down commands should adjust the volume", defaultValue: 2.5)
    input(name: "maxVolume", type: "number", range: -80..15, title: "Max Volume", description: "Enter the maximum volume in reference decibals that the receiver is allowed", defaultValue: 28)
    input(name: "minVolume", type: "number", range: -80..15, title: "Min Volume", description: "Enter the minimum volume in reference decibals that the receiver is allowed", defaultValue: -50)
    input(name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false)
  }

  tiles(scale: 2) {
    multiAttributeTile(name: "mediaMulti", type: "mediaPlayer", width: 6, height: 4) {
        tileAttribute("device.playbackStatus", key: "PRIMARY_CONTROL") {
            attributeState("pause", label: "Paused",)
            attributeState("play", label: "Playing")
            attributeState("stop", label: "Stopped")
        }
        tileAttribute("device.playbackStatus", key: "MEDIA_STATUS") {
            attributeState("pause", label: "Paused", action: "Media Playback.play", nextState: "play", backgroundColor: "#79b821")
            attributeState("play", label: "Playing", action: "Media Playback.pause", nextState: "pause", backgroundColor: "#FFFFFF", defaultState: true)
            attributeState("stop", label: "Stopped", action: "Media Playback.play", nextState: "play", backgroundColor: "#79b821")
        }
        tileAttribute("device.status", key: "PREVIOUS_TRACK") {
          attributeState("status", action: "Media Track Control.previousTrack", defaultState: true)
        }
        tileAttribute("device.status", key: "NEXT_TRACK") {
          attributeState("status", action: "Media Track Control.nextTrack", defaultState: true)
        }
        tileAttribute ("device.volume", key: "SLIDER_CONTROL") {
          attributeState("volume", action: "Audio Volume.setVolume")
        }
        tileAttribute ("device.mute", key: "MEDIA_MUTED") {
          attributeState("unmuted", action: "Audio Mute.muted", nextState: "muted")
          attributeState("muted", action: "Audio Mute.unmuted", nextState: "unmuted")
        }
        tileAttribute("device.input", key: "MARQUEE") {
          attributeState("input", label: "${currentValue}", defaultState: true)
        }
    }

    standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: false, canChangeBackground: true) {
      state "on", label: '${name}', action: "switch.off", backgroundColor: "#79b821", icon:"st.Electronics.electronics16"
      state "off", label: '${name}', action: "switch.on", backgroundColor: "#ffffff", icon:"st.Electronics.electronics16"
    }

    controlTile("levelSliderControl", "device.volume", "slider", inactiveLabel: false, width: 2, height: 1, range: "(0..100)") {
      state "volume", action: "Audio Volume.setVolume"
    }

    childDeviceTile("volumeChild", "yamahaVolume", height: 2, width: 2, childTileName: "Volume")

    valueTile("volume", "device.volume", width: 1, height: 1) {
      state "volume", label: '${currentValue}'
    }
    
    valueTile("dB", "device.dB", width: 1, height: 1) {
      state "", label: '${currentValue}'
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

    main "mediaMulti"
    details(["mediaMulti", "switch", "levelSliderControl", "volume", "dB", "volumeChild", "input", "mute", "refresh", "playback"])
  }
}

def parse(String description) {
  def map = parseLanMessage(description)
  
  log.debug ("Parse started")
  if (! map.body) { 
    logger ("No body parsed")
    return 
  }
  
  //
  def body = getHttpBody(map.body);
   
  def event = body.children()[0]

  if (event == null) {
    logger("No data found in event")
    return
  }

  if (event.name() == "System") {
    def system_leaf = event.children()[0]

    if (system_leaf.name() == "Misc") {
      updateMisc(event)
      return
    }

    updateZone(event)
    return
  }

  updateZone(event)
}

def updateMisc(system_event) {
  logger("updateMisc()")

  if (system_event.Misc.Network.Info.MAC_ADDRESS.text()) {
    state.MAC_Address= system_event.Misc.Network.Info.MAC_ADDRESS.text()
  }
}

def updateZone(zone_info) {
  logger("updateZone()")
  
  if (zone_info == null) {
    logger("updateZone() passed NULL")
  }
  
  if (zone_info.Basic_Status.Power_Control.Power.text()) {
    def power = zone_info.Basic_Status.Power_Control.Power.text()
    log.debug ("$Zone Power - ${power}")

    if (power != "") {
      sendEvent(name: "switch", value: (power == "On") ? "on" : "off")
    }
  }
  
  if (zone_info.Basic_Status.Play_Control.Playback.text()) {
    def play_status = zone_info.Basic_Status.Play_Control.Playback.text()
    log.debug ("$Zone Play Status - ${play_status}")

    if (play_status != "") {
      if (play_status == "Play") {
        sendEvent(name: "switch", value: "play")
      } else if (play_status == "Pause") {
        sendEvent(name: "switch", value: "pause")
      } else if (play_status == "Stop") {
        sendEvent(name: "switch", value: "stop")
      }
    }
  }

  if (zone_info.Basic_Status.Input.Input_Sel.text()) {
    def inputChan = zone_info.Basic_Status.Input.Input_Sel.text()
    log.debug ("$Zone Input - ${inputChan}")
    if (inputChan != "") {
      sendEvent(name: "input", value: inputChan)
    }
  }
  
  if (zone_info.Config.Name.Input.text()) {
    def supportedSources = zone_info.Config.Name.Input.children().findAll {
      node ->
        node.text() ?.trim()
    }
    state.supportedSources = supportedSources.join(", ")
  }

  if (zone_info.Basic_Status.Volume.Mute.text()) {
    sendEvent(name: "mute", value: (zone_info.Basic_Status.Volume.Mute.text() == "On") ? "muted" : "unmuted")
  }
  
  if (zone_info.Basic_Status.Volume.Lvl.Val.text()) { 
    def int volLevel = zone_info.Basic_Status.Volume.Lvl.Val.toInteger() ?: -250
    def double dB = volLevel / 10.0
    
    sendEvent(name: "volume", value: calcRelativePercent(dB).intValue())
    sendEvent(name: "level", value: calcRelativePercent(dB).intValue())
    sendEvent(name: "dB", value: dB)
  }

  if (zone_info.Input.Input_Sel_Item_Info.Param.text()) {
    def inputInterface = Input.Input_Sel_Item_Info.Param.text() 
    def inputTitle = ""

    if (zone_info.Input.Input_Sel_Item_Info.Title.text()) {
      inputTitle = Input.Input_Sel_Item_Info.Title.text() 
    }
  }
}

def setVolume(value) {
  logger("setVolume(${value})")
  setLevel(value)
}

def setLevel(value) {
  logger("setLevel(${value})")
  
  if (value > 100) value = 100
  if (value < 0) value = 0
  
  def db = calcRelativeValue(value)
  sendVolume(db)

  sendEvent(name: "mute", value: "unmuted")     
  sendEvent(name: "level", value: value)
  sendEvent(name: "volume", value: value)
}

def sendVolume(double db) {
  logger("sendVolume(${db})")
  db = roundNearestHalf(db)
  def strCmd = "<YAMAHA_AV cmd=\"PUT\"><${getZone()}><Volume><Lvl><Val>${(db * 10).intValue()}</Val><Exp>1</Exp><Unit>dB</Unit></Lvl></Volume></${getZone()}></YAMAHA_AV>"
  logger groovy.xml.XmlUtil.escapeXml("strCmd ${strCmd}")
  request(strCmd, true)
  sendEvent(name: "dB", value: db)
  request("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"GET\"><${getZone()}><Volume><Lvl>GetParam</Lvl></Volume></${getZone()}></YAMAHA_AV>", true)
  request("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"GET\"><${getZone()}><Volume><Mute>GetParam</Mute></Volume></${getZone()}></YAMAHA_AV>", true)
}

def setDb(value) {
  logger("setDb(${value})")
  def double dB = value
  if (dB > maxVolume) dB = maxVolume
  if (dB < minVolume) dB = minVolume
  logger("Zone ${getZone()} volume set to ${value}")
  sendVolume(value)
  sendEvent(name: "volume", value: calcRelativePercent(value).intValue())
  sendEvent(name: "level", value: calcRelativePercent(value).intValue())
}

def volumeUp() {
  logger("volumeUp()")
  setDb(device.currentValue("dB") + volumeStep)
}

def volumeDown() {
  logger("volumeDown()")
  setDb(device.currentValue("dB") - volumeStep)
}

def childOn() {
  logger("childOn()")
}

def childOff() {
  logger("childOff()")
}

def on() {
  logger("on()")
  sendEvent(name: "switch", value: 'on')
  request("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"PUT\"><${getZone()}><Power_Control><Power>On</Power></Power_Control></${getZone()}></YAMAHA_AV>")
}

def off() {
  logger("off()")
  sendEvent(name: "switch", value: 'off')
  request("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"PUT\"><${getZone()}><Power_Control><Power>Standby</Power></Power_Control></${getZone()}></YAMAHA_AV>")
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
  request("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"PUT\"><${getZone()}><Volume><Mute>On</Mute></Volume></${getZone()}></YAMAHA_AV>")
}

def unmute() {
  logger("unmute()") 
  request("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"PUT\"><${getZone()}><Volume><Mute>Off</Mute></Volume></${getZone()}></YAMAHA_AV>")
}

def inputNext() {
  logger("inputNext()")
  
  def cur = device.currentValue("input")
  // modify your inputs right here!
  def selectedInputs = ["HDMI1", "HDMI2", "Pandora", "HDMI1"]

  def semaphore = 0
  for (selectedInput in selectedInputs) {
    if (semaphore == 1) {
      return setInputSelect(selectedInput)
    }
    if (cur == selectedInput) {
      semaphore = 1
    }
  }
}

def setInputSource(mode) {
  logger("setInputSource($mode)")
  request("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"PUT\"><${getZone()}><Input><Input_Sel>$channel</Input_Sel></Input></${getZone()}></YAMAHA_AV>")
}

def getInputSource() {
  logger("getInputSource($mode)")
  request("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"GET\"><Main_Zone><Input><Input_Sel_Item_Info>GetParam</Input_Sel_Item_Info></Input></Main_Zone></YAMAHA_AV>", true)
}

def setPlaybackStatus(status) {
  logger("setPlaybackStatus($status)")
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
  request("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"PUT\"><${getZone()}><Play_Control><Playback>Play</Playback></Play_Control></${getZone()}></YAMAHA_AV>")
}

def stop() {
  logger("stop()")
  request("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"PUT\"><${getZone()}><Play_Control><Playback>Stop</Playback></Play_Control></${getZone()}></YAMAHA_AV>")
}

def pause() {
  logger("pause()")
  request("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"PUT\"><${getZone()}><Play_Control><Playback>Pause</Playback></></${getZone()}></YAMAHA_AV>")
}

def nextTrack() {
  logger("nextTrack()")
  request("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"PUT\"><${getZone()}><Play_Control><Playback>Skip Fwd</Playback></Play_Control></${getZone()}></YAMAHA_AV>")
}

def previousTrack() {
  logger("previousTrack()")
  request("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"PUT\"><${getZone()}><Play_Control><Playback>Skip Rev</Playback></Play_Control></${getZone()}></YAMAHA_AV>")
}

def poll() {
  logger("poll()")
  refresh()
}

def refresh() {
  logger ("refresh()")
  request("<YAMAHA_AV cmd=\"GET\"><${getZone()}><Basic_Status>GetParam</Basic_Status></${getZone()}></YAMAHA_AV>", true)
  request("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"GET\"><System><Power_Control><Power>GetParam</Power></Power_Control></System></YAMAHA_AV>", true)
}

def startActivity(activityId) {
  logger("startActivity($activityId)")
}

def setCurrentActivity(activity) {
  logger("setCurrentActivity($activity)")
  sendEvent(name: "currentActivity", value: "$activity")
}

def buildScenes() {
  def sceneList = ["Scene_1", "Scene_2", "Scene_3", "Scene_4"]
  def json = new groovy.json.JsonBuilder(sceneList)
  def data = json.toString()
    	
  sendEvent(name: "activities", value: data)
}

def getNetworkMisc() {
  request("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"GET\"><System><Misc><Network><Info>GetParam</Info></Network></Misc></System></YAMAHA_AV>", true)
}

def request(body, noRefresh = false) {
  def hosthex = convertIPtoHex(destIp)
  def porthex = convertPortToHex(destPort)
  device.deviceNetworkId = "$hosthex:$porthex"

  def hubAction = new physicalgraph.device.HubAction(
    'method': 'POST',
    'path': "/YamahaRemoteControl/ctrl",
    'body': body,
    'headers': [ HOST: "$destIp:$destPort" ]
  )

  sendHubCommand(hubAction)
  
  if (noRefresh == false) {
    refresh()
  }
}

private getHttpBody(body) {
  def obj = null;
  if (body) {
    obj = new XmlSlurper().parseText(new String(body))
  }
  return obj
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
  String switch_name = "Mute"
  if (1) {
    String childDni = "${device.displayName}/${switch_name}"
    def child = childDevices.find { it.deviceNetworkId == childDni }
    addChildDevice("smartthings", "Child Switch", childDni, null, [
      completedSetup: true,
      label         : "$device.displayName ${switch_name}",
      isComponent   : true,
      componentName : "switch${switch_name}",
      componentLabel: "${switch_name}"
    ])
  }
}

def installed() {
  logger("$device.displayName installed()")
  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
  createChildDevices()
  buildScenes()

  getNetworkMisc() // get Mac ADDR

  refresh()
}

def updated() {
  logger("$device.displayName updated() debug: ${settings.debugLevel}")

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  if (1) { //! childDevices) {
    createChildDevices()
  }

  childDevices.each { logger("${it.deviceNetworkId}") }
  buildScenes()

  getNetworkMisc() // get Mac ADDR

  refresh()
}

private getZone() {
  return new String("Main_Zone")
}

private calcRelativePercent(db) {
  logger "calcRelativePercent(${db})"
  def range = maxVolume - minVolume
  def correctedStartValue = db - minVolume
  def percentage = (correctedStartValue * 100) / range
  logger "percentage: ${percentage}"
  
  return percentage
}

private calcRelativeValue(perc) {
  logger "calcRelativeValue(${perc})"
  def value = (perc * (maxVolume - minVolume) / 100) + minVolume
  logger "value: ${value}"
  
  return value
}

private roundNearestHalf(value) {
  logger "roundNearestHalf(${value})"
  logger "result: ${Math.round(value * 2) / 2.0}"
  return Math.round(value * 2) / 2.0
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
    case "parse":
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
