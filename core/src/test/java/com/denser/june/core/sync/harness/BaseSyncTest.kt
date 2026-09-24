package com.denser.june.core.sync.harness

import org.junit.After
import org.junit.Before

abstract class BaseSyncTest {
    protected val harness = SyncTestHarness()

    @Before
    fun setUp() {
        harness.setUp()
    }

    @After
    fun tearDown() {
        harness.tearDown()
    }
}
