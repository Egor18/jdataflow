# scan utils

Simple utils which (supposedly) allow to run jdataflow analysis on Ant, Maven, and Gradle based projects.  
They analyze build output of your project and attempt to extract sources and classpath, then they run jdataflow using this information.

Usage:  
`scan-ant [options] -- <build command> [build command options]`  
`scan-maven [options] -- <build command> [build command options]`  
`scan-gradle [options] -- <build command> [build command options]`  

Examples:  
`scan-ant.py -o 'report.txt' -- ant clean compile`  
`scan-maven.py -o 'report.txt' -- mvn clean compile`  
`scan-gradle.py -o 'report.txt' -- ./gradlew clean assemble`  
