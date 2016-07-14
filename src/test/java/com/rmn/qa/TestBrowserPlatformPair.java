package com.rmn.qa;

import org.junit.Test;
import org.openqa.selenium.Platform;

import junit.framework.Assert;

/**
 * Created by matthew on 5/26/16.
 */
public class TestBrowserPlatformPair extends BaseTest {

	@Test
	public void getGet() {
		BrowserPlatformPair one = new BrowserPlatformPair("chrome", Platform.LINUX);
		Assert.assertEquals("Browsers should be equal", "chrome", one.getBrowser());
		Assert.assertEquals("Platforms should be equal", Platform.LINUX, one.getPlatform());
	}

	@Test
	public void testEquals() {
		BrowserPlatformPair one = new BrowserPlatformPair("chrome", Platform.LINUX);
		BrowserPlatformPair two = new BrowserPlatformPair("chrome", Platform.LINUX);
		Assert.assertTrue("Objects should be equal", one.equals(two));
	}

	@Test
	public void testNotEqualsPlatform() {
		BrowserPlatformPair one = new BrowserPlatformPair("chrome", Platform.LINUX);
		BrowserPlatformPair two = new BrowserPlatformPair("chrome", Platform.UNIX);
		Assert.assertFalse("Objects should not be equal due to non-matching platform", one.equals(two));
	}

	@Test
	public void testNotEqualsPlatformDifferentFamily() {
		BrowserPlatformPair one = new BrowserPlatformPair("chrome", Platform.UNIX);
		BrowserPlatformPair two = new BrowserPlatformPair("chrome", Platform.WINDOWS);
		Assert.assertFalse("Objects should not be equal due to non-matching platform", one.equals(two));
	}

	@Test
	public void testNotEqualsBrowser() {
		BrowserPlatformPair one = new BrowserPlatformPair("chrome", Platform.LINUX);
		BrowserPlatformPair two = new BrowserPlatformPair("firefox", Platform.LINUX);
		Assert.assertFalse("Objects should not be equal due to non-matching platform", one.equals(two));
	}

}
