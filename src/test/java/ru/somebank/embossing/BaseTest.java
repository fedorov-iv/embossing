package ru.somebank.embossing;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.somebank.embossing.clients.SftpClient;
import ru.somebank.embossing.models.FileToFolderMap;
import ru.somebank.embossing.utils.Utils;
import ru.somebank.embossing.utils.config.Config;
import ru.somebank.embossing.utils.config.UserConfig;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.ZipOutputStream;

public class BaseTest {

    private static final Logger log = LoggerFactory.getLogger(BaseTest.class);

    @Test
    public void upAndDownTest() throws Exception{

        //Config cfg = Config.getInstance();
        UserConfig ucfg = UserConfig.getInstance();

        String username = System.getProperty("username");
        String password = System.getProperty("password");

        SftpClient client = new SftpClient(ucfg.primeSFTPHost(), ucfg.primeSFTPPort(), username, password);
        client.upload("pom.xml", "/userdata/prime_acfs/RAIFF_raiff/pos_files");

        File downloadDir = new File("./testdownload");
        boolean isDownloadDirCreated = downloadDir.mkdir();
        Assert.assertTrue(isDownloadDirCreated);

        client.download(downloadDir.getPath(), "/userdata/prime_acfs/RAIFF_raiff/pos_files/**");
        client.close();

        FileUtils.cleanDirectory(downloadDir);

        boolean isDeleted = downloadDir.delete();
        Assert.assertTrue(isDeleted);


    }

    @Test
    public void configTest(){
        //Config cfg = Config.getInstance();
        UserConfig ucfg = UserConfig.getInstance();
        Assert.assertNotEquals("", ucfg.SDCBatPath());
    }


    @Test
    public void copyEmbossingTest() throws Exception{
        Config cfg = Config.getInstance();
        UserConfig ucfg = UserConfig.getInstance();

        String username = System.getProperty("username");
        String password = System.getProperty("password");

        File downloadDir = new File(ucfg.embossingDestinationPath());

        if(!downloadDir.exists()){
            boolean isDownloadDirCreated = downloadDir.mkdir();
            Assert.assertTrue(isDownloadDirCreated);
        }


        File registryDir = new File(ucfg.embossingRegistryDestinationPath());

        if(!registryDir.exists()){
            boolean isRegistryDirCreated = registryDir.mkdir();
            Assert.assertTrue(isRegistryDirCreated);
        }


        SftpClient client = new SftpClient(ucfg.primeSFTPHost(), ucfg.primeSFTPPort(), username, password);

        //copy embossing
        client.copyEmbossingFiles(ucfg.embossingDestinationPath(), ucfg.primeEmbossingSourcesPath());

        //copy registry
        client.copyRegistryFiles(ucfg.embossingRegistryDestinationPath(), ucfg.primeEmbossingRegistrySourcesPath());
        client.close();

        FileUtils.cleanDirectory(registryDir);
        FileUtils.cleanDirectory(downloadDir);

    }


    @Test
    public void sortFilesTest() throws Exception{

        //Config cfg = Config.getInstance();
        UserConfig ucfg = UserConfig.getInstance();

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(ucfg.filemapJsonPath());


        File srcFolder = new File(ucfg.embossingDestinationPath());

        log.info("***** Sorting files *****");

        Gson gson = new Gson();
        FileToFolderMap[] fileMaps = gson.fromJson(new FileReader(file), FileToFolderMap[].class);
        for(FileToFolderMap fileMap : fileMaps){

            log.info("folder -" + fileMap.getDir());

            //создать директорию, если не существует
            File targetDir = new File(fileMap.getDir());
            if(!targetDir.exists())
                targetDir.mkdir();


            ArrayList<String> fileMasks = fileMap.getFiles();

            for(String fileMask: fileMasks){
                FileFilter filter = new RegexFileFilter(fileMask);
                File[] sortedFiles = srcFolder.listFiles(filter);

                for(File sortedFile: sortedFiles) {
                    log.info(sortedFile.getName());

                    String[] fileNameParts = sortedFile.getName().split("\\.");
                    String newFileName = "";

                    if(fileMap.getExtension().isEmpty()){
                        newFileName =  sortedFile.getName();
                    }else{
                        newFileName = fileNameParts[0] + fileMap.getExtension();
                    }
                    log.info("Renamed to: {}", newFileName);
                    FileUtils.copyFile(sortedFile, new File(fileMap.getDir() + File.separator + newFileName));
                }

            }

        }

    }

    @Test
    public void currentDateTest(){
        SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMdd");
        String dateString = sdf.format(new Date());
        System.out.println(dateString);
        Assert.assertTrue(dateString.matches("^[0-9]{8}$"));

    }

    @Test
    public void convertLineEndings(){
        Config cfg = Config.getInstance();
        UserConfig ucfg = UserConfig.getInstance();
        File srcFolder = new File(ucfg.embossingDestinationPath());

        File[] sortedFiles = srcFolder.listFiles();

        for(File f: sortedFiles) {
            log.info(f.getName());
            try{
                String content = FileUtils.readFileToString(f, "US-ASCII");
                String converted = content.replaceAll("\n", "\r\n");
                //log.info("File content: {}", content);
                FileUtils.writeStringToFile(f, converted);
            }catch (IOException e){
                log.error(e.getMessage());
            }

            //FileUtils.copyFile(sortedFile, new File(fileMap.getDir() + File.separator + sortedFile.getName()));
        }

    }



    @Test
    public void prepareOutputTest() throws Exception{
        log.info("**********  Moving files to input SCPE folder **********");
        UserConfig ucfg = UserConfig.getInstance();
        Config cfg = Config.getInstance();

        // создаем директорию
        File inputFolder = new File(ucfg.SDCInputPath());
        Utils.createDirIfNotExist(inputFolder);

        // чистим директорию
        FileUtils.cleanDirectory(inputFolder);

        // переносим файлы
        File srcDir = new File(ucfg.sortedPronitOutFilesBaseDir());
        FileUtils.copyDirectory(srcDir, inputFolder);
        log.info("Files successfully copied from {} to {}", srcDir.getPath(), inputFolder.getPath());

    }

    @Test
    public void eofConvertionTest(){
        log.info("********** Convert LF to CR LF **********");

        Config cfg = Config.getInstance();
        UserConfig ucfg = UserConfig.getInstance();

        File srcFolder = new File(ucfg.embossingDestinationPath());

        File[] sortedFiles = srcFolder.listFiles();

        for(File f: sortedFiles) {
            log.info(f.getName());
            try{
                String content = FileUtils.readFileToString(f, "UTF-8");
                String converted = content.replaceAll("\n", "\r\n");
                //log.info("File content: {}", content);
                FileUtils.writeStringToFile(f, converted);
            }catch (IOException e){
                log.error(e.getMessage());
            }

            //FileUtils.copyFile(sortedFile, new File(fileMap.getDir() + File.separator + sortedFile.getName()));
        }
    }

    @Test
    public void ecryptTest(){


       /* Config cfg = Config.getInstance();
        Encryptor encryptor = Encryptor.getInstance();
        //String encryptedText = encryptor.encrypt("1386(ifedor)");
        //System.out.println(encryptedText);
        String decryptedText = encryptor.decrypt(cfg.getPassword());
        Assert.assertEquals("1386(ifedor)", decryptedText);*/

    }

    @Test
    public void cleanEmbossingFromDirectory(){
        Config cfg = Config.getInstance();
        UserConfig ucfg = UserConfig.getInstance();
        File dir = new File(ucfg.embossingDestinationPath());
        Utils.cleanEmbossingFilesFromDirectory(dir);

    }

    @Test
    public void zipLargeDirectory() throws IOException{
        String dateTime = Utils.currentDateTime();

        log.info("********** Zipping registry files**********");
        Config cfg = Config.getInstance();
        UserConfig ucfg = UserConfig.getInstance();
        String sourceFile = ucfg.embossingRegistryDestinationPath();
        FileOutputStream fos = new FileOutputStream(ucfg.archivePath() + File.separator + "registry" + dateTime + ".zip");
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File fileToZip = new File(sourceFile);

        Utils.zipFile(fileToZip, zipOut);
        zipOut.close();
        fos.close();

    }



}
