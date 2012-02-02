package std.concurrent

import std.concurrent.*
import std.util.*

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import java.util.Date

/*
Message handling class which process only one message at any given moment

Three main ways to use it
- post and forget
- post and await result
- post and assign callback when message processed
*/
abstract class Actor(protected val executor: Executor) {
    // we can not do it private or protected
    // otherwise updater defined in class object will not be able to access it
    volatile var queue : FunctionalQueue<Any> = emptyQueue

    /*
    Handle message and return result
    This method guaranteed to be executed only one per object at any given time
    */
    protected abstract fun onMessage(message: Any) : Any?

    /*
    Post message to the actor.
    The method returns immediately and the message itself will be processed later
    */
    fun post(message: Any) {
        while(true) {
            val q = queue
            if(q.empty) {
                if(queueUpdater.compareAndSet(this, q, busyEmptyQueue)) {
                    executor.execute { process(message) }
                    break
                }
            }
            else {
                val newQueue = (if(q == busyEmptyQueue) emptyQueue else q) add message
                if(queueUpdater.compareAndSet(this, q, newQueue)) {
                    break
                }
            }
        }
    }

    /*
    Post message to the actor and schedule callback to be executed on given executor when message processed
    */
    fun post(message: Any, executor: Executor = this.executor, callback: (Any?)->Unit) =
    post(Callback(message, executor, callback))

    /*
    Send message to the actor and await result
    */
    fun send(message: Any) : Any? {
        val request = Request(message)
        post(request)
        request.await()
        return request.result
    }

    private fun nextMessage() {
        while(true) {
            val q = queue
            if(q == busyEmptyQueue) {
                if(queueUpdater.compareAndSet(this, busyEmptyQueue, emptyQueue)) {
                    break;
                }
            }
            else {
                val removed = q.removeFirst()
                val newQueue = if(removed._2.empty) busyEmptyQueue else removed._2
                if(queueUpdater.compareAndSet(this, q, newQueue)) {
                    executor.execute { process(removed._1) }
                    break;
                }
            }
        }
    }

    private fun process(message: Any) {
        when(message) {
            is Request -> {
                message.result = onMessage(message.message)
                message.countDown()
            }
            is Callback -> {
                val result = onMessage(message.message)
                message.executor execute {
                    val callback = message.callback;
                    callback(result)
                }
            }
            else -> onMessage(message)
        }

        nextMessage()
    }

    /*
    Create actor on the same executor
    */
    fun actor(handler: (Any)->Any?) = executor.actor(handler)

    class object {
        val queueUpdater = AtomicReferenceFieldUpdater.newUpdater(typeinfo<Actor>.javaClassForType,typeinfo<FunctionalQueue<Any>>().javaClassForType, "queue").sure()
        val emptyQueue = FunctionalQueue<Any>()
        val busyEmptyQueue = FunctionalQueue<Any>() add "busy empty queue"

        class Request(val message: Any) : java.util.concurrent.CountDownLatch(1) {
            var result: Any? = null
        }

        class Callback(val message: Any, val executor: Executor, val callback: (Any?) -> Unit)
    }
}

fun Executor.actor(handler: (Any)->Any?) : Actor = object: Actor(this) {
    override fun onMessage(message: Any) {
        handler(message)
    }
}

fun singleThreadActor(handler: (Any)->Any?) : Actor = Executors.newSingleThreadExecutor().sure().actor(handler)

