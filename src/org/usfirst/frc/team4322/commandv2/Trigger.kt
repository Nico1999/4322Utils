package org.usfirst.frc.team4322.commandv2

abstract class Trigger {


    companion object {
        val triggers = mutableListOf<Trigger>()
        var enabled = true

        @JvmStatic
        fun updateTriggers() {
            if (enabled)
                triggers.forEach { it.poll() }
        }
    }

    init {
        triggers.add(this)
    }

    private var prevState: Boolean = false
    private var pressCmd: Command? = null
    private var releaseCmd: Command? = null
    private var holdCmd: Command? = null
    private var cancelCmd: Command? = null
    private var toggleCmd: Command? = null
    private var holdStarted: Boolean = false
    private var toggleState: Boolean = false


    operator fun invoke(): Boolean {
        return get()
    }

    abstract fun get(): Boolean

    fun whenPressed(c: Command) {
        pressCmd = c
    }


    fun whileHeld(c: Command) {
        holdCmd = c
    }

    fun whenReleased(c: Command) {
        releaseCmd = c
    }

    fun cancelWhenPressed(c: Command) {
        cancelCmd = c
    }

    fun toggleWhenPressed(c: Command) {
        toggleCmd = c
    }

    fun poll() {
        var cur = get()
        if (cur && prevState) {
            if (!holdStarted) {
                holdStarted = true
                holdCmd?.start()
            } else {
                if (holdCmd?.isRunning() != true) {
                    holdCmd?.start()
                }
            }
        } else if (cur && !prevState) {
            pressCmd?.start()
            if (toggleState) {
                toggleCmd?.cancel()
            } else {
                toggleCmd?.start()
            }
            toggleState = !toggleState
            cancelCmd?.cancel()
        } else if (!cur && prevState) {
            holdStarted = false
            holdCmd?.cancel()
            releaseCmd?.start()
        }
        prevState = cur
    }
}