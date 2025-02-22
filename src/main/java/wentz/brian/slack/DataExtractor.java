package wentz.brian.slack;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.response.conversations.ConversationsHistoryResponse;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DataExtractor {
    private final String token;
    private final Slack slack;
    private final ObjectMapper mapper;

    public DataExtractor(String token) {
        this.token = token;
        this.slack = Slack.getInstance();
        this.mapper = new ObjectMapper();
    }

    public void extractData(String outputPath) throws Exception {
        MethodsClient client = slack.methods(token);
        List<ObjectNode> allData = new ArrayList<>();

        // Get all channels
        ConversationsListResponse channels = client.conversationsList(r -> r
                .types(List.of())
                .limit(1000));

        // For each channel, get history
        for (var channel : channels.getChannels()) {
            ConversationsHistoryResponse history = client.conversationsHistory(r -> r
                    .channel(channel.getId())
                    .limit(1000));

            history.getMessages().forEach(message -> {
                ObjectNode messageNode = mapper.createObjectNode()
                        .put("channel", channel.getName())
                        .put("timestamp", message.getTs())
                        .put("text", message.getText())
                        .put("user", message.getUser())
                        .put("type", message.getType());
                allData.add(messageNode);
            });
        }

        // Write to file
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(outputPath), allData);
    }
}