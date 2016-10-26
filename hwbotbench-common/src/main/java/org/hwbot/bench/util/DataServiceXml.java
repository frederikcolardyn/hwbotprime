package org.hwbot.bench.util;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.codec.binary.Base64;
import org.hwbot.bench.model.Hardware;
import org.hwbot.bench.model.Request;
import org.hwbot.bench.model.Response;
import org.hwbot.bench.security.EncryptionModule;

public class DataServiceXml {

    public static String createScreenshot() {
        try {
            BufferedImage image = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", byteArrayOutputStream);
            return Base64.encodeBase64String(byteArrayOutputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String createXml(String client, String version, Hardware hardware, String scorePoints, boolean addScreenshot,
            EncryptionModule encryptionModule,Integer applicationId) {
        Request request = new Request(client, version, scorePoints, hardware);
        request.setApplicationId(applicationId);

        if (addScreenshot) {
            request.addScreenshot(createScreenshot());
        }

        if (encryptionModule != null) {
            encryptionModule.addChecksum(request);
        }

        JAXBContext jc;
        try {
            jc = JAXBContext.newInstance(Request.class);
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            m.marshal(request, byteArrayOutputStream);
            return new String(byteArrayOutputStream.toByteArray(), "utf8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Response parseResponse(String xml) {
        try {
            return (Response) JAXBContext.newInstance(Response.class).createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes()));
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to parse response from HWBOT!", e);
        }
    }

}
