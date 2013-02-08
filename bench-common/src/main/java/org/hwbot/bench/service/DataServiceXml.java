package org.hwbot.bench.service;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DataServiceXml {

	public static String createXml(String client, String version, String processorModel, Float processorSpeed, long scorePoints) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();

			Element root = doc.createElement("submission");

			// application part
			Element application = doc.createElement("application");
			Element appName = doc.createElement("name");
			appName.setTextContent(client);
			Element appVersion = doc.createElement("version");
			appVersion.setTextContent(version);
			application.appendChild(appName);
			application.appendChild(appVersion);

			// score part
			Element score = doc.createElement("score");
			Element points = doc.createElement("points");
			points.setTextContent(String.valueOf((scorePoints / 1000f)));
			score.appendChild(points);

			// hardware part
			Element hardware = doc.createElement("hardware");
			Element processor = doc.createElement("processor");
			Element processorName = doc.createElement("name");
			Element processorClock = doc.createElement("coreClock");
			processorName.setTextContent(processorModel);
			processorClock.setTextContent(String.valueOf((processorSpeed * 1000)));
			processor.appendChild(processorName);
			processor.appendChild(processorClock);
			hardware.appendChild(processor);

			// software part
			Element software = doc.createElement("software");
			Element os = doc.createElement("os");
			Element osFamily = doc.createElement("family");
			osFamily.setTextContent(HardwareService.OS);
			os.appendChild(osFamily);
			software.appendChild(os);

			// metadata part
			Element metadata = doc.createElement("metadata");
			StringBuffer buffer = new StringBuffer();
			Map<String, String> env = System.getenv();
			for (String envName : env.keySet()) {
				buffer.append(String.format("%s=%s%n\n", envName, env.get(envName)));

			}
			metadata.setAttribute("name", "java_environment");
			metadata.setTextContent(buffer.toString());

			// add to root
			root.appendChild(application);
			root.appendChild(score);
			root.appendChild(hardware);
			root.appendChild(software);
			root.appendChild(metadata);

			doc.appendChild(root);

			// Save the Created XML on Local Disc using Transformation APIs as Discussed
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();

			Source s = new DOMSource(doc);
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			Result res = new StreamResult(byteArrayOutputStream);
			transformer.transform(s, res);
			System.out.println("XML File Created Succesfully");
			return new String(byteArrayOutputStream.toByteArray(), "utf8");
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (TransformerConfigurationException e) {
			throw new RuntimeException(e);
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
