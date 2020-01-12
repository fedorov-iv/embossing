package ru.somebank.embossing.models;

import java.util.ArrayList;

public class FileToFolderMap {

    private String dir;
    private ArrayList<String> files;
    private String extension;

    public String getDir() {
        return dir;
    }

    public ArrayList<String> getFiles() {
        return files;
    }

    public String getExtension(){
        return extension;
    }


}
