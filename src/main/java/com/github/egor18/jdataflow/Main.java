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

        Option noFailsafeOption = new Option(null, "no-failsafe", false, "Terminate analysis immediately on any internal error");
        noFailsafeOption.setRequired(false);
        options.addOption(noFailsafeOption);

        return options;
    }

    private static String[] parseClasspath(String classpathString)
    {
        return classpathString.trim().split(File.pathSeparator);
    }

    private static boolean isSubElement(File file, String[] elements)
    {
        if (file == null || elements == null)
        {
            return false;
        }

        try
        {
            String filePath = file.getCanonicalPath();
            for (String element : elements)
            {
                String elementPath = new File(element).getCanonicalPath();
                if (filePath.startsWith(elementPath))
                {
                    return true;
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return false;
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
        String classpath = cmd.getOptionValue("cp");
        String classpathFile = cmd.getOptionValue("cf");
        String outputFile = cmd.getOptionValue("o");
        String[] excludes = cmd.getOptionValues("e");
        String[] includes = cmd.getOptionValues("i");
        boolean noFailsafe = cmd.hasOption("no-failsafe");

        Launcher launcher = new Launcher();
        //launcher.getEnvironment().setNoClasspath(false);
        launcher.getEnvironment().setCommentEnabled(false);
        launcher.getEnvironment().setComplianceLevel(10);
        Arrays.stream(sources).forEach(launcher::addInputResource);
        if (classpath != null)
        {
            launcher.getEnvironment().setSourceClasspath(parseClasspath(classpath));
        }
        else if (classpathFile != null)
        {
            FileInputStream fisTargetFile = new FileInputStream(new File(classpathFile));
            String content = IOUtils.toString(fisTargetFile, Charset.defaultCharset()).trim();
            if (!content.isEmpty())
            {
                launcher.getEnvironment().setSourceClasspath(parseClasspath(content));
            }
        }

        System.out.println("Building model");
        CtModel ctModel = launcher.buildModel();

        CheckersScanner scanner = new CheckersScanner(launcher.getFactory(), !noFailsafe);

        ctModel.getAllTypes().stream()
                             .filter(t -> !isSubElement(t.getPosition().getFile(), excludes)
                                          || isSubElement(t.getPosition().getFile(), includes))
                             .forEach(scanner::scan);

        if (outputFile != null)
        {
            PrintWriter printWriter = new PrintWriter(new FileWriter(outputFile));
            scanner.getWarnings().forEach(w -> printWriter.println("WARNING: " + w));
            printWriter.close();
        }
        else
        {
            scanner.getWarnings().forEach(w -> System.out.println("WARNING: " + w));
        }
    }
}
