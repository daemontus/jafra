[![Release](https://jitpack.io/v/daemontus/jafra.svg)](https://jitpack.io/#daemontus/jafra)
[![Build Status](https://travis-ci.org/daemontus/jafra.svg?branch=master)](https://travis-ci.org/daemontus/jafra)
[![codecov.io](https://codecov.io/github/daemontus/jafra/coverage.svg?branch=master)](https://codecov.io/github/daemontus/jafra?branch=master)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat)](https://github.com/daemontus/jafra/blob/master/LICENSE.txt)
[![Kotlin](https://img.shields.io/badge/kotlin-1.0.0--rc--1036-blue.svg)](http://kotlinlang.org)

# Jafra
Implementation of Safra's algorithm [[pdf]](http://fmt.cs.utwente.nl/courses/cdp/slides/cdp-8-mpi-2-4up.pdf) in reusable manner for efficient termination detection in distributed Java applications.

Assumes fixed number of ordered processes communicating over realiable, order preserving channels. (MPI, Ethernet, shared memory, blocking queues...)

###How to get it
The repo is jitpack-compatibile, so all you have to do is look up the latest version on jitpack and then integrate it into your favorite build system: [Jafra on Jitpack](https://jitpack.io/#daemontus/jafra)

## How to use
 - Extend [IdTokenMessenger](https://github.com/daemontus/jafra/blob/master/src/main/kotlin/com/github/daemontus/jafra/TokenMessenger.kt) or directly implement [TokenMessenger](https://github.com/daemontus/jafra/blob/master/src/main/kotlin/com/github/daemontus/jafra/TokenMessenger.kt).
 - Use [Factory or static constructor](https://github.com/daemontus/jafra/blob/master/src/main/kotlin/com/github/daemontus/jafra/Terminator.kt#L90) to create a terminator in each process.
 - Each time a message is received or sent from process, notify terminator. (These are work-initiating/load-balancing messages. Not every data packet is considered to be a message, see safra's algorithm for more info)
 - When all work is done, call [setDone](https://github.com/daemontus/jafra/blob/master/src/main/kotlin/com/github/daemontus/jafra/Terminator.kt#L35) on terminator to mark process as idle.
 - Use blocking [waitForTermination](https://github.com/daemontus/jafra/blob/master/src/main/kotlin/com/github/daemontus/jafra/Terminator.kt#L68) call to actually wait for termination. When this method returns, all processes are guaranteed to be idle (but some of them may be still waiting for this method to return).
 - After waitForTermination returns, the terminator is marked as finished and should not be used again. Create new terminator instead. This is mainly to prevent reuse related bugs in multi-round algorithms.
 - For testing, you can use a [shared memory token messenger](https://github.com/daemontus/jafra/blob/master/src/main/kotlin/com/github/daemontus/jafra/SharedMemoryMessenger.kt#L19) based on blocking queues

### Future work
 - provide a non blocking variant to waitForTermination, preferably using Observable pattern
 - privde a MPJ token messanger example
