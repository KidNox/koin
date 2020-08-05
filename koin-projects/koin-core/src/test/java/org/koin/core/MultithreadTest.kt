package org.koin.core

import org.junit.Assert.*
import org.junit.Test
import org.koin.Simple
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.getInstanceFactory
import java.lang.RuntimeException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

const val MAX_TIME = 1000L

class MultithreadTest {

    @Test
    fun `multi thread get`() {
        val app = koinApplication {
            modules(
                module {
                    single { Simple.ComponentA() }
                    single { Simple.ComponentB(get()) }
                    single { Simple.ComponentC(get()) }
                })
        }

        val threads = arrayListOf<Thread>()
        threads.add(Thread(Runnable {
            randomSleep()
            app.koin.get<Simple.ComponentA>()
        }))
        threads.add(Thread(Runnable {
            randomSleep()
            app.koin.get<Simple.ComponentB>()
        }))
        threads.add(Thread(Runnable {
            randomSleep()
            app.koin.get<Simple.ComponentC>()
        }))

        threads.forEach { it.start() }

        val a = app.getInstanceFactory(Simple.ComponentA::class)!!
        val b = app.getInstanceFactory(Simple.ComponentA::class)!!
        val c = app.getInstanceFactory(Simple.ComponentA::class)!!

        while (!a.isCreated() && !b.isCreated() && !c.isCreated()) {
            Thread.sleep(100L)
        }

        assertTrue(a.isCreated())
        assertTrue(b.isCreated())
        assertTrue(c.isCreated())
        app.close()
    }

    private fun randomSleep() {
        val timer = Random.nextLong(MAX_TIME)
        println("thread sleep  $timer")
        Thread.sleep(timer)
    }

    @Test
    fun `multi thread singleton`() {
        val iteration = 512
        val executeCycles = 256
        val threads = 64
        val executor = Executors.newScheduledThreadPool(threads)

        repeat(iteration) {
            val factory = assertedSingleFactory()

            val app = koinApplication {
                modules(module { single { factory.invoke() } })
            }
            val errRef = AtomicReference<Exception>()

            executor.repeatExecute(executeCycles) {
                try {
                    app.koin.get<Simple.ComponentA>()
                } catch (ex: Exception) {
                    errRef.set(ex)
                }
            }
            app.close()

            errRef.get()?.cause?.printStackTrace()
            assertNull(errRef.get())
        }
    }

    private fun assertedSingleFactory(): () -> Simple.ComponentA {
        val instanceCreated = AtomicBoolean(false)
        return {
            if (instanceCreated.compareAndSet(false, true)) {
                Simple.ComponentA()
            } else {
                throw RuntimeException("can't create one more instance")
            }
        }
    }

    private fun Executor.repeatExecute(repeats: Int, block: () -> Unit) {
        val lock = Semaphore(0)
        repeat(repeats) {
            execute {
                try {
                    block()
                } finally {
                    lock.release()
                }
            }
        }
        lock.acquire(repeats)
    }
}