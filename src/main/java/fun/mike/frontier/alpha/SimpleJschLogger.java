package fun.mike.frontier.alpha;

import java.util.HashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleJschLogger implements com.jcraft.jsch.Logger {
    private static Logger log = LoggerFactory.getLogger(SimpleJschLogger.class);

    private HashMap<Integer, Consumer<String>> logMap = new HashMap<>();
    private HashMap<Integer, BooleanSupplier> enabledMap = new HashMap<>();

    {
        logMap.put(com.jcraft.jsch.Logger.DEBUG, log::debug);
        logMap.put(com.jcraft.jsch.Logger.ERROR, log::error);
        logMap.put(com.jcraft.jsch.Logger.FATAL, log::error);
        logMap.put(com.jcraft.jsch.Logger.INFO, log::info);
        logMap.put(com.jcraft.jsch.Logger.WARN, log::warn);

        enabledMap.put(com.jcraft.jsch.Logger.DEBUG, log::isDebugEnabled);
        enabledMap.put(com.jcraft.jsch.Logger.ERROR, log::isErrorEnabled);
        enabledMap.put(com.jcraft.jsch.Logger.FATAL, log::isErrorEnabled);
        enabledMap.put(com.jcraft.jsch.Logger.INFO, log::isInfoEnabled);
        enabledMap.put(com.jcraft.jsch.Logger.WARN, log::isWarnEnabled);
    }

    @Override
    public void log(int level, String message) {
        logMap.get(level).accept(message);
    }

    @Override
    public boolean isEnabled(int level) {
        return enabledMap.get(level).getAsBoolean();
    }
}