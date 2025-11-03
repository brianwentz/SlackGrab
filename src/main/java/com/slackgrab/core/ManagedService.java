package com.slackgrab.core;

/**
 * Interface for services that can be started and stopped
 *
 * All managed services must implement this interface to participate in the
 * application lifecycle managed by ServiceCoordinator
 */
public interface ManagedService {

    /**
     * Start the service
     *
     * @throws Exception if service fails to start
     */
    void start() throws Exception;

    /**
     * Stop the service gracefully
     *
     * @throws Exception if service fails to stop cleanly
     */
    void stop() throws Exception;
}
