/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.util.concurrent.CountDownLatch;

import m1.Program;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-12-13 nsano initial version <br>
 */
public class TestCase {

    Runnable runnable = () -> {};

    Runnable[] runnables = new Runnable[] {runnable};

    @Test
    @DisplayName("compare method reference")
    void test1() throws Exception {
        // we cannot compare like `if (runnables[0] == this::runnable) ...`
Debug.println(runnable.hashCode());
        assertEquals(runnables[0], runnable);
        assertEquals(runnables[0].hashCode(), runnable.hashCode());
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test0() throws Exception {
        CountDownLatch cdl = new CountDownLatch(1);
        Program.main(new String[0]);
        cdl.await();
    }
}
