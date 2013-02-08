import org.apache.commons.lang.StringUtils;
import org.hwbot.bench.service.DataServiceXml;
import org.junit.Assert;
import org.junit.Test;

public class DataServiceTest {

	@Test
	public void testCreateXml() {

		String xml = DataServiceXml.createXml("primebench", "1.0.0", "Intel i920", 2900f, 1234l);

		System.out.println(xml);

		String metadata = StringUtils.substringBetween(xml, "<metadata", "</metadata>");
		System.out.println("metadata: " + metadata);
		xml = xml.replace(metadata, "");
		Assert.assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><submission><application><name>primebench</name><version>1.0.0</version></application><score><points>1.234</points></score><hardware><processor><name>Intel i920</name><coreClock>2900000.0</coreClock></processor></hardware><software><os><family>mac os x</family></os></software><metadata</metadata></submission>",
				xml);

	}

}
