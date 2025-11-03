package com.slackgrab.core;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.slackgrab.data.ChannelRepository;
import com.slackgrab.data.ConnectionPool;
import com.slackgrab.data.DatabaseManager;
import com.slackgrab.data.MessageRepository;
import com.slackgrab.oauth.OAuthManager;
import com.slackgrab.security.CredentialManager;
import com.slackgrab.slack.MessageCollector;
import com.slackgrab.slack.SlackApiClient;
import com.slackgrab.webhook.WebhookServer;

/**
 * Guice dependency injection module for SlackGrab application
 *
 * Configures all service dependencies and their lifecycles
 */
public class ApplicationModule extends AbstractModule {

    @Override
    protected void configure() {
        // Core services
        bind(ConfigurationManager.class).in(Singleton.class);
        bind(ErrorHandler.class).in(Singleton.class);
        bind(ServiceCoordinator.class).in(Singleton.class);

        // Data layer
        bind(ConnectionPool.class).in(Singleton.class);
        bind(DatabaseManager.class).in(Singleton.class);
        bind(MessageRepository.class).in(Singleton.class);
        bind(ChannelRepository.class).in(Singleton.class);

        // Security
        bind(CredentialManager.class).in(Singleton.class);

        // OAuth
        bind(OAuthManager.class).in(Singleton.class);

        // Slack integration
        bind(SlackApiClient.class).in(Singleton.class);
        bind(MessageCollector.class).in(Singleton.class);

        // Webhook service
        bind(WebhookServer.class).in(Singleton.class);
    }
}
