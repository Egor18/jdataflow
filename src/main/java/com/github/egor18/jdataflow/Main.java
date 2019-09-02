package com.github.egor18.jdataflow;

import com.github.egor18.jdataflow.scanners.CheckersScanner;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import spoon.Launcher;
import spoon.reflect.CtModel;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;

public class Main
{
    private static Options createCommandLineOptions()
    {
        Options options = new Options();

        Option sourcesOption = new Option("s", "Input sources");
        sourcesOption.setLongOpt("sources");
        sourcesOption.setRequired(true);
        sourcesOption.setArgName("args...");
        sourcesOption.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(sourcesOption);

        Option classpathOption = new Option("cp", "Sources classpath");
        classpathOption.setLongOpt("classpath");
        classpathOption.setRequired(false);
        classpathOption.setArgName("arg");
        classpathOption.setArgs(1);
        options.addOption(classpathOption);

        Option classpathFileOption = new Option("cf", "Text file with sources classpath");
        classpathFileOption.setLongOpt("classpath-file");
        classpathFileOption.setRequired(false);
        classpathFileOption.setArgName("arg");
        classpathFileOption.setArgs(1);
        options.addOption(classpathFileOption);

        Option outputOption = new Option("o", "Path to the file to output the report");
        outputOption.setLongOpt("output");
        outputOption.setRequired(false);
        outputOption.setArgName("arg");
        outputOption.setArgs(1);
        options.addOption(outputOption);

        Option excludeOption = new Option("e", "Exclude these files/dirs from analysis");
        excludeOption.setLongOpt("exclude");
        excludeOption.setRequired(false);
        excludeOption.setArgName("args...");
        excludeOption.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(excludeOption);

        Option includeOption = new Option("i", "Analyze only these files/dirs");
        includeOption.setLongOpt("include");
        includeOption.setRequired(false);
        includeOption.setArgName("args...");
        includeOption.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(includeOption);

        Option relativizeOption = new Option("r", "Relativize paths in report against this path");
        relativizeOption.setLongOpt("relativize");
        relativizeOption.setRequired(false);
        relativizeOption.setArgName("arg");
        relativizeOption.setArgs(1);
        options.addOption(relativizeOption);

        Option noFailsafeOption = new Option(null, "no-failsafe", false, "Terminate analysis immediately on any internal error");
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

        Configuration config = new Configuration();
        config.setSources(cmd.getOptionValues("s"));
        config.setClasspath(cmd.getOptionValue("cp"));
        config.setClasspathFile(cmd.getOptionValue("cf"));
        config.setOutputFile(cmd.getOptionValue("o"));
        config.setExcludes(cmd.getOptionValues("e"));
        config.setIncludes(cmd.getOptionValues("i"));
        config.setRelativizer(cmd.getOptionValue("r"));
        config.setNoFailsafe(cmd.hasOption("no-failsafe"));

        Launcher launcher = new Launcher();
        //launcher.getEnvironment().setNoClasspath(false);
        launcher.getEnvironment().setCommentEnabled(false);
        launcher.getEnvironment().setComplianceLevel(10);
        Arrays.stream(config.getSources()).forEach(launcher::addInputResource);
        if (config.getClasspath() != null)
        {
            launcher.getEnvironment().setSourceClasspath(parseClasspath(config.getClasspath()));
        }
        else if (config.getClasspathFile() != null)
        {
            FileInputStream fisTargetFile = new FileInputStream(new File(config.getClasspathFile()));
            String content = IOUtils.toString(fisTargetFile, Charset.defaultCharset()).trim();
            if (!content.isEmpty())
            {
                launcher.getEnvironment().setSourceClasspath(parseClasspath(content));
            }
        }

        System.out.println("Building model");
        CtModel ctModel = launcher.buildModel();

        CheckersScanner scanner = new CheckersScanner(launcher.getFactory(), config);

        ctModel.getAllTypes().stream()
                             .filter(t -> !config.isInExcludedFile(t))
                             .forEach(scanner::scan);

        if (config.getRelativizer() != null)
        {
            scanner.getWarnings().forEach(w -> w.relativizer = config.getRelativizer());
        }

        if (config.getOutputFile() != null)
        {
            PrintWriter printWriter = new PrintWriter(new FileWriter(config.getOutputFile()));
            scanner.getWarnings().forEach(w -> printWriter.println("WARNING: " + w));
            printWriter.close();
        }
        else
        {
            scanner.getWarnings().forEach(w -> System.out.println("WARNING: " + w));
        }
    }
}
