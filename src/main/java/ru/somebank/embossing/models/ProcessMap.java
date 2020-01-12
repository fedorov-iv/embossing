package ru.somebank.embossing.models;

import java.util.ArrayList;

public class ProcessMap {
    private ArrayList<FileToFolderMap> pronit;
    private ArrayList<FileToFolderMap> novacard;

    public ArrayList<FileToFolderMap> getPronit() {
        return pronit;
    }
    public ArrayList<FileToFolderMap> getNovacard() {
        return novacard;
    }
}
