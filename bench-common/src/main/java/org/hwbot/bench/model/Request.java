package org.hwbot.bench.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "submission")
@XmlType(propOrder = { "application", "score", "screenshot", "hardware" })
public class Request {

	private Application application;
	private Score score;
	private Screenshot screenshot;
	private Hardware hardware;

	public Request() {
		super();
	}

	public Request(String client, String version, String processorModel, Float processorSpeed, float points) {
		super();
		application = new Application();
		application.setName(client);
		application.setVersion(version);

		score = new Score();
		score.setPoints(points);

		hardware = new Hardware();
		Processor processor = new Processor();
		processor.setCoreClock(processorSpeed);
		processor.setName(processorModel);
		hardware.setProcessor(processor);
	}

	@XmlElement
	public Application getApplication() {
		return application;
	}

	public void setApplication(Application application) {
		this.application = application;
	}

	@XmlElement
	public Score getScore() {
		return score;
	}

	public void setScore(Score score) {
		this.score = score;
	}

	@XmlElement
	public Screenshot getScreenshot() {
		return screenshot;
	}

	public void setScreenshot(Screenshot screenshot) {
		this.screenshot = screenshot;
	}

	@XmlElement
	public Hardware getHardware() {
		return hardware;
	}

	public void setHardware(Hardware hardware) {
		this.hardware = hardware;
	}

	static class Application {
		String name;
		String version;

		@XmlElement
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@XmlElement
		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}
	}

	static class Score {
		float points;

		@XmlElement
		public float getPoints() {
			return points;
		}

		public void setPoints(float points) {
			this.points = points;
		}
	}

	static class Screenshot {
		String screenshot;

		@XmlElement
		public String getScreenshot() {
			return screenshot;
		}

		public void setScreenshot(String screenshot) {
			this.screenshot = screenshot;
		}

	}

	static class Hardware {

		Processor processor;

		@XmlElement
		public Processor getProcessor() {
			return processor;
		}

		public void setProcessor(Processor processor) {
			this.processor = processor;
		}

	}

	static class Processor {
		String name;
		Float coreClock;

		@XmlElement
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@XmlElement
		public Float getCoreClock() {
			return coreClock;
		}

		public void setCoreClock(Float coreClock) {
			this.coreClock = coreClock;
		}

	}

	public void addScreenshot(String base64) {
		screenshot = new Screenshot();
		screenshot.setScreenshot(base64);
	}

}
