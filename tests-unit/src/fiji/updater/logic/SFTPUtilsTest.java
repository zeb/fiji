/*
 * Copyright (c) 2000-$today.year Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek at ieee.org
 *
 */
package fiji.updater.logic;

import org.junit.After;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import pg.vision.log4j.Log4JConfigurator;


/**
 * @author Jarek Sacha
 * @since 4/21/11 2:16 PM
 */
public final class SFTPUtilsTest {

    /**
     * The fixture set up called before every test method.
     */
    @Before
    public void setUp() {
        // Configure default appenders if non created yet
        Log4JConfigurator.initialize();
    }


    /**
     * The fixture clean up called after every test method.
     */
    @After
    public void tearDown() {
    }


    @Test
    public void testSomething() throws Exception {
        assertEquals(1, 1);
    }
}