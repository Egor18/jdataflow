package com.github.egor18.jdataflow;

import com.github.egor18.jdataflow.scanners.CheckersScanner;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import spoon.Launcher;
import spoon.reflect.CtModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

public class Main
{
    private static Options createCommandLineOptions()
    {
        Options options = new Options();

        Option sourcesOption = new Option("s", "sources to be analyzed");
        sourcesOption.setLongOpt("sources");
        sourcesOption.setRequired(true);
        sourcesOption.setArgName("args...");
        sourcesOption.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(sourcesOption);

        Option classpathOption = new Option("cp", "sources classpath");
        classpathOption.setLongOpt("classpath");
        classpathOption.setRequired(false);
        classpathOption.setArgName("arg");
        classpathOption.setArgs(1);
        options.addOption(classpathOption);

        Option classpathFileOption = new Option("cf", "text file with sources classpath");
        classpathFileOption.setLongOpt("classpath-file");
        classpathFileOption.setRequired(false);
        classpathFileOption.setArgName("arg");
        classpathFileOption.setArgs(1);
        options.addOption(classpathFileOption);

        Option noFailsafeOption = new Option(null, "no-failsafe", false, "terminate analysis immediately on any internal error");
        noFailsafeOption.setRequired(false);
        options.addOption(noFailsafeOption);

        return options;
    }

    private static String[] parseClasspath(String classpathString)
    {
        return classpathString.trim().split(File.pathSeparator);
    }

    public static void main(String[] args) throws IOException
    {
        Options options = createCommandLineOptions();

        CommandLine cmd;
        try
        {
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options, args);
        }
        catch (ParseException e)
        {
            System.out.println("ERROR: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(null);
            formatter.printHelp( " java -jar jdataflow.jar", null, options, null, true);
            System.exit(1);
            return;
        }

        String[] sources = cmd.getOptionValues("s");
        String[] classpath = cmd.getOptionValues("cp");
        String[] classpathFile = cmd.getOptionValues("cf");
        boolean noFailsafe = cmd.hasOption("no-failsafe");

        Launcher launcher = new Launcher();
        //launcher.getEnvironment().setNoClasspath(false);
        launcher.getEnvironment().setCommentEnabled(false);
        launcher.getEnvironment().setComplianceLevel(10);
        Arrays.stream(sources).forEach(launcher::addInputResource);
        if (classpath != null)
        {
            launcher.getEnvironment().setSourceClasspath(parseClasspath(classpath[0]));
        }
        else if (classpathFile != null)
        {
            FileInputStream fisTargetFile = new FileInputStream(new File(classpathFile[0]));
            String content = IOUtils.toString(fisTargetFile, Charset.defaultCharset());
            launcher.getEnvironment().setSourceClasspath(parseClasspath(content));
        }

        System.out.println("Building model");
        CtModel ctModel = launcher.buildModel();

        CheckersScanner scanner = new CheckersScanner(launcher.getFactory(), !noFailsafe);
        ctModel.getAllTypes().forEach(scanner::scan);
        scanner.getWarnings().forEach(w -> System.out.println("WARNING: " + w));
    }
}
