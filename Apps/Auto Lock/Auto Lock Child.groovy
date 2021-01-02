/* 
 *   Hubitat Import URL: https://raw.githubusercontent.com/heidrickla/Hubitat/Apps/Auto%20Lock/Auto%20Lock%20Child.groovy
 *
 *   Author Chris Sader, modified by Lewis Heidrick with permission from Chris to takeover the project.
 *   
 *   12/28/2020 - Project Published to GitHub
 */

def setVersion() {
    state.name = "Auto Lock"
	state.version = "1.1.4"
}

definition(
    name: "Auto Lock Child",
    namespace: "heidrickla",
    author: "Lewis Heidrick",
    description: "Automatically locks a specific door after X minutes/seconds when closed and unlocks it when open.",
    category: "Convenience",
    parent: "heidrickla:Auto Lock",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/heidrickla/Hubitat/Apps/Auto%20Lock/Auto%20Lock%20Child.groovy")

preferences {
    page(name: "mainPage")
    page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
        }
    }
}

def mainPage() {    
    dynamicPage(name: "", title: "", install: true, uninstall: true, refreshInterval:0) {
    ifTrace("mainPage")
    turnOffLoggingTogglesIn30()
        
    setPauseButtonName()
    section("") {
      input name: "Pause", type: "button", title: state.pauseButtonName, submitOnChange:true
    }
    section("") {
        input name: "thisName", type: "text", title: "", required:true, submitOnChange:true
        updateLabel()
    }
    section("") {
        input "lock1", "capability.lock", title: "Lock: ", required: true, submitOnChange: true
        input "contact", "capability.contactSensor", title: "Door Contact: ", required: false, submitOnChange: true
        input "refresh", "bool", title: "Click here to refresh the device status", submitOnChange:true
        app.updateSetting("refresh",[value:"false",type:"bool"])
    }
    section() {
        input type: "bool", name: "minSec", title: "Default is minutes. Use seconds instead?", required: true, defaultValue: false
        input "duration", "number", title: "Lock it how many minutes/seconds later?", required: true, defaultValue: 10
//        input "retryLock", "bool", title: "Enable retries if lock fails to change state.", require: false, defaultValue: true
        input "maxRetries", "number", title: "Maximum number of retries?", require: false, defaultValue: 3
        input "delayBetweenRetries", "number", title: "Delay between retries?", require: false, defaultValue: 5
//        input "autoRefreshXMinutes", "enum", title: "Force a refresh of the state of the lock?", require: false, options: ["Never", "1", "5", "15", "30", "60"], defaultValue: "Never"
    }
    section("Logging Options", hideable: true, hidden: hideLoggingSection()) {
            input "isInfo", "bool", title: "Enable Info logging for 30 minutes", submitOnChange: false, defaultValue: false
            input "isDebug", "bool", title: "Enable debug logging for 30 minutes", submitOnChange: false, defaultValue: false
		    input "isTrace", "bool", title: "Enable Trace logging for 30 minutes", submitOnChange: false, defaultValue: false
            input "ifLevel","enum", title: "IDE logging level",required: true, options: getLogLevels(), defaultValue : "1"
            paragraph "NOTE: IDE logging level overrides the temporary logging selections."
    }
    section(title: "Only Run When:", hideable: true, hidden: hideOptionsSection()) {
			def timeLabel = timeIntervalLabel()
			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null
			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
			    options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
            input "disabledSwitch", "capability.switch", title: "Switch to Enable and Disable this app", submitOnChange:true, required:false, multiple:true
    }
    }
}

// Application settings and startup
def installed() {
    ifTrace("installed")
    ifDebug("Auto Lock Door installed.")
    state.installed = true
    if (batteryStatus == null) {lock1BatteryStatus = " "}
    if (contactStatus == null) {scontactBatteryStatus = " "}
    initialize()
}

def updated() {
    ifTrace("updated")
    ifDebug("Settings: ${settings}")
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    ifTrace("initialize")
    ifDebug("Settings: ${settings}")
    subscribe(lock1, "lock", doorHandler)
    subscribe(contact, "contact.open", doorHandler)
    subscribe(contact, "contact.closed", doorHandler)
    subscribe(disabledSwitch, "switch.on", disabledHandler)
    subscribe(disabledSwitch, "switch.off", disabledHandler)
    subscribe(deviceActivationSwitch, "switch", deviceActivationSwitchHandler)
    subscribe(lock1, "lock", diagnosticHandler)
    subscribe(contact, "contact.open", diagnosticHandler)
    subscribe(contact, "contact.closed", diagnosticHandler)
    subscribe(lock1, "battery", batteryHandler)
    subscribe(contact, "battery", batteryHandler)
    updateLabel()
    getAllOk()
}

// Device Handlers
def diagnosticHandler(evt) {
    ifTrace("diagnosticHandler")
    if (lock1?.currentValue("lock") != null) {
        lock1Status = lock1.currentValue("lock")
        
    } else if (lock1?.latestValue("lock") != null) {
        lock1Status = lock1.latestValue("lock")
    } else {
        lock1Status = " "
        ifTrace("diagnosticHandler: lock1Status = ${lock1Status}")
    }
    if (contactSensor?.currentValue("contact") != null) {
        contactStatus = contactSensor.currentValue("contact")
    } else if (contactSensor?.latestValue("contact") != null) {
        contactStatus = contactSensor.latestValue("contact")
    } else {
        contactStatus = " "   
    }
    updateLabel()
}

def batteryHandler(evt) {
    if (lock1?.currentValue("battery") != null) {
        lock1BatteryStatus = lock1.currentValue("battery")
    } else if (lock1?.latestValue("battery") != null) {
        lock1BatteryStatus = lock1.latestValue("battery")
    } else {
        lock1BatteryStatus = " "
    }
    if (contact?.currentValue("battery") != null) {
        contactBatteryStatus = contact.currentValue("battery")
    } else if (contact?.latestValue("contact") != null) {
        contactBatteryStatus = contact.latestValue("battery")
    } else {
        lock1BatteryStatus = " "
    }
}

def doorHandler(evt) {
    ifTrace("doorHandler")
    if (getAllOk() != true) {
    } else {
        updateLabel()
        if (state?.pausedOrDisabled == false) {
            if (evt.value == "closed") {ifDebug("Door Closed")}
            if (evt.value == "opened") {
                    ifDebug("Door open reset previous lock task...")
                    unschedule(lockDoor)
                    if (minSec) {
                        def delay = duration
                        runIn(delay, lockDoor)
                    } else {
	                    def delay = duration * 60
                        runIn(delay, lockDoor)
                    }
            }
            if (evt.value == "locked") {                  // If the human locks the door then...
                ifDebug("Cancelling previous lock task...")
                unschedule(lockDoor)                  // ...we don't need to lock it later.
                state.status = "(Locked)"
            } else {                                      // If the door is unlocked then...
                state.status = "(Unlocked)"
                if (minSec) {
	                def delay = duration
                    ifDebug("Re-arming lock in in $duration second(s)")
                    runIn(delay, lockDoor)
                } else {
                    def delay = duration * 60
	                ifDebug("Re-arming lock in in $duration minute(s)")
                    runIn(delay, lockDoor)
                }
            }
        }
      updateLabel()
    }
}

def disabledHandler(evt) {
    ifTrace("disabledHandler")
    if (getAllOk() != true) {
        } else {
        if(disabledSwitch) {
            disabledSwitch.each { it ->
            disabledSwitchState = it.currentValue("switch")
                if (disabledSwitchState == "on") {
                    ifTrace("disabledHandler: Disable switch turned on")
                    state.disabled = false
                    if (state?.paused == true) {
                        state.status = "(Paused)"
                        state.pausedOrDisabled = true
                    } else {
                        state.paused = false
                        state.disabled = false
                        state.pausedOrDisabled = false
                        if (lock1?.currentValue("lock") == "unlocked" && (contact?.currentValue("contact") == "closed" || contact == null)) {
                            ifDebug("disabledHandler: App was enabled or unpaused and lock was unlocked. Locking door.")
                            lock1.lock()
                            count = maxRetries
                            if (minSec) {
                                def delay = duration
                                runIn(delay, checkLockedStatus)
                            } else {
	                            def delay = duration * 60
                                runIn(delay, checkLockedStatus)
                            }
                        }
                    }
                } else if (disabledSwitchState == "off") {
                    state.pauseButtonName = "Disabled by Switch"
                    state.status = "(Disabled)"
                    ifTrace("disabledHandler: (Disabled)")
                    state.disabled = true
                    updateLabel()
                }
            }
        }
        updateLabel()
    }
}

def deviceActivationSwitchHandler(evt) {
    ifTrace("deviceActivationSwitchHandler")
    if (getAllOk() != true) {
    } else {
        updateLabel()
        if (state.pausedOrDisabled == false) {
            if (deviceActivationSwitch) {
                deviceActivationSwitch.each { it ->
                    deviceActivationSwitchState = it.currentValue("switch")
                }
                    if (deviceActivationSwitchState == "on") {
                        ifDebug("deviceActivationSwitchHandler: Locking the door now")
                        lock1.lock()
                        count = maxRetries
                        if (minSec) {
                            def delay = duration
                            runIn(delay, checkLockedStatus)
                        } else {
	                        def delay = duration * 60
                            runIn(delay, checkLockedStatus)
                        }
                    } else if (deviceActivationSwitchState == "off") {
                        ifDebug("deviceActivationSwitchHandler: Unlocking the door now")
                        lock1.unlock()
                        count = maxRetries
                        if (minSec) {
                            def delay = duration
                            runIn(delay, checkUnlockedStatus)
                        } else {
	                        def delay = duration * 60
                            runIn(delay, checkUnlockedStatus)
                        }
                   }
            }
        } else {
            ifTrace("deviceActivationSwitchHandler: Application is paused or disabled.")
        }
        updateLabel()
    }
}

// Application Functions
def lockDoor() {
    ifTrace("lockDoor")
    ifDebug("Locking Door if Closed")
    if (getAllOk() != true) {
        ifTrace("TurnOffFanSwitch: getAllOk = ${getAllOk()}")
    } else {
        updateLabel()
        if (state?.pausedOrDisabled == false) {
            ifTrace("lockDoor: contact == ${contact}")
            if ((contact?.currentValue("contact") == "closed" || contact == null)) {
                ifDebug("Door Closed")
                lock1.lock()
                count = maxRetries
                if (minSec) {
                    def delay = duration
                    runIn(delay, checkLockedStatus)
                } else {
	                def delay = duration * 60
                    runIn(delay, checkLockedStatus)
                    } 
            } else {
                if (contact?.currentValue("contact") == "open") {
                    if (lock1?.currentValue("lock") == "locked") {
                        count = maxRetries
                        lock1.unlock()
                        if (minSec) {
                                def delay = duration
                                runIn(delay, checkUnlockedStatus)
                            } else {
	                            def delay = duration * 60
                                runIn(delay, checkUnlockedStatus)
                            }
                    }
                    ifTrace("lockDoor Door was open - waiting")
                    if (minSec) {
                        def delay = duration
                        runIn(delay, lockDoor)
                        ifDebug("Door open will try again in $duration second(s)")
                    } else {
	                    def delay = duration * 60
                        runIn(delay, lockDoor)
                        ifDebug("Door open will try again in $duration minute(s)")
                    }
                }
	        }
        }
        updateLabel()
    }
}


def checkLockedStatus() {
    ifTrace("checkUnlockedStatus")
    if (lock1.currentValue("lock") == "locked") {
        state.status = "(Locked)"
        ifTrace("checkLockedStatus: The lock was Locked successfully")
    } else if (lock1.CurrentValue("lock") != "locked") {
        state.status = "(Unlocked)"
        count = (count - 1)
        if (count > -1) {
            runIn(delayBetweenRetries, retryLockingCommand)
        } else {
            ifInfo("Maximum retries exceeded. Giving up on locking door.")
        }
    }
    updateLabel()
}

def retryLockingCommand() {
    ifTrace("retryLockingCommand")
    lock1.lock()
    runIn(delay, checkLockedStatus())
}

def unlockDoor() {
    ifTrace("unlockDoor")
    ifDebug("Unlocking Door if Open")
    if (getAllOk() != true) {
        ifTrace("unlockDoor: getAllOk = ${getAllOk()}")
    } else {
        updateLabel()
        if (state?.pausedOrDisabled == false) {
            ifInfo("unlockDoor: Unlocking the door now")
            lock1.unlock()
            count = maxRetries
            if (minSec) {
                def delay = duration
                runIn(delay, checkUnlockedStatus)
            } else {
	            def delay = duration * 60
                runIn(delay, checkUnlockedStatus)
            }
        }
    updateLabel()
    }
}

def checkUnlockedStatus() {
    ifTrace("checkUnlockedStatus")
    if (lock1.currentValue("lock") == "unlocked") {
        state.status = "(Unlocked)"
        ifTrace("checkUnlockedStatus: The lock was unlocked successfully")
    } else if (lock1.CurrentValue("lock") != "unlocked")  {
        state.status = "(Locked)"
        count = (count - 1)
        if (count > -1) {
            runIn(delayBetweenRetries, retryUnlockingCommand)
        } else {
            ifInfo("Maximum retries exceeded. Giving up on unlocking door.")
        }
    }
    updateLabel()
}

def retryUnlockingCommand() {
    ifTrace("retryUnlockingCommand")
    lock1.unlock()
    runIn(delay, checkUnlockedStatus())
}

//Label Updates
void updateLabel() {
    ifTrace("updateLabel")
    if (getAllOk() != true) {
        ifTrace("updateLabel: getAllOk = ${getAllOk()}")
        state.status = "(Disabled by Time, Day, or Mode)"
        appStatus = "<span style=color:brown>(Disabled by Time, Day, or Mode)</span>"
    } else {
        if ((state?.pause == true) || (state?.disabled == true)) {
            state.pausedOrDisabled = true
        } else {
            state.pausedOrDisabled = false
        }
        if (state?.disabled == true) {
            state.status = "(Disabled)"
            appStatus = "<span style=color:red>(Disabled)</span>"
        } else if (state?.paused == true) {
            state.status = "(Paused)"
            appStatus = "<span style=color:red>(Paused)</span>"
        } else if (lock1?.currentValue("lock") == "locked") {
            state.status = "(Locked)"
            appStatus = "<span style=color:green>(Locked)</span>"
        } else if (lock1?.currentValue("lock") == "unlocked") {
            state.status = "(Unlocked)"
            appStatus = "<span style=color:orange>(Unlocked)</span>"
        } else {
            state.paused = false
            state.disabled = false
            state.pausedOrDisabled = false
            state.status = "(Unknown)"
            appStatus = "<span style=color:pink>(Unknown)</span>"
        }
    }
    app.updateLabel("${thisName} ${appStatus}")
}

//Enable, Resume, Pause button
def appButtonHandler(btn) {
    ifTrace("appButtonHandler")
    if (btn == "Disabled by Switch") {
        state.disabled = false
    } else if (btn == "Resume") {
        state.disabled = false
        state.paused = !state.paused
    } else if (btn == "Pause") {
        state.paused = !state.paused
        if (state?.paused) {
            unschedule()
            unsubscribe()
        } else {
            initialize()
            state.pausedOrDisabled = false
            if (lock1?.currentValue("lock") == "unlocked") {
                ifTrace("appButtonHandler: App was enabled or unpaused and fan was on. Locking the door.")
                lockDoor()
            }
        }
    }
    updateLabel()
}

def setPauseButtonName() {
    if (state?.disabled == true) {
        state.pauseButtonName = "Disabled by Switch"
        unsubscribe()
        unschedule()
        subscribe(disabledSwitch, "switch", disabledHandler)
        updateLabel()
    } else if (state?.paused == true) {
        state.pauseButtonName = "Resume"
        unsubscribe()
        unschedule()
        subscribe(disabledSwitch, "switch", disabledHandler)
        updateLabel()
    } else {
        state.pauseButtonName = "Pause"
        initialize()
        updateLabel()
    }
}

// Application Page settings
private hideLoggingSection() {
	(isInfo || isDebug || isTrace || ifLevel) ? true : true
}

private hideOptionsSection() {
	(starting || ending || days || modes || manualCount) ? true : true
}

def getAllOk() {
    if (modeOk && daysOk && timeOk) {
        return true
    } else {
        return false
    }
}

private getModeOk() {
	def result = (!modes || modes.contains(location.mode))
	result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
	result
}

private getTimeOk() {
	def result = true
	if ((starting != null) && (ending != null)) {
		def currTime = now()
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	result
}

private hhmm(time, fmt = "h:mm a") {
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private timeIntervalLabel() {
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}

// Logging functions
def getLogLevels() {
    return [["0":"None"],["1":"Info"],["2":"Debug"],["3":"Trace"]]
}

def turnOffLoggingTogglesIn30() {
if (!isDebug) {
        app.updateSetting("isDebug", false)
    }
    if (isTrace == true) {
        runIn(1800, traceOff)
    }
    if (isDebug == true) {
        runIn(1800, debugOff)
    }
    if (isTrace == true) {
        runIn(1800, traceOff)
    }
}

def infoOff() {
    app.updateSetting("isInfo", false)
    log.info "${thisName}: Info logging disabled."
    app.updateSetting("isInfo",[value:"false",type:"bool"])
}

def debugOff() {
    app.updateSetting("isDebug", false)
    log.info "${thisName}: Debug logging disabled."
    app.updateSetting("isDebug",[value:"false",type:"bool"])
}

def traceOff() {
    app.updateSetting("isTrace", false)
    log.trace "${thisName}: Trace logging disabled."
    app.updateSetting("isTrace",[value:"false",type:"bool"])
}

def disableInfoIn30() {
    if (isInfo == true) {
        runIn(1800, infoOff)
        log.info "Info logging disabling in 30 minutes."
    }
}

def disableDebugIn30() {
    if (isDebug == true) {
        runIn(1800, debugOff)
        log.debug "Debug logging disabling in 30 minutes."
    }
}

def disableTraceIn30() {
    if (isTrace == true) {
        runIn(1800, traceOff)
        log.trace "Trace logging disabling in 30 minutes."
    }
}

def ifWarn(msg) {
    log.warn "${thisName}: ${msg}"
}

def ifInfo(msg) {       
    def logL = 0
    if (ifLevel) logL = ifLevel.toInteger()
    if (logL == 1 && isInfo == false) {return}//bail
    else if (logL > 0) {
		log.info "${thisName}: ${msg}"
	}
}

def ifDebug(msg) {
    def logL = 0
    if (ifLevel) logL = ifLevel.toInteger()
    if (logL < 2 && isDebug == false) {return}//bail
    else if (logL > 1) {
		log.debug "${thisName}: ${msg}"
    }
}

def ifTrace(msg) {       
    def logL = 0
    if (ifLevel) logL = ifLevel.toInteger()
    if (logL < 3 && isTrace == false) {return}//bail
    else if (logL > 2) {
		log.trace "${thisName}: ${msg}"
    }
}
