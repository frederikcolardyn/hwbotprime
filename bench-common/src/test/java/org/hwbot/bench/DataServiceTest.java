package org.hwbot.bench;

import org.hwbot.bench.model.Response;
import org.hwbot.bench.service.DataServiceXml;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class DataServiceTest {

    @Test
    @Ignore("fix metadata")
    public void testCreateXml() {
        String xml = DataServiceXml.createXml("primebench", "1.0.0", "Intel i920", 2900f, 1024, 1234l, false, null);

        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + "<submission>\n" + "    <application>\n"
                + "        <name>primebench</name>\n" + "        <version>1.0.0</version>\n" + "    </application>\n" + "    <score>\n"
                + "        <points>1.234</points>\n" + "    </score>\n" + "    <hardware>\n" + "        <memory>\n"
                + "            <totalSize>1024</totalSize>\n" + "        </memory>\n" + "        <processor>\n" + "            <coreClock>2900.0</coreClock>\n"
                + "            <name>Intel i920</name>\n" + "        </processor>\n" + "    </hardware>\n" + "    <metadata name=\"java\">\n" + "    .?\n"
                + "    </metadata>\n" + "</submission>\n" + "", xml);
    }

    @Test
    public void testParseXml() {
        Response response = DataServiceXml.parseResponse("<response><status>success</status></response>");

        System.out.println(response);
        Assert.assertEquals("success", response.getStatus());
    }

}
