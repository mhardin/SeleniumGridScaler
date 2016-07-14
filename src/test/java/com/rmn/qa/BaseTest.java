package com.rmn.qa;

import org.junit.After;

/**
 * Created by matthew on 5/26/16.
 */
public abstract class BaseTest {

	@After
	// Since AutomationContext is a shared singleton, make sure and clear it after every test
	public void afterTest() {
		AutomationContext.refreshContext();
	}
}
