package ru.somebank.embossing.utils.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

public class UserConfig {

    private static final Logger log = LoggerFactory.getLogger(UserConfig.class);
    private static UserConfig instance;
    private Properties prop;

    /**
     * Хост сервера SFTP Prime
     * @return хост
     */
    public String primeSFTPHost(){
        return prop.getProperty("primeSFTPHost");
    }

    /**
     * Порт сервера SFTP Prime
     * @return порт
     */
    public int primeSFTPPort(){
        return Integer.parseInt(prop.getProperty("primeSFTPPort"));
    }

    /**
     * Путь до директории с исходниками embossing файлов на SFTP Prime
     * @return путь
     */
    public String primeEmbossingSourcesPath(){
        return prop.getProperty("primeEmbossingSourcesPath");
    }

    /**
     * Путь до директории с исходниками реестра embossing файлов на SFTP Prime
     * @return путь
     */
    public String primeEmbossingRegistrySourcesPath(){
        return prop.getProperty("primeEmbossingRegistrySourcesPath");
    }
    /**
     * Путь, куда переложить embossing файлы
     * @return путь
     */
    public String embossingDestinationPath(){
        return prop.getProperty("embossingDestinationPath");
    }

    /**
     * Путь, куда переложить embossing реестр
     * @return путь
     */
    public String embossingRegistryDestinationPath(){
        return prop.getProperty("embossingRegistryDestinationPath");
    }

    /**
     * Путь, куда архивировать embossing-файлы и реестр
     * @return путь
     */
    public String archivePath(){
        return prop.getProperty("archivePath");
    }

    /**
     * Корневая директория для сортировки .out файлов Pronit
     * @return путь
     */
    public String sortedPronitOutFilesBaseDir(){
        return prop.getProperty("sortedPronitOutFilesBaseDir");
    }

    /**
     * Корневая директория для сортировки .out файлов Novacard
     * @return путь
     */
    public String sortedNovacardOutFilesBaseDir(){
        return prop.getProperty("sortedNovacardOutFilesBaseDir");
    }

    /**
     * Путь до папки input (папка эмбоссера)
     * @return путь
     */
    public String SDCInputPath(){ return prop.getProperty("SDCInputPath"); }

    /**
     * Путь до папки output (папка эмбоссера)
     * @return путь
     */
    public String SDCOutputPath(){ return prop.getProperty("SDCOutputPath"); }

    /**
     * Путь до папки input (папка эмбоссера Novacard)
     * @return путь
     */
    public String novacardInputPath(){ return prop.getProperty("novacardInputPath"); }

    /**
     * Путь до папки output(папка эмбоссера Novacard)
     * @return путь
     */
    public String novacardOutputPath(){ return prop.getProperty("novacardOutputPath"); }

    /**
     * Путь до файла filemap.json
     * @return путь
     */
    public String filemapJsonPath(){
        return prop.getProperty("filemapJsonPath");
    }

    /**
     * Путь до исполняемого файла утилиты Novacard
     * @return Путь до файла
     */
    public String novacardBatPath(){
        return prop.getProperty("novacardBatPath");
    }

    /**
     * Путь до исполняемого файла утилиты SDC
     * @return Путь до файла
     */
    public String SDCBatPath(){
        return prop.getProperty("SDCBatPath");
    }

    public static UserConfig getInstance(){

        if(instance != null){
            return instance;
        }

        instance = new UserConfig();
        return instance;

    }

    private UserConfig(){

        try {
            prop = new Properties();
            InputStream inputStream = new FileInputStream(new File(System.getProperty("config")));
            log.info("User config file {} is loaded successfully", System.getProperty("config"));

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("Property file " + System.getProperty("config") + " not found in the classpath");
            }

        } catch (Exception e) {
            log.error("Exception: " + e);
        }

    }
}
