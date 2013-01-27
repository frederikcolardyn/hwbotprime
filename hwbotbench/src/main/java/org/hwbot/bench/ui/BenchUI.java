package org.hwbot.bench.ui;

import org.hwbot.bench.service.BenchmarkStatusAware;

public interface BenchUI extends BenchmarkStatusAware {

	void waitForCommands();

}
