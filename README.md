**Compiling**

    mvn clean compile assembly:single

**Running**

    java -Dusername=<username> -Dpassword=<password> -Dconfig=<config> -Dclean=0 -DdownloadFilesFromPrime=1 -DrunPronitProcess=1 -DrunNovacardProcess=1 -DcleanPronitOutputDir=1 -DcleanNovacardOutputDir=0 -jar embossing-<version>.jar

    -Dusername - Prime server ssh login
    -Dpassword - Prime server ssh password
    -Dconfig - path to configuration file
    -Dclean - clean source files on Prime after download
    -DdownloadFilesFromPrime - download files from Prime (if set to "0", run with current local copy of files)
    -DrunPronitProcess - process files Pronit way
    -DrunNovacardProcess - process files Novacard way