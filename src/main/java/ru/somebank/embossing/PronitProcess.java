package ru.somebank.embossing;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.somebank.embossing.models.FileToFolderMap;
import ru.somebank.embossing.models.ProcessMap;
import ru.somebank.embossing.utils.Utils;
import ru.somebank.embossing.utils.config.UserConfig;

import java.io.*;
import java.util.ArrayList;

public class PronitProcess implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(PronitProcess.class);

    public void run(){
        log.info("Pronit process started...");

        try{

            //сортировка файлов .out
            //sortFiles();

            // переносит файлы в папку input для эмбоссера
            prepareInput();


            // чистит директорию Pronit Output
            if(Main.cleanPronitOutputDir)
                cleanOutput();

            // запуск .bat (обработка файлов input)
            runSDCApp();

        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException();

        }

    }

    /**
     * Сортирует эмбоссинговые файлы по папкам внутри input на О
     */
    public static void sortFiles() throws Exception{

        log.info("********** Sorting Pronit files **********");

        //Config cfg = Config.getInstance();
        UserConfig ucfg = UserConfig.getInstance();
        File srcFolder = new File(ucfg.embossingDestinationPath());


        // создаем папку input
        File inputFolder = new File(ucfg.sortedPronitOutFilesBaseDir());
        Utils.createDirIfNotExist(inputFolder);

        // чистим директорию
        FileUtils.cleanDirectory(inputFolder);

        File jsonFile = new File(ucfg.filemapJsonPath());
        if(!jsonFile.exists())
            throw new FileNotFoundException();

        JsonReader reader = new JsonReader(new FileReader(jsonFile));
        ProcessMap processMap = new Gson().fromJson(reader, ProcessMap.class);

        for(FileToFolderMap fileMap : processMap.getPronit()){

            log.info(fileMap.getDir());

            //создать директорию, если не существует
            File targetDir = new File(fileMap.getDir());
            Utils.createDirIfNotExist(targetDir);


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


    /**
     *  Чистит папку input на С scpe и переносит в нее файлы для обработки
     */
    private void prepareInput() throws Exception{
        log.info("**********  Moving Pronit files to input folder **********");
        //Config cfg = Config.getInstance();
        UserConfig ucfg = UserConfig.getInstance();

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

    private void cleanOutput() throws Exception{
        log.info("**********  Cleaning Pronit Output **********");
        UserConfig ucfg = UserConfig.getInstance();
        File outputFolder = new File(ucfg.SDCOutputPath());
        FileUtils.cleanDirectory(outputFolder);

    }

    private void runSDCApp(){
        log.info("**********  Running SDC .bat **********");

        //Config cfg = Config.getInstance();
        UserConfig ucfg = UserConfig.getInstance();

        Runtime runtime = Runtime.getRuntime();
        try {
            Process p1 = runtime.exec(String.format("cmd /c start %s", ucfg.SDCBatPath()));
            InputStream is = p1.getInputStream();
            int i = 0;
            while( (i = is.read() ) != -1) {
                System.out.print((char)i);
            }
        } catch(IOException ioException) {
            log.error(ioException.getMessage());
        }
    }
}
