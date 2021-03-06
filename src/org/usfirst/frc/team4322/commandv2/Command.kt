package org.usfirst.frc.team4322.commandv2

import edu.wpi.first.wpilibj.Timer
import kotlinx.coroutines.*
import org.usfirst.frc.team4322.logging.RobotLogger
import java.util.concurrent.TimeUnit

/**
 * The base unit of work in this library. Commands are periodic tasks which run until their [isFinished] method returns true.
 */
abstract class Command() {
    enum class InterruptBehavior {
        Suspend,
        Terminate
    }
    internal var parented = false
    private var periodMS: Double = .02
    protected var interruptBehavior = InterruptBehavior.Terminate
    private var timeout: Double = 0.0
    private var startTime: Double = 0.0
    internal var subsystem: Subsystem? = null
    internal var job: Deferred<Unit>? = null

    companion object {
        /**
         * Creates a command derived from the lambda passed in.
         */
        @JvmStatic
        fun lambda(fn: () -> Unit): Command {
            return object : Command() {
                override fun execute() {
                    fn()
                }

                override fun isFinished(): Boolean {
                    return true
                }
            }
        }

        /**
         * Creates a command derived from the lambda passed in, requiring the passed subsystem.
         */
        @JvmStatic
        fun lambda(subsystem: Subsystem, fn: () -> Unit): Command {

            return object : Command() {
                init {
                    require(subsystem)
                }
                override fun execute() {
                    fn()
                }

                override fun isFinished(): Boolean {
                    return true
                }
            }
        }

        @JvmStatic
        val empty: Command = lambda {}

        @JvmStatic
        fun waitFor(fn: () -> Boolean): Command {
            return object : Command() {
                override fun execute() {

                }

                override fun isFinished(): Boolean {
                    return fn()
                }
            }
        }

        @JvmStatic
        fun delay(seconds: Double): Command {

            return object : Command(seconds) {
                override fun isFinished() = false
            }
        }

    }

    /**
     * Create command with timeout. Timeouts are incompatible with commands that do not terminate on suspension, and will be ignored in this case.
     */
    constructor(timeout: Double) : this() {
        this.timeout = timeout
    }


    /**
     * Returns true if the command is currently scheduled for execution.
     */
    fun isRunning(): Boolean {
        return job?.isActive ?: false
    }

    /**
     * Cancels the command's execution if it is running.
     * @return true if the command was successfully cancelled or was not running, false if the command couldnt be cancelled.
     */
    fun cancel(): Boolean {
        if (isRunning()) {
            job?.cancel()
        }
        return true
    }

    fun start(): Deferred<Unit> {
        val ret = invoke()
        ret.start()
        return ret
    }

    /**
     * This method is called when the command finishes running.
     */
    open operator fun invoke(coroutineScope: CoroutineScope = GlobalScope): Deferred<Unit> {
        job = coroutineScope.async(start = CoroutineStart.LAZY) {
            try {
                /*******************/
                /**** INIT CODE ****/
                /*******************/
                subsystem?.commandStack?.push(job)
                Scheduler.commandsChanged = true
                startTime = Timer.getFPGATimestamp()
                initialize()
                /*******************/
                /**** LOOP CODE ****/
                /*******************/
                do {
                    val currentTop = subsystem?.commandStack?.peek()
                    if (currentTop != null && currentTop != job) {
                        if (interruptBehavior == InterruptBehavior.Terminate) {
                            this@Command.cancel()
                        } else if (interruptBehavior == InterruptBehavior.Suspend) {
                            interrupted()
                            currentTop.join()
                            resumed()
                            execute()
                        }
                    } else {
                        execute()
                    }
                    delay(TimeUnit.MILLISECONDS.toMillis((periodMS * 1000).toLong()))
                } while (!isFinished() && (timeout == 0.0 || startTime + timeout > Timer.getFPGATimestamp()))
            } catch (ex: Exception) {
                if (ex is CancellationException) {
                } else {
                    RobotLogger.exc("Exception in command main loop:", ex)
                }
            } finally {
                this@Command.end()
                subsystem?.commandStack?.remove(job)
                Scheduler.commandsChanged = true
                job = null
            }
        }
        return job!!
    }

    /**
     * This method is called when the command starts running.
     */
    protected open fun initialize() {

    }

    /**
     * This method is called when the command finishes running.
     */
    protected open fun end() {

    }

    /**
     * This method is called when another command acquires the subsystem associated with this command
     * to allow for pre-transition cleanup.
     */
    protected open fun interrupted() {

    }

    /**
     * If a command has it's [interruptBehavior] set to [InterruptBehavior.Suspend],
     * this method is called when the command is brought back to the foreground.
     */
    protected open fun resumed() {

    }

    /**
     * Main loop method for command. Runs every [periodMS] milliseconds while [isFinished] is false.
     */
    protected open fun execute() {

    }

    /**
     * Returns true when the Command is ready to terminate.
     */
    protected abstract fun isFinished(): Boolean

    /**
     * Sets the [Subsystem] associated with a Command. Commands may only require 1 subsystem.
     * @param [s] Subsystem to be associated with this Command.
     */
    protected fun require(s: Subsystem) {
        subsystem = s
    }

}