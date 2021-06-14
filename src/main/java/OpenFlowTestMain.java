import connect.network.xhttp.XMultiplexCacheManger;
import flow.rtsp.RtspOpenFlow;
import task.executor.TaskExecutorPoolManager;
import util.AnalysisConfig;

import java.io.File;

public class OpenFlowTestMain {

    private static final String CURRENT_COMMAND = "OpenFlowTestMain";
    private static final String KEY_USER_DIR = "user.dir";
    private static final String KEY_COMMAND = "sun.java.command";
    private static final String FILE_CONFIG = "of_config.cfg";

    private static String IDE_URL = "src" + File.separator + "main" + File.separator + "resources" + File.separator;

    public static void main(String[] args) {
        String configPath = initEnv(FILE_CONFIG);
        AnalysisConfig.getInstance().analysis(configPath);
        RtspOpenFlow.getInstance().startFlow();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            RtspOpenFlow.getInstance().stopFlow();
            TaskExecutorPoolManager.getInstance().destroyAll();
            XMultiplexCacheManger.destroy();
        }));
    }

    private static String initEnv(String configFile) {
        String currentWorkDir = System.getProperty(KEY_USER_DIR) + File.separator;
        String currentCommand = System.getProperty(KEY_COMMAND);
        String filePath;
        if (CURRENT_COMMAND.equals(currentCommand)) {
            //ide model，not create file
            filePath = currentWorkDir + IDE_URL + configFile;
        } else {
            filePath = currentWorkDir + configFile;
        }
        return filePath;
    }
}
