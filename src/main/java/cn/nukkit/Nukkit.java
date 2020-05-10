package cn.nukkit;

import cn.nukkit.network.protocol.ProtocolInfo;
import cn.nukkit.utils.ServerKiller;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

/*
 * `_   _       _    _    _ _
 * | \ | |     | |  | |  (_) |
 * |  \| |_   _| | _| | ___| |_
 * | . ` | | | | |/ / |/ / | __|
 * | |\  | |_| |   <|   <| | |_
 * |_| \_|\__,_|_|\_\_|\_\_|\__|
 */

/**
 * Nukkit启动类，包含{@code main}函数。<br>
 * The launcher class of Nukkit, including the {@code main} function.
 *
 * @author MagicDroidX(code) @ Nukkit Project
 * @author 粉鞋大妈(javadoc) @ Nukkit Project
 * @since Nukkit 1.0 | Nukkit API 1.0.0
 */
@Log4j2
public class Nukkit {

    public static final Properties GIT_INFO = Nukkit.getGitInfo();

    public static final String VERSION = Nukkit.getVersion();

    public static final String API_VERSION = "1.0.9";

    public static final String CODENAME = "";

    @Deprecated
    public static final String MINECRAFT_VERSION = ProtocolInfo.MINECRAFT_VERSION;

    @Deprecated
    public static final String MINECRAFT_VERSION_NETWORK = ProtocolInfo.MINECRAFT_VERSION_NETWORK;

    public static final String PATH = System.getProperty("user.dir") + "/";

    public static final String DATA_PATH = System.getProperty("user.dir") + "/";

    public static final String PLUGIN_PATH = Nukkit.DATA_PATH + "plugins";

    public static final long START_TIME = System.currentTimeMillis();

    public static boolean ANSI = true;

    public static boolean TITLE = false;

    public static boolean shortTitle = Nukkit.requiresShortTitle();

    public static int DEBUG = 1;

    public static void main(final String[] args) {
        // Force IPv4 since Nukkit is not compatible with IPv6
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("log4j.skipJansi", "false");

        // Force Mapped ByteBuffers for LevelDB till fixed.
        System.setProperty("leveldb.mmap", "true");

        // Define args
        final OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        final OptionSpec<Void> helpSpec = parser.accepts("help", "Shows this page").forHelp();
        final OptionSpec<Void> ansiSpec = parser.accepts("disable-ansi", "Disables console coloring");
        final OptionSpec<Void> titleSpec = parser.accepts("enable-title", "Enables title at the top of the window");
        final OptionSpec<String> vSpec = parser.accepts("v", "Set verbosity of logging").withRequiredArg().ofType(String.class);
        final OptionSpec<String> verbositySpec = parser.accepts("verbosity", "Set verbosity of logging").withRequiredArg().ofType(String.class);
        final OptionSpec<String> languageSpec = parser.accepts("language", "Set a predefined language").withOptionalArg().ofType(String.class);

        // Parse arguments
        final OptionSet options = parser.parse(args);

        if (options.has(helpSpec)) {
            try {
                // Display help page
                parser.printHelpOn(System.out);
            } catch (final IOException e) {
                // ignore
            }
            return;
        }

        Nukkit.ANSI = !options.has(ansiSpec);
        Nukkit.TITLE = options.has(titleSpec);

        String verbosity = options.valueOf(vSpec);
        if (verbosity == null) {
            verbosity = options.valueOf(verbositySpec);
        }
        if (verbosity != null) {

            try {
                final Level level = Level.valueOf(verbosity);
                Nukkit.setLogLevel(level);
            } catch (final Exception e) {
                // ignore
            }
        }

        final String language = options.valueOf(languageSpec);

        try {
            if (Nukkit.TITLE) {
                System.out.print((char) 0x1b + "]0;Nukkit is starting up..." + (char) 0x07);
            }
            new Server(Nukkit.PATH, Nukkit.DATA_PATH, Nukkit.PLUGIN_PATH, language);
        } catch (final Throwable t) {
            Nukkit.log.throwing(t);
        }

        if (Nukkit.TITLE) {
            System.out.print((char) 0x1b + "]0;Stopping Server..." + (char) 0x07);
        }
        Nukkit.log.info("Stopping other threads");

        for (final Thread thread : Thread.getAllStackTraces().keySet()) {
            if (!(thread instanceof InterruptibleThread)) {
                continue;
            }
            Nukkit.log.debug("Stopping {} thread", thread.getClass().getSimpleName());
            if (thread.isAlive()) {
                thread.interrupt();
            }
        }

        final ServerKiller killer = new ServerKiller(8);
        killer.start();

        if (Nukkit.TITLE) {
            System.out.print((char) 0x1b + "]0;Server Stopped" + (char) 0x07);
        }
        System.exit(0);
    }

    public static Level getLogLevel() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration log4jConfig = ctx.getConfiguration();
        final LoggerConfig loggerConfig = log4jConfig.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        return loggerConfig.getLevel();
    }

    public static void setLogLevel(final Level level) {
        Preconditions.checkNotNull(level, "level");
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration log4jConfig = ctx.getConfiguration();
        final LoggerConfig loggerConfig = log4jConfig.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(level);
        ctx.updateLoggers();
    }

    private static boolean requiresShortTitle() {
        //Shorter title for windows 8/2012
        final String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("windows") && (osName.contains("windows 8") || osName.contains("2012"));
    }

    private static Properties getGitInfo() {
        final InputStream gitFileStream = Nukkit.class.getClassLoader().getResourceAsStream("git.properties");
        if (gitFileStream == null) {
            return null;
        }
        final Properties properties = new Properties();
        try {
            properties.load(gitFileStream);
        } catch (final IOException e) {
            return null;
        }
        return properties;
    }

    private static String getVersion() {
        final StringBuilder version = new StringBuilder();
        version.append("git-");
        final String commitId;
        if (Nukkit.GIT_INFO == null || (commitId = Nukkit.GIT_INFO.getProperty("git.commit.id.abbrev")) == null) {
            return version.append("null").toString();
        }
        return version.append(commitId).toString();
    }

}
