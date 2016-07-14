package com.rmn.qa;

import org.openqa.selenium.Platform;

/**
 * Created by jchan on 5/16/16.
 */
public class BrowserPlatformPair {

    private String browser;
    private Platform platform;

    public BrowserPlatformPair(String browser, Platform platform){
        this.browser = browser;
        this.platform = platform;
    }
    public void setBrowser(String browser){
        this.browser = browser;
    }
    public void setPlatform(Platform platform){
        this.platform = platform;
    }
    public String getBrowser(){
        return this.browser;
    }
    public Platform getPlatform(){
        return this.platform;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BrowserPlatformPair that = (BrowserPlatformPair) o;

        if (browser != null ? !browser.equals(that.browser) : that.browser != null) {
            return false;
        }
        return platform == that.platform;

    }

    @Override
    public int hashCode() {
        int result = browser != null ? browser.hashCode() : 0;
        result = 31 * result + (platform != null ? platform.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BrowserPlatformPair{" +
                "browser='" + browser + '\'' +
                ", platform=" + platform +
                '}';
    }
}
