package wentz.brian;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import wentz.brian.slack.DataExtractor;

public class Main {

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption("h", "help", false, "print this message");
        options.addOption("f", "file", true, "the file to load or save users from/to");
        options.addOption("t", "threads", true, "number of threads to use for");
        options.addOption("s", "slack", false, "extract Slack data");
        options.addOption("tk", "token", true, "Slack API token");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar walkusers.jar", options);
            return;
        }

        String filename = null;
        if (cmd.hasOption("f")) {
            filename = cmd.getOptionValue("f");
        }

        int threads = 3;
        if (cmd.hasOption("t")) {
            threads = Integer.parseInt(cmd.getOptionValue("t"));
        }

        if (cmd.hasOption("s")) {
            if (!cmd.hasOption("tk") || filename == null) {
                System.err.println("Slack token and filename are required for Slack data extraction");
                return;
            }

            String token = cmd.getOptionValue("tk");

            System.out.println("Extracting Slack data...");
            var extractor = new DataExtractor(token);
            extractor.extractData(filename);
            System.out.println("Data extracted to: " + filename);
        }

    }
}