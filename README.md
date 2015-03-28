# Jafra
Implementation of Safra's algorithm [[pdf]](http://fmt.cs.utwente.nl/courses/cdp/slides/cdp-8-mpi-2-4up.pdf) in reusable manner for efficient termination detection in distributed Java applications.

Assumes fixed number of ordered processes communicating over realiable, order preserving channels. (MPI, Ethernet, shared memory, blocking queues...)

## How to use
 - Extend [IdTokenMessenger](https://github.com/daemontus/jafra/blob/master/jafra/src/main/java/com/daemontus/jafra/IdTokenMessenger.java) or directly implement [TokenMessenger](https://github.com/daemontus/jafra/blob/master/jafra/src/main/java/com/daemontus/jafra/TokenMessenger.java).
 - Use [static factory](https://github.com/daemontus/jafra/blob/master/jafra/src/main/java/com/daemontus/jafra/Terminator.java#L36) to create a terminator in each process.
 - Each time a message is received or sent from process, notify terminator. (These are work-initiating/load-balancing messages. Not every data packet is considered to be a message, see safra's algorithm for more info)
 - When all work is done, call [setDone](https://github.com/daemontus/jafra/blob/master/jafra/src/main/java/com/daemontus/jafra/Terminator.java#L72) on terminator to mark process as idle.
 - Use blocking [waitForTermination](https://github.com/daemontus/jafra/blob/master/jafra/src/main/java/com/daemontus/jafra/Terminator.java#L104) call to actually wait for termination. When this method returns, all processes are guaranteed to be idle (but some of them may be still waiting for this method to return).
 - After waitForTermination returns, the terminator is marked as finished and should not be used again. Create new terminator instead. This is mainly to prevent reuse related bugs in multi-round algorithms.
 
### Future work
 - provide a non blocking variant to waitForTermination, preferably using Observable pattern
 - privde a MPJ token messanger example
