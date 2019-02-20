package de.dev_kiste.galaxy_srn_client.message;

/**
 * Interface used to handle an incoming <code>SRNMessage</code>
 *
 * @author Benny Lach
 */
public interface SRNMessageHandler {
    void received(SRNMessage message);
}
