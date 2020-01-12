package ru.somebank.embossing;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.somebank.embossing.clients.SftpClient;
import ru.somebank.embossing.utils.Utils;
import ru.somebank.embossing.utils.config.UserConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.zip.ZipOutputStream;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static Main app;
    public static String dateTime;
    private static boolean cleanPrimeSources;
    private static boolean downloadFilesFromPrime;
    private static boolean runPronitProcess;
    private static boolean runNovacardProcess;
    public static boolean cleanPronitOutputDir;
    public static boolean cleanNovacardOutputDir;

    public static void main(String[] args) throws Exception{

        // фиксируем дату и время запуска (для имени директории архива)
        dateTime = Utils.currentDateTime();

        String username = System.getProperty("username");
        String password = System.getProperty("password");
        String config = System.getProperty("config");


        downloadFilesFromPrime =  System.getProperty("downloadFilesFromPrime").equals("1"); // скачивать ли исходные файлы с Prime
        runPronitProcess =  System.getProperty("runPronitProcess").equals("1"); // запускать ли процесс Pronit
        runNovacardProcess =  System.getProperty("runNovacardProcess").equals("1"); //запускать ли процесс Novacard
        cleanPrimeSources  = System.getProperty("clean").equals("1");  // чистить ли исходные файлы на Prime
        cleanPronitOutputDir  = System.getProperty("cleanPronitOutputDir").equals("1");  // чистить Pronit Output
        cleanNovacardOutputDir  = System.getProperty("cleanNovacardOutputDir").equals("1");  // чистить Novacard Output


        if(username == null || password == null || config == null){
            throw new IllegalArgumentException("Run with -Dusername=<username> -Dpassword=<password> -Dconfig=<config> -DdownloadFilesFromPrime=<> -DrunPronitProcess=<> -DrunNovacardProcess=<> -DcleanPronitOutputDir=<> -DcleanNovacardOutputDir=<>");
        }

        //UserConfig ucfg = UserConfig.getInstance();


        log.info("App started with username: {} and password: ********", username);

        //Config cfg = Config.getInstance(); // загружаем конфиг

        app = new Main();
        //app.checkIfAlreadyRunning();

        if(downloadFilesFromPrime)
            app.downloadAndPrepare();

        if(runPronitProcess)
            (new Thread(new PronitProcess())).start();

        if(runNovacardProcess)
            (new Thread(new NovacardProcess())).start();

    }

    private void checkIfAlreadyRunning(){

        ServerSocket socket;
        try {
            socket = new ServerSocket( 62235);
            // not already running.
        } catch (Exception e) {
            // already running.
            log.error(e.getMessage());
            //log.error("Another instance of app is already running. Exiting...");
            System.exit(1);
        }

    }

    /**
     * Архивируем текущие и скачиваем новые файлы с Prime
     * @throws Exception
     */
    private void downloadAndPrepare() throws Exception{

        // архивируем текущее состояние директории
        archiveCurrentFiles();

        // перенос embossing c Prime
        transferEmbossingFromPrime();

        // архивируем embossing файлы и реестр
        archiveFiles();

        // конвертирует LF -> CR LF
        convertLineEndings();

        // сортировка файлов для Pronit и Novacard
        sortFiles();
    }

    /**
     * Перенос embossing файлов с Prime
     * @throws Exception
     */
    private void transferEmbossingFromPrime() throws Exception{

        log.info("********** Transferring files form AIX (Prime) **********");

        //Config cfg = Config.getInstance();

        UserConfig ucfg = UserConfig.getInstance();

        //String username = cfg.getUsername();
        //String password = Encryptor.getInstance().decrypt(cfg.getPassword());
        String username = System.getProperty("username");
        String password = System.getProperty("password");

        SftpClient client = new SftpClient(ucfg.primeSFTPHost(), ucfg.primeSFTPPort(), username, password);


        // чистим директорию embossing destination
        File destFolder = new File(ucfg.embossingDestinationPath());
        Utils.createDirIfNotExist(destFolder);
        Utils.cleanEmbossingFilesFromDirectory(destFolder);
        //FileUtils.cleanDirectory(destFolder);

        // чистим директорию embossing registry
        File registryFolder = new File(ucfg.embossingRegistryDestinationPath());
        Utils.createDirIfNotExist(registryFolder);
        FileUtils.cleanDirectory(registryFolder);

        //copy embossing
        client.copyEmbossingFiles(ucfg.embossingDestinationPath(), ucfg.primeEmbossingSourcesPath());
        if(cleanPrimeSources){
            client.cleanEmbossingFiles(ucfg.primeEmbossingSourcesPath());
        }

        //copy registry
        client.copyRegistryFiles(ucfg.embossingRegistryDestinationPath(), ucfg.primeEmbossingRegistrySourcesPath());
        if(cleanPrimeSources){
            client.cleanRegistryFiles(ucfg.primeEmbossingRegistrySourcesPath());
        }

        client.close();

    }

    private void archiveCurrentFiles() throws IOException {
        log.info("********** Archiving current files **********");
        //Config cfg = Config.getInstance();
        UserConfig ucfg = UserConfig.getInstance();
        File currentArchiveDir = new File(ucfg.archivePath() + File.separator + dateTime);
        Utils.createDirIfNotExist(currentArchiveDir);

        zipCurrentFiles(currentArchiveDir);

    }

    private void zipCurrentFiles(File currentArchiveDir) throws IOException{
        log.info("********** Zipping current files **********");
        //Config cfg = Config.getInstance();
        UserConfig ucfg = UserConfig.getInstance();
        FileOutputStream fos = new FileOutputStream(currentArchiveDir + File.separator + "current.zip");
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File srcDir = new File(ucfg.embossingDestinationPath());

        File[] filesToZip = srcDir.listFiles();
        //фильтруем вложенные директории
        Arrays.stream(filesToZip).filter(f->!f.isDirectory()).forEach(f-> {
            try{
                Utils.zipFile(f, zipOut);
            }catch (Exception e){
                log.error("Could not zip {}", f.getAbsolutePath());
            }

        });
        zipOut.close();
        fos.close();
    }

    private void archiveFiles() throws IOException{
        log.info("********** Archiving files **********");
        UserConfig ucfg = UserConfig.getInstance();
        File currentArchiveDir = new File(ucfg.archivePath() + File.separator + dateTime);
        Utils.createDirIfNotExist(currentArchiveDir);
        zipEmbossingFiles(currentArchiveDir);
        zipRegistryFiles(currentArchiveDir);
    }

    private void zipEmbossingFiles(File currentArchiveDir) throws IOException{
        log.info("********** Zipping embossing files **********");
        UserConfig ucfg = UserConfig.getInstance();
        FileOutputStream fos = new FileOutputStream(currentArchiveDir + File.separator + "embossing.zip");
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File srcDir = new File(ucfg.embossingDestinationPath());

        File[] filesToZip = srcDir.listFiles();
        //фильтруем вложенные директории
        Arrays.stream(filesToZip).filter(f->!f.isDirectory()).forEach(f-> {
            try{
                Utils.zipFile(f, zipOut);
            }catch (Exception e){
                log.error("Could not zip {}", f.getAbsolutePath());
            }

        });
        zipOut.close();
        fos.close();
    }

    private void zipRegistryFiles(File currentArchiveDir) throws IOException{
        log.info("********** Zipping registry files**********");
        UserConfig ucfg = UserConfig.getInstance();
        File fileToZip =  new File(ucfg.embossingRegistryDestinationPath());
        File zipOut = new File(currentArchiveDir + File.separator + "registry.zip");
        Utils.zip(fileToZip, zipOut);
    }

    private void  convertLineEndings(){

        log.info("********** Convert LF to CR LF **********");
        UserConfig ucfg = UserConfig.getInstance();
        File srcFolder = new File(ucfg.embossingDestinationPath());

        File[] sortedFiles = srcFolder.listFiles();

        for(File f: sortedFiles) {
            log.info(f.getName());
            try{
                String content = FileUtils.readFileToString(f, "US-ASCII");
                String converted = content.replaceAll("\n", "\r\n");
                FileUtils.writeStringToFile(f, converted);
            }catch (IOException e){
                log.error(e.getMessage());
            }
        }
    }

    private void sortFiles() throws Exception{
        PronitProcess.sortFiles();
        NovacardProcess.sortFiles();
    }
}
