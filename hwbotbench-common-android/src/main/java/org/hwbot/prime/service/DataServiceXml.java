package org.hwbot.prime.service;

import java.util.Locale;
import java.util.Map;

import org.hwbot.api.bench.dto.DeviceInfoDTO;
import org.hwbot.api.bench.dto.PersistentLoginDTO;
import org.hwbot.bench.model.Request;
import org.hwbot.bench.prime.Log;
import org.hwbot.bench.security.EncryptionModule;

import android.os.Build;

/**
 * Very dumb xml implementation in order to avoid any dependencies. JDK libraries are even avoided so it can be ported easier to iOS with RoboVM.
 * 
 * @author frederik
 * 
 */
public class DataServiceXml {

    private static final String xml10pattern = "[^" + "\u0009\r\n" + "\u0020-\uD7FF" + "\uE000-\uFFFD" + "\ud800\udc00-\udbff\udfff" + "]";

    private static DataServiceXml service;

    private DataServiceXml() {
    }

    public static DataServiceXml getInstance() {
        if (service == null) {
            service = new DataServiceXml();
        }
        return service;
    }

    public static String createXml(String version, Number score, Integer stageId, DeviceInfoDTO deviceInfo, PersistentLoginDTO credentials, EncryptionModule encryptionModule) {

        AndroidHardwareService hardwareService = AndroidHardwareService.getInstance();

        StringBuffer xml = new StringBuffer();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        xml.append("<submission xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://hwbot.org/submit/api\">");
        xml.append("<application>");
        xml.append("<name>HWBOT Prime</name>");
        xml.append("<version>" + version + "</version>");
        xml.append("</application>");
        if (credentials != null) {
            xml.append("<credentials>");
            xml.append("<token>" + credentials.getToken() + "</token>");
            xml.append("</credentials>");
        }
        xml.append("<score><points>" + score + "</points></score>");

        if(stageId != null){
            xml.append("<competitionStageId>" + stageId + "</competitionStageId>");
        }

        if (encryptionModule != null) {
            Request request = new Request("HWBOT Prime", version, String.format(Locale.ENGLISH, "%.2f", score.floatValue()), null);
            encryptionModule.addChecksum(request);
            String checksum = request.getApplicationChecksum();
            xml.append("<applicationChecksum>" + checksum + "</applicationChecksum>");
        }

        xml.append("<hardware>");
        xml.append("<device>");
        if (deviceInfo != null) {
            if (deviceInfo.getId() != null) {
                xml.append("<id>" + deviceInfo.getId() + "</id>");
            }
            if (deviceInfo.getName() != null) {
                xml.append("<name><![CDATA[" + deviceInfo.getName() + "]]></name>");
            }
        }
        xml.append("</device>");
        if (deviceInfo.getProcessor() != null) {
            xml.append("<processor>");
            if (deviceInfo.getProcessorId() != null) {
                xml.append("<id>" + deviceInfo.getProcessorId() + "</id>");
            }
            xml.append("<name><![CDATA[" + deviceInfo.getProcessor() + "]]></name>");
            if (hardwareService.getMaxRecordedProcessorSpeed() > 0) {
                xml.append("<coreClock>" + (hardwareService.getMaxRecordedProcessorSpeed()) + "</coreClock>");
            }
            if (hardwareService.getIdleTemperature() > 0 && hardwareService.getIdleTemperature() < Integer.MAX_VALUE) {
                xml.append("<idleTemp>" + (hardwareService.getIdleTemperature()) + "</idleTemp>");
            }
            if (hardwareService.getLoadTemperature() > 0 && hardwareService.getLoadTemperature() < Integer.MAX_VALUE) {
                xml.append("<loadTemp>" + (hardwareService.getLoadTemperature()) + "</loadTemp>");
            }

            xml.append("</processor>");
        }
        if (deviceInfo.getVideocard() != null) {
            xml.append("<videocard>");
            if (deviceInfo.getVideocard() != null) {
                xml.append("<id>" + deviceInfo.getVideocardId() + "</id>");
            }
            xml.append("<name><![CDATA[" + deviceInfo.getVideocard() + "]]></name>");
            xml.append("</videocard>");
        }
        xml.append("</hardware>");
        xml.append("<software>");
        xml.append("<os>");
        xml.append("<family>Android</family>");
        xml.append("<fullName>Android " + Build.VERSION.RELEASE + "</fullName>");
        xml.append("</os>");
        xml.append("</software>");

        xml.append("<metadata name=\"java_environment\"><![CDATA[");
        Map<String, String> env = System.getenv();
        for (String envName : env.keySet()) {
            xml.append(String.format("%s=%s%n\n", envName, env.get(envName)).replaceAll(xml10pattern, ""));

        }
        xml.append("]]></metadata>");

        xml.append("<metadata name=\"kernel\"><![CDATA[");
        xml.append(hardwareService.getKernel());
        xml.append("]]></metadata>");
        xml.append("<metadata name=\"os_build\">");
        xml.append(hardwareService.getOsBuild());
        xml.append("</metadata>");
        String osDebugInfo = hardwareService.getOsDebugInfo();
        if (osDebugInfo != null) {
            xml.append("<metadata name=\"os_dump\"><![CDATA[");
            xml.append(osDebugInfo.replaceAll(xml10pattern, ""));
            xml.append("]]></metadata>");
        }

        xml.append("</submission>");

        // Log.info(DataServiceXml.class.getSimpleName(), xml.toString());
        return xml.toString();
    }

}
