package org.hwbot.prime.api;

public interface VersionStatusAware {

    /**
     * A newer version is available
     * 
     * @param newVersion
     * @param url
     *            place where new version can be downloaded
     * @param required
     *            upgrade is required in order to use all functionality
     */
    public void showNewVersionPopup(String newVersion, String url, boolean required);

}
