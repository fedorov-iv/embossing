package ru.somebank.embossing.clients;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;
import java.util.Vector;


/**
 * Класс Sftp-клиент
 */
public class SftpClient {

    private static final Logger log = LoggerFactory.getLogger(SftpClient.class);

    private String host;
    private int port;
    private String username;
    private String password;
    private Session session;
    private Channel channel;
    private ChannelSftp channelSftp;

    public SftpClient(String host, int port, String username, String password) throws Exception {

        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;

        log.info(String.format("Login - %s, Host - %s, Port - %s", username, host, port));
        JSch jsch = new JSch();
        session = jsch.getSession(username, host, port);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications", "password");
        session.setConfig(config);

        session.setPassword(password);
        session.connect();
        log.info("Host connected");
        channel = session.openChannel("sftp");
        channel.connect();
        log.info("Sftp channel opened and connected");

        channelSftp = (ChannelSftp) channel;

    }


    /**
     * Копирует файл из указаннной папки по sftp на удаленный сервер
     *
     * @param remoteFilePath путь к папке на удаленном сервере
     * @param localFilePath  путь к локальной папке
     * @throws Exception
     */
    public void upload(String localFilePath, String remoteFilePath) throws Exception {
        channelSftp.cd(remoteFilePath);
        channelSftp.put(localFilePath, "test.upload");
        log.info("File {} transferred successfully to {}{}", localFilePath, this.host, remoteFilePath);
    }

    /**
     * Копирует файл из указаннной папки по sftp с удаленного сервера
     *
     * @param remoteFilePath путь к папке на удаленном сервере
     * @param localFilePath  путь к локальной папке
     * @throws Exception
     */
    public void download(String localFilePath, String remoteFilePath) throws Exception {
        channelSftp.get(remoteFilePath, localFilePath);
        log.info("File {}{} transferred successfully to {}", this.host, remoteFilePath, localFilePath);

    }

    /**
     * Копирует embossing файлы с удаленного сервера по шаблону
     *
     * @param remoteFilePath путь к папке на удаленном сервере
     * @param localFilePath  путь к локальной папке
     * @throws Exception
     */
    public void copyEmbossingFiles(String localFilePath, String remoteFilePath) throws Exception {

        Vector filelist = channelSftp.ls(remoteFilePath);

        for (Object f : filelist) {

            ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) f;

            /*if (entry.getFilename().matches("^[0-9]+_(mc|vc|rc)[0-9]{1,2}.out$")) {
                channelSftp.get(remoteFilePath + "/" + entry.getFilename(), localFilePath);
                log.info("File {}{} transferred successfully to {}", this.host, remoteFilePath + "/" + entry.getFilename(), localFilePath);
            }*/

            if (!entry.getFilename().endsWith(".cmd") && !entry.getFilename().endsWith(".bat")) {

                try{
                    channelSftp.get(remoteFilePath + "/" + entry.getFilename(), localFilePath);
                    log.info("File {}{} transferred successfully to {}", this.host, remoteFilePath + "/" + entry.getFilename(), localFilePath);
                }catch (Exception e){

                }

            }
        }
    }

    /**
     * Удаляем embossing-файлы
     *
     * @param remoteFilePath путь к папке на удаленном сервере
     * @throws Exception
     */
    public void cleanEmbossingFiles(String remoteFilePath) throws Exception {
        Vector filelist = channelSftp.ls(remoteFilePath);

        for (Object f : filelist) {

            ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) f;

            if (entry.getFilename().matches("^[0-9]+_(mc|vc|rc)[0-9]{1,2}.out$")) {
                try {
                    channelSftp.rm(remoteFilePath + "/" + entry.getFilename());
                    log.info("File {}{} successfully removed", this.host, remoteFilePath + "/" + entry.getFilename());
                } catch (Exception e) {
                    log.info("Failed to remove {}{}", this.host, remoteFilePath + "/" + entry.getFilename());
                }
            }
        }
    }

    /**
     * Удаляем директории с реестром
     *
     * @param remoteFilePath путь к папке на удаленном сервере
     * @throws Exception
     */
    public void cleanRegistryFiles(String remoteFilePath) throws Exception {
        Vector dirlist = channelSftp.ls(remoteFilePath);

        for (Object d : dirlist) {

            ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) d;

            if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {

                try {
                    channelSftp.rmdir(remoteFilePath + "/" + entry.getFilename());
                    log.info("Directory {}{} successfully removed", this.host, remoteFilePath + "/" + entry.getFilename());
                } catch (Exception e) {

                    Vector filelist = channelSftp.ls(remoteFilePath + "/" + entry.getFilename());

                    for (Object f : filelist){

                        ChannelSftp.LsEntry entry1 = (ChannelSftp.LsEntry) f;
                        if(!entry1.getFilename().equals(".") && !entry1.getFilename().equals("..")){
                            try {
                                channelSftp.rm(remoteFilePath + "/" + entry.getFilename() + "/" +entry1.getFilename());
                                log.info("File {}{} successfully removed", this.host, remoteFilePath + "/" + entry.getFilename() + "/" +entry1.getFilename());
                            } catch (Exception e1) {
                                log.info("Failed to remove {}{}", this.host, remoteFilePath + "/" + entry.getFilename() + "/" +entry1.getFilename());
                            }
                        }

                    }


                }finally{
                    try {
                        channelSftp.rmdir(remoteFilePath + "/" + entry.getFilename());
                        log.info("Directory {}{} successfully removed", this.host, remoteFilePath + "/" + entry.getFilename());
                    }catch (Exception e2){
                        log.info(e2.getMessage());
                    }

                }

            }
        }
    }

    /**
     * Копирует файлы реестра (embossing директория/reestr/**)
     *
     * @param remoteFilePath путь к папке на удаленном сервере
     * @param localFilePath  путь к локальной папке
     * @throws Exception
     */
    public void copyRegistryFiles(String localFilePath, String remoteFilePath) throws Exception {

        Vector filelist = channelSftp.ls(remoteFilePath);

        for (Object f : filelist) {

            ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) f;

            if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {

                //entry.

                File subDir = new File(localFilePath + File.separator + entry.getFilename());

                if (!subDir.exists()) {
                    boolean success = subDir.mkdir();
                    if (success)
                        log.info("Successfully created {} embossing registry subdirectory", subDir.getPath());
                    else
                        log.error("Could not create {} subdirectory", subDir.getPath());
                }

                //log.info(remoteFilePath + "/" + entry.getFilename() + "/**");
                try {
                    channelSftp.get(remoteFilePath + "/" + entry.getFilename() + "/**", localFilePath + File.separator + subDir.getName());
                    log.info("Files {}{} transferred successfully to {}", this.host, remoteFilePath + "/" + entry.getFilename() + "/**", localFilePath + File.separator + subDir.getName());
                } catch (Exception e) {
                    log.info(e.getMessage());
                }

            }
        }
    }


    /**
     * Закрывает SFTP-соединение с удаленным сервером
     */
    public void close() {
        channelSftp.disconnect();
        session.disconnect();
    }


}
