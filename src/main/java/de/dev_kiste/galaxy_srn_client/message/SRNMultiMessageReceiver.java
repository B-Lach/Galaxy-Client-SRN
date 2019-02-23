package de.dev_kiste.galaxy_srn_client.message;

/**
 * @author Benny Lach
 */
public interface SRNMultiMessageReceiver {
    void handleNewMessage(SRNMultiMessage message);
}
