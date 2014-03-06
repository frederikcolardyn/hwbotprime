package org.hwbot.prime.ui;

import org.hwbot.prime.service.BenchmarkStatusAware;

public interface BenchUI extends BenchmarkStatusAware {

	void waitForCommands();

}
