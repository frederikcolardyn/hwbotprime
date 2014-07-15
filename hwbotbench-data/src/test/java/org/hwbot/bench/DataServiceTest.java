package org.hwbot.bench;

import org.hwbot.bench.model.Hardware;
import org.hwbot.bench.model.Memory;
import org.hwbot.bench.model.Processor;
import org.hwbot.bench.model.Response;
import org.hwbot.bench.util.DataServiceXml;
import org.junit.Assert;
import org.junit.Test;

public class DataServiceTest {

    @Test
    public void testCreateXml() {

        Hardware hardware = new Hardware();

        Processor processor = new Processor();
        processor.setName("Intel i920");
        processor.setCoreClock(2900f);

        Memory memory = new Memory();
        memory.setTotalSize(1024);

        hardware.setProcessor(processor);
        hardware.setMemory(memory);

        String xml = DataServiceXml.createXml("primebench", "1.0.0", hardware, "1234", false, null);

        Assert.assertTrue("xml not as expected: " + xml, xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + "<submission>\n"
                + "    <application>\n" + "        <name>primebench</name>\n" + "        <version>1.0.0</version>\n" + "    </application>\n" + "    <score>\n"
                + "        <points>1234</points>\n" + "    </score>\n" + "    <hardware>\n" + "        <processor>\n"
                + "            <coreClock>2900.0</coreClock>\n" + "            <name>Intel i920</name>\n" + "        </processor>\n" + "        <memory>\n"
                + "            <totalSize>1024</totalSize>\n" + "        </memory>\n" + "    </hardware>\n"));
    }

    @Test
    public void testParseXml() {
        Response response = DataServiceXml.parseResponse("<response><status>success</status></response>");

        System.out.println(response);
        Assert.assertEquals("success", response.getStatus());
    }

}
